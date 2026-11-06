# root 권한 1일 사용 시 작업 체크리스트

기록일: 2026-10-07 · 개정: 2026-10-12 (작업 계정을 `userATMS` → **`atmswas`** 로 변경, `/home/userATMS` 사용 안 함)

## 0. 전제

- 모든 서버 작업은 **`atmswas`** 계정으로 한다. 평소 root 권한은 없다 (`sudo`/`su` 는 HIWARE 차단, 금지명령 3회 입력 시 세션 차단).
- **절차를 거치면 하루 동안 root 권한을 받을 수 있다.**
- 따라서 root 가 필요한 작업은 **미리 이 목록에 모아 두었다가, 권한을 받은 날 한 번에 처리**한다.
- 일상 운영(배포, 기동/중지, 로그 확인)은 계속 **rootless(atmswas 일반 계정)** 기준으로 한다. 권한이 끝나면 다시 쓸 수 없기 때문.
- 권한 형태와 HIWARE 허용 명령 범위는 신청 시 확인한다. 아래 명령은 root 셸 기준으로 적었다.
- **2026-10-15 실제: root 계정 직접 접속 방식이었고, atmswas 의 `sudo` 는 그날도 금지명령이었다** (`sudo -l` 1회 집계). 다음 root 날에도 `sudo` 를 먼저 치지 말고 root 로 직접 접속한다.

## 1. 신청 전에 준비할 것

- [ ] 아래 작업 목록 중 이번에 할 항목 확정 (신청서 작업 사유에 기재)
- [ ] 방화벽 포트 목록 확정
- [ ] 현재 상태 캡처 (작업 전후 비교용, atmswas 로 미리 확인 가능한 것)
  ```bash
  grep atmswas /etc/subuid /etc/subgid
  loginctl show-user atmswas -p Linger
  df -h /home/atmswas
  systemctl is-active firewalld
  ```

## 2. 작업 목록 (권한 받은 날)

| # | 작업 | 필수 | 왜 필요한가 |
|---|---|---|---|
| 1 | **atmswas** subuid / subgid 등록 | ~~필수~~ **완료** | 2026-10-14 확인: `atmswas:493216:65536` 등록돼 있음 → 이번에는 불필요 |
| 2 | **atmswas** linger 활성화 | ~~필수~~ **완료 (2026-10-15, `Linger=yes`)** | 2026-10-14 확인: `Linger=no`. 2026-10-15 확인: 로그아웃하면 Gitea·Jenkins 컨테이너가 같이 내려간다 (재접속 후 `podman ps` 비어 있음). 로그아웃·재부팅 후에도 Gitea·Jenkins 유지·자동 기동, Gitea 백업 타이머 |
| 3 | 서버 방화벽(firewalld) 포트 개방 | 불필요 | 2026-10-14 확인: firewalld `inactive`, 2026-10-15 root 로 `not running` 재확인. 나중에 켜지면 3000·8090 개방 |
| 4 | 로그 디렉토리 생성·권한 | 선택 | 컨테이너 대안 경로(보류)에서 은행 표준 로그 위치(`/WASlogs`) 사용 시 |
| 5 | 1024 미만 포트 허용 | 선택 | 대안 경로에서 프론트엔드를 80 포트로 서비스하고 싶을 때 |
| 6 | 패키지 설치 (`git` 등) | 선택 | 서버 셸에서 git 이 필요할 때 (`/repo` 로컬 저장소 사용 가능 여부 확인) |
| 7 | `/home/atmswas` 소유·권한 바로잡기 | 불필요 | 2026-10-14 확인: 소유자 atmswas(그룹 atms, 755) → atmswas 가 직접 `chgrp jeus`·`chmod 750` 가능 (`SERVER_DIRECTORY_LAYOUT.md` 3-2). 2026-10-15 확인: `jeusatms` 는 주 그룹이 jeus(2002) → 그룹 추가 불필요 |
| 8 | `/home/atmswas` 디스크 확보 | **처리됨 (2026-10-16, 인프라)** — `vg_app-lv_home_atmswas` 10G 를 `/home/atmswas` 에 마운트. 기존 내용이 가려짐 → 2-11 | 2026-10-14 확인: `/home` 공용 10G(`vg_os-lv_home`), 여유 9.8G. 인프라에 증설·전용 디스크 요청 (`SERVER_DIRECTORY_LAYOUT.md` 3-3) |
| 9 | rootless cgroup `cpu` 위임 | **보류 (2026-10-16 결정)** | 2026-10-15 위임 파일 작성(첫 시도 `[Service]` 누락). CPU 제한 자체를 나중에 다시 정하기로 함 → 2-9 의 "보류 결정" 참고 |

