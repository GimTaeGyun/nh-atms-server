# 백엔드 6개 모듈 JEUS 배포 + 프론트엔드 WebtoB 배포 가이드 (호환성 확인 겸)

작성일: 2026-10-07 · 개정: 2026-10-12 (6개 모듈 WAR, 컨텍스트 경로 분리)

> **현재 구조 아님 (2026-11-03 표시)** — 이 문서는 **JEUS 서버 1개(atms01)에 WAR 6개·컨텍스트 경로 분리**(2026-10-12~25) 배포 절차 기준이다. 현재는 **JEUS 매니지드 서버 6개**(atms01·ajou01·abat01·file01·ifin01·ifex01, 포트 8080~8085, 컨텍스트 `/`, 게이트웨이 끔, DB 는 JEUS JNDI `dsATMS01`). 지금 할 작업은 [INFRA_MANUAL.md](INFRA_MANUAL.md) 를 따르고, 결정 배경은 [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md). 아래 본문은 이력·참고용으로 남긴다.

| 항목 | 이번 범위 |
|---|---|
| 백엔드 | **WAR 6개** → JEUS 9.1 `atms01` 1개 서버에 컨텍스트로 구분 (`/`=atms-web, `/journal`, `/batch`, `/file`, `/ifin`, `/ifex`) |
| 프론트엔드 | React 빌드 결과(`dist`) → WebtoB (`nbatmsltwb01`, DocBase `/home/atmswas/docs`, 포트 9000) |
| DB | 없으면 **기동·화면 표시까지만** 확인 (로그인·조회 실패가 정상, **atms-batch 는 DB 없으면 배포 실패**) |
| 사용 계정 | `jeusatms` (JEUS 조작), `atmswas` (배포 경로), WebtoB 는 접근 권한 확인부터 |

> 소스가 어떻게·왜 바뀌었는지(게이트웨이, `jeus` 프로파일, 설정 파일)는 [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md).
> 호환성만 먼저 볼 때는 STEP 2~6 을 **atms-web 1개**로 진행해도 된다 (`:atms-web:bootWar -Pjeus`).

> 명령은 **한 줄씩** 입력하고 `sudo`/`su` 는 쓰지 않는다 (HIWARE).
> JEUS/WebtoB 명령·설정 이름은 설치 환경(Fix 버전, 은행 커스텀 메뉴)에 따라 다를 수 있으니, 결과를 보면서 진행한다.

```
[STEP 1] JEUS 버전·Jakarta 지원 확인  ── 미지원이면 여기서 중단 (8장)
[STEP 2] 내부 PC: 설정값 기입 + WAR 6개 빌드
[STEP 3] 서버 업로드
[STEP 4] /home/atmswas/catms, catms-<모듈> 에 배포 (atmswas)
[STEP 5] JEUS 재기동 (jeusatms)
[STEP 6] 기동 확인 = 호환성 최종 판정 + 게이트웨이 확인
[STEP 7] 프론트엔드 빌드 → WebtoB DocBase 배포 + /api 연결
```

---

## STEP 1. JEUS 버전과 Jakarta 지원 확인 (`jeusatms` 계정, nbatmsltws01)

> 버전·도메인·서버·데이터소스·배포 앱을 한 번에 조회하는 읽기 전용 절차와 회신용 기록표: [JEUS_INFO_COLLECTION.md](JEUS_INFO_COLLECTION.md)

Spring Boot 3.5 는 **Jakarta Servlet 5.0 이상**(`jakarta.servlet` 패키지)이 필요하다. JEUS 가 이전 세대(`javax.servlet`)만 지원하면 WAR 를 올려도 앱이 시작되지 않는다.

> 참고 (2026-10-14): TmaxSoft 공식 소개서상 **JEUS 9 는 Jakarta EE 9 인증(Servlet 5.0)** 이다 → javax 전용일 가능성은 낮다. 그래도 Spring Boot 3.5 는 Servlet 6.0 기준이므로 아래 1-3 과 STEP 6 으로 실제 동작을 확인한다.

