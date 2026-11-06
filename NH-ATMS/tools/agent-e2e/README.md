# agent E2E 테스트 (실 agent → atms-journal)

시뮬레이터 없이 **실제 .NET agent**로 저널 수신 전 구간을 검증한다.
2026-07-10 전 구간 검증 완료: 저널 생성 → agent 송신(바이너리) → atms-journal 파싱 → Oracle T_JOURNAL 적재 → agent 파일 삭제(ack).

## 구성 요소

```
[New-Journal.ps1]          임의 저널(idx) 생성 → D:\atms\ap-jnl\jnl\{yyyyMMdd}\JNLIDX\
[Start-ApSimulator.ps1]    AP 소켓 시뮬레이터(:21001) — agent 핸드셰이크(ATM ID 조회) 응답
[eztams.agent.exe]         실 agent (0.기본자료\...\atms.agent\bin\Debug)
[atms-journal]             수신 서버 (:8081) — LegacyJournalDataController(put.jnl.data.do)
```

agent 는 ① AP 소켓 핸드셰이크 성공 → ② HTTP 개국(open.do) → ③ 개국 응답의 atms 접속정보가
ini 와 일치해야 저널 워커를 시작한다 (하드코딩 제거됨 — 2026-07-10 재빌드).

## 실행 순서

```powershell
# 1. atms-journal 기동 (개국 응답 접속정보 = agent ini 와 일치시켜야 함)
$env:AGENT_ATMS_HOST="127.0.0.1"; $env:AGENT_ATMS_PORT="8081"; $env:AGENT_ATMS_SCHEMA="http"
cd Backend\nh-atms-api; .\gradlew.bat :atms-journal:bootRun

# 2. AP 시뮬레이터
.\tools\agent-e2e\Start-ApSimulator.ps1        # :21001

# 3. agent 실행 (ini: [atms] host=127.0.0.1 port=8081 / [apif] port=21001, jnl=D:\atms\ap-jnl\jnl)
& "...\atms.agent\bin\Debug\eztams.agent.exe"

# 4. 저널 생성 → 5초 내 자동 송신
.\tools\agent-e2e\New-Journal.ps1 -Kind 11 -Count 2 -WithReceipt
.\tools\agent-e2e\New-Journal.ps1 -Kind 12 -Count 1
```

**성공 판정**: idx 파일이 삭제되면 성공 (agent 는 `##emsg##` 공백 응답 시 삭제).
서버 로그에서 `JournalMapper.insert Updates: 1` 확인. 처리 실패분은 T_JOURNAL_ERROR 보존(agent 재전송 안 함 — 서버측 재처리).

## agent ini 요점 (eztams.agent.ini — bin\Debug, 원본 백업: .bak-remote)

| 키 | 값 | 의미 |
|---|---|---|
| [atms] host/port | 127.0.0.1 / 8081 | 로컬 atms-journal (배포 검증 시 서버 IP로) |
| [atms] put.jnl.data | /atmr/agent/put.jnl.data.do | 바이너리 저널 엔드포인트 |
| [apif] port / jnl | 21001 / D:\atms\ap-jnl\jnl | AP 시뮬레이터 포트 / 저널 루트 |
| [rmsg] port | 21002 | agent 자체 리슨(원격명령) — AP 심과 충돌 회피용 변경 |
| [ap] atmid | 00722447 | AP 심 응답과 일치해야 함 |

## 배포(도커) 환경 테스트 — ap-server(192.168.28.21)

로컬 대신 배포 컨테이너(nh-atms-journal, 19082→8081)를 대상으로 동일 테스트 가능.
**전제: 바이너리 수신(legacy 패키지) 포함 이미지가 배포돼 있어야 함** — 미포함 시 유효 전문에도 500 반환.

```powershell
# 1) 스모크 1건 (내 PC에서)
.\Invoke-JournalLoad.ps1 -Server http://192.168.28.21:19082 -Devices 1 -PerDevice 1 -Kind 11
# 2) 피크 5,500건
.\Invoke-JournalLoad.ps1 -Server http://192.168.28.21:19082 -Devices 550 -PerDevice 10 -Parallel 32
```

확인 3종:
- 서버 로그: `docker logs -f nh-atms-journal 2>&1 | grep -E "put.jnl|dispatch|stored"`
- DB: `SELECT COUNT(*) FROM T_JOURNAL WHERE RECV_DATE = TO_CHAR(SYSDATE,'YYYYMMDD')`
- Kibana `http://192.168.28.21:19601` → Discover(데이터뷰 atms-journal-*, 최초 1회 setup-kibana.sh 또는 수동 등록)
  — 배포 compose 는 ATMS_KAFKA_ENABLED=true 라 색인까지 자동

실 agent 경유 시: journal 컨테이너에 AGENT_ATMS_HOST/PORT/SCHEMA(=192.168.28.21/19082/http) env 필요
(compose 반영됨), agent ini [atms] host=192.168.28.21 / port=19082 로 변경 + AP 심 기동 후 New-Journal 생성.

## Kibana 운영 대시보드 (2026-07-13)

`infra/elastic/build-dashboard.ps1 http://192.168.28.21:19601` 실행 →
**NH-ATMS 전자저널 운영 대시보드** 생성 (RFP 「전자저널 가동현황 모니터링」 데모).
URL: http://192.168.28.21:19601/app/dashboards#/view/nhatms-journal-dashboard
패널 4종: 총 수신 건수 / 유입 추이(시계열) / 유형(jnlKind) 분포(도넛) / 기기별 수신 Top(테이블).
자동 새로고침 10초, 기본 기간 24h.

## 알려진 설정 불일치 (수정 대기)

