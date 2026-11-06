package kr.co.nh.atms.journal.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JournalReceiveResult {
    private boolean accepted;     // 200 받았는가
    private boolean parsed;       // 헤더 파싱 성공
    private boolean stored;       // 본문 저장 성공
    private String jnlKind;
    private String reason;        // 실패 시 사유
    private Long errorSeq;        // T_JOURNAL_ERROR.seq (저장됐다면)
}
