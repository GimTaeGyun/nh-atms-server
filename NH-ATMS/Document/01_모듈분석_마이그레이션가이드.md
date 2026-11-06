# 새마을금고 ATMS 6개 모듈 분석 → NH-ATMS 마이그레이션 가이드

> 분석일: 2026-05-14
> 원본 위치: `2.프로젝트/0.기본자료/새마을금고/{atms,atmr,atmb,atmf,atmi,atme}`
> 대상: NH-ATMS Spring Boot 3.5 / Java 21 멀티모듈

---

## 0. 공통 기술 스택 (AS-IS)

| 항목 | 새마을금고 | NH-ATMS (TO-BE) | 차이 |
|---|---|---|---|
| Java | **8** | **21** | record, sealed, pattern matching, virtual thread |
| Build | Maven (war) | Gradle (jar/bootJar) | 멀티모듈 |
| Spring | 4.3.x | Boot 3.5 | 자동설정, starter |
| 프레임워크 | **eGov 3.9** (`EgovAbstractServiceImpl`) | 순수 Spring | eGov 의존 제거 |
| Servlet | javax.servlet | **jakarta.servlet** | 모든 import 변경 |
| Validation | javax.validation | jakarta.validation | 동일 |
| MyBatis | 3.x + log4jdbc | mybatis-spring-boot-starter 3.0.4 | 그대로 사용 가능 |
| DI | `@Resource(name="...")` | `@RequiredArgsConstructor` (생성자 주입) | Lombok 활용 |
| JSON | Jackson + **Gson + json-lib (혼재)** | Jackson 통일 | 표준화 |
| DB | MySQL 8 | Oracle 19C | DDL/SQL 변환 |
| 로깅 | SLF4J + log4j | SLF4J + Logback | Boot 기본 |
| View | JSP | **분리** — Frontend(React) / Backend(REST) | JSP 폐기 |
| 패키징 | WAR (Tomcat 외부) | JAR (내장 Tomcat) → JEUS 9.1 배포 시 WAR 빌드 | bootWar |

---

## 1. atms (메인 화면 백엔드) — 가장 큰 모듈

### 통계
- Java **300개** / JSP **194개** / XML **67개** (MyBatis 매퍼 포함)
- 13개 도메인 패키지

### 도메인 매핑 (NH atms-web 으로)

| 새마을 도메인 | Java | JSP | NH 매핑 | 비고 |
|---|---|---|---|---|
| `device` | 20 | 9 | `web/atm/*` | T_ATM, T_BRANCH, T_CORNER (마스터) |
| `info` | 36 | 48 | `web/info/*` | 코너/지역/모델/벤더 등 정보 |
| `cash` | 24 | 8 | `web/cash/*` | 카세트/시재/권종 |
| `fault` | 20 | 22 | `web/fault/*` (+ atms-journal) | 장애 관리 (T_FAULTMANAGEMENT) |
| `obst` | 32 | 12 | `web/fault/*` | 장애 처리 (Obstacle) |
| `journal` | 4 | 7 | atms-journal | 저널 조회 화면 |
| `remote` | 18 | 12 | atms-journal | 원격명령 |
| `report` | 4 | 4 | `web/report/*` | 보고서 (T_DAILYREPORT01~07) |
| `user` | 9 | 11 | `common/user/*` | 사용자 관리 |
| `sys` | 12 | 14 | `common/sys/*` | 시스템 관리 (코드/메뉴) |
| `board` | 16 | 13 | `web/board/*` | 게시판 (공지/FAQ) |
| `main` | 7 | 194 | `web/main/*` | 메인 대시보드 (JSP 대량 = 화면 단편) |
| `cmm` | 38 | 7 | atms-common | 공통 유틸/엑셀/파라미터 |

### 표준 패턴 (새마을금고)

```java
// AS-IS: AtmController.java
@Controller
public class AtmController {
    @Resource(name = "atmService")
    protected AtmService atmService;

    @RequestMapping(value = "/device/atmList.do")
    public String selectAtmList(HttpServletRequest request, ModelMap model) throws Exception {
        atmService.selectAtmList(request, model);
        return "/device/atmList";
    }
}

// AS-IS: AtmServiceImpl.java
@Service("atmService")
public class AtmServiceImpl extends EgovAbstractServiceImpl implements AtmService {
    @Resource(name="atmMapper")
    private AtmMapper atmMapper;
    ...
}
```

