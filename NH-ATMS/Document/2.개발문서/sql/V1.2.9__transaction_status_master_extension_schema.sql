-- =====================================================================
-- V1.2.9 : 거래/상태/마스터 보강 11종 (AS-IS 미커버 + TO-BE 메뉴 직접연관)
--   T_FUND_RETURN       : 자금반환            (12000/11000)
--   T_FUND_RETURN_MEMO   : 자금반환 메모         (12000)
--   T_ECASH_TRANSACTION : 전자화폐 거래상세      (9113)
--   T_NONBANK_DEPOSIT   : 무통장 실명입금현황     (1006)
--   T_TRAN_STATUS_MSG    : 거래상태 메시지 코드     (3100/3101)
--   T_NETWORK_STATUS    : 망상태 상세           (3100)
--   T_DAEMON_STATUS     : 데몬 가동정보         (7227)
--   T_UNFINISHED_COUNT  : 업무별 미완료 건수      (6102/7227)
--   T_CARD_BIN          : 카드 BIN 마스터       (1004/9122)
--   T_DEVICE_HEAT_TIME   : 기기 온열시간 설정      (9302)
--   T_GIRO_MONTHLY_TOTAL : 지로 월별 거래통계      (통계)
--
-- 근거: 10_AS-IS전수분석_및_TO-BE테이블설계.md (4)
-- 선행: V1.2.8. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_FUND_RETURN (T_자금반환) - 12000/11000
-- ---------------------------------------------------------------------
CREATE TABLE T_FUND_RETURN (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 거래일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    RETURN_SEQ    NUMBER(10)     NOT NULL,   -- 반환일련번호
    BRANCH_TYPE   CHAR(1),                   -- 종조구분
    REFUND_BANK   CHAR(3),                   -- 환불은행코드
    ACCOUNT_NO    VARCHAR2(20),              -- 계좌번호
    TR_AMOUNT     NUMBER(19),                -- 거래금액
    RETURN_STATUS CHAR(2),                   -- 반환처리상태
    PROC_DATE     CHAR(8),                   -- 처리일자
    CONSTRAINT PK_T_FUND_RETURN PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, RETURN_SEQ)
);

COMMENT ON TABLE  T_FUND_RETURN               IS 'T_자금반환';
COMMENT ON COLUMN T_FUND_RETURN.JNL_DATE      IS '거래일자';
COMMENT ON COLUMN T_FUND_RETURN.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_FUND_RETURN.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_FUND_RETURN.RETURN_SEQ    IS '반환일련번호';
COMMENT ON COLUMN T_FUND_RETURN.BRANCH_TYPE   IS '종조구분';
COMMENT ON COLUMN T_FUND_RETURN.REFUND_BANK   IS '환불은행코드';
COMMENT ON COLUMN T_FUND_RETURN.ACCOUNT_NO    IS '계좌번호';
COMMENT ON COLUMN T_FUND_RETURN.TR_AMOUNT     IS '거래금액';
COMMENT ON COLUMN T_FUND_RETURN.RETURN_STATUS IS '반환처리상태';
COMMENT ON COLUMN T_FUND_RETURN.PROC_DATE     IS '처리일자';

-- ---------------------------------------------------------------------
-- 2) T_FUND_RETURN_MEMO (T_자금반환메모) - 12000
-- ---------------------------------------------------------------------
CREATE TABLE T_FUND_RETURN_MEMO (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 거래일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    MEMO_SEQ      NUMBER(10)     NOT NULL,   -- 메모일련번호
    MEMO_CD       VARCHAR2(10),              -- 메모코드
    MEMO_CONTENT  VARCHAR2(200),             -- 메모내용
    REG_USER_ID   VARCHAR2(10),              -- 등록자ID
    REG_DATE      CHAR(14),                  -- 등록일시
    CONSTRAINT PK_T_FUND_RETURN_MEMO PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, MEMO_SEQ)
);

