# 현업 실제 값 반영 — 서버·소스에서 직접 고치는 곳 (명령 모음)

작성일: 2026-11-03 · 대상: ws01(`nbatmsltws01`, atmswas) · Jenkins · 내부 PC 소스(내부 Gitea `NH-ATMS/nh-atms`) · wb01(WebtoB 블록)

> 배포킷은 **가정값**(서버 `atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01`, 포트 8080~8085, 리스너 16.88.122.113, 폴더 `catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex`, JNDI `dsATMS01`, registration-id = 서버 이름)으로 완성돼 있다. 현업(WAS·WebtoB 담당, 인프라) 회신이 이 값과 **다를 때만** 아래 해당 절을 한다. 같으면 할 일 없음.
>
> 규칙: 명령은 한 줄씩. `sudo`·`su`·`ssh` 금지 (HIWARE). 고치기 전에 백업, 고친 뒤 `bash -n`·`status` 로 확인. 비밀번호·키가 사진에 찍히지 않게.

## 한눈에 보기

| # | 현업 값 | 고칠 곳 | 반영 |
|---|---|---|---|
| A | 서버별 재기동 메뉴 번호 (반드시 받음) | ws01 `~/deploy/deploy-war.env` | 즉시 (`status` 로 확인) |
| B | 서버 이름이 다름 | ws01 `~/deploy/deploy-war.env` (`SERVER_*`) | 즉시 — 표시용 |
| C | HTTP 포트가 8080~8085 가 아님 | ws01 `~/deploy/deploy-war.env` (`PORT_*`) + 내부 PC `jeus-common.yml` (`jeus.url.*`, 에이전트 포트) | 소스 push → 6개 재배포 |
| D | 리스너 IP 가 16.88.122.113 이 아님 | ws01 `deploy-war.env` (`JEUS_HOST`) + 내부 PC `jeus-common.yml` (`jeus.host`) | 소스 push → 6개 재배포 |
| E | 앱 경로(docBase)를 다른 폴더로 등록 | 담당에 수정 요청 (권장) / ws01 `swap-deploy.sh` 폴더 표 | 즉시 |
| F | WebtoB registration-id·노드 이름·JSV 그룹이 다름 | wb01 `~/deploy/kit/deploy/webtob/http.m.atms-jsv.txt` → WebtoB 담당 전달 | WebtoB 재기동 (담당) |
| G | 데이터소스(JNDI) 이름이 `dsATMS01` 이 아님 | 내부 PC `jeus-common.yml` (`jndi-name`) | 소스 push → 6개 재배포 |
| H | 앱 로그 폴더 경로가 다름 | 내부 PC `jeus-common.yml` (`log-dir`) | 소스 push → 6개 재배포 |
| I | L4 VIP | 내부 PC `jeus-common.yml` (`jeus.agent-entry.host`) | 소스 push → 6개 재배포 |
| — | (값과 무관, 첫 배포 전에 꼭) 필드 암호화 키·화면 라이브러리 캐시 | ws01 `/home/atmswas/keys`, Jenkins npm 캐시 | 0-2·0-3 |

---

## 0. 반입 직후 (가정값 그대로, 현업 값과 무관)

### 0-1. 배포킷 반입 후 기본 점검 (ws01, atmswas)

내부 PC 에서 배포킷 zip 을 풀어 `bin`, `conf`, `deploy`, `docs`, `README.md` 를 `/home/atmswas/atms/` 에 덮어쓴다 (FileZilla 바이너리). wb01 은 `deploy` 폴더를 `/home/atmswas/deploy/kit/` 에.

```bash
cd ~/atms
find bin deploy -name "*.sh" -exec sed -i 's/\r$//' {} +
find bin deploy -name "*.sh" -exec chmod 700 {} +
for f in bin/*.sh deploy/*/*.sh; do bash -n "$f" || echo "FAIL $f"; done
grep -c -e ajou01 -e STOP_MENU_web deploy/jeus/deploy-war.sh
sed -i 's/\r$//' deploy/gitea/gitea-backup.service
grep -c -- "-f - >" deploy/gitea/gitea-backup.service
```
- `bash -n` 출력 없음, `grep -c` 두 줄 모두 1 이상이면 새 판
- 2026-11-03 판 zip 에는 옛 `gitea-backup.service` 가 들어 있다 → 내부 PC 에서 수정본(INFRA_MANUAL T65)으로 바꿔 올린다. 마지막 줄이 `0` 이면 수정본만 다시 올린다 (실제 적용본 `~/.config/systemd/user/` 는 덮어쓰기와 무관)

