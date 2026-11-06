package kr.co.nh.atms.ifex.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대외 수신전문 이력 (T_NET_RECV_DATA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetRecvData {
    private Long recvSeq;
    private String globalId;
    private String trCode;
    private Long sendSeq;
    private String recvMsg;
    private String resultCd;
    private String resultMsg;
    private String recvDate;
    private String recvTime;
    /** 송신업체코드 (전문 헤더 vend_cd — 레거시 PROVIDER_CD) */
    private String providerCd;
    /** ATMS 전문일련번호 (수신등록 채번 "004"/netrecv_reg — 레거시 REGIST_SEQNO) */
    private String atmsSn;
    /** 업체 전문일련번호 (수신전문 헤더 vend_sn — 레거시 RECV_SEQ_NO) */
    private String vendSn;
    /** 원시 전문 (고정길이 패킷 문자열 — 레거시 RECV_DATA) */
    private String rawMsg;
    private LocalDateTime regDt;
}
