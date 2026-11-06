# NH-ATMS 소스 변경 설명 — 컨테이너 6개 → JEUS 1대에 WAR 6개 (경로 분리)

작성일: 2026-10-12 · 개정: 2026-10-13 (보완 4건, 4-7) · 브랜치 `main` · 대상 소스 `NH-ATMS/Backend/nh-atms-api`

> **현재 구조 아님 (2026-11-03 표시)** — 이 문서는 **JEUS 서버 1개에 WAR 6개(경로 분리) + atms-web 게이트웨이 + WAR 안 Hikari**(2026-10-12~25) 소스 설명. HMAC 컨텍스트 대응·배치 스케줄러 종료·orai18n 등은 지금도 소스에 남아 있다 기준이다. 현재는 **JEUS 매니지드 서버 6개**(atms01·ajou01·abat01·file01·ifin01·ifex01, 포트 8080~8085, 컨텍스트 `/`, 게이트웨이 끔, DB 는 JEUS JNDI `dsATMS01`). 지금 할 작업은 [INFRA_MANUAL.md](INFRA_MANUAL.md) 를 따르고, 결정 배경은 [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md). 아래 본문은 이력·참고용으로 남긴다.

> 2026-10-13~15 추가 검토(게이트웨이 필요성·WebtoB rewrite 대안·성능 실측·스레드 고갈·모듈 단독 배포): 12장

> 폐쇄망 PC 소스(업체 원본)에 파일별로 옮겨 적는 순서·전체 내용: [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md)

## 0. 한눈에 보기

| 항목 | 기존 (업체 개발환경) | 변경 후 (은행 개발서버 `nbatmsltws01`) |
|---|---|---|
| 실행 | 모듈마다 컨테이너 1개 (docker compose), 내장 톰캣 | **JEUS 9.1 `atms01` 서버 1개(JVM 1개)** 안에 WAR 6개 |
| 구분 방법 | 포트 (8080~8085) | **컨텍스트 경로** (`/`, `/journal`, `/batch`, `/file`, `/ifin`, `/ifex`) |
| 외부 입구 | nginx 가 경로별로 각 컨테이너에 분배 | WebtoB → JEUS 루트(`/` = atms-web) → **atms-web 게이트웨이**가 모듈 컨텍스트로 전달 |
| 빌드 산출물 | `bootJar` (실행형 JAR) | `bootWar -Pjeus` (WAR, 내장 톰캣 제외) |
| 설정 | 환경변수(`.env`) + `application-local.yml` | WAR 안의 `application-jeus.yml` + `jeus-common.yml` (프로파일 `jeus`) |
| DB 연결 | Spring Hikari | **그대로 Spring Hikari** (JEUS 데이터소스 `dsATMS01` 는 사용하지 않음) |
| 컨테이너(Podman) | 앱 실행 | 앱은 컨테이너로 돌리지 않음. **Gitea·Jenkins 같은 지원 인프라만** (+ JEUS 준비 전 임시 실행) |

로컬 개발(`bootRun`, IntelliJ)과 기존 JAR/컨테이너 빌드는 **바뀌지 않는다**. 위 변경은 `-Pjeus` 로 빌드할 때만 적용된다.

### 결정 내역 (2026-10-12 질의응답)

| 질문 | 선택 |
|---|---|
| 모듈 배치 | **A. 경로 분리**: JEUS 서버 1개, WAR 6개, 컨텍스트로 구분 (B. WAR 1개 통합은 충돌이 많아 제외 — 2장) |
| WAR 로 만들 범위 | **6개 모듈 전부** |
| 운영 설정 위치 | **WAR 안에 포함** (`application-jeus.yml`) |
| DB 연결 | **Spring Hikari 유지** |
| 참고 | JEUS 는 현업(WAS 담당)이 설치함 → 앱 등록·리스너·JVM 옵션은 현업에 요청 (7장) |

---

## 1. 동작 구조

```
 PC 브라우저 / ATM 에이전트 / 대외기관
        │  http://nbatmsltwb01:9000  (WebtoB)
        ▼
 WebtoB ── 정적 화면(/home/atmswas/docs) 은 직접 응답
        │  /api/  /atms/  /atmr/  /atmf/  /swagger-ui/  /v3/api-docs  /actuator/  → JEUS(JSV)
        ▼
 JEUS atms01 (JVM 1개, HTTP 리스너 8766 — 2026-10-15 domain.xml 확인)
  ├─ /          atms-web          ← 외부 요청이 들어오는 유일한 앱
  │                ├─ 자기 API (/api/auth, /api/atm, ...) 처리
  │                ├─ LegacyAgentGatewayController : /atms/agent/**, /atmr/agent/**, /atms/remote-exec/** → /journal
  │                └─ ModuleGatewayFilter (신규)   : /api/agent, /api/journal → /journal
  │                                                  /api/batch → /batch,  /api/file, /atmf → /file
  │                                                  /api/if/internal → /ifin,  /api/if/external → /ifex
  ├─ /journal   atms-journal
  ├─ /batch     atms-batch
  ├─ /file      atms-file
  ├─ /ifin      atms-if-internal
  └─ /ifex      atms-if-external
        모듈끼리 호출: http://127.0.0.1:<JEUS HTTP 포트>/<컨텍스트>/...  (jeus.base-url)
```

왜 게이트웨이가 필요한가:
- 화면(React)은 `/api/batch/...`, `/api/file/...` 처럼 **컨텍스트 없는 고정 경로**를 상대경로로 부른다.
- ATM 에이전트 펌웨어는 `/atms/agent/...`, `/atmf/agent/...`, `/api/agent/...` 를 **바꿀 수 없다**.
- 컨텍스트 경로로 나누면 이 경로들은 JEUS 에서 모두 루트(`/` = atms-web)로 들어온다 → atms-web 이 접두어를 보고 해당 모듈로 넘긴다.
- 결과: **프론트엔드·에이전트·대외기관 쪽은 수정이 없다.** WebtoB 도 루트 하나로만 보내면 된다.

---

## 2. B(WAR 1개 통합)를 택하지 않은 이유 (소스 실측)

6개 모듈을 한 WAR(한 Spring 컨텍스트)로 합치면 아래 충돌이 생긴다 (2026-10-12 소스 조사).

