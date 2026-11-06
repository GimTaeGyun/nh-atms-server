package kr.co.nh.atms.journal.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 저널 DB 적재 성공 후 Kafka 로 이벤트 발행 (fire-and-forget).
 *
 * 원칙:
 *  - 수신 경로 무영향: 발행 실패는 로그만 남기고 절대 예외를 전파하지 않는다.
 *    (저널 원본은 이미 T_JOURNAL_DATA 에 저장되어 있으므로 이벤트 유실 시 재색인으로 복구 가능)
 *  - atms.stream.kafka.enabled=false(기본)면 즉시 no-op — 로컬/기존 환경 동작 불변.
 *  - 메시지 key = termGroupId:termId → 같은 기기의 저널은 같은 파티션(순서 보장).
 */
@Slf4j
@Component
public class JournalEventPublisher {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JournalStreamProperties props;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    public JournalEventPublisher(JournalStreamProperties props,
                                 ObjectMapper objectMapper,
                                 ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.kafkaTemplateProvider = kafkaTemplateProvider;
    }

    public void publishStored(JifHeadKey key, JifMessage msg) {
        if (!props.getKafka().isEnabled()) {
            return;
        }
        KafkaTemplate<String, String> template = kafkaTemplateProvider.getIfAvailable();
        if (template == null) {
            return;
        }
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("termGroupId", key.getTermGroupId());
            event.put("termId", key.getTermId());
            event.put("jnlDate", key.getJnlDate());
            event.put("jnlTime", key.getJnlTime());
            event.put("jnlSeq", key.getJnlSeq());
            event.put("jnlKind", key.getJnlKind());
            event.put("receivedAt", LocalDateTime.now().format(ISO));
            event.put("head", msg.getHead());
            event.put("body", msg.getBody());

            String kafkaKey = key.getTermGroupId() + ":" + key.getTermId();
            String payload = objectMapper.writeValueAsString(event);

            template.send(props.getKafka().getTopic(), kafkaKey, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("journal event publish failed key={} kind={}: {}",
                                    kafkaKey, key.getJnlKind(), ex.toString());
                        }
                    });
        } catch (Exception e) {
            log.warn("journal event publish skipped: {}", e.toString());
        }
    }
}