이어서 [INFRA_MANUAL.md](INFRA_MANUAL.md) 3-1 표의 A 단계: 폴더 이름 정리(10-2), `deploy-war.env` 새로 만들기(10-3), Jenkins 백엔드 잡 교체(9-7). 새 서버가 뜨기 전에는 백엔드 `build-only` 만.

### 0-2. 화면 비밀번호 필드 암호화 키 (ws01, atmswas — atms01 첫 배포 전)

2026-11-03 소스부터 atms-web 이 이 키 파일을 읽는다. 없으면 atms01(atms-web)이 기동하지 못한다 ([FIELD_ENCRYPTION_DESIGN.md](FIELD_ENCRYPTION_DESIGN.md) 4·5장).
```bash
mkdir -p /home/atmswas/keys
chgrp jeus /home/atmswas/keys
chmod 2750 /home/atmswas/keys
cd /home/atmswas/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out field-k202611.key.pem
openssl pkey -in field-k202611.key.pem -pubout -out field-k202611.pub.pem
chgrp jeus field-k202611.key.pem field-k202611.pub.pem
chmod 640 field-k202611.key.pem field-k202611.pub.pem
ls -l /home/atmswas/keys
namei -l /home/atmswas/keys/field-k202611.key.pem
```
- 정상: 두 파일 `-rw-r----- atmswas jeus`, `namei` 의 모든 단계에 jeus 그룹이 들어갈 수 있음
- 개인키(`.key.pem`) 내용은 화면에 띄우거나 사진 찍지 않는다. Gitea·Jenkins·PC 로 옮기지 않는다
- 경로·이름을 바꾸면 atms-web `application-jeus.yml` 의 `crypto.field.*-key-path`·`kid` 도 같이 바꿔야 한다 (그대로 쓰는 것을 권장)

### 0-3. 화면 라이브러리(node-forge) Jenkins 캐시 (ws01, atmswas — 화면 빌드 전)

화면 소스에 `node-forge@1.4.0`·`@types/node-forge@1.3.14` 가 추가됐다. Jenkins npm 캐시에 없으면 화면 빌드가 `ENOTCACHED` 로 실패한다 (INFRA_MANUAL T20). 반입한 tgz 2개를 `~/atms/inbox/` 에 올린 뒤:
```bash
ls -l ~/atms/inbox/node-forge-1.4.0.tgz ~/atms/inbox/types-node-forge-1.3.14.tgz
podman unshare mkdir -p ~/atms/infra/jenkins/cache/npm/_add
podman unshare cp ~/atms/inbox/node-forge-1.4.0.tgz ~/atms/inbox/types-node-forge-1.3.14.tgz ~/atms/infra/jenkins/cache/npm/_add/
podman unshare chown -R 1000:1000 ~/atms/infra/jenkins/cache/npm/_add
podman exec atms-jenkins npm cache add /cache/npm/_add/node-forge-1.4.0.tgz --cache /cache/npm
podman exec atms-jenkins npm cache add /cache/npm/_add/types-node-forge-1.3.14.tgz --cache /cache/npm
podman unshare rm -rf ~/atms/infra/jenkins/cache/npm/_add
```
- `cache/npm` 은 컨테이너 1000 소유라 `podman unshare` 를 붙인다 (그냥 `mkdir`·`cp` 는 `Permission denied`)
- 확인: 화면 잡 `MODE=build-only` 가 `ENOTCACHED` 없이 성공

---

## 1. 먼저 직접 조회 (현업 회신과 맞춰 보기, ws01 atmswas, 읽기만)

```bash
D=/JEUS/jeusATMS/domains/ATMS/config/domain.xml
grep -A3 '<server>' $D | grep -o '<name>[^<]*'
grep -n -E '<listen-port>|<listen-address>' $D
grep -n -E 'registration-id' $D
grep -n -E '/home/atmswas|context-path' $D
grep -n -i -A12 'dsATMS01' $D | grep -i -E 'dsATMS01|export|url|server-name|port-number|database-name|user|class|target|min|max'
grep -n -E 'boot|down' /JEUS/jeusATMS/jeusMENU/jeusmenu.c
printf '0\n' | /JEUS/jeusATMS/jeusMENU/jeusRstart
ss -ltn | grep -E ':(808[0-5]|21001|8766|8736) '
ls -ld /WASlogs/jeusATMSlogs/nbatmsltws01/app
```
→ 결과로 아래 A~H 중 어느 절이 필요한지 정한다. 데이터소스 결과에 `password` 줄이 나오면 사진에서 가린다.

