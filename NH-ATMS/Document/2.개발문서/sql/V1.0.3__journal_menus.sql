-- =====================================================================
-- V1.0.3 : 저널 도메인 메뉴 시드 + 권한 매핑
-- 참조: Document/10_DB스키마_저널.md (4. 메뉴 추가 계획)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) ATM 운영 그룹 보강
--    2015 기기상태이력 (T_ATM_MONITOR)  ← 1주차 MVP
-- ---------------------------------------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (2015, 2000, '기기상태이력', '/atm/monitor', 2, 2, NULL);


-- ---------------------------------------------------------------------
-- 2) 저널관리 그룹 (5000)
-- ---------------------------------------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (5000, NULL, '저널관리', NULL, 1, 4, 'FileTextOutlined');

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (5010, 5000, '저널조회',  '/journal/list',      2, 1, NULL);


-- ---------------------------------------------------------------------
-- 3) 권한-메뉴 매핑 (ROLE_ADMIN 전체, ROLE_OPER 저널조회/기기상태이력 R/W)
-- ---------------------------------------------------------------------
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', MENU_NO, 'Y', 'Y'
  FROM T_MENU
 WHERE MENU_NO IN (2015, 5000, 5010)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = T_MENU.MENU_NO
   );

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', MENU_NO, 'Y', 'Y'
  FROM T_MENU
 WHERE MENU_NO IN (2015, 5000, 5010)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = T_MENU.MENU_NO
   );

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_MONITOR', MENU_NO, 'Y', 'N'
  FROM T_MENU
 WHERE MENU_NO IN (2015, 5000, 5010)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_MONITOR' AND MENU_NO = T_MENU.MENU_NO
   );

COMMIT;
