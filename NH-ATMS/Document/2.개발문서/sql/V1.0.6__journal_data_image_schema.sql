-- =====================================================================
-- V1.0.6 : 저널 본문/이미지 메타 스키마 (Oracle 19c)
--   - T_JOURNAL_DATA  : ATM 원본 출력문 (저널 본문 텍스트, CLOB)
--   - T_JOURNAL_IMAGE : ATM 카메라/명세표/통장 이미지 경로 메타
--   참조: Document/09_ATMR_*.md §6.4 이미지 다운로드 API
--         원본 소스: 새마을금고 datm.t_journaldata / datm.t_journalimage
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_JOURNAL_DATA : 저널 원본 출력문
--    - T_JOURNAL 1:1 (동일 4PK)
--    - 거래/운영/장애 모든 jnl_kind 공통, 가용하면 적재
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL_DATA (
    TERM_GROUP_ID    CHAR(5)       NOT NULL,
    TERM_ID          VARCHAR2(20)  NOT NULL,
    JNL_DATE         CHAR(8)       NOT NULL,
    JNL_TIME         CHAR(10)      NOT NULL,
    JNL_DATA         CLOB,
    CONSTRAINT PK_T_JOURNAL_DATA PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME)
);
CREATE INDEX IX_T_JOURNAL_DATA_DATE ON T_JOURNAL_DATA (JNL_DATE, JNL_TIME);

COMMENT ON TABLE  T_JOURNAL_DATA                IS '저널 원본 출력문 (ATM Printer 텍스트, CLOB)';
COMMENT ON COLUMN T_JOURNAL_DATA.TERM_GROUP_ID  IS '영업점코드';
COMMENT ON COLUMN T_JOURNAL_DATA.TERM_ID        IS '단말번호';
COMMENT ON COLUMN T_JOURNAL_DATA.JNL_DATE       IS '저널발생일 YYYYMMDD';
COMMENT ON COLUMN T_JOURNAL_DATA.JNL_TIME       IS '저널발생시간 HHMMSSFFF+flag';
COMMENT ON COLUMN T_JOURNAL_DATA.JNL_DATA       IS '원본 출력문 (POWER OFF/ON, 거래명세, 차단사유 등)';


-- ---------------------------------------------------------------------
-- 2) T_JOURNAL_IMAGE : 저널 첨부 이미지 메타
--    - 거래(71) 1건당 IMAGE_TYPE 별 N행 (1=카드, 2=통장, 3=명세표 등)
--    - 실파일은 ATMR 측 NAS(예: /atms/share/ejnl/img/) 경로 그대로 보관
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL_IMAGE (
    TERM_GROUP_ID    CHAR(5)        NOT NULL,
    TERM_ID          VARCHAR2(20)   NOT NULL,
    JNL_DATE         CHAR(8)        NOT NULL,
    JNL_TIME         CHAR(10)       NOT NULL,
    IMAGE_TYPE       CHAR(1)        NOT NULL,
    IMAGE_PATH       VARCHAR2(255)  NOT NULL,
    CONSTRAINT PK_T_JOURNAL_IMAGE PRIMARY KEY (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE)
);
CREATE INDEX IX_T_JOURNAL_IMAGE_DATE ON T_JOURNAL_IMAGE (JNL_DATE, JNL_TIME);

COMMENT ON TABLE  T_JOURNAL_IMAGE                IS '저널 첨부 이미지 메타 (실파일은 NAS)';
COMMENT ON COLUMN T_JOURNAL_IMAGE.TERM_GROUP_ID  IS '영업점코드';
COMMENT ON COLUMN T_JOURNAL_IMAGE.TERM_ID        IS '단말번호';
COMMENT ON COLUMN T_JOURNAL_IMAGE.JNL_DATE       IS '저널발생일';
COMMENT ON COLUMN T_JOURNAL_IMAGE.JNL_TIME       IS '저널발생시간';
COMMENT ON COLUMN T_JOURNAL_IMAGE.IMAGE_TYPE     IS '이미지 구분 1=카드/2=통장/3=명세표 (추정)';
COMMENT ON COLUMN T_JOURNAL_IMAGE.IMAGE_PATH     IS '이미지 파일 경로 (NAS 절대경로)';

COMMIT;
