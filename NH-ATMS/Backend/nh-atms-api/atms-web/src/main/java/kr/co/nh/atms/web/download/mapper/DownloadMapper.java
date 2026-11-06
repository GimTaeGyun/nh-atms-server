package kr.co.nh.atms.web.download.mapper;

import kr.co.nh.atms.web.download.dto.DownloadDeviceSearchDto;
import kr.co.nh.atms.web.download.dto.DownloadJobSearchDto;
import kr.co.nh.atms.web.download.dto.DownloadPackageSearchDto;
import kr.co.nh.atms.web.download.dto.IndividualHistorySearchDto;
import kr.co.nh.atms.web.download.entity.AtmApplyHistory;
import kr.co.nh.atms.web.download.entity.AtmDownload;
import kr.co.nh.atms.web.download.entity.DownloadDevice;
import kr.co.nh.atms.web.download.entity.DownloadJob;
import kr.co.nh.atms.web.download.entity.DownloadPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 다운로드관리(개별/일괄) Mapper. */
@Mapper
public interface DownloadMapper {

    // ── 개별다운로드관리 : 대상 기기 목록(현재버전 vs 최종버전) ──────────
    List<DownloadDevice> selectDeviceList(DownloadDeviceSearchDto search);

    long countDeviceList(DownloadDeviceSearchDto search);

    // ── 일괄다운로드관리 : 패키지 마스터 ────────────────────────────────
    List<DownloadPackage> selectPackageList(DownloadPackageSearchDto search);

    long countPackageList(DownloadPackageSearchDto search);

    DownloadPackage selectPackage(@Param("pkgId") Long pkgId);

    int insertPackage(DownloadPackage pkg);

    /** 결재/반려 처리(CONFIRM_STATUS, CONFIRM_USER_ID, CONFIRM_DT 갱신). */
    int updateConfirm(@Param("pkgId") Long pkgId,
                      @Param("status") String status,
                      @Param("userId") String userId);

    /** 일괄 배포 대상 : 해당 기종(업체+모델)의 운영중 기기 목록. */
    List<DownloadDevice> selectTargetDevices(@Param("atmCoCd") String atmCoCd,
                                             @Param("modelCd") String modelCd);

    // ── 배포 작업/이력 ──────────────────────────────────────────────────
    int insertJob(DownloadJob job);

    List<DownloadJob> selectJobList(DownloadJobSearchDto search);

    long countJobList(DownloadJobSearchDto search);

    // ── 개별다운로드(자체 파일 업로드) : MG indDownManage 정합 ───────────
    /** 업로드 파일 마스터 채번(DOWN_NO = LPAD(SEQ.NEXTVAL, 7, '0')). */
    String nextDownNo();

    /** 업로드 파일 마스터 등록(T_ATM_DOWNLOAD). */
    int insertAtmDownload(AtmDownload download);

    /** 기기 단건 조회(금고+기번 → 업체/모델/IP). 엑셀/수동 대상 검증용. */
    DownloadDevice selectDeviceOne(@Param("termGroupId") String termGroupId,
                                   @Param("termId") String termId);

    /** 기기별 개별다운로드 적용이력 등록(T_ATM_APPLY_HISTORY). */
    int insertApplyHistory(AtmApplyHistory history);

    List<AtmApplyHistory> selectApplyHistoryList(IndividualHistorySearchDto search);

    long countApplyHistoryList(IndividualHistorySearchDto search);
}
