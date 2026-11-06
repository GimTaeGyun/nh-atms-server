-- =====================================================================
-- V1.1.8 : T_ATM_UNIT_STATUS (기기 장치(디바이스) 상태/버전 현황)
-- atmr 1-B. 상태/장애 저널(21/22) 수신 시 디바이스 단위 상태를 upsert.
-- 레거시 ATMS.T_ATM_UNIT_STATUS 를 신규 JIF 포맷에 맞춰 현대화:
--   - DEVICE_ID/STATUS 를 VARCHAR2(8) 로 확장 (deviceStatus 맵의 1~8자, devices[] 의 MAN/CHK 등 모두 수용)
-- =====================================================================

CREATE TABLE T_ATM_UNIT_STATUS (
    TERM_GROUP_ID   CHAR(5)        NOT NULL,           -- 영업점코드
    TERM_ID         VARCHAR2(20)   NOT NULL,           -- 단말번호
    DEVICE_ID       VARCHAR2(8)    NOT NULL,           -- 장치ID (예: 7, MAN, CHK)
    AGENT_TYPE      CHAR(1)        DEFAULT 'A' NOT NULL,-- 에이전트구분 (A/M ...)
    LOGICAL_SERVICE VARCHAR2(64),                      -- 논리서비스명
    STATUS          VARCHAR2(8),                       -- 상태(1자리 코드 또는 8byte 상태)
    FAULT_FLAG      CHAR(1),                           -- 장애여부 (Y/N)
    JNL_DATE        CHAR(8),                           -- 저널발생일
    JNL_TIME        CHAR(10),                          -- 저널발생시간
    SP_VERSION      VARCHAR2(32),                      -- SP버전
    EP_VERSION      VARCHAR2(32),                      -- EP버전
    UPD_DT          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_T_ATM_UNIT_STATUS PRIMARY KEY (TERM_GROUP_ID, TERM_ID, DEVICE_ID, AGENT_TYPE)
);

CREATE INDEX IX_T_ATM_UNIT_STATUS_FAULT ON T_ATM_UNIT_STATUS (FAULT_FLAG, TERM_GROUP_ID, TERM_ID);

COMMENT ON TABLE  T_ATM_UNIT_STATUS                 IS '기기 장치(디바이스) 상태/버전 현황';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.TERM_GROUP_ID  IS '영업점코드';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.TERM_ID        IS '단말번호';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.DEVICE_ID      IS '장치ID';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.AGENT_TYPE     IS '에이전트구분';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.LOGICAL_SERVICE IS '논리서비스명';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.STATUS         IS '상태';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.FAULT_FLAG     IS '장애여부(Y/N)';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.JNL_DATE       IS '저널발생일';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.JNL_TIME       IS '저널발생시간';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.SP_VERSION     IS 'SP버전';
COMMENT ON COLUMN T_ATM_UNIT_STATUS.EP_VERSION     IS 'EP버전';
