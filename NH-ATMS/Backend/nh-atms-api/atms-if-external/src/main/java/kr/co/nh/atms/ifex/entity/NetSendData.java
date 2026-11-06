package kr.co.nh.atms.ifex.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대외 송신전문 이력 (T_NET_SEND_DATA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetSendData {
    private Long sendSeq;
    private String globalId;
    private String trCode;
    private String ifType;
    private String termGroupId;
    private String termId;
    private String sendMsg;
    private String resultCd;
    private String resultMsg;
    private String sendDate;
    private String sendTime;
    /** 수신업체코드 (전문 헤더 vend_cd — 레거시 PROVIDER_CD) */
    private String providerCd;
    /** ATMS 전문일련번호 (7자리, T_SEQ_NO_MASTER "004" 채번 — 레거시 SEND_SEQ_NO) */
    private String atmsSn;
    /** 업체 전문일련번호 (7자리, T_SEQ_NO_MASTER "005" 채번 — 레거시 REGIST_SEQNO) */
    private String vendSn;
    /** 원시 전문 (고정길이 패킷 문자열 — 레거시 SEND_DATA) */
    private String rawMsg;
    private LocalDateTime regDt;
}
