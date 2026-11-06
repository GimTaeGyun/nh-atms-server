-- =====================================================================
-- 개별다운로드(자체 파일 업로드) — 구 새마을금고 indDownManage 정합
-- ---------------------------------------------------------------------
-- 운영자가 개별다운로드관리 화면에서 AP/일반 ZIP 파일을 "직접 업로드"하고,
-- 선택한 기기들에 즉시 배포한다(결재 무관). 일괄다운로드(T_DOWNLOAD_PACKAGE,
-- 결재 필수)와 완전히 분리된 전용 테이블을 사용한다.
--   - T_ATM_DOWNLOAD       : 업로드 파일 마스터        (MG T_ATMDOWNLOAD)
--   - T_ATM_APPLY_HISTORY  : 기기별 개별다운로드 적용이력 (MG T_ATMAPPLYHISTORY)
-- MySQL → Oracle 변환(CHAR/VARCHAR2/NUMBER/SEQUENCE).
-- =====================================================================

-- 파일 마스터 채번용 시퀀스 (DOWN_NO = LPAD(NEXTVAL, 7, '0'))
CREATE SEQUENCE SEQ_T_ATM_DOWNLOAD START WITH 1 INCREMENT BY 1 NOCACHE;

-- ---------------------------------------------------------------------
-- T_ATM_DOWNLOAD : 개별다운로드 업로드 파일 마스터
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_DOWNLOAD (
    DOWN_NO          VARCHAR2(7)    NOT NULL,   -- 다운로드일련번호(PK)
    APP_TYPE_CD      CHAR(1),                   -- 패키지유형 A=AP / G=일반 / M=매체
    ATM_CO_CD        VARCHAR2(8),               -- 기기업체코드
    MODEL_CD         VARCHAR2(8),               -- 모델코드
    APP_FILE_NAME    VARCHAR2(128),             -- 업로드 원본 파일명
    APP_FILE_SIZE    NUMBER(19),                -- 파일사이즈(byte)
    APP_VER          VARCHAR2(32),              -- 파일(다운로드) 버전
    FILE_COMPRESS    CHAR(1)        DEFAULT '0',-- 압축여부(0:비압축)
    EXE_TYPE_CD      CHAR(1)        DEFAULT '0',-- 실행구분 0=개별 / 1=일반(dl_kind)
    EXE_FILE_PATH    VARCHAR2(512),             -- 서버 보관 절대경로
    DOWNLOAD_PATH    VARCHAR2(255),             -- 기기내 다운로드 경로(cp_path)
    SVR_UPLOAD_DATE  CHAR(8),                   -- 서버 업로드일자(YYYYMMDD)
    SVR_UPLOAD_TIME  CHAR(6),                   -- 서버 업로드시간(HH24MISS)
    APPLY_CAUSE      CHAR(1),                   -- 적용원인
    APPLY_MEMO       VARCHAR2(255),             -- 적용내역(메모)
    CRC_VALUE        VARCHAR2(20),              -- CRC값(무결성)
    ENTER_DATE       CHAR(8),                   -- 등록일자(YYYYMMDD)
    ENTER_TIME       CHAR(6),                   -- 등록시간(HH24MISS)
    ENTER_USER_ID    VARCHAR2(20),              -- 등록자ID
    CONSTRAINT PK_T_ATM_DOWNLOAD PRIMARY KEY (DOWN_NO)
);

