package kr.co.nh.atms.ifex.gateway.eif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.regex.Pattern;

/**
 * 구 새마을금고 atme.lib (atme.util.Json) 이식. Converter/EifExternalGateway 가 사용하는
 * 최소 기능만 유지, 패키지만 변경. (atms-file remote/util/Json 과 동일 계열)
 */
public final class Json {

    private static final Gson GSON_PRETTY = create(true);
    private static final Gson GSON_COMPACT = create(false);

    private Json() {
    }

    public static Gson gson() {
        return GSON_PRETTY;
    }

    public static String toJsonText(JsonElement json) {
        return GSON_PRETTY.toJson(json);
    }

    public static String toJsonText(JsonElement json, boolean pretty) {
        return pretty ? GSON_PRETTY.toJson(json) : GSON_COMPACT.toJson(json);
    }

    public static JsonObject toJsonObject(String jsonText) {
        return JsonParser.parseString(jsonText).getAsJsonObject();
    }

    private static Gson create(boolean pretty) {
        GsonBuilder gb = new GsonBuilder().serializeNulls().disableHtmlEscaping();
        if (pretty) {
            gb.setPrettyPrinting();
        }
        return gb.create();
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
        return jele.isJsonNull() ? new JsonObject() : jele.getAsJsonObject();
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
}
