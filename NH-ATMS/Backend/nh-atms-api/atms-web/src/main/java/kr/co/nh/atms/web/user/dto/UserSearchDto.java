package kr.co.nh.atms.web.user.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserSearchDto extends PageRequest {
    private String userId;
    private String userName;
    private String deptCd;
    private String userTypeCd;
    private String useYn;
    private Long   userGroupNo;
}
