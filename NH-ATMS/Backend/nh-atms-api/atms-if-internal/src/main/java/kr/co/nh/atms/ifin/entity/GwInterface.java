package kr.co.nh.atms.ifin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대내 연계전문 송수신 이력 (T_GW_INTERFACE).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GwInterface {
    private Long gwSeq;
    private String globalId;
    /** GlobalID 24~30자리 일련번호(7자리). 응답 전문 매칭 키 (AS-IS substring(23,30)) */
    private String msgSeqNo;
    /** 게이트웨이 처리 플래그: '0' 송신, '1' 응답수신 (AS-IS GW_FLAG) */
    private String gwFlag;
    private String trCode;
    private String ifType;
    private String termGroupId;
    private String termId;
    private String sendData;
    private String recvData;
    private String resultCd;
    private String resultMsg;
    private String sendDate;
    private String sendTime;
    private String recvDate;
    private String recvTime;
    private LocalDateTime regDt;
}
