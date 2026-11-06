-- =====================================================================
-- V1.2.2 : 저널 부수효과 상세 테이블 5종
--   (새마을금고 atmr processor 가 거래/장애/시재 저널 처리 시 함께 적재하던 상세 테이블)
--   T_CASH_TRANSACTION   : 71 거래 - 권종별 현금 입출 매수 (T_현금거래상세)
--   T_CHECK_TRANSACTION  : 71 거래 - 수표 입출 명세      (T_수표거래상세)
--   T_CASSETTE_STATUS    : 41 시재 - 카세트별 잔여/상태 스냅샷 (T_카세트상태정보)
--   T_FAULT_ANALYSIS     : 21/22 장애 - 장애 이력/분석    (T_장애이력)
--   T_ATM_STOP_HISTORY    : 21/22 장애 - 기기 운영중지 이력  (T_기기운영이력)
--
-- 출처: 0.기본자료/새마을금고/oracle19c_ddl.sql 의 검증 스키마.
-- 정정 사항(원본의 MySQL 잔재 → Oracle 문법):
--   - T_FAULT_ANALYSIS 의 인라인 `KEY xxx (...)` → 별도 CREATE INDEX 로 분리
--
-- 선행: V1.2.1(T_ATM_STATUS 등 마스터). 적용 후 저널 핸들러 부수효과 보강 가능.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_CASH_TRANSACTION (T_현금거래상세) - 71 거래 권종별 매수
-- ---------------------------------------------------------------------
CREATE TABLE T_CASH_TRANSACTION (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    JNL_DATE      CHAR(8)        NOT NULL,   -- 저널발생일
    JNL_TIME      CHAR(10)       NOT NULL,   -- 저널발생시간
    DENOM_CD      VARCHAR2(10)   NOT NULL,   -- 권종코드
    DENOM_COUNT   NUMBER(19),                -- 거래매수
    CONSTRAINT PK_T_CASH_TRANSACTION PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, DENOM_CD)
);

COMMENT ON TABLE  T_CASH_TRANSACTION               IS 'T_현금거래상세';
COMMENT ON COLUMN T_CASH_TRANSACTION.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_CASH_TRANSACTION.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_CASH_TRANSACTION.JNL_DATE      IS '저널발생일';
COMMENT ON COLUMN T_CASH_TRANSACTION.JNL_TIME      IS '저널발생시간';
COMMENT ON COLUMN T_CASH_TRANSACTION.DENOM_CD      IS '권종코드';
COMMENT ON COLUMN T_CASH_TRANSACTION.DENOM_COUNT   IS '거래매수';

-- ---------------------------------------------------------------------
-- 2) T_CHECK_TRANSACTION (T_수표거래상세) - 71 거래 수표 명세
-- ---------------------------------------------------------------------
CREATE TABLE T_CHECK_TRANSACTION (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    JNL_DATE      CHAR(8)        NOT NULL,   -- 저널발생일
    JNL_TIME      CHAR(10)       NOT NULL,   -- 저널발생시간
    CHECK_SEQ     NUMBER(10)     NOT NULL,   -- 발생일련번호
    CHECK_AMOUNT  VARCHAR2(10),              -- 수표금액
    BANK_CD       CHAR(3),                   -- 은행코드
    CHECK_NO      VARCHAR2(32),              -- 수표번호
    CONSTRAINT PK_T_CHECK_TRANSACTION PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, CHECK_SEQ)
);

COMMENT ON TABLE  T_CHECK_TRANSACTION               IS 'T_수표거래상세';
COMMENT ON COLUMN T_CHECK_TRANSACTION.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_CHECK_TRANSACTION.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_CHECK_TRANSACTION.JNL_DATE      IS '저널발생일';
COMMENT ON COLUMN T_CHECK_TRANSACTION.JNL_TIME      IS '저널발생시간';
COMMENT ON COLUMN T_CHECK_TRANSACTION.CHECK_SEQ     IS '발생일련번호';
COMMENT ON COLUMN T_CHECK_TRANSACTION.CHECK_AMOUNT  IS '수표금액';
COMMENT ON COLUMN T_CHECK_TRANSACTION.BANK_CD       IS '은행코드';
COMMENT ON COLUMN T_CHECK_TRANSACTION.CHECK_NO      IS '수표번호';