COMMENT ON TABLE  T_FUND_RETURN_MEMO               IS 'T_자금반환메모';
COMMENT ON COLUMN T_FUND_RETURN_MEMO.JNL_DATE      IS '거래일자';
COMMENT ON COLUMN T_FUND_RETURN_MEMO.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_FUND_RETURN_MEMO.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_FUND_RETURN_MEMO.MEMO_SEQ      IS '메모일련번호';
COMMENT ON COLUMN T_FUND_RETURN_MEMO.MEMO_CD       IS '메모코드';
COMMENT ON COLUMN T_FUND_RETURN_MEMO.MEMO_CONTENT  IS '메모내용';
COMMENT ON COLUMN T_FUND_RETURN_MEMO.REG_USER_ID   IS '등록자ID';
COMMENT ON COLUMN T_FUND_RETURN_MEMO.REG_DATE      IS '등록일시';

-- ---------------------------------------------------------------------
-- 3) T_ECASH_TRANSACTION (T_전자화폐거래상세) - 9113
-- ---------------------------------------------------------------------
CREATE TABLE T_ECASH_TRANSACTION (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    JNL_DATE      CHAR(8)        NOT NULL,   -- 저널발생일
    JNL_TIME      CHAR(10)       NOT NULL,   -- 저널발생시간
    SLIP_SEQ      NUMBER(10),                -- 명세표일련번호
    ECASH_TYPE    CHAR(2),                   -- 전자화폐구분
    CARD_NO       VARCHAR2(20),              -- 전자화폐카드번호
    CHARGE_AMOUNT NUMBER(19),                -- 충전금액
    BALANCE       NUMBER(19),                -- 잔액
    RESULT_CD     CHAR(2),                   -- 처리결과코드
    CONSTRAINT PK_T_ECASH_TRANSACTION PRIMARY KEY
        (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME)
);

COMMENT ON TABLE  T_ECASH_TRANSACTION               IS 'T_전자화폐거래상세';
COMMENT ON COLUMN T_ECASH_TRANSACTION.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_ECASH_TRANSACTION.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_ECASH_TRANSACTION.JNL_DATE      IS '저널발생일';
COMMENT ON COLUMN T_ECASH_TRANSACTION.JNL_TIME      IS '저널발생시간';
COMMENT ON COLUMN T_ECASH_TRANSACTION.SLIP_SEQ      IS '명세표일련번호';
COMMENT ON COLUMN T_ECASH_TRANSACTION.ECASH_TYPE    IS '전자화폐구분';
COMMENT ON COLUMN T_ECASH_TRANSACTION.CARD_NO       IS '전자화폐카드번호';
COMMENT ON COLUMN T_ECASH_TRANSACTION.CHARGE_AMOUNT IS '충전금액';
COMMENT ON COLUMN T_ECASH_TRANSACTION.BALANCE       IS '잔액';
COMMENT ON COLUMN T_ECASH_TRANSACTION.RESULT_CD     IS '처리결과코드';

-- ---------------------------------------------------------------------
-- 4) T_NONBANK_DEPOSIT (T_무통장실명입금현황) - 1006
-- ---------------------------------------------------------------------
CREATE TABLE T_NONBANK_DEPOSIT (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 거래일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    TR_SEQ        NUMBER(10)     NOT NULL,   -- 거래일련번호
    SCREEN_NO     VARCHAR2(4),               -- 화면번호
    CUSTOMER_NO   VARCHAR2(20),              -- 입금지정고객번호
    REALNAME_NO   VARCHAR2(20),              -- 실명확인번호
    DEPOSIT_AMT   NUMBER(19),                -- 입금관리금액
    TOTAL_AMT     NUMBER(19),                -- 총거래금액
    CONSTRAINT PK_T_NONBANK_DEPOSIT PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, TR_SEQ)
);

