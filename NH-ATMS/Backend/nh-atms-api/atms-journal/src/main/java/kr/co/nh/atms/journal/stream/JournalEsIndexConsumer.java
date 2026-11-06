package kr.co.nh.atms.journal.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka 저널 이벤트 → Elasticsearch 일별 인덱스 색인.
 *
 *  - 인덱스: {prefix}-yyyy.MM.dd (저널 거래일 기준 → 보존주기/삭제를 인덱스 단위로 관리)
 *  - 문서 ID: termGroupId-termId-jnlDate-jnlTime-jnlSeq → 재전송/재처리 시에도 중복 없이 멱등
 *  - 색인 실패 시 예외 전파 → spring-kafka DefaultErrorHandler 가 재시도 후 스킵(로그)
 *  - ES 는 검색/대시보드 용도 사본. 원본은 T_JOURNAL_DATA — 유실 시 재색인으로 복구
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "atms.stream.elasticsearch.enabled", havingValue = "true")
public class JournalEsIndexConsumer {

    private final JournalStreamProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient esRestClient;

    @KafkaListener(
            topics = "${atms.stream.kafka.topic:atms.journal.received}",
            groupId = "atms-journal-es-indexer",
            concurrency = "${atms.stream.elasticsearch.consumer-concurrency:3}"
    )
    public void onJournalEvent(String payload) throws Exception {
        Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});

        String jnlDate = str(event, "jnlDate");
        String docId = String.join("-",
                str(event, "termGroupId"), str(event, "termId"),
                jnlDate, str(event, "jnlTime"), str(event, "jnlSeq"));

        Map<String, Object> doc = new LinkedHashMap<>(event);
        // Kibana 전문검색용 — head/body 전체를 text 필드로도 보존
        doc.put("searchText", payload);

        Request request = new Request("PUT", "/" + indexName(jnlDate) + "/_doc/" + docId);
        request.setEntity(new NStringEntity(objectMapper.writeValueAsString(doc), ContentType.APPLICATION_JSON));
        esRestClient.performRequest(request);
    }

    private String indexName(String jnlDate) {
        String suffix;
        if (jnlDate != null && jnlDate.length() == 8) {
            suffix = jnlDate.substring(0, 4) + "." + jnlDate.substring(4, 6) + "." + jnlDate.substring(6, 8);
        } else {
            suffix = "unknown";
        }
        return props.getElasticsearch().getIndexPrefix() + "-" + suffix;
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : String.valueOf(v);
    }
}
