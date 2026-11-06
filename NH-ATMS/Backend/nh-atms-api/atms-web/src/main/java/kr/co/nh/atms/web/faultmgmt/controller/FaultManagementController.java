package kr.co.nh.atms.web.faultmgmt.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.faultmgmt.dto.FaultManagementSearchDto;
import kr.co.nh.atms.web.faultmgmt.dto.FaultReceiptRequest;
import kr.co.nh.atms.web.faultmgmt.entity.FaultManagement;
import kr.co.nh.atms.web.faultmgmt.service.FaultManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 기기장애접수(출동요청) 관리.
 *
 *   GET    /api/fault-mgmt                 : 장애접수 이력 조회(검색+페이징)
 *   GET    /api/fault-mgmt/{faultSeq}      : 장애접수 단건 조회
 *   POST   /api/fault-mgmt                 : 기기장애접수(출동요청) 등록
 *   POST   /api/fault-mgmt/{faultSeq}/cancel : 출동취소
 */
@RestController
@RequestMapping("/api/fault-mgmt")
@RequiredArgsConstructor
public class FaultManagementController {

    private final FaultManagementService service;

    @GetMapping
    public ApiResponse<PageResult<FaultManagement>> list(@ModelAttribute FaultManagementSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @GetMapping("/{faultSeq}")
    public ApiResponse<FaultManagement> get(@PathVariable Long faultSeq) {
        return ApiResponse.ok(service.get(faultSeq));
    }

    @PostMapping
    public ApiResponse<FaultManagement> receive(@Valid @RequestBody FaultReceiptRequest req) {
        return ApiResponse.ok(service.receive(req), "장애접수가 등록되었습니다.");
    }

    @PostMapping("/{faultSeq}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long faultSeq) {
        service.cancel(faultSeq);
        return ApiResponse.ok(null, "출동이 취소되었습니다.");
    }
}
