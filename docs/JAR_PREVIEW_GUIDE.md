# 방화벽·JEUS 전 임시 기동 (jar 6개 + WebtoB 화면 연동)

작성일: 2026-10-21 · 서버 `nbatmsltws01`(113) · 계정 `atmswas`

> **임시 경로**다. 목표 런타임은 JEUS WAR ([JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md)). PC → 113 방화벽과 atms01 기동을 기다리는 동안, 서버에 이미 있는 JDK 로 6개 모듈을 jar 로 띄우고 WebtoB(112:9000, PC 접속 가능) 화면과 연결해 본다.
>
> - DB 없음 (사용자 결정 2026-10-21) → 기동·화면·게이트웨이까지만 확인. 로그인·조회는 DB 연결 실패로 동작하지 않는다
> - **atms-batch 는 DB 가 없으면 기동 실패**한다 (Spring Batch). 나머지 5개는 DB 없이 뜬다
> - 프로파일은 `local` (jar 안의 `application-local.yml`: JWT 시크릿 포함, 게이트웨이가 `localhost:8081~8085` 로 전달) — 이 서버에서는 jar 6개가 같은 서버에서 8080~8085 로 뜨므로 그대로 맞는다

## 1. PC: jar 6개 빌드

```powershell
cd D:\NH-ATMS\Backend\nh-atms-api
.\gradlew.bat bootJar --offline
dir atms-*\build\libs\*.jar
```
- Temurin 을 못 찾는다는 오류면 `-Porg.gradle.java.installations.paths=<temurin-21 경로> -Porg.gradle.java.installations.auto-download=false` 를 붙인다
- `-plain.jar` 는 올리지 않는다 (실행 불가)

FileZilla(바이너리) 로 6개를 `/home/atmswas/atms/release/jar/` 에 올린다.

## 2. 서버: 실행 준비 (atmswas)

```bash
ls -d /usr/lib/jvm/java-21-openjdk-*
J=$(ls -d /usr/lib/jvm/java-21-openjdk-*/bin/java | head -1)
$J -version
ss -ltn | grep -E ":808[0-5]"
mkdir -p ~/atms/run/logs
ls ~/atms/release/jar
```
- `$J -version` 이 `21.0.x` 면 사용 가능. `ss` 결과는 비어 있어야 한다 (8080~8085 비어 있음)

## 3. 서버: 6개 기동

```bash
cd ~/atms/run
nohup $J -Xmx512m -jar ~/atms/release/jar/atms-web-*.jar --server.port=8080 > logs/atms-web.log 2>&1 &
nohup $J -Xmx384m -jar ~/atms/release/jar/atms-journal-*.jar --server.port=8081 > logs/atms-journal.log 2>&1 &
nohup $J -Xmx384m -jar ~/atms/release/jar/atms-batch-*.jar --server.port=8082 > logs/atms-batch.log 2>&1 &
nohup $J -Xmx384m -jar ~/atms/release/jar/atms-file-*.jar --server.port=8083 > logs/atms-file.log 2>&1 &
nohup $J -Xmx256m -jar ~/atms/release/jar/atms-if-internal-*.jar --server.port=8084 > logs/atms-if-internal.log 2>&1 &
nohup $J -Xmx256m -jar ~/atms/release/jar/atms-if-external-*.jar --server.port=8085 > logs/atms-if-external.log 2>&1 &
```
- 새 HIWARE 창에서 `J=...` 를 다시 정의해야 한다 (변수는 창마다)
- 로그아웃해도 `nohup` 이라 유지된다 (linger=yes). 서버 재부팅 시에는 다시 실행
- 중지: `pkill -u atmswas -f "release/jar/atms-"` (atmswas 자신의 jar 만 종료)

## 4. 서버: 확인 (1~2분 뒤)

```bash
ss -ltn | grep -E ":808[0-5]"
grep -h "Started Atms" logs/*.log
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/v3/api-docs
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/file/
tail -20 logs/atms-batch.log
```

| 확인 | 정상 |
|---|---|
| `ss` | 8080·8081·8083·8084·8085 (8082 batch 는 DB 없으면 없음) |
| `Started Atms...` | 5줄 (batch 제외) |
| `/v3/api-docs` | `200` |
| `/api/file/` | `401` 등 **502 가 아니면** 게이트웨이 → atms-file 전달 정상 |
| batch 로그 | DB 연결 오류로 종료 — 예상된 결과 |

`/actuator/health` 는 DB 가 없어 `DOWN`(503) 이 정상이므로 확인용으로 쓰지 않는다.

## 5. WebtoB(112) → jar(113:8080) 연결

**5-1. 112 에서 113:8080 이 닿는지 (atmswas, 112 서버)**
```bash
curl -s -m 5 -o /dev/null -w "%{http_code}\n" http://16.88.122.113:8080/v3/api-docs
```
`200` 이면 서버 간 경로 열림. `000` 이면 112 → 113:8080 방화벽 확인 필요.

**5-2. WebtoB 담당 요청** (atmswas 는 `http.m` 을 읽을 수 없음)
```
[요청] nbatmsltwb01 WebtoB 임시 리버스 프록시 설정 (JEUS 연동 전 테스트용)
- 대상: http://16.88.122.113:8080 (atms-web, 내장 톰캣)
- 경로: /api/  /atms/  /atmr/  /atmf/  /swagger-ui/  /v3/api-docs
- 그 외(/) 는 기존대로 DocBase /home/atmswas/docs
- JEUS atms01 기동 후에는 원래 계획대로 JSV(19000) 연동으로 되돌림
```
- 화면 파일(dist)은 112 의 `/home/atmswas/docs` 에 둔다 ([JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) 7-2·7-3)
- 화면은 `/api` 상대경로로 호출하므로 WebtoB 가 같은 주소에서 전달하면 CORS 설정 변경이 필요 없다

**5-3. PC 확인**
브라우저 `http://16.88.122.112:9000` → 로그인 화면이 뜨면 화면 연동 성공. 로그인 시도는 DB 가 없어 실패(오류 메시지)하는 것이 정상. 개발자도구(F12) → 네트워크에서 `/api/auth/login` 이 **404/502 가 아니라 백엔드 응답(500·오류 JSON 등)** 이면 WebtoB → jar 연결까지 확인된 것.
