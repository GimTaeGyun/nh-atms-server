# 개발서버 JEUS 정보 수집 (읽기 전용)

작성일: 2026-10-12 · 대상: `nbatmsltws01` JEUS 9.1 (`/JEUS/jeusATMS`) · 관련: [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 1, [DEV_SERVER_INFRA.md](DEV_SERVER_INFRA.md)

JEUS 버전·도메인·서버 목록·데이터소스·배포된 애플리케이션을 **설정을 바꾸지 않고** 조회하는 절차다. 결과는 맨 아래 7장 표에 적어 회신한다.

규칙
- 한 줄씩 그대로 입력한다. `sudo`/`su` 는 쓰지 않는다 (HIWARE 세션 잠김).
- 아래에 없는 명령은 실행하지 않는다. 특히 jeusadmin 의 `start-*`, `stop-*`, `deploy*`, `undeploy*`, `redeploy*`, `add-*`, `remove-*`, `modify-*`, `set-*`, `create-*`, `delete-*`, `apply-*`, `local-shutdown`, `shutdown` 은 **금지** (설정·상태 변경).
- 비밀번호는 명령줄(`-p`)에 쓰지 않는다. 생략하면 jeusadmin 이 직접 물어본다 (history·`ps` 에 남지 않게).

| 방법 | 필요한 것 | 장 |
|---|---|---|
| A. 계정 없이 확인 (atmswas 로도 가능) | 없음 | 1~3장 |
| B. jeusadmin 조회 | JEUS 관리자 ID/비밀번호, jeusadmin 실행 권한 (보통 `jeusatms` 계정) | 4~5장 |
| C. WAS 담당에게 요청 | 6장 문구 전달 | 6장 |

A 만으로 대부분 알 수 있고, B 는 A 에서 못 읽은 항목(데이터소스 상태, 배포 상태)을 채운다.

---

## 1. JEUS 설치·프로세스 (A, atmswas 가능)

```bash
hostname
id
echo $JEUS_HOME
ls -l /JEUS/jeusATMS
ls -l /JEUS/jeusATMS/bin
ls -l /JEUS/jeusATMS/domains
ps -ef | grep -v grep | grep -i jeus | cut -c1-300
```
기록: 도메인 폴더 이름, 실행 중인 프로세스(관리 서버 `-DadminServer`, NodeManager, `atms01` 등 업무 서버), 각 프로세스의 java 경로.

`ps` 줄이 잘려 서버 이름이 안 보이면:
```bash
ps -eo user,pid,args | grep -v grep | grep -i jeus | grep -o "\-Djeus.server.name=[^ ]*\|\-Djeus.domain.name=[^ ]*\|\-DadminServer\|\-Xmx[^ ]*" | sort | uniq
```

## 2. 버전 (A)

```bash
/JEUS/jeusATMS/bin/jeusadmin -version
ls /WASlogs/jeusATMSlogs/nbatmsltws01
find /WASlogs/jeusATMSlogs -name "JeusServer*.log" | head
```
`-version` 이 권한 오류면 로그 배너로 확인 (경로는 위 `find` 결과 중 하나):
```bash
grep -m3 "JEUS" <JeusServer.log 경로>
```
기록: `JEUS 9 (Fix#n)` 형태의 전체 버전 문자열.

## 3. 도메인 설정 파일 읽기 (A, 읽기 권한이 있을 때)

```bash
find /JEUS/jeusATMS/domains -maxdepth 3 -name domain.xml
```
나온 경로를 `D` 에 넣고 필요한 부분만 본다 (파일 수정 금지, `vi` 대신 `grep`/`less` 사용):
```bash
D=<위 domain.xml 경로>
grep -n "<domain-name>\|<name>" $D | head -40
grep -n -A3 "<server>" $D | grep "<name>" | head
grep -n "listen-port\|<port>\|<listener>" $D | head -40
grep -n -A12 "<data-source>\|<database>" $D | grep -i "<data-source-id>\|<export-name>\|<data-source-class-name>\|<server-name>\|<port-number>\|<database-name>\|<user>\|<min>\|<max>" | head -60
grep -n -A8 "<deployed-application>\|<application>" $D | grep -i "<id>\|<path>\|<type>\|<context-path>\|<target>" | head -60
grep -n -i "webtob\|wjp\|<registration-id>" $D | head -20
grep -n -i "jvm-option\|Xmx\|MaxMetaspace\|spring.profiles" $D | head -20
```
- `Permission denied` 이면 B 또는 C 로 간다.
- 데이터소스 `<password>` 줄은 **기록·사진 촬영하지 않는다** (위 grep 에서 일부러 뺐음).

---

## 4. jeusadmin 접속 (B)

관리 서버 접속 주소는 1장 `ps` 결과의 관리 서버 줄 또는 domain.xml 의 관리 서버 리스너 포트에서 확인한다 (JEUS 기본값 9736).

```bash
/JEUS/jeusATMS/bin/jeusadmin -host localhost:9736 -u administrator
```
- 비밀번호를 물으면 입력. 관리자 ID 가 `administrator` 가 아니면 WAS 담당에게 확인.
- 접속되면 프롬프트가 `[DAS]<도메인명>>` 형태로 바뀐다. 끝낼 때는 `exit`.

## 5. jeusadmin 조회 명령 (B, 모두 읽기 전용)

프롬프트에서 한 줄씩 입력한다. 명령 이름이 버전에 따라 다를 수 있으니, "없는 명령" 이 나오면 `help` 목록에서 비슷한 `list-*`/`*-info`/`show-*` 이름을 찾는다 (`help <명령>` 으로 설명 확인 후 실행).

| 알고 싶은 것 | 명령 | 기록할 것 |
|---|---|---|
| 버전 | `version` | 전체 버전 문자열 |
| 명령 목록 | `help` | (참고용) |
| 서버 목록·상태 | `server-info` | 서버 이름, 상태(RUNNING/SHUTDOWN), 노드, 포트 |
| 노드·클러스터 | `list-nodes` · `list-clusters` | 이름 |
| 서버 리스너 | `list-server-listeners -server atms01` | 리스너 이름, 포트 (HTTP 리스너 → `jeus.base-url`) |
| 웹 엔진 설정 | `show-web-engine-configuration -server atms01` | 웹 커넥터(WebtoB) 등록 ID·포트 |
| 데이터소스 목록 | `list-data-sources` | 데이터소스 ID, JNDI 이름, URL/DB 이름 |
| 데이터소스 상세 | `list-data-sources -id <데이터소스ID>` | 드라이버, 사용자, 풀 min/max (**비밀번호 제외**) |
| 커넥션 풀 상태 | `connection-pool-info -server atms01` | 활성/유휴 수 |
| 배포된 앱 목록 | `list-applications` | 앱 ID, 타입(WAR/EAR), 상태, 대상 서버 |
| 앱 상세 | `application-info -id <앱ID>` | 경로(`/home/atmswas/catms` 등), context-path |
| JVM 옵션 | `show-server-configuration -server atms01` 또는 `server-info -server atms01 -detail` | `-Xmx`, Metaspace, `-Dspring.profiles.active` 유무 |

`atms01` 이 없거나 이름이 다르면 `server-info` 에 나온 이름으로 바꿔 입력한다.

---

## 6. WAS 담당에게 요청할 때 (C)

> nbatmsltws01 JEUS(jeusATMS) 의 아래 정보를 조회 결과(텍스트 또는 화면)로 공유 부탁드립니다. 설정 변경은 필요 없습니다.
> 1) JEUS 전체 버전(Fix 번호 포함) 2) 도메인명, 서버 목록과 상태(atms01 생성·기동 여부) 3) atms01 HTTP 리스너 포트, WebtoB 연동 등록 ID·포트 4) 데이터소스 목록(ID, JNDI, DB 접속 URL, 계정명 — 비밀번호 제외) 5) 배포된 애플리케이션 목록(ID, 경로, context-path, 대상 서버) — 특히 /home/atmswas/catms 등록 여부 6) atms01 JVM 옵션(-Xmx, MaxMetaspaceSize) 7) 웹 컨테이너가 jakarta.servlet(Servlet 5.0+) 를 지원하는지

