package kr.co.nh.atms.journal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * IF 큐 워커가 atme/atmi 로 호출할 때 사용할 RestClient 빈.
 */
@Configuration
public class IfQueueClientConfig {

    @Bean
    public RestClient ifQueueRestClient() {
        return RestClient.builder().build();
    }
}
