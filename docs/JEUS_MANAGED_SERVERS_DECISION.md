# JEUS 매니지드 서버 6개 구성 결정 — 개념·고민 과정·변경 범위

작성일: 2026-10-26 · 서버 `nbatmsltws01`(16.88.122.113) · WebtoB `nbatmsltwb01`(16.88.122.112)

> **결정 (2026-10-26, 사용자)**
> 1. JEUS **도메인 1개 안에 매니지드 서버 6개** — 모듈마다 JVM 하나, 각자 포트, 컨텍스트는 모두 `/`
> 2. 포트는 **로컬 개발과 같은 8080~8085** (web 8080, journal 8081, batch 8082, file 8083, if-internal 8084, if-external 8085)
> 3. **게이트웨이 제거**: WebtoB 가 경로별로 각 서버에 직접 분배 (atms-web 이 다른 모듈로 전달하지 않음)
> 4. 이번에는 문서·결정 기록·WAS 담당 요청까지. **소스·배포킷 수정은 서버명·포트가 확정된 뒤** (지금 바꾸면 현재 atms01(8766) 배포가 깨짐)
>
> **2026-10-27 확정**: 서버 이름 `atms01`(web)·`ajou01`(journal)·`abat01`(batch)·`file01`·`ifin01`·`ifex01`, 폴더 `catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex`, base 리스너 = HTTP 리스너 (포트 1개). 요청서: [WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md)
>
> 이 결정은 2026-10-12 의 "서버 1개(atms01) + 컨텍스트 경로 6개 + 게이트웨이" 구성을 대체한다. 비교표: [JEUS_MULTI_PORT_OPTIONS.md](JEUS_MULTI_PORT_OPTIONS.md)

---

## 1. 개념 — 매니지드 서버란

### 1-1. JEUS 의 층

```
물리 서버 nbatmsltws01 (16.88.122.113, 2코어)
└─ JEUS 설치 /JEUS/jeusATMS                       ← 프로그램 (한 벌)
   └─ 도메인 (domain.xml 하나)                     ← 관리 단위
      ├─ DAS (Domain Administration Server)        ← 관리 전용 JVM. 설정 보관·배포·서버 기동/정지 명령 (8736)
      ├─ NodeManager                               ← 이 물리 서버에서 서버 프로세스를 띄우고 감시하는 데몬
      └─ 매니지드 서버 (Managed Server) × N        ← 실제 앱이 도는 JVM. 각자 리스너(포트)·힙·로그
         └─ 애플리케이션 (WAR)                     ← 우리 모듈
```

| 용어 | 뜻 | 지금 이 서버 |
|---|---|---|
| 도메인 | DAS 하나가 관리하는 서버들의 묶음. 설정은 `domain.xml` 한 파일 | `/JEUS/jeusATMS/domains/<도메인>/config/domain.xml` |
| DAS | 관리 서버. 사용자 요청(앱 처리)은 하지 않음. `jeusadmin` 이 접속하는 곳 | `-DadminServer`, 8736, `-Xmx1024m` |
| NodeManager | 물리 서버마다 1개. DAS 명령을 받아 매니지드 서버 프로세스를 실행 | 실행 중 |
| **매니지드 서버** | 앱을 실행하는 JVM 하나. 자기 base 리스너·HTTP 리스너·WebtoB 커넥터·JVM 옵션·로그를 가짐 | `nbatmsltws01_atms01` (8766), 같은 도메인에 `nbatmsltws02_atms01` 정의 |
| 클러스터 | 같은 앱을 올리는 서버 묶음 (세션 공유·같이 배포) | `cluster_atms` (atms 앱의 target) |
| 리스너 | 서버가 여는 포트. base 리스너(관리·내부 통신)와 HTTP 리스너가 같은 포트를 공유하기도 함 | atms01 base=HTTP 8766 |
| WebtoB 커넥터 | 매니지드 서버 → WebtoB JSV 포트(19000) 로 **JEUS 쪽에서 접속해 가는** 연결. 서버마다 따로, `registration-id` 로 WebtoB 의 `*SERVER` 와 짝 | `nbatmsltws01_atms01-nbatmsltwb01_webatms` |
| 애플리케이션 | 서버(또는 클러스터)에 등록된 WAR. 경로(`path`)·컨텍스트(`context-path`)·대상 | `atms` → `/home/atmswas/catms`, `/` |

### 1-2. 톰캣과의 대응

