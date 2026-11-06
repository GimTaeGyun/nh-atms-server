package kr.co.nh.atms.journal.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 기동 시 저널 인덱스 템플릿을 idempotent 하게 등록.
 * 일별 인덱스({prefix}-yyyy.MM.dd)가 생성될 때 이 매핑이 자동 적용된다.
 * ES 미기동 상태여도 앱 기동은 막지 않는다 (경고 로그 후 계속 — 색인 시점에 재시도됨).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "atms.stream.elasticsearch.enabled", havingValue = "true")
public class JournalEsIndexInitializer implements ApplicationRunner {

    private final JournalStreamProperties props;
    private final RestClient esRestClient;

    @Override
    public void run(ApplicationArguments args) {
        String prefix = props.getElasticsearch().getIndexPrefix();
        String template = """
            {
              "index_patterns": ["%s-*"],
              "template": {
                "settings": {
                  "number_of_shards": 1,
                  "number_of_replicas": "${ES_INDEX_REPLICAS:-0}"
                },
                "mappings": {
                  "properties": {
                    "termGroupId": { "type": "keyword" },
                    "termId":      { "type": "keyword" },
                    "jnlDate":     { "type": "keyword" },
                    "jnlTime":     { "type": "keyword" },
                    "jnlSeq":      { "type": "keyword" },
                    "jnlKind":     { "type": "keyword" },
                    "receivedAt":  { "type": "date", "format": "date_hour_minute_second_fraction||date_hour_minute_second||strict_date_optional_time" },
                    "head":        { "type": "flattened" },
                    "body":        { "type": "flattened" },
                    "searchText":  { "type": "text" }
                  }
                }
              },
              "priority": 100
            }
            """.formatted(prefix)
            .replace("\"${ES_INDEX_REPLICAS:-0}\"", "0");

        try {
            Request request = new Request("PUT", "/_index_template/" + prefix);
            request.setEntity(new NStringEntity(template, ContentType.APPLICATION_JSON));
            esRestClient.performRequest(request);
            log.info("ES index template [{}] registered", prefix);
        } catch (Exception e) {
            log.warn("ES index template registration failed (색인 시점에 동적 매핑으로 동작, ES 기동 후 재기동 권장): {}", e.toString());
        }
    }
}
