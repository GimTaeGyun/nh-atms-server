package kr.co.nh.atms.web.cash.entity;

import lombok.Data;

/**
 * 기기시재 조회 결과 (T_ADD_CASH 카세트별 최신 잔량 스냅샷).
 */
@Data
public class CashPosition {
    private String termGroupId;    // 영업점코드
    private String termId;         // 단말번호
    private String jnlDate;        // 시재 기준일자 (최신 행, YYYYMMDD)
    private String jnlTime;        // 시재 기준시각
    private String cassetteType;   // 카세트 구분
    private String cassetteNo;     // 카세트 번호
    private String currency;       // 통화
    private Long denomination;     // 권종(액면)
    private Integer remainQty;     // 잔량(매)
    private Long amount;           // 금액 = 권종 * 잔량
}
