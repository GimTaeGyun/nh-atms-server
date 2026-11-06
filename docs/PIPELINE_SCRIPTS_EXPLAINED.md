# 배포킷 스크립트 해설 (CI/CD · JEUS · WebtoB · 보류 중인 컨테이너 경로)

> 개정 2026-11-03 — 현재 구성 기준으로 다시 씀: JEUS 도메인 1개 + **매니지드 서버 6개**(모듈마다 JVM·포트 하나, 컨텍스트 `/`),
> DB 는 JEUS JNDI `dsATMS01`, 게이트웨이 없음(WebtoB 가 경로별로 각 서버에 JSV 연결).
> 파일마다 **역할 · 누가/언제 실행 · 주요 단계 · 주의점** 만 짧게 적는다. 절차는 [INFRA_MANUAL.md](INFRA_MANUAL.md),
> 결정 배경은 [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) · [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md).
> 스크립트가 바뀌면 이 문서도 같이 고친다.

| 모듈 | 서버 | 포트 | 배포 폴더 (`/home/atmswas/…`) | 설정 키 |
|---|---|---|---|---|
| atms-web | atms01 | 8080 | `catms` | `web` |
| atms-journal | ajou01 | 8081 | `cajou` | `journal` |
| atms-batch | abat01 | 8082 | `cabat` | `batch` |
| atms-file | file01 | 8083 | `cfile` | `file` |
| atms-if-internal | ifin01 | 8084 | `cifin` | `ifin` |
| atms-if-external | ifex01 | 8085 | `cifex` | `ifex` |

---

## 1. 전체 흐름

```
PC git push → Gitea(atms-gitea:3000) ─웹훅 1개(POST)→ Jenkins(atms-jenkins:8080) notifyCommit
                                                   │  (podman 네트워크 atms-net, 컨테이너 이름으로 통신)
              ┌────────────────────────────────────┴───────────────────────────────┐
  nh-atms-frontend (Frontend/ 바뀔 때)                       nh-atms-backend (Backend/ 바뀔 때)
  npm 빌드 → tar.gz (기본: 바로 배포)                          bootWar -Pjeus → WAR 6개 (기본: 빌드만)
      │ ssh wb01-deploy-key                                       │ ssh ws01-deploy-key (사람이 배포 MODE 를 고를 때)
      ▼                                                           ▼
  wb01 deploy-front.sh ─ swap-deploy.sh → ~/docs             ws01 deploy-war.sh ─ swap-deploy.sh → ~/catms·cajou·…
  / 200 + index.html 일치, 실패 시 자동 롤백                   restart: 모듈 서버만 jeusRstart 정지→기동 → /v3/api-docs 200
                                                              실패 시 그 모듈만 자동 롤백·재기동
```

공통 설계 3가지
1. **웹훅 1개 + 폴더 필터** — 두 잡 모두 깨우고, `PathRestriction` 으로 자기 폴더가 바뀐 push 만 빌드
2. **SSH 고정 명령** — 배포 키는 `restrict,from=…,command=<스크립트>` 로 묶여 그 스크립트만 실행. 파일은 ssh 표준입력으로 전달 (scp 불가)
3. **새 폴더에 풀고 mv 로 교체** — 덮어 풀지 않음, 직전 폴더가 곧 백업(5개), 롤백 = mv

---

## 2. Jenkins · Gitea (ws01 컨테이너 기반)

### `deploy/gitea/install-gitea.sh <서버IP>` — Gitea 설치·기동
- **언제**: ws01 에서 atmswas 로 처음 한 번, IP·설정이 바뀔 때 다시 (재실행 안전)
- **하는 일**: 이미지 확인 → `~/atms/infra/gitea/{data,config}` 를 컨테이너 git(1000) 소유로(`podman unshare chown`) → `gitea.env`(600, 비밀키 자동 생성) → `atms-net` 네트워크에 붙여 기동(linger 있으면 Quadlet) → `/api/healthz` 확인
- **주의**: `--userns=keep-id` 는 이 서버에서 실패해서 안 씀. 자원 제한은 memory 만(CPU 보류). 관리자 계정 생성은 출력되는 명령으로 따로

