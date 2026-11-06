-- =====================================================================
-- V1.4.8 : 기기 디바이스정보(개국정보) 마스터 T_ATM_DEVICE_STATUS 신설
--   대외 개국/장애/상태통보 전문(EM 01001110)의 개국정보 영역(35~43)
--   - APP버전 / DES / IC / EMV / IR / RF / 지문(FP) / 오만원권 / 암호화 가능여부
--   를 적재/조회하기 위한 기기별 디바이스 스펙 마스터.
--
--   레거시 atme T_ATMDEVICESTATUS 정합. 표준 언더스코어 명명으로 신설한다.
--   (사용: kr.co.nh.atms.ifex.service.ExternalIfService.statusNotify)
-- =====================================================================

CREATE TABLE T_ATM_DEVICE_STATUS (
    TERM_GROUP_ID   CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID         VARCHAR2(20)   NOT NULL,   -- 단말번호
    DES_APPLY       CHAR(1),                   -- DES적용여부
    IC_APPLY        CHAR(1),                   -- IC적용여부
    EMV_APPLY       CHAR(1),                   -- EMV적용여부
    IR_APPLY        CHAR(1),                   -- IR적용여부
    RF_APPLY        CHAR(1),                   -- RF적용여부
    FP_APPLY        CHAR(1),                   -- FP(지문)적용여부
    OMAN_APPLY      CHAR(1),                   -- 오만원권가능여부
    ENCODE_APPLY    CHAR(1),                   -- 암호화적용여부
    OMAN_O_STATUS   CHAR(1),                   -- 오만원권출금부상태
    OMAN_I_STATUS   CHAR(1),                   -- 오만원권입금부상태
    MAN_O_STATUS    CHAR(1),                   -- 만원권출금부상태
    MAN_I_STATUS    CHAR(1),                   -- 만원권입금부상태
    CHK_O_STATUS    CHAR(1),                   -- 수표출금부상태
    CHK_I_STATUS    CHAR(1),                   -- 수표입금부상태
    PB_STATUS       CHAR(1),                   -- 통장부상태
    OCHUN_STATUS    CHAR(1),                   -- 오천원입금부상태
    ADD_INFO        VARCHAR2(200),             -- 부가정보
    BARCODE_APPLY   CHAR(1),                   -- 바코드적용여부
    CHK_STATUS      CHAR(1),                   -- 수표부상태
    PB_AUTO_TURN    CHAR(1),                   -- 통장자동넘김여부
    HANDICAP_ATM    CHAR(1),                   -- 장애인용ATM여부
    IP_ADDRESS      CHAR(20),                  -- IP주소
    OS_INFO         CHAR(50),                  -- OS정보
    CPU_INFO        CHAR(50),                  -- CPU정보
    MEM_INFO        CHAR(20),                  -- 메모리정보
    HDD1_TOT        CHAR(10),                  -- HDD1용량
    HDD1_USED       CHAR(10),                  -- HDD1사용량
    HDD2_TOT        CHAR(10),                  -- HDD2용량
    HDD2_USED       CHAR(10),                  -- HDD2사용량
    INSTALL_DATE    VARCHAR2(8),               -- 설치일자
    UPGRADE_INFO    VARCHAR2(2),               -- 업그레이드정보
    CONSTRAINT PK_T_ATM_DEVICE_STATUS PRIMARY KEY (TERM_GROUP_ID, TERM_ID)
);

COMMENT ON TABLE  T_ATM_DEVICE_STATUS              IS 'T_기기디바이스정보(개국정보)';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.DES_APPLY     IS 'DES적용여부';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.IC_APPLY      IS 'IC적용여부';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.EMV_APPLY     IS 'EMV적용여부';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.IR_APPLY      IS 'IR적용여부';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.RF_APPLY      IS 'RF적용여부';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.FP_APPLY      IS 'FP(지문)적용여부';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.OMAN_APPLY    IS '오만원권가능여부';
COMMENT ON COLUMN T_ATM_DEVICE_STATUS.ENCODE_APPLY  IS '암호화적용여부';
