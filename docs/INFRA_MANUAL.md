# NH-ATMS 프로젝트 — 인프라 구성 매뉴얼 (구축·배포·운영·인수인계)

작성일: 2026-10-30 · 대상: 개발서버 1호기(`nbatmsltws01`·`nbatmsltwb01`) 운영, 2호기(`nbatmsltws02`·`nbatmsltwb02`) 신규 구축 대비 · 작업 계정 `atmswas`

> 이 문서 한 권으로 다음을 처음부터 끝까지 따라 할 수 있게 썼다.
> - 반입물 제작 → 서버 셋팅 → Gitea·Jenkins → 배포 연결
> - JEUS·WebtoB 연동 → 첫 배포·검증
> - 2호기 → 운영·백업·보안 → 문제 해결
>
> 다른 문서는 근거·이력·더 깊은 설명용이다 (부록 C).
>
> 전제: JEUS 는 WAS 담당이 확정 구성도(1-1)대로 셋팅한다 — 도메인 1개 + 매니지드 서버 6개. 이 문서는 그 셋팅이 끝난 뒤 우리가 할 일과, 서버를 처음부터 다시 세울 때의 우리 작업 전부를 다룬다.

---

## 목차

| 장 | 내용 | 주로 하는 곳 |
|---|---|---|
| 0 | 이 문서 읽는 법 — 범위, 표기, 반드시 지킬 것 | — |
| 1 | 한눈에 보기 — 구성도, 값표(01/02), 포트·방화벽, 계정, 폴더, 버전 | — |
| 2 | 핵심 개념 8가지 (짧게, 자세한 것은 부록 A) | — |
| 3 | 할 일 순서 — **1호기에서 지금 남은 일** / 새로 구축할 때 | — |
| 4 | 반입물 만들기 (이미지·캐시·배포킷·해시) | 외부망 PC |
| 5 | 담당자에게 받아야 할 것 (선행 조건) | — |
| 6 | 서버 기본 셋팅 (계정·root 작업·폴더·업로드·이미지) | ws01 |
| 7 | Gitea | ws01, PC |
| 8 | Jenkins | ws01, PC |
| 9 | 배포 연결 (wb01 준비, 배포 키, Credentials, 잡, 웹훅) | ws01, wb01, PC |
| 10 | JEUS 구성 확인과 연결 (WAS 담당 셋팅 후) | ws01 |
| 11 | WebtoB 연동 (`http.m`) | wb01 |
| 12 | 소스 설정 (내부 Gitea 사본의 jeus 프로파일) | 내부 PC |
| 13 | 첫 배포와 검증 (모듈 → WebtoB → 화면·DB → 에이전트) | Jenkins, ws01, wb01, PC |
| 14 | 2호기(ws02·wb02) 구축 — 이중화 | ws02, wb02 |
| 15 | 일상 운영·점검 | — |
| 16 | 백업·복구 | — |
| 17 | 보안·계정·키 관리 | — |
| 18 | 트러블슈팅 색인 (증상 → 원인 → 조치) | — |
| 부록 A·B·C | 개념 상세 / 결정 이력 / 문서 지도 | — |

---

## 0. 이 문서 읽는 법

### 0-1. 범위

| 포함 (우리 작업) | 제외 (담당자 업무 — 5장에 "받아야 할 것"으로만) |
|---|---|
| 외부망 반입물 제작 (Gitea 이미지, 빌드 캐시, 배포킷, 해시) | JEUS 설치·도메인·서버 생성·앱 등록·재기동 메뉴 (WAS 담당) |
| ws·wb 서버 셋팅 (atmswas 계정) | WebtoB 설치·기동 (WebtoB 담당) |
| Gitea·Jenkins 구축, 배포 키·잡·웹훅 | 방화벽·L4·디스크·계정 발급 (인프라) |
| 내부 소스 설정, 빌드·배포·검증 | DB 계정·권한 (DBA) |
| 2호기 구축 절차, 운영·백업·보안, 트러블슈팅 | 반입 승인 (보안·반입 담당) |

- Jenkins 이미지는 현업이 만들지만, 사양과 확인 방법은 4-3 에 적었다
- WebtoB `http.m` 은 WebtoB 담당이 하거나, root 작업일에 우리가 한다. 두 경우 모두 11장

### 0-2. 표기

| 표기 | 뜻 |
|---|---|
| ★ | 2호기(02)에서 값이 바뀌는 곳 → 1-2 값표의 02 칸 값으로 바꿔 입력 |
| [확인 필요] | 아직 확인하지 못한 값·동작 |
| [미시험] | 문서로만 정리했고 이 서버에서 실제로 해 보지 않은 절차 — 처음 할 때 결과를 기록해 둔다 |
| [확인 날짜] | 그날 서버에서 직접 확인한 사실 |
| (root) | 1일 root 권한을 받은 날에만 하는 작업 |
| (담당) | WAS·WebtoB·인프라 담당자가 하는 작업 |
| T01 ~ T64 | 18장 트러블슈팅 번호 |

명령 블록 위에는 **어디서·어떤 계정으로** 치는지 적었다.
- `ws01 (atmswas)` / `wb01 (atmswas)`: HIWARE 로 그 서버에 atmswas 로 로그인한 셸
- `PC (PowerShell)`: 개발 PC
- `외부망 PC`: 인터넷이 되는 PC

### 0-3. 반드시 지킬 것

1. **`sudo`, `su`, `ssh …` 를 셸에 치지 않는다.** HIWARE 금지명령이라 3회 입력하면 세션이 잠긴다.
   - 실제로 집계된 사례: 2026-10-15 `sudo -l`, 2026-10-23 ws01 의 `ssh -i …` 각 1회
   - `ssh-keygen` 은 허용된다
   - root 작업일에도 `sudo` 를 치지 않고, root 로 직접 접속한다
2. **명령은 한 줄씩**, 줄바꿈 없이 끝까지 친다. 사진을 보고 옮겨 칠 때 경로가 잘리지 않게 한다.
3. **JEUS·WebtoB 재기동 수단은 정해진 것만 쓴다.**
   - jeusRstart 메뉴 3(jeusatms 의 java 전부 `kill -9`)·7·8(WebtoB) 은 쓰지 않는다 — 배포 스크립트도 거부한다
   - `webtobRstart` 는 실행하지 않는다
   - JEUS 프로세스를 손으로 kill 하지 않는다
4. **비밀값은 정해진 곳에만 둔다.**
   - 대상: DB 비밀번호, JWT 시크릿, Gitea 토큰, 개인키, Jenkins 관리자 비밀번호
   - 둘 곳: 내부 Gitea 사본, Jenkins Credentials, 서버의 600 파일
   - GitHub 등 외부로 올리지 않는다
   - **사진에 찍히지 않게** 한다 (화면을 찍기 전에 확인)
5. **컨테이너 옵션 두 가지는 쓰지 않는다** — 이 서버에서는 기동이 실패한다.
   - `--userns=keep-id` (T05)
   - `--cpus` (T06)
6. **컨테이너끼리는 이름으로 접속한다** (`atms-gitea:3000`, `atms-jenkins:8080`). 서버 IP 로는 서로 닿지 않는다 (T10).
7. **용도를 모르는 자원은 쓰지 않는다**: `/nhod`, 포트 6600·6700·6800.
8. **WebtoB DocBase(`wb01:~/docs`) 아래에는 배포 파일만 둔다.** 배포킷·백업·반입 파일을 두면 9000 포트로 누구나 내려받을 수 있다.
9. **저장소는 `main` 브랜치 하나**만 쓰고, 강제 푸시(`--force`)는 하지 않는다.

---

## 1. 한눈에 보기

### 1-1. 확정 구성도 (2026-10-27)

```
[개발 PC] 10.248.170.169 · .171 · .177 · .205
   │ 화면 HTTP 9000                        │ Gitea 3000 · Jenkins 8090 · (직접 확인용 8080~8085)
   ▼                                        ▼
nbatmsltwb01 16.88.122.112              nbatmsltws01 16.88.122.113   RHEL 9.8 · 2코어 · SELinux
WebtoB 5.0.0.4 (webatms) :9000          ├─ rootless Podman (atmswas) — podman 네트워크 atms-net
 ├ / · /index.html · /assets/…          │    ├─ atms-gitea   :3000  Git (HTTP 만)
 │     → DocBase /home/atmswas/docs     │    └─ atms-jenkins :8090  빌드·배포 (executor 1)
 └ http.m *URI ─── JSV :19000 ◀─────────┤   (JEUS 서버 쪽에서 WebtoB 로 접속해 온다)
    /api/journal/ /api/agent/ → ajou01  │
    /api/batch/               → abat01  └─ JEUS 9.1 (jeusatms) · 도메인 ATMS · DAS :8736 · NodeManager
    /api/file/ /atmf/         → file01       MS#1 atms01 :8080  atms-web           /home/atmswas/catms
    /api/if/internal/         → ifin01       MS#2 ajou01 :8081  atms-journal       /home/atmswas/cajou
    /api/if/external/         → ifex01       MS#3 abat01 :8082  atms-batch         /home/atmswas/cabat
    /api/(나머지) /atms/ /atmr/              MS#4 file01 :8083  atms-file (+21001) /home/atmswas/cfile
    /swagger-ui/ /v3/api-docs              MS#5 ifin01 :8084  atms-if-internal   /home/atmswas/cifin
    /actuator/                → atms01       MS#6 ifex01 :8085  atms-if-external   /home/atmswas/cifex

ATM·STM·공과금수납기 ─▶ 내부망 L4 (VIP [확인 필요]) ─▶ ws01 직접 (http, WebtoB 를 거치지 않음)
      저널 REST → ajou01 :8081 · 파일 REST → file01 :8083 · 파일 TCP 소켓 → file01 :21001
서버 6개 ─JDBC─▶ nbatmsltdb01 16.88.250.91:1531  Oracle · 서비스 NBATMSTP · 계정 ATMS_APP · 문자셋 KO16MSWIN949
ifin01·ifex01 ─▶ 대외MCA · API GW/FOS · 코어뱅킹 (지금은 mock)
Jenkins ─SSH 22 (배포 전용 키, 실행 명령 고정)─▶ ws01 호스트 deploy-war.sh · wb01 deploy-front.sh
```

요청 흐름 네 가지:

| 흐름 | 경로 |
|---|---|
| 화면 | PC 브라우저 → `wb01:9000` → 정적 파일은 DocBase, API 는 `*URI` 규칙대로 JSV(19000) 로 해당 JEUS 서버 |
| 에이전트 | 단말 → L4 → `ws01:8081`(저널 REST)·`8083`(파일 REST)·`21001`(파일 소켓). 레거시 `.do` 경로도 journal 에 있어 8081 직접으로 동작 |
| 모듈 간 호출 | `http://16.88.122.113:<포트>` 로 직접 (`jeus.url.*`)<br>· web → journal: 레거시 `.do` 전달<br>· web → file: 원격전문<br>· file → web: 비동기 응답<br>· journal → ifin·ifex: 연동 |
| 배포 | PC push → Gitea → 웹훅 → Jenkins 빌드<br>· 백엔드: SSH → ws01 `deploy-war.sh` → 폴더 교체 → 그 모듈 서버만 재기동<br>· 화면: SSH → wb01 `deploy-front.sh` → DocBase 교체 |

### 1-2. 값표 (1호기 / 2호기)

★ 표시가 있는 명령은 2호기에서 이 표의 02 칸 값으로 바꾼다. 02 칸의 빈 값은 담당 회신을 받는 대로 이 표에 채운다.

| 항목 | 1호기 (01) | 2호기 (02) ★ | 근거·비고 |
|---|---|---|---|
| WAS 호스트 | `nbatmsltws01` | `nbatmsltws02` | domain.xml 에 `nbatmsltws02_atms01` 정의 [확인 2026-10-15] |
| WAS 서비스 IP (HIWARE 접속 대상) | `16.88.122.113` | [확인 필요] | `hostname -I` 첫 값(`fe80::…`)은 IPv6 내부 주소라 쓰지 않음 |
| WAS 그 밖의 IP | 16.89.122.86 · 16.88.250.88 · 16.89.250.68 | [확인 필요] | 쓰지 않음 (기록용) |
| WebtoB 호스트 | `nbatmsltwb01` | `nbatmsltwb02` | JEUS 커넥터 `…-nbatmsltwb02_webatms` [확인 2026-10-15] |
| WebtoB IP | `16.88.122.112` | [확인 필요] | |
| WebtoB 포트 / JSV 포트 | 9000 / 19000 | 9000 / 19000 [가정] | |
| WebtoB 설치 / DocBase | `/WEBTOB/webtobATMS` / `/home/atmswas/docs` | 같음 [가정] | |
| JEUS 설치 | `/JEUS/jeusATMS` (jeusatms) | 같음 [가정] | |
| JEUS 도메인 / DAS | `ATMS` (`/JEUS/jeusATMS/domains/ATMS`) / ws01 `16.88.122.113:8736` | 같은 도메인에 노드 추가 [확인 필요] | 도메인 이름 [확인 2026-10-30] |
| JEUS 서버 (web·journal·batch·file·ifin·ifex) | `atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01` | `atms02`·`ajou02`·`abat02`·`file02`·`ifin02`·`ifex02` [가정 — WAS 담당 확인] | 이름 확정 [2026-10-27] |
| 서버 포트 (같은 순서) | 8080 · 8081 · 8082 · 8083 · 8084 · 8085 | 같음 [가정] | base 리스너 = HTTP 리스너 (포트 1개) |
| 앱 폴더 (docBase, 같은 순서) | `/home/atmswas/catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex` | 같음 | |
| WebtoB registration-id | 서버 이름과 같다고 가정 [WAS 회신] | 14-6 결정 | `http.m` `*SERVER` 이름과 같아야 연결됨 |
| 서버별 재기동 메뉴 (jeusRstart) | [WAS 회신] | [WAS 회신] | `~/deploy/deploy-war.env` |
| 앱 로그 폴더 | `/WASlogs/jeusATMSlogs/nbatmsltws01/app` | `/WASlogs/jeusATMSlogs/nbatmsltws02/app` ★ | 02 는 JVM 옵션 `-Djeus.log-dir` (14-6) |
| `jeus.host` (모듈 간 호출·21001 bind) | `16.88.122.113` (소스 기본값) | ws02 IP ★ | 02 는 JVM 옵션 `-Djeus.host` (14-6) |
| 앱 데이터 폴더 | `/home/atmswas/data` | 같은 경로, 서버마다 따로 | 공유 여부 14-8 |
| Gitea / Jenkins | ws01 `:3000` / `:8090` | 없음 (01 것을 같이 씀) | |
| DB | `16.88.250.91:1531` / `NBATMSTP` / `ATMS_APP` — 앱은 JEUS 데이터소스 **JNDI `dsATMS01`** 로 접속 (2026-11-02) | 같은 DB. 2호기 서버도 데이터소스 대상에 넣을지 [확인 필요] (지금은 ws01 서버 6개만) | 로컬 PC 는 `local-common.yml` 직접 접속 (12-4) |
| 에이전트 L4 VIP | [확인 필요] → `CHANGE_ME_L4_VIP` | 같은 VIP 가 01·02 로 분산 | 12-3 |
| 개발 PC | 10.248.170.169 · .171 · .177 · .205 | 같음 | 방화벽 출발지 |
| HIWARE 접속 출발지 | 16.24.126.203 | 같음 | |
| 계정 atmswas | uid 50219 · 주 그룹 atms(50082) + jeus(2002) | [확인 필요] (같은 uid 권장) | |
| 계정 jeusatms | uid 40119 · 주 그룹 jeus(2002) + comp(2001) | [확인 필요] | |

### 1-3. 포트·방화벽

| 출발 → 도착:포트 | 용도 | 1호기 상태 | 2호기에서 ★ |
|---|---|---|---|
| PC → wb01:9000 | 화면 | 열림 [확인 2026-10-21] | PC → wb02:9000 (또는 화면용 L4) |
| PC → ws01:3000 · 8090 | Gitea · Jenkins | 열림 [확인 2026-10-23] | 해당 없음 |
| PC → ws01:8080~8085 | 서버 직접 확인 (Swagger·health) | **신청 필요** (2026-10-16 신청한 8766 을 8080~8085 로 변경, T62) | PC → ws02:8080~8085 |
| ws01(JEUS) → wb01:19000 | JSV (WebtoB 연동) | 열림 [확인 2026-10-15] | ws01·ws02 → wb01·wb02:19000 |
| ws01(Jenkins) → wb01:22 | 화면 배포 | 열림 [확인 2026-10-21] | ws01 → wb02:22 |
| Jenkins 컨테이너 → ws01 호스트:22 | WAR 배포 (`host.containers.internal`) | 열림 [확인 2026-10-23] | ws01 → ws02:22 |
| ws01 → DB 16.88.250.91:1531 | JDBC | 열림 [확인 2026-10-26] | ws02 → DB |
| ATM망 → L4 → ws01:8081 · 8083 · 21001 | 에이전트 | **신청 필요** (L4 VIP 확정 후) | → ws02 도 |
| ws01(ifin01) → EAI :39021 | 내부 연동 | 쓰지 않음 (mock) | |
| 서버 안에서만 | DAS 8736, NodeManager(포트 [확인 필요]), Gitea↔Jenkins(atms-net) | — | |

쓰지 않는 포트:
- 6600·6700·6800: 은행 사용 중
- 8766: 옛 `nbatmsltws01_atms01`, 정리 대상
- 1024 미만: rootless 는 열 수 없음

서버 자체 방화벽(firewalld)은 꺼져 있다 [확인 2026-10-15]. 막는 곳은 PC·ATM망과 서버 사이의 **네트워크 방화벽**이다.

### 1-4. 계정·권한

| 계정 | 정체 | 로그인 | 하는 일 |
|---|---|---|---|
| `atmswas` | 업무팀 계정 = **우리 작업 계정** (ws·wb 모두) | HIWARE | 앱 폴더·DocBase 소유, 배포킷·Gitea·Jenkins 운영, 배포 |
| `jeusatms` | JEUS 엔진 계정 | 우리는 안 씀 | JEUS 실행. **jeus 그룹으로 `catms*` 읽기, `data` 쓰기** |
| `webatms` | WebtoB 엔진 계정 | 로그인 불가 | WebtoB 실행. **jeus 그룹으로 `docs` 읽기** |
| `root` | 1일 권한 (승인 절차) | root 직접 접속 | subuid·linger·`/WASlogs` 하위 폴더·WebtoB 설정 등 (6-2, 11장) |

권한 원칙:
- 엔진 계정(jeusatms·webatms)은 **jeus 그룹으로 읽기만** 한다. 쓰기는 `data` 폴더(2770)뿐이다
- atmswas 의 주 그룹은 `atms` 다. 그래서 JEUS 가 읽어야 하는 폴더는 setgid(`2xxx`)로 jeus 그룹을 물려받게 한다
- `~/atms`(배포킷·WAR·Gitea·Jenkins 데이터)는 700 으로 atmswas 만 본다. WAR 에 JWT 시크릿이 들어 있고, Jenkins·Gitea 데이터에 소스·키가 있기 때문이다 (DB 비밀번호는 2026-11-02 부터 JEUS 데이터소스에만 있음)

### 1-5. 폴더 구조

ws01 (`/home/atmswas`, 전용 디스크 30GB [확인 2026-10-21]):
```
/home/atmswas/                     750  atmswas:jeus   ← jeus 그룹이 들어갈(x) 수 있어야 함
├─ catms/ cajou/ cabat/ cfile/ cifin/ cifex/   2750  JEUS 앱 6개 (exploded WAR, 배포 때 통째로 교체)
├─ data/                           2770  앱이 쓰는 파일 (업로드·이미지·다운로드·atms-repo), jeus.data-dir
├─ deploy/                         700   backup/<폴더>-<시각> (모듈별 5개), deploy-history.log,
│                                         war-deploy.log, deploy-war.env(600)
├─ atms/                           700   배포킷 (이 저장소, NH-ATMS/ 제외)
│  ├─ bin/ conf/ deploy/ docs/ README.md
│  ├─ inbox/                             업로드 직후 (확인 전)
│  ├─ release/<모듈>/                    Jenkins 가 보낸 WAR + .sha256 (모듈별 5개)
│  ├─ images/                            반입 이미지 tar + SHA256SUMS.txt (적재 후 지워도 됨)
│  └─ infra/                       700
│     ├─ gitea/   data/ config/ gitea.env(600)            ← data·config 는 컨테이너 1000 = 호스트 subuid 소유
│     └─ jenkins/ home/ cache/{gradle,npm} keys/ jenkins.env(600)
├─ .ssh/authorized_keys            600   ws01 배포 키 1줄 (command=deploy-war.sh 고정)
├─ .config/containers/systemd/atms-gitea.container        Gitea 자동기동 (Quadlet)
├─ .config/systemd/user/gitea-backup.{service,timer}      Gitea 매일 백업
└─ .local/share/containers/        rootless Podman 이미지·컨테이너 저장소
/WASlogs/jeusATMSlogs/nbatmsltws01/app/<모듈>.log          앱 로그 (jeusatms 가 씀, 6개 서버 공용)
/JEUS/jeusATMS/logs → /WASlogs/jeusATMSlogs                JEUS 로그 (서버별 하위 폴더)
```

wb01 (`/home/atmswas`):
```
/home/atmswas/                     750  atmswas:jeus
├─ docs/                           2750 ★ WebtoB DocBase (화면 dist). 배포 때 통째로 교체
├─ deploy/                         700
│  ├─ bin/deploy-front.sh, swap-deploy.sh
│  ├─ inbox/ (받은 tar.gz 5개)   backup/ (docs 백업 5개)
│  ├─ kit/                             배포킷 deploy/ 폴더 (설치용, 끝나면 지워도 됨)
│  └─ front-deploy.log, front-current.txt, deploy-history.log
└─ .ssh/authorized_keys            600   wb01 배포 키 1줄 (command=deploy-front.sh 고정)
```

### 1-6. 소프트웨어 버전

