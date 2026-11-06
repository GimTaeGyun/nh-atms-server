package kr.co.nh.atms.batch.scheduler;

import kr.co.nh.atms.batch.entity.BatchJob;
import kr.co.nh.atms.batch.mapper.BatchJobMapper;
import kr.co.nh.atms.batch.service.BatchRunService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 앱 종료(@PreDestroy) 시 스케줄러 스레드가 남지 않아야 한다.
 * JEUS 에서 atms-batch 만 재배포/중지해도 이전 버전 스케줄러가 JVM 에 남지 않게 하기 위함.
 */
class DynamicBatchSchedulerTest {

    private static boolean schedulerThreadAlive() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.isAlive() && t.getName().startsWith("atms-batch-"));
    }

    @Test
    void shutdown_stopsSchedulerThreads() throws Exception {
        DynamicBatchScheduler scheduler =
                new DynamicBatchScheduler(mock(BatchJobMapper.class), mock(BatchRunService.class));
        BatchJob job = BatchJob.builder().procName("TEST01").cronExpr("* * * * * *").useFlag("Y").build();

        assertTrue(scheduler.schedule(job));
        for (int i = 0; i < 30 && !schedulerThreadAlive(); i++) {
            Thread.sleep(100);
        }
        assertTrue(schedulerThreadAlive(), "잡 등록 후 스케줄러 스레드가 떠 있어야 함");

        scheduler.shutdown();
        for (int i = 0; i < 50 && schedulerThreadAlive(); i++) {
            Thread.sleep(100);
        }
        assertFalse(schedulerThreadAlive(), "shutdown 후 스케줄러 스레드가 남으면 안 됨");
    }
}
