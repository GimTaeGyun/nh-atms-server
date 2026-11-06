package kr.co.nh.atms.common.security.sso;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock SSO 구현체 (개발용).
 * application.yml 의 sso.provider=mock 일 때 활성화 (기본값).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sso.provider", havingValue = "mock", matchIfMissing = true)
public class MockSsoService implements SsoService {

    private static final Map<String, MockUser> USERS = Map.of(
            "admin", new MockUser("admin", "admin", "관리자", "0001", "IT운영팀", "ADMIN"),
            "user",  new MockUser("user",  "user",  "사용자", "0002", "영업부",   "USER")
    );

    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    @Override
    public SsoResult authenticate(String userId, String password, String clientIp) {
        MockUser user = USERS.get(userId);
        if (user == null || !user.password.equals(password)) {
            log.warn("[MockSSO] 인증 실패: userId={}", userId);
            return SsoResult.fail("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String ssoToken = "MOCK-SSO-" + UUID.randomUUID();
        tokenStore.put(ssoToken, userId);
        log.info("[MockSSO] 인증 성공: userId={}, token={}", userId, ssoToken);

        return SsoResult.builder()
                .success(true)
                .ssoToken(ssoToken)
                .userId(user.userId)
                .userName(user.userName)
                .deptCode(user.deptCode)
                .deptName(user.deptName)
                .roleCode(user.roleCode)
                .build();
    }

    @Override
    public SsoResult verifyToken(String ssoToken, String clientIp) {
        String userId = tokenStore.get(ssoToken);
        if (userId == null) {
            return SsoResult.fail("유효하지 않은 SSO 토큰입니다.");
        }
        MockUser user = USERS.get(userId);
        return SsoResult.builder()
                .success(true)
                .ssoToken(ssoToken)
                .userId(user.userId)
                .userName(user.userName)
                .deptCode(user.deptCode)
                .deptName(user.deptName)
                .roleCode(user.roleCode)
                .build();
    }

    @Override
    public void logout(String ssoToken) {
        String userId = tokenStore.remove(ssoToken);
        log.info("[MockSSO] 로그아웃: userId={}", userId);
    }

    private record MockUser(String userId, String password, String userName,
                             String deptCode, String deptName, String roleCode) {}
}
