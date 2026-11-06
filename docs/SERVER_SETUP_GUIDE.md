# ATMS 개발서버 컨테이너 환경 셋팅 매뉴얼 & 계획

| 항목 | 내용 |
|---|---|
| 대상 서버 | `nbatmsltws01` (자동화기기 관리시스템 WAS, RHEL 9.8) |
| 작업 계정 / 경로 | `atmswas` / `/home/atmswas/atms` (2026-10-12 변경, 디스크 크기 [미확인]) — 예전 `userATMS` / `/home/userATMS/atms` 는 사용 안 함 |
| 대상 프로젝트 | `NH-ATMS / Backend / nh-atms-api` (Spring Boot, Java 21, Gradle 멀티모듈) |
| 런타임 | **rootless Podman 5.8.2** (서버 기설치, Docker 호환) |
| 작성일 | 2026-10-07 (초안 v0.2) |

> **이 문서의 모든 명령은 `atmswas` 일반 권한으로 실행한다. `sudo`, `su` 는 절대 입력하지 않는다.**
> (HIWARE 통제: 금지명령 3회 입력 시 세션 차단. 2026-10-07 현재 1회 누적)
> **변경(2026-10-07) — 실행 환경:** 로컬 개발은 Spring Boot **내장 톰캣**, 개발서버는 이미 셋팅된 **JEUS 9.1(WAR 배포)** 을 사용한다. 이 문서의 컨테이너 기반 앱 배포(Phase 2~5)는 JEUS 배포가 준비되기 전 **프로토타입/임시 경로**이며, 서버의 Podman 은 Gitea·Jenkins 등 지원 인프라 용도로 쓴다. 자세한 영향은 `CLAUDE.md` 의 Runtime decision 참고.
>
> **변경(2026-10-07):** 절차를 거치면 **하루 동안 root 권한**을 받을 수 있다. root 가 필요한 작업은 [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 에 모아 두었다가 권한을 받은 날 한 번에 처리한다.

---

## 1. 현황 분석 요약

### 1.1 서버 (확인 완료)

| 항목 | 값 | 판단 |
|---|---|---|
| OS / Kernel | RHEL 9.8 (Plow) / 5.14.0-687.26.1.el9_8 | Podman Quadlet(systemd --user) 사용 가능 |
| 권한 | `sudo` HIWARE 차단 / **1일 root 권한 신청 가능** | root 작업은 `ROOT_DAY_CHECKLIST.md` 에 모아 하루에 처리 (또는 인프라팀 요청) |
| 컨테이너 | `podman-5.8.2`, `cockpit-podman` 설치됨 / docker, podman-compose 없음 | **Podman rootless 채택** |
| CPU / MEM | **2 core** / 31Gi (가용 25Gi), swap 4G | CPU 가 병목. JEUS 공존 → 컨테이너 합계 CPU 1.5 이하 |
| 디스크 | (2026-10-07) `/home/userATMS` 20G 전용 — **2026-10-12 부터 사용 안 함**. `/home/atmswas` 는 [미확인] (`/home` 공용 10G 일 수 있음), `/WASlogs` 50G, `/tmp` 10G | `df -h /home/atmswas` 확인 필요 (`SERVER_DIRECTORY_LAYOUT.md` 3-3) |
| LISTEN 포트 | 6600, 6700, 6800 (0.0.0.0), 42539, 44775 (127.0.0.1) | 앱 포트는 8080~ 사용 (재확인 필요) |
| 서버 JDK | `/usr/lib/jvm/java-21-openjdk-21.0.2...` (JEUS 용) | 오래된 21.0.2 → 앱은 반입한 **21.0.12** 사용 |

### 1.2 은행 표준 구성 (환경구성표 기준)

```
[PC/단말] ──> nbatmsltwb01  WebtoB 5.0 (webatms, :9000, DocBase /home/atmswas/docs)
                   │
                   ▼
              nbatmsltws01  JEUS 9.1 (jeusatms, 컨테이너 atms01, Deploy /home/atmswas/catms)   ← 이 서버
                   │                          [신규] rootless Podman 컨테이너 (atmswas, :8080~)
                   ▼
              Oracle  dsATMS01 / Service NBATMSTP / 계정 ATMS_APP
```

| 구성표 항목 | 컨테이너 환경 반영 |
|---|---|
| JDK 21 | 이미지/직접실행 모두 Java 21 (21.0.12) |
| DB `NBATMSTP` / `ATMS_APP` | JEUS 데이터소스(JNDI) 사용 불가 → Spring HikariCP 로 직접 접속. **DB 호스트:포트 확인 필요** |
| 로그 표준 `/WASlogs/...` | 1차: `~/atms/logs/<앱>` → 2차: `/WASlogs/` 하위로 이전 검토(권한 요청) |
| 에러페이지 의무 (403/404/503 등) | Spring Boot 앱에 커스텀 에러페이지/ErrorController 필수 (WebtoB/JEUS 와 동일 기준) |
| 앞단 WebtoB (`nbatmsltwb01:9000`) | 향후 WebtoB → `nbatmsltws01:<앱포트>` 리버스 프록시 연동 (방화벽 필요) |
| 업무계정 `atmswas` (그룹 jeus, GID 2002) | 공식 JEUS 배포 계정. **2026-10-12 부터 컨테이너 환경·배포킷도 atmswas 로 통합** |

### 1.3 반입 파일 (PC `D:\07. 반입파일\260929반입파일`, `D:\dev`)

| 파일 | 용도 | 서버 반입 |
|---|---|---|
| `docker/docker-29.8.0.tgz` (containerd, dockerd, docker, runc, ctr, docker-proxy, docker-init) | Docker 엔진 static 바이너리 | **보류** (아래 1.4) |
| `docker/Docker Desktop Installer.exe`, `install_docker_desktop.docx` | PC 용 Docker Desktop | PC 전용 |
| `docker/install_offline_docker.md` | 오프라인 Docker 설치 문서 | 참고 (root 전제일 가능성 높음) |
| `D:\dev\jdk-21.0.12_linux` (bin, conf, lib, jmods ...) | Linux JDK 21.0.12 (압축해제본) | **반입 → `~/atms/jdk`** |
| `D:\dev\jdk-21.0.12`, `gradle`, `gradle-home`, `offline-seed`(gradle 캐시, wrapper) | PC 오프라인 빌드 환경 | PC 전용 |
| `node-v22.23.2`, `pnpm-offline-package`, `npm-cache` | Frontend 빌드 | PC 전용 (Frontend 배포 방식 미정) |
| `Jenkins_v1.1.zip` | 향후 CI/CD | Phase 6 |
| `NH-ATMS.zip` → `1.Project` | 소스 (Backend / Frontend / infra / tools / `docker-compose.yml` / `Jenkinsfile` / `.env.example`) | 소스는 PC 에서 빌드 |

**누락: Java 21 컨테이너 베이스 이미지(tar)** → Phase 2 에서 확보 (가장 큰 선결과제)
> 2026-10-12: JEUS 배포 기준으로 바뀌어 이 이미지는 **보류**(JEUS 가 Jakarta 미지원일 때만 반입). 현재 반입 목록은 [REQUIRED_FILES.md](REQUIRED_FILES.md)

### 1.4 Docker static 바이너리(29.8.0)를 쓰지 않는 이유

| 항목 | Docker 29.8.0 static | Podman 5.8.2 (기설치) |
|---|---|---|
| 설치 | `/usr/bin` 복사 + systemd 등록 → **root 필요** | 설치 불필요 |
| 일반계정 실행 | rootless 모드에 `docker-rootless-extras`(rootlesskit 등) 필요 → **반입본에 없음** | 기본 rootless |
| 보안 | `docker` 그룹 = 사실상 root 권한 → 금융권 보안심사 이슈 | 사용자 권한 내에서만 동작 |
| 호환 | - | Dockerfile, `docker` 명령 문법 동일 |

→ **Podman rootless 로 진행**, Docker 엔진 설치는 인프라팀이 공식 지원할 때만 재검토.
(참고: 반입본의 `docker` CLI 바이너리는 Podman 소켓에 붙여 쓸 수 있음 → Phase 6 compose 연동 시 활용)

### 1.5 소스 구조 (nh-atms-api)

```
nh-atms-api/
├─ atms-web          ← API 서버 (bootJar 예상, 1차 대상)
├─ atms-batch        ← 배치 (bootJar 여부 확인)
├─ atms-if-external  ← 대외 인터페이스 (bootJar 여부 확인)
├─ atms-if-internal  ← 대내 인터페이스 (bootJar 여부 확인)
├─ atms-common / atms-file / atms-journal  ← 공통 라이브러리로 추정
├─ build.gradle, settings.gradle, gradle.properties, gradlew
1.Project/
├─ Backend / Frontend / infra / tools / Document
├─ docker-compose.yml (12KB), Jenkinsfile, .env, .env.example
```

배포킷은 **앱(모듈) 단위**로 구성 → `conf/apps/<모듈명>.env` 하나 추가 = 컨테이너 하나 추가.

---

## 2. 목표 아키텍처

```
[1차: 지금]                                     [2차: 향후]
PC(IntelliJ, offline gradle)                    Jenkins (Jenkins_v1.1)
  │ bootJar                                         │ build → jar/image
  ▼ SFTP                                            ▼
~/atms/release/*.jar                            ~/atms/release
  │ bin/deploy.sh <앱> <jar>                        │ bin/deploy.sh (Jenkinsfile 에서 호출)
  ▼                                                 ▼
podman build → localhost/<앱>:<태그>             WebtoB(nbatmsltwb01:9000) ─proxy─> :8080
podman run  -p 8080:8080 (rootless)             compose(docker-compose.yml) 로 다중 서비스
  │
  ├─ ~/atms/config/<앱>/application-dev.yml (ro)
  └─ ~/atms/logs/<앱>/
```

---

## 3. 서버 셋팅 계획 (Phase / 담당 / 선행조건)

| Phase | 작업 | 담당 | 선행 | 산출물 / 완료 기준 |
|---|---|---|---|---|
| **0** | 사전 점검, 인프라팀 요청 발송 | 본인 | - | `precheck.sh` 결과, 요청서(7장) 발송 |
| **1** | 배포킷·JDK 반입, 디렉토리 초기화 | 본인 | 0 | `~/atms` 구조 생성, `~/atms/jdk/bin/java -version` = 21.0.12 |
| **2** | 베이스 이미지 확보 → `podman load` | 본인 (+반입 승인) | 1 | `podman images` 에 Java 21 이미지 |
| **2-B** | (이미지 지연 시) JDK 직접 실행으로 임시 기동 | 본인 | 1, 3 | `bin/direct.sh start` 헬스체크 OK |
| **3** | PC 에서 bootJar 빌드 → SFTP 업로드 | 본인 | - | `~/atms/release/atms-web-*.jar` |
| **4** | 이미지 빌드·컨테이너 기동·검증 | 본인 | 2, 3, DB방화벽 | `bin/deploy.sh` 성공, DB 연결, PC 브라우저 접속 |
| **5** | 재부팅 자동기동 (Quadlet) | 본인 | 4, **linger(인프라)** | 재부팅 후 자동 기동 확인 |
| **6** | 확장: 추가 모듈, compose, WebtoB 연동, Jenkins | 본인+인프라 | 5 | 별도 계획 |

### 일정(안)

| 일차 | 내용 |
|---|---|
| D+0 | Phase 0 (점검·요청서 발송), Phase 1, Phase 3 |
| D+0~ | Phase 2 반입 신청 → 승인 대기 중 Phase 2-B 로 개발서버 선 가동 |
| 이미지 반입 후 | Phase 4 |
| linger 처리 후 | Phase 5 |

---

## 4. 상세 절차

### Phase 0. 사전 점검

> 명령은 **한 줄씩** 입력 (한 줄에 금지명령이 섞이면 전체가 차단·카운트됨)

```bash
id
podman --version
podman info | head -40
grep atmswas /etc/subuid /etc/subgid
loginctl show-user atmswas -p Linger
getenforce
ss -tln
ls -ld /WASlogs
```

| 확인 항목 | 정상 | 비정상 시 |
|---|---|---|
| `podman info` 의 `rootless: true` | 출력됨 | subuid/subgid 미등록 가능 → 인프라 요청 |
| `/etc/subuid`, `/etc/subgid` 에 atmswas | `atmswas:100000:65536` 형태 | 인프라 요청 (필수) |
| `Linger=yes` | yes | 인프라 요청 (Phase 5 필수) |
| `getenforce` | Enforcing 이어도 OK | 볼륨에 `:Z` 적용(스크립트 반영됨) |
| `ss -tln` 에 `:8080` 없음 | 없음 | `conf/apps/<앱>.env` HOST_PORT 변경 |

배포킷 반입 후에는 `bin/precheck.sh` 로 동일 항목을 한 번에 점검할 수 있다.

### Phase 1. 배포킷·JDK 반입 및 초기화

**1) PC 준비**

