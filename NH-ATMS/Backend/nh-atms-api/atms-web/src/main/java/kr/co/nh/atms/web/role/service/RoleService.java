package kr.co.nh.atms.web.role.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.role.dto.MenuRolePermDetail;
import kr.co.nh.atms.web.role.dto.MenuRolePermSaveRequest;
import kr.co.nh.atms.web.role.dto.RoleSearchDto;
import kr.co.nh.atms.web.role.entity.Role;
import kr.co.nh.atms.web.role.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {
    private final RoleMapper mapper;

    public PageResult<Role> search(RoleSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    public Role get(String authCode) {
        if (!StringUtils.hasText(authCode))
            throw new BusinessException("ROL_003", "권한 코드는 필수입니다.");
        Role r = mapper.selectOne(authCode);
        if (r == null) throw new BusinessException("ROL_001", "권한을 찾을 수 없습니다.");
        return r;
    }

    public List<Role> getAll() {
        return mapper.selectAll();
    }

    @Transactional
    public void create(Role r) {
        if (r == null)
            throw new BusinessException("ROL_003", "권한 정보가 없습니다.");
        if (!StringUtils.hasText(r.getAuthCode()))
            throw new BusinessException("ROL_003", "권한 코드는 필수입니다.");
        if (!StringUtils.hasText(r.getAuthName()))
            throw new BusinessException("ROL_004", "권한명은 필수입니다.");
        if (mapper.selectOne(r.getAuthCode()) != null)
            throw new BusinessException("ROL_002", "이미 존재하는 권한 코드입니다.");
        if (r.getUseYn() == null) r.setUseYn("Y");
        mapper.insert(r);
    }

    @Transactional
    public void update(Role r) {
        if (r == null || !StringUtils.hasText(r.getAuthCode()))
            throw new BusinessException("ROL_003", "권한 코드는 필수입니다.");
        if (mapper.update(r) == 0) throw new BusinessException("ROL_001", "권한을 찾을 수 없습니다.");
    }

    public MenuRolePermDetail getMenuRolePerm(Long menuNo, String authCode) {
        if (menuNo == null)
            throw new BusinessException("MNU_003", "메뉴 번호는 필수입니다.");
        if (!StringUtils.hasText(authCode))
            throw new BusinessException("ROL_003", "권한 코드는 필수입니다.");
        MenuRolePermDetail perm = mapper.selectMenuRolePerm(menuNo, authCode);
        if (perm == null)
            return MenuRolePermDetail.builder()
                    .authCode(authCode).menuNo(menuNo)
                    .readYn("N").writeYn("N")
                    .build();
        return perm;
    }

    @Transactional
    public void saveMenuRolePerm(Long menuNo, String authCode, MenuRolePermSaveRequest req) {
        if (menuNo == null)
            throw new BusinessException("MNU_003", "메뉴 번호는 필수입니다.");
        if (!StringUtils.hasText(authCode))
            throw new BusinessException("ROL_003", "권한 코드는 필수입니다.");
        if (mapper.selectOne(authCode) == null)
            throw new BusinessException("ROL_001", "권한을 찾을 수 없습니다.");

        String readYn  = "Y".equals(req.getReadYn())  ? "Y" : "N";
        String writeYn = "Y".equals(req.getWriteYn()) ? "Y" : "N";

        mapper.upsertMenuRolePerm(
                MenuRolePermDetail.builder()
                        .menuNo(menuNo).authCode(authCode)
                        .readYn(readYn).writeYn(writeYn)
                        .build()
        );
    }

    @Transactional
    public void delete(String authCode) {
        if (!StringUtils.hasText(authCode))
            throw new BusinessException("ROL_003", "권한 코드는 필수입니다.");
        if (mapper.countUsedByGroups(authCode) > 0)
            throw new BusinessException("ROL_005", "사용 중인 권한은 삭제할 수 없습니다.");
        if (mapper.delete(authCode) == 0) throw new BusinessException("ROL_001", "권한을 찾을 수 없습니다.");
    }
}
