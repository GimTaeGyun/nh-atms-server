# 개발서버 Git 서버(Gitea) 구축·운영 가이드

작성일: 2026-10-08 · 개정 2026-10-12: 작업 계정 **atmswas**, 경로 `/home/atmswas/atms` (`~/atms` 는 atmswas 의 홈 기준)

## 0. 결정 사항

| 항목 | 결정 |
|---|---|
| 위치 | 개발서버 `nbatmsltws01`, rootless Podman 컨테이너 `atms-gitea` (**atmswas** 계정) |
| 푸시하는 PC | **내부망 Windows PC** (Git for Windows / Fork / IntelliJ) |
| 접속 방식 | **HTTP 만** (`http://<서버IP>:3000/...`), SSH 끔 |
| 사용자 | 소수 팀 2~5명 (계정 수동 생성, 가입 차단) |
| DB | Gitea 내장 SQLite (별도 DB 불필요) |
| 데이터 | `/home/atmswas/atms/infra/gitea/` (700). 디스크 여유는 `SERVER_DIRECTORY_LAYOUT.md` 3-3 확인 |
| 네트워크 | 현재 PC → 서버는 HIWARE(SSH 접근제어)만 가능 → **PC → 서버 TCP 3000 방화벽 신청 필요** |

```
[내부망 Windows PC들]                         [nbatmsltws01]
 Git / Fork / IntelliJ                         rootless Podman (atmswas)
   │  git push (HTTP, 계정+토큰)                ┌──────────────────────────┐
   └───────── TCP 3000 ─── 방화벽 ──────────▶ │ atms-gitea  :3000        │
                         (신청 필요)            │  data → ~/atms/infra/gitea│
                                               └──────────┬───────────────┘
   ※ SSH(22) 접속은 지금처럼 HIWARE 경유                     │ 같은 서버 안 (방화벽 불필요)
                                               ┌──────────▼───────────────┐
                                               │ Jenkins (추후)            │
                                               └──────────────────────────┘
```

> HIWARE 는 서버 **로그인(SSH)** 을 통제하는 장비라 Git HTTP(3000) 통신과는 별개다. Git 푸시는 PC 에서 서버 3000 포트로 **직접** 가야 하므로 네트워크 방화벽이 열려야 한다.

---

## 1. 준비 체크리스트

### 1-1. 반입 파일

| 파일 | 상태 | 비고 |
|---|---|---|
| `gitea-rootless.tar` (`gitea/gitea:latest-rootless`) | 반입 목록에 있음 (`REQUIRED_FILES.md`) | 받을 때 버전을 `versions.txt` 로 기록. 업그레이드 시 같은 방식으로 새 tar 반입 |
| 배포킷 (`deploy/gitea/`) | 저장소에 있음 | `install-gitea.sh`, `gitea.env.example`, Quadlet·백업 파일 |

### 1-2. 서버 권한 (1일 root 권한 날 처리 → `ROOT_DAY_CHECKLIST.md`)

| 항목 | 필수 | 없으면 |
|---|---|---|
| `/etc/subuid`, `/etc/subgid` 에 atmswas | **필수** | 이미지 적재·컨테이너 실행 불가 |
| `loginctl enable-linger atmswas` | 권장 | 로그아웃해도 유지는 되지만 **서버 재부팅 후 자동 기동 안 됨**, 백업 타이머도 안 돎 |
| 서버 방화벽(firewalld) 3000/tcp 허용 | firewalld 가 켜져 있으면 **필수** | PC 에서 접속 불가. 확인: `systemctl is-active firewalld` (일반 계정으로 가능) |

### 1-3. 네트워크 방화벽 신청 (보안/네트워크 담당)

| 항목 | 내용 |
|---|---|
| 출발지 | 개발자 PC IP (2~5대, PC 에서 `ipconfig` 로 확인) |
| 목적지 | `nbatmsltws01` IP (서버에서 `hostname -I`) |
| 포트 | **TCP 3000** |
| 용도 | 개발 소스 형상관리(Git, HTTP) — 사내 개발서버 내부 서비스 |
| 기간 | 프로젝트 종료 시까지 |

### 1-4. PC 준비 (각 개발자)

| 항목 | 확인 |
|---|---|
| Git for Windows | `git --version` (Fork 만 있으면 Fork 내장 git 사용 가능, 명령창용은 별도 설치 권장) |
| Git Credential Manager | Git for Windows 기본 포함 → 계정·토큰 저장 |
| 브라우저 | `http://<서버IP>:3000` 접속 (계정·토큰 관리) |
| 프록시 | PC 에 프록시가 설정돼 있으면 서버 IP 는 제외 (5장) |

