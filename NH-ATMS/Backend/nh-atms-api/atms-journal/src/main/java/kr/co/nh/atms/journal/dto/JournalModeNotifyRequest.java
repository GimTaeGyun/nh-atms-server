package kr.co.nh.atms.journal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 저널 모드 변경 통보 (계약: JournalModeNotify, 2.3.11).
 * 에이전트가 종이/전자 저널 모드 전환 시 서버로 통보.
 */
@Data
public class JournalModeNotifyRequest {

    @NotBlank
    private String atmId;        // 8자리

    /** 저널 모드. */
    @NotBlank
    private String jnlmode;

    /** 사유: HDD_FULL/WRITE_ERROR/MANUAL/REMOTE_CMD. */
    private String reason;

    /** 발생시각 ISO-8601 (없으면 서버시각). */
    private String occurredAt;
}
