-- =====================================================================
-- V1.3.6 : 기기 원격제어 메뉴 추가
--   ATM 운영(2000) 그룹 하위에 원격제어 화면을 등록한다.
--   - 2070 기기 원격제어  /atm/remote-control
--     (2040~2060은 기기개국현황/장애다발/기기시재가 선점 → 2070 사용)
--   화면: 운영자가 T_AGENT_COMMAND 큐에 명령을 발행(STATUS=W)하면
--         에이전트(atms-journal 폴링)가 수령·실행·결과통보한다.
--   ※ 원격제어는 실제 기기 제어 행위이므로 ROLE_ADMIN / ROLE_OPER 에만
--      읽기/쓰기 권한을 부여한다(ROLE_MONITOR 제외).
-- 참조: V1.0.1 (ATM 운영 메뉴), V1.3.4 (T_AGENT_COMMAND 큐 스키마)
-- =====================================================================

-- 2차 서브메뉴: 기기 원격제어 -----------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 2070, 2000, '기기 원격제어', '/atm/remote-control', 2, 7, NULL
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 2070);

-- 권한 매핑 (ROLE_ADMIN / ROLE_OPER : R/W) ----------------------------
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', 2070, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = 2070);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', 2070, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = 2070);

COMMIT;
