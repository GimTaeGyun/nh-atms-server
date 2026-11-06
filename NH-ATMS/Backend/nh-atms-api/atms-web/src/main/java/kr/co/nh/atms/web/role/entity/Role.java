package kr.co.nh.atms.web.role.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** T_AUTH (권한/롤 마스터) — PK: authCode */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Role {
    private String authCode;
    private String authName;
    private String authDesc;
    private String authType;       // SYSTEM / USER
    private Long   fstgMenuNo;    // 로그인 후 초기 이동 메뉴번호
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime updDt;
}
