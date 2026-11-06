# [요청서] NH-ATMS JEUS 매니지드 서버 구성 — WAS 담당용

작성일: 2026-10-27 · 대상: nbatmsltws01 (16.88.122.113) JEUS 9.1 · WebtoB nbatmsltwb01 (16.88.122.112)
근거·상세: [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md)

---

## 1. 요청서 (1장)

```
제목: [요청] NH-ATMS JEUS 매니지드 서버 구성 (서버 6개 생성 · 기존 atms01 정리 · WebtoB 커넥터 · 재기동 메뉴)

1. 목적
   NH-ATMS 백엔드(Spring Boot 3.5 WAR 6개)를 모듈별 매니지드 서버로 분리
   (같은 도메인, 서버마다 앱 1개, 컨텍스트 "/")

2. 요청 사항
   ① 매니지드 서버 6개 생성 (아래 이름·포트·폴더, 명명규칙 2026-10-27 확정)
   ② 기존 nbatmsltws01_atms01(8766, 앱 atms) 은 새 atms01 로 대체 — 정리(중지·삭제) 시점 협의
   ③ 6개 서버 모두 WebtoB(nbatmsltwb01:19000) 커넥터 등록
   ④ 서버별 재기동(boot/down) jeusRstart 메뉴 추가
   ⑤ 데이터소스 dsATMS01 (Oracle, 16.88.250.91:1531/NBATMSTP, ATMS_APP) 을 서버 6개 대상으로 — 가이드 2-8

3. 구성
   서버                   포트  앱 id          배포 폴더(exploded)              컨텍스트
   atms01                 8080  atms           /home/atmswas/catms              /
   ajou01   8081  atms-journal   /home/atmswas/cajou      /
   abat01     8082  atms-batch     /home/atmswas/cabat        /
   file01      8083  atms-file      /home/atmswas/cfile         /
   ifin01      8084  atms-ifin      /home/atmswas/cifin         /
   ifex01      8085  atms-ifex      /home/atmswas/cifex         /
   - ws01 에서 8080~8085 미사용 확인 완료 (2026-10-26)
   - 배포 폴더 6개는 이미 생성·배포됨 (atmswas:jeus 2750)

4. 회신 요청
   - 서버 이름(변경 시) · 서버별 WebtoB 커넥터 registration-id
   - 서버별 jeusRstart 메뉴 번호 (boot / down)
   - 적용 일정

5. 참고
   - 2호기(nbatmsltws02) 도 같은 구성이 필요한지 회신 부탁드립니다
   - 상세 설정값·확인 방법은 첨부 가이드 참고
```

---

## 2. 담당자 가이드

### 2-1. 서버 공통 설정 (기존 `nbatmsltws01_atms01` 설정을 참고)

| 항목 | 값 |
|---|---|
| 리스너 | 서버당 **포트 1개**, base 리스너 = HTTP 리스너 (지금 atms01 의 8766 과 같은 방식) |
| 리스너 주소 | `16.88.122.113` (또는 0.0.0.0) — **127.0.0.1 만 열면 안 됨** (모듈끼리 `http://16.88.122.113:<포트>` 로 호출) |
| JVM 힙 | 서버당 512m~768m 수준 (2코어·공용 서버. DAS 1G, Jenkins 2G, Gitea 768m 와 함께 사용) |
| JDK | 기존과 같음 (OpenJDK 21) |
| NodeManager | 기존 NodeManager 에 등록 |
| 로그 | JEUS 서버 로그는 담당 규칙대로. 앱 로그는 `/WASlogs/jeusATMSlogs/nbatmsltws01/app/` (jeusatms 쓰기 가능, 6개 공용) |
| JVM 옵션 | `-Dspring.profiles.active` 는 **넣지 않음** (WAR 안 web.xml 이 지정) |

### 2-2. 애플리케이션 등록 (서버마다 1개, 기존 `atms` 와 같은 값)

| 항목 | 값 |
|---|---|
| type | WAR (exploded 폴더 경로로 등록, 복사 없이) |
| path | 위 표의 배포 폴더 |
| context-path | `/` |
| target | 해당 서버 하나 (기존 atms 는 `cluster_atms` — 유지 여부는 담당 판단) |
| classloading | ISOLATED |
| security-domain | SYSTEM_DOMAIN |
| 주의 | 배포는 폴더를 **통째로 교체** (`cajou` → 백업, 새 폴더로 교체). 경로는 고정 |

