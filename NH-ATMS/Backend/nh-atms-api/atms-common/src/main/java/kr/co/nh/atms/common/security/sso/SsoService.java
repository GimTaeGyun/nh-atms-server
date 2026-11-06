package kr.co.nh.atms.common.security.sso;

/**
 * SSO 인증 인터페이스.
 * 구현체를 교체하여 Mock / WiseAccess / NH SSO 전환.
 */
public interface SsoService {

    /** ID/PW 로 SSO 인증 */
    SsoResult authenticate(String userId, String password, String clientIp);

    /** SSO 토큰 검증 */
    SsoResult verifyToken(String ssoToken, String clientIp);

    /** SSO 로그아웃 (토큰 폐기) */
    void logout(String ssoToken);
}
