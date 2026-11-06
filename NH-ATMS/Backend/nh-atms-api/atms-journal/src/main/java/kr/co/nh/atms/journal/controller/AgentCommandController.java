package kr.co.nh.atms.journal.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.journal.dto.CommandIssueRequest;
import kr.co.nh.atms.journal.dto.CommandListResponse;
import kr.co.nh.atms.journal.dto.CommandResultRequest;
import kr.co.nh.atms.journal.entity.AgentCommand;
import kr.co.nh.atms.journal.service.AgentCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 서버→에이전트 원격제어 명령 큐 엔드포인트 (계약: command 태그).
 *
 *   POST /api/agent/{atmId}/command                  : (운영자) 원격제어 명령 발행
 *   GET  /api/agent/{atmId}/commands?max=10          : (에이전트) 대기 명령 폴링
 *   POST /api/agent/{atmId}/command/{cmdId}/ack      : (에이전트) 수령 확인
 *   POST /api/agent/{atmId}/command/{cmdId}/result   : (에이전트) 실행 결과 통보
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentCommandController {

    private final AgentCommandService service;

    /** 운영자/시스템 원격제어 명령 발행. */
    @PostMapping("/{atmId}/command")
    public ApiResponse<Map<String, Object>> issue(@PathVariable String atmId,
                                                   @Valid @RequestBody CommandIssueRequest req) {
        AgentCommand cmd = service.issue(atmId, req);
        return ApiResponse.ok(Map.of("cmdId", cmd.getCmdId(), "status", cmd.getStatus()),
                "명령이 발행되었습니다.");
    }

    /** 에이전트 폴링: 대기 명령 목록. */
    @GetMapping("/{atmId}/commands")
    public ApiResponse<CommandListResponse> poll(@PathVariable String atmId,
                                                 @RequestParam(defaultValue = "10") int max) {
        return ApiResponse.ok(service.poll(atmId, max));
    }

    /** 에이전트 수령 확인(W→A). */
    @PostMapping("/{atmId}/command/{cmdId}/ack")
    public ApiResponse<Map<String, Object>> ack(@PathVariable String atmId,
                                                 @PathVariable String cmdId) {
        service.ack(atmId, cmdId);
        return ApiResponse.ok(Map.of("cmdId", cmdId, "ack", true));
    }

    /** 에이전트 실행 결과 통보(→S/F). */
    @PostMapping("/{atmId}/command/{cmdId}/result")
    public ApiResponse<Map<String, Object>> result(@PathVariable String atmId,
                                                    @PathVariable String cmdId,
                                                    @Valid @RequestBody CommandResultRequest req) {
        service.result(atmId, cmdId, req);
        return ApiResponse.ok(Map.of("cmdId", cmdId, "success", req.isSuccess()));
    }
}
