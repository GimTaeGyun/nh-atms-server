package kr.co.nh.atms.web.atmmonitor.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtmMonitorSearchDto extends PageRequest {
    private String termGroupId;
    private String termId;
    private String atmStatus;
    private String errCd;
    private String fromDate;     // YYYYMMDD
    private String toDate;       // YYYYMMDD
    private Boolean errorOnly;   // true 면 ERR_CD IS NOT NULL 만
}
