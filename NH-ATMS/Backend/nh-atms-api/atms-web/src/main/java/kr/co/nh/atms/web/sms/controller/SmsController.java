package kr.co.nh.atms.web.sms.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.sms.dto.SmsSearchDto;
import kr.co.nh.atms.web.sms.dto.SmsSendRequest;
import kr.co.nh.atms.web.sms.entity.SmsSendData;
import kr.co.nh.atms.web.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * SMS 전송.
 *
 *   GET  /api/sms        : SMS 발송이력 조회(검색+페이징)
 *   POST /api/sms        : SMS 전송 등록(발송 대기)
 */
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService service;

    @GetMapping
    public ApiResponse<PageResult<SmsSendData>> list(@ModelAttribute SmsSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @PostMapping
    public ApiResponse<SmsSendData> send(@Valid @RequestBody SmsSendRequest req) {
        return ApiResponse.ok(service.send(req), "SMS 전송이 등록되었습니다.");
    }
}