> 삭제된 항목 (2026-10-12): "userATMS 를 jeus 그룹에 추가" — 작업 계정이 atmswas 로 바뀌어 필요 없음. "atmswas 계정·홈 생성" — 2026-10-12 생성 확인. 하위 폴더(`catms*`, `data`, `deploy`, `atms`)는 atmswas 가 직접 만든다 (`SERVER_DIRECTORY_LAYOUT.md` 3-2).
>
> 삭제된 항목 (2026-10-21): "wb01 atmswas linger" — 화면 배포를 wb01 타이머(A안) 대신 Jenkins SSH(B안)로 바꿔 필요 없음 (`FRONTEND_CICD_PLAN.md`).

### 2-1. subuid / subgid 등록 (필수)

```bash
grep atmswas /etc/subuid /etc/subgid           # 이미 있으면 생략
cat /etc/subuid                                # 다른 계정과 범위가 겹치지 않는지 확인
usermod --add-subuids 300000-365535 --add-subgids 300000-365535 atmswas
grep atmswas /etc/subuid /etc/subgid
```
- 범위(300000~)는 예시. `cat /etc/subuid` 에서 이미 쓰는 범위(예: userATMS 의 범위)와 겹치지 않게 정한다.
- 이후 **atmswas 로** 실행: `podman system migrate`

### 2-2. linger 활성화 (필수)

```bash
loginctl enable-linger atmswas
loginctl show-user atmswas -p Linger           # Linger=yes
```

### 2-3. 방화벽 포트 개방 (firewalld 가 active 일 때)

```bash
firewall-cmd --state
firewall-cmd --permanent --add-port=3000/tcp        # Gitea HTTP (Git 푸시·웹, SSH 는 사용 안 함)
firewall-cmd --permanent --add-port=8090/tcp        # Jenkins
firewall-cmd --reload
firewall-cmd --list-ports
```
- 컨테이너 대안 경로(보류)를 쓰게 되면 그때 `8080-8085/tcp`(백엔드), `8088/tcp`(화면)를 추가한다.
- 이것은 **서버 자체 방화벽**이다. 네트워크 구간 방화벽(PC → 서버, 서버 → DB)은 별도 신청 대상.
- 출발지를 제한해야 하면 `--add-rich-rule` 로 대역 지정 (보안 담당 기준 따름).

### 2-4. 로그 디렉토리 (선택, 대안 경로용)

```bash
mkdir -p /WASlogs/atmsContainer
chown atmswas:jeus /WASlogs/atmsContainer
chmod 750 /WASlogs/atmsContainer
```

### 2-5. 1024 미만 포트 허용 (선택)

```bash
echo 'net.ipv4.ip_unprivileged_port_start=80' > /etc/sysctl.d/90-atms-unprivileged-port.conf
sysctl --system | grep unprivileged
```
서버 전체 설정이므로 보안 담당 승인 필요. 불필요하면 8088 등 높은 포트 유지.

### 2-6. 패키지 설치 (선택)

