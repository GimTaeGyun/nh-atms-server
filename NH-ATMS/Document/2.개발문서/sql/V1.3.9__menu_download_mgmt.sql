-- =====================================================================
-- V1.3.9 : 개별다운로드관리 / 일괄다운로드관리 메뉴 추가
--   ATM 운영(2000) 그룹 하위에 두 화면을 등록한다.
--   - 2100 개별다운로드관리   /atm/download-individual
--       특정 기기를 선택(현재버전 vs 최종버전 비교)하여 결재완료된
--       패키지를 그 기기들에만 배포한다. 결재 불필요(즉시 배포).
--   - 2110 일괄다운로드관리   /atm/download-batch
--       기종(업체+모델)별 다운로드 패키지를 등록·결재(승인)하고,
--       결재완료 패키지를 해당 기종 전체 기기에 일괄 배포한다.
--   화면 동작:
--     · 패키지등록 → T_DOWNLOAD_PACKAGE (CONFIRM_STATUS='N')
--     · 결재       → T_DOWNLOAD_PACKAGE.CONFIRM_STATUS='Y'
--     · 배포       → T_DOWNLOAD_JOB + T_AGENT_COMMAND(CMD_TYPE=DLD/GDL)
--   실제 조치(배포/결재) 행위이므로 ROLE_ADMIN / ROLE_OPER 만 부여.
-- 참조: V1.0.1(ATM 운영 메뉴), V1.3.4(T_AGENT_COMMAND), V1.3.8(다운로드 스키마)
-- =====================================================================

-- 2차 서브메뉴: 개별다운로드관리 --------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 2100, 2000, '개별다운로드관리', '/atm/download-individual', 2, 10, NULL
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 2100);

-- 2차 서브메뉴: 일괄다운로드관리 --------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 2110, 2000, '일괄다운로드관리', '/atm/download-batch', 2, 11, NULL
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 2110);

-- 권한 매핑 : 개별다운로드관리(2100) ---------------------------------
--   ROLE_ADMIN / ROLE_OPER : R/W (개별 배포)
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', 2100, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = 2100);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', 2100, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = 2100);

-- 권한 매핑 : 일괄다운로드관리(2110) ---------------------------------
--   ROLE_ADMIN / ROLE_OPER : R/W (패키지 등록/결재/일괄 배포)
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', 2110, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = 2110);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', 2110, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = 2110);

COMMIT;
