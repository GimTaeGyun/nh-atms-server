package kr.co.nh.atms.web.operationjournal.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.operationjournal.dto.OperationJournalDetailDto;
import kr.co.nh.atms.web.operationjournal.dto.OperationJournalSearchDto;
import kr.co.nh.atms.web.operationjournal.entity.OperationJournal;
import kr.co.nh.atms.web.operationjournal.service.OperationJournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation-journal")
@RequiredArgsConstructor
public class OperationJournalController {
    private final OperationJournalService service;

    @GetMapping
    public ApiResponse<PageResult<OperationJournal>> list(@ModelAttribute OperationJournalSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @GetMapping("/{termGroupId}/{termId}/{jnlDate}/{jnlTime}/detail")
    public ApiResponse<OperationJournalDetailDto> detail(@PathVariable String termGroupId,
                                                         @PathVariable String termId,
                                                         @PathVariable String jnlDate,
                                                         @PathVariable String jnlTime) {
        return ApiResponse.ok(service.getDetail(termGroupId, termId, jnlDate, jnlTime));
    }
}
