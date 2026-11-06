package kr.co.nh.atms.batch.service;

import kr.co.nh.atms.batch.entity.BatchHistory;
import kr.co.nh.atms.batch.entity.BatchJob;
import kr.co.nh.atms.batch.executor.BatchTask;
import kr.co.nh.atms.batch.executor.BatchTaskResult;
import kr.co.nh.atms.batch.executor.SpringBatchJobRunner;
import kr.co.nh.atms.batch.mapper.BatchHistoryMapper;
import kr.co.nh.atms.batch.mapper.BatchJobMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 배치 단건 실행 + 이력 기록 담당.
 * - 디스패치 우선순위:
 *   ① procName 과 동일한 이름의 Spring Batch Job 빈 → SpringBatchJobRunner 로 실행
 *   ② legacy BatchTask 구현 (Spring Batch 미전환 단순 배치용 fallback)
 *   ③ 둘 다 없으면 no-op
 * - 시작/종료 시 T_BATCH_JOB 상태 갱신 + T_BATCH_HISTORY 적재 (기존 화면 호환 유지).
 */
@Slf4j
@Service
public class BatchRunService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmmss");

    private final BatchJobMapper jobMapper;
    private final BatchHistoryMapper historyMapper;
    private final SpringBatchJobRunner springBatchJobRunner;
    private final Map<String, BatchTask> taskIndex;
    private final String hostName;

    public BatchRunService(BatchJobMapper jobMapper,
                           BatchHistoryMapper historyMapper,
                           SpringBatchJobRunner springBatchJobRunner,
                           List<BatchTask> tasks) {
        this.jobMapper = jobMapper;
        this.historyMapper = historyMapper;
        this.springBatchJobRunner = springBatchJobRunner;
        this.taskIndex = tasks.stream()
                .collect(Collectors.toMap(BatchTask::procName, Function.identity(),
                        (a, b) -> a, ConcurrentHashMap::new));
        this.hostName = resolveHost();
    }

    private static String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 배치 실행.
     * @param runType SCHEDULE | MANUAL
     */
    public BatchHistory run(String procName, String runType) {
        BatchJob job = jobMapper.selectByProcName(procName);
        if (job == null) {
            throw new IllegalArgumentException("존재하지 않는 배치잡: " + procName);
        }

        LocalDateTime start = LocalDateTime.now();
        String startDate = start.format(DF);
        String startTime = start.format(TF);

        BatchHistory hist = BatchHistory.builder()
                .procName(procName)
                .hostName(hostName)
                .runType(runType)
                .statusCd("RUNNING")
                .startDate(startDate)
                .startTime(startTime)
                .build();
        historyMapper.insert(hist);
        jobMapper.updateRunStart(procName, startDate, startTime);

        log.info("[BATCH] 실행 시작 proc={} type={} histId={}", procName, runType, hist.getHistId());

        BatchTaskResult result = BatchTaskResult.empty();
        String status = "SUCCESS";
        String errMsg = null;
        try {
            if (springBatchJobRunner.supports(procName)) {
                // ① Spring Batch Job 실행 (메타테이블 BATCH_* 에 상세 실행정보 기록)
                SpringBatchJobRunner.Outcome outcome = springBatchJobRunner.run(procName);
                result = BatchTaskResult.of(outcome.getTargetCnt(), outcome.getProcCnt(), outcome.getErrCnt());
                if (!outcome.isSuccess()) {
                    status = "FAIL";
                    errMsg = abbreviate(outcome.getErrMsg(), 1000);
                    log.error("[BATCH] Spring Batch Job 실패 proc={} jobExecutionId={} : {}",
                            procName, outcome.getJobExecutionId(), outcome.getErrMsg());
                } else {
                    log.info("[BATCH] Spring Batch Job 완료 proc={} jobExecutionId={}",
                            procName, outcome.getJobExecutionId());
                }
            } else {
                // ② legacy BatchTask fallback
                BatchTask task = taskIndex.get(procName);
                if (task != null) {
                    result = task.run(job);
                } else {
                    log.warn("[BATCH] 등록된 Job/BatchTask 없음 → no-op 처리 proc={}", procName);
                }
            }
        } catch (Exception e) {
            status = "FAIL";
            errMsg = abbreviate(e.toString(), 1000);
            log.error("[BATCH] 실행 실패 proc={} : {}", procName, e.getMessage(), e);
        }

        LocalDateTime end = LocalDateTime.now();
        hist.setStatusCd(status);
        hist.setEndDate(end.format(DF));
        hist.setEndTime(end.format(TF));
        hist.setDelayTime(java.time.Duration.between(start, end).toMillis());
        hist.setTargetCnt(result.getTargetCnt());
        hist.setProcCnt(result.getProcCnt());
        hist.setErrCnt(result.getErrCnt());
        hist.setErrMsg(errMsg);
        historyMapper.updateResult(hist);
        jobMapper.updateRunEnd(procName, hist.getEndDate(), hist.getEndTime());

        log.info("[BATCH] 실행 종료 proc={} status={} delay={}ms", procName, status, hist.getDelayTime());
        return hist;
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
