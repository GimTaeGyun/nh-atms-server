package kr.co.nh.atms.web.remoteexec.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 원격명령 송신(atms-file) 연동 설정.
 * 구 새마을금고 atms 는 atmf(127.0.0.1:8600) 로 직접 호출했으나,
 * NH 는 atms-file 모듈(docker 서비스 atms-file:8083)로 전송한다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "atmf.remote")
public class RemoteExecProperties {

    /** atms-file 의 전문 송신 엔드포인트(snd.msg.do). */
    private String sndMsgUri = "http://localhost:8083/atmf/snd.msg.do";

    /** 송신 연결/응답 타임아웃(ms). */
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 10000;
}
