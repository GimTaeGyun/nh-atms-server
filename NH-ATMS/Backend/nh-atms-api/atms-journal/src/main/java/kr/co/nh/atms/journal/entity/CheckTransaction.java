package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_CHECK_TRANSACTION - 거래저널(jnl_kind=71) 수표 거래 상세.
 * PK: termGroupId + termId + jnlDate + jnlTime + checkSeq
 *
 * interface.journal.json "71" trx_checks[] 매핑:
 *   (배열 index+1) -> checkSeq, amt -> checkAmount, bk_cd -> bankCd, no -> checkNo
 * (atmr RecvJournalMapper.xml insert_ignore_T_CHECKTRANSACTION 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckTransaction {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private Integer checkSeq;   // NUMBER(10) 수표 순번 (1부터)
    private String checkAmount; // VARCHAR2(10) 수표 금액
    private String bankCd;      // CHAR(3) 은행 코드
    private String checkNo;     // VARCHAR2(32) 수표 번호
}
