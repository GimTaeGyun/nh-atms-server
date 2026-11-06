package kr.co.nh.atms.journal.config;

import kr.co.nh.atms.journal.security.HmacAuthFilter;
import kr.co.nh.atms.journal.security.JournalHmacProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * atms-journal 모듈 보안 설정.
 * 수신 엔드포인트(/api/journal/**)는 내부망 Agent → ATMS 채널이라
 * Spring Security 인증 대신 HMAC-SHA256(JournalHmacProperties) 으로 검증한다.
 * dev 에서는 journal.hmac.enabled=false → 무조건 통과.
 *
 * securityMatcher 로 신규(/api/journal·/api/agent) + 레거시 에이전트 .do 경로 + actuator 만 처리.
 * /api/agent/** 와 레거시 /atms|/atmr/agent/**·/atms/remote-exec/** 는 ATM 에이전트
 * 연결관리(open/exit/ping/저널모드) 채널로 내부망 전용 → permitAll.
 * (실 에이전트는 수정 불가하며 form-urlencoded 로 /atms|/atmr/agent/*.do 를 호출한다.)
 * HMAC 필터는 /api/journal 에만 적용(shouldNotFilter)되므로 agent 는 토큰/HMAC 없이 통과.
 * 그 외 요청은 atms-common 의 SecurityConfig (JWT 체인) 가 처리.
 */
@Configuration
public class JournalSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain journalSecurity(HttpSecurity http,
                                               JournalHmacProperties hmacProps) throws Exception {
        http
            .securityMatcher("/api/journal/**", "/api/agent/**",
                             "/atms/agent/**", "/atmr/agent/**", "/atms/remote-exec/**",
                             "/actuator/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .addFilterBefore(new HmacAuthFilter(hmacProps),
                             UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

