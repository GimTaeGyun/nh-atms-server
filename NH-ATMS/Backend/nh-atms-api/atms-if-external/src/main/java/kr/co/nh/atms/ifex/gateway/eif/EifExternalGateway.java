package kr.co.nh.atms.ifex.gateway.eif;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kr.co.nh.atms.ifex.gateway.ExternalGateway;
import kr.co.nh.atms.ifex.gateway.GatewayResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EIF 실연동 게이트웨이 — 고정길이 전문(512바이트, euc-kr) 조립 후 업체별 TCP 송신.
 *
 * <p>구 atme.lib Gateway(send_post + send_process) + MessageMakeUtil.makeHead 의 동기 축약 이식.
 * 레거시는 상주 NIO 세션/큐 기반 비동기였으나, TO-BE 는 요청 단위 동기 송신(응답 read timeout
 * 시 송신완료로 간주)으로 단순화한다. 전문 스펙은 interface.external.json (Converter) 사용.</p>
 *
 * <p>송신전문(JSON) 규약: {@code ExternalDispatchService} 가 만든 순서 보존 JSON 에서
 * vendorCd/atmsSn/vendSn/responseCd 는 헤더로, 나머지 필드는 스펙 바디 필드에 순서대로 매핑한다.</p>
 *
 * <p>빈 등록은 {@code GatewayConfig} 가 담당 (external.gateway.mode=eif 일 때만).</p>
 */
@Slf4j
public class EifExternalGateway implements ExternalGateway {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmmss");

    /** msg 맵 헤더 예약 키 (ExternalDispatchService 와 동일 규약) */
    private static final Set<String> HEAD_KEYS = Set.of("vendorCd", "atmsSn", "vendSn", "responseCd");

    private static final String OK = "0000";
    private static final String FAIL = "9999";

    private final EifProperties props;
    private final Converter converter = new Converter();

    public EifExternalGateway(EifProperties props) {
        this.props = props;
        loadSpec();
    }

