package kr.co.nh.atms.web.remotecontrol.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.agentstatus.mapper.AgentStatusMapper;
import kr.co.nh.atms.web.remotecontrol.dto.RemoteCommandIssueRequest;
import kr.co.nh.atms.web.remotecontrol.dto.RemoteCommandSearchDto;
import kr.co.nh.atms.web.remotecontrol.entity.RemoteCommand;
import kr.co.nh.atms.web.remotecontrol.mapper.RemoteCommandMapper;
import kr.co.nh.atms.web.remoteexec.service.RemoteExecuteCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 운영자 원격제어 명령 발행/이력 조회 서비스.
 *
 * <p>구 새마을금고와 동일하게 발행 즉시 기기로 <b>서버 푸시</b>(RSA/AES 소켓)한다.
 * 명령을 T_AGENT_COMMAND 에 적재(STATUS=W) 후, 접속중인 기기 IP(T_AGENT_STATUS)를
 * 찾아 atms-file(snd.msg.do)로 전문을 송신한다. 동기 명령은 즉시 결과(S/F), 비동기
 * (파일수집/다운로드/직접실행)는 수령(A)으로 표시되고 완료는 put.msg.resp.do 콜백으로
 * 갱신된다. 기기 미접속 시 푸시 불가로 STATUS=W 를 유지한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RemoteControlService {

    private static final int DEFAULT_TIMEOUT_SEC = 180;

    private final RemoteCommandMapper mapper;
    private final ObjectMapper objectMapper;
    private final AgentStatusMapper agentStatusMapper;
    private final RemoteExecuteCommandService remoteExecuteCommandService;

    /** 명령 발행이력 조회. */
    public PageResult<RemoteCommand> search(RemoteCommandSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    /** 명령 단건 조회(결과 확인). */
    public RemoteCommand get(String cmdId) {
        RemoteCommand cmd = mapper.selectOne(cmdId);
        if (cmd == null) throw new BusinessException("RMC_001", "명령 정보가 없습니다.");
        return cmd;
    }

    /** 원격제어 명령 발행 + 기기 푸시. */
    @Transactional
    public RemoteCommand issue(RemoteCommandIssueRequest req) {
        int timeout = (req.getTimeoutSec() == null || req.getTimeoutSec() <= 0)
                ? DEFAULT_TIMEOUT_SEC : req.getTimeoutSec();

        RemoteCommand cmd = RemoteCommand.builder()
                .termGroupId(req.getTermGroupId().trim())
                .termId(req.getTermId().trim())
                .cmdType(req.getCmdType())
                .payload(toJson(req.getPayload()))
                .status("W")
                .timeoutSec(timeout)
                .regUserId(currentUserId())
                .build();
        mapper.insert(cmd);
        log.info("[remote-control] issued cmdId={} term={}/{} type={} by={}",
                cmd.getCmdId(), cmd.getTermGroupId(), cmd.getTermId(), cmd.getCmdType(), cmd.getRegUserId());

        pushToDevice(cmd, req.getPayload());
        return cmd;
    }

    /** 발행된 명령을 접속중인 기기로 즉시 푸시하고 결과로 상태를 갱신한다. */
    private void pushToDevice(RemoteCommand cmd, Map<String, Object> payload) {
        String atmIp = agentStatusMapper.selectConnectedIp(cmd.getTermGroupId(), cmd.getTermId());
        if (atmIp == null || atmIp.isBlank()) {
            log.warn("[remote-control] cmdId={} term={}/{} 기기 미접속 → 푸시 보류(STATUS=W)",
                    cmd.getCmdId(), cmd.getTermGroupId(), cmd.getTermId());
            return;
        }

        try {
            JsonObject resp = remoteExecuteCommandService.sendCommand(atmIp.trim(), cmd.getCmdType(), payload);
            int httpStatus = resp.has("status") ? resp.get("status").getAsInt() : 0;
            JsonObject result = resp.has("result") && resp.get("result").isJsonObject()
                    ? resp.getAsJsonObject("result") : new JsonObject();

            if (httpStatus != 200) {
                fail(cmd, "ATMF", "atms-file 송신 실패(status=" + httpStatus + ")", result);
                return;
            }

            // atms-file 가 기기 소켓 송신에 실패하면(연결 거부/타임아웃 등) head 없이 ##emsg## 만 내려온다.
            // 동기/비동기 판정 이전에 전송오류로 간주하여 실패(F) 처리한다.
            if (result.has("##emsg##")) {
                String emsg = result.get("##emsg##").isJsonNull()
                        ? "기기 송신 오류" : result.get("##emsg##").getAsString();
                fail(cmd, "SEND", emsg, result);
                return;
            }

            if (remoteExecuteCommandService.isAsync(cmd.getCmdType())) {
                if (result.has("async_uid")) {
                    Long asyncUid = parseAsyncUid(result.get("async_uid").getAsString());
                    mapper.updateAfterPush(cmd.getCmdId(), "A", asyncUid, null, result.toString(), null);
                    cmd.setStatus("A");
                    cmd.setAsyncUid(asyncUid);
                    log.info("[remote-control] cmdId={} 비동기 수령(A) asyncUid={}", cmd.getCmdId(), asyncUid);
                } else {
                    fail(cmd, "ASYNC", "async_uid 미수신(요청 실패)", result);
                }
            } else {
                String respcd = respcdOf(result);
                if ("0".equals(respcd) || respcd == null) {
                    mapper.updateAfterPush(cmd.getCmdId(), "S", null, respcd, result.toString(), null);
                    cmd.setStatus("S");
                    log.info("[remote-control] cmdId={} 동기 성공(S) respcd={}", cmd.getCmdId(), respcd);
                } else {
                    fail(cmd, respcd, "기기 응답 오류(respcd=" + respcd + ")", result);
                }
            }
        } catch (Exception e) {
            log.error("[remote-control] cmdId={} 푸시 예외", cmd.getCmdId(), e);
            mapper.updateAfterPush(cmd.getCmdId(), "F", null, "EXC", null, truncate(e.getMessage()));
            cmd.setStatus("F");
        }
    }

    private void fail(RemoteCommand cmd, String resultCd, String errMsg, JsonObject result) {
        mapper.updateAfterPush(cmd.getCmdId(), "F", null, resultCd,
                result == null ? null : result.toString(), truncate(errMsg));
        cmd.setStatus("F");
        log.warn("[remote-control] cmdId={} 실패(F): {}", cmd.getCmdId(), errMsg);
    }

    /** 기기 응답 head.respcd 추출(없으면 null → 동기 명령은 200 응답을 성공으로 간주). */
    private static String respcdOf(JsonObject result) {
        if (result != null && result.has("head") && result.get("head").isJsonObject()) {
            JsonObject head = result.getAsJsonObject("head");
            if (head.has("respcd") && !head.get("respcd").isJsonNull()) {
                String v = head.get("respcd").getAsString();
                return v.isBlank() ? null : v.trim();
            }
        }
        return null;
    }

    private static Long parseAsyncUid(String s) {
        try {
            return s == null ? null : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }

    private String currentUserId() {
        try {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            return (name == null || name.isBlank()) ? "SYSTEM" : name;
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException("RMC_002", "명령 파라미터 변환에 실패했습니다.");
        }
    }
}
