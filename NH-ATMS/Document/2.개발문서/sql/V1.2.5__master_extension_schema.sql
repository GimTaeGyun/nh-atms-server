-- =====================================================================
-- V1.2.5 : 마스터 보강 테이블 7종
--   T_ATM_MODEL        : 기기모델 마스터        (3202)
--   T_VENDOR          : 용역업체 마스터        (6303/6002/6004/3202)
--   T_VENDOR_ATM       : 용역업체-기기 매핑      (6303/6302)   ※ T_VENDOR 선행
--   T_ERROR_MESSAGE    : ATM 에러메시지 마스터   (7203/7002/2301/3101)
--   T_HOLIDAY         : 휴일 마스터            (마감/통계 공통)
--   T_FEE             : 출금수수료 마스터       (거래/정산 공통)
--   T_DISPATCH_HISTORY : 출동조치 이력          (6002/6004)
--
-- 근거: 09_메뉴별_테이블_매핑_및_추가설계.md (3.2)
--   AS-IS '자동화기기_ATM에러메세지 / 휴일코드 / 엡출금수수료 / 용역관리기기정보' 기반.
-- 선행: V1.2.4. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_ATM_MODEL (T_기기모델마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_MODEL (
    MODEL_CD      CHAR(3)        NOT NULL,   -- 모델코드
    MODEL_NAME    VARCHAR2(100),             -- 모델명
    ATM_CO_CD     CHAR(2),                   -- 기기업체코드(효성/청호/에이텍)
    DEVICE_TYPE   CHAR(2),                   -- 기기유형(ATM/CD/STM)
    CASSETTE_CNT  NUMBER(10),                -- 카세트수
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_DATE      CHAR(8),                   -- 등록일
    UPD_DATE      CHAR(8),                   -- 수정일
    CONSTRAINT PK_T_ATM_MODEL PRIMARY KEY (MODEL_CD)
);

COMMENT ON TABLE  T_ATM_MODEL              IS 'T_기기모델마스터';
COMMENT ON COLUMN T_ATM_MODEL.MODEL_CD     IS '모델코드';
COMMENT ON COLUMN T_ATM_MODEL.MODEL_NAME   IS '모델명';
COMMENT ON COLUMN T_ATM_MODEL.ATM_CO_CD    IS '기기업체코드';
COMMENT ON COLUMN T_ATM_MODEL.DEVICE_TYPE  IS '기기유형';
COMMENT ON COLUMN T_ATM_MODEL.CASSETTE_CNT IS '카세트수';
COMMENT ON COLUMN T_ATM_MODEL.USE_YN       IS '사용여부';
COMMENT ON COLUMN T_ATM_MODEL.REG_DATE     IS '등록일';
COMMENT ON COLUMN T_ATM_MODEL.UPD_DATE     IS '수정일';

-- ---------------------------------------------------------------------
-- 2) T_VENDOR (T_용역업체마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_VENDOR (
    VENDOR_CD     CHAR(4)        NOT NULL,   -- 용역업체코드
    VENDOR_NAME   VARCHAR2(100),             -- 용역업체명
    VENDOR_TYPE   CHAR(2),                   -- 업체구분(경비/유지보수/현금수송)
    BIZ_NO        VARCHAR2(12),              -- 사업자번호
    PHONE_NO      VARCHAR2(50),              -- 대표전화
    EMERGENCY_NO  VARCHAR2(50),              -- 비상연락처
    MANAGER_NAME  VARCHAR2(50),              -- 담당자명
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_DATE      CHAR(8),                   -- 등록일
    UPD_DATE      CHAR(8),                   -- 수정일
    CONSTRAINT PK_T_VENDOR PRIMARY KEY (VENDOR_CD)
);

COMMENT ON TABLE  T_VENDOR              IS 'T_용역업체마스터';
COMMENT ON COLUMN T_VENDOR.VENDOR_CD    IS '용역업체코드';
COMMENT ON COLUMN T_VENDOR.VENDOR_NAME  IS '용역업체명';
COMMENT ON COLUMN T_VENDOR.VENDOR_TYPE  IS '업체구분';
COMMENT ON COLUMN T_VENDOR.BIZ_NO       IS '사업자번호';
COMMENT ON COLUMN T_VENDOR.PHONE_NO     IS '대표전화';
COMMENT ON COLUMN T_VENDOR.EMERGENCY_NO IS '비상연락처';
COMMENT ON COLUMN T_VENDOR.MANAGER_NAME IS '담당자명';
COMMENT ON COLUMN T_VENDOR.USE_YN       IS '사용여부';
COMMENT ON COLUMN T_VENDOR.REG_DATE     IS '등록일';
COMMENT ON COLUMN T_VENDOR.UPD_DATE     IS '수정일';

-- ---------------------------------------------------------------------
-- 3) T_VENDOR_ATM (T_용역업체기기매핑)  ※ T_VENDOR / T_ATM 논리참조
-- ---------------------------------------------------------------------
CREATE TABLE T_VENDOR_ATM (
    VENDOR_CD     CHAR(4)        NOT NULL,   -- 용역업체코드
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    DISPATCH_SEQ  NUMBER(10)     NOT NULL,   -- 차수(1차/2차)
    ASSIGN_DATE   CHAR(8),                   -- 배정일
    RELEASE_DATE  CHAR(8),                   -- 해제일
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    CONSTRAINT PK_T_VENDOR_ATM PRIMARY KEY
        (VENDOR_CD, TERM_GROUP_ID, TERM_ID, DISPATCH_SEQ)
);

