# atms-batch 이관 노트 (AS-IS: 새마을금고 atmb → TO-BE: nh-atms-api/atms-batch)

AS-IS 소스: `2.Project/0.기본자료/새마을금고/atmb` (태스크 java + `WEB-INF/config/sqlmap/atms/*.xml`)
TO-BE 기준 스키마: atms-journal / atms-web 모듈 mapper XML 의 실존 테이블·컬럼명 (Oracle 19c).

## 공통 사항

- 잡 등록 체계: Job 빈 이름 = `T_BATCH_JOB.PROC_NAME` = `batdayNN` (Spring Batch, Tasklet/Chunk).
- 대상일자: JobParameter `statDate`(yyyyMMdd, identifying). `SpringBatchJobRunner` 가 항상 **전일**을
  주입 — AS-IS `sel_yesterday`(job_date 공백 시 전일) 의 기본 동작과 동일.
  (AS-IS 의 job_date 수동 지정 실행은 현행 `/run` API 미지원 — param-spec 은 메타데이터로만 노출)
- 멱등성: 모든 집계는 대상일자 DELETE 후 INSERT.
- 자행(NH) 은행코드: `atms.batch.bank-cd`(기본 `011`) — AS-IS MG 상수 `045` 대체.
- 신규 DDL: `src/main/resources/sql/` (ATMS 계정 1회 수동 실행)
  - `stat-tables-oracle.sql` : T_ATM_RUN_HISTORY, T_ATM_ERR_GROUP_HISTORY,
    T_TRANSACTION_DAILY_TOTAL(재정의), T_TRANSACTION_CODE(신규)
  - `daily-report-tables-oracle.sql` : T_DAILY_REPORT01~07
  - `table-management-oracle.sql` : T_TABLE_MANAGEMENT + 시드(비활성)
  - `batch-job-seed-oracle.sql` : T_BATCH_JOB 크론 시드(멱등 MERGE)

## batday01 — 장애율/가동률 집계 (재구현)

| 항목 | 내용 |
|---|---|
| Job | `Batday01JobConfig` (runHistoryStep → errGroupHistoryStep) |
| Mapper | `Batday01Mapper(.java/.xml)`, DTO `RunHistoryRow`/`StopHistoryRow` |
| 유틸 | `job/support/ElapsedTimeCalculator` — AS-IS `f_GetElapsetime`/`f_getElapsedTime` Java 등가 이식 |
| 결과 | `T_ATM_RUN_HISTORY`, `T_ATM_ERR_GROUP_HISTORY` (신규 DDL) |

- 기존 ERR_CD 건수비율 방식(T_FAULT_RATE_MANAGEMENT / T_OPERATION_RATE_MANAGEMENT)은 AS-IS 와
  다른 임시 로직이어서 **제거** (두 테이블은 atms-batch 외 사용처 없음 확인).
- AS-IS 시간 기반 로직 이식 내용:
  - 위치유형별 기본 운영시간대(미설정 시): 1점내 09~16시, 2점외 08~22시, 3무인 00~24시.
    기본 가동대상시간(점내/무인 86400초, 점외 28800초)은 **AS-IS 원본 값 그대로 유지**
    (점내 7h·점외 14h 운영인데 대상시간이 불일치하는 AS-IS 특성 보존).
  - 장애경과시간: 당일복구 / 당일미복구(운영종료시각 보정) / 전일발생·당일복구(운영시작~복구,
    지연개국 미계상) 3케이스, 주간형·심야형(자정넘김) 운영시간 클리핑 포함 전부 이식.
  - 실제가동시간 = 대상시간 − 총장애시간 − 지연개국시간 − 미개국시간 (음수면 0).
  - 미개국 판단: 당일 개국(FO) 저널 없음 → 대상시간 전체 미개국
    (AS-IS `obtainNotOpenTime()` 은 0 고정이므로 동일).
- 스키마 조정(가정):
  - `t_faultanalysis.time_elapsed` 부재 → `FINISHED_DATE/TIME − JNL_DATE/TIME`(초)로 SQL 인라인 산출,
    미복구 건은 0. (AS-IS 는 온라인 처리 적재값 사용 — 산식 동일 전제)
  - T_CORNER 에 branch_cd 없음 → 조인키 `TERM_GROUP_ID + CORNER_CD` (TO-BE PK).
  - 개국시간(t4): AS-IS 단순 조인은 복수 FO 저널 시 행 중복 → `MIN(JNL_TIME)` 으로 단일화.
  - "240000" 시각은 java.time 이 미허용 → 익일 00:00 으로 등가 해석.

