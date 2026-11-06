package kr.co.nh.atms.web.journal.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.journal.dto.JournalDetailDto;
import kr.co.nh.atms.web.journal.dto.JournalSearchDto;
import kr.co.nh.atms.web.journal.entity.Journal;
import kr.co.nh.atms.web.journal.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 통합 저널조회 (메뉴 5010 /journal/list).
 * 모든 jnl_kind 의 T_JOURNAL 헤더를 한 페이지에서 조회 + 원본 본문(T_JOURNAL_DATA) 미리보기.
 *
 * 주의: 이미 정의된 {@code /api/journal/**} URL prefix 는 atms-journal 수신 엔드포인트가 사용 중이므로
 *       atms-web 의 조회용 prefix 는 {@code /api/journals} 로 분리.
 */
@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {
    private final JournalService service;

    @GetMapping
    public ApiResponse<PageResult<Journal>> list(@ModelAttribute JournalSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @GetMapping("/{termGroupId}/{termId}/{jnlDate}/{jnlTime}/detail")
    public ApiResponse<JournalDetailDto> detail(@PathVariable String termGroupId,
                                                @PathVariable String termId,
                                                @PathVariable String jnlDate,
                                                @PathVariable String jnlTime) {
        return ApiResponse.ok(service.getDetail(termGroupId, termId, jnlDate, jnlTime));
    }
}