---

## A. 재기동 메뉴 번호 (ws01, atmswas)

예: WAS 담당 회신 "atms01 down 11 / boot 12, ajou01 down 13 / boot 14 …"

① 번호가 맞는 서버를 가리키는지 확인 (`<번호>` 자리에 숫자):
```bash
grep -n -A3 'case <번호>' /JEUS/jeusATMS/jeusMENU/jeusmenu.c
printf '0\n' | /JEUS/jeusATMS/jeusMENU/jeusRstart
```
- `case 11:` 아래에 `down atms01`(또는 그 서버 이름)이 보여야 한다. 3·7·8 이면 넣지 않는다 (스크립트도 거부)
- 메뉴 화면(둘째 줄)에 새 메뉴가 보이는지 — 소스(`jeusmenu.c`)만 고치고 실행 파일을 다시 안 만들었으면 화면에 없다 → 담당 확인

② 기입 (백업 후, 키마다 지우고 다시 넣음 → 줄이 있든 없든 동작):
```bash
cp -p ~/deploy/deploy-war.env ~/deploy/deploy-war.env.bak-$(date +%Y%m%d%H%M)
sed -i -E '/^(STOP|BOOT)_MENU_(web|journal|batch|file|ifin|ifex)=/d' ~/deploy/deploy-war.env
echo 'STOP_MENU_web=<atms01 down>' >> ~/deploy/deploy-war.env
echo 'BOOT_MENU_web=<atms01 boot>' >> ~/deploy/deploy-war.env
echo 'STOP_MENU_journal=<ajou01 down>' >> ~/deploy/deploy-war.env
echo 'BOOT_MENU_journal=<ajou01 boot>' >> ~/deploy/deploy-war.env
echo 'STOP_MENU_batch=<abat01 down>' >> ~/deploy/deploy-war.env
echo 'BOOT_MENU_batch=<abat01 boot>' >> ~/deploy/deploy-war.env
echo 'STOP_MENU_file=<file01 down>' >> ~/deploy/deploy-war.env
echo 'BOOT_MENU_file=<file01 boot>' >> ~/deploy/deploy-war.env
echo 'STOP_MENU_ifin=<ifin01 down>' >> ~/deploy/deploy-war.env
echo 'BOOT_MENU_ifin=<ifin01 boot>' >> ~/deploy/deploy-war.env
echo 'STOP_MENU_ifex=<ifex01 down>' >> ~/deploy/deploy-war.env
echo 'BOOT_MENU_ifex=<ifex01 boot>' >> ~/deploy/deploy-war.env
```
- `<…>` 를 숫자로 바꿔 친다 (예: `echo 'STOP_MENU_web=11' >> ~/deploy/deploy-war.env`). `=` 앞뒤 공백 없음

③ 확인:
```bash
bash -n ~/deploy/deploy-war.env && echo OK
grep MENU ~/deploy/deploy-war.env
~/atms/deploy/jeus/deploy-war.sh status
```
- `status` 각 줄이 `재기동 메뉴 <down>/<boot>` (미설정이 없어야 함)
- 예전 `JEUS_STOP_MENU=…` 줄이 남아 있으면 지운다: `sed -i '/^JEUS_/d' ~/deploy/deploy-war.env`

④ 시험 (WAS 담당과 시간 약속, 영향이 작은 서버부터):
```bash
~/atms/deploy/jeus/deploy-war.sh restart atms-if-external
```
- 정상: `정지 확인: atms-if-external` → `기동 확인: atms-if-external` → `재기동 완료`. 앱 배포 전이면 기동 확인이 실패(404)할 수 있음 — 그때는 `HEALTH_SKIP` 없이 첫 배포 후 다시 시험

## B. 서버 이름이 다름 (ws01, atmswas — 표시용, 동작 영향 없음)

```bash
cp -p ~/deploy/deploy-war.env ~/deploy/deploy-war.env.bak-$(date +%Y%m%d%H%M)
sed -i '/^SERVER_/d' ~/deploy/deploy-war.env
echo 'SERVER_web=<실제 이름>' >> ~/deploy/deploy-war.env
echo 'SERVER_journal=<실제 이름>' >> ~/deploy/deploy-war.env
echo 'SERVER_batch=<실제 이름>' >> ~/deploy/deploy-war.env
echo 'SERVER_file=<실제 이름>' >> ~/deploy/deploy-war.env
echo 'SERVER_ifin=<실제 이름>' >> ~/deploy/deploy-war.env
echo 'SERVER_ifex=<실제 이름>' >> ~/deploy/deploy-war.env
~/atms/deploy/jeus/deploy-war.sh status
```
- registration-id 도 서버 이름을 따랐다면 F 절도 한다