---

## 7. 결과 기록표 (회신용)

| # | 항목 | 값 | 확인 방법(장) |
|---|---|---|---|
| 1 | JEUS 전체 버전 | | 2 / 5 |
| 2 | 도메인명 | | 1 / 3 |
| 3 | 관리 서버 접속 포트 | | 4 |
| 4 | 서버 목록·상태 (`atms01` 존재/기동) | | 1 / 5 |
| 5 | `atms01` HTTP 리스너 포트 | | 3 / 5 |
| 6 | WebtoB 연동 등록 ID·포트 | | 3 / 5 |
| 7 | 데이터소스 (ID / JNDI / URL / 계정) | | 3 / 5 |
| 8 | 배포된 앱 (ID / 경로 / context-path / 대상) | | 3 / 5 |
| 9 | `/home/atmswas/catms` 등록 여부 | | 3 / 5 |
| 10 | `atms01` JVM 옵션 (`-Xmx`, Metaspace, profiles) | | 1 / 3 / 5 |
| 11 | 실행 java 경로·버전 | | 1 |
| 12 | 서블릿 세대 (JAKARTA / JAVAX) | | 배포 가이드 STEP 1-3 |

확인된 값은 [DEV_SERVER_INFRA.md](DEV_SERVER_INFRA.md) 의 `[미확인]` 표시를 `[확인 날짜]` 로 바꾸고, 5·8·9 는 `jeus-common.yml` 의 `jeus.base-url` 과 앱 등록 요청([JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 7장)에 반영한다.
