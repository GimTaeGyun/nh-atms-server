package kr.co.nh.atms.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ATMS Batch (배치) Spring Boot Application.
 * 구 atmb 모듈에 해당. 포트: 8082 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /batch)
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
public class AtmsBatchApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsBatchApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsBatchApplication.class, args);
    }
}
