package kr.co.nh.atms.web.user.mapper;

import kr.co.nh.atms.web.user.dto.UserSearchDto;
import kr.co.nh.atms.web.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    List<User> selectList(UserSearchDto search);
    long countList(UserSearchDto search);
    User selectOne(@Param("userId") String userId);

    /** 로그인용: 사용자그룹/권한코드 조인 조회 */
    User findForLogin(@Param("userId") String userId);

    int insert(User u);
    int update(User u);
    int delete(@Param("userId") String userId);

    int updatePassword(@Param("userId") String userId, @Param("password") String password);

    /** 계정 잠금 해제: LOCK_YN='N', LOGIN_FAIL_CNT=0 */
    int unlock(@Param("userId") String userId);

    /** 사용여부 변경 */
    int updateUseYn(@Param("userId") String userId, @Param("useYn") String useYn);

    /** 로그인 성공 처리: LAST_LOGIN_DT, LOGIN_FLAG='Y', LOGIN_FAIL_CNT=0 */
    int updateLoginSuccess(@Param("userId") String userId);

    /** 로그인 실패 처리: LOGIN_FAIL_CNT+=1, 5회 도달 시 LOCK_YN='Y' */
    int incrementLoginFail(@Param("userId") String userId);

    /** 로그아웃 처리: LOGIN_FLAG='N' */
    int updateLogout(@Param("userId") String userId);
}
