-- =====================================================================
-- V1.4.1 : 마감관리 메뉴 추가
--   ATM 운영(2000) 그룹 하위에 마감조회 화면을 등록한다.
--   - 2120 마감관리   /cash/day-total
--       T_DAY_TOTAL(마감저널 jnl_kind=31) 마감항목별 조회.
--       소계/합계, 집계원천(호스트/기기), 항목코드/집계값, 마감기간 표시.
--   참조: V1.3.9(다운로드관리 2100/2110), V1.4.0(T_DAY_TOTAL 재설계)
-- =====================================================================

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 2120, 2000, '마감관리', '/cash/day-total', 2, 12, NULL
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 2120);

-- 권한 매핑 : 마감관리(2120) - 조회 화면
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', 2120, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = 2120);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', 2120, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = 2120);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_MONITOR', 2120, 'Y', 'N'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_MONITOR' AND MENU_NO = 2120);

COMMIT;
