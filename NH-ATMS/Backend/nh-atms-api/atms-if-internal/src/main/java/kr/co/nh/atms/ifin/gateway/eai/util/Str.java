package kr.co.nh.atms.ifin.gateway.eai.util;

/**
 * 구 새마을금고 atmi.lib (atmi.util.Str) 원본 이식. 패키지만 변경.
 */
public final class Str {

    private Str() {
    }

    public static String nvl(String v) {
        return v == null ? "" : v;
    }

    public static String nvlt(String v) {
        return v == null ? "" : v.trim();
    }

    public static String nvl(String v, String def) {
        return v == null ? def : v;
    }

    public static String nvlt(String v, String def) {
        if (v == null) {
            return def;
        }
        v = v.trim();
        return v.length() == 0 ? def : v;
    }
}
