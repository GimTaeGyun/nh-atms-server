package kr.co.nh.atms.web.agentstatus.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.agentstatus.dto.AgentStatusSearchDto;
import kr.co.nh.atms.web.agentstatus.entity.AgentStatus;
import kr.co.nh.atms.web.agentstatus.service.AgentStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent-status")
@RequiredArgsConstructor
public class AgentStatusController {
    private final AgentStatusService service;

    @GetMapping
    public ApiResponse<PageResult<AgentStatus>> list(@ModelAttribute AgentStatusSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @GetMapping("/{atmIp}")
    public ApiResponse<AgentStatus> get(@PathVariable String atmIp) {
        return ApiResponse.ok(service.get(atmIp));
    }
}
