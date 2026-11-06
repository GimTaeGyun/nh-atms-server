package kr.co.nh.atms.web.role.mapper;

import kr.co.nh.atms.web.role.dto.MenuRolePermDetail;
import kr.co.nh.atms.web.role.dto.RoleSearchDto;
import kr.co.nh.atms.web.role.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    List<Role> selectList(RoleSearchDto search);
    long countList(RoleSearchDto search);
    Role selectOne(@Param("authCode") String authCode);
    /** 전체 권한 목록 (셀렉트박스용) */
    List<Role> selectAll();
    int insert(Role r);
    int update(Role r);
    int delete(@Param("authCode") String authCode);
    /** T_USER_GROUP_AUTH 에서 해당 권한을 참조하는 그룹 수 */
    int countUsedByGroups(@Param("authCode") String authCode);

    /** T_AUTH_MENU 단건 조회 — 특정 메뉴 + 권한의 조회/수정 허용 여부 */
    MenuRolePermDetail selectMenuRolePerm(@Param("menuNo") Long menuNo,
                                         @Param("authCode") String authCode);

    /** T_AUTH_MENU UPSERT — 레코드 없으면 INSERT, 있으면 UPDATE */
    int upsertMenuRolePerm(MenuRolePermDetail perm);
}