package kr.co.nh.atms.web.operationjournal.dto;

import kr.co.nh.atms.web.operationjournal.entity.OperationJournal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class OperationJournalDetailDto {
    private OperationJournal journal;
    private String journalText; // T_JOURNAL_DATA.JNL_DATA
}
