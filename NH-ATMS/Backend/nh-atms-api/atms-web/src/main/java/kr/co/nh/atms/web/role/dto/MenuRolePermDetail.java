package kr.co.nh.atms.web.role.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** T_AUTH_MENU 단건 조회 결과 — 특정 메뉴 + 권한의 조회/수정 허용 여부 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MenuRolePermDetail {
    private String authCode;
    private Long   menuNo;
    private String readYn;
    private String writeYn;
}
