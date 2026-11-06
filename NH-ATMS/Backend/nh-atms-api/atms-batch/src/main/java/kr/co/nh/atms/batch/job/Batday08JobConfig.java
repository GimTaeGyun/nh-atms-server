package kr.co.nh.atms.batch.job;

import kr.co.nh.atms.batch.mapper.Batday08Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * batday08 : 자동화기기 장애/거래 보고서 집계 Job — AS-IS atmb batday08 이식.
 * Job 빈 이름 = T_BATCH_JOB.PROC_NAME = "batday08" (크론: 00 15 03 * * ?)
 *
 * Step 1) reportBaseStep   — T_DAILY_REPORT01 (기기별) 삭제 후 집계 적재
 *         AS-IS 는 기기별 루프 + 건별 서브집계(장애/5만원권/수표/거래종류)였으나
 *         동일 결과의 set 기반 INSERT...SELECT 로 재구성.
 * Step 2) reportRollupStep — T_DAILY_REPORT02(지역)/03(설치장소)/04(영업본부)/
 *         05(기종)/07(그룹·기종) 을 REPORT01 에서 상향집계 (AS-IS 동일 구조).
 *
 * JobParameters: statDate(yyyyMMdd, identifying) — AS-IS job_date(기본 전일)
 */
@Slf4j
@Configuration
public class Batday08JobConfig {

    public static final String JOB_NAME = "batday08";

    /** 자행(NH) 은행코드 (AS-IS MG '045' → NH '011') */
    @Value("${atms.batch.bank-cd:011}")
    private String bankCd;

    @Bean(name = JOB_NAME)
    public Job batday08Job(JobRepository jobRepository,
                           @Qualifier("reportBaseStep") Step reportBaseStep,
                           @Qualifier("reportRollupStep") Step reportRollupStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(reportBaseStep)
                .next(reportRollupStep)
                .build();
    }

    @Bean
    public Step reportBaseStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               @Qualifier("reportBaseTasklet") Tasklet reportBaseTasklet) {
        return new StepBuilder("batday08.reportBaseStep", jobRepository)
                .tasklet(reportBaseTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step reportRollupStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 @Qualifier("reportRollupTasklet") Tasklet reportRollupTasklet) {
        return new StepBuilder("batday08.reportRollupStep", jobRepository)
                .tasklet(reportRollupTasklet, transactionManager)
                .build();
    }

    /** Step 1) 기기별 장애/거래 집계 → T_DAILY_REPORT01 */
    @Bean
    @StepScope
    public Tasklet reportBaseTasklet(Batday08Mapper mapper,
                                     @Value("#{jobParameters['statDate']}") String statDate) {
        return (contribution, chunkContext) -> {
            long target = mapper.countReportTarget();
            mapper.deleteReport01(statDate);
            int inserted = mapper.insertReport01(statDate, bankCd);

            ExecutionContext jobCtx = contribution.getStepExecution()
                    .getJobExecution().getExecutionContext();
            jobCtx.putLong("targetCnt", target);
            jobCtx.putLong("procCnt", jobCtx.getLong("procCnt", 0L) + inserted);

            log.info("[batday08] 기기별 보고서 집계 trDate={} 대상={} 적재={}", statDate, target, inserted);
            return RepeatStatus.FINISHED;
        };
    }

    /** Step 2) 지역/설치장소/영업본부/기종/그룹·기종별 상향집계 → T_DAILY_REPORT02~05,07 */
    @Bean
    @StepScope
    public Tasklet reportRollupTasklet(Batday08Mapper mapper,
                                       @Value("#{jobParameters['statDate']}") String statDate) {
        return (contribution, chunkContext) -> {
            int total = 0;
            mapper.deleteReport02(statDate);
            total += mapper.insertReport02(statDate);
            mapper.deleteReport03(statDate);
            total += mapper.insertReport03(statDate);
            mapper.deleteReport04(statDate);
            total += mapper.insertReport04(statDate);
            mapper.deleteReport05(statDate);
            total += mapper.insertReport05(statDate);
            mapper.deleteReport07(statDate);
            total += mapper.insertReport07(statDate);

            ExecutionContext jobCtx = contribution.getStepExecution()
                    .getJobExecution().getExecutionContext();
            jobCtx.putLong("procCnt", jobCtx.getLong("procCnt", 0L) + total);

            log.info("[batday08] 보고서 상향집계 trDate={} 적재합계={}", statDate, total);
            return RepeatStatus.FINISHED;
        };
    }
}
