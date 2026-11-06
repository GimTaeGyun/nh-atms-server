-- =====================================================================
-- V1.3.1 : 일괄용역/SLA 기준 마스터 3종 (6208/6209/6211)
--   T_SLA_EVAL_ITEM   : SLA 평가항목 기준 마스터(이력)   (6208)
--   T_PNI_GRADE       : P&I 등급컷 마스터                (6209)
--   T_SERVICE_FEE_STD : 용역료 단가 기준 마스터(이력)     (6211)
--
-- 근거: 16_통합스키마설계.md §6.2 (SLA 기준 마스터, 하드코딩 금지)
--   V1.3.0 은 SLA "결과/이력" 8종을 생성했으나, 그 결과를 산정하는
--   "기준값"(평가항목 배점·등급컷·용역료 단가)은 테이블이 없어
--   하드코딩될 위험이 있었음. 본 마이그레이션이 그 기준 마스터를 보강한다.
--   SLA 산정 흐름:
--     출동raw(T_DISPATCH_HISTORY) → 항목점수(T_SLA_EVAL_ITEM 기준)
--       → 합산(T_CORNER_SLA/T_SLA_GRADE) → 등급(T_PNI_GRADE)
--       → 용역료(T_SERVICE_FEE_STD × P&I율 = T_SERVICE_FEE_CALC)
--   업체키 VENDOR_CD 는 T_VENDOR 논리참조.
-- 명명: 16_통합스키마설계.md §0.5 명명 표준(언더스코어)을 따른다.
--   (헤더에 보이는 T_SLA_GRADE/T_CORNER_SLA 등은 기존 트랙 A 테이블이라 원형 유지)
-- 선행: V1.3.0. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_SLA_EVAL_ITEM (T_SLA평가항목기준) - 6208 평가기준관리
--    월별 이력 마스터: 평가항목(가동률/30분출동율/평균출동시간/복구율/
--    현장조치/코너환경/CS/현금부족/운영자금보유율 등)의 배점 기준을
--    기준월 단위로 보관하여 과거 평가 재현이 가능하도록 한다.
-- ---------------------------------------------------------------------
CREATE TABLE T_SLA_EVAL_ITEM (
    APPLY_MONTH   CHAR(6)        NOT NULL,   -- 기준월(yyyyMM)
    ITEM_CD       VARCHAR2(2)    NOT NULL,   -- 평가코드(01~11)
    ITEM_NM       VARCHAR2(100),             -- 평가종류명
    ITEM_TYPE1    CHAR(1),                   -- 계산구분타입1
    ITEM_TYPE2    CHAR(1),                   -- 계산구분타입2
    ITEM_TYPE3    CHAR(1),                   -- 계산구분타입3
    ITEM_TYPE4    CHAR(1),                   -- 계산구분타입4
    MAX_SCORE     NUMBER(7,2),               -- 최고점
    MIN_SCORE     NUMBER(7,2),               -- 최저점
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_DT        CHAR(14),                  -- 등록일시
    REG_ID        VARCHAR2(10),              -- 등록자ID
    UPD_DT        CHAR(14),                  -- 수정일시
    UPD_ID        VARCHAR2(10),              -- 수정자ID
    CONSTRAINT PK_T_SLA_EVAL_ITEM PRIMARY KEY (APPLY_MONTH, ITEM_CD)
);

COMMENT ON TABLE  T_SLA_EVAL_ITEM             IS 'T_SLA평가항목기준';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.APPLY_MONTH IS '기준월(yyyyMM)';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.ITEM_CD     IS '평가코드(01~11)';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.ITEM_NM     IS '평가종류명';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.ITEM_TYPE1  IS '계산구분타입1';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.ITEM_TYPE2  IS '계산구분타입2';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.ITEM_TYPE3  IS '계산구분타입3';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.ITEM_TYPE4  IS '계산구분타입4';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.MAX_SCORE   IS '최고점';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.MIN_SCORE   IS '최저점';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.USE_YN      IS '사용여부';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.REG_DT      IS '등록일시';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.REG_ID      IS '등록자ID';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.UPD_DT      IS '수정일시';
COMMENT ON COLUMN T_SLA_EVAL_ITEM.UPD_ID      IS '수정자ID';

-- ---------------------------------------------------------------------
-- 2) T_PNI_GRADE (T_PNI등급컷) - 6209 P&I등급관리
--    종합점수 → 등급(S/A/B/C/D/E/F) 매핑 및 등급별 용역료 가감율.
--    예) S 951~1000(+5%), C 851~900, F 0~800(-5%).
-- ---------------------------------------------------------------------
CREATE TABLE T_PNI_GRADE (
    GRADE         VARCHAR2(2)    NOT NULL,   -- 등급(S/A/B/C/D/E/F)
    MIN_SCORE     NUMBER(7,2),               -- 점수구간 하한
    MAX_SCORE     NUMBER(7,2),               -- 점수구간 상한
    PNI_RATE      NUMBER(5,2),               -- P&I적용율(용역료 가감율 %)
    GRADE_DESC    VARCHAR2(100),             -- 등급설명
    USE_YN        CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_DT        CHAR(14),                  -- 등록일시
    REG_ID        VARCHAR2(10),              -- 등록자ID
    UPD_DT        CHAR(14),                  -- 수정일시
    UPD_ID        VARCHAR2(10),              -- 수정자ID
    CONSTRAINT PK_T_PNI_GRADE PRIMARY KEY (GRADE)
);

