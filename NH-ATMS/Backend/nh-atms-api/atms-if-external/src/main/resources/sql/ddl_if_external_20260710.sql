-- ============================================================================
-- atms-if-external 대외 전문 처리 스키마 보강 (2026-07-10)
-- 대상 DB: Oracle 19c (ATMS)
-- 근거: AS-IS atme ExternalInterfaceMapper.xml (T_NETSENDDATA / T_NETRECVDATA /
--       T_FAULTMANAGEMENT / T_CORNERTIMEINFO) 정합 이식
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) T_NET_SEND_DATA : 송신전문 이력 보강
--    - PROVIDER_CD : 수신업체코드 (전문 헤더 vend_cd — 레거시 PROVIDER_CD)
--    - ATMS_SN     : ATMS 전문일련번호 7자리 (T_SEQ_NO_MASTER "004"/netsend_snd 채번
--                    — 레거시 SEND_SEQ_NO). 결번요청(IQ_04001110) 재전송 조회키.
--    - VEND_SN     : 업체 전문일련번호 7자리 ("005" 채번 — 레거시 REGIST_SEQNO)
--    - RAW_MSG     : 원시 고정길이 전문 (레거시 SEND_DATA. Mock 모드에서는 NULL)
-- ----------------------------------------------------------------------------
ALTER TABLE T_NET_SEND_DATA ADD (
    PROVIDER_CD  VARCHAR2(2 CHAR),
    ATMS_SN      VARCHAR2(7 CHAR),
    VEND_SN      VARCHAR2(7 CHAR),
    RAW_MSG      VARCHAR2(2048 CHAR)
);

COMMENT ON COLUMN T_NET_SEND_DATA.PROVIDER_CD IS '수신업체코드 (전문 헤더 vend_cd)';
COMMENT ON COLUMN T_NET_SEND_DATA.ATMS_SN     IS 'ATMS 전문일련번호 (7자리, SP_GET_SEQNO 004/netsend_snd)';
COMMENT ON COLUMN T_NET_SEND_DATA.VEND_SN     IS '업체 전문일련번호 (7자리, SP_GET_SEQNO 005)';
COMMENT ON COLUMN T_NET_SEND_DATA.RAW_MSG     IS '원시 고정길이 전문 (euc-kr 512바이트 문자열)';

-- 결번요청(IQ_04001110) 재전송 조회용 인덱스 (레거시 selectNetSendData 등가)
CREATE INDEX IX_T_NET_SEND_DATA_RESEND ON T_NET_SEND_DATA (PROVIDER_CD, SEND_DATE, ATMS_SN);

-- ----------------------------------------------------------------------------
-- 2) T_NET_RECV_DATA : 수신전문 이력 보강
--    - PROVIDER_CD : 송신업체코드 (전문 헤더 vend_cd)
--    - ATMS_SN     : ATMS 수신등록 일련번호 ("004"/netrecv_reg 채번 — 레거시 REGIST_SEQNO)
--    - VEND_SN     : 업체 전문일련번호 (수신전문 헤더 vend_sn — 레거시 RECV_SEQ_NO)
--    - RAW_MSG     : 원시 고정길이 전문 (레거시 RECV_DATA)
-- ----------------------------------------------------------------------------
ALTER TABLE T_NET_RECV_DATA ADD (
    PROVIDER_CD  VARCHAR2(2 CHAR),
    ATMS_SN      VARCHAR2(7 CHAR),
    VEND_SN      VARCHAR2(7 CHAR),
    RAW_MSG      VARCHAR2(2048 CHAR)
);

COMMENT ON COLUMN T_NET_RECV_DATA.PROVIDER_CD IS '송신업체코드 (전문 헤더 vend_cd)';
COMMENT ON COLUMN T_NET_RECV_DATA.ATMS_SN     IS 'ATMS 수신등록 일련번호 (7자리, SP_GET_SEQNO 004/netrecv_reg)';
COMMENT ON COLUMN T_NET_RECV_DATA.VEND_SN     IS '업체 전문일련번호 (수신전문 헤더 vend_sn)';
COMMENT ON COLUMN T_NET_RECV_DATA.RAW_MSG     IS '원시 고정길이 전문 (euc-kr 512바이트 문자열)';