| 충돌 | 개수 | 내용 |
|---|---|---|
| MyBatis 타입 별칭 중복 | 11 | 모듈마다 같은 이름의 엔티티 클래스 → SqlSessionFactory 기동 실패 |
| 빈 이름 중복 | 5 | 같은 클래스명(@Component/@Service) → 기동 실패 |
| Spring Security 필터 체인 | 5 | 모듈별 `@Order` 체인이 서로 덮어씀 (journal HMAC, file/IF permitAll …) |
| `application.yml` | 6 | `server.port`, `mybatis.type-aliases-package` 등 같은 키에 다른 값 |
| `@SpringBootApplication` | 6 | 진입점 1개로 합쳐야 함 |

→ 모듈 경계를 그대로 두는 **A(경로 분리)** 가 수정량이 가장 적고, 기존 컨테이너 구성과 1:1 로 대응된다.

---

## 3. 변경 파일 목록

경로는 `NH-ATMS/Backend/nh-atms-api/` 기준.

| 파일 | 구분 | 내용 |
|---|---|---|
| `build.gradle` (루트) | 수정 | `-Pjeus` 일 때 Spring Boot 모듈 6개 모두 `war` 플러그인 + 내장 톰캣 `providedRuntime` + `WEB-INF/web.xml` 포함 |
| `atms-web/build.gradle` | 수정 | atms-web 전용 `-Pjeus` 블록 삭제 (루트로 이동) |
| `jeus/web.xml` | **신규** | WAR 에 들어가는 `web.xml` 템플릿. `spring.profiles.active=jeus` 지정 |
| `atms-*/…/Atms*Application.java` (6개) | 수정 | `SpringBootServletInitializer` 상속 (atms-web 은 이전에 반영) — JEUS 가 앱을 시작하는 진입점 |
| `atms-common/src/main/resources/jeus-common.yml` | **신규** | 6개 모듈 공통값: DB, JWT, 모듈 간 주소, 로그·데이터 폴더 |
| `atms-*/src/main/resources/application-jeus.yml` (6개) | **신규** | 모듈별 값: 로그 파일명, 다른 모듈 주소, 저장 경로, (web) 게이트웨이 경로표 |
| `atms-web/…/web/gateway/ModuleGatewayProperties.java` | **신규** | 게이트웨이 설정 (`gateway.enabled`, 경로표, 타임아웃) |
| `atms-web/…/web/gateway/ModuleGatewayFilter.java` | **신규** | 접두어별로 모듈 컨텍스트로 전달하는 리버스 프록시 필터 |
| `atms-web/…/web/gateway/ModuleGatewayConfig.java` | **신규** | `gateway.enabled=true` 일 때만 필터 등록 |
| `atms-web/src/test/…/gateway/ModuleGatewayFilterTest.java` | **신규** | 전달·통과·502 단위 테스트 3건 |
| **2026-10-13 보완** (4-7) | | |
| `atms-journal/…/journal/security/HmacAuthFilter.java` | 수정 | 적용 경로 판정을 컨텍스트 경로를 뺀 경로로 (JEUS `/journal` 에서 HMAC 검증이 빠지던 결함) |
| `atms-web/…/web/gateway/ModuleGatewayFilter.java` | 수정 | 연결 실패 시 `sendError` 대신 502 JSON 을 직접 응답 (401 로 바뀌어 로그아웃되던 문제) |
| `atms-web/src/main/resources/application.yml` | 수정 | `local` 프로파일 문서에 게이트웨이 켜기 (대상 `localhost:8081~8085`) |
| `atms-batch/…/batch/scheduler/DynamicBatchScheduler.java` | 수정 | `@PreDestroy` 로 잡 해제·스케줄러 종료 (앱 단위 재배포 시 스레드 잔류 방지) |
| `HmacAuthFilterTest`, `DynamicBatchSchedulerTest` | **신규** | 위 2건 단위 테스트 (4건, 1건) |

배포킷 쪽: `deploy/jeus/swap-deploy.sh` 가 WAR 이름으로 모듈별 배포 폴더를 자동 선택 (7-2).

---

## 4. 변경 상세

### 4-1. 빌드 — `build.gradle` (루트, `subprojects {}` 안)

```groovy
pluginManager.withPlugin('org.springframework.boot') {
    if (project.hasProperty('jeus')) {
        apply plugin: 'war'
        dependencies {
            providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat'
        }
        def jeusProfile = project.findProperty('jeusProfile') ?: 'jeus'
        tasks.named('bootWar') {
            from(rootProject.file('jeus/web.xml')) {
                into 'WEB-INF'
                expand(profile: jeusProfile, moduleName: project.name)
            }
        }
    }
}
```

| 부분 | 의미 |
|---|---|
| `withPlugin('org.springframework.boot')` | Spring Boot 플러그인을 쓰는 6개 모듈에만 적용 (`atms-common` 라이브러리는 제외) |
| `hasProperty('jeus')` | `-Pjeus` 를 줄 때만. 없으면 기존과 100% 동일 |
| `providedRuntime …tomcat` | 내장 톰캣을 `WEB-INF/lib` 가 아닌 `WEB-INF/lib-provided` 로 → JEUS 가 서블릿 컨테이너 역할. 배포 스크립트가 `lib-provided` 를 지운다 |
| `web.xml` 복사 | 템플릿의 `${profile}`, `${moduleName}` 을 채워 `WEB-INF/web.xml` 로 넣음. `-PjeusProfile=<이름>` 으로 다른 프로파일(예: 검증서버용) 지정 가능 |

빌드 명령:
```bat
gradlew.bat bootWar -Pjeus --offline -x test               :: 6개 모두
gradlew.bat :atms-journal:bootWar -Pjeus --offline -x test :: 하나만
```
결과: `<모듈>\build\libs\<모듈>-0.0.1-SNAPSHOT.war` (2026-10-12 실측 66~105MB, journal 이 가장 큼)

### 4-2. 프로파일 지정 — `jeus/web.xml` (신규)

```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="5.0" metadata-complete="false">
    <display-name>${moduleName}</display-name>
    <context-param>
        <param-name>spring.profiles.active</param-name>
        <param-value>${profile}</param-value>
    </context-param>
</web-app>
```
- 왜 `web.xml` 인가: 6개 앱이 **JVM 하나를 공유**하므로 JVM 옵션 `-Dspring.profiles.active` 는 6개 모두에 똑같이 걸린다. 앱마다 WAR 안에서 지정하는 게 안전하다.
- 우선순위: `application.yml` 의 기본값 `local` 보다 높다 → JEUS 에서는 자동으로 `jeus` 프로파일. (단 JEUS JVM 옵션·OS 환경변수에 `spring.profiles.active` 가 있으면 그쪽이 이긴다 → **넣지 않도록 현업에 요청**)
- `metadata-complete="false"`: JEUS 가 WAR 안의 `ServletContainerInitializer`(Spring 시작점)를 찾도록 둔다.

