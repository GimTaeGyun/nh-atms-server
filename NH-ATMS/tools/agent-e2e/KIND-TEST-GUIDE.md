# 저널 유형별 단독 테스트 가이드 (jnl_kind 11·12·21·22·31·41·52·71)

- 목적: 유형별로 저널 1건을 보내고 **수신 → 파싱 → DB(유형별 테이블) → ES/Kibana** 가
  어디까지 정상 진행되는지 단계별 확인. 결과는 하단 기록표에 채워 PPT 로 옮긴다.
- 소요: 유형당 ~5분

---

## 0. 준비 (한 번만)

```powershell
cd D:\0.atec\2.Project\25.NH-ATMS\2.Project\1.Project\tools\agent-e2e

# 서버 살아있는지
curl.exe "http://192.168.28.21:19082/atms/agent/ping.do?atmid=00722447"   # {"succ":true...}
```

**유형별 전용 단말번호 규칙** — 어느 테이블에서든 내 테스트 데이터만 골라내기 위해
`00722 + 9 + kind` 를 쓴다:

| kind | AtmId | | kind | AtmId |
|---|---|---|---|---|
| 11 | `00722911` | | 31 | `00722931` |
| 12 | `00722912` | | 41 | `00722941` |
| 21 | `00722921` | | 52 | `00722952` |
| 22 | `00722922` | | 71 | `00722971` |

→ 모든 DB 확인 SQL 은 `TERM_GROUP_ID='00722' AND TERM_ID='9XX'` 로 조회하면 된다.

**공통 확인 명령 3종** (매 유형마다 반복 사용):

```powershell
# ① 서버 처리 로그 (최근 2분 — dispatch 실패 여부가 핵심)
ssh atec@192.168.28.21 "docker logs nh-atms-journal --since 2m 2>&1 | grep -aE 'stored=false|dispatch failed|Updates: [0-9]'" 

# ② ES 색인 확인 (termId 를 유형별 번호로)
Invoke-RestMethod "http://192.168.28.21:19200/atms-journal-*/_search?q=termId:971&size=1&sort=receivedAt:desc" | 
  ForEach-Object { $_.hits.hits[0]._source | ConvertTo-Json -Depth 5 }
```

③ DB 는 각 유형 섹션의 SQL 을 SQL Developer(또는 DBeaver)에서 실행. 접속: `192.168.28.21:1521/ATMSPDB`, ATMS 계정.

