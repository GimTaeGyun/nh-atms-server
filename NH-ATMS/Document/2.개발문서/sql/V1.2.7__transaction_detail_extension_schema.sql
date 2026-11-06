-- =====================================================================
-- V1.2.7 : 거래상세 보강 + 부수 테이블 7종
--   T_GIRO_TRANSACTION     : 지로/공과금 거래상세  (1003)
--   T_FOREX_TRANSACTION    : 외화 거래상세        (1008)
--   T_PASSBOOK_TRANSACTION : 통장기장 거래상세     (1007)
--   T_FOREX_TOTAL          : 외화 합계            (2102/3208/3209)
--   T_FILE_ATTACH          : 통합 첨부파일         (7001/3100)
--   T_SLIP_FORMAT          : 명세표 양식 설정       (2301)
--   T_REMOTE_COMMAND       : 기기 원격지시 이력      (7205)
--
-- 근거: 09_메뉴별_테이블_매핑_및_추가설계.md (3.4)
--   AS-IS '자동화기기_거래' 60+컬럼을 유형별 상세로 정규화 분리(04_DB스키마설계 4.1).
--   공통키(TERM_GROUP_ID/TERM_ID/JNL_DATE/JNL_TIME)로 T_TRANSACTION_JOURNAL 과 1:1 연결.
-- 선행: V1.2.6. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_GIRO_TRANSACTION (T_지로거래상세) - 1003 공과금/납부
-- ---------------------------------------------------------------------
CREATE TABLE T_GIRO_TRANSACTION (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    JNL_DATE      CHAR(8)        NOT NULL,   -- 저널발생일
    JNL_TIME      CHAR(10)       NOT NULL,   -- 저널발생시간
    GIRO_SEQ      NUMBER(10)     NOT NULL,   -- 지로순번
    GIRO_TYPE     CHAR(1),                   -- 지로구분(장표/조회)
    INST_CD       VARCHAR2(10),              -- 이용기관번호
    ENVELOPE_CD   VARCHAR2(20),              -- 봉투코드
    SLIP_CNT      NUMBER(10),                -- 장표건수
    GIRO_AMOUNT   NUMBER(19),                -- 지로금액
    RECEIPT_RST   CHAR(2),                   -- 수납결과
    REGION_CD     CHAR(4),                   -- 지역코드
    CONSTRAINT PK_T_GIRO_TRANSACTION PRIMARY KEY
        (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, GIRO_SEQ)
);

COMMENT ON TABLE  T_GIRO_TRANSACTION               IS 'T_지로거래상세';
COMMENT ON COLUMN T_GIRO_TRANSACTION.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_GIRO_TRANSACTION.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_GIRO_TRANSACTION.JNL_DATE      IS '저널발생일';
COMMENT ON COLUMN T_GIRO_TRANSACTION.JNL_TIME      IS '저널발생시간';
COMMENT ON COLUMN T_GIRO_TRANSACTION.GIRO_SEQ      IS '지로순번';
COMMENT ON COLUMN T_GIRO_TRANSACTION.GIRO_TYPE     IS '지로구분';
COMMENT ON COLUMN T_GIRO_TRANSACTION.INST_CD       IS '이용기관번호';
COMMENT ON COLUMN T_GIRO_TRANSACTION.ENVELOPE_CD   IS '봉투코드';
COMMENT ON COLUMN T_GIRO_TRANSACTION.SLIP_CNT      IS '장표건수';
COMMENT ON COLUMN T_GIRO_TRANSACTION.GIRO_AMOUNT   IS '지로금액';
COMMENT ON COLUMN T_GIRO_TRANSACTION.RECEIPT_RST   IS '수납결과';
COMMENT ON COLUMN T_GIRO_TRANSACTION.REGION_CD     IS '지역코드';

