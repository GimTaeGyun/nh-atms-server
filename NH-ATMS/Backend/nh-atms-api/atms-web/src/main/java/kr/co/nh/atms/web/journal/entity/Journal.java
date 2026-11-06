package kr.co.nh.atms.web.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_JOURNAL — 저널 공통 헤더 조회 엔티티 (모든 jnl_kind 공통).
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
    private String jnlTypeCd;       // jnl_kind (11/12/21/22/31/41/52/71)
    private String jnlWriteTypeCd;
    private String recvDate;
    private String recvTime;
}
