# JEUS 멀티 포트 구성 비교 — 리스너 6개 vs 서버 6개 vs JEUS 6개

작성일: 2026-10-26 · 서버 `nbatmsltws01`(16.88.122.113, 2코어, JEUS 9.1)

> **결정 (2026-10-26): ② 도메인 1개 + 매니지드 서버 6개, 포트 8080~8085, 컨텍스트 `/`, 게이트웨이 제거** → [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md)
>
> 배경 (2026-10-26 사용자): "WAS 한 개(JEUS 한 개)에 모듈 6개를 각자 포트로" 운영하려 함. 아직 어느 방식인지 확정 전 → 세 방식의 차이·장단점과, 각 방식에서 **현재 구성(서버 1개 + 컨텍스트 6개, 2026-10-12 결정)** 대비 무엇이 바뀌는지 정리.
> 사용자 결정: 포트를 나누면 **모든 앱의 컨텍스트는 `/`** (로컬 `localhost:8080~8085` 와 같은 구조).

## 0. 용어

| 용어 | 뜻 | 이 서버의 예 |
|---|---|---|
| 도메인 | JEUS 관리 단위. DAS(관리 서버) 1개 + 여러 매니지드 서버. 설정 파일 `domain.xml` 하나 | `/JEUS/jeusATMS/domains/<도메인>` |
| DAS | 도메인 관리 서버 (별도 JVM). 설정 배포·서버 기동/정지 담당 | 8736 리스너, `-Xmx1024m` |
| 매니지드 서버 | 실제 앱이 도는 JVM 하나. JEUS 6 시절 "컨테이너" 에 해당 | `nbatmsltws01_atms01` (8766) |
| 리스너 | 서버가 여는 포트. 서버 하나에 여러 개 가능 | 8766 (base 겸 HTTP) |
| WebtoB 커넥터 | 서버 → WebtoB JSV(19000) 연결. 서버마다 따로 등록 | `nbatmsltws01_atms01-nbatmsltwb01_webatms` |

## 1. 세 방식 한눈에

```
현재(A)   [JVM atms01 :8766]  /  /journal  /batch  /file  /ifin  /ifex        ← 경로로 구분

① 리스너6 [JVM atms01 :8766 :p2 :p3 :p4 :p5 :p6]                             ← JVM 1개, 포트만 6개
            가상호스트(포트별) → 앱 1개씩, 각자 '/'

② 서버6   DAS ─┬ [JVM atms01 :8766] web '/'
              ├ [JVM atms02 :p2]  journal '/'                                ← JVM 6개, 도메인 1개
              ├ … atms06 :p6]  ifex '/'

③ JEUS6   [DAS1+JVM web] [DAS2+JVM journal] … [DAS6+JVM ifex]                ← 도메인(설치) 6개, JVM 12개
```

| 항목 | 현재 A (서버1·경로) | ① 서버1·리스너6 | ② 도메인1·서버6 | ③ JEUS(도메인) 6개 |
|---|---|---|---|---|
| JVM 수 | 1 (+DAS) | 1 (+DAS) | **6** (+DAS) | **6 + DAS 6 = 12** |
| 메모리 (힙 1G 기준, 대략) | 1G | 1G | **6G** | **6G + DAS 6G** |
| 포트 | 1 | 6 | 6 (+서버별 base 리스너) | 6 + DAS 6 |
| 장애 격리 (한 모듈 OOM·스레드 고갈) | 없음 | **없음** (같은 JVM) | 있음 | 있음 |
| 모듈 단독 재기동 | 불가 (6개 같이) | 불가 (6개 같이) | **가능** | 가능 |
| 모듈별 힙·스레드풀 | 공유 | 공유 (리스너별 스레드풀은 가능) | 서버별로 따로 | 따로 |
| 설정 관리 | domain.xml 1, 앱 6 | domain.xml 1 + 리스너 6 + 가상호스트 6 | domain.xml 1 + 서버 6 | **도메인 6개 각각** |
| WebtoB 연결 | 커넥터 1 | 커넥터 1 (JVM 1개) | **커넥터 6** (서버마다) | 커넥터 6 |
| JEUS 지원 확인 | 사용 중 | **미확인** — 리스너별 가상호스트 매핑을 9.1 이 지원하는지 WAS 담당 확인 필요 | 표준 기능 (이미 ws01·ws02 서버 2개 정의) | 표준이지만 설치·라이선스 부담 |
| 은행 운영 관행 | — | 드묾 | 흔함 (업무별 서버 분리) | 개발서버에서는 드묾 |
| 2코어 서버 적합성 | 적합 | 적합 | **빠듯함** (JVM 6개 기동만으로 CPU·메모리 경쟁, Jenkins·Gitea 와 공유) | 부적합 |

