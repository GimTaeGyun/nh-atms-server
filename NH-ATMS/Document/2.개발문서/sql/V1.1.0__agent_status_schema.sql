-- =====================================================================
-- V1.1.0 : T_AGENT_STATUS (ATM 에이전트 상태 마스터)
-- 레거시 atmr T_AGENT_STATUS 를 신규 핵심 컬럼만으로 단순화.
-- PK = ATM_IP (기기 IP).  open/exit/ping 시 upsert/update.
-- =====================================================================

CREATE TABLE T_AGENT_STATUS (
    ATM_IP           CHAR(15)      NOT NULL,   -- 기기 IP (PK)
    AGENT_TYPE       CHAR(1),                  -- 에이전트 구분 (1=ATM)
    TERM_GROUP_ID    CHAR(5),                  -- 영업점코드
    TERM_ID          VARCHAR2(20),             -- 단말번호
    CONNECT_FLAG     CHAR(1)       DEFAULT '0',-- 접속여부 (1=접속, 0=해제)
    AGENT_VER        CHAR(10),                 -- 에이전트(AG) 버전
    AP_DOWN_VER      CHAR(10),                 -- AP 버전
    OS_NAME          VARCHAR2(100),            -- OS 정보
    ATM_MAC_ADDR     VARCHAR2(20),             -- 기기 MAC 주소
    CONNECT_IP       CHAR(15),                 -- 접속 IP (host)
    CONNECT_PORT     CHAR(5),                  -- 접속 PORT
    JNL_MODE         CHAR(1),                  -- 저널 모드
    CARD_IMAGE       CHAR(1)       DEFAULT '0',-- 카드 이미지 전송여부
    DISPENSER_IMAGE  CHAR(1)       DEFAULT '0',-- 방출부 이미지 전송여부
    CUSTOMER_IMAGE   CHAR(1)       DEFAULT '0',-- 안면부 이미지 전송여부
    POLL_CYCLE       CHAR(3)       DEFAULT '30',-- 폴링 주기(초)
    START_DATE       CHAR(8),                  -- 기동 일자 (yyyyMMdd)
    START_TIME       CHAR(6),                  -- 기동 시각 (HHmmss)
    LAST_POLL_DATE   CHAR(8),                  -- 최종 폴링 일자
    LAST_POLL_TIME   CHAR(6),                  -- 최종 폴링 시각
    EXIT_DATE        CHAR(8),                  -- 종료 일자
    EXIT_TIME        CHAR(6),                  -- 종료 시각
    REG_DT           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    UPD_DT           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_T_AGENT_STATUS PRIMARY KEY (ATM_IP)
);

CREATE INDEX IX_T_AGENT_STATUS_TERM ON T_AGENT_STATUS (TERM_GROUP_ID, TERM_ID);

COMMENT ON TABLE  T_AGENT_STATUS                  IS 'ATM 에이전트 상태 마스터';
COMMENT ON COLUMN T_AGENT_STATUS.ATM_IP          IS '기기 IP (PK)';
COMMENT ON COLUMN T_AGENT_STATUS.AGENT_TYPE      IS '에이전트 구분';
COMMENT ON COLUMN T_AGENT_STATUS.TERM_GROUP_ID   IS '영업점코드';
COMMENT ON COLUMN T_AGENT_STATUS.TERM_ID         IS '단말번호';
COMMENT ON COLUMN T_AGENT_STATUS.CONNECT_FLAG    IS '접속여부(1=접속,0=해제)';
COMMENT ON COLUMN T_AGENT_STATUS.AGENT_VER       IS '에이전트 버전';
COMMENT ON COLUMN T_AGENT_STATUS.AP_DOWN_VER     IS 'AP 버전';
COMMENT ON COLUMN T_AGENT_STATUS.OS_NAME         IS 'OS 정보';
COMMENT ON COLUMN T_AGENT_STATUS.POLL_CYCLE      IS '폴링 주기(초)';
COMMENT ON COLUMN T_AGENT_STATUS.START_DATE      IS '기동 일자';
COMMENT ON COLUMN T_AGENT_STATUS.LAST_POLL_DATE  IS '최종 폴링 일자';
COMMENT ON COLUMN T_AGENT_STATUS.EXIT_DATE       IS '종료 일자';

COMMIT;
