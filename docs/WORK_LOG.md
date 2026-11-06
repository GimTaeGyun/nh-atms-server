# 개발서버 작업 이력

작성일: 2026-10-16 · 대상: `nbatmsltws01` (WAS, 16.88.122.113) / `nbatmsltwb01` (WebtoB, 16.88.122.112) · 작업 계정 `atmswas`

> 날짜순으로 **무엇을 했고, 무엇을 확인했고, 무엇을 정했는지** 모은다. 절차는 링크한 문서를 따른다. 새 작업을 하면 맨 아래 날짜에 이어 적고, 끝난 남은 일은 지운다.

---

## 2026-10-08 ~ 10-12 · 준비·반입

| 구분 | 내용 | 문서 |
|---|---|---|
| 결정 | 저장소는 `main` 브랜치 하나만 사용 | — |
| 결정 | 서버 백엔드는 기설치 **JEUS 9.1** 에 WAR 로 배포, 프론트는 WebtoB DocBase | [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) |
| 결정 | Git = Gitea (HTTP 3000, SSH 끔), CI = Jenkins (8090), rootless Podman | [GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) |
| 작업 | Jenkins 버전·플러그인 고정표 작성 → 현업이 직접 tar 제작 (2.580.1, 플러그인 87개, tar 약 400MB / 적재 후 1.21GB) | [REQUIRED_FILES.md](REQUIRED_FILES.md) 2-3 |
| 작업 | 반입 제외 검토서 PDF (동료 전달용) | `review/반입항목_제외검토_20261008.pdf` |
| 결정 | 반입은 Gitea 이미지·Jenkins 이미지·배포킷 zip·체크섬 4종. JRE·nginx·compose 는 보류 | [REQUIRED_FILES.md](REQUIRED_FILES.md) |
| 작업 | 95MB 분할 압축으로 메일 반입 → **반입 완료** | — |
| 결정 | NH-ATMS 6개 모듈 모두 WAR, 같은 atms01 에 컨텍스트 경로로 분리 (`/`, `/journal`, `/batch`, `/file`, `/ifin`, `/ifex`), atms-web 게이트웨이, 설정은 WAR 안, DB 는 Spring Hikari | [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) |
| **결정 (10-12)** | **모든 서버 작업을 `atmswas` 로, 기준 경로 `/home/atmswas`. `/home/userATMS` 사용 안 함** | [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) |

## 2026-10-13 · 소스 보완