### 4-3. 시작점 — `Atms*Application.java` (6개)

```java
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
public class AtmsJournalApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsJournalApplication.class);
    }

    public static void main(String[] args) {   // 로컬 bootRun 용, 그대로
        SpringApplication.run(AtmsJournalApplication.class, args);
    }
}
```
외부 WAS 는 `main()` 을 부르지 않는다. JEUS → `SpringServletContainerInitializer` → 이 클래스의 `configure()` 로 Spring 이 시작된다.
`main()` 은 그대로라 로컬 실행은 영향 없다.

### 4-4. 설정 — `jeus-common.yml` + 모듈별 `application-jeus.yml`

읽히는 순서:
```
application.yml (기본값, profile=local)
  └ application-jeus.yml (web.xml 이 jeus 프로파일 활성화)
      └ spring.config.import: classpath:jeus-common.yml  (atms-common jar 안, 6개 공통)
JVM 옵션(-D...) · OS 환경변수 가 있으면 위 파일보다 우선
```

**`jeus-common.yml` — 반드시 채울 값 (`CHANGE_ME`)**

| 키 | 기본값 | 넣을 값 / 확인처 |
|---|---|---|
| `jeus.base-url` | `http://16.88.122.113:8766` | atms01(`nbatmsltws01_atms01`) 의 **HTTP 리스너 포트** — 2026-10-15 `domain.xml` 확인: base 리스너 8766 을 HTTP 리스너가 공유. 2026-10-23 `ss -ltn`: 16.88.122.113 에만 LISTEN (127.0.0.1 불가). 모듈 간 호출·게이트웨이가 사용 |
| `jeus.log-dir` | `/WASlogs/jeusATMSlogs/nbatmsltws01/app` | `jeusatms` 가 쓸 수 있는 폴더 |
| `jeus.data-dir` | `/home/atmswas/data` | 업로드·이미지·다운로드 저장. `jeusatms` 쓰기 가능해야 함 (7-3) |
| `jeus.agent-entry.host/port/schema` | `CHANGE_ME_AGENT_ENTRY_HOST` / 9000 / http | ATM 에이전트가 접속할 주소 (보통 WebtoB). journal 이 에이전트 open 응답에 내려줌 |
| `spring.datasource.url` | `jdbc:oracle:thin:@//CHANGE_ME_DB_HOST:1521/NBATMSTP` | DB 호스트 (DBA). 서비스명 NBATMSTP |
| `spring.datasource.username` | `ATMS_APP` | 환경구성표 |
| `spring.datasource.password` | `CHANGE_ME_DB_PASSWORD` | DBA |
| `jwt.secret` | `CHANGE_ME_JWT_SECRET_AT_LEAST_32_BYTES_LONG` | 32바이트 이상 임의 문자열. **6개 모듈이 같은 값** (web 이 발급한 토큰을 batch 등이 검증) |
| `cors.allowed-origins` | `http://CHANGE_ME_WEBTOB_HOST:9000` | WebtoB 주소 |

> **비밀값은 내부망 소스(Gitea)에서만 채운다.** GitHub 등 외부에는 `CHANGE_ME` 상태로 둔다.
> WAR 에 비밀번호가 들어가므로 WAR 파일도 비밀 취급 (10장 주의 2).

**모듈별 `application-jeus.yml` 요약**

| 모듈 | 컨텍스트 | 지정 내용 |
|---|---|---|
| atms-web | `/` | 로그 `atms-web.log` · 레거시 에이전트 → `${jeus.base-url}/journal` · 원격제어 송신 → `/file/atmf/snd.msg.do` · 공지 업로드·다운로드 저장 경로 · **게이트웨이 경로표** |
| atms-journal | `/journal` | 로그 · IF 큐 → `/ifin`, `/ifex` · 에이전트 접속 정보(`jeus.agent-entry`) |
| atms-batch | `/batch` | 로그 |
| atms-file | `/file` | 로그 · 비동기 응답 전달 → `/atms/remote-exec/put.msg.resp.do`(web 경유 journal) · 파일 저장소 3곳 |
| atms-if-internal | `/ifin` | 로그 |
| atms-if-external | `/ifex` | 로그 |

기존 `application.yml` 의 `server.port` 는 외부 WAS 에서 무시되므로 그대로 둔다.

### 4-5. 게이트웨이 — `ModuleGatewayFilter` (atms-web, 신규)

| 항목 | 동작 |
|---|---|
| 켜는 조건 | `gateway.enabled=true` — `application-jeus.yml`(JEUS) 과 `application.yml` 의 `local` 프로파일 문서(로컬, 4-7). 컨테이너(프로파일 `dev`)는 nginx 가 라우팅하므로 등록 안 됨 |
| 순서 | 서블릿 필터 중 가장 앞 (`HIGHEST_PRECEDENCE + 10`) → Spring Security 보다 먼저. **인증·HMAC 검증은 대상 모듈의 보안설정이 그대로 판단** (헤더·본문을 바꾸지 않음) |
| 경로 일치 | `요청경로 == 접두어` 또는 `접두어/` 로 시작 (`/api/batchx` 는 불일치). 위에서부터 첫 일치 |
| 전달 주소 | `target + 원래 경로 + ?쿼리` (예: `/api/batch/jobs?x=1` → `http://16.88.122.113:8766/batch/api/batch/jobs?x=1`) |
| 본문 | 스트림 그대로 (JSON·form·multipart·바이너리, chunked 포함). 길이를 알면 Content-Length 유지 |
| 헤더 | hop-by-hop(`Connection`, `Transfer-Encoding` 등)·`Host` 제외 전부 전달. `X-Forwarded-For` 에 직전 IP 추가, `X-Forwarded-Proto/Host` 설정 → 대상 모듈이 원 클라이언트 IP 로 로그·HMAC 처리 |
| 응답 | 상태코드·헤더·본문 그대로 스트림 (파일 다운로드 포함) |
| 실패 | 대상에 연결 불가 → **502** + JSON 본문 `{"success":false,"code":"502",...}` (로그 `[module gateway] ... 전달 실패`). `sendError` 를 쓰지 않는 이유는 4-7 |
| 타임아웃 | 연결 3초 / 응답 120초 (`gateway.connect-timeout-ms`, `gateway.read-timeout-ms`) |
| 구현 | JDK `HttpClient` (추가 라이브러리 없음) |

