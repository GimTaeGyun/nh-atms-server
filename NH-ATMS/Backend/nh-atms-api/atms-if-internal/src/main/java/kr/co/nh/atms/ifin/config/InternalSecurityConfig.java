package kr.co.nh.atms.ifin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * atms-if-internal 모듈 보안 설정.
 * /api/if/internal/** 은 내부망(다른 ATMS 모듈 → atmi) 전용 채널이므로
 * securityMatcher 로 분리하여 permitAll 처리한다.
 * 그 외 요청은 atms-common 의 SecurityConfig(JWT 체인)가 담당.
 */
@Configuration
public class InternalSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain internalIfSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/if/internal/**", "/actuator/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable());
        return http.build();
    }
}