| Spring Boot 내장 톰캣 (로컬) | JEUS |
|---|---|
| `java -jar atms-journal.jar` 프로세스 1개 | 매니지드 서버 1개 (JVM) |
| 그 안의 Tomcat (서블릿 컨테이너) | 매니지드 서버 안의 JEUS 웹엔진 |
| `server.port=8081` | 서버의 HTTP 리스너 포트 |
| 앱이 Tomcat 을 품음 (JAR) | 서버가 앱을 품음 (WAR 등록) |
| (없음) — 각 프로세스를 직접 실행·정지 | DAS·NodeManager 가 서버들을 관리 |

→ "물리 서버에 JEUS 매니지드 서버 6개" = "물리 서버에 내장 톰캣 6개" 와 **실행 단위로는 같다**. 차이는 JEUS 쪽에 관리 층(DAS·NodeManager)과 무거운 엔진, WAS 담당 운영 체계가 붙는다는 것뿐.

### 1-3. "JEUS 6개" 라는 말이 가리킬 수 있는 세 가지

| 표현 | 실제 구성 | JVM | 이번 결정 |
|---|---|---|---|
| ① 서버 1개 + 리스너 6개 | 매니지드 서버 1개가 포트 6개를 엶 | 1 | × (포트만 나뉘고 격리 없음, JEUS 9.1 지원 미확인) |
| **② 도메인 1개 + 매니지드 서버 6개** | DAS 하나 아래 서버 6개 | 6 (+DAS) | **채택** |
| ③ JEUS(도메인) 6개 | DAS·설정이 6벌 | 12 | × (관리 층 중복, 2코어 부적합) |

---

## 2. 고민 과정 (2026-10-26 대화 기록)

| 순서 | 질문·관찰 | 결론 |
|---|---|---|
| 1 | 10-26 점검: atms01 에 앱 `atms` 1개만 등록, 나머지 5개 미등록, 8766 전부 404 | 앱 5개 추가 등록은 JEUS 관리자 계정이 필요한 WAS 담당 작업 (root 로 `domain.xml` 수정 금지) |
| 2 | "8766 JEUS 하나에 6개를 각자 포트로 띄울 수 있나?" | 서버 하나(JVM 하나)에서는 WAR 가 포트를 갖지 않음 — 서버의 리스너를 공유하고 컨텍스트로 구분. 포트를 나누려면 ①(리스너+가상호스트, 미확인) 또는 ②·③ |
| 3 | "WAS 한 개에 컨테이너 6개(멀티포트)" — ①·②·③ 의 차이? | 비교표 작성 ([JEUS_MULTI_PORT_OPTIONS.md](JEUS_MULTI_PORT_OPTIONS.md)). 차이의 핵심은 JVM 수 |
| 4 | "내장 톰캣을 컨테이너 6개로 띄우는 것은 셋 중 무엇과 같나?" | 구조(JVM 6·포트 6·격리)는 ② 와 같음. 운영 주체(우리 vs WAS 담당)·배포물(JAR vs WAR)·WebtoB 연결(HTTP 프록시 vs JSV)이 다름 |
| 5 | "톰캣과 JEUS 는 같은 개념 아닌가?" | 맞음 — 둘 다 서블릿 컨테이너. "JEUS 서버 6개" 를 ② 로 읽으면 내장 톰캣 6개와 같고, 차이는 관리 층뿐 |
| 6 | 결정 | **②**, 포트 8080~8085, 컨텍스트 `/`, 게이트웨이 제거 |

### 2-1. ② 를 고른 이유와 감수하는 것

| 이유 | 감수하는 것 |
|---|---|
| 모듈별 장애 격리 (한 모듈의 OOM·스레드 고갈이 다른 모듈에 안 번짐) | JVM 6개분 메모리·CPU (2코어, Jenkins·Gitea 와 공유) → 서버당 힙을 작게 |
| 모듈만 따로 재기동 (지금은 jeusRstart 가 6개를 함께 내림) | 서버 5개 신규 생성·커넥터 등 WAS 담당 작업 |
| 로컬과 같은 주소 체계 (포트 8080~8085, 컨텍스트 `/`) → JEUS 전용 경로 처리가 대부분 불필요 | 서버마다 로그·재기동 메뉴·모니터링이 따로 |
| JEUS 표준 방식 (같은 도메인에 이미 서버 2개 정의) | 기동 시간이 서버 수만큼 |
| 게이트웨이 제거 → 요청당 ~2.6ms·처리량 -25%·스레드 2개 점유 문제 해소, 게이트웨이 교착 위험 제거 | WebtoB 경로 규칙이 6개 서버로 늘어남 |

---

## 3. 목표 구성