    private void loadSpec() {
        try {
            ClassPathResource resource = new ClassPathResource(props.getSpecLocation());
            try (InputStream in = resource.getInputStream()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                converter.load(props.getInterfaceName(), json);
            }
            log.info("[EIF] spec loaded: name={}, charset={}, location={}",
                    converter.name(), converter.charset(), props.getSpecLocation());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "[EIF] 전문 스펙 로딩 실패: " + props.getSpecLocation(), e);
        }
    }

    @Override
    public GatewayResult send(String globalId, String trCode, String ifType, String sendMsg) {
        try {
            // 1) trCode 파싱: "EM_01001120" -> kind EM / txcode 0100 / trcode 1120
            String[] code = parseMsgCode(trCode);
            String kind = code[0];
            String txcode = code[1];
            String trcode = code[2];

            // 2) 송신 JSON(순서 보존) 파싱 — 헤더 예약키/바디 분리
            JsonObject jmsg = Json.toJsonObject(sendMsg);
            String vendorCd = Json.strt(jmsg, "vendorCd");
            String atmsSn = Json.strt(jmsg, "atmsSn");
            String vendSn = Json.strt(jmsg, "vendSn");
            String responseCd = jmsg.has("responseCd") ? Json.strt(jmsg, "responseCd") : OK;

            // 3) 전문 head + body(스펙 필드 순서 매핑) 조립
            JsonObject message = new JsonObject();
            message.add("head", makeHead(kind, txcode, trcode, vendorCd, atmsSn, vendSn, responseCd));
            message.add("body", makeBody(txcode, trcode, jmsg));

            byte[] packet = converter.toPacket(message);
            if (packet == null) {
                return new GatewayResult(FAIL, "전문 조립 실패(스펙 없음): " + trCode, null, null);
            }
            String rawSend = new String(packet, converter.charset());

            // 4) 업체 엔드포인트 라우팅
            EifProperties.Endpoint ep = props.getVendors().get(vendorCd);
            if (ep == null) {
                log.error("[EIF] 업체 라우팅 정보 없음: vendorCd={} tr={}", vendorCd, trCode);
                return new GatewayResult(FAIL, "업체 라우팅 정보 없음: " + vendorCd, null, rawSend);
            }

            // 5) TCP 송신 + (best-effort) 응답 수신
            return transmit(ep, packet, rawSend, trCode);
        } catch (Exception e) {
            log.error("[EIF] 송신 실패 globalId={} tr={}", globalId, trCode, e);
            return new GatewayResult(FAIL, "송신 실패: " + e.getMessage(), null, null);
        }
    }

    private GatewayResult transmit(EifProperties.Endpoint ep, byte[] packet, String rawSend, String trCode) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ep.getIp(), ep.getPort()), props.getConnectTimeoutMs());
            socket.setSoTimeout(props.getReadTimeoutMs());
            socket.setKeepAlive(true);

            OutputStream out = socket.getOutputStream();
            out.write(packet);
            out.flush();

            // 응답 수신: 선두 4바이트 tlen(잔여 길이) + 잔여
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                byte[] lenBuf = new byte[4];
                in.readFully(lenBuf);
                int remain = Integer.parseInt(new String(lenBuf, StandardCharsets.US_ASCII).trim());
                byte[] rest = new byte[Math.max(remain, 0)];
                in.readFully(rest);

                byte[] resp = new byte[4 + rest.length];
                System.arraycopy(lenBuf, 0, resp, 0, 4);
                System.arraycopy(rest, 0, resp, 4, rest.length);

                JsonObject jresp = converter.toJson(resp);
                String rspCd = Json.strt(Json.obj(jresp, "head"), "rsp_cd");
                if (rspCd.isEmpty()) {
                    rspCd = OK;
                }
                return new GatewayResult(rspCd,
                        OK.equals(rspCd) ? "정상" : "업체 응답코드 " + rspCd,
                        Json.toJsonText(jresp, false), rawSend);
            } catch (SocketTimeoutException te) {
                // 레거시 비동기 특성상 즉시 응답이 없을 수 있음 — 송신 성공으로 간주
                log.info("[EIF] 응답 미수신(timeout) — 송신완료 처리: tr={}", trCode);
                return new GatewayResult(OK, "송신완료(응답 미수신)", null, rawSend);
            }
        } catch (Exception e) {
            log.error("[EIF] TCP 전송 실패 {}:{} tr={}", ep.getIp(), ep.getPort(), trCode, e);
            return new GatewayResult(FAIL, "전송 실패: " + e.getMessage(), null, rawSend);
        }
    }

    /** "EM_01001120" -> [EM, 0100, 1120] */
    private static String[] parseMsgCode(String trCode) {
        if (trCode == null || !trCode.matches("[A-Z]{2}_\\d{8}")) {
            throw new IllegalArgumentException("전문코드 형식 오류(예: EM_01001120): " + trCode);
        }
        String kind = trCode.substring(0, 2);
        String txcode = trCode.substring(3, 7);
        String trcode = trCode.substring(7, 11);
        return new String[]{kind, txcode, trcode};
    }

    /**
     * 전문 헤더 조립 (레거시 MessageMakeUtil.makeHead 정합 — 스펙 head 필드 키 순서 사용).
     * tlen 0508 / len 0486 고정, atms_sn·vend_sn 은 디스패처가 채번한 값 사용.
     */
    private JsonObject makeHead(String kind, String txcode, String trcode,
                                String vendorCd, String atmsSn, String vendSn, String responseCd) {
        List<String> keys = headKeys();
        LocalDateTime now = LocalDateTime.now();

        JsonObject head = new JsonObject();
        head.addProperty(keys.get(0), "0508");                 // tlen
        head.addProperty(keys.get(1), "0");                    // act
        head.addProperty(keys.get(2), "");                     // filler
        head.addProperty(keys.get(3), "0486");                 // len
        head.addProperty(keys.get(4), kind);                   // kind
        head.addProperty(keys.get(5), now.format(DF));         // date
        head.addProperty(keys.get(6), now.format(TF));         // time
        head.addProperty(keys.get(7), atmsSn);                 // atms_sn
        head.addProperty(keys.get(8), vendSn);                 // vend_sn
        head.addProperty(keys.get(9), txcode);                 // txcode
        head.addProperty(keys.get(10), trcode);                // trcode
        head.addProperty(keys.get(11), responseCd);            // rsp_cd
        head.addProperty(keys.get(12), vendorCd);              // vend_cd
        head.addProperty(keys.get(13), props.getOrgCd());      // org_cd (045)
        head.addProperty(keys.get(14), "0");                   // snd_cd (0: atms 송신)
        head.addProperty(keys.get(15), "");                    // reserve
        return head;
    }

    private List<String> headKeys() {
        JsonArray fields = converter.head().getAsJsonArray(Converter.FIELD_KEY);
        List<String> keys = new ArrayList<>();
        for (JsonElement e : fields) {
            keys.add(e.getAsJsonObject().get(Converter.KEY_KEY).getAsString());
        }
        return keys;
    }

    /**
     * 전문 바디 조립 — 디스패처 JSON 의 바디 필드(헤더 예약키 제외)를
     * 스펙(txcode/trcode) 필드 키에 순서대로 매핑한다 (레거시 getKeyNames 등가).
     * 제공 필드 수가 스펙보다 적으면 잔여 필드는 공백으로 채워진다.
     */
    private JsonObject makeBody(String txcode, String trcode, JsonObject jmsg) {
        JsonObject body = new JsonObject();
        JsonObject spec = Json.obj(converter.body(txcode), trcode);
        JsonElement jfields = spec.get(Converter.FIELD_KEY);
        if (jfields == null || !jfields.isJsonArray()) {
            log.warn("[EIF] 전문 바디 스펙 없음: {}/{}", txcode, trcode);
            return body;
        }
        JsonArray fields = jfields.getAsJsonArray();

        List<String> values = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : jmsg.entrySet()) {
            if (HEAD_KEYS.contains(e.getKey())) {
                continue;
            }
            values.add(e.getValue().isJsonNull() ? "" : e.getValue().getAsString());
        }

        int n = Math.min(fields.size(), values.size());
        if (values.size() > fields.size()) {
            log.warn("[EIF] 바디 필드 초과: 제공 {} / 스펙 {} ({} {})",
                    values.size(), fields.size(), txcode, trcode);
        }
        for (int i = 0; i < n; i++) {
            String key = fields.get(i).getAsJsonObject().get(Converter.KEY_KEY).getAsString();
            body.addProperty(key, values.get(i));
        }
        return body;
    }
}