### 1-5. 결재·보안 확인 (권장)

- 개발서버에 **소스 저장 서비스(Gitea)를 신규로 띄운다**는 점을 담당자에게 공유 (사용 포트, 사용자, 백업 방식)
- 소스에 비밀번호가 포함된 파일(`application-local.yml` 등)이 있으므로 저장소는 **비공개 + 로그인 필수**로 운영 (기본 설정에 반영됨)

---

## 2. 설치 (atmswas, 한 줄씩)

```bash
# 1) 이미지 적재
podman load -i ~/atms/images/gitea-rootless.tar
podman images | grep gitea

# 2) 서버 IP: PC 가 접속하는 IPv4 (2026-10-14 판단: 16.88.122.113)
#    hostname -I 첫 값의 fe80::... 는 IPv6 내부 주소 → 쓰면 안 됨 (스크립트가 거부)

# 3) 설치·기동 (배포킷 위치 기준)
sed -i 's/\r$//' ~/atms/deploy/gitea/install-gitea.sh
chmod +x ~/atms/deploy/gitea/install-gitea.sh
~/atms/deploy/gitea/install-gitea.sh 16.88.122.113
```

스크립트가 하는 일:
- `~/atms/infra/gitea/{data,config}` 생성 (700) + `podman unshare chown -R 1000:1000` 으로 컨테이너 git(1000) 소유로 맞춤. 호스트에서는 atmswas 의 subuid 소유로 보이며, 직접 다룰 때는 `podman unshare` 를 붙인다 (`--userns=keep-id` 는 이 서버에서 기동 실패해 쓰지 않음, 2026-10-14)
- `gitea.env` 생성 (600): 서버 IP 반영, 비밀키(SECRET_KEY·INTERNAL_TOKEN) 자동 생성. 다시 실행하면 IP 는 새 값으로 바뀌고 비밀키는 유지
- 자원 제한: atmswas 에 위임된 cgroup 컨트롤러만 사용 (`cpu` 미위임이면 `--cpus` 생략, `memory` 있으면 768MB)
- linger 가 켜져 있으면 Quadlet(재부팅 자동기동), 아니면 `podman run`
- `http://127.0.0.1:3000/api/healthz` 응답 확인

주요 설정 (`gitea.env`):

| 설정 | 값 | 이유 |
|---|---|---|
| SSH | 끔 | HTTP 만 사용 |
| `OFFLINE_MODE`, Gravatar 끔, 업데이트 확인 끔 | 켬/끔 | 폐쇄망 (외부 접속 시도 방지) |
| 회원가입 | 차단 | 관리자가 계정 생성 |
| 로그인해야 보기, 저장소 기본 비공개 | 켬 | 소스 보호 |
| 기본 브랜치 | `main` | |
| 자원 | 메모리 768MB (+ CPU 0.5 는 cgroup `cpu` 위임 후 자동 적용) | 서버 2코어·JEUS 공존. 2026-10-14 기준 cpu 미위임 |

---

## 3. 초기 설정

### 3-1. 관리자 계정 (서버)

```bash
podman exec atms-gitea gitea admin user create --admin --username <관리자ID> --email <메일> --random-password --must-change-password
```
화면에 출력된 임시 비밀번호로 첫 로그인 → 비밀번호 변경.
(방화벽 개방 전이면 서버 안에서 `curl -s http://127.0.0.1:3000/api/healthz` 로만 확인 가능)

### 3-2. 웹 화면에서 (방화벽 개방 후, `http://16.88.122.113:3000`, 관리자 `atmsadmin` 으로)

결정 (2026-10-23): 개발자 4명(한신·오제혁·김태균·이태성) = `developers`(쓰기), `jenkins-bot` = `readers`(읽기), 관리는 `atmsadmin`(조직 Owners). `main` 은 강제 푸시·삭제만 금지.

**① 조직** — 우상단 `+` → 새 조직

| 항목 | 값 | 이유 |
|---|---|---|
| 조직 이름 | `NH-ATMS` | 저장소 주소 `.../NH-ATMS/nh-atms.git` 에 들어감 (Jenkinsfile·웹훅과 같아야 함) |
| 공개 범위 | **비공개** | 조직 구성원만 조직이 보임 |
| 저장소 관리자 권한 (팀에서 협업자 추가·제거 가능) | 체크 해제 | 권한 관리는 Owners 만 |

**② 사용자 계정** — 우상단 프로필 → 사이트 관리 → 사용자 계정 → 사용자 계정 생성 (5개: 4명 + `jenkins-bot`)