COMMENT ON TABLE  T_ATM_DOWNLOAD                 IS '개별다운로드 업로드 파일 마스터(MG T_ATMDOWNLOAD)';
COMMENT ON COLUMN T_ATM_DOWNLOAD.DOWN_NO         IS '다운로드일련번호(PK, 7자리 0패딩)';
COMMENT ON COLUMN T_ATM_DOWNLOAD.APP_TYPE_CD     IS '패키지유형 A=AP/G=일반/M=매체';
COMMENT ON COLUMN T_ATM_DOWNLOAD.ATM_CO_CD       IS '기기업체코드';
COMMENT ON COLUMN T_ATM_DOWNLOAD.MODEL_CD        IS '모델코드';
COMMENT ON COLUMN T_ATM_DOWNLOAD.APP_FILE_NAME   IS '업로드 원본 파일명';
COMMENT ON COLUMN T_ATM_DOWNLOAD.APP_FILE_SIZE   IS '파일사이즈(byte)';
COMMENT ON COLUMN T_ATM_DOWNLOAD.APP_VER         IS '파일(다운로드) 버전';
COMMENT ON COLUMN T_ATM_DOWNLOAD.FILE_COMPRESS   IS '압축여부(0:비압축)';
COMMENT ON COLUMN T_ATM_DOWNLOAD.EXE_TYPE_CD     IS '실행구분 0=개별/1=일반';
COMMENT ON COLUMN T_ATM_DOWNLOAD.EXE_FILE_PATH   IS '서버 보관 절대경로';
COMMENT ON COLUMN T_ATM_DOWNLOAD.DOWNLOAD_PATH   IS '기기내 다운로드 경로';
COMMENT ON COLUMN T_ATM_DOWNLOAD.CRC_VALUE       IS 'CRC값(무결성)';

-- ---------------------------------------------------------------------
-- T_ATM_APPLY_HISTORY : 기기별 개별다운로드 적용이력
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_APPLY_HISTORY (
    DOWN_NO          VARCHAR2(7)    NOT NULL,   -- 다운로드일련번호(FK)
    TERM_GROUP_ID    VARCHAR2(20)   NOT NULL,   -- 영업점(금고)코드
    TERM_ID          VARCHAR2(20)   NOT NULL,   -- 단말번호(기번)
    CMD_ID           VARCHAR2(30),              -- 원격명령 ID(T_AGENT_COMMAND 연계)
    START_DATE       CHAR(8),                   -- 시작일자(YYYYMMDD)
    START_TIME       CHAR(6),                   -- 시작시간(HH24MISS)
    END_DATE         CHAR(8),                   -- 종료일자
    END_TIME         CHAR(6),                   -- 종료시간
    APPLY_FLAG       CHAR(1),                   -- 적용여부 0=미적용/1=적용완료/2=다운로드중
    APPLY_RESULT     CHAR(2),                   -- 적용결과 0=실패/1=성공
    ASYNC_UID        NUMBER(19),                -- 비동기 상관 ID(T_MSG_ASYNC)
    REG_USER_ID      VARCHAR2(20),              -- 등록자ID
    REG_DT           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_T_ATM_APPLY_HISTORY PRIMARY KEY (DOWN_NO, TERM_GROUP_ID, TERM_ID)
);

CREATE INDEX IX_T_ATM_APPLY_HISTORY_01 ON T_ATM_APPLY_HISTORY (ASYNC_UID);

COMMENT ON TABLE  T_ATM_APPLY_HISTORY               IS '기기별 개별다운로드 적용이력(MG T_ATMAPPLYHISTORY)';
COMMENT ON COLUMN T_ATM_APPLY_HISTORY.DOWN_NO       IS '다운로드일련번호(FK)';
COMMENT ON COLUMN T_ATM_APPLY_HISTORY.TERM_GROUP_ID IS '영업점(금고)코드';
COMMENT ON COLUMN T_ATM_APPLY_HISTORY.TERM_ID       IS '단말번호(기번)';
COMMENT ON COLUMN T_ATM_APPLY_HISTORY.CMD_ID        IS '원격명령 ID(T_AGENT_COMMAND)';
COMMENT ON COLUMN T_ATM_APPLY_HISTORY.APPLY_FLAG    IS '적용여부 0=미적용/1=적용완료/2=다운로드중';
COMMENT ON COLUMN T_ATM_APPLY_HISTORY.APPLY_RESULT  IS '적용결과 0=실패/1=성공';
COMMENT ON COLUMN T_ATM_APPLY_HISTORY.ASYNC_UID     IS '비동기 상관 ID(T_MSG_ASYNC)';