COMMENT ON TABLE  T_NONBANK_DEPOSIT               IS 'T_무통장실명입금현황';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.JNL_DATE      IS '거래일자';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.TR_SEQ        IS '거래일련번호';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.SCREEN_NO     IS '화면번호';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.CUSTOMER_NO   IS '입금지정고객번호';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.REALNAME_NO   IS '실명확인번호';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.DEPOSIT_AMT   IS '입금관리금액';
COMMENT ON COLUMN T_NONBANK_DEPOSIT.TOTAL_AMT     IS '총거래금액';

-- ---------------------------------------------------------------------
-- 5) T_TRAN_STATUS_MSG (T_거래상태메시지) - 3100/3101 장애코드 마스터
-- ---------------------------------------------------------------------
CREATE TABLE T_TRAN_STATUS_MSG (
    STATUS_CD     VARCHAR2(4)    NOT NULL,   -- ATM거래상태코드
    MSG_TYPE      VARCHAR2(4)    NOT NULL,   -- 메시지유형
    PROC_STATUS   CHAR(2),                   -- 처리상태
    MSG_CONTENT   VARCHAR2(200),             -- 메시지내용
    REMARK        VARCHAR2(200),             -- 비고
    REG_DATE      CHAR(14),                  -- 생성일
    UPD_DATE      CHAR(14),                  -- 수정일
    CONSTRAINT PK_T_TRAN_STATUS_MSG PRIMARY KEY (STATUS_CD, MSG_TYPE)
);

COMMENT ON TABLE  T_TRAN_STATUS_MSG             IS 'T_거래상태메시지';
COMMENT ON COLUMN T_TRAN_STATUS_MSG.STATUS_CD   IS 'ATM거래상태코드';
COMMENT ON COLUMN T_TRAN_STATUS_MSG.MSG_TYPE    IS '메시지유형';
COMMENT ON COLUMN T_TRAN_STATUS_MSG.PROC_STATUS IS '처리상태';
COMMENT ON COLUMN T_TRAN_STATUS_MSG.MSG_CONTENT IS '메시지내용';
COMMENT ON COLUMN T_TRAN_STATUS_MSG.REMARK      IS '비고';
COMMENT ON COLUMN T_TRAN_STATUS_MSG.REG_DATE    IS '생성일';
COMMENT ON COLUMN T_TRAN_STATUS_MSG.UPD_DATE    IS '수정일';

-- ---------------------------------------------------------------------
-- 6) T_NETWORK_STATUS (T_망상태상세) - 3100
-- ---------------------------------------------------------------------
CREATE TABLE T_NETWORK_STATUS (
    CHANNEL_ID    VARCHAR2(4)    NOT NULL,   -- 채널ID
    ORG_CD        VARCHAR2(32)   NOT NULL,   -- 대외기관코드
    LINK_STATUS   CHAR(1),                   -- 연결상태
    LAST_CHK_TIME CHAR(14),                  -- 최종점검일시
    ERROR_CNT     NUMBER(10),                -- 오류건수
    CONSTRAINT PK_T_NETWORK_STATUS PRIMARY KEY (CHANNEL_ID, ORG_CD)
);

COMMENT ON TABLE  T_NETWORK_STATUS               IS 'T_망상태상세';
COMMENT ON COLUMN T_NETWORK_STATUS.CHANNEL_ID    IS '채널ID';
COMMENT ON COLUMN T_NETWORK_STATUS.ORG_CD        IS '대외기관코드';
COMMENT ON COLUMN T_NETWORK_STATUS.LINK_STATUS   IS '연결상태';
COMMENT ON COLUMN T_NETWORK_STATUS.LAST_CHK_TIME IS '최종점검일시';
COMMENT ON COLUMN T_NETWORK_STATUS.ERROR_CNT     IS '오류건수';

