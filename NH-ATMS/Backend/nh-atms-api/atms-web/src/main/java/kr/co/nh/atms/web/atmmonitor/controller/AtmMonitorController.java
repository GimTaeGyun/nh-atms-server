package kr.co.nh.atms.web.atmmonitor.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.atmmonitor.dto.AtmMonitorSearchDto;
import kr.co.nh.atms.web.atmmonitor.entity.AtmMonitor;
import kr.co.nh.atms.web.atmmonitor.service.AtmMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm-monitor")
@RequiredArgsConstructor
public class AtmMonitorController {
    private final AtmMonitorService service;

    @GetMapping
    public ApiResponse<PageResult<AtmMonitor>> list(@ModelAttribute AtmMonitorSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @GetMapping("/{termGroupId}/{termId}/{jnlDate}/{jnlTime}")
    public ApiResponse<AtmMonitor> get(@PathVariable String termGroupId,
                                       @PathVariable String termId,
                                       @PathVariable String jnlDate,
                                       @PathVariable String jnlTime) {
        return ApiResponse.ok(service.get(termGroupId, termId, jnlDate, jnlTime));
    }
}
