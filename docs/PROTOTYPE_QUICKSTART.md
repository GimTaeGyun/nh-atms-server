# 프로토타입 기동 가이드 (반입 파일 배치 + 실행)

기준 경로: **`/home/atmswas`** (서버 `nbatmsltws01`, 계정 `atmswas`)
> 2026-10-12: 계정·경로를 `userATMS` · `/home/userATMS` 에서 **`atmswas` · `/home/atmswas`** 로 변경. 이 문서는 컨테이너 대안 경로(보류)용이다. 현재 셋팅 순서는 [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md)
목표: `nh-atms-api` 의 웹 모듈(`atms-web`)을 개발서버에 **오늘 바로** 띄워 동작 확인

> - `sudo`, `su` 입력 금지 (HIWARE). 명령은 한 줄씩 입력.
> - 컨테이너 베이스 이미지가 아직 없으므로 **1단계는 JDK 직접 실행**, 이미지 반입 후 **2단계에서 컨테이너로 전환**한다.
>   두 단계는 같은 디렉토리·설정 파일을 그대로 사용한다.

---

## 1. 최종 디렉토리 구조

```
/home/atmswas/
└─ atms/
   ├─ jdk/                          ← (선택) 반입 JDK 21.0.12. 없으면 서버 OpenJDK 21 심볼릭 링크
   ├─ release/
   │   └─ atms-web-0.0.1.jar        ← PC 에서 빌드한 실행 jar
   ├─ config/
   │   └─ atms-web/
   │       └─ application-dev.yml   ← DB 등 서버 전용 설정 (chmod 600)
   ├─ logs/
   │   └─ atms-web/                 ← 앱 로그, stdout.log
   ├─ run/                          ← pid 파일
   ├─ images/                       ← (2단계) 베이스 이미지 tar
   ├─ bin/ conf/ deploy/ docs/      ← (2단계) 배포킷 (본 저장소)
```

## 2. 반입 파일 목록

| # | PC 원본 | 서버 위치 | 필수 | 비고 |
|---|---|---|---|---|
| 1 | `nh-atms-api\atms-web\build\libs\atms-web-*.jar` (PC 빌드 결과) | `/home/atmswas/atms/release/` | **필수** | `-plain.jar` 는 제외 |
| 2 | `application-dev.yml` (아래 4.3 내용으로 PC 에서 작성) | `/home/atmswas/atms/config/atms-web/` | **필수** | 프로젝트에 dev 설정이 이미 있으면 그것을 기준으로 |
| 3 | `jdk-21.0.12_linux-x64_bin.tar.gz` (반입본 `.tar.7z` 에서 꺼낸 **tar.gz 그대로**) | `/home/atmswas/atms/` → `jdk/` 로 압축해제 | 선택 | 없으면 서버 기설치 OpenJDK 21 사용 |
| 4 | 본 저장소(ATMS-Server) zip | `/home/atmswas/atms/` | 2단계 | 반입 절차 필요 시 1단계는 수동 명령으로 진행 |
| 5 | Java 21 베이스 이미지 tar (`eclipse-temurin:21-jre-jammy`) | `/home/atmswas/atms/images/` | 2단계 | **보류** — JEUS 가 Jakarta 미지원일 때만 반입 (`REQUIRED_FILES.md` 2-4) |

**반입하지 않는 것**: Docker 29.8.0 바이너리(root 필요), Docker Desktop, IntelliJ/VSCode/Node(PC 전용), gradle 캐시(PC 빌드용), 소스 전체(서버에서 빌드하지 않음)

> JDK 를 Windows 에서 압축해제한 폴더(`D:\dev\jdk-21.0.12_linux`)를 그대로 올리면 실행권한(+x)이 사라진다.
> 가급적 **원본 tar.gz** 를 올리고, 폴더를 올렸다면 4.2 의 `chmod` 를 실행한다.

## 3. PC 작업

### 3.1 JAR 빌드 (오프라인)

```bat
set JAVA_HOME=D:\dev\jdk-21.0.12
set GRADLE_USER_HOME=D:\dev\gradle-home
cd /d <NH-ATMS>\2.Project\1.Project\Backend\nh-atms-api
gradlew.bat :atms-web:bootJar --offline
dir atms-web\build\libs
```

- `atms-web` 이 실행 모듈이 아니면 `gradlew.bat tasks --all --offline | findstr bootJar` 로 bootJar 가 있는 모듈 확인
- IntelliJ 에서 Gradle 창 → `atms-web > Tasks > build > bootJar` 실행해도 동일

### 3.2 FileZilla 업로드

1. 원격 경로 `/home/atmswas` 에서 `atms` 폴더 생성 (또는 서버에서 4.1 먼저 실행)
2. jar → `/home/atmswas/atms/release/`
3. `application-dev.yml` → `/home/atmswas/atms/config/atms-web/`
4. (선택) `jdk-21.0.12_linux-x64_bin.tar.gz` → `/home/atmswas/atms/`
5. 전송 유형: 메뉴 `전송 > 전송 유형 > 바이너리`

## 4. 서버 작업 — 1단계: JDK 직접 실행 (즉시 가능)

### 4.1 디렉토리 생성

```bash
mkdir -p ~/atms/release ~/atms/config/atms-web ~/atms/logs/atms-web ~/atms/run ~/atms/images
chmod 700 ~/atms/config
```

### 4.2 JDK 준비 (둘 중 하나)

**A. 서버 기설치 OpenJDK 21 사용 (반입 불필요, 가장 빠름)**

