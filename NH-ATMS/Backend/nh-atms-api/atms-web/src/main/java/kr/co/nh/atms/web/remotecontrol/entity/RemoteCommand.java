package kr.co.nh.atms.web.remotecontrol.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_AGENT_COMMAND — 서버→에이전트 원격제어 명령 큐 (운영자 발행 화면용 뷰).
 * 운영자가 발행하면 STATUS=W 로 적재되고, 에이전트(atms-journal 폴링)가
 * 수령(A)·실행 후 결과(S/F)를 통보한다. 미수령/미완료는 워커가 T(타임아웃) 처리.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteCommand {
    private String cmdId;          // CMD-yyyyMMdd-NNNNNN (PK)
    private String termGroupId;    // 영업점(금고)코드
    private String termId;         // 기번(단말번호)
    private String cmdType;        // STAT/IMGC/DLD/FCOL/RST/RBT/POFF/STOP/OPEN_OP/JMOD/MSG/TMOD/CLS/RCMD/GDL
    private String payload;        // 명령 파라미터(JSON)
    private String status;         // W=대기,A=수령,S=성공,F=실패,T=타임아웃,C=취소
    private Integer timeoutSec;
    private Long asyncUid;         // 비동기 명령 상관 ID(atms-file 부여) — put.msg.resp.do 콜백 매칭용
    private String resultCd;
    private String resultData;     // 결과 상세(JSON)
    private String errMsg;
    private String regUserId;
    private LocalDateTime issuedAt;
    private LocalDateTime ackAt;
    private LocalDateTime completedAt;
}
