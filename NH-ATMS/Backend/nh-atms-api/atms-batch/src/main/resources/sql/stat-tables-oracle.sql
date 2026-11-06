-- =====================================================================
-- 배치 통계 테이블 DDL (Oracle 19c / ATMS 스키마)
--
-- 적용 대상 : atms-batch 집계 결과 테이블 (batday01 / batday02)
-- 적용 방법 : ATMS 계정으로 1회 수동 실행
-- 원본      : 새마을금고(atmb) t_atmrunhistory / t_atmerrgrouphistory /
--             t_transactiondailytotal / t_transactioncode (MySQL) 를
--             TO-BE Oracle 명명 규칙(T_XXX_YYY)으로 재정의
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. T_ATM_RUN_HISTORY : 일별 기기 가동률 이력 (batday01)
--    AS-IS t_atmrunhistory 대응. 시간 항목 단위는 모두 '초'.
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_RUN_HISTORY (
    BASE_DATE             CHAR(8)       NOT NULL,  -- 기준일자(yyyyMMdd)
    TERM_GROUP_ID         CHAR(5)       NOT NULL,  -- 영업점(금고)코드
    TERM_ID               VARCHAR2(20)  NOT NULL,  -- 단말번호
    CORNER_CD             VARCHAR2(5),             -- 코너코드
    CORNER_TYPE_CD        CHAR(1),                 -- 코너유형
    OPERATION_TYPE_CD     CHAR(1),                 -- 운영유형
    LOCATION_TYPE_CD      CHAR(1),                 -- 설치장소유형(1:점내 2:점외 3:무인점포)
    AREA_CD               VARCHAR2(20),            -- 지역코드(T_CORNER.AREA1_CD)
    SERVICE_START_TIME    CHAR(4),                 -- 운영시작시간(HHmm)
    SERVICE_END_TIME      CHAR(4),                 -- 운영종료시간(HHmm)
    ATM_CO_CD             VARCHAR2(3),             -- 기기제조사
    MODEL_CD              VARCHAR2(4),             -- 기종코드
    SECURITY_PROVIDER_CD  VARCHAR2(4),             -- 경비업체(T_CORNER.GUARD_CO_CD)
    SERVICE_PROVIDER_CD   VARCHAR2(4),             -- 관리용역업체(T_CORNER.OUTSRC_CO_CD)
    OPEN_TIME             CHAR(6),                 -- 당일 개국시간(HHmmss, 미개국 시 NULL/공백)
    TOTAL_TARGET_TIME     NUMBER(10)  DEFAULT 0,   -- 가동대상시간(초)
    TOTAL_RUN_TIME        NUMBER(10)  DEFAULT 0,   -- 실제가동시간(초) = 대상 - 장애 - 지연개국 - 미개국
    DELAY_OPEN_TIME       NUMBER(10)  DEFAULT 0,   -- 지연개국시간(초)
    SPR_FAULT_CNT         NUMBER(8)   DEFAULT 0,   SPR_FAULT_TIME        NUMBER(10) DEFAULT 0,
    PB_FAULT_CNT          NUMBER(8)   DEFAULT 0,   PB_FAULT_TIME         NUMBER(10) DEFAULT 0,
    JPR_FAULT_CNT         NUMBER(8)   DEFAULT 0,   JPR_FAULT_TIME        NUMBER(10) DEFAULT 0,
    DOC_FAULT_CNT         NUMBER(8)   DEFAULT 0,   DOC_FAULT_TIME        NUMBER(10) DEFAULT 0,
    SCN_FAULT_CNT         NUMBER(8)   DEFAULT 0,   SCN_FAULT_TIME        NUMBER(10) DEFAULT 0,
    IDC_FAULT_CNT         NUMBER(8)   DEFAULT 0,   IDC_FAULT_TIME        NUMBER(10) DEFAULT 0,
    CDM_BILL_FAULT_CNT    NUMBER(8)   DEFAULT 0,   CDM_BILL_FAULT_TIME   NUMBER(10) DEFAULT 0,
    CDM_COIN_FAULT_CNT    NUMBER(8)   DEFAULT 0,   CDM_COIN_FAULT_TIME   NUMBER(10) DEFAULT 0,
    PIN_FAULT_CNT         NUMBER(8)   DEFAULT 0,   PIN_FAULT_TIME        NUMBER(10) DEFAULT 0,
    CHK_FAULT_CNT         NUMBER(8)   DEFAULT 0,   CHK_FAULT_TIME        NUMBER(10) DEFAULT 0,
    DEP_FAULT_CNT         NUMBER(8)   DEFAULT 0,   DEP_FAULT_TIME        NUMBER(10) DEFAULT 0,
    TTU_FAULT_CNT         NUMBER(8)   DEFAULT 0,   TTU_FAULT_TIME        NUMBER(10) DEFAULT 0,
    SIU_FAULT_CNT         NUMBER(8)   DEFAULT 0,   SIU_FAULT_TIME        NUMBER(10) DEFAULT 0,
    VDM_FAULT_CNT         NUMBER(8)   DEFAULT 0,   VDM_FAULT_TIME        NUMBER(10) DEFAULT 0,
    CAM_FAULT_CNT         NUMBER(8)   DEFAULT 0,   CAM_FAULT_TIME        NUMBER(10) DEFAULT 0,
    ALM_FAULT_CNT         NUMBER(8)   DEFAULT 0,   ALM_FAULT_TIME        NUMBER(10) DEFAULT 0,
    CEU_FAULT_CNT         NUMBER(8)   DEFAULT 0,   CEU_FAULT_TIME        NUMBER(10) DEFAULT 0,
    CIM_BILL_FAULT_CNT    NUMBER(8)   DEFAULT 0,   CIM_BILL_FAULT_TIME   NUMBER(10) DEFAULT 0,
    CIM_COIN_FAULT_CNT    NUMBER(8)   DEFAULT 0,   CIM_COIN_FAULT_TIME   NUMBER(10) DEFAULT 0,
    CRD_FAULT_CNT         NUMBER(8)   DEFAULT 0,   CRD_FAULT_TIME        NUMBER(10) DEFAULT 0,
    BCR_FAULT_CNT         NUMBER(8)   DEFAULT 0,   BCR_FAULT_TIME        NUMBER(10) DEFAULT 0,
    IPM_FAULT_CNT         NUMBER(8)   DEFAULT 0,   IPM_FAULT_TIME        NUMBER(10) DEFAULT 0,
    AP_FAULT_CNT          NUMBER(8)   DEFAULT 0,   AP_FAULT_TIME         NUMBER(10) DEFAULT 0,
    MACHINE_FAULT_CNT     NUMBER(8)   DEFAULT 0,   MACHINE_FAULT_TIME    NUMBER(10) DEFAULT 0,  -- 기기(01)
    OPERATION_FAULT_CNT   NUMBER(8)   DEFAULT 0,   OPERATION_FAULT_TIME  NUMBER(10) DEFAULT 0,  -- 운영(02)
    EMP_FAULT_CNT         NUMBER(8)   DEFAULT 0,   EMP_FAULT_TIME        NUMBER(10) DEFAULT 0,  -- 직원(03)
    CUS_FAULT_CNT         NUMBER(8)   DEFAULT 0,   CUS_FAULT_TIME        NUMBER(10) DEFAULT 0,  -- 고객(04)
    NET_FAULT_CNT         NUMBER(8)   DEFAULT 0,   NET_FAULT_TIME        NUMBER(10) DEFAULT 0,  -- 회선(05)
    ETC_FAULT_CNT         NUMBER(8)   DEFAULT 0,   ETC_FAULT_TIME        NUMBER(10) DEFAULT 0,  -- 기타(99)
    TRANSACTION_CNT       NUMBER(10)  DEFAULT 0,   -- 당일 거래건수(TR_TYPE != '0131')
    REG_DT                TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_T_ATM_RUN_HISTORY PRIMARY KEY (BASE_DATE, TERM_GROUP_ID, TERM_ID)
);

