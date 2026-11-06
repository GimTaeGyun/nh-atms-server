package kr.co.nh.atms.common.db;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;

/**
 * 로컬(PC, 프로파일 local) 전용 DB 계정 잠김 방지 (2026-11-05).
 * - 계정 파일 D:\local-db.yml 이 없거나 비어 있으면 DB 에 접속하지 않고 기동을 멈춘다.
 * - 로그인 실패(ORA-01017 아이디/비번 틀림, ORA-28000 잠김)를 D:\db-login-failed 에 기록하고,
 *   같은 주소·계정·비번으로는 다시 접속하지 않는다 (재기동·재빌드 포함).
 *   비번을 고치면 자동으로 1회 다시 시도한다. 기록 파일에는 해시만 남고 비번은 남지 않는다.
 * 개발서버(jeus 프로파일, JNDI)에는 적용되지 않는다.
 */
@Slf4j
@Component
@Profile("local")
public class LocalDbLoginGuard implements BeanPostProcessor {

    private static final Path DIR = Path.of("D:/");   // PC별 파일은 D 드라이브 루트
    private static final Path ACCOUNT_FILE = DIR.resolve("local-db.yml");
    private static final Path FAILED_FILE = DIR.resolve("db-login-failed");
    private static final Set<Integer> LOGIN_ERRORS = Set.of(1017, 28000);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // p6spy 등이 먼저 감싸도 안쪽 Hikari 를 찾아 검사한다
        if (!(bean instanceof DataSource outer) || bean instanceof Guarded) {
            return bean;
        }
        HikariDataSource ds = unwrapHikari(outer);
        if (ds == null) {
            return bean;
        }
        if (isBlank(ds.getUsername()) || isBlank(ds.getPassword())) {
            throw new IllegalStateException("[DB-GUARD] 로컬 DB 계정이 없습니다. " + ACCOUNT_FILE
                    + " 에 spring.datasource.username / password 를 적으세요. (DB 접속은 시도하지 않았습니다)");
        }
        String key = fingerprint(ds.getJdbcUrl(), ds.getUsername(), ds.getPassword());
        if (alreadyFailed(key)) {
            throw new IllegalStateException("[DB-GUARD] 이 계정(" + ds.getUsername()
                    + ")·비밀번호는 이미 로그인에 실패한 기록이 있어 DB 에 접속하지 않습니다 (계정 잠김 방지). "
                    + ACCOUNT_FILE + " 의 비밀번호를 고치면 자동으로 다시 시도합니다. "
                    + "비밀번호가 맞는데 잠김 해제를 받은 경우에만 " + FAILED_FILE + " 파일을 지우세요.");
        }
        return new Guarded(outer, key, ds.getUsername());
    }

    private static final class Guarded extends DelegatingDataSource {
        private final String key;
        private final String username;
        private volatile SQLException blocked;

        Guarded(DataSource target, String key, String username) {
            super(target);
            this.key = key;
            this.username = username;
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (blocked != null) {
                throw new SQLException("[DB-GUARD] 로그인 실패 후 재접속 차단: " + blocked.getMessage(), blocked);
            }
            try {
                return super.getConnection();
            } catch (SQLException e) {
                Integer code = loginErrorCode(e);
                if (code != null) {
                    blocked = e;
                    record(key, code, username);
                }
                throw e;
            }
        }
    }

    private static HikariDataSource unwrapHikari(DataSource ds) {
        try {
            return ds.isWrapperFor(HikariDataSource.class) ? ds.unwrap(HikariDataSource.class) : null;
        } catch (SQLException e) {
            return null;
        }
    }

    private static Integer loginErrorCode(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException s && LOGIN_ERRORS.contains(s.getErrorCode())) {
                return s.getErrorCode();
            }
        }
        return null;
    }

    private static void record(String key, int code, String username) {
        try {
            String line = key + " " + LocalDateTime.now().withNano(0) + " ORA-" + String.format("%05d", code)
                    + " " + username + System.lineSeparator();
            Files.writeString(FAILED_FILE, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) {
            log.warn("[DB-GUARD] 실패 기록 저장 못함 {} : {}", FAILED_FILE, ex.getMessage());
        }
        log.error("[DB-GUARD] DB 로그인 실패 ORA-{} (계정 {}) → 이 계정·비번으로는 더 접속하지 않습니다. {} 를 확인하세요.",
                String.format("%05d", code), username, ACCOUNT_FILE);
    }

    private static boolean alreadyFailed(String key) {
        try {
            return Files.exists(FAILED_FILE)
                    && Files.readAllLines(FAILED_FILE, StandardCharsets.UTF_8).stream().anyMatch(l -> l.startsWith(key));
        } catch (Exception ex) {
            return false;
        }
    }

    private static String fingerprint(String url, String user, String password) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest((url + "\n" + user + "\n" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
