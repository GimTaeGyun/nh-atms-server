# 백엔드 WAR 빌드 + Jenkins·Gitea 설정 순서 (프론트·백엔드 공통)

작성일: 2026-10-22 · 서버 `nbatmsltws01`(113) · Jenkins `atms-jenkins`(8090) · Gitea `atms-gitea`(3000)

> 결정 (2026-10-22)
> - 저장소는 `NH-ATMS/nh-atms` 하나 (Frontend·Backend 함께). **웹훅 1개**, 잡이 바뀐 폴더로 나눠 빌드: `Frontend/` → `nh-atms-frontend`, `Backend/` → `nh-atms-backend` (Jenkinsfile 의 `PathRestriction`)
> - 백엔드 잡은 **기본 빌드·보관만** (`bootWar -Pjeus` → WAR 6개 + sha256, 웹훅 빌드도 배포 안 함)
> - (2026-10-23, 재기동 가능 전제로 변경) **배포 후 자동 재기동**: `build-deploy`·`redeploy`·`rollback` 뒤 `RESTART=true`(기본)면 `deploy-war.sh restart` 가 `jeusRstart` 메뉴(정지 → 5 기동)로 atms01 재기동 → 모듈별 기동 확인 → 실패 시 교체한 모듈 자동 롤백·재기동. `MODE=restart` 추가. 웹훅 빌드는 여전히 배포 안 함 (6-8)
> - (2026-10-23, JEUS 복구·atms01 기동 확인 후) **배포 단계 추가**: `MODE=build-deploy`/`redeploy`/`rollback`/`status` 를 사람이 고를 때만 Jenkins 컨테이너 → ws01 호스트 SSH(배포 전용 키, `deploy-war.sh` 고정) → `catms*` 교체. **JEUS 재기동은 WAS 담당** (6장)
> - Jenkins·Gitea 화면 설정과 산출물 확인은 **PC → 113:3000·8090 방화벽 개방 후 PC 브라우저**에서 한다
>
> 프론트 배포(SSH → wb01) 세부: [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) · 캐시: [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) 9-5

## 1. 흐름

```
개발 PC ─git push─▶ Gitea (NH-ATMS/nh-atms)
                      │ 웹훅 1개: POST http://atms-jenkins:8080/git/notifyCommit?url=http://atms-gitea:3000/NH-ATMS/nh-atms.git&token=…
                      ▼
                   Jenkins: url= 이 같은 잡 2개가 각자 새 커밋 확인
                      ├─ nh-atms-frontend : Frontend/ 가 바뀌었으면 → npm ci --offline → build → wb01 배포
                      └─ nh-atms-backend  : Backend/ 가 바뀌었으면 → gradlew bootWar -Pjeus --offline → WAR 6개 보관
                                              (사람이 MODE=build-deploy 로 돌리면) → ssh atmswas@ws01 "deploy …" < WAR
                                                 → deploy-war.sh → swap-deploy.sh → /home/atmswas/catms* 교체 → WAS 담당 jeusRstart
```

## 2. 파일

| 파일 | 내용 |
|---|---|
| `deploy/jenkins/pipelines/frontend.Jenkinsfile` | 화면 빌드·배포. 소스 받기에 `PathRestriction(Frontend/.*)` |
| `deploy/jenkins/pipelines/backend.Jenkinsfile` | WAR 빌드·배포. `PathRestriction(Backend/.*)`, `MODE`(기본 build-only), `MODULE`(all 또는 하나), `REDEPLOY_BUILD`, `WS01_SSH`(기본 `atmswas@host.containers.internal`). 산출물 `<모듈>-0.0.1-<YYYYMMDD>-<빌드번호>.war` + `.sha256` (swap-deploy.sh 가 이름으로 배포 폴더를 고름), 최근 10개 빌드 보관 |
| `deploy/jeus/deploy-war.sh` | ws01 SSH 고정 명령 (2026-10-23). `deploy <WAR파일명> <sha256> [커밋] < WAR` / `rollback <모듈>` / `status [모듈]` 만 받음. 받은 WAR 는 `~/atms/release/<모듈>/` (모듈별 5개), 기록 `~/deploy/war-deploy.log` |
| `deploy/jeus/install-deploy-war.sh` | ws01 준비·공개키 등록 (`restrict[,from=…],command=…/deploy-war.sh`) |
| `deploy/jeus/swap-deploy.sh` | 실제 교체 (새 폴더에 풀기 → `lib-provided` 삭제 → `error.jsp` 유지 → 교체, 백업 5개). 2026-10-23: 첫 배포 때 폴더 그룹을 상위(`/home/atmswas` = jeus)에서 받아 2750 으로 만듦 |

