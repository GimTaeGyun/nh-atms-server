package kr.co.nh.atms.web.atmstatus.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.atmstatus.dto.AtmStatusSearchDto;
import kr.co.nh.atms.web.atmstatus.entity.AtmStatus;
import kr.co.nh.atms.web.atmstatus.mapper.AtmStatusMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtmStatusService {
    private final AtmStatusMapper mapper;

    public PageResult<AtmStatus> search(AtmStatusSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }
    public AtmStatus get(String termGroupId, String termId) {
        AtmStatus s = mapper.selectOne(termGroupId, termId);
        if (s == null) throw new BusinessException("STA_001", "기기 상태 정보가 없습니다.");
        return s;
    }
    @Transactional
    public void upsert(AtmStatus s) { mapper.upsert(s); }
}
