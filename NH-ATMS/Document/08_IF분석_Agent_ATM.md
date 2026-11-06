# ATM ↔ AGENT ↔ ATMS 서버 인터페이스 카탈로그

> 원본: `새마을ATM재구축_AGENT_ATM_IF_전체_20251112.txt` (CP949, 896줄)
> 분석일: 2026-05-14
> 대상: NH-ATMS Agent 재설계 + ATMS 서버 수신 API

---

## 0. 통신 구조 개요

```
┌─────────────┐  Local TCP        ┌──────────────┐  HTTP(S)     ┌──────────────┐
│   ATM AP    │◄─────────────────►│ ezTAMS Agent │◄────────────►│  ATMS 서버   │
│ (Server     │  localhost:21012  │ (Client      │  (RFP 확정)  │ (atms-       │
│  Socket)    │  Header(20)+Data  │  Socket EXE) │              │  journal/    │
└─────────────┘                   └──────────────┘              │  if-internal)│
                                                                └──────────────┘
       ▲                                  ▲
       │                                  │
   거래/장애 발생                     저널 파일 읽기
                                      D:\ezTAMS\JNL\YYYYMMDD\...
```

| 구간 | 프로토콜 | 패킷 | 설명 |
|---|---|---|---|
| ATM AP ↔ Agent | TCP/IP `localhost:21012` | Header(20B) + Data(가변, `|` 구분 ASCII) | AP가 Server, Agent가 Client |
| Agent ↔ ATMS 서버 | **HTTP(S)** (RFP 기준) | JSON | NH는 **에이전트 폐쇄망 HTTP** 채택 |
| 저널 파일 | 로컬 파일시스템 | 5종 파일 (HDX/IDX/TXT/IMG/IDX 헤더) | Agent가 폴링 후 서버 업로드 |

---

## 1. ATM AP ↔ Agent (로컬 TCP 소켓) — Command 카탈로그

### 1.1 Message 공통 포맷

**Header (20 Byte 고정)**

| 필드 | 크기 | 설명 |
|---|---|---|
| Length | 5B | Data 부 길이 (00015~99999) |
| Message Type | 4B | (예: OPEN, POLL, RCMD …) |
| Sequence No | 4B | 1로 시작하는 일련번호 |
| Result Code | 1B | 응답 시 `0x30(성공)` / 기타(실패) |
| Reserved | 6B | Space(0x20) |

**Data 부**
- 항목 구분자: `|` (delimiter)
- 미사용: 숫자 → `0x30`, 문자 → `0x20`
- 파일 데이터: binary, 그 외: ASCII

### 1.2 명령 카탈로그 (전체 18종)

| # | 명령 ID | 방향 | 사용 가능 상태 | 설명 |
|---|---|---|---|---|
| **세션 관리** | | | | |
| 0 | `OPEN` | Agent→AP | 항상 | 세션 생성 후 모니터링 시작 |
| 1 | `POLL` | Agent→AP | 항상 (600초 주기) | 세션 헬스체크 |
| **2.3 Commands (16종)** | | | | |
| 2 | `STAT` | 양방향 | 항상 | ATM 상태 조회 |
| 3 | `IMGC` | Agent→AP | 장애/고객대기 | 화상/화면 이미지 촬영 |
| 4 | `DLD` | Agent→AP | 항상 | 개별/일괄 다운로드 통보 |
| 5 | `FCOL` | Agent→AP | 항상 | 파일 수집 (로그/저널 업로드) |
| 6 | `RST` | Agent→AP | **장애 시만** | Device Reset |
| 7 | `RBT` | Agent→AP | 거래중×, 계원모드× | Reboot |
| 8 | `POFF` | Agent→AP | 거래중×, 계원모드× | Power Off |
| 9 | `STOP` | Agent→AP | **거래대기 시만** | 폐국(중지) |
| 10 | `OPEN_OP` | Agent→AP | 운영중지 시만 | 개국(운영) |
| 11 | `JMOD` | Agent→AP | 항상 | ATM 저널운영모드 변경 |
| 12 | `JMOD_NOTI` | AP→Agent→서버 | (AP 자발) | 저널운영모드 변경 통보 |
| 13 | `MSG` | Agent→AP | 장애/중지 시 | 메시지 전송 (화면 표시, 40B×5줄) |
| 14 | `TMOD` | Agent→AP | 항상 | 거래모드 변경 |
| 15 | `CLS` | Agent→AP | 항상 | 마감/합계/소계 제어 |
| 16 | `RCMD` | Agent→AP | **거래대기 시만** | 원격 CMD 실행 (ShellExecute) |
| 17 | `GDL` | Agent→AP | **거래대기 시만** | 일반 다운로드 (1회성, CMD 동반, 3분 timeout) |

