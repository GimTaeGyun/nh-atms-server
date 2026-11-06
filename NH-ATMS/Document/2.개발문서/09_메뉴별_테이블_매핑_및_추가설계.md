# NH-ATMS 메뉴별 테이블 매핑 및 추가 테이블 설계

> 작성일: 2026-04-27
> 목적: AS-IS 메뉴 분석 + AS-IS 테이블 분석(ERD 이미지 1-1-1~2-2-3) + TO-BE 메뉴 구조를 종합하여,
>       **TO-BE 메뉴별로 어떤 테이블이 필요한지** 매핑하고, **이미 만든 공통 + 저널 테이블 외에 추가로 설계할 테이블**을 정의한다.
> 참고 문서: `0.기본자료/농협은행/NH AS-IS메뉴 분석.md`, `0.기본자료/농협은행/1.DB/NH AS-IS 테이블 분석.md`,
>            `2.개발문서/04_DB스키마설계.md`, `2.개발문서/07_to-be메뉴.md`
> 마이그레이션: `2.개발문서/sql/V1.0.0 ~ V1.2.3`

---

## 1. 현재까지 생성된 테이블 (유지 대상 — 손대지 않음)

> 공통/인증 + 저널 + 마스터 + 저널상세까지 V1.0.0 ~ V1.2.3 에서 생성 완료. 본 설계는 이 위에 **추가**만 한다.

### 1.1 공통/인증/시스템 (V1.0.0)
`T_USER_GROUP`, `T_USER`, `T_AUTH`, `T_USER_GROUP_AUTH`, `T_MENU`, `T_AUTH_MENU`, `T_LOGIN_HIST`, `T_USER_AUTH_HIST`

### 1.2 저널 기본 (V1.0.2 / V1.0.6 / V1.0.9)
`T_JOURNAL`, `T_ATMMONITOR`, `T_TRANSACTIONJOURNAL`, `T_OPERATIONJOURNAL`,
`T_JOURNAL_DATA`, `T_JOURNAL_ERROR`, `T_JOURNAL_FOOTNOTE`, `T_JOURNAL_IMAGE`, `T_JNLIMAGE_HISTORY`,
`T_JOURNALDATA`, `T_JOURNALIMAGE`

### 1.3 집계/배치 (V1.0.2 / V1.1.1 / V1.1.2)
`T_DAYTOTAL`, `T_ADDCASH`, `T_ADDCHECK`, `T_BATCHJOB`, `T_BATCHHISTORY`,
`T_TRANSACTIONDAILYTOTAL`, `T_FAULTRATEMANAGEMENT`, `T_OPERATIONRATEMANAGEMENT`

### 1.4 에이전트/상태/인터페이스 (V1.1.0 / V1.1.3~V1.1.8)
`T_AGENTSTATUS`, `T_ATMUNITSTATUS`, `T_GWINTERFACE`, `T_SMSSENDDATA`,
`T_NETSENDDATA`, `T_NETRECVDATA`, `T_FAULTMANAGEMENT`, `T_IF_QUEUE`

### 1.5 마스터 (V1.2.0 / V1.2.1)
`T_BRANCH`, `T_CORNER`, `T_CODE`, `T_CODEGROUP`, `T_NOTICE`, `T_ATMGROUP`, `T_ATM`, `T_ATMSTATUS`

### 1.6 저널 상세 (V1.2.2)
`T_CASHTRANSACTION`, `T_CHECKTRANSACTION`, `T_CASSETTESTATUS`, `T_FAULTANALYSIS`, `T_ATMSTOPHISTORY`

---

## 2. TO-BE 메뉴별 테이블 매핑

> 범례: ✅ 이미 생성 / 🆕 추가 설계 필요(본 문서) / ⏳ 후순위
> AS-IS 거대 테이블은 `04_DB스키마설계.md` 4.1 정규화 방침에 따라 공통 + 유형별 상세로 분리한다.

### 2.1 공통

