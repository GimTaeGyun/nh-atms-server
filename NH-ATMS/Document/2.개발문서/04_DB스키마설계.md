# NH-ATMS DB 스키마 설계

> 최종 수정: 2026-04-01  
> DB 종류: 미정 (Oracle 또는 MySQL 예상)  
> 참고: 새마을금고 ATMS DB (MySQL), 디리아 인터페이스 데이터 목록

---

## 1. 설계 원칙

| 항목 | 규칙 |
|------|------|
| 테이블명 | `T_` 접두어 + 대문자 SNAKE_CASE |
| 컬럼명 | 대문자 SNAKE_CASE |
| PK | 복합키 사용 (영업점코드 + 기기번호 + 일시 등) |
| 인코딩 | UTF-8 |
| 날짜 | `CHAR(8)` (yyyyMMdd) 또는 `CHAR(14)` (yyyyMMddHHmmss) |
| 시간 | `CHAR(6)` (HHmmss) 또는 `CHAR(10)` (HHmmssSSS) |
| 코드값 | `CHAR(1~3)` (고정 길이 코드) |
| 금액 | `BIGINT` 또는 `DECIMAL(18,0)` |
| COMMENT | 모든 컬럼에 한글 COMMENT 필수 |
| 파티셔닝 | 저널/거래 테이블은 월별 RANGE 파티션 |

---

## 2. 테이블 분류

### 2.1 분류 체계

| 분류 | 접두어 | 설명 | 예시 |
|------|--------|------|------|
| 마스터 | `T_` | 기준 정보 (초기 이관) | T_ATM, T_BRANCH |
| 저널/트랜잭션 | `T_` | 에이전트 수신 데이터 | T_TRANSACTIONJOURNAL, T_FAULTANALYSIS |
| 집계/통계 | `T_` | 배치 집계 결과 | T_DAYTOTAL, T_DAILYREPORT |
| 시스템 | `T_` | 시스템 관리 | T_USER, T_WEBSITEMENU |
| 이력 | `_HIS` 접미어 | 변경 이력 | T_USER_AUTH_HIST |

### 2.2 전체 테이블 목록

> 새마을금고 기준 약 90개 테이블. NH 규모에 맞게 조정 필요.  
> ★ = 핵심 테이블 (우선 설계), ○ = 필요 테이블, △ = 선택/후순위

---

## 3. 마스터 테이블 (디리아 이관 대상)

### 3.1 T_BRANCH (조직/지점 정보) ★

> 새마을금고의 `t_branch` + `t_atmgroup` 참고

| 컬럼명 | 타입 | NULL | 설명 | 비고 |
|--------|------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 | PK |
| BRANCH_NAME | VARCHAR(200) | NOT NULL | 지점명 | |
| BRANCH_TYPE | CHAR(1) | | 지점구분 | 본점/지점/출장소 |
| PARENT_CODE | CHAR(5) | | 상위조직코드 | 지역본부 등 |
| ZIPCODE | VARCHAR(6) | | 우편번호 | |
| ADDRESS | VARCHAR(255) | | 주소 | |
| DETAIL_ADDRESS | VARCHAR(100) | | 상세주소 | |
| PHONE_NO | VARCHAR(50) | | 전화번호 | |
| FAX_NO | VARCHAR(30) | | 팩스번호 | |
| USE_YN | CHAR(1) | | 사용여부 | Y/N |
| REG_DATE | CHAR(8) | | 등록일 | |
| UPD_DATE | CHAR(8) | | 수정일 | |

```sql
-- PK
PRIMARY KEY (BRANCH_CODE)
```

### 3.2 T_CORNER (코너/설치장소 정보) ★

> 새마을금고의 `t_corner` 참고