### `deploy/jenkins/run-jenkins.sh` (+ `jenkins.env.example`) — Jenkins 컨테이너 생성·재생성
- **언제**: 처음, 이미지 교체·설정 변경 때 (데이터·캐시는 그대로, 컨테이너만 새로)
- **하는 일**: 이미지 확인 → `home`, `cache/gradle`, `cache/npm` 중 1000 소유가 아닌 파일이 있으면 chown → `jenkins.env` 없으면 만들고 `CHANGE_ME` 남아 있으면 멈춤 → `atms-net`, 포트 8090, `--restart=always`, memory 2g → linger 면 `podman-restart` 활성화 → `/login` 200 대기
- **주의**: 관리자 비밀번호는 `jenkins.env` 에서 바꾼 뒤 다시 실행. env 파일은 따옴표 없이

### `pipelines/frontend.Jenkinsfile` — 잡 `nh-atms-frontend`
- **언제**: Gitea 웹훅(`Frontend/` 변경) 또는 수동. 잡 화면 "Pipeline script" 에 붙여 넣어 씀
- **단계**: 소스(항상, 비교 기준 유지) → `npm ci --offline` + `npm run build` → `nh-atms-web-dist-<날짜>-<빌드>.tar.gz` + sha256 보관(10개) → `ssh atmswas@16.88.122.112 "deploy …" < tar.gz`
- **MODE**: `build-deploy`(기본) / `build-only` / `redeploy`(보관본 재배포) / `rollback` / `status`
- **주의**: 웹훅 빌드가 바로 화면에 반영됨. npm 캐시에 없는 패키지가 추가되면 빌드 실패 → 캐시 재반입

### `pipelines/backend.Jenkinsfile` — 잡 `nh-atms-backend`
- **언제**: 웹훅(`Backend/` 변경)은 빌드만. 배포는 사람이 MODE 를 골라 실행
- **단계**: 소스 → `gradlew bootWar -Pjeus --offline`(메모리 아끼는 옵션) → `<모듈>-0.0.1-<날짜>-<빌드>.war` + sha256 보관 → WAR 마다 `ssh … "deploy …" < war` → `RESTART=true` 면 `restart --rollback <교체한 모듈>`
- **MODE**: `build-only`(기본) / `build-deploy` / `redeploy` / `rollback`(모듈 하나) / `restart` / `status`, `MODULE` = all 또는 하나
- **결과 색**: 성공 초록 / 재기동 메뉴가 없는 모듈이 있으면 **UNSTABLE 노랑**(교체는 됨, WAS 담당 재기동 필요 = 종료코드 4) / 그 밖은 빨강
- **주의**: WAR 파일명이 ws01 쪽에서 모듈·폴더를 정하는 근거라 이름 규칙을 바꾸지 않는다. 접속 주소 `WS01_SSH=atmswas@host.containers.internal`

---

## 3. 백엔드 배포 (ws01, `deploy/jeus/`)

### `install-deploy-war.sh [공개키 [출발IP]]` — ws01 배포 준비
- **언제**: 배포킷 갱신 뒤, 키 등록·`from=` 고정 때 (재실행 안전)
- **하는 일**: `deploy-war.sh`, `swap-deploy.sh` CRLF 제거·700·문법 검사 → `~/deploy`, `~/atms/release` 생성 → `~/deploy/deploy-war.env` 없으면 예시 복사(있으면 유지) → 홈이 `jeus 750` 인지 확인만 → 공개키를 주면 `authorized_keys` 에 `restrict[,from=],command=…deploy-war.sh` 한 줄(같은 키 줄은 교체)
- **주의**: ws01 셸에서 `ssh …` 시험은 HIWARE 금지 명령 → 키 동작은 Jenkins `MODE=status` 로 확인. 출발IP 는 `16.88.122.113` (2026-10-23 확인)