COMMENT ON TABLE  T_VENDOR_ATM               IS 'T_용역업체기기매핑';
COMMENT ON COLUMN T_VENDOR_ATM.VENDOR_CD     IS '용역업체코드';
COMMENT ON COLUMN T_VENDOR_ATM.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_VENDOR_ATM.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_VENDOR_ATM.DISPATCH_SEQ  IS '차수';
COMMENT ON COLUMN T_VENDOR_ATM.ASSIGN_DATE   IS '배정일';
COMMENT ON COLUMN T_VENDOR_ATM.RELEASE_DATE  IS '해제일';
COMMENT ON COLUMN T_VENDOR_ATM.USE_YN        IS '사용여부';

CREATE INDEX IX_T_VENDOR_ATM_ATM ON T_VENDOR_ATM (TERM_GROUP_ID, TERM_ID);

-- ---------------------------------------------------------------------
-- 4) T_ERROR_MESSAGE (T_에러메시지마스터)
--    AS-IS 자동화기기_ATM에러메세지: 구분코드(2)+에러코드(4) 복합키.
-- ---------------------------------------------------------------------
CREATE TABLE T_ERROR_MESSAGE (
    ERR_GROUP_CD  CHAR(2)        NOT NULL,   -- 에러구분코드
    ERR_CD        VARCHAR2(4)    NOT NULL,   -- 에러코드
    ERR_NAME      VARCHAR2(100),             -- 에러명
    SLIP_MSG      VARCHAR2(255),             -- 명세표인자메시지
    SCREEN_MSG    VARCHAR2(255),             -- 화면표시메시지
    MODULE_TYPE   CHAR(2),                   -- 모듈구분
    SEVERITY      CHAR(1),                   -- 심각도(1~5)
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_DATE      CHAR(8),                   -- 등록일
    UPD_USER_ID   VARCHAR2(10),              -- 수정자ID
    UPD_DATE      CHAR(8),                   -- 수정일
    CONSTRAINT PK_T_ERROR_MESSAGE PRIMARY KEY (ERR_GROUP_CD, ERR_CD)
);

COMMENT ON TABLE  T_ERROR_MESSAGE              IS 'T_에러메시지마스터';
COMMENT ON COLUMN T_ERROR_MESSAGE.ERR_GROUP_CD IS '에러구분코드';
COMMENT ON COLUMN T_ERROR_MESSAGE.ERR_CD       IS '에러코드';
COMMENT ON COLUMN T_ERROR_MESSAGE.ERR_NAME     IS '에러명';
COMMENT ON COLUMN T_ERROR_MESSAGE.SLIP_MSG     IS '명세표인자메시지';
COMMENT ON COLUMN T_ERROR_MESSAGE.SCREEN_MSG   IS '화면표시메시지';
COMMENT ON COLUMN T_ERROR_MESSAGE.MODULE_TYPE  IS '모듈구분';
COMMENT ON COLUMN T_ERROR_MESSAGE.SEVERITY     IS '심각도';
COMMENT ON COLUMN T_ERROR_MESSAGE.USE_YN       IS '사용여부';
COMMENT ON COLUMN T_ERROR_MESSAGE.REG_DATE     IS '등록일';
COMMENT ON COLUMN T_ERROR_MESSAGE.UPD_USER_ID  IS '수정자ID';
COMMENT ON COLUMN T_ERROR_MESSAGE.UPD_DATE     IS '수정일';

-- ---------------------------------------------------------------------
-- 5) T_HOLIDAY (T_휴일마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_HOLIDAY (
    HOLIDAY_DATE  CHAR(8)        NOT NULL,   -- 해당일자(YYYYMMDD)
    HOLIDAY_NAME  VARCHAR2(100),             -- 휴일명
    HOLIDAY_TYPE  CHAR(1),                   -- 구분(법정/임시/주말)
    BATCH_CLOSE_YN CHAR(1)       DEFAULT 'N',-- 일괄마감여부
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    CONSTRAINT PK_T_HOLIDAY PRIMARY KEY (HOLIDAY_DATE)
);

COMMENT ON TABLE  T_HOLIDAY                IS 'T_휴일마스터';
COMMENT ON COLUMN T_HOLIDAY.HOLIDAY_DATE   IS '해당일자';
COMMENT ON COLUMN T_HOLIDAY.HOLIDAY_NAME   IS '휴일명';
COMMENT ON COLUMN T_HOLIDAY.HOLIDAY_TYPE   IS '구분';
COMMENT ON COLUMN T_HOLIDAY.BATCH_CLOSE_YN IS '일괄마감여부';
COMMENT ON COLUMN T_HOLIDAY.USE_YN         IS '사용여부';

-- ---------------------------------------------------------------------
-- 6) T_FEE (T_출금수수료마스터)
--    거래구분/자타구분/시간대별 수수료 정책.
-- ---------------------------------------------------------------------
CREATE TABLE T_FEE (
    FEE_CD        CHAR(4)        NOT NULL,   -- 수수료코드
    TR_GUBUN_CD   CHAR(2),                   -- 거래구분코드
    SELF_FLAG     CHAR(1),                   -- 자타구분
    TIME_ZONE     CHAR(1),                   -- 시간대구분(영업중/마감후)
    FEE_AMOUNT    NUMBER(19),                -- 수수료금액
    APPLY_START   CHAR(8),                   -- 적용시작일
    APPLY_END     CHAR(8),                   -- 적용종료일
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    CONSTRAINT PK_T_FEE PRIMARY KEY (FEE_CD)
);

