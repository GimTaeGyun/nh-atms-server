-- =====================================================================
-- V1.3.8 : 다운로드관리 스키마 (개별/일괄 다운로드 통합 리팩터링)
--
--   배경 : (구)새마을금고 ATMS 는 파일 등록 팝업이 3종(AP일반/일반/AP)으로
--          갈라져 운영자가 어떤 화면에서 무엇을 등록하는지 매우 혼란스러웠다.
--          본 설계는 "무엇을 보낼지(패키지)" 와 "누구에게 어떻게 보낼지(작업)"
--          를 단 두 개의 테이블로 분리하여 개념을 명확화한다.
--
--     T_DOWNLOAD_PACKAGE : 다운로드 패키지 마스터 = "무엇을 보낼지"
--        - 기종(업체+모델)별 1개의 파일 묶음. 유형은 PKG_TYPE 단일 컬럼
--          (A=AP / G=일반 / M=매체)으로 통합 → 팝업 3종을 1종으로 단순화.
--        - 결재(CONFIRM_STATUS) 후에만 일괄배포 가능.
--     T_DOWNLOAD_JOB     : 다운로드 작업/배포 이력 = "누구에게 어떻게 보냈는지"
--        - 패키지를 특정 기기에 배포한 단위. EXEC_TYPE(I=개별 / B=일괄).
--        - 실제 전송은 T_AGENT_COMMAND(CMD_TYPE=DLD/GDL) 큐로 발행하고
--          그 CMD_ID 를 연계 보관한다(V1.3.4 명령 큐 재사용).
--
--   명명 : UPPER_SNAKE_CASE. 선행: V1.3.4(T_AGENT_COMMAND). 신규 CREATE 만 수행.
-- =====================================================================

-- ── 다운로드 패키지 마스터 ───────────────────────────────────────────
CREATE TABLE T_DOWNLOAD_PACKAGE (
    PKG_ID          NUMBER(19)    NOT NULL,            -- 패키지 ID(PK, SEQ 채번)
    PKG_NAME        VARCHAR2(100),                     -- 패키지 명(운영자 식별용)
    PKG_TYPE        CHAR(1)       DEFAULT 'A' NOT NULL,-- 유형(A=AP, G=일반, M=매체)
    ATM_CO_CD       VARCHAR2(2)   NOT NULL,            -- 기기업체코드
    MODEL_CD        VARCHAR2(3)   NOT NULL,            -- 모델코드
    DOWN_VER        VARCHAR2(10)  NOT NULL,            -- 다운로드버전(업체2+모델3+일련5)
    FILE_NAME       VARCHAR2(200) NOT NULL,            -- 파일명
    FILE_SIZE       NUMBER(19),                        -- 파일크기(byte)
    FILE_PATH       VARCHAR2(300),                     -- 서버 보관 경로
    DOWNLOAD_PATH   VARCHAR2(300) DEFAULT 'D:\LGDOWN' NOT NULL, -- 기기내 다운로드 경로
    EXEC_CMD        VARCHAR2(500),                     -- 적용 실행 CMD(선택)
    APPLY_DATE      CHAR(8),                           -- 적용 예약일(YYYYMMDD)
    APPLY_TIME      CHAR(4),                           -- 적용 예약시각(HH24MI)
    CRC_VALUE       VARCHAR2(20),                      -- 파일 CRC(무결성)
    CONFIRM_STATUS  CHAR(1)       DEFAULT 'N' NOT NULL,-- 결재(N=미결재,Y=결재완료,R=반려)
    CONFIRM_USER_ID VARCHAR2(30),                      -- 결재자 ID
    CONFIRM_DT      TIMESTAMP,                         -- 결재시각
    USE_YN          CHAR(1)       DEFAULT 'Y' NOT NULL,-- 사용여부
    REG_USER_ID     VARCHAR2(30),                      -- 등록자 ID
    REG_DT          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 등록시각
    UPD_DT          TIMESTAMP,                         -- 수정시각
    CONSTRAINT PK_T_DOWNLOAD_PACKAGE PRIMARY KEY (PKG_ID)
);

CREATE SEQUENCE SEQ_T_DOWNLOAD_PACKAGE START WITH 1 INCREMENT BY 1 NOCACHE;

-- 기종(업체+모델)별 패키지 조회용
CREATE INDEX IX_T_DOWNLOAD_PACKAGE_MODEL ON T_DOWNLOAD_PACKAGE (ATM_CO_CD, MODEL_CD, USE_YN);
-- 결재 대기/완료 필터용
CREATE INDEX IX_T_DOWNLOAD_PACKAGE_CONFIRM ON T_DOWNLOAD_PACKAGE (CONFIRM_STATUS, REG_DT);

