package kr.co.nh.atms.ifex.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 대외(atme) 인터페이스 공통 응답.
 */
@Data
@Builder
public class ExternalIfResult {
    private String globalId;
    private String resultCd;
    private String resultMsg;
    /** 송신 이력 키 (T_NET_SEND_DATA.SEND_SEQ) */
    private Long sendSeq;
    /** 장애관리 키 (T_FAULT_MANAGEMENT.FAULT_SEQ) — 출동 관련 시 */
    private Long faultSeq;
}
