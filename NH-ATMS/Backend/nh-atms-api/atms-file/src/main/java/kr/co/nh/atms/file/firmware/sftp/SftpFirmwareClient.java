package kr.co.nh.atms.file.firmware.sftp;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.file.firmware.config.FirmwareProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 원격 SFTP 펌웨어 서버에서 파일을 로컬 저장소로 내려받는 클라이언트 (구 atmf 펌웨어 채널).
 *
 * <p>spring-integration-sftp 의 {@link Session} 추상화를 사용하므로 JSch/MINA 등
 * 하위 구현 변화에 영향받지 않는다. {@code file.firmware.sftp.enabled=true} 일 때만 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SftpFirmwareClient {

    private final FirmwareProperties props;

    public boolean isEnabled() {
        return props.getSftp().isEnabled();
    }

    /**
     * 원격 {remoteDir}/{fileName} 을 localTarget 으로 내려받는다.
     *
     * @throws BusinessException SFTP 비활성/원격 미존재/전송 실패 시
     */
    public void fetch(String fileName, Path localTarget) {
        FirmwareProperties.Sftp s = props.getSftp();
        if (!s.isEnabled()) {
            throw new BusinessException("SFTP_DISABLED", "SFTP 펌웨어 연동이 비활성화되어 있습니다.");
        }
        DefaultSftpSessionFactory factory = buildFactory(s);
        String remotePath = joinRemote(s.getRemoteDir(), fileName);

        try (Session<?> session = factory.getSession()) {
            if (!session.exists(remotePath)) {
                throw new BusinessException("FW_NOT_FOUND_REMOTE", "원격 펌웨어가 없습니다: " + fileName);
            }
            Path parent = localTarget.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            InputStream in = session.readRaw(remotePath);
            try {
                Files.copy(in, localTarget, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                if (in != null) {
                    in.close();
                }
                session.finalizeRaw();
            }
            log.info("[firmware] SFTP fetched {} -> {}", remotePath, localTarget);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("SFTP_FETCH_FAIL", "SFTP 펌웨어 다운로드 실패: " + e.getMessage());
        }
    }

    private DefaultSftpSessionFactory buildFactory(FirmwareProperties.Sftp s) {
        DefaultSftpSessionFactory f = new DefaultSftpSessionFactory();
        f.setHost(s.getHost());
        f.setPort(s.getPort());
        f.setUser(s.getUsername());
        // 내부망 펌웨어 NAS 대상 — 호스트키 사전등록이 어려운 환경을 고려해 미검증 허용.
        f.setAllowUnknownKeys(true);
        if (s.getPrivateKeyPath() != null && !s.getPrivateKeyPath().isBlank()) {
            f.setPrivateKey(new FileSystemResource(s.getPrivateKeyPath()));
            if (s.getPrivateKeyPassphrase() != null && !s.getPrivateKeyPassphrase().isBlank()) {
                f.setPrivateKeyPassphrase(s.getPrivateKeyPassphrase());
            }
        } else {
            f.setPassword(s.getPassword());
        }
        return f;
    }

    private static String joinRemote(String dir, String file) {
        if (dir == null || dir.isBlank()) {
            return file;
        }
        return dir.endsWith("/") ? dir + file : dir + "/" + file;
    }
}
