-- =====================================================================
-- V1.2.8  AP 자동배포 스키마 (에이전트 open 응답 files[] 소스)
--   T_AP_LAST_VERSION : 일괄다운로드 마스터 (기종별 승인/최종버전)
--   T_DOWNLOAD_SERVER : 일괄다운로드 서버측 배포본 이력
--
-- AS-IS(새마을금고 atmr) T_APLASTVERSION / T_DOWNLOADSERVER 이식.
--   - BANK_CD 컬럼 제거: NH 단일기관 구성 가정 (AS-IS 조회 SQL 도 BANK_CD 미사용)
--   - 테이블/컬럼명은 TO-BE 표준(언더스코어 분리) 적용
-- 참조: atms-journal ApVersionMapper.selectApLastVersion
-- 적용 후 Document/2.개발문서/sql 로 취합 필요 (버전 넘버 충돌 주의)
-- =====================================================================

CREATE TABLE T_AP_LAST_VERSION (
    ATM_CO_CD        CHAR(2)       NOT NULL,      -- 기기업체코드
    MODEL_CD         CHAR(3)       NOT NULL,      -- 모델코드
    LAST_DOWN_VER    CHAR(10)      NOT NULL,      -- 최종다운로드버전
    AP_VER           VARCHAR2(32),                -- 기기AP버전
    FILE_NAME        VARCHAR2(255),               -- 파일명
    FILE_SIZE        NUMBER(19),                  -- 파일크기
    FILE_PATH        VARCHAR2(256),               -- 기기측 다운로드경로
    APPLY_FLAG       CHAR(1)       DEFAULT '0',   -- 적용여부
    COMPRESS_TYPE_CD CHAR(1)       DEFAULT '0',   -- 압축유무
    APPLY_DATE       CHAR(8),                     -- 적용일
    APPLY_TIME       CHAR(6),                     -- 적용시간
    CRC_VALUE        VARCHAR2(20),                -- CRC값
    CONFIRM_STEP     CHAR(1),                     -- 승인단계('1'=승인)
    CONFIRM_DATE     CHAR(8),                     -- 승인일
    CONFIRM_TIME     CHAR(6),                     -- 승인시간
    USER_ID          VARCHAR2(10),                -- 등록자
    CONSTRAINT PK_T_AP_LAST_VERSION PRIMARY KEY (ATM_CO_CD, MODEL_CD)
);

COMMENT ON TABLE  T_AP_LAST_VERSION               IS 'T_일괄다운로드마스터';
COMMENT ON COLUMN T_AP_LAST_VERSION.ATM_CO_CD     IS '기기업체코드';
COMMENT ON COLUMN T_AP_LAST_VERSION.MODEL_CD      IS '모델코드';
COMMENT ON COLUMN T_AP_LAST_VERSION.LAST_DOWN_VER IS '최종다운로드버전';
COMMENT ON COLUMN T_AP_LAST_VERSION.AP_VER        IS '기기AP버전';
COMMENT ON COLUMN T_AP_LAST_VERSION.FILE_NAME     IS '파일명';
COMMENT ON COLUMN T_AP_LAST_VERSION.FILE_SIZE     IS '파일크기';
COMMENT ON COLUMN T_AP_LAST_VERSION.FILE_PATH     IS '기기측 다운로드경로';
COMMENT ON COLUMN T_AP_LAST_VERSION.CONFIRM_STEP  IS '승인단계(1=승인)';

CREATE TABLE T_DOWNLOAD_SERVER (
    ATM_CO_CD     CHAR(2)       NOT NULL,   -- 기기업체코드
    MODEL_CD      CHAR(3)       NOT NULL,   -- 모델코드
    LAST_DOWN_VER CHAR(10)      NOT NULL,   -- 최종다운로드버전
    SERVER_NAME   VARCHAR2(20)  NOT NULL,   -- 서버명
    FILE_NAME     VARCHAR2(255),            -- 파일명
    FILE_SIZE     NUMBER(19),               -- 파일크기
    FILE_PATH     VARCHAR2(256),            -- 서버측 파일경로
    DOWN_PATH     VARCHAR2(256),            -- 일괄다운로드경로
    APPLY_DATE    CHAR(8),                  -- 적용일
    APPLY_TIME    CHAR(6),                  -- 적용시간
    CRC_VALUE     VARCHAR2(20),             -- CRC값
    CONSTRAINT PK_T_DOWNLOAD_SERVER PRIMARY KEY (ATM_CO_CD, MODEL_CD, LAST_DOWN_VER, SERVER_NAME)
);

COMMENT ON TABLE  T_DOWNLOAD_SERVER               IS 'T_일괄다운로드모델이력';
COMMENT ON COLUMN T_DOWNLOAD_SERVER.ATM_CO_CD     IS '기기업체코드';
COMMENT ON COLUMN T_DOWNLOAD_SERVER.MODEL_CD      IS '모델코드';
COMMENT ON COLUMN T_DOWNLOAD_SERVER.LAST_DOWN_VER IS '최종다운로드버전';
COMMENT ON COLUMN T_DOWNLOAD_SERVER.SERVER_NAME   IS '서버명';
COMMENT ON COLUMN T_DOWNLOAD_SERVER.FILE_NAME     IS '파일명';
COMMENT ON COLUMN T_DOWNLOAD_SERVER.FILE_SIZE     IS '파일크기';
COMMENT ON COLUMN T_DOWNLOAD_SERVER.FILE_PATH     IS '서버측 파일경로';
COMMENT ON COLUMN T_DOWNLOAD_SERVER.DOWN_PATH     IS '일괄다운로드경로';

COMMIT;