**공통 판정 기준** (모든 유형 동일):
- [ ] 전송 결과 `성공 1` (HTTP 200 + ##emsg## 공백)
- [ ] 서버 로그에 `dispatch failed` / `stored=false` **없음**
- [ ] `T_JOURNAL` 에 1행 (공통 헤더 — 모든 유형의 1차 관문)
- [ ] `T_JOURNAL_DATA` 에 원본 JSON 1행
- [ ] `T_AGENT_STATUS.LAST_JNL_DATE/TIME` 갱신
- [ ] ES 문서 1건 (Kibana Discover 에서 body 필드 파싱 확인)
- [ ] **유형별 테이블** (아래 각 섹션) 에 행 생성

```sql
-- 공통 관문 확인 (9XX 를 유형 번호로 교체)
SELECT * FROM T_JOURNAL      WHERE TERM_GROUP_ID='00722' AND TERM_ID='971' ORDER BY RECV_DATE DESC, RECV_TIME DESC;
SELECT * FROM T_JOURNAL_DATA WHERE TERM_GROUP_ID='00722' AND TERM_ID='971';
SELECT LAST_JNL_DATE, LAST_JNL_TIME FROM T_AGENT_STATUS WHERE TERM_GROUP_ID='00722' AND TERM_ID='971';
-- 실패했다면 여기로 빠져 있다:
SELECT * FROM T_JOURNAL_ERROR WHERE ATM_ID LIKE '007229%' ORDER BY SEQ DESC;
```

---

## 1. kind 11 — AP 시작

AP 프로그램 기동 통보. 디바이스별 S/W 버전이 전달된다.

```powershell
.\Send-TestJournal.ps1 -Kind 11 -AtmId 00722911
```

| 확인 | 내용 |
|---|---|
| 핸들러 | OperationJournalHandler (11) |
| 유형별 테이블 | `T_ATM_UNIT_STATUS` — 디바이스별 SP/EP 버전 갱신 |

```sql
SELECT DEVICE_ID, SP_VERSION, EP_VERSION FROM T_ATM_UNIT_STATUS
 WHERE TERM_GROUP_ID='00722' AND TERM_ID='911';
```
- [ ] 버전값이 저널의 ver1/ver2(AP-1.0.0-TEST 등)로 갱신

## 2. kind 12 — 서비스오픈(개국)

기기 개/폐국. **가장 많은 테이블을 만지는 저널** — 기기 신규 등록까지 일어난다.

```powershell
.\Send-TestJournal.ps1 -Kind 12 -AtmId 00722912
```

| 확인 | 내용 |
|---|---|
| 핸들러 | OperationJournalHandler (12) |
| 유형별 테이블 | `T_ATM`(기기 등록/갱신), `T_ATM_HISTORY`, `T_ATM_MONITOR`, `T_ATM_STATUS`, `T_ATM_DEVICE_STATUS` (+`T_BRANCH`/`T_ATM_GROUP` 미존재 시 생성) |

```sql
SELECT * FROM T_ATM        WHERE TERM_GROUP_ID='00722' AND TERM_ID='912';
SELECT * FROM T_ATM_STATUS WHERE TERM_GROUP_ID='00722' AND TERM_ID='912';
SELECT * FROM T_ATM_MONITOR WHERE TERM_GROUP_ID='00722' AND TERM_ID='912' ORDER BY REG_DT DESC;
```
- [ ] 미등록 기기였는데 T_ATM 에 신규 행 생성됨 (개국의 핵심 동작)

## 3. kind 21 — 상태저널

기기 상태 변화(정상 계열). 카세트 현황이 **삭제 후 재생성**된다.

```powershell
.\Send-TestJournal.ps1 -Kind 21 -AtmId 00722921
```

| 확인 | 내용 |
|---|---|
| 핸들러 | AtmMonitorJournalHandler (21) |
| 유형별 테이블 | `T_ATM_MONITOR`(상태 00/02/05만 적재 — 가드), `T_ATM_UNIT_STATUS`(디바이스 상태), `T_CASSETTE_STATUS`(재생성), `T_ATM_STATUS`(디바이스별 상태 컬럼) |

```sql
SELECT CASSETTE_TYPE, CASSETTE_NO, DENOM_CD, REMAIN_COUNT FROM T_CASSETTE_STATUS
 WHERE TERM_GROUP_ID='00722' AND TERM_ID='921';
SELECT SERVICE_STATUS, CDU_STATUS, ERR_GROUP_CD FROM T_ATM_STATUS
 WHERE TERM_GROUP_ID='00722' AND TERM_ID='921';
```
- [ ] 카세트 4행(논리2+물리2), 잔량 = 저널 값
- [ ] **한 번 더 보내도** 카세트가 중복되지 않고 갱신됨 (지난번 수정한 MERGE 검증)

## 4. kind 22 — 장애저널

21과 동일 구조 + 장애코드. **후속 연계(IF큐) 적재까지** 일어나는 유일한 저널.

```powershell
.\Send-TestJournal.ps1 -Kind 22 -AtmId 00722922
```

| 확인 | 내용 |
|---|---|
| 핸들러 | AtmMonitorJournalHandler (22) |
| 유형별 테이블 | 21의 테이블 + `T_FAULT_ANALYSIS`(장애분석), `T_ATM_STOP_HISTORY`, **`T_IF_QUEUE`(대외출동+SMS+메신저 3행, 상태 W)** |

```sql
SELECT * FROM T_FAULT_ANALYSIS WHERE TERM_GROUP_ID='00722' AND TERM_ID='922';
SELECT IF_TYPE, MSG_TYPE, STATUS FROM T_IF_QUEUE ORDER BY REG_DT DESC FETCH FIRST 5 ROWS ONLY;
SELECT FAULT_ARISED_DATE, FAULT_ARISED_TIME, ERR_CD FROM T_ATM_STATUS
 WHERE TERM_GROUP_ID='00722' AND TERM_ID='922';
```
- [ ] T_IF_QUEUE 에 3행(상태 W) — ※ IF큐 워커(`IF_QUEUE_WORKER_ENABLED`)가 기본 off 라 W로 남는 것이 정상.
  워커를 켜면 atms-if-external/internal 로 실제 발송 시도까지 확인 가능 (2차 테스트)
- [ ] FAULT_ARISED_DATE/TIME 기록

## 5. kind 31 — 마감저널

합계/소계. 항목코드별 집계값이 host/atm 두 계열로 적재.

```powershell
.\Send-TestJournal.ps1 -Kind 31 -AtmId 00722931
```

| 확인 | 내용 |
|---|---|
| 핸들러 | DayTotalJournalHandler (31) |
| 유형별 테이블 | `T_DAY_TOTAL` (host 3행 + atm 2행 — 테스트 데이터 기준) |

```sql
SELECT TOTAL_SOURCE, ITEM_CD, ITEM_VALUE FROM T_DAY_TOTAL
 WHERE TERM_GROUP_ID='00722' AND TERM_ID='931' ORDER BY TOTAL_SOURCE, ITEM_CD;
```
- [ ] 항목코드 1010/1020/1030(host) + 2010/2020(atm), 값이 저널과 일치

## 6. kind 41 — 시재저널

시재 보충/회수. 현금·수표 보충 내역 + 카세트 잔량 갱신.

```powershell
.\Send-TestJournal.ps1 -Kind 41 -AtmId 00722941
```

| 확인 | 내용 |
|---|---|
| 핸들러 | CashJournalHandler (41) |
| 유형별 테이블 | `T_ADD_CASH`(논리+물리), `T_ADD_CHECK`, `T_CASSETTE_STATUS`(잔량 upsert), `T_ATM_STATUS`(최종시재 touch) |

```sql
SELECT * FROM T_ADD_CASH  WHERE TERM_GROUP_ID='00722' AND TERM_ID='941';
SELECT * FROM T_ADD_CHECK WHERE TERM_GROUP_ID='00722' AND TERM_ID='941';
```
- [ ] **연속 2회 전송해도 성공** (카세트 MERGE blank-pad 수정 검증 — 수정 전엔 2번째부터 무조건 실패했음)

## 7. kind 52 — 운영저널

운영자 조작 기록 (op_cd + 내용 텍스트).

```powershell
.\Send-TestJournal.ps1 -Kind 52 -AtmId 00722952
```

| 확인 | 내용 |
|---|---|
| 핸들러 | OperationJournalHandler (52) |
| 유형별 테이블 | `T_OPERATION_JOURNAL` (※ 11/12는 여기 기록 안 함 — AS-IS 정합으로 수정됨) |

```sql
SELECT OP_CD, OP_ACT FROM T_OPERATION_JOURNAL
 WHERE TERM_GROUP_ID='00722' AND TERM_ID='952' ORDER BY REG_DT DESC;
```

## 8. kind 71 — 거래저널

거래(출금/입금/이체). 가장 중요한 저널 — 가상 데이터 파라미터로 값 추적이 쉽다.

```powershell
.\Send-TestJournal.ps1 -Kind 71 -AtmId 00722971 -Bank 088 -Amount 770000 -Name 테스트71 -WithReceipt
```

| 확인 | 내용 |
|---|---|
| 핸들러 | TransactionJournalHandler (71) |
| 유형별 테이블 | `T_TRANSACTION_JOURNAL`(거래), `T_CASH_TRANSACTION`(권종별), `T_CASSETTE_STATUS`, `T_ATM_STATUS` |

```sql
SELECT BANK_CD, ACCOUNT_NO, TR_AMOUNT, CUSTOMER_NM, TR_TYPE FROM T_TRANSACTION_JOURNAL
 WHERE TERM_GROUP_ID='00722' AND TERM_ID='971' ORDER BY JNL_DATE DESC, JNL_TIME DESC;
```
- [ ] BANK_CD=**088** (3자리 정규화 — bank3 수정 검증: 전문은 4자리 `0088`로 감), 금액 770000, 고객명 테스트71
- [ ] `-WithReceipt` 명세표가 T_JOURNAL_DATA 원본 JSON 의 extra.명세표 에 포함

---

## 결과 기록표 (PPT 용)

| kind | 유형 | 전송 | T_JOURNAL | 유형별 테이블 | ES/Kibana | 비고 |
|---|---|---|---|---|---|---|
| 11 | AP시작 | | | T_ATM_UNIT_STATUS: | | |
| 12 | 개국 | | | T_ATM 등록: | | |
| 21 | 상태 | | | 카세트 4행: | | |
| 22 | 장애 | | | IF큐 3행(W): | | |
| 31 | 마감 | | | T_DAY_TOTAL 5행: | | |
| 41 | 시재 | | | T_ADD_CASH: / 2회째 성공: | | |
| 52 | 운영 | | | T_OPERATION_JOURNAL: | | |
| 71 | 거래 | | | 값 일치(088/770000): | | |

기입: ✅ 정상 / ⚠ 부분(비고에 내용) / ❌ 실패(T_JOURNAL_ERROR 의 ERR_MSG 를 비고에)

> 참고 수치 (PPT 앞장용): 피크 부하 5,500건 TPS 343/s 무실패, Kafka→ES 무손실 색인,
> 실 agent(파일 기반) E2E 검증 완료 — 상세 README.md 검증 결과 표 참조.

---

## 다음 검증 과제 (오늘 범위 아님 — 메모)

1. **저널 변경/추가 시 agent 영향**: agent 는 idx 를 **내용 해석 없이 통째로 전송**(head 60B 의
   idx_enc/jnl_enc 플래그만 읽음) → **body 필드가 바뀌어도 agent 수정 불필요.** 수정이 필요한 경우는
   ① head 60B 레이아웃 변경 ② 새 첨부(이미지 4종째 등) ③ 암호화 방식 변경 — 즉 신규 jnl_kind 추가는
   서버 interface.journal.json + 핸들러 추가만으로 대응 가능(구조 확인 완료, 실증은 신규 kind 모의 테스트로)
2. **AP↔agent 메시지 검증**: 저널 외 AP 소켓 전문(상태조회 X0001/X0002, ATM ID 조회 10, 12,
   파일수집 17, 원격명령 등) — 스펙 캡처본: `C:\Users\user\Desktop\정찬식\15.시스템\agent 인터페이스\*.PNG`
   + `새마을ATM재구축_AGENT_ATM_IF_전체_20251112.pdf`. Start-ApSimulator.ps1 확장으로 검증 가능
