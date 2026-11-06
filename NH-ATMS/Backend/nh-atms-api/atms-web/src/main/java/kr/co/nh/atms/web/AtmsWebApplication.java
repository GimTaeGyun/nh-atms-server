package kr.co.nh.atms.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * ATMS Web (화면 백엔드) Spring Boot Application.
 * 포트: 8080 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /, 다른 모듈로 가는 입구 게이트웨이 겸함)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
public class AtmsWebApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsWebApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsWebApplication.class, args);
    }
}
