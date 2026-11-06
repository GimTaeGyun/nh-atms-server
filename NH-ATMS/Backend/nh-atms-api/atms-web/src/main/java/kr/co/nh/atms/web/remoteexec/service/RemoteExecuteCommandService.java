package kr.co.nh.atms.web.remoteexec.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.co.nh.atms.web.remoteexec.config.InterfaceMessage;
import kr.co.nh.atms.web.remoteexec.config.RemoteExecProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 운영자 원격제어 명령을 MIF 전문(JSON)으로 조립하여 atms-file(snd.msg.do)로 송신한다.
 *
 * <p>구 새마을금고 atms {@code RemoteExecuteCommandServiceImpl} 의 전처리 로직
 * (makeHead / makeCmd / getKeyNames / requestCmd)을 그대로 이식하고, MG 가 DB
 * (T_REMOTEINTERFACE)에서 읽던 명령정보를 NH 의 명령유형(cmdType) + 파라미터(payload)
 * 모델에 맞춰 매핑한다. 실제 기기 푸시(RSA/AES 소켓)는 atms-file 이 담당한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteExecuteCommandService {

    private final RemoteExecProperties props;

    /** 비동기(파일수집/다운로드/직접실행) agent_tx_id. 결과는 put.msg.resp.do 콜백으로 수신. */
    private static final Set<String> ASYNC_TX = Set.of("16", "17", "28", "29");

    /** NH 명령유형(cmdType) → 에이전트 전문코드(agent_tx_id) 매핑. (구 MG txId→agentTxId 동일) */
    private static String agentTxId(String cmdType) {
        return switch (cmdType) {
            case "STAT"    -> "12";   // 상태조회(공통)
            case "IMGC"    -> "14";   // 화상이미지 촬영
            case "DLD"     -> "16";   // 개별 파일다운로드(async)
            case "FCOL"    -> "17";   // 파일수집(async)
            case "RST"     -> "20";   // 재시작(공통)
            case "RBT"     -> "21";   // 리부팅(공통)
            case "POFF"    -> "22";   // 전원off(공통)
            case "STOP"    -> "23";   // 운영중지(공통)
            case "OPEN_OP" -> "24";   // 운영개시(공통)
            case "JMOD"    -> "25";   // 저널운영모드 변경
            case "MSG"     -> "27";   // 메시지전송
            case "RCMD"    -> "28";   // 직접 명령실행(async)
            case "GDL"     -> "29";   // 일반 파일다운로드(async)
            case "TMOD"    -> "32";   // 거래모드 변경
            case "CLS"     -> "33";   // 마감제어(합계/소계)
            default -> throw new IllegalArgumentException("지원하지 않는 명령유형: " + cmdType);
        };
    }

    public boolean isAsync(String cmdType) {
        return ASYNC_TX.contains(agentTxId(cmdType));
    }

    /**
     * 명령을 조립하여 기기(atmIp)로 송신하고 atms-file 응답을 반환한다.
     * 반환 JSON: {@code {status:HTTP코드, result:기기응답 or {async_uid}}}.
     */
    public JsonObject sendCommand(String atmIp, String cmdType, Map<String, Object> payload) {
        String tx = agentTxId(cmdType);

        JsonObject head = makeHead(tx);
        JsonObject body = makeBody(tx, payload);
        JsonObject cmd = makeCmd(atmIp, head, body);

        return requestCmd(cmd);
    }

    // ── 전문 조립 (구 MG RemoteExecuteCommandServiceImpl 이식) ─────────────

    /** 명령유형별 body 구성. getKeyNames(스펙 #key#) 순서에 맞춰 값 매핑. */
    private JsonObject makeBody(String tx, Map<String, Object> payload) {
        JsonObject body = new JsonObject();
        switch (tx) {
            case "12", "20", "21", "22", "23", "24" -> {
                // 공통 명령: body 없음
            }
            case "14" -> {                                   // 화상이미지 촬영
                String[] k = getKeyNames(tx);
                body.addProperty(k[0], str(payload, "scrKind", str(payload, "kind", "")));  // scr_kind
                body.addProperty(k[1], "");                                                 // img_path
            }
            case "25" -> {                                   // 저널운영모드 변경
                String[] k = getKeyNames(tx);
                body.addProperty(k[0], str(payload, "jnlMode", str(payload, "mode", "")));  // jnl_mode
            }
            case "27" -> {                                   // 메시지전송
                String[] k = getKeyNames(tx);
                int showSec = intOf(payload, "showSec", 10);
                String msg = str(payload, "showMsg", str(payload, "message", ""));
                body.addProperty(k[0], showSec);             // show_sec
                body.addProperty(k[1], msg);                 // show_msg
            }
            case "32" -> {                                   // 거래모드 변경
                String[] k = getKeyNames(tx);
                String[] values = trxModeValues(payload);
                IntStream.range(0, Math.min(k.length, values.length))
                         .forEach(i -> body.addProperty(k[i], values[i]));
            }
            case "33" -> {                                   // 마감제어(합계/소계)
                String[] k = getKeyNames(tx);
                body.addProperty(k[0], str(payload, "kind", ""));   // kind
            }
            case "17" -> {                                   // 파일수집(async)
                String[] k = getKeyNames(tx);
                String flInfo = str(payload, "flInfo", str(payload, "fileInfo", ""));
                body.addProperty(k[0], str(payload, "flType", str(payload, "fileType", "")));   // fl_type
                body.addProperty(k[1], flInfo);                                                 // fl_info
                body.addProperty(k[2], str(payload, "flPath", flInfo));                         // fl_path
                body.addProperty(k[3], "1");                                                    // fl_comp
                body.addProperty(k[4], "");                                                     // fl_csum
                body.addProperty(k[5], "");                                                     // fl_size
            }
            case "16" -> {                                   // 개별 파일다운로드(async)
                String[] k = getKeyNames(tx);
                body.addProperty(k[0], str(payload, "exeTypeCd", str(payload, "pkgType", ""))); // dl_kind
                body.addProperty(k[1], str(payload, "flType", str(payload, "pkgType", "")));    // fl_type
                body.addProperty(k[2], str(payload, "dlPath", str(payload, "filePath", "")));   // dl_path(서버경로)
                body.addProperty(k[3], str(payload, "dlVers", str(payload, "downVer", "")));    // dl_vers
                body.addProperty(k[4], str(payload, "cpPath", str(payload, "downloadPath", ""))); // cp_path(기기경로)
            }
            case "29" -> {                                   // 일반 파일다운로드(async)
                String[] k = getKeyNames(tx);
                String fileName = str(payload, "fileName", "");
                String downloadPath = str(payload, "downloadPath", "");
                String filePath = str(payload, "filePath", "");

                JsonArray files = new JsonArray();
                JsonObject file = new JsonObject();
                file.addProperty(k[1], join(downloadPath, fileName));  // 기기경로
                files.add(file);

                JsonArray downloads = new JsonArray();
                JsonObject dl = new JsonObject();
                dl.addProperty(k[4], filePath);                        // 서버경로
                downloads.add(dl);

                body.add(k[0], files);                                 // files
                body.addProperty(k[2], str(payload, "execCmd", ""));   // cmd
                body.add(k[3], downloads);                             // downloads
            }
            case "28" -> {                                   // 직접 명령실행(async)
                String[] k = getKeyNames(tx);
                body.addProperty(k[0], str(payload, "command", str(payload, "execCmd", "")));   // cmd
            }
            default -> { /* no-op */ }
        }
        return body;
    }

    /** 거래모드 변경값: payload.modes(List) 우선, 없으면 sendData("a|b|c"). */
    @SuppressWarnings("unchecked")
    private static String[] trxModeValues(Map<String, Object> payload) {
        Object modes = payload == null ? null : payload.get("modes");
        if (modes instanceof List<?> list) {
            return list.stream().map(o -> o == null ? "" : String.valueOf(o)).toArray(String[]::new);
        }
        String sendData = str(payload, "sendData", "");
        return Arrays.stream(sendData.split("\\|")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    /** 전문 키 가져오기 (스펙 #mif#/#body#/{tx}/#field#). 중첩 #field# 도 평탄화. */
    private String[] getKeyNames(String agentTxId) {
        JsonArray ja = InterfaceMessage.getDefinition()
                .get("#mif#").getAsJsonObject()
                .get("#body#").getAsJsonObject()
                .get(agentTxId).getAsJsonObject()
                .get("#field#").getAsJsonArray();

        return StreamSupport.stream(ja.spliterator(), false)
                .flatMap(elem -> {
                    JsonObject obj = elem.getAsJsonObject();
                    Stream<String> mainKey = Stream.of(obj.get("#key#").getAsString());
                    if (obj.has("#field#")) {
                        JsonArray nested = obj.getAsJsonArray("#field#");
                        Stream<String> nestedKeys = StreamSupport.stream(nested.spliterator(), false)
                                .map(e -> e.getAsJsonObject().get("#key#").getAsString());
                        return Stream.concat(mainKey, nestedKeys);
                    }
                    return mainKey;
                })
                .toArray(String[]::new);
    }

    /** 전문 헤더 구성 (스펙 #mif#/#head#/#field#). */
    private JsonObject makeHead(String agentTxId) {
        JsonArray ja = InterfaceMessage.getDefinition()
                .get("#mif#").getAsJsonObject()
                .get("#head#").getAsJsonObject()
                .get("#field#").getAsJsonArray();

        String[] keys = StreamSupport.stream(ja.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(o -> o.get("#key#").getAsString())
                .toArray(String[]::new);

        JsonObject head = new JsonObject();
        head.addProperty(keys[0], "");          // len
        head.addProperty(keys[1], agentTxId);   // txcode
        head.addProperty(keys[2], "01");        // trcode
        head.addProperty(keys[3], "");          // seqno
        head.addProperty(keys[4], "");          // maxseq
        head.addProperty(keys[5], "");          // respcd
        head.addProperty(keys[6], "");          // filler
        return head;
    }

    /** 송신 명령 구성: {ip, dat:{head, body}}. */
    private JsonObject makeCmd(String atmIp, JsonObject head, JsonObject body) {
        JsonObject dat = new JsonObject();
        dat.add("head", head);
        dat.add("body", body);

        JsonObject cmd = new JsonObject();
        cmd.addProperty("ip", atmIp.trim());
        cmd.add("dat", dat);

        if (log.isDebugEnabled()) {
            log.debug("\n ***** 원격명령 요청 to atms-file \n {}",
                    new GsonBuilder().setPrettyPrinting().create().toJson(cmd));
        }
        return cmd;
    }

    /** atms-file(snd.msg.do)로 전문 송신. (구 MG requestCmd, URL만 설정값으로 교체) */
    private JsonObject requestCmd(JsonObject requestJson) {
        JsonObject result = new JsonObject();
        StringBuilder responseText = new StringBuilder();
        int responseCode = 0;
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(props.getSndMsgUri()).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(props.getConnectTimeoutMs());
            conn.setReadTimeout(props.getReadTimeoutMs());
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = new Gson().toJson(requestJson).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            responseCode = conn.getResponseCode();
            result.addProperty("status", responseCode);
            InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseText.append(line.trim());
                }
            }

            JsonObject responseBody = JsonParser.parseString(responseText.toString()).getAsJsonObject();
            result.add("result", responseBody);
        } catch (Exception e) {
            result.addProperty("status", responseCode);
            JsonObject fallback = new JsonObject();
            fallback.addProperty("raw", responseCode == 0 ? String.valueOf(e.getMessage()) : responseText.toString());
            result.add("result", fallback);
            log.error("[원격명령] atms-file 송신 실패: {}", e.toString());
        } finally {
            if (conn != null) conn.disconnect();
        }

        if (log.isDebugEnabled()) {
            log.debug("\n ***** 원격명령 결과 from atms-file \n {}",
                    new GsonBuilder().setPrettyPrinting().create().toJson(result));
        }
        return result;
    }

    // ── payload 헬퍼 ─────────────────────────────────────────────────────

    private static String str(Map<String, Object> payload, String key, String def) {
        if (payload == null) return def;
        Object v = payload.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private static int intOf(Map<String, Object> payload, String key, int def) {
        if (payload == null) return def;
        Object v = payload.get(key);
        if (v == null) return def;
        try {
            return v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String join(String dir, String name) {
        if (dir == null || dir.isBlank()) return name == null ? "" : name;
        if (name == null || name.isBlank()) return dir;
        String sep = dir.endsWith("/") || dir.endsWith("\\") ? "" : "/";
        return dir + sep + name;
    }
}