```
PC 브라우저 ──9000──▶ WebtoB (wb01)
                        ├ /index.html, /assets/…          → DocBase ~/docs (화면)
                        ├ /api/journal/, /api/agent/        → JSV ─▶ atms-journal 서버 :8081  (/home/atmswas/cajou)
                        ├ /api/batch/                       → JSV ─▶ atms-batch   서버 :8082  (cabat)
                        ├ /api/file/, /atmf/                → JSV ─▶ atms-file    서버 :8083  (cfile)
                        ├ /api/if/internal/                 → JSV ─▶ atms-ifin    서버 :8084  (cifin)
                        ├ /api/if/external/                 → JSV ─▶ atms-ifex    서버 :8085  (cifex)
                        └ /api/ (나머지), /atms/, /atmr/,    → JSV ─▶ atms-web     서버 :8080  (catms)
                          /swagger-ui/, /v3/api-docs, /actuator/
   (JSV: 각 JEUS 서버가 wb01:19000 으로 접속해 오는 커넥터, 서버마다 registration-id 1개)

모듈 간 직접 호출 (같은 ws01, http://16.88.122.113:<포트>)
   web → file  (원격전문 snd.msg.do)      web → journal (레거시 /atms/agent·/atmr/agent·/atms/remote-exec .do 전달)
   file → web  (원격실행 응답 전달)         journal → ifin·ifex
```

| 서버 (이름은 WAS 담당이 정함, 예시) | 포트 | 앱 id | docBase | 컨텍스트 |
|---|---|---|---|---|
| `atms01` (기존 nbatmsltws01_atms01 대체, 2026-10-27 이름 확정) | 8080 | atms (atms-web) | `/home/atmswas/catms` | `/` |
| `nbatmsltws01_atms02` | 8081 | atms-journal | `/home/atmswas/cajou` | `/` |
| `nbatmsltws01_atms03` | 8082 | atms-batch | `/home/atmswas/cabat` | `/` |
| `nbatmsltws01_atms04` | 8083 | atms-file | `/home/atmswas/cfile` | `/` |
| `nbatmsltws01_atms05` | 8084 | atms-ifin | `/home/atmswas/cifin` | `/` |
| `nbatmsltws01_atms06` | 8085 | atms-ifex | `/home/atmswas/cifex` | `/` |

- WebtoB `*URI` 는 **구체적인 경로를 먼저** 적는다 (`/api/journal/` 이 `/api/` 보다 앞). WebtoB 5 의 URI 매칭 규칙(등록 순서 vs 최장 일치)은 WebtoB 담당 확인
- 각 앱의 HTTP 리스너는 **16.88.122.113 에서 LISTEN** 해야 모듈 간 호출이 된다 (현재 8766 은 113 에만 LISTEN, 127.0.0.1 불가 — 2026-10-23)
- 다른 서버의 Swagger 는 WebtoB 경유로 보이지 않음 (모두 `/v3/api-docs`) → 각 포트로 직접 확인

---

## 4. 바뀌는 것 (포트·서버명 확정 후 적용)

### 4-1. JEUS (WAS 담당)
- 매니지드 서버 5개 생성 (+ atms01 포트 정리): base·HTTP 리스너, NodeManager 등록, JVM 옵션
- 서버당 힙: 2코어·공용 서버라 **512m~768m 수준** 제안 (DAS 1G, Jenkins 2G, Gitea 768m 와 합산 — 물리 메모리 확인 `free -g`)
- 서버마다 WebtoB 커넥터 (wb01, 필요 시 wb02) → registration-id 회신
- 앱 6개를 각 서버에 1개씩, exploded 경로, 컨텍스트 `/`, 기존 atms 와 같은 공통값 (WAR, classloading ISOLATED, SYSTEM_DOMAIN)
- 클러스터: 기존 `cluster_atms` 는 web 용으로 두고, 나머지는 서버 단독 대상 (또는 담당 판단)
- 로그: 서버별 `/WASlogs/jeusATMSlogs/nbatmsltws01/<서버>/`
- 재기동: jeusRstart 메뉴 5·6 은 `nbatmsltws01_atms01` 고정 → **서버별 boot/down 수단** 필요 (메뉴 추가 또는 담당 절차)
- (확인) JEUS 9.1 Jakarta Servlet 지원, 현재 atms 앱 미기동 원인 — 방식과 무관한 선행 문제

### 4-2. WebtoB (wb01)
- `*SERVER` 6개 (각 registration-id), `*URI` 를 3장 표대로 (모듈 경로 → 해당 서버, 나머지 `/api/` → web)
- ROOT_DAY_CHECKLIST 2-13 절차를 6개 서버 기준으로 확장

### 4-3. 소스 (NH-ATMS, 내부 Gitea)

