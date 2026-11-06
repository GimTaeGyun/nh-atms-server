package kr.co.nh.atms.web.atm.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.atm.dto.AtmSearchDto;
import kr.co.nh.atms.web.atm.entity.Atm;
import kr.co.nh.atms.web.atm.mapper.AtmMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtmService {

    private final AtmMapper atmMapper;

    public PageResult<Atm> search(AtmSearchDto search) {
        long total = atmMapper.countList(search);
        return PageResult.of(atmMapper.selectList(search), total, search);
    }

    public Atm get(String termGroupId, String termId) {
        Atm atm = atmMapper.selectOne(termGroupId, termId);
        if (atm == null) {
            throw new BusinessException("ATM_001", "ATM 을 찾을 수 없습니다.");
        }
        return atm;
    }

    @Transactional
    public void create(Atm atm) {
        if (atmMapper.selectOne(atm.getTermGroupId(), atm.getTermId()) != null) {
            throw new BusinessException("ATM_002", "이미 존재하는 ATM 입니다.");
        }
        atmMapper.insert(atm);
    }

    @Transactional
    public void update(Atm atm) {
        if (atmMapper.update(atm) == 0) {
            throw new BusinessException("ATM_001", "ATM 을 찾을 수 없습니다.");
        }
    }

    @Transactional
    public void delete(String termGroupId, String termId) {
        if (atmMapper.delete(termGroupId, termId) == 0) {
            throw new BusinessException("ATM_001", "ATM 을 찾을 수 없습니다.");
        }
    }
}
