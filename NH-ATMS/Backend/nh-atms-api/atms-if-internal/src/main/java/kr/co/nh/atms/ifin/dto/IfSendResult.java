package kr.co.nh.atms.ifin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 대내 IF 처리 공통 응답 (SMS/메신저 송신 결과).
 */
@Data
@Builder
public class IfSendResult {
    private String globalId;
    private String resultCd;
    private String resultMsg;
    /** 생성된 이력 키 (SMS_SEQ 또는 GW_SEQ) */
    private Long refSeq;
}
