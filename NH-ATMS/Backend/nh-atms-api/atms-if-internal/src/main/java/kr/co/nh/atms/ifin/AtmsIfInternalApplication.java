package kr.co.nh.atms.ifin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * ATMS IF-Internal (대내 인터페이스) Spring Boot Application.
 * 구 atmi 모듈에 해당. EAI / 계정계 / SMS / 메신저 등.
 * 포트: 8084 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /ifin)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
@ConfigurationPropertiesScan(basePackages = "kr.co.nh.atms.ifin")
public class AtmsIfInternalApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsIfInternalApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsIfInternalApplication.class, args);
    }
}
