package kr.co.nh.atms.batch.job;

import kr.co.nh.atms.batch.mapper.Batday98Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

/**
 * batday98 : 테이블 데이터 정리 Job — AS-IS atmb batday98 이식.
 * Job 빈 이름 = T_BATCH_JOB.PROC_NAME = "batday98" (크론: 00 55 00 * * ?)
 *
 * T_TABLE_MANAGEMENT(KEEP_CYCLE='1', DELETE_YN='1') 에 등록된 이력성 테이블의
 * 보관기간(KEEP_PERIOD) 초과 데이터를 삭제한다. 관리 행이 없거나 비활성이면
 * 해당 테이블은 0건 삭제로 통과 (AS-IS 동일).
 *
 * ※ AS-IS 대상 중 TO-BE 미존재 테이블(t_remoteinterface / t_journalfootnote /
 *   t_atms_crud)은 제외 — MIGRATION-NOTES.md 참조.
 * ※ statDate 파라미터는 JobInstance 식별용일 뿐 로직은 항상 SYSDATE 기준 (AS-IS 동일).
 */
@Slf4j
@Configuration
public class Batday98JobConfig {

    public static final String JOB_NAME = "batday98";

    @Bean(name = JOB_NAME)
    public Job batday98Job(JobRepository jobRepository,
                           @Qualifier("tableCleanupStep") Step tableCleanupStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(tableCleanupStep)
                .build();
    }

    @Bean
    public Step tableCleanupStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 @Qualifier("tableCleanupTasklet") Tasklet tableCleanupTasklet) {
        return new StepBuilder("batday98.tableCleanupStep", jobRepository)
                .tasklet(tableCleanupTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet tableCleanupTasklet(Batday98Mapper mapper) {
        return (contribution, chunkContext) -> {
            List<Map<String, Object>> targets = mapper.selectCleanupTargets();
            for (Map<String, Object> t : targets) {
                log.info("[batday98] 정리 대상 [{}][기준일 {} 이전]", t.get("tableName"), t.get("delDate"));
            }

            long total = 0;
            total += logDelete("T_BATCH_HISTORY", mapper.deleteBatchHistory());
            total += logDelete("T_ATM_MONITOR", mapper.deleteAtmMonitor());
            total += logDelete("T_GW_INTERFACE", mapper.deleteGwInterface());
            total += logDelete("T_NET_RECV_DATA", mapper.deleteNetRecvData());
            total += logDelete("T_NET_SEND_DATA", mapper.deleteNetSendData());
            total += logDelete("T_JOURNAL", mapper.deleteJournal());
            total += logDelete("T_JOURNAL_DATA", mapper.deleteJournalData());
            total += logDelete("T_JOURNAL_ERROR", mapper.deleteJournalError());
            total += logDelete("T_JOURNAL_IMAGE", mapper.deleteJournalImage());
            total += logDelete("T_TRANSACTION_JOURNAL", mapper.deleteTransactionJournal());
            total += logDelete("T_OPERATION_JOURNAL", mapper.deleteOperationJournal());

            ExecutionContext jobCtx = contribution.getStepExecution()
                    .getJobExecution().getExecutionContext();
            jobCtx.putLong("targetCnt", targets.size());
            jobCtx.putLong("procCnt", total);

            log.info("[batday98] 테이블 정리 완료 활성테이블={} 삭제합계={}", targets.size(), total);
            return RepeatStatus.FINISHED;
        };
    }

    private long logDelete(String table, int deleted) {
        log.info("[batday98] [{}] {}건 삭제", table, deleted);
        return deleted;
    }
}
