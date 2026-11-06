package kr.co.nh.atms.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * ATMS File (파일 송수신/펌웨어 다운로드) Spring Boot Application.
 * 구 atmf 모듈에 해당. 포트: 8083 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /file)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
@ConfigurationPropertiesScan(basePackages = "kr.co.nh.atms.file")
public class AtmsFileApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsFileApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsFileApplication.class, args);
    }
}
