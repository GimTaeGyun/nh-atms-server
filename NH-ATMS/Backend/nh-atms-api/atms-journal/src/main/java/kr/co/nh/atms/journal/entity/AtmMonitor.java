package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM_MONITOR — 기기 상태/장애 이력 (jnl_kind 21/22).
 * atms-web 의 동명 entity 와 의도적으로 분리: 도메인 모듈마다 자기 entity 보유.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmMonitor {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String agentType;
    private String atmStatus;
    private String errCd;
    private String atmErrCd;
    private String processCount;
    private String manStatus;
    private String omanStatus;
    private String sprStatus;
    private String jprStatus;
    private String chkStatus;
    private String chaStatus;
    private String mcrStatus;
    private String pbStatus;
    private String rjtStatus;
    private String sendFlag;
    private String recvDate;
    private String recvTime;
}
