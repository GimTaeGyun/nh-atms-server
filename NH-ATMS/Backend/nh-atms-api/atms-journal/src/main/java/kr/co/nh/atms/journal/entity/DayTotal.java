package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_DAY_TOTAL - 마감 저널 (jnl_kind=31). 마감항목 1건 = 1행 (MG t_daytotal 정합).
 * PK: termGroupId + termId + jnlDate + jnlTime + totalSrcCd + totalCd
 *
 * interface.journal.json "31" 매핑:
 *   tot_ty(5소계/6합계)  -> actionType
 *   bgn_, end_ 필드        -> beginDate/beginTime/endDate/endTime
 *   host_entries[]       -> totalSrcCd="1", totalCd=cd, totalVal=val
 *   atm_entries[]        -> totalSrcCd="2", totalCd=cd, totalVal=val
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayTotal {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String totalSrcCd;  // 집계원천: 1=호스트(host), 2=기기(atm)
    private String totalCd;     // 마감항목코드(저널 entry.cd, 4자리)
    private String totalVal;    // 집계값(건수5/금액15)
    private String actionType;  // 마감종류: 5=소계, 6=합계 (저널 tot_ty)
    private String beginDate;   // CHAR(8) 마감기간 시작일
    private String beginTime;   // CHAR(6) 마감기간 시작시각
    private String endDate;     // CHAR(8) 마감기간 종료일
    private String endTime;     // CHAR(6) 마감기간 종료시각
}
