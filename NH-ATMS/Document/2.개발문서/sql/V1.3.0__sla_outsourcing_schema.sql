-- =====================================================================
-- V1.3.0 : 일괄용역/SLA 평가관리 8종 (6200번대 메뉴)
--   T_SLA_GRADE          : 용역업체 SLA등급        (6200)
--   T_SLA_GRADE_HIST      : SLA등급 이력           (6201)
--   T_CORNER_SLA         : 코너 SLA 평가점수        (6202)
--   T_DEVICE_OP_RATE_HIST  : 기기별 가동율 이력        (6203)
--   T_DEVICE_FAULT_HIST   : 기기별 장애이력          (6204)
--   T_VENDOR_CS_EVAL      : 용역업체 C/S 평가         (6206)
--   T_NON_OP_REASON       : 미가동 사유             (6210)
--   T_SERVICE_FEE_CALC    : 용역료 산정             (6212)
--
-- 근거: 10_AS-IS전수분석_및_TO-BE테이블설계.md (5)
--   AS-IS 단일 원천테이블 없음 → 통계/평가 결과 관리용 신규 설계.
--   업체키 VENDOR_CD 는 T_VENDOR 논리참조.
-- 선행: V1.2.9. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_SLA_GRADE (T_SLA등급) - 6200
-- ---------------------------------------------------------------------
CREATE TABLE T_SLA_GRADE (
    EVAL_YYYYMM   CHAR(6)        NOT NULL,   -- 평가년월
    VENDOR_CD     VARCHAR2(10)   NOT NULL,   -- 용역업체코드
    SLA_SCORE     NUMBER(5,2),               -- SLA점수
    SLA_GRADE     CHAR(2),                   -- SLA등급
    OP_RATE       NUMBER(5,2),               -- 가동율
    FAULT_CNT     NUMBER(10),                -- 장애건수
    EVAL_STATUS   CHAR(1)        DEFAULT 'N',-- 확정여부
    CONSTRAINT PK_T_SLA_GRADE PRIMARY KEY (EVAL_YYYYMM, VENDOR_CD)
);

COMMENT ON TABLE  T_SLA_GRADE             IS 'T_SLA등급';
COMMENT ON COLUMN T_SLA_GRADE.EVAL_YYYYMM IS '평가년월';
COMMENT ON COLUMN T_SLA_GRADE.VENDOR_CD   IS '용역업체코드';
COMMENT ON COLUMN T_SLA_GRADE.SLA_SCORE   IS 'SLA점수';
COMMENT ON COLUMN T_SLA_GRADE.SLA_GRADE   IS 'SLA등급';
COMMENT ON COLUMN T_SLA_GRADE.OP_RATE     IS '가동율';
COMMENT ON COLUMN T_SLA_GRADE.FAULT_CNT   IS '장애건수';
COMMENT ON COLUMN T_SLA_GRADE.EVAL_STATUS IS '확정여부';

-- ---------------------------------------------------------------------
-- 2) T_SLA_GRADE_HIST (T_SLA등급이력) - 6201
-- ---------------------------------------------------------------------
CREATE TABLE T_SLA_GRADE_HIST (
    EVAL_YYYYMM   CHAR(6)        NOT NULL,   -- 평가년월
    VENDOR_CD     VARCHAR2(10)   NOT NULL,   -- 용역업체코드
    HIST_SEQ      NUMBER(10)     NOT NULL,   -- 이력일련번호
    BEFORE_GRADE  CHAR(2),                   -- 변경전등급
    AFTER_GRADE   CHAR(2),                   -- 변경후등급
    CHG_REASON    VARCHAR2(200),             -- 변경사유
    CHG_USER_ID   VARCHAR2(10),              -- 변경자ID
    CHG_DATE      CHAR(14),                  -- 변경일시
    CONSTRAINT PK_T_SLA_GRADE_HIST PRIMARY KEY
        (EVAL_YYYYMM, VENDOR_CD, HIST_SEQ)
);

COMMENT ON TABLE  T_SLA_GRADE_HIST              IS 'T_SLA등급이력';
COMMENT ON COLUMN T_SLA_GRADE_HIST.EVAL_YYYYMM  IS '평가년월';
COMMENT ON COLUMN T_SLA_GRADE_HIST.VENDOR_CD    IS '용역업체코드';
COMMENT ON COLUMN T_SLA_GRADE_HIST.HIST_SEQ     IS '이력일련번호';
COMMENT ON COLUMN T_SLA_GRADE_HIST.BEFORE_GRADE IS '변경전등급';
COMMENT ON COLUMN T_SLA_GRADE_HIST.AFTER_GRADE  IS '변경후등급';
COMMENT ON COLUMN T_SLA_GRADE_HIST.CHG_REASON   IS '변경사유';
COMMENT ON COLUMN T_SLA_GRADE_HIST.CHG_USER_ID  IS '변경자ID';
COMMENT ON COLUMN T_SLA_GRADE_HIST.CHG_DATE     IS '변경일시';