> **주의**: 위 명령 ID는 본 문서에서 RESTful 변환을 위해 **부여**한 식별자.
> 원 스펙은 message_type 4B 코드로 구분되며, 정확한 코드값은 PoC 단계에서 송수신 샘플을 캡처해 확정 필요 (R16).

### 1.3 저널 모드 코드

| 값 | 의미 |
|---|---|
| `0` | None (운영중지) |
| `1` | 전자저널 모드 |
| `2` | 종이 + 전자저널 모드 |
| `3` | 종이저널 모드 |

### 1.4 저널 종류 (Journal Header `JNL_TYPE` 11~41)

| 코드 | 종류 | 폴더 | 설명 |
|---|---|---|---|
| `11` | AP Start | `JNLIDX/STA/` | AP 시작 저널 |
| `12` | 개국 | `JNLIDX/STA/` | 개국 시점 (장비/IP/OS/HDD 정보) |
| `21` | 상태저널 | `JNLIDX/STA/` | 상태 변화 |
| `22` | 장애저널 | `JNLIDX/STA/` | 거래중 장애, 미수취 등 |
| `31` | 마감저널 | `JNLIDX/TOT/` | 합계/소계/마감, 항목 가변 |
| `41` | 시재저널 | `JNLIDX/BAL/` | 보충/회수, 권종별 |
| (운영) | 운영저널 | `JNLIDX/OPA/` | (3.3.6) |
| (거래) | 거래저널 | `JNLIDX/TRA/` | (3.3.7) 거래 상세 |

### 1.4-A 거래저널(3.3.7) 88필드 전체 명세

> 출처: 인터페이스 설계서 PDF p.37 `<표 3.3.7>`

| # | 필드 | Max | 설명 |
|---|---|---:|---|
| 1 | Transaction Number | 4 | ATM 거래 일련번호 |
| 2 | Transaction Code | 6 | 거래코드 (코드표 참조) |
| 3 | Transaction Form Account | 2 | 미사용 `|` |
| 4 | Transaction to Account | 2 | 미사용 `|` |
| 5 | Bank Code | 4 | 거래 매체 은행코드 |
| 6 | Bank Account | 32 | 거래 매체 계좌/카드 번호 |
| 7 | Transfer Bank Code | 4 | 이체 대상 은행코드 |
| 8 | Transfer Account | 32 | 이체 대상 계좌번호 |
| 9~14 | Host Date(month/day/year/hour/minute/second) | 2/2/4/2/2/2 | Host 거래일시 |
| 15 | Host Retrieval Number | 8 | 호스트 인증번호 |
| 16 | Host Trace Audit Number | 8 | 미사용 |
| 17 | Host Net ID | 8 | 미사용 |
| 18~20 | Host Settle Date(month/day/year) | 2/2/4 | 미사용 |
| 21 | Deposit Number | 8 | 미사용 |
| 22 | Charge Amount | 15 | 거래 수수료 |
| 23 | Request Amount | 15 | 요청 금액 |
| 24 | Transaction Amount | 15 | 거래 금액 |
| 25 | Remain Amount | 15 | 잔액(전체) |
| 26 | Available Amount | 15 | 잔액(출금가능) |
| 27 | Proc Count | 1 | 진행 카운트 |
| 28 | Transaction Result | 6 | Host 거래결과 코드 |
| 29~31 | Error Code 1/2/3 | 10/10/10 | 장애코드 |
| 32 | MS Data | 104 | 카드/통장 MS 데이터 |
| 33 | Non-Cash Value | 16 | 미사용 |
| 34 | Non-Cash Type | 2 | 미사용 |
| 35 | Host Message | 64 | Host Message |
| 36 | Media ID | 1 | 매체 구분(코드표) |
| 37 | 마감 전 후 구분 | 1 | `1`=마감전, `2`=마감후 |
| 38 | Host Cash Amount | 15 | Host 현금 시재 잔액 |
| 39 | Host Check Amount | 15 | Host 수표 시재 잔액 |
| **40** | **No. of Deno. In Tran.** | 2 | 입/출금 거래 시 지폐 권종 개수 N |
| 41~43 (×N) | Currency code / Deno Code / Count in Tran. | 3/10/5 | 권종별 반복 |
| **48** | **Check Total Count** | 2 | 입/출금 거래 시 수표 매수 N |
| 49~51 (×N) | Check Amount / Bank Code / Number | 15/4/32 | 수표별 반복 |
| **56** | **No of Cassette (Logical)** | 2 | 권종별 카세트 개수 N |
| 57~60 (×N) | CST Currency / Denomination / Bill Count / Status | 3/10/5/2 | Logical 카세트 반복 |
| **66** | **No of Cassette (Physical)** | 2 | 물리적 카세트 개수 N |
| 67~71 (×N) | Cassette No / Currency / Denomination / Bill Count / Status | 2/3/10/5/2 | Physical 카세트 반복 |
| **78** | **No of ATM Check Deno** | 2 | ATM 출금 수표 권종 개수 N |
| 79~82 (×N) | Denomination Code / Currency Code / Count Total / Check Information | 10/3/5/32 | ATM 출금 수표 반복 |
| 88 | 고객명 | 20 | 이체(송금)/입금 시 대상 고객명 |

