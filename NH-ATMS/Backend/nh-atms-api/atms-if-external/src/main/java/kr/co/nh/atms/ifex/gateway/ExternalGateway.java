package kr.co.nh.atms.ifex.gateway;

/**
 * 대외(은행 API GW / 장애처리업체 EAI) 연계 게이트웨이.
 * 실제 통신(고정길이 전문 송수신)은 구현체에서 담당하며,
 * 개발/테스트에서는 {@link MockExternalGateway} 가 더미 수신전문을 반환한다.
 */
public interface ExternalGateway {

    /**
     * 대외 전문 송신.
     * @param globalId 글로벌ID(32자)
     * @param trCode   거래코드(EM_01001110 등)
     * @param ifType   IF유형
     * @param sendMsg  송신전문(JSON/고정길이)
     * @return 송신 결과 + 수신전문
     */
    GatewayResult send(String globalId, String trCode, String ifType, String sendMsg);
}
