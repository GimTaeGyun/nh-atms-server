package kr.co.nh.atms.web.transactionjournal.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.journalattachment.mapper.JournalDataMapper;
import kr.co.nh.atms.web.journalattachment.mapper.JournalImageMapper;
import kr.co.nh.atms.web.transactionjournal.dto.TransactionJournalDetailDto;
import kr.co.nh.atms.web.transactionjournal.dto.TransactionJournalSearchDto;
import kr.co.nh.atms.web.transactionjournal.entity.TransactionJournal;
import kr.co.nh.atms.web.transactionjournal.mapper.TransactionJournalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionJournalService {
    private final TransactionJournalMapper mapper;
    private final JournalDataMapper journalDataMapper;
    private final JournalImageMapper journalImageMapper;

    public PageResult<TransactionJournal> search(TransactionJournalSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    public TransactionJournal get(String termGroupId, String termId, String jnlDate, String jnlTime) {
        TransactionJournal t = mapper.selectOne(termGroupId, termId, jnlDate, jnlTime);
        if (t == null) throw new BusinessException("TRX_001", "거래저널이 없습니다.");
        return t;
    }

    public TransactionJournalDetailDto getDetail(String termGroupId, String termId, String jnlDate, String jnlTime) {
        TransactionJournal t = get(termGroupId, termId, jnlDate, jnlTime);
        return TransactionJournalDetailDto.builder()
                .journal(t)
                .journalText(journalDataMapper.selectData(termGroupId, termId, jnlDate, jnlTime))
                .images(journalImageMapper.selectImages(termGroupId, termId, jnlDate, jnlTime))
                .build();
    }
}
