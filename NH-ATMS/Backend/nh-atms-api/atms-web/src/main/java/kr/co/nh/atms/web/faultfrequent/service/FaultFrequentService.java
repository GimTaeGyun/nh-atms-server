package kr.co.nh.atms.web.faultfrequent.service;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.faultfrequent.dto.FaultFrequent;
import kr.co.nh.atms.web.faultfrequent.dto.FaultFrequentSearchDto;
import kr.co.nh.atms.web.faultfrequent.mapper.FaultFrequentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaultFrequentService {
    private final FaultFrequentMapper mapper;

    public PageResult<FaultFrequent> search(FaultFrequentSearchDto s) {
        if (s.getMinFaultCnt() == null) s.setMinFaultCnt(1);
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }
}