### TO-BE 변환 패턴 (NH)

```java
// TO-BE: AtmController.java
@RestController
@RequestMapping("/api/atm")
@RequiredArgsConstructor
public class AtmController {
    private final AtmService atmService;

    @GetMapping("/list")
    public ApiResponse<PageResult<AtmDto>> list(@Valid AtmSearchDto search) {
        return ApiResponse.ok(atmService.search(search));
    }
}

// TO-BE: AtmService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtmService {
    private final AtmMapper atmMapper;
    public PageResult<AtmDto> search(AtmSearchDto search) { ... }
}
```

### 마이그레이션 체크리스트

| 항목 | 작업 |
|---|---|
| `extends EgovAbstractServiceImpl` | **제거** (불필요) |
| `@Controller` + `return "/path/page"` | `@RestController` + `ApiResponse<T>` 반환 |
| `HttpServletRequest` 파라미터 직접 받음 | DTO 바인딩 (`@RequestBody`, `@ModelAttribute`) |
| `ModelMap model` | 응답 DTO 반환 |
| `*.do` URL | `/api/{도메인}/{action}` REST 스타일 |
| `@Resource(name="xxx")` | `@RequiredArgsConstructor` (생성자 주입) |
| JSP 화면 | **전부 React 컴포넌트로 재구성** (Frontend 팀) |
| `EgovMap` (key:value 맵) | **DTO 클래스로 명시화** (Map 사용 금지) |
| MyBatis XML | **그대로 활용 가능** (단 Oracle DDL/페이징 변환 필요) |
| 페이징: MySQL `LIMIT` | Oracle `OFFSET ... FETCH FIRST ... ROWS ONLY` |

### 우선순위 (이행 순서)
1. **cmm** (공통 유틸) — 다른 도메인이 모두 의존
2. **user/sys** — 권한/메뉴는 기반
3. **device/info** — 마스터 (T_ATM, T_BRANCH, T_CORNER)
4. **journal/fault/obst** — 핵심 운영 화면
5. **cash/remote/report/board/main** — 부가 기능

---

## 2. atmr (전자저널/원격명령) — 핵심 실시간 처리

### 통계
- Java **64개** / XML **15개** / JSP 1개 (관리 화면)
- 패키지: `agent`, `journal/processor`, `journal/worker`, `journal/filter`

### 핵심 컴포넌트

| 컴포넌트 | 역할 | NH 매핑 |
|---|---|---|
| `AgentController` | 에이전트 등록/하트비트 수신 | `atms-journal` AgentController |
| `RecvJournalServiceImpl` | 저널 수신 디스패처 | `RecvJournalService` |
| `processor/APStart, Cash, Open, Operation, StatusFailure` | 저널 타입별 처리기 | **Strategy 패턴 유지** |
| `worker/*` | 비동기 백그라운드 워커 | `@Async` + `@Scheduled` |
| `filter/*` | 저널 필터링 | `Predicate<Journal>` |
| `JournalType` (enum) | 저널 종류 | 그대로 활용 |

### 표준 패턴

```java
// AS-IS: RecvJournalServiceImpl
@Service("recvJournalService")
public class RecvJournalServiceImpl extends EgovAbstractServiceImpl implements RecvJournalService {
    @Resource(name = "recvJournalMapper")
    private RecvJournalMapper recvJournalMapper;
    private final ApplicationContext applicationContext;  // ← processor 동적 lookup
    
    public Map<String, Object> processJournal(HttpServletRequest req, JsonObject jnl) {
        // 1) 저널 타입 판별
        // 2) ApplicationContext에서 processor bean 조회
        // 3) processor.process() 호출
    }
}
```

### TO-BE 변환 패턴

