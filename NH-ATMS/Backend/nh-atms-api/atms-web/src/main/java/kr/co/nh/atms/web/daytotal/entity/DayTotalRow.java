package kr.co.nh.atms.web.daytotal.entity;

import lombok.Data;

/**
 * 마감조회 결과 (T_DAY_TOTAL 마감항목별 1행).
 */
@Data
public class DayTotalRow {
    private String termGroupId;   // 영업점코드
    private String termId;        // 단말번호
    private String jnlDate;       // 저널발생일(YYYYMMDD)
    private String jnlTime;       // 저널발생시각
    private String totalSrcCd;    // 집계원천(1=호스트,2=기기)
    private String totalCd;       // 마감항목코드
    private String totalVal;      // 집계값
    private String actionType;    // 마감종류(5=소계,6=합계)
    private String beginDate;     // 마감기간 시작일
    private String beginTime;     // 마감기간 시작시각
    private String endDate;       // 마감기간 종료일
    private String endTime;       // 마감기간 종료시각
}
