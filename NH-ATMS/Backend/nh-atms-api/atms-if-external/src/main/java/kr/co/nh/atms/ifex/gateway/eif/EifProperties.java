package kr.co.nh.atms.ifex.gateway.eif;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * EIF 실연동 게이트웨이 설정 (external.gateway.*).
 *
 * <pre>
 * external:
 *   gateway:
 *     mode: eif                                # mock(기본) | eif
 *     spec-location: eif/interface.external.json
 *     org-cd: "045"
 *     connect-timeout-ms: 3000
 *     read-timeout-ms: 5000
 *     vendors:
 *       "11": { ip: 10.0.0.1, port: 9001 }     # 업체코드별 송신 엔드포인트 (구 config.json vendor/remote)
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "external.gateway")
public class EifProperties {

    /** 게이트웨이 모드 (mock: 기본 Mock, eif: 고정길이 전문 TCP 실연동) */
    private String mode = "mock";

    /** 전문 스펙(json) 클래스패스 위치 */
    private String specLocation = "eif/interface.external.json";

    /** Converter 인터페이스명 (스펙 루트 키) */
    private String interfaceName = "#eif#";

    /** 기관코드 (045: 전문 헤더 org_cd) */
    private String orgCd = "045";

    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;

    /** 업체코드별 송신 엔드포인트 (구 atme config.json vendor[].remote) */
    private Map<String, Endpoint> vendors = new HashMap<>();

    @Getter
    @Setter
    public static class Endpoint {
        private String ip;
        private int port;
    }
}
