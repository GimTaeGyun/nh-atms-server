-- =====================================================================
-- V1.4.5 : 자동출동(대외IF) 마스터/컬럼 보강
--   T_ERRORGROUP       : 장애그룹별 출동업체 설정 (용역/경비/기기 출동여부)
--   T_AUTOREQUESTCHECK : 관리/운영/시간대/장애분류 조합별 출동업체 판정 룰
--   T_HOLIDAY.GROUP_CD : 영업점(금고)별 휴일 구분 컬럼 추가
--   T_FAULT_MANAGEMENT : 자동출동 선정 결과 기록용 컬럼 보강
--
-- 레거시 datm.t_errorgroup / t_autorequestcheck (MySQL) 를 Oracle 로 이식.
-- 두 테이블은 기준정보이므로 운영 데이터를 별도 적재해야 자동출동이 동작한다.
-- (레거시 RecvJnlExtIFServiceImpl.statusFailureJnlExtIFAutoServiceCallHandler 정합)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_ERRORGROUP (T_장애그룹마스터) - 장애그룹별 자동출동/SMS 설정
-- ---------------------------------------------------------------------
CREATE TABLE T_ERRORGROUP (
    LANG_CD        CHAR(3)       NOT NULL,   -- 언어코드
    ERR_GROUP_CD   CHAR(2)       NOT NULL,   -- 장애그룹코드
    ERR_GROUP_NAME VARCHAR2(40),             -- 장애그룹명
    AUTO_REQUEST   CHAR(1),                  -- 자동출동대상여부
    SEND_SMS       CHAR(1),                  -- SMS대상여부
    SERVICE_CALL   CHAR(1),                  -- 용역업체출동여부
    SECURITY_CALL  CHAR(1),                  -- 경비업체출동여부
    ATM_CALL       CHAR(1),                  -- 기기업체출동여부
    VIEW_CATEGORY  CHAR(1),                  -- 표시그룹
    FAULT_CLASS_CD CHAR(2),                  -- 장애대분류코드
    CONSTRAINT PK_T_ERRORGROUP PRIMARY KEY (LANG_CD, ERR_GROUP_CD)
);

COMMENT ON TABLE  T_ERRORGROUP                IS 'T_장애그룹마스터';
COMMENT ON COLUMN T_ERRORGROUP.LANG_CD        IS '언어코드';
COMMENT ON COLUMN T_ERRORGROUP.ERR_GROUP_CD   IS '장애그룹코드';
COMMENT ON COLUMN T_ERRORGROUP.ERR_GROUP_NAME IS '장애그룹명';
COMMENT ON COLUMN T_ERRORGROUP.AUTO_REQUEST   IS '자동출동대상여부';
COMMENT ON COLUMN T_ERRORGROUP.SEND_SMS       IS 'SMS대상여부';
COMMENT ON COLUMN T_ERRORGROUP.SERVICE_CALL   IS '용역업체출동여부';
COMMENT ON COLUMN T_ERRORGROUP.SECURITY_CALL  IS '경비업체출동여부';
COMMENT ON COLUMN T_ERRORGROUP.ATM_CALL       IS '기기업체출동여부';
COMMENT ON COLUMN T_ERRORGROUP.VIEW_CATEGORY  IS '표시그룹';
COMMENT ON COLUMN T_ERRORGROUP.FAULT_CLASS_CD IS '장애대분류코드';

CREATE INDEX IX_T_ERRORGROUP_1 ON T_ERRORGROUP (ERR_GROUP_CD, ERR_GROUP_NAME);

