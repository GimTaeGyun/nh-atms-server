-- =====================================================================
-- V1.3.3 : 역방향 갭 보정 — 매퍼는 사용하나 DDL CREATE가 없던 2종
--   명명 표준: 16_통합스키마설계.md §0.6 (UPPER_SNAKE_CASE, 언더스코어 표기)
--   배경: 백엔드 매퍼(AttachedFileMapper / ServiceProviderMapper)는
--         T_ATTACHED_FILE / T_SERVICE_PROVIDER 를 참조하지만, V1.x 마이그레이션에
--         CREATE문이 없어 새 환경에서 재현 불가 → AS-IS(새마을금고 datm.*) 정의를
--         Oracle 19c + 언더스코어 표준으로 역추출하여 정식화한다.
--   원본: 새마을금고.sql datm.t_attachedfile / datm.t_serviceprovider
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_ATTACHED_FILE : 공지 등 첨부파일 메타
-- ---------------------------------------------------------------------
CREATE TABLE T_ATTACHED_FILE (
    CAT_CD              CHAR(5)        NOT NULL,
    SEQ_NO              NUMBER(19)     NOT NULL,
    FILE_NO             NUMBER(10)     NOT NULL,
    ATTACHED_FILE_NAME  VARCHAR2(255),
    ATTACHED_FILE_SIZE  NUMBER(19),
    CONSTRAINT PK_T_ATTACHED_FILE PRIMARY KEY (CAT_CD, SEQ_NO, FILE_NO)
);

COMMENT ON TABLE  T_ATTACHED_FILE                     IS 'T_첨부파일정보';
COMMENT ON COLUMN T_ATTACHED_FILE.CAT_CD              IS '집계구분코드';
COMMENT ON COLUMN T_ATTACHED_FILE.SEQ_NO              IS '일련번호';
COMMENT ON COLUMN T_ATTACHED_FILE.FILE_NO             IS '파일번호';
COMMENT ON COLUMN T_ATTACHED_FILE.ATTACHED_FILE_NAME  IS '첨부파일명';
COMMENT ON COLUMN T_ATTACHED_FILE.ATTACHED_FILE_SIZE  IS '첨부파일사이즈';


-- ---------------------------------------------------------------------
-- 2) T_SERVICE_PROVIDER : 업체(서비스 제공자) 마스터
--    - AS-IS 원본 PK는 (PROVIDER_CD, PROVIDER_NAME) 복합키였으나,
--      백엔드 매퍼가 PROVIDER_CD 단독으로 단건 조회/수정/삭제하므로
--      PROVIDER_CD 단독 PK로 정규화한다. PROVIDER_NAME은 NOT NULL 유지.
-- ---------------------------------------------------------------------
CREATE TABLE T_SERVICE_PROVIDER (
    PROVIDER_CD       CHAR(2)        NOT NULL,
    PROVIDER_NAME     VARCHAR2(40)   NOT NULL,
    PROVIDER_TYPE_CD  CHAR(1),
    PERSON            VARCHAR2(40),
    PHONE_NO          VARCHAR2(50),
    FAX_NO            VARCHAR2(30),
    EMAIL             VARCHAR2(50),
    ZIP_CD            CHAR(6),
    ADDRESS           VARCHAR2(255),
    SEND_TYPE         CHAR(1),
    FAULT_SEND_TYPE   CHAR(1),
    USE_YN            CHAR(1),
    WOORI_FLAG        CHAR(1),
    KNBANK_FLAG       CHAR(1),
    CONSTRAINT PK_T_SERVICE_PROVIDER PRIMARY KEY (PROVIDER_CD)
);

COMMENT ON TABLE  T_SERVICE_PROVIDER                   IS 'T_업체마스터';
COMMENT ON COLUMN T_SERVICE_PROVIDER.PROVIDER_CD       IS '업체코드';
COMMENT ON COLUMN T_SERVICE_PROVIDER.PROVIDER_NAME     IS '업체명';
COMMENT ON COLUMN T_SERVICE_PROVIDER.PROVIDER_TYPE_CD  IS '업체구분코드';
COMMENT ON COLUMN T_SERVICE_PROVIDER.PERSON            IS '담당자명';
COMMENT ON COLUMN T_SERVICE_PROVIDER.PHONE_NO          IS '전화번호';
COMMENT ON COLUMN T_SERVICE_PROVIDER.FAX_NO            IS '팩스번호';
COMMENT ON COLUMN T_SERVICE_PROVIDER.EMAIL             IS '이메일';
COMMENT ON COLUMN T_SERVICE_PROVIDER.ZIP_CD            IS '우편번호';
COMMENT ON COLUMN T_SERVICE_PROVIDER.ADDRESS           IS '주소';
COMMENT ON COLUMN T_SERVICE_PROVIDER.SEND_TYPE         IS '전송방식구분';
COMMENT ON COLUMN T_SERVICE_PROVIDER.FAULT_SEND_TYPE   IS '장애전송구분';
COMMENT ON COLUMN T_SERVICE_PROVIDER.USE_YN            IS '사용여부 (1=사용/0=미사용)';
COMMENT ON COLUMN T_SERVICE_PROVIDER.WOORI_FLAG        IS '우리은행사용여부 (1=예/0=아니오)';
COMMENT ON COLUMN T_SERVICE_PROVIDER.KNBANK_FLAG       IS '경남은행사용여부 (1=예/0=아니오)';

COMMIT;
