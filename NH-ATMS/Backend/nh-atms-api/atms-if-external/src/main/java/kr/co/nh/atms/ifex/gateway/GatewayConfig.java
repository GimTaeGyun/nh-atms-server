package kr.co.nh.atms.ifex.gateway;

import kr.co.nh.atms.ifex.gateway.eif.EifExternalGateway;
import kr.co.nh.atms.ifex.gateway.eif.EifProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 대외 연계 게이트웨이 빈 설정.
 *
 * <ul>
 *   <li>external.gateway.mode=eif : {@link EifExternalGateway} (고정길이 전문 TCP 실연동)</li>
 *   <li>그 외(기본) : {@link MockExternalGateway}</li>
 * </ul>
 * (@ConditionalOnMissingBean 은 @Component 가 아닌 @Bean 메서드에서만 신뢰성 있게 동작 —
 *  eif 빈 선언을 mock 보다 먼저 두어 조건 평가 순서를 보장한다)
 */
@Configuration
@EnableConfigurationProperties(EifProperties.class)
public class GatewayConfig {

    @Bean
    @ConditionalOnProperty(name = "external.gateway.mode", havingValue = "eif")
    public ExternalGateway eifExternalGateway(EifProperties props) {
        return new EifExternalGateway(props);
    }

    @Bean
    @ConditionalOnMissingBean(ExternalGateway.class)
    public ExternalGateway externalGateway() {
        return new MockExternalGateway();
    }
}
