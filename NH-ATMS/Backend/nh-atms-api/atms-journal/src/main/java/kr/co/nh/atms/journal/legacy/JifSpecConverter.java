package kr.co.nh.atms.journal.legacy;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JIF(전자저널) 고정길이/구분자 전문 ↔ JSON 변환기.
 *
 * AS-IS atmr/web/Converter.java 의 순수 포팅(3.Migration Java 21 검증본 기준).
 * 스펙 정의는 classpath:jif/interface.journal.json (AS-IS WEB-INF/config 원본 그대로,
 * 주석 포함 → lenient 파싱). agent(.NET)가 보내는 idx 바이너리와 바이트 호환 — 파싱 로직 변경 금지.
 */
@Slf4j
public class JifSpecConverter {

    public static final String CHARSET_KEY = "#charset#";
    public static final String HEAD_KEY = "#head#";
    public static final String BODY_KEY = "#body#";
    public static final String TYPE_KEY = "#type#";
    public static final String SEP_KEY = "#sep#";
    public static final String TLEN_KEY = "#tlen#";
    public static final String FIELD_KEY = "#field#";
    public static final String KEY_KEY = "#key#";
    public static final String LEN_KEY = "#len#";
    public static final String OFF_KEY = "#off#";
    public static final String DT_KEY = "#dt#";
    public static final String BODY_KEY_FIELD = "#body-key#";
    public static final String DATA_KEY_FIELD = "#data-key#";
    public static final String LINK_KEY = "#link#";

    private static final String TYPE_FIXED = "fixed";
    private static final String TYPE_DELIM = "delim";

    private JsonObject jspecs;
    private String name;
    private JsonObject jspec;
    private Charset encoding;

    public Charset charset() { return encoding; }

