package kr.co.nh.atms.web.daytotal.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 마감조회 검색조건.
 */
@Getter
@Setter
public class DayTotalSearchDto extends PageRequest {
    private String termGroupId;   // 영업점코드
    private String termId;        // 단말번호
    private String jnlDate;       // 마감일자(YYYYMMDD)
    private String actionType;    // 마감종류(5=소계,6=합계)
    private String totalSrcCd;    // 집계원천(1=호스트,2=기기)
    private String totalCd;       // 마감항목코드
}
