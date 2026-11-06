package kr.co.nh.atms.journal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 저널 에러 재처리 워커 설정 (atmr 1-E).
 * journal.error-retry.enabled    : 스케줄러 활성화 여부 (dev 기본 off)
 * journal.error-retry.fixed-delay: 폴링 주기(ms)
 * journal.error-retry.batch-size : 1회 처리 건수
 * journal.error-retry.max-retry  : 최대 재시도 횟수(초과 시 STATUS='F')
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "journal.error-retry")
public class JournalErrorRetryProperties {
    private boolean enabled = false;
    private int batchSize = 50;
    private int maxRetry = 3;
}
