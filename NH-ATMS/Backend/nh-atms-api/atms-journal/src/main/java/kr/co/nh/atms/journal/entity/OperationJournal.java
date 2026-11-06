package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_OPERATION_JOURNAL — 운영 저널 (jnl_kind=52).
 * PK: termGroupId + termId + jnlDate + jnlTime
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
    private String operationCd;     // 2자리 코드
    private String operationAction; // 한글 처리내용
}
