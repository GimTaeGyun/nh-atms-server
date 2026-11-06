package kr.co.nh.atms.journal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * JIF head 에서 추출한 공통 키. 모든 jnl_kind 분기/저장에 사용.
 *  - atm_id (20byte) = termGroupId(5) + termId(3) + filler
 *  - jnl_time 10byte = HHMMSSFFF + flag(1)
 *  - jnl_date 8byte  = YYYYMMDD
 */
@Data
@AllArgsConstructor
public class JifHeadKey {
    private final String termGroupId;   // 5
    private final String termId;        // 3 (또는 그 이상)
    private final String jnlDate;       // 8
    private final String jnlTime;       // 10
    private final String jnlKind;       // 2
    private final String jnlSeq;        // 5
    private final String bankCd;        // 3
    private final String atmIdRaw;      // 20

    /** atm_id 그대로 보고 싶을 때 (T_JOURNAL_ERROR.atmId 컬럼용) */
    public String atmIdForError() {
        return atmIdRaw != null ? atmIdRaw : (termGroupId + "/" + termId);
    }
}
