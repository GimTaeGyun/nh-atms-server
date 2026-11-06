-- =====================================================================
-- V1.1.9 : T_JOURNAL_ERROR 재처리 컬럼 추가 (atmr 1-E)
-- 파싱/처리 실패로 적재된 저널을 재파싱·재처리하기 위한 상태 관리 컬럼.
--   STATUS  : E=에러(재처리대기), R=재처리성공, F=재시도초과(영구실패)
--   RETRY_CNT     : 재시도 횟수
--   LAST_RETRY_DT : 마지막 재처리 시각
-- =====================================================================

ALTER TABLE T_JOURNAL_ERROR ADD (
    STATUS        CHAR(1)     DEFAULT 'E',
    RETRY_CNT     NUMBER(3)   DEFAULT 0,
    LAST_RETRY_DT TIMESTAMP
);

CREATE INDEX IX_T_JOURNAL_ERROR_STATUS ON T_JOURNAL_ERROR (STATUS, SEQ);

COMMENT ON COLUMN T_JOURNAL_ERROR.STATUS        IS '재처리상태 E=대기 R=성공 F=영구실패';
COMMENT ON COLUMN T_JOURNAL_ERROR.RETRY_CNT     IS '재시도 횟수';
COMMENT ON COLUMN T_JOURNAL_ERROR.LAST_RETRY_DT IS '마지막 재처리 시각';
