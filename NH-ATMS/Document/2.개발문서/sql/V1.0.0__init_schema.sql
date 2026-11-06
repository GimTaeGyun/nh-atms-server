-- =====================================================================
-- NH-ATMS 초기 스키마 생성 스크립트 (Oracle 19C)
-- 실행 순서:
--   1. SYS / SYSDBA 로 접속 → 00_create_user.sql 먼저 실행 (계정/테이블스페이스)
--   2. ATMS 사용자로 PDB(ATMSPDB)에 접속 후 이 파일 실행
--
-- 적용 범위 (V1.0.0):
--   인증/권한/메뉴 1차 (로그인 구현용)
--   T_USER, T_USER_GROUP, T_AUTH, T_USER_GROUP_AUTH,
--   T_MENU, T_AUTH_MENU, T_LOGIN_HIST, T_USER_AUTH_HIST
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. T_USER_GROUP : 사용자 그룹 (새마을금고 t_usergroup 참고)
-- ---------------------------------------------------------------------
CREATE TABLE T_USER_GROUP (
    USER_GROUP_NO       NUMBER(10)     NOT NULL,    -- 사용자그룹번호
    USER_GROUP_NAME     VARCHAR2(50)   NOT NULL,    -- 사용자그룹명
    USER_TYPE_CD        CHAR(1),                    -- 사용자구분(A:관리자, U:일반, M:모니터링)
    DESCRIPTION         VARCHAR2(255),              -- 설명
    USE_YN              CHAR(1) DEFAULT 'Y',        -- 사용여부
    REG_DT              TIMESTAMP DEFAULT SYSTIMESTAMP,
    UPD_DT              TIMESTAMP,
    CONSTRAINT PK_T_USER_GROUP PRIMARY KEY (USER_GROUP_NO)
);
COMMENT ON TABLE T_USER_GROUP IS '사용자그룹';

CREATE SEQUENCE SEQ_T_USER_GROUP START WITH 100 INCREMENT BY 1 NOCACHE;


-- ---------------------------------------------------------------------
-- 2. T_USER : 로그인 계정 (새마을금고 t_user 참고, 슬림화)
-- ---------------------------------------------------------------------
CREATE TABLE T_USER (
    USER_ID             VARCHAR2(20)   NOT NULL,    -- 로그인 ID
    USER_NAME           VARCHAR2(50)   NOT NULL,    -- 사용자명
    PASSWORD            VARCHAR2(255),              -- BCrypt 해시 (SSO 시 NULL 가능)
    EMP_NO              VARCHAR2(13),               -- 직원번호 (SSO 연계키, 인사시스템 참조용)
    USER_GROUP_NO       NUMBER(10),                 -- 사용자그룹번호 (T_USER_GROUP FK)
    USER_TYPE_CD        CHAR(1) DEFAULT 'U',        -- 사용자구분 (A/U/M)
    EMAIL               VARCHAR2(100),              -- 이메일
    PHONE_NO            VARCHAR2(20),               -- 사무전화
    CELL_PHONE_NO       VARCHAR2(20),               -- 휴대전화
    DEPT_CD             VARCHAR2(10),               -- 부서코드 (캐시)
    DEPT_NAME           VARCHAR2(50),               -- 부서명 (캐시)
    BRANCH_CD           VARCHAR2(6),                -- 본지점코드 (캐시)
    USER_POSITION       VARCHAR2(30),               -- 직위
    LAST_LOGIN_DT       TIMESTAMP,                  -- 최종 로그인 일시
    LOGIN_FLAG          CHAR(1) DEFAULT 'N',        -- 현재 로그인 여부 (Y:로그인중, N:로그아웃)
    LOGIN_FAIL_CNT      NUMBER(3) DEFAULT 0,        -- 로그인 실패 회수
    LOCK_YN             CHAR(1) DEFAULT 'N',        -- 계정 잠김 여부
    PW_EXPIRE_DT        TIMESTAMP,                  -- 비밀번호 만료일
    INFO_CHG_DT         TIMESTAMP,                  -- 정보변경일시 (보안 감사용)
    USE_YN              CHAR(1) DEFAULT 'Y',        -- 사용여부
    REG_DT              TIMESTAMP DEFAULT SYSTIMESTAMP,
    REG_ID              VARCHAR2(20),
    UPD_DT              TIMESTAMP,
    UPD_ID              VARCHAR2(20),
    CONSTRAINT PK_T_USER PRIMARY KEY (USER_ID),
    CONSTRAINT FK_T_USER_GROUP FOREIGN KEY (USER_GROUP_NO) REFERENCES T_USER_GROUP(USER_GROUP_NO)
);
COMMENT ON TABLE  T_USER IS '사용자(로그인 계정)';
COMMENT ON COLUMN T_USER.USER_ID       IS '로그인 ID (PK)';
COMMENT ON COLUMN T_USER.PASSWORD      IS '비밀번호 BCrypt 해시';
COMMENT ON COLUMN T_USER.EMP_NO        IS '직원번호 (SSO 연계키, 인사시스템 참조용)';
COMMENT ON COLUMN T_USER.USER_TYPE_CD  IS '사용자구분 A:관리자 U:일반 M:모니터링';
COMMENT ON COLUMN T_USER.LOGIN_FLAG    IS '현재 로그인 여부 (Y:로그인중 N:로그아웃 - 중복로그인 제어)';
COMMENT ON COLUMN T_USER.LOCK_YN       IS '계정 잠김 여부 (5회 실패 시 Y)';
COMMENT ON COLUMN T_USER.INFO_CHG_DT   IS '최종 정보변경일시 (비밀번호 변경 등)';

