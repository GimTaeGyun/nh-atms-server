-- =====================================================================
-- V1.0.2 : 저널 도메인 스키마 (Oracle 19c)
-- 참조: Document/10_DB스키마_저널.md
-- 작성: 2026-05
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_JOURNAL : 저널 공통 헤더
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL (
    TERM_GROUP_ID       CHAR(5)       NOT NULL,
    TERM_ID             VARCHAR2(20)  NOT NULL,
    JNL_DATE            CHAR(8)       NOT NULL,
    JNL_TIME            CHAR(10)      NOT NULL,
    AGENT_TYPE          CHAR(1),
    SEQ_NO              CHAR(5),
    JNL_TYPE_CD         CHAR(2),
    JNL_WRITE_TYPE_CD   CHAR(1),
    RECV_DATE           CHAR(8),
    RECV_TIME           CHAR(10),
    CONSTRAINT PK_T_JOURNAL PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME)
);
CREATE INDEX IX_T_JOURNAL_DATE ON T_JOURNAL (JNL_DATE, JNL_TIME);
CREATE INDEX IX_T_JOURNAL_KIND ON T_JOURNAL (JNL_TYPE_CD, JNL_DATE);

COMMENT ON TABLE  T_JOURNAL                     IS '저널 공통 헤더 (모든 jnl_kind 공통)';
COMMENT ON COLUMN T_JOURNAL.TERM_GROUP_ID       IS '영업점코드';
COMMENT ON COLUMN T_JOURNAL.TERM_ID             IS '단말번호';
COMMENT ON COLUMN T_JOURNAL.JNL_DATE            IS '저널발생일 YYYYMMDD';
COMMENT ON COLUMN T_JOURNAL.JNL_TIME            IS '저널발생시간 HHMMSSFFF+flag';
COMMENT ON COLUMN T_JOURNAL.AGENT_TYPE          IS '에이전트구분';
COMMENT ON COLUMN T_JOURNAL.SEQ_NO              IS '저널일련번호';
COMMENT ON COLUMN T_JOURNAL.JNL_TYPE_CD         IS '저널종류 (11/12/21/22/31/41/52/71)';
COMMENT ON COLUMN T_JOURNAL.JNL_WRITE_TYPE_CD   IS '저널인자모드';
COMMENT ON COLUMN T_JOURNAL.RECV_DATE           IS 'ATMS 수신일';
COMMENT ON COLUMN T_JOURNAL.RECV_TIME           IS 'ATMS 수신시간';


-- ---------------------------------------------------------------------
-- 2) T_ATM_MONITOR : 기기 상태/장애 이력 (jnl_kind 21,22)  ★ MVP
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_MONITOR (
    TERM_GROUP_ID    CHAR(5)       NOT NULL,
    TERM_ID          VARCHAR2(20)  NOT NULL,
    JNL_DATE         CHAR(8)       NOT NULL,
    JNL_TIME         CHAR(10)      NOT NULL,
    AGENT_TYPE       CHAR(1)       NOT NULL,
    ATM_STATUS       CHAR(2),
    ERR_CD           VARCHAR2(10),
    ATM_ERR_CD       VARCHAR2(10),
    PROCESS_COUNT    CHAR(1),
    MAN_STATUS       CHAR(1),
    OMAN_STATUS      CHAR(1),
    SPR_STATUS       CHAR(1),
    JPR_STATUS       CHAR(1),
    CHK_STATUS       CHAR(1),
    CHA_STATUS       CHAR(1),
    MCR_STATUS       CHAR(1),
    PB_STATUS        CHAR(1),
    RJT_STATUS       CHAR(1),
    SEND_FLAG        CHAR(1),
    RECV_DATE        CHAR(8),
    RECV_TIME        CHAR(10),
    CONSTRAINT PK_T_ATM_MONITOR PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME)
);
CREATE INDEX IX_T_ATM_MONITOR_RECV ON T_ATM_MONITOR (RECV_DATE, RECV_TIME);
CREATE INDEX IX_T_ATM_MONITOR_ERR  ON T_ATM_MONITOR (ERR_CD, JNL_DATE);
CREATE INDEX IX_T_ATM_MONITOR_STAT ON T_ATM_MONITOR (ATM_STATUS, JNL_DATE);