| 반입 대상 (PC) | 서버 경로 |
|---|---|
| 본 저장소(ATMS-Server) 전체 (zip 다운로드 후 반입) | `/home/atmswas/atms/` |
| `D:\dev\jdk-21.0.12_linux\` 폴더 전체 | `/home/atmswas/atms/jdk/` |

- FileZilla 전송 유형: **바이너리** (스크립트 줄바꿈 깨짐 방지)
- JDK 폴더는 파일이 많으므로 PC 에서 tar.gz 로 묶어 올리는 것을 권장
  (반입본 `jdk-21.0.12_linux-x64_bin.tar.gz` 원본이 있으면 그대로 업로드)

**2) 서버**

```bash
cd /home/atmswas/atms

# (JDK 를 tar.gz 로 올린 경우)
mkdir -p jdk && tar -xzf jdk-21.0.12_linux-x64_bin.tar.gz -C jdk --strip-components=1

# Windows 줄바꿈 제거 (필요 시) 및 실행권한
sed -i 's/\r$//' bin/*.sh conf/*.example conf/apps/*.example
chmod +x bin/*.sh

./jdk/bin/java -version          # 21.0.12 확인
bin/init-dirs.sh                 # conf/*.env 생성 + 앱별 logs/config 디렉토리 생성
bin/precheck.sh                  # 서버 점검
```

**3) 생성되는 구조**

```
/home/atmswas/atms/
├─ bin/                 배포 스크립트
├─ conf/
│  ├─ atms.env          공통 설정 (BASE_IMAGE 등)
│  └─ apps/
│     ├─ atms-web.env            앱 설정 (포트, CPU/MEM, 헬스체크)
│     └─ atms-web.container.env  컨테이너 환경변수 (프로파일, JAVA_OPTS)
├─ config/atms-web/application-dev.yml   ← DB 비밀번호 등 (chmod 600, Git 제외)
├─ deploy/              Dockerfile
├─ images/              반입 이미지 tar
├─ jdk/                 JDK 21.0.12
├─ logs/atms-web/       앱 로그
├─ release/             업로드한 jar
├─ build/, run/         작업용
└─ docs/
```

**4) 설정 수정**

```bash
vi config/atms-web/application-dev.yml   # DB_HOST:PORT, 비밀번호 입력
vi conf/apps/atms-web.env                # 포트/자원 확인
```

### Phase 2. 베이스 이미지 확보 (선결과제)

| 방법 | 절차 | 비고 |
|---|---|---|
| **A. 외부망 반입 (권장)** | 인터넷 PC: `docker pull eclipse-temurin:21-jre-jammy` → `docker save eclipse-temurin:21-jre-jammy \| gzip > temurin-21-jre-jammy.tar.gz` → 반입 신청 | 약 90MB(gzip 시). 반입 절차 소요 |
| B. PC Docker Desktop 보유분 | 개발 PC 에서 `docker images` 확인 → 있으면 `docker save` | Docker Desktop 기설치(설치파일 반입됨) |
| C. OS 이미지 + 반입 JDK | `ubi9-minimal` 등 OS 이미지만 반입 → `DOCKERFILE=Dockerfile.jdk-local` | 서버 `~/atms/jdk` 를 이미지에 포함 |
| **2-B. 임시: 직접 실행** | 이미지 없이 `bin/direct.sh` 로 JDK 실행 | 즉시 가능. 컨테이너 전환 시 동일 설정 재사용 |

적재:

```bash
# PC -> SFTP -> ~/atms/images/
bin/load-image.sh images/temurin-21-jre-jammy.tar.gz
podman images        # REPOSITORY:TAG 확인 → conf/atms.env 의 BASE_IMAGE 와 일치시킬 것
```

> 프로젝트의 `docker-compose.yml` 에서 사용하는 이미지 목록도 함께 반입 신청하면 Phase 6 에서 재신청이 필요 없다.

### Phase 3. PC 에서 JAR 빌드 (오프라인)

```bat
:: PC (cmd) - offline-seed 의 gradle 캐시가 D:\dev\gradle-home 에 풀려 있다는 전제
set JAVA_HOME=D:\dev\jdk-21.0.12
set GRADLE_USER_HOME=D:\dev\gradle-home
cd <...>\1.Project\Backend\nh-atms-api
gradlew.bat :atms-web:bootJar --offline
dir atms-web\build\libs
```

- `*-plain.jar` 가 아닌 **실행 가능한 jar** 를 업로드
- 업로드 위치: `/home/atmswas/atms/release/atms-web-<버전>.jar`
- 앱 요구사항 체크
  - [ ] `ojdbc` 드라이버가 의존성에 포함 (오프라인 캐시 확인)
  - [ ] `spring-boot-starter-actuator` 포함 (없으면 `HEALTH_PATH` 를 실제 URL 로 변경)
  - [ ] 403/404/500/503 커스텀 에러페이지 (환경구성표 요구사항)
  - [ ] 로그 경로가 `logging.file.path` 를 따름 (logback 설정에 하드코딩 경로 없음)

### Phase 4. 이미지 빌드·기동·검증

```bash
cd /home/atmswas/atms
bin/deploy.sh atms-web release/atms-web-0.0.1.jar
```

`deploy.sh` 동작: ① `podman build --pull=never` → ② 기존 `current` 를 `previous` 로 태깅 → ③ 새 이미지를 `current` 로 → ④ 재기동 → ⑤ 헬스체크(최대 180초) → ⑥ 실패 시 `previous` 로 자동 롤백

검증:

```bash
bin/status.sh atms-web               # 컨테이너/자원/헬스체크
bin/logs.sh atms-web                 # 표준출력 로그 (Ctrl+C 종료)
curl -i http://127.0.0.1:8080/actuator/health
```

| 검증 항목 | 방법 | 기대 |
|---|---|---|
| 기동 | `bin/status.sh atms-web` | Up, HTTP 200 |
| DB 연결 | 로그에 `HikariPool-1 - Start completed` | 오류 없음 |
| 외부 접속 | PC 브라우저 `http://nbatmsltws01:8080/actuator/health` | 200 (실패 시 방화벽 요청) |
| 자원 | `podman stats` | CPU ≤ 1.0, MEM ≤ 2g |
| JEUS 영향 | JEUS 기동 상태 / 서버 load (`uptime`) | 영향 없음 |

**Phase 2-B (이미지 확보 전 임시 실행)**

```bash
bin/direct.sh start  atms-web release/atms-web-0.0.1.jar
bin/direct.sh status atms-web
bin/direct.sh stop   atms-web
```

컨테이너 전환 시: `bin/direct.sh stop atms-web` → `bin/deploy.sh atms-web <jar>` (설정 파일 그대로 사용)

### Phase 5. 재부팅 자동기동 (Quadlet)

전제: `loginctl show-user atmswas -p Linger` → `Linger=yes` (인프라팀 처리)

```bash
bin/install-quadlet.sh atms-web
systemctl --user status atms-web.service
```

- 이후 `bin/start.sh / stop.sh / deploy.sh` 는 자동으로 systemd 를 통해 동작
- 해제: `bin/uninstall-quadlet.sh atms-web`
- Linger 없이 `podman run` 만 사용할 경우: 서버 재부팅 시 수동 기동 필요 (`bin/start.sh atms-web`)

### Phase 6. 확장 (향후)

| 항목 | 방법 | 필요 사항 |
|---|---|---|
| 모듈 추가 (batch, if-external 등) | `conf/apps/atms-web.env.example` 복사 → `atms-batch.env.example` (포트 8081 등) → `bin/init-dirs.sh` | CPU 2 core 한계 → 동시 기동 수 조절 |
| 프로젝트 `docker-compose.yml` 사용 | `systemctl --user enable --now podman.socket` + 반입한 `docker` CLI + `docker-compose` 플러그인(추가 반입) + `DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock` | compose 내 이미지 전부 반입 |
| WebtoB 연동 | `nbatmsltwb01` WebtoB 에서 `nbatmsltws01:8080` 으로 Reverse Proxy | WebtoB 설정 권한(webatms), 방화벽 |
| Jenkins | `Jenkins_v1.1` 구성 후 Jenkinsfile 에서 SFTP 업로드 + `ssh ... bin/deploy.sh` 호출 | Jenkins 서버 위치/계정 결정 |
| 로그 표준화 | `/WASlogs/<디렉토리>` 생성 후 `BASE_DIR` 하위 logs 를 심볼릭 링크 | 인프라 권한 요청 |

---

## 5. 운영 명령 요약

| 목적 | 명령 |
|---|---|
| 배포 | `bin/deploy.sh <앱> <jar>` |
| 롤백 | `bin/rollback.sh <앱>` (직전) / `bin/rollback.sh <앱> 20261007-103000` |
| 기동/중지 | `bin/start.sh <앱>` / `bin/stop.sh <앱>` |
| 상태 | `bin/status.sh` (전체) / `bin/status.sh <앱>` |
| 로그 | `bin/logs.sh <앱>` / `bin/logs.sh <앱> file` |
| 이미지 적재 | `bin/load-image.sh <tar>` |
| 자동기동 | `bin/install-quadlet.sh <앱>` / `bin/uninstall-quadlet.sh <앱>` |
| 임시 직접실행 | `bin/direct.sh start|stop|status <앱> [jar]` |

---

## 6. 트러블슈팅

| 증상 | 원인 | 조치 |
|---|---|---|
| `cannot find UID/GID for user atmswas` | subuid/subgid 미등록 | 인프라 요청 |
| `potentially insufficient UIDs or GIDs` | subuid 범위 부족 | 인프라 요청 후 `podman system migrate` |
| `/run/user/<uid>` 관련 오류, 로그아웃 후 컨테이너 사라짐 | Linger 미설정 | 인프라 요청 (Phase 5) |
| 로그 디렉토리 `Permission denied` | SELinux 라벨 | 볼륨 `:Z` 확인 (스크립트 기본 적용) |
| `bind: permission denied` (포트) | rootless 는 1024 미만 불가 | 8080 등 사용 |
| PC 에서 접속 불가, 서버 curl 은 OK | firewalld/네트워크 방화벽 | 인프라 요청 |
| `Error: ... image not known` / pull 시도 | 베이스 이미지 미적재 | `bin/load-image.sh`, `BASE_IMAGE` 이름 일치 확인 |
| `/bin/bash^M: bad interpreter` | Windows 줄바꿈 | `sed -i 's/\r$//' bin/*.sh` |
| `ORA-12170/12541` | DB 방화벽/주소 | DB 호스트·포트 확인, 방화벽 요청 |
| OOMKilled | MEM_LIMIT 부족 | `conf/apps/<앱>.env` MEM_LIMIT 상향 |
| 응답 지연 | CPU 2 core 경합 | CPU_LIMIT 조정, 동시 기동 앱 수 축소 |

---

## 7. 인프라팀 요청서 (양식)

```
[요청] nbatmsltws01 개발서버 rootless 컨테이너(Podman) 사용 설정
요청자: <성명> / 계정: atmswas

1. /etc/subuid, /etc/subgid 에 atmswas 범위 등록 (예: atmswas:200000:65536)
   - rootless podman 필수 (이미 등록 시 생략)
2. loginctl enable-linger atmswas
   - 서버 재부팅 시 사용자 서비스 자동 기동용
3. 방화벽 오픈
   - 개발자 PC 대역 → nbatmsltws01 TCP 8080 (향후 8081~8089)
   - nbatmsltwb01(WebtoB) → nbatmsltws01 TCP 8080 (향후 연동)
   - nbatmsltws01 → Oracle(NBATMSTP) <DB_IP>:<PORT>
4. (선택) /WASlogs/<컨테이너로그 디렉토리> 생성 및 atmswas 쓰기 권한
5. HIWARE 허용 명령 확인: podman, systemctl --user, loginctl show-user, journalctl --user
```

---

## 8. 미결정 사항 (확인 후 본 문서 갱신)

| # | 항목 | 현재 가정 |
|---|---|---|
| 1 | Java 21 베이스 이미지 반입 가능 여부/방법 | 방법 A 신청, 그동안 2-B |
| 2 | bootJar 대상 모듈 목록 | `atms-web` 1개 (추가 시 conf/apps 추가) |
| 3 | 프로젝트 `docker-compose.yml`, `.env.example`, `Jenkinsfile`, `infra/` 내용 | 미확인 → 공유 시 배포킷과 정합화 |
| 4 | Frontend 배포 방식 (JAR 포함 / 별도 컨테이너 / WebtoB DocBase) | 미정 |
| 5 | DB 호스트·포트 | 미확인 (DBA / JEUS dsATMS01) |
| 6 | 컨테이너 환경의 공식 위상 (개발 전용 / JEUS 대체 PoC) | 개발 전용 |
| 7 | 로그 위치 (`~/atms/logs` vs `/WASlogs`) | `~/atms/logs` 로 시작 |
