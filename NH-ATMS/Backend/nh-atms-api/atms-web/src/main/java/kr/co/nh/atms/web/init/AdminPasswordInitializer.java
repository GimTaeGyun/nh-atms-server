package kr.co.nh.atms.web.init;

import kr.co.nh.atms.web.user.entity.User;
import kr.co.nh.atms.web.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 부팅 시 admin 계정 비밀번호 보정.
 *  - V1.0.1 시드 BCrypt 해시가 실제 평문과 매칭되지 않을 수 있어,
 *    개발 환경에서 자동으로 재해싱하여 UPDATE.
 *  - 운영에서는 application.yml 에서 init.admin.reset-password=false 로 끄세요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPasswordInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${init.admin.reset-password:false}")
    private boolean enabled;

    @Value("${init.admin.user-id:admin}")
    private String userId;

    @Value("${init.admin.raw-password:admin1234!}")
    private String rawPassword;

    @Override
    public void run(String... args) {
        if (!enabled) return;
        try {
            User u = userMapper.selectOne(userId);
            if (u == null) {
                log.warn("[AdminPasswordInitializer] '{}' 사용자가 없어 건너뜀", userId);
                return;
            }
            if (passwordEncoder.matches(rawPassword, u.getPassword())) {
                log.info("[AdminPasswordInitializer] '{}' 비밀번호가 이미 정상입니다", userId);
                return;
            }
            String newHash = passwordEncoder.encode(rawPassword);
            int updated = userMapper.updatePassword(userId, newHash);
            log.warn("[AdminPasswordInitializer] '{}' 비밀번호 재설정 완료 (rows={})", userId, updated);
        } catch (Exception e) {
            // DB 연결 실패 등 일시적 오류 시 앱 구동을 막지 않고 경고만 출력
            log.warn("[AdminPasswordInitializer] 비밀번호 초기화 건너뜀 — DB 연결 실패: {}", e.getMessage());
        }
    }
}
