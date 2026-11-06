package kr.co.nh.atms.web.download.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.download.dto.DownloadDeviceSearchDto;
import kr.co.nh.atms.web.download.dto.DownloadJobSearchDto;
import kr.co.nh.atms.web.download.dto.DownloadPackageSaveRequest;
import kr.co.nh.atms.web.download.dto.DownloadPackageSearchDto;
import kr.co.nh.atms.web.download.dto.IndividualHistorySearchDto;
import kr.co.nh.atms.web.download.dto.IndividualUploadRequest;
import kr.co.nh.atms.web.download.entity.AtmApplyHistory;
import kr.co.nh.atms.web.download.entity.DownloadDevice;
import kr.co.nh.atms.web.download.entity.DownloadJob;
import kr.co.nh.atms.web.download.entity.DownloadPackage;
import kr.co.nh.atms.web.download.service.DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 다운로드관리(개별/일괄) REST API.
 *
 * <p>개별다운로드관리 : 기기 목록 조회 → ZIP 파일 직접 업로드 후 선택 기기 배포(결재 무관)</p>
 * <p>일괄다운로드관리 : 패키지 등록/결재 → 기종 전체 일괄 배포(결재 필수)</p>
 */
@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService service;

    // ── 개별다운로드관리 (자체 파일 업로드) ─────────────────────────────

    /** 개별 배포 대상 기기 목록(현재버전 vs 최종버전 비교). */
    @GetMapping("/devices")
    public ApiResponse<PageResult<DownloadDevice>> devices(@ModelAttribute DownloadDeviceSearchDto search) {
        return ApiResponse.ok(service.searchDevices(search));
    }

    /** 업로드한 ZIP 파일을 선택한 기기들에 개별 배포(결재 무관, MG indDownManage 정합). */
    @PostMapping(value = "/individual/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Integer> uploadIndividual(@RequestPart("file") MultipartFile file,
                                                 @RequestPart("request") @Valid IndividualUploadRequest request) {
        int count = service.uploadAndDistributeIndividual(file, request);
        return ApiResponse.ok(count, count + "대 기기에 다운로드를 발행했습니다.");
    }

    /** 개별다운로드 적용이력. */
    @GetMapping("/individual/history")
    public ApiResponse<PageResult<AtmApplyHistory>> individualHistory(@ModelAttribute IndividualHistorySearchDto search) {
        return ApiResponse.ok(service.searchIndividualHistory(search));
    }

    // ── 일괄다운로드관리 ────────────────────────────────────────────────

    /** 다운로드 패키지 목록(기종별). */
    @GetMapping("/packages")
    public ApiResponse<PageResult<DownloadPackage>> packages(@ModelAttribute DownloadPackageSearchDto search) {
        return ApiResponse.ok(service.searchPackages(search));
    }

    /** 패키지 단건 조회. */
    @GetMapping("/packages/{pkgId}")
    public ApiResponse<DownloadPackage> getPackage(@PathVariable Long pkgId) {
        return ApiResponse.ok(service.getPackage(pkgId));
    }

    /** 다운로드 패키지 등록(결재대기). */
    @PostMapping("/packages")
    public ApiResponse<DownloadPackage> registerPackage(@Valid @RequestBody DownloadPackageSaveRequest req) {
        return ApiResponse.ok(service.registerPackage(req), "패키지가 등록되었습니다.");
    }

    /** 패키지 결재(승인). */
    @PostMapping("/packages/{pkgId}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long pkgId) {
        service.confirm(pkgId);
        return ApiResponse.ok(null, "결재 처리되었습니다.");
    }

    /** 패키지 반려. */
    @PostMapping("/packages/{pkgId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long pkgId) {
        service.reject(pkgId);
        return ApiResponse.ok(null, "반려 처리되었습니다.");
    }

    /** 결재완료 패키지를 해당 기종 전체에 일괄 배포. */
    @PostMapping("/packages/{pkgId}/distribute")
    public ApiResponse<Integer> distributeBatch(@PathVariable Long pkgId) {
        int count = service.distributeBatch(pkgId);
        return ApiResponse.ok(count, count + "대 기기에 일괄 다운로드를 발행했습니다.");
    }

    // ── 배포 이력 ───────────────────────────────────────────────────────

    /** 다운로드 작업/배포 이력. */
    @GetMapping("/jobs")
    public ApiResponse<PageResult<DownloadJob>> jobs(@ModelAttribute DownloadJobSearchDto search) {
        return ApiResponse.ok(service.searchJobs(search));
    }
}
