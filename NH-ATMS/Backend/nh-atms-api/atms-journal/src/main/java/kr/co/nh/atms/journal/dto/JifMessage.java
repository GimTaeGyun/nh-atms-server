package kr.co.nh.atms.journal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Agent → ATMS 로 들어오는 JIF 메시지(JSON).
 * 09_ATMR_저널프로토콜_분석.md §3 의 구조를 따른다:
 *   {
 *     "head": { yyyy, mm, dd, hh, mi, ss, fff, flag, bk_cd, atm_id,
 *               jnl_seq, jnl_kind, idx_enc, jnl_enc, enc_key_ver, ... },
 *     "body": { ... jnl_kind 별 스펙 ... },
 *     "extra": { ... 선택 ... }
 *   }
 *
 * body / extra 는 jnl_kind 별로 스키마가 다르므로 Map 으로 받아 핸들러에서 분기.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JifMessage {
    private Map<String, Object> head;
    private Map<String, Object> body;
    private Map<String, Object> extra;
}