| 파일 | 변경 |
|---|---|
| `atms-common/.../jeus-common.yml` | `jeus.base-url` 하나 → 모듈별 `jeus.url.web/journal/batch/file/ifin/ifex` (`http://16.88.122.113:8080`~`8085`) |
| `atms-web/.../application-jeus.yml` | `gateway.enabled: false`, `routes` 삭제. `agent.proxy.journal-base-url: ${jeus.url.journal}`, `atmf.remote.snd-msg-uri: ${jeus.url.file}/atmf/snd.msg.do` |
| `atms-file/.../application-jeus.yml` | `msg-async-resp-forward-uri: ${jeus.url.web}/atms/remote-exec/put.msg.resp.do` |
| `atms-journal/.../application-jeus.yml` | `internal-base-url: ${jeus.url.ifin}`, `external-base-url: ${jeus.url.ifex}` |
| `jeus/web.xml`, `SpringBootServletInitializer`, `bootWar -Pjeus` | 그대로 (WAR 6개) |
| `ModuleGatewayFilter`, HMAC 의 `getContextPath()` 처리 | 코드는 그대로 둠 (비활성·컨텍스트 빈 값으로 동작). 정리는 선택 |
| `docs/NH-ATMS_LOCAL_APPLY_GUIDE.md`, `docs/JEUS_SOURCE_CHANGES.md` | 변경 반영 |

### 4-4. 배포킷 (이 저장소)

| 파일 | 변경 |
|---|---|
| `deploy/jeus/swap-deploy.sh` | 그대로 (`catms*` 6개 폴더가 그대로 각 서버의 docBase) |
| `deploy/jeus/deploy-war.sh` | 모듈→컨텍스트 매핑을 **모듈→포트** 로, 기동 확인 `http://16.88.122.113:<포트>/v3/api-docs`, `restart <모듈>` 이 **그 서버만** 정지·기동 (WAS 담당이 준 서버별 수단 사용), `JEUS_URL` → 포트표 |
| `deploy/jenkins/pipelines/backend.Jenkinsfile` | `RESTART` 가 배포한 모듈의 서버만 재기동 (6개 동시 중단 없음) |
| `docs/` | BACKEND_CICD_PLAN 6장, JEUS_WEBTOB_DEPLOY_GUIDE, SERVER_DIRECTORY_LAYOUT, DEV_SERVER_INFRA 포트표, ROOT_DAY 2-13, CLAUDE.md |

### 4-5. 방화벽·포트
- ws01 에서 8080~8085 가 비어 있는지 확인 (atmswas): `ss -ltn | grep -E ':808[0-5] '` → 아무것도 없어야 함. Jenkins 는 호스트 8090, 컨테이너 내부 8080 이라 무관
- PC → 113:8080~8085 는 직접 확인용으로만 필요 (화면은 9000 경유). 지금 신청된 8766 은 web 이 8080 으로 옮기면 쓰지 않게 됨 → 신청서 갱신 여부 판단
- JEUS → WebtoB 19000 은 이미 열림 (2026-10-15)

---

## 5. WAS 담당 요청문

```
제목: [요청] NH-ATMS JEUS 구성 변경 — 매니지드 서버 6개(모듈별 1개)로 분리

1. 배경
 - NH-ATMS 백엔드는 Spring Boot 3.5 WAR 6개입니다. 현재 nbatmsltws01_atms01 하나에
   atms(/home/atmswas/catms) 1개만 등록되어 있습니다.
 - 모듈별 장애 격리·단독 재기동을 위해 같은 도메인에 매니지드 서버를 모듈당 1개로
   구성하고자 합니다 (각 앱 컨텍스트는 "/").

2. 요청 — 서버·앱 구성 (서버 이름은 기존 규칙대로 정해 주시면 됩니다)
   포트   앱 id          docBase(exploded)                  컨텍스트
   8080   atms           /home/atmswas/catms                 /     (기존 atms01 포트 변경 또는 신규 서버)
   8081   atms-journal   /home/atmswas/cajou         /
   8082   atms-batch     /home/atmswas/cabat           /
   8083   atms-file      /home/atmswas/cfile            /
   8084   atms-ifin      /home/atmswas/cifin            /
   8085   atms-ifex      /home/atmswas/cifex            /
   공통: type WAR / classloading ISOLATED / security-domain SYSTEM_DOMAIN
         / 폴더 경로로 등록(배포 시 폴더를 통째로 교체하므로 경로 고정)
   - HTTP 리스너는 16.88.122.113 에서 LISTEN (모듈끼리 이 주소로 호출)
   - 서버당 힙: 2코어·공용 서버이므로 512m~768m 수준 협의
   - 서버별 로그: /WASlogs/jeusATMSlogs/nbatmsltws01/<서버명>/

3. 요청 — WebtoB 연동
   - 서버마다 nbatmsltwb01 WebtoB 커넥터(JSV 19000) 등록, registration-id 회신
   - (wb02 도 필요하면 같이)

4. 요청 — 재기동 수단
   - 현재 jeusRstart 메뉴 5·6 은 nbatmsltws01_atms01 만 boot/down 합니다.
     배포 자동화(Jenkins)에서 모듈 서버를 하나씩 재기동할 수 있도록
     서버별 boot/down 메뉴 추가 또는 사용 가능한 절차를 안내 부탁드립니다.

5. 확인 요청 (구성과 무관하게 선행)
   - JEUS 9.1 의 Jakarta Servlet(jakarta.servlet) 지원 여부
   - 현재 atms 앱이 기동되지 않는 원인 (8766 전 경로 404, 기동 로그 위치)
   - atms01 stderr.log 의 "[WebT]:webtCPInfo is null / NullPointerException" 반복 (매초)
   - 데이터소스 iddsATMS01 의 DB 종류(Tibero/Oracle) — 앱은 당분간 직접 접속, 추후 JNDI 전환
   - 2호기(nbatmsltws02_atms01) 도 같은 구성이 필요한지

6. 일정
   - 구성 완료 후 서버명·포트·registration-id·재기동 방법을 회신해 주시면
     배포 스크립트·소스 설정을 맞춰 첫 배포를 진행하겠습니다.
```