| 메뉴ID | 메뉴명 | 필요 테이블 | 상태 |
|------|------|-----------|------|
| - | 로그인 | `T_USER`, `T_LOGIN_HIST` | ✅ |
| - | 대시보드 | `T_ATMMONITOR`, `T_ATMSTATUS`, `T_FAULTANALYSIS` | ✅ |
| 3100 | 장애상황 | `T_ATMSTATUS`, `T_FAULTANALYSIS`, `T_ATM`, `T_ERRORMESSAGE` 🆕 | 일부 🆕 |
| 7001 | 업무연락 | `T_NOTICE`, `T_FILEATTACH` 🆕 | 일부 🆕 |

### 2.2 장애관리

| 메뉴ID | 메뉴명 | 필요 테이블 | 상태 |
|------|------|-----------|------|
| 3101 | 장애내역 조회 | `T_FAULTANALYSIS`, `T_ATM`, `T_CORNER`, `T_ERRORMESSAGE` 🆕 | 일부 🆕 |
| 6000 | 기기상태조회 | `T_ATMMONITOR`, `T_ATMSTATUS`, `T_CASSETTESTATUS` | ✅ |
| 6001 | 기기상태이력조회 | `T_ATMSTATUS`, `T_ATMSTOPHISTORY` | ✅ |
| 6003 | 개국및거래현황조회 | `T_ATMMONITOR`, `T_TRANSACTIONDAILYTOTAL`, `T_OPENCLOSESTATUS` 🆕 | 일부 🆕 |
| 2301 | 장애명세표 조회 | `T_FAULTANALYSIS`, `T_ERRORMESSAGE` 🆕, `T_SLIPFORMAT` 🆕 | 🆕 |
| 6002 | 업체출동현황조회 | `T_DISPATCHHISTORY` 🆕, `T_VENDOR` 🆕, `T_ATM` | 🆕 |
| 6004 | 2차출동현황조회 | `T_DISPATCHHISTORY` 🆕, `T_VENDOR` 🆕 | 🆕 |

### 2.3 저널관리

| 메뉴ID | 메뉴명 | 필요 테이블 | 상태 |
|------|------|-----------|------|
| 4000 | 저널조회 | `T_JOURNAL`, `T_JOURNALDATA`, `T_JOURNALIMAGE` | ✅ |
| 4200 | 저널처리 일/월별 건수(A) | `T_TRANSACTIONDAILYTOTAL`, `T_TRANSACTIONMONTHLYTOTAL` 🆕 | 일부 🆕 |
| 4104 | 저널처리유형집계(A) | `T_JOURNALPROCESSTOTAL` 🆕 | 🆕 |
| 4105 | 저널 운영현황(A) | `T_JOURNALPROCESSTOTAL` 🆕, `T_TRANSACTIONDAILYTOTAL` | 일부 🆕 |
| 4202 | 운영모드 운영률(A) | `T_OPERATIONRATEMANAGEMENT`, `T_TRANSACTIONHOURLYTOTAL` 🆕 | 일부 🆕 |
| 4003 | 저널이미지 열람기록관리 | `T_JNLIMAGE_HISTORY`, `T_JOURNALVIEWLOG` 🆕 | 일부 🆕 |
| 4101 | 운영환경 설정(A) | `T_SYSTEMCONFIG` 🆕 | 🆕 |

### 2.4 기기관리

| 메뉴ID | 메뉴명 | 필요 테이블 | 상태 |
|------|------|-----------|------|
| 3204 | 자동화기기 위치조회 | `T_ATM`, `T_CORNER`(주소/좌표) | ✅ |
| 3202 | 자동화기기 자산현황 | `T_ATM`, `T_ATMMODEL` 🆕, `T_VENDOR` 🆕 | 일부 🆕 |
| 7205 | 기기원격지시(매체배포) | `T_REMOTECOMMAND` 🆕, `T_ATM` | 🆕 |
| 6302 | 자동화기기설치코너관리 | `T_CORNER`, `T_ATM`, `T_VENDOR` 🆕 | 일부 🆕 |
| 6303 | 용역업체등록관리 | `T_VENDOR` 🆕, `T_VENDORATM` 🆕 | 🆕 |

### 2.5 시재관리

