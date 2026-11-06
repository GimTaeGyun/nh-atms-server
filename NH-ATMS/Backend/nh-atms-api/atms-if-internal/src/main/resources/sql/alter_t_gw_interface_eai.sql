-- =====================================================================
-- atms-if-internal : 은행 EAI 실연동 대응 DDL (Oracle 19C)
--  1) T_GW_INTERFACE 2-phase 복원용 컬럼 추가
--     - MSG_SEQNO : GlobalID 24~30자리 일련번호(7자리). 응답 전문 매칭 키 (AS-IS substring(23,30))
--     - GW_FLAG   : '0' 송신(INSERT 시), '1' 응답수신(UPDATE 시)
--  2) EAI 전문 일련번호 채번 시퀀스 (구 SP_GET_SEQNO('010','0','0') 대응)
-- =====================================================================

ALTER TABLE T_GW_INTERFACE ADD (
    MSG_SEQNO VARCHAR2(10),
    GW_FLAG   CHAR(1) DEFAULT '0'
);

COMMENT ON COLUMN T_GW_INTERFACE.MSG_SEQNO IS 'EAI 전문 일련번호(7자리, GlobalID substring(23,30)) - 응답 매칭 키';
COMMENT ON COLUMN T_GW_INTERFACE.GW_FLAG   IS '게이트웨이 처리 플래그 (0:송신, 1:응답수신)';

CREATE INDEX IX_T_GW_INTERFACE_MSG_SEQNO ON T_GW_INTERFACE (MSG_SEQNO);

-- 구 SP_GET_SEQNO(MySQL 프로시저) 대응: 7자리 순환 시퀀스
CREATE SEQUENCE SEQ_EAI_MSG_SEQNO
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9999999
    CYCLE
    NOCACHE;

-- (참고) SEND_DATA / RECV_DATA 에 EUC-KR 전문 원문(시재조회 약 433byte, SMS 약 1,301byte)이
-- 저장되므로 컬럼 길이가 부족하면 아래와 같이 확장 필요.
-- ALTER TABLE T_GW_INTERFACE MODIFY (SEND_DATA VARCHAR2(4000), RECV_DATA VARCHAR2(4000));