### 1-1. ① 서버 1개 + 리스너 6개
- 특징: JVM 은 그대로 하나. 포트만 앱별로 다르게 보이게 하는 것
- 장점: 메모리 그대로, 로컬과 같은 "앱=포트" 주소 체계, 서버 등록·WebtoB 커넥터 추가 없음
- 단점: **장애·재기동은 지금과 똑같이 6개가 함께** (포트를 나눈 효과는 주소뿐). 리스너→가상호스트→앱 매핑이 JEUS 9.1 에서 되는지 미확인, 설정이 비표준이라 담당자가 꺼릴 수 있음. 포트 6개만큼 방화벽·WebtoB 경로 설정
- 언제: "주소를 로컬처럼 포트로 맞추고 싶다" 가 목적이고 자원이 부족할 때

### 1-2. ② 도메인 1개 + 매니지드 서버 6개
- 특징: 모듈마다 JVM 하나. DAS 하나가 6개를 관리 (지금 atms01 을 5개 더 만드는 것)
- 장점: **장애 격리·모듈 단독 재기동·모듈별 힙 조정**. JEUS 표준 방식, 담당자에게 익숙. 각 앱이 `/` 라 로컬과 같은 구조
- 단점: 메모리·CPU 가 JVM 수만큼 (2코어에 JVM 6개 + DAS + NodeManager + Jenkins·Gitea 컨테이너). 서버 5개 신규 생성·WebtoB 커넥터 5개·JVM 옵션·로그 경로 등 **WAS 담당 작업량이 큼**. 기동 시간도 서버 수만큼
- 언제: 운영과 같은 격리가 필요하고 서버 자원이 충분할 때 (보통 모듈 수 ≤ 코어 수 권장)

### 1-3. ③ JEUS(도메인) 6개 — "일반적인 멀티모듈을 JEUS 6개로"
- 특징: 모듈마다 독립 JEUS (DAS·설정·재기동 체계가 각각)
- 장점: 완전 분리 (설정 실수가 다른 모듈로 번지지 않음), 모듈별로 JEUS 버전·패치도 따로 가능
- 단점: JVM 12개(DAS 포함), 설치·라이선스·계정·디렉토리·jeusRstart 메뉴가 6벌. 개발서버 1대(2코어)에는 과함
- 일반적인 멀티모듈(MSA) 운영에서 "모듈=독립 프로세스" 는 보통 ②(같은 도메인의 서버 여러 개)나 서버(VM)를 나누는 방식이지, 같은 서버에 도메인을 6개 까는 경우는 드묾

## 2. 어떤 방식이든 바뀌는 것 (컨텍스트 `/`, 포트 분리)

포트 예시: web `8766`(기존), journal·batch·file·ifin·ifex 는 WAS 담당이 정하는 `<p2>~<p6>` (6600·6700·6800·8736·3000·8090 은 이미 사용 중)

### 2-1. 소스 (NH-ATMS, 내부 Gitea)

