package kr.co.nh.atms.journal.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.journal.config.JournalErrorRetryProperties;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.entity.JournalError;
import kr.co.nh.atms.journal.mapper.JournalErrorMapper;
import kr.co.nh.atms.journal.service.JournalDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * T_JOURNAL_ERROR 의 재처리 대기(STATUS='E') 건을 폴링하여 보관된 JIF 를 재파싱·재처리 (atmr 1-E).
 * journal.error-retry.enabled=true 일 때만 동작.
 * 성공 → STATUS='R', 재시도 초과 → STATUS='F', 그 외 RETRY_CNT+1 후 다음 주기에 재시도.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JournalErrorRetryWorker {

    private final JournalErrorMapper errorMapper;
    private final JournalDispatchService dispatchService;
    private final ObjectMapper objectMapper;
    private final JournalErrorRetryProperties props;

    @Scheduled(fixedDelayString = "${journal.error-retry.fixed-delay:60000}")
    public void run() {
        if (!props.isEnabled()) {
            return;
        }
        List<JournalError> targets = errorMapper.selectRetryable(props.getBatchSize(), props.getMaxRetry());
        if (targets.isEmpty()) {
            return;
        }
        int ok = 0, failed = 0, retried = 0;
        for (JournalError e : targets) {
            switch (retryOne(e)) {
                case 1 -> ok++;
                case 2 -> failed++;
                default -> retried++;
            }
        }
        log.info("[jnl-error-retry] 대상={} 성공={} 영구실패={} 재시도={}",
                targets.size(), ok, failed, retried);
    }

    /** @return 1=성공, 2=영구실패, 0=재시도 */
    private int retryOne(JournalError e) {
        try {
            JifMessage msg = objectMapper.readValue(e.getJnlJson(), JifMessage.class);
            dispatchService.reprocess(msg);
            errorMapper.markResolved(e.getSeq());
            return 1;
        } catch (Exception ex) {
            int next = (e.getRetryCnt() == null ? 0 : e.getRetryCnt()) + 1;
            if (next >= props.getMaxRetry()) {
                errorMapper.markFailed(e.getSeq(), next);
                log.warn("[jnl-error-retry] 영구실패 seq={} cnt={} err={}", e.getSeq(), next, ex.toString());
                return 2;
            }
            errorMapper.incrementRetry(e.getSeq());
            log.debug("[jnl-error-retry] 재시도 seq={} cnt={} err={}", e.getSeq(), next, ex.toString());
            return 0;
        }
    }
}
