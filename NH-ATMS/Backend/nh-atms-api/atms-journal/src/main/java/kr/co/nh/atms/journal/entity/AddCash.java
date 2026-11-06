package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ADD_CASH — 현금 시재 보충/회수 (jnl_kind=41).
 * PK: termGroupId + termId + jnlDate + jnlTime + actionType + cassetteType + cassetteNo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCash {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String actionType;    // 1=보충, 2=회수
    private String cassetteType;  // 1=현금, 2=수표 등
    private String cassetteNo;    // 2자리
    private String currency;      // KRW, USD ...
    private Long   denomination;  // 권종(원)
    private Integer remainQty;
    private Integer addQty;
}
