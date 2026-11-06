package kr.co.nh.atms.web.download.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** 개별다운로드 적용이력(T_ATM_APPLY_HISTORY) 검색 조건. */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class IndividualHistorySearchDto extends PageRequest {
    private String downNo;
    private String termGroupId;
    private String termId;
    private String applyFlag;   // 0=미적용/1=적용완료/2=다운로드중
    private String fromDate;    // YYYYMMDD
    private String toDate;      // YYYYMMDD
}