| 컬럼명 | 타입 | NULL | 설명 | 비고 |
|--------|------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 | PK |
| CORNER_CODE | CHAR(3) | NOT NULL | 코너코드 | PK |
| CORNER_NAME | VARCHAR(100) | | 코너명 | |
| CORNER_TYPE | CHAR(2) | | 코너구분 | |
| ADDRESS | VARCHAR(255) | | 주소 | |
| PHONE_NO | VARCHAR(50) | | 전화번호 | |
| SERVICE_START_TIME | CHAR(4) | | 운영시작시간 | HHmm |
| SERVICE_END_TIME | CHAR(4) | | 운영종료시간 | HHmm |
| SECURITY_CD | CHAR(2) | | 경비업체코드 | 에스원/캡스 등 |
| USE_YN | CHAR(1) | | 사용여부 | |

```sql
PRIMARY KEY (BRANCH_CODE, CORNER_CODE)
```

### 3.3 T_ATM (기기 마스터) ★

> 새마을금고의 `t_atm` 참고. 가장 핵심 마스터.

| 컬럼명 | 타입 | NULL | 설명 | 비고 |
|--------|------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 | PK |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 | PK |
| ATM_STATUS | CHAR(2) | | 기기상태 | 정상/장애/폐기 |
| CORNER_CODE | CHAR(3) | | 코너코드 | FK |
| MODEL_CODE | CHAR(3) | | 모델코드 | FK |
| ATM_VENDOR_CD | CHAR(2) | | 기기업체코드 | 효성/청호/에이텍 |
| LOCATION_TYPE | CHAR(2) | | 설치장소구분 | |
| LOCATION_NAME | VARCHAR(255) | | 설치장소명 | |
| ATM_IP | CHAR(15) | | 기기 IP | |
| PHONE_NO | VARCHAR(50) | | 전화번호 | |
| SERVICE_START_TIME | CHAR(4) | | 운영시작시간 | |
| SERVICE_END_TIME | CHAR(4) | | 운영종료시간 | |
| SERVICE_START_DATE | CHAR(8) | | 운영시작일 | |
| SERVICE_END_DATE | CHAR(8) | | 운영종료일 | |
| OPERATE_WEEK | CHAR(7) | | 운영요일 | 1100100 등 |
| SECURITY_CD | CHAR(2) | | 보안업체코드 | |
| SERVICE_PROVIDER_CD | CHAR(2) | | 용역업체코드 | |
| ASSET_NO | VARCHAR(20) | | 자산관리번호 | |
| MACHINE_SNO | VARCHAR(20) | | 제조일련번호 | |
| APP_VERSION | VARCHAR(32) | | 기기 S/W 버전 | |
| SETTLE_DATE | CHAR(8) | | 설치일 | |
| DISUSE_DATE | CHAR(8) | | 폐기일 | |
| REMOTE_FLAG | CHAR(1) | | 원격제어방식 | |
| REG_DATE | CHAR(8) | | 등록일 | |
| UPD_DATE | CHAR(8) | | 수정일 | |

```sql
PRIMARY KEY (BRANCH_CODE, DEVICE_ID)
-- INDEX
CREATE INDEX IX_ATM_01 ON T_ATM (BRANCH_CODE, CORNER_CODE, DEVICE_ID);
CREATE INDEX IX_ATM_02 ON T_ATM (ATM_STATUS, BRANCH_CODE);
```

### 3.4 T_ATM_MODEL (기기모델 마스터)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| MODEL_CODE | CHAR(3) | NOT NULL | 모델코드 (PK) |
| MODEL_NAME | VARCHAR(100) | | 모델명 |
| VENDOR_CD | CHAR(2) | | 업체코드 |
| ATM_TYPE | CHAR(2) | | 기기종류 |
| CASSETTE_CNT | INT | | 카세트 수 |
| DESCRIPTION | VARCHAR(500) | | 설명 |
| USE_YN | CHAR(1) | | 사용여부 |

### 3.5 T_CODE_GROUP / T_CODE (공통코드)