-- ---------------------------------------------------------------------
-- 2) T_FOREX_TRANSACTION (T_외화거래상세) - 1008 외화 입/출금
-- ---------------------------------------------------------------------
CREATE TABLE T_FOREX_TRANSACTION (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    JNL_DATE      CHAR(8)        NOT NULL,   -- 저널발생일
    JNL_TIME      CHAR(10)       NOT NULL,   -- 저널발생시간
    CURRENCY_CD   CHAR(3)        NOT NULL,   -- 통화코드(USD/EUR/CNY/JPY)
    FOREX_AMOUNT  NUMBER(19,2),              -- 외화금액
    KRW_AMOUNT    NUMBER(19),                -- 원화환산금액
    EXCHANGE_RATE NUMBER(19,4),              -- 적용환율
    TR_GUBUN_CD   CHAR(2),                   -- 거래구분(지급/입금)
    CONSTRAINT PK_T_FOREX_TRANSACTION PRIMARY KEY
        (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, CURRENCY_CD)
);

COMMENT ON TABLE  T_FOREX_TRANSACTION               IS 'T_외화거래상세';
COMMENT ON COLUMN T_FOREX_TRANSACTION.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_FOREX_TRANSACTION.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_FOREX_TRANSACTION.JNL_DATE      IS '저널발생일';
COMMENT ON COLUMN T_FOREX_TRANSACTION.JNL_TIME      IS '저널발생시간';
COMMENT ON COLUMN T_FOREX_TRANSACTION.CURRENCY_CD   IS '통화코드';
COMMENT ON COLUMN T_FOREX_TRANSACTION.FOREX_AMOUNT  IS '외화금액';
COMMENT ON COLUMN T_FOREX_TRANSACTION.KRW_AMOUNT    IS '원화환산금액';
COMMENT ON COLUMN T_FOREX_TRANSACTION.EXCHANGE_RATE IS '적용환율';
COMMENT ON COLUMN T_FOREX_TRANSACTION.TR_GUBUN_CD   IS '거래구분';

-- ---------------------------------------------------------------------
-- 3) T_PASSBOOK_TRANSACTION (T_통장기장상세) - 1007 통장 기장
-- ---------------------------------------------------------------------
CREATE TABLE T_PASSBOOK_TRANSACTION (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    JNL_DATE      CHAR(8)        NOT NULL,   -- 저널발생일
    JNL_TIME      CHAR(10)       NOT NULL,   -- 저널발생시간
    ACCOUNT_NO    VARCHAR2(64),              -- 계좌번호
    PRINT_LINE    NUMBER(10),                -- 기장LINE
    PASSBOOK_PAGE NUMBER(10),                -- 통장페이지
    PASSBOOK_NO   VARCHAR2(20),              -- 통장발급번호
    TR_STATUS     CHAR(2),                   -- 거래상태
    CONSTRAINT PK_T_PASSBOOK_TRANSACTION PRIMARY KEY
        (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME)
);

COMMENT ON TABLE  T_PASSBOOK_TRANSACTION               IS 'T_통장기장상세';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.JNL_DATE      IS '저널발생일';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.JNL_TIME      IS '저널발생시간';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.ACCOUNT_NO    IS '계좌번호';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.PRINT_LINE    IS '기장LINE';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.PASSBOOK_PAGE IS '통장페이지';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.PASSBOOK_NO   IS '통장발급번호';
COMMENT ON COLUMN T_PASSBOOK_TRANSACTION.TR_STATUS     IS '거래상태';

-- ---------------------------------------------------------------------
-- 4) T_FOREX_TOTAL (T_외화합계) - 2102/3208/3209
--    통화별 센터분/기기분 합계전·후 집계.
-- ---------------------------------------------------------------------
CREATE TABLE T_FOREX_TOTAL (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 영업일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    CURRENCY_CD   CHAR(3)        NOT NULL,   -- 통화코드
    TOTAL_GUBUN   CHAR(1)        NOT NULL,   -- 집계구분(센터분/기기분)
    TR_GUBUN_CD   CHAR(2)        NOT NULL,   -- 거래구분(지급/입금)
    BEFORE_CNT    NUMBER(19)     DEFAULT 0,  -- 합계전건수
    BEFORE_AMT    NUMBER(19,2)   DEFAULT 0,  -- 합계전금액
    AFTER_CNT     NUMBER(19)     DEFAULT 0,  -- 합계후건수
    AFTER_AMT     NUMBER(19,2)   DEFAULT 0,  -- 합계후금액
    REG_DATE      CHAR(14),                  -- 집계생성일시
    CONSTRAINT PK_T_FOREX_TOTAL PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, CURRENCY_CD, TOTAL_GUBUN, TR_GUBUN_CD)
);