```java
// TO-BE: ProcessorRegistry — 정적 매핑으로 명확히
@Component
@RequiredArgsConstructor
public class JournalProcessorRegistry {
    private final Map<JournalType, JournalProcessor> processors;

    public JournalProcessorRegistry(List<JournalProcessor> all) {
        this.processors = all.stream()
            .collect(toMap(JournalProcessor::type, identity()));
    }

    public JournalProcessor get(JournalType type) {
        return Optional.ofNullable(processors.get(type))
            .orElseThrow(() -> new BusinessException("JNL_001", "지원하지 않는 저널 타입"));
    }
}

// TO-BE: 처리기 인터페이스
public interface JournalProcessor {
    JournalType type();
    void process(JournalContext ctx);
}

@Component
@RequiredArgsConstructor
class CashProcessor implements JournalProcessor {
    private final RecvJournalMapper mapper;

    @Override public JournalType type() { return JournalType.CASH; }
    @Override public void process(JournalContext ctx) { ... }
}
```

### 마이그레이션 핵심 포인트

| 항목 | 작업 |
|---|---|
| `JsonObject` (Gson) | **`Map<String, Object>` 또는 DTO** + Jackson |
| `EgovMap` 반환 | DTO 또는 `record` 활용 |
| `ApplicationContext.getBean(name)` | **Strategy Map 패턴** (위 예) |
| 워커 스레드 직접 생성 | `@Async` + `ThreadPoolTaskExecutor` |
| 필터 체인 | Java 8 `Predicate.and()` 활용 (이미 가능) |
| **저널 수신은 atms-journal API** | HTTP 엔드포인트로 노출 (`POST /api/journal/recv`) |
| **에이전트 → ATMS HTTP 통신** | RFP에서 운영기기는 HTTP 확정 |

---

## 3. atmb (배치) — task 클래스 다수

### 통계
- Java **28개** (대부분 `batday01.java` ~ `batday43.java` 일배치)
- XML 0 (자체 SqlSession 사용)
- 패키지: `atmb.batch.task`

### 주요 task

| 클래스 | cron | 역할 (주석에서) |
|---|---|---|
| `batday01` | `00 00 01 * * ?` | 장애율/가동률 배치 |
| `batday02` | (확인 필요) | 거래집계 등 |
| `batday03~43` | 각종 일배치 | T_DAILYREPORT01~07, T_DAYTOTAL, T_TRANSACTIONDAILYTOTAL 생성 |

### 표준 패턴

```java
// AS-IS
public class batday01 extends BatchScheduler.Task {
    public static class RunHistory extends Struct { ... }  // ← 내부 클래스로 DTO
    
    @Override
    public void execute() {
        try (Connection con = ...; PreparedStatement ps = ...; ResultSet rs = ...) {
            // 직접 JDBC + Json 처리
        }
    }
}
```

### TO-BE 변환 패턴

```java
// TO-BE: Spring Batch Job + Quartz Scheduler
@Configuration
@RequiredArgsConstructor
public class BatDay01JobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final RunHistoryMapper mapper;

    @Bean
    public Job batDay01Job() {
        return new JobBuilder("batDay01", jobRepository)
            .start(calculateFaultRateStep())
            .next(saveRunHistoryStep())
            .build();
    }
}

@Component
@RequiredArgsConstructor
public class BatDay01Trigger {
    private final JobLauncher jobLauncher;
    private final Job batDay01Job;

    @Scheduled(cron = "0 0 1 * * ?")
    public void run() throws Exception {
        jobLauncher.run(batDay01Job, new JobParametersBuilder()
            .addLocalDate("baseDate", LocalDate.now().minusDays(1))
            .toJobParameters());
    }
}
```

### 마이그레이션 핵심

| 항목 | 작업 |
|---|---|
| `BatchScheduler.Task` 자체 추상클래스 | **Spring Batch 표준 (Job/Step/Tasklet)** |
| 직접 JDBC | `JdbcTemplate` 또는 MyBatis 매퍼 |
| `Struct` 내부 클래스 | `record` 또는 별도 DTO |
| 시간 포맷터 인스턴스 변수 | `private static final` |
| 병렬 실행 제어 | Spring Batch `partitioning` / `parallelStep` |
| 재시도 | `RetryTemplate` / `@Retryable` |
| **43개 task → Job 그룹화** | 도메인별 그룹 (장애/거래/시재/보고서) |

