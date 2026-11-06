package kr.co.nh.atms.web.faultfrequent.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.faultfrequent.dto.FaultFrequent;
import kr.co.nh.atms.web.faultfrequent.dto.FaultFrequentSearchDto;
import kr.co.nh.atms.web.faultfrequent.service.FaultFrequentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fault-frequent")
@RequiredArgsConstructor
public class FaultFrequentController {
    private final FaultFrequentService service;

    @GetMapping
    public ApiResponse<PageResult<FaultFrequent>> list(@ModelAttribute FaultFrequentSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }
}