| 메뉴ID | 메뉴명 | 필요 테이블 | 상태 |
|------|------|-----------|------|
| 4001 | 저널업무 개시현황 | `T_JOURNAL`, `T_OPENCLOSESTATUS` 🆕 | 일부 🆕 |
| 1300 | 기기시재조회/전거래 | `T_TRANSACTIONJOURNAL`, `T_CASHTRANSACTION` | ✅ |
| 1001 | 입금/출금 거래 | `T_TRANSACTIONJOURNAL`, `T_CASHTRANSACTION` | ✅ |
| 1003 | 공과금/납부 거래 | `T_GIROTRANSACTION` 🆕 | 🆕 |
| 1004 | 신용카드 거래 | `T_TRANSACTIONJOURNAL`(카드) | ✅ |
| 1005 | 수표입출금 거래 | `T_CHECKTRANSACTION` | ✅ |
| 1006 | 무통장/무카드 거래 | `T_TRANSACTIONJOURNAL` | ✅ |
| 1007 | 통장 기장 거래 | `T_PASSBOOKTRANSACTION` 🆕 | 🆕 |
| 1008 | 외화 입/출금 거래 | `T_FOREXTRANSACTION` 🆕 | 🆕 |
| 2101 | 집계내역/합계상태 조회 | `T_DAYTOTAL`, `T_TRANSACTIONDAILYTOTAL` | ✅ |
| 2102 | 외화합계 상태 조회 | `T_FOREXTOTAL` 🆕 | 🆕 |
| 3200 | 자동화기기 시재현황 | `T_CASSETTESTATUS`, `T_ATMSTATUS` | ✅ |
| 3207 | STM 매체현황 조회 | `T_CASSETTESTATUS` | ✅ |
| 3208 | 외화/통화 시재현황 | `T_CASSETTESTATUS`(외화), `T_FOREXTOTAL` 🆕 | 일부 🆕 |
| 3209 | 외화ATM 시재확인 | `T_CASSETTESTATUS`(외화), `T_FOREXTOTAL` 🆕 | 일부 🆕 |
| 1020/1301 | 거래내역 2차 파기 조회 | `T_TRANSACTIONJOURNAL`, `T_PERSONALINFOPRINTHIST` 🆕 | 🆕(감사) |

### 2.6 정보관리

| 메뉴ID | 메뉴명 | 필요 테이블 | 상태 |
|------|------|-----------|------|
| 3206 | 지역본부 담당자 조회 | `T_PERSON` 🆕, `T_BRANCH` | 일부 🆕 |
| 4002 | 저널이미지 권한 관리 | `T_JOURNALAUTH` 🆕, `T_JOURNALAUTHHIST` 🆕 | 🆕 |
| 7100 | 시스템 사용권한부여(A) | `T_USER`, `T_AUTH`, `T_PERSON` 🆕 | 일부 🆕 |
| 7201 | 자동화기기 코드관리(A) | `T_CODE`, `T_CODEGROUP` | ✅ |
| 7203 | 에러메세지 관리(A) | `T_ERRORMESSAGE` 🆕 | 🆕 |

---

## 3. 추가 설계 테이블 목록 (본 설계 산출물)

> 우선순위/주제별로 마이그레이션 파일을 분리한다. 모두 기존 컨벤션(Oracle, `T_` 접두어,
> `TERM_GROUP_ID CHAR(5)`/`TERM_ID VARCHAR2(20)`, 컬럼 한글 COMMENT, 인라인 KEY 금지 → `CREATE INDEX`)을 따른다.

### 3.1 V1.2.4 — 통계/집계 보강
| 테이블 | 한글명 | 근거 메뉴 |
|------|------|---------|
| `T_TRANSACTIONHOURLYTOTAL` | 시간별 거래통계 | 4202 |
| `T_TRANSACTIONMONTHLYTOTAL` | 월별 거래통계 | 4200 |
| `T_JOURNALPROCESSTOTAL` | 저널처리유형별 집계 | 4104, 4105 |
| `T_OPENCLOSESTATUS` | 개국/마감 현황 | 4001, 6003 |

