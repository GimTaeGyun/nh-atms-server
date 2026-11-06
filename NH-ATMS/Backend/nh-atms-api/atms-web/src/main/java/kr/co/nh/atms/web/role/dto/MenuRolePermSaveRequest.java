package kr.co.nh.atms.web.role.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PUT /api/role/menu/{menuNo}/perm/{authCode} 요청 바디 */
@Getter @Setter @NoArgsConstructor
public class MenuRolePermSaveRequest {
    private String readYn;
    private String writeYn;
}
