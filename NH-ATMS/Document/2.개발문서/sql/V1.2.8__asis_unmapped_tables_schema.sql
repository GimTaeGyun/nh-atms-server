-- =====================================================================
-- V1.2.8 : AS-IS 이미지 기준 미매핑 테이블 8종 (TO-BE 보강)
--   T_VIP_CUSTOMER       : 우수고객 관리        (자동화기기_우수고객)
--   T_PROMOTION         : 홍보문구 관리        (자동화기기_홍보문구관리)
--   T_OVERSEA_REMITTANCE : 해외송금 관리        (자동화기기_해외송금관리)
--   T_DESTROY_VIEW_HIST   : 1차파기 거래조회 기록 (자동화기기_1차파기거래조회기록)
--   T_ACCIDENT_REPORT    : 사고신고 전문        (자동화기기_사고신고전문)
--   T_CASH_SHORTAGE      : 시재부족 관리거래     (자동화기기_시재부족관리거래)
--   T_EAPPROVAL_HIST     : 전자결재 이력        (자동화기기_전자결재이력)
--   T_SLIP_EDIT          : 명세표 편집          (자동화기기_명세표편집)
--
-- 근거: 0.기본자료/농협은행/1.DB/*.png ERD(이미 Oracle 타입) +
--       NH AS-IS 테이블 분석.md. 한글테이블명→T_+영문 SNAKE_CASE 변환.
-- 선행: V1.2.7. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_VIP_CUSTOMER (T_우수고객)
-- ---------------------------------------------------------------------
CREATE TABLE T_VIP_CUSTOMER (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    CUSTOMER_NO   VARCHAR2(20)   NOT NULL,   -- 고객번호
    VIP_ID        VARCHAR2(20),              -- VIPID
    VIP_GRADE     CHAR(2),                   -- 우수등급
    START_DATE    CHAR(8),                   -- 유지시작일
    END_DATE      CHAR(8),                   -- 유지종료일
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_DATE      CHAR(8),                   -- 등록일
    CONSTRAINT PK_T_VIP_CUSTOMER PRIMARY KEY (TERM_GROUP_ID, CUSTOMER_NO)
);

COMMENT ON TABLE  T_VIP_CUSTOMER               IS 'T_우수고객';
COMMENT ON COLUMN T_VIP_CUSTOMER.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_VIP_CUSTOMER.CUSTOMER_NO   IS '고객번호';
COMMENT ON COLUMN T_VIP_CUSTOMER.VIP_ID        IS 'VIPID';
COMMENT ON COLUMN T_VIP_CUSTOMER.VIP_GRADE     IS '우수등급';
COMMENT ON COLUMN T_VIP_CUSTOMER.START_DATE    IS '유지시작일';
COMMENT ON COLUMN T_VIP_CUSTOMER.END_DATE      IS '유지종료일';
COMMENT ON COLUMN T_VIP_CUSTOMER.USE_YN        IS '사용여부';
COMMENT ON COLUMN T_VIP_CUSTOMER.REG_DATE      IS '등록일';

-- ---------------------------------------------------------------------
-- 2) T_PROMOTION (T_홍보문구관리)
-- ---------------------------------------------------------------------
CREATE TABLE T_PROMOTION (
    PROMO_ID      NUMBER(19)     NOT NULL,   -- 홍보문구ID
    TERM_GROUP_ID CHAR(5),                   -- 영업점코드
    BRANCH_TYPE   CHAR(1),                   -- 점조구분
    START_DATE    CHAR(8),                   -- 홍보시작일
    END_DATE      CHAR(8),                   -- 홍보종료일
    PROMO_CONTENT VARCHAR2(2000),            -- 홍보문구내용
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_USER_ID   VARCHAR2(10),              -- 등록자ID
    REG_DATE      CHAR(14),                  -- 등록일시
    CONSTRAINT PK_T_PROMOTION PRIMARY KEY (PROMO_ID)
);

COMMENT ON TABLE  T_PROMOTION               IS 'T_홍보문구관리';
COMMENT ON COLUMN T_PROMOTION.PROMO_ID      IS '홍보문구ID';
COMMENT ON COLUMN T_PROMOTION.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_PROMOTION.BRANCH_TYPE   IS '점조구분';
COMMENT ON COLUMN T_PROMOTION.START_DATE    IS '홍보시작일';
COMMENT ON COLUMN T_PROMOTION.END_DATE      IS '홍보종료일';
COMMENT ON COLUMN T_PROMOTION.PROMO_CONTENT IS '홍보문구내용';
COMMENT ON COLUMN T_PROMOTION.USE_YN        IS '사용여부';
COMMENT ON COLUMN T_PROMOTION.REG_USER_ID   IS '등록자ID';
COMMENT ON COLUMN T_PROMOTION.REG_DATE      IS '등록일시';

-- ---------------------------------------------------------------------
-- 3) T_OVERSEA_REMITTANCE (T_해외송금관리)
-- ---------------------------------------------------------------------
CREATE TABLE T_OVERSEA_REMITTANCE (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 거래일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    TR_SEQ        NUMBER(10)     NOT NULL,   -- 거래일련번호
    EXCEPT_CD     CHAR(2),                   -- 예외사유코드
    CURRENCY_CD   CHAR(3),                   -- 통화코드
    REMIT_AMOUNT  NUMBER(19,2),              -- 송금금액
    RECV_COUNTRY  CHAR(3),                   -- 수취국가코드
    RECV_BANK     VARCHAR2(100),             -- 수취은행
    STATUS        CHAR(2),                   -- 처리상태
    CONSTRAINT PK_T_OVERSEA_REMITTANCE PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, TR_SEQ)
);

COMMENT ON TABLE  T_OVERSEA_REMITTANCE               IS 'T_해외송금관리';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.JNL_DATE      IS '거래일자';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.TR_SEQ        IS '거래일련번호';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.EXCEPT_CD     IS '예외사유코드';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.CURRENCY_CD   IS '통화코드';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.REMIT_AMOUNT  IS '송금금액';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.RECV_COUNTRY  IS '수취국가코드';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.RECV_BANK     IS '수취은행';
COMMENT ON COLUMN T_OVERSEA_REMITTANCE.STATUS        IS '처리상태';

-- ---------------------------------------------------------------------
-- 4) T_DESTROY_VIEW_HIST (T_1차파기거래조회기록) - 1020/1301 파기조회 감사
-- ---------------------------------------------------------------------
CREATE TABLE T_DESTROY_VIEW_HIST (
    VIEW_DATE     CHAR(8)        NOT NULL,   -- 조회일자
    VIEW_SEQ      NUMBER(19)     NOT NULL,   -- 조회일련번호
    VIEW_TIME     CHAR(6),                   -- 조회시각
    TERM_GROUP_ID CHAR(5),                   -- 조회사무소코드
    INVEST_DATE   CHAR(8),                   -- 조사일자
    APPROVER1_NO  VARCHAR2(9),              -- 1차승인자사번
    APPROVER2_NO  VARCHAR2(9),              -- 2차승인자사번
    REASON_CD     CHAR(2),                   -- 조회사유코드
    REASON_DESC   VARCHAR2(200),             -- 상세사유
    CONSTRAINT PK_T_DESTROY_VIEW_HIST PRIMARY KEY (VIEW_DATE, VIEW_SEQ)
);

COMMENT ON TABLE  T_DESTROY_VIEW_HIST               IS 'T_1차파기거래조회기록';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.VIEW_DATE     IS '조회일자';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.VIEW_SEQ      IS '조회일련번호';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.VIEW_TIME     IS '조회시각';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.TERM_GROUP_ID IS '조회사무소코드';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.INVEST_DATE   IS '조사일자';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.APPROVER1_NO  IS '1차승인자사번';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.APPROVER2_NO  IS '2차승인자사번';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.REASON_CD     IS '조회사유코드';
COMMENT ON COLUMN T_DESTROY_VIEW_HIST.REASON_DESC   IS '상세사유';

-- ---------------------------------------------------------------------
-- 5) T_ACCIDENT_REPORT (T_사고신고전문)
-- ---------------------------------------------------------------------
CREATE TABLE T_ACCIDENT_REPORT (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 거래일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    REPORT_SEQ    NUMBER(10)     NOT NULL,   -- 신고일련번호
    SEND_RESULT   VARCHAR2(4),               -- 전송결과
    SEND_MSG      VARCHAR2(2000),            -- 전송전문
    SEND_TIME     CHAR(14),                  -- 전송일시
    CONSTRAINT PK_T_ACCIDENT_REPORT PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, REPORT_SEQ)
);

COMMENT ON TABLE  T_ACCIDENT_REPORT               IS 'T_사고신고전문';
COMMENT ON COLUMN T_ACCIDENT_REPORT.JNL_DATE      IS '거래일자';
COMMENT ON COLUMN T_ACCIDENT_REPORT.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_ACCIDENT_REPORT.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_ACCIDENT_REPORT.REPORT_SEQ    IS '신고일련번호';
COMMENT ON COLUMN T_ACCIDENT_REPORT.SEND_RESULT   IS '전송결과';
COMMENT ON COLUMN T_ACCIDENT_REPORT.SEND_MSG      IS '전송전문';
COMMENT ON COLUMN T_ACCIDENT_REPORT.SEND_TIME     IS '전송일시';

-- ---------------------------------------------------------------------
-- 6) T_CASH_SHORTAGE (T_시재부족관리거래)
-- ---------------------------------------------------------------------
CREATE TABLE T_CASH_SHORTAGE (
    JNL_DATE      CHAR(8)        NOT NULL,   -- 거래일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    SHORTAGE_SEQ  NUMBER(10)     NOT NULL,   -- 부족일련번호
    DENOM_CD      VARCHAR2(10),              -- 권종코드
    DEVICE_AMOUNT NUMBER(19),                -- 기기대사금액
    SHORTAGE_AMT  NUMBER(19),                -- 부족금액
    NOTIFY_FLAG   CHAR(1),                   -- 알림여부
    CONSTRAINT PK_T_CASH_SHORTAGE PRIMARY KEY
        (JNL_DATE, TERM_GROUP_ID, TERM_ID, SHORTAGE_SEQ)
);

COMMENT ON TABLE  T_CASH_SHORTAGE               IS 'T_시재부족관리거래';
COMMENT ON COLUMN T_CASH_SHORTAGE.JNL_DATE      IS '거래일자';
COMMENT ON COLUMN T_CASH_SHORTAGE.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_CASH_SHORTAGE.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_CASH_SHORTAGE.SHORTAGE_SEQ  IS '부족일련번호';
COMMENT ON COLUMN T_CASH_SHORTAGE.DENOM_CD      IS '권종코드';
COMMENT ON COLUMN T_CASH_SHORTAGE.DEVICE_AMOUNT IS '기기대사금액';
COMMENT ON COLUMN T_CASH_SHORTAGE.SHORTAGE_AMT  IS '부족금액';
COMMENT ON COLUMN T_CASH_SHORTAGE.NOTIFY_FLAG   IS '알림여부';

-- ---------------------------------------------------------------------
-- 7) T_EAPPROVAL_HIST (T_전자결재이력)
-- ---------------------------------------------------------------------
CREATE TABLE T_EAPPROVAL_HIST (
    APPROVE_DATE  CHAR(8)        NOT NULL,   -- 발생일자
    APPROVE_SEQ   NUMBER(19)     NOT NULL,   -- 결재일련번호
    TERM_GROUP_ID CHAR(5),                   -- 사무소코드
    DRAFTER_NO    VARCHAR2(9),               -- 기안자개인번호
    APPROVER_NO   VARCHAR2(9),               -- 결재자개인번호
    APPROVE_TIME  CHAR(14),                  -- 결재일시
    RESULT_CD     CHAR(2),                   -- 결과코드(승인/반려)
    APPROVE_TYPE  CHAR(2),                   -- 결재구분
    CONSTRAINT PK_T_EAPPROVAL_HIST PRIMARY KEY (APPROVE_DATE, APPROVE_SEQ)
);

COMMENT ON TABLE  T_EAPPROVAL_HIST               IS 'T_전자결재이력';
COMMENT ON COLUMN T_EAPPROVAL_HIST.APPROVE_DATE  IS '발생일자';
COMMENT ON COLUMN T_EAPPROVAL_HIST.APPROVE_SEQ   IS '결재일련번호';
COMMENT ON COLUMN T_EAPPROVAL_HIST.TERM_GROUP_ID IS '사무소코드';
COMMENT ON COLUMN T_EAPPROVAL_HIST.DRAFTER_NO    IS '기안자개인번호';
COMMENT ON COLUMN T_EAPPROVAL_HIST.APPROVER_NO   IS '결재자개인번호';
COMMENT ON COLUMN T_EAPPROVAL_HIST.APPROVE_TIME  IS '결재일시';
COMMENT ON COLUMN T_EAPPROVAL_HIST.RESULT_CD     IS '결과코드';
COMMENT ON COLUMN T_EAPPROVAL_HIST.APPROVE_TYPE  IS '결재구분';

-- ---------------------------------------------------------------------
-- 8) T_SLIP_EDIT (T_명세표편집) - 2301 명세표 양식 항목편집
-- ---------------------------------------------------------------------
CREATE TABLE T_SLIP_EDIT (
    SLIP_ID       VARCHAR2(6)    NOT NULL,   -- 명세표ID
    ITEM_SEQ      NUMBER(10)     NOT NULL,   -- 항목순번
    ROW_NUMBER    NUMBER(10),                -- 행번호
    TITLE         VARCHAR2(100),             -- 항목제목
    COL_NUMBER    NUMBER(10),                -- 열번호
    ITEM_POSITION NUMBER(10),                -- 항목위치
    ATTRIBUTE     CHAR(1),                   -- 속성
    CONSTRAINT PK_T_SLIP_EDIT PRIMARY KEY (SLIP_ID, ITEM_SEQ)
);

COMMENT ON TABLE  T_SLIP_EDIT               IS 'T_명세표편집';
COMMENT ON COLUMN T_SLIP_EDIT.SLIP_ID       IS '명세표ID';
COMMENT ON COLUMN T_SLIP_EDIT.ITEM_SEQ      IS '항목순번';
COMMENT ON COLUMN T_SLIP_EDIT.ROW_NUMBER    IS '행번호';
COMMENT ON COLUMN T_SLIP_EDIT.TITLE         IS '항목제목';
COMMENT ON COLUMN T_SLIP_EDIT.COL_NUMBER    IS '열번호';
COMMENT ON COLUMN T_SLIP_EDIT.ITEM_POSITION IS '항목위치';
COMMENT ON COLUMN T_SLIP_EDIT.ATTRIBUTE     IS '속성';

COMMIT;
