-- =====================================================================
-- V1.3.7 : 실시간모니터링 / 기기유지보수접수 메뉴 추가
--   ATM 운영(2000) 그룹 하위에 두 화면을 등록한다.
--   - 2080 실시간모니터링      /atm/realtime-monitor
--       기기 상태를 실시간 조회하고, 선택한 기기에 대해 장애접수(출동요청)
--       를 발행한다. 조회는 모니터링 권한 포함, 장애접수는 운영 권한.
--   - 2090 기기유지보수접수    /atm/maintenance
--       장애접수(출동요청) 이력을 관리하며 장애접수/SMS전송/출동취소를
--       수행한다. 실제 조치 행위이므로 ROLE_ADMIN / ROLE_OPER 만 부여.
--   화면 동작:
--     · 장애접수 → T_FAULT_MANAGEMENT (FAULT_STATUS='O') 적재
--     · SMS전송  → T_SMS_SEND_DATA   (SEND_STATUS='W') 적재
-- 참조: V1.0.1(ATM 운영 메뉴), V1.1.3(T_SMS_SEND_DATA), V1.1.4(T_FAULT_MANAGEMENT)
-- =====================================================================

-- 2차 서브메뉴: 실시간모니터링 ----------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 2080, 2000, '실시간모니터링', '/atm/realtime-monitor', 2, 8, NULL
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 2080);

-- 2차 서브메뉴: 기기유지보수접수 --------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 2090, 2000, '기기유지보수접수', '/atm/maintenance', 2, 9, NULL
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 2090);

-- 권한 매핑 : 실시간모니터링(2080) -----------------------------------
--   ROLE_ADMIN / ROLE_OPER : R/W (장애접수 가능)
--   ROLE_MONITOR           : R   (조회 전용)
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', 2080, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = 2080);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', 2080, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = 2080);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_MONITOR', 2080, 'Y', 'N'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_MONITOR' AND MENU_NO = 2080);

-- 권한 매핑 : 기기유지보수접수(2090) ---------------------------------
--   ROLE_ADMIN / ROLE_OPER : R/W (장애접수/SMS전송/출동취소)
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', 2090, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = 2090);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', 2090, 'Y', 'Y'
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = 2090);

COMMIT;
