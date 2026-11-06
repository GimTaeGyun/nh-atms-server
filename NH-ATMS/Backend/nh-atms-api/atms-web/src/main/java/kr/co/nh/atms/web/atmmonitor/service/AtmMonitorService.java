package kr.co.nh.atms.web.atmmonitor.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.atmmonitor.dto.AtmMonitorSearchDto;
import kr.co.nh.atms.web.atmmonitor.entity.AtmMonitor;
import kr.co.nh.atms.web.atmmonitor.mapper.AtmMonitorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtmMonitorService {
    private final AtmMonitorMapper mapper;

    public PageResult<AtmMonitor> search(AtmMonitorSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    public AtmMonitor get(String termGroupId, String termId, String jnlDate, String jnlTime) {
        AtmMonitor m = mapper.selectOne(termGroupId, termId, jnlDate, jnlTime);
        if (m == null) throw new BusinessException("MON_001", "기기상태이력이 없습니다.");
        return m;
    }
}