```bash
dnf repolist                 # 사용 가능한 저장소 확인 (/repo 로컬 저장소 여부)
dnf install -y git
```
폐쇄망이라 저장소가 없으면 설치 불가 → 생략.

### 2-7. `/home/atmswas` 소유·권한 (조건부)

```bash
ls -ld /home/atmswas
chown atmswas:jeus /home/atmswas
chmod 750 /home/atmswas
id jeusatms | grep -q "(jeus)" || usermod -aG jeus jeusatms
ls -ld /home/atmswas
```
`jeusatms` 를 그룹에 추가했다면 **JEUS 재기동 후** 적용된다.

### 2-8. `/home/atmswas` 디스크 (조건부, 보통 인프라 작업)

```bash
df -h /home/atmswas
lsblk
```
`/home` 공용(10G)이면 인프라에 `/home/atmswas` 전용 디스크 할당을 요청한다 (예: 사용하지 않게 된 `/home/userATMS` 20G LV 이관). 데이터가 이미 있으면 옮긴 뒤 소유자·SELinux 라벨(`restorecon -R /home/atmswas`)을 확인한다.

### 2-9. rootless cgroup `cpu` 위임 (권장)

> **보류 결정 (2026-10-16)**: 지금은 CPU 제한 없이 운영하고, 나중에 다시 정한다. 컨테이너는 메모리 제한만 둔다 (Gitea 768MB, Jenkins 2GB).
>
> - **무엇을 제한하나**: Gitea·Jenkins 컨테이너가 쓸 수 있는 CPU 상한 (Gitea 0.5코어, Jenkins 1.0코어 계획). 이 위임이 있어야 일반 계정(atmswas)이 `--cpus` 를 쓸 수 있다
> - **왜 필요한가**: 서버가 2코어이고 은행 공식 WAS(JEUS)가 같이 돈다. Jenkins 는 빌드(Gradle·npm) 때 CPU 를 있는 대로 쓴다. 상한을 두면 빌드 중에도 JEUS 몫(0.5~1코어)이 남는다
> - **안 하면**: 평소에는 영향 거의 없음 (Gitea 는 CPU 를 거의 안 씀). Jenkins 빌드 중 몇 분 동안 JEUS 응답이 느려질 수 있음 (화면 지연, API 타임아웃 가능). 서버가 멈추지는 않음. 메모리는 이미 제한돼 있어 메모리 부족으로 JEUS 가 죽는 위험은 막혀 있음
> - **그동안 줄이는 방법**: 빌드는 사람이 적은 시간에, Jenkins executor 1개 유지 (이미 1개)
> - **다시 할 때**: atmswas 로 `podman info --format '{{.Host.CgroupControllers}}'` 에 `cpu` 가 있는지 확인 → 없으면 root 날에 아래 명령 → `GITEA_CPUS=0.5 ~/atms/deploy/gitea/install-gitea.sh 16.88.122.113`, Jenkins 는 `--cpus=1.0` 넣어 재생성

RHEL 9 기본값은 일반 계정에 `memory`·`pids` 만 위임한다. 아래는 Red Hat 이 rootless Podman 자원 제한용으로 안내하는 방법이며 **서버의 모든 사용자 세션에 적용**된다 (보안 담당 확인 권장).

