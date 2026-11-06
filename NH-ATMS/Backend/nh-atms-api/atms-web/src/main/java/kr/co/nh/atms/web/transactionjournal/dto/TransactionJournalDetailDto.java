package kr.co.nh.atms.web.transactionjournal.dto;

import kr.co.nh.atms.web.journalattachment.entity.JournalImage;
import kr.co.nh.atms.web.transactionjournal.entity.TransactionJournal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TransactionJournalDetailDto {
    private TransactionJournal journal;
    private String journalText;
    private List<JournalImage> images;
}