COMMENT ON TABLE  T_DOWNLOAD_PACKAGE                IS '다운로드 패키지 마스터(무엇을 보낼지)';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.PKG_ID         IS '패키지 ID(PK)';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.PKG_NAME       IS '패키지 명';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.PKG_TYPE       IS '유형(A=AP,G=일반,M=매체)';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.ATM_CO_CD      IS '기기업체코드';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.MODEL_CD       IS '모델코드';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.DOWN_VER       IS '다운로드버전(업체2+모델3+일련5)';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.FILE_NAME      IS '파일명';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.FILE_SIZE      IS '파일크기(byte)';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.FILE_PATH      IS '서버 보관 경로';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.DOWNLOAD_PATH  IS '기기내 다운로드 경로';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.EXEC_CMD       IS '적용 실행 CMD';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.APPLY_DATE     IS '적용 예약일(YYYYMMDD)';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.APPLY_TIME     IS '적용 예약시각(HH24MI)';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.CRC_VALUE      IS '파일 CRC';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.CONFIRM_STATUS IS '결재(N=미결재,Y=결재완료,R=반려)';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.CONFIRM_USER_ID IS '결재자 ID';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.CONFIRM_DT     IS '결재시각';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.USE_YN         IS '사용여부';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.REG_USER_ID    IS '등록자 ID';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.REG_DT         IS '등록시각';
COMMENT ON COLUMN T_DOWNLOAD_PACKAGE.UPD_DT         IS '수정시각';

-- ── 다운로드 작업/배포 이력 ──────────────────────────────────────────
CREATE TABLE T_DOWNLOAD_JOB (
    JOB_ID          NUMBER(19)    NOT NULL,            -- 작업 ID(PK, SEQ 채번)
    PKG_ID          NUMBER(19)    NOT NULL,            -- 패키지 ID(FK)
    TERM_GROUP_ID   CHAR(5)       NOT NULL,            -- 영업점코드(대상기기)
    TERM_ID         VARCHAR2(20)  NOT NULL,            -- 단말번호(대상기기)
    EXEC_TYPE       CHAR(1)       NOT NULL,            -- 배포유형(I=개별, B=일괄)
    STATUS          CHAR(1)       DEFAULT 'W' NOT NULL,-- W=대기,S=전송중,C=완료,F=실패,X=취소
    CMD_ID          VARCHAR2(30),                      -- 연계 발행명령 ID(T_AGENT_COMMAND)
    RESERVE_DATE    CHAR(8),                           -- 예약일(YYYYMMDD)
    RESERVE_TIME    CHAR(4),                           -- 예약시각(HH24MI)
    RESULT_CD       VARCHAR2(10),                      -- 결과코드
    RESULT_MSG      VARCHAR2(500),                     -- 결과 메시지
    RESULT_DT       TIMESTAMP,                         -- 결과시각
    REG_USER_ID     VARCHAR2(30),                      -- 발행자 ID
    REG_DT          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 발행시각
    CONSTRAINT PK_T_DOWNLOAD_JOB PRIMARY KEY (JOB_ID)
);

CREATE SEQUENCE SEQ_T_DOWNLOAD_JOB START WITH 1 INCREMENT BY 1 NOCACHE;

-- 패키지별 배포이력 조회용
CREATE INDEX IX_T_DOWNLOAD_JOB_PKG ON T_DOWNLOAD_JOB (PKG_ID, REG_DT);
-- 기기별 배포이력 조회용
CREATE INDEX IX_T_DOWNLOAD_JOB_TERM ON T_DOWNLOAD_JOB (TERM_GROUP_ID, TERM_ID, REG_DT);

COMMENT ON TABLE  T_DOWNLOAD_JOB               IS '다운로드 작업/배포 이력(누구에게 어떻게 보냈는지)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.JOB_ID        IS '작업 ID(PK)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.PKG_ID        IS '패키지 ID(FK)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.TERM_GROUP_ID IS '영업점코드(대상기기)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.TERM_ID       IS '단말번호(대상기기)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.EXEC_TYPE     IS '배포유형(I=개별,B=일괄)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.STATUS        IS '상태(W=대기,S=전송중,C=완료,F=실패,X=취소)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.CMD_ID        IS '연계 발행명령 ID(T_AGENT_COMMAND)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.RESERVE_DATE  IS '예약일(YYYYMMDD)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.RESERVE_TIME  IS '예약시각(HH24MI)';
COMMENT ON COLUMN T_DOWNLOAD_JOB.RESULT_CD     IS '결과코드';
COMMENT ON COLUMN T_DOWNLOAD_JOB.RESULT_MSG    IS '결과 메시지';
COMMENT ON COLUMN T_DOWNLOAD_JOB.RESULT_DT     IS '결과시각';
COMMENT ON COLUMN T_DOWNLOAD_JOB.REG_USER_ID   IS '발행자 ID';
COMMENT ON COLUMN T_DOWNLOAD_JOB.REG_DT        IS '발행시각';

COMMIT;
