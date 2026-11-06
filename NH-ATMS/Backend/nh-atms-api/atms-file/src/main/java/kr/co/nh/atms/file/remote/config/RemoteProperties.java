package kr.co.nh.atms.file.remote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 원격제어(MIF push) 관련 설정.
 * 구 atmf config.json (agent.remote.port / agent.msg.async.resp.forward) 이식.
 */
@ConfigurationProperties(prefix = "atmf.remote")
public class RemoteProperties {

    /** 기기측 AP(원격명령 수신서버) 접속 포트. 구 atmf agent.remote.port = 21001. */
    private int agentRemotePort = 21001;

    /** 소켓 연결 타임아웃(ms). */
    private int connectTimeoutMs = 1000;

    /** 비동기 결과 forward 대상(atms-web). 구 atmf agent.msg.async.resp.forward. */
    private String msgAsyncRespForwardUri = "http://localhost:8080/atms/remote-exec/put.msg.resp.do";

    /** MIF 스펙(interface.message.json) classpath 위치. */
    private String specLocation = "remote/interface.message.json";

    /** MIF 인터페이스명(스펙 루트 키). */
    private String interfaceName = "#mif#";

    public int getAgentRemotePort() { return agentRemotePort; }
    public void setAgentRemotePort(int agentRemotePort) { this.agentRemotePort = agentRemotePort; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public String getMsgAsyncRespForwardUri() { return msgAsyncRespForwardUri; }
    public void setMsgAsyncRespForwardUri(String msgAsyncRespForwardUri) { this.msgAsyncRespForwardUri = msgAsyncRespForwardUri; }

    public String getSpecLocation() { return specLocation; }
    public void setSpecLocation(String specLocation) { this.specLocation = specLocation; }

    public String getInterfaceName() { return interfaceName; }
    public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
}