COMMENT ON TABLE  T_ATM_MONITOR                IS '기기 상태/장애 이력 (저널 21,22 분기)';
COMMENT ON COLUMN T_ATM_MONITOR.TERM_GROUP_ID  IS '영업점코드';
COMMENT ON COLUMN T_ATM_MONITOR.TERM_ID        IS '단말번호';
COMMENT ON COLUMN T_ATM_MONITOR.JNL_DATE       IS '저널발생일';
COMMENT ON COLUMN T_ATM_MONITOR.JNL_TIME       IS '저널발생시간';
COMMENT ON COLUMN T_ATM_MONITOR.AGENT_TYPE     IS '에이전트구분';
COMMENT ON COLUMN T_ATM_MONITOR.ATM_STATUS     IS 'ATM 전체 상태 (코드표)';
COMMENT ON COLUMN T_ATM_MONITOR.ERR_CD         IS '은행 장애코드';
COMMENT ON COLUMN T_ATM_MONITOR.ATM_ERR_CD     IS '기기업체(벤더) 장애코드';
COMMENT ON COLUMN T_ATM_MONITOR.PROCESS_COUNT  IS '진행카운트';
COMMENT ON COLUMN T_ATM_MONITOR.MAN_STATUS     IS '만원권 상태';
COMMENT ON COLUMN T_ATM_MONITOR.OMAN_STATUS    IS '오만원권 상태';
COMMENT ON COLUMN T_ATM_MONITOR.SPR_STATUS     IS '명세표부 상태';
COMMENT ON COLUMN T_ATM_MONITOR.JPR_STATUS     IS '저널부 상태';
COMMENT ON COLUMN T_ATM_MONITOR.CHK_STATUS     IS '수표부 상태';
COMMENT ON COLUMN T_ATM_MONITOR.CHA_STATUS     IS '수표출금부2 상태';
COMMENT ON COLUMN T_ATM_MONITOR.MCR_STATUS     IS '카드부 상태';
COMMENT ON COLUMN T_ATM_MONITOR.PB_STATUS      IS '통장부 상태';
COMMENT ON COLUMN T_ATM_MONITOR.RJT_STATUS     IS '회수함 상태';
COMMENT ON COLUMN T_ATM_MONITOR.SEND_FLAG      IS '송신여부';
COMMENT ON COLUMN T_ATM_MONITOR.RECV_DATE      IS '수신일';
COMMENT ON COLUMN T_ATM_MONITOR.RECV_TIME      IS '수신시간';