**T_CODE_GROUP**

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| CODE_GROUP_ID | VARCHAR(10) | NOT NULL | 코드그룹ID (PK) |
| CODE_GROUP_NAME | VARCHAR(100) | | 코드그룹명 |
| DESCRIPTION | VARCHAR(200) | | 설명 |
| USE_YN | CHAR(1) | | 사용여부 |

**T_CODE**

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| CODE_GROUP_ID | VARCHAR(10) | NOT NULL | 코드그룹ID (PK) |
| CODE_ID | VARCHAR(10) | NOT NULL | 코드ID (PK) |
| CODE_NAME | VARCHAR(100) | | 코드명 |
| SORT_ORDER | INT | | 정렬순서 |
| USE_YN | CHAR(1) | | 사용여부 |
| ATTR1 | VARCHAR(100) | | 속성1 |
| ATTR2 | VARCHAR(100) | | 속성2 |
| ATTR3 | VARCHAR(100) | | 속성3 |

### 3.6 T_FAULT_MAP (장애코드 매핑) ★

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| FAULT_CODE | VARCHAR(10) | NOT NULL | 장애코드 (PK) |
| FAULT_NAME | VARCHAR(200) | | 장애명 |
| FAULT_GROUP | VARCHAR(10) | | 장애그룹 |
| DEVICE_TYPE | CHAR(2) | | 장치구분 |
| SEVERITY | CHAR(1) | | 심각도 (H/M/L) |
| AUTO_DISPATCH | CHAR(1) | | 자동출동여부 |
| DESCRIPTION | VARCHAR(500) | | 설명 |
| USE_YN | CHAR(1) | | 사용여부 |

### 3.7 T_TRANSACTION_CODE (거래코드)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| TRANS_CODE | VARCHAR(10) | NOT NULL | 거래코드 (PK) |
| TRANS_NAME | VARCHAR(100) | | 거래명 |
| TRANS_TYPE | CHAR(2) | | 거래구분 |
| USE_YN | CHAR(1) | | 사용여부 |

### 3.8 T_VENDOR_INFO (용역/기기업체)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| VENDOR_CD | CHAR(2) | NOT NULL | 업체코드 (PK) |
| VENDOR_NAME | VARCHAR(100) | | 업체명 |
| VENDOR_TYPE | CHAR(1) | | 업체구분 (기기/용역/보안) |
| PHONE_NO | VARCHAR(50) | | 전화번호 |
| ADDRESS | VARCHAR(255) | | 주소 |
| USE_YN | CHAR(1) | | 사용여부 |

---

## 4. 시스템 테이블

### 4.1 T_USER (사용자) ★

| 컬럼명 | 타입 | NULL | 설명 | 비고 |
|--------|------|------|------|------|
| USER_ID | VARCHAR(20) | NOT NULL | 사용자ID | PK |
| USER_NAME | VARCHAR(50) | | 사용자명 | |
| PASSWORD | VARCHAR(280) | | 비밀번호 | BCrypt 해시 |
| USER_TYPE | CHAR(1) | | 사용자구분 | 관리자/일반/업체 |
| DEPT_CODE | VARCHAR(10) | | 부서코드 | |
| DEPT_NAME | VARCHAR(50) | | 부서명 | |
| POSITION | VARCHAR(20) | | 직위 | |
| EMAIL | VARCHAR(50) | | 이메일 | |
| PHONE_NO | VARCHAR(50) | | 전화번호 | |
| CELL_PHONE_NO | VARCHAR(20) | | 휴대폰 | |
| BRANCH_CODE | CHAR(5) | | 소속지점 | |
| AUTH_CODE | VARCHAR(50) | | 권한코드 | FK |
| LOGIN_FLAG | CHAR(1) | | 로그인상태 | |
| LAST_LOGIN_DATE | CHAR(14) | | 최종로그인일시 | |
| PW_EXPIRE_DATE | CHAR(8) | | 비밀번호만료일 | |
| LOCK_YN | CHAR(1) | | 계정잠금여부 | |
| LOCK_CNT | INT | | 로그인실패횟수 | |
| USE_YN | CHAR(1) | | 사용여부 | |
| REG_DATE | CHAR(14) | | 등록일시 | |
| UPD_DATE | CHAR(14) | | 수정일시 | |