    /** classpath 리소스에서 스펙 로드 (AS-IS load(name, path) 의 classpath 버전) */
    public void load(String interfaceName, String classpathResource) throws IOException {
        String jsonInterface;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IOException("jif spec not found on classpath: " + classpathResource);
            }
            jsonInterface = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        this.name = interfaceName;
        this.jspecs = parseLenient(jsonInterface);
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
            }
        });
        for (JsonElement jelement : jfields) {
            JsonArray jarray = jelement.getAsJsonObject().get(FIELD_KEY).getAsJsonArray();
            int tlen = 0, ilen;
            for (int i = 0; i < jarray.size(); i++) {
                JsonElement field = jarray.get(i);
                if (field.isJsonNull()) {
                    jarray.remove(i);
                    i--;
                    continue;
                }
                JsonObject jfieldObj = field.getAsJsonObject();
                String sl = jfieldObj.has(LEN_KEY) ? jfieldObj.get(LEN_KEY).getAsString() : "0";
                try {
                    ilen = Integer.parseInt(sl);
                } catch (NumberFormatException e) {
                    ilen = 0;
                }
                jfieldObj.addProperty(OFF_KEY, tlen);
                jfieldObj.addProperty(LEN_KEY, ilen);
                jfieldObj.addProperty(KEY_KEY, jfieldObj.get(KEY_KEY).getAsString().trim());
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
                JsonObject jlinkObj = link.getAsJsonObject();
                for (String key : jlinkObj.keySet()) {
                    if (jobject.has(key)) { continue; }
                    jobject.add(key, jlinkObj.get(key));
                }
            }
        }
        if (jspecs.has(name)) {
            jspec = jspecs.get(name).getAsJsonObject();
        }
        log.info("JIF spec loaded: {} (charset={})", classpathResource, encoding);
    }

    @FunctionalInterface
    interface RecursiveHandler { void invoke(JsonObject jtoken); }

    void recursive(JsonElement jelement, RecursiveHandler on) {
        if (jelement.isJsonObject()) {
            JsonObject obj = jelement.getAsJsonObject();
            on.invoke(obj);
            for (String propKey : obj.keySet()) {
                recursive(obj.get(propKey), on);
            }
        } else if (jelement.isJsonArray()) {
            for (JsonElement item : jelement.getAsJsonArray()) {
                recursive(item, on);
            }
        }
    }

    /** 전문 바이트 → {head, body} JSON (AS-IS toJson 무변경 포팅) */
    public JsonObject toJson(byte[] packet) {
        if (jspec == null) { return null; }
        JsonObject jmetaHead = jspec.getAsJsonObject(HEAD_KEY);
        JsonObject[] jmetaBody = new JsonObject[]{jspec.getAsJsonObject(BODY_KEY)};
        JsonElement jtype = jmetaHead.get(TYPE_KEY);
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
        JsonObject jdataHead = new JsonObject();
        JsonObject jdataBody = new JsonObject();
        jdata.add("head", jdataHead);
        jdata.add("body", jdataBody);

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
        final PacketTokens packetTokens = new PacketTokens();

        class ParseConvert {
            boolean fix;
            String sep;
            boolean head;

            ParseConvert(JsonElement jmeta, JsonObject jdataPart, int offset, boolean head) {
                if (jmeta == null) return;
                this.head = head;
                JsonElement t = jmeta.getAsJsonObject().get(TYPE_KEY);
                switch (t == null ? "-" : t.getAsString()) {
                    case TYPE_FIXED:
                        fix = true;
                        sep = null;
                        break;
                    case TYPE_DELIM:
                        fix = false;
                        sep = jmeta.getAsJsonObject().has(SEP_KEY)
                                ? jmeta.getAsJsonObject().get(SEP_KEY).getAsString()
                                : "|";
                        packetTokens.split(sep);
                        break;
                    default:
                        return;
                }
                convert(jmeta, jdataPart, offset);
            }

            void convert(JsonElement jmeta, JsonObject jdataPart, int offset) {
                if (jmeta == null) { return; }
                JsonElement jele = jmeta.getAsJsonObject().get(FIELD_KEY);
                if (jele == null) { return; }
                JsonArray fields = jele.getAsJsonArray();
                for (JsonElement field : fields) {
                    JsonObject jfield = field.getAsJsonObject();
                    String key = jfield.has(KEY_KEY) ? jfield.get(KEY_KEY).getAsString().trim() : "";
                    int len = jfield.has(LEN_KEY) ? jfield.get(LEN_KEY).getAsInt() : 0;
                    int off = jfield.has(OFF_KEY) ? jfield.get(OFF_KEY).getAsInt() : 0;
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
                    } else if (packetTokens.has()) {
                        val = packetTokens.next();
                    }

                    if (jfield.has(FIELD_KEY) && jfield.get(FIELD_KEY).isJsonArray()) {
                        int cnt = Integer.parseInt(val == null ? "0" : val.trim());
                        JsonArray jardata = new JsonArray();
                        jdataPart.add(key + "_count_verification", new JsonPrimitive(cnt));
                        jdataPart.add(key, jardata);
                        for (int i = 0; i < cnt; i++) {
                            JsonObject jobj = new JsonObject();
                            jardata.add(jobj);
                            convert(jfield, jobj, offset);
                        }
                    } else {
                        jdataPart.addProperty(key, val == null ? "" : val);
                        if (head) {
                            if (jfield.has(BODY_KEY_FIELD) && jfield.get(BODY_KEY_FIELD).getAsBoolean()
                                    || jfield.has(DATA_KEY_FIELD) && jfield.get(DATA_KEY_FIELD).getAsBoolean()) {
                                if (jmetaBody[0] != null) {
                                    jmetaBody[0] = jmetaBody[0].getAsJsonObject(val);
                                }
                            }
                        }
                    }
                }
            }
        }
        new ParseConvert(jmetaHead, jdataHead, 0, true);
        new ParseConvert(jmetaBody[0], jdataBody, jmetaHead.has(TLEN_KEY) ? jmetaHead.get(TLEN_KEY).getAsInt() : 0, false);
        return jdata;
    }

    /** {head, body} JSON → 전문 바이트 (AS-IS toPacket 무변경 포팅 — 저널 생성/테스트용) */
    public byte[] toPacket(JsonObject json) {
        if (jspec == null) { return null; }
        JsonObject jmetaHead = jspec.getAsJsonObject(HEAD_KEY);
        JsonObject[] jmetaBody = new JsonObject[]{jspec.getAsJsonObject(BODY_KEY)};
        JsonObject jdataHead = json.getAsJsonObject("head");
        JsonObject jdataBody = json.getAsJsonObject("body");
        switch (jmetaHead.get(TYPE_KEY).getAsString()) {
            case TYPE_FIXED:
            case TYPE_DELIM:
                break;
            default:
                return json.toString().getBytes(encoding);
        }

        class BuildConvert {
            boolean fix;
            String ssep;
            byte[] sep;
            boolean head;

            BuildConvert(JsonElement jmeta, JsonObject jdataPart, ByteArrayOutputStream ms, boolean head) {
                if (jmeta == null) return;
                this.head = head;
                JsonElement t = jmeta.getAsJsonObject().get(TYPE_KEY);
                switch (t == null ? "-" : t.getAsString()) {
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
                convert(jmeta, jdataPart, ms);
            }

            void convert(JsonElement jmeta, JsonObject jdataPart, ByteArrayOutputStream ms) {
                if (jmeta == null) { return; }
                JsonElement jele = jmeta.getAsJsonObject().get(FIELD_KEY);
                if (jele == null) { return; }
                JsonArray fields = jele.getAsJsonArray();
                for (JsonElement field : fields) {
                    JsonObject jfield = field.getAsJsonObject();
                    String key = jfield.has(KEY_KEY) ? jfield.get(KEY_KEY).getAsString().trim() : "";
                    int len = jfield.has(LEN_KEY) ? jfield.get(LEN_KEY).getAsInt() : 0;
                    int dtp = jfield.has(DT_KEY) ? jfield.get(DT_KEY).getAsInt() : 0;
                    if (key.isEmpty()) { continue; }
                    if (fix && len <= 0) { continue; }

                    JsonElement jval = jdataPart != null && jdataPart.has(key) ? jdataPart.get(key) : null;
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
                        val = jvalArray.toString();
                    } else {
                        val = jval != null ? jval.getAsString() : "";
                        if (head) {
                            if (jfield.has(BODY_KEY_FIELD) && jfield.get(BODY_KEY_FIELD).getAsBoolean()
                                    || jfield.has(DATA_KEY_FIELD) && jfield.get(DATA_KEY_FIELD).getAsBoolean()) {
                                if (jmetaBody[0] != null) {
                                    jmetaBody[0] = jmetaBody[0].getAsJsonObject(val);
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
            BuildConvert chead = new BuildConvert(jmetaHead, jdataHead, ms, true);
            BuildConvert cbody = new BuildConvert(jmetaBody[0], jdataBody, ms, false);

            boolean fieldFix = TYPE_FIXED.equals(jmetaHead.get(TYPE_KEY).getAsString());
            JsonElement fieldLen = jmetaHead.getAsJsonArray(FIELD_KEY).size() > 0 ? jmetaHead.getAsJsonArray(FIELD_KEY).get(0) : null;
            byte[] fieldSep = (jmetaHead.has(SEP_KEY) ? jmetaHead.get(SEP_KEY).getAsString() : "|").getBytes(encoding);

            byte[] packet = ms.toByteArray();
            if (fieldLen != null) {
                int len = fieldLen.getAsJsonObject().has(LEN_KEY) ? fieldLen.getAsJsonObject().get(LEN_KEY).getAsInt() : 0;
                int dtp = fieldLen.getAsJsonObject().has(DT_KEY) ? fieldLen.getAsJsonObject().get(DT_KEY).getAsInt() : 0;
                if (len > 0) {
                    ByteBuffer bb = ByteBuffer.wrap(packet);
                    bb.position(fieldFix ? 0 : fieldSep.length);
                    String val = String.valueOf(packet.length - len);
                    if (dtp == 1) {
                        bb.put(lpad(val, len, '0', encoding));
                    } else {
                        bb.put(rpad(val, len, ' ', encoding));
                    }
                }
            }
            return packet;
        } catch (IOException e) {
            log.error("[runtime exception]", e);
            return null;
        }
    }

    /** 주석 포함 JSON(스펙 파일) lenient 파싱 */
    static JsonObject parseLenient(String json) {
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(true);
        return JsonParser.parseReader(reader).getAsJsonObject();
    }

    public static byte[] lpad(String input, int length, char c, Charset cs) {
        return pad(input, length, c, true, cs);
    }

    public static byte[] rpad(String input, int length, char c, Charset cs) {
        return pad(input, length, c, false, cs);
    }

    public static byte[] pad(String input, int length, char c, boolean left, Charset cs) {
        byte[] bin = input.getBytes(cs);
        byte[] rst = new byte[length];
        int copy = Math.min(bin.length, length);
        Arrays.fill(rst, (byte) c);
        if (left) {
            System.arraycopy(bin, 0, rst, length - copy, copy);
        } else {
            System.arraycopy(bin, 0, rst, 0, copy);
        }
        return rst;
    }
}