### 우선순위
1. **공통 인프라** — JobRepository, JobLauncher, TaskExecutor 설정
2. **batday01 (장애/가동률)** — 운영 KPI 핵심
3. **거래집계 (batday02 류)** — 보고서 base 데이터
4. 나머지 일/월 배치 점진 이행

---

## 4. atmf (파일/펌웨어) — 거의 빈 모듈

### 통계
- Java **0개** / JSP 1개
- → 새마을금고에서는 **별도 모듈로 거의 사용 안 함** (다른 모듈에 분산되어 있을 가능성)

### NH atms-file 설계 방향
새마을금고 atmf 가 비어 있으니 **신규 설계** 필요. 참고 데이터:
- T_ATMDOWNLOAD, T_ATMFILEINFO, T_ATMAPPLYHISTORY, T_DOWNLOADPROCESS, T_DOWNLOADSERVER
- 펌웨어/소프트웨어 다운로드 서버 역할

```java
// 신규 설계 (atms-file 모듈)
@RestController
@RequestMapping("/api/file")
public class FileDownloadController {
    @PostMapping("/upload")        // 펌웨어 등록
    @GetMapping("/list")            // 다운로드 목록
    @PostMapping("/dispatch")       // ATM 다운로드 지시 (배포 트리거)
    @GetMapping("/{fileId}/binary") // ATM 에이전트가 다운로드
}
```

→ **atms-file 모듈은 RFP 요구사항 + Saemaul T_ATM* 다운로드 테이블 기반으로 PL이 신규 설계**

---

## 5. atmi (대내 인터페이스) — 작고 명확

### 통계
- Java **11개** / 패키지: `kr.go.atmi.internal.{web,service,mapper,util,proc,constants}`

### 컴포넌트

| 파일 | 역할 |
|---|---|
| `InternalInterfaceController` | **`HttpServlet` 직접 상속** (`@WebServlet`) — Spring 미사용 |
| `InternalInterfaceService` | **new 직접 생성** — DI 미사용 |
| `proc/ATMBYDD10100_OUT.java` | 시재조회 OUT 전문 |
| `proc/ATMONST10100_OUT.java` | 시재조회 OUT 전문 (TCP) |
| `proc/SMSONSS10200_OUT.java` | UMS SMS 장애알림 OUT |
| `util/MessageMakeUtil` | 전문 생성 유틸 |
| `util/SequenceNoUtil` | 전문 일련번호 채번 |

### 처리하는 인터페이스 (URL 매핑에서)

| URL | 전문 ID | 설명 | 방식 |
|---|---|---|---|
| `/int-if/getDrawerCheck.do` | ATMONST10100 | 시재 조회 | **TCP Socket** |
| `/int-if/sendFaultSms.do` | SMSONSS10200 | UMS SMS 장애알림 | TCP Socket |
| `/int-if/sendFaultMessenger.do` | (M-Genie) | 메신저 장애알림 | **DB to DB** |

### TO-BE 변환

```java
// TO-BE: 표준 Spring REST + 명확한 처리기
@RestController
@RequestMapping("/api/internal-if")
@RequiredArgsConstructor
public class InternalInterfaceController {
    private final InternalIfDispatcher dispatcher;

    @PostMapping("/drawer-check")
    public ApiResponse<DrawerCheckResponse> drawerCheck(@RequestBody DrawerCheckRequest req) {
        return ApiResponse.ok(dispatcher.send(InternalIfId.ATMONST10100, req));
    }
    @PostMapping("/fault-sms")
    public ApiResponse<Void> faultSms(@RequestBody FaultSmsRequest req) { ... }
}

// 전문 송수신 추상화 (TCP Socket / DB-to-DB 모두 지원)
public interface InternalIfChannel {
    InternalIfId supports();
    Object send(Object request);
}
```

### 마이그레이션 핵심