**구조 요약**: Fixed(39) + 5개 가변 배열 (지폐/수표/Logical CST/Physical CST/ATM출금수표) + 고객명

→ DB 매핑: `T_TRANSACTION` (메인) + `T_TRANSACTIONDENOMINATION`/`T_TRANSACTIONCHECK`/`T_TRANSACTIONCASSETTE` (배열 전개)

### 1.5 ATM ID 체계

```
[금고번호 5자리] + [단말번호 3자리] = 총 8자리
예: "12345" + "001" = "12345001"
```

### 1.6 환경 설정 파일 (`eztams.agent.ini`)

| 키 | 의미 | Write 시점 |
|---|---|---|
| `atmid` | ATM 8자리 ID | AP가 부팅/지점·단말번호 변경 시 |
| `atm_type` | 기기 종류 | AP 기동 전 |
| `version.1` | 다운로드 적용 버전 | 다운로드 적용 후 |
| `jnlmode` | 0/1/2/3 | 저널 모드 변경 시 |
| `machine_no` | 기기번호 | 변경 시 |
| `barcode_no` | 바코드번호 | 변경 시 |
| `[AP]` 섹션 | AP 관련 설정 | - |

### 1.7 저널 파일 경로 표준

```
D:\ezTAMS\
└── JNL\
    └── YYYYMMDD\           # 일자별
        ├── Hidx{yymmdd}.hdx        # 헤더 인덱스 (20B 헤더×N)
        ├── JNLIDX\                  # 인덱스 파일 (헤더+데이터, 데이터 암호화)
        │   ├── STA\<yyyymmddHHmmssSSS{N}>.idx   # AP Start, 개국, 상태/장애
        │   ├── TRA\...                            # 거래
        │   ├── BAL\...                            # 시재
        │   ├── TOT\...                            # 마감
        │   └── OPA\...                            # 운영
        ├── JNLTXT\<yyyymmddHHmmssSSS{N}>.dat   # Text 원본 (전체 암호화)
        └── JNLIMG\                              # 이미지 (업체별 폴더)
            ├── FACE.jpg                # 고객 안면 (CNT_IMG01)
            ├── IMIT.jpg                # 방출구 (CNT_IMG02)
            └── CARD.jpg                # 카드 (CNT_IMG03)
```

**파일 생성 순서**: `TEXT → IDX → HDX` (서버는 HDX 존재 확인 후 처리)

**동시구분 Flag**: 같은 시각 발생 시 마지막 1자리 증가 (0,1,2,…)

---

## 2. Agent ↔ ATMS 서버 (HTTP) — 변환 대상 API

> NH는 RFP에 따라 **Agent ↔ ATMS 서버는 HTTP 통신**이며, 이는 **신규 설계** 필요 영역.
> 아래는 **본 문서가 제안하는 표준 API 카탈로그** (PoC에서 확정).

### 2.1 Agent → ATMS 서버 (Inbound, ATMS가 수신)

#### A. 상태/하트비트
| Method | Path | 설명 | Body |
|---|---|---|---|
| `POST` | `/api/agent/register` | 에이전트 등록/재기동 | `AgentRegisterRequest` |
| `POST` | `/api/agent/heartbeat` | 폴링 결과 전달 (600초) | `HeartbeatRequest` |
| `POST` | `/api/agent/status` | ATM 상태 통보 | `AtmStatusRequest` (Device Status 32B) |

#### B. 저널 송신 (5종)
| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/journal/recv/ap-start` | AP Start 저널 (11) |
| `POST` | `/api/journal/recv/open` | 개국 저널 (12) |
| `POST` | `/api/journal/recv/status` | 상태/장애 저널 (21/22) |
| `POST` | `/api/journal/recv/total` | 마감 저널 (31) |
| `POST` | `/api/journal/recv/balance` | 시재 저널 (41) |
| `POST` | `/api/journal/recv/operation` | 운영 저널 |
| `POST` | `/api/journal/recv/transaction` | 거래 저널 |

#### C. 파일 업로드 (Agent → 서버)
| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/file/upload/journal` | 저널 IDX/TXT/IMG 업로드 (multipart) |
| `POST` | `/api/file/upload/log` | AP/Agent 로그 업로드 |
| `POST` | `/api/file/upload/image` | 거래 이미지 업로드 |