레거시 `.do` 경로(`/atms/agent/**`, `/atmr/agent/**`, `/atms/remote-exec/**`)는 **기존 `LegacyAgentGatewayController`** 가 계속 담당한다 (주소만 `${jeus.base-url}/journal` 로 바뀜).

### 4-6. 바꾸지 않은 것과 이유

| 대상 | 이유 |
|---|---|
| 프론트엔드 (`Frontend/nh-atms-web`) | API 를 상대경로(`/api/...`)로 호출 → WebtoB 같은 주소로 가고, 게이트웨이가 분배 |
| ATM 에이전트·대외기관 경로 | 게이트웨이가 기존 고정 경로를 그대로 받음 |
| 각 모듈의 보안설정·컨트롤러 경로 | 컨텍스트만 붙을 뿐 내부 경로는 동일 (`/journal` + `/api/agent/...`) |
| `application.yml`, `application-local.yml` | 로컬 개발용 그대로. `jeus` 프로파일에서만 덮어씀 |
| MyBatis·DB 접근 코드 | Hikari 유지 결정 → JNDI 전환 불필요 |
| `Dockerfile`, `docker-compose.yml`, `Jenkinsfile`, nginx 설정 | 업체 환경·로컬용. 은행 서버에서는 앱을 컨테이너로 돌리지 않으므로 수정하지 않음 (11장) |
| `atmf.server.enabled`(파일전송 소켓 서버 21001), EAI TCP | 기본값 꺼짐 유지. 켜면 **JEUS JVM 안에서** 포트를 연다 → 켤 때 방화벽·현업 협의 |

### 4-7. 2026-10-13 보완 (JEUS·로컬 공통 점검 결과)

| # | 문제 | 원인 | 조치 |
|---|---|---|---|
| 1 | **JEUS 에서 저널 수신 HMAC 검증이 빠짐** (보안) | `HmacAuthFilter` 가 `getRequestURI()` 로 `/api/journal` 여부를 판정 → `/journal` 컨텍스트에서는 URI 가 `/journal/api/journal/...` 라 필터를 건너뜀 (경로는 `permitAll`) | 컨텍스트 경로를 뺀 경로로 판정. 로컬(루트 컨텍스트)은 기존과 동일 |
| 2 | **대상 모듈이 꺼져 있으면 사용자가 로그아웃됨** | 게이트웨이가 `sendError(502)` → `/error` 재디스패치에는 인증 정보가 없어 Spring Security 가 **401** 로 바꿈 (로그인 사용자도 동일, 실측). 화면 axios 는 401 이면 로그인 화면으로 보냄 | 502 상태와 JSON 본문을 직접 씀 |
| 3 | **로컬에서 화면의 `/api/file/...` 이 404** (첨부·이미지·펌웨어) | 로컬 vite 프록시는 `/api/batch` 외 `/api/**` 를 모두 8080(web)으로 보냄. web 에는 `/api/file` 이 없음 (도커도 nginx 가 같은 분기라 동일) | `local` 프로파일에서 게이트웨이를 켜서 8080 하나로 JEUS 와 같은 경로 구조. 대상 주소는 환경변수로 바꿀 수 있음(`ATMS_JOURNAL_URL`, `ATMS_BATCH_URL`, `ATMS_FILE_URL`, `ATMS_IF_INTERNAL_URL`, `ATMS_IF_EXTERNAL_URL`), 끄기 `GATEWAY_ENABLED=false` |
| 4 | **앱 단위 재배포·중지 시 배치 스케줄러 스레드가 남음** | `DynamicBatchScheduler` 가 스케줄러를 빈이 아닌 필드로 직접 만들어 Spring 이 종료하지 않음 (비데몬 스레드 4개). JEUS 에서 batch 만 재배포하면 이전 버전 스레드·클래스로더가 JVM 에 남음 | `@PreDestroy` 에서 등록 잡 해제 + 스케줄러 종료. 실행 중인 잡은 끝까지 수행 |

로컬에서 모듈 하나만 띄워도 된다: 꺼진 모듈의 경로만 502 이고 나머지는 정상이다.
도커(`docker-compose.yml`, 프로파일 `dev`)는 이번에 바꾸지 않았다 (업체 환경용, 3번의 `/api/file` 404 는 그대로).

---

## 5. 로컬 개발·기존 빌드 영향

| 확인 (2026-10-12) | 결과 |
|---|---|
| `gradlew bootJar` (`-Pjeus` 없음) 6개 | 내장 톰캣 포함, `web.xml` 없음 → 기존과 동일 |
| `bootRun` / IntelliJ | `main()` 그대로, 프로파일 `local`. **2026-10-13 부터 게이트웨이 켜짐** — 8080 으로 들어온 `/api/file`, `/api/agent`, `/api/journal`, `/atmf`, `/api/batch`, `/api/if/*` 를 각 모듈 포트로 전달 (4-7 의 3). 끄기 `GATEWAY_ENABLED=false` |
| 컨테이너 빌드 (`deploy/Dockerfile`, 업체 Dockerfile) | JAR 기준이라 영향 없음 |

---

## 6. 검증 결과 (2026-10-12, JEUS 대신 Tomcat 10.1.41 로 대체 검증)

같은 구조(서버 1개, 컨텍스트 `/`, `/journal`, `/batch`, `/file`, `/ifin`, `/ifex`, DB 없음)로 6개 WAR 를 올려 확인했다.

| 항목 | 결과 |
|---|---|
| 프로파일 | 5개 앱 로그에 `The following 1 profile is active: "jeus"` → `web.xml` 지정·`jeus-common.yml` import 정상 |
| atms-batch | **배포 실패** — `Unable to configure the default job repository` (DB 접속 불가). Spring Batch 는 기동 시 DB 가 필요 → DB 연결 후 해결. 다른 5개는 DB 없이도 기동 |
| 게이트웨이 경로 7개 | 직접 호출(`/journal/...`)과 게이트웨이 호출(`/api/agent/...`)의 응답이 동일 (DB 없음 → 대상 모듈에서 DB 오류 500 = **대상까지 도달 확인**) |
| 레거시 에이전트 경로 | `/atms/agent/open.do` → journal 도달 확인 |
| 파일 업로드 (3MB multipart) | 게이트웨이 경유, Content-Length·chunked 두 방식 모두 저장 파일 SHA-256 원본과 일치 |
| `/api/batch/...` (batch 미기동) | 401 (루트 앱이 받음, 무한 전달 없음) |
| 로그 폴더 쓰기 불가 | 앱은 정상 기동, 파일 로그만 안 생김 (JEUS 서버 로그에 logback ERROR 출력) |
| 메모리 (앱 5개, 기동 직후, GC 후) | 힙 사용 약 140MB · Metaspace 약 250MB · 프로세스 RSS 약 1GB |
| 기동 시간 (4코어) | 앱당 6~9초, 전체 약 43초 → **2코어 서버는 1~2분 예상** |
| 게이트웨이 단위 테스트 | 3건 통과 (전달 / 비대상 통과 / 502) |

