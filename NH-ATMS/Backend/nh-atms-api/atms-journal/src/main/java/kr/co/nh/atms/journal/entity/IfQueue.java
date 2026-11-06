package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_IF_QUEUE — 대내/대외 IF 비동기 큐 (atmr 1-D).
 * 장애 저널(22) 수신 시 적재되어 IfQueueWorker 가 atme/atmi 로 디스패치.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IfQueue {
    private Long queueSeq;        // SEQ_T_IF_QUEUE
    private String ifTarget;      // I=대내(atmi), E=대외(atme)
    private String ifAction;      // SERVICE_CALL, FAULT_SMS 등
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String errCd;
    private String payload;       // 호출 본문(JSON)
    private String status;        // W=대기,P=처리중,S=성공,F=실패
    private Integer retryCnt;
    private String resultCd;
    private String resultMsg;
    private LocalDateTime regDt;
    private LocalDateTime sentDt;
}
