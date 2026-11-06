-- =====================================================================
-- V1.2.4 : 통계/집계 보강 테이블 4종
--   T_TRANSACTION_HOURLY_TOTAL  : 시간별 거래통계      (4202 운영모드 운영률)
--   T_TRANSACTION_MONTHLY_TOTAL : 월별 거래통계        (4200 저널처리 월별 건수)
--   T_JOURNAL_PROCESS_TOTAL     : 저널처리유형별 집계  (4104/4105)
--   T_OPEN_CLOSE_STATUS         : 개국/마감 현황       (4001/6003)
--
-- 근거: 09_메뉴별_테이블_매핑_및_추가설계.md (3.1)
--   AS-IS '자동화기기_시간별거래통계 / 월별거래통계 / 집계' 정규화 분리.
-- 선행: V1.2.3. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_TRANSACTION_HOURLY_TOTAL (T_시간별거래통계)
-- ---------------------------------------------------------------------
CREATE TABLE T_TRANSACTION_HOURLY_TOTAL (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 거래일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    HOUR_CD       CHAR(2)        NOT NULL,   -- 시간구분(00~23)
    FEE_FLAG      CHAR(1)        NOT NULL,   -- 수수료구분(Y/N)
    TR_COUNT      NUMBER(19)     DEFAULT 0,  -- 거래건수
    TR_AMOUNT     NUMBER(19)     DEFAULT 0,  -- 거래금액
    FEE_AMOUNT    NUMBER(19)     DEFAULT 0,  -- 수수료금액
    REG_DATE      CHAR(14),                  -- 집계생성일시
    CONSTRAINT PK_T_TRANSACTION_HOURLY_TOTAL PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, HOUR_CD, FEE_FLAG)
);

COMMENT ON TABLE  T_TRANSACTION_HOURLY_TOTAL               IS 'T_시간별거래통계';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.JNL_DATE      IS '거래일자';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.HOUR_CD       IS '시간구분';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.FEE_FLAG      IS '수수료구분';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.TR_COUNT      IS '거래건수';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.TR_AMOUNT     IS '거래금액';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.FEE_AMOUNT    IS '수수료금액';
COMMENT ON COLUMN T_TRANSACTION_HOURLY_TOTAL.REG_DATE      IS '집계생성일시';

CREATE INDEX IX_T_TRXHOURLY_GROUP ON T_TRANSACTION_HOURLY_TOTAL (TERM_GROUP_ID, JNL_DATE, HOUR_CD);

-- ---------------------------------------------------------------------
-- 2) T_TRANSACTION_MONTHLY_TOTAL (T_월별거래통계)
-- ---------------------------------------------------------------------
CREATE TABLE T_TRANSACTION_MONTHLY_TOTAL (
    JNL_MONTH     CHAR(6)        NOT NULL,   -- 거래년월(YYYYMM)
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    TR_GUBUN_CD   CHAR(2)        NOT NULL,   -- 거래구분코드
    SELF_FLAG     CHAR(1)        NOT NULL,   -- 자타구분(자행/타행)
    TR_COUNT      NUMBER(19)     DEFAULT 0,  -- 거래건수
    TR_AMOUNT     NUMBER(19)     DEFAULT 0,  -- 거래금액
    FEE_COUNT     NUMBER(19)     DEFAULT 0,  -- 수수료건수
    FEE_AMOUNT    NUMBER(19)     DEFAULT 0,  -- 수수료금액
    REG_DATE      CHAR(14),                  -- 집계생성일시
    CONSTRAINT PK_T_TRANSACTION_MONTHLY_TOTAL PRIMARY KEY
        (JNL_MONTH, TERM_GROUP_ID, TERM_ID, TR_GUBUN_CD, SELF_FLAG)
);

COMMENT ON TABLE  T_TRANSACTION_MONTHLY_TOTAL               IS 'T_월별거래통계';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.JNL_MONTH     IS '거래년월';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.TR_GUBUN_CD   IS '거래구분코드';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.SELF_FLAG     IS '자타구분';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.TR_COUNT      IS '거래건수';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.TR_AMOUNT     IS '거래금액';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.FEE_COUNT     IS '수수료건수';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.FEE_AMOUNT    IS '수수료금액';
COMMENT ON COLUMN T_TRANSACTION_MONTHLY_TOTAL.REG_DATE      IS '집계생성일시';

CREATE INDEX IX_T_TRXMONTHLY_GROUP ON T_TRANSACTION_MONTHLY_TOTAL (TERM_GROUP_ID, JNL_MONTH);

