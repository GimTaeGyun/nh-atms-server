package kr.co.nh.atms.web.gateway;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 모듈 게이트웨이 설정 (JEUS 단일 서버 배포 + 로컬 bootRun).
 *
 * JEUS 에서는 6개 모듈이 한 서버(atms01)에 컨텍스트 경로로 나뉘어 올라간다
 * (atms-web='/', atms-journal='/journal', atms-batch='/batch' …).
 * 화면·ATM 에이전트·대외기관은 기존처럼 '/api/batch', '/atmf/agent' 같은 고정 경로를 부르므로,
 * 루트 컨텍스트(atms-web)가 접두어별로 해당 모듈 컨텍스트로 요청을 그대로 전달한다.
 *
 * 로컬(bootRun)에서는 같은 고정 경로를 각 모듈 포트(8081~8085)로 전달해 JEUS 와 같은 경로 구조로 동작한다.
 *
 * 기본값 enabled=false. 켜는 곳: application-jeus.yml (JEUS), application.yml 의 local 프로파일 문서 (로컬).
 * 컨테이너(Docker Compose, 프로파일 dev)는 nginx 가 모듈별로 직접 라우팅하므로 꺼져 있다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class ModuleGatewayProperties {

    /** 게이트웨이 사용 여부. */
    private boolean enabled = false;

    /** 다운스트림 연결 타임아웃(ms). */
    private int connectTimeoutMs = 3000;

    /** 다운스트림 응답 타임아웃(ms). 파일 다운로드 등을 고려해 넉넉히. */
    private int readTimeoutMs = 120000;

    /** 접두어 → 대상 모듈 주소. 위에서부터 먼저 일치하는 항목 사용. */
    private List<Route> routes = new ArrayList<>();

    @Getter
    @Setter
    public static class Route {
        /** 요청 경로 접두어 (예: /api/batch). 이 경로 자체 또는 '접두어/...' 가 일치. */
        private String prefix;
        /** 대상 주소 (예: http://127.0.0.1:8766/batch). 원래 경로가 그대로 뒤에 붙는다. */
        private String target;
    }
}
