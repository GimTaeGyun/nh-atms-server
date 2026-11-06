# 개발서버 폴더 구조 & WAR 반입·배포 위치 (v4)

작성일: 2026-10-08 · 서버 `nbatmsltws01`
개정 v3 (2026-10-12): 백엔드 6개 모듈을 JEUS 1개 서버에 **WAR 6개(컨텍스트 경로 분리)** 로 배포 → 배포 폴더 6개, 쓰기용 `data` 폴더 추가. 소스 변경은 [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md)
개정 v4 (2026-10-12): **`/home/userATMS` 는 더 이상 쓰지 않는다.** 모든 작업(업로드, 배포킷, Gitea·Jenkins 컨테이너, WAR 배포)을 **`atmswas` 계정으로 로그인해 `/home/atmswas` 아래에서** 한다.

> **일부 현재 구조 아님 (2026-11-03 표시)** — 계정·권한·`data`·`deploy` 구조는 그대로 유효하지만, **배포 폴더 이름(`catms-journal` 등)과 "JEUS 서버 1개" 서술은 옛 구조**다. 지금 배포 폴더는 `catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex` (매니지드 서버 6개, [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6-10). 현재 작업은 [INFRA_MANUAL.md](INFRA_MANUAL.md) 를 따른다.

## 0. 결정 사항과 현재 상태

| 항목 | 내용 |
|---|---|
| 작업 계정 | **`atmswas`** (HIWARE 로 로그인 가능, 2026-10-12 사용자 확인). 파일 소유·rootless Podman·WAR 배포 모두 이 계정 |
| 기준 경로 | **`/home/atmswas`** (생성됨, 2026-10-12 사용자 확인) |
| 배포킷 위치 (`BASE_DIR`) | **`/home/atmswas/atms`** — 예전 `/home/userATMS/atms` 구조를 그대로 옮김 |
| `/home/userATMS` | 사용 안 함 (올린 파일 없음). 계정은 개인 접속용으로만 남음 |
| JEUS 재기동 | 그대로 `jeusatms` (`/JEUS/jeusATMS/jeusMENU/jeusRstart`) |

### 0-1. 서버 확인 결과 (2026-10-14, atmswas 로 실행)

| 확인 | 결과 | 판단 |
|---|---|---|
| `id` | `uid=50219(atmswas) gid=50082(atms) groups=50082(atms),2002(jeus)` | **주 그룹은 `atms`**, `jeus` 는 보조 그룹 → `chgrp jeus` 가능. 새 파일은 기본 `atms` 그룹이므로 JEUS 가 읽을 폴더(`catms*`, `data`)는 setgid 로 `jeus` 를 물려받게 한다 |
| `$HOME` | `/home/atmswas` | 정상 |
| `ls -ld /home/atmswas` | `drwxr-xr-x atmswas atms` (755) | **바꿔야 함** → `chgrp jeus` + `chmod 750` (3-2, atmswas 가 직접 가능) |
| `df -h /home/atmswas` | `/dev/mapper/vg_os-lv_home` **10G**, 사용 217M, 여유 9.8G, `/home` 에 마운트 | **`/home` 공용 10G 확정**. 지금은 진행 가능하지만 여유가 빠듯 → 디스크 할당 요청 (3-3) |
| `XDG_RUNTIME_DIR` | `/run/user/50219` | 정상 (rootless Podman·`systemctl --user` 사용 가능) |
| subuid / subgid | `atmswas:493216:65536` (둘 다) | **등록돼 있음** → 1일 root 불필요 |
| Linger | `no` | 재부팅 자동기동·로그아웃 후 유지가 안 될 수 있음 → 1일 root 때 `enable-linger` (권장) |
| Podman rootless | `true` | 정상 |
| firewalld | `inactive` | 서버 자체 방화벽 작업 불필요 (네트워크 구간 방화벽은 별도 신청) |
| 3000·8090 LISTEN | 없음 | 포트 사용 가능 |
| cgroup 위임 (Gitea 기동 시 확인) | `crun: controller 'cpu' is not available` | rootless 에 **cpu 미위임** (RHEL 9 기본) → 스크립트가 `--cpus` 를 빼고 기동. CPU 제한은 1일 root 2-9 후 |
| 서버 IP (`hostname -i`) | IPv4 `16.89.122.86`, `16.88.122.113`, `16.88.250.88`, `16.89.250.68` + IPv6 `fe80::…` | 서비스 주소는 **16.88.122.113** 로 판단 (HIWARE 접속 주소). `fe80::` 는 내부 주소라 사용 금지 |

| 남은 확인 | 이유 |
|---|---|
| `id jeusatms` 에 `jeus` 포함 여부 | JEUS 가 `catms*` 를 그룹으로 읽는 전제 |
| `/home/atmswas` 디스크 증설 | Podman 이미지·Jenkins 데이터·WAR 백업이 모두 공용 10G 로 온다 (3-3) |

## 1. `atmswas` 는 무엇인가

### 1-1. 근거 — 환경구성표 (`참고1_[개발_환경구성표] 프로젝트NEO 선도개발 자동화기기_WEBWAS 엔진 환경구성표`)

| 구성표 항목 | 적힌 내용 |
|---|---|
| 업무팀계정 | `atmswas`(ATMS 메타코드 4자리), `attswas`(ATTS), **그룹명: jeus (GID 2002)** |
| JEUS 계정 | `jeusatms` (`/JEUS/jeusATMS`) |
| JEUS Deploy path (=application path) | nbatmsltws01 (atms01) `<Context docBase="/home/atmswas/catms"/>` ← **"디렉토리 생성 및 그룹권한(JEUS) 부여 완료 후 회신 요망"** |
| WebtoB Doc Base | nbatmsltwb01: `/home/atmswas/docs` (9000) ← **같은 문구** |
| 에러처리 페이지 | WebtoB: DocBase 하위 `/error/error.html`, JEUS: Deploy path 하위 `/error.jsp` |
| 로그 위치 | `/WASlogs/jeusATMSlogs/nbatmsltws01/<컨테이너>/JeusServer.log` 등 |

### 1-2. 해석

| 구분 | 내용 |
|---|---|
| **계정 분리 구조** | 은행 WAS 표준: **엔진 계정**(`jeusatms`, `webatms`)은 JEUS·WebtoB 프로그램을 실행하고, **업무팀 계정**(`atmswas`)은 앱 파일을 소유한다. 둘은 `jeus` 그룹으로 묶여, 엔진이 그룹 권한으로 앱 파일을 읽는다 |
| `/home/atmswas` | 업무팀 계정의 홈. 앱 폴더(`catms*`), 앱 저장 파일(`data`), 배포 이력(`deploy`)과 함께 **배포킷·지원 인프라(`atms/`)** 도 여기에 둔다 (v4) |
| "디렉토리 생성 및 그룹권한 부여 완료 후 회신 요망" | WAS 담당이 업무팀에 "경로를 만들고 jeus 그룹 권한을 준 뒤 알려 달라"고 요청한 것으로 본다. 회신을 받으면 담당이 JEUS(atms01)·WebtoB 에 경로를 등록(또는 확인)한다 |
| JEUS 에 docBase 가 이미 잡혀 있는가 | **아직 모름.** `domain.xml` 확인 필요 (2장) |

### 1-3. 은행 표준을 따르는 근거 (요약)

1. **환경구성표에 경로가 명시**돼 있고, JEUS·WebtoB 설정과 에러페이지 위치(`catms/error.jsp`, `docs/error/error.html`)가 이 경로를 기준으로 정해져 있다.
2. **권한 분리**: 앱 파일은 업무팀 계정 소유 + `jeus` 그룹 읽기. 엔진 계정은 앱 파일을 바꾸지 않는다.
3. 이후 **검증·운영 서버도 같은 구조**(ATTS 는 `attswas` 등)로 가므로 개발서버부터 맞춰야 이관이 쉽다.

## 2. 확인 명령 (atmswas 로 로그인, 읽기 전용, 한 줄씩)

```bash
id
echo $HOME
ls -ld /home/atmswas
df -h /home/atmswas
getent group jeus
id jeusatms
echo $XDG_RUNTIME_DIR
grep -rn -i "catms\|atmswas" /JEUS/jeusATMS/domains/*/config/domain.xml 2>/dev/null | head
```

| 결과 | 정상 | 아니면 |
|---|---|---|
| `id` | groups 에 `2002(jeus)` (2026-10-14: 주 그룹 `atms`, 보조 그룹 `jeus` 확인) | jeus 그룹이 아니면 `chgrp jeus` 가 안 됨 → 인프라 요청 |
| `echo $HOME` | `/home/atmswas` | 다르면 결과를 공유 (문서 경로를 맞춰야 함) |
| `ls -ld /home/atmswas` | `drwxr-x--- atmswas jeus` | 소유자가 root 등이면 인프라 요청. 권한만 다르면 `chmod 750 /home/atmswas`, 그룹만 다르면 `chgrp jeus /home/atmswas` (atmswas 가 jeus 그룹일 때) |
| `df -h` | 여유 **5GB 이상** (2026-10-14: 공용 10G 중 9.8G 여유) | 부족하면 3-3 |
| `id jeusatms` | `2002(jeus)` 포함 | 없으면 인프라에 그룹 추가 요청 (JEUS 재기동 후 적용). **2026-10-15 확인: `uid=40119(jeusatms) gid=2002(jeus) groups=2002(jeus),2001(comp)` → 충족** |
| `echo $XDG_RUNTIME_DIR` | `/run/user/<숫자>` | 비어 있으면 rootless Podman·`systemctl --user` 가 안 될 수 있음 → 접속 방식(HIWARE 가 su 전환인지) 확인 |
| `domain.xml` | `catms` 가 보이면 이미 등록됨 | 없음/권한 없음 → 폴더 생성 후 WAS 담당에 회신·등록 요청 |

## 3. 목표 구조

### 3-1. 전체 (`/home/atmswas`, nbatmsltws01)

```
/home/atmswas/                         drwxr-x---  atmswas:jeus  (750)  ← jeus 그룹이 catms* 로 들어갈 수 있게
├─ catms/                              drwxr-s---  atmswas:jeus  (2750) ★ atms-web         → /   (환경구성표 docBase)
│   ├─ WEB-INF/  META-INF/  org/
│   └─ error.jsp                       은행 에러페이지
├─ cajou/                      (2750)  ★ atms-journal     → /journal
├─ cabat/                        (2750)  ★ atms-batch       → /batch
├─ cfile/                         (2750)  ★ atms-file        → /file
├─ cifin/                         (2750)  ★ atms-if-internal → /ifin
├─ cifex/                         (2750)  ★ atms-if-external → /ifex
├─ data/                               drwxrws---  atmswas:jeus  (2770) 앱이 쓰는 파일 (업로드·이미지·다운로드, jeus.data-dir)
├─ deploy/                             drwx------  (700)  swap-deploy.sh 의 백업·이력 (DEPLOY_HOME)
│   ├─ backup/<폴더명>-<시각>/          교체 전 버전 (모듈별 최근 5개)
│   └─ deploy-history.log
├─ atms/                               drwx------  (700)  ★ 배포킷 BASE_DIR (JEUS 는 볼 필요 없음)
│   ├─ bin/ conf/ deploy/ docs/ README.md   배포킷 (ATMS-Server.zip 내용)
│   ├─ inbox/                          업로드 직후 (SHA256 확인 전)
│   ├─ release/<모듈>/                  확인된 WAR·dist 보관 (.war + .sha256)
│   ├─ images/                         반입 이미지 tar + SHA256SUMS.txt (적재 후 지워도 됨)
│   ├─ infra/                          (700)
│   │   ├─ gitea/   data/ config/ gitea.env(600)
│   │   └─ jenkins/ home/ jenkins.env(600)
│   ├─ tools/  jdk/                    (보류) 대안 경로용
│   └─ config/ logs/ build/ run/       (보류) 컨테이너·직접 실행 대안 경로용
├─ .config/containers/systemd/         Quadlet (atms-gitea.container 등, linger 필요)
├─ .config/systemd/user/               gitea-backup.service/.timer
└─ .local/share/containers/            rootless Podman 이미지·컨테이너 저장소

[nbatmsltwb01]   (2026-10-15 확인: atmswas 로 접속 가능, 같은 uid·그룹. 2026-10-21: docs 생성·화면 배포됨)
/home/atmswas/docs/                    atmswas:jeus (2750)  ★ WebtoB DocBase (dist 를 푼 상태, 9000 DOCROOT 확인)
    └─ error/error.html                은행 에러페이지 (2026-10-21 현재 docs 에는 없음 — WebtoB 쪽 위치 확인 중)
/home/atmswas/deploy/                  700   화면 배포 (FRONTEND_CICD_PLAN.md, Jenkins 가 SSH 로 호출)
    ├─ bin/deploy-front.sh, swap-deploy.sh
    ├─ inbox/  (받은 tar.gz 5개)   backup/  (docs 백업 5개)
    └─ front-deploy.log, front-current.txt, deploy-history.log
/home/atmswas/.ssh/authorized_keys     600   Jenkins 배포 전용 키 1줄 (command=·from= 고정)
```

- `catms*` 에 setgid(`s`)를 주면 안에 새로 생기는 파일도 `jeus` 그룹이 된다 → JEUS 가 항상 읽을 수 있음
- 모듈 폴더를 `catms` **안에** 두지 않고 옆(형제)에 둔다: `catms` 는 `/` 앱의 문서 루트라, 안에 둔 폴더는 정적 경로로 노출될 수 있다 (`WEB-INF/lib` jar 다운로드 위험)
- `data` 만 그룹 쓰기(2770): JEUS(`jeusatms`)가 업로드 파일을 저장한다. 앱 폴더(`catms*`)에는 쓰기 권한을 주지 않는다
- `atms/` 는 700: 배포킷·DB 비밀번호가 든 WAR·Gitea/Jenkins 데이터는 atmswas 만 본다. JEUS 는 `catms*`·`data` 만 읽으면 된다
- 앱 로그는 `/WASlogs/jeusATMSlogs/nbatmsltws01/app/<모듈>.log` (`jeus.log-dir`, jeusatms 가 생성)
- `swap-deploy.sh` 는 배포킷 안의 것을 그대로 실행한다 (`~/atms/deploy/jeus/swap-deploy.sh`). 따로 복사하지 않는다

### 3-2. 생성 명령 (atmswas, 한 줄씩 — root 불필요)

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
(WebtoB 서버의 `/home/atmswas/docs` 도 같은 방식. 정확한 권한값은 WAS 담당 기준이 있으면 그것을 따름)

### 3-3. 디스크 (중요)

| 데이터 | 위치 | 예상 크기 |
|---|---|---|
| Podman 이미지 저장소 (Gitea·Jenkins) | `~/.local/share/containers` | 약 1.5GB |
| 반입 이미지 tar | `~/atms/images` | 약 0.5GB (적재 후 삭제 가능) |
| Jenkins 홈 + 빌드 캐시(offline-seed) | `~/atms/infra/jenkins` | 2~5GB |
| Gitea 저장소 + 백업 | `~/atms/infra/gitea` | 수백 MB ~ |
| WAR 보관 | `~/atms/release` | 배포 1회 약 450MB (WAR 6개) |
| 배포된 앱 + 백업 | `catms*` + `deploy/backup` | 앱 약 450MB + 모듈별 백업 5개 (최대 약 2.3GB) |
| 앱 저장 파일 | `data` | 업무량에 따라 증가 |

합계가 **5~10GB** 에 이른다. **2026-10-14 확인: `/home` 은 공용 10G(`vg_os-lv_home`), 여유 9.8G** → 아래 표의 둘째 줄에 해당한다 (지원 인프라 셋팅은 지금 진행 가능, 증설은 요청).

| 결과 | 조치 |
|---|---|
| 전용 디스크이고 여유 10GB 이상 | 그대로 진행 |
| `/home` 공용 10G 그대로 | 인프라에 **`/home/atmswas` 용 디스크 할당 요청** (예전 `/home/userATMS` 전용 20G LV 를 옮기는 방안 포함). 그 전까지는 tar 적재 후 삭제, `KEEP=3` 으로 백업 축소 |
| 급히 공간이 필요 | `/nhod`(50G)는 용도 미확인이라 **확인 전 사용 금지** — 인프라에 용도·사용 가능 여부 문의 |

## 4. WAR 반입 형태와 흐름

| 항목 | 정한 형태 |
|---|---|
| 파일 | `.war` 그대로 (zip 형식, 모듈당 66~105MB, 6개) |
| 이름 | `<모듈>-<버전>-<YYYYMMDD>-<순번>.war` + 같은 이름 `.sha256` (예: `atms-journal-0.0.1-SNAPSHOT-20261012-01.war`). **이 이름으로 배포 폴더가 자동 결정**되므로 바꾸지 않는다 |
| 반입 시스템이 `.war` 를 막을 때 | `.zip` 으로 감싸 반입 후 풀기 |

```
[내부 PC] bootWar 빌드 → 이름 변경 + .sha256 생성 (PowerShell)
    │ FileZilla(바이너리), atmswas 접속
    ▼
/home/atmswas/atms/inbox/                     업로드
    │ sha256sum -c  → 확인되면 mv
    ▼
/home/atmswas/atms/release/<모듈>/            보관
    │ atmswas: ~/atms/deploy/jeus/swap-deploy.sh deploy <war>   (WAR 이름 → 폴더 자동)
    ▼
/home/atmswas/catms, catms-<모듈>/             ★ 풀린 상태로 교체 (이전 버전은 deploy/backup)
    │ jeusatms: jeusRstart
    ▼
JEUS atms01 기동 (앱 6개 함께)
```

PC 준비 (PowerShell, 6개 한 번에): [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 3 의 반복문 사용.
한 개만 할 때:
```powershell
cd D:\NH-ATMS\Backend\nh-atms-api\atms-journal\build\libs
$name = "atms-journal-0.0.1-SNAPSHOT-$(Get-Date -Format yyyyMMdd)-01.war"
Copy-Item atms-journal-0.0.1-SNAPSHOT.war $name
"{0}  {1}" -f (Get-FileHash $name -Algorithm SHA256).Hash.ToLower(), $name | Set-Content -Encoding ascii "$name.sha256"
```

서버 (atmswas):
```bash
cd ~/atms/inbox && sha256sum -c atms-*.war.sha256
for m in atms-web atms-journal atms-batch atms-file atms-if-internal atms-if-external; do mkdir -p ~/atms/release/$m; mv $m-[0-9]*.war* ~/atms/release/$m/ 2>/dev/null; done
ls -R ~/atms/release
```

배포 (atmswas) → 재기동 (jeusatms):
```bash
~/atms/deploy/jeus/swap-deploy.sh deploy ~/atms/release/atms-journal/atms-journal-0.0.1-SNAPSHOT-20261012-01.war
/JEUS/jeusATMS/jeusMENU/jeusRstart
```
롤백: `~/atms/deploy/jeus/swap-deploy.sh rollback atms-journal` → `jeusRstart`

## 5. WAR 를 푸는 위치

- **각 배포 폴더(`catms`, `cajou` …) 바로 아래에 압축을 푼 내용물**(`WEB-INF/`, `META-INF/` …)이 와야 한다. WAR 파일 자체를 넣는 것이 아니다.
- 기존 폴더 위에 덮어 풀지 않는다 (삭제된 클래스가 남음) → `swap-deploy.sh` 가 새 폴더에 풀고 통째로 교체
- `WEB-INF/lib-provided`(내장 톰캣)는 삭제, `error.jsp` 는 유지
- 단, JEUS 에 docBase 가 **WAR 파일 경로**로 등록돼 있다면(2장 `domain.xml` 확인) 풀지 않고 WAR 파일을 교체하는 방식으로 바꾼다

## 6. 지금 할 일 순서

1. 2장 확인 명령 실행 (atmswas) → 디스크·권한·그룹 확인
2. 3-2 생성 명령으로 `catms*`·`data`·`deploy`·`atms` 생성 (+ WebtoB 서버 `docs`)
3. WAS 담당(현업)에게 **회신**: "경로 생성·그룹권한 부여 완료, atms01 에 앱 6개(컨텍스트 경로 분리) 등록 요청" — 7-3 문구, JEUS_SOURCE_CHANGES.md 7-6
4. 지원 인프라(Gitea·Jenkins) 셋팅: [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md)
5. JEUS Jakarta 호환성 확인(JEUS 가이드 STEP 1) 과 WAR 빌드 진행

## 7. 디렉토리·그룹권한(JEUS) 절차

### 7-1. 왜 필요한가 (근거)

| # | 근거 | 출처 |
|---|---|---|
| 1 | JEUS 배포 경로는 `/home/atmswas/catms`, WebtoB DocBase 는 `/home/atmswas/docs` 이며 "디렉토리 생성 및 그룹권한(JEUS) 부여 완료 후 회신 요망" | 환경구성표 |
| 2 | 업무팀계정 `atmswas`, 그룹명 `jeus` (GID 2002) | 환경구성표 |
| 3 | JEUS 는 `jeusatms` 계정으로 실행된다 | 2026-10-07 `ps -ef` 결과 |
| 4 | 다른 계정 소유 파일을 읽으려면 **그룹(또는 기타) 권한**이 있어야 하고, 폴더 안으로 들어가려면 **모든 상위 폴더에 실행(x) 권한**이 필요하다 | 리눅스 파일 권한 규칙 |

```
/home/atmswas        atmswas:jeus  drwxr-x---  (750)   ← jeus 그룹이 "들어갈"(x) 수 있어야 함
├─ catms             atmswas:jeus  drwxr-s---  (2750)  ← jeus 그룹이 읽기(r)·들어가기(x). s = 새 파일도 jeus 그룹으로
│  └─ WEB-INF/...    atmswas:jeus  -rw-r-----  (640)   ← 파일은 그룹 읽기
└─ atms              atmswas:jeus  drwx------  (700)   ← JEUS 는 들어갈 필요 없음
```
- 쓰기 권한(w)은 주지 않는다: JEUS 는 앱 파일을 읽기만 하면 된다 (변경은 atmswas 의 배포로만)
- 앱이 파일을 저장해야 하는 경로는 `/home/atmswas/data`(2770) (`jeus-common.yml` 의 `jeus.data-dir`)

### 7-2. 검증 (jeusatms 로 접속, 한 줄씩)

```bash
ls -ld /home/atmswas /home/atmswas/catms
ls -la /home/atmswas/catms
ls /home/atmswas/atms
```
앞의 두 줄은 `Permission denied` 가 **나오지 않아야** 하고, 마지막 줄은 `Permission denied` 가 **나와야** 정상이다 (`atms/` 는 atmswas 전용).

### 7-3. WAS 담당 회신·요청 문구

```
[회신] nbatmsltws01 / nbatmsltwb01 디렉토리 생성 및 그룹권한(JEUS) 부여 완료
- nbatmsltws01: /home/atmswas/catms, cajou, cabat, cfile, cifin, cifex  (atmswas:jeus, 2750)
- nbatmsltws01: /home/atmswas/data   (atmswas:jeus, 2770, 앱 업로드 저장용)
- nbatmsltwb01: /home/atmswas/docs   (atmswas:jeus, 2750)
- jeusatms / webatms 그룹(jeus) 읽기 확인 완료
요청 1: atms01 에 아래 6개 애플리케이션 등록 및 WebtoB DocBase(/home/atmswas/docs) 확인 부탁드립니다.
  catms → /,  cajou → /journal,  cabat → /batch,  cfile → /file,  cifin → /ifin,  cifex → /ifex
요청 2: /home/atmswas 디스크 용량 확인 부탁드립니다. 개발 도구(Git·빌드 서버)와 배포 백업을 이 경로에 두므로
  여유 10GB 이상이 필요합니다. 공용 /home(10G) 이라면 전용 디스크 할당(예: 기존 /home/userATMS 20G 의 이관)을 요청드립니다.
```

### 7-4. 확인되지 않은 것 (담당자에게 물어볼 것)

| 항목 | 이유 |
|---|---|
| `/home/atmswas` 디스크 (공용 10G 인지, 전용인지) | 3-3 |
| 권한값(750/2750/2770) 은행 기준 존재 여부 | 위 값은 "그룹 읽기만" 원칙으로 정한 제안값 |
| docBase 가 이미 JEUS 에 등록돼 있는지 | `domain.xml` 확인 필요 |
| HIWARE 로 atmswas 접속 시 `systemctl --user` 사용 가능 여부 | rootless Podman 자동기동(Quadlet)·백업 타이머 전제 |
