package kr.co.nh.atms.ifin.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 대내 연계 게이트웨이 빈 설정.
 * internal.gateway.mode=eai 로 EaiInternalGateway 빈이 등록되면 자동 대체되고,
 * 그 외에는 {@link MockInternalGateway} 를 기본 등록한다.
 * (@ConditionalOnMissingBean 은 @Component 가 아닌 @Bean 메서드에서만 신뢰성 있게 동작)
 */
@Configuration
public class GatewayConfig {

    @Bean
    @ConditionalOnMissingBean(InternalGateway.class)
    public InternalGateway internalGateway(GwInterfaceRecorder recorder) {
        return new MockInternalGateway(recorder);
    }
}
