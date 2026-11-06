package kr.co.nh.atms.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 연계전문용 GlobalID(32자) 생성기.
 * 레이아웃: 기관코드(3) + yyyyMMddHHmmss(14) + nanoSuffix(6) + 일련(4) + filler.
 * 레거시 atmi/atme 의 GlobalID 채번 규약을 단순화/표준화한 버전.
 */
public final class GlobalIdGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private GlobalIdGenerator() {
    }

    /**
     * @param orgCode 기관코드(3자, 예: NH "045"). null/짧으면 0 패딩.
     * @return 32자 글로벌ID
     */
    public static String generate(String orgCode) {
        String org = leftPad(orgCode == null ? "" : orgCode, 3, '0');
        String ts = LocalDateTime.now().format(TS);                  // 14
        String nano = String.format("%06d", System.nanoTime() % 1_000_000); // 6
        String seq = String.format("%04d", SEQ.incrementAndGet() % 10_000); // 4
        String base = org + ts + nano + seq;                          // 3+14+6+4 = 27
        return rightPad(base, 32, '0');
    }

    private static String leftPad(String s, int len, char c) {
        if (s.length() >= len) return s.substring(0, len);
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < len; i++) sb.append(c);
        return sb.append(s).toString();
    }

    private static String rightPad(String s, int len, char c) {
        if (s.length() >= len) return s.substring(0, len);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(c);
        return sb.toString();
    }
}
