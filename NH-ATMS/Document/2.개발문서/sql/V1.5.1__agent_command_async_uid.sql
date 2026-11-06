-- =====================================================================
-- V1.5.1 : T_AGENT_COMMAND 비동기 명령 상관 ID(ASYNC_UID) 추가
-- ---------------------------------------------------------------------
-- 원격제어/파일다운로드를 MG식 서버 푸시로 전환하면서, 비동기(파일수집/
-- 다운로드/직접실행) 명령은 atms-file 이 부여한 async_uid 로 완료 콜백
-- (put.msg.resp.do)과 명령을 상관(correlation)한다.
-- =====================================================================

ALTER TABLE T_AGENT_COMMAND ADD (ASYNC_UID NUMBER(19));

COMMENT ON COLUMN T_AGENT_COMMAND.ASYNC_UID IS '비동기 명령 상관 ID(atms-file t_msg_async.MSG_UID)';

CREATE INDEX IX_T_AGENT_COMMAND_ASYNC ON T_AGENT_COMMAND (ASYNC_UID);
