package kr.co.nh.atms.web.daytotal.service;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.daytotal.dto.DayTotalSearchDto;
import kr.co.nh.atms.web.daytotal.entity.DayTotalRow;
import kr.co.nh.atms.web.daytotal.mapper.DayTotalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DayTotalService {
    private final DayTotalMapper mapper;

    public PageResult<DayTotalRow> search(DayTotalSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }
}
