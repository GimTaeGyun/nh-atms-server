package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 환경관리업무 수신 (SM_02001190, 대외업체 -> ATMS).
 * AS-IS 저장 테이블(T_CLEANMANAGE)은 미사용 폐지 — 수신 이력 적재 + 응답만 수행.
 */
@Data
public class CleanManageRequest {

    /** 송신업체코드 (전문 헤더 vend_cd) */
    @NotBlank(message = "vendorCd(업체코드)는 필수입니다.")
    private String vendorCd;

    /** 관리일자(yyyyMMdd) */
    private String manageDate;
    private String termGroupId;
    private String termId;
    private String cornerCd;
    private String cornerName;
    /** 기기내부청소여부 (1:여 0:부) */
    private String innerCleanFlag;
    /** 주변청소여부 (1:여 0:부) */
    private String aroundCleanFlag;
    /** 대청소여부 (1:여 0:부) */
    private String bigCleanFlag;
    /** 벽면게시물상태 (1:양호 2:불량 9:기타) */
    private String wallStatus;
    /** 조치내용 */
    private String actionComment;

    /** ATMS 전문일련번호 (전문 헤더 atms_sn) */
    private String atmsSn;
    /** 업체 전문일련번호 (전문 헤더 vend_sn) */
    private String vendSn;
}
