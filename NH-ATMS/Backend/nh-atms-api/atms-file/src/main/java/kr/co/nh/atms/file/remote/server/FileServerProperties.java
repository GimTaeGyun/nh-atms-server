package kr.co.nh.atms.file.remote.server;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인바운드 NIO 소켓 파일서버 설정.
 * 구 atmf config.json (bind.ip/port, session.buf/max, repository, l4) 이식.
 *
 * enabled 기본 false — 명시적으로 켜기 전에는 포트를 bind 하지 않아 기존 배포에 무영향.
 */
@ConfigurationProperties(prefix = "atmf.server")
public class FileServerProperties {

    /** 소켓 서버 기동 여부. 기본 false (기존 배포 무영향). */
    private boolean enabled = false;

    /** bind IP. 구 atmf config bind.ip. */
    private String bindIp = "0.0.0.0";

    /** bind 포트. */
    private int port = 21001;

    /** 세션당 버퍼 사이즈(byte). 구 atmf config session.buf (128KB). 내부에서 128 배수로 보정. */
    private int buf = 131072;

    /** 최대 동시 세션 수. 구 atmf config session.max. */
    private int max = 1000;

    /** 파일 저장소 루트. 구 atmf config repository. */
    private String repository = "./data/atms-repo";

    /**
     * L4 스위치 헬스체크 IP 목록. 구 atmf config l4 배열.
     * 목록의 IP 에서 접속하면 헬스체크로 간주하고 즉시 연결을 종료한다 (구 Server.bgn 매칭 로직 동일).
     */
    private List<String> l4Allowlist = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBindIp() { return bindIp; }
    public void setBindIp(String bindIp) { this.bindIp = bindIp; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getBuf() { return buf; }
    public void setBuf(int buf) { this.buf = buf; }

    public int getMax() { return max; }
    public void setMax(int max) { this.max = max; }

    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }

    public List<String> getL4Allowlist() { return l4Allowlist; }
    public void setL4Allowlist(List<String> l4Allowlist) { this.l4Allowlist = l4Allowlist; }
}
