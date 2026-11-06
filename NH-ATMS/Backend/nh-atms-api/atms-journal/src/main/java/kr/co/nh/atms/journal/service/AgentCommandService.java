package kr.co.nh.atms.journal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.journal.dto.CommandDto;
import kr.co.nh.atms.journal.dto.CommandIssueRequest;
import kr.co.nh.atms.journal.dto.CommandListResponse;
import kr.co.nh.atms.journal.dto.CommandResultRequest;
import kr.co.nh.atms.journal.entity.AgentCommand;
import kr.co.nh.atms.journal.mapper.AgentCommandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 서버→에이전트 원격제어 명령 큐 서비스.
 *
 *   issue : 운영자/시스템 명령 발행 → T_AGENT_COMMAND(STATUS=W)
 *   poll  : 에이전트가 기기별 대기 명령 조회(STATUS=W)
 *   ack   : 에이전트 수령 확인(W→A)
 *   result: 에이전트 실행 결과 통보(→S/F)
 *
 * atmId(8) = termGroupId(5) + termId(3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCommandService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int DEFAULT_TIMEOUT_SEC = 180;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final AgentCommandMapper mapper;
    private final ObjectMapper objectMapper;

    /** 원격제어 명령 발행. 적재된 명령은 에이전트 폴링을 대기한다. */
    @Transactional
    public AgentCommand issue(String atmId, CommandIssueRequest req) {
        String id = requireAtmId(atmId);
        int timeout = (req.getTimeoutSec() == null || req.getTimeoutSec() <= 0)
                ? DEFAULT_TIMEOUT_SEC : req.getTimeoutSec();
        String regUser = (req.getRegUserId() == null || req.getRegUserId().isBlank())
                ? "SYSTEM" : req.getRegUserId().trim();

        AgentCommand cmd = AgentCommand.builder()
                .termGroupId(id.substring(0, 5))
                .termId(id.substring(5, 8))
                .cmdType(req.getType())
                .payload(toJson(req.getPayload()))
                .status("W")
                .timeoutSec(timeout)
                .regUserId(regUser)
                .build();
        mapper.insert(cmd);
        log.info("[agent-command] issued cmdId={} atmId={} type={} by={}",
                cmd.getCmdId(), id, req.getType(), regUser);
        return cmd;
    }

    /** 에이전트 폴링: 기기별 대기 명령 목록(계약 CommandList). */
    @Transactional(readOnly = true)
    public CommandListResponse poll(String atmId, int max) {
        String id = requireAtmId(atmId);
        int limit = (max <= 0) ? 10 : Math.min(max, 100);
        List<AgentCommand> pending =
                mapper.selectPending(id.substring(0, 5), id.substring(5, 8), limit);
        List<CommandDto> commands = pending.stream()
                .map(c -> CommandDto.builder()
                        .cmdId(c.getCmdId())
                        .type(c.getCmdType())
                        .atmId(id)
                        .payload(fromJson(c.getPayload()))
                        .issuedAt(c.getIssuedAt() == null ? null : c.getIssuedAt().format(ISO))
                        .timeoutSec(c.getTimeoutSec())
                        .build())
                .toList();
        return CommandListResponse.builder().commands(commands).build();
    }

    /** 수령 확인(W→A). 존재하지 않거나 이미 종료된 명령이면 예외. */
    @Transactional
    public void ack(String atmId, String cmdId) {
        AgentCommand cmd = requireCommand(atmId, cmdId);
        int n = mapper.markAck(cmdId);
        if (n == 0) {
            throw new BusinessException("CMD_NOT_PENDING",
                    "수령 가능한 상태가 아닙니다. (현재=" + cmd.getStatus() + ")");
        }
        log.info("[agent-command] ack cmdId={} atmId={}", cmdId, atmId);
    }

    /** 실행 결과 통보(→S/F). */
    @Transactional
    public void result(String atmId, String cmdId, CommandResultRequest req) {
        requireCommand(atmId, cmdId);
        AgentCommand upd = AgentCommand.builder()
                .cmdId(cmdId)
                .status(req.isSuccess() ? "S" : "F")
                .resultCd(req.getResultCode())
                .resultData(toJson(req.getResultData()))
                .errMsg(req.getErrorMessage())
                .build();
        int n = mapper.updateResult(upd);
        if (n == 0) {
            throw new BusinessException("CMD_ALREADY_DONE", "이미 처리 완료된 명령입니다.");
        }
        log.info("[agent-command] result cmdId={} atmId={} success={} code={}",
                cmdId, atmId, req.isSuccess(), req.getResultCode());
    }

    /** atmId 소속 명령인지 검증 후 반환. */
    private AgentCommand requireCommand(String atmId, String cmdId) {
        String id = requireAtmId(atmId);
        AgentCommand cmd = mapper.selectById(cmdId);
        if (cmd == null) {
            throw new BusinessException("CMD_NOT_FOUND", "명령을 찾을 수 없습니다. cmdId=" + cmdId);
        }
        String cmdAtmId = nz(cmd.getTermGroupId()) + nz(cmd.getTermId());
        if (!id.equals(cmdAtmId)) {
            throw new BusinessException("CMD_ATM_MISMATCH", "명령의 기기와 atmId 가 일치하지 않습니다.");
        }
        return cmd;
    }

    private static String requireAtmId(String atmId) {
        String id = atmId == null ? "" : atmId.trim();
        if (id.length() != 8) {
            throw new BusinessException("INVALID_ATM_ID", "atmId 는 8자리여야 합니다.");
        }
        return id;
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
