package kr.co.nh.atms.file.firmware.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 펌웨어 다운로드 저장소/원격연동 설정 (구 atmf 펌웨어 채널).
 *
 *   file.firmware.repo-dir      : Agent 에 제공할 펌웨어 파일을 보관하는 로컬 루트
 *   file.firmware.sftp.enabled  : true 일 때만 로컬에 없는 펌웨어를 원격 SFTP 서버에서 끌어온다
 *                                 (dev/local 기본 off — IfQueueWorker 등과 동일한 안전 기본값)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "file.firmware")
public class FirmwareProperties {

    /** 로컬 펌웨어 저장소 루트. 하위에 펌웨어 파일을 평면 보관. */
    private String repoDir = "./data/atms-firmware";

    private final Sftp sftp = new Sftp();

    @Getter
    @Setter
    public static class Sftp {
        /** 원격 SFTP 펌웨어 서버 연동 사용 여부 (기본 off). */
        private boolean enabled = false;
        private String host;
        private int port = 22;
        private String username;
        private String password;
        /** 개인키 파일 경로(선택). 지정 시 비밀번호 대신 키 인증을 사용한다. */
        private String privateKeyPath;
        private String privateKeyPassphrase;
        /** 원격 펌웨어 디렉터리. */
        private String remoteDir = "/firmware";
    }
}