COMMENT ON TABLE  T_FOREX_TOTAL               IS 'T_외화합계';
COMMENT ON COLUMN T_FOREX_TOTAL.JNL_DATE      IS '영업일자';
COMMENT ON COLUMN T_FOREX_TOTAL.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_FOREX_TOTAL.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_FOREX_TOTAL.CURRENCY_CD   IS '통화코드';
COMMENT ON COLUMN T_FOREX_TOTAL.TOTAL_GUBUN   IS '집계구분';
COMMENT ON COLUMN T_FOREX_TOTAL.TR_GUBUN_CD   IS '거래구분';
COMMENT ON COLUMN T_FOREX_TOTAL.BEFORE_CNT    IS '합계전건수';
COMMENT ON COLUMN T_FOREX_TOTAL.BEFORE_AMT    IS '합계전금액';
COMMENT ON COLUMN T_FOREX_TOTAL.AFTER_CNT     IS '합계후건수';
COMMENT ON COLUMN T_FOREX_TOTAL.AFTER_AMT     IS '합계후금액';
COMMENT ON COLUMN T_FOREX_TOTAL.REG_DATE      IS '집계생성일시';

-- ---------------------------------------------------------------------
-- 5) T_FILE_ATTACH (T_첨부파일)
--    AS-IS BLOB → TO-BE 파일스토리지 경로 관리(공지/업무연락 등 공용).
-- ---------------------------------------------------------------------
CREATE TABLE T_FILE_ATTACH (
    FILE_ID       NUMBER(19)     NOT NULL,   -- 파일ID
    REF_TYPE      CHAR(2),                   -- 참조구분(공지/업무연락)
    REF_KEY       VARCHAR2(50),              -- 참조키
    FILE_NAME     VARCHAR2(255),             -- 원본파일명
    STORE_PATH    VARCHAR2(500),             -- 저장경로
    FILE_SIZE     NUMBER(19),                -- 파일크기
    CONTENT_TYPE  VARCHAR2(100),             -- 파일타입
    TEMP_YN       CHAR(1)        DEFAULT 'N',-- 임시여부
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_USER_ID   VARCHAR2(10),              -- 등록자ID
    REG_DATE      CHAR(14),                  -- 등록일시
    CONSTRAINT PK_T_FILE_ATTACH PRIMARY KEY (FILE_ID)
);

COMMENT ON TABLE  T_FILE_ATTACH              IS 'T_첨부파일';
COMMENT ON COLUMN T_FILE_ATTACH.FILE_ID      IS '파일ID';
COMMENT ON COLUMN T_FILE_ATTACH.REF_TYPE     IS '참조구분';
COMMENT ON COLUMN T_FILE_ATTACH.REF_KEY      IS '참조키';
COMMENT ON COLUMN T_FILE_ATTACH.FILE_NAME    IS '원본파일명';
COMMENT ON COLUMN T_FILE_ATTACH.STORE_PATH   IS '저장경로';
COMMENT ON COLUMN T_FILE_ATTACH.FILE_SIZE    IS '파일크기';
COMMENT ON COLUMN T_FILE_ATTACH.CONTENT_TYPE IS '파일타입';
COMMENT ON COLUMN T_FILE_ATTACH.TEMP_YN      IS '임시여부';
COMMENT ON COLUMN T_FILE_ATTACH.USE_YN       IS '사용여부';
COMMENT ON COLUMN T_FILE_ATTACH.REG_USER_ID  IS '등록자ID';
COMMENT ON COLUMN T_FILE_ATTACH.REG_DATE     IS '등록일시';

CREATE INDEX IX_T_FILE_ATTACH_REF ON T_FILE_ATTACH (REF_TYPE, REF_KEY);

