package kr.co.nh.atms.batch.executor;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Spring Batch 실행 어댑터.
 *
 * T_BATCH_JOB.PROC_NAME 과 동일한 이름의 Spring Batch {@link Job} 빈이 있으면
 * BatchRunService 는 (legacy BatchTask 대신) 이 러너로 실행을 위임한다.
 * 기존 REST API / 프론트 화면 / T_BATCH_HISTORY 기록 방식은 그대로 유지된다.
 *
 * 실행 규칙:
 * - JobParameters: statDate(전일, identifying)
 * - 동일 statDate 가 FAILED 상태면 → 같은 JobInstance 재시작(실패 Step/Chunk 지점부터 재개)
 * - 동일 statDate 가 COMPLETED 상태면 → rerun.ts 파라미터를 더해 새 JobInstance 로 재실행
 *   (DELETE 후 INSERT 멱등 설계이므로 재집계 안전 — 기존 시스템의 "항상 실행" 의미 보존)
 * - 동일 statDate 가 실행 중이면 → JobExecutionAlreadyRunningException → 실패 처리
 *   (다중 인스턴스/중복 트리거 방어 — MG의 run 플래그 락을 JobRepository 가 대체)
 */
@Slf4j
@Component
public class SpringBatchJobRunner {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobsByName;   // 빈 이름(=PROC_NAME) → Job

    public SpringBatchJobRunner(JobLauncher jobLauncher, Map<String, Job> jobsByName) {
        this.jobLauncher = jobLauncher;
        this.jobsByName = jobsByName;
        log.info("[BATCH] Spring Batch Job 등록: {}", jobsByName.keySet());
    }

    /** procName 에 해당하는 Spring Batch Job 존재 여부 */
    public boolean supports(String procName) {
        return jobsByName.containsKey(procName);
    }

    /** Job 실행 후 결과(성공여부/건수/오류메시지) 반환 */
    public Outcome run(String procName) {
        Job job = jobsByName.get(procName);
        String statDate = LocalDate.now().minusDays(1).format(DF);

        JobExecution exec;
        try {
            try {
                // 신규 실행 또는 FAILED JobInstance 재시작
                exec = jobLauncher.run(job, params(statDate, null));
            } catch (JobInstanceAlreadyCompleteException e) {
                // 이미 성공한 일자 → 새 JobInstance 로 재실행 (멱등 재집계)
                log.info("[BATCH] 완료된 JobInstance 재실행 proc={} statDate={}", procName, statDate);
                exec = jobLauncher.run(job, params(statDate, System.currentTimeMillis()));
            }
        } catch (JobExecutionAlreadyRunningException e) {
            return Outcome.failed("동일 일자 배치가 이미 실행 중입니다 (중복 실행 차단): " + e.getMessage());
        } catch (Exception e) {
            return Outcome.failed(e.toString());
        }

        return toOutcome(exec);
    }

    private JobParameters params(String statDate, Long rerunTs) {
        JobParametersBuilder b = new JobParametersBuilder().addString("statDate", statDate);
        if (rerunTs != null) {
            b.addLong("rerun.ts", rerunTs);
        }
        return b.toJobParameters();
    }

    /**
     * JobExecution → 기존 T_BATCH_HISTORY 건수 체계 매핑.
     * 건수 우선순위: JobExecutionContext(targetCnt/procCnt/errCnt) → Step 집계 합산.
     */
    private Outcome toOutcome(JobExecution exec) {
        boolean success = exec.getStatus() == BatchStatus.COMPLETED;
        ExecutionContext ctx = exec.getExecutionContext();

        long readSum = 0, writeSum = 0, skipSum = 0;
        for (StepExecution se : exec.getStepExecutions()) {
            readSum += se.getReadCount();
            writeSum += se.getWriteCount();
            skipSum += se.getSkipCount();
        }

        long target = ctx.containsKey("targetCnt") ? ctx.getLong("targetCnt") : readSum;
        long proc = ctx.containsKey("procCnt") ? ctx.getLong("procCnt") : writeSum;
        long err = ctx.containsKey("errCnt") ? ctx.getLong("errCnt") : skipSum;

        String errMsg = null;
        if (!success) {
            errMsg = exec.getAllFailureExceptions().stream()
                    .findFirst()
                    .map(Throwable::toString)
                    .orElse(exec.getExitStatus().getExitDescription());
        }

        return Outcome.builder()
                .success(success)
                .jobExecutionId(exec.getId())
                .targetCnt(target)
                .procCnt(proc)
                .errCnt(err)
                .errMsg(errMsg)
                .build();
    }

    /** 실행 결과 */
    @Getter
    @Builder
    public static class Outcome {
        private final boolean success;
        private final Long jobExecutionId;
        private final long targetCnt;
        private final long procCnt;
        private final long errCnt;
        private final String errMsg;

        static Outcome failed(String errMsg) {
            return Outcome.builder().success(false).errMsg(errMsg).build();
        }
    }
}
