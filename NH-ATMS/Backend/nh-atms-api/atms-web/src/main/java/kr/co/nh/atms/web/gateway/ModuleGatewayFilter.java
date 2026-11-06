package kr.co.nh.atms.web.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 루트 컨텍스트(atms-web)로 들어온 다른 모듈 경로를 해당 모듈 컨텍스트로 그대로 전달하는 리버스 프록시.
 *
 * - Spring Security·DispatcherServlet 보다 앞에서 동작한다. 인증·HMAC 검증은 원래처럼
 *   대상 모듈의 보안 설정이 판단하므로(헤더·본문을 바꾸지 않고 전달) 기존 동작과 같다.
 * - 본문은 스트림 그대로 전달한다 (JSON·form·multipart·바이너리 공통, 서명 검증에 필요).
 * - 레거시 에이전트 .do 경로(/atms/agent 등)는 기존 LegacyAgentGatewayController 가 담당한다.
 */
@Slf4j
public class ModuleGatewayFilter extends OncePerRequestFilter {

    /** 대상 서버로 넘기지 않는 hop-by-hop 헤더 및 HttpClient 가 직접 설정하는 헤더. */
    private static final Set<String> SKIP_REQUEST_HEADERS = Set.of(
            "host", "connection", "content-length", "expect", "upgrade", "keep-alive",
            "transfer-encoding", "te", "trailer", "proxy-connection", "proxy-authorization",
            "http2-settings");

    private static final Set<String> SKIP_RESPONSE_HEADERS = Set.of(
            "connection", "content-length", "transfer-encoding", "keep-alive", "te", "trailer",
            "upgrade", "proxy-connection", ":status");

    private final List<ModuleGatewayProperties.Route> routes;
    private final Duration readTimeout;
    private final HttpClient client;

    public ModuleGatewayFilter(ModuleGatewayProperties props) {
        this.routes = List.copyOf(props.getRoutes());
        this.readTimeout = Duration.ofMillis(props.getReadTimeoutMs());
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        routes.forEach(r -> log.info("[module gateway] {} -> {}", r.getPrefix(), r.getTarget()));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return findRoute(pathOf(request)) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = pathOf(req);
        ModuleGatewayProperties.Route route = findRoute(path);
        if (route == null) {
            chain.doFilter(req, res);
            return;
        }

        String query = req.getQueryString();
        URI target = URI.create(stripTrailingSlash(route.getTarget()) + path + (query != null ? "?" + query : ""));

        HttpRequest.Builder builder = HttpRequest.newBuilder(target).timeout(readTimeout);
        for (String name : Collections.list(req.getHeaderNames())) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (SKIP_REQUEST_HEADERS.contains(lower) || lower.startsWith("x-forwarded-")) {
                continue;
            }
            for (String value : Collections.list(req.getHeaders(name))) {
                builder.header(name, value);
            }
        }
        builder.header("X-Forwarded-For", forwardedFor(req));
        builder.header("X-Forwarded-Proto", req.getScheme());
        if (req.getHeader("Host") != null) {
            builder.header("X-Forwarded-Host", req.getHeader("Host"));
        }
        builder.method(req.getMethod(), bodyOf(req));

        HttpResponse<InputStream> down;
        try {
            down = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[module gateway] {} {} -> {} 중단", req.getMethod(), path, target);
            badGateway(res);
            return;
        } catch (IOException e) {
            log.warn("[module gateway] {} {} -> {} 전달 실패: {}", req.getMethod(), path, target, e.toString());
            badGateway(res);
            return;
        }

        res.setStatus(down.statusCode());
        down.headers().map().forEach((name, values) -> {
            if (SKIP_RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            values.forEach(v -> res.addHeader(name, v));
        });
        try (InputStream in = down.body(); OutputStream out = res.getOutputStream()) {
            in.transferTo(out);
        }
    }

    /**
     * 대상 모듈 연결 실패 → 502 를 직접 쓴다.
     * sendError() 를 쓰면 /error 로 다시 디스패치되는데, 그 디스패치에는 인증 정보가 없어 Spring Security 가
     * 401 로 바꾼다 (로그인한 사용자도 동일). 화면(axios)은 401 이면 로그아웃시키므로,
     * 모듈 하나가 내려가 있는 것만으로 사용자가 로그인 화면으로 튕기게 된다.
     */
    private static void badGateway(HttpServletResponse res) throws IOException {
        if (res.isCommitted()) {
            return;
        }
        res.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                "{\"success\":false,\"code\":\"502\",\"message\":\"대상 모듈에 연결할 수 없습니다.\",\"data\":null}");
    }

    /** 요청 본문을 스트림 그대로 전달. 길이를 알면 Content-Length 를 유지한다. */
    private static HttpRequest.BodyPublisher bodyOf(HttpServletRequest req) {
        long length = req.getContentLengthLong();
        boolean chunked = req.getHeader("Transfer-Encoding") != null;
        if (length <= 0 && !chunked) {
            return HttpRequest.BodyPublishers.noBody();
        }
        HttpRequest.BodyPublisher stream = HttpRequest.BodyPublishers.ofInputStream(() -> {
            try {
                return req.getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        return length > 0 ? HttpRequest.BodyPublishers.fromPublisher(stream, length) : stream;
    }

    private ModuleGatewayProperties.Route findRoute(String path) {
        for (ModuleGatewayProperties.Route r : routes) {
            String prefix = stripTrailingSlash(r.getPrefix());
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return r;
            }
        }
        return null;
    }

    /** 컨텍스트 경로를 뺀 요청 경로 (atms-web 은 JEUS 에서 '/' 이므로 보통 그대로). */
    private static String pathOf(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        return (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
    }

    private static String stripTrailingSlash(String s) {
        return (s != null && s.length() > 1 && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }

    /** 기존 X-Forwarded-For 뒤에 직전 클라이언트 IP 를 덧붙인다 (원 클라이언트 IP 보존). */
    private static String forwardedFor(HttpServletRequest req) {
        String remote = req.getRemoteAddr();
        String xff = req.getHeader("X-Forwarded-For");
        return (xff == null || xff.isBlank()) ? remote : xff + ", " + remote;
    }
}
