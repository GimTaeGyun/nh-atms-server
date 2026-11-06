package kr.co.nh.atms.journal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ATMS Journal (전자저널) Spring Boot Application.
 * 구 atmr 모듈에 해당. 포트: 8081 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /journal)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
@ConfigurationPropertiesScan(basePackages = "kr.co.nh.atms.journal")
@EnableScheduling
public class AtmsJournalApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsJournalApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsJournalApplication.class, args);
    }
}
