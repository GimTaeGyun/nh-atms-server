-- =====================================================================
-- V1.2.1 : 마스터 테이블 6종 (코드 참조하나 마이그레이션에 누락되어 있던 테이블)
--   T_CODE, T_CODE_GROUP   (공통코드 / CodeMapper)
--   T_NOTICE              (공지사항 / NoticeMapper)
--   T_ATM                 (기기마스터 / AtmMapper)
--   T_ATM_STATUS           (기기상태마스터 / AtmStatusMapper)
--   T_ATM_GROUP            (기기그룹마스터 / AtmGroupMapper)
--
-- 출처: 0.기본자료/새마을금고/oracle19c_ddl.sql 의 검증 스키마.
-- 정정 사항(원본의 MySQL 잔재 → Oracle 문법):
--   - CREATE TABLE 내부의 `KEY xxx (...)` 인라인 인덱스 → 별도 CREATE INDEX 로 분리
--   - 앱(NoticeMapper)이 사용하는 USE_YN/START_DATE/END_DATE 컬럼을 T_NOTICE 에 추가
--
-- ※ 시드 데이터(특히 T_CODE/T_CODE_GROUP 코드값)는 별도로 적재 필요.
--   본 파일은 스키마(DDL)만 생성하여 ORA-00942(table or view does not exist) 를 해소한다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) T_CODE (T_코드마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_CODE (
    LANG_CD   CHAR(3)        NOT NULL,   -- 언어코드
    GROUP_CD  CHAR(5)        NOT NULL,   -- 그룹코드
    CODE      VARCHAR2(10)   NOT NULL,   -- 코드
    NAME      VARCHAR2(100),             -- 코드명
    ORDER_NO  NUMBER(10),                -- 표시순서
    CONSTRAINT PK_T_CODE PRIMARY KEY (LANG_CD, GROUP_CD, CODE)
);

COMMENT ON TABLE  T_CODE          IS 'T_코드마스터';
COMMENT ON COLUMN T_CODE.LANG_CD  IS '언어코드';
COMMENT ON COLUMN T_CODE.GROUP_CD IS '그룹코드';
COMMENT ON COLUMN T_CODE.CODE     IS '코드';
COMMENT ON COLUMN T_CODE.NAME     IS '코드명';
COMMENT ON COLUMN T_CODE.ORDER_NO IS '표시순서';

CREATE INDEX IX_T_CODE_GROUP ON T_CODE (GROUP_CD, CODE, NAME);

-- ---------------------------------------------------------------------
-- 2) T_CODE_GROUP (T_코드그룹마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_CODE_GROUP (
    LANG_CD    CHAR(3)       NOT NULL,   -- 언어코드
    GROUP_CD   CHAR(5)       NOT NULL,   -- 그룹코드
    GROUP_NAME VARCHAR2(50),             -- 그룹명
    CONSTRAINT PK_T_CODE_GROUP PRIMARY KEY (LANG_CD, GROUP_CD)
);

COMMENT ON TABLE  T_CODE_GROUP            IS 'T_코드그룹마스터';
COMMENT ON COLUMN T_CODE_GROUP.LANG_CD    IS '언어코드';
COMMENT ON COLUMN T_CODE_GROUP.GROUP_CD   IS '그룹코드';
COMMENT ON COLUMN T_CODE_GROUP.GROUP_NAME IS '그룹명';

-- ---------------------------------------------------------------------
-- 3) T_NOTICE (T_공지사항)
--    원본 컬럼 + 앱(NoticeMapper)이 추가로 사용하는 USE_YN/START_DATE/END_DATE.
-- ---------------------------------------------------------------------
CREATE TABLE T_NOTICE (
    CAT_CD              CHAR(5)        NOT NULL,   -- 집계구분코드(카테고리)
    SEQ_NO              NUMBER(19)     NOT NULL,   -- 일련번호
    SUBJECT             VARCHAR2(255),             -- 제목
    CONTENT             VARCHAR2(4000),            -- 공지내용
    HIT                 NUMBER(19),                -- 조회수
    STEP                NUMBER(19),                -- 스탭
    DEPTH               NUMBER(19),                -- 댑스
    ENTER_DATE          CHAR(14),                  -- 등록일자(YYYYMMDDHH24MISS)
    ENTER_USER_ID       VARCHAR2(10),              -- 등록자ID
    ENTER_USER_NAME     VARCHAR2(50),              -- 작업자이름
    ENTER_TERM_GROUP_ID CHAR(5),                   -- 등록조합인가번호
    ATTACHED_FILE_FLAG  CHAR(1),                   -- 파일첨부여부
    PARENT_SEQ_NO       NUMBER(19),                -- 상위게시물번호
    BANK_CD             CHAR(3),                   -- 은행코드
    TARGET_USER         CHAR(1),                   -- 대상정보
    USE_YN              CHAR(1)        DEFAULT 'Y', -- 사용여부(앱 확장)
    START_DATE          CHAR(8),                   -- 게시시작일(앱 확장)
    END_DATE            CHAR(8),                   -- 게시종료일(앱 확장)
    CONSTRAINT PK_T_NOTICE PRIMARY KEY (CAT_CD, SEQ_NO)
);