### 4.2 T_ROLE / T_ROLE_AUTH / T_AUTH_MENU (권한)

**T_ROLE (권한)**

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| AUTH_CODE | VARCHAR(50) | NOT NULL | 권한코드 (PK) |
| AUTH_NAME | VARCHAR(100) | | 권한명 |
| DESCRIPTION | VARCHAR(200) | | 설명 |
| USE_YN | CHAR(1) | | 사용여부 |

**T_AUTH_MENU (권한-메뉴 매핑)**

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| AUTH_CODE | VARCHAR(50) | NOT NULL | 권한코드 (PK) |
| MENU_ID | VARCHAR(20) | NOT NULL | 메뉴ID (PK) |
| READ_YN | CHAR(1) | | 조회권한 |
| WRITE_YN | CHAR(1) | | 등록/수정권한 |
| DELETE_YN | CHAR(1) | | 삭제권한 |
| EXCEL_YN | CHAR(1) | | 엑셀다운로드권한 |

### 4.3 T_WEBSITE_MENU (메뉴)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| MENU_ID | VARCHAR(20) | NOT NULL | 메뉴ID (PK) |
| MENU_NAME | VARCHAR(100) | | 메뉴명 |
| PARENT_MENU_ID | VARCHAR(20) | | 상위메뉴ID |
| MENU_URL | VARCHAR(200) | | 메뉴 URL |
| MENU_LEVEL | INT | | 메뉴레벨 (1,2,3) |
| SORT_ORDER | INT | | 정렬순서 |
| ICON | VARCHAR(50) | | 아이콘 |
| USE_YN | CHAR(1) | | 사용여부 |

### 4.4 T_USER_CONN_INFO (접속이력)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| SEQ_NO | BIGINT | NOT NULL | 일련번호 (PK, AUTO) |
| USER_ID | VARCHAR(20) | | 사용자ID |
| CONN_DATE | CHAR(8) | | 접속일 |
| CONN_TIME | CHAR(6) | | 접속시간 |
| LOGOUT_TIME | CHAR(6) | | 로그아웃시간 |
| CLIENT_IP | VARCHAR(15) | | 접속IP |
| CONN_TYPE | CHAR(1) | | 접속구분 (로그인/로그아웃) |

### 4.5 T_NOTICE (공지사항)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| NOTICE_ID | BIGINT | NOT NULL | 공지ID (PK, AUTO) |
| TITLE | VARCHAR(200) | NOT NULL | 제목 |
| CONTENT | TEXT | | 내용 |
| NOTICE_TYPE | CHAR(1) | | 공지구분 |
| VIEW_CNT | INT | | 조회수 |
| TOP_YN | CHAR(1) | | 상단고정여부 |
| REG_USER_ID | VARCHAR(20) | | 등록자ID |
| REG_DATE | CHAR(14) | | 등록일시 |
| UPD_DATE | CHAR(14) | | 수정일시 |
| USE_YN | CHAR(1) | | 사용여부 |

---

## 5. 에이전트 수신 테이블 (저널/트랜잭션)

> 에이전트로부터 실시간 수신되는 데이터. 파티셔닝 대상.

### 5.1 T_TRANSACTION_JOURNAL (거래 저널) ★

> 저널코드: Transaction(71). 가장 데이터량이 많은 테이블.