-- ---------------------------------------------------------------------
-- 3) T_TRANSACTION_JOURNAL : 거래 저널 (jnl_kind 71)
-- ---------------------------------------------------------------------
CREATE TABLE T_TRANSACTION_JOURNAL (
    TERM_GROUP_ID      CHAR(5)       NOT NULL,
    TERM_ID            VARCHAR2(20)  NOT NULL,
    JNL_DATE           CHAR(8)       NOT NULL,
    JNL_TIME           CHAR(10)      NOT NULL,
    TR_NO              VARCHAR2(4),
    TR_TYPE            CHAR(6),
    TR_FROM_ACCOUNT    CHAR(2),
    TR_TO_ACCOUNT      CHAR(2),
    BANK_CD            CHAR(3),
    ACCOUNT_NO         VARCHAR2(64),
    TO_BANK_CD         CHAR(3),
    TO_ACCOUNT_NO      VARCHAR2(64),
    TR_HOST_DATE       CHAR(8),
    TR_HOST_TIME       CHAR(6),
    TR_HOST_NO         VARCHAR2(8),
    TR_HOST_AUDIT_NO   VARCHAR2(8),
    HOST_NET_ID        VARCHAR2(8),
    HOST_SETTLE_DATE   CHAR(8),
    DEPOSIT_NO         VARCHAR2(8),
    SURCHARGE          NUMBER(19),
    REQUEST_AMOUNT     NUMBER(19),
    DISPENSE_AMOUNT    NUMBER(19),
    REMAIN_AMOUNT      NUMBER(19),
    AVAIL_AMOUNT       NUMBER(19),
    PROC_COUNT         CHAR(1),
    TR_RESULT          CHAR(4),
    ERR_CD1            VARCHAR2(10),
    ERR_CD2            VARCHAR2(10),
    ERR_CD3            VARCHAR2(10),
    CARD_DATA          VARCHAR2(152),
    NON_CASH_VALUE     VARCHAR2(16),
    NON_CASH_TYPE      CHAR(2),
    TR_MEDIA_CD        CHAR(1),
    MESSAGE            VARCHAR2(255),
    HOST_CASH_AMOUNT   NUMBER(19),
    HOST_CHECK_AMOUNT  NUMBER(19),
    TOTAL_TYPE_CD      CHAR(1),
    TRANSFER_NAME      VARCHAR2(60),
    CONSTRAINT PK_T_TRANSACTION_JOURNAL PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME)
);
CREATE INDEX IX_T_TRX_DATE ON T_TRANSACTION_JOURNAL (JNL_DATE, JNL_TIME);
CREATE INDEX IX_T_TRX_ACC  ON T_TRANSACTION_JOURNAL (ACCOUNT_NO);
CREATE INDEX IX_T_TRX_CARD ON T_TRANSACTION_JOURNAL (CARD_DATA);

COMMENT ON TABLE T_TRANSACTION_JOURNAL IS '거래 저널 (jnl_kind=71)';


-- ---------------------------------------------------------------------
-- 6) T_JOURNAL_ERROR : 파싱 실패 raw 보관
-- ---------------------------------------------------------------------
CREATE SEQUENCE SEQ_T_JOURNAL_ERROR START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE T_JOURNAL_ERROR (
    SEQ         NUMBER(19)  NOT NULL,
    JNL_DATE    CHAR(8),
    JNL_TIME    CHAR(10),
    ATM_ID      CHAR(20),
    JNL_SEQ     CHAR(5),
    JNL_KIND    CHAR(2),
    JNL_JSON    CLOB,
    ERR_MSG     CLOB,
    REG_DT      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_T_JOURNAL_ERROR PRIMARY KEY (SEQ)
);
CREATE INDEX IX_T_JOURNAL_ERROR_DATE ON T_JOURNAL_ERROR (JNL_DATE, JNL_TIME);
COMMENT ON TABLE T_JOURNAL_ERROR IS '저널 처리 에러 (파싱 실패 raw 보관)';


-- ---------------------------------------------------------------------
-- 7) T_JOURNAL_FOOTNOTE : 특이저널 메모
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL_FOOTNOTE (
    TERM_GROUP_ID  CHAR(5)       NOT NULL,
    TERM_ID        VARCHAR2(20)  NOT NULL,
    JNL_DATE       CHAR(8)       NOT NULL,
    JNL_TIME       CHAR(10)      NOT NULL,
    DELETE_FLAG    CHAR(1)       DEFAULT '0',
    NOTE_HTML      VARCHAR2(4000),
    NOTE           VARCHAR2(2000),
    CONSTRAINT PK_T_JOURNAL_FOOTNOTE PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME)
);
COMMENT ON TABLE T_JOURNAL_FOOTNOTE IS '특이저널 메모';