### 1-1. JEUS 위치·실행 정보

```bash
echo $JEUS_HOME
ls /JEUS/jeusATMS
ls /JEUS/jeusATMS/jeusMENU
ps -ef | grep -v grep | grep -i jeus | cut -c1-400
```
- `ps` 결과에서 확인: 실행 중인 **java 경로**(JDK 버전), 서버 이름(`atms01` 등), 도메인 이름

**2026-10-07 1차 확인 결과 (userATMS)**
- `$JEUS_HOME` 은 userATMS 에 없음 → 경로는 `/JEUS/jeusATMS` 직접 사용 (2026-10-12 부터 작업 계정은 atmswas — atmswas 에서도 `echo $JEUS_HOME` 로 확인)
- `/JEUS/jeusATMS`: `Apache bin ctest derby docs domains jeusMENU lib license logs nodemanager setup shell templates webserver`
- 실행 중: **관리 서버(`-DadminServer`) + NodeManager**, 둘 다 `/usr/lib/jvm/java-21-openjdk-21.0.11` (**Java 21**) 로 구동. 로그 수집용 Flume(JDK 8) 별도
- **업무 서버(`atms01`) 프로세스는 보이지 않음** → 생성 여부/기동 상태를 1-4 에서 확인 (`ls /JEUS/jeusATMS/domains/*/servers`)
- 판정 보류: 1-2(버전), 1-3(jar 검사) 결과 필요. `unzip`/`jar` 가 없으면 python3 로 대체:
  ```bash
  for f in $(find /JEUS/jeusATMS/lib -name "*.jar"); do L=$(python3 -c "import zipfile,sys;print('\n'.join(zipfile.ZipFile(sys.argv[1]).namelist()))" "$f" 2>/dev/null); echo "$L" | grep -q "jakarta/servlet/Servlet.class" && echo "JAKARTA  $f"; echo "$L" | grep -q "javax/servlet/Servlet.class" && echo "JAVAX    $f"; done
  ```

### 1-2. 버전

```bash
jeusadmin -version
find /WASlogs/jeusATMSlogs -name "JeusServer*.log" | head
grep -m5 -i "jeus" <위에서 찾은 JeusServer.log 경로>
```
- `jeusadmin -version` 이 안 되면 로그 앞부분의 기동 배너(`JEUS 9 (Fix#..)` 형태)로 확인

### 1-3. 서블릿 API 세대 판정 (가장 확실한 방법)

JEUS 라이브러리 안에 `jakarta/servlet/Servlet.class` 가 있는지 찾는다.

```bash
J=$(ls -d /usr/lib/jvm/java-21-openjdk-*/bin 2>/dev/null | head -1)
JH=${JEUS_HOME:-/JEUS/jeusATMS}
for f in $(find $JH/lib -name "*.jar" 2>/dev/null); do
  if command -v unzip >/dev/null; then L=$(unzip -l "$f" 2>/dev/null); else L=$("$J/jar" tf "$f" 2>/dev/null); fi
  echo "$L" | grep -q "jakarta/servlet/Servlet.class" && echo "JAKARTA  $f"
  echo "$L" | grep -q "javax/servlet/Servlet.class"   && echo "JAVAX    $f"
done
```

| 결과 | 판정 | 다음 |
|---|---|---|
| `JAKARTA ...` 줄이 있음 | Spring Boot 3 실행 가능성 있음 | STEP 2 진행 |
| `JAVAX ...` 만 있음 | **Spring Boot 3 실행 불가** | 8장 대안 검토 (STEP 2~6 은 확인용으로만 진행 가능) |
| 둘 다 없음 | 경로가 다름 | `find / -name "*.jar" -path "*jeus*" 2>/dev/null \| head` 로 lib 위치 재확인 |

### 1-4. 배포 구성 확인 (STEP 4~6 에 필요)

