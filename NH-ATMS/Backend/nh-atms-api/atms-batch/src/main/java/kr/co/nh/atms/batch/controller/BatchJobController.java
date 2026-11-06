package kr.co.nh.atms.batch.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.batch.dto.BatchJobSearchRequest;
import kr.co.nh.atms.batch.entity.BatchHistory;
import kr.co.nh.atms.batch.entity.BatchJob;
import kr.co.nh.atms.batch.service.BatchJobService;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 배치작업관리 REST API (atms-batch, 포트 8082).
 * 프론트 /batch/job 화면에서 사용.
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchJobController {

    private final BatchJobService service;

    /** 배치잡 목록(페이징) */
    @GetMapping("/jobs")
    public ApiResponse<PageResult<BatchJob>> list(BatchJobSearchRequest req) {
        return ApiResponse.ok(service.list(req));
    }

    /** 배치잡 단건 */
    @GetMapping("/jobs/{procName}")
    public ApiResponse<BatchJob> get(@PathVariable String procName) {
        return ApiResponse.ok(service.get(procName));
    }

    /** 신규 등록 */
    @PostMapping("/jobs")
    public ApiResponse<BatchJob> create(@Valid @RequestBody BatchJob job) {
        return ApiResponse.ok(service.create(job), "등록되었습니다.");
    }

    /** 수정 */
    @PutMapping("/jobs/{procName}")
    public ApiResponse<BatchJob> update(@PathVariable String procName,
                                        @Valid @RequestBody BatchJob job) {
        return ApiResponse.ok(service.update(procName, job), "수정되었습니다.");
    }

    /** 삭제 */
    @DeleteMapping("/jobs/{procName}")
    public ApiResponse<Void> delete(@PathVariable String procName) {
        service.delete(procName);
        return ApiResponse.ok(null, "삭제되었습니다.");
    }

    /** 수동 실행(재실행) */
    @PostMapping("/jobs/{procName}/run")
    public ApiResponse<BatchHistory> run(@PathVariable String procName) {
        return ApiResponse.ok(service.runManual(procName), "실행되었습니다.");
    }

    /** 수동실행 파라미터 스키마 (AS-IS Task.paramSpec() 이식 — 프론트 입력폼 구성용) */
    @GetMapping("/jobs/{procName}/param-spec")
    public ApiResponse<Map<String, Object>> paramSpec(@PathVariable String procName) {
        return ApiResponse.ok(service.paramSpec(procName));
    }

    /** 실행 이력(페이징) */
    @GetMapping("/jobs/{procName}/history")
    public ApiResponse<PageResult<BatchHistory>> history(@PathVariable String procName,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.history(procName, page, size));
    }

    /** Cron 표현식 검증 */
    @GetMapping("/cron/validate")
    public ApiResponse<Map<String, Object>> validateCron(@RequestParam String expr) {
        boolean valid = CronExpression.isValidExpression(expr);
        String next = null;
        if (valid) {
            try {
                var n = CronExpression.parse(expr).next(java.time.LocalDateTime.now());
                next = n == null ? null : n.toString();
            } catch (Exception ignored) {
            }
        }
        return ApiResponse.ok(Map.of("valid", valid, "next", next == null ? "" : next));
    }
}
