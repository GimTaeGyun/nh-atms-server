-- =====================================================================
-- V1.4.6 : 영구 채번 마스터 (구 MG T_SEQNOMASTER)
--   T_SEQ_NO_MASTER : 대외 송수신 전문의 기관측 일련번호(atms_sn, BASE_CD_TYPE='004'),
--                     벤더 일련번호(vend_sn, '005'), 대내 모니터링(010), SMS 등
--                     (기준,컨텐츠) 조합별로 1..9999999 를 순환하며 영구 채번한다.
--
--   레거시 datm.SP_GET_SEQNO / SP_UPDATE_SEQNO (MySQL) 를 Oracle + Java 서비스로 이식.
--   신규는 인메모리 GlobalIdGenerator(32자 글로벌ID)만 있어 재기동 시 일련번호가
--   초기화되므로, 벤더 프로토콜이 요구하는 영구 단조증가 일련번호를 본 테이블로 보장한다.
--   (적용: kr.co.nh.atms.ifex.service.SequenceNoService / SequenceNoMapper)
-- =====================================================================

-- ---------------------------------------------------------------------
-- T_SEQ_NO_MASTER (구 T_SEQNOMASTER)
--   PK = (BASE_CD_TYPE, BASE_CONTENT1..5). 미사용 컨텐츠는 '0' 으로 채운다.
--   BASE_NO 가 현재 일련번호. SP_GET_SEQNO 는 +1 후 9999999 초과 시 1 로 순환.
-- ---------------------------------------------------------------------
CREATE TABLE T_SEQ_NO_MASTER (
    BASE_CD_TYPE   VARCHAR2(3)              NOT NULL,   -- 기준코드구분 (004:기관일련 005:벤더일련 010:모니터링 ...)
    BASE_CONTENT1  VARCHAR2(30) DEFAULT '0' NOT NULL,   -- 기준컨텐츠1 (예: 벤더코드)
    BASE_CONTENT2  VARCHAR2(30) DEFAULT '0' NOT NULL,   -- 기준컨텐츠2 (예: 채번용도)
    BASE_CONTENT3  VARCHAR2(30) DEFAULT '0' NOT NULL,   -- 기준컨텐츠3
    BASE_CONTENT4  VARCHAR2(30) DEFAULT '0' NOT NULL,   -- 기준컨텐츠4
    BASE_CONTENT5  VARCHAR2(30) DEFAULT '0' NOT NULL,   -- 기준컨텐츠5
    BASE_NO        NUMBER(19)   DEFAULT 0   NOT NULL,   -- 현재 일련번호
    CONSTRAINT PK_T_SEQ_NO_MASTER PRIMARY KEY
        (BASE_CD_TYPE, BASE_CONTENT1, BASE_CONTENT2, BASE_CONTENT3, BASE_CONTENT4, BASE_CONTENT5)
);

COMMENT ON TABLE  T_SEQ_NO_MASTER               IS 'T_채번마스터 (구 T_SEQNOMASTER)';
COMMENT ON COLUMN T_SEQ_NO_MASTER.BASE_CD_TYPE  IS '기준코드구분 (004:기관일련 005:벤더일련 010:모니터링)';
COMMENT ON COLUMN T_SEQ_NO_MASTER.BASE_CONTENT1 IS '기준컨텐츠1 (예: 벤더코드)';
COMMENT ON COLUMN T_SEQ_NO_MASTER.BASE_CONTENT2 IS '기준컨텐츠2 (예: 채번용도)';
COMMENT ON COLUMN T_SEQ_NO_MASTER.BASE_CONTENT3 IS '기준컨텐츠3';
COMMENT ON COLUMN T_SEQ_NO_MASTER.BASE_CONTENT4 IS '기준컨텐츠4';
COMMENT ON COLUMN T_SEQ_NO_MASTER.BASE_CONTENT5 IS '기준컨텐츠5';
COMMENT ON COLUMN T_SEQ_NO_MASTER.BASE_NO       IS '현재 일련번호 (1..9999999 순환)';
