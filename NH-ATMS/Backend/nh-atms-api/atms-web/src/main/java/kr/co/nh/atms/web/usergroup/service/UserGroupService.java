package kr.co.nh.atms.web.usergroup.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.role.mapper.RoleMapper;
import kr.co.nh.atms.web.usergroup.dto.UserGroupSearchDto;
import kr.co.nh.atms.web.usergroup.entity.UserGroup;
import kr.co.nh.atms.web.usergroup.mapper.UserGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserGroupService {

    private final UserGroupMapper mapper;
    private final RoleMapper roleMapper;

    public PageResult<UserGroup> search(UserGroupSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    public UserGroup get(Long userGroupNo) {
        UserGroup g = mapper.selectOne(userGroupNo);
        if (g == null) throw new BusinessException("GRP_001", "사용자그룹을 찾을 수 없습니다.");
        g.setAuthCodes(mapper.selectAuthCodes(userGroupNo));
        return g;
    }

    public List<UserGroup> getAll() {
        return mapper.selectAll();
    }

    @Transactional
    public void create(UserGroup g) {
        mapper.insert(g);
        saveAuthCodes(g.getUserGroupNo(), g.getAuthCodes());
    }

    @Transactional
    public void update(UserGroup g) {
        if (mapper.update(g) == 0) throw new BusinessException("GRP_001", "사용자그룹을 찾을 수 없습니다.");
        if (g.getAuthCodes() != null) {
            saveAuthCodes(g.getUserGroupNo(), g.getAuthCodes());
        }
    }

    @Transactional
    public void delete(Long userGroupNo) {
        if (mapper.delete(userGroupNo) == 0)
            throw new BusinessException("GRP_001", "사용자그룹을 찾을 수 없습니다.");
    }

    /** 그룹 권한 코드 교체 (DELETE → INSERT) */
    @Transactional
    public void updateAuthCodes(Long userGroupNo, List<String> authCodes) {
        if (mapper.selectOne(userGroupNo) == null)
            throw new BusinessException("GRP_001", "사용자그룹을 찾을 수 없습니다.");
        saveAuthCodes(userGroupNo, authCodes);
    }

    private void saveAuthCodes(Long userGroupNo, List<String> authCodes) {
        mapper.deleteAuthCodes(userGroupNo);
        if (!CollectionUtils.isEmpty(authCodes)) {
            for (String code : authCodes) {
                if (roleMapper.selectOne(code) == null)
                    throw new BusinessException("ROL_001", "존재하지 않는 권한 코드입니다: " + code);
                mapper.insertAuthCode(userGroupNo, code);
            }
        }
    }
}
