package kr.co.nh.atms.web.atmgroup.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.atmgroup.dto.AtmGroupSearchDto;
import kr.co.nh.atms.web.atmgroup.entity.AtmGroup;
import kr.co.nh.atms.web.atmgroup.mapper.AtmGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtmGroupService {
    private final AtmGroupMapper mapper;

    public PageResult<AtmGroup> search(AtmGroupSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }
    public AtmGroup get(String id) {
        AtmGroup g = mapper.selectOne(id);
        if (g == null) throw new BusinessException("GRP_001", "그룹을 찾을 수 없습니다.");
        return g;
    }
    @Transactional public void create(AtmGroup g) { mapper.insert(g); }
    @Transactional public void update(AtmGroup g) {
        if (mapper.update(g) == 0) throw new BusinessException("GRP_001", "그룹을 찾을 수 없습니다.");
    }
    @Transactional public void delete(String id) {
        if (mapper.delete(id) == 0) throw new BusinessException("GRP_001", "그룹을 찾을 수 없습니다.");
    }
}