백엔드 빌드 옵션의 이유:
- `--offline` + `GRADLE_USER_HOME=/cache/gradle`: 폐쇄망, offline-seed 캐시 사용 (Gradle 8.14.4)
- `-Porg.gradle.java.installations.paths=$JAVA_HOME`, `auto-download=false`: `build.gradle` 이 Temurin 21 을 요구 — Jenkins 이미지의 JDK(`/opt/java/openjdk`)가 Temurin 21 이라 그대로 쓴다
- `-Dorg.gradle.jvmargs=-Xmx768m`, `--max-workers=1`, `parallel=false`: 소스의 `gradle.properties`(-Xmx2g, parallel) 는 컨테이너 메모리 2g(Jenkins 1g 포함)에 과하다
- `sh ./gradlew`: Windows 에서 push 해 실행 권한이 빠져도 동작

## 3. 설정 순서

> 화면 항목별 상세(토큰 → 첫 push → 팀원 clone → Jenkins Credentials·잡 → 웹훅): [GITEA_JENKINS_WALKTHROUGH.md](GITEA_JENKINS_WALKTHROUGH.md) (방화벽 개방 후, PC 브라우저)

| # | 작업 | 어디서 | 확인 |
|---|---|---|---|
| 1 | 소스 첫 push (`NH-ATMS/nh-atms`, 최상위에 `Frontend/`·`Backend/`) | PC git | Gitea 화면에 두 폴더가 보임 ([GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) 4장) |
| 2 | `jenkins-bot` 계정 + 토큰(저장소 읽기), 저장소에 읽기 권한 | Gitea 사이트 관리 / 저장소 설정 → 협업자 | — |
| 3 | Credentials `gitea-jenkins-bot` (Username with password: jenkins-bot / 토큰) | Jenkins 관리 → Credentials → System → Global | — |
| 4 | (프론트 배포용) Credentials `wb01-deploy-key` | 같은 곳 | [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) B-3·B-4 |
| 4-1 | (백엔드 배포용) Credentials `ws01-deploy-key` | 같은 곳 | 6-5 (배포를 쓰기 전까지는 없어도 build-only 는 동작) |
| 5 | 잡 `nh-atms-frontend`: 새 Item → Pipeline → "Pipeline script" 에 `frontend.Jenkinsfile` 붙여 넣기 | Jenkins | 저장 |
| 6 | 잡 `nh-atms-backend`: 같은 방법으로 `backend.Jenkinsfile` | Jenkins | 저장 |
| 7 | 첫 실행 (매개변수·트리거 등록): 프론트 `MODE=status` 또는 `build-only`, 백엔드 `MODULE=all` | 각 잡 "Build with Parameters" | 백엔드 콘솔 `BUILD SUCCESSFUL`, 빌드 화면 "Build Artifacts" 에 WAR 6개 + sha256 |
| 8 | notifyCommit 토큰 `gitea-webhook` 생성 (값 한 번만 보임) | Jenkins 관리 → Security → "Git plugin notifyCommit access tokens" | — |
| 9 | Gitea 웹훅 **1개**: URL `http://atms-jenkins:8080/git/notifyCommit?url=http://atms-gitea:3000/NH-ATMS/nh-atms.git&token=<8번 토큰>`, 방법 **POST** (GET 은 414 URI Too Long, 2026-10-23), Push 이벤트, 브랜치 `main` | Gitea 저장소 → 설정 → Webhooks → Gitea | "테스트 전송" 응답 200, 본문에 `Scheduled polling of nh-atms-frontend`, `... nh-atms-backend` 두 잡 |
| 10 | 동작 확인: `Frontend/` 만 고쳐 push → 프론트 잡만, `Backend/` 만 고쳐 push → 백엔드 잡만 빌드 | PC git → Jenkins | 다른 잡은 폴링 기록에 "No changes" |