### `deploy-war.env.example` → `~/deploy/deploy-war.env` — 서버별 설정
- **내용**: `JEUS_HOST`(16.88.122.113, 127.0.0.1 안 됨), 모듈별 `PORT_*`·`SERVER_*`, **재기동 메뉴 `STOP_MENU_<키>`·`BOOT_MENU_<키>`**, 대기 시간, `HEALTH_SKIP`
- **주의**: 메뉴 번호는 WAS 담당이 서버 6개용 jeusRstart 메뉴를 추가해 준 뒤 채움 (예전 5·6 은 옛 `nbatmsltws01_atms01` 용이라 무시·경고). 2호기(ws02)는 자기 서버의 값으로 따로 둠

### `deploy-war.sh` — SSH 고정 명령 (Jenkins 가 부름, ws01 에서 직접도 가능)
- **명령 4개만**: `deploy <WAR> <sha256> [커밋] < WAR` / `rollback <모듈>` / `status [모듈]` / `restart [--rollback] [모듈…]`. 그 외는 REJECT 기록
- **deploy**: 파일명 검사로 모듈 판별 → `release/<모듈>/` 에 300MB 제한·sha256 확인하며 수신(5개 보관) → `swap-deploy.sh` 로 폴더 교체 (재기동은 안 함)
- **restart**: 모듈마다 **그 서버만** 차례로 — jeusRstart 에 정지 메뉴 → `http://16.88.122.113:<포트>/` 무응답 확인(180초) → 기동 메뉴 → `/v3/api-docs` 200 대기(600초). `--rollback` 이면 실패한 모듈만 직전 백업으로 돌려 다시 재기동
- **안전장치**: 메뉴는 숫자만, **3(java 전부 kill)·7·8(WebtoB) 거부**, 메뉴 없는 모듈은 건너뛰고 종료코드 4, `flock` 으로 동시 실행 방지, 기록 `~/deploy/war-deploy.log`
- **주의**: 기동 확인을 `/actuator/health` 가 아닌 `/v3/api-docs` 로 하는 이유 — health 는 DB 가 안 되면 503 이라 정상 기동도 롤백됨

### `swap-deploy.sh` — 교체 엔진 (ws01·wb01 공용)
- **명령**: `deploy <war|zip|tar.gz> [대상]` / `rollback <대상|모듈>` / `list <대상|모듈>`
- **deploy**: WAR 이름으로 폴더 결정(위 표) → `<대상>.new-<시각>` 에 풀기 → `WEB-INF/lib-provided`(내장 톰캣) 삭제 → 은행 에러페이지(`error.jsp`, `error/`) 이어받기 → 그룹·권한 맞춤(첫 배포는 jeus·2750) → 기존 폴더를 `~/deploy/backup/` 으로 mv, 새 폴더를 mv → 백업 5개 유지 → WAR 의 `spring.profiles.active` 출력
- **rollback**: 최신 백업을 되돌리고 현재 것은 `<이름>.failed-<시각>` 으로
- **주의**: `.failed-*` 는 자동 정리 안 됨(가끔 디스크 확인). 롤백 두 번 = 두 단계 전. 재기동은 하지 않음

---

## 4. 화면 배포 (wb01, `deploy/webtob/`)

### `install-wb01.sh [공개키 [출발IP]]` — wb01 준비
- **언제**: wb01 에 배포킷을 `~/deploy/kit` 에 올린 뒤 (재실행 안전)
- **하는 일**: `~/deploy/{bin,inbox,backup}` → `deploy-front.sh`·`swap-deploy.sh` 를 `~/deploy/bin` 에 설치 → 명령 확인 → 홈 `jeus 750`, DocBase `~/docs` `jeus 2750` → 공개키 등록(`from=16.88.122.113`)
- **주의**: 배포킷을 `~/docs` 아래 두면 9000 으로 내려받아짐

### `deploy-front.sh` — SSH 고정 명령 (wb01)
- **명령 3개**: `deploy <버전> <sha256> [커밋] < tar.gz` / `rollback` / `status`
- **deploy**: 형식 검사 → 200MB 제한·sha256 확인하며 `inbox` 에 수신 → `swap-deploy.sh` 로 `~/docs` 교체 → **`/` 200 이고 실제 서비스되는 index.html 이 새 파일과 같아야 성공**, 아니면 자동 롤백·종료코드 1
- **주의**: WebtoB 재기동 불필요(정적 파일). 기록 `~/deploy/front-deploy.log`, 현재 버전 `front-current.txt`

