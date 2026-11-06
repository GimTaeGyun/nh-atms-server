package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 시설물관리업무보고 수신 (SM_02001180, 대외업체 -> ATMS).
 * AS-IS 저장 테이블(T_EQUIPMANAGE)은 미사용 폐지 — 수신 이력 적재 + 응답만 수행.
 */
@Data
public class FacilityReportRequest {

    /** 송신업체코드 (전문 헤더 vend_cd) */
    @NotBlank(message = "vendorCd(업체코드)는 필수입니다.")
    private String vendorCd;

    /** 점검일자(yyyyMMdd) */
    private String checkDate;
    private String termGroupId;
    /** 관리점코드 */
    private String branchCd;
    private String cornerCd;
    private String cornerName;
    /** 장소구분코드 */
    private String cornerTypeCd;
    /** 운영시간구분코드 */
    private String operationTypeCd;
    /** CCTV유무 (1:유 0:무) */
    private String cctvFlag;
    /** CCTV상태 (1:가동 0:중지) */
    private String cctvStatus;
    /** 담당자명 */
    private String personName;
    /** 사무소명 */
    private String officeName;
    /** 사진촬영일자 */
    private String photoDate;
    /** 특이사항 */
    private String comment;
    /** 조치사항 */
    private String actionComment;

    /** ATMS 전문일련번호 (전문 헤더 atms_sn) */
    private String atmsSn;
    /** 업체 전문일련번호 (전문 헤더 vend_sn) */
    private String vendSn;
}
