-- =====================================================================
-- V1.3.2 : 기기 자원정보 + 저널 보관정책 2종
--   T_ATM_RESOURCE   : 기기 자원정보(에이전트 보고)   (9322)
--   T_JOURNAL_POLICY : 저널/이미지 보관정책            (4101)
--
-- 근거: 16_통합스키마설계.md §3.4, §7.2
--   1) T_ATM_RESOURCE : 에이전트가 주기적으로 보고하는 자산/스펙/모듈
--      정보. 기기 기준정보(T_ATM)와는 갱신주기·성격이 달라 분리(3NF).
--      펌웨어배포(atms-file)·호환성점검·자산관리의 기반.
--      T_ATM_MONITOR 의 AGENT_VER/OS 등 자원성 컬럼은 본 테이블로 단일화.
--   2) T_JOURNAL_POLICY : 기기별 운영모드(종이/전자) + 저널범위(내용/안면/
--      수취/카드) + 항목별 보관·삭제 주기(월). 카드이미지는 제외대상
--      (SCOPE_CARD_YN 기본 'N'). atms-journal/batch 파기처리가 참조.
--   기기키 TERM_GROUP_ID / TERM_ID 는 T_ATM 논리참조.
-- 명명: 16_통합스키마설계.md §0.5 명명 표준(언더스코어)을 따른다.
-- 선행: V1.3.1. 기존 테이블 미변경, 신규 CREATE 만 수행.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_ATM_RESOURCE (T_기기자원정보) - 9322 운영현황(자원정보)
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_RESOURCE (
    TERM_GROUP_ID CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID       VARCHAR2(20)   NOT NULL,   -- 단말번호
    STATUS_DT     CHAR(14),                  -- 최종보고일시(yyyyMMddHHmmss)
    VENDOR_CD     VARCHAR2(10),              -- 공급업체코드
    MODEL_NM      VARCHAR2(100),             -- 모델명
    PRODUCT_NO    VARCHAR2(50),              -- 제품번호
    OS_TYPE       VARCHAR2(30),              -- OS종류
    AP_VER        VARCHAR2(30),              -- AP(업무)버전
    AGENT_VER     VARCHAR2(30),              -- Agent버전
    CPU_INFO      VARCHAR2(100),             -- CPU정보
    MEMORY        VARCHAR2(30),              -- 메모리
    DISK_CNT      NUMBER(5),                 -- 디스크수
    DISK_TOTAL    VARCHAR2(30),              -- 디스크총용량
    DISK_FREE     VARCHAR2(30),              -- 디스크여유
    COMM_TYPE     VARCHAR2(20),              -- 통신방식
    COMM_SPEED    VARCHAR2(20),              -- 통신속도
    IP_ADDR       VARCHAR2(15),              -- IP주소
    MAC_ADDR      VARCHAR2(20),              -- MAC주소
    IC_KCASH      CHAR(1),                   -- IC모듈:K-CASH
    IC_MYBI       CHAR(1),                   -- IC모듈:마이비
    IC_FIN        CHAR(1),                   -- IC모듈:금융IC
    IC_EMV1       CHAR(1),                   -- IC모듈:EMV1
    IC_EMV2       CHAR(1),                   -- IC모듈:EMV2
    MOD_SEED      CHAR(1),                   -- 모듈:시드
    MOD_SECURITY  CHAR(1),                   -- 모듈:보안
    MOD_VACCINE   CHAR(1),                   -- 모듈:백신
    MOD_CAMERA    CHAR(1),                   -- 모듈:카메라
    MOD_UPS       CHAR(1),                   -- 모듈:UPS
    MOD_50K       CHAR(1),                   -- 모듈:5만원권
    REG_DT        CHAR(14),                  -- 등록일시
    UPD_DT        CHAR(14),                  -- 수정일시
    CONSTRAINT PK_T_ATM_RESOURCE PRIMARY KEY (TERM_GROUP_ID, TERM_ID)
);