CREATE INDEX IDX_T_USER_GROUP_NO ON T_USER (USER_GROUP_NO);


-- ---------------------------------------------------------------------
-- 3. T_AUTH : 권한(롤) 마스터
-- ---------------------------------------------------------------------
CREATE TABLE T_AUTH (
    AUTH_CODE           VARCHAR2(50)   NOT NULL,    -- 권한코드 (예: ROLE_ADMIN)
    AUTH_NAME           VARCHAR2(100)  NOT NULL,    -- 권한명
    AUTH_DESC           VARCHAR2(300),              -- 권한설명
    AUTH_TYPE           VARCHAR2(20),               -- 권한유형 (SYSTEM/USER)
    FSTG_MENU_NO        NUMBER(10),                 -- 로그인 후 초기 이동 메뉴번호 (NULL=대시보드)
    USE_YN              CHAR(1) DEFAULT 'Y',
    REG_DT              TIMESTAMP DEFAULT SYSTIMESTAMP,
    UPD_DT              TIMESTAMP,
    CONSTRAINT PK_T_AUTH PRIMARY KEY (AUTH_CODE)
);
COMMENT ON TABLE  T_AUTH IS '권한(롤) 마스터';
COMMENT ON COLUMN T_AUTH.FSTG_MENU_NO IS '로그인 후 초기 이동 메뉴번호 (NULL이면 기본 대시보드)';


-- ---------------------------------------------------------------------
-- 4. T_USER_GROUP_AUTH : 사용자그룹 ↔ 권한 매핑
-- ---------------------------------------------------------------------
CREATE TABLE T_USER_GROUP_AUTH (
    USER_GROUP_NO       NUMBER(10)     NOT NULL,
    AUTH_CODE           VARCHAR2(50)   NOT NULL,
    REG_DT              TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT PK_T_USER_GROUP_AUTH PRIMARY KEY (USER_GROUP_NO, AUTH_CODE),
    CONSTRAINT FK_UGA_GROUP FOREIGN KEY (USER_GROUP_NO) REFERENCES T_USER_GROUP(USER_GROUP_NO),
    CONSTRAINT FK_UGA_AUTH  FOREIGN KEY (AUTH_CODE)     REFERENCES T_AUTH(AUTH_CODE)
);
COMMENT ON TABLE T_USER_GROUP_AUTH IS '사용자그룹-권한 매핑';


-- ---------------------------------------------------------------------
-- 5. T_MENU : 메뉴(트리 구조)
-- ---------------------------------------------------------------------
CREATE TABLE T_MENU (
    MENU_NO             NUMBER(10)     NOT NULL,    -- 메뉴번호 (PK)
    PARENT_NO           NUMBER(10),                 -- 상위 메뉴번호 (NULL=루트)
    MENU_NAME           VARCHAR2(100)  NOT NULL,    -- 메뉴명
    MENU_URL            VARCHAR2(200),              -- 라우팅 URL (예: /system/user)
    MENU_LEVEL          NUMBER(2),                  -- 메뉴 깊이 (1=대분류)
    MENU_ORDER          NUMBER(5),                  -- 정렬순서
    MENU_ICON           VARCHAR2(50),               -- 아이콘
    MENU_TYPE           CHAR(1) DEFAULT 'M',        -- M:메뉴 / B:버튼 / D:디렉토리
    VISIBLE_YN          CHAR(1) DEFAULT 'Y',        -- 화면 표시 여부
    USE_YN              CHAR(1) DEFAULT 'Y',
    DESCRIPTION         VARCHAR2(255),
    REG_DT              TIMESTAMP DEFAULT SYSTIMESTAMP,
    UPD_DT              TIMESTAMP,
    CONSTRAINT PK_T_MENU PRIMARY KEY (MENU_NO)
);
COMMENT ON TABLE T_MENU IS '메뉴 트리';

