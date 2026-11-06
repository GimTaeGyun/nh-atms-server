package kr.co.nh.atms.file.remote.util;

import java.math.*;
import java.util.regex.*;
import com.google.gson.*;

/**
 * 구 새마을금고 atmf.lib (atmf.util.Json) 원본 이식. 패키지만 변경.
 */
final public class Json {
	private static Gson gson0 = create(true);
	private static Gson gson1 = create(false);

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
		JsonElement ele = null;
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
		return ele;
	}

	public static JsonObject obj(JsonObject obj, String key) {
		JsonElement jele = get(obj, key);
		return jele.isJsonNull() ? new JsonObject() : jele.getAsJsonObject();
	}

	public static String str(JsonObject obj, String key, String def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
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
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
		return jele.getAsBoolean();
	}

	public static boolean bool(JsonObject obj, String key) {
		return bool(obj, key, false);
	}

	public static byte int8(JsonObject obj, String key, byte def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
		return jele.getAsByte();
	}

	public static byte int8(JsonObject obj, String key) {
		return int8(obj, key, (byte) 0);
	}

	public static short int16(JsonObject obj, String key, byte def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
		return jele.getAsShort();
	}

	public static short int16(JsonObject obj, String key) {
		return int16(obj, key, (byte) 0);
	}

	public static int int32(JsonObject obj, String key, int def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
		return jele.getAsInt();
	}

	public static int int32(JsonObject obj, String key) {
		return int32(obj, key, 0);
	}

	public static long int64(JsonObject obj, String key, long def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
		return jele.getAsLong();
	}

	public static long int64(JsonObject obj, String key) {
		return int64(obj, key, 0);
	}

	public static float float32(JsonObject obj, String key, float def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
		return jele.getAsFloat();
	}

	public static float float32(JsonObject obj, String key) {
		return float32(obj, key, 0);
	}

	public static double float64(JsonObject obj, String key, double def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
		return jele.getAsDouble();
	}

	public static double float64(JsonObject obj, String key) {
		return float64(obj, key, 0);
	}

	public static Number number(JsonObject obj, String key, Number def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull() || !jele.isJsonPrimitive()) { return def; }
		return jele.getAsNumber();
	}

	public static Number number(JsonObject obj, String key) {
		return number(obj, key, 0);
	}

	public static BigInteger bigint32(JsonObject obj, String key, BigInteger def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull()) { return def; }
		return jele.getAsBigInteger();
	}

	public static BigInteger bigint32(JsonObject obj, String key) {
		return bigint32(obj, key, BigInteger.ZERO);
	}

	public static BigDecimal bigdecimal(JsonObject obj, String key, BigDecimal def) {
		JsonElement jele = get(obj, key);
		if (jele == null || jele.isJsonNull()) { return def; }
		return jele.getAsBigDecimal();
	}

	public static BigDecimal bigdecimal(JsonObject obj, String key) {
		return bigdecimal(obj, key, BigDecimal.ZERO);
	}

	public static String toJavaScriptLiteral(String input) {
		if (input == null) return "";
		StringBuilder sb = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
				case '\"': sb.append("\\\""); break;
				case '\'': sb.append("\\'"); break;
				case '\\': sb.append("\\\\"); break;
				case '\b': sb.append("\\b"); break;
				case '\f': sb.append("\\f"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				default:
					if (c < 32) {
						sb.append("\\u").append(String.format("%04x", (int) c));
					} else {
						sb.append(c);
					}
					break;
			}
		}
		return sb.toString();
	}
}