**2026-10-13 재검증** (4-7 보완 후, 같은 Tomcat 10.1.41 구성 + `swap-deploy.sh` 로 exploded 배포, DB 없음, `-Djournal.hmac.enabled=true`)

| 항목 | 결과 |
|---|---|
| 빌드 | `bootWar -Pjeus` 6개, `bootJar` 6개 모두 성공. WAR 에만 `WEB-INF/web.xml`·`lib-provided` 있음 |
| 단위 테스트 | 전체 통과 (`HmacAuthFilterTest` 4건, `DynamicBatchSchedulerTest` 1건 신규). 기존부터 컴파일 안 되는 `NoticeServiceTest` 는 실행에서만 제외 (소스 그대로) |
| 결함 1 재현 | 수정 전 코드: `/journal` 컨텍스트에서 서명 없는 요청이 **200**(검증 건너뜀). 수정 후 **401** |
| HMAC (Tomcat) | 서명 없음 → 게이트웨이 경유·직접 호출 모두 401 `AUTH_HMAC`. 올바른 서명 → 필터 통과, 컨트롤러 도달 (DB 없어 500) |
| 결함 2 재현 | 수정 전: 대상 모듈이 꺼진 경로에 **유효한 JWT** 로 요청해도 401. 수정 후 502 JSON |
| 결함 4 재현 | 스케줄러 종료 호출을 빼면 테스트 실패(스레드 잔류), 넣으면 통과 |
| 같은 서버에서 batch 만 배포 실패한 상태 | `/api/batch/...` 는 루트 앱이 받아 로그인 사용자 500 / 비로그인 401 (리스너는 살아 있으므로 502 아님) |
| 종료 시 스레드 누수 경고 | 기동된 5개 앱은 없음 (Tomcat `failed to stop it` 경고는 기동 실패한 batch 의 ThreadLocal·JDBC 드라이버만) |
| 로컬 (`java -jar`, 프로파일 `local`, web·journal·file) | `/api/file/firmware/list` via 8080 → 200, `/api/journal/receive` 서명 없음 → 401, 꺼진 batch·ifin 경로 → 502, `GATEWAY_ENABLED=false` → 게이트웨이 미등록 |

**Tomcat 은 JEUS 가 아니다.** JEUS 고유 항목(Jakarta 지원, 클래스 로딩, 앱 등록 방식)은 `JEUS_WEBTOB_DEPLOY_GUIDE.md` STEP 1, 6 에서 실제로 확인해야 한다.

---

## 7. 서버 쪽 준비 (현업·인프라 요청 포함)

### 7-1. JEUS(atms01) 앱 등록 — 현업(WAS 담당) 요청

| 앱 | 배포 폴더 (exploded) | 컨텍스트 경로 |
|---|---|---|
| atms-web | `/home/atmswas/catms` (환경구성표 docBase 그대로) | `/` |
| atms-journal | `/home/atmswas/cajou` | `/journal` |
| atms-batch | `/home/atmswas/cabat` | `/batch` |
| atms-file | `/home/atmswas/cfile` | `/file` |
| atms-if-internal | `/home/atmswas/cifin` | `/ifin` |
| atms-if-external | `/home/atmswas/cifex` | `/ifex` |

- **모듈 폴더를 `catms` 안에 넣지 않는다.** `catms` 는 `/` 앱의 문서 루트라 그 안의 하위 폴더가 정적 경로로 노출될 수 있다 (`/cajou/WEB-INF/lib/...` 처럼 jar 다운로드 위험).
- 등록 방식(콘솔 / `jeusadmin` / `jeus-web-dd.xml` 의 context-path)은 JEUS 버전·현업 기준을 따른다. WAR 안에 JEUS 전용 설정 파일은 넣지 않았다.

### 7-2. 배포 스크립트 (`deploy/jeus/swap-deploy.sh`)

WAR 이름 `atms-<모듈>-<버전>-<YYYYMMDD>-<순번>.war` 에서 모듈을 읽어 위 표의 폴더로 자동 배포한다.
```bash
~/atms/deploy/jeus/swap-deploy.sh deploy ~/atms/release/atms-journal/atms-journal-0.0.1-SNAPSHOT-20261012-01.war
~/atms/deploy/jeus/swap-deploy.sh list atms-journal
~/atms/deploy/jeus/swap-deploy.sh rollback atms-journal
```
배포 후 `WAR 프로파일: jeus` 가 출력되는지 확인 (`-Pjeus` 없이 빌드한 WAR 이면 경고).

### 7-3. 폴더·권한 (환경구성표 원칙: atmswas 소유, jeus 그룹)

| 경로 | 소유 | 권한 | 이유 |
|---|---|---|---|
| `/home/atmswas/catms`, `catms-*` (6개) | atmswas:jeus | 2750 | JEUS 는 읽기만 |
| `/home/atmswas/data` | atmswas:jeus | **2770** | 앱(jeusatms)이 업로드·이미지·다운로드 파일을 **써야 함** |
| `/WASlogs/jeusATMSlogs/nbatmsltws01/app` | jeusatms | 750 이상 | 앱 로그 (상위 폴더에 jeusatms 쓰기 권한이 있으면 앱이 자동 생성) |

`/home` 은 공용 10G 디스크다. 첨부·이미지가 많아지면 `jeus.data-dir` 을 다른 디스크로 옮긴다 (`/nhod` 는 용도 확인 후).

### 7-4. JEUS JVM 옵션·리스너 — 현업 확인/요청

