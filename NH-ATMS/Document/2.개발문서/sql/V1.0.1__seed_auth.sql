-- =====================================================================
-- NH-ATMS 초기 데이터 (권한/그룹/메뉴/관리자 계정)
-- 사전 조건: V1.0.0__init_schema.sql 적용 완료
--
-- admin 계정 비밀번호 = 'admin1234!' (BCrypt strength 10)
-- 운영 배포 전 반드시 비밀번호 변경할 것.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 권한 (롤) 마스터
-- ---------------------------------------------------------------------
INSERT INTO T_AUTH (AUTH_CODE, AUTH_NAME, AUTH_DESC, AUTH_TYPE) VALUES
    ('ROLE_ADMIN',   '시스템 관리자', '모든 기능 접근 가능',          'SYSTEM');
INSERT INTO T_AUTH (AUTH_CODE, AUTH_NAME, AUTH_DESC, AUTH_TYPE) VALUES
    ('ROLE_OPER',    '운영자',        'ATM 운영/모니터링/기준정보',  'SYSTEM');
INSERT INTO T_AUTH (AUTH_CODE, AUTH_NAME, AUTH_DESC, AUTH_TYPE) VALUES
    ('ROLE_MONITOR', '모니터링',      '대시보드/조회 전용',          'SYSTEM');
INSERT INTO T_AUTH (AUTH_CODE, AUTH_NAME, AUTH_DESC, AUTH_TYPE) VALUES
    ('ROLE_USER',    '일반사용자',    '기본 조회',                   'SYSTEM');

-- ---------------------------------------------------------------------
-- 사용자 그룹
-- ---------------------------------------------------------------------
INSERT INTO T_USER_GROUP (USER_GROUP_NO, USER_GROUP_NAME, USER_TYPE_CD, DESCRIPTION)
VALUES (1, '시스템관리자그룹', 'A', '시스템 전체 관리');
INSERT INTO T_USER_GROUP (USER_GROUP_NO, USER_GROUP_NAME, USER_TYPE_CD, DESCRIPTION)
VALUES (10, '운영자그룹',     'U', 'ATM 운영 담당');
INSERT INTO T_USER_GROUP (USER_GROUP_NO, USER_GROUP_NAME, USER_TYPE_CD, DESCRIPTION)
VALUES (20, '모니터링그룹',   'M', '모니터링 전용');

-- 그룹-권한 매핑
INSERT INTO T_USER_GROUP_AUTH (USER_GROUP_NO, AUTH_CODE) VALUES (1,  'ROLE_ADMIN');
INSERT INTO T_USER_GROUP_AUTH (USER_GROUP_NO, AUTH_CODE) VALUES (10, 'ROLE_OPER');
INSERT INTO T_USER_GROUP_AUTH (USER_GROUP_NO, AUTH_CODE) VALUES (20, 'ROLE_MONITOR');

-- ---------------------------------------------------------------------
-- 메뉴 (트리)
-- ---------------------------------------------------------------------
-- 1000 대시보드
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (1000, NULL, '대시보드', '/dashboard', 1, 1, 'DashboardOutlined');

-- 2000 ATM 운영
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (2000, NULL, 'ATM 운영',   NULL,            1, 2, 'DesktopOutlined');
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (2010, 2000, '기기현황',   '/atm/status',   2, 1, NULL);
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (2020, 2000, '거래조회',   '/atm/journal',  2, 2, NULL);
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (2030, 2000, '장애관리',   '/atm/fault',    2, 3, NULL);

-- 3000 기준정보
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (3000, NULL, '기준정보',   NULL,                1, 3, 'DatabaseOutlined');
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (3010, 3000, '코드관리',   '/base/code',        2, 1, NULL);
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (3020, 3000, '업체관리',   '/base/vendor',      2, 2, NULL);
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (3030, 3000, '지점관리',   '/base/branch',      2, 3, NULL);
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (3040, 3000, '기기관리',   '/base/atm',         2, 4, NULL);

-- 9000 시스템관리
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (9000, NULL, '시스템관리', NULL,                  1, 9, 'SettingOutlined');
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (9010, 9000, '사용자관리', '/system/user',        2, 1, NULL);
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (9020, 9000, '그룹/권한관리','/system/group',     2, 2, NULL);
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (9030, 9000, '메뉴관리',   '/system/menu',        2, 3, NULL);
INSERT INTO T_MENU (MENU_NO, PARENT_NO, MENU_NAME, MENU_URL, MENU_LEVEL, MENU_ORDER, MENU_ICON)
VALUES (9040, 9000, '로그인이력', '/system/login-hist',  2, 4, NULL);

-- ---------------------------------------------------------------------
-- 권한-메뉴 매핑
--  ADMIN  : 전체 R/W
--  OPER   : 대시보드, ATM, 기준정보 R/W
--  MONITOR: 대시보드, ATM 조회만
-- ---------------------------------------------------------------------
INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', MENU_NO, 'Y', 'Y' FROM T_MENU;

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_OPER', MENU_NO, 'Y', 'Y'
  FROM T_MENU
 WHERE MENU_NO IN (1000, 2000,2010,2020,2030, 3000,3010,3020,3030,3040);

INSERT INTO T_AUTH_MENU (AUTH_CODE, MENU_NO, READ_YN, WRITE_YN)
SELECT 'ROLE_MONITOR', MENU_NO, 'Y', 'N'
  FROM T_MENU
 WHERE MENU_NO IN (1000, 2000,2010,2020,2030);

-- ---------------------------------------------------------------------
-- 관리자 계정 시드
--   USER_ID  = admin
--   PASSWORD = admin1234!  (BCrypt strength 10)
-- ---------------------------------------------------------------------
INSERT INTO T_USER (
    USER_ID, USER_NAME, PASSWORD, EMP_NO, USER_GROUP_NO, USER_TYPE_CD,
    EMAIL, DEPT_NAME, USE_YN, REG_ID
) VALUES (
    'admin', '시스템관리자',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '9999999999999', 1, 'A',
    'admin@nh-atms.local', '시스템운영팀', 'Y', 'SYSTEM'
);

COMMIT;
