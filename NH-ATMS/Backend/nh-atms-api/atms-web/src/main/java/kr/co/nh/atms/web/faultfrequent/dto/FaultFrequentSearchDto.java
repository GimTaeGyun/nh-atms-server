package kr.co.nh.atms.web.faultfrequent.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaultFrequentSearchDto extends PageRequest {
    private String termGroupId;   // 영업점코드
    private String termId;        // 단말번호
    private String fromDate;      // 발생일자 시작 (YYYYMMDD)
    private String toDate;        // 발생일자 종료 (YYYYMMDD)
    private Integer minFaultCnt;  // 최소 장애 건수 (기본 1)
}
