package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_AGENT_COMMAND — 서버→에이전트 원격제어 명령 큐.
 * 운영자/시스템이 발행하면 STATUS=W 로 적재되고, 에이전트가 폴링으로
 * 수령(A)·실행 후 결과(S/F)를 통보한다. 미수령/미완료는 워커가 T(타임아웃) 처리.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCommand {
    private String cmdId;          // CMD-yyyyMMdd-NNNNNN (PK)
    private String termGroupId;
    private String termId;
    private String cmdType;        // STAT/IMGC/DLD/FCOL/RST/RBT/POFF/STOP/OPEN_OP/JMOD/MSG/TMOD/CLS/RCMD/GDL
    private String payload;        // 명령 파라미터(JSON)
    private String status;         // W=대기,A=수령,S=성공,F=실패,T=타임아웃,C=취소
    private Integer timeoutSec;
    private String resultCd;
    private String resultData;     // 결과 상세(JSON)
    private String errMsg;
    private String regUserId;
    private LocalDateTime issuedAt;
    private LocalDateTime ackAt;
    private LocalDateTime completedAt;
}
