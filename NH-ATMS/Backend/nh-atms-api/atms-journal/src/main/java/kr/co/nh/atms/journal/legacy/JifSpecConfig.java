package kr.co.nh.atms.journal.legacy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * JIF 전문 스펙 로딩 — AS-IS interface.journal.json 원본을 classpath 에서 로드.
 */
@Configuration
public class JifSpecConfig {

    @Bean
    public JifSpecConverter jifSpecConverter() throws IOException {
        JifSpecConverter converter = new JifSpecConverter();
        converter.load("#jif#", "jif/interface.journal.json");
        return converter;
    }
}
