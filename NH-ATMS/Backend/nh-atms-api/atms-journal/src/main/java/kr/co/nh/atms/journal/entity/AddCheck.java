package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ADD_CHECK — 수표 시재 보충/회수 (jnl_kind=41).
 * PK: termGroupId + termId + jnlDate + jnlTime + actionType
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCheck {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String actionType;
    private String currency;
    private Long   denomination;
    private Long   jaAmount;
    private Integer jaCount;
    private Long   taAmount;
    private Integer taCount;
    private String issueBank;
    private String issueBranch;
    private String regNo;
}
