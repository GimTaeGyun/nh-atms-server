# atms-journal 마이그레이션 노트 — 전자저널 수신 (구 atmr)

재검증(AS-IS 정합)에서 발견된 정확성 갭 보완 내역. AS-IS 기준 소스는
`2.Project/0.기본자료/새마을금고/atmr` (StatusFailure.java, RecvJournalMapper.xml,
AgentServiceImpl.java, AgentMapper.xml).

## 구현 내역

### 1. T_ATM_STATUS 갱신 필드 확장 (완료)

`AtmMonitorJournalHandler.syncAtmStatus` + `AtmStatusWriteMapper.updateMonitorStatus`
를 AS-IS `update_T_ATMSTATUS` 와 동일 컬럼 셋으로 확장 (장애현황 화면 3100/3101 소스):

| 컬럼 | 소스 (AS-IS StatusFailure 8단계 정합) |
|---|---|
| SERVICE_STATUS | body.atm_st (값 있을 때만 갱신) |
| ATM_FAULT_STATUS / OP_FAULT_STATUS | T_FAULT_MAP 대조 집계. **'1'/'0'** (AS-IS 정합 — 기존 'Y'/'N' 에서 변경) |
| ERR_GROUP_CD | 마지막 장애모듈의 err_group_cd (`FaultSummary.lastErrGroup` — AS-IS errorGroupCd 덮어쓰기 정합) |
| ERR_CD | body.bk_ecd (매 저널 덮어쓰기, 복구 시 클리어) |
| ATM_SESSION_STATUS | 00→'0', 02→'1', 그 외 null |
| CDU/CHK/MCR/SPR/JPR_STATUS | T_FAULT_MAP `display_flag='1'` 모듈의 `monitor_flag` (디바이스ID 7/9/6/1/2) |
| MAN/OMAN_STATUS | 논리카세트 stat 2번째 자리 (권종 10000/50000) |
| FAULT_ARISED_DATE/TIME | 기기·운영장애 발생 시 저널 키. 아래 2 참고 |
| SVR_RECEIVE_DATE/TIME | SYSDATE (XML 에서 채움) |

- T_ATM_STATUS 에 위 컬럼이 모두 존재(`Document/2.개발문서/sql/V1.2.1__master_tables_schema.sql`)하여 **DDL ALTER 불요**.
- AS-IS 처럼 SERVICE_STATUS 외 컬럼은 무조건 덮어쓰기(null 이면 클리어) — 스냅샷 의미.
- `analyzeFaults` 가 장애 여부와 무관하게 display_flag=1 모듈의 monitor_flag 를 수집하도록 수정
  (기존에는 장애 아닌 row 를 조기 skip 하여 수집 불가였음).

### 2. 만/오만 부족 장애발생일시 (완료)

FAULT_ARISED_DATE/TIME 기록 조건을 기존 `jnl_kind=22 && errCd 존재` 에서
AS-IS 와 동일한 `기기장애 || 운영장애` 로 변경. 만/오만 부족은
`analyzeManOmanShortage` 가 `fs.opFault=true` + `fs.lastErrGroup=73/81/82` 로 반영하므로
**jnl_kind=21 에서도** 부족 시 장애발생일시가 기록된다
(AS-IS `update_T_ATMSTATUS_at_ManOman` 은 동일 컬럼 재갱신이라 단일 UPDATE 로 통합).

### 3. T_ATM_MONITOR 적재 가드 (완료)

AS-IS StatusFailure 9단계와 동일하게 `atm_st ∈ {00(사용가능), 02(중지), 05(서비스오픈)}`
일 때만 INSERT. 그 외 상태는 skip (기존은 21/22 전건 INSERT).
디바이스 상태·카세트·장애분석 등 후속 처리는 가드와 무관하게 기존대로 수행.

### 4. 11/12 저널의 T_OPERATION_JOURNAL 기록 제거 (완료)

AS-IS 는 52(운영저널, OperationProcessor)만 `T_OPERATIONJOUNAL` 에 기록하고
11(AP시작)/12(개국)은 APStartProcessor/OpenProcessor 가 버전/기기등록 처리만 수행.
초기 포팅에서 11/12 도 운영이력으로 INSERT 하던 것을 재검증 결과에 따라 제거
(`OperationJournalHandler.handle` 주석에 변경 이유 기록).
11 의 T_ATM_UNIT_STATUS 버전갱신, 12 의 기기등록/개국 처리(T_ATM_MONITOR·T_ATM·INIT 등)는 유지.

