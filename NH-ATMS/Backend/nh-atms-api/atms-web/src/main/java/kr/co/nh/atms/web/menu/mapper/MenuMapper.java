package kr.co.nh.atms.web.menu.mapper;

import kr.co.nh.atms.web.menu.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper {

    /** 사용자에게 권한 부여된 메뉴 (USE_YN/VISIBLE_YN='Y') */
    List<Menu> selectAccessibleMenusByUserId(@Param("userId") String userId);

    /** 전체 메뉴 (관리자 화면용) */
    List<Menu> selectAllActiveMenus();

    Menu selectOne(@Param("menuNo") Long menuNo);

    /** 신규 등록 (SEQ_T_MENU.NEXTVAL) */
    int insert(Menu menu);

    /** 수정 */
    int update(Menu menu);

    /** 소프트 삭제 (USE_YN='N') */
    int softDelete(@Param("menuNo") Long menuNo);

    /** 자식 메뉴 존재 여부 */
    int countChildren(@Param("menuNo") Long menuNo);

    /** 상위 메뉴의 권한코드를 신규 메뉴에 복사 (T_AUTH_MENU) */
    int copyAuthMenuFromParent(@Param("parentNo") Long parentNo, @Param("menuNo") Long menuNo);
}
