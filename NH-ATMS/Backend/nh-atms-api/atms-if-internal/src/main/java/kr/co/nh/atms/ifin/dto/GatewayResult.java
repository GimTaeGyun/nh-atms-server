package kr.co.nh.atms.ifin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 게이트웨이 송신 결과 (SMS/메신저 공통).
 */
@Data
@Builder
public class GatewayResult {
    /** 응답코드 (0000/0097/0098/0099) */
    private String resultCd;
    private String resultMsg;
    /** T_GW_INTERFACE 이력 키 */
    private Long gwSeq;
}
