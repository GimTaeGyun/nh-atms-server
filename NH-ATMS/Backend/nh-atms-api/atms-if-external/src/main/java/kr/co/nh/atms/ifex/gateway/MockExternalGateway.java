package kr.co.nh.atms.ifex.gateway;

import lombok.extern.slf4j.Slf4j;

/**
 * 대외 연계 Mock 구현 — 실제 전문 송수신 없이 정상 수신전문을 흉내낸다.
 * 빈 등록은 {@link GatewayConfig} 가 담당하며, 실 게이트웨이를 빈으로 등록하면 자동 대체된다.
 */
@Slf4j
public class MockExternalGateway implements ExternalGateway {

    private static final String OK = "0000";

    @Override
    public GatewayResult send(String globalId, String trCode, String ifType, String sendMsg) {
        log.info("[MOCK-EXT] 송신 globalId={} tr={} type={} msg={}", globalId, trCode, ifType, sendMsg);
        String recvMsg = "{\"globalId\":\"" + globalId + "\",\"trCode\":\"" + trCode
                + "\",\"resultCd\":\"" + OK + "\",\"resultMsg\":\"정상수신\"}";
        return new GatewayResult(OK, "정상", recvMsg);
    }
}
