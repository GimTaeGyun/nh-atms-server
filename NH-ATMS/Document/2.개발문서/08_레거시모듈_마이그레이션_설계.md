# 08. 레거시 모듈 마이그레이션 설계 (atmr 잔여 / atmb / atmi / atme)

> 작성일: 2026-05-30
> 대상: 새마을금고 레거시 ATMS(atmr/atmb/atme/atmi/atmf) → NH-ATMS 신규(Spring Boot 3.5 / Java 21 멀티모듈)
> 외부 연계 정책: **개발 단계에서는 Mock/Stub** 로 구현(실 엔드포인트 미연동), 추후 실연동 전환

---

## 0. 레거시 → 신규 매핑 개요

| 레거시 | 약어 의미 | 역할 | 신규 모듈 | 현재 상태 |
|---|---|---|---|---|
| atmr | Receiver | 저널 수신 + 에이전트 관리 + IF 큐잉 | `atms-journal` | 저널 적재 완료 / 잔여 있음 |
| atmb | Batch | 정산·집계·장애율/가동률 배치 | `atms-batch` | 스켈레톤 |
| atme | External IF | 은행/장애처리업체 EAI 연계 | `atms-if-external` | 스켈레톤 |
| atmi | Internal IF | 시재조회/SMS/메신저(TCP·DB2DB) | `atms-if-internal` | 스켈레톤 |
| atmf | Frontend(JSP) | 웹 UI | React `nh-atms-web` | 대체 완료 |

---

## 1. atmr 잔여 작업 (atms-journal)

저널 적재 파이프라인(`/api/journal/receive` → Dispatch → kind별 Handler)은 완료. 아래가 레거시 atmr 대비 미구현분.

### 1-A. 에이전트 관리 (우선순위 1 — 외부의존 없음) **[본 차수 구현]**
- 레거시: `/agent/open.do`(로그인), `/agent/exit.do`(로그아웃), `/agent/ping.do`(폴링)
- 대상 테이블: `T_AGENTSTATUS` (PK = ATM_IP)
- 신규 엔드포인트(REST/JSON 화):
  - `POST /api/agent/open`  : 에이전트 기동 → upsert(connect_flag=1, start_date/time, 버전, 이미지옵션)
  - `POST /api/agent/exit`  : 종료 → update(connect_flag=0)
  - `POST /api/agent/ping`  : 헬스체크 → update(last_poll_date/time)
- 응답: 레거시는 AP 다운로드/서버설정/저널모드까지 내려주나, 신규 1차는 **연결상태 추적 + 기본 ping 주기/저널모드** 응답까지만. 파일배포(atmf) 연동분은 atms-file 단계로 이월.

### 1-B. T_ATMUNITSTATUS 갱신 (우선순위 2)
- 저널 수신 시 기기 단위 디바이스(SP/EP) 버전·상태 갱신. AP_START(11) body.devices[] 활용.
- AtmMonitorJournalHandler/APStart 단계에서 부수 갱신 또는 별도 핸들러.

### 1-C. T_JOURNALIMAGE 적재 (우선순위 3)
- 명세표/수표/고객 이미지 경로 저장. 이미지 업로드 채널(atms-file)과 연계되므로 atms-file 단계와 함께.

### 1-D. 대내/대외 IF 큐잉 (우선순위 4)
- 장애 저널(22) 수신 시 atme(출동요청)/atmi(SMS) 로 전달하는 비동기 큐.
- 신규: `T_IF_QUEUE` 테이블 + 스케줄러(또는 이벤트) → atms-if-* 호출. atmb/atmi/atme 구현 후 연결.

### 1-E. 에러 재처리 (우선순위 5)
- 레거시 `/reprocessErrorJnl.do` → `T_JOURNAL_ERROR` 재파싱. atms-batch 잡으로 편입 권장.

---

## 2. atmb (atms-batch) 설계

Quartz/Spring Batch 기반 일배치. 외부의존 없음 → atmr 다음 우선.

| 배치ID(레거시) | 스케줄 | 역할 | 신규 잡명 | 대상 테이블 |
|---|---|---|---|---|
| batday01 | 01:00 | 장애율/가동률 집계 | `faultRateJob` | T_FAULTRATEMANAGEMENT, T_OPERATIONRATEMANAGEMENT |
| batday02 | 01:05 | 거래실적 시간대별 집계 | `txDailyTotalJob` | T_TRANSACTIONDAILYTOTAL |
| batday03~ | 일배치 | 거래건수/금액 집계 | `txSummaryJob` | (집계 테이블) |
| (신규) | 02:00 | 저널 에러 재처리 | `journalErrorRetryJob` | T_JOURNAL_ERROR |