-- ---------------------------------------------------------------------
-- 3) T_CORNER_SLA (T_코너SLA평가) - 6202
-- ---------------------------------------------------------------------
CREATE TABLE T_CORNER_SLA (
    EVAL_YYYYMM   CHAR(6)        NOT NULL,   -- 평가년월
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점(코너)코드
    VENDOR_CD     VARCHAR2(10),              -- 용역업체코드
    ENV_SCORE     NUMBER(5,2),               -- 코너환경점수
    OP_SCORE      NUMBER(5,2),               -- 운영점수
    TOTAL_SCORE   NUMBER(5,2),               -- 종합점수
    CORNER_GRADE  CHAR(2),                   -- 코너등급
    CONSTRAINT PK_T_CORNER_SLA PRIMARY KEY (EVAL_YYYYMM, TERM_GROUP_ID)
);

COMMENT ON TABLE  T_CORNER_SLA               IS 'T_코너SLA평가';
COMMENT ON COLUMN T_CORNER_SLA.EVAL_YYYYMM   IS '평가년월';
COMMENT ON COLUMN T_CORNER_SLA.TERM_GROUP_ID IS '영업점(코너)코드';
COMMENT ON COLUMN T_CORNER_SLA.VENDOR_CD     IS '용역업체코드';
COMMENT ON COLUMN T_CORNER_SLA.ENV_SCORE     IS '코너환경점수';
COMMENT ON COLUMN T_CORNER_SLA.OP_SCORE      IS '운영점수';
COMMENT ON COLUMN T_CORNER_SLA.TOTAL_SCORE   IS '종합점수';
COMMENT ON COLUMN T_CORNER_SLA.CORNER_GRADE  IS '코너등급';

-- ---------------------------------------------------------------------
-- 4) T_DEVICE_OP_RATE_HIST (T_기기별가동율이력) - 6203
-- ---------------------------------------------------------------------
CREATE TABLE T_DEVICE_OP_RATE_HIST (
    STAT_DATE     CHAR(8)        NOT NULL,   -- 통계일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    TOTAL_MIN     NUMBER(10),                -- 총운영분
    DOWN_MIN      NUMBER(10),                -- 미가동분
    OP_RATE       NUMBER(5,2),               -- 가동율
    CONSTRAINT PK_T_DEVICE_OP_RATE_HIST PRIMARY KEY
        (STAT_DATE, TERM_GROUP_ID, TERM_ID)
);

COMMENT ON TABLE  T_DEVICE_OP_RATE_HIST               IS 'T_기기별가동율이력';
COMMENT ON COLUMN T_DEVICE_OP_RATE_HIST.STAT_DATE     IS '통계일자';
COMMENT ON COLUMN T_DEVICE_OP_RATE_HIST.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_DEVICE_OP_RATE_HIST.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_DEVICE_OP_RATE_HIST.TOTAL_MIN     IS '총운영분';
COMMENT ON COLUMN T_DEVICE_OP_RATE_HIST.DOWN_MIN      IS '미가동분';
COMMENT ON COLUMN T_DEVICE_OP_RATE_HIST.OP_RATE       IS '가동율';

-- ---------------------------------------------------------------------
-- 5) T_DEVICE_FAULT_HIST (T_기기별장애이력) - 6204
-- ---------------------------------------------------------------------
CREATE TABLE T_DEVICE_FAULT_HIST (
    FAULT_DATE    CHAR(8)        NOT NULL,   -- 장애발생일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    FAULT_SEQ     NUMBER(10)     NOT NULL,   -- 장애일련번호
    FAULT_CD      VARCHAR2(10),              -- 장애코드
    OCCUR_TIME    CHAR(6),                   -- 발생시각
    RECOVER_TIME  CHAR(6),                   -- 복구시각
    VENDOR_CD     VARCHAR2(10),              -- 처리용역업체코드
    CONSTRAINT PK_T_DEVICE_FAULT_HIST PRIMARY KEY
        (FAULT_DATE, TERM_GROUP_ID, TERM_ID, FAULT_SEQ)
);

COMMENT ON TABLE  T_DEVICE_FAULT_HIST               IS 'T_기기별장애이력';
COMMENT ON COLUMN T_DEVICE_FAULT_HIST.FAULT_DATE    IS '장애발생일자';
COMMENT ON COLUMN T_DEVICE_FAULT_HIST.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_DEVICE_FAULT_HIST.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_DEVICE_FAULT_HIST.FAULT_SEQ     IS '장애일련번호';
COMMENT ON COLUMN T_DEVICE_FAULT_HIST.FAULT_CD      IS '장애코드';
COMMENT ON COLUMN T_DEVICE_FAULT_HIST.OCCUR_TIME    IS '발생시각';
COMMENT ON COLUMN T_DEVICE_FAULT_HIST.RECOVER_TIME  IS '복구시각';
COMMENT ON COLUMN T_DEVICE_FAULT_HIST.VENDOR_CD     IS '처리용역업체코드';