### 2-3. 기존 nbatmsltws01_atms01 정리
- 새 `atms01`(8080, `/home/atmswas/catms`) 이 기존 앱 `atms` 를 대신한다 → 같은 폴더를 두 서버가 함께 쓰지 않도록 기존 서버는 중지·삭제 (시점 협의)
- 기존 jeusRstart 메뉴 5(boot)·6(down) 은 `nbatmsltws01_atms01` 용 → 새 서버 메뉴로 바꾸거나 새로 추가 (2-5)
- 기존 WebtoB 커넥터 `nbatmsltws01_atms01-nbatmsltwb01_webatms` 도 새 이름으로 교체

### 2-4. WebtoB 커넥터 (서버마다 1개)

| 서버 | 커넥터 (예) | 대상 | registration-id (예) |
|---|---|---|---|
| atms01 | `atms01-nbatmsltwb01_webatms` | 16.88.122.112:19000 | `atms01` |
| ajou01 | `ajou01-nbatmsltwb01_webatms` | 〃 | `ajou01` |
| abat01 | `abat01-nbatmsltwb01_webatms` | 〃 | `abat01` |
| file01 | `file01-nbatmsltwb01_webatms` | 〃 | `file01` |
| ifin01 | `ifin01-nbatmsltwb01_webatms` | 〃 | `ifin01` |
| ifex01 | `ifex01-nbatmsltwb01_webatms` | 〃 | `ifex01` |

- registration-id 는 WebtoB `http.m` 의 `*SERVER` 이름과 같아야 연결됨 → WebtoB 설정 블록: 배포킷 `deploy/webtob/http.m.atms-jsv.txt`
- ws01 → wb01:19000 TCP 는 열려 있음 (2026-10-15 확인)

### 2-5. jeusRstart 메뉴 추가 (`/JEUS/jeusATMS/jeusMENU/jeusmenu.c`)
- 지금: 5 = `boot nbatmsltws01_atms01`, 6 = `down nbatmsltws01_atms01`
- 요청: 새 서버 6개의 boot / down 메뉴 (기존 5·6 과 같은 방식, `jeusadmin … 'boot|down <서버>'`)
- 배포 자동화(Jenkins)가 `printf '<번호>\n' | jeusRstart` 로 **한 서버씩** 호출 → 다른 서버는 멈추지 않음
- 3(java 전부 kill)·7·8(WebtoB) 은 자동화에서 쓰지 않음 (스크립트가 거부)
- 회신 예:

| 서버 | down | boot |
|---|---|---|
| atms01 | ? | ? |
| ajou01 | ? | ? |
| abat01 | ? | ? |
| file01 | ? | ? |
| ifin01 | ? | ? |
| ifex01 | ? | ? |

### 2-6. 적용 후 확인 (atmswas 도 가능)
```bash
ss -ltn | grep -E ':808[0-5] '
for p in 8080 8081 8082 8083 8084 8085; do echo "$p $(curl -s -o /dev/null -w '%{http_code}' http://16.88.122.113:$p/v3/api-docs)"; done
```
- 6개 포트 모두 LISTEN, `/v3/api-docs` 200 (DB 미연결이면 atms-batch 는 기동 실패일 수 있음)
- wb01: `ss -tn | grep 19000` → 16.88.122.113 과 ESTAB (WebtoB `http.m` 적용 후. 서버당 연결 수는 커넥터 설정에 따라 여러 줄일 수 있어, 6개 모두인지는 3-7 의 경로별 응답으로 확인)

### 2-6-1. ATM 에이전트 직접 연결 (확정 구성도 2026-10-27)
- ATM·STM·공과금수납기 → 내부망 L4 → ws01 **ajou01:8081**(REST 저널), **file01:8083**(REST 파일), **file01:21001**(TCP 소켓, 앱이 직접 bind)
- 지금 요청 구성은 서버마다 base 리스너 = HTTP 리스너(포트 1개)라, 8081·8083 을 ATM 망에 열면 **JEUS base 리스너(내부·관리용)도 같이 노출**됨 → base 포트를 따로 둘지 판단 부탁 (분리 시 HTTP 8080~8085 는 그대로)
- L4 VIP·헬스체크(8081·8083 은 HTTP `GET /v3/api-docs` 200 또는 TCP, 21001 은 TCP)·ATM망 방화벽은 별도 협의

