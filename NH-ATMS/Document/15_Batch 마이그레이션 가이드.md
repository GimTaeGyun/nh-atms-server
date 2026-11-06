# NH-ATMS 배치 Spring Batch 마이그레이션 가이드

> 작성일: 2026-06-10 / 1차(Phase 1) 적용 완료
> 전략: **어댑터 방식 점진 전환** — REST API·프론트 화면·T_BATCH_JOB/T_BATCH_HISTORY는 그대로 두고 실행 엔진만 Spring Batch로 교체

---

## 1. 전환 후 아키텍처

```
프론트(/batch/job) ─ REST ─▶ BatchJobController ─▶ BatchJobService
                                                       │ runManual
DynamicBatchScheduler (동적 cron, T_BATCH_JOB) ────────┤ SCHEDULE
                                                       ▼
                                               BatchRunService ──── T_BATCH_HISTORY 적재 (기존 그대로)
                                                       │
                                  ┌────────────────────┴───────────────────┐
                                  │ ① PROC_NAME = Job 빈 이름 매칭          │ ② 매칭 없으면
                                  ▼                                        ▼
                          SpringBatchJobRunner                      legacy BatchTask
                                  │ JobLauncher.run(job, statDate)   (단순 배치 fallback)
                                  ▼
                       Spring Batch JobRepository ──▶ BATCH_* 메타테이블 (Oracle)
                                  │
                    ┌─────────────┴─────────────┐
              batday01 Job                 batday02 Job
           (Tasklet 멀티스텝)            (Tasklet 삭제 + Chunk 적재)
```

핵심 규칙: **`T_BATCH_JOB.PROC_NAME` == Spring Batch Job 빈 이름**. 이름이 일치하는 Job 빈이 있으면 Spring Batch로, 없으면 기존 `BatchTask`로 실행된다. 화면/REST 계약은 변경 없음.

## 2. Phase 1에서 변경된 파일

| 구분 | 파일 | 내용 |
|---|---|---|
| 신규 | `atms-batch/src/main/resources/sql/spring-batch-meta-oracle.sql` | BATCH_* 테이블 6 + 시퀀스 3 DDL (운영 적용용) |
| 신규 | `batch/job/Batday01JobConfig.java` | **batday01** Job — 장애율/가동률, Tasklet 2-Step 구성 예 |
| 신규 | `batch/job/Batday02JobConfig.java` | **batday02** Job — 거래실적 집계, 삭제 Tasklet + **Chunk(1000)** 시범 |
| 신규 | `batch/job/Batday30JobConfig.java` | **batday30** Job — 사용자 부서(금고)명 동기화 (MG 이식 1호, Phase 2) |
| 신규 | `batch/dto/TxDailyTotalRow.java` | Chunk 아이템 DTO |
| 신규 | `batch/executor/SpringBatchJobRunner.java` | JobLauncher 어댑터 (파라미터/재시작/건수 매핑) |
| 수정 | `batch/service/BatchRunService.java` | 디스패치 우선순위 ①Job ②BatchTask ③no-op |
| 수정 | `mapper/stat/StatMapper.xml` | Chunk용 `selectTxDailyTotalRows`/`insertTxDailyTotalRow`, batday30용 `updateUserDeptName` 추가 |
| 수정 | `application.yml` | `table-prefix: BATCH_` 명시, 주석 갱신 (`initialize-schema: never` 유지) |
| 삭제 | `batch/task/FaultRateTask.java`, `<br/>TxDailyTotalTask.java` | Job으로 대체 (BatchTask 인터페이스는 fallback용으로 유지) |

> 클래스 명명 규칙: **`BatdayNNJobConfig`** — MG 배치 번호 체계를 따르되 자바 클래스 관례(PascalCase)를 준수한다.
> Job 빈 이름(= `T_BATCH_JOB.PROC_NAME`)은 MG와 동일하게 소문자 `batdayNN`.

> batday30 등록 SQL (T_BATCH_JOB):
> ```sql
> INSERT INTO T_BATCH_JOB (PROC_NAME, JOB_DESC, CRON_EXPR, USE_FLAG, SORT_ORDER, REG_DT)
> VALUES ('batday30', '사용자 부서(금고)명 동기화', '0 5 0 * * *', 'N', 30, SYSDATE);
> -- 검증 후 USE_FLAG='Y' 로 활성화 (MG 명목 스케줄: 매일 00:05)
> ```

## 3. 적용 절차 (환경별 1회)

