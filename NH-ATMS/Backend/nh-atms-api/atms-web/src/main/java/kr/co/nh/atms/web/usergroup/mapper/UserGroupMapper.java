package kr.co.nh.atms.web.usergroup.mapper;

import kr.co.nh.atms.web.usergroup.dto.UserGroupSearchDto;
import kr.co.nh.atms.web.usergroup.entity.UserGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserGroupMapper {
    List<UserGroup> selectList(UserGroupSearchDto search);
    long countList(UserGroupSearchDto search);
    UserGroup selectOne(@Param("userGroupNo") Long userGroupNo);
    /** 전체 그룹 (셀렉트박스용) */
    List<UserGroup> selectAll();

    int insert(UserGroup g);
    int update(UserGroup g);
    int delete(@Param("userGroupNo") Long userGroupNo);

    // T_USER_GROUP_AUTH 관련
    /** 그룹에 매핑된 권한 코드 목록 */
    List<String> selectAuthCodes(@Param("userGroupNo") Long userGroupNo);
    /** 기존 권한 매핑 삭제 */
    int deleteAuthCodes(@Param("userGroupNo") Long userGroupNo);
    /** 권한 코드 단건 삽입 */
    int insertAuthCode(@Param("userGroupNo") Long userGroupNo, @Param("authCode") String authCode);
}