---

## 6. 다음 순서
1. (지금, atmswas) 포트 비어 있는지 확인 — 4-5
2. WAS 담당에 5장 요청 → 회신 (서버명·포트·registration-id·재기동 수단)
3. 회신 기준으로 소스(4-3)·배포킷(4-4) 수정 → 내부 Gitea push, 서버에는 수정분 반영
4. WebtoB 연동 (4-2, 담당 또는 root 작업일)
5. Jenkins `redeploy` → 모듈별 `http://16.88.122.113:808N/v3/api-docs` 200 → `/actuator/health` UP(DB) → 화면 9000 확인

## 8. 포트 구성 (WAS 담당 요청용, 2026-10-26 확정 기준)

기준: **업체 원본 멀티모듈 소스(게이트웨이 도입 전)를 그대로 적용하기 쉬운 방식** — 원본은 모듈마다 `server.port` 하나(8080~8085)이고 모듈 간 주소도 그 포트로 설정돼 있다 (`ATMS_JOURNAL_URL=http://localhost:8081`, `IF_INTERNAL_BASE_URL=http://localhost:8084` …). 그래서 서버마다 **포트 1개 (base 리스너 = HTTP 리스너 공유, 지금 atms01 과 같은 방식)**, 컨텍스트 `/`.

### 8-1. JEUS 매니지드 서버 (ws01, 16.88.122.113 에서 LISTEN)

| 포트 | 서버 (이름은 담당 규칙) | 모듈 (앱 id) | docBase | 컨텍스트 | 비고 |
|---|---|---|---|---|---|
| **8080** | `atms01` (기존 atms01 대체) | atms-web (`atms`) | `/home/atmswas/catms` | `/` | 화면 API·로그인·메뉴 등. 기존 WebtoB 커넥터·jeusRstart 메뉴 5·6 재사용 |
| **8081** | 신규 (예: `nbatmsltws01_atms02`) | atms-journal | `/home/atmswas/cajou` | `/` | 저널·에이전트(`/api/agent`). ATM 에이전트에 통보되는 저널 포트(`AGENT_ATMS_PORT`) |
| **8082** | 신규 (`…_atms03`) | atms-batch | `/home/atmswas/cabat` | `/` | DB 필수 (Spring Batch 메타 테이블) |
| **8083** | 신규 (`…_atms04`) | atms-file | `/home/atmswas/cfile` | `/` | 파일·원격전문(`/atmf`). 에이전트 파일 포트(`AGENT_ATMF_PORT`) |
| **8084** | 신규 (`…_atms05`) | atms-if-internal | `/home/atmswas/cifin` | `/` | 내부 연동 (EAI) |
| **8085** | 신규 (`…_atms06`) | atms-if-external | `/home/atmswas/cifex` | `/` | 대외 연동 |

- 서버마다 base 리스너와 HTTP 리스너를 **같은 포트로** (별도 base 포트 없음). 담당 관행상 분리가 필요하면 base 만 따로 받고, HTTP 는 위 포트 고정
- 리스너 주소: `16.88.122.113` (또는 0.0.0.0). 모듈 간 호출이 `http://16.88.122.113:<포트>` 로 간다 — 127.0.0.1 만 열면 안 됨
- 2코어 서버: 서버당 힙 512m~768m 수준 협의