## batday02 — 거래실적 집계 (재구현)

- `Batday02JobConfig`(Chunk 1000) + `StatMapper(.java/.xml)` + `TxDailyTotalRow` 재설계.
- AS-IS 와의 **논리 동등성** 확보:
  - 성공 거래만 집계: `TRIM(TR_RESULT)='0000'` (AS-IS `'OK'` — 스키마 재설계에 따른 값 차이).
  - 차원: 거래코드(`TR_CD`=TR_TYPE) / 자행·타행(`TR_BANK_TYPE`, 매체 공백·'2'는 자행 간주 후
    BANK_CD 비교 — AS-IS 판정식 동일) / 마감구분(`CLOSING_TYPE`=TOTAL_TYPE_CD) / 요일(`DAY_WEEK`,
    영문 3자 대문자 MON~SUN — AS-IS `upper(%a)` 동일) / 지점(`BRANCH_CD`).
  - 건수/금액(방출금액 합 — AS-IS tr_amount=dispense_amount 동일)/수수료 합.
  - **시간대는 STAT_HOUR 행 구조 유지**: AS-IS 24시간 pivot 컬럼(tr_cnt_00_07…tr_amt_23_24)과
    정보량 동일 — `SUM(… WHERE STAT_HOUR BETWEEN …)` 로 임의 구간 복원 가능.
- 제외(사유): AS-IS `tr_type_cd`(거래종류) 차원은 `t_transactioncode` 매핑 필요 —
  TO-BE 미이관 코드테이블. 원거래코드(TR_CD) 차원으로 대체(코드매핑 시 상향집계 가능).
  대상 기기 필터 중 `term_group_id like '0%'` 는 MG 채번 규칙 의존이므로 제외.
- `T_TRANSACTION_DAILY_TOTAL` DDL 재정의 — 기존(구) 테이블 생성돼 있으면 DROP 후 재생성 필요.

## batday08 — 장애/거래 보고서 집계 (신규 이관)

- `Batday08JobConfig`(reportBaseStep → reportRollupStep) + `Batday08Mapper(.java/.xml)`.
- T_DAILY_REPORT01(기기별) : AS-IS 기기 루프 + 4개 서브집계(장애그룹별/5만원권/수표/
  자행·타행×거래종류별)를 **동일 결과의 set 기반 INSERT...SELECT** 로 통합.
  FAULT_ETC=전체 장애건수(AS-IS COUNT(*) 그대로), 71 은 출금부(7)/입금부(J) 분리 동일.
- T_DAILY_REPORT02(지역)/03(설치장소)/04(영업본부)/05(기종)/07(그룹·기종) :
  REPORT01 상향집계 — AS-IS 필터(`atm_type_cd in 1,2,4`, `model_cd not in 102,201,202`,
  `atm_status='1'`) 그대로.
- 가정:
  - `T_TRANSACTION_CODE`(TR_CD→TR_TYPE_CD 매핑) 신규 정의 — **데이터 시드 필요**
    (미시드 시 거래종류 기반 항목(TR_D/T_*, 오만원권/수표)은 0건. AS-IS t_transactioncode 이관 권장).
  - AREA_CD: AS-IS 한글 지역명(AREA1_CD) CASE 매핑 유지, 미매칭 '99'.
  - BANK_CD 상수(`011`) — AS-IS `t_atmgroup.parent_id = 은행코드` 필터는 NH 그룹체계와
    의미가 달라 제외(그룹 존재 조인만 유지). SUPER_ID/ATM_TYPE_CD 는 T_ATM_GROUP/T_ATM_MODEL 사용.

## batday09 — 일별 기기이력 (신규 이관)

- `Batday09JobConfig` + `Batday09Mapper(.java/.xml)` → `T_DAILY_REPORT06`.
- 가정:
  - AS-IS 조인 `a.branch_cd = b.corner_cd` 는 MG 데이터 특성(지점=코너) 의존 →
    `T_ATM.CORNER_CD = T_CORNER.CORNER_CD` 로 대체.
  - T_ATM_UNIT_STATUS 키에 AGENT_TYPE 포함 → 카메라(G) STATUS 는 MAX 로 단일화.
  - BANK_CD 상수(parent_id 미사용, `parent_id is not null` 필터 제외).

## batday98 — 테이블 정리 (신규 이관)

