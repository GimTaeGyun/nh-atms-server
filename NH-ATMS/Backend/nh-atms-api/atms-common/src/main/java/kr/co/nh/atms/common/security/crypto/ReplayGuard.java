package kr.co.nh.atms.common.security.crypto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 재전송 방지: 같은 nonce 를 유효시간(maxSkew) 안에 두 번 받으면 거부한다.
 * 서버(JVM)별 메모리 — 비밀번호 요청은 atms-web(atms01) 하나로만 들어오므로 1대 기준으로 충분.
 */
public class ReplayGuard {

    private final Map<String, Long> seen = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public ReplayGuard(long ttlSeconds) {
        this.ttlMillis = Math.max(1, ttlSeconds) * 2000L; // ts 허용 범위가 ±maxSkew 이므로 2배 보관
    }

    /** 처음 보는 nonce 면 true (기록), 이미 본 값이면 false. */
    public boolean firstUse(String nonce, long nowMillis) {
        if (seen.size() > 10_000) {
            seen.entrySet().removeIf(e -> e.getValue() < nowMillis);
        }
        Long prev = seen.putIfAbsent(nonce, nowMillis + ttlMillis);
        if (prev == null) return true;
        if (prev < nowMillis) {           // 만료된 기록이면 새로 사용 가능
            seen.put(nonce, nowMillis + ttlMillis);
            return true;
        }
        return false;
    }
}
