package kr.co.nh.atms.web.daytotal.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.daytotal.dto.DayTotalSearchDto;
import kr.co.nh.atms.web.daytotal.entity.DayTotalRow;
import kr.co.nh.atms.web.daytotal.service.DayTotalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마감관리(마감조회) — T_DAY_TOTAL 마감항목별 조회.
 */
@RestController
@RequestMapping("/api/day-total")
@RequiredArgsConstructor
public class DayTotalController {
    private final DayTotalService service;

    @GetMapping
    public ApiResponse<PageResult<DayTotalRow>> list(@ModelAttribute DayTotalSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }
}
