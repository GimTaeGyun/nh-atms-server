package kr.co.nh.atms.web.atmstatus.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AtmStatusSearchDto extends PageRequest {
    private String termGroupId;
    private String termId;
    private String serviceStatus;
    private String atmFaultStatus;
    private String errGroupCd;
}