| 항목 | 값 |
|---|---|
| 인증 소스 | Local |
| 사용자명 | 영문 ID (예: `taesung.lee`). 이후 git 로그인 ID |
| 이메일 | 사람마다 다른 주소 (발송은 안 됨, 형식만). `jenkins-bot` 은 `jenkins-bot@atms.local` |
| 비밀번호 | 임시값 |
| 비밀번호 변경 필요 | **체크** (첫 로그인 때 본인이 변경) |

만든 뒤 각 계정 "편집" 에서: **조직 생성 가능 해제**, 관리자 해제(기본), `jenkins-bot` 은 이후 웹 로그인할 일이 토큰 발급뿐이다.

**③ 팀** — 조직 `NH-ATMS` → 팀 → 새 팀 (`Owners` 팀은 자동 생성, `atmsadmin` 이 들어 있음)

| 항목 | `developers` | `readers` |
|---|---|---|
| 설명 | 개발자 (push 가능) | Jenkins 등 읽기 전용 |
| 저장소 접근 | **모든 저장소** (조직 저장소 전부, 새로 만드는 저장소도 자동) | **모든 저장소** |
| 저장소 만들기 (팀 구성원이 조직에 새 저장소 생성) | 해제 | 해제 |
| 권한 | 일반 접근 → 아래 표 | 일반 접근 → 아래 표 |

| 권한 항목 | developers | readers |
|---|---|---|
| 코드 | **쓰기** | **읽기** |
| 이슈 | 쓰기 | 없음 |
| 풀 리퀘스트 | 쓰기 | 없음 |
| 릴리스 | 쓰기 | 읽기 |
| 위키 | 쓰기 | 없음 |
| 프로젝트 | 쓰기 | 없음 |
| 패키지 | 없음 | 없음 |
| 액션 | 없음 | 없음 |
| 외부 위키·외부 트래커 | 없음 | 없음 |

- "관리자 접근" 은 고르지 않는다 (저장소 설정·웹훅·삭제까지 가능해짐 → Owners 만)
- 팀 화면 → 구성원 추가: `developers` ← 4명, `readers` ← `jenkins-bot`

**④ 저장소** — 조직 `NH-ATMS` → 새 저장소

| 항목 | 값 | 이유 |
|---|---|---|
| 소유자 | `NH-ATMS` | 개인 계정 아래로 만들지 않는다 |
| 저장소 이름 | `nh-atms` | Jenkinsfile `GITEA_REPO` 와 같아야 함 |
| 공개 여부 | **비공개 체크** | |
| 템플릿·이슈 라벨·.gitignore·라이선스·README | 모두 비움, **저장소 초기화 해제** | PC 의 기존 소스를 그대로 push 하므로 빈 저장소여야 충돌이 없다 |
| 기본 브랜치 | `main` | |

**⑤ main 보호** — 첫 push 가 끝난 뒤 저장소 → 설정 → 브랜치 → 브랜치 보호 추가

| 항목 | 값 |
|---|---|
| 보호 브랜치 이름 패턴 | `main` |
| 푸시 | **푸시 허용** (화이트리스트 없음 = 쓰기 권한자 모두 직접 push) |
| 강제 푸시 | **금지** (이력 덮어쓰기 방지) |
| 승인 필요·상태 확인 등 | 사용 안 함 |

브랜치 삭제는 보호 규칙이 있는 브랜치면 막힌다.

