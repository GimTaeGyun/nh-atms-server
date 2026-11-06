package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 결번요청 수신 (IQ_04001110, 대외업체 -> ATMS).
 *
 * <ul>
 *   <li>reqSeqNo 공백: ATMS 최종 전문일련번호를 message 로 응답 (응답코드 0098)</li>
 *   <li>reqSeqNo 존재: 해당 원 송신전문(T_NET_SEND_DATA)을 재전송</li>
 * </ul>
 */
@Data
public class MissingMsgRequest {

    /** 송신업체코드 (전문 헤더 vend_cd) */
    @NotBlank(message = "vendorCd(업체코드)는 필수입니다.")
    private String vendorCd;

    /** 요청전문일자 (결번 전문의 송신일자, yyyyMMdd) */
    private String reqDate;

    /** 요청전문일련번호 (결번 전문의 ATMS 전문일련번호, 공백이면 최종일련번호 응답) */
    private String reqSeqNo;

    /** ATMS 전문일련번호 (전문 헤더 atms_sn) */
    private String atmsSn;
    /** 업체 전문일련번호 (전문 헤더 vend_sn) */
    private String vendSn;
}
