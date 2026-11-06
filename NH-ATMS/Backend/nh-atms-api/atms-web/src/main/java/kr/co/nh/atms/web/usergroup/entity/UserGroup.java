package kr.co.nh.atms.web.usergroup.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** T_USER_GROUP (사용자 그룹) — PK: userGroupNo */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGroup {
    private Long userGroupNo;
    private String userGroupName;
    private String userTypeCd;     // A:관리자 U:일반 M:모니터링
    private String description;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime updDt;

    /** 조회 시 매핑된 권한 코드 목록 */
    private List<String> authCodes;
}