## 4. PC 에서 산출물 확인

- Jenkins 백엔드 빌드 화면 → Build Artifacts → WAR 와 `.sha256` 내려받기
- PC (PowerShell): `Get-FileHash .\atms-web-0.0.1-<..>.war -Algorithm SHA256` 와 `.sha256` 값 비교
- (선택) PC 에서 기동 시험: `java -jar atms-web-0.0.1-<..>.war` — bootWar 는 내장 톰캣을 `WEB-INF/lib-provided` 에 담고 있어 `java -jar` 로도 뜬다 (이때는 web.xml 이 쓰이지 않아 프로파일은 기본 `local`). DB 가 없으면 기동·스웨거(`/swagger-ui.html`)까지만 확인

## 5. 문제 해결

| 증상 | 원인·대처 |
|---|---|
| `Could not resolve ...` / `No cached version ... available for offline mode` | offline-seed 에 없는 라이브러리 → 인터넷 PC 에서 캐시 재생성·재반입 (9-5) |
| `Cannot find a Java installation ... vendor=Eclipse Temurin` | `java -version` 콘솔 확인. Temurin 이 아니면 `-Porg.gradle.java.installations.paths` 를 실제 JDK 경로로 |
| 빌드 중 컨테이너가 죽음 / 콘솔이 끊기고 exit 137 | 메모리 부족 → `JENKINS_MEMORY=3g ~/atms/deploy/jenkins/run-jenkins.sh` 로 다시 만들기 (데이터 유지) |
| 웹훅 후 두 잡 모두 빌드 / 둘 다 안 됨 | 첫 실행(7번)을 안 했으면 트리거 미등록. `url=` 이 Jenkinsfile `GITEA_REPO` 와 다르면 잡을 못 찾음 |
| `permission denied: ./gradlew` | `sh ./gradlew` 로 바뀌었는지 (이 Jenkinsfile 은 이미 `sh` 사용) |

## 6. WAR 배포 (2026-10-23 추가 — JEUS 복구·atms01 기동 후)

원칙
- 웹훅 빌드는 **배포하지 않는다** (`MODE` 기본 `build-only`). 재기동이 6개 모듈을 모두 멈추므로, 배포는 사람이 시점을 정해 `MODE=build-deploy` 또는 `redeploy` 로 한다
- `deploy-war.sh deploy` 는 파일 교체까지만, 반영은 같은 스크립트의 `restart` (6-8). Jenkins 는 교체 직후 자동으로 `restart --rollback <교체한 모듈>` 을 부른다 (`RESTART=false` 면 교체만). 한 모듈만 바꿔도 6개가 모두 재기동된다
- 배포 키로 할 수 있는 일은 `deploy`·`rollback`·`status`·`restart` 4가지뿐 (셸·다른 명령 불가, `REJECT` 기록)

### 6-0. 먼저 확인 (WAS 담당·서버)