### 2-7. 함께 확인 부탁 (선행)
- JEUS 9.1 Jakarta Servlet(`jakarta.servlet`) 지원 여부 — 현재 atms 앱이 8766 에서 모든 경로 404 (Spring 미기동)
- atms01 `stderr.log` 의 `[WebT]:webtCPInfo is null` / NullPointerException 매초 반복
- 데이터소스: 앱은 2026-11-02 부터 JNDI `dsATMS01` 사용 → 2-8 (기존 `iddsATMS01` 은 Tibero 드라이버로 설정돼 있었음)

### 2-8. 데이터소스 `dsATMS01` (JNDI, 2026-11-02 — 앱이 JNDI 로 전환)

앱은 이제 DB 를 직접 접속하지 않고 JEUS 데이터소스를 JNDI 이름 **`dsATMS01`** 로 찾는다 (`spring.datasource.jndi-name`). DB 주소·계정·비밀번호는 JEUS 에만 둔다.

| 항목 | 값 |
|---|---|
| DBMS | nbatmsltdb01_NBATMSTP (Oracle, 문자셋 KO16MSWIN949) |
| 주소 | **16.88.250.91 : 1531**, 서비스 `NBATMSTP` (`jdbc:oracle:thin:@//16.88.250.91:1531/NBATMSTP`) |
| 계정 | `ATMS_APP` |
| JNDI (export-name) | **`dsATMS01`** — 앱은 `java:comp/env/dsATMS01` → `dsATMS01` 순서로 찾음 |
| 대상(target) | **ws01 서버 6개 모두** (`atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01`) — 하나라도 빠지면 그 서버의 앱이 기동 실패 |
| 드라이버 | **Oracle** (`oracle.jdbc.OracleDriver` 또는 `oracle.jdbc.pool.OracleDataSource`). 2026-10-15 에 본 `iddsATMS01` 은 Tibero 드라이버로 설정돼 있었음 → 이 데이터소스를 쓰려면 Oracle 로 |
| JEUS 라이브러리 | Oracle JDBC(ojdbc) + **`orai18n.jar`** — 은행 DB 문자셋(KO16MSWIN949) 때문에 없으면 조회 시 `ORA-17056` (앱 WAR 안의 것은 JNDI 풀에는 쓰이지 않음) |
| 풀 크기 (서버마다) | 예: web·journal 최소 2 / 최대 10, batch·file·ifin·ifex 최소 1 / 최대 5 (예전 앱 설정과 같은 수준, 합계 최대 40) — 담당 기준 |
| 확인 | JEUS 관리 화면·`jeusadmin` 의 데이터소스 연결 시험(test connection) 성공 |

회신 부탁: 실제 export-name(다르면 앱 설정 1줄을 그 이름으로 바꿈), 대상 서버, 연결 시험 결과.

---

## 3. WebtoB `http.m` 수정 가이드 (nbatmsltwb01, WebtoB 담당)

대상: `/WEBTOB/webtobATMS/config/http.m` (webatms) · 붙여 넣을 원본: 배포킷 `deploy/webtob/http.m.atms-jsv.txt`
원칙: **기존 항목은 지우거나 바꾸지 않고, 없는 것만 추가**. DOCROOT 는 그대로.

### 3-1. 백업
```bash
cd /WEBTOB/webtobATMS/config
cp -p http.m http.m.bak-$(date +%Y%m%d%H%M%S)
grep -n -E "^\*|DOCROOT|JSVPORT|SVRTYPE|SvrType|SVGNAME|SvgName|Uri" http.m
```

### 3-2. `*NODE` — 확인만 (없는 항목만 추가)
```
*NODE
nbatmsltwb01    WEBTOBDIR = "/WEBTOB/webtobATMS",
                DOCROOT   = "/home/atmswas/docs",
                PORT      = "9000",
                JSVPORT   = 19000
                (그 밖의 기존 항목은 그대로)
```
| 항목 | 값 | 비고 |
|---|---|---|
| 노드 이름 | `nbatmsltwb01` | hostname 과 같아야 함 (기존 값) |
| DOCROOT | `/home/atmswas/docs` | **변경 없음** — 화면 배포 스크립트가 이 폴더를 교체 (2026-10-21 확인한 실제 DocBase) |
| PORT | `9000` | 기존 |
| JSVPORT | `19000` | JEUS 커넥터 6개가 접속하는 포트 (이미 LISTEN 중 → 있을 가능성 높음) |