### 8-2. 앱이 직접 여는 포트 (JEUS 리스너 아님, 해당 서버 JVM 안에서 bind)

| 포트 | 모듈 | 용도 | 기본 |
|---|---|---|---|
| 21001 | atms-file | ATM 에이전트 파일 송수신 NIO 소켓 서버 (`ATMS_FILE_SERVER_*`) | **꺼짐** (`enabled=false`). 켤 때 포트 중복·방화벽 확인 |

### 8-3. 나가는 연결 (ws01 → 외부)

| 대상 | 포트 | 누가 | 상태 |
|---|---|---|---|
| WebtoB wb01 16.88.122.112 | **19000** (JSV) | 6개 서버 모두 (registration-id 각각) | TCP 열림 (2026-10-15) |
| DB 16.88.250.91 | 1531 | 6개 모듈 (Hikari 직접 접속) | TCP 열림 (2026-10-26) |
| EAI | 39021 | atms-if-internal (`INTERNAL_GATEWAY_MODE=eai` 일 때만, 기본 mock) | 미정 |
| SFTP (펌웨어) | 22 | atms-file (`ATMS_FIRMWARE_SFTP_ENABLED`, 기본 꺼짐) | 미정 |

### 8-4. 그대로 두는 포트 (ws01)

| 포트 | 용도 |
|---|---|
| 8736 | JEUS DAS (변경 없음) |
| NodeManager 포트 | 변경 없음 (값 미확인) |
| 6600·6700·6800 | 기존 사용 중 (은행) |
| 3000 / 8090 | Gitea / Jenkins (rootless Podman) — Jenkins 컨테이너 안의 8080 은 호스트 포트가 아니라 충돌 없음 |
| 8766 | atms01 이 8080 으로 옮기면 **사용 안 함** |

### 8-5. 방화벽 변경 (PC 직접 확인용)
- 기존 신청 PC → 113:**8766** → **8080~8085** 로 변경 (화면은 9000 경유라 필수는 아님 — Swagger·health 직접 확인용)
- (실기기 연동 시) ATM 에이전트 → 113:8081·8083 (·21001) 은 운영 단계에서 별도 검토 (지금은 WebtoB 9000 경유 `jeus.agent-entry` 설정)

### 8-6. 요청 문구 (5장 요청문의 "2. 요청" 을 이 표로 대체)
```
포트 구성 (업체 원본 소스의 모듈별 포트와 동일하게)
 - 서버당 포트 1개 (base 리스너 = HTTP 리스너), 리스너 주소 16.88.122.113
- 8080 atms01 (기존 atms01 대체)            atms        /home/atmswas/catms          /
 - 8081 신규 서버                                          atms-journal /home/atmswas/cajou  /
 - 8082 신규 서버                                          atms-batch   /home/atmswas/cabat    /
 - 8083 신규 서버                                          atms-file    /home/atmswas/cfile     /
 - 8084 신규 서버                                          atms-ifin    /home/atmswas/cifin     /
 - 8085 신규 서버                                          atms-ifex    /home/atmswas/cifex     /
 - 6개 서버 모두 nbatmsltwb01:19000 WebtoB 커넥터 (registration-id 회신)
 - 사용 전 확인: ws01 에서 8080~8085 미사용 여부
```
확인 (atmswas): `ss -ltn | grep -E ':808[0-5] '` → 아무것도 없어야 함

## 9. 서버 생성 요청용 구성도 (간략, 2026-10-26 — ws01 8080~8085 미사용 확인)

서버 이름·폴더 (2026-10-27 확정, JEUS 명명규칙): 서버 `atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01`, 폴더 `/home/atmswas/catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex`. 기존 `atms01`(8766) 은 새 `atms01` 로 대체. base 리스너 = HTTP 리스너 (포트 1개)

```
[PC] ─9000─▶ [WebtoB wb01 16.88.122.112]
                  │ JSV 19000 (각 서버가 접속, registration-id 6개)
                  ▼
[ws01 16.88.122.113] JEUS 도메인 1개 (DAS 8736 그대로)
  ├ atms01    :8080  atms-web          /home/atmswas/catms
  ├ ajou01   :8081  atms-journal      /home/atmswas/cajou
  ├ abat01     :8082  atms-batch        /home/atmswas/cabat
  ├ file01      :8083  atms-file         /home/atmswas/cfile
  ├ ifin01      :8084  atms-if-internal  /home/atmswas/cifin
  └ ifex01      :8085  atms-if-external  /home/atmswas/cifex
                  │ 모두 → DB 16.88.250.91:1531
```

