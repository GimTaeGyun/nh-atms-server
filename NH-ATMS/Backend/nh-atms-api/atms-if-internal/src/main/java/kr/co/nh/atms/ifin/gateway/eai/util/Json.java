package kr.co.nh.atms.ifin.gateway.eai.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 구 새마을금고 atmi.lib (atmi.util.Json) 원본 이식. 패키지만 변경.
 * "//./a/b/c" 형태의 경로 탐색을 지원한다.
 */
public final class Json {
    private static final Gson gson0 = create(true);
    private static final Gson gson1 = create(false);

    private Json() {
    }

    public static Gson gson() {
        return gson0;
    }

    public static Gson v(boolean pretty) {
        return pretty ? gson0 : gson1;
    }

    public static String toJsonText(JsonElement json) {
        return gson0.toJson(json);
    }

    public static String toJsonText(JsonElement json, boolean pretty) {
        return pretty ? gson0.toJson(json) : gson1.toJson(json);
    }

    public static JsonObject toJsonObject(String jsonText) {
        return JsonParser.parseString(jsonText).getAsJsonObject();
    }

    private static Gson create(boolean pretty) {
        if (pretty) {
            return new com.google.gson.GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping()
                    .create();
        }
        return new com.google.gson.GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    }

    public static JsonElement get(JsonObject obj, String path) {
        if (obj == null || !obj.isJsonObject()) {
            return JsonNull.INSTANCE;
        }
        JsonElement ele;
        if (!path.startsWith("//./")) {
            ele = obj.getAsJsonObject().get(path);
            return ele == null ? JsonNull.INSTANCE : ele;
        }
        path = path.substring(4);
        String[] keys = path.split(Pattern.quote("/"));
        ele = obj;
        for (String key : keys) {
            if (ele == null || !ele.isJsonObject()) {
                return JsonNull.INSTANCE;
            }
            ele = ele.getAsJsonObject().get(key);
        }
        return ele == null ? JsonNull.INSTANCE : ele;
    }

    public static JsonObject obj(JsonObject obj, String key) {
        JsonElement jele = get(obj, key);
        return jele == null || jele.isJsonNull() ? new JsonObject() : jele.getAsJsonObject();
    }

    public static String str(JsonObject obj, String key, String def) {
        JsonElement jele = get(obj, key);
        if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) {
            return def;
        }
        String val = jele.getAsString();
        return val == null ? def : val;
    }

    public static String str(JsonObject obj, String key) {
        return str(obj, key, "");
    }

    public static String strt(JsonObject obj, String key) {
        return str(obj, key, "").trim();
    }

    public static String strt(JsonObject obj, String key, String def) {
        return str(obj, key, def).trim();
    }

    public static boolean bool(JsonObject obj, String key, boolean def) {
        JsonElement jele = get(obj, key);
        if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) {
            return def;
        }
        return jele.getAsBoolean();
    }

    public static boolean bool(JsonObject obj, String key) {
        return bool(obj, key, false);
    }

    public static int int32(JsonObject obj, String key, int def) {
        JsonElement jele = get(obj, key);
        if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) {
            return def;
        }
        return jele.getAsInt();
    }

    public static int int32(JsonObject obj, String key) {
        return int32(obj, key, 0);
    }

    public static long int64(JsonObject obj, String key, long def) {
        JsonElement jele = get(obj, key);
        if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) {
            return def;
        }
        return jele.getAsLong();
    }

    public static long int64(JsonObject obj, String key) {
        return int64(obj, key, 0);
    }

    public static BigInteger bigint32(JsonObject obj, String key, BigInteger def) {
        JsonElement jele = get(obj, key);
        if (jele == null || jele.isJsonNull()) {
            return def;
        }
        return jele.getAsBigInteger();
    }

    public static BigDecimal bigdecimal(JsonObject obj, String key, BigDecimal def) {
        JsonElement jele = get(obj, key);
        if (jele == null || jele.isJsonNull()) {
            return def;
        }
        return jele.getAsBigDecimal();
    }
}
