package kr.co.nh.atms.web.agentstatus.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.agentstatus.dto.AgentStatusSearchDto;
import kr.co.nh.atms.web.agentstatus.entity.AgentStatus;
import kr.co.nh.atms.web.agentstatus.mapper.AgentStatusMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentStatusService {
    private final AgentStatusMapper mapper;

    public PageResult<AgentStatus> search(AgentStatusSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    public AgentStatus get(String atmIp) {
        AgentStatus a = mapper.selectOne(atmIp);
        if (a == null) throw new BusinessException("AGT_001", "기기 개국정보가 없습니다.");
        return a;
    }
}
