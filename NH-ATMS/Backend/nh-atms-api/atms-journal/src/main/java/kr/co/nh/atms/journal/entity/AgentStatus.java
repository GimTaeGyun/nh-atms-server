package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_AGENT_STATUS — ATM 에이전트 상태 마스터.
 * PK: atmIp
 * 레거시 atmr T_AGENT_STATUS 를 신규 핵심 컬럼으로 단순화.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatus {
    private String atmIp;
    private String agentType;
    private String termGroupId;
    private String termId;
    private String connectFlag;     // 1=접속, 0=해제
    private String agentVer;
    private String apDownVer;
    private String osName;
    private String atmMacAddr;
    private String connectIp;
    private String connectPort;
    private String jnlMode;
    private String cardImage;
    private String dispenserImage;
    private String customerImage;
    private String pollCycle;
    private String startDate;
    private String startTime;
    private String lastPollDate;
    private String lastPollTime;
    private String exitDate;
    private String exitTime;
}
