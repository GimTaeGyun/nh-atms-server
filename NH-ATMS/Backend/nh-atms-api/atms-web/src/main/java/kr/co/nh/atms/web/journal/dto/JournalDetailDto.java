package kr.co.nh.atms.web.journal.dto;

import kr.co.nh.atms.web.journal.entity.Journal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 통합 저널조회 상세 응답.
 *   - journal      : T_JOURNAL 헤더
 *   - journalText  : T_JOURNAL_DATA.JNL_DATA (원본 JSON CLOB, dispatcher 가 자동 적재)
 * 거래/운영 상세는 각 전용 페이지(/journal/transaction, /journal/operation)에서 확인.
 */
@Data
@Builder
@AllArgsConstructor
public class JournalDetailDto {
    private Journal journal;
    private String journalText;
}
