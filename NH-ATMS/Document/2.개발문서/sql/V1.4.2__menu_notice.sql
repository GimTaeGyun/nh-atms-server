-- =====================================================================
-- V1.4.2 : 게시물관리(공지사항) 메뉴 추가
--   기준정보(3000) 그룹 하위에 공지사항 화면을 등록한다.
--   - 3060 공지사항   /base/notice
--       NoticePage(이미 구현됨) 메뉴 미등록 상태였으므로 등록만 수행.
--   참조: V1.2.3(기준정보 3010~3050), MG [게시물관리] 메뉴
-- =====================================================================

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 3060, 3000, '공지사항', '/base/notice', 2, 6, NULL
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 3060);

-- 권한 매핑 : 공지사항(3060)
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', 3060, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = 3060);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', 3060, 'Y', 'N'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = 3060);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_MONITOR', 3060, 'Y', 'N'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_MONITOR' AND MENU_NO = 3060);

COMMIT;
