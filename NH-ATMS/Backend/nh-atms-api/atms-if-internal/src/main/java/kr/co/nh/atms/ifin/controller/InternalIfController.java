package kr.co.nh.atms.ifin.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.ifin.dto.DrawerCheckRequest;
import kr.co.nh.atms.ifin.dto.DrawerCheckResponse;
import kr.co.nh.atms.ifin.dto.FaultMessengerRequest;
import kr.co.nh.atms.ifin.dto.FaultSmsRequest;
import kr.co.nh.atms.ifin.dto.IfSendResult;
import kr.co.nh.atms.ifin.service.InternalIfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대내(atmi) 인터페이스 엔드포인트 (레거시 /int-if/*.do 신규 포팅).
 *
 *   POST /api/if/internal/drawer-check     : 시재 조회 (ATMONST10100)
 *   POST /api/if/internal/fault-sms        : 장애 SMS  (SMSONSS10200)
 *   POST /api/if/internal/fault-messenger  : 장애 메신저 (ATMBYDD10100)
 */
@Slf4j
@RestController
@RequestMapping("/api/if/internal")
@RequiredArgsConstructor
public class InternalIfController {

    private final InternalIfService service;

    @PostMapping("/drawer-check")
    public ApiResponse<DrawerCheckResponse> drawerCheck(@Valid @RequestBody DrawerCheckRequest req) {
        return ApiResponse.ok(service.drawerCheck(req));
    }

    @PostMapping("/fault-sms")
    public ApiResponse<IfSendResult> faultSms(@Valid @RequestBody FaultSmsRequest req) {
        return ApiResponse.ok(service.faultSms(req));
    }

    @PostMapping("/fault-messenger")
    public ApiResponse<IfSendResult> faultMessenger(@Valid @RequestBody FaultMessengerRequest req) {
        return ApiResponse.ok(service.faultMessenger(req));
    }
}
