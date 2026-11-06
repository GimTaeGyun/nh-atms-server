# 화면(프론트엔드) 빌드·배포 — Gitea + Jenkins(ws01) → SSH → WebtoB(wb01)

작성일: 2026-10-21 · 개정 2026-10-21 (배포 방식을 A안 "wb01 타이머가 당겨오기" → **B안 "Jenkins 가 SSH 로 배포"** 로 변경) · 개정 2026-10-22 (빌드 시작을 1분 폴링 → **Gitea 웹훅**으로 변경) · 대상: `nbatmsltws01`(113, Gitea·Jenkins) / `nbatmsltwb01`(112, WebtoB 9000) · 계정 `atmswas`

> 결정 (사용자, 2026-10-21)
> - 화면 빌드·업로드도 ws01 의 Gitea·Jenkins 를 쓴다
> - **배포는 Jenkins 가 SSH 로 wb01 에 직접 한다** (B안, 전달 방식 ①): 빌드 결과 `tar.gz` 를 SSH 표준입력으로 보내고, 같은 연결에서 wb01 의 `deploy-front.sh` 가 검증 → 교체 → 확인까지 한다. 결과(성공/실패)는 Jenkins 빌드 결과로 바로 보인다
> - 근거: **ws01 호스트와 Jenkins 컨테이너 둘 다 → wb01:22 접속 가능** [확인 2026-10-21]
> - 키 제한: wb01 `authorized_keys` 에 `command=`(배포 스크립트 고정) + `from="16.88.122.113"` + `restrict` → 키가 새도 셸 접속·다른 명령 불가
> - **빌드 시작은 Gitea 웹훅** (2026-10-22): push 하면 Gitea 가 Jenkins git 플러그인의 `/git/notifyCommit` 을 호출 → 잡이 바로 새 커밋을 확인해 빌드. 주기 폴링은 없음(웹훅만)
> - **Gitea·Jenkins 는 같은 podman 네트워크 `atms-net`** (2026-10-22): rootless 기본(pasta) 모드에서 Gitea 컨테이너 → `16.88.122.113:8090` 은 `Could not connect to server`(연결 거부) [확인 2026-10-22]. `host.containers.internal` 은 처음 `000` 이 주소 오타였고 다시 확인하지 않음 → 서버 IP 에 묶이지 않는 방식으로 같은 네트워크에 두고 컨테이너 이름으로 접속 (**구성 완료·확인 2026-10-22**: Gitea→`atms-jenkins:8080` 200, Jenkins→`atms-gitea:3000` 200, Jenkins→wb01:22 OK) (Jenkins → `http://atms-gitea:3000`, Gitea 웹훅 → `http://atms-jenkins:8080`). 이 주소는 사설 IP(10.89.x)라 Gitea 웹훅 허용 목록은 기본값 `private` 그대로
> - 쓰지 않는 것: Gitea 패키지 저장소, wb01 용 Gitea 토큰, wb01 타이머·linger (A안, 같은 날 검토 후 변경), scp(키 명령 고정과 함께 쓸 수 없음)
> - 현재: Gitea·Jenkins 는 ws01 에서 기동 중, PC → 113:3000·8090 만 막힘 (112:9000 은 열림)

