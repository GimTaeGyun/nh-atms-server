package kr.co.nh.atms.journal.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.parser.JifFieldUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * agent(.NET) 바이너리 저널 수신 체인 검증 (DB/HTTP 불요):
 * JSON → toPacket(idx 바이트) → Mask(agent 송신) → unmask(서버 수신) → toJson → JifMessage → parseHead
 */
class LegacyJournalConvertTest {

    static JifSpecConverter jif;
    static final ObjectMapper om = new ObjectMapper();

    @BeforeAll
    static void setup() throws Exception {
        jif = new JifSpecConverter();
        jif.load("#jif#", "jif/interface.journal.json");
    }

    /** agent Ag2AtmsJounalTransferWorker.Mask 와 동일 (체인 XOR — 직전 원문 바이트) */
    static byte[] mask(byte[] data, byte key) {
        byte m = key;
        byte original;
        for (int i = 0; i < data.length; i++) {
            original = data[i];
            data[i] ^= m;
            m = original;
        }
        return data;
    }

    static JsonObject kind11Journal() {
        JsonObject head = new JsonObject();
        head.addProperty("len", "");             // toPacket 이 재계산
        head.addProperty("yyyy", "2026");
        head.addProperty("mm", "07");
        head.addProperty("dd", "10");
        head.addProperty("hh", "09");
        head.addProperty("mi", "30");
        head.addProperty("ss", "15");
        head.addProperty("fff", "123");
        head.addProperty("flag", "0");
        head.addProperty("bk_cd", "011");
        head.addProperty("atm_id", "03704401            ");
        head.addProperty("jnl_seq", "00001");
        head.addProperty("jnl_kind", "11");
        head.addProperty("idx_enc", "0");
        head.addProperty("jnl_enc", "0");
        head.addProperty("enc_key_ver", "0");
        head.addProperty("filler", "     ");

        JsonObject body = new JsonObject();
        body.addProperty("mm", "0007");
        body.addProperty("dd", "10");
        body.addProperty("yyyy", "26");
        body.addProperty("hh", "09");
        body.addProperty("mi", "30");
        body.addProperty("ss", "00");
        JsonArray devices = new JsonArray();
        JsonObject dev = new JsonObject();
        dev.addProperty("id", "1");
        dev.addProperty("ver1", "AP-1.0.0");
        dev.addProperty("ver2", "FW-2.0.0");
        devices.add(dev);
        body.add("devices", devices);

        JsonObject jnl = new JsonObject();
        jnl.add("head", head);
        jnl.add("body", body);
        return jnl;
    }

    @Test
    @DisplayName("toPacket: head 60바이트 + len 필드 재계산")
    void toPacket_headLayout() {
        byte[] packet = jif.toPacket(kind11Journal());
        assertThat(packet.length).isGreaterThan(60);
        String text = new String(packet, jif.charset());
        // len(4) = 전체 - 4
        assertThat(text.substring(0, 4)).isEqualTo(String.format("%04d", packet.length - 4));
        assertThat(text.substring(4, 8)).isEqualTo("2026");
        assertThat(text.substring(22, 25)).isEqualTo("011");            // bk_cd (len4+일시17+flag1 = 22)
        assertThat(text.substring(25, 45)).startsWith("03704401");      // atm_id(20)
        assertThat(text.substring(50, 52)).isEqualTo("11");             // jnl_kind
    }

    @Test
    @DisplayName("송수신 왕복: Mask → unmask 원복")
    void maskRoundTrip() {
        byte[] packet = jif.toPacket(kind11Journal());
        byte[] original = packet.clone();
        byte key = (byte) 0x5A;
        mask(packet, key);
        assertThat(packet).isNotEqualTo(original);
        LegacyJournalDataController.unmask(packet, key);
        assertThat(packet).isEqualTo(original);
    }

    @Test
    @DisplayName("전체 체인: idx 바이트 → toJson → JifMessage → parseHead")
    void fullChain() throws Exception {
        byte[] packet = jif.toPacket(kind11Journal());

        JsonObject parsed = jif.toJson(packet);
        JsonObject head = parsed.getAsJsonObject("head");
        assertThat(head.get("jnl_kind").getAsString()).isEqualTo("11");
        assertThat(head.get("atm_id").getAsString()).startsWith("03704401");

        JsonObject body = parsed.getAsJsonObject("body");
        assertThat(body.get("devices").getAsJsonArray()).hasSize(1);
        assertThat(body.get("devices").getAsJsonArray().get(0).getAsJsonObject()
                .get("ver1").getAsString().trim()).isEqualTo("AP-1.0.0");

        JifMessage msg = om.readValue(parsed.toString(), JifMessage.class);
        JifHeadKey key = JifFieldUtils.parseHead(msg);
        assertThat(key.getJnlKind()).isEqualTo("11");
        assertThat(key.getJnlDate()).isEqualTo("20260710");
    }
}