```bash
find $JH/domains -name domain.xml 2>/dev/null
grep -n -i "catms\|<name>\|listen-port\|context-path\|webtob\|<port>" <위 domain.xml 경로> | head -60
ls -la /home/atmswas/catms
```
> 2026-10-15 확인 (atmswas 로 `domain.xml` 읽기 가능): 서버 이름 `nbatmsltws01_atms01`, base 리스너 **8766** 을 HTTP 리스너가 공유 → `jeus.base-url` = `http://16.88.122.113:8766` (소스 반영 완료 — 2026-10-23 `ss -ltn` 으로 8766 이 113 에만 LISTEN 확인, 127.0.0.1 불가). WebtoB 커넥터 → `nbatmsltwb01`·`nbatmsltwb02` 의 **19000**. DAS 8736. 같은 도메인에 `nbatmsltws02_atms01`(2호기)도 있음. JEUS 로그 `/JEUS/jeusATMS/logs/JeusSystem/nbatmsltws01_atms01/`. 데이터소스 `iddsATMS01` 등록돼 있음 (앱은 Hikari 사용).

기록할 것:
- `atms01` 의 **HTTP 리스너 포트** → `jeus-common.yml` 의 `jeus.base-url` (모듈 간 호출·게이트웨이가 사용)
- `/home/atmswas/catms` 가 어떤 애플리케이션으로 등록돼 있는지, **context-path** (`/` 인지)
- 나머지 5개 앱(`cajou` → `/journal` …) 등록 여부 → 없으면 현업에 등록 요청 ([JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 7-1, 7-6)
- JVM 옵션에 `-Dspring.profiles.active`, `-Xmx`, `MaxMetaspaceSize` 가 있는지 (같은 문서 7-4)
- WebtoB 연동(커넥터) 설정 유무와 포트
- `catms` 안에 이미 있는 파일 (`error.jsp` 등 — 지우지 않는다)

---

## STEP 2. 내부 PC: 설정값 기입 + WAR 6개 빌드

### 2-1. 소스 반영 확인

이 저장소(`main` 브랜치)의 `NH-ATMS/` 에 반영돼 있다. 내부 PC 소스가 별도 사본이면 [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 3장 파일 목록을 그대로 옮긴다.
```powershell
cd D:\NH-ATMS\Backend\nh-atms-api
Select-String -Path build.gradle -Pattern "jeus"
dir jeus\web.xml, atms-common\src\main\resources\jeus-common.yml
```
세 개가 모두 보이면 반영된 것.

### 2-2. 설정값 기입 (내부망 소스에서만)

`atms-common\src\main\resources\jeus-common.yml` 의 `CHANGE_ME` 를 채운다 (DB 호스트·비밀번호, JWT 시크릿, WebtoB·에이전트 접속 주소, `jeus.base-url` 포트).
항목별 의미는 JEUS_SOURCE_CHANGES.md 4-4 표. **실제 값을 넣은 소스는 외부(GitHub)로 push 하지 않는다.**
DB 없이 호환성만 볼 때는 그대로 둬도 기동은 된다 (batch 제외).

### 2-3. 빌드

```bat
set JAVA_HOME=D:\dev\jdk-21.0.12
set GRADLE_USER_HOME=D:\dev\gradle-home
cd /d <NH-ATMS>\Backend\nh-atms-api
gradlew.bat bootWar -Pjeus --offline -x test
dir atms-*\build\libs\*.war
```
**PowerShell 에서 실행할 때** (위 `set`, `gradlew.bat` 은 cmd 문법이라 PowerShell 에서는 동작하지 않음)
```powershell
cd D:\NH-ATMS\Backend\nh-atms-api
$env:JAVA_HOME = "D:\dev\jdk-21.0.12"
$env:GRADLE_USER_HOME = "D:\dev\gradle-home"
.\gradlew.bat bootWar -Pjeus --offline -x test
dir atms-*\build\libs\*.war
```
- PowerShell 은 현재 폴더 실행파일에 `.\` 가 필요하고, 환경변수는 `$env:이름 = "값"` 으로 설정
- 옵션은 `--offline` (대시 2개)
- 한 모듈만: `.\gradlew.bat :atms-journal:bootWar -Pjeus --offline -x test`
- `Task 'bootWar' not found` → 2-1 소스 변경 미반영

결과 (크기는 2026-10-12 실측):

| WAR | 크기 |
|---|---|
| `atms-web\build\libs\atms-web-0.0.1-SNAPSHOT.war` | 약 71MB |
| `atms-journal\build\libs\atms-journal-0.0.1-SNAPSHOT.war` | 약 105MB |
| `atms-batch\build\libs\atms-batch-0.0.1-SNAPSHOT.war` | 약 70MB |
| `atms-file\build\libs\atms-file-0.0.1-SNAPSHOT.war` | 약 73MB |
| `atms-if-internal\build\libs\atms-if-internal-0.0.1-SNAPSHOT.war` | 약 66MB |
| `atms-if-external\build\libs\atms-if-external-0.0.1-SNAPSHOT.war` | 약 66MB |

**빌드 오류 `Cannot find a Java installation ... matching {languageVersion=21, vendor=Eclipse Temurin}`**
루트 `build.gradle` 이 JDK 를 **Eclipse Temurin 21** 로 지정하고 있어, Oracle JDK 만 있는 PC 에서 오프라인이면 실패한다.
- `D:\dev\gradle-home\jdks` 에 Temurin 이 있으면 정상 (이전에 jar 빌드가 됐다면 대부분 여기 있음)
- 없으면 Temurin 21 JDK(Windows zip)를 반입·압축해제 후:
  `gradlew.bat bootWar -Pjeus --offline -x test -Porg.gradle.java.installations.paths=D:\dev\temurin-21`

---

## STEP 3. 서버 업로드

> 2026-10-23: Jenkins 백엔드 잡(`MODE=build-deploy`)이 STEP 2~4(빌드·업로드·교체)를 대신한다 — `deploy-war.sh` 가 SSH 로 WAR 를 받아 `swap-deploy.sh` 를 호출 ([BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6장). 아래는 PC 수동 경로.

반입 형태·이름·업로드 위치는 [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 4장 기준.

PC (PowerShell) — 6개를 한 번에 이름 변경 + `.sha256` 생성:
```powershell
cd D:\NH-ATMS\Backend\nh-atms-api
$out = "D:\atms-release"; New-Item -ItemType Directory -Force $out | Out-Null
$tag = "$(Get-Date -Format yyyyMMdd)-01"
foreach ($m in "atms-web","atms-journal","atms-batch","atms-file","atms-if-internal","atms-if-external") {
  $name = "$m-0.0.1-SNAPSHOT-$tag.war"
  Copy-Item "$m\build\libs\$m-0.0.1-SNAPSHOT.war" "$out\$name"
  "{0}  {1}" -f (Get-FileHash "$out\$name" -Algorithm SHA256).Hash.ToLower(), $name | Set-Content -Encoding ascii "$out\$name.sha256"
}
dir $out
```
- FileZilla(**바이너리**)로 **atmswas** 접속 → `D:\atms-release\*` 를 `/home/atmswas/atms/inbox/` 에 업로드
- 서버 (atmswas, 한 줄씩):
  ```bash
  cd ~/atms/inbox
  sha256sum -c atms-*.war.sha256
  for m in atms-web atms-journal atms-batch atms-file atms-if-internal atms-if-external; do mkdir -p ~/atms/release/$m; mv $m-[0-9]*.war* ~/atms/release/$m/; done
  ls -R ~/atms/release
  ```
- 업로드·보관·배포를 모두 atmswas 한 계정이 한다 (2026-10-12 결정, `/home/userATMS` 사용 안 함). `~/atms` 는 700 이라 WAR(DB 비밀번호 포함)는 atmswas 만 읽는다
- 전제: `/home/atmswas` 아래 6개 배포 폴더와 `atms/` 가 만들어져 있어야 함 (레이아웃 문서 3-2)

---

## STEP 4. 배포 (`atmswas` 계정)

6개 앱이 각자의 exploded 폴더로 등록돼 있다는 전제다 (STEP 1-4, [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 7-1):

| WAR | 배포 폴더 | 컨텍스트 |
|---|---|---|
| atms-web | `/home/atmswas/catms` | `/` |
| atms-journal | `/home/atmswas/cajou` | `/journal` |
| atms-batch | `/home/atmswas/cabat` | `/batch` |
| atms-file | `/home/atmswas/cfile` | `/file` |
| atms-if-internal | `/home/atmswas/cifin` | `/ifin` |
| atms-if-external | `/home/atmswas/cifex` | `/ifex` |

기존 폴더 위에 덮어 풀지 않고 **새 폴더에 푼 뒤 통째로 교체**한다 (이전 버전의 삭제된 클래스가 남지 않게).
모듈 폴더를 `catms` **안에** 넣지 않는다 (`/` 앱의 정적 경로로 WEB-INF 가 노출될 수 있음).

최초 1회: 배포킷 안의 스크립트를 그대로 쓴다 (따로 복사하지 않음). 백업·이력은 `/home/atmswas/deploy` 에 쌓인다
```bash
mkdir -p /home/atmswas/deploy/backup
sed -i 's/\r$//' ~/atms/deploy/jeus/swap-deploy.sh
chmod +x ~/atms/deploy/jeus/swap-deploy.sh
```

배포 (WAR 이름에서 모듈을 읽어 위 표의 폴더로 자동 배포):
```bash
ls -ld /home/atmswas/catms* /home/atmswas/catms/error.jsp          # 현재 상태 확인
for w in ~/atms/release/atms-*/atms-*-20261012-01.war; do ~/atms/deploy/jeus/swap-deploy.sh deploy "$w" || break; done
ls -d /home/atmswas/catms*/WEB-INF
```
- 한 개만: `~/atms/deploy/jeus/swap-deploy.sh deploy ~/atms/release/atms-journal/atms-journal-0.0.1-SNAPSHOT-20261012-01.war`
- 각 배포 끝에 `WAR 프로파일: jeus` 가 나와야 한다 (경고가 나오면 `-Pjeus` 없이 빌드한 WAR)

스크립트 동작: SHA256 확인 → `<폴더>.new-<시각>` 에 압축 해제(`unzip` 없으면 python3) → `WEB-INF/lib-provided` 삭제 → 기존 `error.jsp`·`error/` 와 그룹·권한 이어받기 → 기존 폴더를 `deploy/backup/<폴더명>-<시각>` 로 이동 → 새 폴더로 교체 → 이력 기록, 모듈별 백업 5개 유지.

---

## STEP 5. JEUS 재기동 (`jeusatms` 계정)

```bash
cd /JEUS/jeusATMS/jeusMENU
./jeusRstart
```
- 메뉴가 나오면 `atms01` 을 선택 (은행 커스텀 스크립트라 화면 안내를 따른다)
- 다른 서버/컨테이너를 같이 재기동하지 않도록 주의
- **모듈 1개만 배포했어도 `jeusRstart` 는 6개 앱을 모두 재기동한다** (1~2분 중단, 업무시간 외 권장). 앱 단위 재배포는 현업 확인 전까지 쓰지 않는다 ([JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 12-5). 폴더 교체(STEP 4) 직후 바로 재기동한다 — 교체 후 방치하면 옛/새 클래스가 섞일 수 있다.
- `atms-common` 이나 `jeus-common.yml` 이 바뀌었으면 6개 WAR 를 모두 다시 배포한다.

---

## STEP 6. 기동 확인 = 호환성 최종 판정 + 게이트웨이 확인

JVM 1개에 앱 6개라 기동이 끝나기까지 **1~2분** 걸린다 (2코어 기준 예상).

```bash
L=<STEP 1-2 에서 찾은 atms01 의 JeusServer.log>
grep -E "Started Atms|profile is active|APPLICATION FAILED|Exception" $L | tail -30
P=<atms01 HTTP포트>
curl -s -o /dev/null -w "web      %{http_code}\n" http://127.0.0.1:$P/v3/api-docs
curl -s -o /dev/null -w "journal  %{http_code}\n" http://127.0.0.1:$P/journal/v3/api-docs
curl -s -o /dev/null -w "file(gw) %{http_code}\n" http://127.0.0.1:$P/api/file/firmware/list
curl -s http://127.0.0.1:$P/actuator/health
```
앱 로그는 `jeus.log-dir`(기본 `/WASlogs/jeusATMSlogs/nbatmsltws01/app/<모듈>.log`) 에도 남는다.

| 관찰 결과 | 의미 |
|---|---|
| `Started Atms…Application` 6개 + 각 앱 `profile is active: "jeus"` + web `200` | **성공**. JEUS 에서 Spring Boot 3 동작 |
| `file(gw) 200` | 게이트웨이 정상 (루트 → `/file` 전달). `502` 면 `jeus.base-url` 포트 틀림, `401`/`500` 이면 file 앱 미기동(요청이 루트 앱으로 떨어짐) |
| health `DOWN` | DB 미연결 (DB 기입 전이면 정상) |
| atms-batch 만 `Unable to configure the default job repository` | DB 접속 불가 → DB 기입·방화벽 확인 후 해결 (다른 앱은 영향 없음) |
| `[AdminPasswordInitializer] ... DB 연결 실패` WARN | DB 가 없어서 나오는 정상 경고 |
| Spring 로그가 **전혀 없음** + 모든 URL 404 | **javax 세대 JEUS → Spring Boot 3 미지원.** Spring 의 시작점이 `jakarta.servlet` 방식으로만 선언돼 JEUS 가 앱을 시작하지 않은 것. 8장 |
| `ClassNotFoundException: jakarta.servlet...` | 동일 (미지원) |
| `NoSuchMethodError` / `LinkageError` (jackson, slf4j 등) | JEUS 기본 라이브러리와 충돌 → JEUS 에 "WEB-INF/lib 우선 로딩" 설정 필요. 담당자 확인 |
| `profile is active: "local"` 또는 `Could not resolve placeholder 'JWT_SECRET'` | WAR 에 `web.xml` 이 없음(`-Pjeus` 누락) 또는 JEUS JVM 옵션의 `-Dspring.profiles.active` 가 덮어씀 → 옵션 제거 요청 |
| `ORA-17868 Unknown host CHANGE_ME_DB_HOST` | STEP 2-2 미기입 |
| 로그 파일만 안 생김 (JEUS 로그에 logback `openFile ... failed`) | `jeus.log-dir` 권한. 앱 동작에는 영향 없음 |

서버 안에서 확인되면 PC 브라우저에서 `http://<서버IP>:<atms01 HTTP포트>/swagger-ui.html` 도 확인 (방화벽 필요).

> PC → 8766 직접 접속은 **기능 확인용으로만** 쓴다. 이 경로로 `/api/file` 등 게이트웨이 경로에 부하를 주면 들어온 요청과 전달 요청이 같은 스레드 풀을 써서 서버 전체가 멈출 수 있다 ([JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 12-4).

**롤백** (모듈별)
```bash
~/atms/deploy/jeus/swap-deploy.sh rollback atms-journal     # atmswas: 직전 버전으로 교체
/JEUS/jeusATMS/jeusMENU/jeusRstart                                 # jeusatms: 재기동
```

---

## STEP 7. 프론트엔드 → WebtoB

> **평소 화면 배포는 Jenkins 가 SSH 로 한다** (2026-10-21 결정) → [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md). 아래 7-2·7-3 은 Jenkins 없이 수동으로 할 때의 방법이다. 7-4(`http.m`)·7-6 은 두 방식 공통.

### 7-1. WebtoB 접근 권한 확인 (nbatmsltwb01)

> 2026-10-15 확인 (atmswas): 서버 IP 16.88.122.112 (서비스 IP 잠정), WebtoB 기동 중(webatms), :9000 응답 302, `config/` 는 `webatms:jeus` 700 이라 **http.m 읽기 불가** → 7-4 는 WebtoB 담당 요청 대상.
> 2026-10-21 확인: `/home/atmswas/docs`(2750) 생성·화면 배포됨, 9000 의 DocBase 로 확인 (`/` → 200). wb01 에서 쓸 수 있는 계정은 atmswas 뿐 (webatms 로그인 불가) → 표의 두 번째 줄에 해당.

```bash
id
ls -ld /WEBTOB/webtobATMS /WEBTOB/webtobATMS/config /home/atmswas/docs
ls /WEBTOB/webtobATMS/config
```

| 결과 | 진행 |
|---|---|
| `webatms`(또는 접속 계정)로 `config/http.m` 수정 가능 | 7-3, 7-4 직접 진행 |
| `/home/atmswas/docs` 에 쓰기만 가능 | 7-3 직접, 7-4 는 담당자 요청 |
| 접근 불가 | 7-3, 7-4 모두 인프라/WebtoB 담당 요청 |

### 7-2. 내부 PC: 프론트엔드 빌드

```bat
cd /d <NH-ATMS>\Frontend\nh-atms-web
npm run build
```
`dist\` 폴더를 zip 으로 묶는다 (`dist-YYYYMMDD.zip`).
API 주소는 상대경로(`/api`)라 **코드 수정 없이** WebtoB 와 같은 주소로 호출된다.

### 7-3. DocBase 배포 (nbatmsltwb01)

`swap-deploy.sh` 를 WebtoB 서버에도 두었다면 같은 방식으로 교체 배포할 수 있다 (`error/` 자동 유지, `dist/` 한 겹 자동 처리, zip·tar.gz 모두 가능). Jenkins 배포용으로 `~/deploy/bin/` 에 두었다면 `~/deploy/bin/deploy-front.sh deploy <버전> <sha256> < 파일.tar.gz` 로 Jenkins 와 같은 확인·자동 되돌림까지 할 수 있다 ([FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4-2):
```bash
swap-deploy.sh deploy /home/atmswas/deploy/inbox/nh-atms-web-dist-YYYYMMDD-01.zip /home/atmswas/docs
```
스크립트 없이 수동으로 할 때:

```bash
TS=$(date +%Y%m%d%H%M%S)
tar -czf ~/docs-backup-$TS.tar.gz -C /home/atmswas docs
cd /home/atmswas/docs
unzip -oq /tmp/dist-YYYYMMDD.zip        # 또는 python3 -m zipfile -e /tmp/dist-YYYYMMDD.zip .
ls index.html assets error/error.html   # error/error.html(은행 에러페이지) 유지 확인
```
zip 안에 `dist/` 폴더가 한 겹 더 있으면 그 안의 파일을 DocBase 바로 아래로 옮긴다.

### 7-4. WebtoB → JEUS 연결 (`http.m`)

화면(정적 파일)은 WebtoB 가 직접 주고, 아래 경로는 JEUS(atms01)로 넘겨야 한다.
모두 JEUS 루트(`/` = atms-web)로 가고, 다른 모듈로의 분배는 atms-web 게이트웨이가 한다 → **모듈 컨텍스트(`/journal` 등)는 WebtoB 에 등록하지 않는다.**

| 경로 | 이유 |
|---|---|
| `/api/` | 화면이 호출하는 REST API |
| `/swagger-ui/`, `/v3/api-docs` | API 문서 (확인용) |
| `/actuator/` | 상태 확인 |
| `/atms/`, `/atmr/`, `/atmf/` | ATM 에이전트 고정 경로 (실기기 연동 시) |

`/api/` 하나로 화면 API 와 batch·file·journal·IF 경로(`/api/batch`, `/api/file`, `/api/agent`, `/api/if/...`)가 모두 포함된다.

먼저 현재 설정을 확인한다:
```bash
cd /WEBTOB/webtobATMS/config
cp http.m http.m.bak-$(date +%Y%m%d%H%M%S)
grep -n -i -E "^\*|DOCROOT|SVRTYPE|SVGNAME|URI|JSV|ErrorDocument|PORT" http.m
```

이미 JEUS 연동(`SVRTYPE = JSV` 서버그룹)이 있다면 `*URI` 에 경로만 추가한다. 예시 (이름은 기존 설정에 맞출 것):
```
*URI
uri_api       Uri = "/api/",          Svrtype = JSV, SvrName = <기존 JSV 서버명>
uri_swagger   Uri = "/swagger-ui/",   Svrtype = JSV, SvrName = <기존 JSV 서버명>
uri_apidocs   Uri = "/v3/api-docs",   Svrtype = JSV, SvrName = <기존 JSV 서버명>
uri_actuator  Uri = "/actuator/",     Svrtype = JSV, SvrName = <기존 JSV 서버명>
```
적용 (WebtoB 담당 절차가 있으면 그것을 우선):
```bash
wscfl -i http.m        # 설정 컴파일 (오류 없으면 진행)
wsdown -i              # 중지
wsboot                 # 기동
```
JEUS 쪽에도 WebtoB 커넥터(등록 ID·포트)가 맞게 설정돼 있어야 한다 (STEP 1-4 의 `webtob` 검색 결과). 없거나 다르면 JEUS/WebtoB 담당 확인.

### 7-5. 확인

- 브라우저: `http://nbatmsltwb01:9000/` (또는 `http://<WebtoB서버IP>:9000/`) → **로그인 화면이 나오면 성공**
- 브라우저 개발자도구(F12) → Network → 로그인 시도 시 `/api/auth/login` 요청이 JEUS 까지 가는지 확인 (DB 없어서 실패 응답은 정상, **404 가 아니어야 함**)
- `http://<WebtoB>:9000/swagger-ui.html` → Swagger 화면

### 7-6. 알아둘 점 — 화면 새로고침 404

React 는 `BrowserRouter` 를 쓴다. 메뉴 이동은 되지만, 하위 화면(예: `/atm/list`)에서 **F5(새로고침)** 하면 WebtoB 가 해당 파일을 찾다가 404 가 난다.
- 해결: 존재하지 않는 경로를 `index.html` 로 돌려주는 설정 필요
- 단, 은행 기준(404 등은 `error/error.html` 로 처리)과 충돌하므로 **WebtoB 담당·보안 기준과 협의** 후 적용 (API 경로 `/api/` 는 제외해야 함)

---

## 8. JEUS 가 Jakarta(Spring Boot 3)를 지원하지 않을 때

| 대안 | 내용 | 비고 |
|---|---|---|
| A. JEUS 상위 버전 | Jakarta EE 9 이상을 지원하는 JEUS 버전으로 교체/추가 설치 | TmaxSoft·인프라 협의, 라이선스 |
| **B. 내장 톰캣 + WebtoB 프록시** | 6개 모듈을 `bin/direct.sh`(JDK 직접 실행, 8080~8085) 또는 컨테이너로 띄우고(`-Pjeus` 없는 `bootJar`), WebtoB 가 `/api/` 등을 `nbatmsltws01:8080` 으로 전달 (WebtoB Reverse Proxy). 모듈 분배는 기존 방식(포트) | 소스 변경 없음. 운영 표준(JEUS)과 다르므로 승인 필요. WebtoB → 서버 8080 방화벽 |
| C. Spring Boot 2.7 로 낮춤 | `javax` 로 전환 | 전 모듈 대규모 수정, 2.7 은 지원 종료 → 비권장 |

STEP 6 결과(로그 일부)를 공유하면 판정과 다음 단계를 정리한다.
