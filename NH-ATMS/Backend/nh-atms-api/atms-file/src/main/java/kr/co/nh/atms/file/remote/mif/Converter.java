package kr.co.nh.atms.file.remote.mif;

import kr.co.nh.atms.file.remote.util.*;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Converter
 * 구 새마을금고 atmf.lib (atmf.server.agent.Converter) 원본 이식.
 * MIF(전문) 바이너리 <-> JSON 변환. 프로토콜 호환을 위해 로직 변경 없이 패키지/import만 변경.
 */
public class Converter {
	public static final Converter MIF = new Converter();

	private static final Logger LOGGER = LoggerFactory.getLogger(Converter.class);
	public static final String CHARSET_KEY = "#charset#";
	public static final String HEAD_KEY    = "#head#";
	public static final String BODY_KEY    = "#body#";

	public static final String DESC_KEY    = "#desc#";
	public static final String TYPE_KEY    = "#type#";
	public static final String SEP_KEY     = "#sep#";
	public static final String TLEN_KEY    = "#tlen#";

	public static final String FIELD_KEY   = "#field#";
	public static final String KEY_KEY     = "#key#";
	public static final String LEN_KEY     = "#len#";
	public static final String OFF_KEY     = "#off#";
	public static final String DT_KEY      = "#dt#";

	public static final String BODY_KEY_FIELD = "#body-key#";
	public static final String DATA_KEY_FIELD = "#data-key#";

	public static final String LINK_KEY = "#link#";

	private static final String TYPE_FIXED = "fixed";
	private static final String TYPE_DELIM = "delim";

	/**
	 * RecursiveHandler
	 */
	@FunctionalInterface
	static public interface RecursiveHandler {
		void invoke(JsonObject jtoken);
	}

	private JsonObject jspecs;

	public JsonObject specs() { return jspecs; }

	private String name;
	private JsonObject jspec;
	private Charset encoding;

	public String name() { return name; }

	public Charset charset() { return encoding; }

	public JsonObject spec() { return jspec; }

	public JsonObject head() { return Json.obj(jspec, HEAD_KEY); }

	public JsonObject body(String key) {
		JsonObject jbody = Json.obj(jspec, BODY_KEY);
		return Json.obj(jbody, key);
	}

