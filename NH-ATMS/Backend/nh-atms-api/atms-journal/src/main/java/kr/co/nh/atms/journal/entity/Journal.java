package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_JOURNAL — 모든 jnl_kind 공통 헤더.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journal {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String agentType;
    private String seqNo;
    private String jnlTypeCd;        // jnl_kind 그대로
    private String jnlWriteTypeCd;
    private String recvDate;
    private String recvTime;
}
