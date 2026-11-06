package kr.co.nh.atms.journal.stream;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 저널 스트리밍(Kafka → Elasticsearch 색인) 설정.
 *
 * atms.stream.kafka.enabled          : 저널 수신 이벤트 Kafka 발행 여부 (기본 false — 미설정 시 기존 동작과 완전 동일)
 * atms.stream.kafka.topic            : 발행 토픽
 * atms.stream.kafka.partitions       : 토픽 파티션 수 (기기별 key 해싱 — 5,500대 기준 12 권장)
 * atms.stream.elasticsearch.enabled  : ES 색인 컨슈머 활성화 여부 (kafka.enabled 전제)
 * atms.stream.elasticsearch.index-prefix : 일별 인덱스 접두어 (예: atms-journal-2026.07.09)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "atms.stream")
public class JournalStreamProperties {

    private final Kafka kafka = new Kafka();
    private final Elasticsearch elasticsearch = new Elasticsearch();

    @Getter
    @Setter
    public static class Kafka {
        private boolean enabled = false;
        private String topic = "atms.journal.received";
        private int partitions = 12;
        private short replicas = 1;
    }

    @Getter
    @Setter
    public static class Elasticsearch {
        private boolean enabled = false;
        private String indexPrefix = "atms-journal";
    }
}
