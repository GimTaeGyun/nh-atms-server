package kr.co.nh.atms.ifin.gateway.eai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 은행 EAI 게이트웨이 접속/동작 설정.
 * 구 atmi config.*.json 의 eai 섹션 이식.
 * NH EAI 규격 확정 시 접속 정보(ip/port)와 클라이언트 식별 값(client-ip/mac)이 변경될 수 있다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "internal.gateway.eai")
public class EaiGatewayProperties {

    /** EAI 게이트웨이 IP. 구 config.json eai.ip */
    private String ip = "127.0.0.1";

    /** EAI 게이트웨이 포트. 구 config.json eai.port */
    private int port = 39021;

    /** 송신 워커 수. 구 eai.send.worker */
    private int sendWorker = 1;

    /** 상시 연결 유지 여부. 구 eai.send.keep-connect */
    private boolean keepConnect = true;

    /** 소켓 연결 타임아웃(ms). 구 AS-IS selector.select(2500) */
    private int connectTimeoutMs = 2500;

    /** 소켓 응답 수신 타임아웃(ms) */
    private int readTimeoutMs = 30_000;

    /** 호출 스레드의 응답 대기 타임아웃(ms). 구 AS-IS waitor.wait(1000*30) */
    private long replyTimeoutMs = 30_000;

    /** 미연결 상태에서 keep-connect 재접속 시도 간격(ms). 구 AS-IS 1000*30 */
    private long reconnectIntervalMs = 30_000;

    /** IIF 전문 스펙(interface.internal.json) classpath 위치 */
    private String specLocation = "eai/interface.internal.json";

    /** IIF 인터페이스명(스펙 루트 키) */
    private String interfaceName = "#iif#";

    /**
     * GlobalID 요청시스템(8자) 값. 미지정 시 환경변수 HOSTNAME 사용
     * (AS-IS: atmap* 이 아니면 atmap01t 폴백 후 대문자화)
     */
    private String hostname = "";

    /** 표준전문 헤더 클라이언트IP(12자). AS-IS 고정값 이식 - NH 규격 확정 시 재검토 */
    private String clientIp = "172025010123";

    /** 표준전문 헤더 클라이언트MAC(12자). AS-IS 고정값 이식 - NH 규격 확정 시 재검토 */
    private String clientMac = "005056b9601b";
}
