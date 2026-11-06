-- =====================================================================
-- V1.1.5 : 저널 연관 조회 메뉴 추가 (기기개국현황 / 장애다발기기조회)
--   - 새마을금고(MG) 기준 저널 연관 조회 화면. NH 저널 구조 동일 적용.
--   - 2040 기기개국현황    (/atm/agent-status)  ← T_AGENT_STATUS
--   - 2050 장애다발기기조회 (/atm/fault-frequent) ← T_ATM_MONITOR 집계
-- 참조: 기존 V1.0.1(ATM 운영 그룹 2000), V1.0.3(저널 메뉴)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) ATM 운영 그룹(2000) 하위 메뉴 추가
-- ---------------------------------------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (2040, 2000, '기기개국현황', '/atm/agent-status', 2, 4, NULL);

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (2050, 2000, '장애다발기기조회', '/atm/fault-frequent', 2, 5, NULL);

-- ---------------------------------------------------------------------
-- 2) 권한-메뉴 매핑
--    ROLE_ADMIN / ROLE_OPER : R/W,  ROLE_MONITOR : R only
-- ---------------------------------------------------------------------
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', MENU_NO, 'Y', 'Y'
  FROM T_MENU
 WHERE MENU_NO IN (2040, 2050)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = T_MENU.MENU_NO
   );

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', MENU_NO, 'Y', 'Y'
  FROM T_MENU
 WHERE MENU_NO IN (2040, 2050)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = T_MENU.MENU_NO
   );

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_MONITOR', MENU_NO, 'Y', 'N'
  FROM T_MENU
 WHERE MENU_NO IN (2040, 2050)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_MONITOR' AND MENU_NO = T_MENU.MENU_NO
   );

COMMIT;
