# 개발서버 인프라 구성도

작성일: 2026-10-08 · 개정 2026-10-12 (백엔드 6개 모듈 = atms01 에 WAR 6개, 컨텍스트 경로 분리 결정 → [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md)) · 대상: ATMS 개발서버 `nbatmsltws01`(WAS) / `nbatmsltwb01`(WebtoB) / Oracle · 개정 2026-10-12 (작업 계정·기준 경로를 `atmswas` · `/home/atmswas` 로 변경, `/home/userATMS` 사용 안 함)

> **현재 구조 아님 (2026-11-03 표시)** — 이 문서는 **2026-10-12 단일 서버(atms01 에 WAR 6개, 8766)** 결정 시점에 그린 구성도이며 이후 확인값(IP·포트·계정)은 본문에 계속 반영돼 있다. 서버 구성 부분 기준이다. 현재는 **JEUS 매니지드 서버 6개**(atms01·ajou01·abat01·file01·ifin01·ifex01, 포트 8080~8085, 컨텍스트 `/`, 게이트웨이 끔, DB 는 JEUS JNDI `dsATMS01`). 지금 할 작업은 [INFRA_MANUAL.md](INFRA_MANUAL.md) 를 따르고, 결정 배경은 [JEUS_MANAGED_SERVERS_DECISION.md](JEUS_MANAGED_SERVERS_DECISION.md). 아래 본문은 이력·참고용으로 남긴다.

근거: 환경구성표(`[개발_환경구성표] 프로젝트NEO 선도개발 자동화기기_WEBWAS 엔진 환경구성표`), 2026-10-07~08 서버 확인 결과, 배포킷 문서

> 이 문서는 **무엇이 어디에 있고 어떻게 연결되는가**만 정리한다. 작업 절차는 각 장에 링크한 문서를 따른다.
>
> 표기: **[확인]** 서버에서 직접 확인 · **[구성표]** 환경구성표 설계값(실제 미검증) · **[계획]** 배포킷에서 정한 계획 · **[코드]** NH-ATMS 소스 기본값 · **[미확인]** 확인 필요

---

## 1. 전체 구성도 (목표: JEUS 기준)

```
[은행 PC 브라우저]              [ATM 에이전트 (실기기 연동 시)]
      │ HTTP :9000                     │ 고정 경로 /atms/agent/** 등
      │                                │ (WebtoB 경유 / ws01 직접, 포트 [미확인])
      ▼                                ▼
nbatmsltwb01 (16.88.122.112) ── WebtoB 5.0 · 계정 webatms · :9000 [확인 2026-10-15]
  ├─ /                 정적 파일 → DocBase /home/atmswas/docs  (React dist)
  └─ /api/ /swagger-ui/ /v3/api-docs /actuator/ /atms/ /atmr/ /atmf/
      │ JSV 연동 (JEUS 가 WebtoB :19000 에 접속) [확인 2026-10-15 domain.xml]
      ▼
nbatmsltws01 (16.88.122.113) ── JEUS 9.1 · 계정 jeusatms · /JEUS/jeusATMS [구성표]
  ├─ DAS(adminServer) + NodeManager      OpenJDK 21.0.11 로 실행 중 [확인]
  ├─ ★ 목표 구성 (2026-10-26 결정, 배포킷·소스는 2026-10-27 반영) — 매니지드 서버 6개, 컨텍스트 모두 '/', 게이트웨이 없음
  │    atms01 :8080 atms-web (catms) · ajou01 :8081 (cajou) · abat01 :8082 (cabat)   [이름·폴더 확정 2026-10-27, 기존 nbatmsltws01_atms01 대체]
  │    file01 :8083 (cfile) · ifin01 :8084 (cifin) · ifex01 :8085 (cifex)
  │    WebtoB 가 경로별로 각 서버에 분배 (deploy/webtob/http.m.atms-jsv.txt), 모듈 간 호출 http://16.88.122.113:<포트> [계획 — WAS 담당 서버 생성 대기]
  └─ (현재 상태) atms01 = nbatmsltws01_atms01 (업무 서버, JVM 1개, HTTP 8766)  앱 atms 1개만 등록 [확인 2026-10-26] — 아래는 이전 구성
      ├─ /          atms-web          /home/atmswas/catms          ← 외부 요청 입구 + 게이트웨이
      ├─ /journal   atms-journal      /home/atmswas/cajou
      ├─ /batch     atms-batch        /home/atmswas/cabat
      ├─ /file      atms-file         /home/atmswas/cfile
      ├─ /ifin      atms-if-internal  /home/atmswas/cifin
      └─ /ifex      atms-if-external  /home/atmswas/cifex
         모듈 간 호출: http://127.0.0.1:8766/<컨텍스트> (jeus.base-url) [확인 2026-10-15 domain.xml]
      │ JDBC — Spring 커넥션풀(Hikari) [계획, 2026-10-12 결정] (JEUS 데이터소스 dsATMS01·iddsATMS01 미사용)
      ▼
Oracle ── dsATMS01 · 서비스 NBATMSTP · 계정 ATMS_APP [구성표] · 호스트:포트 [미확인]
```