| 컬럼명 | 타입 | NULL | 설명 | 비고 |
|--------|------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 | PK |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 | PK |
| JNL_DATE | CHAR(8) | NOT NULL | 저널발생일 | PK, 파티션키 |
| JNL_TIME | CHAR(10) | NOT NULL | 저널발생시간 | PK |
| TRANS_TYPE | CHAR(2) | | 거래구분 | |
| TRANS_CODE | VARCHAR(10) | | 거래코드 | |
| RESULT_CODE | CHAR(2) | | 처리결과 | 성공/실패/취소 |
| AMOUNT | BIGINT | | 거래금액 | |
| ACCOUNT_NO | VARCHAR(20) | | 계좌번호 | 마스킹 저장 |
| CARD_NO | VARCHAR(20) | | 카드번호 | 마스킹 저장 |
| BANK_CODE | CHAR(3) | | 은행코드 | |
| CURRENCY_CD | CHAR(3) | | 통화코드 | |
| MEDIA_TYPE | CHAR(1) | | 매체구분 | 통장/카드/무매체 |
| HOST_DATE | CHAR(8) | | HOST 처리일 | |
| HOST_TIME | CHAR(6) | | HOST 처리시간 | |
| HOST_SEQ_NO | VARCHAR(20) | | HOST 거래번호 | |
| ERROR_CODE | VARCHAR(10) | | 에러코드 | |
| DESTROY_FLAG | CHAR(1) | | 2차파기여부 | |
| DESTROY_DATE | CHAR(8) | | 2차파기일 | |

```sql
PRIMARY KEY (BRANCH_CODE, DEVICE_ID, JNL_DATE, JNL_TIME)
-- 월별 파티션
PARTITION BY RANGE (JNL_DATE) (
    PARTITION P_202604 VALUES LESS THAN ('202605'),
    ...
)
-- INDEX
CREATE INDEX IX_TRANSJNL_01 ON T_TRANSACTION_JOURNAL (JNL_DATE, BRANCH_CODE);
CREATE INDEX IX_TRANSJNL_02 ON T_TRANSACTION_JOURNAL (JNL_DATE, DEVICE_ID);
```

### 5.2 T_FAULT_ANALYSIS (장애 분석) ★

> 저널코드: Failure(22). 장애 발생/해제 이력.

| 컬럼명 | 타입 | NULL | 설명 | 비고 |
|--------|------|------|------|------|
| FAULT_SEQ | BIGINT | NOT NULL | 장애일련번호 | PK |
| BRANCH_CODE | CHAR(5) | | 지점코드 | |
| DEVICE_ID | VARCHAR(20) | | 기기번호 | |
| FAULT_DATE | CHAR(8) | | 장애발생일 | 파티션키 |
| FAULT_TIME | CHAR(6) | | 장애발생시간 | |
| CLEAR_DATE | CHAR(8) | | 해제일 | |
| CLEAR_TIME | CHAR(6) | | 해제시간 | |
| FAULT_CODE | VARCHAR(10) | | 장애코드 | |
| FAULT_NAME | VARCHAR(200) | | 장애명 | |
| DEVICE_TYPE | CHAR(2) | | 장치구분 | |
| FAULT_STATUS | CHAR(1) | | 장애상태 | 발생/해제/조치중 |
| FAULT_DURATION | INT | | 장애시간(분) | |
| ACTION_TYPE | CHAR(1) | | 조치구분 | 자동복구/수동/출동 |
| ACTION_USER_ID | VARCHAR(20) | | 조치자ID | |
| ACTION_DATE | CHAR(8) | | 조치일 | |
| ACTION_MEMO | VARCHAR(500) | | 조치내용 | |
| DISPATCH_FLAG | CHAR(1) | | 출동요청여부 | |
| DISPATCH_DATE | CHAR(8) | | 출동요청일 | |
| VENDOR_CD | CHAR(2) | | 출동업체 | |

```sql
PRIMARY KEY (FAULT_SEQ)
CREATE INDEX IX_FAULT_01 ON T_FAULT_ANALYSIS (FAULT_DATE, BRANCH_CODE);
CREATE INDEX IX_FAULT_02 ON T_FAULT_ANALYSIS (DEVICE_ID, FAULT_DATE);
CREATE INDEX IX_FAULT_03 ON T_FAULT_ANALYSIS (FAULT_STATUS);
```

### 5.3 T_ATM_MONITOR (기기 상태 이력)

