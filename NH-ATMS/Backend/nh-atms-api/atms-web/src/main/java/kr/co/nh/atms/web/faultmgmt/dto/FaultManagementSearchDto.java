package kr.co.nh.atms.web.faultmgmt.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 장애접수(출동요청) 이력 검색 조건.
 */
@Getter
@Setter
public class FaultManagementSearchDto extends PageRequest {
    private String termGroupId;   // 영업점(금고)코드
    private String termId;        // 기번(단말번호)
    private String faultStatus;   // 상태 (O/P/C/X)
    private String faultType;     // 출동요청사유
    private String vendorCd;      // 출동업체코드
    private String fromDate;      // 출동요청일 시작 (YYYYMMDD)
    private String toDate;        // 출동요청일 종료 (YYYYMMDD)
}