| 항목 | 방법 | 왜 |
|---|---|---|
| JEUS 경로·서버명·포트가 예전과 같은지 | [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 1-1·1-4 (`domain.xml`: `nbatmsltws01_atms01`, 리스너 8766, WebtoB 커넥터 19000) | 재설치로 바뀌었으면 `jeus.base-url`(8766)·`JEUS_URL` 수정 |
| atms01 에 앱 6개가 **`/home/atmswas/catms*` exploded 폴더·컨텍스트 경로**로 등록됐는지 | 같은 STEP 1-4, 아니면 WAS 담당에 [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 7-3 요청 | 등록 안 됐으면 폴더를 바꿔도 JEUS 가 읽지 않음 |
| Jakarta 지원 | STEP 1-3 | 미지원이면 배포해도 404 (STEP 8) |
| 내부 Gitea 사본의 `jeus-common.yml` `CHANGE_ME` 기입 | DB·JWT 값 (GitHub 에는 올리지 않음) | 없으면 기동 실패 (atms-batch 는 DB 없으면 배포 실패) |
| 홈 `/home/atmswas` = `atmswas:jeus` 750 | `ls -ld ~` | jeusatms 가 jeus 그룹으로 catms* 를 읽음 |

### 6-1. ws01 준비 (atmswas)

2026-10-23 배포킷을 `~/atms` 에 반입한 뒤 ([DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) "배포킷 갱신"):
```bash
~/atms/deploy/jeus/install-deploy-war.sh
~/atms/deploy/jeus/deploy-war.sh status
```
- 스크립트 실행 권한·CRLF·문법, `flock`·`curl`·`sha256sum`·`unzip`(없으면 python3) 확인, `~/deploy`(700)·`~/atms/release` 생성, 홈 권한 점검
- `status` → 모듈별 `응답 <코드>` (`http://127.0.0.1:8766<컨텍스트>/`). atms01 이 떠 있으면 `000` 이 아닌 값 (404·302 등도 "JEUS 가 응답함")

### 6-2. 배포 키 만들기·등록 (ws01, atmswas)

같은 서버라 공개키를 옮길 필요가 없다.
```bash
mkdir -p ~/atms/infra/jenkins/keys
chmod 700 ~/atms/infra/jenkins/keys
ssh-keygen -t ed25519 -N "" -C jenkins-ws01-deploy -f ~/atms/infra/jenkins/keys/ws01-deploy
~/atms/deploy/jeus/install-deploy-war.sh ~/atms/infra/jenkins/keys/ws01-deploy.pub
ssh -i ~/atms/infra/jenkins/keys/ws01-deploy -o BatchMode=yes -o StrictHostKeyChecking=accept-new atmswas@127.0.0.1 status
ssh -i ~/atms/infra/jenkins/keys/ws01-deploy -o BatchMode=yes atmswas@127.0.0.1 id
```
- 처음에는 출발 IP(`from=`) 없이 등록된다 (Jenkins 컨테이너의 접속이 어느 IP 로 보일지 6-6 에서 확인)
- ⚠ **2026-10-23: ws01 의 HIWARE 가 `ssh …` 로 시작하는 명령을 금지 명령으로 막는다** (금지 입력 카운트 증가, 3회면 세션 잠김) → 위 `ssh -i …` 두 줄은 **입력하지 않는다**. 키 등록은 `install-deploy-war.sh` 출력의 `restrict,command=…deploy-war.sh` 줄로 확인하고, 키 동작은 Jenkins `MODE=status` 로 확인한다 (컨테이너 안의 ssh 는 HIWARE 입력이 아님). `ssh-keygen` 은 허용됨
- (참고, HIWARE 가 없을 때) `status` → `호출 : ssh:127.0.0.1` 과 모듈 목록이 나오면 성공. `id` → `사용법: …` 이면 정상 (키로는 다른 명령 불가)
- `Permission denied (publickey…)` → `ls -Zd ~/.ssh ~/.ssh/authorized_keys` (700/600, `ssh_home_t`), 그래도 같으면 sshd 가 atmswas 의 키 접속을 막는 설정일 수 있음 → 서버 담당 확인

### 6-3. Jenkins 컨테이너 → ws01 호스트 sshd 접속 확인 (ws01, atmswas)

Jenkins 는 rootless podman 네트워크(`atms-net`) 안에 있어, 서버 자기 IP(16.88.122.113)로는 닿지 않을 수 있다 (2026-10-22 Gitea → 113:8090 실패와 같은 이유). 차례로 시험해 **OPEN 이 나온 첫 주소**를 쓴다:
```bash
podman exec atms-jenkins getent hosts host.containers.internal
podman exec atms-jenkins bash -c "timeout 5 bash -c '</dev/tcp/host.containers.internal/22' && echo OPEN || echo FAIL"
podman exec atms-jenkins bash -c "timeout 5 bash -c '</dev/tcp/16.88.122.113/22' && echo OPEN || echo FAIL"
podman exec atms-jenkins bash -c "timeout 5 bash -c '</dev/tcp/16.89.122.86/22' && echo OPEN || echo FAIL"
```
- `host.containers.internal` 이 OPEN 이면 Jenkinsfile 그대로 (`WS01_SSH = 'atmswas@host.containers.internal'`)
- 아니면 OPEN 이 나온 IP 로 Jenkinsfile 의 `WS01_SSH` 를 바꿔 잡에 다시 붙여 넣는다 (16.89.122.86 등 ws01 의 다른 주소는 컨테이너 쪽에 복사되지 않아 호스트로 나간다 — 추정, 시험으로 판정)
- 모두 FAIL 이면 결과를 공유 (대안: Jenkins 산출물을 내려받아 STEP 3·4 수동 배포)

known_hosts (위에서 고른 주소로, 예: `host.containers.internal`):
```bash
podman exec atms-jenkins sh -c "mkdir -p ~/.ssh && chmod 700 ~/.ssh && ssh-keyscan -t ed25519 host.containers.internal >> ~/.ssh/known_hosts"
podman exec atms-jenkins ssh-keygen -lf /var/jenkins_home/.ssh/known_hosts
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```
- 두 번째 명령 결과 중 그 주소 줄의 `SHA256:…` 이 세 번째(ws01 호스트 키)와 같아야 한다 (wb01 줄은 따로 있음)

### 6-4. (방화벽 개방 후, PC 브라우저) Credentials `ws01-deploy-key`

Jenkins 관리 → Credentials → System → Global → Add: 종류 **SSH Username with private key**, ID `ws01-deploy-key`, Username `atmswas`, Private Key "Enter directly" 에 ws01 에서 `cat ~/atms/infra/jenkins/keys/ws01-deploy` 한 내용 (`-----BEGIN` ~ `END-----` 전부). 등록 후 ws01 의 개인키 파일 삭제:
```bash
rm ~/atms/infra/jenkins/keys/ws01-deploy
```

### 6-5. 잡 갱신

`nh-atms-backend` 잡의 Pipeline script 를 2026-10-23 판 `backend.Jenkinsfile` 로 바꿔 붙여 넣고 저장 → **한 번 `MODE=build-only` 로 실행**해 새 매개변수(MODE, REDEPLOY_BUILD)를 등록한다.

### 6-6. 출발 IP 고정 (`from=`)

> 2026-10-23 확인: Jenkins 컨테이너의 접속은 `호출 : ssh:16.88.122.113` 으로 보임 → `from="16.88.122.113"` 로 등록, status 정상.

`MODE=status` 로 실행 → 콘솔의 `호출    : ssh:<IP>` 를 보고 ws01 에서:
```bash
~/atms/deploy/jeus/install-deploy-war.sh ~/atms/infra/jenkins/keys/ws01-deploy.pub <그 IP>
grep jenkins-ws01-deploy ~/.ssh/authorized_keys
```
- 같은 키 줄을 `restrict,from="<IP>",command=…` 로 바꿔 쓴다 (개인키를 지웠어도 공개키 `.pub` 로 다시 등록 가능)
- 다시 `MODE=status` 가 되는지 확인. 이후 6-2 의 `127.0.0.1` 시험은 `from=` 때문에 거부된다 (정상)

### 6-7. 첫 배포

1. WAS 담당과 재기동 시간 약속 (업무시간 외 권장)
2. Jenkins `nh-atms-backend` → Build with Parameters: `MODE=build-deploy`, `MODULE=all` (이미 빌드한 것을 쓰려면 `MODE=redeploy`, `REDEPLOY_BUILD=<번호>`)
3. 콘솔: WAR 마다 `=== <파일명>` → `SHA256 일치` → `WAR 프로파일: jeus` → `교체 완료`, 이어서 `jeusRstart 메뉴 <정지>` → `정지 확인` → `jeusRstart 메뉴 5` → `기동 확인: …` → `재기동 완료` (6-8)
4. 이어서 [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 6 확인
5. 기동 확인이 실패하면 교체한 모듈이 자동으로 직전 백업으로 돌아가고 다시 재기동된 뒤 빌드가 실패로 끝난다. 수동으로는 `MODE=rollback`, `MODULE=<모듈>` (모듈마다, 뒤에 재기동 포함)

| 운영 | Jenkins (`nh-atms-backend`) | ws01 에서 직접 (atmswas) |
|---|---|---|
| 한 모듈만 배포 | `MODE=build-deploy`, `MODULE=atms-journal` | — |
| 예전 빌드 다시 | `MODE=redeploy`, `REDEPLOY_BUILD=<번호>` | `~/atms/deploy/jeus/swap-deploy.sh deploy ~/atms/release/<모듈>/<WAR>` |
| 되돌리기 | `MODE=rollback`, `MODULE=<모듈>` | `~/atms/deploy/jeus/deploy-war.sh rollback <모듈>` |
| 상태 | `MODE=status` | `~/atms/deploy/jeus/deploy-war.sh status` |
| 재기동만 | `MODE=restart` (`MODULE=all` 이면 6개 확인) | `~/atms/deploy/jeus/deploy-war.sh restart` |
| 키 즉시 차단 | — | `vi ~/.ssh/authorized_keys` 에서 `jenkins-ws01-deploy` 줄 삭제 |

| 증상 | 원인·대처 |
|---|---|
| `Host key verification failed` | 6-3 known_hosts (Jenkinsfile `WS01_SSH` 의 주소와 같은 이름으로 등록했는지) |
| `Permission denied (publickey)` | `from=` IP 불일치(6-6) 또는 Credentials 의 개인키가 다른 키 |
| `Connection refused` / `timed out` | 6-3 다시 — 그 주소로 컨테이너가 호스트 22 에 닿지 않음 |
| `WAR 파일명 형식 오류` / `SHA256 불일치` | 산출물 이름을 바꿨거나 전송 중 손상 → 다시 빌드 |
| `WAR 프로파일` 경고 | `-Pjeus` 없이 빌드된 WAR (이 Jenkinsfile 은 항상 `-Pjeus`) |
| 재기동 후 `/` 가 404, 기동 로그 없음 | Jakarta 미지원 또는 앱 미등록 → 6-0 |

보안: 같은 서버 안의 SSH 라 망을 나가지 않지만 HIWARE 기록에는 남지 않는다 → `~/deploy/war-deploy.log` 에 호출·결과가 남는다. 보안 담당 공유 문구에 함께 넣는다 ([FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) A-13).

### 6-8. 자동 재기동 (2026-10-23, 재기동 가능 전제)

`/JEUS/jeusATMS/jeusMENU/jeusRstart` 는 `root:sys -rwsr-xr-x` (누구나 실행, root 로 동작) 이고, 소스 `jeusmenu.c` (같은 폴더, 읽기 가능) 는 `setuid(0)` 뒤 메뉴 번호 0~8 을 받아 `system("su jeusatms -c …")` 를 실행한다 — 계정 검사 없음 (2026-10-23 확인). 확인된 메뉴:

| 번호 | 내용 | 파이프라인 |
|---|---|---|
| 0 | 종료 | 입력 시험용 (아무 일 없음) |
| 1·2 | `bin/servlet start/stop` (옛 방식) | 쓰지 않음 |
| 3 | jeusatms 의 java **전부** `kill -9` (DAS·NodeManager 포함) | **절대 쓰지 않음** |
| 4 | `webhome/jspwork/*` 삭제 | 쓰지 않음 |
| 5 | `jeusadmin -host 16.88.122.113:8736 … 'boot nbatmsltws01_atms01'` | 기동 (`JEUS_BOOT_MENU`, 기본 5) |
| 6 | `jeusadmin … 'down nbatmsltws01_atms01'` (atms01 만 정지, 2026-10-23 확인) | 정지 (`JEUS_STOP_MENU=6`) |
| 7·8 | `su webatms` wsboot / `wsdown -i` | 쓰지 않음 |

`deploy-war.sh restart [--rollback] [모듈…]` 동작:
1. 정지 메뉴 번호를 `printf '<번호>\n' | jeusRstart` 로 넣는다 → `JEUS_URL`(`http://16.88.122.113:8766` — 8766 은 113 에만 LISTEN, 127.0.0.1 불가)이 응답하지 않을 때까지 최대 `STOP_WAIT`(180초). 안 내려가면 **기동하지 않고** 실패
2. 기동 메뉴(5) → 모듈마다 기동 확인 주소가 200 이 될 때까지 최대 `BOOT_WAIT`(600초): 모듈마다 `<컨텍스트>/v3/api-docs` (공통 springdoc, permitAll. atms-web 도 2026-10-23 부터 — `/actuator/health` 는 DB 없으면 503 이라 롤백됨). `HEALTH_SKIP` 모듈은 제외 (예: DB 없는 atms-batch)
3. `--rollback` 이고 확인 실패면 그 모듈들을 `swap-deploy.sh rollback` → 1·2 다시 → 결과와 관계없이 실패 종료 (Jenkins 빨간색). 직전 버전도 안 뜨면 "WAS 담당 확인 필요"
4. jeusRstart 는 root 로 돌아 atmswas 가 중간에 멈출 수 없다 → 0~8 이외 번호는 넣지 않고, 입력이 잘못돼 메뉴가 반복 대기하는 일이 없게 **먼저 메뉴 0 으로 입력 방식을 시험**한다

서버 설정 (ws01, atmswas) — 재반입 없이 기존 파일만 수정:
```bash
grep -n -A3 -E 'case [6-8]' /JEUS/jeusATMS/jeusMENU/jeusmenu.c
grep -n -B2 -A12 'MenuSelect' /JEUS/jeusATMS/jeusMENU/jeusmenu.c | head -40
printf '0\n' | /JEUS/jeusATMS/jeusMENU/jeusRstart
echo 'JEUS_STOP_MENU=6' > ~/deploy/deploy-war.env
echo 'JEUS_URL=http://16.88.122.113:8766' >> ~/deploy/deploy-war.env
chmod 600 ~/deploy/deploy-war.env
```
- 세 번째 줄은 메뉴 0(종료)이라 아무것도 하지 않는다. `Promgram Exit` 이 나오고 바로 프롬프트로 돌아오면 표준입력 방식이 동작하는 것
- `deploy-war.sh` 수정분은 `restart` 관련 함수·case 추가, `status` 응답 코드를 기동 확인 주소로 변경. Jenkinsfile 은 `MODE` 에 `restart`, `RESTART` 매개변수, 타임아웃 60분, `ServerAliveInterval=30`
- 재기동은 업무 영향이 있으므로 첫 `MODE=restart` 는 WAS 담당과 시간을 맞춘다. 서버 간 SSH 가 아닌 같은 서버 안의 호출이지만 root 프로그램을 자동 실행하는 것이라 보안 담당 공유 문구에 포함한다

### 6-9. 매니지드 서버 6개 구성 (2026-10-27 반영 — 2026-10-26 결정)

> 6-8 까지는 atms01 하나(8766, 컨텍스트 6개) 기준. 아래가 목표 구성이며, 배포킷·소스는 이 기준으로 바뀌었다. WAS 담당이 서버를 만들기 전에는 web(8080) 외 모듈이 응답하지 않는다 (`status` 응답 `000`).

| 모듈 | 서버 | 포트 | 폴더 | 재기동 메뉴 (deploy-war.env) |
|---|---|---|---|---|
| atms-web | atms01 | 8080 | catms | `STOP_MENU_web`, `BOOT_MENU_web` (WAS 담당 추가 후 — 예전 5·6 은 nbatmsltws01_atms01 용) |
| atms-journal | ajou01 | 8081 | cajou | `STOP_MENU_journal`, `BOOT_MENU_journal` (WAS 담당 추가 후) |
| atms-batch | abat01 | 8082 | cabat | `..._batch` |
| atms-file | file01 | 8083 | cfile | `..._file` |
| atms-if-internal | ifin01 | 8084 | cifin | `..._ifin` |
| atms-if-external | ifex01 | 8085 | cifex | `..._ifex` |

동작 (`deploy-war.sh`):
- `deploy`·`rollback` 은 그대로 (폴더 교체). `status` 는 모듈마다 서버·주소·응답·재기동 메뉴 설정 여부
- `restart [--rollback] [모듈…]` 은 **그 모듈의 서버만 하나씩** 정지(메뉴)→정지 확인(포트 응답 없음)→기동(메뉴)→`http://16.88.122.113:<포트>/v3/api-docs` 200 확인. 다른 모듈은 멈추지 않음
- 확인 실패 + `--rollback` → 그 모듈만 직전 백업으로 되돌리고 다시 재기동 → 실패로 종료 (Jenkins 빨간색)
- 재기동 메뉴가 비어 있는 모듈 → 재기동하지 않고 "WAS 담당 재기동 필요" 출력, 종료코드 **4** → Jenkins **UNSTABLE(노란색)**
- 메뉴 번호는 숫자만, **3·7·8 은 거부** (3 = jeusatms java 전부 kill, 7·8 = WebtoB)
- 예전 설정 이름 `JEUS_STOP_MENU`/`JEUS_BOOT_MENU`·`JEUS_URL` 은 쓰지 않음 (2026-10-27 서버 이름 변경 — 있으면 경고만)

서버 적용 (ws01, atmswas — 반입한 배포킷을 `~/atms` 에 덮어쓴 뒤):
```bash
find ~/atms/deploy -name "*.sh" -exec sed -i 's/\r$//' {} +
mv ~/deploy/deploy-war.env ~/deploy/deploy-war.env.old-$(date +%Y%m%d)
~/atms/deploy/jeus/install-deploy-war.sh
~/atms/deploy/jeus/deploy-war.sh status
```
- 두 번째 줄: 예전 설정(JEUS_URL 8766)을 보관하고 새 예시(`deploy/jeus/deploy-war.env.example`)로 다시 만들게 한다
- WAS 담당이 서버별 재기동 메뉴를 알려 주면 `vi ~/deploy/deploy-war.env` 로 `STOP_MENU_<키>=`·`BOOT_MENU_<키>=` 를 채운다 (먼저 `printf '0\n' | /JEUS/jeusATMS/jeusMENU/jeusRstart` 로 메뉴 화면에서 번호 확인)
- Jenkins `nh-atms-backend` 잡의 Pipeline script 를 새 `backend.Jenkinsfile` 로 교체 (UNSTABLE 처리 추가)
- 소스: 내부 Gitea 사본의 `jeus-common.yml`·`application-jeus.yml` 6개를 [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 9~15단계 내용으로 교체 (개정 2026-10-27), `CHANGE_ME` 실제 값 다시 기입
- WebtoB: `deploy/webtob/http.m.atms-jsv.txt` (담당 또는 ROOT_DAY 2-13)

### 6-10. 서버 이름·폴더 확정 (2026-10-27) — 서버 폴더 이름 바꾸기

확정: 서버 `atms01`(web)·`ajou01`(journal)·`abat01`(batch)·`file01`·`ifin01`·`ifex01`, 폴더 `/home/atmswas/catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex`, 포트 8080~8085 (base = HTTP). 기존 `nbatmsltws01_atms01`(8766) 은 새 `atms01` 로 대체.

ws01 (atmswas) — 배포킷 반입 후, WAS 담당이 새 서버에 앱을 등록하기 **전에** 한 번:
```bash
ls -ld ~/catms*
mv ~/catms-journal ~/cajou
mv ~/catms-batch ~/cabat
mv ~/catms-file ~/cfile
mv ~/catms-ifin ~/cifin
mv ~/catms-ifex ~/cifex
ls -ld ~/catms ~/cajou ~/cabat ~/cfile ~/cifin ~/cifex
```
- `mv` 는 그룹(jeus)·권한(2750)을 그대로 유지한다. `catms` 는 이름이 같아 그대로
- 예전 백업(`~/deploy/backup/catms-journal-*` 등)은 새 이름으로 롤백 대상이 아님 → 필요 없으면 `rm -rf ~/deploy/backup/catms-journal-*` 처럼 정리 (선택)
- `~/deploy/deploy-war.env` 는 6-9 대로 새로 만들고 메뉴 번호는 WAS 담당 회신 후 채운다