1. **DDL 적용**: ATMS 계정으로 `sql/spring-batch-meta-oracle.sql` 실행 (DBA 절차 준수)
2. 배포 후 기동 — 메타테이블이 없어도 기동은 되지만 첫 Job 실행에서 `ORA-00942`로 실패하므로 DDL 선행 필수
3. T_BATCH_JOB의 batday01/batday02 행은 변경 불필요 (PROC_NAME 그대로 매칭됨)

## 4. 실행 의미 (기존과 달라지는 점)

| 상황 | 기존 동작 | 전환 후 동작 |
|---|---|---|
| 정상 스케줄 실행 | 항상 실행 | 동일 — `statDate`(전일) JobInstance 신규 생성 |
| 같은 일자 재실행(수동) | 항상 전체 재실행 | COMPLETED면 `rerun.ts`를 더해 새 인스턴스로 전체 재실행 (DELETE+INSERT 멱등) |
| **실패 후 재실행** | 처음부터 전체 재실행 | **실패 지점부터 재개** — batday01은 실패한 Step부터, batday02 Chunk는 마지막 커밋 지점부터 |
| 동시 중복 실행 | HOST_NAME 필터만 (DB 락 없음) | JobRepository가 동일 JobInstance RUNNING이면 차단 → 이력에 FAIL + "중복 실행 차단" 기록 |
| 건수 기록 | BatchTaskResult 수기 반환 | JobExecutionContext(targetCnt/procCnt/errCnt) 우선, 없으면 Step read/write/skip 합산 자동 매핑 |

T_BATCH_HISTORY에는 기존과 동일하게 기록되고, **상세 실행 정보(Step별 건수·소요시간·실패 스택)는 BATCH_STEP_EXECUTION 등 메타테이블에서 추가로 조회**할 수 있다.

## 5. 신규 Job 작성 규칙

1. `batch/job/` 에 `@Configuration` 작성, `@Bean(name = "<PROC_NAME>")` 으로 Job 등록
2. `statDate` 는 `@StepScope` + `@Value("#{jobParameters['statDate']}")` 로 주입받는다 (Job 내부에서 날짜 계산 금지 — 어댑터가 전일 기준으로 일괄 주입)
3. SQL은 반드시 **대상일자 DELETE 후 INSERT 멱등** 구조 유지
4. 단문 SQL 배치 → Tasklet Step / 대량 행 처리 → Chunk Step(`MyBatisCursorItemReader` + `MyBatisBatchItemWriter`, Reader 쿼리에 ORDER BY 필수)
5. 집계 건수는 `JobExecutionContext` 의 `targetCnt`/`procCnt`/`errCnt` 키에 기록 (Chunk는 생략 시 read/write 자동 합산)
6. T_BATCH_JOB 에 PROC_NAME(=Job 빈 이름)·cron·USE_FLAG 등록 → 화면에서 즉시 관리 가능

## 6. MG 배치 이식 가능성 평가 (2026-06-10 기준)

> 기준: MG에서 **운영 사용 중인 배치만** 평가 ([미사용] batday31/35/36/39, TEST batday41, 샘플 batsec01 제외).
> "외부 의존성"은 MG의 EAI batchagent 셸(`batch.sh -i …`) 직접 실행을 의미하며, NH 내부 모듈 간 HTTP 연계는 이식 가능으로 본다.

### A. 이식 완료 / 이식 불필요 (NH에 대응 존재)

| MG 배치 | 기능 | 판정 근거 |
|---|---|---|
| batday01 | 장애율/가동률 | NH 스키마로 재작성 + Spring Batch 전환 완료 (`Batday01JobConfig`) |
| batday02 | 거래실적 집계 | 〃 (`Batday02JobConfig`, Chunk) |
| batday30 | 사용자 부서명 동기화 | **이번에 이식 완료** (`Batday30JobConfig`) — T_USER/T_ATM_GROUP 1:1 대응 |
| batday97 | 에러저널 재처리 | **이식 불필요** — atms-journal `JournalErrorRetryWorker`(1분 폴링, 재시도 횟수 관리)가 동일 역할을 이미 더 정교하게 수행 |

### B. 즉시 이식 가능 (외부 의존성 없음, NH 대응 확인됨)

