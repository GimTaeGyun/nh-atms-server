package kr.co.nh.atms.web.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginHistMapper {

    /** 로그인 시도 이력 INSERT (성공/실패 공통) */
    int insertHist(@Param("userId") String userId,
                   @Param("loginIp") String loginIp,
                   @Param("loginResult") String loginResult,
                   @Param("failReason") String failReason,
                   @Param("userAgent") String userAgent);

    /** 로그아웃 시각 갱신 (해당 사용자 가장 최근 성공 이력) */
    int updateLogoutTime(@Param("userId") String userId);
}