### 3-3. `*SVRGROUP` — JSV 그룹 1개
```
*SVRGROUP
atms_jsvg       NodeName = "nbatmsltwb01", SvrType = JSV
```
- 이미 `SvrType = JSV` 그룹이 있으면 추가하지 않고 그 이름을 아래 `SvgName` 에 쓴다

### 3-4. `*SERVER` — 서버 6개 (이름 = JEUS 커넥터 registration-id)
```
*SERVER
atms01                  SvgName = atms_jsvg, MinProc = 2, MaxProc = 10
ajou01    SvgName = atms_jsvg, MinProc = 2, MaxProc = 10
abat01      SvgName = atms_jsvg, MinProc = 1, MaxProc = 5
file01       SvgName = atms_jsvg, MinProc = 2, MaxProc = 10
ifin01       SvgName = atms_jsvg, MinProc = 1, MaxProc = 5
ifex01       SvgName = atms_jsvg, MinProc = 1, MaxProc = 5
```
- 이름이 registration-id 와 다르면 연결되지 않는다 (2-4 의 회신값으로). MinProc·MaxProc 은 예시

### 3-5. `*URI` — 경로별 분배 (구체적인 경로를 위에)
```
*URI
uri_journal     Uri = "/api/journal/",     Svrtype = JSV, SvrName = ajou01
uri_agent       Uri = "/api/agent/",       Svrtype = JSV, SvrName = ajou01
uri_batch       Uri = "/api/batch/",       Svrtype = JSV, SvrName = abat01
uri_file        Uri = "/api/file/",        Svrtype = JSV, SvrName = file01
uri_atmf        Uri = "/atmf/",            Svrtype = JSV, SvrName = file01
uri_ifin        Uri = "/api/if/internal/", Svrtype = JSV, SvrName = ifin01
uri_ifex        Uri = "/api/if/external/", Svrtype = JSV, SvrName = ifex01
uri_api         Uri = "/api/",             Svrtype = JSV, SvrName = atms01
uri_atms        Uri = "/atms/",            Svrtype = JSV, SvrName = atms01
uri_atmr        Uri = "/atmr/",            Svrtype = JSV, SvrName = atms01
uri_swagger     Uri = "/swagger-ui/",      Svrtype = JSV, SvrName = atms01
uri_apidocs     Uri = "/v3/api-docs",      Svrtype = JSV, SvrName = atms01
uri_actuator    Uri = "/actuator/",        Svrtype = JSV, SvrName = atms01
```
- 그 밖의 경로(`/`, `/index.html`, `/assets/…`)는 지금처럼 DOCROOT 정적 파일
- 기존 `*URI`·오류 페이지(`/error/error.html`) 설정은 유지. URI 매칭이 등록 순서인지 최장 일치인지에 따라 순서 확인

### 3-6. 적용 (webatms)
```bash
wscfl -i http.m
wsdown -i
wsboot
```
- `wscfl` 오류가 나면 표시된 줄을 고친 뒤 다시. 재기동 중 9000 이 잠시 중단됨 → 시간 공지

### 3-7. 확인
```bash
ss -tn | grep 19000
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9000/
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9000/v3/api-docs
curl -s -o /dev/null -w "%{http_code} %{redirect_url}\n" http://127.0.0.1:9000/api/
```
| 결과 | 판정 |
|---|---|
| 19000 에 16.88.122.113 ESTAB (서버당 여러 줄일 수 있음) | JEUS 쪽 연결됨 — 서버별 연결은 경로별 응답이 `503` 이 아닌지로 확인 ([INFRA_MANUAL.md](INFRA_MANUAL.md) 11-3) |
| `/` 200 | 화면 정상 |
| `/v3/api-docs` 200 | atms-web 연결 (JEUS 쪽 기동 후) |
| `/api/` 가 `302 /error/error.html` 이 아님 | URI 적용됨 |

### 3-8. 되돌리기
```bash
cd /WEBTOB/webtobATMS/config
cp -p http.m.bak-<시각> http.m
wscfl -i http.m
wsdown -i
wsboot
```