- 입력: T_JOURNAL, T_TRANSACTIONJOURNAL, T_ATMMONITOR
- 출력: 일집계/통계 테이블 + T_BATCHHISTORY(실행이력)
- 설계: `@Scheduled`(cron) 또는 Spring Batch Job + JobLauncher. 멱등성(재실행 안전) 위해 집계 대상일 DELETE 후 INSERT.

---

## 3. atmi (atms-if-internal) 설계 — Mock/Stub

| 레거시 URL | TX/TR | 역할 | 통신 | 신규 엔드포인트 |
|---|---|---|---|---|
| /int-if/getDrawerCheck.do | ATMONST10100 | 시재 조회 | TCP Socket | `POST /api/if/internal/drawer-check` |
| /int-if/sendFaultSms.do | SMSONSS10200 | 장애 SMS | TCP Socket | `POST /api/if/internal/fault-sms` |
| /int-if/sendFaultMessenger.do | ATMBYDD10100 | 장애 메신저 | DB2DB | `POST /api/if/internal/fault-messenger` |

- 공통상수: BANK_CODE="045"(새마을금고→NH 코드로 치환), HOSTNAME, GlobalID(32자) 생성기.
- **Mock 전략**: `InternalGateway` 인터페이스 + `MockInternalGateway`(더미 응답) / 추후 `TcpInternalGateway` 구현 스왑.
- 대상 테이블: T_GWINTERFACE(연계전문 이력), T_SMSSENDDATA(SMS 큐).

---

## 4. atme (atms-if-external) 설계 — Mock/Stub

| 레거시 URL | TX/TR | 역할 | 신규 엔드포인트 |
|---|---|---|---|
| /ext-if/openFailureStatus.do | EM_01001110 | 개국/장애/상태 통보 | `POST /api/if/external/status-notify` |
| /ext-if/serviceCallRequest.do | EM_01001120 | 출동 요청 | `POST /api/if/external/service-call` |
| /ext-if/serviceCallCancel.do | EM_01001160 | 출동 취소 | `POST /api/if/external/service-call/cancel` |
| /ext-if/serviceCallChange.do | EM_01001170 | 출동 변경 | `POST /api/if/external/service-call/change` |
| /ext-if/manyFailureRequest.do | EM_01001180 | 동시다발 장애 | `POST /api/if/external/many-failure` |
| /ext-if/cornerChange.do | SM_02001160 | 코너 변경 | `POST /api/if/external/corner-change` |
| /ext-if/machineInfoNotification.do | IQ_04001140 | 기기정보 통보 | `POST /api/if/external/machine-info` |

- **Mock 전략**: `ExternalGateway` 인터페이스 + `MockExternalGateway`(전문 생성 후 송신 흉내, T_NETSENDDATA 저장) / 추후 실 Gateway 구현.
- 전문(고정길이) 생성 유틸: `MessageMakeUtil` 포팅(레이아웃 메타 기반).
- 대상 테이블: T_NETSENDDATA(송신), T_NETRECVDATA(수신), T_FAULTMANAGEMENT(장애관리).

---

## 5. 구현 순서 (로드맵)

1. **atmr 잔여 1-A 에이전트 관리** ← 본 차수
2. atmb 거래/장애 집계 배치(txDailyTotalJob, faultRateJob) + T_BATCHHISTORY
3. atmi 시재조회/SMS Mock + T_GWINTERFACE/T_SMSSENDDATA
4. atme 출동요청/상태통보 Mock + T_NETSENDDATA/T_FAULTMANAGEMENT
5. atmr 1-D IF 큐잉으로 2~4 연결 (장애저널 → atme/atmi 자동 트리거)
6. atmr 1-B/1-C (T_ATMUNITSTATUS, T_JOURNALIMAGE) + atms-file 연계
7. atmr 1-E 에러 재처리 배치 편입

---

## 6. 공통 설계 원칙

- 모든 신규 IF는 **REST/JSON** 표준화(레거시 .do + 고정길이 → JSON DTO).
- 외부 송수신은 **Gateway 인터페이스 + Mock 구현** 으로 분리하여 테스트 가능성 확보.
- 매퍼는 모듈별 `mapper/**/*.xml`, 엔티티는 Lombok `@Data @Builder`, PK 규약 유지.
- 신규 테이블은 Flyway `V*.sql` 로 버전 관리(2.개발문서/sql).