| 서버 | 포트 | 앱 | 폴더 | 비고 |
|---|---|---|---|---|
| atms01 | 8080 | atms-web | catms | 기존 atms01 대체 |
| ajou01 | 8081 | atms-journal | cajou | 신규 |
| abat01 | 8082 | atms-batch | cabat | 신규 |
| file01 | 8083 | atms-file | cfile | 신규 |
| ifin01 | 8084 | atms-if-internal | cifin | 신규 |
| ifex01 | 8085 | atms-if-external | cifex | 신규 |

공통: 서버당 포트 1개(base=HTTP), 리스너 16.88.122.113, 컨텍스트 `/`, WAR exploded, WebtoB 커넥터 wb01:19000 (커넥터 이름 예: `ajou01-nbatmsltwb01_webatms`, WebtoB `*SERVER` 이름 = registration-id), 힙 512m~768m. 이름은 담당 규칙이 따로 있으면 그에 따름.

### 9-1. WebtoB `http.m` 추가 블록 (서버 이름 = registration-id 가정)

```
*SVRGROUP
atms_jsvg       NodeName = "<기존 *NODE 이름>", SvrType = JSV

*SERVER
atms01     SvgName = atms_jsvg, MinProc = 2, MaxProc = 10    # atms-web          :8080
ajou01    SvgName = atms_jsvg, MinProc = 2, MaxProc = 10    # atms-journal      :8081
abat01      SvgName = atms_jsvg, MinProc = 1, MaxProc = 5     # atms-batch        :8082
file01       SvgName = atms_jsvg, MinProc = 2, MaxProc = 10    # atms-file         :8083
ifin01       SvgName = atms_jsvg, MinProc = 1, MaxProc = 5     # atms-if-internal  :8084
ifex01       SvgName = atms_jsvg, MinProc = 1, MaxProc = 5     # atms-if-external  :8085

*URI
uri_journal     Uri = "/api/journal/",     Svrtype = JSV, SvrName = ajou01
uri_agent       Uri = "/api/agent/",       Svrtype = JSV, SvrName = ajou01
uri_batch       Uri = "/api/batch/",       Svrtype = JSV, SvrName = abat01
uri_file        Uri = "/api/file/",        Svrtype = JSV, SvrName = file01
uri_atmf        Uri = "/atmf/",            Svrtype = JSV, SvrName = file01
uri_ifin        Uri = "/api/if/internal/", Svrtype = JSV, SvrName = ifin01
uri_ifex        Uri = "/api/if/external/", Svrtype = JSV, SvrName = ifex01
uri_api         Uri = "/api/",             Svrtype = JSV, SvrName = atms01
uri_atms        Uri = "/atms/",            Svrtype = JSV, SvrName = atms01
uri_atmr        Uri = "/atmr/",            Svrtype = JSV, SvrName = atms01
uri_swagger     Uri = "/swagger-ui/",      Svrtype = JSV, SvrName = atms01
uri_apidocs     Uri = "/v3/api-docs",      Svrtype = JSV, SvrName = atms01
uri_actuator    Uri = "/actuator/",        Svrtype = JSV, SvrName = atms01
```
- `*SERVER` 이름은 JEUS 커넥터의 registration-id 와 **같아야** 연결됨 (다르면 담당이 정한 id 로 교체)
- `*NODE` 에 `JSVPORT = 19000` 확인, 기존 JSV 서버그룹이 있으면 `*SVRGROUP` 은 추가하지 않고 그 이름을 `SvgName` 에
- `*URI` 는 구체적인 경로를 위에 (`/api/journal/` 이 `/api/` 보다 먼저). DOCROOT·기존 URI·오류 페이지는 그대로
- `MinProc`/`MaxProc` 는 예시 — 담당 기준

## 11. 확정 구성도 (2026-10-27, 사용자 확정)

