package kr.co.nh.atms.web.sms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_SMS_SEND_DATA — 장애 SMS 발송 큐/이력.
 * 운영자가 SMS 전송을 등록하면 SEND_STATUS='W'(대기)로 적재되어
 * 발송 모듈(atms-if-internal)이 처리한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendData {
    private Long smsSeq;            // PK (SEQ_T_SMS_SEND_DATA)
    private String globalId;
    private String termGroupId;
    private String termId;
    private String recvPhone;      // 수신번호
    private String sendPhone;      // 발신(회신)번호
    private String msgContent;     // 메시지내용
    private String errCd;
    private String sendStatus;     // W=대기,S=성공,F=실패
    private String sendDate;
    private String sendTime;
    private String resultCd;
    private String resultMsg;
    private LocalDateTime regDt;
}