| 파일 | 지금 | 변경 |
|---|---|---|
| `atms-common/.../jeus-common.yml` | `jeus.base-url: http://16.88.122.113:8766` 하나 | 모듈별 주소로: `jeus.url.web/journal/batch/file/ifin/ifex: http://16.88.122.113:<포트>` |
| `atms-web/.../application-jeus.yml` 게이트웨이 `routes` | `target: ${jeus.base-url}/journal` 등 (경로 붙임) | `target: ${jeus.url.journal}` 등 (경로 없음, 대상 앱이 `/`) — 로컬 프로파일(`localhost:8081~8085`)과 같은 형태 |
| 같은 파일 `agent.proxy.journal-base-url`, `atmf.remote.snd-msg-uri` | `${jeus.base-url}/journal`, `${jeus.base-url}/file/atmf/...` | `${jeus.url.journal}`, `${jeus.url.file}/atmf/...` |
| `atms-file/.../application-jeus.yml` `msg-async-resp-forward-uri` | `${jeus.base-url}/atms/remote-exec/...` (web 앞) | `${jeus.url.web}/atms/remote-exec/...` |
| `atms-journal/.../application-jeus.yml` `internal-/external-base-url` | `${jeus.base-url}/ifin`, `/ifex` | `${jeus.url.ifin}`, `${jeus.url.ifex}` |
| `jeus/web.xml` (프로파일 `jeus`), `SpringBootServletInitializer` | — | 그대로 |
| HMAC 필터·경로 비교의 `getContextPath()` 처리 | 컨텍스트 `/journal` 대응 | 그대로 둬도 동작 (컨텍스트가 빈 값이 될 뿐) |
| `docs/NH-ATMS_LOCAL_APPLY_GUIDE.md` | — | 위 변경분 반영 필요 |

### 2-2. 게이트웨이 유지 여부 (선택)
- **유지**: WebtoB 는 지금처럼 `/api/` 를 web 하나로 보내고, web 이 다른 포트의 모듈로 전달. 소스 변경은 2-1 뿐
- **제거 가능 (②·③ 에서 이점)**: 각 앱이 `/` 에서 자기 경로(`/api/journal/...`)를 그대로 받으므로 WebtoB `*URI` 에서 `/api/journal/`→journal 서버, `/api/batch/`→batch 서버 … 로 **직접 분배** → 게이트웨이 한 단계(측정: 요청당 ~2.6ms, 처리량 -25%, 스레드 2개 점유) 제거. 대신 WebtoB 에 서버(커넥터)·URI 가 6벌, `gateway.enabled: false`. ①은 JVM 이 하나라 WebtoB 커넥터도 하나 → 직접 분배하려면 가상호스트를 WebtoB 쪽에서 구분해야 해 복잡

### 2-3. 배포킷 (이 저장소)

| 파일 | 변경 |
|---|---|
| `deploy/jeus/swap-deploy.sh` | 그대로 (`catms*` 폴더 6개 유지 — 서버/가상호스트별 docBase 로 그대로 사용) |
| `deploy/jeus/deploy-war.sh` | `context_of`(경로) → 모듈별 **포트** 매핑, `status`·기동 확인 주소를 `http://16.88.122.113:<포트>/v3/api-docs` 로. **②·③ 은 재기동이 서버별**이 되므로 `restart <모듈>` 이 그 서버만 정지·기동하도록 변경 |
| jeusRstart 연동 | 지금 메뉴 5·6 은 `nbatmsltws01_atms01` 고정 → ②·③ 에서는 **나머지 서버용 메뉴가 없음** → WAS 담당에 메뉴 추가(서버별 boot/down) 요청 필요. ①은 그대로 |
| `deploy/jenkins/pipelines/backend.Jenkinsfile` | `RESTART` 가 ②·③ 에서는 배포한 모듈의 서버만 재기동 (6개 동시 중단이 없어짐) |
| `docs/` (BACKEND_CICD_PLAN 6장, JEUS_WEBTOB_DEPLOY_GUIDE, SERVER_DIRECTORY_LAYOUT, DEV_SERVER_INFRA 포트표, CLAUDE.md) | 구조 설명·포트 갱신 |

