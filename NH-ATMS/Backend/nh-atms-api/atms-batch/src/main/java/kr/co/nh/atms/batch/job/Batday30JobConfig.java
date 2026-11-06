package kr.co.nh.atms.batch.job;

import kr.co.nh.atms.batch.mapper.StatMapper;
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

/**
 * batday30 : 사용자 부서(금고)명 동기화 Job — MG batday30 이식 (Phase 2 첫 배치).
 *
 * T_ATM_GROUP.TERM_GROUP_NAME 이 변경된 경우 T_USER.DEPT_NAME 을 일괄 동기화한다.
 * MG 원본은 ① dept_cd = term_group_id 셋업 ② 명칭 갱신의 2단계였으나,
 * NH 사용자 모델은 DEPT_CD 를 직접 관리하므로 ② 명칭 동기화만 수행한다.
 *
 * - JobParameters: statDate(yyyyMMdd, identifying) — 일 단위 JobInstance 식별용(로직 미사용)
 * - 멱등성: 불일치 행만 UPDATE — 반복 실행 안전
 * - 등록: T_BATCH_JOB 에 PROC_NAME='batday30' 행 추가 시 화면/스케줄에서 즉시 사용 가능
 */
@Slf4j
@Configuration
public class Batday30JobConfig {

    public static final String JOB_NAME = "batday30";

    @Bean(name = JOB_NAME)
    public Job batday30Job(JobRepository jobRepository,
                           @Qualifier("userDeptNameSyncStep") Step userDeptNameSyncStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(userDeptNameSyncStep)
                .build();
    }

    @Bean
    public Step userDeptNameSyncStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     @Qualifier("userDeptNameSyncTasklet") Tasklet userDeptNameSyncTasklet) {
        return new StepBuilder("batday30.userDeptNameSyncStep", jobRepository)
                .tasklet(userDeptNameSyncTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet userDeptNameSyncTasklet(StatMapper statMapper) {
        return (contribution, chunkContext) -> {
            long target = statMapper.countUserDeptSyncTarget();
            int updated = statMapper.updateUserDeptName();

            ExecutionContext jobCtx = contribution.getStepExecution()
                    .getJobExecution().getExecutionContext();
            jobCtx.putLong("targetCnt", target);
            jobCtx.putLong("procCnt", updated);

            log.info("[batday30] 부서명 동기화 대상={}건 갱신={}건", target, updated);
            return RepeatStatus.FINISHED;
        };
    }
}