-- ---------------------------------------------------------------------
-- 7) T_DAEMON_STATUS (T_데몬가동정보) - 7227
-- ---------------------------------------------------------------------
CREATE TABLE T_DAEMON_STATUS (
    PROG_ID       VARCHAR2(16)   NOT NULL,   -- 프로그램ID
    RUN_SERVER_ID VARCHAR2(21)   NOT NULL,   -- 실행서버ID
    RUN_DATE      CHAR(8),                   -- 거래일자
    RUN_TIME      CHAR(6),                   -- 실행시간
    ALIVE_FLAG    CHAR(1),                   -- 가동여부
    CONSTRAINT PK_T_DAEMON_STATUS PRIMARY KEY (PROG_ID, RUN_SERVER_ID)
);

COMMENT ON TABLE  T_DAEMON_STATUS               IS 'T_데몬가동정보';
COMMENT ON COLUMN T_DAEMON_STATUS.PROG_ID       IS '프로그램ID';
COMMENT ON COLUMN T_DAEMON_STATUS.RUN_SERVER_ID IS '실행서버ID';
COMMENT ON COLUMN T_DAEMON_STATUS.RUN_DATE      IS '거래일자';
COMMENT ON COLUMN T_DAEMON_STATUS.RUN_TIME      IS '실행시간';
COMMENT ON COLUMN T_DAEMON_STATUS.ALIVE_FLAG    IS '가동여부';

-- ---------------------------------------------------------------------
-- 8) T_UNFINISHED_COUNT (T_업무별미완료건수) - 6102/7227
-- ---------------------------------------------------------------------
CREATE TABLE T_UNFINISHED_COUNT (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 거래일자
    EQUIP_KIND    VARCHAR2(10)   NOT NULL,   -- 기기종류
    PROC_STATUS   CHAR(2)        NOT NULL,   -- 처리상태
    PROC_TIME     CHAR(6),                   -- 처리시각
    UNFIN_CNT     NUMBER(10),                -- 미완료건수
    CONSTRAINT PK_T_UNFINISHED_COUNT PRIMARY KEY
        (JNL_DATE, EQUIP_KIND, PROC_STATUS)
);

COMMENT ON TABLE  T_UNFINISHED_COUNT             IS 'T_업무별미완료건수';
COMMENT ON COLUMN T_UNFINISHED_COUNT.JNL_DATE    IS '거래일자';
COMMENT ON COLUMN T_UNFINISHED_COUNT.EQUIP_KIND  IS '기기종류';
COMMENT ON COLUMN T_UNFINISHED_COUNT.PROC_STATUS IS '처리상태';
COMMENT ON COLUMN T_UNFINISHED_COUNT.PROC_TIME   IS '처리시각';
COMMENT ON COLUMN T_UNFINISHED_COUNT.UNFIN_CNT   IS '미완료건수';

-- ---------------------------------------------------------------------
-- 9) T_CARD_BIN (T_카드BIN마스터) - 1004/9122
-- ---------------------------------------------------------------------
CREATE TABLE T_CARD_BIN (
    BIN_CD        VARCHAR2(8)    NOT NULL,   -- BIN코드
    PARTNER_NAME  VARCHAR2(30),              -- 제휴상품명
    ACCT_TYPE_CD  VARCHAR2(4),               -- 계좌종류코드
    CARD_TYPE_CD  VARCHAR2(4),               -- 카드종류코드
    SETTLE_BANK   CHAR(3),                   -- 결제지점
    DELETE_YN     CHAR(1)        DEFAULT 'N',-- 삭제여부
    REG_DATE      CHAR(8),                   -- 생성일자
    UPD_DATE      CHAR(8),                   -- 수정일자
    CONSTRAINT PK_T_CARD_BIN PRIMARY KEY (BIN_CD)
);

