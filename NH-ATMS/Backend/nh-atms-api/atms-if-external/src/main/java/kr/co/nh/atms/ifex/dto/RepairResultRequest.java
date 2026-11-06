package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 조치결과 수신 요청 (EM 01001150 / 동시다발 01001190, 대외업체 -> ATMS).
 */
@Data
public class RepairResultRequest {

    @NotNull
    private Long faultSeq;

    /** 조치결과코드(1:조치완료 0:미복구) */
    private String repairFlag;
    /** 조치완료일자(yyyyMMdd) */
    private String finishedDate;
    /** 조치완료시각(HHmmss) */
    private String finishedTime;
    /** 조치불가사유코드 */
    private String failureReasonCd;
    /** 조치의견 */
    private String repairComment;

    /** 송신업체코드 (전문 헤더 vend_cd — 응답 역송신/2차출동 판정용) */
    private String vendorCd;
    /** ATMS 전문일련번호 (전문 헤더 atms_sn) */
    private String atmsSn;
    /** 업체 전문일련번호 (전문 헤더 vend_sn) */
    private String vendSn;
    /** 전문 헤더 발생일자 (조치일자 공백 시 fallback — 레거시 20251010 보완) */
    private String msgDate;
    /** 전문 헤더 발생시각 (조치시각 공백 시 fallback) */
    private String msgTime;
}