| 항목 | 요청 |
|---|---|
| HTTP 리스너 | atms01 에 서버 내부(127.0.0.1)에서 접속 가능한 HTTP 리스너가 있는지·포트 → `jeus.base-url` 에 반영 |
| 힙 | `-Xms1g -Xmx2g` 이상 권장 (6개 앱, 실측 기동 직후 힙 140MB + 업무 처리분). 31G 서버라 여유 있음 |
| Metaspace | `MaxMetaspaceSize` 를 지정한다면 **512m 이상** (실측 250MB, 앱마다 Spring 클래스를 따로 올림) |
| 넣지 말 것 | `-Dspring.profiles.active` (6개 앱 모두에 적용돼 web.xml 지정을 덮어씀) |
| 넣어도 되는 것 | 6개 공통값만: `-Djeus.base-url=...`, `-Djeus.log-dir=...`, `-Djeus.data-dir=...` (yml 값보다 우선) |
| 재기동 | JVM 1개라 `jeusRstart` 는 6개 앱이 함께 재기동됨 (에이전트 접속 잠시 끊김). 앱 1개만 재배포하는 방법이 있는지 현업 확인 (12-5) |
| 스레드 풀 | WebtoB 커넥터·HTTP 리스너(8766) 의 thread-pool 최소·최대 확인. 게이트웨이·모듈 간 호출은 요청당 스레드 2개 → 8766 최대 50 이상 권장 (12-4) |

### 7-5. WebtoB (`http.m`)

모든 동적 경로를 **JEUS 하나(JSV)** 로 보내면 된다. 모듈 컨텍스트(`/journal` 등)는 외부에 열 필요 없다 (서버 내부 호출 전용).

| URI | 대상 |
|---|---|
| `/api/` | JSV (atms01) |
| `/atms/`, `/atmr/`, `/atmf/` | JSV (ATM 에이전트, 실기기 연동 시) |
| `/swagger-ui/`, `/v3/api-docs`, `/actuator/` | JSV (확인용, 운영 시 보안 기준에 따라 제외) |

### 7-6. 현업 요청 문구 (예시)

```
[요청] ATMS 개발서버 JEUS(atms01) 애플리케이션 등록 (6개, 컨텍스트 경로 분리)

1. 애플리케이션 등록 (exploded 폴더, 소유 atmswas:jeus)
   - /home/atmswas/catms          → context /         (atms-web)
   - /home/atmswas/cajou  → context /journal  (atms-journal)
   - /home/atmswas/cabat    → context /batch    (atms-batch)
   - /home/atmswas/cfile     → context /file     (atms-file)
   - /home/atmswas/cifin     → context /ifin     (atms-if-internal)
   - /home/atmswas/cifex     → context /ifex     (atms-if-external)
2. 확인 요청
   - atms01 HTTP 리스너 포트 (서버 내부 127.0.0.1 접속용, 모듈 간 호출에 사용)
   - JEUS 의 Jakarta EE 9+(jakarta.servlet) 지원 여부 (Spring Boot 3.5 필수)
   - 앱 1개만 재배포/재기동하는 절차
3. JVM 옵션
   - 힙 -Xms1g -Xmx2g 이상, MaxMetaspaceSize 지정 시 512m 이상
   - -Dspring.profiles.active 는 지정하지 않음
4. 쓰기 경로: /home/atmswas/data (atmswas:jeus 2770) — jeusatms 쓰기 필요
5. WebtoB: /api/, /atms/, /atmr/, /atmf/ → atms01 (JSV)
```

---

## 8. 빌드 → 배포 순서 (요약)

상세 명령은 `JEUS_WEBTOB_DEPLOY_GUIDE.md`.

1. **내부망 소스(Gitea)** 의 `atms-common/src/main/resources/jeus-common.yml` `CHANGE_ME` 채우기 (4-4 표)
2. 내부 PC: `gradlew.bat bootWar -Pjeus --offline -x test`
3. WAR 6개 이름 변경 + `.sha256` 생성 → FileZilla 로 `/home/atmswas/atms/inbox/` (atmswas 접속)
4. `sha256sum -c` → `~/atms/release/<모듈>/` 로 이동
5. atmswas: `swap-deploy.sh deploy <WAR>` × 6
6. jeusatms: `jeusRstart`
7. 확인: 로그에 `Started Atms…Application` 6개, `curl http://127.0.0.1:<포트>/v3/api-docs` (web), `curl http://127.0.0.1:<포트>/api/file/firmware/list` (게이트웨이 → file)

---

## 9. 문제 상황별 판단

| 증상 | 원인 / 조치 |
|---|---|
| Spring 로그가 전혀 없고 모든 URL 404 | JEUS 가 javax 세대 → Spring Boot 3 미지원 (가이드 8장) |
| `The following 1 profile is active: "local"` 또는 `JWT_SECRET` placeholder 오류 | WAR 를 `-Pjeus` 없이 빌드(`web.xml` 없음) 또는 JVM 옵션에 `spring.profiles.active` 있음 |
| `ORA-17868 Unknown host CHANGE_ME_DB_HOST` | `jeus-common.yml` 미기입 |
| atms-batch 만 배포 실패 `Unable to configure the default job repository` | DB 접속 불가 (Spring Batch 는 기동 시 DB 필요) |
| `/api/batch/...` 등이 502 | `jeus.base-url` 포트 틀림(리스너 연결 불가) (atms-web 로그 `[module gateway] ... 전달 실패`) |
| `/api/batch/...` 가 500(로그인)/401(비로그인)이고 batch 로그에 요청 흔적 없음 | batch 앱이 기동 실패·미등록 → 요청이 루트 앱(web)으로 떨어짐. JEUS 로그에서 batch 기동 오류 확인 |
| web 로그에 `[module gateway] /api/... -> ...` 줄이 없음 | 게이트웨이가 꺼짐 → 프로파일이 `jeus` 인지 확인 |
| `NoSuchMethodError` / `LinkageError` | JEUS 기본 라이브러리와 충돌 → WEB-INF/lib 우선 로딩 설정 현업 문의 |
| 업로드 시 `AccessDeniedException` | `/home/atmswas/data` 에 jeusatms 쓰기 권한 없음 (7-3) |
| 로그 파일이 안 생김 (JEUS 로그에 logback `openFile ... failed`) | `jeus.log-dir` 권한 → 앱은 정상, 폴더 권한만 조치 |

---

## 10. 주의 사항