- **ping URL 불일치**: 개국(open.do) 응답은 `ping.url = /api/agent/ping` 을 내려주는데(application.yml `agent.ping.url`),
  실제 컨트롤러 매핑은 `/atms/agent/ping.do`·`/atmr/agent/ping.do` 뿐이라 agent 폴링이 404
  (`NoResourceFoundException: No static resource api/agent/ping`)로 실패한다. 저널 수신에는 무관하나 로그 소음.
  → 해결: application.yml `agent.ping.url` 을 `/atms/agent/ping.do` 로 맞추거나, `/api/agent/ping` 매핑 추가.

## 실 agent E2E 트러블슈팅 (중요)

- **agent 저널 워커가 파일을 안 보냄** = 저널 루트(`[apif] jnl`)에 **비어있는 옛 날짜 폴더**가 남아있을 때 발생.
  원인: agent 코드(Ag2AtmsJounalTransferWorker.cs:110)가 `today=true` 하드코딩 + idx 0개면 `break`(루프 전체 중단).
  날짜 폴더 오름차순 스캔이라 빈 옛 폴더가 먼저 걸려 그날 파일을 못 봄.
  → **해결: 저널 루트의 지난 날짜 폴더 삭제 후 재시도.** (실기기에선 당일 폴더만 존재해 무해하나, 테스트 시 반드시 정리)
- agent open 응답의 AP 파일 항목이 빈 값이면 `Path.GetDirectoryName("")` 예외 로그가 뜨지만 저널 송신에는 무해(무시 가능).
- 검증(2026-07-13): 실 agent(00722447) → 배포서버 71 저널 3건 송신 → T_JOURNAL 적재 → ES 색인, body 파싱(cust_nm/trx_amt) 정상 확인.

## 이슈/이력

- **agent 하드코딩 제거(2026-07-10)**: `Ag2ApMessageTransferClient.cs`의 ATMS host 하드코딩(192.168.28.21:19081)을
  개국 응답 기반으로 원복 — 서버 설정(agent.atms.*)으로 접속처 제어 가능. MSBuild(VS2022) 재빌드.
- **atms.ap.tp exe는 기동 불가(포트 미바인딩)** → Start-ApSimulator.ps1 로 대체 (동일 프로토콜).
- **DB 스키마 드리프트 주의**: 개발 DB에 최신 DDL 미적용 시 dispatch가 T_JOURNAL_ERROR 로 빠짐
  (예: T_AGENT_STATUS.LAST_JNL_DATE/TIME — 2026-07-10 수동 ALTER 적용 완료).

## 저널 유형 / 다중 기기 / 부하 테스트 (2026-07-10 추가)

- **JournalLib.ps1** — 전문 빌더 공통 라이브러리 (7유형: 11/12/21/22/31/41/71 + wire format)
- **New-Journal.ps1** — `-Kind 71|21|...|mix`, `-AtmId` 배열(다중 기기), 유형별 JNLIDX 하위폴더(TRA/STA/OPR/BAL/TOT)
- **Invoke-JournalLoad.ps1** — 부하 테스트: agent wire format 을 직접 병렬 POST
  (실 agent 1프로세스 = 기기 1대라 부하 불가 → 프로토콜 검증은 agent, 부하는 direct 로 역할 분리)

```powershell
.\Invoke-JournalLoad.ps1 -Devices 20  -PerDevice 5                 # 스모크
.\Invoke-JournalLoad.ps1 -Devices 550 -PerDevice 10 -Parallel 32   # 피크(5,500건)
```

### 검증 결과 — 배포 환경 (2026-07-13, ap-server 컨테이너 + Kafka/ES **on**)

| 시나리오 | 결과 |
|---|---|
| **피크 5,500건** (550대×10, mix, 병렬32) | **5,500/5,500 성공, 16s, TPS 343건/초** — 피크 상한(300/s)도 상회 |
| Kafka→ES 색인 | **5,501/5,501 무손실** (atms-journal-2026.07.13, 멱등 docId) |
| 서버 오류 | dispatch failed 0 / publish failed 0 |
| Kibana | 데이터뷰 'ATMS Journal' 등록 완료 → http://192.168.28.21:19601 Discover |

배포 환경 이슈 이력: images 볼륨(/home/atec/docker-volumes/nh-atms/images)이 root 소유라
컨테이너 사용자(atms/999) 쓰기 실패 → `chown -R 999:999` 로 해결 (신규 볼륨 생성 시 주의).

### 검증 결과 (2026-07-10, 로컬 1인스턴스 + 개발 Oracle, Kafka/ES off)

| 시나리오 | 결과 |
|---|---|
| 스모크 100건 (mix) | 100/100 성공, 실패 0 |
| **피크 5,500건** (550대×10, mix, 병렬32) | **5,500/5,500 성공, 42s, TPS 131건/초** — 피크 하한(100/s) 상회 |
| 유형 분포 | 71:2,840 / 31:714 / 41:712 / 21:671 / 22:663 — 전 유형 핸들러 정상 |

### 부하 테스트가 찾아낸 결함 (수정 완료)

1. **T_CASSETTE_STATUS MERGE blank-pad 결함** — CASSETTE_NO CHAR(5) vs VARCHAR 바인드 비교로
   항상 NOT MATCHED → 같은 기기 두 번째 시재/거래 저널부터 ORA-00001. `CAST(... AS CHAR(5))` 로 수정
2. **거래저널 은행코드 오버플로** — JIF 스펙 bk_cd 4자리("0011") vs BANK_CD 컬럼 3자리.
   bank3() 정규화(뒤 3자리) 추가. ※ 스펙-스키마 불일치는 설계 검증 항목으로 잔존