root 셸:
```bash
cat /sys/fs/cgroup/user.slice/user-50219.slice/user@50219.service/cgroup.controllers
mkdir -p /etc/systemd/system/user@.service.d
echo '[Service]' > /etc/systemd/system/user@.service.d/delegate.conf
echo 'Delegate=memory pids cpu cpuset io' >> /etc/systemd/system/user@.service.d/delegate.conf
systemctl daemon-reload
```
(참고) sudo 가 허용되는 환경이라면 `sudo echo ... > 파일` 은 리다이렉션이 일반 권한으로 처리돼 실패하므로 `tee` 사용 — **이 서버에서는 sudo 금지 (2026-10-15)**:
```bash
sudo mkdir -p /etc/systemd/system/user@.service.d
echo '[Service]' | sudo tee /etc/systemd/system/user@.service.d/delegate.conf
echo 'Delegate=memory pids cpu cpuset io' | sudo tee -a /etc/systemd/system/user@.service.d/delegate.conf
cat /etc/systemd/system/user@.service.d/delegate.conf
sudo systemctl daemon-reload
```
- 적용: `systemctl restart user@50219.service`. **linger 를 켠 뒤에는 로그아웃해도 user@ 서비스가 계속 떠 있어서 재로그인만으로는 적용되지 않는다.** 재시작하면 atmswas 의 실행 중 컨테이너(Gitea·Jenkins)가 멈추므로 아래 "이후" 단계로 다시 띄운다. HIWARE 접속 셸은 별도 세션이라 끊기지 않는다
- 확인 (atmswas): `podman info --format '{{.Host.CgroupControllers}}'` 에 `cpu` 포함
- 이후 (atmswas): `~/atms/deploy/gitea/install-gitea.sh 16.88.122.113` 재실행 → linger 가 켜져 있으면 Quadlet(자동기동)으로, `--cpus=0.5` 적용. Jenkins 는 `podman rm -f atms-jenkins` 후 `--cpus=1.0`·`--restart=always` 로 다시 실행 (`DEV_SERVER_SETUP.md` 9-2)

### 2-10. 권한 받은 날 기록 (2026-10-15)

