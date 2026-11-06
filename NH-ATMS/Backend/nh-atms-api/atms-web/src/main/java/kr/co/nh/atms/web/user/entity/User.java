package kr.co.nh.atms.web.user.entity;

import kr.co.nh.atms.common.security.crypto.EncryptedField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** T_USER (사용자 / 로그인 계정) — PK: userId */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String userId;
    private String userName;
    @EncryptedField   // 사용자 등록 시 화면이 ENC1 암호문으로 보냄 (docs/FIELD_ENCRYPTION_DESIGN.md)
    private String password;
    private String empNo;
    private Long userGroupNo;
    private String userTypeCd;       // A:관리자 U:일반 M:모니터링
    private String email;
    private String phoneNo;
    private String cellPhoneNo;
    private String deptCd;
    private String deptName;
    private String branchCd;
    private String userPosition;
    private LocalDateTime lastLoginDt;
    private String loginFlag;        // Y:로그인중 N:로그아웃
    private Integer loginFailCnt;
    private String lockYn;
    private LocalDateTime pwExpireDt;
    private LocalDateTime infoChgDt;
    private String useYn;
    private LocalDateTime regDt;
    private String regId;
    private LocalDateTime updDt;
    private String updId;

    // 조회 시 권한 정보 캐시 (T_USER_GROUP_AUTH 조인 결과 - 단일 권한 대표)
    private String authCode;
    private String userGroupName;
}