### 5. AP 자동배포 응답 (완료)

`AgentService.open` 의 files[] placeholder(`emptyApFile`)를 AS-IS
`AgentServiceImpl.handleAgentOpen` 과 동일한 DB 조회 기반으로 교체:

- 신규 `ApVersionMapper.selectApLastVersion` (AS-IS AgentMapper.selectApLastVersion 이식,
  Oracle 문법 — `FETCH FIRST 1 ROWS ONLY`): T_DOWNLOAD_SERVER + T_AP_LAST_VERSION + T_ATM 조인.
  승인(CONFIRM_STEP='1') 버전이면 동일 배포본, 미승인이면 승인버전 직전 배포본.
- 응답: vers=LAST_DOWN_VER, file=FILE_PATH 에서 저장소 루트(`agent.ap.repository` 설정,
  기본 빈값)를 제거한 상대경로('/' 구분, 선행 '/'), dest=T_AP_LAST_VERSION.FILE_PATH+'\'+FILE_NAME,
  size=FILE_SIZE.
- 조회 실패/데이터 없음/FILE_PATH 빈값 → 기존처럼 빈 항목 1건 fallback (에이전트 다운로드 스킵).
- DDL 신규: `src/main/resources/sql/V1.2.8__ap_deploy_schema.sql`
  (T_AP_LAST_VERSION / T_DOWNLOAD_SERVER — 적용 후 Document/2.개발문서/sql 로 취합 필요).

### 6. IF 큐 수신처 설정화 (완료)

`IfQueueService` 의 하드코딩 placeholder("0000000000"/"ATMS_OP")를
`IfQueueProperties.Notify`(`if.queue.notify.sms-phones` / `messenger-users`, 콤마 구분 목록)
주입으로 교체. 수신처별로 FAULT_SMS / FAULT_MESSENGER 큐를 1건씩 적재.
운영자/담당자 동적 매핑(T_USER 계열 조회)은 NH 요건 확정 전이므로 TODO 주석으로 유보
— 설정 주입 구조까지만 구현. 기본값은 기존 placeholder 와 동일하여 동작 불변.

## 가정

- **ATM_FAULT_STATUS/OP_FAULT_STATUS 값 체계 '1'/'0'**: AS-IS `update_T_ATMSTATUS` 의
  `기기장애여부 1/0` 정합. 기존 TO-BE 'Y'/'N' 은 조회측(atms-web) 하드코딩이 없어 영향 없음.
- **ERR_GROUP_CD 는 마지막 장애모듈 기준**: AS-IS 루프에서 errorGroupCd 를 계속 덮어쓰는
  동작 그대로. 운영중지 이력(T_ATM_STOP_HISTORY)은 기존대로 최초 장애그룹 사용.
- **T_AP_LAST_VERSION/T_DOWNLOAD_SERVER 에서 BANK_CD 제거**: NH 단일기관 구성 가정
  (AS-IS 조회 SQL 도 BANK_CD 미사용). 테이블/컬럼명은 TO-BE 표준(언더스코어) 적용.
- AS-IS 의 저장소 경로 하드코딩 fallback(`/atmsap/pack_down`, `/atms/share`)은 새마을금고
  운영환경 전용이므로 미이식 — `agent.ap.repository` 단일 설정으로 대체.
- AP 배포 조회 실패는 에이전트 기동(open)을 실패시키지 않고 빈 항목 fallback
  (AS-IS 도 open 전체 try-catch 로 동일 성격).

## 스킵

- **바이너리 저널 수신(put.jnl.data.do)**: agent 전송방식이 PM 협의 대상이라 미구현.
  협의 확정 후 JournalReceiveController 에 수신 엔드포인트 추가 예정.
- **IF 큐 운영자 매핑 규칙**: NH 요건 확정 전 — 설정 주입까지만 (위 6 참고).
- **stream/ 패키지(Kafka/ES)**: 본 보완 범위 밖 — 무변경.