1. **atms-batch 는 DB 가 있어야 배포된다.** JEUS 재기동 시 DB 가 내려가 있으면 batch 만 실패 → DB 복구 후 batch 재기동 필요.
2. **WAR 에 DB 비밀번호·JWT 시크릿이 들어 있다** ("WAR 안에 포함" 결정). 보관본(`/home/atmswas/atms/release`, 700)은 atmswas 만 읽지만, 배포 폴더(`catms*`, 2750)는 JEUS 가 읽어야 하므로 **jeus 그룹 구성원**(jeusatms, webatms 등)도 읽을 수 있다.
   - 개발서버에서는 수용하되, 검증·운영 전환 시에는 WAR 에 `CHANGE_ME` 를 두고 JEUS 쪽 OS 환경변수 `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` 로 넣는 방식을 권장 (환경변수가 yml 보다 우선 → 소스 변경 없이 전환 가능)
   - WAR 파일을 외부(GitHub 등)로 내보내지 않는다.
3. **JVM 1개 공유**: 한 앱의 메모리 누수·스레드 고갈이 6개 모두에 영향. 2코어라 batch 대량 작업 중 화면 응답이 느려질 수 있음 → 배치 일정은 업무시간 외로.
4. JEUS 의 Jakarta 지원은 **서버에서 아직 미확인**이다. TmaxSoft 공식 소개서상 **JEUS 9 는 Jakarta EE 9(Servlet 5.0) 인증 제품**이라 javax 전용일 가능성은 낮아졌다 (2026-10-14 확인). 단 Spring Boot 3.5 는 Servlet 6.0(Tomcat 10.1) 기준이므로 5.0 에서의 동작은 STEP 1·6 으로 확인한다. STEP 1 결과가 javax 만이면 실행 환경을 재결정해야 한다 (가이드 8장).
5. **게이트웨이 경유 요청은 스레드 2개를 쓴다.** 같은 스레드 풀에서 몰리면 서버 전체가 멈출 수 있다 (Tomcat 재현, 12-4). PC 에서 8766 직접 접속은 기능 확인용으로만.

---

## 11. Podman 관점 정리 ("docker 대신 podman")

| 항목 | 정리 |
|---|---|
| 앱 실행 | 은행 서버에서는 **JEUS** (컨테이너 아님) → 앱 소스에 Podman 관련 수정 없음 |
| 업체 `Dockerfile`·`docker-compose.yml` | 업체 환경/로컬용으로 그대로 둠. Podman 도 같은 Dockerfile 을 읽지만, 이미지 안에서 Gradle 로 의존성을 내려받으므로 폐쇄망에서는 사용 불가 |
| 서버의 Podman 용도 | Gitea(소스 저장소), Jenkins(WAR 빌드), JEUS 준비 전 임시 실행(`bin/deploy.sh`, `bootJar` 사용) |
| Jenkins 빌드 명령 | `./gradlew bootWar -Pjeus --offline -x test` → WAR 6개 → 위 8장 3~6 단계 |
| 임시(컨테이너) 경로와의 관계 | 임시 경로는 `-Pjeus` 없는 JAR + 포트 분리라서 게이트웨이를 쓰지 않는다. JEUS 로 옮기면 배포킷 컨테이너는 정리 |

---

## 12. 추가 검토 (2026-10-13~15) — 게이트웨이 필요성·대안·성능·모듈 단독 배포

> 질의응답으로 정리한 내용. 코드 변경은 없고, 결정이 필요한 항목은 12-6 에 모았다.

### 12-1. 게이트웨이(`atms-web/.../web/gateway/` 3개 파일)가 하는 일

| 파일 | 줄 수 | 역할 |
|---|---|---|
| `ModuleGatewayProperties` | 48 | yml 의 경로표(`gateway.routes`)·타임아웃을 읽는 설정 클래스 |
| `ModuleGatewayConfig` | 27 | `gateway.enabled=true` 일 때만 필터 등록, **Spring Security 보다 앞** 순서 (web 의 JWT 규칙이 대상 모듈의 HMAC·permitAll 판단을 가로막지 않도록) |
| `ModuleGatewayFilter` | 180 | 전달 본체: 본문 스트림 그대로(multipart·HMAC 서명), 헤더·원 IP(`X-Forwarded-*`), 연결 실패 502 |

`ModuleGatewayProperties` 를 먼저 만들면 IntelliJ 가 "Not registered via @EnableConfigurationProperties…" 경고를 띄운다. 등록은 `ModuleGatewayConfig` 의 `@EnableConfigurationProperties` 가 하므로 그 파일을 만들면 사라진다 (컴파일·실행 영향 없음).

**게이트웨이 없이도 WAR 6개는 JEUS 1대에 정상 기동한다.** 안 되는 것은 컨텍스트 없는 고정 경로의 분배뿐이다 (JEUS 는 경로 앞부분으로 앱을 고르므로 모두 루트 = web 으로 떨어짐).

| 경로 | 호출 주체 | 게이트웨이 없을 때 |
|---|---|---|
| web 자체 API (`/api/auth`, `/api/atm` 등) | 화면 | 정상 |
| 레거시 `.do` (`/atms/agent`, `/atmr/agent`, `/atms/remote-exec`) | ATM 실기기 | 정상 (업체 코드 `LegacyAgentGatewayController`) |
| `/api/batch/**`, `/api/file/**` | 화면 | 실패 |
| `/atmf/**`, `/api/agent`, `/api/journal` | 에이전트·시뮬레이터 | 실패 |
| `/api/if/internal`, `/api/if/external` | 대내·대외기관 | 실패 |
| 모듈 간 호출 (`jeus.base-url` + `/journal` 등) | 서버 내부 | 정상 (이미 컨텍스트를 붙여 호출) |

멀티포트가 아니다: 기존(컨테이너) 8080~8085 → JEUS 리스너 1개(8766) + 컨텍스트 경로 6개. 각 모듈 `server.port` 는 외부 WAS 에서 무시된다.

### 12-2. 서버 1개(atms01) 유지 시 대안 비교

| 안 | 코드 | 필요한 것 | 판단 |
|---|---|---|---|
| **A. 현행** (web 게이트웨이) | 3개 파일 + yml | 없음 (WebtoB 는 루트 하나로) | Tomcat 검증 완료. 인프라 요청 불필요 |
| **B. WebtoB URL Rewrite** | 게이트웨이 불필요 — `application-jeus.yml` 의 `gateway.enabled: false` **한 줄**이면 필터 미등록 (파일 삭제는 선택) | WebtoB 에 internal rewrite 7규칙 (예: `^/api/batch(/.*)?$ → /batch/api/batch$1 [L]`), 로컬은 `vite.config.ts` 프록시 추가 | 코드·성능 최선. **rewrite 지원 확인 필요** — TmaxSoft 문서로 확인한 것은 WebtoB 6(mod_rewrite 문법 일부). 은행은 `http.m` 형식이라 그 이전 버전일 수 있음 |
| C. 게이트웨이를 1개 파일로 합침 | 3 → 1 파일 | 없음 | 줄 수 거의 동일 → 실익 적음 |
| D. 프론트가 `/batch/api/batch` 처럼 컨텍스트 포함 호출 | 프론트 API 파일 | 없음 | 에이전트·대외기관 경로는 못 바꿈 → 단독으로 해결 불가 |
| E. 같은 JVM 내 cross-context forward | 필터 1개(~40줄) | JEUS 컨텍스트 간 접근 허용 | 대상 앱 보안 필터 동작 미검증 → 비권장 |

