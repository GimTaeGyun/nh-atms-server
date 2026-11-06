package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_JOURNAL_DATA — 모든 jnl_kind 공통 본문 텍스트 적재 (CLOB).
 * dispatcher 가 매 메시지마다 1건 INSERT (idempotent).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalData {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String jnlData; // JSON 직렬화된 head+body
}