-- ----------------------------------------------------------------------------
-- 3) T_FAULT_MANAGEMENT : 출동요청 전문(EM_01001120) 조립 필드 보강
--    - MANAGE_NO          : 출동요청차수 (동일 장애 N차 출동 — 레거시 MANAGE_NO)
--    - REQUEST_REASON_CD  : 출동요청사유코드 (레거시 REQUEST_REASON_CD)
--    - CUSTOMER_WAIT_FLAG : 고객대기여부 0/1 (레거시 CUSTOMER_WAIT_FLAG)
--    - SECURITY_OFF_DATE/TIME : 경비해제일시 (IQ_04001130 — 레거시 SECURITY_OFF_*)
-- ----------------------------------------------------------------------------
ALTER TABLE T_FAULT_MANAGEMENT ADD (
    MANAGE_NO           NUMBER(3)         DEFAULT 1,
    REQUEST_REASON_CD   VARCHAR2(1 CHAR),
    CUSTOMER_WAIT_FLAG  VARCHAR2(1 CHAR)  DEFAULT '0',
    SECURITY_OFF_DATE   VARCHAR2(8 CHAR),
    SECURITY_OFF_TIME   VARCHAR2(6 CHAR)
);

COMMENT ON COLUMN T_FAULT_MANAGEMENT.MANAGE_NO          IS '출동요청차수 (1부터, 2차 자동출동 시 +1)';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.REQUEST_REASON_CD  IS '출동요청사유코드';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.CUSTOMER_WAIT_FLAG IS '고객대기여부 (0:미대기 1:대기)';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.SECURITY_OFF_DATE  IS '경비해제일자 (IQ_04001130 코너입퇴실)';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.SECURITY_OFF_TIME  IS '경비해제시각 (IQ_04001130 코너입퇴실)';

-- ----------------------------------------------------------------------------
-- 4) T_CORNER_TIME_INFO : 코너입퇴실시간정보 (IQ_04001130 — 레거시 T_CORNERTIMEINFO 이식)
-- ----------------------------------------------------------------------------
CREATE TABLE T_CORNER_TIME_INFO (
    PROVIDER_CD        VARCHAR2(2 CHAR)   NOT NULL,   -- 송신업체코드 (경비업체)
    INOUT_DAY          VARCHAR2(8 CHAR)   NOT NULL,   -- 출입일자 (yyyyMMdd)
    INOUT_TIME         VARCHAR2(6 CHAR)   NOT NULL,   -- 출입시각 (HHmmss)
    INOUT_LOT          VARCHAR2(1 CHAR),              -- 출입구분 (1:카드SET=퇴실 2:카드OFF=입실)
    TERM_GROUP_ID      VARCHAR2(5 CHAR)   NOT NULL,   -- 금고코드
    TERM_ID            VARCHAR2(4 CHAR)   NOT NULL,   -- 기기번호
    BUSINESS_LOT       VARCHAR2(1 CHAR),              -- 업무구분 (1:경비 2:장애 3:현송 4:환경 5:은행)
    CARD_NUM           VARCHAR2(20 CHAR),             -- 카드(직원)번호
    ATMS_RECEIVE_DATE  VARCHAR2(8 CHAR),              -- ATMS 수신일자
    ATMS_RECEIVE_TIME  VARCHAR2(6 CHAR)               -- ATMS 수신시각
);

COMMENT ON TABLE T_CORNER_TIME_INFO IS '코너입퇴실시간정보 (IQ_04001130 수신 — 레거시 T_CORNERTIMEINFO)';

CREATE INDEX IX_T_CORNER_TIME_INFO ON T_CORNER_TIME_INFO (TERM_GROUP_ID, TERM_ID, INOUT_DAY);

-- ----------------------------------------------------------------------------
-- (참고) 본 모듈 SQL 이 참조하는 타 컬럼 — TO-BE 스키마에 없으면 별도 반영 필요:
--   T_CORNER.GUARD_STATUS      : 코너 경비상태 (IQ_04001130 갱신 대상, 레거시 존재)
--   T_ATM.LOCATION_TYPE_CD     : 위치구분 (경비해제일시 갱신 시 동일 위치 단말 매칭)
--   T_CORNER.PARTNER_CD        : 협력업체코드 (IQ_04001140 기기정보통보 조인 키)
--   T_ATM_STATUS.LAST_KEY_*    : 경비업체 Key지원 최종 출동정보 (updateAtmStatusClear)
-- ----------------------------------------------------------------------------
