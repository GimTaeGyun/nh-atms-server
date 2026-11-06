package kr.co.nh.atms.batch.scheduler;

import kr.co.nh.atms.batch.entity.BatchJob;
import kr.co.nh.atms.batch.mapper.BatchJobMapper;
import kr.co.nh.atms.batch.service.BatchRunService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * T_BATCH_JOB(USE_FLAG='Y') 기반 동적 cron 스케줄러.
 * - 부팅 완료 시 사용중 잡을 등록.
 * - CRUD 시 BatchJobService 가 reschedule/unschedule 호출.
 * - HOST_NAME 이 지정된 잡은 현재 호스트와 일치할 때만 등록.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicBatchScheduler {

    private final BatchJobMapper jobMapper;
    private final BatchRunService runService;

    private final ThreadPoolTaskScheduler taskScheduler = createScheduler();
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    private static ThreadPoolTaskScheduler createScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(4);
        s.setThreadNamePrefix("atms-batch-");
        s.setWaitForTasksToCompleteOnShutdown(true);
        s.initialize();
        return s;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        int count = 0;
        for (BatchJob job : jobMapper.selectEnabled()) {
            if (schedule(job)) count++;
        }
        log.info("[BATCH-SCHED] 기동 완료 — 등록된 잡 {}개", count);
    }

    /** 잡 (재)등록. 등록 성공 시 true. */
    public synchronized boolean schedule(BatchJob job) {
        unschedule(job.getProcName());
        if (!"Y".equals(job.getUseFlag())) {
            return false;
        }
        if (!hostMatches(job.getHostName())) {
            log.info("[BATCH-SCHED] 호스트 불일치로 미등록 proc={} host={}", job.getProcName(), job.getHostName());
            return false;
        }
        try {
            CronTrigger trigger = new CronTrigger(job.getCronExpr(),
                    TimeZone.getTimeZone("Asia/Seoul"));
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> runService.run(job.getProcName(), "SCHEDULE"), trigger);
            if (future != null) {
                futures.put(job.getProcName(), future);
                log.info("[BATCH-SCHED] 등록 proc={} cron='{}'", job.getProcName(), job.getCronExpr());
                return true;
            }
        } catch (Exception e) {
            log.error("[BATCH-SCHED] 등록 실패 proc={} cron='{}' : {}",
                    job.getProcName(), job.getCronExpr(), e.getMessage());
        }
        return false;
    }

    /** 잡 등록 해제. */
    public synchronized void unschedule(String procName) {
        ScheduledFuture<?> future = futures.remove(procName);
        if (future != null) {
            future.cancel(false);
            log.info("[BATCH-SCHED] 해제 proc={}", procName);
        }
    }

    /**
     * 앱 종료 시 등록된 잡을 모두 해제하고 스케줄러 스레드를 끝낸다.
     * 스케줄러가 Spring 빈이 아니라 자동 종료되지 않으므로 직접 정리한다 — 그대로 두면
     * JEUS 에서 이 앱만 재배포/중지할 때 이전 버전의 스케줄러 스레드(비데몬)가 JVM 에 남아
     * 닫힌 컨텍스트로 잡 실행을 계속 시도하고, 이전 클래스로더도 해제되지 않는다.
     * 이미 실행 중인 잡은 중단하지 않는다 (waitForTasksToCompleteOnShutdown=true).
     */
    @PreDestroy
    public synchronized void shutdown() {
        futures.values().forEach(f -> f.cancel(false));
        futures.clear();
        taskScheduler.shutdown();
        log.info("[BATCH-SCHED] 종료 — 등록 잡 해제, 스케줄러 중지");
    }

    private boolean hostMatches(String hostName) {
        if (hostName == null || hostName.isBlank()) return true;
        try {
            return hostName.equalsIgnoreCase(InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            return false;
        }
    }
}
