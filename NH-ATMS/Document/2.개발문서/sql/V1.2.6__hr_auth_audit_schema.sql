-- =====================================================================
-- V1.2.6 : 인사/권한/시스템설정/감사 테이블 7종
--   T_PERSON                : 인사정보 마스터        (3206/7100)
--   T_JOURNAL_AUTH           : 저널열람권한          (4002)   ※ T_PERSON 논리참조
--   T_JOURNAL_AUTH_HIST       : 저널권한 변경이력      (4002)
--   T_SYSTEM_CONFIG          : 시스템 공통설정        (4101/3002)
--   T_JOURNAL_VIEW_LOG        : 거래내역 열람 감사로그   (4003)
--   T_PERSONAL_INFO_PRINT_HIST : 개인정보 출력 감사로그   (1020/1301)
--   T_DATA_DELETE_HIST        : 데이터삭제 이력        (법적 의무)
--
-- 근거: 09_메뉴별_테이블_매핑_및_추가설계.md (3.3)
--   AS-IS '자동화기기_인사정보 / 저널권한 / 저널권한이력 / 시스템공통관리 /
--          거래내역열람이력 / 개인정보출력이력 / 데이터삭제이력' 기반.
-- 선행: V1.2.5. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_PERSON (T_인사정보마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_PERSON (
    PERSON_NO     VARCHAR2(9)    NOT NULL,   -- 인사개인번호
    PERSON_NAME   VARCHAR2(50),              -- 성명
    DEPT_CD       CHAR(5),                   -- 부서코드
    TERM_GROUP_ID CHAR(5),                   -- 소속영업점코드
    POSITION_CD   CHAR(3),                   -- 직급코드
    PHONE_NO      VARCHAR2(50),              -- 연락처
    EMAIL         VARCHAR2(100),             -- 이메일
    DUTY_DESC     VARCHAR2(200),             -- 담당업무
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 재직여부
    REG_DATE      CHAR(8),                   -- 등록일
    UPD_DATE      CHAR(8),                   -- 수정일
    CONSTRAINT PK_T_PERSON PRIMARY KEY (PERSON_NO)
);

COMMENT ON TABLE  T_PERSON               IS 'T_인사정보마스터';
COMMENT ON COLUMN T_PERSON.PERSON_NO     IS '인사개인번호';
COMMENT ON COLUMN T_PERSON.PERSON_NAME   IS '성명';
COMMENT ON COLUMN T_PERSON.DEPT_CD       IS '부서코드';
COMMENT ON COLUMN T_PERSON.TERM_GROUP_ID IS '소속영업점코드';
COMMENT ON COLUMN T_PERSON.POSITION_CD   IS '직급코드';
COMMENT ON COLUMN T_PERSON.PHONE_NO      IS '연락처';
COMMENT ON COLUMN T_PERSON.EMAIL         IS '이메일';
COMMENT ON COLUMN T_PERSON.DUTY_DESC     IS '담당업무';
COMMENT ON COLUMN T_PERSON.USE_YN        IS '재직여부';
COMMENT ON COLUMN T_PERSON.REG_DATE      IS '등록일';
COMMENT ON COLUMN T_PERSON.UPD_DATE      IS '수정일';

CREATE INDEX IX_T_PERSON_GROUP ON T_PERSON (TERM_GROUP_ID, DEPT_CD);

-- ---------------------------------------------------------------------
-- 2) T_JOURNAL_AUTH (T_저널열람권한)
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL_AUTH (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    PERSON_NO     VARCHAR2(9)    NOT NULL,   -- 직원개인번호
    AUTH_CD       CHAR(2)        NOT NULL,   -- 권한코드(조회/이미지/관리)
    ASSIGN_DATE   CHAR(8),                   -- 지정일
    ASSIGN_BY     VARCHAR2(9),               -- 지정자
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    CONSTRAINT PK_T_JOURNAL_AUTH PRIMARY KEY (TERM_GROUP_ID, PERSON_NO, AUTH_CD)
);

COMMENT ON TABLE  T_JOURNAL_AUTH               IS 'T_저널열람권한';
COMMENT ON COLUMN T_JOURNAL_AUTH.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_JOURNAL_AUTH.PERSON_NO     IS '직원개인번호';
COMMENT ON COLUMN T_JOURNAL_AUTH.AUTH_CD       IS '권한코드';
COMMENT ON COLUMN T_JOURNAL_AUTH.ASSIGN_DATE   IS '지정일';
COMMENT ON COLUMN T_JOURNAL_AUTH.ASSIGN_BY     IS '지정자';
COMMENT ON COLUMN T_JOURNAL_AUTH.USE_YN        IS '사용여부';