> 저널코드: Status(21), APStart(11), Open(12)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 (PK) |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 (PK) |
| JNL_DATE | CHAR(8) | NOT NULL | 저널발생일 (PK) |
| JNL_TIME | CHAR(10) | NOT NULL | 저널발생시간 (PK) |
| JNL_TYPE | CHAR(2) | | 저널구분 (11/12/21) |
| ATM_STATUS | CHAR(2) | | 기기상태 |
| OPERATE_MODE | CHAR(1) | | 운영모드 |
| AGENT_VERSION | VARCHAR(32) | | 에이전트버전 |
| OS_INFO | VARCHAR(100) | | OS 정보 |

### 5.4 T_CASSETTE_STATUS (카세트 상태)

> 저널코드: Cash(41). 카세트별 시재 정보.

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 (PK) |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 (PK) |
| CASSETTE_NO | INT | NOT NULL | 카세트번호 (PK) |
| LAST_UPDATE_DATE | CHAR(14) | | 최종갱신일시 |
| DENOM_CODE | VARCHAR(10) | | 권종코드 |
| REMAIN_CNT | BIGINT | | 잔여매수 |
| MAX_CNT | BIGINT | | 최대매수 |
| STATUS | CHAR(1) | | 상태 |

### 5.5 T_ADD_CASH (현금 보충/회수 저널)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 (PK) |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 (PK) |
| JNL_DATE | CHAR(8) | NOT NULL | 저널발생일 (PK) |
| JNL_TIME | CHAR(10) | NOT NULL | 저널발생시간 (PK) |
| ACTION_TYPE | CHAR(1) | NOT NULL | 처리구분 (PK) |
| CASSETTE_TYPE | CHAR(1) | NOT NULL | 카세트구분 (PK) |
| CASSETTE_NO | INT | NOT NULL | 카세트번호 (PK) |
| CURRENCY_CD | CHAR(3) | | 통화코드 |
| DENOM_CODE | VARCHAR(10) | | 권종코드 |
| ADD_CNT | BIGINT | | 추가매수 |
| REMAIN_CNT | BIGINT | | 추가후보유매수 |

### 5.6 T_DAY_TOTAL (마감 집계) ★

> 저널코드: Total(31). 일일 마감 합계.

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 (PK) |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 (PK) |
| TOTAL_DATE | CHAR(8) | NOT NULL | 마감일 (PK) |
| TOTAL_TIME | CHAR(6) | | 마감시간 |
| TOTAL_TYPE | CHAR(1) | | 마감구분 |
| DEPOSIT_CNT | INT | | 입금건수 |
| DEPOSIT_AMT | BIGINT | | 입금금액 |
| WITHDRAW_CNT | INT | | 출금건수 |
| WITHDRAW_AMT | BIGINT | | 출금금액 |
| TRANSFER_CNT | INT | | 이체건수 |
| TRANSFER_AMT | BIGINT | | 이체금액 |
| TOTAL_CNT | INT | | 총건수 |
| TOTAL_AMT | BIGINT | | 총금액 |
| APPROVAL_STATUS | CHAR(1) | | 결재상태 |
| APPROVAL_USER | VARCHAR(20) | | 결재자 |
| APPROVAL_DATE | CHAR(14) | | 결재일시 |

### 5.7 T_OPERATION_JOURNAL (운영모드 전환)

> 저널코드: Operation(52)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 (PK) |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 (PK) |
| JNL_DATE | CHAR(8) | NOT NULL | 저널발생일 (PK) |
| JNL_TIME | CHAR(10) | NOT NULL | 저널발생시간 (PK) |
| BEFORE_MODE | CHAR(1) | | 변경전모드 |
| AFTER_MODE | CHAR(1) | | 변경후모드 |
| CHANGE_TYPE | CHAR(1) | | 변경구분 (자동/수동/원격) |

---

## 6. 집계/보고서 테이블

