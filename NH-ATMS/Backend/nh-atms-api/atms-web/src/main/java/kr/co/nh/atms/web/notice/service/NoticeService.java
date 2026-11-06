package kr.co.nh.atms.web.notice.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.notice.dto.NoticeSearchDto;
import kr.co.nh.atms.web.notice.entity.Notice;
import kr.co.nh.atms.web.notice.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeMapper mapper;

    public PageResult<Notice> search(NoticeSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    @Transactional
    public long increaseHit(String catCd, Long seqNo) {
        mapper.increaseHit(catCd, seqNo);
        Notice n = mapper.selectOne(catCd, seqNo);
        return n != null && n.getHit() != null ? n.getHit() : 0L;
    }

    @Transactional
    public Long create(Notice n) {
        Long seq = mapper.nextSeqNo(n.getCatCd());
        n.setSeqNo(seq);
        if (n.getUseYn() == null) n.setUseYn("Y");
        n.setDepth(0);
        n.setParentSeqNo(0L);
        mapper.insert(n);
        return seq;
    }

    @Transactional
    public void update(Notice n) {
        if (mapper.update(n) == 0)
            throw new BusinessException("NTC_001", "공지사항을 찾을 수 없습니다.");
    }

    @Transactional
    public void delete(String catCd, Long seqNo) {
        if (mapper.delete(catCd, seqNo) == 0)
            throw new BusinessException("NTC_001", "공지사항을 찾을 수 없습니다.");
    }
}
