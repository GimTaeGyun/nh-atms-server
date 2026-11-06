package kr.co.nh.atms.web.cash.service;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.cash.dto.CashSearchDto;
import kr.co.nh.atms.web.cash.entity.CashPosition;
import kr.co.nh.atms.web.cash.mapper.CashMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashService {
    private final CashMapper mapper;

    public PageResult<CashPosition> search(CashSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }
}