**⑥ 확인**
- 팀원 한 명으로 로그인 → `NH-ATMS/nh-atms` 가 보이고 push 가능 (4장)
- `jenkins-bot` 으로 로그인 → 저장소는 보이지만 설정 메뉴·쓰기 없음 → 3-3 에서 토큰 발급 (권한 `repository` **읽기**) → Jenkins Credentials `gitea-jenkins-bot` 에 등록 ([BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 3장)

### 3-3. 각자 토큰 발급

> 화면 항목별 상세(토큰 → 첫 push → 팀원 clone → Jenkins Credentials·잡 → 웹훅): [GITEA_JENKINS_WALKTHROUGH.md](GITEA_JENKINS_WALKTHROUGH.md) (권장)

HTTP 방식은 푸시할 때 계정·비밀번호를 보낸다. 비밀번호 대신 **개인 토큰**을 쓴다.
웹 화면 → 우상단 프로필 → 설정 → 애플리케이션 → 토큰 생성 (권한: `repository` 읽기·쓰기) → 값 복사(한 번만 보임)

### 3-4. Jenkins 웹훅 (push → 자동 빌드, 2026-10-22 결정)

화면 빌드 잡(`nh-atms-frontend`)은 주기 폴링 없이 **Gitea 웹훅으로만** 시작한다. 상세·순서: [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4-7, B-8 ~ B-10.

1. **같은 네트워크** (서버, atmswas): Gitea·Jenkins 를 podman 네트워크 `atms-net` 에 함께 둔다 (`install-gitea.sh` 가 생성·연결, Jenkins 는 `--network atms-net`). 서버 IP 로는 컨테이너끼리 닿지 않았다 (2026-10-22, 연결 거부). 2026-10-22 구성·확인 완료. 웹훅 대상 `atms-jenkins` 는 사설 IP(10.89.x)라 `ALLOWED_HOST_LIST` 는 기본값 `private` 그대로
2. **Jenkins 토큰**: Jenkins 관리 → Security → "Git plugin notifyCommit access tokens" 에서 발급
3. **웹훅 추가**: 저장소 → 설정 → Webhooks → 웹훅 추가 → Gitea
   - 대상 URL `http://atms-jenkins:8080/git/notifyCommit?url=http://atms-gitea:3000/NH-ATMS/nh-atms.git&token=<토큰>` (`url=` 은 Jenkins 잡의 저장소 URL 과 같게)
   - HTTP 방법 `POST` (콘텐츠 유형 `application/json`; GET 은 414 URI Too Long), 트리거 "Push 이벤트", 브랜치 필터 `main`
4. "테스트 전송" → 최근 전송의 응답이 200 이고 `Scheduled polling of nh-atms-frontend` 면 성공

---

## 4. Windows PC 에서 첫 푸시

### 4-1. 기본 설정 (1회, PowerShell 또는 cmd)

```powershell
git config --global user.name "<이름>"
git config --global user.email "<메일>"
git config --global credential.helper manager
git config --global core.autocrlf true
```

### 4-2. 기존 소스(이미 git 저장소인 경우) 올리기

```powershell
cd D:\NH-ATMS
git status
git remote -v
git remote add bank http://<서버IP>:3000/NH-ATMS/nh-atms.git
git push -u bank main
```
- 처음 푸시할 때 로그인 창이 뜨면: 사용자명 = Gitea ID, 비밀번호 = **3-3 토큰**
- 브랜치 이름이 `master` 면 `git push -u bank master`
- 기존 업체 원격(GitLab)은 그대로 두고 `bank` 라는 이름으로 추가하는 방식 (나중에 정리 가능)

### 4-3. git 저장소가 아닌 폴더를 처음 올릴 때

```powershell
cd D:\NH-ATMS
git init -b main
git add .
git status
git commit -m "Initial import"
git remote add bank http://<서버IP>:3000/NH-ATMS/nh-atms.git
git push -u bank main
```
`git status` 에서 `build/`, `node_modules/`, `.gradle/`, `.idea/`, `.env`, jar·exe 가 올라가지 않는지 먼저 확인 (`.gitignore`).

### 4-4. 다른 팀원은 받기만

```powershell
git clone http://<서버IP>:3000/NH-ATMS/nh-atms.git
```

---

## 5. 문제 해결

| 증상 | 원인 / 조치 |
|---|---|
| PC 에서 `Failed to connect to <IP> port 3000` / 접속 시간 초과 | 네트워크 방화벽 미개방 또는 서버 firewalld. PC: `Test-NetConnection <IP> -Port 3000`, 서버: `curl -s http://127.0.0.1:3000/api/healthz` 가 되는지 비교 |
| 서버 안 `curl` 도 실패 | 컨테이너 문제 → `podman ps -a`, `podman logs --tail 100 atms-gitea` |
| `Authentication failed` | 비밀번호 대신 토큰 사용, Windows 자격 증명 관리자에서 `git:http://<IP>:3000` 항목 삭제 후 재시도 |
| PC 프록시 때문에 접속 안 됨 | `git config --global http.http://<서버IP>:3000/.proxy ""` |
| 푸시 중 `RPC failed; HTTP 413` / 큰 파일 | 저장소에 대용량 파일(jar·zip·exe) 포함 → `.gitignore` 정리. 필요 시 `git config --global http.postBuffer 524288000` |
| `crun: controller 'cpu' is not available under /sys/fs/cgroup/user.slice/...` | rootless 계정에 cgroup `cpu` 미위임 (RHEL 9 기본). 최신 `install-gitea.sh` 는 자동으로 `--cpus` 를 뺀다. 예전 스크립트면 `sed -i 's/--cpus=0.5 //' ~/atms/deploy/gitea/install-gitea.sh ~/atms/deploy/gitea/atms-gitea.container` 후 재실행. CPU 제한이 필요하면 1일 root 때 위임 (ROOT_DAY 2-9) |
| `GITEA__server__ROOT_URL` 이 `http://fe80::...` | IPv6 내부 주소로 설치함 → 최신 스크립트로 `install-gitea.sh 16.88.122.113` 재실행 (예전 스크립트면 `gitea.env` 의 `DOMAIN`·`ROOT_URL` 두 줄을 `sed` 로 수정) |
| `crun: readlink (빈 경로): No such file or directory ... command that was not found` | `--userns=keep-id` 사용 시 발생 (podman 5.8.2 / crun 1.27, 2026-10-14 확인). 최신 스크립트는 keep-id 를 쓰지 않는다. 예전 스크립트면 `sed -i 's/--userns=keep-id:uid=1000,gid=1000 //' ~/atms/deploy/gitea/install-gitea.sh`, `sed -i '/^UserNS=/d' ~/atms/deploy/gitea/atms-gitea.container`, `podman unshare chown -R 1000:1000 ~/atms/infra/gitea/data ~/atms/infra/gitea/config` 후 재실행 |
| 서버 재부팅 후 Gitea 안 뜸 | linger 미설정 → `install-gitea.sh <서버IP>` 재실행 (설정·데이터 유지) |
| 화면 시간이 9시간 차이 | `GITEA__time__DEFAULT_UI_LOCATION=Asia/Seoul` 확인 |
| 웹훅 최근 전송에 `webhook can only call allowed HTTP servers` | 대상 URL 이 `atms-jenkins` 가 아니거나 `ALLOWED_HOST_LIST` 가 `private` 가 아님 → 3-4 의 1 |

---

## 6. 운영

| 작업 | 명령 (atmswas) |
|---|---|
| 상태 | `podman ps --filter name=atms-gitea` · `curl -s http://127.0.0.1:3000/api/healthz` |
| 로그 | `podman logs --tail 100 atms-gitea` |
| 재기동 | Quadlet: `systemctl --user restart atms-gitea` / 아니면 `podman restart atms-gitea` |
| 설정 변경 | `~/atms/infra/gitea/gitea.env` 수정 → 재기동 |
| 수동 백업 | `podman exec atms-gitea gitea dump --skip-log --type tar.gz -f - > ~/atms/backup/gitea/gitea-dump-manual.tar.gz` (atmswas 소유, 바로 PC 로 옮길 수 있음). **데이터 폴더(`/var/lib/gitea/...`) 안에 만들지 말 것** — 다음 dump 가 그 파일까지 담아 계속 불어난다 (2026-11-04, INFRA_MANUAL T65) |
| 자동 백업 (linger 필요) | 아래 6-1 |
| 업그레이드 | 새 버전 tar 반입 → `podman load` → 백업 → 재기동 (`latest-rootless` 태그가 새 이미지를 가리킴) |

### 6-1. 매일 자동 백업 (crontab 사용 불가 → systemd --user 타이머)

```bash
mkdir -p ~/.config/systemd/user
cp ~/atms/deploy/gitea/gitea-backup.service ~/atms/deploy/gitea/gitea-backup.timer ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user enable --now gitea-backup.timer
systemctl --user list-timers | grep gitea
```
매일 02:30 `~/atms/backup/gitea/gitea-dump-YYYYMMDD.tar.gz` 생성, 7일 지난 파일 삭제 (2026-11-04 변경 전에는 `data/backup` 에 만들어 dump 가 이전 dump 를 담으며 불어났음 — T65).
같은 서버에만 있으면 서버 장애 시 함께 잃으므로, **주 1회는 백업 파일을 PC 등 다른 곳으로 복사** 권장.

---

## 7. 진행 순서 요약

| 순서 | 작업 | 담당 | 선행 |
|---|---|---|---|
| 1 | Gitea 이미지 반입 (`gitea-rootless.tar`) | 본인 | 반입 승인 |
| 2 | 방화벽 신청: PC → 서버 TCP 3000 | 네트워크/보안 | PC IP 목록 |
| 3 | 1일 root: subuid/subgid, linger, firewalld 3000 | 본인(root 권한일) | 권한 신청 |
| 4 | `install-gitea.sh <서버IP>` → 관리자 생성 | 본인 | 1, 3 |
| 5 | 조직·팀·계정·저장소 생성, 토큰 발급 | 본인 | 2, 4 |
| 6 | 각 PC 에서 첫 푸시 / 클론 | 팀원 | 5 |
| 7 | 자동 백업 타이머 등록 | 본인 | linger |
| 8 | (추후) Jenkins 연동: `jenkins-bot` 토큰 + 웹훅 | 본인 | Jenkins 구축 |
