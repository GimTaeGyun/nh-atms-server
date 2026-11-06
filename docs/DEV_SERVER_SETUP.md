# 개발서버 셋팅 가이드 (atmswas 기준)

작성일: 2026-10-12 · 서버 `nbatmsltws01` · 계정 **`atmswas`** · 기준 경로 **`/home/atmswas`**

> - **2026-10-30: 처음부터 끝까지(반입물 → 서버 → Gitea·Jenkins → 배포 → JEUS·WebtoB → 2호기 → 운영·백업·보안·트러블슈팅) 한 권으로 정리한 통합 매뉴얼은 [INFRA_MANUAL.md](INFRA_MANUAL.md)** — 이 문서는 단계별 원본과 진행 이력이다
> - 2026-10-12 결정: 모든 서버 작업을 **atmswas 로 로그인해서** 한다. `/home/userATMS` 는 사용하지 않는다.
> - 명령은 **한 줄씩** 입력하고 `sudo`/`su` 는 쓰지 않는다 (HIWARE, 금지명령 3회 → 세션 차단).
> - 이 문서 범위: 반입 파일 배치 → 배포킷 → Gitea → Jenkins 기동. WAR 배포는 [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md).

```
1 상태 확인 → 2 폴더 만들기 → 3 PC 에서 업로드 → 4 무결성 확인 → 5 배포킷 정리·점검
→ 6 (필요 시) 1일 root 작업 → 7 이미지 적재 → 8 Gitea → 9 Jenkins → 10 방화벽 신청·최종 점검
```

### 진행 현황 (2026-10-23)

