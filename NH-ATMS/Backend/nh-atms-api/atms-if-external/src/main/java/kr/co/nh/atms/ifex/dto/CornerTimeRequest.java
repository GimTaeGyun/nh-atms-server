package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 코너입퇴실시간정보 수신 (IQ_04001130, 대외업체 -> ATMS).
 * T_CORNER_TIME_INFO 생성 + T_CORNER.GUARD_STATUS 갱신 +
 * (업무구분 1/2/3/5 & 출입구분 2:입실 시) 장애관리 경비해제일시 갱신.
 */
@Data
public class CornerTimeRequest {

    /** 송신업체코드 (전문 헤더 vend_cd) */
    @NotBlank(message = "vendorCd(업체코드)는 필수입니다.")
    private String vendorCd;

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    /** 기기번호 (경비업체 관리 코너의 대표 단말) */
    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    /** 출입일자(yyyyMMdd) */
    private String inoutDate;
    /** 출입시각(HHmmss) */
    private String inoutTime;
    /** 출입구분 (1:카드SET=퇴실 2:카드OFF=입실) */
    private String inoutType;
    /** 업무구분 (1:경비 2:장애 3:현송 4:환경 5:은행/고객) */
    private String workType;
    /** 카드(직원)번호 */
    private String cardNo;
    /** 코너코드 */
    private String cornerCd;

    /** ATMS 전문일련번호 (전문 헤더 atms_sn) */
    private String atmsSn;
    /** 업체 전문일련번호 (전문 헤더 vend_sn) */
    private String vendSn;
}
