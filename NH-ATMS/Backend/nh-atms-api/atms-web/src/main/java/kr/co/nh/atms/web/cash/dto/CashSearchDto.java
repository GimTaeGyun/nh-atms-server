package kr.co.nh.atms.web.cash.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashSearchDto extends PageRequest {
    private String termGroupId;   // 영업점코드
    private String termId;        // 단말번호
    private String baseDate;      // 시재 기준일자 (YYYYMMDD, 이 일자까지의 최신 시재)
}
