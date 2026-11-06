package kr.co.nh.atms.ifin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 시재조회(ATMONST10100) 응답의 텔러별 시재 (AS-IS s_amt 57byte * 30 반복 파싱 결과).
 */
@Data
@Builder
public class TellerAmount {
    /** 텔러번호(3) */
    private String tellerNo;
    /** 텔러시재금액(18) */
    private Long tellerAmt;
    /** 자점금액(18) */
    private Long selfBranchAmt;
    /** 타점금액(18) */
    private Long otherBranchAmt;
}
