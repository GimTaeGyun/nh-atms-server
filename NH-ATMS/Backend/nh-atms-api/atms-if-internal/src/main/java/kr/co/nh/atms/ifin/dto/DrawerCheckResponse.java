package kr.co.nh.atms.ifin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 시재 조회 응답.
 */
@Data
@Builder
public class DrawerCheckResponse {
    private String globalId;
    private String resultCd;
    private String resultMsg;
    private String termGroupId;
    private String termId;
    /** 권종별 시재 보유 매수 합계 */
    private Long totalCashCount;
    /** 권종별 시재 보유 금액 합계 */
    private Long totalCashAmount;
    /** 텔러별 시재 (AS-IS ATMONST10100 응답 s_amt 반복 파싱) */
    private List<TellerAmount> tellerAmounts;
    /** T_GW_INTERFACE 이력 키 */
    private Long gwSeq;
}