```bash
ls /usr/lib/jvm/
ln -s /usr/lib/jvm/java-21-openjdk-21.0.2.0.13-1.el9.x86_64 ~/atms/jdk
~/atms/jdk/bin/java -version
```

**B. 반입한 JDK 21.0.12 사용 (권장 버전)**

```bash
cd ~/atms
mkdir -p jdk
tar -xzf jdk-21.0.12_linux-x64_bin.tar.gz -C jdk --strip-components=1
chmod +x jdk/bin/* jdk/lib/jspawnhelper 2>/dev/null
jdk/bin/java -version
```

### 4.3 설정 파일 (`~/atms/config/atms-web/application-dev.yml`)

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:oracle:thin:@//<DB_HOST>:<DB_PORT>/NBATMSTP
    username: ATMS_APP
    password: <비밀번호>
    driver-class-name: oracle.jdbc.OracleDriver
logging:
  file:
    path: /home/atmswas/atms/logs/atms-web
```

```bash
chmod 600 ~/atms/config/atms-web/application-dev.yml
```

DB 네트워크 사전 확인 (응답 없으면 방화벽 요청 필요):

```bash
timeout 3 bash -c '</dev/tcp/<DB_HOST>/<DB_PORT>' && echo DB-OK || echo DB-NG
```

### 4.4 포트 확인

```bash
ss -tln | grep ':8080 '
```
출력이 없으면 사용 가능. 있으면 `--server.port=8081` 등으로 변경.

### 4.5 기동

```bash
cd ~/atms
nohup ~/atms/jdk/bin/java \
  -Xms256m -Xmx1g -XX:ActiveProcessorCount=1 \
  -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul \
  -jar ~/atms/release/atms-web-0.0.1.jar \
  --spring.profiles.active=dev \
  --server.port=8080 \
  --spring.config.additional-location=optional:file:/home/atmswas/atms/config/atms-web/ \
  > ~/atms/logs/atms-web/stdout.log 2>&1 &
echo $! > ~/atms/run/atms-web.pid
```

> 배포킷(bin/)을 반입했다면 위 대신: `bin/init-dirs.sh` → `bin/direct.sh start atms-web release/atms-web-0.0.1.jar`

### 4.6 확인

```bash
tail -f ~/atms/logs/atms-web/stdout.log                 # "Started ...Application in N seconds" 확인 후 Ctrl+C
curl -i http://127.0.0.1:8080/actuator/health          # actuator 없으면 실제 API URL 로
ps -ef | grep atms-web | grep -v grep
```

| 확인 | 정상 기준 |
|---|---|
| 기동 로그 | `Started ... in N seconds`, `Tomcat started on port 8080` |
| DB | `HikariPool-1 - Start completed` (DB 미사용 화면만 볼 경우 생략) |
| 응답 | `HTTP/1.1 200` |
| 자원 | `top -u atmswas` 에서 CPU 과점유 없음 (서버 2 core, JEUS 공존) |
| PC 접속 | 브라우저 `http://nbatmsltws01:8080/...` → 실패 시 방화벽 요청 (서버 내 curl 성공이면 앱은 정상) |

### 4.7 중지 / 재기동

```bash
kill $(cat ~/atms/run/atms-web.pid)            # 정상 종료 (30초 내 종료 안 되면 kill -9)
```
재배포: jar 교체 업로드 → 중지 → 4.5 재실행

## 5. 서버 작업 — 2단계: 컨테이너(Podman) 전환

전제: 배포킷(본 저장소) 반입 + Java 21 베이스 이미지 tar 반입

```bash
cd ~/atms                                      # 배포킷을 ~/atms 에 풀어 둔 상태 (기존 release/config/logs/jdk 유지)
sed -i 's/\r$//' bin/*.sh conf/*.example conf/apps/*.example
chmod +x bin/*.sh
bin/init-dirs.sh                               # 기존 config/atms-web/application-dev.yml 은 덮어쓰지 않음
bin/precheck.sh

kill $(cat ~/atms/run/atms-web.pid)            # 1단계 프로세스 중지 (포트 반납)
bin/load-image.sh images/temurin-21-jre-jammy.tar.gz
podman images                                  # 이미지명 = conf/atms.env 의 BASE_IMAGE 와 일치 확인
bin/deploy.sh atms-web release/atms-web-0.0.1.jar
bin/status.sh atms-web
```

> 컨테이너 안에서는 로그 경로가 `/app/logs` 이므로, 2단계에서는 `application-dev.yml` 의 `logging.file.path` 를 `/app/logs` 로 바꾼다
> (호스트의 `~/atms/logs/atms-web` 에 그대로 쌓임).

## 6. 문제 발생 시

| 증상 | 조치 |
|---|---|
| `Permission denied` (java 실행) | `chmod +x ~/atms/jdk/bin/*` |
| `UnsupportedClassVersionError` | jar 가 21 보다 높은 버전으로 빌드됨 → PC `JAVA_HOME` 확인 |
| `no main manifest attribute` | `-plain.jar` 업로드함 → bootJar 결과물 사용 |
| `Port 8080 was already in use` | `--server.port` 변경 |
| `ORA-12170` / `Connection refused` (DB) | 4.3 DB 네트워크 확인 → 방화벽 요청 |
| `Failed to determine a suitable driver class` | jar 에 ojdbc 미포함 또는 datasource 설정 누락 |
| 로그아웃 후 프로세스 종료됨 | `nohup ... &` 로 기동했는지 확인. 계속되면 linger 요청(인프라) |
