package kr.co.nh.atms.journal.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.dto.JournalReceiveResult;
import kr.co.nh.atms.journal.service.JournalDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * ATM Agent → ATMS 저널 수신 엔드포인트.
 *
 *   POST /api/journal/receive       (단건 JifMessage)
 *   POST /api/journal/receive/batch (배열 JifMessage)
 *
 * 모든 에러는 ApiResponse 안에서 JournalReceiveResult.reason 으로 전달.
 * 200 OK 자체는 "수신 접수" 의미일 뿐, parsed/stored 플래그를 확인할 것.
 */
@Slf4j
@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
public class JournalReceiveController {

    private final JournalDispatchService dispatch;

    @PostMapping("/receive")
    public ApiResponse<JournalReceiveResult> receive(@Valid @RequestBody JifMessage msg) {
        return ApiResponse.ok(dispatch.dispatch(msg));
    }

    @PostMapping("/receive/batch")
    public ApiResponse<List<JournalReceiveResult>> receiveBatch(@RequestBody List<JifMessage> list) {
        List<JournalReceiveResult> out = new ArrayList<>(list.size());
        for (JifMessage m : list) out.add(dispatch.dispatch(m));
        return ApiResponse.ok(out);
    }
}
