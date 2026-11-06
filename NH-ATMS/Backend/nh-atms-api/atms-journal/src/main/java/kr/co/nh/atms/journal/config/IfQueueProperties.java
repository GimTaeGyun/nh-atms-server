package kr.co.nh.atms.journal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * IF 큐 워커 설정.
 * if.queue.worker.enabled    : 스케줄러 활성화 여부
 * if.queue.worker.batch-size : 한 번에 처리할 큐 건수
 * if.queue.worker.max-retry  : 최대 재시도 횟수(초과 시 F)
 * if.queue.internal-base-url : atms-if-internal 베이스 URL
 * if.queue.external-base-url : atms-if-external 베이스 URL
 * if.queue.notify.sms-phones      : 장애 SMS 기본 수신 전화번호 목록
 * if.queue.notify.messenger-users : 장애 메신저(MGenie) 기본 수신 사용자 목록
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "if.queue")
public class IfQueueProperties {
    private Worker worker = new Worker();
    private Notify notify = new Notify();
    private String internalBaseUrl = "http://localhost:8084";
    private String externalBaseUrl = "http://localhost:8085";

    @Getter
    @Setter
    public static class Worker {
        private boolean enabled = false;
        private int batchSize = 20;
        private int maxRetry = 3;
    }

    /**
     * 장애 통보 기본 수신처.
     * 운영자/담당자 매핑 규칙(T_USER 계열 조회)은 NH 요건 확정 전이므로
     * 우선 설정 주입 구조만 제공한다 — 미설정 시 아래 placeholder 기본값.
     */
    @Getter
    @Setter
    public static class Notify {
        /** 장애 SMS 수신 전화번호 목록 (기본: 운영 장애대응 데스크 placeholder). */
        private List<String> smsPhones = new ArrayList<>(List.of("0000000000"));
        /** 장애 메신저 수신 사용자ID 목록 (기본: placeholder). */
        private List<String> messengerUsers = new ArrayList<>(List.of("ATMS_OP"));
    }
}
