package kr.co.nh.atms.web.auth.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.security.jwt.JwtTokenProvider;
import kr.co.nh.atms.web.auth.dto.LoginRequest;
import kr.co.nh.atms.web.auth.dto.LoginResponse;
import kr.co.nh.atms.web.auth.dto.RefreshRequest;
import kr.co.nh.atms.web.auth.mapper.LoginHistMapper;
import kr.co.nh.atms.web.user.entity.User;
import kr.co.nh.atms.web.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증 서비스.
 *   - sso.provider = local : T_USER (Oracle) 기반 로그인
 *   - sso.provider = mock  : 동일하게 로컬 DB 로그인 (개발 편의)
 *   - sso.provider = nh    : (TODO) NH SSO 연동
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAIL_COUNT = 5;

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final LoginHistMapper loginHistMapper;

    @Value("${sso.provider:local}")
    private String ssoProvider;

    // TODO: 운영에서는 Redis 로 교체
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    @Transactional
    public LoginResponse login(LoginRequest request) {
        final String userId  = request.getUserId();
        final String rawPw   = request.getPassword();
        final String clientIp = request.getClientIp();

        // 1) 사용자 조회
        User user = userMapper.findForLogin(userId);
        if (user == null) {
            writeHist(userId, clientIp, "F", "사용자 없음");
            throw new BusinessException("AUTH_001", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 2) 사용 가능 여부
        if (!"Y".equalsIgnoreCase(user.getUseYn())) {
            writeHist(userId, clientIp, "F", "비활성 계정");
            throw new BusinessException("AUTH_003", "비활성화된 계정입니다.");
        }
        if ("Y".equalsIgnoreCase(user.getLockYn())) {
            writeHist(userId, clientIp, "F", "잠금 계정");
            throw new BusinessException("AUTH_004", "잠긴 계정입니다. 관리자에게 문의하세요.");
        }

        // 3) 비밀번호 검증
        if (!passwordEncoder.matches(rawPw, user.getPassword())) {
            userMapper.incrementLoginFail(userId);
            int after = (user.getLoginFailCnt() == null ? 0 : user.getLoginFailCnt()) + 1;
            String reason = after >= MAX_FAIL_COUNT
                    ? "비밀번호 불일치 (자동 잠금)"
                    : "비밀번호 불일치 (" + after + "/" + MAX_FAIL_COUNT + ")";
            writeHist(userId, clientIp, "F", reason);
            throw new BusinessException("AUTH_001", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 4) 로그인 성공 처리
        userMapper.updateLoginSuccess(userId);
        writeHist(userId, clientIp, "S", null);

        // 5) JWT 발급
        String roleId = user.getAuthCode() != null ? user.getAuthCode() : "ROLE_USER";
        String accessToken  = jwtTokenProvider.createAccessToken(user.getUserId(), user.getUserName(), roleId);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
        refreshTokenStore.put(user.getUserId(), refreshToken);

        log.info("로그인 성공: userId={}, provider={}, role={}", userId, ssoProvider, roleId);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserInfo.builder()
                        .userId(user.getUserId())
                        .userName(user.getUserName())
                        .deptName(user.getDeptName())
                        .roleId(roleId)
                        .build())
                .build();
    }

    public LoginResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("AUTH_002", "유효하지 않은 Refresh Token입니다.");
        }

        String userId = jwtTokenProvider.getUserId(refreshToken);
        String storedToken = refreshTokenStore.get(userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException("AUTH_002", "유효하지 않은 Refresh Token입니다.");
        }

        User user = userMapper.selectOne(userId);
        if (user == null || !"Y".equalsIgnoreCase(user.getUseYn())) {
            throw new BusinessException("AUTH_003", "사용할 수 없는 계정입니다.");
        }

        String userName = jwtTokenProvider.getUserName(refreshToken);
        String roleId   = jwtTokenProvider.getRoleId(refreshToken);

        String newAccessToken  = jwtTokenProvider.createAccessToken(userId, userName, roleId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        refreshTokenStore.put(userId, newRefreshToken);

        log.info("토큰 갱신 성공: userId={}", userId);
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(LoginResponse.UserInfo.builder()
                        .userId(userId)
                        .userName(userName)
                        .deptName(user.getDeptName())
                        .roleId(roleId)
                        .build())
                .build();
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenStore.remove(userId);
        userMapper.updateLogout(userId);
        loginHistMapper.updateLogoutTime(userId);
        log.info("로그아웃: userId={}", userId);
    }

    private void writeHist(String userId, String ip, String result, String reason) {
        try {
            loginHistMapper.insertHist(userId, ip, result, reason, null);
        } catch (Exception e) {
            log.warn("로그인 이력 기록 실패: userId={}, err={}", userId, e.getMessage());
        }
    }
}

