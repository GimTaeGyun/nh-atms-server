-- =====================================================================
-- V1.4.4 : 개국/장애분석 마스터 보강 테이블
--   T_ATM_HISTORY : 12 개국 - 기기 설치/변경 이력          (T_기기설치이력)
--   T_ATM_UNIT    : 장치 마스터(장애맵 조인용)             (T_장치정보)
--   T_FAULT_MAP   : 장치 상태코드 -> 장애그룹/분류 맵핑     (T_자동화기기 장애그룹코드 맵핑정보)
--
-- 레거시 datm.t_atmhistory / t_atmunit / t_faultmap (MySQL) 을 Oracle 로 이식.
-- T_ATM_UNIT / T_FAULT_MAP 는 기준정보 테이블이므로 운영 데이터를 별도 적재해야
-- 21/22 장애분석이 정상 동작한다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_ATM_HISTORY (T_기기설치이력) - 12 개국 시 기기 등록/변경 이력
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_HISTORY (
    MACHINE_SNO   VARCHAR2(20)  NOT NULL,   -- 제조일련번호
    UPDATE_DATE   CHAR(8)       NOT NULL,   -- 변경일
    UPDATE_TIME   CHAR(6)       NOT NULL,   -- 변경시간
    TERM_GROUP_ID CHAR(5)       NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)  NOT NULL,   -- 단말번호
    ATM_CO_CD     CHAR(2),                  -- 기기업체코드
    MODEL_CD      CHAR(3),                  -- 모델코드
    UPDATE_CD     CHAR(1),                  -- 변경구분(1:신규등록, 3:제조번호변경)
    UPDATE_DESC   VARCHAR2(255),            -- 변경내용
    CONSTRAINT PK_T_ATM_HISTORY PRIMARY KEY (MACHINE_SNO, UPDATE_DATE, UPDATE_TIME)
);

COMMENT ON TABLE  T_ATM_HISTORY               IS 'T_기기설치이력';
COMMENT ON COLUMN T_ATM_HISTORY.MACHINE_SNO   IS '제조일련번호';
COMMENT ON COLUMN T_ATM_HISTORY.UPDATE_DATE   IS '변경일';
COMMENT ON COLUMN T_ATM_HISTORY.UPDATE_TIME   IS '변경시간';
COMMENT ON COLUMN T_ATM_HISTORY.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_ATM_HISTORY.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_ATM_HISTORY.ATM_CO_CD     IS '기기업체코드';
COMMENT ON COLUMN T_ATM_HISTORY.MODEL_CD      IS '모델코드';
COMMENT ON COLUMN T_ATM_HISTORY.UPDATE_CD     IS '변경구분';
COMMENT ON COLUMN T_ATM_HISTORY.UPDATE_DESC   IS '변경내용';

-- ---------------------------------------------------------------------
-- 2) T_ATM_UNIT (T_장치정보) - 장치 마스터
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_UNIT (
    DEVICE_ID       CHAR(1)       NOT NULL,   -- 장치ID
    DEVICE_NAME     VARCHAR2(40)  NOT NULL,   -- 장치명
    FAULT_THRESHOLD NUMBER(19),               -- 장애임계값
    SUPPLY_MODULE1  CHAR(1),                  -- 공급모듈1
    SUPPLY_MODULE2  CHAR(1),                  -- 공급모듈2
    SUPPLY_MODULE3  CHAR(1),                  -- 공급모듈3
    SUPPLY_MODULE4  CHAR(1),                  -- 공급모듈4
    SUPPLY_MODULE5  CHAR(1),                  -- 공급모듈5
    SUPPLY_MODULE6  CHAR(1),                  -- 공급모듈6
    SUPPLY_MODULE7  CHAR(1),                  -- 공급모듈7
    SUPPLY_MODULE8  CHAR(1),                  -- 공급모듈8
    CONSTRAINT PK_T_ATM_UNIT PRIMARY KEY (DEVICE_ID, DEVICE_NAME)
);

COMMENT ON TABLE  T_ATM_UNIT                 IS 'T_장치정보';
COMMENT ON COLUMN T_ATM_UNIT.DEVICE_ID       IS '장치ID';
COMMENT ON COLUMN T_ATM_UNIT.DEVICE_NAME     IS '장치명';
COMMENT ON COLUMN T_ATM_UNIT.FAULT_THRESHOLD IS '장애임계값';

-- ---------------------------------------------------------------------
-- 3) T_FAULT_MAP (T_장애그룹코드 맵핑정보) - 장치 상태코드별 장애 분류
-- ---------------------------------------------------------------------
CREATE TABLE T_FAULT_MAP (
    LANG_CD        CHAR(3)       NOT NULL,   -- 언어코드
    DEVICE_ID      CHAR(1)       NOT NULL,   -- 장치ID
    MODULE_TYPE_CD CHAR(2)       NOT NULL,   -- 모듈구분코드
    STATUS_CD      CHAR(1)       NOT NULL,   -- 상태코드
    FAULT_CLASS_CD CHAR(2)       NOT NULL,   -- 장애대분류코드
    ERR_GROUP_CD   CHAR(2)       NOT NULL,   -- 장애그룹코드
    FAULT_FLAG     CHAR(1),                  -- 장애여부(1:기기장애, 2:운영장애)
    AUTO_REQUEST   CHAR(1),                  -- 자동출동대상여부
    DISPLAY_FLAG   CHAR(1),                  -- 상태표시여부
    MONITOR_FLAG   CHAR(1),                  -- 모니터링여부
    STATUS_NAME    VARCHAR2(40),             -- 상태표시명
    CONSTRAINT PK_T_FAULT_MAP PRIMARY KEY (LANG_CD, DEVICE_ID, MODULE_TYPE_CD, STATUS_CD)
);

COMMENT ON TABLE  T_FAULT_MAP                IS 'T_자동화기기 장애그룹코드 맵핑정보';
COMMENT ON COLUMN T_FAULT_MAP.LANG_CD        IS '언어코드';
COMMENT ON COLUMN T_FAULT_MAP.DEVICE_ID      IS '장치ID';
COMMENT ON COLUMN T_FAULT_MAP.MODULE_TYPE_CD IS '모듈구분코드';
COMMENT ON COLUMN T_FAULT_MAP.STATUS_CD      IS '상태코드';
COMMENT ON COLUMN T_FAULT_MAP.FAULT_CLASS_CD IS '장애대분류코드';
COMMENT ON COLUMN T_FAULT_MAP.ERR_GROUP_CD   IS '장애그룹코드';
COMMENT ON COLUMN T_FAULT_MAP.FAULT_FLAG     IS '장애여부';
COMMENT ON COLUMN T_FAULT_MAP.DISPLAY_FLAG   IS '상태표시여부';
COMMENT ON COLUMN T_FAULT_MAP.MONITOR_FLAG   IS '모니터링여부';
COMMENT ON COLUMN T_FAULT_MAP.STATUS_NAME    IS '상태표시명';

CREATE INDEX IX_T_FAULT_MAP_1 ON T_FAULT_MAP
    (DEVICE_ID, DISPLAY_FLAG, MONITOR_FLAG, STATUS_NAME);

COMMIT;