COMMENT ON TABLE  T_NOTICE                     IS 'T_공지사항';
COMMENT ON COLUMN T_NOTICE.CAT_CD              IS '집계구분코드';
COMMENT ON COLUMN T_NOTICE.SEQ_NO              IS '일련번호';
COMMENT ON COLUMN T_NOTICE.SUBJECT             IS '제목';
COMMENT ON COLUMN T_NOTICE.CONTENT             IS '공지내용';
COMMENT ON COLUMN T_NOTICE.HIT                 IS '조회수';
COMMENT ON COLUMN T_NOTICE.ENTER_DATE          IS '등록일자';
COMMENT ON COLUMN T_NOTICE.ENTER_USER_ID       IS '등록자ID';
COMMENT ON COLUMN T_NOTICE.BANK_CD             IS '은행코드';
COMMENT ON COLUMN T_NOTICE.TARGET_USER         IS '대상정보';
COMMENT ON COLUMN T_NOTICE.USE_YN              IS '사용여부';
COMMENT ON COLUMN T_NOTICE.START_DATE          IS '게시시작일';
COMMENT ON COLUMN T_NOTICE.END_DATE            IS '게시종료일';

-- ---------------------------------------------------------------------
-- 4) T_ATM_GROUP (T_기기그룹마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_GROUP (
    TERM_GROUP_ID   CHAR(5)     NOT NULL,   -- 영업점코드
    TERM_GROUP_NAME CHAR(40),               -- 영업점명
    GROUP_LEVEL     NUMBER(10),             -- 그룹레벨
    PARENT_ID       CHAR(3),                -- 부모그룹ID
    SUPER_ID        CHAR(3),                -- 관리ID
    CONSTRAINT PK_T_ATM_GROUP PRIMARY KEY (TERM_GROUP_ID)
);

COMMENT ON TABLE  T_ATM_GROUP                 IS 'T_기기그룹마스터';
COMMENT ON COLUMN T_ATM_GROUP.TERM_GROUP_ID   IS '영업점코드';
COMMENT ON COLUMN T_ATM_GROUP.TERM_GROUP_NAME IS '영업점명';
COMMENT ON COLUMN T_ATM_GROUP.GROUP_LEVEL     IS '그룹레벨';
COMMENT ON COLUMN T_ATM_GROUP.PARENT_ID       IS '부모그룹ID';
COMMENT ON COLUMN T_ATM_GROUP.SUPER_ID        IS '관리ID';

CREATE INDEX IX_T_ATM_GROUP_PARENT ON T_ATM_GROUP (TERM_GROUP_ID, PARENT_ID);

-- ---------------------------------------------------------------------
-- 5) T_ATM (T_기기마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM (
    TERM_GROUP_ID        CHAR(5)        NOT NULL,   -- 영업점코드
    TERM_ID              VARCHAR2(20)   NOT NULL,   -- 단말번호
    ATM_STATUS           CHAR(2),                   -- ATM상태
    ASSET_FLAG           CHAR(1),                   -- 자산구분
    MANAGE_TYPE_CD       CHAR(1),                   -- 관리구분코드
    LOCATION_TYPE_CD     CHAR(2),                   -- 설치장소구분코드
    LOCATION_NAME        VARCHAR2(255),             -- 설치장소명
    SERVICE_DAY_TYPE_CD  CHAR(1),                   -- 운영일구분코드
    SERVICE_START_TIME   CHAR(4),                   -- 운영시작시간
    SERVICE_END_TIME     CHAR(4),                   -- 운영종료시간
    SERVICE_START_DATE   CHAR(8),                   -- 운영시작일
    SERVICE_END_DATE     CHAR(8),                   -- 운영종료일
    ENTER_DATE           CHAR(8),                   -- 등록일자
    ENTER_TIME           CHAR(6),                   -- 입력시간
    PHONE_NO             VARCHAR2(50),              -- 전화번호
    ATM_CO_CD            CHAR(2),                   -- 기기업체코드
    MODEL_CD             CHAR(3),                   -- 모델코드
    ATM_IP               CHAR(15),                  -- 기기IP주소
    BP_SERVER_IP         CHAR(15),                  -- BP서버IP주소
    SECURITY_PROVIDER_CD CHAR(2),                   -- 보안업체코드
    SERVICE_PROVIDER_CD  CHAR(2),                   -- 서비스업체코드
    OFFICE_CD            CHAR(4),                   -- 업체지점코드
    TEAM_CD              CHAR(4),                   -- 업체팀코드
    ASSET_SNO            VARCHAR2(20),              -- 자산관리번호
    MACHINE_SNO          VARCHAR2(20),              -- 제조일련번호
    RECV_MACHINE_SNO     VARCHAR2(20),              -- 기기설정제조일련번호
    OWNER_CD             CHAR(10),                  -- 소유자코드
    APP_VERSION          VARCHAR2(32),              -- 기기APP버전
    STATUS_FLAG          CHAR(1),                   -- 접속방식구분
    TRAN_FLAG            CHAR(1),                   -- 거래방식구분
    RECV_ATM_IP          CHAR(15),                  -- 수신기기IP주소
    OPERATE_WEEK         CHAR(7),                   -- 운영요일구분
    INTRODUCE_DATE       CHAR(8),                   -- 도입일
    PROCESSOR_TYPE       CHAR(1),                   -- 프로세스타입구분
    SETTLE_DATE          CHAR(8),                   -- 설치일
    DISUSE_DATE          CHAR(8),                   -- 폐기일
    REMOTE_FLAG          CHAR(1),                   -- 원격제어방식
    ATM_COST             NUMBER(19),                -- 도입비용
    DURABLE_YEARS        CHAR(4),                   -- 내용연수
    FIRST_SETUP_COSTS    NUMBER(19),                -- 최초설치비용
    BRANCH_CD            VARCHAR2(5),               -- 본지점코드
    PROTOCOL_TYPE        CHAR(1),                   -- 프로토콜구분
    BOOTH_TYPE_CD        CHAR(2),                   -- 부스타입
    CORNER_CD            CHAR(3),                   -- 코너코드
    CONSTRAINT PK_T_ATM PRIMARY KEY (TERM_GROUP_ID, TERM_ID)
);

COMMENT ON TABLE  T_ATM                  IS 'T_기기마스터';
COMMENT ON COLUMN T_ATM.TERM_GROUP_ID    IS '영업점코드';
COMMENT ON COLUMN T_ATM.TERM_ID          IS '단말번호';
COMMENT ON COLUMN T_ATM.ATM_STATUS       IS 'ATM상태';
COMMENT ON COLUMN T_ATM.LOCATION_NAME    IS '설치장소명';
COMMENT ON COLUMN T_ATM.ATM_CO_CD        IS '기기업체코드';
COMMENT ON COLUMN T_ATM.MODEL_CD         IS '모델코드';
COMMENT ON COLUMN T_ATM.ATM_IP           IS '기기IP주소';
COMMENT ON COLUMN T_ATM.APP_VERSION      IS '기기APP버전';
COMMENT ON COLUMN T_ATM.BRANCH_CD        IS '본지점코드';
COMMENT ON COLUMN T_ATM.CORNER_CD        IS '코너코드';

CREATE INDEX IX_T_ATM_CORNER ON T_ATM (TERM_GROUP_ID, CORNER_CD, BRANCH_CD, TERM_ID);
CREATE INDEX IX_T_ATM_STATUS ON T_ATM (TERM_GROUP_ID, CORNER_CD, ATM_STATUS, TERM_ID);

-- ---------------------------------------------------------------------
-- 6) T_ATM_STATUS (T_기기상태마스터)
-- ---------------------------------------------------------------------
CREATE TABLE T_ATM_STATUS (
    TERM_GROUP_ID           CHAR(5)      NOT NULL,   -- 영업점코드
    TERM_ID                 VARCHAR2(20) NOT NULL,   -- 단말번호
    SERVICE_STATUS          CHAR(2)      NOT NULL,   -- 기기운영상태
    INIT_DATE               CHAR(8),                 -- 개국일
    INIT_TIME               CHAR(6),                 -- 초기시간
    ATM_FAULT_STATUS        CHAR(1),                 -- 기기장애상태
    OP_FAULT_STATUS         CHAR(1),                 -- 운영장애상태
    ERR_GROUP_CD            CHAR(2),                 -- 장애그룹코드
    ERR_CD                  VARCHAR2(10),            -- 장애코드
    ATM_SESSION_STATUS      CHAR(1),                 -- 기기세션상태
    FAULT_ARISED_DATE       CHAR(8),                 -- 기기장애발생일
    FAULT_ARISED_TIME       CHAR(10),                -- 기기장애발생시간
    SVR_RECEIVE_DATE        CHAR(8),                 -- 서버수신일
    SVR_RECEIVE_TIME        CHAR(6),                 -- 서버수신시간
    REMAIN_AMOUNT           NUMBER(19),              -- 기기잔액
    CHECK_AMOUNT            NUMBER(19),              -- 수표금액
    CDU_STATUS              CHAR(1),                 -- 현금부상태
    MAN_STATUS              CHAR(1),                 -- 만원권상태
    OMAN_STATUS             CHAR(1),                 -- 오만원권상태
    CHK_STATUS              CHAR(1),                 -- 수표부상태
    MCR_STATUS              CHAR(1),                 -- 카드부상태
    SPR_STATUS              CHAR(1),                 -- 명세표부상태
    JPR_STATUS              CHAR(1),                 -- 저널부상태
    RJT_STATUS              CHAR(1),                 -- 회수함상태
    LAST_TR_DATE            CHAR(8),                 -- 최종거래일
    LAST_TR_TIME            CHAR(10),                -- 최종거래시간
    LAST_ADD_CASH_DATE      CHAR(8),                 -- 최종현금보충일
    LAST_ADD_CASH_TIME      CHAR(10),                -- 최종현금보충시간
    LAST_DAY_TOTAL_DATE     CHAR(8),                 -- 최종일마감일
    LAST_DAY_TOTAL_TIME     CHAR(10),                -- 최종일마감시간
    STATUS                  CHAR(1),                 -- 상태
    LAST_OUT_CALL_DATE      CHAR(8),                 -- 최종출동요청일
    LAST_OUT_CALL_TIME      CHAR(10),                -- 최종출동요청시간
    LAST_OUT_SNO            NUMBER(10),              -- 최종출동요청일련번호
    FAULT_CLASS_CD          CHAR(2),                 -- 장애분류코드
    LAST_ATM_CALL_DATE      CHAR(8),                 -- 최종기기출동요청일
    LAST_ATM_CALL_TIME      CHAR(10),                -- 최종기기출동요청시간
    LAST_ATM_FAULT_CLASS_CD CHAR(2),                 -- 최종기기장애분류코드
    LAST_ATM_SNO            NUMBER(10),              -- 최종기기출동일련번호
    LAST_KEY_CALL_DATE      CHAR(8),                 -- 최종키출동요청일
    LAST_KEY_CALL_TIME      CHAR(10),                -- 최종키출동요청시간
    LAST_KEY_FAULT_CLASS_CD CHAR(2),                 -- 최종키장애분류코드
    LAST_KEY_SNO            NUMBER(10),              -- 최종키출동일련번호
    CONSTRAINT PK_T_ATM_STATUS PRIMARY KEY (TERM_GROUP_ID, TERM_ID)
);

COMMENT ON TABLE  T_ATM_STATUS                IS 'T_기기상태마스터';
COMMENT ON COLUMN T_ATM_STATUS.TERM_GROUP_ID  IS '영업점코드';
COMMENT ON COLUMN T_ATM_STATUS.TERM_ID        IS '단말번호';
COMMENT ON COLUMN T_ATM_STATUS.SERVICE_STATUS IS '기기운영상태';
COMMENT ON COLUMN T_ATM_STATUS.ATM_FAULT_STATUS IS '기기장애상태';
COMMENT ON COLUMN T_ATM_STATUS.OP_FAULT_STATUS  IS '운영장애상태';
COMMENT ON COLUMN T_ATM_STATUS.ERR_GROUP_CD   IS '장애그룹코드';
COMMENT ON COLUMN T_ATM_STATUS.ERR_CD         IS '장애코드';
COMMENT ON COLUMN T_ATM_STATUS.REMAIN_AMOUNT  IS '기기잔액';
COMMENT ON COLUMN T_ATM_STATUS.FAULT_CLASS_CD IS '장애분류코드';

COMMIT;
