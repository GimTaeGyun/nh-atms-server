package kr.co.nh.atms.journal.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * /api/journal/** 수신 엔드포인트 HMAC 인증 설정.
 *
 *   journal.hmac.enabled            : 활성 여부 (dev=false / staging,prod=true)
 *   journal.hmac.secret             : 공유 비밀 (env: JOURNAL_HMAC_SECRET)
 *   journal.hmac.key-id             : 키 식별자 (요청 헤더 X-Atms-KeyId 검증)
 *   journal.hmac.clock-skew-seconds : 허용 시계 오차 (재전송 공격 방지, default 300)
 */
@Data
@ConfigurationProperties(prefix = "journal.hmac")
public class JournalHmacProperties {
    private boolean enabled = false;
    private String secret = "";
    private String keyId = "default";
    private long clockSkewSeconds = 300;
}
