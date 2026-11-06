package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_JOURNAL_ERROR — 파싱/처리 실패 시 raw JSON 보관.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalError {
    private Long seq;            // SEQ_T_JOURNAL_ERROR
    private String jnlDate;
    private String jnlTime;
    private String atmId;
    private String jnlSeq;
    private String jnlKind;
    private String jnlJson;
    private String errMsg;
    private LocalDateTime regDt;
    private String status;          // E=대기, R=성공, F=영구실패
    private Integer retryCnt;
    private LocalDateTime lastRetryDt;
}
