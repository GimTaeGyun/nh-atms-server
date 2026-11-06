package kr.co.nh.atms.web.cash.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.cash.dto.CashSearchDto;
import kr.co.nh.atms.web.cash.entity.CashPosition;
import kr.co.nh.atms.web.cash.service.CashService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {
    private final CashService service;

    @GetMapping
    public ApiResponse<PageResult<CashPosition>> list(@ModelAttribute CashSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }
}