COMMENT ON TABLE  T_PNI_GRADE            IS 'T_PNI등급컷';
COMMENT ON COLUMN T_PNI_GRADE.GRADE      IS '등급(S/A/B/C/D/E/F)';
COMMENT ON COLUMN T_PNI_GRADE.MIN_SCORE  IS '점수구간 하한';
COMMENT ON COLUMN T_PNI_GRADE.MAX_SCORE  IS '점수구간 상한';
COMMENT ON COLUMN T_PNI_GRADE.PNI_RATE   IS 'P&I적용율(용역료 가감율 %)';
COMMENT ON COLUMN T_PNI_GRADE.GRADE_DESC IS '등급설명';
COMMENT ON COLUMN T_PNI_GRADE.USE_YN     IS '사용여부';
COMMENT ON COLUMN T_PNI_GRADE.REG_DT     IS '등록일시';
COMMENT ON COLUMN T_PNI_GRADE.REG_ID     IS '등록자ID';
COMMENT ON COLUMN T_PNI_GRADE.UPD_DT     IS '수정일시';
COMMENT ON COLUMN T_PNI_GRADE.UPD_ID     IS '수정자ID';

-- ---------------------------------------------------------------------
-- 3) T_SERVICE_FEE_STD (T_용역료단가기준) - 6211 용역기준관리
--    월별 이력 마스터: 용역업체×운영구분(무인점포/점외24시간/점외365/
--    점외시간내)별 기본용역료 및 추가용역료 단가. T_SERVICE_FEE_CALC 가
--    이 단가에 P&I율(T_PNI_GRADE)을 적용해 최종 용역료를 산정한다.
-- ---------------------------------------------------------------------
CREATE TABLE T_SERVICE_FEE_STD (
    APPLY_MONTH      CHAR(6)        NOT NULL,-- 기준월(yyyyMM)
    VENDOR_CD        VARCHAR2(10)   NOT NULL,-- 용역업체코드
    OPER_TYPE        VARCHAR2(20)   NOT NULL,-- 운영구분
    BASE_DEV_CNT     NUMBER(10),             -- 운영기준대수
    BASE_FEE         NUMBER(18),             -- 기본용역료
    CHK_OUT_FEE      NUMBER(18),             -- 수표출금 추가용역료
    CHK_IN_FEE       NUMBER(18),             -- 수표입금 추가용역료
    INCENTIVE_MAX    NUMBER(18),             -- 인센티브 최대금액
    ADD_FEE_SECURITY NUMBER(18),             -- 추가용역료(경비구축)
    ADD_FEE_DISTANCE NUMBER(18),             -- 추가용역료(거리별)
    USE_YN           CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_DT           CHAR(14),               -- 등록일시
    REG_ID           VARCHAR2(10),           -- 등록자ID
    UPD_DT           CHAR(14),               -- 수정일시
    UPD_ID           VARCHAR2(10),           -- 수정자ID
    CONSTRAINT PK_T_SERVICE_FEE_STD PRIMARY KEY (APPLY_MONTH, VENDOR_CD, OPER_TYPE)
);

COMMENT ON TABLE  T_SERVICE_FEE_STD                  IS 'T_용역료단가기준';
COMMENT ON COLUMN T_SERVICE_FEE_STD.APPLY_MONTH      IS '기준월(yyyyMM)';
COMMENT ON COLUMN T_SERVICE_FEE_STD.VENDOR_CD        IS '용역업체코드';
COMMENT ON COLUMN T_SERVICE_FEE_STD.OPER_TYPE        IS '운영구분(무인점포/점외24시간/점외365/점외시간내)';
COMMENT ON COLUMN T_SERVICE_FEE_STD.BASE_DEV_CNT     IS '운영기준대수';
COMMENT ON COLUMN T_SERVICE_FEE_STD.BASE_FEE         IS '기본용역료';
COMMENT ON COLUMN T_SERVICE_FEE_STD.CHK_OUT_FEE      IS '수표출금 추가용역료';
COMMENT ON COLUMN T_SERVICE_FEE_STD.CHK_IN_FEE       IS '수표입금 추가용역료';
COMMENT ON COLUMN T_SERVICE_FEE_STD.INCENTIVE_MAX    IS '인센티브 최대금액';
COMMENT ON COLUMN T_SERVICE_FEE_STD.ADD_FEE_SECURITY IS '추가용역료(경비구축)';
COMMENT ON COLUMN T_SERVICE_FEE_STD.ADD_FEE_DISTANCE IS '추가용역료(거리별)';
COMMENT ON COLUMN T_SERVICE_FEE_STD.USE_YN           IS '사용여부';
COMMENT ON COLUMN T_SERVICE_FEE_STD.REG_DT           IS '등록일시';
COMMENT ON COLUMN T_SERVICE_FEE_STD.REG_ID           IS '등록자ID';
COMMENT ON COLUMN T_SERVICE_FEE_STD.UPD_DT           IS '수정일시';
COMMENT ON COLUMN T_SERVICE_FEE_STD.UPD_ID           IS '수정자ID';

COMMIT;
