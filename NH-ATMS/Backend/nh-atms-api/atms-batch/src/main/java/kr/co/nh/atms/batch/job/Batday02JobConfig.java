package kr.co.nh.atms.batch.job;

import kr.co.nh.atms.batch.dto.TxDailyTotalRow;
import kr.co.nh.atms.batch.mapper.StatMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

/**
 * batday02 : 거래실적 집계 Job (Chunk 지향) — AS-IS atmb batday02 논리 동등 이식.
 * Job 빈 이름 = T_BATCH_JOB.PROC_NAME = "batday02" (크론: 00 05 01 * * ?)
 *
 * AS-IS 와의 논리 동등성:
 * - 성공 거래만 집계 (AS-IS tr_result='OK' → TO-BE TR_RESULT='0000', 스키마 재설계에 따른 값 차이)
 * - 집계 차원: 거래코드(TR_CD) / 자행·타행(TR_BANK_TYPE) / 마감구분(CLOSING_TYPE) / 요일(DAY_WEEK)
 * - 건수/금액(방출금액)/수수료 합계
 * - AS-IS 24시간 pivot 컬럼(tr_cnt_00_07…)은 STAT_HOUR 행 구조로 정규화 (정보량 동일)
 * - AS-IS tr_type_cd(거래종류) 차원은 T_TRANSACTION_CODE 미이관으로 제외 (MIGRATION-NOTES 참조)
 *
 * Step 1) txDailyTotalDeleteStep — 대상일자 기존 집계 삭제 (멱등)
 * Step 2) txDailyTotalLoadStep   — Chunk(1000건 커밋):
 *           Reader : MyBatisCursorItemReader — GROUP BY 집계 행 스트리밍
 *           Writer : MyBatisBatchItemWriter  — JDBC Batch INSERT
 *
 * JobParameters: statDate(yyyyMMdd, identifying)
 */
@Slf4j
@Configuration
public class Batday02JobConfig {

    public static final String JOB_NAME = "batday02";
    private static final int CHUNK_SIZE = 1000;

    /** 자행(NH) 은행코드 — 자행/타행 판정 기준 (AS-IS MG '045' → NH '011') */
    @Value("${atms.batch.bank-cd:011}")
    private String bankCd;

    @Bean(name = JOB_NAME)
    public Job batday02Job(JobRepository jobRepository,
                           StatMapper statMapper,
                           @Qualifier("txDailyTotalDeleteStep") Step deleteStep,
                           @Qualifier("txDailyTotalLoadStep") Step loadStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(targetCountListener(statMapper))
                .start(deleteStep)
                .next(loadStep)
                .build();
    }

    /** beforeJob: 소스(저널) 건수를 대상건수로 JobExecutionContext 에 기록 */
    private JobExecutionListener targetCountListener(StatMapper statMapper) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                String statDate = jobExecution.getJobParameters().getString("statDate");
                long target = statMapper.countTxSource(statDate);
                jobExecution.getExecutionContext().putLong("targetCnt", target);
                log.info("[batday02] 집계 시작 statDate={} 소스={}건", statDate, target);
            }
        };
    }

    @Bean
    public Step txDailyTotalDeleteStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       @Qualifier("txDailyTotalDeleteTasklet") Tasklet deleteTasklet) {
        return new StepBuilder("batday02.deleteStep", jobRepository)
                .tasklet(deleteTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet txDailyTotalDeleteTasklet(StatMapper statMapper,
                                             @Value("#{jobParameters['statDate']}") String statDate) {
        return (contribution, chunkContext) -> {
            int deleted = statMapper.deleteTxDailyTotal(statDate);
            log.info("[batday02] 기존 집계 삭제 statDate={} {}건", statDate, deleted);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step txDailyTotalLoadStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     @Qualifier("txDailyTotalReader") MyBatisCursorItemReader<TxDailyTotalRow> reader,
                                     @Qualifier("txDailyTotalWriter") MyBatisBatchItemWriter<TxDailyTotalRow> writer) {
        return new StepBuilder("batday02.loadStep", jobRepository)
                .<TxDailyTotalRow, TxDailyTotalRow>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public MyBatisCursorItemReader<TxDailyTotalRow> txDailyTotalReader(
            SqlSessionFactory sqlSessionFactory,
            @Value("#{jobParameters['statDate']}") String statDate) {
        MyBatisCursorItemReader<TxDailyTotalRow> reader = new MyBatisCursorItemReader<>();
        reader.setName("txDailyTotalReader");
        reader.setSqlSessionFactory(sqlSessionFactory);
        reader.setQueryId("kr.co.nh.atms.batch.mapper.StatMapper.selectTxDailyTotalRows");
        reader.setParameterValues(Map.of("statDate", statDate, "bankCd", bankCd));
        return reader;
    }

    @Bean
    public MyBatisBatchItemWriter<TxDailyTotalRow> txDailyTotalWriter(SqlSessionFactory sqlSessionFactory) {
        MyBatisBatchItemWriter<TxDailyTotalRow> writer = new MyBatisBatchItemWriter<>();
        writer.setSqlSessionFactory(sqlSessionFactory);
        writer.setStatementId("kr.co.nh.atms.batch.mapper.StatMapper.insertTxDailyTotalRow");
        return writer;
    }
}