-- ---------------------------------------------------------------------
-- 3) T_JOURNAL_PROCESS_TOTAL (T_저널처리유형집계)
--    저널유형(11/12/21/22/31/41/52/71)별 처리 건수 일집계.
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL_PROCESS_TOTAL (
    JNL_DATE       CHAR(8)       NOT NULL,   -- 집계일자
    TERM_GROUP_ID  CHAR(5)       NOT NULL,   -- 영업점코드
    TERM_ID        VARCHAR2(20)  NOT NULL,   -- 단말번호
    JNL_TYPE_CD    CHAR(2)       NOT NULL,   -- 저널유형코드
    PROCESS_CNT    NUMBER(19)    DEFAULT 0,  -- 처리건수
    SUCCESS_CNT    NUMBER(19)    DEFAULT 0,  -- 정상건수
    ERROR_CNT      NUMBER(19)    DEFAULT 0,  -- 오류건수
    REG_DATE       CHAR(14),                 -- 집계생성일시
    CONSTRAINT PK_T_JOURNAL_PROCESS_TOTAL PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, JNL_TYPE_CD)
);

COMMENT ON TABLE  T_JOURNAL_PROCESS_TOTAL               IS 'T_저널처리유형집계';
COMMENT ON COLUMN T_JOURNAL_PROCESS_TOTAL.JNL_DATE      IS '집계일자';
COMMENT ON COLUMN T_JOURNAL_PROCESS_TOTAL.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_JOURNAL_PROCESS_TOTAL.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_JOURNAL_PROCESS_TOTAL.JNL_TYPE_CD   IS '저널유형코드';
COMMENT ON COLUMN T_JOURNAL_PROCESS_TOTAL.PROCESS_CNT   IS '처리건수';
COMMENT ON COLUMN T_JOURNAL_PROCESS_TOTAL.SUCCESS_CNT   IS '정상건수';
COMMENT ON COLUMN T_JOURNAL_PROCESS_TOTAL.ERROR_CNT     IS '오류건수';
COMMENT ON COLUMN T_JOURNAL_PROCESS_TOTAL.REG_DATE      IS '집계생성일시';

CREATE INDEX IX_T_JNLPROCTOTAL_TYPE ON T_JOURNAL_PROCESS_TOTAL (JNL_TYPE_CD, JNL_DATE);

-- ---------------------------------------------------------------------
-- 4) T_OPEN_CLOSE_STATUS (T_개국마감현황)
--    기기별 개국(Open 12)/마감(Total 31) 진행 상태 일자별 스냅샷.
-- ---------------------------------------------------------------------
CREATE TABLE T_OPEN_CLOSE_STATUS (
    JNL_DATE        CHAR(8)       NOT NULL,   -- 영업일자
    TERM_GROUP_ID   CHAR(5)       NOT NULL,   -- 영업점코드
    TERM_ID         VARCHAR2(20)  NOT NULL,   -- 단말번호
    OPEN_FLAG       CHAR(1),                  -- 개국여부(Y/N)
    OPEN_TIME       CHAR(6),                  -- 개국시간
    CLOSE_FLAG      CHAR(1),                  -- 마감여부(Y/N)
    CLOSE_TIME      CHAR(6),                  -- 마감시간
    TOTAL_BEFORE    CHAR(1),                  -- 합계전/후구분
    REMOTE_FLAG     CHAR(1),                  -- 원격처리여부
    LAST_STATUS     CHAR(2),                  -- 최종상태
    REG_DATE        CHAR(14),                 -- 갱신일시
    CONSTRAINT PK_T_OPEN_CLOSE_STATUS PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID)
);

COMMENT ON TABLE  T_OPEN_CLOSE_STATUS               IS 'T_개국마감현황';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.JNL_DATE      IS '영업일자';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.OPEN_FLAG     IS '개국여부';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.OPEN_TIME     IS '개국시간';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.CLOSE_FLAG    IS '마감여부';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.CLOSE_TIME    IS '마감시간';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.TOTAL_BEFORE  IS '합계전후구분';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.REMOTE_FLAG   IS '원격처리여부';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.LAST_STATUS   IS '최종상태';
COMMENT ON COLUMN T_OPEN_CLOSE_STATUS.REG_DATE      IS '갱신일시';

CREATE INDEX IX_T_OPENCLOSE_GROUP ON T_OPEN_CLOSE_STATUS (TERM_GROUP_ID, JNL_DATE);

COMMIT;
