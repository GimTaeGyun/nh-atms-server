package kr.co.nh.atms.web.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 모듈 게이트웨이 등록 (gateway.enabled=true 일 때만: JEUS 프로파일 jeus, 로컬 프로파일 local).
 * Spring Security 필터 체인(-100)보다 먼저 실행되도록 가장 앞 순서로 등록한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "gateway", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ModuleGatewayProperties.class)
public class ModuleGatewayConfig {

    @Bean
    public FilterRegistrationBean<ModuleGatewayFilter> moduleGatewayFilter(ModuleGatewayProperties props) {
        FilterRegistrationBean<ModuleGatewayFilter> reg = new FilterRegistrationBean<>(new ModuleGatewayFilter(props));
        reg.setName("moduleGatewayFilter");
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