| 항목 | 작업 |
|---|---|
| `HttpServlet` + `@WebServlet` | `@RestController` + `@PostMapping` |
| `new InternalInterfaceService()` | DI (`@RequiredArgsConstructor`) |
| `JsonObject` (Gson) | DTO + Jackson |
| `_OUT` 전문 클래스 | **그대로 활용** (전문 포맷은 변경 없음, 단 enum/record 활용) |
| TCP Socket | Spring Integration TCP / Netty |
| DB to DB (M-Genie) | **별도 DataSource Bean** + `@Qualifier` |

---

## 6. atme (대외 인터페이스) — 대외 채널

### 통계
- Java **23개** / 패키지: `kr.go.atms.ext.*`

### 처리 전문 (URL 매핑 + proc 클래스)

| URL | 전문 ID | 설명 |
|---|---|---|
| `/ext-if/openFailureStatus.do` | EM_01001110 | 개국/장애/상태 통보 (OUT) |
| `/ext-if/serviceCallRequest.do` | EM_01001120 | 출동 요청 (OUT) |
| (서비스 호출 응답) | EM_01001130 | 출동 응답 (IN) |
| (서비스 콜 결과) | EM_01001140 | 출동 결과 (IN) |
| (서비스 진행) | EM_01001150 | 진행상황 (IN) |
| `/ext-if/serviceCallCancel.do` | EM_01001160 | 출동 취소 (OUT) |
| `/ext-if/serviceCallChange.do` | EM_01001170 | 출동 정보변경 (OUT) |
| `/ext-if/manyFailureRequest.do` | EM_01001180 | 동시 다발장애 (OUT) |
| (다발장애 응답) | EM_01001190 | 다발장애 응답 (IN) |
| `/ext-if/cornerChange.do` | SM_02001160 | 코너 변경 요청 (OUT) |
| (코너 변경 응답) | SM_02001180 | 코너 변경 응답 (IN) |
| (코너 정보 통보) | SM_02001190 | 코너 정보 통보 (IN) |
| (조회 요청) | IQ_04001110 | 조회 (IN) |
| (조회 추가) | IQ_04001130 | 조회 추가 (IN) |
| `/ext-if/machineInfoNotification.do` | IQ_04001140 | 기기 정보 통보 (OUT) |

> ⚠ **이 17개 전문이 곧 NH-ATMS 대외 IF 핵심 카탈로그**.
> NH는 RFP상 **API GW + FOS** 통해 외부 채널과 연동 → 다음 단계(IF 명세서 분석)에서 NH 전문 ID로 변환 매핑 필요.

### 마이그레이션 핵심

| 항목 | 작업 |
|---|---|
| atmi와 동일 (HttpServlet → RestController) | 동일 |
| 전문 ID 체계 (EM_/SM_/IQ_) | **Enum 으로 모델링** (`ExternalIfId.EM_01001110`) |
| OUT/IN 분리 | `Outbound{IfId}Service` / `Inbound{IfId}Controller` |
| **NH FOS/API GW 연동** | RestTemplate / WebClient + 인증/암호화 (NH 표준) |

---

## 7. 종합 의존성 / 변환 매트릭스

### 라이브러리 변환 표

| 새마을금고 | 버전 | NH-ATMS | 버전 | 변환 |
|---|---|---|---|---|
| commons-dbcp | 1.4 | HikariCP | (Boot 기본) | DataSource 설정만 변경 |
| mysql-connector-j | 8.0.32 | ojdbc11 | 23.x | 드라이버 교체 |
| log4jdbc | 1.16 | p6spy (선택) | - | 또는 제거 |
| log4j | 1.2 | logback (Boot 기본) | - | logback-spring.xml |
| Jackson | 2.5.1 | Jackson | (Boot 관리) | 자동 |
| Gson | 2.12.1 | **제거** | - | Jackson 으로 통일 |
| json-lib | 2.4 | **제거** | - | Jackson 으로 통일 |
| eGov framework | 3.9.0 | **제거** | - | 순수 Spring |
| spring-* | 4.3.22 | spring-boot 3.5 | - | starter 사용 |
| servlet-api | 3.1 (javax) | (Boot 내장) | jakarta | import 변경 |
| Lombok | 1.18.38 | Lombok | (Boot 관리) | 그대로 |
| poi | 3.x (추정) | poi-ooxml 5.3.0 | - | API 일부 변경 (`HSSFCell` enum) |

