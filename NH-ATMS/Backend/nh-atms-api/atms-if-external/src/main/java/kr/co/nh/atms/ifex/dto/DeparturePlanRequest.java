package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 도착예정 수신 요청 (EM 01001130, 대외업체 -> ATMS).
 * 출동요청 응답에 담긴 faultSeq 로 장애관리 건을 식별한다.
 */
@Data
public class DeparturePlanRequest {

    @NotNull
    private Long faultSeq;

    /** 도착예정일자(yyyyMMdd) */
    private String departureDate;
    /** 도착예정시각(HHmmss) */
    private String departureTime;
    /** 업체접수자명 */
    private String responsePerson;
    /** 출동요원명 */
    private String servicePerson;
    /** 출동요원전화번호 */
    private String servicePhoneNo;

    /** 송신업체코드 (전문 헤더 vend_cd — 응답 역송신용, 선택) */
    private String vendorCd;
    /** ATMS 전문일련번호 (전문 헤더 atms_sn) */
    private String atmsSn;
    /** 업체 전문일련번호 (전문 헤더 vend_sn) */
    private String vendSn;
}