COMMENT ON TABLE  T_ATM_RESOURCE               IS 'T_기기자원정보';
COMMENT ON COLUMN T_ATM_RESOURCE.TERM_GROUP_ID IS '영업점코드';
COMMENT ON COLUMN T_ATM_RESOURCE.TERM_ID       IS '단말번호';
COMMENT ON COLUMN T_ATM_RESOURCE.STATUS_DT     IS '최종보고일시(yyyyMMddHHmmss)';
COMMENT ON COLUMN T_ATM_RESOURCE.VENDOR_CD     IS '공급업체코드';
COMMENT ON COLUMN T_ATM_RESOURCE.MODEL_NM      IS '모델명';
COMMENT ON COLUMN T_ATM_RESOURCE.PRODUCT_NO    IS '제품번호';
COMMENT ON COLUMN T_ATM_RESOURCE.OS_TYPE       IS 'OS종류';
COMMENT ON COLUMN T_ATM_RESOURCE.AP_VER        IS 'AP(업무)버전';
COMMENT ON COLUMN T_ATM_RESOURCE.AGENT_VER     IS 'Agent버전';
COMMENT ON COLUMN T_ATM_RESOURCE.CPU_INFO      IS 'CPU정보';
COMMENT ON COLUMN T_ATM_RESOURCE.MEMORY        IS '메모리';
COMMENT ON COLUMN T_ATM_RESOURCE.DISK_CNT      IS '디스크수';
COMMENT ON COLUMN T_ATM_RESOURCE.DISK_TOTAL    IS '디스크총용량';
COMMENT ON COLUMN T_ATM_RESOURCE.DISK_FREE     IS '디스크여유';
COMMENT ON COLUMN T_ATM_RESOURCE.COMM_TYPE     IS '통신방식';
COMMENT ON COLUMN T_ATM_RESOURCE.COMM_SPEED    IS '통신속도';
COMMENT ON COLUMN T_ATM_RESOURCE.IP_ADDR       IS 'IP주소';
COMMENT ON COLUMN T_ATM_RESOURCE.MAC_ADDR      IS 'MAC주소';
COMMENT ON COLUMN T_ATM_RESOURCE.IC_KCASH      IS 'IC모듈:K-CASH 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.IC_MYBI       IS 'IC모듈:마이비 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.IC_FIN        IS 'IC모듈:금융IC 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.IC_EMV1       IS 'IC모듈:EMV1 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.IC_EMV2       IS 'IC모듈:EMV2 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.MOD_SEED      IS '모듈:시드 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.MOD_SECURITY  IS '모듈:보안 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.MOD_VACCINE   IS '모듈:백신 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.MOD_CAMERA    IS '모듈:카메라 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.MOD_UPS       IS '모듈:UPS 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.MOD_50K       IS '모듈:5만원권 장착여부';
COMMENT ON COLUMN T_ATM_RESOURCE.REG_DT        IS '등록일시';
COMMENT ON COLUMN T_ATM_RESOURCE.UPD_DT        IS '수정일시';

-- ---------------------------------------------------------------------
-- 2) T_JOURNAL_POLICY (T_저널보관정책) - 4101 운영환경설정
--    저장범위 카드(SCOPE_CARD_YN)는 회의 결정에 따라 기본 제외('N').
-- ---------------------------------------------------------------------
CREATE TABLE T_JOURNAL_POLICY (
    TERM_GROUP_ID    CHAR(5)        NOT NULL,-- 영업점코드
    TERM_ID          VARCHAR2(20)   NOT NULL,-- 단말번호
    OPER_MODE        VARCHAR2(2),             -- 운영모드(종이/전자/전자+종이)
    SCOPE_CONTENT_YN CHAR(1)        DEFAULT 'Y',-- 저널범위:내용
    SCOPE_FACE_YN    CHAR(1)        DEFAULT 'Y',-- 저널범위:안면
    SCOPE_RECEIVE_YN CHAR(1)        DEFAULT 'Y',-- 저널범위:수취
    SCOPE_CARD_YN    CHAR(1)        DEFAULT 'N',-- 저널범위:카드(제외대상)
    KEEP_CONTENT_M   NUMBER(5),               -- 내용 보관주기(월)
    KEEP_IMAGE_M     NUMBER(5),               -- 이미지 보관주기(월)
    DEL_CONTENT_M    NUMBER(5),               -- 내용 삭제주기(월)
    DEL_IMAGE_M      NUMBER(5),               -- 이미지 삭제주기(월)
    USE_YN           CHAR(1)        DEFAULT 'Y',-- 사용여부
    REG_DT           CHAR(14),                -- 등록일시
    UPD_DT           CHAR(14),                -- 수정일시
    CONSTRAINT PK_T_JOURNAL_POLICY PRIMARY KEY (TERM_GROUP_ID, TERM_ID)
);

COMMENT ON TABLE  T_JOURNAL_POLICY                  IS 'T_저널보관정책';
COMMENT ON COLUMN T_JOURNAL_POLICY.TERM_GROUP_ID    IS '영업점코드';
COMMENT ON COLUMN T_JOURNAL_POLICY.TERM_ID          IS '단말번호';
COMMENT ON COLUMN T_JOURNAL_POLICY.OPER_MODE        IS '운영모드(종이/전자/전자+종이)';
COMMENT ON COLUMN T_JOURNAL_POLICY.SCOPE_CONTENT_YN IS '저널범위:내용';
COMMENT ON COLUMN T_JOURNAL_POLICY.SCOPE_FACE_YN    IS '저널범위:안면';
COMMENT ON COLUMN T_JOURNAL_POLICY.SCOPE_RECEIVE_YN IS '저널범위:수취';
COMMENT ON COLUMN T_JOURNAL_POLICY.SCOPE_CARD_YN    IS '저널범위:카드(제외대상)';
COMMENT ON COLUMN T_JOURNAL_POLICY.KEEP_CONTENT_M   IS '내용 보관주기(월)';
COMMENT ON COLUMN T_JOURNAL_POLICY.KEEP_IMAGE_M     IS '이미지 보관주기(월)';
COMMENT ON COLUMN T_JOURNAL_POLICY.DEL_CONTENT_M    IS '내용 삭제주기(월)';
COMMENT ON COLUMN T_JOURNAL_POLICY.DEL_IMAGE_M      IS '이미지 삭제주기(월)';
COMMENT ON COLUMN T_JOURNAL_POLICY.USE_YN           IS '사용여부';
COMMENT ON COLUMN T_JOURNAL_POLICY.REG_DT           IS '등록일시';
COMMENT ON COLUMN T_JOURNAL_POLICY.UPD_DT           IS '수정일시';

COMMIT;