| 단계 | 상태 |
|---|---|
| 1~5 | 완료 (2026-10-14) |
| 6 root 작업 | 2026-10-15 완료: linger `yes`, firewalld 꺼져 있음. **CPU 제한은 보류 (2026-10-16 결정)** → 컨테이너는 메모리 제한만 ([ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2-9) |
| 7 이미지 적재 | 완료 (Gitea 177MB, Jenkins 780MB) |
| 8 Gitea | 2026-10-21 재구성 때 Quadlet 으로 재설치·관리자 생성 완료, 2026-10-22 `atms-net` 연결 |
| 9 Jenkins | 2026-10-21 `--restart=always`·`--network atms-net` 로 재생성 (플러그인 87개), 2026-10-22 캐시 마운트. 이후 재생성은 `run-jenkins.sh` |
| 10 방화벽 | 2026-10-16 신청 완료. 개방 확인 대기 |
| 재구성 | 2026-10-16 `/home/atmswas` 에 새 디스크가 덮여 기존 내용이 가려짐 → 2026-10-21 30GB 증설 후 2~9단계를 다시 해서 **Gitea·Jenkins 정상 기동** ([WORK_LOG.md](WORK_LOG.md)) |
| 캐시 | 2026-10-22 offline-seed(npm·Gradle) 마운트 완료 (9-5) |
| 배포킷 | 2026-10-23 판 (서버 실행 스크립트 추가: `run-jenkins.sh`, `install-wb01.sh`, `deploy-war.sh`, `install-deploy-war.sh`) → 아래 "배포킷 갱신" 으로 ws01·wb01 에 반입 |
| JEUS | 2026-10-22 설치 폴더가 비었다가 **이후 WAS 담당이 복구, atms01 기동** (사용자 확인. 경로·포트·docBase 등록 여부는 [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) 로 다시 확인) |

작업 이력 전체: [WORK_LOG.md](WORK_LOG.md)

### 배포킷 갱신 (2026-10-23 판으로, 이후 재반입 때도 같은 방법)

> 2026-10-14 반입본의 보정 명령(keep-id·`--cpus` 제거 등)은 이 판에 모두 들어 있어 더 이상 필요 없다.

① PC (인터넷): GitHub `main` → Code → Download ZIP → `ATMS-Server-main.zip`. 반입 신청 시 sha256 을 같이 적는다 (`Get-FileHash .\ATMS-Server-main.zip -Algorithm SHA256`)

①-1 내부 PC: **zip 이 2026-11-04 이전에 받은 판이면** 풀린 폴더의 `deploy\gitea\gitea-backup.service` 를 2026-11-04 수정본(데이터 폴더 밖 `~/atms/backup/gitea` 에 저장, `-f -`)으로 바꾼 뒤 올린다 — 안 바꾸면 서버 배포킷의 수정본이 옛 내용으로 돌아간다 ([INFRA_MANUAL.md](INFRA_MANUAL.md) T65, 6-5). 확인: `findstr /c:"-f - >" deploy\gitea\gitea-backup.service` 가 한 줄 나오면 수정본

② 내부 PC: 7-Zip 으로 풀고, 풀린 폴더 **안의** `bin`, `conf`, `deploy`, `docs`, `README.md` 만 FileZilla(바이너리)로 올린다 (같은 이름은 **덮어쓰기**). `NH-ATMS/` 는 올리지 않는다 (소스는 Gitea 로)

| 서버 | 올릴 곳 | 비고 |
|---|---|---|
| ws01 | `/home/atmswas/atms/` | 덮어써도 `conf/*.env`(실제 설정)·`infra/`·`images/`·`release/` 는 zip 에 없으므로 그대로 남는다 |
| wb01 | `/home/atmswas/deploy/kit/` (새로 만듦) | `deploy` 폴더만 올려도 된다. **`~/docs` 아래에는 절대 두지 않는다** (9000 으로 내려받아짐) |

③ ws01 (atmswas):
```bash
cd ~/atms
find bin deploy -name "*.sh" -exec sed -i 's/\r$//' {} +
find bin deploy -name "*.sh" -exec chmod 700 {} +
for f in bin/*.sh deploy/*/*.sh; do bash -n "$f" || echo "FAIL $f"; done
ls deploy/jenkins deploy/jeus
```
```bash
sed -i 's/\r$//' deploy/gitea/gitea-backup.service
grep -c -- "-f - >" deploy/gitea/gitea-backup.service
diff deploy/gitea/gitea-backup.service ~/.config/systemd/user/gitea-backup.service && echo SAME
```
- `grep -c` 가 `1` 이어야 한다 (`0` 이면 옛 백업 서비스 — ①-1 을 빠뜨린 것, 수정본을 다시 올린다). `diff` 가 `SAME` 이면 실제 적용본과 같다. 내용이 다르면(주석 차이 제외) `cp deploy/gitea/gitea-backup.service ~/.config/systemd/user/` → `systemctl --user daemon-reload`
- `bash -n` 은 아무것도 안 나와야 한다. `ls` 에 `run-jenkins.sh jenkins.env.example` / `deploy-war.sh install-deploy-war.sh swap-deploy.sh` 가 보이면 새 판
- 이미 떠 있는 Gitea·Jenkins 는 다시 만들 필요 없다. Jenkins 를 다시 만들 일이 생기면 `~/atms/deploy/jenkins/run-jenkins.sh` (9단계)

④ wb01 (atmswas): [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4-1 (`install-wb01.sh`)

## 완성 후 구조

```
/home/atmswas/                          750 atmswas:jeus
├─ catms/ cajou/ cabat/ cfile/ cifin/ cifex/   2750  JEUS 앱 6개 (WAR 배포 시 채움)
├─ data/                                2770  앱 저장 파일
├─ deploy/                              700   WAR 배포 백업·이력
├─ atms/                                700   ★ 배포킷 (ATMS-Server.zip 내용)
│  ├─ bin/ conf/ deploy/ docs/ README.md ...
│  ├─ inbox/  release/                  WAR 업로드·보관
│  ├─ images/                           gitea-rootless.tar, nh-atms-jenkins-bank.tar, ATMS-Server.zip, *.txt
│  └─ infra/                            700
│     ├─ gitea/    data/ config/ gitea.env(600)
│     └─ jenkins/  home/ cache/{gradle,npm} keys/ jenkins.env(600)
├─ .config/containers/systemd/atms-gitea.container    (linger 있을 때 Gitea 자동기동)
├─ .config/systemd/user/gitea-backup.*               (Gitea 매일 백업)
└─ .local/share/containers/                           (Podman 이미지 저장소)
```
권한·이유: [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 3장

---

## 1단계. 상태 확인 (atmswas 로 로그인, 읽기만 함)

> **2026-10-14 실행 결과**: 주 그룹 `atms` + 보조 그룹 `jeus` / `/home/atmswas` 는 `atmswas:atms` 755 (2단계에서 바꿈) / `/home` 공용 10G 여유 9.8G (증설 요청) / `XDG_RUNTIME_DIR` 정상 / **subuid·subgid 등록됨** / `Linger=no` / Podman rootless / firewalld `inactive` / 3000·8090 비어 있음 / **cgroup `cpu` 미위임** (8단계에서 확인) → **1일 root 없이 2~9단계 진행 가능** (linger 만 나중에). 상세: [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 0-1

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
ss -tln | grep -E ':3000|:8090'
python3 --version
```

| 명령 | 정상 | 아니면 |
|---|---|---|
| `id` | `atmswas`, 그룹에 `jeus(2002)` | jeus 그룹이 아니면 인프라 요청 |
| `echo $HOME` | `/home/atmswas` | 다르면 결과 공유 (문서 경로 조정 필요) |
| `ls -ld` | `drwxr-x--- atmswas jeus` | 2단계에서 `chgrp`/`chmod`, 소유자가 다르면 6단계(2-7) |
| `df -h` | 여유 **10GB 이상** 권장 (최소 5GB) | 부족하면 인프라에 디스크 요청 (LAYOUT 3-3). 그동안은 7단계 후 tar 삭제 |
| `XDG_RUNTIME_DIR` | `/run/user/<숫자>` | 비어 있으면 rootless Podman·`systemctl --user` 가 안 될 수 있음 → 결과 공유 |
| `grep ... subuid subgid` | `atmswas:숫자:65536` 두 줄 | **없으면 7단계 전에 6단계(1일 root) 필수** (Jenkins 이미지 적재 불가) |
| `Linger` | `Linger=yes` | `no` 여도 진행 가능. 단 **로그아웃·재부팅하면 Gitea·Jenkins 가 내려가서** (2026-10-15 확인) 다시 접속할 때마다 손으로 띄워야 함 → 1일 root 때 켠다 |
| `Rootless` | `true` | 결과 공유 |
| `CgroupControllers` | `[memory pids]` 이상 (`cpu` 가 있으면 CPU 제한도 적용) | `cpu` 없음 = RHEL 9 기본. 스크립트가 CPU 제한을 빼고 기동, 필요하면 1일 root 2-9 |
| `firewalld` | `active` / `inactive` | `active` 면 6단계에서 3000·8090 개방 |
| `ss -tln` | 출력 없음 | 사용 중이면 결과 공유 |

## 2단계. 폴더 만들기 (atmswas, root 불필요)

```bash
cd /home/atmswas
mkdir -p catms cajou cabat cfile cifin cifex data deploy/backup
mkdir -p atms/inbox atms/release atms/images atms/infra
chgrp jeus /home/atmswas /home/atmswas/catms* /home/atmswas/data
chmod 750 /home/atmswas
chmod 2750 /home/atmswas/catms*
chmod 2770 /home/atmswas/data
chmod 700 /home/atmswas/deploy /home/atmswas/atms /home/atmswas/atms/infra
ls -la /home/atmswas
```
- `catms*`·`data` 는 JEUS 배포용이다. 지금 만들어 두면 WAS 담당에게 회신할 수 있다 (LAYOUT 7-3 문구).
- `chgrp` 에서 `Operation not permitted` 가 나오면 atmswas 가 jeus 그룹이 아닌 것 → 1단계 `id` 결과와 함께 인프라 요청.

## 3단계. 내부 PC → 서버 업로드 (FileZilla, **atmswas 로 접속**)

FileZilla 메뉴 **전송 → 전송 유형 → 바이너리** 를 먼저 선택한다.

**3-1. 반입 파일 6개 → `/home/atmswas/atms/images/`**

| 파일 | 비고 |
|---|---|
| `gitea-rootless.tar`, `nh-atms-jenkins-bank.tar` | 이미지 |
| `ATMS-Server.zip` | 무결성 확인용으로 같이 올림 |
| `SHA256SUMS.txt`, `versions.txt`, `jenkins-plugins.txt` | 확인·기록용 |

**3-2. 배포킷은 PC 에서 풀어서 올린다**
1. 내부 PC 에서 `ATMS-Server.zip` 우클릭 → 7-Zip → `ATMS-Server\ 에 압축 풀기`
2. 풀린 폴더 **안의 내용**(`bin`, `conf`, `deploy`, `docs`, `README.md` …)을 `/home/atmswas/atms/` 에 올린다 → 결과가 `/home/atmswas/atms/bin/` 이어야 한다 (`/home/atmswas/atms/ATMS-Server/bin/` 이면 잘못)

서버에서 풀지 않는 이유: Windows PowerShell 로 만든 zip 은 경로 구분자가 `\` 로 저장될 수 있어, 리눅스에서 풀면 `deploy\jenkins\Dockerfile` 같은 이름의 파일 하나가 생겨 폴더 구조가 깨진다.

## 4단계. 무결성 확인

```bash
cd ~/atms/images
ls -l
sha256sum -c SHA256SUMS.txt
```
모든 줄이 `OK` 면 정상.

| 증상 | 대처 |
|---|---|
| `FAILED` | 그 파일만 FileZilla(바이너리)로 다시 올림 |
| `No such file` | 안 올렸거나 이름이 다름 → `ls -l` 확인 |
| 모든 줄이 이상하게 실패 | `sed -i 's/\r$//' SHA256SUMS.txt` 후 다시 (줄바꿈 문제) |

## 5단계. 배포킷 정리·점검

```bash
cd ~/atms
ls
sed -i 's/\r$//' bin/*.sh deploy/gitea/*.sh deploy/jeus/*.sh
chmod +x bin/*.sh deploy/gitea/*.sh deploy/jeus/*.sh
bash -n deploy/gitea/install-gitea.sh && echo SYNTAX-OK
bin/precheck.sh 8090
```

| 명령 | 정상 |
|---|---|
| `ls` | `bin conf deploy docs images inbox infra release README.md ...` |
| `sed`, `chmod` | 출력 없음 (Windows 줄바꿈 제거, 실행 권한 부여) |
| `bash -n` | `SYNTAX-OK` |
| `precheck.sh 8090` | 1단계 항목 + 8090 포트를 한 번에 점검. `[NG]` 가 있으면 6단계 |

`bin/init-dirs.sh` 는 실행하지 않는다 (컨테이너 대안 경로(보류)용 설정 생성 스크립트).

## 6단계. (필요할 때만) 1일 root 작업

1단계·`precheck.sh` 결과에 따라 root 권한 받는 날 [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2장을 처리한다.

| 항목 | 체크리스트 | 필요 조건 |
|---|---|---|
| atmswas subuid/subgid 등록 → 이후 atmswas 로 `podman system migrate` | 2-1 | subuid 가 없을 때 (**필수**) |
| atmswas linger 활성화 | 2-2 | `Linger=no` 일 때 (권장) — **2026-10-15 완료** |
| rootless cgroup `cpu` 위임 | 2-9 | **보류 (2026-10-16)** — CPU 제한을 다시 하기로 할 때 |
| firewalld 3000·8090 개방 | 2-3 | firewalld `active` 일 때 |
| `/home/atmswas` 소유·권한 | 2-7 | `atmswas jeus`·750 이 아닐 때 |
| `/home/atmswas` 디스크 | 2-8 (보통 인프라) | 여유 부족할 때 |

## 7단계. 이미지 적재

```bash
cd ~/atms/images
podman load -i gitea-rootless.tar
podman load -i nh-atms-jenkins-bank.tar
podman images
podman run --rm --entrypoint sh localhost/nh-atms-jenkins:bank -c "ls /usr/share/jenkins/ref/plugins/*.jpi | wc -l"
```

| 확인 | 정상 |
|---|---|
| `podman images` | `docker.io/gitea/gitea  latest-rootless`, `localhost/nh-atms-jenkins  bank` |
| 마지막 줄 | `87` (Jenkins 플러그인) |

| 증상 | 대처 |
|---|---|
| `potentially insufficient UIDs or GIDs` | subuid 없음 → 6단계 후 `podman system migrate`, 다시 load |
| Jenkins 가 `<none>` 으로 적재 | `podman tag <IMAGE ID> localhost/nh-atms-jenkins:bank` |
| `no space left on device` | `df -h /home/atmswas` 확인 → 디스크 요청 |

디스크가 빠듯하면 적재 확인 후 tar 를 지운다 (체크섬 파일은 남김):
```bash
rm ~/atms/images/gitea-rootless.tar ~/atms/images/nh-atms-jenkins-bank.tar
```

## 8단계. Gitea ([GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) 2·3장)

```bash
~/atms/deploy/gitea/install-gitea.sh 16.88.122.113
```
- 서버 IP 는 **PC 가 접속하는 IPv4 `16.88.122.113`** (HIWARE 접속 창 제목의 주소, 2026-10-14 판단 — 방화벽 신청 때 인프라에 서비스 IP 로 맞는지 확인). `hostname -I` 의 첫 값은 `fe80::...%ens161` 같은 **IPv6 내부 주소라 쓰면 안 된다** (스크립트가 거부함)
- 스크립트: `~/atms/infra/gitea/` 생성, `gitea.env`(600) 비밀키 자동 생성, linger 가 있으면 Quadlet(자동기동)·없으면 `podman run`, 상태 확인. 다시 실행하면 IP 를 새 값으로 바꾸고 비밀키는 유지한다
- 자원 제한: **메모리 768MB 만**. CPU 제한은 보류 (2026-10-16 결정, 이유·영향은 ROOT_DAY 2-9). 나중에 하려면 `GITEA_CPUS=0.5 ~/atms/deploy/gitea/install-gitea.sh 16.88.122.113` (cpu 위임 필요)
- 마지막에 **`Gitea 정상 기동`** 이 나오면 성공

관리자 계정 (임시 비밀번호가 한 번만 출력되니 적어 둔다):
```bash
podman exec atms-gitea gitea admin user create --admin --username <관리자ID> --email <메일> --random-password --must-change-password
```

확인:
```bash
curl -s http://127.0.0.1:3000/api/healthz
podman ps --filter name=atms-gitea
```
`"status": "pass"` 포함, `Up` 이면 정상.

자동 백업 (linger=yes 일 때):
```bash
mkdir -p ~/.config/systemd/user
cp ~/atms/deploy/gitea/gitea-backup.service ~/atms/deploy/gitea/gitea-backup.timer ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user enable --now gitea-backup.timer
systemctl --user list-timers | grep gitea
```
조직·팀·저장소 생성, PC 첫 푸시는 방화벽 3000 개방 후 웹 화면에서 (가이드 3-2, 4장).

## 9단계. Jenkins

> 2026-10-23 배포킷부터 **`~/atms/deploy/jenkins/run-jenkins.sh`** 가 9-1·9-2·9-5 ④⑤ 를 한 번에 한다 (폴더·소유자·`jenkins.env`·네트워크·캐시 마운트·`--restart=always`·`podman-restart` enable·`/login` 200 대기). 데이터(`home`, `cache`)는 그대로 두고 컨테이너만 다시 만든다.
> ```bash
> ~/atms/deploy/jenkins/run-jenkins.sh
> ```
> - `jenkins.env` 가 없으면 `deploy/jenkins/jenkins.env.example` 을 복사하고 멈춘다 → `vi ~/atms/infra/jenkins/jenkins.env` 로 `CHANGE_ME`(관리자 비밀번호)를 바꾸고 다시 실행
> - 메모리 바꾸기: `JENKINS_MEMORY=3g ~/atms/deploy/jenkins/run-jenkins.sh`
>
> 아래 수동 명령은 스크립트가 하는 일과 같다 (참고·문제 확인용).

**9-1. 데이터 폴더와 설정 파일**
```bash
mkdir -p ~/atms/infra/jenkins/home
chmod 700 ~/atms/infra/jenkins
echo 'JENKINS_ADMIN_ID=admin' > ~/atms/infra/jenkins/jenkins.env
echo 'JENKINS_ADMIN_PASSWORD=<관리자비밀번호>' >> ~/atms/infra/jenkins/jenkins.env
echo 'JENKINS_URL=http://16.88.122.113:8090/' >> ~/atms/infra/jenkins/jenkins.env
echo 'JAVA_OPTS=-Djenkins.install.runSetupWizard=false -Dfile.encoding=UTF-8 -Xmx1g' >> ~/atms/infra/jenkins/jenkins.env
chmod 600 ~/atms/infra/jenkins/jenkins.env
cat ~/atms/infra/jenkins/jenkins.env
```
- 관리자 계정은 이미지 안의 JCasC(`deploy/jenkins/casc/jenkins.yaml`)가 첫 기동 때 이 값으로 만든다. 비밀번호에 `'` 는 쓰지 않는다
- `JAVA_OPTS` 는 이미지 기본값(설정 마법사 끔, UTF-8)에 힙 1GB 제한을 더한 것 (2코어·JEUS 공존)

**9-2. 기동**
```bash
podman unshare chown -R 1000:1000 ~/atms/infra/jenkins/home
podman network exists atms-net || podman network create atms-net
podman run -d --name atms-jenkins --restart=always --network atms-net -p 8090:8080 --env-file ~/atms/infra/jenkins/jenkins.env -v ~/atms/infra/jenkins/home:/var/jenkins_home:Z --memory=2g localhost/nh-atms-jenkins:bank
```
CPU 제한은 보류 (2026-10-16 결정) → `--cpus` 는 넣지 않는다. 나중에 적용하면 `--memory=2g` 앞에 `--cpus=1.0` 을 넣는다 (cpu 위임 필요). 이미 떠 있으면 `podman rm -f atms-jenkins` 후 다시 실행 (데이터는 `~/atms/infra/jenkins/home` 에 남는다).

linger=yes 이면 재부팅 자동기동을 한 번 켜 둔다 (`--restart=always` 컨테이너를 부팅 때 띄우는 Podman 기본 서비스):
```bash
systemctl --user enable podman-restart.service
systemctl --user is-enabled podman-restart.service
```
- 첫 줄: 데이터 폴더를 컨테이너 안 jenkins(1000) 소유로 맞춘다. 호스트에서는 atmswas 의 subuid 소유로 보이며, atmswas 가 직접 지우거나 바꿀 때는 `podman unshare rm ...` 처럼 `podman unshare` 를 붙인다
- `--userns=keep-id` 는 쓰지 않는다: 2026-10-14 이 서버(podman 5.8.2 / crun 1.27)에서 `crun: readlink (빈 경로): No such file or directory` 로 기동 실패 확인
- `--cpus=1.0` 은 cgroup `cpu` 위임 전에는 넣지 않는다: 위임 전에는 `crun: controller 'cpu' is not available` 로 실패한다 (2026-10-14 확인)
- `--network atms-net`: Gitea 와 같은 podman 네트워크 → 컨테이너끼리 이름(`atms-gitea:3000`, `atms-jenkins:8080`)으로 접속 (소스 받기·웹훅). rootless 기본(pasta) 모드에서는 서버 IP 로 서로 닿지 않았다 (2026-10-22, 연결 거부). 2026-10-22 이 구성으로 재생성·확인 완료. Gitea 는 `install-gitea.sh` 가 같은 네트워크에 붙인다
- `--restart=always`: `unless-stopped` 와 달리 `podman-restart.service` 가 부팅 때 다시 띄워 준다
- `podman info --format '{{.Host.CgroupControllers}}'` 에 `memory` 가 없으면 `--memory=2g` 도 빼야 한다

| 옵션 | 이유 |
|---|---|
| `-p 8090:8080` | 서버 8090 으로 접속 |
| `-v ...:Z` | 데이터를 `~/atms/infra/jenkins/home` 에 보관 (SELinux 때문에 `:Z`) |
| `podman unshare chown -R 1000:1000` (기동 전) | 컨테이너 jenkins(1000) 가 데이터 폴더에 쓸 수 있게 (keep-id 대신) |
| `--memory=2g` | JEUS 와 자원 공유 (CPU 제한은 보류) |

**9-3. 확인 (기동 1~2분)**
```bash
podman logs --tail 30 atms-jenkins
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8090/login
podman exec atms-jenkins sh -c "ls /var/jenkins_home/plugins/*.jpi | wc -l"
```

| 명령 | 정상 |
|---|---|
| `logs` | `Jenkins is fully up and running` |
| `curl` | `200` |
| 플러그인 개수 | `87` |

| 증상 | 대처 |
|---|---|
| `curl` 이 `000` | 아직 기동 중 → 1분 뒤 다시 |
| `Permission denied` (`/var/jenkins_home`) | 9-2 첫 줄(`podman unshare chown`)을 했는지 확인 후 다시. 그래도 나면 `podman unshare ls -ld ~/atms/infra/jenkins/home` 결과 공유 (`1000 1000` 이어야 함) |
| `crun: readlink (빈 경로): No such file or directory ... command that was not found` | `--userns=keep-id` 를 넣은 경우 → 빼고 다시 |
| `OutOfMemoryError` | `jenkins.env` 의 `-Xmx1g` → `-Xmx1500m`, `podman rm -f atms-jenkins` 후 9-2 다시 |

**9-4. 남은 일**
- 빌드 캐시: `offline-seed`(Gradle·npm 캐시)를 넣는다 → 9-5
- 자동기동: linger=yes + `podman-restart.service` enable (9-2). linger 가 없으면 서버 재부팅 후 `podman start atms-jenkins`
- Gitea 연동: `jenkins-bot` 계정·토큰, 저장소 등록 (Gitea 가이드 3-2)
- 화면 빌드·배포 잡 (`nh-atms-frontend`, Jenkins → SSH → wb01): npm 캐시를 `/cache/npm` 으로 마운트, 배포 전용 키·known_hosts 준비 → [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4~5장

**9-5. 빌드 캐시(offline-seed) 넣기** (2026-10-22, npm + Gradle 한 번에)

Jenkins 컨테이너 안 위치: npm → `/cache/npm` (프론트 Jenkinsfile `NPM_CACHE` 와 같음), Gradle → `/cache/gradle` (`GRADLE_USER_HOME`).

① PC: `일괄다운_2\offline-seed\offline-seed` 의 파일 4개를 **다시 압축하지 않고 그대로** FileZilla(바이너리) 로 `/home/atmswas/atms/inbox/` 에 올린다 (2026-10-22 확인: 이미 `.tar.gz` 로 묶여 있음, 2026-09-07 생성)

| 파일 | 크기 |
|---|---|
| `gradle-modules-cache.tar.gz` | 약 131MB |
| `gradle-wrapper-dists.tar.gz` | 약 134MB |
| `npm-cache.tar.gz` | 약 1GB |
| `manifest.csv` | 1KB (목록·체크섬 기록용) |

② 서버 (atmswas): 풀고 구조 확인
```bash
df -h /home/atmswas
cat ~/atms/inbox/manifest.csv
mkdir -p ~/atms/inbox/seed
cd ~/atms/inbox
tar -tzf gradle-modules-cache.tar.gz | head -5
tar -tzf gradle-wrapper-dists.tar.gz | head -5
tar -tzf npm-cache.tar.gz | head -5
tar -xzf gradle-modules-cache.tar.gz -C seed
tar -xzf gradle-wrapper-dists.tar.gz -C seed
tar -xzf npm-cache.tar.gz -C seed
find seed -maxdepth 3 -type d | head -30
```
- `manifest.csv` 에 sha256 이 있으면 `sha256sum *.tar.gz` 결과와 비교
- `tar -tzf ... | head` 는 압축 안의 최상위 경로 확인 (풀기 전에 구조 파악)
- 풀린 크기는 압축의 2~3배일 수 있다 → `df` 여유 확인

③ Jenkins 가 쓰는 구조로 배치 (2026-10-22 확인한 압축 안 구조: `modules-2/`, `dists/gradle-8.14.4-bin/...`, `npm-cache/_cacache/...`)
```bash
mkdir -p ~/atms/infra/jenkins/cache/gradle/caches ~/atms/infra/jenkins/cache/gradle/wrapper
mv ~/atms/inbox/seed/modules-2 ~/atms/infra/jenkins/cache/gradle/caches/
mv ~/atms/inbox/seed/dists ~/atms/infra/jenkins/cache/gradle/wrapper/
mv ~/atms/inbox/seed/npm-cache ~/atms/infra/jenkins/cache/npm
ls ~/atms/infra/jenkins/cache/gradle/caches ~/atms/infra/jenkins/cache/gradle/wrapper/dists ~/atms/infra/jenkins/cache/npm
```
- 같은 디스크 안이라 `mv` 는 바로 끝나고 용량을 두 배로 쓰지 않는다 (원본 `.tar.gz` 는 `inbox` 에 남아 다시 풀 수 있음)
- 정상: `modules-2` / `gradle-8.14.4-bin` / `_cacache  _logs  eresolve-report.txt  node-sass`
- 백엔드 wrapper 는 Gradle **8.14.4** (`gradle-wrapper.properties` 의 버전과 같아야 오프라인 빌드 가능)

④ 컨테이너 jenkins(1000) 소유로 맞추고 환경변수 추가
```bash
podman unshare chown -R 1000:1000 ~/atms/infra/jenkins/cache
echo 'GRADLE_USER_HOME=/cache/gradle' >> ~/atms/infra/jenkins/jenkins.env
echo 'npm_config_cache=/cache/npm' >> ~/atms/infra/jenkins/jenkins.env
cat ~/atms/infra/jenkins/jenkins.env
```

⑤ Jenkins 다시 만들기 (데이터는 `home` 에 남음) — 2026-10-23 배포킷부터는 `~/atms/deploy/jenkins/run-jenkins.sh` 한 줄로 같은 일을 한다
```bash
podman rm -f atms-jenkins
podman run -d --name atms-jenkins --restart=always --network atms-net -p 8090:8080 --env-file ~/atms/infra/jenkins/jenkins.env -v ~/atms/infra/jenkins/home:/var/jenkins_home:Z -v ~/atms/infra/jenkins/cache/npm:/cache/npm:Z -v ~/atms/infra/jenkins/cache/gradle:/cache/gradle:Z --memory=2g localhost/nh-atms-jenkins:bank
```

⑥ 확인
```bash
podman exec atms-jenkins sh -c 'echo $GRADLE_USER_HOME $npm_config_cache; ls /cache/npm; ls /cache/gradle/caches/modules-2 /cache/gradle/wrapper/dists'
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8090/login
du -sh ~/atms/infra/jenkins/cache
rm -rf ~/atms/inbox/seed ~/atms/inbox/*.tar.gz
```
- 마지막 줄은 확인이 끝난 뒤 디스크 정리용
- 실제 빌드 확인: 프론트 잡 `npm ci --offline` 이 `ENOTCACHED` 없이 지나가면 npm 캐시 정상. 백엔드는 잡에서 `./gradlew --offline` 이 `Could not resolve` 없이 지나가면 정상
- 의존성(`package.json`, `build.gradle`)이 바뀌면 인터넷 PC 에서 캐시를 다시 만들어 ①~⑥ 반복

## 10단계. 방화벽 신청·최종 점검

네트워크 방화벽 신청 (개발 PC → 개발서버만. 서버 간 통신은 대상 아님):

| 목적지 | 포트 | 용도 |
|---|---|---|
| nbatmsltwb01 16.88.122.112 | TCP 9000 | WebtoB HTTP 리스너 = 브라우저 화면 접속 (2026-10-15: PC 에서 막힘 확인) |
| nbatmsltws01 16.88.122.113 | TCP 8766 | JEUS atms01 HTTP 리스너 = 직접 접속 (WebtoB 우회 테스트용). 2026-10-15 `domain.xml` 확인 ([JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) 1-4) |
| nbatmsltws01 16.88.122.113 | TCP 3000 | Gitea (형상관리) |
| nbatmsltws01 16.88.122.113 | TCP 8090 | Jenkins (CI/CD) |

- 은행 신청 양식(엑셀): 출발지 표에는 **PC 마다 한 줄**, `IP주소(To)` 는 출발지 IP 범위의 끝값이므로 단일 PC 는 비운다. 목적지는 목적지 표에만 적는다. 단일 포트는 `Port(To)` 를 비우고, 포트마다 한 줄 (`80/443` 처럼 한 칸에 두 개 쓰지 않는다)
- WebtoB ↔ JEUS 연동(JEUS → WebtoB 19000 추정)은 서버 간 통신이며 2026-10-15 에 이미 열려 있음을 확인했다

최종 점검 (서버 안에서):
```bash
podman ps
curl -s http://127.0.0.1:3000/api/healthz
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8090/login
df -h /home/atmswas
```
`atms-gitea`·`atms-jenkins` 가 `Up`, 확인 결과 `pass` / `200` 이면 지원 인프라 셋팅 완료.

방화벽 개방 여부 확인 (Gitea·Jenkins 가 떠 있는 상태에서 해야 의미가 있다):

서버 (atmswas) — 포트가 열려 기다리는지:
```bash
ss -ltn | grep -E ':3000|:8090'
```
PC (PowerShell):
```powershell
Test-NetConnection 16.88.122.113 -Port 3000
Test-NetConnection 16.88.122.113 -Port 8090
```
| 서버 `ss` | PC `TcpTestSucceeded` | 판단 |
|---|---|---|
| 두 줄 나옴 | `True` | 개방됨 → 브라우저에서 `http://16.88.122.113:3000`, `:8090` |
| 두 줄 나옴 | `False` (수십 초 기다린 뒤 실패) | 네트워크 방화벽 미개방 → 위 문구로 신청 |
| 안 나옴 | `False` | 컨테이너가 안 떠 있음 → 8·9단계 확인부터 |

- 서버 firewalld 는 2026-10-15 기준 `not running` → 서버 자체는 막지 않는다. 막는 곳은 PC ↔ 서버 사이 네트워크 방화벽
- PowerShell 을 못 쓰면 명령 프롬프트에서 `curl.exe -m 10 http://16.88.122.113:3000/api/healthz` (응답 JSON 이 오면 개방)

### 10-1. 방화벽 "완료" 후에도 PC → 113:3000·8090 이 실패할 때 (2026-10-21, 112:9000 은 성공)

구간을 하나씩 잘라서 어디서 막히는지 찾는다.

| # | 어디서 | 명령 | 정상 | 실패 시 의미 |
|---|---|---|---|---|
| 1 | 113 (atmswas) | `ss -ltn \| grep -E ":3000\|:8090"` | `*:3000`, `*:8090` | 컨테이너·포트 연결 문제 (8·9단계) |
| 2 | 113 | `curl -s -o /dev/null -w "%{http_code}\n" http://16.88.122.113:3000/` | `200` 또는 `302` | 127.0.0.1 만 되고 서버 IP 로 안 됨 → pasta 바인딩 문제 |
| 3 | 112 (atmswas) | `curl -s -m 5 -o /dev/null -w "%{http_code}\n" http://16.88.122.113:3000/` (8090 도) | `200`/`302` | 서버 자체(호스트 방화벽 규칙 등)에서 막힘 → root/인프라 |
| 4 | PC | `Test-NetConnection 16.88.122.113 -Port 3000` 을 하는 동안 113 에서 `ss -tn \| grep ":3000"` | `10.248.170.171` 에서 온 줄이 보임 | 줄이 없으면 PC 의 요청이 서버에 도착하지 않음 = **네트워크 방화벽** |
| 5 | PC | `Measure-Command { Test-NetConnection 16.88.122.113 -Port 3000 }` | — | 약 20초 후 실패 = 중간에서 버림(방화벽), 즉시 실패 = 서버가 거부 |
| 6 | PC | `Test-NetConnection 16.88.122.113 -Port 22` | — | HIWARE 경유만 허용인지 참고 |
| 7 | PC | `ipconfig` 로 PC IP 가 신청서 출발지(10.248.170.x)와 같은지 | 같음 | 다르면 신청 IP 정정 |

3 이 성공하고 4 에서 줄이 안 보이면 서버는 정상이고 **네트워크 방화벽이 아직 적용되지 않은 것**이다 → 신청 담당에 "출발지 10.248.170.171 → 목적지 16.88.122.113 TCP 3000·8090 차단 확인(정책 적용 IP·포트 재확인)" 요청. 9000(112) 이 되는 것으로 보아 신청서의 113 행만 누락·오기됐을 가능성이 높다.

## 다음 단계

| 작업 | 문서 |
|---|---|
| WAS 담당 회신 (폴더 생성 완료, atms01 앱 6개 등록, 디스크 확인 요청) | [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 7-3 |
| JEUS Jakarta 호환성 확인 → WAR 6개 배포 | [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) |
| Gitea 조직·저장소·토큰, PC 첫 푸시 | [GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) 3·4장 |
