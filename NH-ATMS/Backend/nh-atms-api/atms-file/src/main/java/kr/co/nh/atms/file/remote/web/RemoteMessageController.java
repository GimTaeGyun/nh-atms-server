package kr.co.nh.atms.file.remote.web;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.nh.atms.file.remote.config.RemoteProperties;
import kr.co.nh.atms.file.remote.mapper.MsgAsyncMapper;
import kr.co.nh.atms.file.remote.mif.Converter;
import kr.co.nh.atms.file.remote.mif.RemoteCommand;
import kr.co.nh.atms.file.remote.util.Json;
import kr.co.nh.atms.file.remote.util.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.util.HashMap;
import java.util.Map;

/**
 * 원격명령 MIF push 엔드포인트.
 * 구 새마을금고 atmf ServletHandler (snd.msg.do / put.msg.resp.do / get.msg.resp.do) 이식.
 *
 *  - POST /atmf/snd.msg.do          : atms-web → atmf. 명령 전문을 기기측 AP로 RSA/AES push
 *  - POST /atmf/agent/put.msg.resp.do : 기기 → atmf. 비동기 결과 수신 → t_msg_async update + atms-web forward
 *  - GET/POST /atmf/agent/get.msg.resp.do : UI 폴링. 비동기 결과 조회
 */
@RestController
public class RemoteMessageController {

    private static final Logger log = LoggerFactory.getLogger(RemoteMessageController.class);

    private final RemoteProperties props;
    private final MsgAsyncMapper msgAsyncMapper;

    public RemoteMessageController(RemoteProperties props, MsgAsyncMapper msgAsyncMapper) {
        this.props = props;
        this.msgAsyncMapper = msgAsyncMapper;
    }

    /**
     * 명령 전문을 기기로 송신.
     * 비동기(##async##) 명령은 t_msg_async insert 후 async_uid 부여, respcd='A'.
     */
    @PostMapping(value = "/atmf/snd.msg.do", produces = MediaType.APPLICATION_JSON_VALUE)
    public String sndMsg(HttpServletRequest req) throws Exception {
        String jtxt = new String(readAllBody(req), StandardCharsets.UTF_8);
        JsonObject jreq = Json.toJsonObject(jtxt);
        JsonObject jres = null;
        Map<String, Object> dbParam = new HashMap<>();
        boolean async = false;
        try {
            String agIp = jreq.get("ip").getAsString();
            JsonObject jdat = jreq.get("dat").getAsJsonObject();

            JsonObject jspecBody = Converter.MIF.body(Json.str(jdat, "//./head/txcode"));
            async = Json.bool(jspecBody, "##async##");
            String asyncUid = null;
            if (async) {
                dbParam.put("snd", Json.toJsonText(jdat));
                msgAsyncMapper.insert(dbParam);

                asyncUid = String.format("%010d", ((Number) dbParam.get("uid")).longValue());
                jdat.get("head").getAsJsonObject().addProperty("respcd", "A");
                jdat.get("body").getAsJsonObject().addProperty("##async_uid##", asyncUid);
            }

            byte[] sPacket = Converter.MIF.toPacket(jdat);
            byte[] rPacket = RemoteCommand.send(agIp, props.getAgentRemotePort(), props.getConnectTimeoutMs(), sPacket);
            jres = Converter.MIF.toJson(rPacket);
            if (async) {
                jres.addProperty("async_uid", asyncUid);
            }
        } catch (Exception e) {
            log.error("[snd.msg.do][runtime exception]", e);
            jres = new JsonObject();
            jres.addProperty("##emsg##", Str.nvlt(e.getMessage(), "null"));
            if (async && dbParam.get("uid") != null) {
                try {
                    dbParam.put("rcv", Json.toJsonText(jres));
                    dbParam.put("sta", "2");
                    msgAsyncMapper.update(dbParam);
                } catch (Exception ignore) {
                    log.error("[snd.msg.do][t_msg_async update fail]", ignore);
                }
            }
        }
        return Json.toJsonText(jres);
    }

