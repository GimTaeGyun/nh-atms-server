package kr.co.nh.atms.web.role.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RoleSearchDto extends PageRequest {
    private String authCode;
    private String authName;
    private String authType;
    private String useYn;
}
