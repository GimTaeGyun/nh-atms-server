package kr.co.nh.atms.web.remoteexec.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.nh.atms.web.remotecontrol.entity.RemoteCommand;
import kr.co.nh.atms.web.remotecontrol.mapper.RemoteCommandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 원격명령 비동기 결과 콜백 (atms-file put.msg.resp.do 가 호출).
 *
 * <p>구 새마을금고 atms {@code RemoteExecuteController.handleRemoteAsyncResult} 이식.
 * 비동기(파일수집/다운로드/직접실행) 명령 완료 시 atms-file 이 기기 응답 JSON 본문과
 * {@code async-uid} 헤더를 전달한다. async_uid 로 T_AGENT_COMMAND 를 찾아 head.respcd
 * (0=성공)로 상태를 S/F 로 갱신한다.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RemoteExecuteController {

    private final RemoteCommandMapper mapper;

    @PostMapping("/atms/remote-exec/put.msg.resp.do")
    public Map<String, Object> handleRemoteAsyncResult(@RequestBody String json, HttpServletRequest request) {
        String asyncUidHeader = request.getHeader("async-uid");
        Long asyncUid = parseAsyncUid(asyncUidHeader);

        String respcd = null;
        try {
            JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
            JsonObject head = jo.has("head") && jo.get("head").isJsonObject() ? jo.getAsJsonObject("head") : null;
            if (head != null && head.has("respcd") && !head.get("respcd").isJsonNull()) {
                respcd = head.get("respcd").getAsString().trim();
            }
        } catch (Exception e) {
            log.warn("[remote-exec callback] 응답 파싱 실패 asyncUid={} : {}", asyncUidHeader, e.toString());
        }

        if (asyncUid == null) {
            log.warn("[remote-exec callback] async-uid 헤더 없음 → 무시. body={}", abbreviate(json));
            return Map.of("succ", false);
        }

        RemoteCommand cmd = mapper.selectByAsyncUid(asyncUid);
        if (cmd == null) {
            log.warn("[remote-exec callback] asyncUid={} 매칭 명령 없음(이미 처리/타임아웃?)", asyncUid);
            return Map.of("succ", false);
        }

        boolean success = "0".equals(respcd);
        String status = success ? "S" : "F";
        String errMsg = success ? null : ("기기 비동기 결과 오류(respcd=" + respcd + ")");
        mapper.updateAsyncResult(asyncUid, status, respcd, truncate(json), errMsg);

        log.info("[remote-exec callback] cmdId={} asyncUid={} → {} (respcd={})",
                cmd.getCmdId(), asyncUid, status, respcd);
        return Map.of("succ", true);
    }

    private static Long parseAsyncUid(String s) {
        try {
            return s == null || s.isBlank() ? null : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
