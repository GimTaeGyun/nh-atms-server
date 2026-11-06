package kr.co.nh.atms.web.usergroup.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserGroupSearchDto extends PageRequest {
    private String userGroupName;
    private String userTypeCd;
    private String useYn;
}
