package kr.co.nh.atms.web.atmmonitor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM_MONITOR (기기 상태/장애 이력)
 *  - 저널 jnl_kind 21/22 가 들어올 때마다 1행씩 추가됨
 *  - PK: termGroupId + termId + jnlDate + jnlTime
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmMonitor {
    private String termGroupId;
    private String termId;
    private String jnlDate;        // YYYYMMDD
    private String jnlTime;        // HHMMSSFFF + flag
    private String agentType;
    private String atmStatus;      // 00=정상 etc.
    private String errCd;          // 은행 장애코드
    private String atmErrCd;       // 기기업체 장애코드
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