```
단말(PC) ──REST 9000──▶ WebtoB nbatmsltwb01 (16.88.122.112)  /api/→web, /api/journal/→journal, /api/batch/→batch,
                                                             /api/file/→file, /api/if/internal/→ifin, /api/if/external/→ifex
                              └─ JSV 19000 ─▶ ws01 JEUS (서버 6개)
ATM·STM·공과금수납기 ─▶ 내부망 L4 ─┬─ REST journal 8081 ─▶ ajou01   (ws01 직접, http)
                                ├─ REST file    8083 ─▶ file01
                                └─ TCP socket  21001 ─▶ file01 (atmf.server, jeus 프로파일에서 켬)
nbatmsltws01 (16.88.122.113) JEUS 9.1 · jeusatms · 업무계정 atmswas
  MS#1 atms01 :8080 catms · MS#2 ajou01 :8081 cajou · MS#3 abat01 :8082 cabat
  MS#4 file01 :8083 cfile · MS#5 ifin01 :8084 cifin · MS#6 ifex01 :8085 cifex
  → 대외MCA · API GW/FOS · 코어뱅킹#01 (ifin·ifex 가 호출)
DB nbatmsltdb01 (16.88.250.91) Oracle · 1531 · NBATMSTP · ATMS_APP
환경: WebtoB 5 · JEUS 9.1 · JDK 21 · 엔진계정 jeusatms/webatms
```

반영 (2026-10-27): `jeus.agent-entry` → `host: CHANGE_ME_L4_VIP`, `journal-port: 8081`, `file-port: 8083`, `schema: http` (에이전트 open 응답), atms-file `atmf.server.enabled: true`·`bind-ip ${jeus.host}`·`port 21001`. 레거시 `.do`(`/atms/agent` 등)는 journal 에도 있어 8081 직접 연결로 동작.

남은 확인:
- **L4 VIP** 확정 → `CHANGE_ME_L4_VIP` 기입, L4 → 113:8081·8083·21001 분산·헬스체크 (8081·8083 은 HTTP `GET /v3/api-docs` 200 또는 TCP, 21001 은 TCP), ATM망 → L4 → 113 방화벽 신청
  - 2026-10-30 정정: 처음 제안한 `/api/agent/ping` 은 지금 소스에 **없다** — 업체 `LegacyAgentController` 의 `ping` 메서드가 닫히지 않은 주석 안에 있어 매핑되지 않음. 에이전트 폴링도 404 가 되므로 실기기 연동 전 개발 조치 필요 ([INFRA_MANUAL.md](INFRA_MANUAL.md) 13-5, T49)
- **보안**: base 리스너 = HTTP 리스너(포트 공유)라 8081·8083 을 ATM 망에 열면 JEUS 내부·관리용 base 리스너도 같이 노출됨 → 분리 여부 **WAS 담당 판단** (분리하면 HTTP 8080~8085 는 그대로, base 만 별도 포트)
- https 는 운영 전 재검토 (지금 http, 메시지는 HMAC 인증)
- WebtoB 의 `/api/agent/`·`/atmf/`·`/atms/`·`/atmr/` URI 는 구성도에 없음 — 에이전트가 직접 붙으므로 필수는 아님 (`http.m.atms-jsv.txt` 에는 남겨 둠, 필요 없으면 빼도 됨)

## 10. 반영 현황 (2026-10-27)

| 영역 | 파일 | 상태 |
|---|---|---|
| 소스 | `jeus-common.yml` (`jeus.host`, `jeus.url.*`, `log-dir` `/WASlogs/jeusATMSlogs/nbatmsltws01/app`), `application-jeus.yml` 6개 (`${jeus.url.<모듈>}`, web `gateway.enabled: false`) | 반영 — 로컬 PC 는 [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 개정 2026-10-27 |
| 배포 | `deploy/jeus/deploy-war.sh` (모듈→서버·포트, 서버별 재기동, 종료코드 4), `deploy-war.env.example`, `install-deploy-war.sh`, `swap-deploy.sh`(주석·안내) | 반영 · 스텁 시험 통과 |
| Jenkins | `deploy/jenkins/pipelines/backend.Jenkinsfile` (종료코드 4 → UNSTABLE) | 반영 · 문법은 괄호 균형만 확인 (서버 Jenkins 에서 첫 실행으로 확인) |
| WebtoB | `deploy/webtob/http.m.atms-jsv.txt` | 신규 (registration-id 는 회신값으로) |
| 남음 | WAS 담당: 서버 5개 생성·atms01 8080 변경·커넥터·서버별 재기동 메뉴 / WebtoB 적용 / 방화벽 8766 → 8080~8085 | 대기 |

## 7. 열린 항목
- [ ] JEUS 9.1 Jakarta 지원 (선행, 방식 무관)
- [ ] 물리 메모리·서버당 힙
- [ ] 서버별 재기동 수단 (jeusRstart 메뉴 추가 여부)
- [ ] WebtoB URI 매칭 규칙 (등록 순서 vs 최장 일치)
- [x] atms01 포트: **atms01 을 web 서버로 유지, 포트만 8766 → 8080** (2026-10-26 결정)
- [x] base 리스너: 서버당 포트 1개 공유 (원본 소스 적용 기준, 2026-10-26)
- [ ] 8766 방화벽 신청분 처리
