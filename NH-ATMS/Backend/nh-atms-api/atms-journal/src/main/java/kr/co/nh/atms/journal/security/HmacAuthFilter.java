package kr.co.nh.atms.journal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * /api/journal/receive(/batch) 에 대한 HMAC-SHA256 인증.
 *
 * Agent → ATMS 요청 헤더:
 *   X-Atms-KeyId      : 키 식별자
 *   X-Atms-Timestamp  : 발신 시각 (epoch seconds)
 *   X-Atms-Signature  : hex(HMAC_SHA256(secret, timestamp + "." + rawBody))
 *
 * 검증:
 *   1) timestamp skew <= clockSkewSeconds      (재전송 방지)
 *   2) keyId 일치
 *   3) constant-time 시그니처 비교
 *
 * 실패: 401. enabled=false 면 무조건 통과 (개발/시뮬레이션 편의).
 */
@Slf4j
@RequiredArgsConstructor
public class HmacAuthFilter extends OncePerRequestFilter {

    private static final String H_KEY_ID    = "X-Atms-KeyId";
    private static final String H_TIMESTAMP = "X-Atms-Timestamp";
    private static final String H_SIGNATURE = "X-Atms-Signature";
    private static final String HMAC_ALGO   = "HmacSHA256";

    private final JournalHmacProperties props;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /api/journal/** 만 적용. actuator 등은 패스.
        // 컨텍스트 경로를 뺀 경로로 판정: JEUS 에서는 '/journal' 컨텍스트에 배포되어
        // getRequestURI() 가 '/journal/api/journal/...' 이 되므로 그대로 비교하면 HMAC 검증이 빠진다.
        return !pathWithinApplication(request).startsWith("/api/journal");
    }

    static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        return (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // body 캐싱은 항상 수행 (HMAC off 여도 컨트롤러가 정상 동작하도록 그대로 둠)
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, body);

        if (!props.isEnabled()) {
            chain.doFilter(wrapped, response);
            return;
        }

        try {
            verify(wrapped, body);
        } catch (HmacAuthException e) {
            log.warn("HMAC 인증 실패: {} (path={}, remote={})",
                    e.getMessage(), request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"code\":\"AUTH_HMAC\",\"message\":\"" + e.getMessage() + "\"}"
            );
            return;
        }

        chain.doFilter(wrapped, response);
    }

    private void verify(HttpServletRequest req, byte[] body) {
        String keyId = req.getHeader(H_KEY_ID);
        String tsStr = req.getHeader(H_TIMESTAMP);
        String sig   = req.getHeader(H_SIGNATURE);

        if (keyId == null || tsStr == null || sig == null) {
            throw new HmacAuthException("필수 헤더 누락 (KeyId/Timestamp/Signature)");
        }
        if (!props.getKeyId().equals(keyId)) {
            throw new HmacAuthException("알 수 없는 KeyId");
        }

        long ts;
        try {
            ts = Long.parseLong(tsStr);
        } catch (NumberFormatException e) {
            throw new HmacAuthException("Timestamp 형식 오류");
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - ts) > props.getClockSkewSeconds()) {
            throw new HmacAuthException("Timestamp skew 초과");
        }

        if (props.getSecret() == null || props.getSecret().isBlank()) {
            throw new HmacAuthException("서버 secret 미설정");
        }

        String expected = computeHmac(props.getSecret(), tsStr, body);
        if (!constantTimeEquals(expected, sig)) {
            throw new HmacAuthException("Signature 불일치");
        }
    }

    private static String computeHmac(String secret, String tsStr, byte[] body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            mac.update(tsStr.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            mac.update(body);
            return HexFormat.of().formatHex(mac.doFinal());
        } catch (Exception e) {
            throw new HmacAuthException("HMAC 계산 실패: " + e.getMessage());
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aa = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (aa.length != bb.length) return false;
        return MessageDigest.isEqual(aa, bb);
    }

    private static class HmacAuthException extends RuntimeException {
        HmacAuthException(String msg) { super(msg); }
    }
}