	/**
	 * load
	 */
	public void load(String interfaceName, Path jsonInterfacePath) throws UnsupportedEncodingException, IOException {
		if (!Files.exists(jsonInterfacePath)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format("[load json spec][%s][not found][%s]", name, jsonInterfacePath));
			}
			return;
		}
		String jsonInterface = new String(Files.readAllBytes(jsonInterfacePath), "utf-8");
		load(interfaceName, jsonInterface);
	}

	/**
	 * load (클래스패스/문자열 기반 로딩 지원 - NH 추가 오버로드)
	 */
	public void load(String interfaceName, String jsonInterface) {
		this.name = interfaceName;
		this.jspecs = Json.toJsonObject(jsonInterface);
		this.encoding = Charset.forName(jspecs.get(CHARSET_KEY).getAsString());

		List<JsonElement> jfields = new ArrayList<>();
		List<JsonElement> jlinks = new ArrayList<>();
		recursive(jspecs, (jelement) -> {
			if (jelement.has(TYPE_KEY)) {
				switch (jelement.get(TYPE_KEY).getAsString()) {
					case TYPE_FIXED:
					case TYPE_DELIM:
						break;
					default:
						jelement.addProperty(TYPE_KEY, "#undefined#");
						break;
				}
			}
			if (jelement.has(FIELD_KEY) && jelement.get(FIELD_KEY).isJsonArray()) {
				jfields.add(jelement);
				return;
			}
			if (jelement.has(LINK_KEY)) {
				jlinks.add(jelement);
				return;
			}
		});
		for (JsonElement jelement : jfields) {
			JsonArray jarray = jelement.getAsJsonObject().get(FIELD_KEY).getAsJsonArray();
			int tlen = 0, ilen = 0;
			for (int i = 0; i < jarray.size(); i++) {
				JsonElement field = jarray.get(i);
				if (field.isJsonNull()) {
					jarray.remove(i);
					i--;
					continue;
				}
				JsonObject jfield_obj = field.getAsJsonObject();
				String sl = jfield_obj.has(LEN_KEY) ? jfield_obj.get(LEN_KEY).getAsString() : "0";
				try {
					ilen = Integer.parseInt(sl);
				} catch (NumberFormatException e) {
					ilen = 0;
				}
				jfield_obj.addProperty(OFF_KEY, tlen);
				jfield_obj.addProperty(LEN_KEY, ilen);
				jfield_obj.addProperty(KEY_KEY, Json.str(jfield_obj, KEY_KEY).trim());
				tlen += ilen;
			}
			jelement.getAsJsonObject().addProperty(TLEN_KEY, tlen);
		}
		for (JsonElement jelement : jlinks) {
			JsonObject jobject = jelement.getAsJsonObject();
			String[] keys = jobject.get(LINK_KEY).getAsString().split("/");
			JsonElement link = jspecs;
			for (String key : keys) {
				if (link.isJsonObject() && link.getAsJsonObject().has(key)) {
					link = link.getAsJsonObject().get(key);
				}
			}
			if (link != jspecs && link.isJsonObject()) {
				JsonObject jlink_obj = link.getAsJsonObject();
				for (String key : jlink_obj.keySet()) {
					if (jobject.has(key)) { continue; }
					jobject.add(key, jlink_obj.get(key));
				}
			}
		}
		if (jspecs.getAsJsonObject().has(name)) {
			jspec = jspecs.getAsJsonObject().get(name).getAsJsonObject();
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(Json.toJsonText(jspec));
		}
	}

	/**
	 * recursive
	 */
	void recursive(JsonElement jelement, RecursiveHandler on) {
		if (jelement.isJsonObject()) {
			JsonObject obj = jelement.getAsJsonObject();
			on.invoke(obj);
			for (String propKey : obj.keySet()) {
				recursive(obj.get(propKey), on);
			}
		} else if (jelement.isJsonArray()) {
			JsonArray array = jelement.getAsJsonArray();
			for (JsonElement item : array) {
				recursive(item, on);
			}
		}
	}

	/**
	 * toJson
	 */
	public JsonObject toJson(byte[] packet) {
		if (jspec == null) { return null; }
		JsonObject   jmeta_head =                   jspec.getAsJsonObject(HEAD_KEY);
		JsonObject[] jmeta_body = new JsonObject[] { jspec.getAsJsonObject(BODY_KEY) };
		JsonElement jtype = jmeta_head.get(TYPE_KEY);
		switch ((jtype == null ? "-" : jtype.getAsString())) {
			case TYPE_FIXED:
			case TYPE_DELIM:
				break;
			default: {
				JsonObject jdata = new JsonObject();
				jdata.addProperty("packet", new String(packet, encoding));
				return jdata;
			}
		}

		JsonObject jdata = new JsonObject();
		JsonObject jdata_head = new JsonObject();
		JsonObject jdata_body = new JsonObject();
		jdata.add("head", jdata_head);
		jdata.add("body", jdata_body);

		class PacketTokens {
			private String[] tokens;
			private int index = 0;

			public boolean has() {
				if (tokens == null) return false;
				return index < tokens.length;
			}

			public String next() {
				if (!has()) return null;
				return tokens[index++];
			}

			public void split(String sep) {
				if (tokens != null) return;
				tokens = new String(packet, encoding).split(Pattern.quote(sep));
				index++;
			}
		}
		final PacketTokens packet_tokens = new PacketTokens();

		class Convert {
			boolean fix;
			String sep;
			boolean head;

			Convert(JsonElement jmeta, JsonObject jdata, int offset, boolean head) {
				if (jmeta == null) return;
				this.head = head;
				JsonElement jtype = jmeta.getAsJsonObject().get(TYPE_KEY);
				switch (jtype == null ? "-" : jtype.getAsString()) {
					case TYPE_FIXED:
						fix = true;
						sep = null;
						break;
					case TYPE_DELIM:
						fix = false;
						sep = jmeta.getAsJsonObject().has(SEP_KEY)
								? jmeta.getAsJsonObject().get(SEP_KEY).getAsString()
								: "|";
						packet_tokens.split(sep);
						break;
					default:
						return;
				}
				convert(jmeta, jdata, offset);
			}

			void convert(JsonElement jmeta, JsonObject jdata, int offset) {
				if (jmeta == null) { return; }
				JsonElement jele = jmeta.getAsJsonObject().get(FIELD_KEY);
				if (jele == null) { return; }
				JsonArray fields = jele.getAsJsonArray();
				for (JsonElement field : fields) {
					JsonObject jfield = field.getAsJsonObject();
					String key = jfield.has(KEY_KEY) ? jfield.get(KEY_KEY).getAsString().trim() : "";
					int    len = jfield.has(LEN_KEY) ? jfield.get(LEN_KEY).getAsInt() : 0;
					int    off = jfield.has(OFF_KEY) ? jfield.get(OFF_KEY).getAsInt() : 0;
					if (key.isEmpty()) { continue; }
					if (fix && len <= 0) { continue; }

					String val = null;
					if (fix) {
						if (offset + off + len <= packet.length) {
							int pos = 0;
							while (pos < len) {
								if (packet[offset + off + pos] == 0x00) {
									len = pos;
									break;
								}
								pos++;
							}
							val = new String(packet, offset + off, len, encoding);
						}
					} else if (packet_tokens.has()) {
						val = packet_tokens.next();
					}

					if (jfield.has(FIELD_KEY) && jfield.get(FIELD_KEY).isJsonArray()) {
						int cnt = Integer.parseInt(val == null ? "0" : val.trim());
						JsonArray jardata = new JsonArray();
						jdata.add(key + "_count_verification", new JsonPrimitive(cnt));
						jdata.add(key, jardata);
						for (int i = 0; i < cnt; i++) {
							JsonObject jobj = new JsonObject();
							jardata.add(jobj);
							convert(jfield, jobj, offset);
						}
					} else {
						jdata.addProperty(key, val == null ? "" : val);
						if (head) {
							if (
								jfield.has(BODY_KEY_FIELD) && jfield.get(BODY_KEY_FIELD).getAsBoolean()
								||
								jfield.has(DATA_KEY_FIELD) && jfield.get(DATA_KEY_FIELD).getAsBoolean()
							) {
								if (jmeta_body[0] != null) {
									jmeta_body[0] = jmeta_body[0].getAsJsonObject(val);
								}
							}
						}
					}
				}
			}
		}
		new Convert(jmeta_head, jdata_head, 0, true);
		new Convert(jmeta_body[0], jdata_body, jmeta_head.has(TLEN_KEY) ? jmeta_head.get(TLEN_KEY).getAsInt() : 0, false);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format(""
					+ "[to_json]\n"
					+ "[packet]\n"
					+ "{%s}\n"
					+ "[json]\n"
					+ "%s"
				, new String(packet, encoding).replaceAll("[ \t\r\n]", ".")
				, Json.toJsonText(jdata)
			));
		}
		return jdata;
	}

	/**
	 * toPacket
	 */
	public byte[] toPacket(JsonObject json) {
		if (jspec == null) { return null; }
		JsonObject   jdata = json;
		JsonObject   jmeta_head =                   jspec.getAsJsonObject(HEAD_KEY);
		JsonObject[] jmeta_body = new JsonObject[] { jspec.getAsJsonObject(BODY_KEY) };
		JsonObject   jdata_head = jdata.getAsJsonObject("head");
		JsonObject   jdata_body = jdata.getAsJsonObject("body");
		switch (jmeta_head.get(TYPE_KEY).getAsString()) {
			case TYPE_FIXED:
			case TYPE_DELIM:
				break;
			default:
				return Json.toJsonText(json).getBytes(encoding);
		}

		class Convert {
			boolean fix;
			String ssep;
			byte[] sep;
			boolean head;

			Convert(JsonElement jmeta, JsonObject jdata, ByteArrayOutputStream ms, boolean head) {
				if (jmeta == null) return;
				this.head = head;
				JsonElement jtype = jmeta.getAsJsonObject().get(TYPE_KEY);
				switch (jtype == null ? "-" : jtype.getAsString()) {
					case TYPE_FIXED:
						fix = true;
						sep = null;
						break;
					case TYPE_DELIM:
						fix = false;
						ssep = jmeta.getAsJsonObject().has(SEP_KEY)
								? jmeta.getAsJsonObject().get(SEP_KEY).getAsString()
								: "|";
						sep = ssep.getBytes(encoding);
						break;
					default:
						return;
				}
				if (!fix && sep != null) {
					ms.write(sep, 0, sep.length);
				}
				convert(jmeta, jdata, ms);
			}

			void convert(JsonElement jmeta, JsonObject jdata, ByteArrayOutputStream ms) {
				if (jmeta == null) { return; }
				JsonElement jele = jmeta.getAsJsonObject().get(FIELD_KEY);
				if (jele == null) { return; }
				JsonArray fields = jele.getAsJsonArray();
				for (JsonElement field : fields) {
					JsonObject jfield = field.getAsJsonObject();
					String key = jfield.has(KEY_KEY) ? jfield.get(KEY_KEY).getAsString().trim() : "";
					int    len = jfield.has(LEN_KEY) ? jfield.get(LEN_KEY).getAsInt() : 0;
					int    dtp = jfield.has(DT_KEY) ? jfield.get(DT_KEY).getAsInt() : 0;
					if (key.isEmpty()) { continue; }
					if (fix && len <= 0) { continue; }

					JsonElement jval = jdata != null && jdata.has(key) ? jdata.get(key) : null;
					String val;

					if (jval != null && jval.isJsonArray()) {
						JsonArray jvalArray = jval.getAsJsonArray();
						if (jfield.has(FIELD_KEY)) {
							val = String.valueOf(jvalArray.size());
							writeValue(ms, val, len, dtp);
							for (JsonElement jitem : jvalArray) {
								convert(jfield, jitem.getAsJsonObject(), ms);
							}
							continue;
						}
						val = Json.toJsonText(jvalArray, false);
					} else {
						val = jval != null ? jval.getAsString() : "";
						if (head) {
							if (
								jfield.has(BODY_KEY_FIELD) && jfield.get(BODY_KEY_FIELD).getAsBoolean()
								||
								jfield.has(DATA_KEY_FIELD) && jfield.get(DATA_KEY_FIELD).getAsBoolean()
							) {
								if (jmeta_body[0] != null) {
									jmeta_body[0] = jmeta_body[0].getAsJsonObject(val);
								}
							}
						}
					}
					writeValue(ms, val, len, dtp);
				}
			}

			private void writeValue(ByteArrayOutputStream ms, String val, int len, int dtp) {
				byte[] bin;
				boolean delimiter = !fix && sep != null;
				if (delimiter) {
					val = val.replaceAll(Pattern.quote(ssep), ".");
				}
				if (dtp == 1) {
					if (delimiter && len <= 0) len = 1;
					bin = lpad(val, len, '0', encoding);
					ms.write(bin, 0, fix ? len : bin.length);
				} else if (fix || len > 0) {
					bin = rpad(val, len, ' ', encoding);
					ms.write(bin, 0, len);
				} else {
					bin = val.getBytes(encoding);
					ms.write(bin, 0, bin.length);
				}
				if (delimiter) {
					ms.write(sep, 0, sep.length);
				}
			}
		}

		try (ByteArrayOutputStream ms = new ByteArrayOutputStream()) {
			switch (name) {
				case "#iif#": {
					break;
				}
				case "#eif#": {
					jdata_head.addProperty("len", "0486");
					break;
				}
				case "#mif#": {
					break;
				}
				case "#jif#": {
					break;
				}
			}

			Convert chead = new Convert(jmeta_head, jdata_head, ms, true);
			Convert cbody = new Convert(jmeta_body[0], jdata_body, ms, false);

			switch (name) {
				case "#iif#": {
					ms.write('Z');
					ms.write('Z');
					break;
				}
				case "#eif#": {
					break;
				}
				case "#mif#": {
					JsonElement jauid = jdata_body.get("##async_uid##");
					if (jauid != null) {
						cbody.writeValue(ms, jauid.getAsString(), 10, 0);
					}
					break;
				}
				case "#jif#": {
					break;
				}
			}

			boolean     field_fix = TYPE_FIXED.equals(jmeta_head.get(TYPE_KEY).getAsString());
			JsonElement field_len = jmeta_head.getAsJsonArray(FIELD_KEY).size() > 0 ? jmeta_head.getAsJsonArray(FIELD_KEY).get(0) : null;
			byte[]      field_sep = (jmeta_head.has(SEP_KEY) ? jmeta_head.get(SEP_KEY).getAsString() : "|").getBytes(encoding);

			byte[] packet = ms.toByteArray();
			if (field_len != null) {
				int len = field_len.getAsJsonObject().has(LEN_KEY) ? field_len.getAsJsonObject().get(LEN_KEY).getAsInt() : 0;
				int dtp = field_len.getAsJsonObject().has(DT_KEY) ? field_len.getAsJsonObject().get(DT_KEY).getAsInt() : 0;
				if (len > 0) {
					ByteBuffer bb = ByteBuffer.wrap(packet);
					bb.position(field_fix ? 0 : field_sep.length);
					String val = String.valueOf(packet.length - len);
					if (dtp == 1) {
						bb.put(lpad(val, len, '0', encoding));
					} else {
						bb.put(rpad(val, len, ' ', encoding));
					}
				}
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format(""
						+ "[to_packet]\n"
						+ "[json]\n"
						+ "%s\n"
						+ "[packet]\n"
						+ "{%s}"
					, Json.toJsonText(jdata)
					, new String(packet, encoding).replaceAll("[ \t\r\n]", ".")
				));
			}
			return packet;
		} catch (IOException e) {
			LOGGER.error("[runtime exception]", e);
			return null;
		}
	}

	/**
	 * lpad
	 */
	public static byte[] lpad(String input, int length, char c, Charset cs) {
		return pad(input, length, c, true, cs);
	}

	/**
	 * rpad
	 */
	public static byte[] rpad(String input, int length, char c, Charset cs) {
		return pad(input, length, c, false, cs);
	}

	/**
	 * pad
	 */
	public static byte[] pad(String input, int length, char c, boolean left, Charset cs) {
		byte[] bin = input.getBytes(cs);
		byte[] rst = new byte[length];
		int copy = Math.min(bin.length, length);
		if (left) {
			Arrays.fill(rst, (byte) c);
			System.arraycopy(bin, 0, rst, length - copy, copy);
		} else {
			Arrays.fill(rst, (byte) c);
			System.arraycopy(bin, 0, rst, 0, copy);
		}
		return rst;
	}
}
