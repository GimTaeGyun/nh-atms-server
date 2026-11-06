-- =====================================================================
-- V1.4.0 : 마감저널(jnl_kind=31) 저장구조 MG 정합 재설계
--
--   배경 : 기존 T_DAY_TOTAL 은 마감저널 본문을 JSON CLOB 한 행으로만
--          적재하여, 마감종류(소계/합계)와 마감항목(host/atm 집계)이
--          구조화되지 않아 조회/필터가 불가능했다. 또한 핸들러가
--          interface.journal.json 명세 키(tot_ty / bgn_* / end_* /
--          host_entries / atm_entries)가 아닌 임시 키(total_type /
--          begin_date ...)를 읽어 소계/합계 자체가 적재되지 않았다.
--
--   변경 : (구)새마을금고 T_DAYTOTAL 과 동일하게 "마감항목 1건 = 1행"
--          평면 구조로 재설계한다.
--            · TOTAL_SRC_CD : 집계원천 (1=호스트(host), 2=기기(atm))
--            · TOTAL_CD     : 마감항목코드 (저널 entry.cd, 4자리)
--            · TOTAL_VAL    : 집계값 (count 5 / amount 15)
--            · ACTION_TYPE  : 마감종류 (저널 tot_ty → 5=소계, 6=합계)
--          마감 기간(BEGIN/END)은 동일 저널의 모든 행에 비정규화 보관.
--          PK = (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME,
--                TOTAL_SRC_CD, TOTAL_CD)
--
--   주의 : 기존 T_DAY_TOTAL 을 DROP 후 재생성한다(개발 데이터 한정).
--   참조 : interface.journal.json "31" / MG t_daytotal / V1.0.2(구 DDL)
-- =====================================================================

-- ── 기존 테이블 제거(존재 시) ────────────────────────────────────────
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE T_DAY_TOTAL CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN RAISE; END IF;   -- -942 : table does not exist
END;
/

-- ── 마감저널 (jnl_kind=31) : 마감항목별 1행 ──────────────────────────
CREATE TABLE T_DAY_TOTAL (
    TERM_GROUP_ID  CHAR(5)       NOT NULL,   -- 영업점코드
    TERM_ID        VARCHAR2(20)  NOT NULL,   -- 단말번호
    JNL_DATE       CHAR(8)       NOT NULL,   -- 저널발생일(YYYYMMDD)
    JNL_TIME       CHAR(10)      NOT NULL,   -- 저널발생시각(HHMMSSfff+flag)
    TOTAL_SRC_CD   CHAR(1)       NOT NULL,   -- 집계원천(1=호스트,2=기기)
    TOTAL_CD       CHAR(4)       NOT NULL,   -- 마감항목코드(저널 entry.cd)
    TOTAL_VAL      VARCHAR2(15),             -- 집계값(건수5/금액15)
    ACTION_TYPE    CHAR(1),                  -- 마감종류(5=소계,6=합계 : tot_ty)
    BEGIN_DATE     CHAR(8),                  -- 마감기간 시작일(YYYYMMDD)
    BEGIN_TIME     CHAR(6),                  -- 마감기간 시작시각(HHMMSS)
    END_DATE       CHAR(8),                  -- 마감기간 종료일(YYYYMMDD)
    END_TIME       CHAR(6),                  -- 마감기간 종료시각(HHMMSS)
    CONSTRAINT PK_T_DAY_TOTAL PRIMARY KEY
        (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, TOTAL_SRC_CD, TOTAL_CD)
);

-- 일자/영업점/단말 + 마감종류 조회용
CREATE INDEX IX_T_DAY_TOTAL_SEARCH
    ON T_DAY_TOTAL (JNL_DATE, TERM_GROUP_ID, TERM_ID, ACTION_TYPE);

COMMENT ON TABLE  T_DAY_TOTAL               IS '마감저널(jnl_kind=31) : 마감항목별 1행';
COMMENT ON COLUMN T_DAY_TOTAL.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_DAY_TOTAL.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_DAY_TOTAL.JNL_DATE      IS '저널발생일(YYYYMMDD)';
COMMENT ON COLUMN T_DAY_TOTAL.JNL_TIME      IS '저널발생시각';
COMMENT ON COLUMN T_DAY_TOTAL.TOTAL_SRC_CD  IS '집계원천(1=호스트,2=기기)';
COMMENT ON COLUMN T_DAY_TOTAL.TOTAL_CD      IS '마감항목코드(저널 entry.cd)';
COMMENT ON COLUMN T_DAY_TOTAL.TOTAL_VAL     IS '집계값(건수5/금액15)';
COMMENT ON COLUMN T_DAY_TOTAL.ACTION_TYPE   IS '마감종류(5=소계,6=합계 : tot_ty)';
COMMENT ON COLUMN T_DAY_TOTAL.BEGIN_DATE    IS '마감기간 시작일(YYYYMMDD)';
COMMENT ON COLUMN T_DAY_TOTAL.BEGIN_TIME    IS '마감기간 시작시각(HHMMSS)';
COMMENT ON COLUMN T_DAY_TOTAL.END_DATE      IS '마감기간 종료일(YYYYMMDD)';
COMMENT ON COLUMN T_DAY_TOTAL.END_TIME      IS '마감기간 종료시각(HHMMSS)';

COMMIT;
