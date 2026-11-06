package kr.co.nh.atms.journal.worker;

import kr.co.nh.atms.journal.config.IfQueueProperties;
import kr.co.nh.atms.journal.entity.IfQueue;
import kr.co.nh.atms.journal.mapper.IfQueueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * T_IF_QUEUE 의 대기(W) 건을 폴링하여 atme(대외)/atmi(대내) REST 로 디스패치 (atmr 1-D).
 * if.queue.worker.enabled=true 일 때만 동작. 외부 서비스 미기동 시 재시도(max-retry 초과→F).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IfQueueWorker {

    private final IfQueueMapper mapper;
    private final RestClient ifQueueRestClient;
    private final IfQueueProperties props;

    @Scheduled(fixedDelayString = "${if.queue.worker.fixed-delay:5000}")
    public void run() {
        if (!props.getWorker().isEnabled()) {
            return;
        }
        List<IfQueue> pending = mapper.selectPending(props.getWorker().getBatchSize());
        for (IfQueue q : pending) {
            dispatch(q);
        }
    }

    private void dispatch(IfQueue q) {
        String url = resolveUrl(q);
        if (url == null) {
            markFail(q, "NO_ROUTE", "라우팅 대상 없음: " + q.getIfTarget() + "/" + q.getIfAction());
            return;
        }
        try {
            String body = q.getPayload() != null ? q.getPayload() : "{}";
            String resp = ifQueueRestClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            markSuccess(q, resp);
        } catch (Exception e) {
            int next = (q.getRetryCnt() == null ? 0 : q.getRetryCnt()) + 1;
            if (next >= props.getWorker().getMaxRetry()) {
                q.setRetryCnt(next);
                markFail(q, "SEND_FAIL", trim(e.getMessage()));
            } else {
                // 재시도 여지: W 유지하고 retryCnt 증가
                q.setStatus("W");
                q.setRetryCnt(next);
                q.setResultCd("RETRY");
                q.setResultMsg(trim(e.getMessage()));
                mapper.updateResult(q);
                log.warn("[if-queue] retry seq={} cnt={} err={}", q.getQueueSeq(), next, e.toString());
            }
        }
    }

    private String resolveUrl(IfQueue q) {
        String target = q.getIfTarget();
        String action = q.getIfAction();
        if ("E".equals(target)) {
            if ("SERVICE_CALL".equals(action)) {
                return props.getExternalBaseUrl() + "/api/if/external/service-call";
            }
            if ("AUTO_SERVICE_CALL".equals(action)) {
                return props.getExternalBaseUrl() + "/api/if/external/auto-service-call";
            }
            return null;
        }
        if ("I".equals(target)) {
            if ("FAULT_SMS".equals(action)) {
                return props.getInternalBaseUrl() + "/api/if/internal/fault-sms";
            }
            if ("FAULT_MESSENGER".equals(action)) {
                return props.getInternalBaseUrl() + "/api/if/internal/fault-messenger";
            }
            return null;
        }
        return null;
    }

    private void markSuccess(IfQueue q, String resp) {
        q.setStatus("S");
        q.setResultCd("OK");
        q.setResultMsg(trim(resp));
        mapper.updateResult(q);
        log.info("[if-queue] sent ok seq={} target={}/{}", q.getQueueSeq(), q.getIfTarget(), q.getIfAction());
    }

    private void markFail(IfQueue q, String code, String msg) {
        q.setStatus("F");
        q.setResultCd(code);
        q.setResultMsg(msg);
        if (q.getRetryCnt() == null) q.setRetryCnt(0);
        mapper.updateResult(q);
        log.warn("[if-queue] failed seq={} code={} msg={}", q.getQueueSeq(), code, msg);
    }

    private static String trim(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
