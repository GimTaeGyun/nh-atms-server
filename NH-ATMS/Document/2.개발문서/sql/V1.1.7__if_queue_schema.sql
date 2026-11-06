-- =====================================================================
-- V1.1.7 : T_IF_QUEUE (대내/대외 IF 비동기 큐) — atmr 1-D
--   장애 저널(jnl_kind=22) 수신 시 atms-journal 이 큐에 적재하고,
--   IfQueueWorker(@Scheduled) 가 폴링하여 atme(출동요청)/atmi(장애SMS) 호출.
--   참조: 2.개발문서/08_레거시모듈_마이그레이션_설계.md §1-D, §5-5
-- =====================================================================

CREATE TABLE T_IF_QUEUE (
    QUEUE_SEQ      NUMBER(19)    NOT NULL,           -- PK (SEQ_T_IF_QUEUE)
    IF_TARGET      CHAR(1)       NOT NULL,           -- 대상: I=대내(atmi), E=대외(atme)
    IF_ACTION      VARCHAR2(30)  NOT NULL,           -- 동작: SERVICE_CALL, FAULT_SMS 등
    TERM_GROUP_ID  CHAR(5),                          -- 영업점코드
    TERM_ID        VARCHAR2(20),                     -- 단말번호
    JNL_DATE       CHAR(8),                          -- 원천 저널 일자
    JNL_TIME       CHAR(10),                         -- 원천 저널 시각
    ERR_CD         VARCHAR2(20),                     -- 장애코드
    PAYLOAD        CLOB,                             -- 호출 본문(JSON)
    STATUS         CHAR(1)       DEFAULT 'W' NOT NULL,-- W=대기,P=처리중,S=성공,F=실패
    RETRY_CNT      NUMBER(3)     DEFAULT 0   NOT NULL,-- 재시도 횟수
    RESULT_CD      VARCHAR2(10),                     -- 처리 결과코드
    RESULT_MSG     VARCHAR2(500),                    -- 처리 결과메시지
    REG_DT         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    SENT_DT        TIMESTAMP,                        -- 최종 처리(송신) 시각
    CONSTRAINT PK_T_IF_QUEUE PRIMARY KEY (QUEUE_SEQ)
);

CREATE SEQUENCE SEQ_T_IF_QUEUE START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE INDEX IX_T_IF_QUEUE_STATUS ON T_IF_QUEUE (STATUS, QUEUE_SEQ);

COMMENT ON TABLE  T_IF_QUEUE               IS '대내/대외 IF 비동기 큐 (atmr 1-D)';
COMMENT ON COLUMN T_IF_QUEUE.QUEUE_SEQ     IS '큐 일련번호(PK)';
COMMENT ON COLUMN T_IF_QUEUE.IF_TARGET     IS '대상(I=대내 atmi, E=대외 atme)';
COMMENT ON COLUMN T_IF_QUEUE.IF_ACTION     IS '동작(SERVICE_CALL/FAULT_SMS 등)';
COMMENT ON COLUMN T_IF_QUEUE.PAYLOAD       IS '호출 본문(JSON)';
COMMENT ON COLUMN T_IF_QUEUE.STATUS        IS '상태(W=대기,P=처리중,S=성공,F=실패)';
COMMENT ON COLUMN T_IF_QUEUE.RETRY_CNT     IS '재시도 횟수';

COMMIT;