### 3.2 V1.2.5 — 마스터 보강
| 테이블 | 한글명 | 근거 메뉴 |
|------|------|---------|
| `T_ATMMODEL` | 기기모델 마스터 | 3202 |
| `T_VENDOR` | 용역업체 마스터 | 6303, 6002, 6004, 3202 |
| `T_VENDORATM` | 용역업체-기기 매핑 | 6303, 6302 |
| `T_ERRORMESSAGE` | ATM 에러메시지 마스터 | 7203, 7002, 2301, 3101 |
| `T_HOLIDAY` | 휴일 마스터 | (마감/통계 공통) |
| `T_FEE` | 출금수수료 마스터 | (거래/정산 공통) |
| `T_DISPATCHHISTORY` | 출동조치 이력 | 6002, 6004 |

### 3.3 V1.2.6 — 인사/권한/시스템설정/감사
| 테이블 | 한글명 | 근거 메뉴 |
|------|------|---------|
| `T_PERSON` | 인사정보 마스터 | 3206, 7100 |
| `T_JOURNALAUTH` | 저널열람권한 | 4002 |
| `T_JOURNALAUTHHIST` | 저널권한 변경이력 | 4002 |
| `T_SYSTEMCONFIG` | 시스템 공통설정 | 4101, 3002 |
| `T_JOURNALVIEWLOG` | 거래내역 열람 감사로그 | 4003 |
| `T_PERSONALINFOPRINTHIST` | 개인정보 출력 감사로그 | 1020, 1301 |
| `T_DATADELETEHIST` | 데이터삭제 이력 | (법적 의무) |

### 3.4 V1.2.7 — 거래상세 보강 (정규화 분리)
| 테이블 | 한글명 | 근거 메뉴 |
|------|------|---------|
| `T_GIROTRANSACTION` | 지로/공과금 거래상세 | 1003 |
| `T_FOREXTRANSACTION` | 외화 거래상세 | 1008 |
| `T_PASSBOOKTRANSACTION` | 통장기장 거래상세 | 1007 |
| `T_FOREXTOTAL` | 외화 합계 | 2102, 3208, 3209 |
| `T_FILEATTACH` | 통합 첨부파일 | 7001, 3100 |
| `T_SLIPFORMAT` | 명세표 양식 설정 | 2301 |
| `T_REMOTECOMMAND` | 기기 원격지시 이력 | 7205 |

---

## 4. 정규화 방침 (AS-IS 거대 테이블 → TO-BE)

`04_DB스키마설계.md` 4.1 을 따른다.

| AS-IS 테이블 | TO-BE 분리 |
|-------------|-----------|
| 자동화기기_거래 (60+컬럼) | `T_TRANSACTIONJOURNAL`(공통) ✅ + `T_CASHTRANSACTION` ✅ + `T_CHECKTRANSACTION` ✅ + `T_GIROTRANSACTION` 🆕 + `T_FOREXTRANSACTION` 🆕 + `T_PASSBOOKTRANSACTION` 🆕 |
| 자동화기기_상태 (70+컬럼) | `T_ATMSTATUS`(기기상태) ✅ + `T_CASSETTESTATUS`(시재) ✅ |
| 자동화기기_사무소등록관리 (70+컬럼) | `T_BRANCH` ✅ + `T_CORNER` ✅ |
| 자동화기기_업무처리로그 (60+컬럼) | `T_JOURNAL` ✅ + `T_JOURNALDATA` ✅ |
| 일/시간/월별 거래통계 | `T_TRANSACTIONDAILYTOTAL` ✅ + `T_TRANSACTIONHOURLYTOTAL` 🆕 + `T_TRANSACTIONMONTHLYTOTAL` 🆕 |

---

## 5. 적용 순서

```
V1.2.3 (현재 최신)
   └─ V1.2.4  통계/집계 보강
        └─ V1.2.5  마스터 보강 (T_VENDOR → T_VENDORATM 순서 의존)
             └─ V1.2.6  인사/권한/시스템설정/감사
                  └─ V1.2.7  거래상세 보강
```

> 모든 신규 테이블은 기존 테이블을 **변경하지 않으며**, 신규 CREATE 만 수행한다.
> FK 는 운영 편의상 논리적 참조만 정의하고 물리 제약은 생략(저널 적재 성능 보호) — 기존 테이블 방침과 동일.
