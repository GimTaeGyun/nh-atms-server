package kr.co.nh.atms.ifex.gateway;

import lombok.Getter;

/**
 * 대외 송신 결과 (수신전문 포함).
 */
@Getter
public class GatewayResult {
    private final String resultCd;
    private final String resultMsg;
    private final String recvMsg;
    /** 실제 송신된 원시 전문 (고정길이 패킷 문자열). Mock 은 null. */
    private final String rawSendMsg;

    public GatewayResult(String resultCd, String resultMsg, String recvMsg) {
        this(resultCd, resultMsg, recvMsg, null);
    }

    public GatewayResult(String resultCd, String resultMsg, String recvMsg, String rawSendMsg) {
        this.resultCd = resultCd;
        this.resultMsg = resultMsg;
        this.recvMsg = recvMsg;
        this.rawSendMsg = rawSendMsg;
    }

    public boolean isSuccess() {
        return "0000".equals(resultCd);
    }
}