| 구분 | 버전 | 비고 |
|---|---|---|
| OS | RHEL 9.8 (x86_64), SELinux enforcing | |
| 컨테이너 | Podman 5.8.2 / crun 1.27 (rootless) | Docker 없음 (설치에 root 필요해 반입 제외) |
| WAS | JEUS 9.1, OpenJDK 21.0.11 | DAS·NodeManager 를 jeusatms 가 실행 |
| WebtoB | 5.0.0.4 (5.0 SP0 Fix#4) | |
| Gitea | `gitea/gitea:latest-rootless` (2026-10-12 반입본 28.1.0, `versions.txt`) | SQLite 내장 |
| Jenkins | 2.580.1 LTS (JDK 21 Temurin, Debian trixie) + 플러그인 87개 (지정 16개) | Node 22.23.3, docker-ce-cli 29.8.2, compose 5.6.0 포함 |
| 빌드 | Gradle 8.14.4 (wrapper), Java 21 Temurin | Jenkins 에서는 `--offline` |
| 앱 | Spring Boot 3.5 / Java 21, WAR 6개 (`bootWar -Pjeus`) | |
| 화면 | React 19 · Vite 7 · Node 22 | |
| DB 드라이버 | 로컬: WAR 의 ojdbc11 + orai18n 23.7.0.25.01 / 개발서버: **JEUS 라이브러리의 Oracle JDBC + orai18n** (JNDI 풀이 씀) | orai18n 없으면 ORA-17056 (T22) |

---

## 2. 핵심 개념 8가지

처음 보는 사람이 이 문서를 따라가는 데 꼭 필요한 것만 적었다. 자세한 설명은 부록 A 에 있다.

1. **rootless Podman** — root 없이 atmswas 일반 계정으로 컨테이너(Gitea·Jenkins)를 돌린다.
   - 컨테이너 안 root = 호스트 atmswas
   - 컨테이너 안 사용자 1000 = 호스트에서는 atmswas 의 보조 uid(subuid)
   - 그래서 그 파일을 호스트에서 지우거나 소유자를 바꿀 때는 `podman unshare …` 를 붙인다
2. **linger** — atmswas 가 로그아웃해도 atmswas 의 systemd(`--user`)가 남게 하는 설정이다. 이것이 있어야 로그아웃·재부팅 뒤에도 컨테이너가 살아 있다. 켜는 것은 root 작업이다.
3. **JEUS 매니지드 서버** — 앱을 실행하는 JVM 하나다.
   - DAS(관리 서버)가 설정을 갖고, NodeManager 가 프로세스를 띄운다
   - 우리 구성은 서버 하나에 WAR 하나, 컨텍스트 `/`, 포트 하나
   - 로컬의 `java -jar` 6개와 실행 단위가 같다
4. **JSV 19000** — WebtoB 와 JEUS 를 잇는 연결이다.
   - **JEUS 서버가 WebtoB 의 19000 으로 접속해 온다** (방향 주의)
   - JEUS 커넥터의 registration-id 가 WebtoB `http.m` 의 `*SERVER` 이름과 같아야 짝이 맞는다
5. **exploded WAR + 폴더 교체** — JEUS 는 앱을 "폴더 경로"로 등록한다.
   - 우리는 새 폴더에 WAR 를 풀고 기존 폴더와 통째로 바꾼다 (기존 것은 백업)
   - 바꾼 뒤 **그 서버를 재기동해야** 반영된다
6. **jeus 프로파일** — `bootWar -Pjeus` 로 만든 WAR 는 `WEB-INF/web.xml` 이 프로파일 `jeus` 를 켠다.
   - 설정은 `jeus-common.yml`(공통) → `application-jeus.yml`(모듈) → `application.yml` 순으로 앞쪽이 이긴다
   - 실제 비밀값은 내부 Gitea 사본에만 넣는다
7. **명령 고정 배포 키** — Jenkins 가 쓰는 SSH 키는 서버의 `authorized_keys` 에서 `command=…` 로 묶여 있다. 그 키로는 배포 스크립트 하나만 실행된다 (셸 불가). `from=`(출발 IP)과 `restrict` 도 함께 건다.
8. **웹훅 → notifyCommit → PathRestriction** — push 가 Jenkins 빌드로 이어지는 방식이다.
   - push 하면 Gitea 가 Jenkins 를 깨운다 (POST)
   - 저장소는 하나지만, `Frontend/` 가 바뀌면 화면 잡만, `Backend/` 가 바뀌면 백엔드 잡만 빌드된다
   - 백엔드 웹훅 빌드는 배포하지 않는다 (재기동 시점은 사람이 정함)

---

## 3. 할 일 순서

### 3-1. 1호기에서 지금 남은 일 (2026-10-30 기준)

이미 끝난 것:
- [x] Gitea·Jenkins 기동 (Quadlet·`--restart=always`·linger), 빌드 캐시 마운트
- [x] PC → 112:9000·113:3000·8090 개방
- [x] Credentials 3개, 웹훅(POST), 백엔드 빌드
- [x] 화면 자동 배포(wb01)
- [x] orai18n 캐시 반입
- [x] WAR 배포 시도 (옛 단일 서버 구성)

남은 일 (위에서부터 순서대로):

| # | 할 일 | 누가 | 절 |
|---|---|---|---|
| **A. 지금 바로 (JEUS 셋팅 완료 전에도 가능)** | | | |
| 1 | 최신 배포킷(이 문서 포함) 반입 → ws01 `~/atms` 덮어쓰기, wb01 `~/deploy/kit` | 우리 | 4-5, 6-5 |
| 2 | ws01 앱 폴더 이름 정리 (`catms-journal` → `cajou` 등). **WAS 담당이 새 폴더로 앱을 등록하기 전에** | 우리 | 10-2 |
| 3 | `~/deploy/deploy-war.env` 새로 만들기 (예전 8766 설정 보관) | 우리 | 10-3 |
| 4 | Jenkins `nh-atms-backend` 잡의 Pipeline script 를 2026-10-27 판으로 교체 | 우리 | 9-7 |
| 5 | 내부 Gitea 소스: 반영 가이드 9~15단계 7개 파일 교체 + `CHANGE_ME` 기입 → push → `build-only` 성공 확인 | 우리 | 12장 |
| 6 | 앱 로그 폴더 `/WASlogs/jeusATMSlogs/nbatmsltws01/app` 확인 (없으면 WAS 담당 또는 root 날) | 담당/root | 10-4 |
| 6-1 | 화면 비밀번호 필드 암호화 키 `/home/atmswas/keys` 생성 (없으면 atms01 기동 실패), Jenkins npm 캐시에 `node-forge` 추가 | 우리 | [POST_HANDOVER_EDITS.md](POST_HANDOVER_EDITS.md) 0-2·0-3 |
| **B. WAS 담당 셋팅 완료 회신 후** | | | |
| 7 | JEUS 구성 확인 (서버 6개·포트·앱 경로·커넥터·메뉴) | 우리 (읽기만) | 10-1 |
| 8 | 서버별 재기동 메뉴 번호를 `deploy-war.env` 에 기입 (회신 값이 가정값과 다르면 그 항목도) | 우리 | [POST_HANDOVER_EDITS.md](POST_HANDOVER_EDITS.md) A~I |
| 9 | 첫 배포 `MODE=build-deploy`, `MODULE=all` → 6개 `/v3/api-docs` 200 | 우리 | 13-2, 13-3 |
| 10 | WebtoB `http.m` 반영 → wb01 19000 ESTAB, 모듈 경로마다 해당 서버로 (302·503 아님) | WebtoB 담당 또는 root 날 | 11장 |
| 11 | 화면 로그인 → 메뉴 → 조회 (DB) | 우리 | 13-4 |
| **C. 이어서** | | | |
| 12 | 방화벽: PC → 113:8766 신청분을 8080~8085 로 변경 (직접 확인용, 선택) | 인프라 | 1-3 |
| 13 | L4 VIP 확정 → `CHANGE_ME_L4_VIP` 기입·journal 재배포, ATM망 방화벽, L4 헬스체크 | 인프라 + 우리 | 13-5 |
| 14 | 에이전트 폴링 경로(`/api/agent/ping`) 소스 결함 조치 | 개발 | 13-5, T49 |
| 15 | 옛 서버 `nbatmsltws01_atms01`(8766) 정리, 8766 방화벽 회수 | WAS 담당 | 10-1 |
| 16 | 보안 담당 공유 (서버 간 SSH 배포, jeusRstart 자동 호출) | 우리 | 17-6 |
| 17 | 운영 습관: 매일 점검, 주 1회 Gitea 백업 반출 | 우리 | 15, 16장 |

### 3-2. 서버를 새로 구축할 때 (1호기 재구축 / 2호기)

```
[외부망] 4장 반입물 ──▶ [담당] 5장 선행 조건 (계정·디스크·root 승인·JEUS·WebtoB·방화벽)
   ──▶ 6장 서버 기본 ──▶ 7장 Gitea ──▶ 8장 Jenkins ──▶ 9장 배포 연결 (키·Credentials·잡·웹훅)
   ──▶ 10장 JEUS 확인·연결 ──▶ 11장 WebtoB ──▶ 12장 소스 설정 ──▶ 13장 첫 배포·검증
   ──▶ 15장 운영 · 16장 백업 · 17장 보안
```

| 경우 | 하는 장 | 빠지는 것 |
|---|---|---|
| 1호기 재구축 (디스크 교체·재설치) | 4 → 13 전부 + 16-5 복원 순서 | 없음 |
| 2호기 이중화 (ws02·wb02) | 14장 (안에서 6·9·10·11·13 의 해당 절을 가리킴) | Gitea·Jenkins (01 것을 같이 씀), 이미지·캐시 반입, Podman |

---

## 4. 반입물 만들기 (외부망 PC)

폐쇄망 서버는 인터넷이 안 된다. 서버에서 쓰는 것은 모두 외부망 PC 에서 만들어 반입 절차로 들여온다.

### 4-1. 반입 목록

| # | 파일 | 만드는 사람·곳 | 크기(대략) | 다시 만들 때 |
|---|---|---|---|---|
| 1 | `gitea-rootless.tar` | 우리, 외부망 PC (4-2) | 180MB | Gitea 업그레이드 |
| 2 | `nh-atms-jenkins-bank.tar`, `jenkins-plugins.txt` | **현업**, 외부망 PC (4-3) | 400MB (적재 후 1.21GB) | Jenkins·플러그인 버전 변경 |
| 3 | offline-seed 4개 (`gradle-modules-cache.tar.gz`, `gradle-wrapper-dists.tar.gz`, `npm-cache.tar.gz`, `manifest.csv`) | 우리, 외부망 PC (4-4) | 131MB · 134MB · 1GB · 1KB | 의존성(`build.gradle`, `package.json`, `package-lock.json`) 변경 |
| 4 | 배포킷 `ATMS-Server-main.zip` (이 저장소, `NH-ATMS/` 제외) | 우리, GitHub (4-5) | 수 MB 이하 | 배포킷·문서가 바뀔 때마다 |
| 5 | `SHA256SUMS.txt`, `versions.txt` | 우리 (4-6) | — | 위 파일과 함께 |

반입하지 않는 것:

| 대상 | 이유 |
|---|---|
| NH-ATMS 소스 | 폐쇄망 PC 에 이미 있다 (업체 GitLab clone). 우리 변경분은 반영 가이드(배포킷 `docs/NH-ATMS_LOCAL_APPLY_GUIDE.md`)를 보고 옮긴 뒤 내부 Gitea 로 push |
| JRE·nginx 이미지, compose 실행 파일 | JEUS 대안(컨테이너 실행)용이라 보류 ([REQUIRED_FILES.md](REQUIRED_FILES.md) 2-4) |
| Docker 엔진 | 설치·상시 실행에 root 가 필요 → Podman 사용 |

2호기는 Gitea·Jenkins·캐시를 1호기 것으로 같이 쓰므로 **배포킷만** 있으면 된다 (14장).

### 4-2. Gitea 이미지

외부망 PC (Docker, cmd·PowerShell 공통):
```bat
docker pull --platform linux/amd64 gitea/gitea:latest-rootless
docker save -o gitea-rootless.tar gitea/gitea:latest-rootless
docker run --rm --entrypoint gitea gitea/gitea:latest-rootless --version
```
- 판정: 세 번째 줄의 버전을 `versions.txt` 에 적는다. `gitea-rootless.tar` 약 180MB
- `--platform linux/amd64`: 외부망 PC 가 ARM(Mac M 시리즈 등)이어도 서버(x86_64)용 이미지를 받게 한다
- `latest-rootless` 는 받는 날마다 버전이 바뀐다. 운영 중인 Gitea 를 바꿀 때는 **16-3 전체 백업을 먼저** 한다 (Gitea 는 기동하면서 DB 를 새 버전으로 바꾸고, 예전 버전으로 되돌리는 것은 지원하지 않음)

### 4-3. Jenkins 이미지 (현업 제작)

사양은 저장소 `deploy/jenkins/` 에 고정돼 있다 (2026-10-08 업데이트 센터 기준).

| 파일 | 내용 |
|---|---|
| `Dockerfile` | `jenkins/jenkins:2.580.1-lts-jdk21` + Node 22.23.3 + docker-ce-cli 29.8.2 + compose 5.6.0 |
| `plugins.txt` | 지정 16개 (의존 71개 포함 87개) |
| `casc/jenkins.yaml` | 관리자 계정 자동 생성, executor 1개, 로그인한 사용자만 사용 |

현업에 넘기는 명령 (외부망 PC, 저장소를 받은 폴더에서):
```bat
docker build --platform linux/amd64 -t localhost/nh-atms-jenkins:bank deploy\jenkins
docker save -o nh-atms-jenkins-bank.tar localhost/nh-atms-jenkins:bank
docker run --rm localhost/nh-atms-jenkins:bank jenkins-plugin-cli --list > jenkins-plugins.txt
```

반입 전 확인:
```bat
docker run --rm --entrypoint java localhost/nh-atms-jenkins:bank -jar /usr/share/jenkins/jenkins.war --version
docker run --rm --entrypoint node localhost/nh-atms-jenkins:bank --version
docker run --rm --entrypoint docker localhost/nh-atms-jenkins:bank --version
docker run --rm --entrypoint docker localhost/nh-atms-jenkins:bank compose version
docker run --rm --entrypoint sh localhost/nh-atms-jenkins:bank -c "ls /usr/share/jenkins/ref/plugins/*.jpi | wc -l"
```

| 줄 | 정상 결과 (2026-10-12 실제) | 아니면 |
|---|---|---|
| 1 | `2.580.1` | 다시 빌드 |
| 2 | `v22.23.3` | 〃 |
| 3 | `Docker version 29.8.2, …` | 〃 |
| 4 | `Docker Compose version v5.6.0` | 〃 |
| 5 | `87` | 16 이하면 의존 플러그인 누락 → 다시 빌드 |

### 4-4. 빌드 캐시(offline-seed) 다시 만들기 [미시험]

**왜 필요한가.** Jenkins 는 폐쇄망에서 `gradlew --offline`, `npm ci --offline` 으로 빌드한다. 그래서 필요한 라이브러리가 모두 캐시에 있어야 한다.
- 지금 캐시는 2026-09-07 에 만든 것이다
- orai18n 은 2026-10-26 이후 `modules-2` 를 바꿔서 넣었다 (사용자 확인)
- 의존성을 추가·변경하면 아래처럼 다시 만들어 반입한다. 하지 않으면 빌드가 T19·T20 으로 실패한다

준비 (외부망 PC):
- 인터넷, Docker Desktop, JDK 21 (Temurin 권장 — 없으면 Gradle 이 자동으로 받음)
- 폐쇄망과 **같은 의존성의 NH-ATMS 소스**: `build.gradle` 들, `package.json`, `package-lock.json` 이 폐쇄망 Gitea 와 같아야 한다

① Gradle 캐시 (PowerShell) — 빈 전용 폴더(`D:\seed\gradle-home`)에 받는다:
```powershell
cd D:\NH-ATMS\Backend\nh-atms-api
$env:GRADLE_USER_HOME = "D:\seed\gradle-home"
.\gradlew.bat bootWar -Pjeus -x test
.\gradlew.bat bootWar -Pjeus -x test --offline
cd D:\seed
tar -czf gradle-modules-cache.tar.gz -C D:\seed\gradle-home\caches modules-2
tar -czf gradle-wrapper-dists.tar.gz -C D:\seed\gradle-home\wrapper dists
```
- 둘째 줄(온라인)이 캐시를 채우고, 셋째 줄(`--offline`)이 `BUILD SUCCESSFUL` 이면 캐시가 충분한 것
- 압축 안의 최상위가 각각 `modules-2/`, `dists/` 여야 한다 (8-1 에서 그 이름으로 옮김)
- `gradle-home\jdks` 는 넣지 않는다 (Jenkins 는 자기 JDK 사용)

② npm 캐시 (PowerShell + Docker) — **Jenkins 가 리눅스라서 리눅스 컨테이너에서 받는다.** 화면 빌드 도구(vite·rollup·esbuild)가 OS 별 바이너리를 따로 받기 때문에, Windows 에서 받은 캐시로는 리눅스 빌드가 실패한다 (T20):
```powershell
New-Item -ItemType Directory -Force D:\seed\front, D:\seed\npm-cache | Out-Null
Copy-Item D:\NH-ATMS\Frontend\nh-atms-web\package.json, D:\NH-ATMS\Frontend\nh-atms-web\package-lock.json D:\seed\front\
docker run --rm -v D:\seed\front:/app -v D:\seed\npm-cache:/npm-cache -w /app node:22.23.3-trixie-slim npm ci --cache /npm-cache --no-audit --no-fund
docker run --rm -v D:\seed\front:/app -v D:\seed\npm-cache:/npm-cache -w /app node:22.23.3-trixie-slim sh -c "rm -rf node_modules && npm ci --offline --cache /npm-cache --no-audit --no-fund"
cd D:\seed
tar -czf npm-cache.tar.gz npm-cache
```
- 넷째 줄(`--offline`)이 `ENOTCACHED` 없이 끝나면 캐시가 충분한 것
- 폐쇄망 PC(Windows)에서도 같은 캐시를 쓰려면, 압축 전에 PC 에서 `npm ci --cache D:\seed\npm-cache` 를 한 번 더 실행한다 (Windows 용 바이너리가 같은 캐시에 더해짐)

③ 목록·체크섬:
```powershell
Get-ChildItem *.tar.gz | Get-FileHash -Algorithm SHA256 | ForEach-Object { "{0},{1}" -f (Split-Path $_.Path -Leaf), $_.Hash.ToLower() } | Set-Content -Encoding ascii manifest.csv
Get-Content manifest.csv
```

### 4-5. 배포킷 zip

1. GitHub `LeeTaeSung0628/ATMS-Server` → 브랜치 `main` → Code → **Download ZIP** → `ATMS-Server-main.zip`
2. 7-Zip 으로 풀고, 안의 **`NH-ATMS` 폴더를 지운 뒤** 다시 zip 으로 묶는다
   - 반입 용량 제한이 있으면 7-Zip "분할 압축"으로 `.001`, `.002` … 로 나눈다 (2026-10 메일 반입은 95MB 분할)
3. 서버에는 zip 을 그대로 올리지 않는다. **내부 PC 에서 푼 폴더의 `bin`, `conf`, `deploy`, `docs`, `README.md`** 를 올린다 (6-5)
   - 이유: Windows 에서 만든 zip 을 서버에서 풀면 경로가 깨질 수 있다 (T47)

### 4-6. 해시·반입 메일

서버 확인용 `SHA256SUMS.txt` — 외부망 PC, 반입 파일이 모인 폴더에서 (PowerShell):
```powershell
Get-ChildItem *.tar, *.tar.gz, *.zip, *.001, *.002 -ErrorAction SilentlyContinue | ForEach-Object { "{0}  {1}" -f (Get-FileHash $_.Name -Algorithm SHA256).Hash.ToLower(), $_.Name } | Set-Content -Encoding ascii SHA256SUMS.txt
Get-Content SHA256SUMS.txt
```
- 서버에서 `sha256sum -c SHA256SUMS.txt` 로 그대로 확인할 수 있는 형식이다 (6-4)
- 두 칸 띄움·소문자·ascii 저장이 중요하다

반입 신청서에 SHA-512 를 요구하면:
```powershell
Get-ChildItem -File | Get-FileHash -Algorithm SHA512 | Format-List Path, Hash
```
- 서버 쪽 확인: `sha512sum <파일>`

`versions.txt` 에 적을 것:
- Gitea 버전 (4-2)
- Jenkins 2.580.1 + 플러그인 87개
- offline-seed 를 만든 날짜와 기준 소스(커밋)
- 배포킷 다운로드 날짜

반입 메일 예시:
```
제목: [반입 요청] NH-ATMS 개발서버 인프라 파일 (YYYY-MM-DD)

1. 목적: NH-ATMS 개발서버(nbatmsltws01 / nbatmsltwb01) 형상관리·빌드·배포 환경 구성(갱신)
2. 파일 (파일명 / 크기 / SHA-256 [/ SHA-512])
   - ATMS-Server-main.zip.001 / 95MB / <해시>
   - ...
3. 사용처: 내부 개발 PC → 개발서버 /home/atmswas/atms (atmswas 계정)
4. 성격: 배포 스크립트·문서(텍스트), 컨테이너 이미지(tar), 빌드 캐시(tar.gz). 설치형 실행 파일(exe) 없음
```

### 4-7. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| 서버에서 `sha256sum -c` 가 `FAILED` 또는 모든 줄 실패 | T45 |
| 서버에서 zip 을 풀었더니 `deploy\jenkins\Dockerfile` 같은 이름의 파일 하나만 생김 | T47 |
| Jenkins 이미지가 `<none>` 으로 적재됨 | T44 |
| 새 캐시로 빌드했는데 `Could not resolve` / `ENOTCACHED` | T19 / T20 |

---

## 5. 담당자에게 받아야 할 것 (선행 조건)

우리 작업 전에 끝나 있어야 하는 것들이다. 요청서 양식은 이 문서 범위 밖이라 기존 문서를 쓴다.

| 항목 | 담당 | 필요한 장 | 우리가 확인하는 방법 | 요청 문서 |
|---|---|---|---|---|
| ws·wb 에 `atmswas` 계정 (jeus 그룹, HIWARE 로그인) | 인프라 | 6 | `id` (6-1) | — |
| `/home/atmswas` 전용 디스크 **30GB 이상** (ws01 현재 30GB) | 인프라 | 6 | `df -h /home/atmswas` | — |
| 1일 root 권한 (subuid·linger 등이 없을 때) | 인프라·보안 | 6-2 | 6-1 결과 | [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) |
| PC → 서버 방화벽 (1-3 표) | 인프라 | 7~13 | PC `Test-NetConnection` (7-4) | [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) 10장 |
| JEUS: 도메인·노드·서버 6개·앱 6개 등록·WebtoB 커넥터·**서버별 재기동 메뉴** | WAS 담당 | 10 | 10-1 | [WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md) |
| 앱 로그 폴더 `/WASlogs/jeusATMSlogs/<호스트>/app` (jeusatms 쓰기) | WAS 담당 (또는 root) | 10-4 | `ls -ld` | 같은 요청서 2-1 |
| WebtoB `http.m` (JSV 서버그룹·서버 6개·URI) | WebtoB 담당 (또는 root 날 우리) | 11 | 11-3 | 같은 요청서 3장, 배포킷 `deploy/webtob/http.m.atms-jsv.txt` |
| DB 접속 정보, 서버 → DB 방화벽 | DBA·인프라 | 12 | 6-1 의 DB TCP 시험 | — |
| **JEUS 데이터소스 `dsATMS01`** (Oracle 드라이버, 서버 6개 대상, JEUS 라이브러리에 orai18n, 연결 시험) | WAS 담당 | 10-1, 13-3 | 10-1 의 `dsATMS01` 검색 | [WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md) 2-8 |
| 에이전트 L4 VIP·헬스체크·ATM망 방화벽 | 인프라 | 13-5 | 13-5 | 결정 문서 §11 |
| 반입 승인 | 보안·반입 | 4 | — | 4-6 메일 |
| 서버 간 SSH 배포·jeusRstart 자동 호출 공유 | 보안 | 17-6 | — | 17-6 문구 |

WAS 담당 회신으로 받아서 1-2 값표에 적을 것:
- 서버 이름 (바뀌었으면)
- 서버별 WebtoB 커넥터 registration-id
- 서버별 jeusRstart 메뉴 번호 (down / boot)
- base 리스너를 HTTP 와 분리했는지 (17-7)
- 옛 `nbatmsltws01_atms01` 정리 시점
- 데이터소스 `dsATMS01` 의 실제 export-name·대상 서버·연결 시험 결과 (WAS 요청서 2-8)

---

## 6. 서버 기본 셋팅 (ws01, atmswas)

> 2호기 ws02 는 이 장의 6-1, 6-2(linger 제외), 6-3, 6-5 만 한다 (14-4). wb 서버는 9-2.

### 6-1. 상태 확인 (읽기만)

ws01 (atmswas):
```bash
id
echo $HOME
ls -ld /home/atmswas
df -h /home/atmswas
echo $XDG_RUNTIME_DIR
podman --version
grep atmswas /etc/subuid /etc/subgid
loginctl show-user atmswas -p Linger
podman info --format '{{.Host.Security.Rootless}}'
podman info --format '{{.Host.CgroupControllers}}'
systemctl is-active firewalld
ss -ltn | grep -E ':(3000|8090|808[0-5]|21001) '
timeout 5 bash -c '</dev/tcp/16.88.250.91/1531' && echo DB-OPEN
timeout 5 bash -c '</dev/tcp/16.88.122.112/19000' && echo JSV-OPEN
python3 --version
```

| 명령 | 정상 (1호기 실제 값) | 아니면 |
|---|---|---|
| `id` | `uid=50219(atmswas) gid=50082(atms)` … 그룹에 `2002(jeus)` | jeus 그룹이 없으면 인프라 요청 (JEUS 가 앱 폴더를 못 읽음) |
| `echo $HOME` | `/home/atmswas` | 다르면 문서 경로가 안 맞음 → 멈추고 확인 |
| `ls -ld` | 처음엔 `atmswas atms 755` 일 수 있음 → 6-3 에서 `jeus 750` | 소유자가 atmswas 가 아니면 root(6-2) |
| `df -h` | 전용 LV, 여유 **20GB 이상** (1호기 30GB) | 인프라 요청. 그동안은 이미지 적재 후 tar 삭제 |
| `XDG_RUNTIME_DIR` | `/run/user/50219` | 비면 rootless Podman·`systemctl --user` 불가 → 결과 공유 |
| `podman --version` | `podman version 5.8.2` | |
| `grep … subuid subgid` | `atmswas:493216:65536` 두 줄 | 없으면 **6-2 필수** (이미지 적재 불가, T04) |
| `Linger` | `Linger=yes` | `no` 면 로그아웃할 때 컨테이너가 내려감 (T03) → 6-2 |
| `Rootless` | `true` | |
| `CgroupControllers` | `[memory pids]` | `cpu` 가 없는 것이 RHEL 9 기본 → CPU 제한은 쓰지 않음 (2026-10-16 보류 결정) |
| `firewalld` | `inactive` | `active` 면 6-2 에서 3000·8090 개방 |
| `ss -ltn` | 처음엔 아무것도 없음 (나중에 3000·8090, JEUS 셋팅 뒤 8080~8085) | 처음부터 무엇이 있으면 다른 프로그램이 쓰는 것 → 확인 |
| DB | `DB-OPEN` | 인프라에 서버 → DB 방화벽 요청 |
| JSV | `JSV-OPEN` | 인프라에 ws → wb:19000 요청 |
| `python3` | `Python 3.x` | WAR 풀 때 `unzip` 이 없으면 python3 를 쓴다 |

### 6-2. (root) 1일 root 작업

필요할 때만 한다: 6-1 에서 subuid·linger 가 없을 때, 앱 로그 폴더를 WAS 담당이 만들어 주지 않을 때, WebtoB 를 우리가 바꾸기로 했을 때.
- 권한은 하루짜리이고 끝나면 다시 쓸 수 없다 → 일상 운영에 root 가 필요한 구성은 만들지 않는다
- **root 계정으로 직접 접속**한다. atmswas 셸에서 `sudo` 를 치지 않는다 (T01)
- 할 일을 미리 정해 신청서에 적고, 명령·결과를 사진으로 남겨 완료 보고에 붙인다

| # | 작업 | 필요 조건 | 명령 (root 셸) |
|---|---|---|---|
| R1 | subuid·subgid 등록 | `grep` 결과가 없을 때 | 아래 R1 |
| R2 | linger 켜기 | `Linger=no` | `loginctl enable-linger atmswas` → `loginctl show-user atmswas -p Linger` 가 `Linger=yes` |
| R3 | firewalld 3000·8090 | firewalld `active` 일 때만 | `firewall-cmd --permanent --add-port=3000/tcp` → `--add-port=8090/tcp` → `firewall-cmd --reload` |
| R4 | 앱 로그 폴더 | WAS 담당이 안 만들었을 때 | 아래 R4 |
| R5 | WebtoB `http.m` 반영 | WebtoB 담당 대신 우리가 할 때 | 11-2 |

R1 — 다른 계정과 범위가 겹치지 않게 (예시 범위 300000~):
```bash
cat /etc/subuid
usermod --add-subuids 300000-365535 --add-subgids 300000-365535 atmswas
grep atmswas /etc/subuid /etc/subgid
```
→ 끝나면 **atmswas 로** `podman system migrate`

R4 — `★` 2호기는 `nbatmsltws02`:
```bash
ls -ld /WASlogs /WASlogs/jeusATMSlogs /WASlogs/jeusATMSlogs/nbatmsltws01
mkdir -p /WASlogs/jeusATMSlogs/nbatmsltws01/app
chown jeusatms:jeus /WASlogs/jeusATMSlogs/nbatmsltws01/app
chmod 2750 /WASlogs/jeusATMSlogs/nbatmsltws01/app
ls -ld /WASlogs/jeusATMSlogs/nbatmsltws01/app
```
- 상위 폴더 소유는 바꾸지 않는다 (WAS 담당 영역). 2750 = jeusatms 가 쓰고 atmswas 는 jeus 그룹으로 읽는다

root 가 있어도 하지 않는 것:

| 하지 않는 것 | 이유 |
|---|---|
| `domain.xml` 수정, 서버·앱 등록 | WAS 담당 영역 — 요청 |
| jeusRstart 3·7·8, `webtobRstart` | 0-3 |
| Docker 엔진 설치, root 로 컨테이너 실행 | 권한이 끝나면 관리 불가 |
| atmswas 에 영구 sudo | 신청 범위 밖 |

끝나기 전 확인 (atmswas 로 다시 접속):
```bash
~/atms/bin/precheck.sh 8090
```
→ subuid·Linger 항목이 `[OK]`

상세·이력: [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md)

### 6-3. 폴더 만들기 (root 불필요)

ws01 (atmswas):
```bash
cd /home/atmswas
mkdir -p catms cajou cabat cfile cifin cifex data deploy/backup
mkdir -p atms/inbox atms/release atms/images atms/infra
chgrp jeus /home/atmswas /home/atmswas/catms /home/atmswas/cajou /home/atmswas/cabat /home/atmswas/cfile /home/atmswas/cifin /home/atmswas/cifex /home/atmswas/data
chmod 750 /home/atmswas
chmod 2750 /home/atmswas/catms /home/atmswas/cajou /home/atmswas/cabat /home/atmswas/cfile /home/atmswas/cifin /home/atmswas/cifex
chmod 2770 /home/atmswas/data
chmod 700 /home/atmswas/deploy /home/atmswas/atms /home/atmswas/atms/infra
ls -la /home/atmswas
namei -l /home/atmswas/catms
```

| 줄 | 정상 |
|---|---|
| `ls -la` 의 `.` | `drwxr-x---  atmswas jeus` |
| `catms` ~ `cifex` | `drwxr-s---  atmswas jeus` (s = setgid: 안에 생기는 파일도 jeus 그룹) |
| `data` | `drwxrws---  atmswas jeus` |
| `atms`, `deploy` | `drwx------` |
| `namei -l` | `/home` → `atmswas`(drwxr-x--- jeus) → `catms`(drwxr-s--- jeus): 모든 단계에 jeus 그룹이 들어갈 수 있음 |

- `chgrp: Operation not permitted` → atmswas 가 jeus 그룹이 아님 (6-1 `id`) → 인프라
- 이유: JEUS(jeusatms)는 jeus 그룹으로 앱 폴더를 읽는다. 상위 폴더 하나라도 그룹 `x` 가 없으면 못 들어간다
- 앱 폴더는 배포 때 통째로 교체되지만, 교체 스크립트가 그룹·권한을 이어받는다 (`swap-deploy.sh`)

### 6-4. 업로드·무결성

내부 PC: FileZilla 를 **atmswas** 로 접속하고, 메뉴 **전송 → 전송 유형 → 바이너리**를 먼저 고른다. 반입 파일 전부(이미지 tar, offline-seed 4개, `SHA256SUMS.txt`, `versions.txt`, `jenkins-plugins.txt`)를 `/home/atmswas/atms/images/` 에 올린다.

ws01 (atmswas):
```bash
cd ~/atms/images
ls -l
sha256sum -c SHA256SUMS.txt
```
- 모든 줄이 `OK` 면 정상. 하나라도 `FAILED` 면 그 파일만 바이너리로 다시 올린다 (T45)

### 6-5. 배포킷 배치·점검 (처음 + 다시 반입할 때마다)

내부 PC: 배포킷 zip 을 7-Zip 으로 푼다 → 풀린 폴더 **안의** `bin`, `conf`, `deploy`, `docs`, `README.md` 를 FileZilla(바이너리)로 `/home/atmswas/atms/` 에 올린다 (같은 이름은 덮어쓰기).
- 결과가 `/home/atmswas/atms/bin/` 이어야 한다 (`/home/atmswas/atms/ATMS-Server-main/bin/` 이면 잘못)
- 덮어써도 실제 설정·데이터는 zip 에 없으므로 그대로 남는다: `conf/*.env`, `infra/`, `images/`, `release/`, `~/deploy/deploy-war.env`, `~/atms/backup/`, `~/.config/systemd/user/gitea-backup.*` (실제 적용본)
- **zip 이 2026-11-04 이전 판이면** 올리기 전에 내부 PC 에서 `deploy\gitea\gitea-backup.service` 를 수정본(T65)으로 바꾼다. 확인: `findstr /c:"-f - >" deploy\gitea\gitea-backup.service` 가 한 줄
- 올리기 전 사본 (되돌릴 때 `tar -C ~/atms -xzf …`): `tar -C ~/atms -czf ~/atms/kit-before-$(date +%Y%m%d).tar.gz bin conf deploy docs README.md`

ws01 (atmswas):
```bash
cd ~/atms
ls
sed -i 's/\r$//' deploy/gitea/gitea-backup.service
grep -c -- "-f - >" deploy/gitea/gitea-backup.service
diff deploy/gitea/gitea-backup.service ~/.config/systemd/user/gitea-backup.service && echo SAME
find bin deploy -name "*.sh" -exec sed -i 's/\r$//' {} +
find bin deploy -name "*.sh" -exec chmod 700 {} +
for f in bin/*.sh deploy/*/*.sh; do bash -n "$f" || echo "FAIL $f"; done
ls deploy/gitea deploy/jenkins deploy/jeus deploy/webtob
bin/precheck.sh 8090
```

| 줄 | 정상 |
|---|---|
| `ls` | `bin conf deploy docs images inbox infra release README.md` |
| `sed`·`chmod` | 출력 없음 (Windows 줄바꿈 제거·실행 권한) |
| `grep -c -- "-f - >"` | `1` (`0` 이면 옛 백업 서비스가 올라간 것 → 수정본을 다시 올림, T65) |
| `diff … && echo SAME` | `SAME` (실제 적용본과 같음). 다르면 `cp deploy/gitea/gitea-backup.service ~/.config/systemd/user/` → `systemctl --user daemon-reload` |
| `bash -n` | **아무것도 안 나옴** (`FAIL …` 이 나오면 그 파일을 다시 올림) |
| `ls deploy/…` | `install-gitea.sh` · `run-jenkins.sh jenkins.env.example pipelines` · `deploy-war.sh deploy-war.env.example install-deploy-war.sh swap-deploy.sh` · `deploy-front.sh http.m.atms-jsv.txt install-wb01.sh` |
| `precheck.sh` | `[NG]` 없음 (`[WARN]` 은 내용 확인) |

- `bin/init-dirs.sh` 는 실행하지 않는다 (컨테이너 대안 경로용)
- 이 문서도 `~/atms/docs/INFRA_MANUAL.md` 로 같이 들어온다

### 6-6. 이미지 적재

ws01 (atmswas):
```bash
cd ~/atms/images
podman load -i gitea-rootless.tar
podman load -i nh-atms-jenkins-bank.tar
podman images
podman run --rm --entrypoint sh localhost/nh-atms-jenkins:bank -c "ls /usr/share/jenkins/ref/plugins/*.jpi | wc -l"
```

| 확인 | 정상 |
|---|---|
| `podman images` | `docker.io/gitea/gitea   latest-rootless` 와 `localhost/nh-atms-jenkins   bank` |
| 마지막 줄 | `87` |

적재를 확인했으면 디스크를 위해 tar 를 지운다 (원본은 내부 PC 에 보관):
```bash
rm ~/atms/images/gitea-rootless.tar ~/atms/images/nh-atms-jenkins-bank.tar
```

- [ ] 6장 끝: `id`·Linger·subuid 정상, 폴더 권한 정상, `bash -n` 통과, 이미지 2개 적재

### 6-7. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| `sudo`·`ssh` 를 쳤더니 `not allowed … Prohibited input count` | T01 |
| `podman load` 에 `potentially insufficient UIDs or GIDs` | T04 |
| 로그인했더니 `/home/atmswas` 가 비어 있음 | T07 |
| 이미지가 `<none>` 으로 적재 | T44 |
| `$'\r': command not found` | T46 |
| `no space left on device` | T48 |

---

## 7. Gitea (ws01)

Gitea 는 사내 Git 서버다. 개발 PC 가 `http://16.88.122.113:3000` 으로 push 하고, Jenkins 가 `http://atms-gitea:3000` 으로 받는다.
- 접속은 HTTP 만 (SSH 끔), DB 는 내장 SQLite, 가입 차단, 로그인해야 보임, 저장소 기본 비공개, 오프라인 모드

### 7-1. 설치·기동

ws01 (atmswas):
```bash
~/atms/deploy/gitea/install-gitea.sh 16.88.122.113
```
- 인자는 **PC 가 접속하는 IPv4** (`★` 2호기에는 Gitea 를 두지 않는다)
- 마지막에 `Gitea 정상 기동 (n회차)` 와 관리자 생성 안내가 나오면 성공

스크립트가 하는 일 (다시 실행해도 데이터·비밀키 유지):
1. `~/atms/infra/gitea/{data,config}` 생성 → 컨테이너 git(1000) 소유로 (`podman unshare chown`)
2. `gitea.env`(600) 생성 — 서버 IP 반영, SECRET_KEY·INTERNAL_TOKEN 자동 생성
3. podman 네트워크 `atms-net` 생성·연결 (Jenkins 와 이름으로 통신)
4. linger 가 있으면 **Quadlet**(`~/.config/containers/systemd/atms-gitea.container`, 재부팅 자동기동), 없으면 `podman run`
5. 메모리 768MB 제한 (CPU 제한은 보류 — 0-3 의 5)

확인:
```bash
curl -s http://127.0.0.1:3000/api/healthz
podman ps --filter name=atms-gitea
systemctl --user is-active atms-gitea
podman network inspect atms-net --format '{{.DNSEnabled}}'
```
→ `"status": "pass"` 포함 / `Up` / `active` / `true`

### 7-2. 관리자 계정

ws01 (atmswas):
```bash
podman exec atms-gitea gitea admin user create --admin --username atmsadmin --email atmsadmin@atms.local --random-password --must-change-password
```
- 임시 비밀번호가 **한 번만** 출력된다. 바로 PC 에서 로그인해 바꾼다 (화면 사진은 바꾼 뒤 지운다)

### 7-3. 매일 자동 백업 (linger 필요)

ws01 (atmswas):
```bash
mkdir -p ~/.config/systemd/user
cp ~/atms/deploy/gitea/gitea-backup.service ~/atms/deploy/gitea/gitea-backup.timer ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user enable --now gitea-backup.timer
systemctl --user list-timers | grep gitea
```
- 매일 02:30 `~/atms/backup/gitea/gitea-dump-YYYYMMDD.tar.gz`, 7일 보관 (atmswas 소유)
- 2026-11-04 전 배포킷은 Gitea 데이터 폴더 안(`data/backup`)에 만들어 매일 크기가 불어났다 → T65 로 바꾼다
- 같은 서버에만 있으면 서버와 함께 잃는다 → 주 1회 반출 (16-2)

### 7-4. PC 접속 확인

PC (PowerShell):
```powershell
Test-NetConnection 16.88.122.113 -Port 3000
```
- `TcpTestSucceeded : True` → 브라우저 `http://16.88.122.113:3000`
- `False` 면 방화벽: 서버 쪽 `ss -ltn | grep :3000` 은 나오는데 PC 만 실패하면 네트워크 방화벽 (T11)

### 7-5. 화면 설정 (PC 브라우저, `atmsadmin`)

결정 (2026-10-23):
- 개발자 4명 = 팀 `developers` (쓰기)
- `jenkins-bot` = 팀 `readers` (읽기)
- 관리 = `atmsadmin` (조직 Owners)
- `main` 은 강제 푸시·삭제만 금지

| 순서 | 메뉴 | 값 |
|---|---|---|
| ① 조직 | 우상단 `+` → 새 조직 | 이름 **`NH-ATMS`**, 공개 범위 **비공개**, "저장소 관리자 권한(팀에서 협업자 추가·제거)" **해제** |
| ② 사용자 | 프로필 → 사이트 관리 → 사용자 계정 → 생성 (5개) | 인증 Local, 영문 ID, 사람마다 다른 이메일 (`jenkins-bot` 은 `jenkins-bot@atms.local`), 임시 비밀번호, **비밀번호 변경 필요** 체크 → 만든 뒤 편집에서 "조직 생성 가능" 해제 |
| ③ 팀 | 조직 → 팀 → 새 팀 | `developers`: 모든 저장소, 코드·이슈·PR·릴리스·위키·프로젝트 **쓰기** / `readers`: 모든 저장소, 코드·릴리스 **읽기**, 나머지 없음. "관리자 접근"·"저장소 만들기" 는 고르지 않음. 구성원: developers ← 4명, readers ← `jenkins-bot` |
| ④ 저장소 | 조직 → 새 저장소 | 소유자 `NH-ATMS`, 이름 **`nh-atms`**, **비공개**, 템플릿·README·.gitignore 없음, **저장소 초기화 해제**, 기본 브랜치 `main` |
| ⑤ 보호 | (첫 push 뒤) 저장소 → 설정 → 브랜치 → 보호 추가 | 패턴 `main`, 푸시 허용(쓰기 권한자 모두), **강제 푸시 금지**, 승인·상태 확인 사용 안 함 |

- 조직·저장소 이름은 Jenkinsfile 의 `GITEA_REPO`(`http://atms-gitea:3000/NH-ATMS/nh-atms.git`)·웹훅 주소와 **글자까지 같아야** 한다

### 7-6. 토큰·첫 push·팀원 clone (PC)

토큰 (각자): 로그인 → 프로필 → 설정 → 애플리케이션 → 토큰 생성

| 항목 | 개발자 | `jenkins-bot` |
|---|---|---|
| 이름 | `pc-git` | `jenkins` |
| 저장소·조직 접근 | 모든 저장소 | 같음 |
| 권한 `repository` | **읽기 및 쓰기** | **읽기** |
| 그 밖의 권한 | 선택 안 함 | 선택 안 함 |

→ 화면에 한 번만 보이므로 바로 복사·보관한다. 토큰 = 비밀번호다 (메신저·문서·사진 금지)

첫 push (업체 GitLab 에서 clone 한 PC, 저장소 최상위에 `Backend`·`Frontend` 가 보이는 폴더에서) — PC (PowerShell):
```powershell
git config --global user.name "<이름>"
git config --global user.email "<Gitea 계정 이메일>"
git config --global credential.helper manager
git config --global core.autocrlf true
cd D:\NH-ATMS
git status
git remote add bank http://16.88.122.113:3000/NH-ATMS/nh-atms.git
git push -u bank main
```
- 로그인 창: 사용자명 = Gitea ID, 비밀번호 = **토큰**
- 업체 브랜치가 `master` 면 `git push -u bank master:main`. 업체 원격(`origin`)은 그대로 둔다
- 이어서 반영 가이드의 JEUS 변경분을 두 번째 커밋으로 (12-2)

팀원 (각자 PC):
```powershell
cd D:\
git clone http://16.88.122.113:3000/NH-ATMS/nh-atms.git NH-ATMS
cd NH-ATMS
git log --oneline -3
```
- 매일: `git pull` → 작업 → `git add` → `git commit` → `git push` (main 에 직접, 강제 푸시 금지)

- [ ] 7장 끝: `healthz` pass, Quadlet active, 백업 타이머, 조직·팀·계정·저장소·보호, 첫 push

### 7-7. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| `install-gitea.sh` 가 IP 를 거부 / `ROOT_URL` 이 `http://fe80::…` | T02 |
| 옛 스크립트로 `data/backup: Permission denied` | T09 |
| PC 에서 3000 접속 시간 초과 | T11 |
| push 때 `Authentication failed` / `403` / `rejected (fetch first)` | T42 / T43 |
| 화면 시간이 9시간 차이 | T59 |
| 재부팅 뒤 Gitea 가 안 뜸 | T52 |

---

## 8. Jenkins (ws01)

Jenkins 는 Gitea 의 소스를 받아 화면(npm)·백엔드(Gradle)를 빌드하고, SSH 로 wb01·ws01 에 배포한다.
- 컨테이너 `atms-jenkins`, 호스트 포트 8090 → 컨테이너 8080
- 데이터 `~/atms/infra/jenkins/home`, 빌드 캐시 `~/atms/infra/jenkins/cache/{gradle,npm}`
- executor 1개 (2코어를 JEUS 와 나눠 씀)

### 8-1. 빌드 캐시 넣기 (처음 한 번 — 컨테이너 만들기 전에)

ws01 (atmswas):
```bash
df -h /home/atmswas
mkdir -p ~/atms/inbox/seed ~/atms/infra/jenkins/cache/gradle/caches ~/atms/infra/jenkins/cache/gradle/wrapper
chmod 700 ~/atms/infra/jenkins
cd ~/atms/images
tar -tzf gradle-modules-cache.tar.gz | head -3
tar -tzf gradle-wrapper-dists.tar.gz | head -3
tar -tzf npm-cache.tar.gz | head -3
tar -xzf gradle-modules-cache.tar.gz -C ~/atms/inbox/seed
tar -xzf gradle-wrapper-dists.tar.gz -C ~/atms/inbox/seed
tar -xzf npm-cache.tar.gz -C ~/atms/inbox/seed
mv ~/atms/inbox/seed/modules-2 ~/atms/infra/jenkins/cache/gradle/caches/
mv ~/atms/inbox/seed/dists ~/atms/infra/jenkins/cache/gradle/wrapper/
mv ~/atms/inbox/seed/npm-cache ~/atms/infra/jenkins/cache/npm
ls ~/atms/infra/jenkins/cache/gradle/caches ~/atms/infra/jenkins/cache/gradle/wrapper/dists ~/atms/infra/jenkins/cache/npm
```
- `tar -tzf … | head` 는 풀기 전에 맨 위 이름 확인: `modules-2/…`, `dists/…`, `npm-cache/…` 여야 한다
- 마지막 줄 정상: `modules-2` / `gradle-8.14.4-bin` / `_cacache …`
- `cache/npm` 이 이미 있으면 `mv` 가 그 안으로 들어가 버린다 → 처음 설치가 아니면 8-4 교체 절차
- 백엔드 wrapper 는 **Gradle 8.14.4** — 소스의 `gradle-wrapper.properties` 와 같아야 오프라인 빌드가 된다

### 8-2. 설정 파일·기동

ws01 (atmswas):
```bash
~/atms/deploy/jenkins/run-jenkins.sh
vi ~/atms/infra/jenkins/jenkins.env
~/atms/deploy/jenkins/run-jenkins.sh
```
1. 첫 실행은 `jenkins.env` 를 예시에서 만들고 `… CHANGE_ME 를 바꾼 뒤 다시 실행` 으로 멈춘다 (정상)
2. `vi` 에서 `JENKINS_ADMIN_PASSWORD=CHANGE_ME` 를 실제 비밀번호로 바꾼다
   - `'` 는 쓰지 않는다
   - 나머지 줄은 그대로 둔다: `JENKINS_URL=http://16.88.122.113:8090/`, `-Xmx1g`, `GRADLE_USER_HOME=/cache/gradle`, `npm_config_cache=/cache/npm`, `TZ=Asia/Seoul`
3. 두 번째 실행의 정상 출력:
   - `소유자 맞춤: …` (처음만) — 데이터·캐시를 컨테이너 jenkins(1000) 소유로
   - `자원 제한: --memory=2g`
   - `기동: atms-jenkins (localhost/nh-atms-jenkins:bank, 네트워크 atms-net, 포트 8090)`
   - `재부팅 자동기동: podman-restart.service enabled`
   - `Jenkins 정상 기동 (n회차)` + `GRADLE_USER_HOME=/cache/gradle npm_config_cache=/cache/npm` + `플러그인 87개`

- 관리자 계정은 이미지 안 JCasC 가 `jenkins.env` 값으로 만든다 → 관리자 비밀번호의 기준은 `jenkins.env` 다
- 메모리를 바꿀 때: `JENKINS_MEMORY=3g ~/atms/deploy/jenkins/run-jenkins.sh` (데이터 유지)
- 이 스크립트는 **언제 다시 실행해도 데이터를 지우지 않는다** — 컨테이너만 다시 만든다 (설정 변경·이미지 교체 때 그대로 사용)

### 8-3. 확인

ws01 (atmswas):
```bash
podman ps --filter name=atms-jenkins
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8090/login
podman exec atms-jenkins sh -c 'echo $GRADLE_USER_HOME $npm_config_cache; ls /cache/gradle/wrapper/dists; ls /cache/npm | head -3'
podman exec atms-jenkins curl -s -m 5 -o /dev/null -w "%{http_code}\n" http://atms-gitea:3000/api/healthz
podman exec atms-gitea curl -s -m 5 -o /dev/null -w "%{http_code}\n" http://atms-jenkins:8080/login
podman exec atms-jenkins bash -c "echo > /dev/tcp/16.88.122.112/22" && echo WB01-22-OK
systemctl --user is-enabled podman-restart.service
```
→ `Up` / `200` / `/cache/gradle /cache/npm` · `gradle-8.14.4-bin` · `_cacache` / `200` / `200` / `WB01-22-OK` / `enabled`

PC: `Test-NetConnection 16.88.122.113 -Port 8090` → `True` → 브라우저 `http://16.88.122.113:8090` → `admin` 로그인

### 8-4. 캐시 교체 (의존성이 바뀌어 4-4 로 새로 만든 뒤)

ws01 (atmswas) — 빌드가 돌고 있지 않을 때. Gradle 예:
```bash
mkdir -p ~/atms/inbox/seed
tar -xzf ~/atms/images/gradle-modules-cache.tar.gz -C ~/atms/inbox/seed
podman unshare mv ~/atms/infra/jenkins/cache/gradle/caches/modules-2 ~/atms/infra/jenkins/cache/gradle/caches/modules-2.old
podman unshare mv ~/atms/inbox/seed/modules-2 ~/atms/infra/jenkins/cache/gradle/caches/
~/atms/deploy/jenkins/run-jenkins.sh
```
- 캐시 폴더는 컨테이너 1000(= 호스트 subuid) 소유라서 `podman unshare` 를 붙인다
- `run-jenkins.sh` 가 새 파일 소유자를 1000 으로 맞추고 컨테이너를 다시 만든다 (마운트도 새 폴더로)
- 백엔드 잡 `build-only` 가 성공하면 옛 것을 지운다: `podman unshare rm -rf ~/atms/infra/jenkins/cache/gradle/caches/modules-2.old`
- npm 은 같은 방식으로 `cache/npm` 을 `cache/npm.old` 로 바꾸고 새 `npm-cache` 를 `cache/npm` 으로 옮긴다
- orai18n 처럼 jar 하나만 필요해도 **`modules-2` 통째로** 바꾼다 (Gradle 은 jar 와 함께 색인(`metadata-2.*`)도 본다)

- [ ] 8장 끝: `/login` 200, 캐시 마운트, Gitea↔Jenkins 200/200, `WB01-22-OK`, podman-restart enabled, PC 로그인

### 8-5. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| `crun: readlink …` / `controller 'cpu' is not available` | T05 / T06 |
| `Failed to bind port 8090 (Address already in use)` | T08 |
| 빌드에서 `GRADLE_USER_HOME=미설정` → `UnknownHostException` | T17 |
| `….lck (Permission denied)` | T18 |
| `Could not resolve` / `ENOTCACHED` / `Cannot find a Java installation` | T19 / T20 / T21 |
| 빌드 중 컨테이너가 죽음 (exit 137) | T51 |
| 관리자 로그인 불가 | T58 |

---

## 9. 배포 연결 — wb01 준비, 배포 키, Credentials, 잡, 웹훅

### 9-1. 구조 (Jenkins Credentials 3개)

| Credential ID (글자까지 같게) | 종류 | 무엇에 쓰나 | 받는 쪽 |
|---|---|---|---|
| `gitea-jenkins-bot` | **Username with password** (`jenkins-bot` / 토큰) | 소스 받기 (두 잡) | Gitea 계정 `jenkins-bot` (readers 팀, 읽기) |
| `wb01-deploy-key` | SSH Username with private key (`atmswas`) | 화면 배포 | wb01 `authorized_keys`: `restrict,from="16.88.122.113",command="/home/atmswas/deploy/bin/deploy-front.sh"` |
| `ws01-deploy-key` | SSH Username with private key (`atmswas`) | WAR 배포·재기동 | ws01 `authorized_keys`: `restrict,from="16.88.122.113",command="/home/atmswas/atms/deploy/jeus/deploy-war.sh"` |

배포 키를 이렇게 묶는 이유:
- 키가 새도 할 수 있는 일이 정해진 스크립트의 정해진 하위 명령뿐이다
  - 화면: `deploy`·`rollback`·`status`
  - WAR: `deploy`·`rollback`·`status`·`restart`
- 그 밖의 명령은 거부하고 `REJECT` 로 기록한다
- `from=` 으로 ws01 에서 온 접속만 받는다
- 개인키는 Jenkins Credentials 에만 두고 서버의 파일은 지운다

### 9-2. wb01 준비 (스크립트 설치)

내부 PC: FileZilla 로 **wb01** 에 atmswas 로 접속 → 배포킷의 **`deploy` 폴더**를 `/home/atmswas/deploy/kit/` 아래에 올린다 (결과: `/home/atmswas/deploy/kit/deploy/webtob/…`). `~/docs` 아래에는 절대 두지 않는다.

wb01 (atmswas):
```bash
ls -la ~
ls -ld ~ ~/docs
df -h /home/atmswas
sed -i 's/\r$//' ~/deploy/kit/deploy/webtob/install-wb01.sh
bash ~/deploy/kit/deploy/webtob/install-wb01.sh
ls -l ~/deploy/bin
```
- 정상:
  - `설치: /home/atmswas/deploy/bin/deploy-front.sh`, `설치: …/swap-deploy.sh`
  - `명령 확인: flock curl sha256sum tar`, `완료.`
  - `ls` 에 두 스크립트가 `-rwx------`
- 스크립트가 하는 일:
  - `~/deploy/{bin,inbox,backup}` 700, 스크립트 설치 (CRLF 제거·문법 검사)
  - 홈 `jeus` 750, `~/docs` `jeus` 2750 — WebtoB 의 webatms 가 jeus 그룹으로 읽음
- 다시 실행해도 안전하다 (스크립트만 새로 덮어씀)

교체 연습 — 지금 화면을 그대로 다시 배포해 교체·확인·백업 동작만 검증한다. `~/docs` 에 화면이 이미 있을 때만 한다. 비어 있는 새 서버는 첫 Jenkins 배포로 채운다.

wb01 (atmswas):
```bash
tar -C ~/docs -czf ~/front-test.tar.gz .
SHA=$(sha256sum ~/front-test.tar.gz | cut -d' ' -f1)
~/deploy/bin/deploy-front.sh deploy 20261021-0 $SHA < ~/front-test.tar.gz
~/deploy/bin/deploy-front.sh status
ls -ld ~/docs
rm ~/front-test.tar.gz
```
- 정상: `확인: / → 200, index.html 일치 예`, `배포 완료: 20261021-0`, `ls -ld` 가 `drwxr-s--- atmswas jeus`
- 이상하면 `~/deploy/bin/deploy-front.sh rollback`

### 9-3. wb01 배포 키

ws01 (atmswas) — 키 만들기 (`ssh-keygen` 은 HIWARE 허용):
```bash
mkdir -p ~/atms/infra/jenkins/keys
chmod 700 ~/atms/infra/jenkins/keys
ssh-keygen -t ed25519 -N "" -C jenkins-wb01-deploy -f ~/atms/infra/jenkins/keys/wb01-deploy
ls -l ~/atms/infra/jenkins/keys
ip route get 16.88.122.112
```
- `ls` → `wb01-deploy`(개인키, 600)·`wb01-deploy.pub`(공개키)
- `ip route get` 의 `src 16.88.122.113` → wb01 이 보는 출발 IP. 다르면 다음 단계에서 그 IP 를 둘째 인자로 준다

내부 PC: FileZilla 로 ws01 의 `wb01-deploy.pub` **(공개키만)** 를 받아 wb01 `/home/atmswas/deploy/` 에 올린다. 개인키는 옮기지 않는다.

wb01 (atmswas):
```bash
bash ~/deploy/kit/deploy/webtob/install-wb01.sh ~/deploy/wb01-deploy.pub
cat ~/.ssh/authorized_keys
rm ~/deploy/wb01-deploy.pub
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```
- `cat` 에 한 줄: `restrict,from="16.88.122.113",command="/home/atmswas/deploy/bin/deploy-front.sh" ssh-ed25519 AAAA… jenkins-wb01-deploy`
- 스크립트 출력의 `ls -Z` 가 `ssh_home_t` 여야 한다 (아니면 `restorecon -Rv ~/.ssh` — SELinux 라벨이 틀리면 키 인증 실패)
- 마지막 줄의 `SHA256:…` (wb01 호스트 키 지문)을 적어 둔다 → 9-5 에서 대조

### 9-4. ws01 배포 키

ws01 (atmswas):
```bash
ssh-keygen -t ed25519 -N "" -C jenkins-ws01-deploy -f ~/atms/infra/jenkins/keys/ws01-deploy
~/atms/deploy/jeus/install-deploy-war.sh ~/atms/infra/jenkins/keys/ws01-deploy.pub 16.88.122.113
grep jenkins-ws01-deploy ~/.ssh/authorized_keys
```
- 둘째 인자 `16.88.122.113` = Jenkins 컨테이너의 접속이 ws01 sshd 에 보이는 출발 IP [확인 2026-10-23]
  - 새 환경에서 모르면 인자 없이 등록 → 9-8 의 `호출 : ssh:<IP>` 를 보고 다시 실행
- 정상 출력:
  - `스크립트·명령 확인 완료`
  - (처음이면) `생성: /home/atmswas/deploy/deploy-war.env …`, `재기동 메뉴 설정:`
  - `authorized_keys 등록` → `restrict,from="16.88.122.113",command="/home/atmswas/atms/deploy/jeus/deploy-war.sh" ssh-ed25519 AAAA… jenkins-ws01-deploy`, 이어서 `ssh_home_t` 라벨
- **`ssh -i …` 로 시험하지 않는다** (HIWARE 금지명령, T01). 키 동작은 9-8 의 Jenkins `MODE=status` 로 확인한다

### 9-5. Jenkins 컨테이너의 known_hosts (서버 확인)

Jenkins 는 `StrictHostKeyChecking=yes` 로 접속한다. 그래서 미리 등록한 호스트 키와 다르면 접속을 거부한다 — 다른 서버로 잘못 붙는 것을 막기 위해서다.

ws01 (atmswas):
```bash
podman exec atms-jenkins getent hosts host.containers.internal
podman exec atms-jenkins bash -c "timeout 5 bash -c '</dev/tcp/host.containers.internal/22' && echo OPEN || echo FAIL"
podman exec atms-jenkins sh -c "mkdir -p ~/.ssh && chmod 700 ~/.ssh && ssh-keyscan -t ed25519 16.88.122.112 >> ~/.ssh/known_hosts"
podman exec atms-jenkins sh -c "ssh-keyscan -t ed25519 host.containers.internal >> ~/.ssh/known_hosts"
podman exec atms-jenkins ssh-keygen -lf /var/jenkins_home/.ssh/known_hosts
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```
- 둘째 줄 `OPEN` → Jenkinsfile 기본 주소(`atmswas@host.containers.internal`) 그대로 사용 (`FAIL` 이면 T33)
- 다섯째 줄의 `16.88.122.112` 지문 = 9-3 의 wb01 지문, `host.containers.internal` 지문 = 마지막 줄(ws01 호스트 키)
- `/var/jenkins_home` 은 데이터 폴더라 컨테이너를 다시 만들어도 남는다

### 9-6. Credentials 3개 등록 (PC 브라우저, Jenkins `admin`)

Jenkins 관리 → Credentials → System → Global credentials → **Add Credentials**

| 항목 | `gitea-jenkins-bot` | `wb01-deploy-key` | `ws01-deploy-key` |
|---|---|---|---|
| Kind | **Username with password** | SSH Username with private key | SSH Username with private key |
| Scope | Global | Global | Global |
| Username | `jenkins-bot` | `atmswas` | `atmswas` |
| Password / Private Key | jenkins-bot 토큰 (7-6) | "Enter directly" → `wb01-deploy` 개인키 전체 (`-----BEGIN` ~ `END-----`) | "Enter directly" → `ws01-deploy` 개인키 전체 |
| ID | `gitea-jenkins-bot` | `wb01-deploy-key` | `ws01-deploy-key` |
| Description | Gitea 읽기 | wb01 화면 배포 | ws01 WAR 배포 |

- 종류 목록에 "Gitea Personal Access Token" 이 있지만 **고르지 않는다**. 그것은 Gitea 플러그인용이라 Jenkinsfile 의 소스 받기가 쓰지 못한다 (2026-10-23 실수 사례, T16)
- 개인키 내용은 ws01 에서 `cat ~/atms/infra/jenkins/keys/wb01-deploy` 한 것을 터미널에서 복사해 붙여 넣는다
  - 복사가 안 되면 FileZilla 로 PC 에 받아 메모장으로 붙여 넣고 PC 파일은 바로 지운다
  - **사진으로 옮겨 치지 않는다** (개인키가 사진에 남음)

등록을 확인했으면 ws01 의 개인키 파일을 지운다 (공개키 `.pub` 는 다시 등록할 때 쓰므로 남긴다). ws01 (atmswas):
```bash
rm ~/atms/infra/jenkins/keys/wb01-deploy ~/atms/infra/jenkins/keys/ws01-deploy
ls -l ~/atms/infra/jenkins/keys
```

### 9-7. 잡 2개 (처음 만들 때 + Jenkinsfile 이 바뀔 때마다)

대시보드 → 새로운 Item:

| 항목 | 화면 잡 | 백엔드 잡 |
|---|---|---|
| 이름 | `nh-atms-frontend` | `nh-atms-backend` |
| 종류 | Pipeline | Pipeline |
| Pipeline → Definition | **Pipeline script** | **Pipeline script** |
| Script 칸 | `deploy/jenkins/pipelines/frontend.Jenkinsfile` 전체 | `deploy/jenkins/pipelines/backend.Jenkinsfile` 전체 |
| Use Groovy Sandbox | 체크 그대로 | 체크 그대로 |
| 그 밖의 칸 (Build Triggers 등) | 비워 둔다 (트리거·매개변수는 스크립트 안에 있음) | 같음 |

- 붙여 넣을 파일은 서버 `~/atms/deploy/jenkins/pipelines/` 또는 GitHub `main` 에서 연다
- 처음에는 "Build Now" 만 보인다 → 한 번 실행하면 매개변수·트리거가 등록되고 "Build with Parameters" 로 바뀐다
  - 화면 잡의 기본 MODE 는 `build-deploy` 라 첫 실행이 바로 wb01 배포까지 한다 → 9-2~9-6 을 먼저 끝낸다
  - 백엔드 잡의 기본 MODE 는 `build-only` (배포 안 함)
- "Scripts not permitted…" 가 뜨면 Jenkins 관리 → In-process Script Approval → Approve
- **Jenkinsfile 을 바꿨을 때**(배포킷 갱신): 잡 → 구성 → Script 칸 전체를 새 내용으로 바꿔 저장 → `MODE=status` 로 한 번 실행 (새 매개변수 등록)

### 9-8. 키 동작 확인과 출발 IP 고정

1. 화면 잡 → Build with Parameters → `MODE=status`
   - 콘솔에 `DocBase : /home/atmswas/docs`, `현재 : …`, `확인: / → 200, index.html 일치 예`
   - `Finished: SUCCESS`
2. 백엔드 잡 → `MODE=status`
   - 콘솔에 `호출    : ssh:16.88.122.113`, `JEUS    : 16.88.122.113 (모듈별 서버·포트)`, 모듈 6줄
   - `Finished: SUCCESS`
3. `호출` 의 IP 가 9-4 에 넣은 값과 다르면 ws01 에서 그 IP 로 다시 등록한다 (같은 키 줄을 바꿔 씀). ws01 (atmswas):
   ```bash
   ~/atms/deploy/jeus/install-deploy-war.sh ~/atms/infra/jenkins/keys/ws01-deploy.pub <호출 IP>
   ```

### 9-9. 웹훅 (push → 자동 빌드)

① Jenkins notifyCommit 토큰 — Jenkins 관리 → Security → 아래쪽 "Git plugin notifyCommit access tokens" → Add new access token
- 이름 `gitea-webhook` → 값 복사 (한 번만 보임) → 화면 아래 **Save**

② Gitea 웹훅 — 저장소 `NH-ATMS/nh-atms` → 설정 → 웹훅 → 웹훅 추가 → **Gitea**

| 항목 | 값 |
|---|---|
| 대상 URL | `http://atms-jenkins:8080/git/notifyCommit?url=http://atms-gitea:3000/NH-ATMS/nh-atms.git&token=<①의 토큰>` |
| HTTP 방법 | **POST** (GET 은 `414 URI Too Long`, T12) |
| POST 콘텐츠 유형 | `application/json` |
| 비밀 | 비움 |
| 트리거 | 푸시 이벤트 |
| 브랜치 필터 | `main` |
| 활성 | 체크 |

- `url=` 값은 두 Jenkinsfile 의 `GITEA_REPO` 와 글자까지 같아야 한다 (Jenkins 가 이 값으로 잡을 찾음)
- 대상이 컨테이너 이름(사설 IP 10.89.x)이라 Gitea 의 `ALLOWED_HOST_LIST=private` 이 필요하다 — `gitea.env` 에 이미 있다

③ 시험
- "테스트 전송" → 최근 전송: 응답 **200**, 본문에 `Scheduled polling of nh-atms-frontend`, `Scheduled polling of nh-atms-backend`
- 실제 확인:
  - `Frontend/` 파일 하나만 고쳐 push → 화면 잡만 빌드·배포
  - `Backend/` 만 고쳐 push → 백엔드 잡만 `build-only`
  - 다른 잡의 "Polling Log" 는 변경 없음

- [ ] 9장 끝: wb01 스크립트, 키 2개 등록·개인키 삭제, known_hosts 지문 일치, Credentials 3개, 잡 2개, 두 잡 `status` SUCCESS, 웹훅 200

### 9-10. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| 웹훅 `414 URI Too Long` / `No git jobs using repository` / `403` / `not allowed` | T12 / T13 / T14 / T15 |
| 잡 소스 단계 `Authentication failed` | T16 |
| `Host key verification failed` | T31 |
| `Permission denied (publickey)` | T32 |
| `Connection refused` / `timed out` (SSH) | T33 |

---

## 10. JEUS 구성 확인과 연결 (ws01 — WAS 담당 셋팅 후)

WAS 담당이 서버 6개를 만들었다고 회신하면, 우리 쪽은 **읽기만 해서** 확인하고 배포 설정을 맞춘다. JEUS 설정(`domain.xml`)은 고치지 않는다.

### 10-1. 구성 확인 (읽기만)

ws01 (atmswas) — `★` 2호기는 IP 를 ws02 로:
```bash
D=/JEUS/jeusATMS/domains/ATMS
ls -l --time-style=long-iso $D/config/domain.xml
grep -A3 '<server>' $D/config/domain.xml | grep -o '<name>[^<]*'
grep -o '<listen-port>[0-9]*' $D/config/domain.xml | sort | uniq -c
grep -n -e '/home/atmswas' -e 'context-path' $D/config/domain.xml
grep -n 'registration-id' $D/config/domain.xml
grep -n -i -A12 'dsATMS01' $D/config/domain.xml | grep -i -E 'dsATMS01|url|server-name|port-number|database-name|user|class|target|min|max'
ps -u jeusatms -o args= | grep -o 'obj_name=[^ ]*' | sort
ss -ltn | grep -E ':(808[0-5]|21001|8766|8736) '
for p in 8080 8081 8082 8083 8084 8085; do echo "$p $(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://16.88.122.113:$p/v3/api-docs)"; done
grep -n -E 'boot|down' /JEUS/jeusATMS/jeusMENU/jeusmenu.c
printf '0\n' | /JEUS/jeusATMS/jeusMENU/jeusRstart
```
- 서버 이름은 `grep -w` 로 세지 않는다. 담당이 `nbatmsltws01_ajou01` 처럼 접두어를 붙였으면 `-w` 가 찾지 못한다 (2026-10-30 확인 명령의 실수)

| 확인 | 정상 | 아니면 |
|---|---|---|
| `domain.xml` 시각 | 담당 작업일 | — |
| 서버 이름 | `adminServer`(DAS) 외에 `atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01` (정리 전이면 `nbatmsltws01_atms01` 도) | 이름이 다르면 그 이름을 1-2 값표, `deploy-war.env` 의 `SERVER_*`, `http.m` 에 쓴다 |
| `listen-port` | 8080~8085 각 1번 (+ DAS 8736, 정리 전이면 8766) | 포트가 다르면 `deploy-war.env` 의 `PORT_*` 와 `jeus-common.yml` 의 `jeus.url.*` 를 맞춘다 |
| 앱 경로 | `/home/atmswas/catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex`, `context-path` `/` | 옛 경로(`catms-journal` 등)면 담당에 수정 요청 (10-2 와 맞춤) |
| `registration-id` | 서버마다 1개 | 값을 1-2 값표에 적는다 → `http.m` `*SERVER` 이름 (11장) |
| `dsATMS01` | 데이터소스가 있고 Oracle 설정(16.88.250.91·1531·NBATMSTP·ATMS_APP), 대상에 서버 6개 | 없거나 Tibero·대상 누락이면 WAS 담당 (WAS 요청서 2-8, T63·T64). 이름이 다르면 `jeus-common.yml` `jndi-name` |
| `obj_name` | 6개 | 없으면 생성만 하고 기동 안 한 것 → 담당 |
| `ss -ltn` | `16.88.122.113:8080` ~ `:8085` LISTEN | `127.0.0.1` 에만 있으면 모듈 간 호출 실패 → 담당 (리스너 주소) |
| `curl` 6줄 | 앱 배포 전 `404` (서버는 떠 있음), 배포 후 `200` | `000` = 그 서버가 안 떠 있음 |
| `jeusmenu.c` | 새 서버 6개의 boot·down 줄 | 없으면 메뉴 미추가 → 재기동은 담당이 (UNSTABLE, T29) |
| `printf '0\n' …` | 메뉴 목록 → `Promgram Exit` → 바로 프롬프트 | 메뉴가 반복 대기하면 T56 |

### 10-2. 앱 폴더 이름 정리 (1호기 한 번만)

예전 구성(서버 1개 + 컨텍스트 경로)의 폴더 이름을 새 이름으로 바꾼다. **WAS 담당이 새 경로로 앱을 등록하기 전에** 한다. 6-3 을 새로 한 서버는 이미 새 이름이라 건너뛴다.

ws01 (atmswas):
```bash
ls -ld ~/catms* ~/cajou ~/cabat ~/cfile ~/cifin ~/cifex
mv ~/catms-journal ~/cajou
mv ~/catms-batch ~/cabat
mv ~/catms-file ~/cfile
mv ~/catms-ifin ~/cifin
mv ~/catms-ifex ~/cifex
ls -ld ~/catms ~/cajou ~/cabat ~/cfile ~/cifin ~/cifex
```
- 첫 줄에서 새 이름(`cajou` 등)이 **이미 있으면** `mv` 가 그 안으로 들어가 버린다
  - 비어 있으면 `rmdir ~/cajou` 후 `mv`
  - 비어 있지 않으면 멈추고 확인한다
- `mv` 는 그룹(jeus)·권한(2750)을 그대로 유지한다. `catms` 는 이름이 같아 그대로 둔다
- 예전 백업(`~/deploy/backup/catms-journal-*`)은 새 이름의 롤백 대상이 아니다 → 필요 없으면 지운다

### 10-3. 서버별 재기동 설정 (`~/deploy/deploy-war.env`)

`deploy-war.sh restart <모듈>` 은 그 모듈의 서버만 다시 띄운다. 순서는 정지 메뉴 → 포트가 닫힐 때까지 대기 → 기동 메뉴 → `/v3/api-docs` 200 대기다. 메뉴 번호는 jeusRstart(root setuid 메뉴 프로그램)의 번호이고, WAS 담당이 정해 준다.

ws01 (atmswas) — 예전 단일 서버 설정(`JEUS_URL`·`JEUS_STOP_MENU`)이 있을 때만 첫 두 줄:
```bash
cat ~/deploy/deploy-war.env
mv ~/deploy/deploy-war.env ~/deploy/deploy-war.env.old-$(date +%Y%m%d)
~/atms/deploy/jeus/install-deploy-war.sh
vi ~/deploy/deploy-war.env
~/atms/deploy/jeus/deploy-war.sh status
```
`vi` 에서 WAS 담당 회신대로 채운다 (`=` 앞뒤 공백 없이):
```
STOP_MENU_web=<atms01 down 번호>
BOOT_MENU_web=<atms01 boot 번호>
STOP_MENU_journal=<ajou01 down>
BOOT_MENU_journal=<ajou01 boot>
… (batch·file·ifin·ifex 같은 방식)
```
- 넣기 전에 번호가 맞는지 소스로 확인한다: `grep -n -A3 'case <번호>' /JEUS/jeusATMS/jeusMENU/jeusmenu.c` → 그 서버의 `down`/`boot` 여야 한다
- 3·7·8 은 넣어도 스크립트가 거부한다
- 서버 이름이 다르면 `#SERVER_web=atms01` 같은 주석을 풀어 실제 이름으로 바꾼다 (status 표시용)
- DB 가 준비되기 전에 batch 를 빼려면 `HEALTH_SKIP="atms-batch"` (batch 는 DB 없으면 기동 실패, T24)
- `★` 2호기: `JEUS_HOST=<ws02 IP>`, `SERVER_web=atms02` …, ws02 메뉴 번호 (14-4)

`status` 정상 예:
```
호출    : local
JEUS    : 16.88.122.113 (모듈별 서버·포트)
--- atms-web  atms01 http://16.88.122.113:8080  (응답 404, 재기동 메뉴 <down>/<boot>, 최근 수신 …)
--- atms-journal  ajou01 http://16.88.122.113:8081  (응답 404, 재기동 메뉴 <down>/<boot>, …)
…
```
→ 앱 배포 전 `404`, 배포 후 `200`. `재기동 메뉴 미설정` 이 남은 모듈은 배포해도 UNSTABLE (T29)

### 10-4. 앱 로그 폴더

ws01 (atmswas):
```bash
ls -ld /WASlogs/jeusATMSlogs/nbatmsltws01/app
```
- 정상: 있고 jeusatms 가 쓸 수 있음 (예: `drwxr-s--- jeusatms jeus`). 배포 뒤 `<모듈>.log` 6개가 생긴다
- 없으면: WAS 담당에 생성 요청 (jeusatms 로 만들면 됨), 또는 root 날 6-2 R4
- 없어도 앱은 돈다. 다만 파일 로그가 남지 않는다 (T54)

### 10-5. Jakarta 지원 판정

Spring Boot 3.5 는 `jakarta.servlet` 세대 WAS 가 필요하다. TmaxSoft 문서상 JEUS 9 는 Jakarta EE 9 인증이다. 실제 판정은 **첫 배포 결과**로 한다 (13-3).
- 정상: 각 앱 로그에 `Started Atms…Application` 이 있고 `/v3/api-docs` 가 200 → 지원
- 이상: Spring 로그가 전혀 없고 모든 경로가 JEUS 404 → 미지원 또는 앱 미등록 (T26)
  - JEUS 라이브러리 검사는 root 가 필요하다: [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2-14 J1 — WAS 담당에 요청

- [ ] 10장 끝: 서버 6개·포트·앱 경로·registration-id 확인, 폴더 이름 정리, `deploy-war.env` 메뉴, 로그 폴더

### 10-6. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| 모든 경로 JEUS 404, Spring 로그 없음 | T26 |
| `stderr` 에 `[WebT]:webtCPInfo is null` + NPE 매초 | T27 |
| `status` 응답 `000` | T28 |
| `/JEUS` 가 비어 있음 | T53 |
| 앱 로그 파일이 안 생김 | T54 |
| 정지 확인 실패 / jeusRstart 가 대기 | T55 / T56 |

---

## 11. WebtoB 연동 — `http.m` (wb01)

**왜 필요한가.** 지금 wb01:9000 은 화면(정적 파일)만 준다. `/api/` 는 `302 /error/error.html` 이 된다 [확인 2026-10-21]. `http.m` 에 경로 규칙을 넣어야 API 가 JEUS 서버 6개로 간다.
- 설정 폴더(`/WEBTOB/webtobATMS/config`)는 webatms 700 이라 atmswas 는 읽지도 못한다
- 그래서 WebtoB 담당이 하거나, root 작업일에 우리가 한다 (2026-10-23 결정: 재기동까지 우리)

### 11-1. 무엇을 바꾸나

붙여 넣을 원본: 배포킷 `deploy/webtob/http.m.atms-jsv.txt`. 원칙은 **기존 항목은 지우거나 바꾸지 않고 없는 것만 추가**한다.

| 절 | 내용 |
|---|---|
| `*NODE` | 확인만: 노드 이름 `nbatmsltwb01`, `DOCROOT = "/home/atmswas/docs"`(바꾸지 않음), `PORT = "9000"`, `JSVPORT = 19000` (없으면 추가) |
| `*SVRGROUP` | `atms_jsvg  NodeName = "nbatmsltwb01", SvrType = JSV` (JSV 그룹이 이미 있으면 그 이름을 씀) |
| `*SERVER` | 6개 — 이름 = JEUS 커넥터 **registration-id** (10-1 에서 확인한 값). 다르면 연결 안 됨 |
| `*URI` | 13개, **구체적인 경로를 위에** (`/api/journal/` 이 `/api/` 보다 먼저) |

`*URI` 규칙 (요약):

| 경로 | 보낼 서버 |
|---|---|
| `/api/journal/`, `/api/agent/` | ajou01 |
| `/api/batch/` | abat01 |
| `/api/file/`, `/atmf/` | file01 |
| `/api/if/internal/` | ifin01 |
| `/api/if/external/` | ifex01 |
| `/api/` (나머지), `/atms/`, `/atmr/`, `/swagger-ui/`, `/v3/api-docs`, `/actuator/` | atms01 |

- 에이전트는 L4 로 ws01 에 직접 붙으므로, `/api/agent/`·`/atmf/`·`/atms/`·`/atmr/` URI 는 꼭 필요하지는 않다 (있어도 무해, 결정 문서 §11)
- `MinProc`·`MaxProc` 는 예시값 — WebtoB 담당 기준을 따른다

### 11-2. 적용

**(가) WebtoB 담당에게 맡길 때** — 다음 세 가지를 보낸다:
- [WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md) 3장 (수정 가이드)
- `http.m.atms-jsv.txt`
- 10-1 에서 확인한 registration-id 6개

**(나) root 작업일에 우리가 할 때 (wb01 root)** — WebtoB 담당에 작업 시간을 알리고 승인 기록을 남긴다.
- 9000 이 잠깐 끊긴다
- WebtoB 명령은 반드시 **webatms 로** 실행한다 (`runuser -l webatms -c '…'`). root 로 기동하면 프로세스·파일이 root 소유가 되어 이후 webatms 가 다루지 못한다
- `runuser` 가 HIWARE 금지명령인지는 [확인 필요] → 첫 줄(무해한 명령)로 먼저 시험하고, 막히면 여기서 멈추고 (가)로 넘긴다

```bash
runuser -l webatms -c 'id; echo $WEBTOBDIR'
cd /WEBTOB/webtobATMS/config
cp -p http.m http.m.bak-$(date +%Y%m%d%H%M%S)
tar -C /WEBTOB/webtobATMS -czf /root/webtob-config-$(date +%Y%m%d%H%M%S).tar.gz config
grep -n -E "^\*|JSVPORT|DOCROOT|SvrType|SVRTYPE|SvgName|SVGNAME|Uri|URI" http.m
vi http.m
diff http.m.bak-* http.m
runuser -l webatms -c 'cd /WEBTOB/webtobATMS/config && wscfl -i http.m'
runuser -l webatms -c 'wsdown -i'
runuser -l webatms -c 'wsboot'
ps -u webatms -o pid,lstart,args | grep -E "wsm|htl|hth" | grep -v grep
ss -ltn | grep -E ":9000|:19000"
```
- `id` 결과가 `uid=…(webatms)` 와 `/WEBTOB/webtobATMS` 가 아니면 중단
- `grep` 결과를 사진으로 남긴다 (기존 JSV 그룹·서버가 있는지 판단)
- `vi` 에서는 11-1 의 블록만 추가한다
- `diff` 로 추가한 줄만 바뀌었는지 확인한다 (백업이 둘 이상이면 시각을 직접 적는다)
- `wscfl` 은 실행 중인 WebtoB 에 영향이 없다. 오류가 나면 표시된 줄을 고친다
- `wsdown -i` → `wsboot` 는 약속한 시간에 한다 → 9000·19000 LISTEN

### 11-3. 확인 (wb01, atmswas 도 가능)

```bash
ss -tn | grep ':19000'
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9000/
for u in /v3/api-docs /api/ /api/journal/ /api/batch/ /api/file/ /api/if/internal/ /api/if/external/; do echo "$u $(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:9000$u)"; done
```

| 결과 | 판정 |
|---|---|
| 19000 에 `16.88.122.113` 에서 온 ESTAB 이 여러 줄 (JEUS 가 다시 붙는 데 수 초~1분) | JEUS 쪽 연결됨. 서버당 연결 수는 커넥터 설정에 따라 달라 줄 수로는 6개 모두인지 알 수 없다 → 아래 경로별 확인 |
| `/` → `200` | 화면 정상 |
| `/v3/api-docs` → `200` | atms01 까지 연결 (앱 배포 후) |
| 나머지 6개 경로 → `302` 도 `503` 도 아님 (`401`·`403`·`404`·`405` 등) | 각 서버까지 연결 — 그 응답은 Spring 이 준 것 |
| `302 …/error/error.html` | URI 미적용 (T38) |
| `503` | 그 경로의 서버가 WebtoB 에 연결되지 않음 (T39) |

### 11-4. 되돌리기 (11-3 이 실패하면 바로)

wb01 (root):
```bash
cd /WEBTOB/webtobATMS/config
cp -p http.m.bak-<시각> http.m
runuser -l webatms -c 'cd /WEBTOB/webtobATMS/config && wscfl -i http.m'
runuser -l webatms -c 'wsdown -i'
runuser -l webatms -c 'wsboot'
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9000/
```
- 그래도 안 뜨면 `/root/webtob-config-*.tar.gz` 로 `config` 폴더 전체를 되돌린 뒤 같은 순서로 한다

- [ ] 11장 끝: 19000 ESTAB, `/` 200, `/v3/api-docs` 200, 모듈 경로 6개가 302·503 이 아님

### 11-5. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| `/api/` 가 여전히 `302 /error/error.html` | T38 |
| 19000 ESTAB 이 없거나 일부만 / `503` | T39 |
| 화면 하위 메뉴에서 F5 → 404 | T40 |
| `/swagger-ui.html` 이 9000 에서 안 열림 | T41 |
| `wsboot` 뒤 9000 이 응답 없음 | T60 |

---

## 12. 소스 설정 — 내부 Gitea 사본의 jeus 프로파일 (내부 PC)

### 12-1. 설정 구조

- WAR 는 `bootWar -Pjeus` 로 만든다. `WEB-INF/web.xml` 이 `spring.profiles.active=jeus` 를 지정한다
  - JEUS JVM 옵션에 `-Dspring.profiles.active` 를 넣지 않는다 (T36)
- 모듈마다 `application-jeus.yml` 이 `classpath:jeus-common.yml`(atms-common 안)을 불러온다
- **우선순위**: `-D` JVM 옵션·OS 환경변수 > `jeus-common.yml` > `application-jeus.yml` > `application.yml`
- 설정은 WAR 안에 들어간다 (2026-10-12 결정). 그래서 실제 값은 **내부 Gitea 사본에만** 넣는다
- 서버마다 달라야 하는 값(2호기의 `jeus.host`·`log-dir`)은 그 서버의 JVM 옵션 `-D` 로 덮는다 — JVM 이 서버마다 따로라 그 서버에만 적용된다 (14-6)

| 키 (`jeus-common.yml`) | 값 | 쓰는 곳 |
|---|---|---|
| `jeus.host` | `16.88.122.113` | 모듈 간 호출 주소, file01 의 21001 bind 주소 |
| `jeus.url.web` … `ifex` | `http://${jeus.host}:8080` ~ `8085` | web→journal·file, file→web, journal→ifin·ifex |
| `jeus.log-dir` | `/WASlogs/jeusATMSlogs/nbatmsltws01/app` | 모듈별 `<모듈>.log` |
| `jeus.data-dir` | `/home/atmswas/data` | 업로드·이미지·다운로드·`atms-repo` |
| `jeus.agent-entry.*` | `host: CHANGE_ME_L4_VIP`, `journal-port: 8081`, `file-port: 8083`, `schema: http` | 에이전트 open 응답 (journal) |
| `spring.datasource.jndi-name` | `dsATMS01` (2026-11-02) | 6개 모듈 DB 접속 — JEUS 커넥션 풀을 찾아 씀 (주소·계정·비밀번호·풀 크기는 JEUS 데이터소스 설정, WAR 에는 없음) |
| `jwt.secret` | `CHANGE_ME…` | 토큰 서명·검증 (6개 모듈 같은 값) |
| `cors.allowed-origins` | `http://CHANGE_ME_WEBTOB_HOST:9000` | 직접 접근 시 CORS |

### 12-2. 반영 가이드 적용

배포킷 `docs/NH-ATMS_LOCAL_APPLY_GUIDE.md` 를 내부 PC 소스(`NH-ATMS\Backend\nh-atms-api`)에 옮긴다. 이 가이드는 저장소의 실제 파일에서 뽑아 만든 것이다.

| 상황 | 할 일 |
|---|---|
| 업체 원본 그대로 | 가이드 전체 (24개 파일) + 개정 2026-10-26 (`atms-common/build.gradle` 의 orai18n 1줄) |
| 2026-10-27 이전에 반영함 | 가이드 9~15단계 **7개 파일**을 가이드 내용 전체로 교체 (`jeus-common.yml`, `application-jeus.yml` ×6) — Java·빌드 파일 변경 없음 |
| 교체한 뒤 | 12-3 의 실제 값을 **다시** 넣는다 (교체로 `CHANGE_ME` 로 돌아감) |

### 12-3. `CHANGE_ME` 기입 (`atms-common\src\main\resources\jeus-common.yml`)

| 키 | 넣을 값 | 비고 |
|---|---|---|
| `jwt.secret` | 아래 명령으로 만든 임의 문자열 (32바이트 이상) | 비밀. 바꾸면 이미 발급된 로그인 토큰이 모두 무효 |
| `cors.allowed-origins` | `"http://16.88.122.112:9000"` | 화면용 L4 를 두면 그 주소도 |
| `jeus.agent-entry.host` | L4 VIP (받기 전에는 `CHANGE_ME_L4_VIP` 그대로) | 13-5 |

- **DB 는 이 파일에 넣지 않는다** (2026-11-02 부터 JNDI `dsATMS01`). JNDI 이름이 담당 회신과 다르면 `spring.datasource.jndi-name` 한 줄만 바꾼다
- 로컬(PC) DB 는 `atms-common` 의 `local-common.yml` 한 곳에 적는다 (12-4)
- 6개 모듈이 같은 `jeus-common.yml` 을 쓰므로 JWT 값이 자동으로 같아진다

JWT 시크릿 만들기 — 내부 PC (PowerShell, 결과를 복사해 붙여 넣고 **사진에 남기지 않는다**):
```powershell
$b = New-Object byte[] 48; [System.Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

### 12-4. 로컬(PC) DB 설정 — `local-common.yml` (2026-11-02)

로컬 실행(`bootRun`·IntelliJ, 프로파일 `local`)은 JEUS 가 없으니 DB 에 직접 접속한다. 계정·비밀번호는 **한 곳**에만 둔다:
- `atms-common\src\main\resources\local-common.yml` — `url: jdbc:oracle:thin:@//16.88.250.91:1531/NBATMSTP`, `username: ATMS_APP`, `password: <실제 비밀번호>`
- 6개 모듈의 `application-local.yml` 이 `spring.config.import: classpath:local-common.yml` 로 불러온다
- `-Pjeus` WAR 에는 `local-common.yml`·`application-local.yml` 이 들어가지 않는다 (빌드 설정) → 서버 WAR 에 로컬 비밀번호가 남지 않음
- 반영 방법: 반영 가이드 9-1~9-3. 예전에 `application.yml` 등에 직접 넣은 DB 값은 원래대로 되돌린다 (값이 여러 곳이면 어느 것이 쓰이는지 헷갈림)

### 12-5. push → 빌드 확인

내부 PC (PowerShell):
```powershell
cd D:\NH-ATMS
git status
git add Backend
git commit -m "chore(jeus): 매니지드 서버 6개 구성 설정 반영"
git push
```
- 실제 값이 든 `jeus-common.yml` 은 **내부 Gitea 에만** 있다. GitHub 등 외부 원격으로는 절대 push 하지 않는다
- 웹훅으로 `nh-atms-backend` 가 `build-only` 로 돈다
  - 콘솔 끝: `BUILD SUCCESSFUL` → `Finished: SUCCESS`
  - 빌드 화면 "Build Artifacts": `atms-<모듈>-0.0.1-<YYYYMMDD>-<빌드번호>.war` 6개 + `.sha256` 6개 + `VERSION` + `COMMIT`

- [ ] 12장 끝: 반영 가이드 최신(9~15·9-1~9-3), `CHANGE_ME` 기입(JWT·cors, 로컬 `local-common.yml` 비밀번호), push, `build-only` 성공

### 12-6. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| `Could not resolve …` / `No cached version of … orai18n … offline` | T19 |
| `Cannot find a Java installation … Eclipse Temurin` | T21 |
| 로그인 때 `ORA-17056` | T22 |
| 로컬에서 엉뚱한 DB 로 붙음 / 인증 실패 | T23 |
| 앱 기동 실패 `NameNotFoundException` (JNDI) / JNDI 는 찾았는데 접속 실패 | T63 / T64 |

---

## 13. 첫 배포와 검증

### 13-1. 배포 전 확인

- [ ] 10-1: 서버 6개·포트·앱 경로 확인
- [ ] 10-2: 폴더 이름 정리
- [ ] 10-3: 메뉴 번호 (없는 모듈은 교체만 되고 UNSTABLE → 담당이 수동 재기동)
- [ ] 10-4: 로그 폴더
- [ ] 12장: `build-only` 성공
- [ ] WAS 담당과 재기동 시간 약속 — 첫 배포는 업무시간 외 권장. 서버를 하나씩 내리므로 그 모듈의 기능이 1~2분씩 멈춘다

### 13-2. Jenkins 로 배포 (전체)

`nh-atms-backend` → Build with Parameters:
- `MODE=build-deploy`, `MODULE=all`, `RESTART` 체크(기본)
- 이미 빌드한 WAR 를 쓰려면 `MODE=redeploy`, `REDEPLOY_BUILD=<빌드 번호>`

콘솔 흐름 (정상):
```
=== atms-web-0.0.1-20261030-15.war
… 수신: atms-web-0.0.1-20261030-15.war (커밋 …) → /home/atmswas/atms/release/atms-web/…
… 수신 완료: … bytes, SHA256 일치
… 압축 해제: atms-web-0.0.1-20261030-15.war → /home/atmswas/catms.new-…
… 교체 완료: /home/atmswas/catms
… WAR 프로파일: jeus
   (6개 모듈 반복)
… 재기동: atms-web (atms01, http://16.88.122.113:8080)
… jeusRstart 메뉴 <down>
… 정지 확인: atms-web
… jeusRstart 메뉴 <boot>
… 기동 확인: atms-web
   (6개 서버를 하나씩 — 2코어라 한 서버가 확인된 뒤 다음 서버)
… 결과 — 정상: atms-web atms-journal … / 실패: 없음 / WAS 담당 재기동 필요: 없음
… 재기동 완료
Finished: SUCCESS
```

| 결과 | 뜻 | 할 일 |
|---|---|---|
| **SUCCESS** (초록) | 교체·재기동·기동 확인 모두 정상 | 13-3 |
| **UNSTABLE** (노랑) | 교체는 됐지만 메뉴가 없는 모듈은 재기동 안 함 (종료코드 4) | 콘솔의 `WAS 담당 재기동 필요:` 모듈을 담당이 재기동 → `MODE=status` (T29) |
| **FAILURE** (빨강) | 기동 확인 실패 → 그 모듈만 직전 백업으로 자동 롤백·재기동 | 그 모듈 로그 확인 (13-3, T30) |

### 13-3. ws01 에서 확인

ws01 (atmswas):
```bash
~/atms/deploy/jeus/deploy-war.sh status
for p in 8080 8081 8082 8083 8084 8085; do echo "$p $(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://16.88.122.113:$p/v3/api-docs)"; done
curl -s http://16.88.122.113:8080/actuator/health
ls -l /WASlogs/jeusATMSlogs/nbatmsltws01/app
grep -h -E "Started Atms|profile is active|APPLICATION FAILED" /WASlogs/jeusATMSlogs/nbatmsltws01/app/*.log | tail -20
ls /home/atmswas/cajou/WEB-INF/lib | grep -i -E "orai18n|ojdbc"
ss -ltn | grep ':21001 '
```

| 확인 | 정상 | 아니면 |
|---|---|---|
| `status` | 6개 모두 `응답 200` | 13-2 표·T28 |
| `curl` 6줄 | 모두 `200` | `000` 서버 안 뜸 / `404` 앱 미기동 (T26) / batch 만 실패면 DB (T24) |
| `actuator/health` | `{"status":"UP"}` (JNDI 데이터소스로 DB 연결 정상) | `{"status":"DOWN"}`(503) = DB 문제 → 로그의 `ORA-`·`NameNotFoundException`·`dsATMS01` (T22·T63) |
| 로그 파일 | `atms-web.log` 등 6개 | 없으면 10-4 (T54) |
| `grep` | 모듈마다 `Started Atms…Application`, `The following 1 profile is active: "jeus"` | `profile … "local"` 이면 T36, `APPLICATION FAILED` 면 바로 아래 `Caused by` 줄 |
| `WEB-INF/lib` | `ojdbc11-…jar`, `orai18n-23.7.0.25.01.jar` | orai18n 없으면 T22 |
| 21001 | `16.88.122.113:21001` LISTEN | 안 보이면 file01 로그 (T50) |

JEUS 쪽 서버 로그(엔진 로그)가 필요하면:
```bash
find /JEUS/jeusATMS/logs/ -maxdepth 3 -iname '*ajou01*'
```

### 13-4. WebtoB 경유·화면·DB (11장 이후)

wb01 (atmswas):
```bash
for u in /v3/api-docs /api/ /api/journal/ /api/batch/ /api/file/ /api/if/internal/ /api/if/external/; do echo "$u $(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:9000$u)"; done
curl -s -o /dev/null -w "%{http_code}\n" -X POST -H "Content-Type: application/json" -d "{}" http://127.0.0.1:9000/api/auth/login
```
- 정상:
  - 첫 줄 `/v3/api-docs 200`, 나머지 6줄은 `302`·`503` 이 아님 (11-3)
  - 마지막 줄 `200` 또는 `400` — 빈 로그인 요청이 atms01 까지 가서 입력 검사에 걸렸다는 뜻 (DB 조회 전이라 기록이 남지 않음)
- `302`·`503`·`404`(WebtoB 오류 페이지) 면 11장

PC 브라우저 — 확인 순서:
1. `http://16.88.122.112:9000` → 로그인 화면
2. 은행 DB 의 사용자 계정으로 로그인 → 메뉴 → 목록 화면에 데이터가 나오면 **DB 까지 정상**
3. F12 → Network: `/api/auth/login` 응답 200, `success: true`. 이어지는 `/api/…` 요청 200
4. Swagger: `http://16.88.122.112:9000/swagger-ui/index.html` (`/swagger-ui.html` 은 9000 에서 안 열림, T41)
5. 하위 화면에서 F5 → 404 는 알려진 문제 (T40)

### 13-5. 에이전트 연결 (L4 앞단 준비)

ws01 (atmswas) — 에이전트가 붙을 포트가 열렸는지:
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://16.88.122.113:8081/v3/api-docs
curl -s -o /dev/null -w "%{http_code}\n" http://16.88.122.113:8083/v3/api-docs
timeout 5 bash -c '</dev/tcp/16.88.122.113/21001' && echo SOCKET-OPEN
```
→ `200` / `200` / `SOCKET-OPEN`

인프라(L4·방화벽)에 요청할 내용:

| 항목 | 값 |
|---|---|
| VIP → 실서버 | VIP:8081 → 16.88.122.113:8081, VIP:8083 → :8083, VIP:21001 → :21001 (2호기면 ws02 도 추가) |
| 헬스체크 | 8081·8083: HTTP `GET /v3/api-docs` → 200 (또는 TCP 연결). 21001: TCP 연결 |
| 세션 유지 | 21001 (파일 송수신)은 같은 단말이 같은 서버로 — 출발지 IP 기준 (14-8 ④) |
| 방화벽 | ATM·STM·공과금수납기 대역 → VIP 8081·8083·21001, L4 → ws01(·ws02) 같은 포트 |
| 프로토콜 | http (개발서버). 운영 전 https 재검토 (17-7) |

- 헬스체크에 `/api/agent/ping` 은 쓰지 않는다. 지금 소스에는 이 경로가 없다 (T49)
- **base 리스너 노출**: 서버마다 base 리스너 = HTTP 리스너(포트 1개)라, 8081·8083 을 ATM 망에 열면 JEUS 내부·관리용 base 리스너도 함께 열린다 → 분리 여부는 WAS 담당 판단 (2026-10-27 사용자 결정)

VIP 를 받은 뒤:
1. 내부 Gitea `jeus-common.yml` 의 `jeus.agent-entry.host` 에 VIP 를 넣는다 → push
2. `MODE=build-deploy`, `MODULE=all` — `jeus-common.yml` 은 6개 WAR 에 각각 들어가므로 바뀌면 6개 모두 다시 배포한다
3. 에이전트 open 응답의 `atms.host`·`atmf.host` 가 VIP 로 나가는지 단말 시험으로 확인한다

**알려진 소스 결함 (T49, 개발 조치 필요)** — 에이전트 폴링 경로가 없다:
- 상황: 개국(open) 응답이 폴링 주소로 `/api/agent/ping` 을 알려 준다
- 원인: 업체 소스 `atms-journal/…/LegacyAgentController.java` 의 `ping` 메서드가 닫히지 않은 주석(`/** …`) 안에 들어가 있어 매핑되지 않는다
  - `/api/agent/ping`, `/atmr/agent/ping.do` 둘 다 없음
- 영향: 실기기를 붙이면 폴링이 404 가 되어 마지막 폴링 시각이 갱신되지 않는다
- 조치: 실기기 연동 전에 개발 쪽에서 주석을 닫고 매핑을 추가한다 (이 배포킷 범위 밖)

### 13-6. 평소 배포 (모듈 하나 / 화면)

| 할 일 | Jenkins | ws01 에서 직접 (atmswas) |
|---|---|---|
| 한 모듈만 배포 | `nh-atms-backend`: `MODE=build-deploy`, `MODULE=atms-journal` → ajou01 만 재기동 | — |
| 예전 빌드 다시 | `MODE=redeploy`, `REDEPLOY_BUILD=<번호>` (최근 10개 빌드) | — |
| 재기동만 | `MODE=restart`, `MODULE=<모듈>` 또는 `all` | `~/atms/deploy/jeus/deploy-war.sh restart <모듈>` |
| 상태 | `MODE=status` | `~/atms/deploy/jeus/deploy-war.sh status` |
| 화면 | push 하면 웹훅으로 자동 배포 | — |

- 웹훅으로 도는 백엔드 빌드는 배포하지 않는다 (재기동 시점은 사람이 정한다)
- `atms-common` 이나 `jeus-common.yml` 을 바꿨으면 `MODULE=all`

### 13-7. 되돌리기

| 상황 | 방법 |
|---|---|
| 방금 배포한 모듈이 이상함 | `nh-atms-backend`: `MODE=rollback`, `MODULE=<모듈>` (직전 백업 + 그 서버 재기동) |
| 특정 예전 빌드로 | `MODE=redeploy`, `REDEPLOY_BUILD=<번호>` |
| Jenkins 없이 | ws01: `~/atms/deploy/jeus/deploy-war.sh rollback <모듈>` → `~/atms/deploy/jeus/deploy-war.sh restart <모듈>` |
| 화면 | `nh-atms-frontend`: `MODE=rollback` (또는 wb01 `~/deploy/bin/deploy-front.sh rollback`) |
| 소스 자체 | PC 에서 `git revert` → push (기록이 남는 정석) |
| 자동 배포를 잠시 멈춤 | 잡 화면 → "Disable Project" |

백업은 모듈마다 최근 5개 (`~/deploy/backup/<폴더>-<시각>`), 받은 WAR 도 모듈마다 5개 (`~/atms/release/<모듈>/`) 남는다.

- [ ] 13장 끝: 6개 `/v3/api-docs` 200, health UP, 로그 6개·`Started`, 21001 LISTEN, 9000 경유 로그인·조회

### 13-8. 자주 겪는 문제

| 증상 | 번호 |
|---|---|
| batch 만 기동 실패 | T24 |
| `/actuator/health` 503 | T25 |
| 404 전부·Spring 로그 없음 | T26 |
| UNSTABLE / FAILURE | T29 / T30 |
| `WAR 파일명 형식 오류` / `SHA256 불일치` | T34 |
| `WAR 프로파일` 경고 / `profile is active: "local"` | T35 / T36 |
| `NoSuchMethodError` / `LinkageError` | T37 |
| 21001 bind 실패 | T50 |
| 모듈 간 호출 실패 (원격제어·연동이 502·연결 거부) | T61 |

---

## 14. 2호기(ws02·wb02) 구축 — 이중화

> 전제 (2026-10-30 사용자): 2호기는 **1호기와 같은 JEUS 도메인에 붙는 이중화**다. 같은 도메인에 이미 `nbatmsltws02_atms01` 서버와 `…-nbatmsltwb02_webatms` 커넥터가 정의돼 있다 [확인 2026-10-15].
> 배포킷(Jenkinsfile)은 아직 1호기에만 배포한다. 2호기 배포는 아래 14-7 대로 하고, 킷 수정은 2호기 IP·접속 경로가 확정된 뒤에 한다 (2026-10-30 결정).

### 14-1. 이중화 구성

```
        [개발 PC 브라우저]                                [ATM·STM·공과금수납기]
               │ 9000                                            │
     화면용 L4 VIP [확인 필요]                          에이전트 L4 VIP [확인 필요]
        ┌──────┴───────┐                           ┌─────────────┴──────────────┐
   wb01 :9000     wb02 :9000 ★              ws01 :8081·8083·21001      ws02 :8081·8083·21001 ★
        │  JSV 19000 — JEUS 서버 12개가 각자 wb01·wb02 양쪽에 접속
   ┌────┴─────────────────────────┐
 ws01 서버 6개 (atms01 … ifex01)   ws02 서버 6개 (atms02 … ifex02) ★   ← 같은 도메인 ATMS, DAS 는 ws01
   └──────────────┬───────────────┘
              DB 16.88.250.91:1531 (공용)
 Gitea·Jenkins 는 ws01 에만 → SSH 로 ws01·ws02·wb01·wb02 에 배포
```

### 14-2. 1호기와 무엇이 다른가

| 항목 | 1호기 | 2호기 | 2호기에서 할 일 |
|---|---|---|---|
| Gitea·Jenkins·Podman·이미지·캐시 | 있음 | **없음** (1호기 것을 같이 씀) | — |
| 배포킷 | `~/atms` | `~/atms` (`deploy/jeus` 만 실제로 씀) | 6-5 |
| 앱 폴더·`data`·`deploy` | 6-3 | 같은 경로 (서버 자기 디스크) | 6-3 |
| JEUS 서버 | `atms01` … `ifex01` | `atms02` … `ifex02` [가정] | WAS 담당 (14-6) |
| JVM 옵션 | 없음 (소스 기본값) | `-Djeus.host=<ws02 IP>`, `-Djeus.log-dir=/WASlogs/jeusATMSlogs/nbatmsltws02/app` | WAS 담당 (14-6) |
| `deploy-war.env` | 메뉴 번호 | `JEUS_HOST=<ws02 IP>`, `SERVER_*=…02`, ws02 메뉴 번호 | 14-4 |
| 배포 키 | `ws01-deploy-key` | `ws02-deploy-key` (새로) | 14-7 |
| WebtoB | wb01 + `wb01-deploy-key` | wb02 + `wb02-deploy-key` (새로) | 14-5, 14-7 |
| Jenkinsfile | 1호기 대상 | 1·2호기 대상 (킷 수정 — 나중) | 14-7 |

### 14-3. 순서

1. (담당) 선행 조건
   - ws02·wb02 의 `atmswas` 계정·디스크·HIWARE
   - 방화벽 (1-3 표의 ★ 열)
   - JEUS 노드·서버 6개·JVM 옵션·커넥터·재기동 메뉴
   - wb02 WebtoB 설치·`http.m`
   - 화면용·에이전트용 L4
2. ws02 셋팅 (14-4)
3. wb02 셋팅 (14-5)
4. 14-8 의 **결정 필요 항목을 먼저 정한다** — 정하지 않고 양쪽에 트래픽을 나누면 배치 중복 실행·파일 불일치가 생긴다
5. 배포 키·Jenkins 확장 (14-7)
6. 첫 배포·검증 (14-9)

### 14-4. ws02 셋팅 (ws02, atmswas)

1. **6-1** 상태 확인. Podman·subuid·linger 항목은 무시해도 된다 (2호기는 컨테이너를 돌리지 않음). DB·JSV 확인은 wb01·wb02 둘 다:
   ```bash
   timeout 5 bash -c '</dev/tcp/16.88.250.91/1531' && echo DB-OPEN
   timeout 5 bash -c '</dev/tcp/16.88.122.112/19000' && echo WB01-JSV-OPEN
   timeout 5 bash -c '</dev/tcp/<wb02 IP>/19000' && echo WB02-JSV-OPEN
   hostname
   getent hosts $(hostname)
   ```
   - `getent` 이 아무것도 출력하지 않으면 14-8 ①(배치 호스트 지정) 방식이 동작하지 않는다 → 인프라에 `/etc/hosts` 등록 요청
2. **6-3** 폴더 만들기 (명령 그대로)
3. **6-5** 배포킷 배치 — **`~/atms` 에 둔다.** 배포 키의 `command=` 경로가 `/home/atmswas/atms/deploy/jeus/deploy-war.sh` 로 고정돼 있다
4. 배포 설정:
   ```bash
   ~/atms/deploy/jeus/install-deploy-war.sh
   vi ~/deploy/deploy-war.env
   ~/atms/deploy/jeus/deploy-war.sh status
   ```
   `vi` 에 더하는 줄 (`★` 값):
   ```
   JEUS_HOST=<ws02 IP>
   SERVER_web=atms02
   SERVER_journal=ajou02
   SERVER_batch=abat02
   SERVER_file=file02
   SERVER_ifin=ifin02
   SERVER_ifex=ifex02
   STOP_MENU_web=<atms02 down>
   BOOT_MENU_web=<atms02 boot>
   … (나머지 5개 같은 방식)
   ```
   - 메뉴는 ws02 의 `/JEUS/jeusATMS/jeusMENU/jeusRstart` 기준 [확인 필요] — 10-3 처럼 `jeusmenu.c` 와 메뉴 0 으로 먼저 확인
5. 10-1 과 같은 확인을 ws02 IP 로 한다 (`ss -ltn`, `curl` 6줄, `ps … obj_name`). `domain.xml` 원본은 DAS 가 있는 ws01 에서 본다
6. 10-4: `ls -ld /WASlogs/jeusATMSlogs/nbatmsltws02/app`

### 14-5. wb02 셋팅 (wb02, atmswas)

1. 9-2 와 같다. 배포킷 `deploy` 폴더를 `~/deploy/kit/` 에 올리고 실행한다:
   ```bash
   bash ~/deploy/kit/deploy/webtob/install-wb01.sh
   ```
   - 스크립트 이름은 wb01 이지만 그대로 쓴다: DocBase `~/docs`, 출발 IP 기본 `16.88.122.113` (Jenkins 가 있는 ws01)
   - ws01 에서 `ip route get <wb02 IP>` 의 `src` 가 다르면 키 등록 때 둘째 인자로 준다
2. `~/docs` 가 비어 있으면 교체 연습(9-2 아래쪽)은 건너뛴다. 첫 Jenkins 배포가 채운다
3. (WebtoB 담당) `http.m`: 11-1 과 같은 블록이다. 단 `*NODE` 이름과 `*SVRGROUP` 의 `NodeName` 은 `nbatmsltwb02`. `*SERVER` 이름은 14-8 ⑤ 정책대로
4. 확인: `ss -tn | grep ':19000'` 에 ws01·ws02 두 곳에서 온 연결

### 14-6. JEUS 쪽에 필요한 것 (WAS 담당에게 전달할 내용)

| 항목 | 값 | 이유 |
|---|---|---|
| 노드 | ws02 를 도메인 `ATMS` 에 노드로 추가, ws02 NodeManager | DAS(ws01) 하나로 12개 서버 관리 |
| 서버 6개 | `atms02`·`ajou02`·`abat02`·`file02`·`ifin02`·`ifex02`, 포트 8080~8085, 리스너 ws02 IP, 앱 경로 1호기와 같음, 컨텍스트 `/` | 1호기와 같은 WAR 를 그대로 배포 |
| **JVM 옵션 (ws02 서버 6개)** | `-Djeus.host=<ws02 IP>` | 모듈 간 호출이 ws02 안에서 돌고, file02 가 21001 을 **ws02 IP 에 bind** 하게 (없으면 ws01 IP 에 bind 하려다 실패, T50) |
| 〃 | `-Djeus.log-dir=/WASlogs/jeusATMSlogs/nbatmsltws02/app` (폴더도 생성) | 로그 경로에 호스트 이름이 들어 있음 |
| 금지 | `-Dspring.profiles.active` | WAR 의 web.xml 이 지정 |
| 커넥터 | 12개 서버가 각자 wb01·wb02 양쪽에 연결 (옛 `nbatmsltws01_atms01` 도 두 곳에 연결돼 있었음) | 한쪽 WebtoB 가 죽어도 동작 |
| registration-id | 14-8 ⑤ 결정대로 | `http.m` `*SERVER` 와 짝 |
| 재기동 메뉴 | ws02 서버 6개의 boot·down 번호 | `deploy-war.env` |

### 14-7. 배포를 2호기까지 넓히기

**키 2개 추가** — 9-3·9-4 와 같은 방법으로, 이름만 바꾼다.

| Credential ID | 대상 | 받는 쪽 `authorized_keys` |
|---|---|---|
| `ws02-deploy-key` | `atmswas@<ws02 IP>` | ws02: `restrict,from="<ws01 출발 IP>",command="/home/atmswas/atms/deploy/jeus/deploy-war.sh"` |
| `wb02-deploy-key` | `atmswas@<wb02 IP>` | wb02: `restrict,from="16.88.122.113",command="/home/atmswas/deploy/bin/deploy-front.sh"` |

ws01 (atmswas) — 키 만들기·연결 확인·known_hosts:
```bash
ssh-keygen -t ed25519 -N "" -C jenkins-ws02-deploy -f ~/atms/infra/jenkins/keys/ws02-deploy
ssh-keygen -t ed25519 -N "" -C jenkins-wb02-deploy -f ~/atms/infra/jenkins/keys/wb02-deploy
ip route get <ws02 IP>
podman exec atms-jenkins bash -c "echo > /dev/tcp/<ws02 IP>/22" && echo WS02-22-OK
podman exec atms-jenkins bash -c "echo > /dev/tcp/<wb02 IP>/22" && echo WB02-22-OK
podman exec atms-jenkins sh -c "ssh-keyscan -t ed25519 <ws02 IP> >> ~/.ssh/known_hosts"
podman exec atms-jenkins sh -c "ssh-keyscan -t ed25519 <wb02 IP> >> ~/.ssh/known_hosts"
podman exec atms-jenkins ssh-keygen -lf /var/jenkins_home/.ssh/known_hosts
```
- 공개키를 각 서버에 올려 등록한다:
  - ws02: `~/atms/deploy/jeus/install-deploy-war.sh ~/ws02-deploy.pub` (처음엔 출발 IP 없이 → 아래 `status` 의 `호출` IP 로 다시)
  - wb02: `bash ~/deploy/kit/deploy/webtob/install-wb01.sh ~/deploy/wb02-deploy.pub`
- 지문은 각 서버의 `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` 와 대조한다
- 개인키를 Credentials 에 등록하고 서버의 개인키 파일을 지운다 (9-6)

**Jenkins 배포** — 킷 수정 전 임시 방법 (Jenkins 화면에서만):
- 잡 `nh-atms-backend` 를 복사해 `nh-atms-backend-ws02` 를 만든다. Pipeline script 에서 두 곳만 바꾼다:
  - `WS01_SSH = 'atmswas@<ws02 IP>'`
  - `sshagent(credentials: ['ws02-deploy-key'])`
- 복사한 잡은 `build-deploy` 로 쓴다. `redeploy` 는 잡마다 보관 파일이 따로라 원래 잡의 빌드 번호를 쓸 수 없다
- 복사한 백엔드 잡에서는 `triggers { pollSCM('') }` 줄을 지운다. 웹훅 빌드는 어차피 배포하지 않으므로, 남겨 두면 같은 빌드만 한 번 더 돈다
- 화면도 같은 방식으로 `nh-atms-frontend-wb02` 를 만든다: `WB01 = 'atmswas@<wb02 IP>'`, `wb02-deploy-key`
  - 이 잡은 `triggers` 를 그대로 둔다 → push 하면 wb01·wb02 잡이 차례로 돌아 두 WebtoB 화면이 같은 버전이 된다 (executor 1개라 동시에 돌지 않음)
  - 복사한 잡도 처음 한 번 `MODE=status` 로 실행해야 매개변수·트리거가 등록된다 (9-7)
- 백엔드 순서: 1호기 잡 배포·확인 → 2호기 잡 배포·확인 (차례 배포 = 한쪽이 서비스하는 동안 다른 쪽 교체)

킷 수정 (2호기 확정 후, 나중):
- `backend.Jenkinsfile`: 배포 대상 목록(ws01·ws02, 각자 키)과 `TARGET` 매개변수(all·ws01·ws02)
  - 차례 배포: 대상 하나를 교체 → 재기동·확인 → 성공해야 다음 대상으로
  - 실패하면 거기서 멈춤 (나머지 대상은 이전 버전으로 서비스)
- `frontend.Jenkinsfile`: wb01·wb02 차례 배포, 한쪽 실패 시 버전 불일치 경고
- `deploy-war.sh`·`install-*.sh`: 수정 없음 (서버별 설정은 `deploy-war.env`)

### 14-8. 결정 필요 — 이중화하면 생기는 데이터·운영 문제

2호기에 트래픽을 나누기 **전에** 정한다. 소스·인프라 변경이 필요한 것은 해당 담당에게 넘긴다.

| # | 문제 | 지금 동작 (소스·구성 근거) | 선택지 | 권장 |
|---|---|---|---|---|
| ① | **배치 중복 실행** | abat01·abat02 가 둘 다 `T_BATCH_JOB` 의 잡을 등록 → 같은 정산·통계 잡이 두 번 돈다. `HOST_NAME` 칸이 있으면 그 호스트에서만 등록 (`DynamicBatchScheduler`, `InetAddress.getLocalHost().getHostName()` 과 비교) | (a) 모든 잡의 `HOST_NAME` = `nbatmsltws01`<br>(b) abat02 를 만들지 않음 (batch 는 1호기만)<br>(c) 분산 잠금 (소스 변경) | (a) 또는 (b). (a)는 `getent hosts $(hostname)` 이 응답해야 함 (14-4) |
| ② | **파일 저장소** | `/home/atmswas/data` (업로드·다운로드·이미지·펌웨어·`atms-repo`)가 서버마다 따로 → ws01 에 올린 파일이 ws02 에 없음 | (a) 공유 스토리지(NAS)를 두 서버의 `/home/atmswas/data` 에 마운트<br>(b) 파일 관련 경로·21001 은 1호기만 (L4·WebtoB 에서 액티브-스탠바이)<br>(c) 동기화 스크립트 | (a) — 인프라 |
| ③ | **로그인 갱신 토큰** | access 토큰(JWT)은 같은 `jwt.secret` 이라 양쪽에서 검증된다. 하지만 refresh 토큰은 atms-web **메모리**(`AuthService.refreshTokenStore`)에 있다 → 다른 서버로 간 갱신 요청이나 atms-web 재기동 뒤 갱신이 실패해 다시 로그인하게 된다 (T57) | (a) WebtoB 에서 같은 사용자를 같은 서버로 (지원 여부 [확인 필요])<br>(b) refresh 토큰을 DB 에 저장 (소스 변경)<br>(c) 재로그인 감수 | 단기 (c), 장기 (b) — 개발 |
| ④ | **에이전트 21001·파일 송수신** | 단말이 송수신 중 다른 서버로 가면 이어받지 못하고 저장소도 ② 와 같음 | L4 출발지 IP 세션 유지 + ② | 인프라 |
| ⑤ | **JSV registration-id** | WebtoB `*SERVER` 이름 = registration-id | (a) 같은 모듈의 01·02 서버가 같은 id → WebtoB 가 한 `*SERVER` 로 묶어 양쪽에 나눔 [확인 필요]<br>(b) 서버마다 다른 id + WebtoB 분산 설정 | WAS·WebtoB 담당 결정 |
| ⑥ | **화면 입구** | PC 가 wb01 주소로만 접속하면 wb02 는 놀게 된다 | wb01·wb02 앞에 화면용 L4 VIP | 인프라 |
| ⑦ | **Gitea·Jenkins 는 1호기에만** | ws01 이 멈추면 빌드·배포 불가 (서비스는 2호기가 계속) | 16장 백업 주기 반출로 복구 대비 | 운영 |
| ⑧ | **DB 접속 수** | JEUS 데이터소스 풀(서버마다 따로): 예전 앱 설정 기준 web 10 + journal 10 + batch·file·ifin·ifex 각 5 = 서버 한 대 40, 두 대 **80**. 2호기 서버를 `dsATMS01` 대상에 넣을지도 이때 결정 | DBA 세션 한도 확인 | WAS 담당·DBA |
| ⑨ | **배포 중 버전 차이** | 차례 배포라 잠깐 01·02 버전이 다름 | DB 구조가 바뀌는 배포는 양쪽을 함께 내리고 올림 | 운영 규칙 |
| ⑩ | **로그 분산** | 서버마다 로그 폴더가 다름 | 장애 분석 때 두 서버를 함께 확인 (15-2) | 운영 |

### 14-9. 2호기 확인

ws02 (atmswas):
```bash
~/atms/deploy/jeus/deploy-war.sh status
ss -ltn | grep -E ':(808[0-5]|21001) '
for p in 8080 8081 8082 8083 8084 8085; do echo "$p $(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://<ws02 IP>:$p/v3/api-docs)"; done
ls -l /WASlogs/jeusATMSlogs/nbatmsltws02/app
```
- `status` 의 JEUS 가 ws02 IP, 6개 200, 21001 이 **ws02 IP** 에 LISTEN, 로그 6개

wb01·wb02 (atmswas):
```bash
ss -tn | grep ':19000'
```
- ws01 IP 와 ws02 IP 양쪽에서 온 ESTAB

이중화 시험 (WAS 담당과 시간 약속):
- 1호기 atms01 만 내린 상태에서 화면 로그인·조회가 되는지 → 다시 올린다
- ATM L4 는 ws01 의 8081 을 내렸을 때 ws02 로 가는지 (인프라와)

---

## 15. 일상 운영·점검

### 15-1. 매일 (5분)

ws01 (atmswas):
```bash
podman ps --format '{{.Names}} {{.Status}}'
curl -s http://127.0.0.1:3000/api/healthz | grep -o '"status": *"[a-z]*"' | head -1
curl -s -o /dev/null -w "jenkins %{http_code}\n" http://127.0.0.1:8090/login
~/atms/deploy/jeus/deploy-war.sh status | grep -- '^---'
df -h /home/atmswas /WASlogs
```
wb01 (atmswas):
```bash
~/deploy/bin/deploy-front.sh status | head -3
ss -tn | grep -c ':19000'
df -h /home/atmswas
```

| 확인 | 정상 | 아니면 |
|---|---|---|
| `podman ps` | `atms-gitea Up …`, `atms-jenkins Up …` | 15-4 |
| healthz | `"status": "pass"` | `podman logs --tail 100 atms-gitea` |
| jenkins | `jenkins 200` | `podman logs --tail 100 atms-jenkins` |
| `status` | 6개 모듈 `응답 200` | 그 모듈 로그 (15-2) |
| `df` | 사용률 80% 미만 | 15-3 |
| wb01 `status` | `확인: / → 200, index.html 일치 예` | 직전 배포 확인, 필요하면 롤백 |
| 19000 | 0 이 아님 — 정상일 때의 값을 적어 두고 비교 (서버당 연결 수는 커넥터 설정에 따름) | 11-3·T39 |

### 15-2. 로그 위치

| 무엇 | 위치 | 보는 법 |
|---|---|---|
| 앱 (모듈별) | ws01 `/WASlogs/jeusATMSlogs/nbatmsltws01/app/<모듈>.log` (★ ws02 `nbatmsltws02`) | `tail -n 100 <파일>` · `grep -i -E "ERROR\|Exception" <파일> \| tail -20` |
| JEUS 엔진 (서버별) | `/JEUS/jeusATMS/logs/` 아래 (`→ /WASlogs/jeusATMSlogs`) | `find /JEUS/jeusATMS/logs/ -maxdepth 3 -iname '*<서버명>*'` |
| WAR 배포·재기동 기록 | ws01 `~/deploy/war-deploy.log`, `~/deploy/deploy-history.log` | `tail -n 20 ~/deploy/war-deploy.log` |
| 화면 배포 기록 | wb01 `~/deploy/front-deploy.log`, `~/deploy/front-current.txt` | `tail -n 20 ~/deploy/front-deploy.log` |
| WebtoB 접속·오류 | wb01 `/WEBTOB/webtobATMS/log/atms_access.log_MMDDYYYY`, `atms_error.log_*`, `system.log_*` | `tail -n 50 /WEBTOB/webtobATMS/log/atms_error.log_$(date +%m%d%Y)` |
| Gitea | 컨테이너 로그 | `podman logs --tail 100 atms-gitea` |
| Jenkins | 컨테이너 로그, 빌드 콘솔은 화면 | `podman logs --tail 100 atms-jenkins` |
| 웹훅 | Gitea 저장소 → 설정 → 웹훅 → 최근 전송 | 응답 코드·본문 |

- 배포 기록의 `REJECT` 줄 = 배포 키로 허용되지 않은 명령을 시도한 것 → 17-3 확인

### 15-3. 디스크

ws01 (atmswas):
```bash
df -h /home/atmswas
du -sh ~/atms/images ~/atms/inbox ~/atms/release ~/deploy/backup
podman unshare du -sh ~/atms/infra/gitea ~/atms/infra/jenkins/home ~/atms/infra/jenkins/cache
podman system df
```

정리해도 되는 것:
- 적재 끝난 `~/atms/images/*.tar`
- `~/atms/inbox/seed`, 반입 tar.gz (원본은 내부 PC 에 있음)
- 캐시 교체 뒤의 `*.old` (`podman unshare rm -rf …`)
- Gitea 백업 중 반출 끝난 오래된 것

정리하면 안 되는 것:
- `~/.local/share/containers` 를 손으로 지우기 (이미지가 깨짐)
- `~/atms/infra` 원본
- 앱 폴더(`catms` 등)

자동으로 정리되는 것:
- Jenkins 빌드: 30개, 산출물 10개
- `release`·`deploy/backup`: 모듈마다 5개
- wb01 `inbox`·`backup`: 5개
- Gitea 백업: 14일

### 15-4. 재부팅·장애 뒤

ws01 (atmswas):
```bash
loginctl show-user atmswas -p Linger
systemctl --user is-active atms-gitea
podman ps -a --format '{{.Names}} {{.Status}}'
systemctl --user start atms-gitea
podman start atms-jenkins
```
- 셋째 줄에 둘 다 `Up` 이면 넷째·다섯째 줄은 치지 않는다
- 정상이면 linger + Quadlet(Gitea) + `podman-restart.service`(Jenkins) 가 부팅 때 자동으로 띄운다
- JEUS 서버·WebtoB 기동은 담당 영역이다 — 우리는 `deploy-war.sh status`·`ss` 로 확인만 하고, 안 떠 있으면 담당에 알린다

### 15-5. 주기 작업

| 주기 | 할 일 | 절 |
|---|---|---|
| 매일 | 15-1 점검 | 15-1 |
| 주 1회 | Gitea 백업을 PC 로 반출 | 16-2 |
| 월 1회 | 배포 키 줄·Gitea 계정·토큰·Jenkins 계정 점검, 디스크 추이, `REJECT` 기록 확인 | 17-3·17-4 |
| 의존성 변경 때 | 캐시 다시 만들기·교체 | 4-4·8-4 |
| 배포킷 변경 때 | 재반입 → ws01 `~/atms`·wb01 `~/deploy/kit` 덮어쓰기 → `bash -n` | 6-5·9-2 |
| Jenkinsfile 변경 때 | 잡 Pipeline script 교체 → `MODE=status` 1회 | 9-7 |
| `jeus-common.yml` 변경 때 | 6개 모두 재배포 (`MODULE=all`) | 13-6 |

### 15-6. Jenkins 잡 MODE 정리

| 잡 | MODE | 하는 일 |
|---|---|---|
| `nh-atms-frontend` | `build-deploy` (기본·웹훅) | 빌드 → wb01 배포 → 확인 (실패 시 자동 되돌림) |
| | `build-only` | 빌드·보관만 |
| | `redeploy` + `REDEPLOY_BUILD` | 보관된 빌드 다시 배포 (최근 10개) |
| | `rollback` | wb01 직전 백업으로 |
| | `status` | wb01 현재 버전·기록·백업 |
| `nh-atms-backend` | `build-only` (기본·웹훅) | WAR 6개(또는 `MODULE` 하나) 빌드·보관만 |
| | `build-deploy` | 빌드 → 교체 → (`RESTART`) 그 모듈 서버만 재기동·확인 (실패 시 그 모듈 자동 롤백) |
| | `redeploy` + `REDEPLOY_BUILD` | 보관된 WAR 로 같은 동작 |
| | `rollback` + `MODULE`(하나) | 직전 백업 + 재기동 |
| | `restart` | 교체 없이 재기동·확인 |
| | `status` | 모듈별 응답·메뉴·최근 WAR·백업 |

---

## 16. 백업·복구

### 16-1. 무엇을 어디에

| 대상 | 원본 | 백업 | 복구 |
|---|---|---|---|
| 소스 | Gitea | 매일 dump (자동) + 16-3 전체 백업, 개발자 PC 의 clone | 16-3 (최악: 개발자 PC 에서 새 Gitea 로 push) |
| Gitea 계정·토큰·웹훅·설정 | `~/atms/infra/gitea` | 16-3 | 16-3 |
| Jenkins 잡·Credentials(개인키)·설정 | `~/atms/infra/jenkins/home`, `jenkins.env`, `keys/*.pub` | 16-4 | 16-4 |
| 빌드 캐시 | `~/atms/infra/jenkins/cache` | 반입 원본(tar.gz)을 내부 PC 에 보관 | 8-1 |
| WAR | `~/atms/release/<모듈>` (5개) + Jenkins 보관 (10개 빌드) | 자동 | 13-7 |
| 배포된 앱의 이전 버전 | `~/deploy/backup` (모듈마다 5개) | 자동 | `MODE=rollback` |
| 화면 이전 버전 | wb01 `~/deploy/backup` (5개) + Jenkins (10개) | 자동 | `MODE=rollback`·`redeploy` |
| 서버 설정 | ws01 `~/deploy/deploy-war.env`, ws01·wb01 `~/.ssh/authorized_keys` | 바꿀 때 사본 (`cp -p … ….bak-<날짜>`) | 사본 복사 |
| JEUS·WebtoB 설정 | `domain.xml`, `http.m` | 담당 (`http.m.bak-*`) | 담당 / 11-4 |
| 반입 원본 | 내부 PC | 보관 | 4장 |

### 16-2. Gitea 매일 백업 확인·반출 (주 1회)

ws01 (atmswas):
```bash
systemctl --user list-timers | grep gitea
ls -lht ~/atms/backup/gitea | head -5
df -h /home/atmswas
```
- 파일은 atmswas 소유라 FileZilla 로 `~/atms/backup/gitea/` 에서 바로 PC(보관 위치)로 받는다
- 매일 크기가 거의 같아야 정상이다. **날마다 커지면** 이전 dump 를 다시 담고 있는 것 → T65
- 소스가 들어 있는 파일이다 → 보관 위치의 접근을 제한한다

### 16-3. Gitea 전체 백업·복원 (정지 방식) [미시험]

dump 는 SQL·파일을 따로 담은 형식이라 되살리려면 별도 도구(sqlite3)가 필요하다. 그래서 되살릴 때는 **Gitea 를 잠깐 멈추고 폴더째 묶은 백업**을 쓴다. 업그레이드·큰 변경 전과 월 1회 권장.

백업 — ws01 (atmswas), Gitea 가 수 초~1분 멈춘다:
```bash
systemctl --user stop atms-gitea
podman unshare tar -C ~/atms/infra --exclude=gitea/data/backup -czf ~/atms/infra/gitea-full-$(date +%Y%m%d).tar.gz gitea
systemctl --user start atms-gitea
curl -s http://127.0.0.1:3000/api/healthz
ls -l ~/atms/infra/gitea-full-*.tar.gz
```
- `podman unshare tar` 는 파일 소유자(1000)를 그대로 담는다. 매일 dump 폴더는 크기 때문에 뺀다
- 만든 파일은 PC 로 반출해 둔다

복원 — 같은 서버:
```bash
systemctl --user stop atms-gitea
mv ~/atms/infra/gitea ~/atms/infra/gitea.broken-$(date +%Y%m%d%H%M)
podman unshare tar -C ~/atms/infra -xzf ~/atms/infra/gitea-full-<날짜>.tar.gz
systemctl --user start atms-gitea
curl -s http://127.0.0.1:3000/api/healthz
```
- 새 서버라면: 6장까지 끝낸 뒤 백업 파일을 `~/atms/infra/` 에 올리고 위 셋째 줄로 푼다 → `install-gitea.sh 16.88.122.113` (풀린 `gitea.env` 의 비밀키·데이터를 그대로 쓰고 Quadlet 을 만든다)
- 확인: PC 로그인, 저장소·커밋·웹훅이 그대로인지 → 문제없으면 `gitea.broken-*` 는 `podman unshare rm -rf` 로 지운다

### 16-4. Jenkins 백업·복원 [미시험]

백업 — ws01 (atmswas), Jenkins 가 수 분 멈춘다 (빌드가 없을 때):
```bash
podman stop atms-jenkins
podman unshare tar -C ~/atms/infra --exclude='jenkins/home/jobs/*/builds' --exclude=jenkins/home/workspace -czf ~/atms/infra/jenkins-home-$(date +%Y%m%d).tar.gz jenkins/home jenkins/jenkins.env jenkins/keys
podman start atms-jenkins
ls -l ~/atms/infra/jenkins-home-*.tar.gz
```
- 담기는 것: 잡 설정, Credentials (개인키 3개 — 암호화 키 `secrets/` 와 함께), 웹훅 토큰, 플러그인, `jenkins.env`, 공개키
- 빼는 것: 빌드 기록·산출물(WAR, 수 GB)·작업 폴더 — 다시 빌드하면 된다
- 개인키가 들어 있으므로 반출·보관 위치를 제한한다

복원:
```bash
podman rm -f atms-jenkins
podman unshare mv ~/atms/infra/jenkins/home ~/atms/infra/jenkins/home.broken-$(date +%Y%m%d%H%M)
podman unshare tar -C ~/atms/infra -xzf ~/atms/infra/jenkins-home-<날짜>.tar.gz
~/atms/deploy/jenkins/run-jenkins.sh
```
- 확인: 로그인 → 잡 2개·Credentials 3개가 있는지 → 두 잡 `MODE=status` SUCCESS
- 새 서버라면 8-1(캐시)을 먼저 하고, 9-5(known_hosts)는 `home` 안에 들어 있어 그대로 복원된다 (서버 호스트 키가 바뀌었으면 다시)

### 16-5. 1호기를 통째로 다시 세울 때 (재해 복구)

1. 내부 PC 에 보관한 반입 원본(4장)과 최근 Gitea·Jenkins 백업(16-3·16-4)을 준비한다
2. 6장 (root 작업 포함) → 7-1 대신 16-3 복원 + `install-gitea.sh` → 8-1 캐시 → 16-4 복원
3. 9장: 서버 쪽만 다시 한다
   - 9-2 (wb01 은 보통 그대로)
   - 9-3·9-4 의 `authorized_keys` 등록: 복원된 `keys/*.pub` 를 다시 등록
   - 9-5 known_hosts (ws01 호스트 키가 바뀌었으면)
4. 10~13장 (JEUS·WebtoB 는 담당 복구 확인 후)

### 16-6. 큰 변경 전 소스 스냅샷 (저장소 통째 복사)

구성을 크게 바꾸기 전에 지금 소스를 별도 저장소에 그대로 복사해 둔다. 첫 사례는 2026-11-02, 매니지드 서버 6개 전환 전의 `NH-ATMS/nh-atms-old` 다. 원래 저장소(`nh-atms`)와 Jenkins·웹훅 연결은 그대로 둔다.

1. Gitea 화면(`atmsadmin`): 조직 `NH-ATMS` → 새 저장소 `nh-atms-old`
   - **비공개**, 템플릿·README 없음, **저장소 초기화 해제**
   - 웹훅은 만들지 않는다 → Jenkins 가 반응하지 않음
2. PC (PowerShell):
   ```powershell
   cd D:\
   git clone --mirror http://16.88.122.113:3000/NH-ATMS/nh-atms.git nh-atms-mirror.git
   cd D:\nh-atms-mirror.git
   git push --mirror http://16.88.122.113:3000/NH-ATMS/nh-atms-old.git
   git ls-remote http://16.88.122.113:3000/NH-ATMS/nh-atms.git main
   git ls-remote http://16.88.122.113:3000/NH-ATMS/nh-atms-old.git main
   ```
   - `--mirror` 는 모든 브랜치·태그·이력을 그대로 복사한다
   - 마지막 두 줄의 커밋 해시가 같으면 성공
   - `refs/pull/…` 거부 메시지는 무시해도 된다 (브랜치·태그는 복사됨)
3. Gitea: `nh-atms-old` → 설정 → 위험 영역 → **이 저장소 보관(Archive)**
   - 읽기 전용이 된다 — developers 팀은 모든 저장소에 쓰기 권한이 있어서, 보관하지 않으면 실수로 push 될 수 있다
4. 확인이 끝나면 PC 의 `D:\nh-atms-mirror.git` 는 지워도 된다 (남겨 두면 PC 쪽 백업이 하나 더 생긴다)

- 이 저장소에도 실제 값이 든 `jeus-common.yml` 이 들어 있다 → 반드시 비공개
- 되돌릴 때: 보관 해제 → `nh-atms-old` 에서 필요한 커밋·파일을 가져온다. 원래 저장소에 그대로 다시 push 하지 않는다 (`main` 강제 푸시 금지)

---

## 17. 보안·계정·키 관리

### 17-1. HIWARE 규칙

| 규칙 | 내용 |
|---|---|
| 금지명령 | `sudo`, `su`, `ssh …` (ws01 에서 확인). 3회면 세션 잠김 |
| 허용 확인 | `ssh-keygen`, `podman exec … ssh-keyscan` |
| root | 1일 권한 신청 → root 로 직접 접속. 그날도 `sudo` 금지 |
| 기록 | HIWARE 는 사람의 접속만 기록한다. 서버 간 SSH(Jenkins 배포)는 기록되지 않음 → 서버 로그로 남김 (17-6) |

### 17-2. 비밀값 목록

| 비밀값 | 어디에 | 바꿀 때 |
|---|---|---|
| Jenkins 관리자 비밀번호 | ws01 `~/atms/infra/jenkins/jenkins.env` (600) | 파일 수정 → `run-jenkins.sh` |
| Gitea 관리자(`atmsadmin`) 비밀번호 | Gitea | 화면에서 |
| 개발자 Gitea 토큰 | 각자 PC 자격 증명 관리자 | 각자 재발급, 퇴사·분실 시 삭제 |
| `jenkins-bot` 토큰 | Jenkins Credentials `gitea-jenkins-bot` | Gitea 에서 새로 발급 → Credentials 수정 → 옛 토큰 삭제 |
| notifyCommit 토큰 | Jenkins Security + Gitea 웹훅 URL | 둘 다 함께 바꿈 |
| 배포 키 개인키 | Jenkins Credentials 에만 (서버 파일은 삭제) | 17-3 |
| Gitea `SECRET_KEY`·`INTERNAL_TOKEN` | `gitea.env` (600) | 바꾸지 않는다 (바꾸면 저장된 비밀값을 못 읽음) |
| DB 비밀번호 (개발서버) | JEUS 데이터소스 `dsATMS01` (WAS 담당) | WAS 담당이 데이터소스에서 변경 → 서버 재기동 |
| DB 비밀번호 (로컬) | 내부 Gitea `local-common.yml` (서버 WAR 에는 안 들어감) | 파일 수정 |
| JWT 시크릿 | 내부 Gitea `jeus-common.yml` → WAR 안 (`~/atms` 700, 앱 폴더 jeus 그룹 읽기) | 소스 수정 → 6개 재배포 (모두 다시 로그인) |

공통:
- 사진·메신저·문서·GitHub 에 남기지 않는다
- 화면을 찍어야 하면 비밀번호·토큰·키가 보이지 않는지 먼저 본다

### 17-3. 배포 키 관리

| 할 일 | 방법 |
|---|---|
| 등록 현황 | ws01·wb01: `cat ~/.ssh/authorized_keys` → `jenkins-ws01-deploy` / `jenkins-wb01-deploy` 한 줄씩 (`restrict,from=…,command=…`) |
| **즉시 차단** | 그 서버에서 `vi ~/.ssh/authorized_keys` → 해당 줄 삭제 |
| 이상 시도 확인 | ws01 `grep REJECT ~/deploy/war-deploy.log` · wb01 `grep REJECT ~/deploy/front-deploy.log` |
| 교체 (예: 연 1회, 담당자 변경 때) | ① `ssh-keygen` 으로 새 키 (주석에 연도) ② 새 공개키 등록 — 설치 스크립트는 같은 키만 바꿔 쓰므로 새 키는 줄이 더해짐 ③ Credentials 의 개인키를 새 것으로 수정 ④ `MODE=status` 성공 ⑤ 옛 줄 삭제 ⑥ 서버의 새 개인키 파일 삭제 |

### 17-4. Gitea·Jenkins 계정

| 경우 | Gitea | Jenkins |
|---|---|---|
| 입사 | 7-5 ② 계정 생성 → `developers` 팀 → 본인이 토큰 발급 | 필요할 때만 계정 생성 (17-5) |
| 퇴사·역할 변경 | 사이트 관리 → 사용자 → 비활성화(또는 삭제) + 그 사람 토큰 삭제 | 계정 삭제 |
| 정기 점검 | 사용자·토큰 목록 확인 | 사용자 목록 확인 |

### 17-5. Jenkins 권한 모델

- 권한 방식이 "로그인한 사용자는 모든 권한"(`loggedInUsersCanDoAnything`)이다
- 따라서 Jenkins 계정이 있는 사람은 **누구나 배포·되돌리기·재기동**을 할 수 있다 → 계정은 배포 책임자만 둔다
- 세분화(역할별 권한)가 필요하면 플러그인 추가 → Jenkins 이미지 재제작(4-3)이 필요하다

### 17-6. 보안 담당 공유 문구

```
[공유] NH-ATMS 개발서버 배포 자동화 (서버 간 SSH · 배포 전용 키)
- 경로: ws01(16.88.122.113) Jenkins → wb01(16.88.122.112) / ws01 호스트, 계정 atmswas, TCP 22
- 키: ed25519. 서버 authorized_keys 에 restrict + from="16.88.122.113" + command=<배포 스크립트> 로 고정
  · 셸 접속·다른 명령 불가. 허용: 화면 deploy/rollback/status, WAR deploy/rollback/status/restart
  · 그 외 요청은 거부하고 REJECT 로 기록
- 기록: HIWARE 를 거치지 않으므로 호출·결과를 서버 로그에 남김
  (wb01 ~/deploy/front-deploy.log, ws01 ~/deploy/war-deploy.log)
- WAR 재기동: 은행 jeusRstart 메뉴 중 WAS 담당이 정한 서버별 boot/down 번호만 표준입력으로 호출
  (전체 kill 메뉴 3, WebtoB 메뉴 7·8 은 스크립트에서 차단)
- 차단: 해당 서버 authorized_keys 의 키 한 줄 삭제 (즉시 적용)
```

### 17-7. 운영 전에 다시 볼 것

| 항목 | 지금 | 운영 전 |
|---|---|---|
| 프로토콜 | http (화면·에이전트), 에이전트 메시지는 HMAC | https 재검토 |
| base 리스너 노출 | 서버마다 base = HTTP (포트 1개) → ATM 망에 8081·8083 을 열면 base 도 열림 | WAS 담당 판단으로 분리 여부 확정 |
| CPU 제한 | Gitea·Jenkins 메모리 제한만 (2026-10-16 보류) | 필요하면 root 로 cgroup cpu 위임 ([ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2-9) |
| Jenkins 권한 | 로그인 사용자 전체 권한 | 17-5 |
| 백업 반출 | 수동 주 1회 | 자동화 또는 보관 정책 |
| 에이전트 폴링 경로 | 소스 결함 (T49) | 개발 조치 |

---

## 18. 트러블슈팅 색인

각 장 끝의 "자주 겪는 문제" 번호를 여기서 찾는다. 괄호 안 날짜는 실제로 겪은 사례다.

| 번호 | 증상 | 원인 | 조치 | 관련 |
|---|---|---|---|---|
| T01 | `Command "[sudo -l]" is not allowed … Prohibited input count : 1` (`ssh …` 도 같음) | HIWARE 금지명령 — 3회면 세션 잠김 | 다시 치지 않는다. ssh 동작 확인은 Jenkins `MODE=status`, root 작업은 root 직접 접속 | 0-3, 17-1 (10-15 sudo, 10-23 ssh) |
| T02 | `install-gitea.sh` 가 IP 거부 / `ROOT_URL` 이 `http://fe80::…` | `hostname -I` 첫 값(IPv6 내부 주소)을 씀 | `install-gitea.sh 16.88.122.113` 다시 실행 (IP 갱신, 비밀키 유지) | 7-1 (10-14) |
| T03 | 로그아웃하면 Gitea·Jenkins 가 내려감 (재접속 시 `podman ps` 비어 있음) | linger 꺼짐 | root 날 `loginctl enable-linger atmswas` (6-2 R2). 그 전에는 접속할 때 `systemctl --user start atms-gitea`, `podman start atms-jenkins` | 6-1 (10-15) |
| T04 | `podman load` 에 `potentially insufficient UIDs or GIDs` | subuid·subgid 없음 | 6-2 R1 → atmswas 로 `podman system migrate` → 다시 load | 6-1 |
| T05 | `crun: readlink ``: No such file or directory … command that was not found` | `--userns=keep-id` (podman 5.8.2 / crun 1.27 에서 실패) | 빼고 실행. 데이터 폴더는 `podman unshare chown -R 1000:1000 <폴더>` (스크립트가 함) | 0-3 (10-14) |
| T06 | `crun: controller 'cpu' is not available` | rootless 에 cgroup cpu 미위임 (RHEL 9 기본) | `--cpus` 를 뺀다 (스크립트는 자동으로 뺌). CPU 제한은 보류 중 | 6-1 (10-14) |
| T07 | 로그인했더니 `/home/atmswas` 가 비어 있음 (`.config`·`.local` 만) | 인프라가 새 디스크를 기존 폴더 위에 마운트 → 기존 내용이 가려짐 | `df -h /home/atmswas` 로 확인 → 가려진 내용 복사를 인프라에 요청 ([ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2-11) 또는 새로 구성 | (10-19) |
| T08 | `Failed to bind port 8090 (Address already in use)` | 다른 프로세스가 8090 사용 (사례: 가려진 옛 저장소에서 돌던 옛 Jenkins) | `ss -ltnp \| grep 8090` → atmswas 자기 프로세스면 종료 후 `run-jenkins.sh` | 8-2 (10-21) |
| T09 | 옛 `install-gitea.sh` 에서 `data/backup: Permission denied` | 옛 스크립트의 폴더 생성 순서 | 최신 배포킷 사용. 급하면 `podman unshare mkdir -p ~/atms/infra/gitea/data/backup` 후 재실행 | 7-1 (10-21) |
| T10 | Gitea 컨테이너 → `http://16.88.122.113:8090` `Could not connect` | rootless 기본(pasta) 모드에서 컨테이너가 서버 IP 를 자기 주소로 봄 | 두 컨테이너를 `atms-net` 에 두고 이름으로 접속 (스크립트가 구성) | 8-3 (10-22) |
| T11 | PC → 113:3000·8090 시간 초과, 112:9000 은 됨 | 네트워크 방화벽 미적용 (서버는 정상) | 서버 `ss -ltn` 에 포트가 있고, PC 시험 중 서버 `ss -tn` 에 PC 연결이 안 보이면 방화벽 → 신청 담당에 재확인 | 7-4 (10-21) |
| T12 | 웹훅 `414 URI Too Long` | 방법 GET (push 내용을 URL 에 붙여 보냄) | HTTP 방법 **POST**, `application/json` | 9-9 (10-23) |
| T13 | 웹훅 200 인데 `No git jobs using repository: …` | `url=` 이 Jenkinsfile `GITEA_REPO` 와 다름 / 잡을 한 번도 실행 안 함 | `url=http://atms-gitea:3000/NH-ATMS/nh-atms.git` / 잡 1회 실행 | 9-9 |
| T14 | 웹훅 `403`·`401` | notifyCommit 토큰 오타·누락, 만든 뒤 Save 안 함 | 토큰 새로 만들어 Save → 웹훅 URL 수정 | 9-9 |
| T15 | `webhook can only call allowed HTTP servers` / 연결 실패 | 대상이 `atms-jenkins` 가 아님, `ALLOWED_HOST_LIST` 가 `private` 아님, 같은 네트워크 아님 | 대상 URL, `gitea.env` 확인 → `install-gitea.sh` 재실행, T10 | 9-9 |
| T16 | 잡 소스 단계 `Authentication failed` / `repository not found` | Credentials 종류를 "Gitea Personal Access Token" 으로 만듦, 토큰·ID 오타, jenkins-bot 이 readers 팀이 아님 | 종류 **Username with password**, ID `gitea-jenkins-bot`, 팀 확인 | 9-6 (10-23) |
| T17 | `GRADLE_USER_HOME=미설정` → `Downloading https://services.gradle.org/…` → `UnknownHostException` | 컨테이너에 Gradle 캐시 경로가 없음 | 2026-10-23 판 이후 `backend.Jenkinsfile` 붙여 넣기 (경로 고정), `run-jenkins.sh` 재실행 | 8-2 (10-23) |
| T18 | `…gradle-8.14.4-bin.zip.lck (Permission denied)` | 캐시 파일이 컨테이너 jenkins(1000) 소유가 아님 | `podman unshare chown -R 1000:1000 ~/atms/infra/jenkins/cache` 또는 `run-jenkins.sh` 재실행 | 8-1 (10-23) |
| T19 | `Could not resolve …` / `No cached version of … available for offline mode` | 오프라인 캐시에 그 라이브러리가 없음 (사례: orai18n) | 4-4 로 캐시 재생성 → 8-4 로 `modules-2` 통째로 교체 | 4-4 (10-26) |
| T20 | `npm ci` `ENOTCACHED` / `Cannot find module @rollup/rollup-linux-x64-gnu` 류 | npm 캐시에 패키지 또는 리눅스용 바이너리가 없음 | 4-4 ② (리눅스 컨테이너에서) → 8-4 | 4-4 |
| T21 | `Cannot find a Java installation … vendor=Eclipse Temurin` | `build.gradle` 이 Temurin 21 을 요구, 오프라인이라 자동 다운로드 불가 | Jenkins: Jenkinsfile 이 이미지 JDK(Temurin 21)를 지정. PC: `-Porg.gradle.java.installations.paths=<Temurin 21 폴더>` | 12-5 |
| T22 | 로그인 때 `ORA-17056: 지원되지 않는 문자 집합 … KO16MSWIN949` | orai18n 없음 — 개발서버는 **JEUS 라이브러리**(JNDI 풀), 로컬은 앱 의존성 | 개발서버: WAS 담당에 JEUS 라이브러리 orai18n 추가 요청 (WAS 요청서 2-8). 로컬: `atms-common/build.gradle` 의 `runtimeOnly 'com.oracle.database.nls:orai18n'` + 캐시(T19) | 13-3 (10-26) |
| T23 | 로컬에서 엉뚱한 DB 로 붙음 / `ORA-01017`·`Unknown host` | DB 값이 `local-common.yml` 말고 다른 곳(`application.yml`, 환경변수 `SPRING_DATASOURCE_*`)에도 있음, 또는 `CHANGE_ME_DB_PASSWORD` 그대로 | `local-common.yml` 한 곳으로 정리, 환경변수 확인 (`echo %SPRING_DATASOURCE_URL%`) | 12-4 |
| T24 | atms-batch 만 기동 실패 `Unable to configure the default job repository` | DB 접속 불가 (Spring Batch 메타 테이블) | DB 값·방화벽 확인. 그동안 `HEALTH_SKIP="atms-batch"` | 10-3 |
| T25 | `/actuator/health` 503 `{"status":"DOWN"}` | DB 미연결 | 기동 확인은 `/v3/api-docs` (스크립트 기본). DB 해결 뒤 `UP` | 13-3 |
| T26 | 모든 경로가 JEUS 404, 앱 로그에 Spring 로그 없음 | 그 서버에 앱 미등록 / 앱 경로가 실제 폴더와 다름 / (드묾) Jakarta 미지원 | 10-1 앱 경로·10-2 폴더 이름 확인 → 담당에 앱 기동 로그 확인 요청 → 그래도면 10-5 | 10-1 (10-26: 앱 1개만 등록) |
| T27 | 서버 `stderr` 에 `[WebT]:webtCPInfo is null` + NPE 매초 | 은행 JEUS 의 WebtoB 연동 설정 (우리 앱과 무관) | WAS 담당에 전달 | (10-26) |
| T28 | `deploy-war.sh status` 응답 `000` | 그 서버가 안 떠 있음 / 리스너 주소가 다름 (사례: 8766 이 113 에만 LISTEN 이라 127.0.0.1 로는 000) | `ss -ltn` 으로 포트·주소 확인, `deploy-war.env` 의 `JEUS_HOST` | 10-3 (10-23) |
| T29 | Jenkins **UNSTABLE**, `WAS 담당 재기동 필요: <모듈>` | 그 모듈의 `STOP_MENU_*`·`BOOT_MENU_*` 없음 (종료코드 4). 파일은 교체됨 | 담당이 그 서버 재기동 → `MODE=status`. 번호를 받으면 10-3 | 13-2 |
| T30 | Jenkins **FAILURE**, `기동 확인 실패` → `자동 롤백` | 새 WAR 로 그 서버가 200 을 못 냄 (설정·DB·코드) | 그 모듈 로그의 `APPLICATION FAILED`·`Caused by` → 고쳐서 다시. `롤백후에도실패` 면 담당 확인 | 13-2 |
| T31 | `Host key verification failed` | Jenkins 컨테이너 `known_hosts` 에 그 주소 없음 / 서버 호스트 키 바뀜 | 9-5 다시 (지문 대조). 키가 바뀐 이유를 먼저 확인 | 9-5 |
| T32 | `Permission denied (publickey…)` | `from=` IP 불일치 / Credentials 의 개인키가 다른 키 / `~/.ssh` 권한·SELinux 라벨 | `호출` IP 로 다시 등록 (9-8), 키 쌍 확인, `ls -Zd ~/.ssh ~/.ssh/authorized_keys` → `ssh_home_t` 아니면 `restorecon -Rv ~/.ssh` | 9-8 (10-23) |
| T33 | SSH `Connection refused` / `timed out` | 그 주소로 22 에 닿지 않음 | 9-5 OPEN 시험. ws01 은 `host.containers.internal` (컨테이너에서 113·86 은 거부 [확인 10-23]), wb 는 방화벽 | 9-5 |
| T34 | `WAR 파일명 형식 오류` / `SHA256 불일치` | 산출물 이름 변경 / 전송 중 손상 | 다시 빌드 (`<모듈>-<버전>-<YYYYMMDD>-<번호>.war`) | 13-2 |
| T35 | `경고: WEB-INF/web.xml 에 프로파일 지정 없음` | `-Pjeus` 없이 만든 WAR | Jenkinsfile 은 항상 `-Pjeus`. 수동이면 `bootWar -Pjeus` | 13-2 |
| T36 | `profile is active: "local"` / `Could not resolve placeholder 'JWT_SECRET'` | web.xml 없음, 또는 JEUS JVM 옵션 `-Dspring.profiles.active` 가 덮어씀 | T35, 담당에 그 옵션 제거 요청 | 13-3 |
| T37 | `NoSuchMethodError` / `LinkageError` (jackson·slf4j 등) | JEUS 기본 라이브러리와 충돌 | 담당에 WEB-INF/lib 우선 로딩(classloading) 확인 요청 | 13-3 |
| T38 | `/api/` 가 `302 /error/error.html` | `http.m` `*URI` 미적용 (`wscfl`·재기동 안 함 포함) | 11장 | 11-3 |
| T39 | 19000 ESTAB 없음·일부만 / 특정 경로 `503` | registration-id ≠ `*SERVER` 이름, `JSVPORT` 없음, 커넥터 미설정, 그 서버 미기동 | 10-1 의 registration-id 와 `http.m` 대조, 담당 확인 | 11-3 |
| T40 | 하위 화면에서 F5 → 404 (은행 오류 페이지) | React BrowserRouter — 없는 경로를 `index.html` 로 돌려주는 설정 없음 | WebtoB 담당과 협의 (`/api/` 제외 SPA fallback). 그동안 메인에서 메뉴로 이동 | 13-4 |
| T41 | `http://16.88.122.112:9000/swagger-ui.html` 이 안 열림 | `/swagger-ui.html` 은 `*URI` `/swagger-ui/` 에 안 걸려 정적 파일로 감 | `http://16.88.122.112:9000/swagger-ui/index.html` | 13-4 |
| T42 | push `Authentication failed` / `403` | 비밀번호 칸에 토큰이 아닌 값 / 토큰·팀 권한 부족 | Windows 자격 증명 관리자에서 `git:http://16.88.122.113:3000` 삭제 후 토큰으로, `developers` 팀·`repository` 쓰기 확인 | 7-6 |
| T43 | `! [rejected] main -> main (fetch first)` | 다른 사람이 먼저 push | `git pull` 후 다시 push (강제 푸시 금지) | 7-6 |
| T44 | Jenkins 이미지가 `<none>` 으로 적재 | 이름 없이 저장됨 | `podman tag <IMAGE ID> localhost/nh-atms-jenkins:bank` | 6-6 |
| T45 | `sha256sum -c` 가 `FAILED` / 모든 줄 실패 | 업로드 중 손상(텍스트 모드) / `SHA256SUMS.txt` 가 CRLF | 그 파일만 **바이너리**로 다시 / `sed -i 's/\r$//' SHA256SUMS.txt` | 6-4 |
| T46 | `$'\r': command not found` / `bad interpreter` | Windows 줄바꿈(CRLF) | `sed -i 's/\r$//' <파일>` (6-5 의 `find … sed` 한 줄) | 6-5 |
| T47 | 서버에서 zip 을 풀었더니 `deploy\jenkins\Dockerfile` 같은 파일 하나 | Windows 에서 만든 zip 의 경로 구분자 | 내부 PC 에서 풀어 폴더째 올림 | 4-5 |
| T48 | `no space left on device` | `/home/atmswas` 가 참 | 15-3 정리, 인프라에 증설 요청 | 15-3 |
| T49 | 에이전트 폴링 `/api/agent/ping`·`/atmr/agent/ping.do` 가 404 | 업체 소스 `LegacyAgentController` 의 `ping` 메서드가 닫히지 않은 주석 안 → 매핑 없음 (open 응답은 `/api/agent/ping` 을 알려 줌) | 개발 조치 (주석 닫기 + 매핑 추가). L4 헬스체크에는 쓰지 않음 | 13-5 (10-30 소스 확인) |
| T50 | file 서버 로그에 21001 bind 실패 (`Cannot assign requested address` / `Address already in use`) | `jeus.host` 가 그 서버의 IP 가 아님 (2호기에 `-Djeus.host` 누락) / 포트 중복 | 14-6 JVM 옵션, `ss -ltnp \| grep 21001` | 14-6 |
| T51 | 빌드 중 컨테이너가 죽음, 콘솔이 끊기고 exit 137 | 메모리 부족 | `JENKINS_MEMORY=3g ~/atms/deploy/jenkins/run-jenkins.sh` (데이터 유지), 빌드는 한가한 시간에 | 8-2 |
| T52 | 재부팅 뒤 Gitea·Jenkins 가 안 뜸 | linger 없음 / Quadlet·`podman-restart` 미설정 | 15-4. 근본: linger, `install-gitea.sh` 재실행(Quadlet), `systemctl --user enable podman-restart.service` | 15-4 |
| T53 | `/JEUS` 가 비어 있음, `/usr/local/jeusATMS` 링크 끊김 | JEUS 재설치 등 담당 작업 (사례: 이후 복구됨) | **아무것도 건드리지 않고** WAS 담당에 즉시 문의. JEUS 프로세스 kill 금지 | (10-22) |
| T54 | 앱 로그 파일이 안 생김 (JEUS 쪽에 logback `openFile … failed`) | `/WASlogs/jeusATMSlogs/<호스트>/app` 없음 또는 jeusatms 쓰기 불가 | 10-4 (담당 또는 root R4). 앱 동작에는 영향 없음 | 10-4 |
| T55 | `정지 확인 실패 (180초 후에도 응답) → … 기동하지 않음` | 정지 메뉴 번호가 다른 서버를 가리킴 / 정지가 오래 걸림 | `jeusmenu.c` 로 번호 확인 (10-3), 필요하면 `deploy-war.env` 에 `STOP_WAIT=300` | 10-3 |
| T56 | jeusRstart 가 메뉴를 반복 출력하며 대기 | 0~8 외 입력 / 입력 방식 문제 | 스크립트는 숫자 하나만 넣음. 손으로 시험할 때는 메뉴 0 만. 멈추지 않으면 담당 확인 (root 로 돌아 atmswas 가 끌 수 없음) | 10-1 |
| T57 | 로그인 후 한참 뒤(또는 atms-web 재기동 뒤) 갑자기 로그인 화면으로 | refresh 토큰이 atms-web 메모리에만 있음 → 재기동하거나 다른 서버로 가면 갱신 실패 | 단일 서버에서는 재기동 뒤 다시 로그인 (정상). 이중화는 14-8 ③ | 14-8 |
| T58 | Jenkins 관리자 로그인 불가 | `jenkins.env` 비밀번호와 다름 (JCasC 가 env 값으로 만듦), `'` 포함 | `jenkins.env` 확인·수정 → `run-jenkins.sh` | 8-2 |
| T59 | Gitea 화면 시간이 9시간 차이 | 시간대 설정 | `gitea.env` 의 `GITEA__time__DEFAULT_UI_LOCATION=Asia/Seoul` 확인 → 재기동 | 7-1 |
| T60 | `wsboot` 뒤 9000 응답 없음·오류 | `http.m` 오류 | 11-4 되돌리기 | 11-4 |
| T61 | 모듈 간 호출 실패 (원격제어 전송·연동이 502·연결 거부·시간 초과) | 대상 서버 미기동 / 리스너가 127.0.0.1 에만 / `jeus.url.*` 포트가 실제와 다름 | `curl http://16.88.122.113:<포트>/v3/api-docs`, `ss -ltn`, `jeus-common.yml` 확인 | 12-1 |
| T62 | PC 에서 `http://16.88.122.113:8080` 등 직접 접속 안 됨 | PC → 113:8080~8085 방화벽 미신청 (8766 만 신청돼 있음) | 방화벽 신청 변경. 화면은 9000 경유라 업무에는 영향 없음 | 1-3 |
| T63 | 앱 기동 실패 `NameNotFoundException` / `Failed to look up JNDI DataSource with name 'dsATMS01'` | 데이터소스가 그 서버를 대상으로 하지 않음 / export-name 이 다름 / 데이터소스 미등록 | 10-1 의 `dsATMS01` 검색, WAS 담당에 대상 서버·이름 확인 (WAS 요청서 2-8). 이름이 다르면 `jeus-common.yml` 의 `jndi-name` 한 줄 수정 → 재배포 | 10-1 |
| T65 | `/home/atmswas` 사용률 100% (Gitea push·Jenkins 빌드·배포 실패), `du` 로 보면 `~/atms/infra/gitea/data/backup` 이 수십 GB | 옛 `gitea-backup.service` 가 dump 를 Gitea 데이터 폴더 안에 만들어 **다음 dump 가 이전 dump 들을 통째로 담음** → 매일 불어남 (2026-11-04 ws01 22G) | 이 문서 맨 끝 "T65 상세": 타이머 정지 → 옛 dump 정리 → 새 서비스(데이터 폴더 밖 `~/atms/backup/gitea`, 7일) → 새 dump 확인 → 타이머 재개 | 7-3, 16-2 |
| T64 | JNDI 는 찾았는데 접속 실패 (`ORA-…`, Tibero 관련 클래스 오류) | 데이터소스 드라이버·URL·계정 설정 오류 (예: 예전 `iddsATMS01` 처럼 Tibero 드라이버) | WAS 담당에 Oracle 드라이버·`16.88.250.91:1531/NBATMSTP`·`ATMS_APP` 확인과 연결 시험 요청 | 13-3 |
| T66 | 개발자 PC 에서 빌드·실행할 때마다 DB 계정 잠김 (현업에 해제 요청), 또는 로컬 기동 시 `[DB-GUARD] …` / `ScannerException … TAB` | 계정이 공용 `local-common.yml` 하나에 있어 남의 계정·틀린 비번으로 반복 로그인 (`gradlew build` 의 `@SpringBootTest`, 기동 시 접속). 2026-11-05 수정 후에는 `D:\local-db.yml` 누락·오류·Tab 들여쓰기 | 각 PC `D:\local-db.yml` 에 본인 계정 (스페이스 들여쓰기). DB 테스트는 `-PdbTest` 일 때만. 실패 기록 `D:\db-login-failed` 는 비번을 고치면 자동 해제 (잠김 해제만 받은 경우 삭제) | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 12장 |
| T67 | 폐쇄망 PC 에서 `npm install node-forge@1.4.0 --offline` → `ENOTCACHED … only-if-cached` / `npm cache add` 가 `DirFetcher requires either a tree or an Arborist constructor` | `cache add <tgz>` 는 파일 내용만 넣어 이름 설치에 필요한 목록 정보가 없음 / 경로의 공백을 따옴표 없이 써서 폴더로 읽힘 | 앞으로는 tgz 대신 **npm 캐시 폴더째 반입** → 합친 뒤 평소처럼 `npm install 이름@버전 --offline` (json 자동 갱신). 이미 tgz 만 있으면 두 json 에 버전·integrity 를 손으로 적고 `npm install --offline`. 경로는 큰따옴표로 | [REQUIRED_FILES.md](REQUIRED_FILES.md) 3-5, [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 11-24 |

---

## 부록 A. 개념 상세 (후임자용)

### A-1. rootless Podman 과 subuid

- atmswas 같은 일반 계정이 root 없이 컨테이너를 실행하는 방식이다. 컨테이너 엔진 데몬이 없다
- 사용자 번호 대응:

  | 컨테이너 안 | 호스트 |
  |---|---|
  | uid 0 (root) | atmswas (50219) |
  | uid 1 ~ 65536 | `/etc/subuid` 의 atmswas 범위 (493216 부터) — 예: 1000 → 494215 |

- 그래서 Gitea(`git` = 1000)·Jenkins(`jenkins` = 1000)가 만든 파일은 호스트에서 494215 소유로 보인다. atmswas 가 직접 지우거나 바꿀 수 없다
  - `podman unshare <명령>` 은 atmswas 의 사용자 공간 안에서 root 로 실행하는 것이다. 그래서 이 파일들을 다룰 수 있다
- `--userns=keep-id`(컨테이너 1000 을 호스트 atmswas 로 맞추는 옵션)는 이 서버에서 실패한다. 그래서 데이터 폴더를 미리 `podman unshare chown -R 1000:1000` 한다
- 이미지·컨테이너 저장소: `~/.local/share/containers` (atmswas 디스크를 씀)

### A-2. linger · systemd --user · Quadlet · podman-restart

| 이름 | 무엇 |
|---|---|
| systemd --user | 사용자마다 따로 도는 서비스 관리자. `systemctl --user …` |
| linger | `loginctl enable-linger atmswas` (root) — 로그인하지 않아도 부팅 때 atmswas 의 systemd --user 가 시작되고, 로그아웃해도 남는다 |
| Quadlet | `~/.config/containers/systemd/atms-gitea.container` 같은 파일을 systemd 서비스(`atms-gitea.service`)로 만들어 준다 → Gitea 자동 기동·재시작 |
| podman-restart.service | 부팅 때 `--restart=always` 인 컨테이너를 다시 띄운다 → Jenkins |
| systemd 타이머 | crontab 대신 쓰는 예약 실행 (Gitea 매일 백업 02:30) |

### A-3. podman 네트워크 `atms-net`

- rootless 기본 네트워크(pasta)에서는 컨테이너가 서버 IP 를 자기 주소처럼 쓴다. 그래서 컨테이너가 `16.88.122.113:8090` 으로 보내면 자기 자신에게 가서 연결이 거부된다 (2026-10-22 확인)
- 사용자 네트워크 `atms-net`(DNS 켜짐)에 두 컨테이너를 넣으면 서로 이름으로 찾는다: `atms-gitea:3000`, `atms-jenkins:8080` (사설 주소 10.89.x)
- 컨테이너 → 호스트(ws01 sshd)는 `host.containers.internal` 로 간다. 호스트에는 `16.88.122.113` 에서 온 접속으로 보인다 (그래서 `from="16.88.122.113"`)

### A-4. JEUS 구조

```
/JEUS/jeusATMS (설치 1벌)
└─ 도메인 ATMS (설정 파일 domain.xml 하나)
   ├─ DAS (adminServer)  관리 전용 JVM, 8736 — 설정 보관, 서버 기동·정지 명령 (jeusadmin 이 여기 접속, 관리자 계정 필요)
   ├─ NodeManager        물리 서버마다 1개 — DAS 명령을 받아 서버 프로세스를 띄우고 감시
   └─ 매니지드 서버 ×6   앱을 실행하는 JVM — 각자 리스너(포트)·JVM 옵션·로그·WebtoB 커넥터
      └─ 앱 (exploded WAR 폴더, 컨텍스트 /)
```
- 로컬 `java -jar atms-journal.jar` 하나 = 매니지드 서버 하나 (실행 단위가 같다). 차이는 DAS·NodeManager 관리 층과 WAS 담당 운영 체계뿐이다
- 리스너: base(JEUS 내부·관리 통신) + HTTP(앱 요청). 우리 구성은 둘이 한 포트를 쓴다 → 외부에 열면 둘 다 열린다 (17-7)
- 서버 6개로 나눈 이유 (2026-10-26):
  - 모듈별 장애 격리·단독 재기동
  - 로컬과 같은 주소 체계
  - 게이트웨이(요청당 스레드 2개·성능 저하·교착 위험) 제거
  - 상세: [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md)

### A-5. WebtoB 와 JSV

- WebtoB 는 웹 서버다. 정적 파일(화면)을 직접 주고, `*URI` 에 맞는 요청은 JEUS 로 넘긴다
- 설정 `http.m` 은 텍스트다. `wscfl -i http.m` 으로 컴파일하고 재기동(`wsdown -i` → `wsboot`)해야 반영된다
- JSV 연결의 방향: **JEUS 서버가 WebtoB 의 `JSVPORT`(19000)로 접속**해 "나는 registration-id X" 로 등록한다 → WebtoB 는 `*URI` 가 가리키는 `*SERVER X` 에 붙은 연결로 요청을 보낸다
  - 그래서 방화벽은 ws → wb:19000 방향이다
  - JEUS 의 HTTP 리스너(8080 등)는 WebtoB 를 거치지 않는 직접 접속용이다
- `*URI` 는 경로 앞부분으로 맞춘다 → 구체적인 경로를 먼저 적는다

### A-6. L4 · VIP

- L4 스위치는 하나의 가상 주소(VIP)로 받은 연결을 여러 실제 서버로 나눈다
  - 헬스체크로 죽은 서버를 뺀다
  - 같은 출발지를 같은 서버로 보내는 세션 유지를 할 수 있다
- 에이전트는 접속할 서버 주소를 개국(open) 응답으로 받는다 (`agent.atms.host` 등) → VIP 를 `jeus.agent-entry.host` 에 넣는다

### A-7. jeus 프로파일과 WAR

- `-Pjeus` 로 빌드하면 다음이 바뀐다:
  - ① 내장 톰캣을 `WEB-INF/lib-provided` 로 뺀다 (배포 때 지움 — JEUS 가 서블릿 컨테이너)
  - ② `jeus/web.xml` 이 `WEB-INF/web.xml` 로 들어가 프로파일 `jeus` 를 켠다
- 모든 `Atms*Application` 이 `SpringBootServletInitializer` 를 상속한다 → JEUS 가 WAR 를 올릴 때 Spring 이 시작된다
- `-Pjeus` 없이 빌드하면 로컬 개발·실행(`bootRun`, `bootJar`)은 그대로다
- 설정 우선순위는 12-1

### A-8. 폴더 교체 배포 (`swap-deploy.sh`)

1. SHA256 확인
2. `<폴더>.new-<시각>` 에 풀기 (`unzip`, 없으면 python3)
3. `WEB-INF/lib-provided` 삭제
4. 기존 폴더의 은행 오류 페이지(`error.jsp`, `error/`)를 이어받기
5. 그룹·권한을 기존과 같게 (첫 배포면 상위 폴더 그룹 jeus, 2750)
6. 기존 폴더 → `~/deploy/backup/<폴더>-<시각>`, 새 폴더 → 원래 이름 (이름 바꾸기 두 번이라 순간적)
7. 백업 5개 유지, 이력 기록

- 기존 폴더 위에 덮어 풀지 않는 이유: 이전 버전에서 지워진 클래스·jar 가 남지 않게
- 교체한 뒤에는 그 서버를 재기동해야 새 버전이 돈다 (`deploy-war.sh restart`)

### A-9. 명령 고정 SSH 키와 jeusRstart

- `authorized_keys` 의 `command="스크립트"` 가 있으면 무엇을 요청하든 그 스크립트만 실행된다
  - 요청한 문자열은 `SSH_ORIGINAL_COMMAND` 로 들어온다 → 스크립트가 허용한 하위 명령만 처리하고 나머지는 `REJECT` 로 기록한다
  - `from=` 은 출발 IP 제한, `restrict` 는 터미널·포워딩 금지
- 파일은 SSH 표준입력으로 보낸다 (`ssh … "deploy <이름> <sha256>" < 파일`). scp 는 `command=` 와 함께 쓸 수 없어 쓰지 않는다
- `jeusRstart` (`/JEUS/jeusATMS/jeusMENU/`) 는 `root:sys -rwsr-xr-x` setuid 프로그램이다
  - 누구나 실행할 수 있고 root 로 동작한다
  - 표준입력으로 메뉴 번호를 받아 `su jeusatms -c "jeusadmin … boot|down <서버>"` 등을 실행한다
  - 계정 검사가 없으므로, 위험 메뉴(3 = jeusatms java 전부 kill, 7·8 = WebtoB) 차단은 우리 스크립트(`deploy-war.sh`)가 한다

### A-10. 권한 — setgid 폴더

- 다른 계정(jeusatms·webatms)이 우리 파일을 읽으려면 두 가지가 필요하다:
  - 파일에 그룹 읽기(r)
  - 모든 상위 폴더에 그룹 실행(x)
- atmswas 의 주 그룹이 `atms` 라서 그냥 만든 파일은 jeus 그룹이 아니다 → 폴더에 setgid(`chmod 2750`)를 주면 안에 생기는 파일·폴더가 jeus 그룹을 물려받는다
- 쓰기는 `data`(2770)만 연다. 앱 폴더는 읽기만

### A-11. 웹훅 → 빌드 연결

1. push → Gitea 가 `POST http://atms-jenkins:8080/git/notifyCommit?url=<저장소>&token=…`
2. Jenkins git 플러그인이 그 저장소 URL 을 쓰는 잡들에 "새 커밋 확인" 지시
3. 각 잡이 마지막 빌드의 커밋과 비교 → 바뀐 파일이 `includedRegions`(`Frontend/.*` 또는 `Backend/.*`)에 있으면 빌드

- `triggers { pollSCM('') }` 는 "일정 없는 폴링" — 정기 폴링은 하지 않고 웹훅이 올 때만 확인한다
- 잡을 한 번 실행해야 트리거와 비교 기준이 생긴다

### A-12. 오프라인 빌드 캐시

| 캐시 | 서버 위치 | 컨테이너 안 |
|---|---|---|
| Gradle 라이브러리 (`modules-2` — jar 와 색인 `metadata-2.*`) | `~/atms/infra/jenkins/cache/gradle/caches/modules-2` | `/cache/gradle/caches/modules-2` |
| Gradle 배포판 (`gradle-8.14.4-bin`) | `~/atms/infra/jenkins/cache/gradle/wrapper/dists` | `/cache/gradle/wrapper/dists` |
| npm (`_cacache`) | `~/atms/infra/jenkins/cache/npm` | `/cache/npm` |

- 빌드는 `--offline` 이라 여기 없는 것은 받을 수 없다 → 의존성이 바뀌면 4-4 → 8-4

---

## 부록 B. 결정 이력

| 날짜 | 결정 | 이유 | 문서 |
|---|---|---|---|
| 10-07 | 백엔드는 은행 기설치 JEUS 9.1 에 WAR, 화면은 WebtoB DocBase | 은행 표준. 컨테이너 실행은 대안(보류) | [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) |
| 10-07 | root 작업은 1일 권한 날에 모아서, 일상 운영은 rootless | root 상시 불가 | [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) |
| 10-08 | Git = Gitea (HTTP 3000, SSH 끔, SQLite), CI = Jenkins (8090), rootless Podman | 폐쇄망·root 없음·소규모 | [GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) |
| 10-08 | 이 저장소 브랜치는 `main` 하나 | 단순 운영 | — |
| 10-12 | 작업 계정 `atmswas`, 기준 `/home/atmswas` (`userATMS` 사용 안 함) | 환경구성표의 업무팀 계정 = 앱 파일 소유자 | [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) |
| 10-12 | 6개 모듈 모두 WAR, 설정은 WAR 안, DB 는 Spring Hikari | 반입·배포 단순화 | [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) |
| 10-12 | 반입은 Gitea·Jenkins 이미지·배포킷·체크섬만 (JRE·nginx·compose 보류) | 반입 최소화 | [REQUIRED_FILES.md](REQUIRED_FILES.md) |
| 10-14 | `--userns=keep-id` 대신 `podman unshare chown 1000` | 이 서버에서 keep-id 기동 실패 | 부록 A-1 |
| 10-16 | CPU 제한 보류, 메모리 제한만 | cpu 위임은 root·서버 전체 영향 | ROOT_DAY 2-9 |
| 10-21 | 화면은 Jenkins 가 SSH 로 wb01 에 배포 (명령 고정 키). wb01 타이머 방식 취소 | ws01·Jenkins → wb01:22 열림, 결과가 빌드에 바로 보임 | [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) |
| 10-22 | 빌드 시작은 Gitea 웹훅(notifyCommit)만 / Gitea·Jenkins 를 `atms-net` 에 | pasta 모드에서 서버 IP 로 서로 안 닿음 | 같은 문서 4-7 |
| 10-22 | 저장소 1개 + 웹훅 1개, 잡은 `PathRestriction` 으로 폴더별 | 관리 단순 | [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) |
| 10-23 | 백엔드 배포 단계 (Jenkins → ws01 SSH → 폴더 교체 → jeusRstart 재기동 → 확인 → 실패 시 자동 롤백). 웹훅 빌드는 배포 안 함 | 재기동 시점은 사람이 정함 | 같은 문서 6장 |
| 10-23 | 웹훅 POST | GET 은 414 | — |
| 10-23 | WebtoB `http.m` 은 root 작업일에 재기동까지 우리가 할 수 있음 | 담당 일정 의존 줄이기 | ROOT_DAY 2-13 |
| 10-26 | JEUS 도메인 1개 + 매니지드 서버 6개, 포트 8080~8085, 컨텍스트 `/`, 게이트웨이 제거 | 격리·단독 재기동·로컬과 같은 주소·게이트웨이 문제 해소 | [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md) |
| 10-26 | (11-02 에 JNDI 로 대체) DB 는 JNDI 대신 `jdbc:oracle:thin` 직접 (JEUS `iddsATMS01` 은 Tibero 드라이버 설정) + orai18n | 로컬 접속에 성공한 방식, 은행 문자셋 | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) |
| 10-27 | 서버 `atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01`, 폴더 `catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex`, base = HTTP 리스너 | JEUS 명명규칙, 원본 소스 적용이 쉬움 | [WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md) |
| 10-27 | 에이전트는 L4 → ws01 직접 (8081·8083·21001, http), 21001 소켓 서버 켬, base 노출은 WAS 담당 판단 | 확정 구성도 | 결정 문서 §11 |
| 10-30 | 2호기는 같은 도메인 이중화 전제. 배포킷의 2호기 대상 추가는 IP·접속 경로 확정 뒤 | — | 이 문서 14장 |
| 11-02 | DB 연결: 개발서버는 JEUS 데이터소스 JNDI `dsATMS01` (WAR 에 DB 비밀번호 없음, 대체 직접 접속 없음), 로컬은 `atms-common` `local-common.yml` 한 곳에 하드코딩 (WAR 에서 제외) | 은행 표준(데이터소스), 비밀번호를 WAR 밖으로 | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 개정 2026-11-02 |

(날짜는 모두 2026년)

---

## 부록 C. 문서 지도

| 문서 | 내용 | 이 문서와의 관계 |
|---|---|---|
| **INFRA_MANUAL.md** (이 문서) | 통합 매뉴얼 | 기준 |
| [POST_HANDOVER_EDITS.md](POST_HANDOVER_EDITS.md) | 현업 실제 값이 가정값과 다를 때 서버·소스에서 고치는 명령 (메뉴 번호·이름·포트·registration-id·JNDI·VIP), 암호화 키·node-forge 캐시 | 10·11·12장 보조 |
| [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) | 서버 셋팅 단계별 상세·진행 현황 | 6~8장의 원본 |
| [GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md), [GITEA_JENKINS_WALKTHROUGH.md](GITEA_JENKINS_WALKTHROUGH.md) | Gitea 설치·화면 설정·토큰·Jenkins 연동·웹훅 상세 | 7·9장의 원본 |
| [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) | 화면 빌드·배포 설계와 상세 흐름 | 9장 |
| [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) | 백엔드 빌드·배포·재기동 설계 | 9·10·13장 |
| [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md), [JEUS_MULTI_PORT_OPTIONS.md](JEUS_MULTI_PORT_OPTIONS.md) | 매니지드 서버 6개 결정 과정·비교·확정 구성도 | 1-1, 부록 A-4 |
| [WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md) | WAS·WebtoB 담당 요청서와 담당자용 가이드 (`http.m` 포함) | 5·11장 |
| [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) | root 작업일 목록·기록 | 6-2, 11-2 |
| [REQUIRED_FILES.md](REQUIRED_FILES.md) | 반입 목록, Jenkins 이미지 사양 | 4장 |
| [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) | 폐쇄망 소스 반영 (파일별 전문) | 12장 |
| [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) | 폴더·권한 설계 근거 | 1-5 |
| [DEV_SERVER_INFRA.md](DEV_SERVER_INFRA.md) | 인프라 구성도·포트표 (옛 구성 기록 일부 포함) | 1장 |
| [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md), [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) | **옛 구성**(서버 1개 + 컨텍스트 경로 + 게이트웨이) 시절 배포 가이드·소스 변경 설명. JEUS 확인(STEP 1)·대안(8장)은 여전히 참고 | 이력 |
| [JEUS_INFO_COLLECTION.md](JEUS_INFO_COLLECTION.md), [PIPELINE_SCRIPTS_EXPLAINED.md](PIPELINE_SCRIPTS_EXPLAINED.md) | JEUS 정보 수집 절차, 스크립트 설명 | 참고 |
| [JAR_PREVIEW_GUIDE.md](JAR_PREVIEW_GUIDE.md), [PROTOTYPE_QUICKSTART.md](PROTOTYPE_QUICKSTART.md), [SERVER_SETUP_GUIDE.md](SERVER_SETUP_GUIDE.md) | 임시·대안 경로, 초기 분석 | 이력 |
| [WORK_LOG.md](WORK_LOG.md) | 날짜별 작업 이력 | 이력 — 새 작업은 여기에 이어 적는다 |

이 문서를 고칠 때:
- 서버에서 새로 확인한 사실은 해당 절과 1-2 값표에 반영하고 `[확인 날짜]` 를 단다
- 새로 겪은 문제는 18장에 번호를 이어 붙인다
- 결정은 부록 B 와 [WORK_LOG.md](WORK_LOG.md) 에 남긴다


### T65 상세 — Gitea dump 가 불어나 `/home/atmswas` 가 가득 참 (2026-11-04)

확인 (읽기 전용):
```bash
df -h /home/atmswas
podman unshare du -sh ~/atms/infra/gitea/data/backup
podman unshare ls -lhtr ~/atms/infra/gitea/data/backup
```
- 파일 크기가 날마다 커지면 이 문제다. 작은 파일 하나로 안을 보면 이전 dump 가 들어 있다:
  `podman unshare tar -tzf ~/atms/infra/gitea/data/backup/<작은 파일> | grep gitea-dump-`

정리 — 순서대로. 원본 저장소(`~/atms/infra/gitea/data/git`)는 건드리지 않는다:
```bash
systemctl --user stop gitea-backup.timer
podman unshare ls -lhtr ~/atms/infra/gitea/data/backup
podman unshare find ~/atms/infra/gitea/data/backup -name "gitea-dump-*.tar.gz" ! -name "gitea-dump-<가장 최근 날짜>.tar.gz" -delete
df -h /home/atmswas
```
새 서비스 파일 (`~/atms/deploy/gitea/gitea-backup.service` 가 최신 배포킷이면 그것을 복사, 아니면 아래 내용을 그대로 입력):
```bash
cp ~/.config/systemd/user/gitea-backup.service ~/gitea-backup.service.bak-20261104
cp ~/atms/deploy/gitea/gitea-backup.service ~/.config/systemd/user/
grep ExecStart ~/.config/systemd/user/gitea-backup.service
systemctl --user daemon-reload
systemctl --user start gitea-backup.service
systemctl --user status gitea-backup.service --no-pager
ls -lh ~/atms/backup/gitea
tar -tzf ~/atms/backup/gitea/gitea-dump-*.tar.gz | grep -c gitea-dump-
```
- `grep ExecStart` 에 `-f -` 와 `~/atms/backup/gitea` 경로(`%h/atms/backup/gitea`)가 보여야 새 파일이다
- 마지막 줄이 `1` 이면 아직 데이터 폴더에 남은 옛 dump 1개가 새 dump 에 들어간 것이다 (2026-11-04 ws01 실제: 2.4G, 숫자 1). 새 dump 에도 현재 데이터가 다 들어 있으므로 옛 dump 를 지우고 한 번 더 만든다 (같은 날짜 이름은 덮어씀):
```bash
tar -tzvf ~/atms/backup/gitea/gitea-dump-*.tar.gz | grep gitea-dump-
podman unshare rm -f ~/atms/infra/gitea/data/backup/gitea-dump-*.tar.gz
systemctl --user start gitea-backup.service
ls -lh ~/atms/backup/gitea
tar -tzf ~/atms/backup/gitea/gitea-dump-*.tar.gz | grep -c gitea-dump-
```
- 숫자가 `0` 이고 크기가 크게 줄었으면 정상 → 타이머를 다시 켠다:
```bash
systemctl --user start gitea-backup.timer
systemctl --user list-timers | grep gitea
df -h /home/atmswas
curl -s http://127.0.0.1:3000/api/healthz
```
- 디스크가 가득 찼던 동안 Gitea·Jenkins 쓰기가 실패했을 수 있다: `podman logs --tail 50 atms-gitea`, PC 에서 push·Jenkins 빌드 한 번씩 시험
