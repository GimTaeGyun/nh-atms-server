package kr.co.nh.atms.file.image.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * atms-file 모듈 보안 설정.
 * 이미지 업/다운로드(/api/file/**)는 내부망 Agent/운영 채널이라 permitAll.
 * 원격명령 MIF push(/atmf/**)는 atms-web/기기 간 내부 채널이라 permitAll.
 * 그 외는 atms-common 기본 체인(JWT)이 처리.
 */
@Configuration
public class FileSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain fileSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/file/**", "/atmf/**", "/actuator/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable());
        return http.build();
    }
}