### `http.m.atms-jsv.txt` — 스크립트가 아닌 붙여 넣기용 설정 조각
- WebtoB `http.m` 에 넣을 `*SERVER`(서버 6개 = JEUS 커넥터 registration-id), `*URI`(경로별 라우팅: `/api/journal/` → ajou01 … 나머지 `/api/` → atms01) 블록. 적용은 WebtoB 담당(webatms)
- 구체적인 경로를 위에 두고, DOCROOT·기존 URI 는 건드리지 않는다

---

## 5. `bin/` — 컨테이너 대체 경로 (보류 중, JEUS 가 안 될 때만)

모두 `source bin/common.sh <앱>` 으로 `conf/atms.env` + `conf/apps/<앱>.env` 를 읽는다. **앱 이름 = 컨테이너·이미지·로그·설정 폴더 이름.** 현재 실제 배포에는 쓰지 않는다.

| 파일 | 역할 |
|---|---|
| `common.sh` | 설정 로더, 위임된 cgroup 컨트롤러만 `--memory`/`--cpus` 로 붙이는 `limit_args` |
| `init-dirs.sh` | 처음 한 번: `*.example` → 실제 env 복사(덮어쓰지 않음), 앱별 logs/config 폴더 |
| `precheck.sh` | 서버 사전 점검(읽기 전용, sudo 안 씀) |
| `load-image.sh` | 반입한 이미지 tar `podman load` |
| `build.sh` | `localhost/<앱>:<시각>` 이미지 빌드(`--pull=never`), 마지막 줄에 태그 출력 |
| `deploy.sh` | 빌드 → current/previous 태그 → 재기동 → 헬스체크 → 실패 시 `rollback.sh` |
| `start.sh` / `stop.sh` | Quadlet 이 있으면 `systemctl --user`, 없으면 `podman run/stop` |
| `healthcheck.sh` | `HEALTH_PATH` 가 2xx/3xx 면 성공 |
| `rollback.sh` | 태그만 바꿔(기본 previous) 다시 기동 |
| `status.sh` / `logs.sh` | 상태 요약 / 컨테이너·파일 로그 |
| `install-quadlet.sh` / `uninstall-quadlet.sh` | systemd --user 자동기동 등록·해제 (linger 필요) |
| `cleanup-images.sh` | current/previous + 최근 N 개 외 이미지 정리 |
| `direct.sh` | 컨테이너도 없이 `~/atms/jdk` 로 `java -jar` 직접 실행 (최후 수단) |

---

## 6. 로그·기록 위치

| 서버 | 경로 | 내용 |
|---|---|---|
| ws01 | `~/deploy/war-deploy.log` | WAR deploy/rollback/restart 결과 (OK·PARTIAL·FAIL·REJECT, 호출자) |
| ws01 | `~/atms/release/<모듈>/` | 받은 WAR (모듈별 5개) |
| ws01 | `~/deploy/deploy-war.env` | 포트·서버·재기동 메뉴 설정 |
| wb01 | `~/deploy/front-deploy.log`, `front-current.txt`, `inbox/` | 화면 배포 기록·현재 버전·받은 tar.gz |
| 공통 | `~/deploy/backup/`, `deploy-history.log` | 교체 전 폴더(5개), swap 이력 |
| ws01 | `~/atms/infra/{gitea,jenkins}` | Gitea·Jenkins 데이터, Gradle·npm 캐시 |

## 7. 꼭 기억할 주의점
1. ws01 셸에서 `ssh`·`sudo`·`su` 는 HIWARE 금지 명령 (3회면 세션 잠김)
2. jeusRstart 메뉴 3 은 DAS 까지 죽인다 — 스크립트가 거부하지만 손으로도 절대 입력하지 않는다
3. 8080~8085 는 `16.88.122.113` 으로만 접속된다 (127.0.0.1 안 됨)
4. 재기동 메뉴가 비어 있으면 교체만 되고 반영은 WAS 담당 재기동 후 (Jenkins 노랑)
5. 수동 "Build with Parameters" 는 폴더 필터와 관계없이 빌드된다
