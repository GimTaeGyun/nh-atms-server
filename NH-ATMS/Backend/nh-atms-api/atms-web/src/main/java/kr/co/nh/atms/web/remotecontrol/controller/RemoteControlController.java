package kr.co.nh.atms.web.remotecontrol.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.remotecontrol.dto.RemoteCommandIssueRequest;
import kr.co.nh.atms.web.remotecontrol.dto.RemoteCommandSearchDto;
import kr.co.nh.atms.web.remotecontrol.entity.RemoteCommand;
import kr.co.nh.atms.web.remotecontrol.service.RemoteControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 기기 원격제어 (운영자 발행/이력 조회).
 *
 *   POST /api/remote-control/commands            : 원격제어 명령 발행 (T_AGENT_COMMAND STATUS=W)
 *   GET  /api/remote-control/commands            : 명령 발행이력 조회(검색+페이징)
 *   GET  /api/remote-control/commands/{cmdId}    : 명령 단건 조회(처리결과 확인)
 */
@RestController
@RequestMapping("/api/remote-control")
@RequiredArgsConstructor
public class RemoteControlController {

    private final RemoteControlService service;

    @GetMapping("/commands")
    public ApiResponse<PageResult<RemoteCommand>> list(@ModelAttribute RemoteCommandSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @GetMapping("/commands/{cmdId}")
    public ApiResponse<RemoteCommand> get(@PathVariable String cmdId) {
        return ApiResponse.ok(service.get(cmdId));
    }

    @PostMapping("/commands")
    public ApiResponse<RemoteCommand> issue(@Valid @RequestBody RemoteCommandIssueRequest req) {
        return ApiResponse.ok(service.issue(req), "명령이 발행되었습니다.");
    }
}