-- ---------------------------------------------------------------------
-- 2. T_ATM_ERR_GROUP_HISTORY : 일별 기기·장애그룹별 장애건수 (batday01)
--    AS-IS t_atmerrgrouphistory 대응.
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_ERR_GROUP_HISTORY (
    BASE_DATE      CHAR(8)      NOT NULL,          -- 기준일자(yyyyMMdd)
    TERM_GROUP_ID  CHAR(5)      NOT NULL,          -- 영업점코드
    TERM_ID        VARCHAR2(20) NOT NULL,          -- 단말번호
    ERR_GROUP_CD   CHAR(2)      DEFAULT ' ' NOT NULL, -- 장애그룹(중분류)코드
    FAULT_CNT      NUMBER(8)    DEFAULT 0,         -- 장애건수
    FAULT_TIME     NUMBER(10)   DEFAULT 0,         -- 장애시간(초) — AS-IS 와 동일하게 0 고정 적재
    REG_DT         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_T_ATM_ERR_GROUP_HIST PRIMARY KEY (BASE_DATE, TERM_GROUP_ID, TERM_ID, ERR_GROUP_CD)
);

-- ---------------------------------------------------------------------
-- 3. T_TRANSACTION_DAILY_TOTAL : 일별 거래실적 집계 (batday02)
--    AS-IS t_transactiondailytotal 대응(재설계).
--    - AS-IS 의 24시간 pivot 컬럼(tr_cnt_00_07 … tr_amt_23_24)은
--      STAT_HOUR 행 구조로 정규화 (동일 정보량, 시간대 = STAT_HOUR)
--    - AS-IS tr_type_cd(거래종류)는 T_TRANSACTION_CODE 미이관으로 제외,
--      원거래코드(TR_CD) 차원으로 집계 (필요 시 코드매핑으로 상향집계 가능)
--    - 성공 거래(TR_RESULT='0000')만 집계 (AS-IS tr_result='OK' 동일 의미)
--    ※ 기존 (구)T_TRANSACTION_DAILY_TOTAL 이 이미 생성되어 있으면
--      DROP TABLE T_TRANSACTION_DAILY_TOTAL; 후 재생성한다.
-- ---------------------------------------------------------------------
CREATE TABLE T_TRANSACTION_DAILY_TOTAL (
    STAT_DATE      CHAR(8)      NOT NULL,          -- 거래일자(yyyyMMdd) = AS-IS TR_DATE
    TERM_GROUP_ID  CHAR(5)      NOT NULL,          -- 영업점코드
    BRANCH_CD      VARCHAR2(5),                    -- 지점코드(T_ATM.BRANCH_CD)
    TERM_ID        VARCHAR2(20) NOT NULL,          -- 단말번호
    STAT_HOUR      CHAR(2)      NOT NULL,          -- 시간대(00~23)
    TR_CD          VARCHAR2(6)  NOT NULL,          -- 거래코드(T_TRANSACTION_JOURNAL.TR_TYPE)
    TR_BANK_TYPE   CHAR(1)      NOT NULL,          -- 자행/타행(1:자행 2:타행)
    CLOSING_TYPE   CHAR(1)      DEFAULT ' ',       -- 마감구분(TOTAL_TYPE_CD)
    DAY_WEEK       CHAR(3),                        -- 요일(MON~SUN)
    TR_CNT         NUMBER(10)   DEFAULT 0,         -- 성공 거래건수
    TR_AMT         NUMBER(15)   DEFAULT 0,         -- 거래금액(방출금액 합) = AS-IS tr_amount(dispense_amount)
    SURCHARGE_AMT  NUMBER(15)   DEFAULT 0,         -- 수수료 합
    REG_DT         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_T_TRANSACTION_DAILY_TOTAL
        PRIMARY KEY (STAT_DATE, TERM_GROUP_ID, TERM_ID, STAT_HOUR, TR_CD, TR_BANK_TYPE, CLOSING_TYPE)
);

-- ---------------------------------------------------------------------
-- 4. T_TRANSACTION_CODE : 거래코드 매핑 (batday08 이 사용)
--    AS-IS t_transactioncode 대응 — TO-BE 스키마에 부재하여 신규 정의.
--    ※ 데이터는 AS-IS t_transactioncode 를 이관하거나 NH 코드 기준으로
--      별도 시드해야 한다 (미시드 시 batday08 의 거래집계 항목은 0 건).
-- ---------------------------------------------------------------------
CREATE TABLE T_TRANSACTION_CODE (
    TR_CD       VARCHAR2(6)   NOT NULL,            -- 거래코드(전문상 TR_TYPE)
    TR_TYPE_CD  CHAR(2)       NOT NULL,            -- 거래종류코드(01:출금 02:… 99:기타)
    TR_NAME     VARCHAR2(100),                     -- 거래명
    USE_YN      CHAR(1)       DEFAULT 'Y',
    CONSTRAINT PK_T_TRANSACTION_CODE PRIMARY KEY (TR_CD)
);
