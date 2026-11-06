package kr.co.nh.atms.web.atmgroup.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AtmGroupSearchDto extends PageRequest {
    private String termGroupId;
    private String termGroupName;
    private String parentId;
}
