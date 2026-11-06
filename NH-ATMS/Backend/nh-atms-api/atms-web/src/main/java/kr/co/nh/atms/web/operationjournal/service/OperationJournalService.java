package kr.co.nh.atms.web.operationjournal.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.journalattachment.mapper.JournalDataMapper;
import kr.co.nh.atms.web.operationjournal.dto.OperationJournalDetailDto;
import kr.co.nh.atms.web.operationjournal.dto.OperationJournalSearchDto;
import kr.co.nh.atms.web.operationjournal.entity.OperationJournal;
import kr.co.nh.atms.web.operationjournal.mapper.OperationJournalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationJournalService {
    private final OperationJournalMapper mapper;
    private final JournalDataMapper journalDataMapper;

    public PageResult<OperationJournal> search(OperationJournalSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    public OperationJournal get(String termGroupId, String termId, String jnlDate, String jnlTime) {
        OperationJournal t = mapper.selectOne(termGroupId, termId, jnlDate, jnlTime);
        if (t == null) throw new BusinessException("OPJ_001", "운영저널이 없습니다.");
        return t;
    }

    public OperationJournalDetailDto getDetail(String termGroupId, String termId, String jnlDate, String jnlTime) {
        OperationJournal t = get(termGroupId, termId, jnlDate, jnlTime);
        return OperationJournalDetailDto.builder()
                .journal(t)
                .journalText(journalDataMapper.selectData(termGroupId, termId, jnlDate, jnlTime))
                .build();
    }
}
