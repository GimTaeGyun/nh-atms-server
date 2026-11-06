package kr.co.nh.atms.web.corner.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.corner.dto.CornerSearchDto;
import kr.co.nh.atms.web.corner.entity.Corner;
import kr.co.nh.atms.web.corner.mapper.CornerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CornerService {
    private final CornerMapper mapper;

    public PageResult<Corner> search(CornerSearchDto search) {
        return PageResult.of(mapper.selectList(search), mapper.countList(search), search);
    }

    public Corner get(String termGroupId, String cornerCd) {
        Corner c = mapper.selectOne(termGroupId, cornerCd);
        if (c == null) throw new BusinessException("COR_001", "코너 정보를 찾을 수 없습니다.");
        return c;
    }
}