- `Batday98JobConfig` + `Batday98Mapper(.java/.xml)` + `T_TABLE_MANAGEMENT`(신규 DDL/시드).
- 기준일 = `TRUNC(SYSDATE) − KEEP_PERIOD`, `KEEP_CYCLE='1' AND DELETE_YN='1'` 행만 대상
  (행 없으면 0건 삭제 — AS-IS 동일). 시드는 안전을 위해 **DELETE_YN='0' 비활성** 등록,
  보관기간(KEEP_PERIOD)은 가정값 — 운영 정책 확정 후 DBA 조정 필요.
- 대상 테이블 매핑: t_atms_batch_his→T_BATCH_HISTORY(START_DATE), t_atmmonitor→T_ATM_MONITOR,
  t_gwinterface→T_GW_INTERFACE(SEND_DATE), t_netrecvdata/senddata→T_NET_RECV/SEND_DATA,
  t_journal(+data/error/image)→T_JOURNAL(+…), t_transactionjournal/operationjournal 동일.
- 제외(사유): `t_remoteinterface`, `t_journalfootnote`, `t_atms_crud` — TO-BE 스키마 미존재.

## batday99 — 파일 정리 (신규 이관)

- `Batday99JobConfig` + `Batday99Properties`(`atms.batch.batday99.*`).
- 저널 이미지 루트(yyyyMMdd 일자 디렉토리, 보관개월 초과 시 통째 삭제)와
  경로별 보관일수 초과 파일 삭제(재귀, 빈 폴더 유지) — AS-IS 로직 동일.
- 차이: AS-IS 의 수동실행 파라미터(list)는 현행 실행 API 미지원 → application.yml 설정으로 통일.
  운영 배포 시 `jnl-img-root`/`targets` 를 실제 NAS 경로로 설정해야 동작(기본은 스킵).

## 크론 시드 (batch-job-seed-oracle.sql)

AS-IS 태스크 헤더의 크론 그대로 (Spring 6필드 호환):

| PROC_NAME | CRON | 설명 |
|---|---|---|
| batday01 | 00 00 01 * * ? | 장애율/가동률 집계 |
| batday02 | 00 05 01 * * ? | 거래실적 집계(시간대별) |
| batday08 | 00 15 03 * * ? | 장애/거래 보고서 집계 |
| batday09 | 00 20 01 * * ? | 일별 기기 이력정보 |
| batday30 | 00 05 00 * * ? | 부서(금고)명 동기화 |
| batday98 | 00 55 00 * * ? | 테이블 데이터 정리 |
| batday99 | 00 50 23 * * ? | 파일 정리 |

## paramSpec API

- `GET /api/batch/jobs/{procName}/param-spec` (`BatchJobController` → `BatchJobService.paramSpec`
  → `job/support/BatchJobParamSpecs`).
- batday01/02/08/09: `{"job_date": ""}` (공백=전일), batday30/98: `{}`,
  batday99: `{"list":[{"path":"","days":"-7"}]}` — AS-IS `Task.paramSpec()` 스키마 동일.
- 현행 `/run` API 는 파라미터 미수신(전일 고정) — 스키마는 향후 파라미터 실행 지원용 메타데이터.

## 미이관(스킵) 배치와 사유

| AS-IS | 사유 |
|---|---|
| batday03 (동시다발장애 전송) | MG 대외전송(SMS/출동) 인프라 의존 — 별도 과제 |
| batday15~19, 21 | 새마을금고 특화 파일동기화(본지점/우편번호/휴일/직원/신주소/기기동기화) — NH 미해당 |
| batday20 (미개국 출동요청) | MG 출동요청 인터페이스 의존 — 별도 과제 |
| batday31/35/36/39/41 | AS-IS 주석에 [미사용]/TEST 표기 |
| batday42/43 | 새마을금고 ETL/우편번호 특화 |
| batday57 (전자저널 온리모드 변환) | MG 저널저장 구조 특화 — TO-BE 저널 구조 상이 |
| batday97 (에러저널 재처리) | atms-journal 온라인 처리 구조로 대체 검토 필요 — 별도 과제 |
| batsec01 | 초단위 상시배치 — 배치 이관 범위 외 |

## 배포 체크리스트

1. `sql/stat-tables-oracle.sql`, `sql/daily-report-tables-oracle.sql`,
   `sql/table-management-oracle.sql` 실행 (기존 구 T_TRANSACTION_DAILY_TOTAL 은 DROP 후).
2. `T_TRANSACTION_CODE` 데이터 시드 (AS-IS t_transactioncode 이관).
3. `sql/batch-job-seed-oracle.sql` 실행.
4. T_TABLE_MANAGEMENT 보관기간 확정 후 DELETE_YN='1' 활성화.
5. application.yml `atms.batch.bank-cd`, `atms.batch.batday99.*` 운영값 설정.