COMMENT ON TABLE  T_CARD_BIN              IS 'T_카드BIN마스터';
COMMENT ON COLUMN T_CARD_BIN.BIN_CD       IS 'BIN코드';
COMMENT ON COLUMN T_CARD_BIN.PARTNER_NAME IS '제휴상품명';
COMMENT ON COLUMN T_CARD_BIN.ACCT_TYPE_CD IS '계좌종류코드';
COMMENT ON COLUMN T_CARD_BIN.CARD_TYPE_CD IS '카드종류코드';
COMMENT ON COLUMN T_CARD_BIN.SETTLE_BANK  IS '결제지점';
COMMENT ON COLUMN T_CARD_BIN.DELETE_YN    IS '삭제여부';
COMMENT ON COLUMN T_CARD_BIN.REG_DATE     IS '생성일자';
COMMENT ON COLUMN T_CARD_BIN.UPD_DATE     IS '수정일자';

-- ---------------------------------------------------------------------
-- 10) T_DEVICE_HEAT_TIME (T_기기온열시간) - 9302
-- ---------------------------------------------------------------------
CREATE TABLE T_DEVICE_HEAT_TIME (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    START_TIME1   CHAR(4),                   -- 시작시간1
    END_TIME1     CHAR(4),                   -- 종료시간1
    START_TIME2   CHAR(4),                   -- 시작시간2
    END_TIME2     CHAR(4),                   -- 종료시간2
    START_TIME3   CHAR(4),                   -- 시작시간3
    END_TIME3     CHAR(4),                   -- 종료시간3
    START_TIME4   CHAR(4),                   -- 시작시간4
    END_TIME4     CHAR(4),                   -- 종료시간4
    CONSTRAINT PK_T_DEVICE_HEAT_TIME PRIMARY KEY (TERM_GROUP_ID, TERM_ID)
);

COMMENT ON TABLE  T_DEVICE_HEAT_TIME               IS 'T_기기온열시간';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.START_TIME1   IS '시작시간1';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.END_TIME1     IS '종료시간1';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.START_TIME2   IS '시작시간2';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.END_TIME2     IS '종료시간2';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.START_TIME3   IS '시작시간3';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.END_TIME3     IS '종료시간3';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.START_TIME4   IS '시작시간4';
COMMENT ON COLUMN T_DEVICE_HEAT_TIME.END_TIME4     IS '종료시간4';

-- ---------------------------------------------------------------------
-- 11) T_GIRO_MONTHLY_TOTAL (T_지로월별거래통계) - 통계
-- ---------------------------------------------------------------------
CREATE TABLE T_GIRO_MONTHLY_TOTAL (
    STAT_YYYYMM   CHAR(6)        NOT NULL,   -- 통계년월
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    REGION_CD     CHAR(4)        NOT NULL,   -- 지역코드
    GIRO_CNT      NUMBER(19),                -- 지로거래건수
    GIRO_AMOUNT   NUMBER(19),                -- 지로금액
    APT_CNT       NUMBER(19),                -- 아파트관리비거래건수
    APT_AMOUNT    NUMBER(19),                -- 아파트관리비금액
    CONSTRAINT PK_T_GIRO_MONTHLY_TOTAL PRIMARY KEY
        (STAT_YYYYMM, TERM_GROUP_ID, TERM_ID, REGION_CD)
);

COMMENT ON TABLE  T_GIRO_MONTHLY_TOTAL               IS 'T_지로월별거래통계';
COMMENT ON COLUMN T_GIRO_MONTHLY_TOTAL.STAT_YYYYMM   IS '통계년월';
COMMENT ON COLUMN T_GIRO_MONTHLY_TOTAL.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_GIRO_MONTHLY_TOTAL.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_GIRO_MONTHLY_TOTAL.REGION_CD     IS '지역코드';
COMMENT ON COLUMN T_GIRO_MONTHLY_TOTAL.GIRO_CNT      IS '지로거래건수';
COMMENT ON COLUMN T_GIRO_MONTHLY_TOTAL.GIRO_AMOUNT   IS '지로금액';
COMMENT ON COLUMN T_GIRO_MONTHLY_TOTAL.APT_CNT       IS '아파트관리비거래건수';
COMMENT ON COLUMN T_GIRO_MONTHLY_TOTAL.APT_AMOUNT    IS '아파트관리비금액';

COMMIT;