### 2-4. JEUS (WAS 담당 작업)

| 항목 | ① 리스너6 | ② 서버6 | ③ JEUS6 |
|---|---|---|---|
| 서버 생성 | 없음 | atms02~06 (base 리스너·HTTP 리스너·JVM 옵션·NodeManager 등록) | JEUS 설치·도메인 5개 추가 |
| 리스너 | atms01 에 HTTP 리스너 5개 추가 | 서버마다 1개 | 도메인마다 |
| 앱 등록 | 6개, 리스너별 가상호스트에 하나씩, context `/` | 6개, 서버마다 하나, context `/` | 도메인마다 하나 |
| 기존 `atms` 앱 | 가상호스트 지정 추가 | 그대로 (atms01) | 그대로 |
| WebtoB 커넥터 | 1 (기존) | 서버마다 (+5, registration-id 각각) | 도메인마다 |
| JVM 힙 (2코어 고려) | 기존 | 서버당 512m~768m 수준으로 낮춰야 함 (협의) | 동일 + DAS 힙 |
| 로그 | 기존 | 서버별 `/WASlogs/.../atms0N/` | 도메인별 |
| jeusRstart | 그대로 | 서버별 메뉴 추가 | 도메인별 |

### 2-5. WebtoB·방화벽

| 항목 | 변경 |
|---|---|
| WebtoB `http.m` | 게이트웨이 유지: `/api/` → web 서버 하나 (2-13 그대로). 게이트웨이 제거(②·③): 모듈별 `/api/<모듈>/` → 각 서버 JSV, `*SERVER` 6개 |
| PC → 113 방화벽 | 직접 확인용으로 각 모듈 포트를 쓰려면 `<p2>~<p6>` 추가 신청 (8766 만 신청돼 있음). 화면은 9000 경유라 필수는 아님 |
| ws01 내부 호출 | 게이트웨이·모듈 간 호출이 `16.88.122.113:<포트>` 로 (같은 서버라 방화벽 무관 — 단, 각 포트가 113 에 LISTEN 하는지 확인. 8766 은 127.0.0.1 에 없음) |

## 3. 권장

1. **자원 기준으로는 현재 A(또는 ①)**: 2코어 서버에 JVM 6개(②)는 기동만으로 부담. ①은 포트만 나뉘고 장애 격리 이점이 없어, 목적이 "주소를 로컬처럼" 이 아니라면 A 와 실익 차이가 작음
2. **격리·단독 재기동이 목적이면 ②**: 다만 WAS 담당에게 (a) 서버 5개 생성 가능 여부, (b) 서버당 힙, (c) jeusRstart 서버별 메뉴, (d) WebtoB 커넥터 추가를 먼저 확인. 필요하면 "web + 나머지 5개 묶음" 처럼 **서버 2~3개로 나누는 절충**도 가능
3. ③ 은 이 서버에 권장하지 않음
4. 어느 쪽이든 **지금 막힌 문제(atms 앱이 JEUS 에서 기동하지 않음 — Jakarta 지원 여부·기동 로그)** 가 먼저 풀려야 함. 그 원인은 방식과 무관

## 4. 결정 후 할 일 (순서)
1. 방식 확정 (①/②/절충) + WAS 담당 협의 (포트·서버명·힙·재기동 메뉴)
2. JEUS 구성 (WAS 담당)
3. 소스 2-1 변경 → 내부 Gitea push
4. 배포킷 2-3 변경 (deploy-war.sh 포트 매핑·서버별 재기동, Jenkinsfile) — 서버에는 수정분만 반영
5. WebtoB 2-5 (게이트웨이 유지/제거 결정에 따라)
6. Jenkins `redeploy` → 모듈별 `http://16.88.122.113:<포트>/v3/api-docs` 200 → 화면 확인