-- ---------------------------------------------------------------------
-- 2) T_AUTOREQUESTCHECK (T_자동출동업체설정) - 출동업체 판정 룰 매트릭스
--    관리구분/운영구분/장애시간대/장애대분류 조합별 출동업체(기기/용역/경비) 플래그
-- ---------------------------------------------------------------------
CREATE TABLE T_AUTOREQUESTCHECK (
    BANK_CD           CHAR(3)       NOT NULL,   -- 은행코드
    MANAGE_TYPE_CD    CHAR(1)       NOT NULL,   -- 관리구분코드(1:금고관리, 2:용역관리)
    OPERATION_TYPE_CD CHAR(1)       NOT NULL,   -- 운영구분코드(1:24/365, 2:365, 3:시간내)
    FAULT_TIME_CD     CHAR(1)       NOT NULL,   -- 장애시간대분류코드(1:영업시간내, 2:영업시간외)
    FAULT_CLASS_CD    CHAR(2)       NOT NULL,   -- 장애대분류코드(01:기기, 02:운영, 00:기기/운영)
    BANK_START_TIME   CHAR(6),                  -- 영업점운영시작시간
    BANK_END_TIME     CHAR(6),                  -- 영업점운영종료시간
    ATM_CO_FLAG       CHAR(1)       DEFAULT '0',-- 기기업체 출동대상여부
    OURSRC_FLAG       CHAR(1)       DEFAULT '0',-- 용역업체 출동대상여부
    GUARD_FLAG        CHAR(1)       DEFAULT '0',-- 경비업체 출동대상여부
    CONSTRAINT PK_T_AUTOREQUESTCHECK PRIMARY KEY
        (BANK_CD, MANAGE_TYPE_CD, OPERATION_TYPE_CD, FAULT_TIME_CD, FAULT_CLASS_CD)
);

COMMENT ON TABLE  T_AUTOREQUESTCHECK                   IS 'T_자동출동업체설정';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.BANK_CD           IS '은행코드';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.MANAGE_TYPE_CD    IS '관리구분코드';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.OPERATION_TYPE_CD IS '운영구분코드';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.FAULT_TIME_CD     IS '장애시간대분류코드';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.FAULT_CLASS_CD    IS '장애대분류코드';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.BANK_START_TIME   IS '영업점운영시작시간';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.BANK_END_TIME     IS '영업점운영종료시간';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.ATM_CO_FLAG       IS '기기업체 출동대상여부';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.OURSRC_FLAG       IS '용역업체 출동대상여부';
COMMENT ON COLUMN T_AUTOREQUESTCHECK.GUARD_FLAG        IS '경비업체 출동대상여부';

-- ---------------------------------------------------------------------
-- 3) T_HOLIDAY.GROUP_CD - 영업점(금고)별 휴일 구분
--    레거시: GROUP_CD IN (영업점코드, '00001'(전국공통)) 으로 휴일 판정.
--    기존 데이터는 전국공통('00001')으로 채운다.
--    PK 변경(GROUP_CD 포함)은 운영 데이터 영향이 크므로 보조 인덱스로 대체하고,
--    금고별 휴일 운영이 필요해지면 PK 재설계를 검토한다.
-- ---------------------------------------------------------------------
ALTER TABLE T_HOLIDAY ADD (GROUP_CD CHAR(5) DEFAULT '00001');
COMMENT ON COLUMN T_HOLIDAY.GROUP_CD IS '영업점(금고)코드(00001=전국공통)';
CREATE INDEX IX_T_HOLIDAY_GROUP ON T_HOLIDAY (GROUP_CD, HOLIDAY_DATE);

-- ---------------------------------------------------------------------
-- 4) T_FAULT_MANAGEMENT 자동출동 선정 결과 컬럼 보강
--    기존: FAULT_SEQ/GLOBAL_ID/TERM_*/FAULT_TYPE/ERR_CD/FAULT_STATUS/CALL_*/VENDOR_CD/FAULT_DESC
--    추가: 출동업체구분, 장애분류/그룹, 저널키, 요청구분 (레거시 T_FAULTMANAGEMENT 정합)
-- ---------------------------------------------------------------------
ALTER TABLE T_FAULT_MANAGEMENT ADD (
    PROVIDER_TYPE_CD CHAR(1),     -- 출동업체구분(1:기기, 2:경비, 3:용역)
    FAULT_CLASS_CD   CHAR(2),     -- 장애대분류코드
    ERR_GROUP_CD     CHAR(2),     -- 장애그룹코드
    JNL_DATE         CHAR(8),     -- 저널발생일(자동출동 근거 저널키)
    JNL_TIME         CHAR(10),    -- 저널발생시간
    REQUEST_TYPE_CD  CHAR(1)      -- 요청구분(3:자동출동요청)
);

COMMENT ON COLUMN T_FAULT_MANAGEMENT.PROVIDER_TYPE_CD IS '출동업체구분(1:기기,2:경비,3:용역)';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.FAULT_CLASS_CD   IS '장애대분류코드';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.ERR_GROUP_CD     IS '장애그룹코드';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.JNL_DATE         IS '저널발생일';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.JNL_TIME         IS '저널발생시간';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.REQUEST_TYPE_CD  IS '요청구분(3:자동출동)';

COMMIT;