## C. HTTP 포트가 다름

① ws01 (atmswas) — 바뀐 모듈만:
```bash
cp -p ~/deploy/deploy-war.env ~/deploy/deploy-war.env.bak-$(date +%Y%m%d%H%M)
echo 'PORT_web=<포트>' >> ~/deploy/deploy-war.env
echo 'PORT_journal=<포트>' >> ~/deploy/deploy-war.env
~/atms/deploy/jeus/deploy-war.sh status
```
(키: `web` `journal` `batch` `file` `ifin` `ifex`. 같은 키를 두 번 넣으면 마지막 줄이 이긴다 — 고칠 때는 `sed -i '/^PORT_web=/d' ~/deploy/deploy-war.env` 후 다시 `echo`)

② 내부 PC 소스 `atms-common\src\main\resources\jeus-common.yml`:
- `jeus.url.<모듈>: http://${jeus.host}:<포트>` 의 포트
- journal·file 포트가 바뀌었으면 `jeus.agent-entry.journal-port`·`file-port` 도 (에이전트에게 알려 주는 포트)
- 같은 내용을 반영 가이드(`NH-ATMS_LOCAL_APPLY_GUIDE.md`) 9단계 사본에도 적어 둔다 (다음 반입 때 저장소에 반영 요청)

③ push → Jenkins `nh-atms-backend` `MODE=build-deploy`, `MODULE=all` (`jeus-common.yml` 은 6개 WAR 에 다 들어감)

④ 방화벽·L4: PC → 113 직접 확인 포트, ATM망 → L4 포트(journal·file)가 바뀌면 신청서도 고친다

## D. 리스너 IP 가 다름 (예: 0.0.0.0 이 아니라 다른 서비스 IP)

- `0.0.0.0` 으로 열었다면 고칠 것 없음 (16.88.122.113 으로도 받음)
- 다른 IP 하나에만 열었다면:
```bash
echo 'JEUS_HOST=<그 IP>' >> ~/deploy/deploy-war.env
~/atms/deploy/jeus/deploy-war.sh status
```
- 내부 PC `jeus-common.yml` 의 `jeus.host: <그 IP>` → push → `build-deploy`, `MODULE=all` (모듈 간 호출 주소, file01 21001 bind 주소)
- `127.0.0.1` 에만 열었다면 고치지 말고 담당에 `16.88.122.113`(또는 0.0.0.0)으로 요청 — 모듈 간 호출이 서로 닿지 않는다 (INFRA_MANUAL T61)

## E. 앱 경로(docBase)를 다른 폴더로 등록

- 권장: 담당에 `/home/atmswas/catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex` 로 맞춰 달라고 요청 (문서·스크립트가 모두 이 이름)
- 담당 경로를 그대로 써야 한다면 ws01 `swap-deploy.sh` 의 폴더 표를 바꾼다 (예: journal 을 `/home/atmswas/journal` 로 등록한 경우):
```bash
cp -p ~/atms/deploy/jeus/swap-deploy.sh ~/atms/deploy/jeus/swap-deploy.sh.bak-$(date +%Y%m%d)
sed -i 's#APP_HOME}/cajou"#APP_HOME}/journal"#' ~/atms/deploy/jeus/swap-deploy.sh
grep -n 'APP_HOME}/' ~/atms/deploy/jeus/swap-deploy.sh
bash -n ~/atms/deploy/jeus/swap-deploy.sh && echo OK
mv ~/cajou ~/journal
```
- 폴더는 `/home/atmswas` 바로 아래여야 한다 (`catms` 안 금지 — `/` 앱의 정적 경로로 노출)
- 이 경우 다음 배포킷 반입 때 덮어써지므로 저장소 반영도 요청한다

## F. WebtoB registration-id·노드 이름·JSV 그룹 (wb01, atmswas → WebtoB 담당)