COMMENT ON TABLE  T_FEE              IS 'T_출금수수료마스터';
COMMENT ON COLUMN T_FEE.FEE_CD       IS '수수료코드';
COMMENT ON COLUMN T_FEE.TR_GUBUN_CD  IS '거래구분코드';
COMMENT ON COLUMN T_FEE.SELF_FLAG    IS '자타구분';
COMMENT ON COLUMN T_FEE.TIME_ZONE    IS '시간대구분';
COMMENT ON COLUMN T_FEE.FEE_AMOUNT   IS '수수료금액';
COMMENT ON COLUMN T_FEE.APPLY_START  IS '적용시작일';
COMMENT ON COLUMN T_FEE.APPLY_END    IS '적용종료일';
COMMENT ON COLUMN T_FEE.USE_YN       IS '사용여부';

-- ---------------------------------------------------------------------
-- 7) T_DISPATCH_HISTORY (T_출동조치이력)
--    장애 발생 시 용역업체 출동/조치 내역 (1차/2차 출동).
-- ---------------------------------------------------------------------
CREATE TABLE T_DISPATCH_HISTORY (
    DISPATCH_DATE CHAR(8)        NOT NULL,   -- 출동일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    DISPATCH_SEQ  NUMBER(10)     NOT NULL,   -- 출동차수
    VENDOR_CD     CHAR(4),                   -- 용역업체코드
    FAULT_DATE    CHAR(8),                   -- 장애발생일
    FAULT_TIME    CHAR(6),                   -- 장애발생시간
    ERR_CD        VARCHAR2(10),              -- 장애코드
    REQUEST_TIME  CHAR(14),                  -- 출동요청일시
    ARRIVE_TIME   CHAR(14),                  -- 도착일시
    FINISH_TIME   CHAR(14),                  -- 조치완료일시
    DISPATCH_STATUS CHAR(2),                 -- 출동상태(요청/진행/완료)
    ACTION_DESC   VARCHAR2(500),             -- 조치내용
    WORKER_NAME   VARCHAR2(50),              -- 작업자명
    CONSTRAINT PK_T_DISPATCH_HISTORY PRIMARY KEY
        (DISPATCH_DATE, TERM_GROUP_ID, TERM_ID, DISPATCH_SEQ)
);

COMMENT ON TABLE  T_DISPATCH_HISTORY                 IS 'T_출동조치이력';
COMMENT ON COLUMN T_DISPATCH_HISTORY.DISPATCH_DATE   IS '출동일자';
COMMENT ON COLUMN T_DISPATCH_HISTORY.TERM_GROUP_ID   IS '영업점코드';
COMMENT ON COLUMN T_DISPATCH_HISTORY.TERM_ID         IS '단말번호';
COMMENT ON COLUMN T_DISPATCH_HISTORY.DISPATCH_SEQ    IS '출동차수';
COMMENT ON COLUMN T_DISPATCH_HISTORY.VENDOR_CD       IS '용역업체코드';
COMMENT ON COLUMN T_DISPATCH_HISTORY.FAULT_DATE      IS '장애발생일';
COMMENT ON COLUMN T_DISPATCH_HISTORY.FAULT_TIME      IS '장애발생시간';
COMMENT ON COLUMN T_DISPATCH_HISTORY.ERR_CD          IS '장애코드';
COMMENT ON COLUMN T_DISPATCH_HISTORY.REQUEST_TIME    IS '출동요청일시';
COMMENT ON COLUMN T_DISPATCH_HISTORY.ARRIVE_TIME     IS '도착일시';
COMMENT ON COLUMN T_DISPATCH_HISTORY.FINISH_TIME     IS '조치완료일시';
COMMENT ON COLUMN T_DISPATCH_HISTORY.DISPATCH_STATUS IS '출동상태';
COMMENT ON COLUMN T_DISPATCH_HISTORY.ACTION_DESC     IS '조치내용';
COMMENT ON COLUMN T_DISPATCH_HISTORY.WORKER_NAME     IS '작업자명';

CREATE INDEX IX_T_DISPATCH_VENDOR ON T_DISPATCH_HISTORY (VENDOR_CD, DISPATCH_DATE);
CREATE INDEX IX_T_DISPATCH_ATM    ON T_DISPATCH_HISTORY (TERM_GROUP_ID, TERM_ID, DISPATCH_DATE);

COMMIT;
