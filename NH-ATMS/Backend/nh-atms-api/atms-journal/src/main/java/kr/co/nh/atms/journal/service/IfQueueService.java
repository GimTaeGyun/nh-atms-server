package kr.co.nh.atms.journal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.journal.config.IfQueueProperties;
import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.entity.IfQueue;
import kr.co.nh.atms.journal.mapper.IfQueueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 장애 저널(jnl_kind=22) 수신 시 대내/대외 IF 작업을 T_IF_QUEUE 에 적재.
 * 실제 송신은 IfQueueWorker(@Scheduled) 가 비동기로 수행.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IfQueueService {

    /** 정상(무장애)으로 간주하는 코드 — 배치/조회 집계와 동일 기준. */
    private static final Set<String> NORMAL_CODES = Set.of("0", "00", "000", "0000");

    private final IfQueueMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 장애 통보 수신처 설정 (if.queue.notify.*).
     * TODO(NH 요건 확정 후): 고정 수신자 목록 대신 T_USER 계열(운영자/영업점 담당자) 조회로
     *      기기 소속 영업점(term_group_id) 기준 담당자 전화번호/사용자ID 를 동적 매핑한다.
     *      (AS-IS 새마을금고는 대내 IF 모듈에서 수신처를 재조회 — NH 운영자 매핑 규칙 미확정)
     */
    private final IfQueueProperties properties;

    /**
     * 장애 코드가 실제 장애일 때만 대외 출동요청 1건 + 수신처별 대내 장애SMS/메신저 적재.
     * 호출 컨텍스트(JournalDispatchService.doDispatchInTx)의 트랜잭션에 참여.
     */
    public void enqueueOnFault(JifHeadKey key, String errCd) {
        if (!isFault(errCd)) {
            return;
        }

        // 대외(atme): 자동 출동요청 — 저널키로 대외모듈이 T_FAULT_ANALYSIS 재조회해 출동업체 선정
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("termGroupId", key.getTermGroupId());
        ext.put("termId", key.getTermId());
        ext.put("jnlDate", key.getJnlDate());
        ext.put("jnlTime", key.getJnlTime());
        insert(key, errCd, "E", "AUTO_SERVICE_CALL", ext);

        String msgContent = "ATM 장애 발생 [" + key.getTermGroupId() + "/" + key.getTermId()
                + "] 코드=" + errCd;
        IfQueueProperties.Notify notify = properties.getNotify();

        // 대내(atmi): 장애 SMS — 설정된 수신 전화번호별 1건
        for (String phone : notify.getSmsPhones()) {
            if (phone == null || phone.isBlank()) continue;
            Map<String, Object> intl = new LinkedHashMap<>();
            intl.put("termGroupId", key.getTermGroupId());
            intl.put("termId", key.getTermId());
            intl.put("errCd", errCd);
            intl.put("recvPhone", phone.trim());
            intl.put("msgContent", msgContent);
            insert(key, errCd, "I", "FAULT_SMS", intl);
        }

        // 대내(atmi): 장애 메신저(MGenie) 통보 — 설정된 수신 사용자별 1건
        for (String userId : notify.getMessengerUsers()) {
            if (userId == null || userId.isBlank()) continue;
            Map<String, Object> msgr = new LinkedHashMap<>();
            msgr.put("termGroupId", key.getTermGroupId());
            msgr.put("termId", key.getTermId());
            msgr.put("errCd", errCd);
            msgr.put("recvUserId", userId.trim());
            msgr.put("msgContent", msgContent);
            insert(key, errCd, "I", "FAULT_MESSENGER", msgr);
        }

        log.info("[if-queue] enqueued fault term={}/{} errCd={} sms={} messenger={}",
                key.getTermGroupId(), key.getTermId(), errCd,
                notify.getSmsPhones().size(), notify.getMessengerUsers().size());
    }

    private void insert(JifHeadKey key, String errCd, String target, String action,
                        Map<String, Object> payloadMap) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            payload = "{}";
        }
        IfQueue q = IfQueue.builder()
                .ifTarget(target)
                .ifAction(action)
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .errCd(errCd)
                .payload(payload)
                .status("W")
                .retryCnt(0)
                .build();
        mapper.insert(q);
    }

    public static boolean isFault(String errCd) {
        if (errCd == null) return false;
        String c = errCd.trim();
        return !c.isEmpty() && !NORMAL_CODES.contains(c);
    }
}
