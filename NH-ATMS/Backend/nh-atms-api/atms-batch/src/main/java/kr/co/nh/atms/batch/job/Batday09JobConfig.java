package kr.co.nh.atms.batch.job;

import kr.co.nh.atms.batch.mapper.Batday09Mapper;
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
 * batday09 : 일별 자동화기기 이력정보 Job — AS-IS atmb batday09 이식.
 * Job 빈 이름 = T_BATCH_JOB.PROC_NAME = "batday09" (크론: 00 20 01 * * ?)
 *
 * 대상일자 기준으로 기기·코너·디바이스 적용정보 스냅샷을 T_DAILY_REPORT06 에 적재한다.
 * (삭제 후 INSERT...SELECT — 멱등)
 *
 * JobParameters: statDate(yyyyMMdd, identifying) — AS-IS job_date(기본 전일)
 */
@Slf4j
@Configuration
public class Batday09JobConfig {

    public static final String JOB_NAME = "batday09";

    /** 자행(NH) 은행코드 — AS-IS 는 t_atmgroup.parent_id 를 사용했으나 NH 는 단일은행 상수 */
    @Value("${atms.batch.bank-cd:011}")
    private String bankCd;

    @Bean(name = JOB_NAME)
    public Job batday09Job(JobRepository jobRepository,
                           @Qualifier("atmDailyHistoryStep") Step atmDailyHistoryStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(atmDailyHistoryStep)
                .build();
    }

    @Bean
    public Step atmDailyHistoryStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    @Qualifier("atmDailyHistoryTasklet") Tasklet atmDailyHistoryTasklet) {
        return new StepBuilder("batday09.atmDailyHistoryStep", jobRepository)
                .tasklet(atmDailyHistoryTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet atmDailyHistoryTasklet(Batday09Mapper mapper,
                                          @Value("#{jobParameters['statDate']}") String statDate) {
        return (contribution, chunkContext) -> {
            long target = mapper.countReport06Target();
            int deleted = mapper.deleteReport06(statDate);
            int inserted = mapper.insertReport06(statDate, bankCd);

            ExecutionContext jobCtx = contribution.getStepExecution()
                    .getJobExecution().getExecutionContext();
            jobCtx.putLong("targetCnt", target);
            jobCtx.putLong("procCnt", inserted);

            log.info("[batday09] 기기이력 적재 trDate={} 삭제={} 적재={}", statDate, deleted, inserted);
            return RepeatStatus.FINISHED;
        };
    }
}
