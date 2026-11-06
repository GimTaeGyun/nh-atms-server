package kr.co.nh.atms.web.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModuleGatewayFilter 단위 테스트 — JDK HttpServer 를 대상 모듈로 띄워 실제 전달을 확인한다.
 */
@DisplayName("ModuleGatewayFilter 단위 테스트")
class ModuleGatewayFilterTest {

    private HttpServer downstream;
    private final AtomicReference<String> seenUri = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();
    private final AtomicReference<String> seenXff = new AtomicReference<>();
    private final AtomicReference<String> seenAuth = new AtomicReference<>();
    private ModuleGatewayFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        downstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        downstream.createContext("/", ex -> {
            seenUri.set(ex.getRequestURI().toString());
            seenBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            seenXff.set(ex.getRequestHeaders().getFirst("X-Forwarded-For"));
            seenAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            byte[] out = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.getResponseHeaders().add("X-Module", "journal");
            ex.sendResponseHeaders(201, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        downstream.start();

        ModuleGatewayProperties.Route journal = new ModuleGatewayProperties.Route();
        journal.setPrefix("/api/agent");
        journal.setTarget("http://127.0.0.1:" + downstream.getAddress().getPort() + "/journal/");
        ModuleGatewayProperties props = new ModuleGatewayProperties();
        props.setRoutes(List.of(journal));
        filter = new ModuleGatewayFilter(props);
    }

    @AfterEach
    void tearDown() {
        downstream.stop(0);
    }

    @Test
    @DisplayName("접두어가 일치하면 대상 컨텍스트로 경로·쿼리·본문·헤더를 그대로 전달한다")
    void matchingPrefix_forwardsToModuleContext() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/agent/ATM00001/command");
        req.setQueryString("a=1&b=%ED%95%9C");
        req.setContentType("application/json");
        req.setContent("{\"cmd\":\"REBOOT\"}".getBytes(StandardCharsets.UTF_8));
        req.addHeader("Authorization", "Bearer t");
        req.addHeader("X-Forwarded-For", "10.0.0.9");
        req.setRemoteAddr("10.1.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).as("다음 필터로 넘기지 않음").isNull();
        assertThat(seenUri.get()).isEqualTo("/journal/api/agent/ATM00001/command?a=1&b=%ED%95%9C");
        assertThat(seenBody.get()).isEqualTo("{\"cmd\":\"REBOOT\"}");
        assertThat(seenAuth.get()).isEqualTo("Bearer t");
        assertThat(seenXff.get()).isEqualTo("10.0.0.9, 10.1.1.1");
        assertThat(res.getStatus()).isEqualTo(201);
        assertThat(res.getHeader("X-Module")).isEqualTo("journal");
        assertThat(res.getContentAsString()).isEqualTo("{\"ok\":true}");
    }

    @Test
    @DisplayName("접두어가 다르면 전달하지 않고 atms-web 이 처리한다")
    void otherPaths_passThroughChain() throws Exception {
        for (String path : List.of("/api/agentx", "/api/atm/list", "/journal/api/agent/x")) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(req, new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).as(path).isSameAs(req);
        }
        assertThat(seenUri.get()).isNull();
    }

    @Test
    @DisplayName("대상 모듈에 연결할 수 없으면 502 를 직접 응답 (sendError 로 /error 에 넘기면 Security 가 401 로 바꿈)")
    void downstreamUnavailable_returns502() throws Exception {
        downstream.stop(0);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/agent");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(502);
        assertThat(res.getContentType()).startsWith("application/json");
        assertThat(res.getContentAsString(StandardCharsets.UTF_8)).contains("\"success\":false", "\"code\":\"502\"");
    }
}
