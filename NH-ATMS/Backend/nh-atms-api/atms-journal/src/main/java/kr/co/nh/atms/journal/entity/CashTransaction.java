package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_CASH_TRANSACTION - 거래저널(jnl_kind=71) 권종별 현금 거래 상세.
 * PK: termGroupId + termId + jnlDate + jnlTime + denomCd
 *
 * interface.journal.json "71" trx_cash[] 매핑:
 *   denom -> denomCd, qty -> denomCount
 * (atmr RecvJournalMapper.xml insert_ignore_T_CASHTRANSACTION 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashTransaction {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String denomCd;     // VARCHAR2(10) 권종 코드
    private Long denomCount;    // NUMBER(19) 매수
}