| 구분 | 내용 | 문서 |
|---|---|---|
| 작업 | HMAC 필터 컨텍스트 경로 대응, 게이트웨이 502 직접 응답, 로컬 게이트웨이, 배치 스케줄러 종료 | [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 4-7 |
| 작업 | 폐쇄망 PC 반영 가이드 (업체 원본 → 현재, 24개 파일) | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) |
| 검증 | Tomcat 10.1 대체 기동(WAR 6개 exploded, `swap-deploy.sh`), 로컬 `java -jar` — HMAC 결함은 수정 전 200(검증 건너뜀) → 후 401 재현 확인 | [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 6장 |
| 검토 | 모듈 1개만 배포: 파일 교체·롤백은 모듈별, `jeusRstart` 는 6개 전부 재기동, 앱 단위 재배포는 현업 확인 필요. `atms-common`·`jeus-common.yml` 변경 시 6개 모두 | [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 12-5 |

## 2026-10-14 · WAS 서버 초기 셋팅 (1~9단계)

| 구분 | 내용 |
|---|---|
| 확인 | atmswas `uid=50219 gid=50082(atms) groups=atms,2002(jeus)`, subuid `493216:65536` 등록됨, `Linger=no`, firewalld `inactive`, `/home` 공용 10G(여유 9.8G), 3000·8090 비어 있음 |
| 확인 | 서버 IPv4 4개 중 **16.88.122.113** (HIWARE 접속 대상)을 서비스 IP 로 사용. `hostname -I` 첫 값은 IPv6 `fe80::` 라 사용 금지 |
| 문제→해결 | `install-gitea.sh` 에 IPv6 가 들어가 실패 → IPv4 검사 추가, IP 재실행 시 갱신 |
| 문제→해결 | `crun: controller 'cpu' is not available` (rootless 에 cpu 미위임) → 위임된 컨트롤러만 `--cpus`/`--memory` 적용, 위임은 root 작업 2-9 |
| 문제→해결 | `--userns=keep-id` 로 `crun: readlink (빈 경로)` 기동 실패 (podman 5.8.2 / crun 1.27) → keep-id 제거, `podman unshare chown -R 1000:1000 <데이터 폴더>` 로 대체 (호스트에서는 subuid 소유로 보임, 지우거나 복사할 때 `podman unshare`) |
| 작업 | Gitea 기동·관리자 계정 생성, Jenkins 기동 (플러그인 87개) |

## 2026-10-14 ~ 15 · 게이트웨이·성능 검토 (소스 변경 없음)

| 구분 | 내용 | 문서 |
|---|---|---|
| 확인 | 폐쇄망 PC 반영 중 `ModuleGatewayProperties` 의 IntelliJ "Not registered…" 경고 → 17번 `ModuleGatewayConfig` 전까지 정상 (가이드 16번에 안내 추가) | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) |
| 검토 | 게이트웨이 없이도 WAR 6개는 기동, 실패하는 것은 컨텍스트 없는 고정 경로 분배뿐. 서버 1개 유지하며 게이트웨이를 없애는 안 = **WebtoB internal URL rewrite** (그때 `gateway.enabled: false` 한 줄) | [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 12-1·12-2 |
| 실측 | Tomcat(8766): 게이트웨이 경유 +2.6ms/건, 처리량 -25%, 50MB 다운로드 -32% | 12-3 |
| 재현 | 게이트웨이 요청은 스레드 2개 사용 → 같은 풀(4개)에서 동시 16건이면 **전부 타임아웃**(서버 멈춤). JEUS 는 WebtoB 커넥터 풀과 8766 풀이 나뉘는 구조라 위험 낮음(값 미확인). PC→8766 직접 부하 금지 | 12-4 |
| 확인 | TmaxSoft 문서상 **JEUS 9 = Jakarta EE 9 인증(Servlet 5.0)** → javax 전용 우려 낮아짐 (서버 실측은 STEP 1·6) | [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 1 |

## 2026-10-15 · root 작업일, WebtoB·JEUS 확인

| 구분 | 내용 | 문서 |
|---|---|---|
| 확인 | linger 가 꺼져 있어 **로그아웃하면 Gitea·Jenkins 가 같이 내려감** | — |
| 확인 | `jeusatms` = `uid=40119 gid=2002(jeus)` → 그룹 추가 불필요 | — |
| 확인 | root 권한은 **root 계정 직접 접속** 방식. atmswas 의 `sudo` 는 그날도 금지 (`sudo -l` 이 금지명령 1회로 집계) → 앞으로도 sudo 입력 금지 | [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2-10 |
| 작업 (root) | `loginctl enable-linger atmswas` → **`Linger=yes`** | 2-2 |
| 작업 (root) | cgroup cpu 위임 `delegate.conf` 작성 + `systemctl restart user@50219.service`. 첫 시도에서 `[Service]` 줄 누락 → 재작성 안내 | 2-9 |
| 확인 (root) | firewalld `not running` → 서버 자체 방화벽 작업 불필요 | 2-3 |
| 확인 | WebtoB 서버 `nbatmsltwb01`: IP 16.89.122.85 / **16.88.122.112** / 16.88.250.87 / 16.89.250.67, atmswas 접속 가능, WebtoB(webatms) 기동 중, **:9000** LISTEN (302 → `/error/error.html`), `config/` 는 webatms 700 이라 `http.m` 읽기 불가, `/home/atmswas/docs` 없음 | [DEV_SERVER_INFRA.md](DEV_SERVER_INFRA.md) 3장 |
| 확인 | WAS → WebtoB 9000·19000 **열림**. WAS 에서 `nbatmsltwb01` 이름 조회 안 됨 → IP 사용 | 7장 |
| 확인 | 개발 PC(10.248.170.171) → WebtoB 9000 **막힘** (ping 은 됨) → 네트워크 방화벽 필요 | 7장 |
| 확인 | JEUS `domain.xml`: 업무 서버 이름 **`nbatmsltws01_atms01`**, HTTP 포트 **8766** (base 리스너 공유), WebtoB 연동 포트 **19000** (wb01·wb02), DAS 8736, 데이터소스 `iddsATMS01`, JEUS 로그 `/JEUS/jeusATMS/logs/JeusSystem/nbatmsltws01_atms01/`, 같은 도메인에 2호기 `nbatmsltws02_atms01` | [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) 1-4 |
| 소스 | `jeus-common.yml` 의 `jeus.base-url` 기본값 `127.0.0.1:8088` → **`127.0.0.1:8766`** (폐쇄망 PC 는 이 한 줄만 수정) | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) |
| 작업 | Jenkins 재부팅 자동기동 방식 결정: `--restart=always` + `podman-restart.service` | [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) 9-2 |

## 2026-10-16 · 방화벽 신청

| 구분 | 내용 |
|---|---|
| 작업 | 은행 정책신청 엑셀 양식으로 **방화벽 신청 완료** |
| 출발지 | 개발 PC 4대: 10.248.170.169 / .171 / .177 / .205 (`IP주소(To)` 는 비움 — 범위 끝값 칸) |
| 목적지 | 16.88.122.112 TCP **9000** (WebtoB 화면) · 16.88.122.113 TCP **8766** (JEUS 직접), **3000** (Gitea), **8090** (Jenkins) · 기간 2026-10-15 ~ 2027-10-15 |
| 범위 | 개발 PC → 서버만. 서버 간 통신(WAS → WebtoB 19000 등)은 이미 열려 있어 제외. 2호기는 제외 |

---

## 2026-10-16 · CPU 제한 보류 결정

| 구분 | 내용 |
|---|---|
| 결정 | Gitea·Jenkins 의 **CPU 제한은 보류**, 메모리 제한만 유지. 나중에 다시 정함 |
| 이유·영향 | 2코어에 JEUS 공존이라 빌드 중 JEUS 가 느려질 수 있음 (서버 정지·메모리 문제는 없음). 상세 [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2-9 |
| 반영 | `install-gitea.sh` 는 `GITEA_CPUS` 를 지정할 때만 CPU 제한, Quadlet 기본값 `--memory=768m` 만, Jenkins 명령에서 `--cpus` 제외 |

## 2026-10-19 · `/home/atmswas` 하위가 안 보임 → 새 디스크가 덮어 마운트됨

| 구분 | 내용 |
|---|---|
| 발견 | atmswas 로 접속하니 `/home/atmswas` 아래가 비어 있음 (`.config`, `.local` 만) |
| 확인 | `df -h /home/atmswas` → **`/dev/mapper/vg_app-lv_home_atmswas` 10G (104M 사용) 가 `/home/atmswas` 에 별도 마운트**. `stat ~` Birth **2026-10-16 10:52:09** (= 새 파일시스템의 최상위 폴더 생성 시각, inode 128), Modify 2026-10-17 02:30 → **마운트 시각은 10-16 10:52 이후 ~ 10-17 02:30 이전으로 추정** (정확한 시각은 인프라 확인). 소유 `atmswas:jeus` 755 |
| 확인 | 기존 `/home`(`vg_os-lv_home` 10G) 사용량 **1.9G** → 10-14 에 올린 반입 파일·Podman 저장소·Gitea/Jenkins 데이터가 **새 디스크 아래에 가려져 남아 있는 것으로 판단** (삭제 아님) |
| 확인 | 서버 재부팅 없음 (`uptime -s` 2026-08-26), `Linger=yes`, subuid 유지, 계정 uid 50219 그대로. `delegate.conf` 는 `Delegate=...` 한 줄뿐 (`[Service]` 없음 — CPU 제한 보류 중이라 영향 없음) |
| 원인 | 우리가 요청한 `/home` 디스크 증설을 인프라가 **atmswas 전용 LV 를 기존 폴더 위에 마운트**하는 방식으로 처리하면서 기존 내용을 옮기지 않은 것으로 보임 (인프라 확인 필요). 우리 작업에는 홈 전체를 지우는 명령이 없음 |
| 결정 | 인프라에 **가려진 기존 내용을 새 디스크로 복사** 요청 (마운트 해제 없이 bind 마운트로 가능). 요청 문구·명령: [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2-11 |

## 2026-10-21 · 디스크 30GB 증설, 새로 구성 결정

| 구분 | 내용 |
|---|---|
| 확인 | `/home/atmswas` 용량 30GB 로 증설 완료 (인프라) |
| 결정 | 가려진 기존 데이터 복구는 진행하지 않음 → **새로 구성**. PC 에 남아 있는 반입 파일(10-14 본)과 기존 배포킷 zip + 보정 명령 사용 |
| 순서 | [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) 2→3→4→5단계 → "서버의 옛 배포킷 보정" → 7(이미지 적재) → 8(Gitea, Quadlet) → 9(Jenkins `--restart=always`, `--cpus` 없음). root 작업(linger)은 유지돼 다시 할 필요 없음. Gitea 관리자·`jenkins.env` 는 새로 만든다 |

## 2026-10-21 · 재구성 완료 (Gitea·Jenkins)

| 구분 | 내용 |
|---|---|
| 완료 | 폴더 → 업로드 → 무결성 → 배포킷 보정 → 이미지 적재 → Gitea (Quadlet, 백업 타이머) → Jenkins (`--restart=always`, `--memory=2g`, podman-restart enable) |
| 확인 | `podman ps` 둘 다 Up, `healthz` pass, `:8090/login` 200, `ss` 에 `*:3000`·`*:8090` LISTEN |
| 문제→해결 | 옛 `install-gitea.sh` 가 atmswas 권한으로 `data/backup` 을 만들다 `Permission denied` (먼저 `chown 1000` 한 탓) → `podman unshare mkdir -p .../data/backup` 후 chown·재실행 |
| 문제→해결 | Jenkins `Failed to bind port 8090 (Address already in use)` → 덮이기 전에 띄운 옛 Jenkins 프로세스(가려진 저장소라 `podman ps` 에 안 보임)가 포트를 잡고 있었음 → atmswas 프로세스 종료 후 재실행 |

## 2026-10-21 · 방화벽 "완료" 통보 후 검증

| 구분 | 내용 |
|---|---|
| 결과 | PC → 112:9000 **성공**. PC → 113:3000·8090 **실패** (약 10초 후 실패) |
| 검증 | 113 안에서 서버 IP 로 3000·8090 응답 정상 / 112 → 113:3000·8090 정상 / PC 테스트 중 113 의 `ss -tn` 에 PC 연결이 **전혀 안 보임** / PC IP·신청 정보 일치 |
| 판정 | 서버·컨테이너 정상. **PC → 113 구간의 네트워크 방화벽이 아직 적용되지 않음** (신청서 113 행 미적용·오기 가능성) → 신청 담당 재확인 필요 |

## 2026-10-21 · 방화벽 대기 중 jar 임시 기동 결정

| 구분 | 내용 |
|---|---|
| 결정 | PC → 113 방화벽·atms01 기동을 기다리는 동안 **서버 기존 JDK(OpenJDK 21) 로 6개 모듈 jar 를 `local` 프로파일, DB 없이** 띄우고 WebtoB(112:9000) 화면과 연결해 본다 (임시 경로) |
| 예상 | 5개 기동, atms-batch 는 DB 없어 실패. 로그인·조회는 DB 없어 실패 |
| 필요 | WebtoB 담당에 `/api/` 등 → 113:8080 임시 리버스 프록시 요청, 112 → 113:8080 통신 확인 |
| 절차 | [JAR_PREVIEW_GUIDE.md](JAR_PREVIEW_GUIDE.md) |

## 2026-10-21 · 화면(프론트) 빌드·배포 방식 결정

| 구분 | 내용 |
|---|---|
| 확인 | wb01 DocBase = `/home/atmswas/docs` (md5 대조), WebtoB 5.0.0.4, 동료가 화면(Vite 빌드) 배포·mock API 작성(실행 안 함). wb01 은 atmswas 만 사용 가능 (http.m 읽기 불가) |
| 확인 | **ws01 호스트·Jenkins 컨테이너 → wb01:22 둘 다 열림** |
| 결정 | 화면도 ws01 Gitea·Jenkins 로 빌드하고, **Jenkins 가 SSH 로 wb01 에 배포** (tar.gz 를 SSH 입력으로 보내 `deploy-front.sh` 가 검증·교체·확인, 실패 시 자동 되돌림). 키는 `authorized_keys` 에 `command=`·`from="16.88.122.113"`·`restrict` 로 고정. 같은 날 검토한 A안(wb01 타이머가 Gitea 에서 당겨오기)은 취소 → wb01 linger 불필요 |
| 산출물 | `deploy/webtob/deploy-front.sh`, `deploy/jenkins/pipelines/frontend.Jenkinsfile`, `swap-deploy.sh` tar.gz 지원 — 배포킷 재반입본에 포함 |
| 절차 | [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4장(지금 가능) → 5장(PC→113 방화벽 개방 후) |

## 2026-10-22 · JEUS 설치 폴더(`/JEUS/jeusATMS`) 없음 — WAS 담당 확인 필요

| 구분 | 내용 |
|---|---|
| 확인 | JEUS DAS·NodeManager 는 실행 중 (jeusatms, OpenJDK 21.0.11, **10-19 15:25 재기동**, DAS `-Xms/-Xmx1024m`). atms01 프로세스 없음, 8736 은 `16.88.122.113` 에만 LISTEN, 8766 응답 없음(`000`), 6600/6700/6800 그대로 |
| 이상 | `/JEUS` 는 마운트(`vg_app-lv_JEUS` 20G xfs)돼 있지만 **비어 있음** (`jeusatms:jeus` 755, 수정 시각 **10-19 17:50**). `/JEUS/jeusATMS` 없음, `/usr/local/jeusATMS -> /JEUS/jeusATMS` 링크가 끊김. 사용량 1.2G(10-19 오전) → **509M** |
| 판단 | 10-19 15:25 재기동 이후 17:50 에 설치 폴더가 지워지거나 다른 곳으로 옮겨진 것으로 보임. 남은 509M 은 실행 중인 DAS·NodeManager 가 열어 둔 지워진 파일로 추정 → **지금 프로세스가 내려가거나 서버가 재부팅되면 JEUS 가 다시 뜨지 못할 수 있음** |
| 추가 확인 | `/WASlogs` 도 **같은 시각(10-19 17:50)·같은 소유(`jeusatms:jeus` 755)로 비어 있음** (`/WASlogs/jeusATMSlogs/nbatmsltws01` 없음). `find / -maxdepth 4 -iname "*jeus*"` → `/JEUS`, udev 링크, `/tmp/hsperfdata_jeusatms` 뿐 — **다른 위치로 옮겨진 흔적 없음** |
| 판정 | JEUS 설치와 JEUS 로그 폴더가 같은 시각에 함께 비워짐 → 실수보다는 **JEUS 제거·재설치 준비 같은 의도된 작업**으로 보임 (추정). 실행 중인 DAS·NodeManager 는 그 전에 뜬 것 |
| 조치 | 우리는 아무것도 건드리지 않음. WAS 담당에 긴급 문의 (재설치 계획·일정, atms01 구성 유지 여부, 로그 경로) |

## 2026-10-22 · 현업 제시용 "/JEUS·/WASlogs 가 정상이었던 증거" 정리

| 날짜·시각 | 증거 | 자료 |
|---|---|---|
| 10-07 | JEUS DAS·NodeManager 가 jeusatms 로 실행 중, 설치 경로 `/JEUS/jeusATMS` | 서버 화면 사진 (워터마크 날짜) |
| 10-15 | atmswas 로 `/JEUS/jeusATMS/domains/*/config/domain.xml` 을 읽음: `nbatmsltws01_atms01` 8766, WebtoB 커넥터 19000, `iddsATMS01`, 로그 `/JEUS/jeusATMS/logs/JeusSystem/...` | `grep` 결과 사진 (10-15) |
| 10-19 오전 | `df -h`: `/JEUS` **1.2G 사용**, `/WASlogs` **538M 사용** | `df -h` 사진 (10-19) |
| 10-19 15:25 | DAS·NodeManager 재기동 — 실행 인자에 `/JEUS/jeusATMS/bin/startNodeManager`, `/JEUS/jeusATMS/lib/system/extens` → 이 시각에 설치 폴더 존재 | 10-22 `ps` 사진 (시작 시각 `lstart`) |
| 10-19 17:50 | `/JEUS`, `/WASlogs` 수정 시각 (둘 다 비어 있음) | 10-22 `ls -ld` 사진 |
| 10-22 | `/JEUS` 509M 사용·내용 없음, `/usr/local/jeusATMS -> /JEUS/jeusATMS`(3/13 생성) 끊김 | 10-22 `df`·`ls` 사진 |

저장소 커밋 기록(10-15 `4b412f6` domain.xml 값 기록, 10-19 `33e2481` 등)도 당시 확인 시점의 보조 증거다.

## 2026-10-22 · Jenkins 캐시 마운트 완료, 빌드 잡 2개 준비

| 구분 | 내용 |
|---|---|
| 완료 | offline-seed(npm·Gradle 8.14.4 캐시)를 Jenkins 에 마운트 (`/cache/npm`, `/cache/gradle`) |
| 결정 | 저장소 1개 + 웹훅 1개, 잡은 바뀐 폴더로 분리 (`Frontend/` → `nh-atms-frontend`, `Backend/` → `nh-atms-backend`, `PathRestriction`) |
| 결정 | 백엔드 잡은 WAR 빌드·보관만 (JEUS 부재로 배포 보류). 설정·산출물 확인은 방화벽 개방 후 PC 브라우저에서 |
| 추가 | `deploy/jenkins/pipelines/backend.Jenkinsfile`, [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) (설정 순서 1~10) |

## 2026-10-23 · JEUS 복구 확인, 배포킷 정리 (서버 실행 스크립트 추가)

| 구분 | 내용 |
|---|---|
| 확인 | (사용자) WAS 담당이 JEUS 를 복구, **atms01 까지 기동**. 경로·서버명·8766·앱 6개(catms*) 등록 여부는 다시 확인 필요 ([BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6-0) |
| 결정 | wb01 동료 구성(`data`, `mock`, `nodejs`, `tools`)은 지워도 됨 — `docs` 폴더는 DocBase 라 유지 ([FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4-0) |
| 결정 | 백엔드 잡에 배포 단계 추가: Jenkins 컨테이너 → ws01 호스트 SSH(배포 전용 키 `ws01-deploy-key`, `command=` 로 `deploy-war.sh` 고정) → `catms*` 교체. 기본 `MODE=build-only` (웹훅 빌드는 배포 안 함), 재기동은 WAS 담당 |
| 결정 | 배포킷은 **전체 zip 재반입** (GitHub `main` Download ZIP, `NH-ATMS/` 는 올리지 않음) → ws01 `~/atms` 덮어쓰기, wb01 `~/deploy/kit` ([DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) "배포킷 갱신") |
| 추가 | `deploy/jenkins/run-jenkins.sh` + `jenkins.env.example` (9단계 수동 명령 대체), `deploy/webtob/install-wb01.sh` (wb01 폴더·스크립트·키 등록), `deploy/jeus/deploy-war.sh` + `install-deploy-war.sh` (ws01 WAR 수신·교체·키 등록), `backend.Jenkinsfile` MODE·배포 단계 |
| 수정 | `swap-deploy.sh`: 첫 배포 때 새 폴더 그룹을 상위(`/home/atmswas` = jeus)에서 받고 2750 (atmswas 기본 그룹이 atms 라 JEUS 가 못 읽을 수 있었음) |

## 2026-10-23 · Jenkins 배포 키 확인

| 구분 | 내용 |
|---|---|
| 완료 | Credentials 3개(`gitea-jenkins-bot`, `wb01-deploy-key`, `ws01-deploy-key`), 백엔드 잡 `MODE=status` → `Finished: SUCCESS` (16:26) |
| 확인 | Jenkins 컨테이너 → `host.containers.internal:22` 접속이 ws01 sshd 에는 **`16.88.122.113` 에서 온 것으로 보임** → ws01 `authorized_keys` 를 `restrict,from="16.88.122.113",command=…/deploy-war.sh` 로 고정, 다시 status 정상 |

## 2026-10-23 · 웹훅 GET → POST

| 구분 | 내용 |
|---|---|
| 증상 | Gitea 웹훅(방법 GET) 테스트 전송 → Jenkins 응답 `414 URI Too Long` (Jetty) |
| 원인 | Gitea 는 GET 일 때 push 내용(JSON)을 URL 뒤에 붙여 보냄 → 주소가 너무 길어 거부 |
| 조치 | 웹훅 방법을 **POST**, 콘텐츠 유형 `application/json` 으로 변경 (대상 URL 은 그대로). 문서의 GET 표기 모두 수정 |

## 2026-10-23 · 웹훅 200, 백엔드 첫 자동 빌드 실패 (Gradle 캐시 미사용)

| 구분 | 내용 |
|---|---|
| 확인 | 웹훅 POST 로 바꾼 뒤 응답 200, push → `nh-atms-backend` #3 자동 시작 |
| 증상 | 콘솔 `GRADLE_USER_HOME=미설정` → `Downloading https://services.gradle.org/.../gradle-8.14.4-bin.zip` → `UnknownHostException` |
| 원인 | 실행 중인 Jenkins 컨테이너에 `GRADLE_USER_HOME` 환경변수가 없음 (jenkins.env 에 줄이 없거나 추가 전에 만든 컨테이너) |
| 이어서 | 경로 고정 후 #다음 빌드: `/cache/gradle/wrapper/dists/gradle-8.14.4-bin/…/gradle-8.14.4-bin.zip.lck (Permission denied)` → 캐시 파일이 컨테이너 jenkins(1000) 소유가 아님 → `podman unshare chown -R 1000:1000 ~/atms/infra/jenkins/cache`. `run-jenkins.sh` 는 맨 위 폴더만 보던 소유자 검사를 하위 파일까지로 수정 |
| 조치 | `backend.Jenkinsfile` environment 에 `GRADLE_USER_HOME = '/cache/gradle'` 고정 + 캐시 폴더가 없으면 원인 메시지로 멈춤. 잡의 Pipeline script 다시 붙여 넣기 |

## 2026-10-26 · JEUS 배포 점검 — 앱 1개만 등록, Spring 미기동

| 구분 | 내용 |
|---|---|
| 확인 | Jenkins 빌드 9 의 WAR 6개가 `catms*` 에 교체됨 (14:44). ws01→DB `16.88.250.91:1531` TCP open |
| 확인 | `domain.xml` `deployed-application`: `atms` 1개만 (WAR, `/home/atmswas/catms`, `target-cluster` `cluster_atms`, classloading ISOLATED, `context-path` `/`). journal·batch·file·ifin·ifex 미등록 |
| 증상 | `8766` 의 `/actuator/health`·`/v3/api-docs`·`/atms/...` 모두 JEUS 404 (`FileNotFoundException`) → Spring 미기동. `stderr.log` 는 `[WebT]:webtCPInfo is null`+NPE 매초 반복(은행 설정). `/WASlogs/.../atms01/app` 없음. atmswas 는 `lib/system` 읽기 불가 |
| 참고 | JEUS 데이터소스 `iddsATMS01` 은 Tibero 드라이버 설정(16.88.250.91:1531/NBATMSTP, atms_app). 앱은 JNDI 미사용, `jdbc:oracle:thin` 직접 접속(로컬 성공) 유지 — 사용자 결정 |
| 결정 | root 작업일 범위: ws01·wb01 전체, JEUS 설정은 읽기만 (ROOT_DAY 2-14). 앱 5개 등록은 JEUS 관리자 계정이 필요한 WAS 담당 작업 → 요청 (root 로 `domain.xml` 직접 수정하지 않음) |

## 2026-10-26 · JEUS 구성 변경 결정 — 매니지드 서버 6개

| 구분 | 내용 |
|---|---|
| 결정 | 도메인 1개 + **매니지드 서버 6개**(모듈당 JVM 1개), 포트 **8080~8085**(로컬과 동일), 컨텍스트 모두 `/`, **게이트웨이 제거**(WebtoB 가 경로별로 각 서버에 직접 분배). 2026-10-12 의 "atms01 하나 + 컨텍스트 6개 + 게이트웨이" 대체 |
| 범위 | 이번에는 문서·결정·WAS 담당 요청문. 소스·배포킷은 서버명·포트·registration-id·재기동 수단 회신 후 수정 |
| 문서 | [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md) (개념·고민 과정·변경 범위·요청문), [JEUS_MULTI_PORT_OPTIONS.md](JEUS_MULTI_PORT_OPTIONS.md) (비교) |

## 2026-10-27 · 매니지드 서버 6개 구성으로 배포킷·소스 설정 반영

| 구분 | 내용 |
|---|---|
| 확인 | ws01 에서 8080~8085 미사용 (사용자, 2026-10-26) |
| 결정 | 서버 이름: 기존 규칙 + 모듈 약어 (`nbatmsltws01_atms01`(web) / `_journal` / `_batch` / `_file` / `_ifin` / `_ifex`). 서버별 재기동은 jeusRstart 메뉴 번호를 `deploy-war.env` 로 (없으면 UNSTABLE), 반입은 새 구성 기준만 |
| 반영 | 소스 7개(`jeus-common.yml`, `application-jeus.yml` ×6), 배포 `deploy-war.sh`·`deploy-war.env.example`·`install-deploy-war.sh`·`swap-deploy.sh`, `backend.Jenkinsfile`, `deploy/webtob/http.m.atms-jsv.txt`, 문서 (LOCAL_APPLY_GUIDE 9~15, BACKEND_CICD_PLAN 6-9, DEV_SERVER_INFRA, ROOT_DAY, 결정 문서 10장) |
| 시험 | `deploy-war.sh` 스텁(jeusRstart·curl 흉내): 메뉴 있는 모듈 재기동·확인, 메뉴 없는 모듈 → 종료코드 4, 기동 실패 → 롤백·재기동·실패 종료, 메뉴 3 거부, 예전 `JEUS_STOP_MENU` 호환. `bash -n` 전체, YAML 파싱 7개. Jenkinsfile 은 괄호 균형만 (Groovy 파서 미사용) |
| 변경 | 앱 로그 폴더 `jeus.log-dir` → `/WASlogs/jeusATMSlogs/nbatmsltws01/app` (6개 서버 공용, 이전 `…/atms01/app`) |

## 2026-10-27 · 구성도 확정 (서버 이름·폴더·에이전트 연결)

| 구분 | 내용 |
|---|---|
| 확정 | 서버 `atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01`, 폴더 `catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex`, 포트 8080~8085 (base = HTTP), 기존 `nbatmsltws01_atms01` 대체 |
| 확정 | 단말(PC) → WebtoB 9000 → JSV 19000 → 서버 6개. ATM·STM·공과금수납기 → L4 → ws01 직접 (journal 8081, file 8083, TCP 21001, http) |
| 반영 | `jeus.agent-entry` (L4 VIP `CHANGE_ME`, 8081/8083, http), atms-file 21001 소켓 서버 켬, 문서 (결정 문서 11장, WAS 요청서 2-6-1, LOCAL_APPLY_GUIDE 개정 (3), CLAUDE.md) |
| 지적 | base 리스너 공유 상태로 8081·8083 을 ATM 망에 열면 JEUS base 리스너도 노출 → WAS 담당 판단으로 넘김 |

## 남은 일 (방화벽 개방 후 순서)

- [x] (2026-10-26) orai18n 반입: 인터넷 PC 에서 받은 Gradle `modules-2` 로 폐쇄망 PC·Jenkins 캐시 교체 ([NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 개정 2026-10-26) — 2026-10-30 사용자 확인: 완료
- [ ] (2026-10-30) 이후 남은 일은 [INFRA_MANUAL.md](INFRA_MANUAL.md) 3-1 표 (1호기 순서) 로 관리

> 방화벽이 열리기 전에도 할 수 있는 것: 0단계, 3단계, 4단계 1-3 판정, 5단계 PC 빌드 준비.

**0. 서버 컨테이너 정상화** (atmswas, 방화벽과 무관 — 개방 확인 전에 먼저)
- [x] 새로 구성 완료 (2026-10-21): Gitea·Jenkins 기동
- [x] 옛 배포킷 보정 → Gitea 재설치 (Quadlet) → Jenkins `--restart=always` 재생성 (`--cpus` 없이) (2026-10-21 재구성 때 함께 처리)
- [ ] 2026-10-23 배포킷 반입 → ws01 `~/atms` 덮어쓰기, wb01 `~/deploy/kit` ([DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) "배포킷 갱신")

**1. 개방 확인** (PC)
- [ ] `Test-NetConnection` 113:3000 · 113:8090 · 112:9000 → `True` (113:8766 은 atms01 기동 후)
- [ ] 브라우저: `http://16.88.122.113:3000` (Gitea), `:8090` (Jenkins), `http://16.88.122.112:9000` (은행 오류 페이지면 정상)
- [ ] 안 열린 포트는 신청 담당에 재확인, 결과를 이 문서와 [DEV_SERVER_INFRA.md](DEV_SERVER_INFRA.md) 7장에 기록

**2. Gitea·Jenkins 사용 시작** (PC 브라우저 + 서버)
- [ ] Gitea 관리자 첫 로그인·비밀번호 변경 → 조직·팀원 4명 계정·저장소 생성 ([GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) 3-2)
- [ ] 각자 토큰 발급 → PC 에서 NH-ATMS 소스 첫 push, 팀원 clone (같은 가이드 3-3, 4장). 이때 내부망 사본의 `jeus.base-url` 이 8766 인지 확인
- [ ] Jenkins 로그인 → `jenkins-bot` 계정·토큰으로 Gitea 연동, 빌드 잡 등록 (같은 가이드 3-2, [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) 9-4). 빌드 캐시(offline-seed) 반입 필요 여부 판단

**3. 서버 폴더 준비·WAS 회신** (atmswas)
- [ ] WAS 서버: `/home/atmswas` → `atmswas:jeus` 750, `catms*`(2750)·`data`(2770)·`deploy`(700) 생성
- [ ] WebtoB 서버: `/home/atmswas/docs` 생성 (2750)
- [ ] WAS 담당 회신 + 요청: atms01(`nbatmsltws01_atms01`) 기동, 앱 6개 컨텍스트 경로 등록, JVM 옵션, thread-pool·앱 단위 재배포 방법 ([SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 7-3, [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 12-6)

**4. 백엔드 WAR 배포** ([JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md))
- [ ] STEP 1-3 Jakarta 지원 판정 (미지원이면 8장 대안 검토)
- [ ] DB 호스트·포트 확인 (인프라/DBA) → 내부망 사본 `jeus-common.yml` 의 `CHANGE_ME` 기입 (GitHub 에는 올리지 않음)
- [ ] JEUS 복구 후 상태 재확인: 경로·`nbatmsltws01_atms01`·8766·앱 6개(catms*) 등록 ([BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6-0)
- [ ] (지금 가능, ws01) `install-deploy-war.sh` → 배포 키 생성·등록 → Jenkins 컨테이너 → 호스트 22 접속·known_hosts (같은 문서 6-1~6-3)
- [ ] (개방 후) Credentials `ws01-deploy-key`, 잡 갱신, `MODE=status` 로 `from=` 고정 (6-4~6-6)
- [ ] 첫 배포: `MODE=build-deploy` → WAS 담당 재기동 → STEP 6 확인 (6-7). 수동 경로는 STEP 2~5
- [ ] PC 에서 113:8766 접속 확인 (방화벽 확인 마무리)

**5. 프론트 배포** (WebtoB)
- [ ] (지금 가능) Jenkins SSH 배포 준비: SSH 도구·Gitea 주소·npm 캐시 확인, wb01 스크립트·교체 연습, 배포 키 생성·등록·시험, known_hosts ([FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4장 A-1~A-11)
- [ ] (개방 후) Jenkins Credentials 2개·잡 `nh-atms-frontend` 등록 → `MODE=status` → `MODE=build-deploy` (같은 문서 5장)
- [ ] (WebtoB 담당) `http.m` 의 `/api/` 등 → JEUS 연동 설정 확인·요청, WebtoB 버전·URL Rewrite 지원 여부 (STEP 7-4, [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 12-6)
- [ ] PC 브라우저 `http://16.88.122.112:9000` → 로그인 화면·API 호출 확인 (STEP 7-5)

**6. 그 밖의 요청·정리**
- [ ] (보류) CPU 제한 재검토 — 이유·영향·다시 할 때 순서는 [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md) 2-9
- [ ] (인프라) `/home` 디스크 증설, 서비스 IP(113·112) 확정, 2호기(ws02·wb02) 용도·IP
- [ ] 배포킷 zip 재반입 (2026-10-23 판) → 0단계 참고
- [ ] (보안 담당) 서버 간 SSH(ws01 Jenkins → wb01, 배포 전용 키) 사용 공유 — HIWARE 기록에 남지 않음 ([FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 3장)

## 2026-10-23 · JEUS 재기동 점검·파이프라인 재기동 추가

| 구분 | 내용 | 문서 |
|---|---|---|
| 확인 | `/JEUS/jeusATMS` 10-22 15:3x 재설치, `logs -> /WASlogs/jeusATMSlogs`. `jeusMENU/jeusRstart` = `root:sys -rwsr-xr-x` setuid ELF, 소스 `jeusmenu.c`(644) 읽기 가능: `setuid(0)` 후 메뉴 0~8, 계정 검사 없음. 1·2 servlet start/stop, **3 jeusatms java 전부 kill -9**, 4 jspwork 삭제, 5 jeusadmin `boot nbatmsltws01_atms01`, 6~8 미확인 | [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6-8 |
| 결정 | 재기동 가능 전제로 백엔드 파이프라인에 자동 재기동: `deploy-war.sh restart [--rollback]` (jeusRstart 정지 → 5 기동 → 모듈별 기동 확인 → 실패 시 교체 모듈 자동 롤백·재기동), Jenkinsfile `MODE=restart`·`RESTART`(기본 true). 반입 없이 기존 파일 수정 | [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6-8 |
| 확인 | ws01 HIWARE 가 `ssh -i … atmswas@127.0.0.1 status` 를 금지 명령으로 차단 (금지 입력 1회). `ssh-keygen` 은 허용. → atmswas 셸에서 ssh 시험 생략, 키 동작은 Jenkins `MODE=status` 로 확인 (BACKEND_CICD_PLAN 6-2). `install-deploy-war.sh <pub>` 로 키 등록 완료(from= 없음) | [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6-2 |
| 확인 | jeusRstart 메뉴 6 = `jeusadmin 'down nbatmsltws01_atms01'`, 7·8 = webatms wsboot/wsdown. `printf '0\n' \| jeusRstart` → 메뉴 출력 후 `Promgram Exit` (표준입력 방식 동작) → `JEUS_STOP_MENU=6`. Jenkins 컨테이너 → `host.containers.internal:22` OPEN (113·86 은 refused), known_hosts 지문 = ws01 호스트 키. `deploy-war.sh status` 는 6개 모두 000 (8766 무응답) | [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6-3·6-8 |
| 작업 | 첫 `deploy-war.sh restart` (JEUS_STOP_MENU=6): `down` 전송 → 메뉴 5 → `Successfully start the server [nbatmsltws01_atms01]. The server's state is RUNNING` → `ss` 로 8766·8736 LISTEN 확인. 기동 확인 대기 중 Ctrl+C (앱 미배포) | [BACKEND_CICD_PLAN.md](BACKEND_CICD_PLAN.md) 6-8 |
| 확인·수정 | 8766 이 **16.88.122.113 에만 LISTEN** (127.0.0.1 불가) → status 000·정지 확인 즉시 통과의 원인. `deploy-war.sh` `JEUS_URL` 기본값과 `jeus-common.yml` `jeus.base-url` 을 `http://16.88.122.113:8766` 으로 (서버는 `deploy-war.env` 에 `JEUS_URL` 한 줄) | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) |
| 남은 일 | 서버 `deploy-war.env` 에 `JEUS_URL=http://16.88.122.113:8766` 추가, 내부 Gitea 소스 `jeus-common.yml` base-url 수정, PC→113:8090 개방 후 Jenkins Credentials·잡 갱신·`MODE=status`·`from=` 고정, 첫 WAR 배포(`build-deploy`), 보안 담당 공유 | — |

## 2026-10-26 · 새 DB 로컬 연결

| 구분 | 내용 | 문서 |
|---|---|---|
| 확인 | 로컬 PC → 은행 Oracle(NBATMSTP) 접속 성공. 로그인 시 `ORA-17056 지원되지 않는 문자 집합 … KO16MSWIN949` | — |
| 소스 | `atms-common/build.gradle` 에 `runtimeOnly 'com.oracle.database.nls:orai18n'` (23.7.0.25.01, Boot BOM) — JAR·WAR 에 포함 확인 | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 개정 2026-10-26 |
| 남은 일 | 폐쇄망 PC·Jenkins Gradle 캐시(`modules-2`)에 orai18n 반입 | 같은 문서 |

## 2026-10-30 · JEUS 셋팅 진행 확인, 통합 매뉴얼, 에이전트 ping 결함

| 구분 | 내용 | 문서 |
|---|---|---|
| 확인 | (사용자, ws01 atmswas) `domain.xml` = `/JEUS/jeusATMS/domains/ATMS/config/domain.xml` → **도메인 이름 `ATMS`**, 수정 시각 **2026-10-30 16:26** (WAS 담당 작업 중으로 보임). 실행 중 서버는 `obj_name=nbatmsltws01_atms01` 하나, LISTEN 은 8766·8736 뿐 → 새 서버 6개는 아직 기동 전 | [INFRA_MANUAL.md](INFRA_MANUAL.md) 10-1 |
| 정정 | 새 서버 이름을 `grep -cw` 로 센 확인 명령은 잘못 — `-w` 는 `nbatmsltws01_ajou01` 처럼 접두어가 붙은 이름을 못 찾는다 (실제로 옛 `nbatmsltws01_atms01` 도 0 으로 나옴). 서버 목록은 `grep -A3 '<server>' … \| grep -o '<name>[^<]*'` 로 본다 | 같은 문서 10-1 |
| 결정 | 2호기(ws02·wb02)는 **같은 JEUS 도메인의 이중화** 전제. Gitea·Jenkins 는 ws01 에만. 배포킷(Jenkinsfile)의 2호기 대상 추가는 ws02·wb02 IP·접속 경로 확정 후 (그 전에는 복사한 잡으로). 이중화 데이터 문제(배치 중복, data 폴더, refresh 토큰 메모리 보관, 21001 세션, registration-id, DB 접속 수)는 "결정 필요" 로 기록 | 같은 문서 14장 |
| 작업 | **통합 매뉴얼 [INFRA_MANUAL.md](INFRA_MANUAL.md)** (후임자 인수인계용 1권): 반입물 제작·서버 셋팅·Gitea·Jenkins·배포 키·잡·웹훅·JEUS 확인·WebtoB `http.m`·소스 설정·첫 배포·2호기·운영·백업·보안·트러블슈팅 T01~T62, 01/02 값표(★) | — |
| 발견 | 업체 소스 `atms-journal` `LegacyAgentController` 의 `ping` 메서드가 닫히지 않은 주석(`/** …`) 안에 있어 **`/api/agent/ping`·`/atmr/agent/ping.do` 매핑이 없음**. 개국 응답은 `agent.ping.url=/api/agent/ping` 을 알려 주므로 실기기 폴링이 404 가 된다 → 개발 조치 필요 (이 저장소에서는 고치지 않음). L4 헬스체크 제안을 `/api/agent/ping` → `GET /v3/api-docs`(또는 TCP)로 정정 | 같은 문서 13-5, T49 |
| 확인 | (사용자) 1호기 완료: 프론트 자동배포, PC→112:9000, orai18n 캐시 반입, WAR 배포 시도 | 같은 문서 3-1 |


## 2026-11-02 · 소스 스냅샷 백업 결정, 서버 추가 전 작업 순서

| 구분 | 내용 | 문서 |
|---|---|---|
| 결정 | 매니지드 서버 6개 전환 전에 내부 Gitea `NH-ATMS/nh-atms` 를 새 저장소 **`NH-ATMS/nh-atms-old`** 에 mirror 복사 (PC `git clone --mirror` → `git push --mirror`, 비공개·보관(Archive)). 원래 저장소·Jenkins·웹훅 연결은 그대로, 수정은 원래 저장소에 | [INFRA_MANUAL.md](INFRA_MANUAL.md) 16-6 |
| 확인 | (사용자) WAS 담당이 새 서버 6개·앱 등록 진행 중 → ws01 앱 폴더 이름 정리(`catms-journal` → `cajou` 등)를 먼저 하고 경로를 담당에 알림 | 같은 문서 10-2 |
| 원칙 | 새 서버가 뜨기 전에는 백엔드 `build-only` 만 (`build-deploy` 는 옛 서버 `nbatmsltws01_atms01` 의 `catms` 를 새 구성 WAR 로 바꿔 버림), `http.m` 은 registration-id 회신 뒤 적용 | 같은 문서 3-1 |

## 2026-11-02 · DB 연결 방식 변경 — 개발서버 JNDI, 로컬 공통 파일

| 구분 | 내용 | 문서 |
|---|---|---|
| 받음 | 연결 DB 정보: DBMS `nbatmsltdb01_NBATMSTP`, IP 16.88.250.91, 포트 1531, 계정 `ATMS_APP`, **JNDI `dsATMS01`** (Spring XML `jee:jndi-lookup … resource-ref="true"` 방식 안내) | — |
| 결정 | 개발서버(jeus)는 JNDI `dsATMS01` 만 (직접 접속 대체 없음, WAR 에 DB 비밀번호 없음). 로컬은 `atms-common` `local-common.yml` 한 곳에 계정·비밀번호 하드코딩 | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 개정 2026-11-02 |
| 소스 | `jeus-common.yml` `spring.datasource.jndi-name: dsATMS01` / `local-common.yml` 신규 / `application-local.yml` 6개 import / `build.gradle`·`atms-common/build.gradle` 은 `-Pjeus` 때 로컬 설정 파일을 WAR 에서 제외 (빌드 시험은 서버·PC 에서) | 같은 문서 1·9·9-1~9-3 |
| 요청 | WAS 담당: `dsATMS01` 을 Oracle 드라이버·서버 6개 대상으로, JEUS 라이브러리에 orai18n, 연결 시험 | [WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md) 2-8 |

## 2026-11-03 · JWT 제거(보류)·화면↔서버 민감정보 암호화 설계

| 구분 | 내용 | 문서 |
|---|---|---|
| 결정 | JWT 제거는 NH SSO 모듈 정보가 올 때까지 **보류** | [FIELD_ENCRYPTION_DESIGN.md](FIELD_ENCRYPTION_DESIGN.md) 7장 |
| 결정 | HTTPS 미적용 규칙 하에 브라우저↔서버 민감정보 암호화: Java 표준 AES-256-GCM + RSA-2048-OAEP, 1차는 로그인·비밀번호 변경·사용자 등록의 비밀번호, 개인키는 서버 파일(`/home/atmswas/keys`, 640) | 같은 문서 0~6장 |
| 반입 | npm `node-forge-1.4.0.tgz`, `types-node-forge-1.3.14.tgz` (서버 Java 쪽 반입 없음) | 같은 문서 10장 |
| 남은 일 | 보안팀 확인(11장) → 구현 → 키 생성 → `required:false` 배포 → 화면 배포 → `required:true` | 같은 문서 8장 |
| 구현 | 1차 구현 완료: atms-common 공통 복호화(`security/crypto`), `GET /api/auth/public-key`, 로그인·사용자 등록·비밀번호 변경 3곳, 화면 `fieldCrypto.ts`(node-forge 1.4.0). 단위 테스트(화면 암호문 교차 검증 포함)·로컬 기동(재전송 400 확인) 통과 | [FIELD_ENCRYPTION_DESIGN.md](FIELD_ENCRYPTION_DESIGN.md) 12장, 적용 가이드 11장 |
| 남은 일 | 폐쇄망 PC 반영(npm tgz 반입) → ws01 키 생성(`/home/atmswas/keys`, 설계서 5장 — **키 없으면 atms01 기동 실패**) → 배포 → 화면 확인 → `required: true` | 같은 문서 8장 |


## 2026-11-03 · 가정값으로 배포킷 반입, 현업 값 반영 명령 정리

| 구분 | 내용 | 문서 |
|---|---|---|
| 결정 | 현업 회신이 늦어 **가정값(서버 이름·포트·폴더·JNDI·registration-id)으로 배포킷 반입**, 실제 값은 서버·소스에서 직접 수정 | [POST_HANDOVER_EDITS.md](POST_HANDOVER_EDITS.md) |
| 검토 | 배포킷 스크립트 `bash -n` 전체 통과, 옛 값(8766·`catms-journal`·`JEUS_URL`)은 경고 문구에만 남음 | — |
| 정정 | 필드 암호화 npm 캐시 절차: `cache/npm` 은 subuid 소유라 `mkdir`·`cp` 에 `podman unshare` 필요 | [FIELD_ENCRYPTION_DESIGN.md](FIELD_ENCRYPTION_DESIGN.md) 10장 |
| 추가 | 첫 배포 전 필수: 암호화 키 `/home/atmswas/keys` 생성 (없으면 atms01 기동 실패), Jenkins npm 캐시에 node-forge | 같은 문서 0-2·0-3 |


## 2026-11-04 · ws01 `/home/atmswas` 100% — Gitea 매일 dump 누적

| 구분 | 내용 | 문서 |
|---|---|---|
| 현상 | `/home/atmswas` 32G 중 `~/atms/infra/gitea/data/backup` 22G (Jenkins 6.3G, inbox 1.3G, release 862M, images 451M, Podman 940M). 지웠는데 열린 파일 없음 | — |
| 원인 | 배포킷 `gitea-backup.service` 가 dump 를 Gitea 데이터 폴더(`/var/lib/gitea/backup` = `data/backup`) 안에 만들어, 다음 dump 가 이전 dump 들을 다시 담음 → 매일 불어남 (배포킷 설계 오류) | [INFRA_MANUAL.md](INFRA_MANUAL.md) T65 |
| 수정 | dump 를 표준출력(`-f -`)으로 받아 데이터 폴더 밖 `~/atms/backup/gitea` (atmswas 소유) 에 저장, 7일 보관, `.part` 후 이름 변경. `install-gitea.sh` 폴더 생성 변경 | `deploy/gitea/gitea-backup.service` |
| 완료 | ws01 정리·새 서비스 적용 (11:33): 새 dump 41M, 안의 `gitea-dump-` 0개, 타이머 다음 실행 11-05 02:30, `/home/atmswas` 30G 중 8.7G (29%), Gitea healthz pass. 옛 dump 는 마지막 파일 하나가 2.4G (실제 데이터의 약 60배) | INFRA_MANUAL T65 |
| 남은 일 | 디스크가 가득 찼던 동안의 영향 확인 (PC push 1회·Jenkins 빌드 1회). inbox(1.3G)·images(451M) 남은 반입물 정리 검토. 내일 02:30 자동 실행 뒤 크기가 41M 안팎인지 확인 | — |


## 2026-11-04 · 11-03 판 배포킷 반입 적용 (Gitea 백업 수정본 유지)

| 구분 | 내용 | 문서 |
|---|---|---|
| 결정 | 오늘 반입분은 11-03 기준 zip (T65 수정 없음) → **내부 PC 에서 `deploy/gitea/gitea-backup.service` 를 수정본으로 바꾼 뒤** ws01 `~/atms`, wb01 `~/deploy/kit` 에 FileZilla 덮어쓰기. 서버 `docs` 는 다음 반입 때 갱신 | [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) "배포킷 갱신" ①-1, [INFRA_MANUAL.md](INFRA_MANUAL.md) 6-5 |
| 확인 | `grep -c -- "-f - >" deploy/gitea/gitea-backup.service` = 1, 적용본과 `diff` 같음 | 같은 곳 |


## 2026-11-05 · 로컬 DB 계정 PC별 분리 + 계정 잠김 방지

| 구분 | 내용 | 문서 |
|---|---|---|
| 현상 | PC마다 DB 계정이 다른데 `local-common.yml` 한 곳(Gitea)에만 계정이 있어, 빌드(`gradlew build` 의 atms-web `@SpringBootTest contextLoads`)·기동(atms-web `AdminPasswordInitializer`, atms-batch)마다 남의 계정·틀린 비번으로 로그인 실패 → 계정 잠김, 현업에 해제 요청 | — |
| 결정 | 계정은 각 PC **`D:\local-db.yml`** (Git·WAR 밖, 모든 PC 에 D 드라이브 있음). 3중 방지: ① DB 테스트 `@Tag("db")` 빌드 기본 제외(`-PdbTest`) ② 계정 파일 없으면 접속 없이 기동 중단 ③ ORA-01017/28000 을 `D:\db-login-failed` 에 해시로 기록, 같은 계정·비번 재접속 차단 | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 12장 |
| 작업 | `local-common.yml`(계정 줄 제거, `optional:file:D:/local-db.yml` import), 루트 `build.gradle`(test 태그 제외), `AtmsWebApplicationTests`(`@Tag("db")`), 신규 `atms-common/.../db/LocalDbLoginGuard.java` (local 프로파일, p6spy 로 감싼 DataSource 도 안쪽 Hikari 검사) | 같은 문서 12-1~12-4 |
| 검증 | 저장소 사본(Linux): 빌드 성공·`contextLoads` 제외, 파일 없음/실패 기록 있음 → Hikari 시도 0회로 중단, ORA-01017 흉내 → 1회 뒤 차단, 네트워크 오류는 기록 안 함. 실제 Windows D 드라이브·은행 DB 는 PC 첫 실행 때 확인 | 같은 문서 12-6 |
| 발생 | PC 적용 중 `D:\local-db.yml` 을 Tab 으로 들여써 `ScannerException … TAB` (DB 접속 전 단계, 잠김 영향 없음) → 스페이스로 다시 작성 | 같은 문서 12-5, INFRA_MANUAL T66 |
| 남은 일 | 이전에 Gitea `local-common.yml` 에 커밋된 실제 계정·비번은 이력에 남음 → 비번 변경 검토. `init.admin.reset-password: true` 는 로컬 기동마다 공용 DB admin 비번을 덮어씀 (별개, 미결) | — |


## 2026-11-06 · 화면 `api/http.ts` 전환과 암호화 모듈 · npm 오프라인 설치 정정

| 구분 | 내용 | 문서 |
|---|---|---|
| 결정 | 폐쇄망 화면의 다른 모듈은 `axiosInstance` → fetch 기반 `api/http.ts` 로 교체(사용자 작업). **암호화 모듈(`fieldCrypto.ts` 등)은 반입한 axios 기반 그대로** → `axios` 의존성 유지. 잠시 반영했던 fetch 버전 `fieldCrypto.ts` 는 저장소에서도 되돌림 | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 11-20 |
| 발생 | 폐쇄망 PC `npm install node-forge@1.4.0 --save-exact --offline` → `ENOTCACHED` (cache add 는 성공). 첫 `cache add` 는 공백 있는 경로를 따옴표 없이 써 `DirFetcher …` 오류 | INFRA_MANUAL T67 |
| 정정 | 원인: `npm cache add <tgz>` 는 내용만 넣고 레지스트리 목록 정보(packument)는 없음 → 이름 설치 불가. **두 파일에 버전·integrity 를 손으로 적고 `npm install --offline`** 으로 해결 (빈 캐시에서 재현·확인, devDependency 포함). 가이드 11-24·설계서 10장 정정 | [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 11-24 |
| 결정 | **npm 라이브러리는 tgz 대신 npm 캐시 폴더째 반입** (인터넷 PC `npm cache add 이름@버전 --cache D:\npm-carry` → zip). 폐쇄망은 `robocopy` 로 캐시에 합친 뒤 `npm install 이름@버전 --offline` → json 자동 갱신, 손 수정·스크립트 불필요. Jenkins 캐시에도 같은 zip 을 `podman unshare cp -rn` 으로 합침. 확인: 빈 프로젝트 캐시 + 합친 캐시로 json 이 저장소와 같게 생성, `npm ci --offline` 통과 | [REQUIRED_FILES.md](REQUIRED_FILES.md) 3-5 |
| 결정 | **`node_modules` 를 Git 에 올리는 안은 철회** (용량·Windows 전용 바이너리·`npm ci` 가 지우고 재설치·Gitea 백업 증가). 팀원 PC 는 npm 캐시 공유 + `npm ci --offline`. **이번 node-forge 도 외부망에서 캐시로 만들어(`npm cache add 이름@버전 --cache D:\npm-carry`) 반입** — tgz + json 손 추가 방식은 쓰지 않음 | [REQUIRED_FILES.md](REQUIRED_FILES.md) 3-5 |

