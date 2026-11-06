package kr.co.nh.atms.web.journal.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.journal.dto.JournalDetailDto;
import kr.co.nh.atms.web.journal.dto.JournalSearchDto;
import kr.co.nh.atms.web.journal.entity.Journal;
import kr.co.nh.atms.web.journal.mapper.JournalMapper;
import kr.co.nh.atms.web.journalattachment.mapper.JournalDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalService {
    private final JournalMapper mapper;
    private final JournalDataMapper journalDataMapper;

    public PageResult<Journal> search(JournalSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    public JournalDetailDto getDetail(String termGroupId, String termId, String jnlDate, String jnlTime) {
        Journal j = mapper.selectOne(termGroupId, termId, jnlDate, jnlTime);
        if (j == null) throw new BusinessException("JNL_001", "저널 헤더가 없습니다.");
        return JournalDetailDto.builder()
                .journal(j)
                .journalText(journalDataMapper.selectData(termGroupId, termId, jnlDate, jnlTime))
                .build();
    }
}
