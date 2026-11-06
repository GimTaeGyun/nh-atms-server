-- =====================================================================
-- V1.5.0 : T_MSG_ASYNC (비동기 원격명령 송수신 이력)
--   MG식 원격제어 push 구조 이식. atms-web 이 발행한 명령을 atms-file 이
--   기기측 AP 로 RSA/AES push 할 때, 비동기(##async##) 명령은 본 테이블에
--   송신전문을 적재(sta=1)하고 10자리 async_uid 를 부여한다.
--   이후 기기가 결과를 atms-file(/atmf/agent/put.msg.resp.do)로 통보하면
--   sta=3(수신완료)으로 갱신되며, UI 는 /atmf/agent/get.msg.resp.do 로 폴링한다.
--
--   원본: 구 새마을금고 atms.t_msg_async (MySQL, useGeneratedKeys/last_insert_id)
--   ※ Oracle 예약어 회피: 컬럼 UID → MSG_UID, 채번은 SEQ_T_MSG_ASYNC
--   선행: V1.4.9. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

CREATE TABLE T_MSG_ASYNC (
    MSG_UID   NUMBER(19)   NOT NULL,            -- 비동기 식별자(PK), async_uid 는 %010d 포맷
    SND_DT    VARCHAR2(20),                     -- 송신시각(YYYYMMDDHH24MISSFF3)
    SND       CLOB,                             -- 송신 전문(JSON)
    RCV_DT    VARCHAR2(20),                     -- 수신시각(YYYYMMDDHH24MISSFF3)
    RCV       CLOB,                             -- 수신 결과(JSON)
    STA       CHAR(1)      DEFAULT '1' NOT NULL,-- 1=송신,2=에러,3=수신완료
    CONSTRAINT PK_T_MSG_ASYNC PRIMARY KEY (MSG_UID)
);

CREATE SEQUENCE SEQ_T_MSG_ASYNC START WITH 1 INCREMENT BY 1 NOCACHE;

-- 상태/송신순 조회용
CREATE INDEX IX_T_MSG_ASYNC_STA ON T_MSG_ASYNC (STA, SND_DT);

COMMENT ON TABLE  T_MSG_ASYNC        IS '비동기 원격명령 송수신 이력';
COMMENT ON COLUMN T_MSG_ASYNC.MSG_UID IS '비동기 식별자(PK), async_uid=%010d';
COMMENT ON COLUMN T_MSG_ASYNC.SND_DT  IS '송신시각(YYYYMMDDHH24MISSFF3)';
COMMENT ON COLUMN T_MSG_ASYNC.SND     IS '송신 전문(JSON)';
COMMENT ON COLUMN T_MSG_ASYNC.RCV_DT  IS '수신시각(YYYYMMDDHH24MISSFF3)';
COMMENT ON COLUMN T_MSG_ASYNC.RCV     IS '수신 결과(JSON)';
COMMENT ON COLUMN T_MSG_ASYNC.STA     IS '상태(1=송신,2=에러,3=수신완료)';

COMMIT;
