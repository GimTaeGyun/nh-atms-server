package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * T_TRANSACTION_JOURNAL — 거래 저널 (jnl_kind=71).
 * PK: termGroupId + termId + jnlDate + jnlTime (T_JOURNAL 과 동일)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionJournal {
    // PK
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;

    // 거래 구분
    private String trNo;
    private String trType;
    private String trFromAccount;
    private String trToAccount;

    // 계좌
    private String bankCd;
    private String accountNo;
    private String toBankCd;
    private String toAccountNo;

    // 호스트 응답
    private String trHostDate;
    private String trHostTime;
    private String trHostNo;
    private String trHostAuditNo;
    private String hostNetId;
    private String hostSettleDate;
    private String depositNo;

    // 금액
    private BigDecimal surcharge;
    private BigDecimal requestAmount;
    private BigDecimal dispenseAmount;
    private BigDecimal remainAmount;
    private BigDecimal availAmount;
    private BigDecimal hostCashAmount;
    private BigDecimal hostCheckAmount;

    // 결과
    private String procCount;
    private String trResult;
    private String errCd1;
    private String errCd2;
    private String errCd3;

    // 카드/매체
    private String cardData;
    private String nonCashValue;
    private String nonCashType;
    private String trMediaCd;

    // 기타
    private String message;
    private String totalTypeCd;
    private String transferName;
}
