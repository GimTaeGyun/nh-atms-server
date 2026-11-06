package kr.co.nh.atms.journal.worker;

import kr.co.nh.atms.journal.config.AgentCommandProperties;
import kr.co.nh.atms.journal.entity.AgentCommand;
import kr.co.nh.atms.journal.mapper.AgentCommandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * T_AGENT_COMMAND 의 대기(W)/수령(A) 명령 중 ISSUED_AT + TIMEOUT_SEC 가
 * 경과한 건을 주기적으로 T(타임아웃) 처리. 미회수/미완료 명령이 영구 적체되는 것을 방지.
 * agent.command.worker.enabled=true(기본) 일 때만 동작.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentCommandTimeoutWorker {

    private final AgentCommandMapper mapper;
    private final AgentCommandProperties props;

    @Scheduled(fixedDelayString = "${agent.command.worker.fixed-delay:30000}")
    public void run() {
        if (!props.isEnabled()) {
            return;
        }
        List<AgentCommand> timedOut = mapper.selectTimedOut(props.getBatchSize());
        for (AgentCommand c : timedOut) {
            int n = mapper.markTimeout(c.getCmdId());
            if (n > 0) {
                log.warn("[agent-command] timeout cmdId={} atmId={}{} type={} issuedAt={} timeoutSec={}",
                        c.getCmdId(), c.getTermGroupId(), c.getTermId(),
                        c.getCmdType(), c.getIssuedAt(), c.getTimeoutSec());
            }
        }
    }
}
