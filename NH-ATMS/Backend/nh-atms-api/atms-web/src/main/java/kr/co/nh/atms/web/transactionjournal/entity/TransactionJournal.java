package kr.co.nh.atms.web.transactionjournal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * T_TRANSACTION_JOURNAL — 거래 저널 조회 엔티티.
 * 조회 전용. 적재는 atms-journal 의 동명 엔티티.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionJournal {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;

    private String trNo;
    private String trType;
    private String trFromAccount;
    private String trToAccount;

    private String bankCd;
    private String accountNo;
    private String toBankCd;
    private String toAccountNo;

    private String trHostDate;
    private String trHostTime;
    private String trHostNo;
    private String trHostAuditNo;
    private String hostNetId;
    private String hostSettleDate;
    private String depositNo;

    private BigDecimal surcharge;
    private BigDecimal requestAmount;
    private BigDecimal dispenseAmount;
    private BigDecimal remainAmount;
    private BigDecimal availAmount;
    private BigDecimal hostCashAmount;
    private BigDecimal hostCheckAmount;

    private String procCount;
    private String trResult;
    private String errCd1;
    private String errCd2;
    private String errCd3;

    private String cardData;
    private String nonCashValue;
    private String nonCashType;
    private String trMediaCd;

    private String message;
    private String totalTypeCd;
    private String transferName;
}
