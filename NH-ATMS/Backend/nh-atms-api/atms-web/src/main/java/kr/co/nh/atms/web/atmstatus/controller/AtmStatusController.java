package kr.co.nh.atms.web.atmstatus.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.atmstatus.dto.AtmStatusSearchDto;
import kr.co.nh.atms.web.atmstatus.entity.AtmStatus;
import kr.co.nh.atms.web.atmstatus.service.AtmStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm-status")
@RequiredArgsConstructor
public class AtmStatusController {
    private final AtmStatusService service;

    @GetMapping
    public ApiResponse<PageResult<AtmStatus>> list(@ModelAttribute AtmStatusSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }
    @GetMapping("/{termGroupId}/{termId}")
    public ApiResponse<AtmStatus> get(@PathVariable String termGroupId, @PathVariable String termId) {
        return ApiResponse.ok(service.get(termGroupId, termId));
    }
    @PutMapping
    public ApiResponse<Void> upsert(@RequestBody AtmStatus s) {
        service.upsert(s);
        return ApiResponse.ok(null, "상태 갱신 성공");
    }
}
