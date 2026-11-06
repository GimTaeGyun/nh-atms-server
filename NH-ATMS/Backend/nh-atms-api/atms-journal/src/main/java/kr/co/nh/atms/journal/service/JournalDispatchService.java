package kr.co.nh.atms.journal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.dto.JournalReceiveResult;
import kr.co.nh.atms.journal.entity.Journal;
import kr.co.nh.atms.journal.entity.JournalData;
import kr.co.nh.atms.journal.entity.JournalError;
import kr.co.nh.atms.journal.handler.JournalHandler;
import kr.co.nh.atms.journal.mapper.AgentStatusMapper;
import kr.co.nh.atms.journal.mapper.JournalDataWriteMapper;
import kr.co.nh.atms.journal.mapper.JournalErrorMapper;
import kr.co.nh.atms.journal.mapper.JournalMapper;
import kr.co.nh.atms.journal.parser.JifFieldUtils;
import kr.co.nh.atms.journal.stream.JournalEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 수신된 JIF 메시지 1건을 처리:
 *   1) head 파싱 → JifHeadKey
 *   2) T_JOURNAL 헤더 INSERT
 *   3) jnl_kind 매칭 핸들러 호출 (T_ATM_MONITOR / T_TRANSACTION_JOURNAL / ...)
 *   4) 어느 단계든 실패 시 T_JOURNAL_ERROR 로 raw JSON 보존 (NEW 트랜잭션)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalDispatchService {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("HHmmssSSS");

    private final List<JournalHandler> handlers;
    private final JournalMapper journalMapper;
    private final JournalDataWriteMapper journalDataWriteMapper;
    private final JournalErrorMapper errorMapper;
    private final AgentStatusMapper agentStatusMapper;
    private final ObjectMapper objectMapper;
    private final JournalEventPublisher eventPublisher;
    private final PlatformTransactionManager txManager;

    private final Map<String, JournalHandler> handlerIndex = new HashMap<>();

    // 자기호출(this.doDispatchInTx) 시 @Transactional 프록시가 적용되지 않아
    // 문장별 autocommit으로 동작하던 결함(2026-07-16 실증) 교정 — 명시적 TransactionTemplate 사용.
    private TransactionTemplate txRequired;
    private TransactionTemplate txRequiresNew;

    @Value("${journal.receive.unknown-kind-policy:SKIP}")
    private String unknownKindPolicy;

    @PostConstruct
    void buildIndex() {
        for (JournalHandler h : handlers) {
            for (String k : h.supportedKinds()) {
                handlerIndex.put(k, h);
            }
        }
        log.info("JournalDispatch handlers registered: {}", handlerIndex.keySet());

        txRequired = new TransactionTemplate(txManager);
        txRequiresNew = new TransactionTemplate(txManager);
        txRequiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 1건 처리 진입점. 예외 던지지 않음(에러도 T_JOURNAL_ERROR 로 흡수).
     */
    public JournalReceiveResult dispatch(JifMessage msg) {
        JifHeadKey key;
        try {
            key = JifFieldUtils.parseHead(msg);
        } catch (Exception e) {
            Long seq = saveError(null, null, null, null, null, msg, e.getMessage());
            return JournalReceiveResult.builder()
                    .accepted(true).parsed(false).stored(false)
                    .reason("head parse failed: " + e.getMessage())
                    .errorSeq(seq)
                    .build();
        }

        try {
            txRequired.executeWithoutResult(tx -> doDispatchInTx(key, msg));
            // DB 적재 성공 후에만 스트림 발행(ES 색인용) — 발행 실패는 수신 응답에 영향 없음
            eventPublisher.publishStored(key, msg);
            return JournalReceiveResult.builder()
                    .accepted(true).parsed(true).stored(true)
                    .jnlKind(key.getJnlKind())
                    .build();
        } catch (Exception e) {
            log.warn("dispatch failed kind={} term={}/{} time={}: {}",
                    key.getJnlKind(), key.getTermGroupId(), key.getTermId(),
                    key.getJnlTime(), e.toString());
            Long seq = saveError(key.getJnlDate(), key.getJnlTime(),
                    key.atmIdForError(), key.getJnlSeq(), key.getJnlKind(),
                    msg, e.getMessage());
            return JournalReceiveResult.builder()
                    .accepted(true).parsed(true).stored(false)
                    .jnlKind(key.getJnlKind())
                    .reason(e.getMessage())
                    .errorSeq(seq)
                    .build();
        }
    }

    /**
     * 재처리(atmr 1-E): 보관된 JIF 메시지를 다시 파싱·디스패치한다.
     * 성공 시 정상 적재되고, 실패 시 예외를 그대로 던진다(호출자가 상태 관리).
     * dispatch() 와 달리 실패해도 T_JOURNAL_ERROR 를 새로 적재하지 않는다.
     */
    @Transactional
    public void reprocess(JifMessage msg) {
        JifHeadKey key = JifFieldUtils.parseHead(msg);
        doDispatchInTx(key, msg);
    }

    // 트랜잭션 경계는 호출측이 보장: dispatch()는 txRequired(TransactionTemplate),
    // reprocess()는 @Transactional(외부 호출 → 프록시 적용). 여기에 @Transactional을 다시 달지 말 것 —
    // 자기호출이라 프록시가 적용되지 않는다(2026-07-16 로그 실증: synchronization is not active).
    protected void doDispatchInTx(JifHeadKey key, JifMessage msg) {
        LocalDateTime now = LocalDateTime.now();
        journalMapper.insert(Journal.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .agentType(JifFieldUtils.s(msg.getHead(), "agent_type"))
                .seqNo(key.getJnlSeq())
                .jnlTypeCd(key.getJnlKind())
                .jnlWriteTypeCd(JifFieldUtils.s(msg.getHead(), "jnl_enc"))
                .recvDate(now.format(D))
                .recvTime(now.format(T) + "0")
                .build());

        // 모든 jnl_kind 공통: 원본 메시지(head+body) JSON 을 T_JOURNAL_DATA 에 보존
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            rawJson = "(serialize failed: " + e.getMessage() + ")";
        }
        journalDataWriteMapper.insert(JournalData.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .jnlData(rawJson)
                .build());

        // 공통: 기기 최종 저널 수신 일시 갱신(atmr update_T_AGENTSTATUS 대응). 등록 전 기기면 0건(무해).
        agentStatusMapper.updateRecvOnJournal(
                key.getTermGroupId(), key.getTermId(), key.getJnlDate(), key.getJnlTime());

        JournalHandler h = handlerIndex.get(key.getJnlKind());
        if (h == null) {
            if ("ERROR".equalsIgnoreCase(unknownKindPolicy)) {
                throw new IllegalStateException("no handler for jnl_kind=" + key.getJnlKind());
            }
            log.info("skip unknown jnl_kind={} (policy=SKIP)", key.getJnlKind());
            return;
        }
        h.handle(key, msg);
    }

    /** 어떤 트랜잭션이 실패해도 별도 트랜잭션(REQUIRES_NEW)으로 무조건 기록 — 자기호출이므로 TransactionTemplate 사용. */
    protected Long saveError(String jnlDate, String jnlTime, String atmId,
                             String jnlSeq, String jnlKind,
                             JifMessage msg, String errMsg) {
        String json;
        try {
            json = objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            json = "(serialize failed: " + e.getMessage() + ")";
        }
        JournalError e = JournalError.builder()
                .jnlDate(jnlDate)
                .jnlTime(jnlTime)
                .atmId(atmId)
                .jnlSeq(jnlSeq)
                .jnlKind(jnlKind)
                .jnlJson(json)
                .errMsg(errMsg)
                .build();
        return txRequiresNew.execute(tx -> {
            errorMapper.insert(e);
            return e.getSeq();
        });
    }
}
