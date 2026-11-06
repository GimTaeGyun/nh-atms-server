-- =====================================================================
-- V1.4.7 : 장애관리 출동응답(수신/IN) 컬럼 보강
--   대외업체 -> ATMS 수신 전문(EM 01001130/1140/1150/1190)이 갱신하는
--   출동 생명주기 후반부(도착예정/도착/조치결과) 컬럼을 T_FAULT_MANAGEMENT 에 추가한다.
--
--   레거시 atme EM_01001130_IN / EM_01001140_IN / EM_01001150_IN / EM_01001190_IN 정합.
--   (적용: kr.co.nh.atms.ifex.service.ExternalIfInboundService)
--
--   상태 매핑: 신규 FAULT_STATUS(O/P/C/X) 는 유지하고, 레거시 출동상태 숫자코드
--   (3:도착예정 4:도착 5:조치완료 8:미복구) 는 DISPATCH_STATUS_CD 로 별도 보존한다.
-- =====================================================================

ALTER TABLE T_FAULT_MANAGEMENT ADD (
    DISPATCH_STATUS_CD   CHAR(1),         -- 출동상태코드(3:도착예정 4:도착 5:조치완료 8:미복구)
    DEPARTURE_DATE       CHAR(8),         -- 도착예정일자
    DEPARTURE_TIME       CHAR(6),         -- 도착예정시각
    RESPONSE_PERSON      VARCHAR2(50),    -- 업체접수자명
    SERVICE_PERSON       VARCHAR2(50),    -- 출동요원명
    SERVICE_PHONE_NO     VARCHAR2(20),    -- 출동요원전화번호
    ARRIVAL_DATE         CHAR(8),         -- 코너도착일자
    ARRIVAL_TIME         CHAR(6),         -- 코너도착시각
    ARRIVAL_ELAPSED_TIME NUMBER(10),      -- 출동~도착 경과시간(분)
    FINISHED_DATE        CHAR(8),         -- 조치완료일자
    FINISHED_TIME        CHAR(6),         -- 조치완료시각
    REPAIR_FLAG          CHAR(1),         -- 조치결과코드(1:조치 0:미조치)
    FAILURE_REASON_CD    VARCHAR2(10),    -- 조치불가사유코드
    REPAIR_COMMENT       VARCHAR2(500),   -- 조치의견
    FINISH_ELAPSED_TIME  NUMBER(10)       -- 도착~조치 경과시간(분)
);

COMMENT ON COLUMN T_FAULT_MANAGEMENT.DISPATCH_STATUS_CD   IS '출동상태코드(3:도착예정 4:도착 5:조치완료 8:미복구)';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.DEPARTURE_DATE       IS '도착예정일자';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.DEPARTURE_TIME       IS '도착예정시각';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.RESPONSE_PERSON      IS '업체접수자명';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.SERVICE_PERSON       IS '출동요원명';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.SERVICE_PHONE_NO     IS '출동요원전화번호';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.ARRIVAL_DATE         IS '코너도착일자';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.ARRIVAL_TIME         IS '코너도착시각';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.ARRIVAL_ELAPSED_TIME IS '출동~도착 경과시간(분)';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.FINISHED_DATE        IS '조치완료일자';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.FINISHED_TIME        IS '조치완료시각';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.REPAIR_FLAG          IS '조치결과코드(1:조치 0:미조치)';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.FAILURE_REASON_CD    IS '조치불가사유코드';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.REPAIR_COMMENT       IS '조치의견';
COMMENT ON COLUMN T_FAULT_MANAGEMENT.FINISH_ELAPSED_TIME  IS '도착~조치 경과시간(분)';