| MG 배치 | 기능 | 비고 |
|---|---|---|
| batday99 | 파일 삭제(저널이미지/임시파일) | 파일시스템만 사용. atms-file 의 `FileStorageProperties` 경로 체계에 맞춰 설정만 정의하면 됨 |
| batday98 | 보관주기 경과 데이터 삭제 | 삭제 대상 테이블 대부분 NH 존재(T_JOURNAL, T_TRANSACTION_JOURNAL, T_ATM_MONITOR, T_NET_SEND/RECV_DATA, T_GW_INTERFACE, T_BATCH_HISTORY 등). 보관주기 정책 테이블(MG T_TABLEMANAGEMENT 상당)만 신설 필요 |
| batday03 | 동시다발장애 검출·전송 | 수신부(atms-if-external `ManyFailureRequest`)와 화면 도메인(atms-web FaultFrequent) 이미 존재. 집계 소스만 NH 기준(T_ATM_MONITOR)으로 변환 |

### C. 조건부 이식 (NH 스키마 단순화로 컬럼/테이블 부재 — 정책 확정 선행)

| MG 배치 | 기능 | 막히는 지점 |
|---|---|---|
| batday20 | 미개국 출동요청 | 수신부(`ServiceCallRequest`)·T_FAULT_MANAGEMENT 는 존재하나 **휴일 테이블(MG t_holiday 상당) 부재**, 개국 판단 소스(MG t_atmstophistory FO) 매핑 확정 필요 |
| batday43 / batday57 | 전자저널 모드 설정/변환 | NH T_AGENT_STATUS 가 단순화되어 **`jnl_default_mode`/`last_file_name` 컬럼 없음**. `JNL_MODE` + `LAST_POLL_DATE` 로 의미 번역할지 정책 결정 필요 |
| batday08 / batday09 | 일일보고서 집계 | 외부 의존성 없음. 단 **T_DAILYREPORT01~07 상당의 보고서 테이블이 NH에 미설계** — 보고서 도메인 설계 선행 |

### D. 보류 (MG EAI batchagent 셸 의존 — NH 대내 연계 아키텍처 상이)

batday15(금고 본지점), batday16(구우편번호), batday17(금고휴일), batday18(직원정보), batday19(신주소), batday21(기기 동기화), batday42/60(ETL 송신) — MG에서 `/eai/batchagent/bin/batch.sh -i <IF-ID>` 를 직접 실행하는 파일 연계 배치. NH의 대내 연계는 atms-if-internal(Gateway/Mock) HTTP 방식이라 **EAI 파일배치 대체 방안(중계 API? 파일 수신 채널?)이 확정되기 전에는 이식 불가**. batday18의 후처리 SQL(t_user 권한 보정)은 NH의 Role/UserGroup 모델로 재설계 대상.

## 7. 다음 단계 제안 (Phase 2~)

| 단계 | 내용 | 비고 |
|---|---|---|
| Phase 2 | B군 이식: ~~batday30~~(완료) → batday99(파일 삭제) → batday98(보관주기) → batday03 | batday98은 보관주기 정책 테이블 DDL 선행 |
| Phase 3 | C군 정책 확정 후 이식: batday20(휴일 테이블), batday43/57(저널모드 컬럼), batday08/09(보고서 도메인) | 스키마 설계 협의 필요 |
| Phase 4 | 수동 실행 API에 `statDate` 파라미터 추가 (소급 재집계) | `POST /api/batch/jobs/{proc}/run?statDate=` |
| Phase 5 | D군(EAI 연계) 대체 아키텍처 확정 후 이식 / 다중 인스턴스 HA 시 Quartz JDBC 클러스터 검토 | 스케줄러만 교체 — Job/어댑터는 영향 없음 |
| 선택 | T_BATCH_HISTORY 화면에 jobExecutionId 노출 → 메타테이블 상세 드릴다운 | 프론트 변경 수반 |

## 8. 검증 방법

```bash
# 빌드
./gradlew :atms-batch:build          # Phase 1 기준 BUILD SUCCESSFUL 확인됨

# 기동 후 수동 실행 (개발 DB에 DDL 적용 후)
curl -X POST http://localhost:8082/api/batch/jobs/batday02/run

# 확인 포인트
#  - T_BATCH_HISTORY: 기존 화면 그대로 SUCCESS + 건수 기록
#  - BATCH_JOB_INSTANCE/EXECUTION: statDate 파라미터로 인스턴스 생성
#  - BATCH_STEP_EXECUTION: batday02.deleteStep / batday02.loadStep 의 read/write 건수
#  - 중복 실행 테스트: 실행 중 한 번 더 호출 → FAIL("중복 실행 차단") 이력
```
