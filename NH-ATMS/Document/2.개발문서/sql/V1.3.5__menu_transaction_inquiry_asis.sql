-- =====================================================================
-- V1.3.5 : 거래내역조회(AS-IS) 메뉴 추가
--   AS-IS 자동화기기관리시스템 1000~1008 거래조회 화면을 메뉴로 등록
--   - 4000 거래내역조회            (1차 그룹, URL 없음)
--   - 4010 특정(건별) 거래 조회    /transaction/specific          (AS-IS 1000)
--   - 4020 입금/출금 거래          /transaction/deposit-withdraw  (AS-IS 1001)
--   - 4030 공과금/납부 거래        /transaction/giro              (AS-IS 1003)
--   - 4040 신용카드 거래           /transaction/card              (AS-IS 1004)
--   - 4050 수표입출금 거래조회     /transaction/check             (AS-IS 1005)
--   - 4060 무통장/무카드 거래      /transaction/cardless          (AS-IS 1006)
--   - 4070 통장 기장 거래          /transaction/passbook          (AS-IS 1007)
--   - 4080 외화 입/출금 거래       /transaction/forex             (AS-IS 1008)
--   ※ 데모용 화면(Mock 데이터). 백엔드 조회 API는 추후 연동.
-- 참조: V1.1.6 (기기시재조회 메뉴), V1.0.8 (저널 서브메뉴)
-- =====================================================================

-- 1차 그룹: 거래내역조회 ------------------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4000, NULL, '거래내역조회', NULL, 1, 25, NULL
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4000);

-- 2차 서브메뉴 ---------------------------------------------------------
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4010, 4000, '특정(건별) 거래 조회', '/transaction/specific', 2, 1, NULL
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4010);

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4020, 4000, '입금/출금 거래', '/transaction/deposit-withdraw', 2, 2, NULL
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4020);

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4030, 4000, '공과금/납부 거래', '/transaction/giro', 2, 3, NULL
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4030);

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4040, 4000, '신용카드 거래', '/transaction/card', 2, 4, NULL
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4040);

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4050, 4000, '수표입출금 거래조회', '/transaction/check', 2, 5, NULL
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4050);

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4060, 4000, '무통장/무카드 거래', '/transaction/cardless', 2, 6, NULL
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4060);

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4070, 4000, '통장 기장 거래', '/transaction/passbook', 2, 7, NULL
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4070);

INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
SELECT 4080, 4000, '외화 입/출금 거래', '/transaction/forex', 2, 8, NULL
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM T_MENU WHERE MENU_NO = 4080);

-- 권한 매핑 (ROLE_ADMIN / ROLE_OPER : R/W, ROLE_MONITOR : R) -----------
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', MENU_NO, 'Y', 'Y'
  FROM T_MENU
 WHERE MENU_NO IN (4000, 4010, 4020, 4030, 4040, 4050, 4060, 4070, 4080)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_ADMIN' AND MENU_NO = T_MENU.MENU_NO);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', MENU_NO, 'Y', 'Y'
  FROM T_MENU
 WHERE MENU_NO IN (4000, 4010, 4020, 4030, 4040, 4050, 4060, 4070, 4080)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_OPER' AND MENU_NO = T_MENU.MENU_NO);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_MONITOR', MENU_NO, 'Y', 'N'
  FROM T_MENU
 WHERE MENU_NO IN (4000, 4010, 4020, 4030, 4040, 4050, 4060, 4070, 4080)
   AND NOT EXISTS (
       SELECT 1 FROM T_AUTH_MENU
        WHERE AUTH_CODE = 'ROLE_MONITOR' AND MENU_NO = T_MENU.MENU_NO);

COMMIT;
