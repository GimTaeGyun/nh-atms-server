package kr.co.nh.atms.journal.stream;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 저널 토픽 자동 생성 (KafkaAdmin 이 브로커에 idempotent 하게 반영).
 * 파티션 키 = termGroupId:termId — 기기 단위 순서 보장.
 */
@Configuration
@ConditionalOnProperty(value = "atms.stream.kafka.enabled", havingValue = "true")
public class JournalStreamTopicConfig {

    @Bean
    public NewTopic journalReceivedTopic(JournalStreamProperties props) {
        return TopicBuilder.name(props.getKafka().getTopic())
                .partitions(props.getKafka().getPartitions())
                .replicas(props.getKafka().getReplicas())
                // 저널 원본은 DB(T_JOURNAL_DATA)가 보관 — 토픽은 색인/재처리 버퍼 용도로 3일 보존
                .config("retention.ms", String.valueOf(3L * 24 * 60 * 60 * 1000))
                .build();
    }
}