-- ---------------------------------------------------------------------
-- 6) T_SLIP_FORMAT (T_명세표양식설정) - 2301 장애명세표
-- ---------------------------------------------------------------------
CREATE TABLE T_SLIP_FORMAT (
    FORMAT_ID     VARCHAR2(20)   NOT NULL,   -- 양식ID
    REGION_CD     CHAR(4),                   -- 지역코드
    SLIP_TYPE     CHAR(2),                   -- 명세표출력방식
    AD_TYPE       CHAR(2),                   -- 광고출력방식
    AD_CNT        NUMBER(10),                -- 광고건수
    FORMAT_DESC   VARCHAR2(200),             -- 양식설명
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    UPD_DATE      CHAR(8),                   -- 수정일
    CONSTRAINT PK_T_SLIP_FORMAT PRIMARY KEY (FORMAT_ID)
);

COMMENT ON TABLE  T_SLIP_FORMAT             IS 'T_명세표양식설정';
COMMENT ON COLUMN T_SLIP_FORMAT.FORMAT_ID   IS '양식ID';
COMMENT ON COLUMN T_SLIP_FORMAT.REGION_CD   IS '지역코드';
COMMENT ON COLUMN T_SLIP_FORMAT.SLIP_TYPE   IS '명세표출력방식';
COMMENT ON COLUMN T_SLIP_FORMAT.AD_TYPE     IS '광고출력방식';
COMMENT ON COLUMN T_SLIP_FORMAT.AD_CNT      IS '광고건수';
COMMENT ON COLUMN T_SLIP_FORMAT.FORMAT_DESC IS '양식설명';
COMMENT ON COLUMN T_SLIP_FORMAT.USE_YN      IS '사용여부';
COMMENT ON COLUMN T_SLIP_FORMAT.UPD_DATE    IS '수정일';

-- ---------------------------------------------------------------------
-- 7) T_REMOTE_COMMAND (T_기기원격지시이력) - 7205 매체배포/원격지시
-- ---------------------------------------------------------------------
CREATE TABLE T_REMOTE_COMMAND (
    CMD_DATE      CHAR(8)        NOT NULL,   -- 지시일자
    CMD_SEQ       NUMBER(19)     NOT NULL,   -- 지시일련번호
    TERM_GROUP_ID CHAR(5),                   -- 영업점코드
    TERM_ID       VARCHAR2(20),              -- 단말번호
    CMD_TYPE      CHAR(2),                   -- 지시유형(매체배포/재부팅/개국/마감)
    CMD_PARAM     VARCHAR2(500),             -- 지시파라미터
    CMD_STATUS    CHAR(2),                   -- 처리상태(요청/전송/완료/실패)
    REQUEST_TIME  CHAR(14),                  -- 요청일시
    RESULT_TIME   CHAR(14),                  -- 결과일시
    RESULT_MSG    VARCHAR2(500),             -- 결과메시지
    REQ_USER_ID   VARCHAR2(10),              -- 요청자ID
    CONSTRAINT PK_T_REMOTE_COMMAND PRIMARY KEY (CMD_DATE, CMD_SEQ)
);

COMMENT ON TABLE  T_REMOTE_COMMAND               IS 'T_기기원격지시이력';
COMMENT ON COLUMN T_REMOTE_COMMAND.CMD_DATE      IS '지시일자';
COMMENT ON COLUMN T_REMOTE_COMMAND.CMD_SEQ       IS '지시일련번호';
COMMENT ON COLUMN T_REMOTE_COMMAND.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_REMOTE_COMMAND.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_REMOTE_COMMAND.CMD_TYPE      IS '지시유형';
COMMENT ON COLUMN T_REMOTE_COMMAND.CMD_PARAM     IS '지시파라미터';
COMMENT ON COLUMN T_REMOTE_COMMAND.CMD_STATUS    IS '처리상태';
COMMENT ON COLUMN T_REMOTE_COMMAND.REQUEST_TIME  IS '요청일시';
COMMENT ON COLUMN T_REMOTE_COMMAND.RESULT_TIME   IS '결과일시';
COMMENT ON COLUMN T_REMOTE_COMMAND.RESULT_MSG    IS '결과메시지';
COMMENT ON COLUMN T_REMOTE_COMMAND.REQ_USER_ID   IS '요청자ID';

CREATE INDEX IX_T_REMOTECMD_ATM ON T_REMOTE_COMMAND (TERM_GROUP_ID, TERM_ID, CMD_DATE);

COMMIT;