-- ---------------------------------------------------------------------
-- 3) T_JOURNAL_AUTH_HIST (T_저널권한이력)
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL_AUTH_HIST (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    PERSON_NO     VARCHAR2(9)    NOT NULL,   -- 직원개인번호
    AUTH_CD       CHAR(2)        NOT NULL,   -- 권한코드
    CHANGE_SEQ    NUMBER(19)     NOT NULL,   -- 변경일련번호
    CHANGE_TYPE   CHAR(1),                   -- 변경구분(부여/해제)
    ASSIGN_DATE   CHAR(8),                   -- 지정일
    RELEASE_DATE  CHAR(8),                   -- 해제일
    CHANGE_BY     VARCHAR2(9),               -- 처리자
    REG_DATE      CHAR(14),                  -- 이력생성일시
    CONSTRAINT PK_T_JOURNAL_AUTH_HIST PRIMARY KEY
        (TERM_GROUP_ID, PERSON_NO, AUTH_CD, CHANGE_SEQ)
);

COMMENT ON TABLE  T_JOURNAL_AUTH_HIST               IS 'T_저널권한이력';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.PERSON_NO     IS '직원개인번호';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.AUTH_CD       IS '권한코드';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.CHANGE_SEQ    IS '변경일련번호';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.CHANGE_TYPE   IS '변경구분';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.ASSIGN_DATE   IS '지정일';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.RELEASE_DATE  IS '해제일';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.CHANGE_BY     IS '처리자';
COMMENT ON COLUMN T_JOURNAL_AUTH_HIST.REG_DATE      IS '이력생성일시';

-- ---------------------------------------------------------------------
-- 4) T_SYSTEM_CONFIG (T_시스템공통설정)
--    key-value 형태의 시스템 전역 운영환경 설정.
-- ---------------------------------------------------------------------
CREATE TABLE T_SYSTEM_CONFIG (
    CONFIG_GROUP  CHAR(10)       NOT NULL,   -- 설정그룹
    CONFIG_KEY    VARCHAR2(50)   NOT NULL,   -- 설정키
    CONFIG_VALUE  VARCHAR2(500),             -- 설정값
    VALUE_TYPE    CHAR(1),                   -- 값유형(문자/숫자/YN)
    DESCRIPTION   VARCHAR2(200),             -- 설명
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    UPD_USER_ID   VARCHAR2(10),              -- 수정자ID
    UPD_DATE      CHAR(14),                  -- 수정일시
    CONSTRAINT PK_T_SYSTEM_CONFIG PRIMARY KEY (CONFIG_GROUP, CONFIG_KEY)
);

COMMENT ON TABLE  T_SYSTEM_CONFIG              IS 'T_시스템공통설정';
COMMENT ON COLUMN T_SYSTEM_CONFIG.CONFIG_GROUP IS '설정그룹';
COMMENT ON COLUMN T_SYSTEM_CONFIG.CONFIG_KEY   IS '설정키';
COMMENT ON COLUMN T_SYSTEM_CONFIG.CONFIG_VALUE IS '설정값';
COMMENT ON COLUMN T_SYSTEM_CONFIG.VALUE_TYPE   IS '값유형';
COMMENT ON COLUMN T_SYSTEM_CONFIG.DESCRIPTION  IS '설명';
COMMENT ON COLUMN T_SYSTEM_CONFIG.USE_YN       IS '사용여부';
COMMENT ON COLUMN T_SYSTEM_CONFIG.UPD_USER_ID  IS '수정자ID';
COMMENT ON COLUMN T_SYSTEM_CONFIG.UPD_DATE     IS '수정일시';

-- ---------------------------------------------------------------------
-- 5) T_JOURNAL_VIEW_LOG (T_거래내역열람감사로그)
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL_VIEW_LOG (
    VIEW_DATE     CHAR(8)        NOT NULL,   -- 조회일자
    VIEW_SEQ      NUMBER(19)     NOT NULL,   -- 조회일련번호
    VIEW_TIME     CHAR(6),                   -- 조회시간
    PERSON_NO     VARCHAR2(9),               -- 조회자개인번호
    SCREEN_NO     CHAR(4),                   -- 조회화면번호
    ACCESS_CNT    NUMBER(10),                -- 접근건수
    VIEW_DESC     VARCHAR2(200),             -- 조회내용
    TERM_GROUP_ID CHAR(5),                   -- 영업점코드
    CONSTRAINT PK_T_JOURNAL_VIEW_LOG PRIMARY KEY (VIEW_DATE, VIEW_SEQ)
);

COMMENT ON TABLE  T_JOURNAL_VIEW_LOG               IS 'T_거래내역열람감사로그';
COMMENT ON COLUMN T_JOURNAL_VIEW_LOG.VIEW_DATE     IS '조회일자';
COMMENT ON COLUMN T_JOURNAL_VIEW_LOG.VIEW_SEQ      IS '조회일련번호';
COMMENT ON COLUMN T_JOURNAL_VIEW_LOG.VIEW_TIME     IS '조회시간';
COMMENT ON COLUMN T_JOURNAL_VIEW_LOG.PERSON_NO     IS '조회자개인번호';
COMMENT ON COLUMN T_JOURNAL_VIEW_LOG.SCREEN_NO     IS '조회화면번호';
COMMENT ON COLUMN T_JOURNAL_VIEW_LOG.ACCESS_CNT    IS '접근건수';
COMMENT ON COLUMN T_JOURNAL_VIEW_LOG.VIEW_DESC     IS '조회내용';
COMMENT ON COLUMN T_JOURNAL_VIEW_LOG.TERM_GROUP_ID IS '영업점코드';

