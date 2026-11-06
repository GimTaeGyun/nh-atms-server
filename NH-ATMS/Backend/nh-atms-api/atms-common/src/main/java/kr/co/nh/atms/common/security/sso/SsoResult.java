package kr.co.nh.atms.common.security.sso;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SsoResult {

    private boolean success;
    private String ssoToken;
    private String userId;
    private String userName;
    private String deptCode;
    private String deptName;
    private String roleCode;
    private String errorMessage;

    public static SsoResult fail(String message) {
        return SsoResult.builder()
                .success(false)
                .errorMessage(message)
                .build();
    }
}
