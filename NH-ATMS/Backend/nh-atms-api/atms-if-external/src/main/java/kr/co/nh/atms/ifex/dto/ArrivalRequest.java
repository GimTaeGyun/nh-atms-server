package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 도착통보 수신 요청 (EM 01001140, 대외업체 -> ATMS).
 */
@Data
public class ArrivalRequest {

    @NotNull
    private Long faultSeq;

    /** 코너도착일자(yyyyMMdd) */
    private String arrivalDate;
    /** 코너도착시각(HHmmss) */
    private String arrivalTime;

    /** 송신업체코드 (전문 헤더 vend_cd — 응답 역송신용, 선택) */
    private String vendorCd;
    /** ATMS 전문일련번호 (전문 헤더 atms_sn) */
    private String atmsSn;
    /** 업체 전문일련번호 (전문 헤더 vend_sn) */
    private String vendSn;
}
