package kr.co.nh.atms.web.branch.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.branch.dto.BranchSearchDto;
import kr.co.nh.atms.web.branch.entity.Branch;
import kr.co.nh.atms.web.branch.mapper.BranchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {
    private final BranchMapper mapper;

    public PageResult<Branch> search(BranchSearchDto search) {
        return PageResult.of(mapper.selectList(search), mapper.countList(search), search);
    }

    public Branch get(String termGroupId, String branchCd, String branchName) {
        Branch b = mapper.selectOne(termGroupId, branchCd, branchName);
        if (b == null) throw new BusinessException("BRA_001", "조합 정보를 찾을 수 없습니다.");
        return b;
    }

    @Transactional
    public void create(Branch b) { mapper.insert(b); }

    @Transactional
    public void update(Branch b) {
        if (mapper.update(b) == 0) throw new BusinessException("BRA_001", "조합 정보를 찾을 수 없습니다.");
    }

    @Transactional
    public void delete(String termGroupId, String branchCd, String branchName) {
        if (mapper.delete(termGroupId, branchCd, branchName) == 0)
            throw new BusinessException("BRA_001", "조합 정보를 찾을 수 없습니다.");
    }
}