    /**
     * 기기로부터 비동기 결과 수신 → t_msg_async update(sta=3) 후 atms-web 으로 forward.
     */
    @PostMapping(value = "/atmf/agent/put.msg.resp.do", produces = MediaType.APPLICATION_JSON_VALUE)
    public String putMsgResp(HttpServletRequest req) throws Exception {
        JsonObject jresp = Converter.MIF.toJson(readAllBody(req));

        HttpURLConnection conn = null;
        InputStream ins = null;
        try {
            String jtxt = Json.toJsonText(jresp);
            byte[] jbin = jtxt.getBytes(StandardCharsets.UTF_8);
            String asyncUid = req.getHeader("async-uid");

            Map<String, Object> dbParam = new HashMap<>();
            dbParam.put("uid", parseUid(asyncUid));
            dbParam.put("rcv", jtxt);
            dbParam.put("sta", "3");
            msgAsyncMapper.update(dbParam);

            URL url = URI.create(props.getMsgAsyncRespForwardUri()).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("async-uid", asyncUid);
            conn.setFixedLengthStreamingMode(jbin.length);
            conn.setDoOutput(true);
            try (OutputStream ous = conn.getOutputStream()) {
                ous.write(jbin, 0, jbin.length);
            }
            int status = conn.getResponseCode();
            try {
                ins = conn.getInputStream();
            } catch (Exception e) {
                ins = conn.getErrorStream();
            }
            byte[] brsp;
            try (ByteArrayOutputStream bous = new ByteArrayOutputStream()) {
                if (ins != null) {
                    byte[] buf = new byte[1024];
                    int io;
                    while ((io = ins.read(buf, 0, buf.length)) > 0) {
                        bous.write(buf, 0, io);
                    }
                }
                brsp = bous.toByteArray();
            }
            jresp = new JsonObject();
            jresp.addProperty("##emsg##", status == 200 ? "" : new String(brsp, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("[put.msg.resp.do][runtime exception]", e);
            jresp = new JsonObject();
            jresp.addProperty("##emsg##", Str.nvlt(e.getMessage(), "null"));
        } finally {
            if (ins != null) {
                try { ins.close(); } catch (Exception ignore) { /* noop */ }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return Json.toJsonText(jresp);
    }

    /**
     * 비동기 결과 조회(UI 폴링).
     */
    @RequestMapping(value = "/atmf/agent/get.msg.resp.do",
            method = { RequestMethod.GET, RequestMethod.POST },
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String getMsgResp(@RequestParam("async-uid") String asyncUid) {
        JsonObject jres = new JsonObject();
        try {
            Map<String, Object> dbParam = new HashMap<>();
            dbParam.put("uid", parseUid(asyncUid));
            Map<String, Object> result = msgAsyncMapper.selectRcv(dbParam);
            if (result == null) {
                jres.addProperty("##emsg##", "not found");
                return Json.toJsonText(jres);
            }
            jres.addProperty("sta", asString(result.get("sta")));
            jres.addProperty("rcv_dt", asString(result.get("rcv_dt")));
            try {
                jres.add("rcv", Json.toJsonObject(asString(result.get("rcv"))));
            } catch (Exception e) {
                jres.add("rcv", new JsonObject());
            }
        } catch (Exception e) {
            log.error("[get.msg.resp.do][runtime exception]", e);
            jres = new JsonObject();
            jres.addProperty("##emsg##", Str.nvlt(e.getMessage(), "null"));
        }
        return Json.toJsonText(jres);
    }

    // ----------------------------------------------------------------------

    private static Long parseUid(String asyncUid) {
        if (asyncUid == null) { return null; }
        return Long.parseLong(asyncUid.trim());
    }

    /** CLOB/String 모두 안전하게 문자열로 변환. */
    private static String asString(Object v) {
        if (v == null) { return ""; }
        if (v instanceof String) { return (String) v; }
        if (v instanceof Clob) {
            try {
                Clob clob = (Clob) v;
                return clob.getSubString(1, (int) clob.length());
            } catch (Exception e) {
                return "";
            }
        }
        return String.valueOf(v);
    }

    private static byte[] readAllBody(HttpServletRequest req) throws java.io.IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             InputStream ins = req.getInputStream()) {
            byte[] buf = new byte[1024];
            int io;
            while ((io = ins.read(buf, 0, buf.length)) > 0) {
                bos.write(buf, 0, io);
            }
            return bos.toByteArray();
        }
    }
}
