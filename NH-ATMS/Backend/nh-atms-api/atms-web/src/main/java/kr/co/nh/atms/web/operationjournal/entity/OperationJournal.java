package kr.co.nh.atms.web.operationjournal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_OPERATION_JOURNAL — 운영 저널 조회 엔티티 (jnl_kind=52).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationJournal {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String operationCd;
    private String operationAction;
}
