package kr.co.nh.atms.web.agentstatus.entity;

import lombok.Data;

/**
 * T_AGENT_STATUS (ATM 에이전트 상태 마스터) 조회 전용 엔티티.
 * 레거시 새마을금고 기준 기기개국(기동)시간 / 접속상태 조회에 사용.
 */
@Data
public class AgentStatus {
    private String atmIp;          // 기기 IP (PK)
    private String agentType;      // 에이전트 구분
    private String termGroupId;    // 영업점코드
    private String termId;         // 단말번호
    private String connectFlag;    // 접속여부 (1=접속, 0=해제)
    private String agentVer;       // 에이전트 버전
    private String apDownVer;      // AP 버전
    private String osName;         // OS 정보
    private String atmMacAddr;     // MAC 주소
    private String connectIp;      // 접속 IP
    private String connectPort;    // 접속 PORT
    private String jnlMode;        // 저널 모드
    private String pollCycle;      // 폴링 주기(초)
    private String startDate;      // 기동 일자 (yyyyMMdd)
    private String startTime;      // 기동 시각 (HHmmss)
    private String lastPollDate;   // 최종 폴링 일자
    private String lastPollTime;   // 최종 폴링 시각
    private String exitDate;       // 종료 일자
    private String exitTime;       // 종료 시각
}
