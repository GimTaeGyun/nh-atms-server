package kr.co.nh.atms.ifex.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.ifex.dto.AutoServiceCallRequest;
import kr.co.nh.atms.ifex.dto.AutoServiceCallResult;
import kr.co.nh.atms.ifex.dto.ArrivalRequest;
import kr.co.nh.atms.ifex.dto.CleanManageRequest;
import kr.co.nh.atms.ifex.dto.CornerChangeRequest;
import kr.co.nh.atms.ifex.dto.CornerTimeRequest;
import kr.co.nh.atms.ifex.dto.DeparturePlanRequest;
import kr.co.nh.atms.ifex.dto.ExternalIfResult;
import kr.co.nh.atms.ifex.dto.FacilityReportRequest;
import kr.co.nh.atms.ifex.dto.MachineInfoRequest;
import kr.co.nh.atms.ifex.dto.ManyFailureRequest;
import kr.co.nh.atms.ifex.dto.ManyFailureResultRequest;
import kr.co.nh.atms.ifex.dto.MissingMsgRequest;
import kr.co.nh.atms.ifex.dto.RepairResultRequest;
import kr.co.nh.atms.ifex.dto.ServiceCallCancelRequest;
import kr.co.nh.atms.ifex.dto.ServiceCallChangeRequest;
import kr.co.nh.atms.ifex.dto.ServiceCallRequest;
import kr.co.nh.atms.ifex.dto.StatusNotifyRequest;
import kr.co.nh.atms.ifex.service.ExternalIfInboundService;
import kr.co.nh.atms.ifex.service.ExternalIfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대외(atme) 인터페이스 엔드포인트 (레거시 /ext-if/*.do 신규 포팅).
 *
 *   POST /api/if/external/status-notify          : 개국/장애/상태 통보 (EM_01001110)
 *   POST /api/if/external/service-call           : 출동 요청          (EM_01001120)
 *   POST /api/if/external/auto-service-call       : 자동 출동요청      (EM_01001120, 업체선정)
 *   POST /api/if/external/service-call/cancel     : 출동 취소          (EM_01001160)
 *   POST /api/if/external/service-call/change     : 출동 변경          (EM_01001170)
 *   POST /api/if/external/many-failure            : 동시다발 장애      (EM_01001180)
 *   POST /api/if/external/corner-change           : 코너 변경          (SM_02001160)
 *   POST /api/if/external/machine-info            : 기기정보 통보      (IQ_04001140)
 *
 *   -- 수신(대외업체 -> ATMS) --
 *   POST /api/if/external/inbound/departure-plan  : 도착예정          (EM_01001130)
 *   POST /api/if/external/inbound/arrival         : 도착통보          (EM_01001140)
 *   POST /api/if/external/inbound/repair-result   : 조치결과          (EM_01001150)
 *   POST /api/if/external/inbound/many-failure-result : 동시다발 조치결과 (EM_01001190)
 *   POST /api/if/external/inbound/missing-msg     : 결번요청          (IQ_04001110)
 *   POST /api/if/external/inbound/corner-time     : 코너입퇴실시간    (IQ_04001130)
 *   POST /api/if/external/inbound/facility-report : 시설물관리업무보고 (SM_02001180)
 *   POST /api/if/external/inbound/clean-manage    : 환경관리업무      (SM_02001190)
 */
@Slf4j
@RestController
@RequestMapping("/api/if/external")
@RequiredArgsConstructor
public class ExternalIfController {

    private final ExternalIfService service;
    private final ExternalIfInboundService inboundService;

    @PostMapping("/status-notify")
    public ApiResponse<ExternalIfResult> statusNotify(@Valid @RequestBody StatusNotifyRequest req) {
        return ApiResponse.ok(service.statusNotify(req));
    }

    @PostMapping("/service-call")
    public ApiResponse<ExternalIfResult> serviceCall(@Valid @RequestBody ServiceCallRequest req) {
        return ApiResponse.ok(service.serviceCall(req));
    }

    @PostMapping("/auto-service-call")
    public ApiResponse<AutoServiceCallResult> autoServiceCall(@Valid @RequestBody AutoServiceCallRequest req) {
        return ApiResponse.ok(service.autoServiceCall(req));
    }

    @PostMapping("/service-call/cancel")
    public ApiResponse<ExternalIfResult> serviceCallCancel(@Valid @RequestBody ServiceCallCancelRequest req) {
        return ApiResponse.ok(service.serviceCallCancel(req));
    }

    @PostMapping("/service-call/change")
    public ApiResponse<ExternalIfResult> serviceCallChange(@Valid @RequestBody ServiceCallChangeRequest req) {
        return ApiResponse.ok(service.serviceCallChange(req));
    }

    @PostMapping("/many-failure")
    public ApiResponse<ExternalIfResult> manyFailure(@Valid @RequestBody ManyFailureRequest req) {
        return ApiResponse.ok(service.manyFailure(req));
    }

    @PostMapping("/corner-change")
    public ApiResponse<ExternalIfResult> cornerChange(@Valid @RequestBody CornerChangeRequest req) {
        return ApiResponse.ok(service.cornerChange(req));
    }

    @PostMapping("/machine-info")
    public ApiResponse<ExternalIfResult> machineInfo(@Valid @RequestBody MachineInfoRequest req) {
        return ApiResponse.ok(service.machineInfo(req));
    }

    // ---- 수신(IN) : 대외업체 -> ATMS 출동 응답 ----

    @PostMapping("/inbound/departure-plan")
    public ApiResponse<ExternalIfResult> departurePlan(@Valid @RequestBody DeparturePlanRequest req) {
        return ApiResponse.ok(inboundService.arrivalPlan(req));
    }

    @PostMapping("/inbound/arrival")
    public ApiResponse<ExternalIfResult> arrival(@Valid @RequestBody ArrivalRequest req) {
        return ApiResponse.ok(inboundService.arrival(req));
    }

    @PostMapping("/inbound/repair-result")
    public ApiResponse<ExternalIfResult> repairResult(@Valid @RequestBody RepairResultRequest req) {
        return ApiResponse.ok(inboundService.repairResult(req));
    }

    @PostMapping("/inbound/many-failure-result")
    public ApiResponse<ExternalIfResult> manyFailureResult(@Valid @RequestBody ManyFailureResultRequest req) {
        return ApiResponse.ok(inboundService.manyFailureResult(req));
    }

    @PostMapping("/inbound/missing-msg")
    public ApiResponse<ExternalIfResult> missingMsg(@Valid @RequestBody MissingMsgRequest req) {
        return ApiResponse.ok(inboundService.missingMsg(req));
    }

    @PostMapping("/inbound/corner-time")
    public ApiResponse<ExternalIfResult> cornerTime(@Valid @RequestBody CornerTimeRequest req) {
        return ApiResponse.ok(inboundService.cornerTime(req));
    }

    @PostMapping("/inbound/facility-report")
    public ApiResponse<ExternalIfResult> facilityReport(@Valid @RequestBody FacilityReportRequest req) {
        return ApiResponse.ok(inboundService.facilityReport(req));
    }

    @PostMapping("/inbound/clean-manage")
    public ApiResponse<ExternalIfResult> cleanManage(@Valid @RequestBody CleanManageRequest req) {
        return ApiResponse.ok(inboundService.cleanManage(req));
    }
}