#### D. 통보 (AP 자발 → 서버)
| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/agent/notify/journal-mode` | 저널 모드 변경 통보 (2.3.11) |
| `POST` | `/api/agent/notify/download-result` | 다운로드 결과 통보 |
| `POST` | `/api/agent/notify/cmd-result` | 원격 CMD 결과 |

### 2.2 ATMS 서버 → Agent (Outbound, 서버가 송신)

> **2가지 방식 중 선택 필요 (R17)**:
> - **(권장)** 폴링: Agent가 매 N초 `GET /api/agent/{atmId}/commands` 로 대기 명령 조회
> - 푸시: ATMS 서버가 Agent의 콜백 URL 호출 (Agent가 HTTP 서버 동시 운영)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/agent/{atmId}/commands` | 대기 명령 큐 조회 (폴링 방식) |
| `POST` | `/api/agent/{atmId}/command/{cmdId}/ack` | 명령 수령 확인 |
| `POST` | `/api/agent/{atmId}/command/{cmdId}/result` | 명령 실행 결과 |

**명령 페이로드 예 (다운로드)**:
```json
{
  "cmdId": "CMD-20260514-000001",
  "type": "DLD",
  "atmId": "12345001",
  "payload": {
    "fileName": "v2.3.5.zip",
    "downloadUrl": "https://atms-file.nh.local/api/file/12345001/v2.3.5.zip",
    "checksum": "sha256:abc...",
    "applyMode": "AUTO"
  },
  "issuedAt": "2026-05-14T10:00:00+09:00"
}
```

---

## 3. AS-IS → TO-BE 매핑 매트릭스

| AS-IS (새마을) | TO-BE (NH) | 비고 |
|---|---|---|
| ATM AP ↔ Agent: TCP `localhost:21012`, Header(20B)+Data(`|`) | **유지** (ATM 내부, 변경 시 ATM AP 영향 큼) | 단, Agent 자체는 재구현 가능 |
| Agent ↔ 서버: 미정 (eztams.exe 내부) | **HTTP/JSON** (RFP 확정) | atms-journal/file 모듈에서 수신 |
| 저널 파일: 평문 헤더 + 암호화 Data/TXT | 동일 정책 + **NH 키 정책 적용** | 키 관리 R18 |
| 저널 디렉토리: `D:\ezTAMS\JNL\` | 동일 (Windows ATM 기준) | 경로 INI로 외부화 |
| 다운로드: ZIP 압축, 버전.zip, AP가 압축 해제 | 동일 + **체크섬(SHA-256) 추가** | 무결성 |
| Polling 주기: 600초 (Agent→AP) | 동일 + Agent→서버 폴링도 600초 권장 | 트래픽 5,374대 × ~10/h = ~54k/h |
| 결과 코드: `0x30=성공` 1B | HTTP 200/4xx/5xx + `ApiResponse.code` | 명확화 |

---

## 4. 미정/위험 (PoC 확정 필요)

| ID | 항목 | 영향 | 액션 |
|---|---|---|---|
| **R16** | Message Type 4B 코드값 (각 명령) | 高 | **샘플 패킷 캡처** 또는 새마을 atmr 소스 분석에서 추출 |
| **R17** | Agent ↔ 서버 통신 방향 (폴링 vs 푸시) | 中 | NH 보안팀과 협의 (방화벽 정책) |
| **R18** | 저널 암호화 키 정책 (SEED vs AES) | 高 | NH 암호 정책 확인 (`AES-256` 권장) |
| **R19** | Device Status Code Byte별 1~U 코드 (4.2.1) | 中 | 실 ATM 벤더(효성/청호/LG…)별 매핑표 수집 |
| **R20** | 마감 저널 가변 항목 (Host vs ATM 항목 수) | 中 | 새마을 운영 데이터로 검증 |
| ~~R21~~ | ~~거래 저널 포맷~~ | - | **해소** (88필드 확정, OpenAPI 스키마 반영) |
| **R22** | 일반 다운로드 CMD 3분 timeout 보장 정책 | 低 | NH 운영 정책 |
| **R23** | 동시구분 Flag 1자리(0~9) → 초당 10건 초과 시? | 低 | 거의 발생 안 함, 발생 시 재시도 정책 |

---

## 5. 산출물

- 본 문서 (카탈로그)
- `agent-api-openapi.yaml` (OpenAPI 3.0 스펙)
- (다음 단계) Agent ↔ AP 메시지 클래스 (Java) — atms-journal 모듈
- (다음 단계) Agent ↔ ATMS Controller (Java) — atms-journal/file/if-internal 모듈