> 2026-10-22: 백엔드 잡(`nh-atms-backend`)과 웹훅 1개를 함께 쓴다. 두 Jenkinsfile 이 `PathRestriction` 으로 `Frontend/`·`Backend/` 변경에만 빌드 → 전체 설정 순서는 [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 3장

## 1. 흐름

```
개발 PC ──git push──▶ Gitea (113:3000, 소스 저장소)
                          │ push 웹훅 (POST http://atms-jenkins:8080/git/notifyCommit?url=<저장소>&token=<토큰>, podman 네트워크 atms-net 안)
                          │   또는 수동 "Build with Parameters"
                          ▼
                     Jenkins (113:8090, 컨테이너, 잡 nh-atms-frontend)
                       npm ci --offline → npm run build → dist → tar.gz + sha256 (최근 10개 빌드 보관)
                          │ ssh atmswas@16.88.122.112 "deploy <버전> <sha256> <커밋>" < tar.gz
                          │   (키: Jenkins Credentials wb01-deploy-key, 113 → 112:22)
                          ▼
nbatmsltwb01  ~/.ssh/authorized_keys: 이 키는 deploy-front.sh 만 실행 가능
              ~/deploy/bin/deploy-front.sh: 받기 → sha256 확인 → swap-deploy.sh 로 docs 교체 → / 200 · index.html 일치 확인
                                            (실패하면 자동 되돌림 + Jenkins 빌드 실패)
                          │
브라우저 ──:9000──▶ WebtoB ── /  → /home/atmswas/docs (화면, 재기동 불필요)
                          └─ /api/ 등 → JEUS atms01 (JSV 19000)   ← http.m, WebtoB 담당
```

## 2. 구성 파일

| 배포킷 파일 | 놓을 곳 | 비고 |
|---|---|---|
| `deploy/jenkins/pipelines/frontend.Jenkinsfile` | Jenkins 잡 `nh-atms-frontend` 의 "Pipeline script" 칸 (붙여 넣기) | `GITEA_REPO` 기본값 `http://atms-gitea:3000/NH-ATMS/nh-atms.git` (저장소 이름이 다르면 수정). 트리거는 `pollSCM('')`(웹훅 전용) |
| `deploy/gitea/atms-gitea.container`, `install-gitea.sh` | ws01 Gitea (Quadlet) | `Network=atms-net` — 설치 스크립트가 네트워크를 만들고 붙인다. 지금 서버는 4-7 로 직접 반영 |
| `deploy/webtob/deploy-front.sh` | wb01 `~/deploy/bin/deploy-front.sh` (700) | SSH 로 받는 명령: `deploy` / `rollback` / `status` 만 |
| `deploy/webtob/install-wb01.sh` | wb01 에서 배포킷 위치 그대로 실행 (2026-10-23) | 4-1 폴더·스크립트 설치, 4-4 공개키 등록 |
| `deploy/jeus/swap-deploy.sh` | wb01 `~/deploy/bin/swap-deploy.sh` (700) | 2026-10-21 `tar.gz` 지원 추가 (WAR 배포 동작은 그대로) |
| 배포 키 (ed25519) | 개인키 → Jenkins Credentials `wb01-deploy-key` / 공개키 → wb01 `~/.ssh/authorized_keys` | ws01 에서 만들고, Jenkins 등록 후 ws01 의 개인키 파일은 지운다 |

wb01 에 생기는 것 (`/home/atmswas`):
```
deploy/                 700
├─ bin/                 deploy-front.sh, swap-deploy.sh
├─ inbox/               받은 tar.gz + .sha256 (최근 5개)
├─ backup/              docs-<시각> 백업 (최근 5개), docs.failed-<시각> (되돌린 버전)
├─ front-deploy.log     호출 기록 (시각, 호출 IP, 결과)
├─ front-current.txt    지금 배포된 버전·커밋
└─ deploy-history.log   swap-deploy.sh 교체 기록
.ssh/                   700, authorized_keys 600
docs/                   2750 atmswas:jeus  ← WebtoB DocBase (교체 대상)
```

새 파일(`deploy/webtob/`, `deploy/jenkins/pipelines/`, 수정된 `swap-deploy.sh`)은 **배포킷 재반입본**에 포함해 PC 로 가져온다.

## 3. 보안 — 배포 키 제한

wb01 `~/.ssh/authorized_keys` 한 줄:
```
restrict,from="16.88.122.113",command="/home/atmswas/deploy/bin/deploy-front.sh" ssh-ed25519 AAAA... jenkins-wb01-deploy
```

| 옵션 | 효과 |
|---|---|
| `command="…deploy-front.sh"` | 이 키로 접속하면 무엇을 요청하든 이 스크립트만 실행된다. 요청한 명령은 `SSH_ORIGINAL_COMMAND` 로 들어오고, 스크립트는 `deploy <버전> <sha256> [커밋]`·`rollback`·`status` 외에는 거부하고 기록한다 (`REJECT`) |
| `from="16.88.122.113"` | ws01 에서 온 접속만 허용 (Jenkins 컨테이너의 접속도 ws01 주소로 나간다 — A-5 로 확인) |
| `restrict` | 터미널(pty)·포트 포워딩·에이전트 포워딩 등 금지 |

남는 점:
- 서버 간 SSH 는 **HIWARE 를 거치지 않으므로 HIWARE 기록에 남지 않는다** → `front-deploy.log` 에 호출 IP·결과가 남는다. 은행 보안 기준상 신고·승인이 필요한지 보안 담당에 공유해 두는 것을 권장 (A-13)
- Jenkins 는 로그인한 사용자가 모든 권한을 가진다 (`loggedInUsersCanDoAnything`) → Jenkins 사용자 누구나 이 잡으로 배포·되돌리기를 할 수 있다. 키로 할 수 있는 일은 위 3가지뿐
- 개인키는 Jenkins Credentials 에만 둔다 (ws01 파일은 등록 후 삭제). 키가 의심되면 wb01 `authorized_keys` 의 그 줄을 지우면 즉시 차단

## 4. 지금 할 수 있는 작업 (PC → 113 방화벽과 무관)

| # | 작업 | 서버/계정 | 방법·확인 |
|---|---|---|---|
| A-1 | **Jenkins 컨테이너 SSH 도구 확인** | ws01 atmswas | `podman exec atms-jenkins ssh -V` 와 `podman exec atms-jenkins sh -c "command -v ssh-agent"` → 버전·경로가 나와야 함 (공식 이미지에 포함된 것으로 예상). 없으면 Jenkins 이미지 재제작 필요 |
| A-2 | **Jenkins 컨테이너 → Gitea 확인** | ws01 atmswas | 4-7 을 한 뒤 `podman exec atms-jenkins curl -s -m 5 -o /dev/null -w "%{http_code}\n" http://atms-gitea:3000/api/healthz` → `200` |
| A-3 | **npm 오프라인 캐시 확인** | ws01 atmswas | 반입된 `offline-seed` 의 `npm-cache` 가 서버에 있는지. 없으면 업로드 ([REQUIRED_FILES.md](REQUIRED_FILES.md) 2-2) |
| A-4 | **Jenkins 에 npm 캐시 마운트** | ws01 atmswas | `podman rm -f atms-jenkins` 후 [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) 9-2 명령에 `-v <npm-cache 경로>:/cache/npm:Z` 를 더해 다시 실행 (데이터 유지). `podman exec atms-jenkins ls /cache/npm` |
| A-5 | **출발지 IP 확인** (`from=` 값) | ws01 atmswas | `ip route get 16.88.122.112` → `src 16.88.122.113` 이면 그대로. 다른 IP 면 3장의 `from=` 을 그 IP 로 |
| A-6 | **wb01 폴더·스크립트 준비** | wb01 atmswas | 4-1 |
| A-7 | **교체 연습** (지금 화면 그대로) | wb01 atmswas | 4-2. 이때 지금 docs 안의 빈 `inbox/`·`releases/` 도 백업 쪽으로 빠진다 (DocBase 정리 겸) |
| A-8 | **배포 키 만들기** | ws01 atmswas | 4-3 |
| A-9 | **wb01 에 공개키 등록** | wb01 atmswas | 4-4 |
| A-10 | **ws01 에서 키 시험** | ws01 atmswas | 4-5 |
| A-11 | **Jenkins 컨테이너 known_hosts** | ws01 atmswas | 4-6 |
| A-12 | **WebtoB 담당 요청** | — | ① `/api/`, `/swagger-ui/`, `/v3/api-docs`, `/actuator/` (+ 에이전트용 `/atms/`, `/atmr/`, `/atmf/`) → JSV(atms01) `*URI` 등록, ② 화면 새로고침용 SPA fallback(없는 경로 → `index.html`, `/api/` 제외) 가능 여부, ③ `/error/error.html` 의 실제 위치(`~/docs` 에 `error/` 없음). 적용(`wscfl`·`webtobRstart`)은 담당자 — [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) 7-4, 7-6 |
| A-13 | **보안 담당 공유** | — | "ws01(113) Jenkins → wb01(112) atmswas SSH, 배포 전용 키(명령·출발지 고정)로 화면 파일 교체에만 사용" (3장) |
| A-14 | **Gitea·Jenkins 를 같은 네트워크(atms-net)로** | ws01 atmswas | 4-7 |

### 4-0. wb01 정리 (atmswas, 2026-10-23 사용자 결정: 동료 구성은 지워도 됨)

2026-10-23 wb01 `~` 상태: `data`, `docs`, `mock`, `nodejs`, `tools` (동료가 만든 것). `docs` 는 WebtoB DocBase 라 **폴더 자체는 지우지 않는다** (내용은 4-2·배포 때 교체되고 이전 내용은 `~/deploy/backup` 에 남음). 나머지는 백업 후 삭제.
```bash
ls -la ~
ls -ld ~ ~/docs
ls ~/docs
du -sh ~/data ~/mock ~/nodejs ~/tools
df -h /home/atmswas
ps -u atmswas -o pid,args | grep -v -E "bash|ps|grep|sshd"
mkdir -p ~/deploy/backup
tar -C ~ -czf ~/deploy/backup/colleague-20261023.tar.gz data mock nodejs tools
ls -l ~/deploy/backup
rm -rf ~/data ~/mock ~/nodejs ~/tools
chgrp jeus ~
chmod 750 ~
ls -la ~
```
- `ps` 결과에 `node` 가 있으면 먼저 `kill <PID>` (atmswas 자신의 프로세스만)
- 홈은 `atmswas:jeus` 750 (WebtoB 의 webatms 가 jeus 그룹으로 `docs` 를 읽음), `docs` 는 `drwxr-s---` `atmswas jeus` 이어야 한다. 아니면 `chgrp jeus ~/docs` · `chmod 2750 ~/docs`
- 백업 tar 는 한동안 두었다가 필요 없으면 삭제

### 4-1. wb01 폴더·스크립트 (atmswas)

2026-10-23 배포킷의 `deploy/webtob/install-wb01.sh` 로 한다 (폴더 `~/deploy/{bin,inbox,backup}` 700, `deploy-front.sh`·`swap-deploy.sh` 설치(CRLF 제거·700·문법 검사, swap-deploy 가 tar.gz 를 받는 판인지 확인), `flock`·`curl`·`sha256sum`·`tar` 확인, 홈 `jeus` 750·`~/docs` `jeus` 2750).
배포킷의 `deploy` 폴더를 FileZilla(바이너리)로 wb01 `/home/atmswas/deploy/kit/` 에 올린 뒤 ([DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) "배포킷 갱신"):
```bash
sed -i 's/\r$//' ~/deploy/kit/deploy/webtob/install-wb01.sh
bash ~/deploy/kit/deploy/webtob/install-wb01.sh
ls -l ~/deploy/bin
```
- 끝에 `완료.` 가 나오고 `ls` 에 `deploy-front.sh`·`swap-deploy.sh` (`-rwx------`) 가 보이면 정상
- `~/deploy/kit` 은 키 등록(4-4)까지 끝나면 지워도 된다 (`rm -rf ~/deploy/kit`). **`~/docs` 아래에는 두지 않는다**
- 다시 실행해도 안전 (스크립트만 새로 덮어씀)

### 4-2. 교체 연습 (wb01, atmswas)

지금 화면 파일을 묶어 스크립트로 다시 배포해 본다 (내용은 같고, 교체·확인·백업 동작만 검증):
```bash
tar -C ~/docs -czf ~/front-test.tar.gz .
SHA=$(sha256sum ~/front-test.tar.gz | cut -d' ' -f1)
~/deploy/bin/deploy-front.sh deploy 20261021-0 $SHA < ~/front-test.tar.gz
~/deploy/bin/deploy-front.sh status
ls -ld ~/docs
rm ~/front-test.tar.gz
```
- 정상: 마지막에 `확인: / → 200, index.html 일치 예`, `배포 완료`. `ls -ld` 가 `drwxr-s--- atmswas jeus`
- 이상하면 `~/deploy/bin/deploy-front.sh rollback`

### 4-3. 배포 키 만들기 (ws01, atmswas)

```bash
mkdir -p ~/atms/infra/jenkins/keys
chmod 700 ~/atms/infra/jenkins/keys
ssh-keygen -t ed25519 -N "" -C jenkins-wb01-deploy -f ~/atms/infra/jenkins/keys/wb01-deploy
ls -l ~/atms/infra/jenkins/keys
```
FileZilla 로 `wb01-deploy.pub`(공개키만)를 PC 로 받아 wb01 `~/deploy/` 에 올린다. **개인키 `wb01-deploy` 는 옮기지 않는다.**

### 4-4. wb01 에 공개키 등록 (atmswas)

4-3 의 `wb01-deploy.pub` 를 wb01 `~/deploy/` 에 올린 뒤, 4-1 의 스크립트에 공개키 파일을 주어 다시 실행한다 (출발 IP 기본 `16.88.122.113`, A-5 에서 다르게 나왔으면 두 번째 인자로):
```bash
bash ~/deploy/kit/deploy/webtob/install-wb01.sh ~/deploy/wb01-deploy.pub
cat ~/.ssh/authorized_keys
rm ~/deploy/wb01-deploy.pub
```
- 스크립트가 `~/.ssh`(700)·`authorized_keys`(600) 를 만들고, 같은 키의 예전 줄이 있으면 바꿔 쓴 뒤 `restorecon` 으로 SELinux 라벨을 맞춘다
- `cat` 결과에 `restrict,from="16.88.122.113",command="/home/atmswas/deploy/bin/deploy-front.sh" ssh-ed25519 AAAA… jenkins-wb01-deploy` 한 줄이 있어야 한다
- 출력의 `ls -Z` 가 `ssh_home_t` 가 아니면 `restorecon -Rv ~/.ssh` (라벨이 틀리면 sshd 가 파일을 못 읽어 키 인증이 실패한다)

<details><summary>스크립트 없이 수동으로 할 때</summary>

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
K="$(cat ~/deploy/wb01-deploy.pub)"
echo "restrict,from=\"16.88.122.113\",command=\"/home/atmswas/deploy/bin/deploy-front.sh\" $K" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
ls -Zd ~/.ssh ~/.ssh/authorized_keys
```
</details>

### 4-5. ws01 에서 키 시험 (atmswas, Jenkins 등록 전)

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
ssh-keyscan -t ed25519 16.88.122.112 >> ~/.ssh/known_hosts
ssh-keygen -lf ~/.ssh/known_hosts
ssh -i ~/atms/infra/jenkins/keys/wb01-deploy -o BatchMode=yes atmswas@16.88.122.112 status
ssh -i ~/atms/infra/jenkins/keys/wb01-deploy -o BatchMode=yes atmswas@16.88.122.112 id
```
- `ssh-keygen -lf` 의 `SHA256:…` 값이 wb01 에서 `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` 결과와 같아야 한다 (다른 서버가 아닌지 확인)
- `status` → wb01 의 `DocBase : …`, `현재 : …` 가 나오면 성공
- `id` → `사용법: …` 이 나와야 정상 (= 키로는 다른 명령이 안 됨)

| 증상 | 원인·대처 |
|---|---|
| `Permission denied (publickey…)` | `from=` IP 불일치(A-5), `~/.ssh` 권한(700/600)·SELinux 라벨(4-4), 또는 wb01 sshd 가 키 인증을 막음 → 앞의 둘 확인 후에도 같으면 서버 담당 확인 |
| `Host key verification failed` | `known_hosts` 등록 안 됨 → 첫 세 줄 다시 |
| `Connection timed out` | 네트워크 → 22 개방 확인 결과 재확인 |

### 4-6. Jenkins 컨테이너 known_hosts (ws01, atmswas)

```bash
podman exec atms-jenkins sh -c "mkdir -p ~/.ssh && chmod 700 ~/.ssh && ssh-keyscan -t ed25519 16.88.122.112 >> ~/.ssh/known_hosts"
podman exec atms-jenkins ssh-keygen -lf /var/jenkins_home/.ssh/known_hosts
```
`SHA256:…` 이 4-5 의 값과 같아야 한다. `/var/jenkins_home` 은 데이터 폴더라 컨테이너를 다시 만들어도 남는다.

### 4-7. Gitea·Jenkins 를 같은 podman 네트워크로 (ws01, atmswas)

> **완료 (2026-10-22)**: 1~5 진행, 5 의 결과 `200` / `200` / `WB01-22-OK`.
>
> 확인 기록: 두 컨테이너 모두 원래 `pasta` 모드. 서버에서 `127.0.0.1:8090/login` → `200` (Jenkins 정상). Gitea 컨테이너 → `http://16.88.122.113:8090/login` → `curl: (7) Failed to connect … Could not connect to server` (0ms, 연결 거부) — pasta 모드에서는 컨테이너가 서버 IP 를 자기 주소로 갖고 있어 요청이 자기 자신에게 가는 것으로 본다. `host.containers.internal` 은 처음 `000` 이 주소 오타(`host.cotaines.intenal`)였고, 오타 없이 `-sS` 로 다시 쳤을 때는 오류 출력이 없어 **됐을 가능성**이 있다 (`-w "%{http_code}"` 로는 재확인 안 함). 서버 IP 가 바뀌어도 영향이 없는 컨테이너 이름 방식(atms-net)을 유지한다 (사용자 결정 2026-10-22).
> 주의: 3 의 `grep … || sed …` 는 한 줄로 끝까지 입력해야 한다 (파일 경로가 잘리면 `sed: no input files`).

**1. 원인 확인 (기록용, 결과 사진 공유)**
```bash
podman exec atms-gitea curl -sS -m 5 -o /dev/null http://host.containers.internal:8090/login
podman exec atms-gitea curl -sS -m 5 -o /dev/null http://16.88.122.113:8090/login
podman exec atms-gitea cat /etc/hosts
podman inspect atms-gitea atms-jenkins --format '{{.Name}} {{.HostConfig.NetworkMode}}'
podman info --format '{{.Host.NetworkBackend}}'
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8090/login
```
- 앞의 두 줄은 `000` 대신 이유(`Connection refused` / `timed out` / `Could not resolve host`)를 보여 준다
- 마지막 줄(서버에서 직접)이 `200` 이면 Jenkins 자체는 정상

**2. 네트워크 만들기**
```bash
podman network exists atms-net || podman network create atms-net
podman network inspect atms-net --format '{{.DNSEnabled}}'
```
`true` 여야 컨테이너 이름으로 찾을 수 있다.

**3. Gitea 를 네트워크에 붙이기** (Quadlet 파일에 한 줄 추가 → 재기동, 데이터 유지)
```bash
grep -q '^Network=' ~/.config/containers/systemd/atms-gitea.container || sed -i '/^PublishPort=/a Network=atms-net' ~/.config/containers/systemd/atms-gitea.container
grep -n 'Network\|PublishPort' ~/.config/containers/systemd/atms-gitea.container
systemctl --user daemon-reload
systemctl --user restart atms-gitea
curl -s http://127.0.0.1:3000/api/healthz
```
새 배포킷이면 `install-gitea.sh 16.88.122.113` 재실행으로 같은 결과 (네트워크 생성·연결 포함).

**4. Jenkins 를 네트워크에 붙여 다시 만들기** (데이터 유지)
```bash
podman rm -f atms-jenkins
podman run -d --name atms-jenkins --restart=always --network atms-net -p 8090:8080 --env-file ~/atms/infra/jenkins/jenkins.env -v ~/atms/infra/jenkins/home:/var/jenkins_home:Z --memory=2g localhost/nh-atms-jenkins:bank
```
npm 캐시(A-4)를 이미 마운트해 쓰고 있었다면 `-v <npm-cache 경로>:/cache/npm:Z` 도 함께 넣는다. 1~2분 뒤 `curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8090/login` → `200`.

**5. 서로 접속 확인**
```bash
podman exec atms-gitea curl -s -m 5 -o /dev/null -w "%{http_code}\n" http://atms-jenkins:8080/login
podman exec atms-jenkins curl -s -m 5 -o /dev/null -w "%{http_code}\n" http://atms-gitea:3000/api/healthz
podman exec atms-jenkins bash -c "echo > /dev/tcp/16.88.122.112/22" && echo WB01-22-OK
```
- `200`, `200`, `WB01-22-OK` 이면 완료 (마지막 줄: 네트워크를 바꾼 뒤에도 Jenkins → wb01:22 가 되는지)
- Gitea 웹훅 허용 목록은 바꾸지 않는다 (`private` = atms-jenkins 의 10.89.x 포함)
- 네트워크를 바꿔도 Jenkins 의 바깥 접속은 서버 IP 로 나간다 (예상). B-6(`MODE=status`)에서 `Permission denied` 가 나오면 `from=` IP 불일치를 의심 → A-5 재확인

| 증상 | 대처 |
|---|---|
| 5 의 첫 두 줄이 `000`, `Could not resolve host` | `DNSEnabled` 가 `false` 이거나 한쪽이 네트워크에 안 붙음 → `podman inspect atms-gitea atms-jenkins --format '{{.Name}} {{.HostConfig.NetworkMode}}'` 가 둘 다 `atms-net` 인지 |
| `podman network create` 오류 | 결과 사진 공유 (netavark·aardvark-dns 구성 확인 필요) |
| Gitea 재기동 후 `healthz` 응답 없음 | `podman logs --tail 50 atms-gitea`, `systemctl --user status atms-gitea` |

## 5. 방화벽(PC → 113:3000·8090) 개방 후

| # | 작업 | 위치 | 비고 |
|---|---|---|---|
| B-1 | Gitea: 조직·저장소(`NH-ATMS/nh-atms`) 생성, 팀원 계정, PC 에서 첫 push | PC 브라우저, PC git | [GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) 3-2 ~ 4장 |
| B-2 | Gitea: `jenkins-bot` 계정 + 토큰 (저장소 읽기) | Gitea 화면 | 저장소에 jenkins-bot 을 읽기 권한으로 추가 |
| B-3 | Jenkins Credentials 2개 | Jenkins 화면 → Jenkins 관리 → Credentials → System → Global | `gitea-jenkins-bot`: Username with password (jenkins-bot / 토큰). `wb01-deploy-key`: SSH Username with private key, Username `atmswas`, Private key "Enter directly" 에 `cat ~/atms/infra/jenkins/keys/wb01-deploy` 내용을 붙여 넣기 |
| B-4 | ws01 개인키 파일 삭제 | ws01 atmswas | B-3 등록 확인 후 `rm ~/atms/infra/jenkins/keys/wb01-deploy` (공개키 `.pub` 는 남겨도 됨). 잃어버리면 새로 만들어 4-3~4-4 반복 |
| B-5 | Jenkins 잡 `nh-atms-frontend` | Jenkins 화면 | 새 Item → Pipeline → "Pipeline script" 에 `deploy/jenkins/pipelines/frontend.Jenkinsfile` 붙여 넣기 → `GITEA_REPO` 수정 → 저장 |
| B-6 | SSH 연결 시험 | Jenkins | "Build with Parameters" → `MODE=status` → 콘솔에 wb01 의 `DocBase : …` 가 나오면 성공 (첫 실행으로 매개변수·트리거도 등록됨) |
| B-7 | 첫 빌드·배포 | Jenkins → PC 브라우저 | `MODE=build-deploy` → 콘솔 끝에 `배포 완료: <버전>` → `http://16.88.122.112:9000` 새로고침 |
| B-8 | Jenkins notifyCommit 토큰 | Jenkins 화면 → Jenkins 관리 → Security → "Git plugin notifyCommit access tokens" | 이름 `gitea-webhook` 으로 토큰 추가 → 값 복사 (한 번만 보임) |
| B-9 | Gitea 웹훅 등록 | Gitea 화면 → 저장소 `NH-ATMS/nh-atms` → 설정 → Webhooks → 웹훅 추가 → Gitea | 대상 URL `http://atms-jenkins:8080/git/notifyCommit?url=http://atms-gitea:3000/NH-ATMS/nh-atms.git&token=<B-8 토큰>` · HTTP 방법 **POST** (GET 은 push 내용을 URL 에 붙여 `414 URI Too Long`, 2026-10-23) · 트리거 "Push 이벤트" · 브랜치 필터 `main` · 활성. `url=` 값은 Jenkinsfile 의 `GITEA_REPO` 와 **글자까지 같아야** 한다 (Jenkins 가 이 값으로 잡을 찾음) |
| B-10 | 웹훅 시험 | Gitea 웹훅 화면 → "테스트 전송" → 최근 전송 | 응답 200, 본문에 `Scheduled polling of nh-atms-frontend` 가 보이면 연결 성공 (새 커밋이 없으면 빌드는 안 생김). 이후 실제 push → 잠시 뒤 빌드 시작 |
| B-11 | 이후 | — | push 하면 웹훅으로 바로 빌드·배포 (보통 수십 초 안에 시작) |

## 6. 백엔드·WebtoB 연동이 끝난 뒤

| # | 작업 | 비고 |
|---|---|---|
| C-1 | JEUS atms01 기동 → wb01 `ss -tn \| grep 19000` 에 ESTAB | WAS 담당, [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 5~6 |
| C-2 | `curl -sI http://127.0.0.1:9000/api/` → 302(에러 페이지)가 아닌 백엔드 응답 | A-12 의 `*URI` 적용 후 |
| C-3 | 브라우저: 로그인 → 메뉴 이동 → 하위 화면에서 F5 | F5 404 면 A-12 ② 협의 결과 적용 |

## 7. 상세 흐름 — 개발자가 소스를 고친 뒤 화면에 반영되기까지

### 7-1. PC: 수정과 로컬 확인

1. `NH-ATMS/Frontend/nh-atms-web` 에서 수정
2. 로컬 확인: `npm run dev` → `http://localhost:3000`. 개발 서버가 `/api` → `localhost:8080`, `/api/batch` → `localhost:8082` 로 넘기므로 로컬 백엔드(`bootRun`)를 띄우거나, 백엔드 없이 화면만 볼 때는 같은 포트의 mock 을 쓴다 (동료의 `mock-api.mjs` 가 이 용도)
3. push 전 빌드 확인: `npm run build` — 빌드 스크립트가 `tsc -b && vite build` 라서 **타입 오류가 하나라도 있으면 Jenkins 빌드도 실패**한다
4. `git commit` → `git push` (Gitea 토큰). 브랜치는 `main`

> **의존성(`package.json`/`package-lock.json`)을 바꾼 경우**: 폐쇄망이라 Jenkins 는 npm 캐시에 있는 패키지만 설치할 수 있다. 새 패키지·버전을 추가했다면 인터넷 PC 에서 캐시를 다시 만들어 반입해야 한다. 반입 전에 push 하면 Jenkins 빌드가 `ENOTCACHED` 로 실패한다 (화면은 이전 버전 그대로).

### 7-2. Gitea → Jenkins: 빌드 시작

- push 하면 Gitea 가 웹훅으로 Jenkins `/git/notifyCommit` 을 부른다 → `url=` 이 같은 저장소를 쓰는 잡(`nh-atms-frontend`)이 Gitea 에서 새 커밋을 확인 → 새 커밋이 있으면 `build-deploy` 로 빌드. 수동은 "Build with Parameters"
- 주기 폴링이 없으므로 **웹훅이 실패하면 자동 빌드가 안 된다** (Gitea 웹훅 화면의 "최근 전송"에 빨간 표시). 그때는 수동 빌드하고 9장 문제 해결 확인
- 백엔드만 바꾼 push 도 같은 저장소라 웹훅이 오고 화면이 다시 빌드·배포된다 (내용은 같아 영향 없음)
- 실행기 1개 + `disableConcurrentBuilds` → 빌드가 겹치면 차례를 기다린다. ws01 은 2코어를 JEUS 와 같이 쓰므로 빌드 중 JEUS 응답이 느려질 수 있다

### 7-3. Jenkins: 단계 (`frontend.Jenkinsfile`)

| 단계 | 하는 일 | 실패 시 |
|---|---|---|
| ① 소스 | `jenkins-bot` 토큰으로 Gitea 에서 받기 (모든 MODE 에서 실행 — 웹훅 때 새 커밋 비교 기준 유지) | 인증 실패 → 토큰·저장소 권한 |
| ② 빌드 | `npm ci --offline --cache /cache/npm` → `npm run build` (`NODE_OPTIONS=--max-old-space-size=1024`) | `ENOTCACHED` → 캐시 재반입 / 타입 오류 → 로그의 파일·줄 |
| ③ 묶기 | `dist/` 안의 파일을 `nh-atms-web-dist-<YYYYMMDD>-<빌드번호>.tar.gz` 로, `.sha256`·`VERSION`·`COMMIT` 과 함께 **빌드 결과로 보관** (최근 10개 빌드). 빌드 설명에 버전 표시 | `dist/index.html` 없음 → 빌드 확인 |
| ④ 배포 | `ssh atmswas@16.88.122.112 "deploy <버전> <sha256> <커밋>" < tar.gz` (`wb01-deploy-key`, 호스트 키 확인 필수) | wb01 스크립트의 종료코드가 그대로 빌드 결과. SSH 실패 → 4-5 표 |

### 7-4. wb01: `deploy-front.sh deploy` (SSH 로 자동 실행)

1. 버전·sha256·커밋 형식 확인 → 다른 배포가 진행 중이면 최대 5분 대기 (`flock`)
2. 표준입력을 `~/deploy/inbox/nh-atms-web-dist-<버전>.tar.gz` 로 저장 (200MB 초과·빈 파일은 거부) → sha256 이 Jenkins 가 보낸 값과 다르면 중단 (화면 그대로)
3. `swap-deploy.sh deploy <tar.gz> /home/atmswas/docs`
   - 새 폴더에 풀기 → 기존 docs 의 `error/` 가 있으면 옮겨 유지
   - 그룹(jeus)·권한(2750)을 기존 docs 와 맞춤 → webatms 가 읽을 수 있음
   - 기존 docs → `~/deploy/backup/docs-<시각>`, 새 폴더 → `docs` (이름 바꾸기 2번이라 화면이 비는 시간은 순간적)
   - 백업은 최근 5개만
4. 확인: `curl http://127.0.0.1:9000/` 가 `200` 이고, WebtoB 가 주는 `index.html` 이 새 파일과 같은지 → 맞으면 `front-current.txt`·`front-deploy.log` 기록, 아니면 **자동으로 직전 버전으로 되돌리고 실패**로 끝냄
5. inbox 는 최근 5개만 남김

WebtoB 는 요청마다 파일을 읽으므로 **재기동 없이 바로 반영**된다.

### 7-5. 브라우저 반영

- `index.html` 은 이름이 고정, JS·CSS 는 `assets/index-<해시>.js` 처럼 빌드마다 이름이 바뀐다 → 새 `index.html` 만 받으면 새 JS·CSS 를 가져온다
- WebtoB 가 `ETag`/`Last-Modified` 를 보내므로(2026-10-21 확인) 새로고침 때 바뀐 `index.html` 을 받는다. 그대로면 `Ctrl+F5`
- 배포 순간 이미 열려 있던 화면은 새로고침 전까지 이전 버전으로 동작한다

### 7-6. 되돌리기

| 상황 | 방법 |
|---|---|
| 방금 배포한 화면이 이상함 | Jenkins `MODE=rollback` (또는 wb01 에서 `~/deploy/bin/deploy-front.sh rollback`) → 직전 백업으로, 몇 초 |
| 특정 이전 빌드로 | Jenkins `MODE=redeploy`, `REDEPLOY_BUILD=<빌드 번호>` (최근 10개 빌드) |
| 소스 자체를 되돌림 | PC 에서 `git revert` → push → 웹훅으로 자동 빌드·배포 (기록이 남는 정석) |
| 고칠 때까지 자동 배포를 멈춤 | Jenkins 잡 화면 → "Disable Project" (다시 켜면 그동안 쌓인 커밋으로 빌드) |

되돌린 뒤 새 커밋이 push 되면 그 최신 소스로 다시 빌드·배포된다. 되돌린 원인이 고쳐지지 않았으면 먼저 "Disable Project".

### 7-7. 한눈에 보기

```
[PC]      수정 → npm run dev 확인 → npm run build 통과 → git push
            │
[Gitea]   커밋 저장 ─(웹훅)─▶ [Jenkins] 소스 → npm ci --offline → tsc+vite build
                                              → tar.gz+sha256 보관 → ssh "deploy …" < tar.gz
            │
[wb01]    deploy-front.sh: sha256 확인 → 새 폴더 → 권한 맞춤 → docs 교체 (백업 5개) → 200·index.html 확인
            │                 (실패 → 자동 되돌림 → Jenkins 빌드 실패)
[브라우저] 새로고침 → 새 index.html → 새 assets → 반영 완료 (WebtoB 재기동 없음)
```

## 8. 운영 명령

| 할 일 | Jenkins (`nh-atms-frontend`) | wb01 에서 직접 (atmswas) |
|---|---|---|
| 지금 배포된 버전·기록·백업 | `MODE=status` | `~/deploy/bin/deploy-front.sh status` |
| 직전 버전으로 | `MODE=rollback` | `~/deploy/bin/deploy-front.sh rollback` |
| 이전 빌드 다시 배포 | `MODE=redeploy`, `REDEPLOY_BUILD=<번호>` | — |
| 빌드만 (배포 안 함) | `MODE=build-only` | — |
| 호출 기록 | — | `tail -n 20 ~/deploy/front-deploy.log` (`REJECT` 줄 = 허용되지 않은 명령 시도) |
| 키 즉시 차단 | — | `vi ~/.ssh/authorized_keys` 에서 `jenkins-wb01-deploy` 줄 삭제 |
| 웹훅 전송 기록 | Gitea 저장소 → 설정 → Webhooks → 해당 웹훅 → 최근 전송 | — |

웹훅 문제 해결:

| 증상 (Gitea "최근 전송") | 원인·대처 |
|---|---|
| `webhook can only call allowed HTTP servers` / 전송 거부 | 대상 URL 이 `atms-jenkins` 가 아님(서버 IP 등) 또는 `ALLOWED_HOST_LIST` 가 `private` 가 아님 → 4-7 |
| 연결 실패·시간 초과 | Gitea·Jenkins 가 같은 네트워크가 아님 → 4-7 의 5 |
| 응답 403·401 | notifyCommit 토큰 누락·오타 → B-8 토큰 다시 만들어 URL 수정 |
| 응답 200 인데 `No git jobs using repository: …` | `url=` 이 Jenkinsfile `GITEA_REPO` 와 다름, 또는 잡을 한 번도 실행하지 않아 트리거 미등록 (B-6) |
| 응답 200, `Scheduled polling` 인데 빌드 없음 | 새 커밋이 없음(테스트 전송은 정상) 또는 잡이 Disable 상태 |

## 9. 참고

- 동료의 `~/mock/mock-api.mjs`(8080·8082)는 `vite.config.ts` 개발 서버 프록시(`/api` → 8080, `/api/batch` → 8082)와 같은 포트로 만든 가짜 API다. 이 계획에는 필요 없다 (기록만, 실행하지 않음)
- 화면 코드는 API 를 `/api`(상대경로)로 부르므로 빌드할 때 서버 주소를 넣을 필요가 없다
- WebtoB 재기동 프로그램(`webtobMENU/webtobRstart`)은 atmswas 가 실행하지 않는다. 화면 파일 교체는 재기동이 필요 없다
- 검토 이력 (2026-10-21): ① PC 수동 업로드 → ② A안: Gitea 패키지 저장소 + wb01 `systemd --user` 타이머(1분)가 당겨오기 → ③ **B안: Jenkins SSH** (ws01·Jenkins → wb01:22 개방 확인 후 변경). A안용 wb01 linger 는 필요 없어짐
- 빌드 시작 방식 (2026-10-22): 1분 폴링 → **Gitea 웹훅 + git 플러그인 notifyCommit**, 보조 폴링 없음. 검토한 다른 방식: Multibranch + Gitea 플러그인(`/gitea-webhook/post`, Jenkinsfile 을 NH-ATMS 저장소에 넣어야 함), 원격 빌드 URL + Jenkins API 토큰(웹훅에 전체 권한 토큰이 저장됨) — 채택 안 함