- **권한 형태: root 계정 직접 접속** (HIWARE 에서 root 로 로그인). atmswas 의 `sudo` 는 허용되지 않았다 — `sudo -l` 입력 시 `Command "[sudo -l]" is not allowed ... Prohibited input count : 1`. **root 날에도 atmswas 셸에서 `sudo` 를 치지 않는다**
- 처리: linger (2-2) → `Linger=yes` 확인 / cgroup 위임 (2-9) / `jeusatms` 는 주 그룹이 jeus(2002) 라 할 일 없음 / firewalld `not running` 이라 2-3 은 해당 없음 (`FirewallD is not running`)
- 주의: 2-9 첫 줄에서 작은따옴표 대신 백틱(`` ` ``)이 들어가 `>` 대기 상태 → Ctrl+C 로 `[Service]` 줄이 빠졌다. `cat` 결과가 **두 줄**(`[Service]`, `Delegate=...`)인지 꼭 확인한다. 한 줄이면 위임이 적용되지 않는다
- 이후: atmswas 로 cgroup `cpu` 확인 → Gitea 재설치(Quadlet) → 백업 타이머 → Jenkins 재생성 (`DEV_SERVER_SETUP.md` 8·9단계)

### 2-11. 가려진 기존 `/home/atmswas` 내용을 새 디스크로 복사 (2026-10-19, 인프라 요청)

2026-10-16 10:52 에 만들어진 새 LV(마운트 시각은 그 이후 ~ 10-17 02:30 이전 추정)(`vg_app-lv_home_atmswas`, 10G)가 마운트되면서, 그 전에 `/home`(`vg_os-lv_home`) 의 `atmswas` 폴더에 있던 내용(약 1.8G 추정)이 보이지 않게 됐다. 마운트를 풀지 않고 **bind 마운트로 아래 내용을 보고 복사**한다. atmswas 의 실행 중 컨테이너는 없으므로 서비스 영향 없음.

```bash
mkdir /mnt/home_lower
mount --bind /home /mnt/home_lower
ls -la /mnt/home_lower/atmswas
du -sh /mnt/home_lower/atmswas
cp -a /mnt/home_lower/atmswas/. /home/atmswas/
ls -la /home/atmswas /home/atmswas/atms
umount /mnt/home_lower
rmdir /mnt/home_lower
```
- `--bind` 는 하위 마운트를 포함하지 않으므로 `/mnt/home_lower/atmswas` 에 **가려진 원래 내용**이 보인다
- `cp -a` 는 소유자(일부 파일은 Podman subuid 494215 소유)·권한·SELinux 라벨을 그대로 유지한다. 새 디스크의 `.config`·`.local` 과 합쳐진다
- 원본(`/home` 쪽)은 atmswas 확인 후에 지운다: 다시 bind 마운트 → `rm -rf /mnt/home_lower/atmswas/*  /mnt/home_lower/atmswas/.[!.]*` → umount (공용 `/home` 1.8G 회수)
- 확인 (atmswas): `ls ~/atms`, `podman images` (gitea·nh-atms-jenkins 2개), `podman ps -a`
- (선택, CPU 제한을 다시 할 때) 같은 날 `delegate.conf` 에 `[Service]` 줄을 넣는다 (2-9)

### 2-12. 두 번째 root 작업일 후보 (2026-10-23 정리, ws01 + wb01)

원칙: 우리 계정(atmswas)·우리 앱에 필요한 것만 바꾸고, WAS·WebtoB 담당 영역(JEUS 설정, `http.m`, 재기동)은 **읽기만** 한다. root 셸에서도 `sudo`·`su` 는 치지 않는다 (HIWARE 금지명령 가능성).

**ws01 (nbatmsltws01)**

| # | 작업 | 구분 | 명령 |
|---|---|---|---|
| W1 | 앱 로그 폴더 `/WASlogs/jeusATMSlogs/nbatmsltws01/app` 확인·생성 (10-19 `/WASlogs` 비워짐, `jeus-common.yml` `log-dir`) | 필수(없으면) | 아래 W1 |
| W2 | JEUS 가 `catms*` 를 읽을 수 있는지 경로 권한 확인 | 확인 | `namei -l /home/atmswas/catms/WEB-INF` |
| W3 | 가려진 옛 `/home/atmswas` 내용(약 1.8G, 공용 `/home`) 삭제 — 10-21 새로 구성해서 불필요 | 선택 | 2-11 의 bind 마운트 후 확인·삭제 |
| W4 | `delegate.conf` 2줄로 바로잡기 (CPU 제한은 보류 유지, 재시작 안 함) | 선택 | 2-9 의 `echo` 2줄 + `cat` |
| W5 | 디스크·마운트 기록 | 확인 | `df -h /home /home/atmswas /JEUS /WASlogs` |

W1:
```bash
ls -ld /WASlogs /WASlogs/jeusATMSlogs /WASlogs/jeusATMSlogs/nbatmsltws01 /WASlogs/jeusATMSlogs/nbatmsltws01/atms01 2>&1
mkdir -p /WASlogs/jeusATMSlogs/nbatmsltws01/app
chown jeusatms:jeus /WASlogs/jeusATMSlogs/nbatmsltws01/app
chmod 2750 /WASlogs/jeusATMSlogs/nbatmsltws01/app
ls -ld /WASlogs/jeusATMSlogs/nbatmsltws01/app
```
- 이미 있고 `jeusatms` 가 쓸 수 있으면 첫 줄만 기록하고 넘어간다. 상위 폴더 소유는 바꾸지 않는다 (WAS 담당 영역)
- 2750: jeusatms 가 쓰고, atmswas 는 jeus 그룹으로 읽는다

**wb01 (nbatmsltwb01)**

| # | 작업 | 구분 | 명령 |
|---|---|---|---|
| B1 | `http.m` 읽기 → WebtoB 연동 요청문을 정확히 (JSV 서버명·URI·DOCROOT·오류 페이지) | 확인(필수) | `cat /WEBTOB/webtobATMS/config/http.m` 사진, **수정하지 않음** |
| B2 | 19000 연결·포트 | 확인 | `ss -ltnp \| grep -E ":9000\|:19000"`, `ss -tnp \| grep 19000` |
| B3 | webatms 가 DocBase 를 읽는 경로 권한 | 확인 | `namei -l /home/atmswas/docs/index.html` |
| B4 | `/error/error.html` 실제 위치 | 확인 | `grep -n -i error /WEBTOB/webtobATMS/config/http.m` |

하지 않는 것: `webtobRstart`, jeusRstart 메뉴 3·7·8, JEUS 설정 수정 (모두 담당자 업무). `http.m` 수정·`wscfl`·`wsdown/wsboot` 는 2026-10-23 결정으로 2-13 절차에 한해 우리가 한다

### 2-13. wb01 WebtoB ↔ JEUS 연동을 root 작업일에 직접 (2026-10-23 사용자 결정: 재기동까지 우리)

> 2-12 의 "하지 않는 것" 중 `http.m` 수정·`wscfl`·`wsdown/wsboot` 를 이 절차에 한해 우리가 한다. 은행 WebtoB 를 멈추는 작업이므로 **WebtoB 담당(또는 현업)에 작업 시간을 알리고 승인 기록**을 남긴다.
> 지키는 것: `sudo`·`su`·`webtobRstart`·jeusRstart 메뉴 7·8 은 쓰지 않는다. WebtoB 명령은 `runuser -l webatms -c '…'` 로 **webatms 로 실행**한다 (root 로 기동하면 프로세스·생성 파일이 root 소유가 되어 이후 webatms 가 못 다룸). `runuser` 가 HIWARE 금지명령인지는 미확인 → R0 에서 해 없는 명령으로 먼저 시험

**R0. 사전 (ws01 atmswas, 전날 가능)** — JEUS 쪽 커넥터 값
```bash
grep -n -i -B2 -A12 "nbatmsltwb01_webatms" /JEUS/jeusATMS/domains/*/config/domain.xml | head -40
```
→ 커넥터의 `registration-id`(또는 서버 이름)·포트 19000·대상 16.88.122.112 를 기록. WebtoB `*SERVER` 이름이 이 값과 같아야 연결된다

**R1. 백업·현황 (wb01 root)**
```bash
runuser -l webatms -c 'id; echo $WEBTOBDIR'
cd /WEBTOB/webtobATMS/config
ls -la
cp -p http.m http.m.bak-$(date +%Y%m%d%H%M%S)
tar -C /WEBTOB/webtobATMS -czf /root/webtob-config-$(date +%Y%m%d%H%M%S).tar.gz config
grep -n -E "^\*|JSVPORT|DOCROOT|SVRTYPE|SvrType|SVGNAME|SvgName|URI|Uri|ErrorDocument" http.m
ss -tnp | grep 19000
```
- 첫 줄이 `uid=…(webatms)` 와 `/WEBTOB/webtobATMS` 를 출력해야 이후 `runuser` 사용 가능. 막히면(금지명령·오류) **R3 이후는 중단**하고 수정본만 준비해 WebtoB 담당에 적용 요청
- `grep` 결과를 사진으로 남긴다 (JSV 서버그룹·서버가 이미 있는지 판단)

**R2. `http.m` 수정 (root, vi)** — 기존 항목 이름·형식을 그대로 따른다
> 2026-10-27: 매니지드 서버 6개 구성용 블록은 배포킷 `deploy/webtob/http.m.atms-jsv.txt` (서버 6개·경로별 URI). 아래 4줄 예시는 atms01 하나일 때
- `*NODE` 에 `JSVPORT = 19000` 이 있는지 (10-15 `:19000` LISTEN 확인 → 있을 가능성 높음)
- `*SVRGROUP` 에 `SvrType = JSV` 그룹이 없으면 추가: `jsvg  NodeName = "<*NODE 이름>", SvrType = JSV`
- `*SERVER` 에 R0 의 registration-id 와 같은 이름이 없으면 추가: `<registration-id>  SvgName = jsvg, MinProc = 2, MaxProc = 10`
- `*URI` (없으면 맨 끝에 새로 만듦)에 추가:
```
uri_api       Uri = "/api/",        Svrtype = JSV, SvrName = <registration-id>
uri_swagger   Uri = "/swagger-ui/", Svrtype = JSV, SvrName = <registration-id>
uri_apidocs   Uri = "/v3/api-docs", Svrtype = JSV, SvrName = <registration-id>
uri_actuator  Uri = "/actuator/",   Svrtype = JSV, SvrName = <registration-id>
```
- DOCROOT·기존 URI·오류 페이지는 건드리지 않는다. 저장 후 `diff http.m.bak-* http.m` 로 바뀐 줄만 확인

**R3. 컴파일 (webatms 로)** — 실행 중인 WebtoB 에는 아직 영향 없음
```bash
runuser -l webatms -c 'cd /WEBTOB/webtobATMS/config && wscfl -i http.m'
ls -la /WEBTOB/webtobATMS/config
```
- 오류가 나면 메시지의 줄 번호를 고치고 다시. 생성 파일 소유자가 webatms 인지 확인

**R4. 재기동 (webatms 로, 약속한 시간에)**
```bash
runuser -l webatms -c 'wsdown -i'
runuser -l webatms -c 'wsboot'
ps -u webatms -o pid,lstart,args | grep -E "wsm|htl|hth" | grep -v grep
ss -ltn | grep -E ":9000|:19000"
```
- `wsboot` 출력이 정상 기동 메시지로 끝나고, 9000·19000 LISTEN

**R5. 확인 (wb01)**
```bash
ss -tn | grep 19000
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9000/
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9000/v3/api-docs
curl -s -o /dev/null -w "%{http_code} %{redirect_url}\n" http://127.0.0.1:9000/api/
```
- 19000 에 `16.88.122.113` ESTAB (JEUS 가 접속해 오는 데 몇 초~1분 걸릴 수 있음), `/` 200, `/v3/api-docs` 200 (atms01 에 atms-web 이 떠 있을 때), `/api/` 가 302 `/error/error.html` 이 아니면 연동 완료

**R6. 되돌리기 (R4·R5 가 실패하면 바로)**
```bash
cd /WEBTOB/webtobATMS/config
cp -p http.m.bak-<시각> http.m
runuser -l webatms -c 'cd /WEBTOB/webtobATMS/config && wscfl -i http.m'
runuser -l webatms -c 'wsdown -i'
runuser -l webatms -c 'wsboot'
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9000/
```
- 그래도 안 뜨면 `/root/webtob-config-*.tar.gz` 로 config 폴더 전체 복원 후 같은 순서

남은 일: 2호기(wb02) 도 같은 구성이 필요한지 담당 확인, SPA fallback(새로고침 404)은 동작 확인 후 별도 판단

### 2-14. 오늘(2026-10-26) root 작업 순서 — ws01·wb01 전체, JEUS 설정은 읽기만

배경 (10-26 확인): WAR 는 `catms*` 에 배포됐지만 `8766` 의 모든 경로가 JEUS 404 → atms 앱(Spring)이 기동하지 않음. JEUS 에는 앱 `atms`(`/home/atmswas/catms`, context `/`) 1개만 등록, 나머지 5개 미등록. `stderr.log` 는 `[WebT]:webtCPInfo is null`+NPE 가 매초 반복 (은행 JEUS 설정, 우리 앱과 무관). atmswas 는 `/JEUS/jeusATMS/lib/system` 읽기 불가 → Jakarta 판정을 root 로. ws01→DB `16.88.250.91:1531` TCP open (atmswas).

| 순서 | 서버 | 항목 | 성격 |
|---|---|---|---|
| 0 | ws01 atmswas | DB TCP 재확인 (root 불필요) | 확인 |
| 1 | ws01 root | **J1 Jakarta 판정** | 읽기 |
| 2 | ws01 root | **J2 atms 앱 기동 기록 찾기** (WebT 줄 제외) | 읽기 |
| 3 | ws01 root | W1 앱 로그 폴더 생성 | 변경 |
| 4 | ws01 root | W2 경로 권한, W5 디스크 기록 | 읽기 |
| 5 | ws01 root | (선택) W3 가려진 옛 홈 정리, W4 delegate.conf | 변경 |
| 6 | wb01 root | B2·B3·B4 확인 → 2-13 R0~R6 WebtoB 연동 | 변경 (재기동) |
| 7 | atmswas | 권한 종료 후 확인 (Jenkins 재배포·화면) | 확인 |

J1 (ws01 root):
```bash
ls /JEUS/jeusATMS/lib/system | grep -i -E "servlet|jakarta" | head
for j in /JEUS/jeusATMS/lib/system/*.jar; do python3 -I -m zipfile -l "$j" 2>/dev/null | grep -q "jakarta/servlet/Servlet.class" && echo "JAKARTA $j"; done
for j in /JEUS/jeusATMS/lib/system/*.jar; do python3 -I -m zipfile -l "$j" 2>/dev/null | grep -q "javax/servlet/Servlet.class" && echo "JAVAX $j"; done
```
- `JAKARTA …` → 지원. `JAVAX …` 만 → 미지원 (JEUS_WEBTOB_DEPLOY_GUIDE 8장)

J2 (ws01 root):
```bash
find /JEUS/jeusATMS -name "*.log" -path "*atms01*" -mtime -4 | head -20
grep -v -E "WebT|NullPointerException" /JEUS/jeusATMS/logs/JeusSystem/nbatmsltws01_atms01/stderr.log | grep -i -E "atms|spring|deploy|catms|started|failed|jakarta|servlet|ORA-|Hikari|APPLICATION" | tail -40
```
- `find` 로 나온 서버 로그(JeusServer 등)도 같은 `grep -v … | grep -i …` 로 확인. Spring 배너·`Started Atms…`·`APPLICATION FAILED`·`Caused by` 줄을 사진으로

이후 W1·W2·W5 (2-12), wb01 은 2-12 B2~B4 → 2-13. 하지 않는 것: `domain.xml` 수정(앱 5개 등록·WebT 는 WAS 담당 요청), jeusRstart 메뉴 3·7·8, `webtobRstart`, `sudo`·`su`.

권한 종료 후 (atmswas): 앱 기동 원인 해결 → 내부 Gitea `jeus-common.yml` 에 DB(`jdbc:oracle:thin:@//16.88.250.91:1531/NBATMSTP`)·`jwt.secret`·`log-dir` → push → Jenkins `redeploy`+`RESTART` → `8766/actuator/health` `UP` → wb01 `9000/v3/api-docs` 200 → 화면 로그인·조회.

## 3. 권한 종료 전 확인

- [ ] atmswas 로 재접속해 `~/atms/bin/precheck.sh 8090` 실행 → subuid / Linger 항목 `[OK]`
- [ ] `podman info` 정상, `podman run --rm --entrypoint gitea docker.io/gitea/gitea:latest-rootless --version` 성공 (이미지 적재 후)
- [ ] PC 에서 `Test-NetConnection <서버IP> -Port 3000` 성공 (Gitea 기동·네트워크 방화벽 개방 상태에서)
- [ ] 작업 내역(명령·결과) 기록 → 신청 절차의 완료 보고에 첨부

## 4. root 권한이 있어도 하지 않는 것

| 작업 | 이유 |
|---|---|
| Docker Engine 설치 (`docker-29.8.0` static) | 데몬이 상시 root 로 동작 → 권한 종료 후 관리 불가, 보안 심사 부담. Podman rootless 유지 |
| 컨테이너를 root(rootful) 로 실행 | 권한 종료 후 재기동·관리 불가 |
| `atmswas` 에 영구 sudo 부여 | 신청 절차 범위 밖 |
