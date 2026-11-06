package kr.co.nh.atms.journal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 원격제어 명령 큐 타임아웃 워커 설정.
 * agent.command.worker.enabled    : 스케줄러 활성화 여부(기본 true)
 * agent.command.worker.batch-size : 한 번에 처리할 타임아웃 건수
 * agent.command.worker.fixed-delay: 폴링 주기(ms, @Scheduled 기본 30s)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "agent.command.worker")
public class AgentCommandProperties {
    private boolean enabled = true;
    private int batchSize = 50;
}
