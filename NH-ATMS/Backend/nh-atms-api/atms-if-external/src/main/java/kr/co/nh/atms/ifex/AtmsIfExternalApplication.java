package kr.co.nh.atms.ifex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * ATMS IF-External (대외 인터페이스) Spring Boot Application.
 * 구 atme 모듈에 해당. API GW / FOS / 금결원 / 카드사 등.
 * 포트: 8085 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /ifex)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
public class AtmsIfExternalApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsIfExternalApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsIfExternalApplication.class, args);
    }
}