### 6.1 T_ATM_RUN_HISTORY (기기 가동률)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 (PK) |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 (PK) |
| RUN_DATE | CHAR(8) | NOT NULL | 일자 (PK) |
| TOTAL_TIME | INT | | 전체운영시간(분) |
| FAULT_TIME | INT | | 장애시간(분) |
| AVAIL_RATE | DECIMAL(5,2) | | 가동률(%) |
| FAULT_CNT | INT | | 장애발생건수 |

### 6.2 T_DAILY_REPORT (일일 보고서)

> 새마을금고는 7개 보고서 테이블 분리. NH는 통합 검토.

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| REPORT_DATE | CHAR(8) | NOT NULL | 보고서일자 (PK) |
| REPORT_TYPE | CHAR(2) | NOT NULL | 보고서구분 (PK) |
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 (PK) |
| DEVICE_ID | VARCHAR(20) | | 기기번호 |
| (항목별 컬럼) | | | 보고서 유형별 상이 | |

---

## 7. 기타 업무 테이블

### 7.1 T_JOURNAL (저널 원본)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BRANCH_CODE | CHAR(5) | NOT NULL | 지점코드 (PK) |
| DEVICE_ID | VARCHAR(20) | NOT NULL | 기기번호 (PK) |
| JNL_DATE | CHAR(8) | NOT NULL | 저널발생일 (PK) |
| JNL_TIME | CHAR(10) | NOT NULL | 저널발생시간 (PK) |
| JNL_TYPE | CHAR(2) | | 저널구분 (11/12/21/22/31/41/52/71) |
| RAW_DATA | TEXT | | 원본 데이터 |
| PARSED_FLAG | CHAR(1) | | 파싱완료여부 |
| REG_DATE | CHAR(14) | | 등록일시 |

### 7.2 T_REMOTE_COMMAND (원격제어 이력)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| CMD_SEQ | BIGINT | NOT NULL | 일련번호 (PK) |
| BRANCH_CODE | CHAR(5) | | 지점코드 |
| DEVICE_ID | VARCHAR(20) | | 기기번호 |
| CMD_TYPE | CHAR(2) | | 명령구분 (재부팅/모드변경 등) |
| CMD_DATE | CHAR(14) | | 명령일시 |
| RESULT | CHAR(1) | | 결과 (성공/실패) |
| RESULT_DATE | CHAR(14) | | 결과수신일시 |
| REG_USER_ID | VARCHAR(20) | | 실행자 |

### 7.3 T_BATCH (배치 관리)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BATCH_ID | VARCHAR(20) | NOT NULL | 배치ID (PK) |
| BATCH_NAME | VARCHAR(100) | | 배치명 |
| SCHEDULE | VARCHAR(50) | | 스케줄 (CRON) |
| LAST_RUN_DATE | CHAR(14) | | 최종실행일시 |
| LAST_RESULT | CHAR(1) | | 최종결과 |
| USE_YN | CHAR(1) | | 사용여부 |

### 7.4 T_BATCH_HISTORY (배치 실행 이력)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| BATCH_SEQ | BIGINT | NOT NULL | 일련번호 (PK) |
| BATCH_ID | VARCHAR(20) | | 배치ID |
| START_DATE | CHAR(14) | | 시작일시 |
| END_DATE | CHAR(14) | | 종료일시 |
| RESULT | CHAR(1) | | 결과 |
| PROC_CNT | INT | | 처리건수 |
| ERROR_MSG | VARCHAR(500) | | 오류메시지 |

### 7.5 T_ATTACHED_FILE (첨부파일)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| FILE_ID | BIGINT | NOT NULL | 파일ID (PK) |
| REF_TYPE | VARCHAR(20) | | 참조구분 (NOTICE/FAULT 등) |
| REF_ID | VARCHAR(50) | | 참조ID |
| FILE_NAME | VARCHAR(200) | | 원본파일명 |
| STORED_NAME | VARCHAR(200) | | 저장파일명 |
| FILE_PATH | VARCHAR(500) | | 저장경로 |
| FILE_SIZE | BIGINT | | 파일크기 |
| REG_DATE | CHAR(14) | | 등록일시 |

