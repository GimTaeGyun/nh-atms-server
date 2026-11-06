package kr.co.nh.atms.journal.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.journal.dto.DownloadResultNotifyRequest;
import kr.co.nh.atms.journal.dto.JournalModeNotifyRequest;
import kr.co.nh.atms.journal.service.AgentNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 에이전트→서버 통보 엔드포인트 (계약: notify 태그).
 *
 *   POST /api/agent/notify/journal-mode     : 저널모드 변경 통보
 *   POST /api/agent/notify/download-result  : 다운로드 결과 통보
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/notify")
@RequiredArgsConstructor
public class AgentNotifyController {

    private final AgentNotifyService service;

    @PostMapping("/journal-mode")
    public ApiResponse<Map<String, Object>> journalMode(@Valid @RequestBody JournalModeNotifyRequest req) {
        service.journalMode(req);
        return ApiResponse.ok(Map.of("atmId", req.getAtmId(), "jnlmode", req.getJnlmode()));
    }

    @PostMapping("/download-result")
    public ApiResponse<Map<String, Object>> downloadResult(@Valid @RequestBody DownloadResultNotifyRequest req) {
        service.downloadResult(req);
        return ApiResponse.ok(Map.of("atmId", req.getAtmId(), "success", req.isSuccess()));
    }
}
