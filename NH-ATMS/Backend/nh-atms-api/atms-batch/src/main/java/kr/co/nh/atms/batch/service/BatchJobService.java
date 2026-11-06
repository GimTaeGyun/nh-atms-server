package kr.co.nh.atms.batch.service;

import kr.co.nh.atms.batch.dto.BatchJobSearchRequest;
import kr.co.nh.atms.batch.entity.BatchHistory;
import kr.co.nh.atms.batch.entity.BatchJob;
import kr.co.nh.atms.batch.mapper.BatchHistoryMapper;
import kr.co.nh.atms.batch.mapper.BatchJobMapper;
import kr.co.nh.atms.batch.scheduler.DynamicBatchScheduler;
import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치작업관리 서비스 — 조회/CRUD/수동실행/이력 + 스케줄 재등록.
 */
@Service
@RequiredArgsConstructor
public class BatchJobService {

    private final BatchJobMapper jobMapper;
    private final BatchHistoryMapper historyMapper;
    private final BatchRunService runService;
    private final DynamicBatchScheduler scheduler;

    /** 목록(페이징) */
    public PageResult<BatchJob> list(BatchJobSearchRequest req) {
        long total = jobMapper.countList(req);
        return PageResult.of(jobMapper.selectList(req), total, req);
    }

    /** 단건 */
    public BatchJob get(String procName) {
        BatchJob job = jobMapper.selectByProcName(procName);
        if (job == null) {
            throw new BusinessException("BATCH_NOT_FOUND", "배치잡을 찾을 수 없습니다: " + procName);
        }
        return job;
    }

    /** 신규 등록 */
    @Transactional
    public BatchJob create(BatchJob job) {
        validateCron(job.getCronExpr());
        if (jobMapper.selectByProcName(job.getProcName()) != null) {
            throw new BusinessException("BATCH_DUPLICATE", "이미 존재하는 프로세스명입니다: " + job.getProcName());
        }
        if (job.getUseFlag() == null) job.setUseFlag("N");
        if (job.getSortOrder() == null) job.setSortOrder(0);
        jobMapper.insert(job);
        BatchJob saved = jobMapper.selectByProcName(job.getProcName());
        scheduler.schedule(saved);
        return saved;
    }

    /** 수정 */
    @Transactional
    public BatchJob update(String procName, BatchJob job) {
        validateCron(job.getCronExpr());
        BatchJob existing = get(procName);
        existing.setJobDesc(job.getJobDesc());
        existing.setCronExpr(job.getCronExpr());
        existing.setHostName(job.getHostName());
        existing.setUseFlag(job.getUseFlag());
        existing.setSortOrder(job.getSortOrder() == null ? 0 : job.getSortOrder());
        jobMapper.update(existing);
        BatchJob saved = jobMapper.selectByProcName(procName);
        scheduler.schedule(saved);   // 재등록 (USE_FLAG=N 이면 내부에서 해제)
        return saved;
    }

    /** 삭제 */
    @Transactional
    public void delete(String procName) {
        get(procName);
        scheduler.unschedule(procName);
        jobMapper.delete(procName);
    }

    /** 수동 실행(재실행) */
    public BatchHistory runManual(String procName) {
        get(procName);
        return runService.run(procName, "MANUAL");
    }

    /** 수동실행 파라미터 스키마 (AS-IS Task.paramSpec() 이식) */
    public java.util.Map<String, Object> paramSpec(String procName) {
        get(procName);   // 존재 검증
        return kr.co.nh.atms.batch.job.support.BatchJobParamSpecs.of(procName);
    }

    /** 실행 이력(페이징) */
    public PageResult<BatchHistory> history(String procName, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        int offset = (p - 1) * s;
        long total = historyMapper.countByProcName(procName);
        var content = historyMapper.selectByProcName(procName, offset, s);
        int totalPages = (int) Math.ceil((double) total / s);
        return PageResult.<BatchHistory>builder()
                .content(content)
                .totalCount(total)
                .page(p)
                .size(s)
                .totalPages(totalPages)
                .build();
    }

    /** Cron 표현식 검증 (Spring 6필드) */
    public void validateCron(String cron) {
        if (cron == null || cron.isBlank()) {
            throw new BusinessException("BATCH_CRON_EMPTY", "Cron 표현식은 필수입니다.");
        }
        if (!CronExpression.isValidExpression(cron)) {
            throw new BusinessException("BATCH_CRON_INVALID",
                    "유효하지 않은 Cron 표현식입니다: " + cron);
        }
    }
}
