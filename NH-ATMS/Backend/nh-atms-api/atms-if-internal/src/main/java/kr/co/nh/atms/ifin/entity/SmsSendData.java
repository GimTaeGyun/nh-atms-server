package kr.co.nh.atms.ifin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 장애 SMS 발송 큐/이력 (T_SMS_SEND_DATA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendData {
    private Long smsSeq;
    private String globalId;
    private String termGroupId;
    private String termId;
    private String recvPhone;
    private String sendPhone;
    private String msgContent;
    private String errCd;
    private String sendStatus;
    private String sendDate;
    private String sendTime;
    private String resultCd;
    private String resultMsg;
    private LocalDateTime regDt;
}
