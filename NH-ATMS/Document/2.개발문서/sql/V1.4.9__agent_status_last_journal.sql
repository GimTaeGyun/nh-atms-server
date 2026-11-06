-- =====================================================================
-- V1.4.9 : T_AGENT_STATUS 저널 수신 추적 컬럼 추가
-- 레거시 atmr 의 update_T_AGENTSTATUS(LAST_FILE_NAME/LAST_FILE_LOC) 대응.
-- 저널 1건 수신/적재 시 기기별 "최종 저널 수신 일시" 를 갱신하여
-- 무응답(저널 미수신) 기기 탐지/모니터링에 활용한다.
-- =====================================================================

ALTER TABLE T_AGENT_STATUS ADD (
    LAST_JNL_DATE  CHAR(8),   -- 최종 저널 수신 일자 (yyyyMMdd)
    LAST_JNL_TIME  CHAR(6)    -- 최종 저널 수신 시각 (HHmmss)
);

COMMENT ON COLUMN T_AGENT_STATUS.LAST_JNL_DATE IS '최종 저널 수신 일자';
COMMENT ON COLUMN T_AGENT_STATUS.LAST_JNL_TIME IS '최종 저널 수신 시각';

COMMIT;
