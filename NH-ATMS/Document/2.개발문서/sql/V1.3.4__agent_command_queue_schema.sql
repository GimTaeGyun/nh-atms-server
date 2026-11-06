-- =====================================================================
-- V1.3.4 : T_AGENT_COMMAND (서버→에이전트 원격제어 명령 큐)
--   운영자/시스템이 발행한 원격제어 명령을 적재하고, 에이전트가
--   GET /api/agent/{atmId}/commands 로 폴링하여 수령(ack)·실행 후
--   result 를 통보하는 양방향 명령 큐. (계약: agent-api-openapi.yaml command 태그)
--
--   설계기준
--     - 패턴은 V1.1.7 T_IF_QUEUE 를 따른다(시퀀스 채번, CLOB payload,
--       CHAR(1) 상태플래그, 폴링 인덱스).
--     - 단, 큐 키는 계약상 문자열 cmdId("CMD-yyyyMMdd-NNNNNN")를 PK 로
--       사용한다. 일련번호는 SEQ_T_AGENT_COMMAND 로 채번해 서비스에서 조립.
--     - 04_DB스키마설계.md §7.2 T_REMOTE_COMMAND 는 '이력' 성격이라
--       큐(상태전이·payload·timeout)에는 부족하여 본 테이블을 신설한다.
--   명명: 16_통합스키마설계.md §0.6 (UPPER_SNAKE_CASE, 언더스코어 표기)
--   선행: V1.3.3. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

CREATE TABLE T_AGENT_COMMAND (
    CMD_ID         VARCHAR2(30)  NOT NULL,           -- 명령 ID (CMD-yyyyMMdd-NNNNNN), PK
    TERM_GROUP_ID  CHAR(5)       NOT NULL,           -- 영업점코드
    TERM_ID        VARCHAR2(20)  NOT NULL,           -- 단말번호
    CMD_TYPE       VARCHAR2(10)  NOT NULL,           -- 명령유형(STAT/IMGC/DLD/FCOL/RST/RBT/POFF/STOP/OPEN_OP/JMOD/MSG/TMOD/CLS/RCMD/GDL)
    PAYLOAD        CLOB,                             -- 명령 파라미터(JSON)
    STATUS         CHAR(1)       DEFAULT 'W' NOT NULL,-- W=대기,A=수령(ack),S=성공,F=실패,T=타임아웃,C=취소
    TIMEOUT_SEC    NUMBER(5)     DEFAULT 180 NOT NULL,-- 실행 제한(초)
    RESULT_CD      VARCHAR2(10),                     -- 결과코드(0x30=성공)
    RESULT_DATA    CLOB,                             -- 결과 상세(JSON)
    ERR_MSG        VARCHAR2(500),                    -- 실패 메시지
    REG_USER_ID    VARCHAR2(30),                     -- 발행자(운영자) ID, 시스템발행=SYSTEM
    ISSUED_AT      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 발행시각
    ACK_AT         TIMESTAMP,                        -- 에이전트 수령시각
    COMPLETED_AT   TIMESTAMP,                        -- 결과 통보시각
    CONSTRAINT PK_T_AGENT_COMMAND PRIMARY KEY (CMD_ID)
);

CREATE SEQUENCE SEQ_T_AGENT_COMMAND START WITH 1 INCREMENT BY 1 NOCACHE;

-- 에이전트 폴링(대기명령 조회)용: 기기 + 상태 + 발행순
CREATE INDEX IX_T_AGENT_COMMAND_POLL ON T_AGENT_COMMAND (TERM_GROUP_ID, TERM_ID, STATUS, ISSUED_AT);
-- 타임아웃 스윕(대기/수령 건 경과 점검)용
CREATE INDEX IX_T_AGENT_COMMAND_STATUS ON T_AGENT_COMMAND (STATUS, ISSUED_AT);

COMMENT ON TABLE  T_AGENT_COMMAND               IS '서버→에이전트 원격제어 명령 큐';
COMMENT ON COLUMN T_AGENT_COMMAND.CMD_ID        IS '명령 ID(CMD-yyyyMMdd-NNNNNN, PK)';
COMMENT ON COLUMN T_AGENT_COMMAND.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_AGENT_COMMAND.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_AGENT_COMMAND.CMD_TYPE      IS '명령유형(STAT/IMGC/DLD/FCOL/RST/RBT/POFF/STOP/OPEN_OP/JMOD/MSG/TMOD/CLS/RCMD/GDL)';
COMMENT ON COLUMN T_AGENT_COMMAND.PAYLOAD       IS '명령 파라미터(JSON)';
COMMENT ON COLUMN T_AGENT_COMMAND.STATUS        IS '상태(W=대기,A=수령,S=성공,F=실패,T=타임아웃,C=취소)';
COMMENT ON COLUMN T_AGENT_COMMAND.TIMEOUT_SEC   IS '실행 제한(초)';
COMMENT ON COLUMN T_AGENT_COMMAND.RESULT_CD     IS '결과코드(0x30=성공)';
COMMENT ON COLUMN T_AGENT_COMMAND.RESULT_DATA   IS '결과 상세(JSON)';
COMMENT ON COLUMN T_AGENT_COMMAND.ERR_MSG       IS '실패 메시지';
COMMENT ON COLUMN T_AGENT_COMMAND.REG_USER_ID   IS '발행자 ID(시스템발행=SYSTEM)';
COMMENT ON COLUMN T_AGENT_COMMAND.ISSUED_AT     IS '발행시각';
COMMENT ON COLUMN T_AGENT_COMMAND.ACK_AT        IS '에이전트 수령시각';
COMMENT ON COLUMN T_AGENT_COMMAND.COMPLETED_AT  IS '결과 통보시각';

COMMIT;