-- ---------------------------------------------------------------------
-- 9) T_JNLIMAGE_HISTORY : 이미지 조회 감사
-- ---------------------------------------------------------------------
CREATE TABLE T_JNLIMAGE_HISTORY (
    TERM_GROUP_ID        CHAR(5)       NOT NULL,
    TERM_ID              VARCHAR2(20)  NOT NULL,
    JNL_DATE             CHAR(8)       NOT NULL,
    JNL_TIME             CHAR(10)      NOT NULL,
    VIEW_DATE            CHAR(8)       NOT NULL,
    VIEW_TIME            CHAR(6)       NOT NULL,
    VIEW_USER_ID         VARCHAR2(12),
    VIEW_USER_POSITION   VARCHAR2(60),
    VIEW_USER_NAME       VARCHAR2(100),
    VIEW_USER_DEPT       CHAR(5),
    VIEW_USER_DEPT_NAME  VARCHAR2(200),
    CONSTRAINT PK_T_JNLIMAGE_HISTORY PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, VIEW_DATE, VIEW_TIME)
);
COMMENT ON TABLE T_JNLIMAGE_HISTORY IS '이미지 조회 감사 로그';


-- ---------------------------------------------------------------------
-- 10) T_DAY_TOTAL : 마감 저널 (jnl_kind 31)
-- ---------------------------------------------------------------------
CREATE TABLE T_DAY_TOTAL (
    TERM_GROUP_ID  CHAR(5)       NOT NULL,
    TERM_ID        VARCHAR2(20)  NOT NULL,
    JNL_DATE       CHAR(8)       NOT NULL,
    JNL_TIME       CHAR(10)      NOT NULL,
    TOTAL_TYPE     CHAR(1),
    BEGIN_DATE     CHAR(8),
    BEGIN_TIME     CHAR(6),
    END_DATE       CHAR(8),
    END_TIME       CHAR(6),
    TOTAL_DATA     CLOB,
    CONSTRAINT PK_T_DAY_TOTAL PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME)
);
COMMENT ON TABLE T_DAY_TOTAL IS '마감 저널 (jnl_kind=31)';


-- ---------------------------------------------------------------------
-- 11) T_ADD_CASH : 현금 시재 (jnl_kind 41)
-- ---------------------------------------------------------------------
CREATE TABLE T_ADD_CASH (
    TERM_GROUP_ID  CHAR(5)       NOT NULL,
    TERM_ID        VARCHAR2(20)  NOT NULL,
    JNL_DATE       CHAR(8)       NOT NULL,
    JNL_TIME       CHAR(10)      NOT NULL,
    ACTION_TYPE    CHAR(1)       NOT NULL,
    CASSETTE_TYPE  CHAR(1)       NOT NULL,
    CASSETTE_NO    CHAR(2)       NOT NULL,
    CURRENCY       CHAR(3),
    DENOMINATION   NUMBER(10),
    REMAIN_QTY     NUMBER(5),
    ADD_QTY        NUMBER(5),
    CONSTRAINT PK_T_ADD_CASH PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, ACTION_TYPE, CASSETTE_TYPE, CASSETTE_NO)
);
COMMENT ON TABLE T_ADD_CASH IS '현금 시재 보충/회수 (jnl_kind=41)';


-- ---------------------------------------------------------------------
-- 12) T_ADD_CHECK : 수표 시재 (jnl_kind 41)
-- ---------------------------------------------------------------------
CREATE TABLE T_ADD_CHECK (
    TERM_GROUP_ID  CHAR(5)       NOT NULL,
    TERM_ID        VARCHAR2(20)  NOT NULL,
    JNL_DATE       CHAR(8)       NOT NULL,
    JNL_TIME       CHAR(10)      NOT NULL,
    ACTION_TYPE    CHAR(1)       NOT NULL,
    CURRENCY       CHAR(3),
    DENOMINATION   NUMBER(10),
    JA_AMOUNT      NUMBER(19),
    JA_COUNT       NUMBER(5),
    TA_AMOUNT      NUMBER(19),
    TA_COUNT       NUMBER(5),
    ISSUE_BANK     CHAR(3),
    ISSUE_BRANCH   CHAR(5),
    REG_NO         VARCHAR2(32),
    CONSTRAINT PK_T_ADD_CHECK PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, ACTION_TYPE)
);
COMMENT ON TABLE T_ADD_CHECK IS '수표 시재 보충/회수 (jnl_kind=41)';

COMMIT;