- 화면(정적 파일)은 WebtoB 가 직접 주고, API 등 동적 경로만 JEUS 로 넘긴다 → [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 7-4
- **6개 모듈 모두 WAR 로 빌드 가능** (`gradlew bootWar -Pjeus`, 2026-10-12). 같은 atms01 에 컨텍스트 경로로 나눠 올리고, 고정 경로(`/api/batch`, `/atmf` 등)는 atms-web 게이트웨이가 해당 모듈로 전달 → 프론트·에이전트 수정 없음
- JEUS 가 Jakarta(Spring Boot 3)를 지원하지 않으면 대안 B: 6개 모듈을 nbatmsltws01 에서 내장 톰캣(8080~8085)으로 띄우고 WebtoB 가 프록시 (JEUS 가이드 8장)

---

## 2. nbatmsltws01 (WAS 서버) 내부 구성

```
nbatmsltws01   RHEL 9.8 · CPU 2 core · MEM 31Gi(가용 25Gi) · swap 4G · SELinux Enforcing   [확인]
│              IP: 16.88.122.113 (HIWARE 접속·서비스 주소로 판단) · 그 외 16.89.122.86, 16.88.250.88, 16.89.250.68 [확인 2026-10-14]
│              rootless cgroup 위임: cpu 없음 (RHEL 9 기본) → 컨테이너 CPU 제한 불가, 메모리 제한만 [확인 2026-10-14]
│              podman 5.8.2 / crun 1.27: --userns=keep-id 사용 시 기동 실패 → 데이터 폴더는 podman unshare chown 1000:1000 [확인 2026-10-14]
│              접속: HIWARE 통제 (sudo/su 금지, 금지명령 3회 → 세션 차단) · root 는 1일 권한 신청
│
├─ [은행 표준 WAS]  계정 jeusatms · /JEUS/jeusATMS
│   │  ※ 2026-10-22: /JEUS 가 비어 있음 (/JEUS/jeusATMS 없음, 10-19 17:50 변경). DAS·NM 은 10-19 15:25 재기동 후 실행 중 → WAS 담당 확인 중 (WORK_LOG)
│   ├─ JEUS DAS(adminServer), NodeManager         실행 중 [확인] · DAS base 리스너 8736 [확인 2026-10-15 domain.xml]
│   ├─ atms01 (업무 서버)                           프로세스 없음 [확인] → 생성·기동 여부 WAS 담당 확인
│   │   ├─ 서버 이름  nbatmsltws01_atms01 (-Dobj_name 동일)           [확인 2026-10-15 domain.xml]
│   │   ├─ base 리스너 8766 = HTTP 리스너 (server-listener-ref base) → jeus.base-url http://127.0.0.1:8766
│   │   ├─ WebtoB 커넥터  nbatmsltws01_atms01-nbatmsltwb01_webatms → wb01:19000, …-nbatmsltwb02_webatms → wb02:19000
│   │   ├─ 데이터소스  iddsATMS01 (JEUS 쪽 등록, 앱은 Spring Hikari 사용 결정)
│   │   └─ JEUS 로그  /JEUS/jeusATMS/logs/JeusSystem/nbatmsltws01_atms01/stdout.log·stderr.log, GC 로그 /JEUS/jeusATMS/logs/GCLog/gclog_atms01.log
│   ├─ 같은 도메인에 2호기 nbatmsltws02_atms01 (8766) 등록 — 서버·IP [미확인], 이번 방화벽 신청 제외
│   ├─ 재기동                                      /JEUS/jeusATMS/jeusMENU/jeusRstart
│   └─ 로그                                        /WASlogs/jeusATMSlogs/nbatmsltws01/<서버>/JeusServer.log [구성표]
│
├─ [작업 계정 atmswas]  uid 50219 · 주 그룹 atms(50082) + jeus(2002) · /home/atmswas   [확인 2026-10-14]
│   │  ※ 2026-10-12 결정: 모든 작업을 atmswas 로 한다. /home/userATMS 는 사용 안 함
│   ├─ catms/                                      atms-web 배포 폴더 (컨텍스트 /, error.jsp 유지, 2750)
│   ├─ catms-{journal,batch,file,ifin,ifex}/       나머지 5개 모듈 배포 폴더 (2750)
│   ├─ data/                                       앱이 쓰는 파일 (업로드·이미지·다운로드, 2770)
│   ├─ deploy/                                     swap-deploy.sh 의 backup/(모듈별 최근 5개)·배포 이력 (700)
│   └─ atms/                                       배포킷 BASE_DIR (700)
│       ├─ inbox → release/<모듈>/                 업로드 → SHA256 확인 → 보관
│       ├─ images/                                 반입 이미지 tar
│       ├─ infra/gitea, infra/jenkins              지원 인프라 데이터 (700)
│       └─ rootless Podman 5.8.2 (atmswas)          지원 인프라 [계획]
│           ├─ atms-gitea    :3000 (HTTP 만, SSH 끔)  사내 Git
│           ├─ atms-jenkins  :8090                  빌드 (executor 1개)
│           └─ (보류) 대안 경로: 백엔드 :8080~8085, nginx :8088
│
└─ [은행 공통]  건드리지 않음
    ├─ 보안·모니터링 에이전트 계정                     /home/apciagnt, covmagnt, neoinfra, qmciagnt [확인]
    ├─ Flume 로그 수집 (JDK 8)                       [확인]
    ├─ LISTEN 6600 / 6700 / 6800 (0.0.0.0)          [확인] 용도 [미확인] → 사용 금지
    └─ /SMS /SMT /seba /sepa /ssw /stm /mft_* 등     공통 솔루션
```

### 2-1. 디스크

| 경로 | 크기 | 용도 | 비고 |
|---|---|---|---|
| `/home/atmswas` | **`/home` 공용 10G** (`vg_os-lv_home`, 여유 9.8G) [확인 2026-10-14] | 배포 폴더 6개·`data`·백업 + 배포킷·Podman 이미지·Gitea·Jenkins 데이터 (합계 5~10GB 예상) | **빠듯함 → 인프라에 증설·전용 디스크 요청** (`SERVER_DIRECTORY_LAYOUT.md` 3-3) |
| `/home/userATMS` | 20G (전용 LV) | **사용 안 함** (2026-10-12) | `/home/atmswas` 로 이관 요청 후보 |
| `/WASlogs` | 50G (전용) | JEUS 로그 (`jeusATMSlogs`) + 앱 로그 `nbatmsltws01/app/<모듈>.log` [계획] | 하위 폴더 허용 여부 [미확인] (9장) |
| `/nhod` | 50G (전용) | **용도 미확인** | 확인 전 사용 금지 |
| `/tmp` | 10G | 임시 | |

자세한 폴더 구조: [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 3장

### 2-2. Java

| 경로 | 버전 | 사용처 |
|---|---|---|
| `/usr/lib/jvm/java-21-openjdk-21.0.11...` | 21.0.11 | JEUS DAS·NodeManager 실행 중 [확인] |
| `/usr/lib/jvm/java-21-openjdk-21.0.2...` | 21.0.2 | 2026-10-07 초기 조사에 기록됨 → `ls /usr/lib/jvm` 로 재확인 |
| `~/atms/jdk` (반입 21.0.12) | 21.0.12 | 프로토타입 직접 실행(`bin/direct.sh`) 용 |

---

## 3. nbatmsltwb01 (WebtoB 서버)

```
nbatmsltwb01   WebtoB **5.0.0.4 64bit** (5.0 SP0 Fix#4, 2020-12-16 설치) · 계정 webatms · 프로세스 wsm/htl/hth 를 webatms 로 실행 중 (8/29 14:29 기동, path/webtob.pid) [확인 2026-10-21 .profile]
│              IP: 16.88.122.112 (HIWARE 접속 대상, WAS 16.88.122.113 과 같은 대역 → 서비스 IP 로 판단, 인프라 확인 전 잠정)
│                  그 외 16.89.122.85 (ens161), 16.88.250.87 (ens224), 16.89.250.67 (ens256) [확인 2026-10-15]
├─ 접속 계정       atmswas 로그인 가능, uid 50219 · 그룹 atms + jeus (WAS 서버와 동일) [확인 2026-10-15]
├─ 설치 경로       /WEBTOB/webtobATMS [확인] (/usr/local/webtobATMS 는 이곳의 심볼릭 링크 [확인 2026-10-21]) · config/ 는 webatms:jeus 700 → atmswas 는 http.m 을 읽을 수 없음 [확인 2026-10-15]
│                  log/ bin/ webtobMENU/ path/ Apache/ 는 webatms:jeus 750 → atmswas 가 jeus 그룹으로 읽기 가능, config/ docs/ ssl/ 은 700 [확인 2026-10-21]
│                  사용자 계정은 atmswas 뿐 (webatms 로그인 불가) [확인 2026-10-21]
├─ 리스너          :9000 LISTEN (0.0.0.0). 2026-10-15 에는 / → 302 (/error/error.html), **2026-10-21 에는 / · /index.html → 200 (동료가 화면 배포)**, /api/ → 302 Location /error/error.html (API 연동 없음) [확인 2026-10-21]
│                  WebtoB 는 8/29 이후 재기동 없음 → 동료는 WebtoB 설정을 바꾸지 않고 기존 DocBase 에 파일만 올린 것으로 판단 [추정 2026-10-21]
│                  JEUS 연동(JSV) 포트 **19000** [확인 2026-10-15 — JEUS domain.xml 의 WebtoB 커넥터 port 19000] (9900 아님)
│                  그 외 외부 LISTEN: 22, 111, 1556·13724·13782 (백업 에이전트로 보임), 4750, 47006, 53007 — 사용 금지
│                  나가는 연결: 16.24.112.91·93:9092 (Kafka 계열로 보임), 16.24.152.102:1556 / 접속 경로 16.24.126.203 → :22 (HIWARE)
│                  WAS(ws01) 에서 온 연결 없음 → JEUS 미연동 상태 [확인 2026-10-15, 2026-10-21 동일]
│                  그 외 webatms 프로세스: flume 에이전트 2개 (access 로그 → logstash) [확인 2026-10-21]
├─ DocBase         /home/atmswas/docs  (atmswas:jeus 2750) — **9000 의 DOCROOT 확정** [확인 2026-10-21: ~/docs/index.html 과 curl 127.0.0.1:9000/index.html 의 md5 일치]
│   ├─ index.html, assets/, nh_logo*.png, vite.svg   Vite 빌드, 동료가 2026-10-21 11:00 배포 [확인]
│   ├─ inbox/ (700), releases/ (2750)             2026-10-16 생성, 비어 있음 — releases/ 는 그룹 읽기라 파일을 두면 9000 으로 다운로드됨 → DocBase 밖으로 옮길 것
│   └─ (2026-10-15 에는 폴더 없음)
├─ 그 외 홈        ~/nodejs, ~/tools, ~/data: node v22.23.2 사본, ~/mock/mock-api.mjs (실행 중인 node 프로세스 없음) [확인 2026-10-21]
│                  mock-api.mjs = 동료가 만든 가짜 API: node:http 로 8080·8082 LISTEN, /auth/ → 가짜 토큰·사용자, /menu/ → 메뉴 트리, 그 외 빈 목록
│                  (백엔드 없이 화면만 띄워 보는 용도로 보임. 화면과 연결하려면 WebtoB 프록시 필요 — 사용자 결정: 기록만, 실행 안 함)
├─ 환경(.profile)  WEBTOBDIR=/WEBTOB/webtobATMS, LANG/LC_ALL=ko_KR.euckr, umask 027 — atmswas 는 이 파일을 읽을 수만 있음 (자기 PATH 에는 없음) [확인 2026-10-21]
├─ 재기동         /WEBTOB/webtobATMS/webtobMENU/webtobRstart (root:sys setuid, 8/20) + webtobRstart.c (읽기 가능) — **실행 금지**, WebtoB 담당 영역 [확인 2026-10-21]
├─ 로그           /WEBTOB/webtobATMS/log (= logs 링크): atms_access.log_MMDDYYYY, atms_error.log_MMDDYYYY, system.log_MMDDYYYY, engine_info_check/ — webatms:jeus 640 → atmswas 읽기 가능 [확인 2026-10-21]
│                  readme.txt 에 webmon.sh·filebeat 7.4.2 기동 명령이 있으나 실제 실행 중인 것은 flume [확인 2026-10-21]
│   └─ error/error.html                은행 에러페이지 (유지)
├─ 화면 배포       Jenkins(ws01) → SSH :22 → ~/deploy/bin/deploy-front.sh (배포 전용 키, authorized_keys 에 command=·from= 고정) → docs 교체 [계획 2026-10-21, FRONTEND_CICD_PLAN.md]
├─ WAR 배포        Jenkins(ws01 컨테이너) → SSH :22 → ws01 호스트 ~/atms/deploy/jeus/deploy-war.sh (배포 전용 키) → catms* 교체 → WAS 담당 jeusRstart [계획 2026-10-23, BACKEND_CICD_PLAN.md 6장]
│                  ws01 호스트·Jenkins 컨테이너 → wb01:22 접속 가능 [확인 2026-10-21]
└─ JEUS 연동       *URI 로 /api/ 등을 JSV(atms01) 에 전달 — 기존 설정 [미확인]
```

- atmswas 로 접속·`docs` 쓰기 가능 [확인 2026-10-21]. `http.m` 은 읽을 수 없음 → WebtoB 담당

---

## 4. 계정·권한 구조

```
그룹 jeus (GID 2002) [구성표]
├─ atmswas    업무팀 계정 = 작업 계정 — 앱 파일 소유(catms*, data, docs), 배포킷·Gitea·Jenkins 운영   로그인 가능 [확인]
├─ jeusatms   JEUS 엔진 계정 — 그룹 권한으로 catms* 읽기, data 쓰기   JEUS 실행 계정 [확인] · 그룹 소속 [확인] uid 40119, 주 그룹 jeus(2002) + comp(2001) (2026-10-15)
└─ webatms    WebtoB 엔진 계정 — 그룹 권한으로 docs 읽기       [구성표]

userATMS      사용 안 함 (2026-10-12). 예전 개인 작업 계정
atmswas 의 ~/atms 는 700 → WAR(비밀번호 포함)·Gitea/Jenkins 데이터는 atmswas 만 읽음
```

| 작업 | 계정 | 서버 |
|---|---|---|
| 산출물 업로드·SHA256 확인·보관 | atmswas | nbatmsltws01 |
| catms* 교체 배포·롤백 (`swap-deploy.sh`, WAR 이름으로 폴더 자동 선택) | atmswas | nbatmsltws01 |
| JEUS 재기동 (`jeusRstart`) | jeusatms | nbatmsltws01 |
| dist 배포 (`docs`) — Jenkins 가 SSH 로 `deploy-front.sh` 호출 (키는 이 명령만 가능) | atmswas | nbatmsltwb01 |
| WebtoB 설정(`http.m`)·재기동 | webatms 또는 WebtoB 담당 | nbatmsltwb01 |
| Gitea·Jenkins 컨테이너 운영 (rootless Podman) | atmswas | nbatmsltws01 |
| firewalld, atmswas 의 subuid·linger, 디스크 할당 | root (인프라 또는 1일 root) | 양쪽 |

권한값과 생성 절차: [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 7장 · root 작업 목록: [ROOT_DAY_CHECKLIST.md](ROOT_DAY_CHECKLIST.md)

---

## 5. 요청 흐름

### 5-1. 화면·API

```
브라우저 ──:9000──▶ WebtoB ──┬─ /            → DocBase(dist) 직접 응답
                             └─ /api/ 등     → JEUS atms01 / (atms-web) ──▶ Oracle
                                                 └─ /api/batch, /api/file … → 게이트웨이 → /batch, /file …
```
- 화면은 API 를 상대경로(`/api`)로 호출 → WebtoB 와 같은 주소로 나가므로 프론트 코드 수정 불필요
- `/api/batch/**` 는 atms-batch 가 처리한다 [코드]. JEUS 에서는 atms-web 게이트웨이(`ModuleGatewayFilter`)가 `/batch` 컨텍스트로 넘기므로 **WebtoB 는 `/api/` 하나만 JEUS 로 연결**하면 된다 (2026-10-12 Tomcat 대체 검증)

### 5-2. ATM 에이전트 (실기기 연동 시)

```
ATM 에이전트 ──▶ atms-web  /atms/agent/** · /atmr/agent/** · /atms/remote-exec/**
                   └─ LegacyAgentGatewayController 가 그대로 전달 ──▶ atms-journal(/journal, 실제 처리) ──▶ Oracle
ATM 에이전트 ──▶ atms-web  /api/agent/** · /api/journal/** · /atmf/**
                   └─ 게이트웨이 ──▶ /journal, /file
ATM 에이전트 ──▶ atms-file 소켓 파일서버 :21001 (기본 꺼짐, 켜면 JEUS JVM 안에서 포트를 엶) [코드]
```
- 에이전트 펌웨어는 atms-web 주소·경로가 고정이라 바꿀 수 없다 → atms-web 이 JEUS 에 있으면 에이전트 요청도 JEUS(또는 WebtoB)로 들어온다
- 처리 로직은 atms-journal 에 있으므로 **atms-journal 이 떠 있지 않으면 에이전트 연동은 동작하지 않는다**

### 5-3. 모듈 간 HTTP 호출 (소스 기본값 [코드])

| 호출 | 설정 키 (환경변수) | 기본값 | JEUS (`application-jeus.yml`, 2026-10-12 반영) |
|---|---|---|---|
| atms-web → atms-journal (에이전트 전달) | `agent.proxy.journal-base-url` (`AGENT_PROXY_JOURNAL_BASE_URL`) | `http://localhost:8081` | `${jeus.base-url}/journal` |
| atms-web → atms-file (원격제어 전문 송신) | `atmf.remote.snd-msg-uri` (`ATMF_REMOTE_SND_MSG_URI`) | `http://localhost:8083/atmf/snd.msg.do` | `${jeus.base-url}/file/atmf/snd.msg.do` |
| atms-file → atms-web (비동기 응답 전달) | `atmf.remote.msg-async-resp-forward-uri` (`ATMF_REMOTE_MSG_ASYNC_RESP_FORWARD_URI`) | `http://localhost:8080/atms/remote-exec/put.msg.resp.do` | `${jeus.base-url}/atms/remote-exec/put.msg.resp.do` |
| atms-journal → IF 모듈 | `if.queue.internal-base-url`, `external-base-url` | `http://localhost:8084`, `8085` | `${jeus.base-url}/ifin`, `/ifex` |
| atms-web ↔ atms-file 공유 폴더 | `download.storage.base-dir` (`DOWNLOAD_STORAGE_DIR`) | `./data/atms-download` (작업 디렉토리 기준) | `${jeus.data-dir}/atms-download` (절대경로, 기본 `/home/atmswas/data`) |

`jeus.base-url` = atms01 HTTP 리스너 (`jeus-common.yml`, `http://127.0.0.1:8766` — 2026-10-15 domain.xml 확인. atms01 기동 후 `ss -ltn | grep 8766` 으로 127.0.0.1 에서 받는지 확인).

외부 연동 (모두 기본 꺼짐/mock):

| 모듈 | 연결 | 기본값 [코드] |
|---|---|---|
| atms-if-internal | 은행 EAI TCP (나가는 방향) | `mode: mock`, EAI `127.0.0.1:39021` |
| atms-if-external | 대외기관 고정길이 전문 TCP | `mode: mock`, 업체별 IP·포트 미설정 |
| atms-file | 펌웨어 SFTP (나가는 방향) | `enabled: false` |
| atms-journal | Kafka / Elasticsearch | 꺼짐 (은행 구성에서 제외) |

모든 백엔드 모듈은 **`SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` 없이는 기동 실패**하고, 기본 프로파일 `local` 은 업체 DB(`192.168.28.21`)를 가리킨다.
JEUS 에서는 `-Pjeus` WAR 의 `web.xml` 이 프로파일 `jeus` 를 켜고, 은행 DB·JWT 값은 WAR 안의 `jeus-common.yml` 에서 읽는다 (2026-10-12 결정: 설정은 WAR 안에 포함, 실제 값은 내부망 소스에서만 기입).

---

## 6. 배포·반입 흐름

### 6-1. 지금 (수동)

```
[폐쇄망 개발 PC]  gradlew bootWar -Pjeus --offline (WAR 6개)      npm run build → dist zip
      │ FileZilla(바이너리) · atmswas 접속                           │
      ▼                                                               │
nbatmsltws01  ~/atms/inbox ── sha256sum -c ──▶ ~/atms/release/<모듈>/ │
      │ atmswas: swap-deploy.sh deploy <war>                          │
      ▼                                                               ▼
/home/atmswas/catms, catms-<모듈> (교체, 이전본 → deploy/backup)  nbatmsltwb01 /home/atmswas/docs
      │ jeusatms: jeusRstart (atms01)                        (swap-deploy.sh 또는 수동, 업로드 경로 [미확인])
      ▼
JEUS atms01 기동 → 로그 "Started Atms…Application" 6개 + /v3/api-docs 200 + /api/file/firmware/list 200(게이트웨이) 확인
```
절차: [SERVER_DIRECTORY_LAYOUT.md](SERVER_DIRECTORY_LAYOUT.md) 4장, [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 2~7

### 6-2. 향후 (CI) [계획]

```
[내부망 Windows PC] ── git push (HTTP, 토큰) ──▶ Gitea :3000 ──웹훅(notifyCommit)──▶ Jenkins :8090 (offline-seed 캐시로 빌드)
                                                            │ WAR/dist 산출
                                                            ▼
                                                   ~/atms/release/<모듈>/ ──▶ 6-1 과 같은 배포
                                                            │ 화면(dist): tar.gz + sha256
                                                            ▼
                                     ssh atmswas@16.88.122.112 "deploy …" < tar.gz  (113 → 112:22, 배포 전용 키)
                                                            ▼
                                     wb01 deploy-front.sh → swap-deploy.sh → /home/atmswas/docs (확인 실패 시 자동 되돌림)
```
- 화면 배포 상세 (결정 2026-10-21, Jenkins SSH 방식): [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md)
- Gitea 구축·운영: [GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md)
- Jenkins 도 atmswas 의 rootless Podman 으로 돌고(컨테이너 jenkins = 호스트 atmswas), `catms*`·`release` 소유자도 atmswas 라서 그룹 추가 없이 배포 단계까지 자동화할 수 있다 (마운트 설계는 추후)

### 6-3. 반입 (외부망 → 서버)

```
[외부망 PC] 현업 제작: gitea-rootless.tar + nh-atms-jenkins-bank.tar + SHA256SUMS  (보류: JRE·nginx·compose)
      │ 반입 승인
      ▼
[폐쇄망 PC] ── FileZilla ──▶ nbatmsltws01 ~/atms/images ── sha256sum -c ──▶ podman load
```
목록: [REQUIRED_FILES.md](REQUIRED_FILES.md)

---

## 7. 포트·방화벽 총괄

| 포트 | 서버 | 서비스 | 상태 | 접근 주체 | 방화벽 |
|---|---|---|---|---|---|
| 9000 | nbatmsltwb01 (16.88.122.112) | WebtoB | LISTEN [확인 2026-10-15] | 은행 PC 브라우저, ws01 | ws01 → wb01 9000 **열림** (302) [확인 2026-10-15] · **PC(10.248.170.171) → 9000 막힘** (ping 성공, TCP 실패) [확인 2026-10-15] → 네트워크 방화벽 신청 필요 |
| 19000 | nbatmsltwb01 | WebtoB JSV 연동 (JEUS 가 접속) | LISTEN · 용도 [확인 2026-10-15 domain.xml] | ws01 JEUS → wb01 | ws01 → wb01 19000 **열림** (`/dev/tcp` OK) [확인 2026-10-15] |
| 22 | nbatmsltwb01 | SSH | LISTEN [확인] | HIWARE(16.24.126.203) 접속, **Jenkins 화면 배포** (ws01, 배포 전용 키) | ws01 호스트·Jenkins 컨테이너 → wb01 22 **열림** [확인 2026-10-21] |
| 8766 | nbatmsltws01 | JEUS atms01 HTTP 리스너 (base) — **목표 구성에서는 8080 으로 이동, 사용 안 함** | [확인 2026-10-15 domain.xml] · atms01 미기동이라 LISTEN 아님 | 서버 내 curl, **모듈 간 호출·게이트웨이(127.0.0.1)**, 개발 PC (직접 테스트) | PC → 8766 신청 (2026-10-15) |
| ? | nbatmsltws01 | WebtoB ↔ JEUS 커넥터 | WebtoB 방식은 **JEUS 가 WebtoB JSV 포트로 접속**(ws01 → wb01, 위 19000 행) | — | WAS 담당 확인 |
| 6600 / 6700 / 6800 | nbatmsltws01 | 기존 사용 | [확인] 용도 [미확인] | — | 사용 금지 |
| 8080 ~ 8085 | nbatmsltws01 | **JEUS 매니지드 서버 6개** (web 8080 · journal 8081 · batch 8082 · file 8083 · ifin 8084 · ifex 8085, base=HTTP 리스너) | 미사용 확인 [확인 2026-10-26] · 서버 생성 [계획] | 모듈 간 호출, Jenkins 기동 확인, 개발 PC (직접 테스트) | PC → 113:8766 신청분을 8080~8085 로 변경 필요 |
| 8088 | nbatmsltws01 | 프론트 nginx (프로토타입) | [계획] | PC | 신청 필요 |
| 8090 | nbatmsltws01 | Jenkins | [계획] | 개발 PC | 신청 필요 (PC). Gitea → Jenkins 웹훅은 podman 네트워크 `atms-net` 안에서 `atms-jenkins:8080` 으로 [구성·확인 2026-10-22: Gitea→atms-jenkins 200, Jenkins→atms-gitea 200]. 서버 IP 로는 컨테이너끼리 연결 거부 [확인 2026-10-22] |
| 3000 | nbatmsltws01 | Gitea HTTP (SSH 사용 안 함) | [계획] | 내부망 개발 PC (2~5대) | 신청 필요 (현재 PC → 서버는 HIWARE SSH 만 가능) |
| 21001 | nbatmsltws01 | atms-file 소켓 파일서버 | [코드] 기본 꺼짐 | ATM 에이전트 | 실기기 연동 시 |
| DB 포트 | Oracle | NBATMSTP | [미확인] | ws01 → DB | 신청 필요 |
| 39021 (나감) | EAI | atms-if-internal | [코드] mock | ws01 → EAI | 실연동 시 |

- 서버 자체 방화벽(firewalld)은 1일 root 로 처리 (ROOT_DAY 2-3), **네트워크 구간 방화벽**(PC → 서버, 서버 → DB)은 별도 신청
- rootless 는 1024 미만 포트 사용 불가

---

## 8. 현재 상태 vs 목표 (2026-10-08, 2026-10-12·2026-10-16 갱신)

> 날짜별 작업 이력: [WORK_LOG.md](WORK_LOG.md)

| 구성 요소 | 현재 | 목표 | 다음 행동 | 문서 |
|---|---|---|---|---|
| `/home/atmswas` | **전용 LV `vg_app-lv_home_atmswas` 10G 마운트 (2026-10-16)**, atmswas:jeus 755. 기존 내용은 아래 `/home` 에 가려짐 → 복사 요청 (ROOT_DAY 2-11) [확인 2026-10-19] | atmswas:jeus 750, 디스크 증설 | `chgrp`·`chmod`(atmswas 직접), 디스크 요청 | LAYOUT 0-1, 3-2, 3-3 |
| `catms*`, `data`, `deploy`, `atms` | 미생성 | 2750 / 2770 / 700 / 700 | atmswas 가 직접 생성 → WAS 담당 회신 | LAYOUT 3-2, 7-3 |
| WebtoB `docs` | **생성·화면 배포됨, 9000 DOCROOT 확정** (동료, 2026-10-21) [확인]. 안에 빈 `inbox/`·`releases/` 있음 | 같은 구조 | `releases/`·`inbox/` 를 DocBase 밖으로, WebtoB 담당 회신 | LAYOUT 7-5 |
| JEUS atms01 | 프로세스 없음 [확인] | 생성·기동, 앱 6개 등록 (컨텍스트 경로) | 현업(WAS 담당) 요청 | SOURCE_CHANGES 7-6 |
| Jakarta 지원 | 미판정 | 판정 | jar 검사 (python3) | JEUS 가이드 1-3 |
| WAR 6개 | 빌드 가능, Tomcat 10.1 대체 검증 (batch 는 DB 필요) | atms01 배포·기동 | STEP 2~6 | JEUS 가이드, SOURCE_CHANGES |
| WebtoB `http.m` 연동 | `config/` 가 webatms 700 이라 atmswas 읽기 불가 [확인 2026-10-15]. JEUS 쪽 커넥터(19000)는 등록돼 있음 | `/api/`, `/atms/`, `/atmr/`, `/atmf/` → JEUS 루트 | WebtoB 담당 요청 | JEUS 가이드 7 |
| DB | 호스트·포트 [미확인] | 연결 | DBA 문의, 방화벽 | SETUP 7장 |
| atmswas 의 subuid / linger / cgroup | subuid 등록됨, **`Linger=yes`** (2026-10-15 root 작업), cgroup cpu 위임 파일 작성 (`[Service]` 줄 재작성 여부 확인 필요) | — | `podman info` 로 `cpu` 확인 | ROOT_DAY 2-10 |
| Gitea | 반입·적재·기동·관리자 생성까지 완료 (2026-10-14~15). root 작업 후 Quadlet 재설치 필요 | 상시 기동, PC 에서 push | `install-gitea.sh` 재실행, PC 확인 | SETUP 8단계 |
| Jenkins | 반입·기동 완료 (플러그인 87개). root 작업 후 `--restart=always` 로 재생성 필요 | 상시 기동 | 9-2 재실행 + podman-restart enable | SETUP 9단계 |
| 방화벽 | **신청 완료 (2026-10-16)**: 개발 PC 4대 → 112:9000, 113:8766·3000·8090. 서버 firewalld 는 꺼져 있음 | 개방 | PC 에서 `Test-NetConnection` 확인 | SETUP 10단계 |

---

## 9. 확인·결정 필요 사항 (모아보기)

| # | 항목 | 왜 필요한가 | 누구에게 |
|---|---|---|---|
| 1 | atms01 기동 시점, 앱 6개 등록(context-path) 방식(폴더/WAR), 앱 1개만 재배포 가능 여부 — (HTTP 포트 8766·서버 이름 `nbatmsltws01_atms01` 은 2026-10-15 확인) | 배포·모듈 간 호출·WebtoB 연동의 전제 | 현업(WAS 담당) |
| 2 | JEUS 의 Jakarta(Servlet 5+) 지원 — TmaxSoft 문서상 JEUS 9 는 Jakarta EE 9 인증 (2026-10-14), 서버 실측은 미확인 | 미지원이면 WAR 가 조용히 404 → 대안 필요 | 직접 확인 (STEP 1-3) |
| 3 | ~~나머지 5개 모듈 실행 위치~~ → **결정 (2026-10-12): 같은 atms01 에 context-path 분리** | — | — |
| 4 | ~~서버 전용 설정 주입 방법~~ → **결정: WAR 안 `jeus-common.yml` + web.xml 프로파일 `jeus`**. JVM 옵션은 공통값(`-Djeus.*`)만, `-Dspring.profiles.active` 금지, 힙 1~2G·Metaspace 512m 이상 요청 | — | 현업(WAS 담당)에 JVM 옵션 요청 |
| 5 | DB 호스트·포트 (접속 방식은 **Spring 커넥션풀로 결정**, 2026-10-12) | 설정·방화벽 | DBA |
| 6 | ATM 에이전트 진입 주소 (WebtoB 경유 / ws01 직접) 와 포트 | 에이전트 주소는 고정이라 바꿀 수 없음 | 인프라 |
| 7 | ~~WebtoB 에 `/api/batch/` 추가~~ → 게이트웨이로 불필요 (`/api/` 하나로 충분) | — | — |
| 8 | 앱 로그 위치: `/WASlogs/jeusATMSlogs/nbatmsltws01/app/` 사용 가능 여부 (계획) | 은행 로그 표준 | 인프라 |
| 9 | `/nhod`(50G) 용도 | 대용량 데이터 위치 | 인프라 |
| 10 | 6600 / 6700 / 6800 용도 | 포트 충돌 방지 | 인프라 |
| 11 | ~~WebtoB 서버 접속 계정·설치 경로~~ → **확인 (2026-10-15): atmswas 접속 가능, `/WEBTOB/webtobATMS`.** 남은 것: `http.m` 수정은 WebtoB 담당 | `http.m` 수정 | WebtoB 담당 |
| 14 | 2호기 `nbatmsltws02` / `nbatmsltwb02` 의 IP·용도 (domain.xml 에 등록) | 이중화 여부·향후 방화벽 | WAS 담당 |
| 15 | 서비스 IP 확정 (WAS 16.88.122.113, WebtoB 16.88.122.112 로 잠정) | Gitea/Jenkins URL·방화벽 | 인프라 |
| 12 | `/home/atmswas` 디스크 증설 — **공용 10G 확인 (2026-10-14)** | 배포킷·Podman·Jenkins·백업이 모두 여기로 옴 (5~10GB) | 인프라 |
| 13 | ~~atmswas 접속 시 `XDG_RUNTIME_DIR`~~ → **확인 (2026-10-14): `/run/user/50219`, Podman rootless 정상** | — | — |
| 16 | WebtoB 버전·URL Rewrite(internal) 지원 | 지원되면 web 게이트웨이 없이 WebtoB 가 모듈별로 분배 (성능·안정성 개선, `gateway.enabled: false`) | WebtoB 담당 |
| 17 | JEUS WebtoB 커넥터·HTTP 리스너(8766) thread-pool 최소·최대 | 게이트웨이·모듈 간 호출은 요청당 스레드 2개 → 같은 풀이면 고갈 시 서버 멈춤 (Tomcat 재현) | WAS 담당 / `domain.xml` 직접 확인 |

상세: [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) 12장 (게이트웨이 대안·성능 실측·단독 배포).

> 확인되면 이 문서의 **[미확인]** 표기를 고치고, 관련 절차 문서도 함께 갱신한다.