B 주의: redirect(`[R]`)가 아니라 **internal rewrite** 여야 한다 (POST·에이전트 깨짐). 에이전트·대외기관은 반드시 WebtoB(9000)로 들어와야 한다 (8766 직접 접속은 rewrite 안 됨).
게이트웨이 외 변경(Initializer 6개, `build.gradle`, yml, HMAC 수정, 배치 종료)은 어느 안이든 필요하다. `jeus/web.xml` 은 현업이 JVM 옵션 `-Dspring.profiles.active=jeus` 를 넣으면 없앨 수 있지만(6개 앱 모두 같은 프로파일) 20줄이라 유지 권장.

### 12-3. 성능 실측 (2026-10-15, Tomcat 10.1.41 포트 8766, web·file 2개, 4코어 — JEUS 아님, 참고치)

| 항목 | 직접 `/file/api/file/...` | 게이트웨이 `/api/file/...` | 차이 |
|---|---|---|---|
| 작은 JSON 순차 300회 | p50 5.3ms / p95 8.7ms | p50 7.9ms / p95 13.6ms | 건당 약 +2.6ms |
| 처리량 동시 20 | 690 req/s | 518 req/s | 약 -25% |
| 50MB 다운로드 | 596ms (84MB/s) | 883ms (57MB/s) | 약 -32% |
| web 자체 경로 | — | 접두어 7개 비교만 | 사실상 0 (계산상) |

화면 수준 트래픽에서는 체감하기 어렵다. 2코어 서버에서는 전달 요청이 CPU 를 약 1.5배 쓴다. 영향이 큰 경로: 대용량 `/atmf`·`/api/file` 다운로드, 다수 ATM 의 `/api/agent`·`/api/journal`.

### 12-4. 스레드 고갈 위험 (재현함)

게이트웨이 경유 요청은 **스레드 2개**를 쓴다 (web 이 대상 모듈 응답을 기다리는 동안 대상 모듈도 스레드가 필요). 두 스레드가 같은 풀에서 나오면 고갈된다.

| Tomcat 스레드 풀 4개, 동시 16 × 32건 | 결과 |
|---|---|
| 직접 호출 | 32건 모두 성공 (0.5초) |
| 게이트웨이 경유 | **32건 모두 타임아웃** — 스레드 덤프상 4개 모두 `ModuleGatewayFilter` 에서 대기, 대상 앱은 스레드를 못 받음. 읽기 타임아웃(120초)까지 서버 전체 무응답 |

JEUS 에서는 외부 요청이 **WebtoB 커넥터(19000)** 풀로, 게이트웨이·모듈 간 호출이 **HTTP 리스너(8766)** 풀로 들어가는 구조라 서로 막지 않을 가능성이 높다 (각 풀 설정은 미확인). 남는 위험:
- PC 에서 **8766 으로 직접 테스트**할 때 — 들어온 요청과 전달 요청이 같은 풀 → 위 재현과 같은 상황. 직접 접속은 기능 확인용으로만.
- 8766 풀 최대값이 작을 때 — 게이트웨이 외에 레거시 `.do` → journal, web → file 원격제어, journal → IF 호출도 이 풀을 쓴다.

### 12-5. 모듈 1개만 배포 (WAR 단위)

| 단계 | 따로 가능? | 설명 |
|---|---|---|
| 빌드 (`:atms-journal:bootWar -Pjeus`) · 업로드 · `swap-deploy.sh deploy`/`rollback` | 가능 | 해당 `catms-*` 폴더만 바뀜, 백업도 모듈별 |
| 반영: `jeusRstart` | 불가 | `atms01` JVM 전체 재기동 → 6개 앱 1~2분 중단 (현재 가이드 방식) |
| 반영: JEUS 앱 단위 재배포 (`jeusadmin`·콘솔) | **현업 확인** | 되면 그 앱만 재시작. 명령·권한·은행 절차 미확인 |

- 폴더 교체 후 반영 전까지는 옛/새 클래스가 섞일 수 있다 → **교체 직후 바로 반영**.
- 앱 단위 재배포 중에도 그 모듈 경로는 502. **atms-web(`/`) 은 입구라 재시작 중 외부 요청 전체 중단** (A 안 기준).
- 같은 JVM 에서 앱 단위 재배포를 반복하면 Metaspace 가 쌓일 수 있다 → 가끔 `jeusRstart` 로 정리.
- **함께 배포해야 하는 경우**: `atms-common` 코드·`jeus-common.yml` 변경(각 WAR 의 `WEB-INF/lib` 에 복사되므로 6개 전부, JWT 시크릿이 다르면 401), 모듈 간 API 규격 변경(제공 쪽 먼저), DB 스키마 변경. 6개 WAR 는 같은 커밋에서 빌드하는 것이 원칙.

### 12-6. 결정·확인 대기

| # | 항목 | 누구 | 결과에 따라 |
|---|---|---|---|
| 1 | WebtoB 버전과 URL Rewrite(internal) 지원 | WebtoB 담당 | 지원 → B 전환 검토 (`gateway.enabled: false`) / 미지원 → A 유지 |
| 2 | `domain.xml` 의 WebtoB 커넥터·HTTP 리스너(8766) thread-pool 최소·최대 | WAS 담당 (atmswas 로 읽기 가능) | 두 풀 분리 + 8766 최대 50 이상이면 A 로 충분 |
| 3 | 앱 단위 재배포 명령·권한 | WAS 담당 | 가능하면 단독 반영 절차를 배포 가이드에 추가 |
| 4 | (선택) 게이트웨이 동시 전달 상한 — 넘으면 즉시 503 | 결정 필요 | 고갈 시 서버 전체 멈춤 대신 일부 503. 코드 변경 필요해 보류 |