### 코드 변환 자동화 가능 항목

```bash
# javax → jakarta 일괄 치환 (테스트 후 적용)
find . -name "*.java" -exec sed -i \
  -e 's/javax\.servlet/jakarta.servlet/g' \
  -e 's/javax\.validation/jakarta.validation/g' \
  -e 's/javax\.persistence/jakarta.persistence/g' \
  -e 's/javax\.annotation\.Resource/jakarta.annotation.Resource/g' \
  {} \;

# @RequestMapping → @GetMapping/@PostMapping (수동 검토)
grep -rn '@RequestMapping(value' --include="*.java"
```

### 자동화 불가 항목 (수동 변환)

- `@Controller` + `return "xxx"` (JSP) → `@RestController` + DTO
- `EgovMap` → DTO 클래스
- `extends EgovAbstractServiceImpl` → 단순 `@Service`
- 화면(JSP) → React 컴포넌트
- DI 방식: `@Resource(name=)` → 생성자 주입
- JSON 라이브러리 통일

---

## 8. 마이그레이션 전체 순서 (PL 권장)

```
Phase 1 (분석 단계 후반, ~2026.07):
  □ 새마을금고 atms.cmm 패키지 → NH atms-common 으로 이식
  □ atms.user/sys → NH atms-common (사용자/권한/메뉴)

Phase 2 (설계 단계, 2026.08~09):
  □ atms.device/info → NH atms-web (마스터 화면)
  □ DB DDL 확정 (oracle19c_ddl.sql + 신규 테이블)
  □ MyBatis 매퍼 표준 (페이징/공통 ResultMap)

Phase 3 (구현 단계 초반, 2026.10~):
  □ atmr → NH atms-journal (저널 수신/처리)
  □ atms.fault/obst → NH atms-journal+web (장애)
  □ atmi → NH atms-if-internal (대내 IF)
  □ atme → NH atms-if-external (대외 IF)

Phase 4 (구현 단계 후반):
  □ atmb → NH atms-batch (Spring Batch 재구성)
  □ atms.report → NH atms-web (보고서)
  □ atms-file 신규 설계 (Saemaul atmf 빈 모듈)

Phase 5 (테스트):
  □ 전문 호환성 (atmi/atme 17개 전문 + 신규 NH 전문)
  □ 부하 테스트 (저널 수신 RPS)
```

---

## 9. 위험 요소 (R12 추가)

| ID | 항목 | 영향 | 대응 |
|---|---|---|---|
| **R12** | atmf 모듈이 Saemaul에 거의 없음 → NH는 5,374대 다운로드 처리 필요 | 中 | 효성/청호 등 타사 패키지의 다운로드 서버 패턴 참조, **PoC 우선** |
| R13 | atmb 43개 task의 cron/의존관계 미문서화 | 中 | 분석 단계에서 task 헤더 주석 추출, Mermaid 의존도 작성 |
| R14 | atms.main 의 JSP 194개 (대시보드 단편) → React 재구성 공수 큼 | 高 | Frontend 팀과 페이지 리스트 조기 공유, 공통 컴포넌트 식별 |
| R15 | eGov 의존 제거 시 일부 기능(채번, 파일첨부) 자체 구현 필요 | 中 | Phase 1 cmm 이식 시 우선 구현 |

---

## 10. 다음 단계 (이 문서 이후)

→ **IF 명세서 분석** (`새마을ATM재구축_AGENT_ATM_IF_전체_20251112.txt`)
   - atmi/atme 의 17개 전문 + ATM 에이전트 ↔ 서버 전문 전체 카탈로그
   - NH 표준 전문 형식과 매핑

→ **MyBatis 매퍼 + Entity/DTO 생성**
   - atms-web 에 T_ATM, T_BRANCH 등 핵심 5~10개 테이블 실제 코드 추가
   - 본 문서의 TO-BE 패턴이 그대로 적용됨
