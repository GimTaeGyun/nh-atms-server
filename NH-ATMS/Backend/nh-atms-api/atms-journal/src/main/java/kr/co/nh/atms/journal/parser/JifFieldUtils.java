package kr.co.nh.atms.journal.parser;

import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;

import java.util.Map;

/**
 * JIF head/body 에서 값 꺼내는 공용 유틸.
 * Map<String,Object> 라 잘못된 타입에도 안전하게 String 으로 변환.
 */
public final class JifFieldUtils {

    private JifFieldUtils() {}

    public static String s(Map<String, Object> m, String k) {
        if (m == null) return null;
        Object v = m.get(k);
        if (v == null) return null;
        String str = String.valueOf(v).trim();
        return str.isEmpty() ? null : str;
    }

    /** 길이 맞춰 substring (null/짧음 안전). */
    public static String sub(String s, int from, int len) {
        if (s == null) return null;
        if (from >= s.length()) return null;
        int to = Math.min(from + len, s.length());
        String r = s.substring(from, to).trim();
        return r.isEmpty() ? null : r;
    }

    /**
     * head 에서 공통 키 추출. 누락 시 IllegalArgumentException.
     * 09_ATMR 분석 문서의 헤더 spec(yyyy/mm/dd/hh/mi/ss/fff/flag, atm_id, jnl_seq, jnl_kind, bk_cd) 사용.
     */
    public static JifHeadKey parseHead(JifMessage msg) {
        Map<String, Object> h = msg.getHead();
        if (h == null) throw new IllegalArgumentException("head is null");

        String yyyy = s(h, "yyyy");
        String mm   = s(h, "mm");
        String dd   = s(h, "dd");
        String hh   = s(h, "hh");
        String mi   = s(h, "mi");
        String ss   = s(h, "ss");
        String fff  = s(h, "fff");
        String flag = s(h, "flag");
        if (yyyy == null || mm == null || dd == null || hh == null || mi == null || ss == null) {
            throw new IllegalArgumentException("head datetime fields missing");
        }
        String jnlDate = yyyy + pad2(mm) + pad2(dd);
        String jnlTime = pad2(hh) + pad2(mi) + pad2(ss)
                + pad(fff != null ? fff : "000", 3)
                + (flag != null ? flag.substring(0, 1) : "0");

        String atmId = s(h, "atm_id");
        String termGroupId = sub(atmId, 0, 5);
        String termId      = sub(atmId, 5, 3);
        if (termGroupId == null || termId == null) {
            throw new IllegalArgumentException("atm_id is invalid: " + atmId);
        }

        String jnlKind = s(h, "jnl_kind");
        if (jnlKind == null) throw new IllegalArgumentException("jnl_kind missing");

        return new JifHeadKey(
                termGroupId, termId,
                jnlDate, jnlTime,
                jnlKind,
                s(h, "jnl_seq"),
                s(h, "bk_cd"),
                atmId
        );
    }

    private static String pad2(String x) {
        if (x == null) return "00";
        return x.length() >= 2 ? x.substring(0, 2) : ("0" + x);
    }

    private static String pad(String x, int len) {
        if (x == null) return "0".repeat(len);
        if (x.length() >= len) return x.substring(0, len);
        StringBuilder b = new StringBuilder(x);
        while (b.length() < len) b.append('0');
        return b.toString();
    }
}