---

## 8. 테이블 요약 및 우선순위

### 1단계: 기본 인프라 (시스템 관리 화면 구현 시)

| # | 테이블 | 설명 |
|---|--------|------|
| 1 | T_USER | 사용자 |
| 2 | T_ROLE | 권한 |
| 3 | T_AUTH_MENU | 권한-메뉴 |
| 4 | T_WEBSITE_MENU | 메뉴 |
| 5 | T_CODE_GROUP | 코드그룹 |
| 6 | T_CODE | 공통코드 |
| 7 | T_USER_CONN_INFO | 접속이력 |

### 2단계: 마스터 데이터 (디리아 이관 후)

| # | 테이블 | 설명 |
|---|--------|------|
| 8 | T_BRANCH | 조직/지점 |
| 9 | T_CORNER | 코너 |
| 10 | T_ATM | 기기 |
| 11 | T_ATM_MODEL | 기기모델 |
| 12 | T_FAULT_MAP | 장애코드 |
| 13 | T_TRANSACTION_CODE | 거래코드 |
| 14 | T_VENDOR_INFO | 업체 |

### 3단계: 에이전트 수신 (에이전트 서버 구축 후)

| # | 테이블 | 설명 |
|---|--------|------|
| 15 | T_JOURNAL | 저널 원본 |
| 16 | T_TRANSACTION_JOURNAL | 거래 저널 |
| 17 | T_FAULT_ANALYSIS | 장애 분석 |
| 18 | T_ATM_MONITOR | 기기 상태 |
| 19 | T_CASSETTE_STATUS | 카세트 상태 |
| 20 | T_ADD_CASH | 현금 보충/회수 |
| 21 | T_DAY_TOTAL | 마감 집계 |
| 22 | T_OPERATION_JOURNAL | 운영모드 |

### 4단계: 업무 기능

| # | 테이블 | 설명 |
|---|--------|------|
| 23 | T_NOTICE | 공지사항 |
| 24 | T_ATTACHED_FILE | 첨부파일 |
| 25 | T_REMOTE_COMMAND | 원격제어 |
| 26 | T_ATM_RUN_HISTORY | 가동률 |
| 27 | T_DAILY_REPORT | 보고서 |
| 28 | T_BATCH / T_BATCH_HISTORY | 배치 |

---

## 9. 주의사항

### 9.1 NH 은행 vs 새마을금고 차이점

| 항목 | 새마을금고 | NH (예상) |
|------|----------|----------|
| 조직 체계 | 영업점(TERM_GROUP_ID) + 본지점(BRANCH_CD) | 지역본부 > 사무소 > 코너 |
| 기기번호 체계 | TERM_GROUP_ID + TERM_ID | BRANCH_CODE + DEVICE_ID |
| DB | MySQL 9.5 | Oracle 또는 MySQL (미정) |
| 기기수 | ~3,500대 | ~15,000대 (예상) |

### 9.2 디리아 협의 시 확인 사항

- [ ] DB 종류 확정 (Oracle인지 MySQL인지)
- [ ] 조직 코드 체계 (지역본부 - 사무소 - 코너 계층 구조)
- [ ] 기기번호 체계 (자릿수, 규칙)
- [ ] 계좌번호/카드번호 마스킹 규칙
- [ ] 데이터 보관 기간 (파티션 주기)
- [ ] 기존 테이블 구조 DDL 제공 가능 여부

### 9.3 보안 관련

- 비밀번호: 반드시 BCrypt 해시 저장 (평문 저장 금지)
- 계좌번호/카드번호: 마스킹 저장 (`****-****-1234`)
- 개인정보 컬럼: 암호화 저장 고려
- MyBatis에서 `#{}` 파라미터 바인딩 사용 (`${}` 금지 → SQL Injection 방지)
