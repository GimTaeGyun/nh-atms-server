package kr.co.nh.atms.web.transactionjournal.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.transactionjournal.dto.TransactionJournalDetailDto;
import kr.co.nh.atms.web.transactionjournal.dto.TransactionJournalSearchDto;
import kr.co.nh.atms.web.transactionjournal.entity.TransactionJournal;
import kr.co.nh.atms.web.transactionjournal.service.TransactionJournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction-journal")
@RequiredArgsConstructor
public class TransactionJournalController {
    private final TransactionJournalService service;

    @GetMapping
    public ApiResponse<PageResult<TransactionJournal>> list(@ModelAttribute TransactionJournalSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @GetMapping("/{termGroupId}/{termId}/{jnlDate}/{jnlTime}")
    public ApiResponse<TransactionJournal> get(@PathVariable String termGroupId,
                                               @PathVariable String termId,
                                               @PathVariable String jnlDate,
                                               @PathVariable String jnlTime) {
        return ApiResponse.ok(service.get(termGroupId, termId, jnlDate, jnlTime));
    }

    @GetMapping("/{termGroupId}/{termId}/{jnlDate}/{jnlTime}/detail")
    public ApiResponse<TransactionJournalDetailDto> detail(@PathVariable String termGroupId,
                                                           @PathVariable String termId,
                                                           @PathVariable String jnlDate,
                                                           @PathVariable String jnlTime) {
        return ApiResponse.ok(service.getDetail(termGroupId, termId, jnlDate, jnlTime));
    }
}