`http.m` 은 atmswas 가 읽을 수 없다. 우리가 할 일은 담당에게 줄 블록 파일을 회신 값으로 맞추는 것뿐이다.
```bash
cd ~/deploy/kit/deploy/webtob
cp -p http.m.atms-jsv.txt http.m.atms-jsv.txt.bak-$(date +%Y%m%d)
sed -i 's/\batms01\b/<web id>/g' http.m.atms-jsv.txt
sed -i 's/\bajou01\b/<journal id>/g' http.m.atms-jsv.txt
sed -i 's/\babat01\b/<batch id>/g' http.m.atms-jsv.txt
sed -i 's/\bfile01\b/<file id>/g' http.m.atms-jsv.txt
sed -i 's/\bifin01\b/<ifin id>/g' http.m.atms-jsv.txt
sed -i 's/\bifex01\b/<ifex id>/g' http.m.atms-jsv.txt
grep -n -E 'SvgName|SvrName' http.m.atms-jsv.txt
```
- 바뀌는 곳: `*SERVER` 6줄의 이름, `*URI` 13줄의 `SvrName`
- 노드 이름이 `nbatmsltwb01` 이 아니면: `sed -i 's/nbatmsltwb01/<노드 이름>/g' http.m.atms-jsv.txt`
- 기존 JSV 서버그룹을 쓰기로 했으면: `sed -i 's/atms_jsvg/<기존 그룹 이름>/g' http.m.atms-jsv.txt` 후 `*SVRGROUP` 줄은 담당이 추가하지 않는다
- 이 파일을 FileZilla 로 PC 에 받아 WebtoB 담당에게 전달 → 담당 적용 후 확인 (INFRA_MANUAL 11-3):
```bash
ss -tn | grep ':19000'
for u in /v3/api-docs /api/ /api/journal/ /api/batch/ /api/file/ /api/if/internal/ /api/if/external/; do echo "$u $(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:9000$u)"; done
```

## G. 데이터소스(JNDI) 이름이 다름 (내부 PC 소스)

`atms-common\src\main\resources\jeus-common.yml`:
```yaml
spring:
  datasource:
    jndi-name: <실제 export-name>
```
→ push → `build-deploy`, `MODULE=all` → ws01 에서 확인:
```bash
grep -h -i -E "NameNotFound|jndi|dsATMS|Started Atms" /WASlogs/jeusATMSlogs/nbatmsltws01/app/*.log | tail -20
curl -s http://16.88.122.113:8080/actuator/health
```
- `{"status":"UP"}` 이면 DB 연결 정상. `NameNotFoundException` 은 이름·대상 서버 문제 (T63), `ORA-17056` 은 JEUS 라이브러리 orai18n (T22), Tibero 관련 오류는 드라이버 설정 (T64) → WAS 담당

## H. 앱 로그 폴더 경로가 다름 (내부 PC 소스)

- 담당이 `/WASlogs/jeusATMSlogs/nbatmsltws01/app` 을 만들지 않고 다른 곳을 주면 `jeus-common.yml` 의 `jeus.log-dir: <경로>` → push → `MODULE=all`
- 그 폴더는 jeusatms 가 쓸 수 있어야 한다. 확인: `ls -ld <경로>`

## I. L4 VIP (내부 PC 소스)

`jeus-common.yml` 의 `jeus.agent-entry.host: <VIP>` → push → `MODULE=all` → 단말 개국 시험에서 응답의 `atms.host`·`atmf.host` 가 VIP 인지 확인. 헬스체크는 8081·8083 `GET /v3/api-docs`(200) 또는 TCP, 21001 은 TCP (`/api/agent/ping` 은 소스에 없음, T49).

---

## 되돌리기

| 고친 것 | 되돌리기 |
|---|---|
| `deploy-war.env` | `ls -t ~/deploy/deploy-war.env.bak-*` 중 최근 것을 `cp -p <백업> ~/deploy/deploy-war.env` |
| `swap-deploy.sh` | `cp -p ~/atms/deploy/jeus/swap-deploy.sh.bak-<날짜> ~/atms/deploy/jeus/swap-deploy.sh` |
| `http.m.atms-jsv.txt` | `cp -p …bak-<날짜> http.m.atms-jsv.txt` |
| 소스(`jeus-common.yml`) | PC 에서 `git revert <커밋>` → push → 재배포 |

## 다음 배포킷 반입 때

서버에서 직접 고친 `~/deploy/deploy-war.env` 는 배포킷 밖이라 덮어써지지 않는다. `swap-deploy.sh`·`http.m.atms-jsv.txt` 를 고쳤다면 반입본이 덮어쓰므로, 고친 값을 저장소에도 반영해 달라고 요청한다 (실제 값 표: [INFRA_MANUAL.md](INFRA_MANUAL.md) 1-2).
