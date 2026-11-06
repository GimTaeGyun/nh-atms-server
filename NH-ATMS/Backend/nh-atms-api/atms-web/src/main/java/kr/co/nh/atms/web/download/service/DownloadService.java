package kr.co.nh.atms.web.download.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.download.dto.DownloadDeviceSearchDto;
import kr.co.nh.atms.web.download.dto.DownloadJobSearchDto;
import kr.co.nh.atms.web.download.dto.DownloadPackageSaveRequest;
import kr.co.nh.atms.web.download.dto.DownloadPackageSearchDto;
import kr.co.nh.atms.web.download.dto.IndividualHistorySearchDto;
import kr.co.nh.atms.web.download.dto.IndividualUploadRequest;
import kr.co.nh.atms.web.download.entity.AtmApplyHistory;
import kr.co.nh.atms.web.download.entity.AtmDownload;
import kr.co.nh.atms.web.download.entity.DownloadDevice;
import kr.co.nh.atms.web.download.entity.DownloadJob;
import kr.co.nh.atms.web.download.entity.DownloadPackage;
import kr.co.nh.atms.web.download.mapper.DownloadMapper;
import kr.co.nh.atms.web.download.storage.DownloadFileStorage;
import kr.co.nh.atms.web.remotecontrol.dto.RemoteCommandIssueRequest;
import kr.co.nh.atms.web.remotecontrol.entity.RemoteCommand;
import kr.co.nh.atms.web.remotecontrol.service.RemoteControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 다운로드관리(개별/일괄) 서비스.
 *
 * <p>두 메뉴는 구 새마을금고와 동일하게 <b>완전히 분리된 전용 테이블</b>을 사용한다.</p>
 * <ul>
 *   <li>개별다운로드관리 : 화면에서 ZIP 파일을 <b>직접 업로드</b>(T_ATM_DOWNLOAD) →
 *       선택 기기들에 즉시 배포(T_ATM_APPLY_HISTORY). <b>결재 무관.</b></li>
 *   <li>일괄다운로드관리 : 패키지 등록(T_DOWNLOAD_PACKAGE) → 결재(승인) →
 *       해당 기종 전체 배포(T_DOWNLOAD_JOB). <b>결재 필수.</b></li>
 * </ul>
 * <p>실제 전송은 원격제어 명령 큐(T_AGENT_COMMAND, CMD_TYPE=DLD/GDL)를 재사용한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DownloadService {

    private final DownloadMapper mapper;
    private final RemoteControlService remoteControlService;
    private final DownloadFileStorage fileStorage;

    // ── 개별다운로드관리 (자체 파일 업로드, MG indDownManage 정합) ────────

    /** 개별 배포 대상 기기 목록(현재버전 vs 최종버전 비교). */
    public PageResult<DownloadDevice> searchDevices(DownloadDeviceSearchDto s) {
        return PageResult.of(mapper.selectDeviceList(s), mapper.countDeviceList(s), s);
    }

    /**
     * 업로드한 파일을 선택 기기들에 개별 배포한다. (MG 정합 : 결재 무관, 자체 파일 업로드)
     *
     * <p>① DOWN_NO 채번 → ② 파일 저장(서버 저장소 temp) → ③ T_ATM_DOWNLOAD 마스터 등록 →
     * ④ 대상 기기별로 원격다운로드 명령 발행(DLD/GDL) + T_ATM_APPLY_HISTORY 적재.</p>
     *
     * @return 발행(이력 적재)된 기기 수
     */
    @Transactional
    public int uploadAndDistributeIndividual(MultipartFile file, IndividualUploadRequest req) {
        String downNo = mapper.nextDownNo();
        DownloadFileStorage.Stored stored = fileStorage.store(file, downNo);

        String userId = currentUserId();
        AtmDownload dl = AtmDownload.builder()
                .downNo(downNo)
                .appTypeCd(req.getAppType())
                .atmCoCd(req.getAtmCoCd())
                .modelCd(req.getModelCd())
                .appFileName(file.getOriginalFilename())
                .appFileSize(file.getSize())
                .appVer(req.getAppVer())
                .fileCompress("0")
                .exeTypeCd((req.getExecType() == null || req.getExecType().isBlank()) ? "0" : req.getExecType())
                .exeFilePath(stored.absolutePath())
                .downloadPath(req.getDownloadPath())
                .applyMemo(req.getApplyMemo())
                .enterUserId(userId)
                .build();
        mapper.insertAtmDownload(dl);

        int count = 0;
        for (IndividualUploadRequest.Target t : req.getTargets()) {
            dispatchIndividual(dl, stored.relativePath(), t.getTermGroupId(), t.getTermId(), userId);
            count++;
        }
        log.info("[download] individual upload distribute downNo={} file={} targets={} by={}",
                downNo, dl.getAppFileName(), count, userId);
        return count;
    }

    /** 개별다운로드 적용이력(T_ATM_APPLY_HISTORY) 조회. */
    public PageResult<AtmApplyHistory> searchIndividualHistory(IndividualHistorySearchDto s) {
        return PageResult.of(mapper.selectApplyHistoryList(s), mapper.countApplyHistoryList(s), s);
    }

    // ── 일괄다운로드관리 ────────────────────────────────────────────────

    /** 다운로드 패키지 목록(기종별). */
    public PageResult<DownloadPackage> searchPackages(DownloadPackageSearchDto s) {
        return PageResult.of(mapper.selectPackageList(s), mapper.countPackageList(s), s);
    }

    /** 패키지 단건 조회. */
    public DownloadPackage getPackage(Long pkgId) {
        DownloadPackage pkg = mapper.selectPackage(pkgId);
        if (pkg == null) throw new BusinessException("DL_001", "패키지 정보가 없습니다.");
        return pkg;
    }

    /** 다운로드 패키지 등록(결재대기 상태). */
    @Transactional
    public DownloadPackage registerPackage(DownloadPackageSaveRequest req) {
        DownloadPackage pkg = DownloadPackage.builder()
                .pkgName(req.getPkgName())
                .pkgType(req.getPkgType())
                .atmCoCd(req.getAtmCoCd())
                .modelCd(req.getModelCd())
                .downVer(req.getDownVer())
                .fileName(req.getFileName())
                .fileSize(req.getFileSize())
                .filePath(req.getFilePath())
                .downloadPath(req.getDownloadPath())
                .execCmd(req.getExecCmd())
                .applyDate(req.getApplyDate())
                .applyTime(req.getApplyTime())
                .crcValue(req.getCrcValue())
                .regUserId(currentUserId())
                .build();
        mapper.insertPackage(pkg);
        log.info("[download] package registered pkgId={} model={}/{} ver={} by={}",
                pkg.getPkgId(), pkg.getAtmCoCd(), pkg.getModelCd(), pkg.getDownVer(), pkg.getRegUserId());
        return pkg;
    }

    /** 패키지 결재(승인). 승인 후에만 배포 가능. */
    @Transactional
    public void confirm(Long pkgId) {
        DownloadPackage pkg = getPackage(pkgId);
        if ("Y".equals(pkg.getConfirmStatus())) {
            throw new BusinessException("DL_002", "이미 결재완료된 패키지입니다.");
        }
        mapper.updateConfirm(pkgId, "Y", currentUserId());
        log.info("[download] package confirmed pkgId={} by={}", pkgId, currentUserId());
    }

    /** 패키지 반려. */
    @Transactional
    public void reject(Long pkgId) {
        getPackage(pkgId);
        mapper.updateConfirm(pkgId, "R", currentUserId());
        log.info("[download] package rejected pkgId={} by={}", pkgId, currentUserId());
    }

    /** 결재완료 패키지를 해당 기종 전체 기기에 일괄 배포한다. */
    @Transactional
    public int distributeBatch(Long pkgId) {
        DownloadPackage pkg = requireConfirmedPackage(pkgId);
        List<DownloadDevice> targets = mapper.selectTargetDevices(pkg.getAtmCoCd(), pkg.getModelCd());
        if (targets.isEmpty()) {
            throw new BusinessException("DL_004", "배포 대상 기기가 없습니다.");
        }
        for (DownloadDevice d : targets) {
            dispatch(pkg, d.getTermGroupId(), d.getTermId(), "B");
        }
        log.info("[download] batch distribute pkgId={} targets={} by={}",
                pkgId, targets.size(), currentUserId());
        return targets.size();
    }

    // ── 배포 이력 ───────────────────────────────────────────────────────

    public PageResult<DownloadJob> searchJobs(DownloadJobSearchDto s) {
        return PageResult.of(mapper.selectJobList(s), mapper.countJobList(s), s);
    }

    // ── 내부 ────────────────────────────────────────────────────────────

    /** 결재완료 패키지인지 검증 후 반환. */
    private DownloadPackage requireConfirmedPackage(Long pkgId) {
        DownloadPackage pkg = getPackage(pkgId);
        if (!"Y".equals(pkg.getConfirmStatus())) {
            throw new BusinessException("DL_003", "결재완료된 패키지만 배포할 수 있습니다.");
        }
        return pkg;
    }

    /** 단일 기기에 다운로드 명령 발행 + 작업이력 적재. */
    private void dispatch(DownloadPackage pkg, String termGroupId, String termId, String execType) {
        RemoteCommandIssueRequest cmdReq = new RemoteCommandIssueRequest();
        cmdReq.setTermGroupId(termGroupId);
        cmdReq.setTermId(termId);
        // AP/매체 = DLD, 일반 = GDL(일반다운로드)
        cmdReq.setCmdType("G".equals(pkg.getPkgType()) ? "GDL" : "DLD");
        cmdReq.setPayload(buildPayload(pkg));
        RemoteCommand cmd = remoteControlService.issue(cmdReq);

        DownloadJob job = DownloadJob.builder()
                .pkgId(pkg.getPkgId())
                .termGroupId(termGroupId)
                .termId(termId)
                .execType(execType)
                .status("W")
                .cmdId(cmd.getCmdId())
                .reserveDate(pkg.getApplyDate())
                .reserveTime(pkg.getApplyTime())
                .regUserId(currentUserId())
                .build();
        mapper.insertJob(job);
    }

    private Map<String, Object> buildPayload(DownloadPackage pkg) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("pkgId", pkg.getPkgId());
        p.put("pkgType", pkg.getPkgType());
        p.put("downVer", pkg.getDownVer());
        p.put("fileName", pkg.getFileName());
        p.put("filePath", pkg.getFilePath());
        p.put("downloadPath", pkg.getDownloadPath());
        p.put("execCmd", pkg.getExecCmd());
        p.put("applyDate", pkg.getApplyDate());
        p.put("applyTime", pkg.getApplyTime());
        return p;
    }

    /**
     * 개별다운로드 1기기 처리 : 원격명령(DLD/GDL) 발행 + 적용이력(T_ATM_APPLY_HISTORY) 적재.
     * 푸시 결과로 APPLY_FLAG/APPLY_RESULT 를 결정한다.
     */
    private void dispatchIndividual(AtmDownload dl, String relativePath,
                                    String termGroupId, String termId, String userId) {
        RemoteCommandIssueRequest cmdReq = new RemoteCommandIssueRequest();
        cmdReq.setTermGroupId(termGroupId);
        cmdReq.setTermId(termId);
        cmdReq.setCmdType("G".equals(dl.getAppTypeCd()) ? "GDL" : "DLD");
        cmdReq.setPayload(buildIndividualPayload(dl, relativePath));
        RemoteCommand cmd = remoteControlService.issue(cmdReq);

        String status = cmd.getStatus();
        String applyFlag;     // 0=미적용,1=적용완료,2=다운로드중
        String applyResult;   // 0=실패,1=성공
        if ("S".equals(status)) {
            applyFlag = "1";
            applyResult = "1";
        } else if ("A".equals(status)) {
            applyFlag = "2";       // 비동기 수령 → 다운로드중
            applyResult = null;
        } else if ("F".equals(status) || "T".equals(status)) {
            applyFlag = "0";
            applyResult = "0";
        } else {                   // W(기기 미접속 등)
            applyFlag = "0";
            applyResult = null;
        }

        AtmApplyHistory hist = AtmApplyHistory.builder()
                .downNo(dl.getDownNo())
                .termGroupId(termGroupId)
                .termId(termId)
                .cmdId(cmd.getCmdId())
                .applyFlag(applyFlag)
                .applyResult(applyResult)
                .asyncUid(cmd.getAsyncUid())
                .regUserId(userId)
                .build();
        mapper.insertApplyHistory(hist);
    }

    private Map<String, Object> buildIndividualPayload(AtmDownload dl, String relativePath) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("downNo", dl.getDownNo());
        p.put("pkgType", dl.getAppTypeCd());
        p.put("dlKind", dl.getExeTypeCd());     // 0=개별,1=일반
        p.put("downVer", dl.getAppVer());
        p.put("fileName", dl.getAppFileName());
        p.put("filePath", relativePath);        // 저장소 루트 기준 상대경로(예: /temp/0000007_ap.zip)
        p.put("downloadPath", dl.getDownloadPath());
        return p;
    }

    private String currentUserId() {
        try {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            return (name == null || name.isBlank()) ? "SYSTEM" : name;
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
}