CREATE INDEX IDX_T_MENU_PARENT ON T_MENU (PARENT_NO);
CREATE SEQUENCE SEQ_T_MENU START WITH 1000 INCREMENT BY 10 NOCACHE;


-- ---------------------------------------------------------------------
-- 6. T_AUTH_MENU : 권한 ↔ 메뉴 매핑 (R/W)
-- ---------------------------------------------------------------------
CREATE TABLE T_AUTH_MENU (
    AUTH_CODE           VARCHAR2(50)   NOT NULL,
    MENU_NO             NUMBER(10)     NOT NULL,
    READ_YN             CHAR(1) DEFAULT 'Y',        -- 조회권한
    WRITE_YN            CHAR(1) DEFAULT 'N',        -- 등록/수정/삭제권한
    USE_YN              CHAR(1) DEFAULT 'Y',        -- 매핑 활성화 여부 (N이면 해당 권한에서 메뉴 숨김)
    REG_DT              TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT PK_T_AUTH_MENU PRIMARY KEY (AUTH_CODE, MENU_NO),
    CONSTRAINT FK_AM_AUTH FOREIGN KEY (AUTH_CODE) REFERENCES T_AUTH(AUTH_CODE),
    CONSTRAINT FK_AM_MENU FOREIGN KEY (MENU_NO)   REFERENCES T_MENU(MENU_NO)
);
COMMENT ON TABLE  T_AUTH_MENU IS '권한-메뉴 매핑';
COMMENT ON COLUMN T_AUTH_MENU.USE_YN IS '매핑 활성화 여부 (N:비활성 - 권한 있어도 메뉴 숨김)';


-- ---------------------------------------------------------------------
-- 7. T_LOGIN_HIST : 로그인 이력
-- ---------------------------------------------------------------------
CREATE TABLE T_LOGIN_HIST (
    LOGIN_SEQ           NUMBER(15)     NOT NULL,
    USER_ID             VARCHAR2(20)   NOT NULL,
    LOGIN_DT            TIMESTAMP DEFAULT SYSTIMESTAMP,
    LOGIN_IP            VARCHAR2(45),
    LOGIN_RESULT        CHAR(1),                    -- S:성공 F:실패
    FAIL_REASON         VARCHAR2(200),
    LOGOUT_DT           TIMESTAMP,
    USER_AGENT          VARCHAR2(500),
    CONSTRAINT PK_T_LOGIN_HIST PRIMARY KEY (LOGIN_SEQ)
);
COMMENT ON TABLE T_LOGIN_HIST IS '로그인 이력';

CREATE INDEX IDX_T_LOGIN_HIST_USER ON T_LOGIN_HIST (USER_ID, LOGIN_DT DESC);
CREATE SEQUENCE SEQ_T_LOGIN_HIST START WITH 1 INCREMENT BY 1 NOCACHE;


-- ---------------------------------------------------------------------
-- 8. T_USER_AUTH_HIST : 권한 변경 이력
-- ---------------------------------------------------------------------
CREATE TABLE T_USER_AUTH_HIST (
    HIST_SEQ            NUMBER(15)     NOT NULL,
    USER_ID             VARCHAR2(20)   NOT NULL,
    BEFORE_GROUP_NO     NUMBER(10),
    AFTER_GROUP_NO      NUMBER(10),
    CHG_REASON          VARCHAR2(200),
    REG_DT              TIMESTAMP DEFAULT SYSTIMESTAMP,
    REG_ID              VARCHAR2(20),
    CONSTRAINT PK_T_USER_AUTH_HIST PRIMARY KEY (HIST_SEQ)
);
COMMENT ON TABLE T_USER_AUTH_HIST IS '사용자 권한 변경 이력';

CREATE SEQUENCE SEQ_T_USER_AUTH_HIST START WITH 1 INCREMENT BY 1 NOCACHE;


COMMIT;