-- ---------------------------------------------------------------------
-- 6) T_VENDOR_CS_EVAL (T_용역업체CS평가) - 6206
-- ---------------------------------------------------------------------
CREATE TABLE T_VENDOR_CS_EVAL (
    EVAL_YYYYMM   CHAR(6)        NOT NULL,   -- 평가년월
    VENDOR_CD     VARCHAR2(10)   NOT NULL,   -- 용역업체코드
    CS_SCORE      NUMBER(5,2),               -- C/S점수
    CSO_SCORE     NUMBER(5,2),               -- C/SO점수
    RESPONSE_TIME NUMBER(10),                -- 평균응답시간(분)
    EVAL_REMARK   VARCHAR2(200),             -- 평가비고
    CONSTRAINT PK_T_VENDOR_CS_EVAL PRIMARY KEY (EVAL_YYYYMM, VENDOR_CD)
);

COMMENT ON TABLE  T_VENDOR_CS_EVAL               IS 'T_용역업체CS평가';
COMMENT ON COLUMN T_VENDOR_CS_EVAL.EVAL_YYYYMM   IS '평가년월';
COMMENT ON COLUMN T_VENDOR_CS_EVAL.VENDOR_CD     IS '용역업체코드';
COMMENT ON COLUMN T_VENDOR_CS_EVAL.CS_SCORE      IS 'C/S점수';
COMMENT ON COLUMN T_VENDOR_CS_EVAL.CSO_SCORE     IS 'C/SO점수';
COMMENT ON COLUMN T_VENDOR_CS_EVAL.RESPONSE_TIME IS '평균응답시간(분)';
COMMENT ON COLUMN T_VENDOR_CS_EVAL.EVAL_REMARK   IS '평가비고';

-- ---------------------------------------------------------------------
-- 7) T_NON_OP_REASON (T_미가동사유) - 6210
-- ---------------------------------------------------------------------
CREATE TABLE T_NON_OP_REASON (
    NONOP_DATE    CHAR(8)        NOT NULL,   -- 미가동일자
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    NONOP_SEQ     NUMBER(10)     NOT NULL,   -- 미가동일련번호
    REASON_CD     VARCHAR2(10),              -- 미가동사유코드
    REASON_DESC   VARCHAR2(200),             -- 사유설명
    EXCLUDE_YN    CHAR(1)        DEFAULT 'N',-- 가동율제외여부
    CONSTRAINT PK_T_NON_OP_REASON PRIMARY KEY
        (NONOP_DATE, TERM_GROUP_ID, TERM_ID, NONOP_SEQ)
);

COMMENT ON TABLE  T_NON_OP_REASON               IS 'T_미가동사유';
COMMENT ON COLUMN T_NON_OP_REASON.NONOP_DATE    IS '미가동일자';
COMMENT ON COLUMN T_NON_OP_REASON.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_NON_OP_REASON.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_NON_OP_REASON.NONOP_SEQ     IS '미가동일련번호';
COMMENT ON COLUMN T_NON_OP_REASON.REASON_CD     IS '미가동사유코드';
COMMENT ON COLUMN T_NON_OP_REASON.REASON_DESC   IS '사유설명';
COMMENT ON COLUMN T_NON_OP_REASON.EXCLUDE_YN    IS '가동율제외여부';

-- ---------------------------------------------------------------------
-- 8) T_SERVICE_FEE_CALC (T_용역료산정) - 6212
-- ---------------------------------------------------------------------
CREATE TABLE T_SERVICE_FEE_CALC (
    CALC_YYYYMM   CHAR(6)        NOT NULL,   -- 산정년월
    VENDOR_CD     VARCHAR2(10)   NOT NULL,   -- 용역업체코드
    BASE_FEE      NUMBER(19),                -- 기본용역료
    PENALTY_FEE   NUMBER(19),                -- 감액(페널티)
    BONUS_FEE     NUMBER(19),                -- 가산(보너스)
    TOTAL_FEE     NUMBER(19),                -- 최종용역료
    CONFIRM_YN    CHAR(1)        DEFAULT 'N',-- 확정여부
    CONSTRAINT PK_T_SERVICE_FEE_CALC PRIMARY KEY (CALC_YYYYMM, VENDOR_CD)
);

COMMENT ON TABLE  T_SERVICE_FEE_CALC             IS 'T_용역료산정';
COMMENT ON COLUMN T_SERVICE_FEE_CALC.CALC_YYYYMM IS '산정년월';
COMMENT ON COLUMN T_SERVICE_FEE_CALC.VENDOR_CD   IS '용역업체코드';
COMMENT ON COLUMN T_SERVICE_FEE_CALC.BASE_FEE    IS '기본용역료';
COMMENT ON COLUMN T_SERVICE_FEE_CALC.PENALTY_FEE IS '감액(페널티)';
COMMENT ON COLUMN T_SERVICE_FEE_CALC.BONUS_FEE   IS '가산(보너스)';
COMMENT ON COLUMN T_SERVICE_FEE_CALC.TOTAL_FEE   IS '최종용역료';
COMMENT ON COLUMN T_SERVICE_FEE_CALC.CONFIRM_YN  IS '확정여부';

COMMIT;
