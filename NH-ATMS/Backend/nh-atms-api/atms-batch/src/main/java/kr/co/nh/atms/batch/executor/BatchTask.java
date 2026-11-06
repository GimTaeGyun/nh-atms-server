package kr.co.nh.atms.batch.executor;

import kr.co.nh.atms.batch.entity.BatchJob;

/**
 * 배치 작업 실행 로직 인터페이스.
 * 프로세스명(procName)별로 구현 빈을 등록하면 스케줄/수동 실행 시 디스패치된다.
 * 매칭되는 구현이 없으면 BatchRunService 가 no-op(성공/0건) 으로 처리한다.
 */
public interface BatchTask {

    /** 이 태스크가 담당하는 프로세스명 (T_BATCH_JOB.PROC_NAME) */
    String procName();

    /** 실제 배치 로직 수행. 멱등(재실행 안전)하게 구현할 것. */
    BatchTaskResult run(BatchJob job) throws Exception;
}