CREATE INDEX IX_T_JNLVIEWLOG_PERSON ON T_JOURNAL_VIEW_LOG (PERSON_NO, VIEW_DATE);

-- ---------------------------------------------------------------------
-- 6) T_PERSONAL_INFO_PRINT_HIST (T_개인정보출력감사로그)
--    1020/1301 거래내역 2차 파기 조회 등 개인정보 출력 시 감사기록.
-- ---------------------------------------------------------------------
CREATE TABLE T_PERSONAL_INFO_PRINT_HIST (
    PRINT_DATE    CHAR(8)        NOT NULL,   -- 출력일자
    PRINT_SEQ     NUMBER(19)     NOT NULL,   -- 출력일련번호
    PRINT_TIME    CHAR(6),                   -- 출력시간
    PERSON_NO     VARCHAR2(9),               -- 조작자개인번호
    SCREEN_CAT    CHAR(2),                   -- 화면대분류
    SCREEN_NAME   VARCHAR2(100),             -- 화면명
    PRINT_CONTENT VARCHAR2(500),             -- 출력내용
    REASON_CD     CHAR(2),                   -- 조회사유코드
    REASON_DESC   VARCHAR2(200),             -- 상세사유
    PRINT_CNT     NUMBER(10),                -- 출력건수
    APPROVER_NO   VARCHAR2(9),               -- 승인자개인번호
    CONSTRAINT PK_T_PERSONAL_INFO_PRINT_HIST PRIMARY KEY (PRINT_DATE, PRINT_SEQ)
);

COMMENT ON TABLE  T_PERSONAL_INFO_PRINT_HIST               IS 'T_개인정보출력감사로그';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.PRINT_DATE    IS '출력일자';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.PRINT_SEQ     IS '출력일련번호';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.PRINT_TIME    IS '출력시간';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.PERSON_NO     IS '조작자개인번호';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.SCREEN_CAT    IS '화면대분류';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.SCREEN_NAME   IS '화면명';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.PRINT_CONTENT IS '출력내용';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.REASON_CD     IS '조회사유코드';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.REASON_DESC   IS '상세사유';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.PRINT_CNT     IS '출력건수';
COMMENT ON COLUMN T_PERSONAL_INFO_PRINT_HIST.APPROVER_NO   IS '승인자개인번호';

CREATE INDEX IX_T_PIPRINTHIST_PERSON ON T_PERSONAL_INFO_PRINT_HIST (PERSON_NO, PRINT_DATE);

-- ---------------------------------------------------------------------
-- 7) T_DATA_DELETE_HIST (T_데이터삭제이력)
-- ---------------------------------------------------------------------
CREATE TABLE T_DATA_DELETE_HIST (
    DELETE_DATE   CHAR(8)        NOT NULL,   -- 작업일자
    DELETE_SEQ    NUMBER(19)     NOT NULL,   -- 작업일련번호
    DELETE_TIME   CHAR(6),                   -- 작업시간
    TABLE_NAME    VARCHAR2(30),              -- 대상테이블명
    DELETE_DESC   VARCHAR2(200),             -- 삭제설명
    DELETE_CNT    NUMBER(19),                -- 삭제건수
    WORK_TYPE_CD  CHAR(2),                   -- 작업구분코드(수동/배치)
    WORKER_ID     VARCHAR2(10),              -- 작업자ID
    CONSTRAINT PK_T_DATA_DELETE_HIST PRIMARY KEY (DELETE_DATE, DELETE_SEQ)
);

COMMENT ON TABLE  T_DATA_DELETE_HIST              IS 'T_데이터삭제이력';
COMMENT ON COLUMN T_DATA_DELETE_HIST.DELETE_DATE  IS '작업일자';
COMMENT ON COLUMN T_DATA_DELETE_HIST.DELETE_SEQ   IS '작업일련번호';
COMMENT ON COLUMN T_DATA_DELETE_HIST.DELETE_TIME  IS '작업시간';
COMMENT ON COLUMN T_DATA_DELETE_HIST.TABLE_NAME   IS '대상테이블명';
COMMENT ON COLUMN T_DATA_DELETE_HIST.DELETE_DESC  IS '삭제설명';
COMMENT ON COLUMN T_DATA_DELETE_HIST.DELETE_CNT   IS '삭제건수';
COMMENT ON COLUMN T_DATA_DELETE_HIST.WORK_TYPE_CD IS '작업구분코드';
COMMENT ON COLUMN T_DATA_DELETE_HIST.WORKER_ID    IS '작업자ID';

COMMIT;