-- ---------------------------------------------------------------------
-- 3) T_CASSETTE_STATUS (T_카세트상태정보) - 41 시재 카세트 스냅샷
-- ---------------------------------------------------------------------
CREATE TABLE T_CASSETTE_STATUS (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    CASSETTE_TYPE CHAR(1)        NOT NULL,   -- 카세트구분
    CASSETTE_NO   CHAR(5)        NOT NULL,   -- 카세트번호
    DENOM_CD      VARCHAR2(10),              -- 권종코드
    CURRENCY_CD   CHAR(3),                   -- 통화코드
    REMAIN_COUNT  NUMBER(19),                -- 잔여매수
    STATUS        CHAR(2),                   -- 상태
    CHECK_NO      VARCHAR2(32),              -- 수표번호
    CONSTRAINT PK_T_CASSETTE_STATUS PRIMARY KEY (TERM_GROUP_ID, TERM_ID, CASSETTE_TYPE, CASSETTE_NO)
);

COMMENT ON TABLE  T_CASSETTE_STATUS               IS 'T_카세트상태정보';
COMMENT ON COLUMN T_CASSETTE_STATUS.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_CASSETTE_STATUS.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_CASSETTE_STATUS.CASSETTE_TYPE IS '카세트구분';
COMMENT ON COLUMN T_CASSETTE_STATUS.CASSETTE_NO   IS '카세트번호';
COMMENT ON COLUMN T_CASSETTE_STATUS.DENOM_CD      IS '권종코드';
COMMENT ON COLUMN T_CASSETTE_STATUS.CURRENCY_CD   IS '통화코드';
COMMENT ON COLUMN T_CASSETTE_STATUS.REMAIN_COUNT  IS '잔여매수';
COMMENT ON COLUMN T_CASSETTE_STATUS.STATUS        IS '상태';
COMMENT ON COLUMN T_CASSETTE_STATUS.CHECK_NO      IS '수표번호';

-- ---------------------------------------------------------------------
-- 4) T_FAULT_ANALYSIS (T_장애이력) - 21/22 장애 분석
--    원본 인라인 KEY 2종 → CREATE INDEX 로 분리.
-- ---------------------------------------------------------------------
CREATE TABLE T_FAULT_ANALYSIS (
    TERM_GROUP_ID      CHAR(5)       NOT NULL,   -- 영업점코드
    TERM_ID            VARCHAR2(20)  NOT NULL,   -- 단말번호
    JNL_DATE           CHAR(8)       NOT NULL,   -- 저널발생일
    JNL_TIME           CHAR(10)      NOT NULL,   -- 저널발생시간
    DEVICE_ID          CHAR(1)       NOT NULL,   -- 장치ID
    ERR_GROUP_CD       CHAR(2)       NOT NULL,   -- 장애그룹코드
    MODULE_TYPE_CD     CHAR(2)       NOT NULL,   -- 모듈구분코드
    FAULT_CLASS_CD     CHAR(2),                  -- 장애분류코드
    ERR_CD             VARCHAR2(10),             -- 장애코드
    ATM_CO_CD          CHAR(2),                  -- 기기업체코드
    MODEL_CD           CHAR(3),                  -- 모델코드
    SERVICE_START_TIME CHAR(4),                  -- 운영시작시간
    SERVICE_END_TIME   CHAR(4),                  -- 운영종료시간
    FINISHED_DATE      CHAR(8),                  -- 조치완료일
    FINISHED_TIME      CHAR(6),                  -- 조치완료시간
    STATUS             CHAR(1),                  -- 상태
    AUTO_REQUEST       CHAR(1),                  -- 자동요청여부
    REQUEST_FLAG       CHAR(1),                  -- 요청여부
    DELAY_CHECK_DATE   CHAR(8),                  -- 지연확인일
    DELAY_CHECK_TIME   CHAR(6),                  -- 지연확인시간
    DELAY_WAIT_TIME    NUMBER(19),               -- 지연대기시간
    TIME_ELAPSED       NUMBER(19),               -- 경과시간
    FREQ_FAULT_SNO     CHAR(7),                  -- 다발장애일련번호
    TR_NO              VARCHAR2(4),              -- 거래번호
    TR_CD              VARCHAR2(6),              -- 거래코드
    BANK_CD            CHAR(3),                  -- 은행코드
    ACCOUNT_NO         VARCHAR2(64),             -- 계좌번호
    TR_AMOUNT          NUMBER(19),               -- 거래금액
    CHECK_AMOUNT       NUMBER(19),               -- 수표금액
    CONSTRAINT PK_T_FAULT_ANALYSIS PRIMARY KEY
        (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, DEVICE_ID, ERR_GROUP_CD, MODULE_TYPE_CD)
);

COMMENT ON TABLE  T_FAULT_ANALYSIS                IS 'T_장애이력';
COMMENT ON COLUMN T_FAULT_ANALYSIS.TERM_GROUP_ID  IS '영업점코드';
COMMENT ON COLUMN T_FAULT_ANALYSIS.TERM_ID        IS '단말번호';
COMMENT ON COLUMN T_FAULT_ANALYSIS.JNL_DATE       IS '저널발생일';
COMMENT ON COLUMN T_FAULT_ANALYSIS.JNL_TIME       IS '저널발생시간';
COMMENT ON COLUMN T_FAULT_ANALYSIS.DEVICE_ID      IS '장치ID';
COMMENT ON COLUMN T_FAULT_ANALYSIS.ERR_GROUP_CD   IS '장애그룹코드';
COMMENT ON COLUMN T_FAULT_ANALYSIS.MODULE_TYPE_CD IS '모듈구분코드';
COMMENT ON COLUMN T_FAULT_ANALYSIS.FAULT_CLASS_CD IS '장애분류코드';
COMMENT ON COLUMN T_FAULT_ANALYSIS.ERR_CD         IS '장애코드';
COMMENT ON COLUMN T_FAULT_ANALYSIS.STATUS         IS '상태';
COMMENT ON COLUMN T_FAULT_ANALYSIS.REQUEST_FLAG   IS '요청여부';
COMMENT ON COLUMN T_FAULT_ANALYSIS.TIME_ELAPSED   IS '경과시간';

CREATE INDEX IX_T_FAULT_ANALYSIS_3 ON T_FAULT_ANALYSIS
    (JNL_DATE, TERM_GROUP_ID, TERM_ID, JNL_TIME, ERR_GROUP_CD, DEVICE_ID);
CREATE INDEX IX_T_FAULT_ANALYSIS_2 ON T_FAULT_ANALYSIS (REQUEST_FLAG, JNL_DATE);

-- ---------------------------------------------------------------------
-- 5) T_ATM_STOP_HISTORY (T_기기운영이력) - 21/22 운영중지 이력
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_STOP_HISTORY (
    TERM_GROUP_ID CHAR(5)       NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)  NOT NULL,   -- 단말번호
    JNL_DATE      CHAR(8)       NOT NULL,   -- 저널발생일
    JNL_TIME      CHAR(10)      NOT NULL,   -- 저널발생시간
    ERR_GROUP_CD  CHAR(2)       NOT NULL,   -- 장애그룹코드
    DEVICE_ID     CHAR(1),                  -- 장치ID
    FAULT_STATUS  CHAR(2),                  -- 장애상태
    FINISHED_DATE CHAR(8),                  -- 조치완료일
    FINISHED_TIME CHAR(6),                  -- 조치완료시간
    EXCEPT_FLAG   CHAR(1),                  -- 제외여부
    CONSTRAINT PK_T_ATM_STOP_HISTORY PRIMARY KEY
        (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, ERR_GROUP_CD)
);

COMMENT ON TABLE  T_ATM_STOP_HISTORY               IS 'T_기기운영이력';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.JNL_DATE      IS '저널발생일';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.JNL_TIME      IS '저널발생시간';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.ERR_GROUP_CD  IS '장애그룹코드';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.DEVICE_ID     IS '장치ID';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.FAULT_STATUS  IS '장애상태';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.FINISHED_DATE IS '조치완료일';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.FINISHED_TIME IS '조치완료시간';
COMMENT ON COLUMN T_ATM_STOP_HISTORY.EXCEPT_FLAG   IS '제외여부';

COMMIT;
