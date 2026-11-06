# NH-ATMS CI/CD 구축 운영 매뉴얼

> **버전**: 2.0
> **작성일**: 2026-06-02
> **대상 서버**: 192.168.28.21 (ap-server, CentOS 7)
> **문서 성격**: 처음 구축하는 사람이 위에서부터 그대로 따라 하면 완성되는 단계별 안내서

---

## 0. 이 문서를 읽는 법

- 이 문서는 **위에서 아래로 순서대로** 따라 하도록 작성되었습니다. 단계를 건너뛰지 마세요.
- 명령어 블록은 **복사해서 그대로 실행**할 수 있습니다. 단, `<...>` 로 표시된 부분은 본인 환경의 실제 값으로 바꿔야 합니다.
- 각 단계 끝에는 **"성공 기준"** 이 있습니다. 그 결과가 나오지 않으면 다음 단계로 넘어가지 마세요.
- 단계 중간에 아래와 같은 박스가 나오면, 그 단계에서 **실제로 발생했던 문제와 해결법**입니다. 같은 증상이 나타나면 참고하세요.

> 🔧 **트러블슈팅 박스 예시**
> **증상** → **원인** → **해결** 순서로 정리되어 있습니다.

---

## 1. 요약 (TL;DR)

NH-ATMS는 Spring Boot 백엔드 6개 + React 프론트엔드 1개로 구성된 멀티모듈 프로젝트다.
이 프로젝트를 **Docker 컨테이너로 빌드**하고, **GitLab에 코드를 push 하면 Jenkins가 자동으로 서버에 배포**하는 CI/CD 파이프라인을 구축한다.

최종 동작:

```
개발자가 git push origin main 실행
   → GitLab이 Jenkins에 "코드 바뀜" 신호(Webhook) 전송
   → Jenkins가 서버에 SSH 접속
   → 서버에서 git pull → docker compose build → docker compose up -d 자동 실행
   → 서비스 자동 재배포 완료
```

---

## 2. 사전 지식 (개념 먼저 이해하기)

처음 접하는 사람을 위해 핵심 용어를 먼저 설명한다. 이미 알고 있다면 3장으로 넘어가도 된다.

| 용어 | 한 줄 설명 |
|------|-----------|
| **CI/CD** | 코드 변경을 자동으로 빌드·테스트·배포하는 자동화 파이프라인 |
| **Docker** | 애플리케이션을 "컨테이너"라는 격리된 상자에 담아 어디서든 동일하게 실행하는 기술 |
| **Docker Compose** | 여러 컨테이너를 `docker-compose.yml` 파일 하나로 묶어 관리하는 도구 |
| **이미지(Image)** | 컨테이너를 만들기 위한 "설계도/템플릿" (Dockerfile로 빌드) |
| **Jenkins** | 빌드·배포를 자동으로 수행해 주는 자동화 서버 |
| **GitLab** | 소스 코드 저장소 (사내 자체 호스팅) |
| **Webhook** | 특정 사건(코드 push 등)이 발생하면 다른 시스템에 알림을 보내는 기능 |
| **Credential** | Jenkins가 외부(GitLab, 서버)에 접속할 때 쓰는 비밀번호·키를 안전하게 보관하는 저장소 |
| **SSH 키** | 비밀번호 없이 서버에 안전하게 접속하기 위한 한 쌍의 열쇠(공개키 + 개인키) |

### 두 종류의 "접속"을 헷갈리지 말 것

이 구축에서 Jenkins는 **서로 다른 두 곳**에 접속하며, 각각 **다른 인증 수단**을 쓴다. 이걸 혼동하면 반드시 막힌다.

```
① Jenkins → GitLab (소스 코드 받기)
     인증수단: GitLab Deploy Token  (아이디/비밀번호 형태)
     Credential ID: jenkins-deploy-token

② Jenkins → 배포 서버 (실제 배포 실행)
     인증수단: SSH 개인키           (열쇠 형태)
     Credential ID: atms-server-ssh-key
```

---

## 3. 사전 조건 (Prerequisites)

### 3.1 서버 정보

| 항목 | 값 |
|------|----|
| 배포 서버 IP | `192.168.28.21` |
| OS | CentOS 7 (OpenSSH 7.4, SELinux 활성) |
| 배포 계정 | `atec` |
| 프로젝트 경로 | `/home/atec/nh-atms` |
| GitLab 주소 | `http://192.168.28.21:8180` |
| Jenkins 주소 | `http://192.168.28.21:8090` |

### 3.2 미리 설치되어 있어야 하는 것

- Docker Engine + Docker Compose V2 (`docker compose` 명령 사용 가능해야 함)
- Git
- OpenSSH Server (서버에 기본 설치됨)
- GitLab 관리자(Admin) 권한 계정 (Webhook 설정 시 필요)

### 3.3 프로젝트 파일 구조

```
NH-ATMS/
├── Jenkinsfile                          # ★ 파이프라인 정의 (저장소 루트)
├── docker-compose.yml                   # ★ 애플리케이션 서비스 7개 정의
├── .env                                 # 실제 환경변수 (절대 커밋 안 함)
├── .env.example                         # 환경변수 템플릿 (커밋 대상)
├── Backend/nh-atms-api/
│   ├── gradle.properties
│   ├── atms-web/Dockerfile
│   ├── atms-journal/Dockerfile
│   ├── atms-batch/Dockerfile
│   ├── atms-file/Dockerfile
│   ├── atms-if-internal/Dockerfile
│   └── atms-if-external/Dockerfile
├── Frontend/nh-atms-web/
│   ├── Dockerfile
│   └── nginx/default.conf               # API 프록시 + SPA 라우팅
└── infra/jenkins/                       # ★ Jenkins 서버 자체 구성
    ├── Dockerfile                        # Jenkins 커스텀 이미지
    ├── docker-compose.yml                # Jenkins 기동 설정
    ├── plugins.txt                       # 사전 설치 플러그인 목록
    ├── .env                              # Jenkins 관리자 계정 등 (커밋 안 함)
    └── casc/jenkins.yaml                 # Jenkins 자동 설정(JCasC)
```

---

## 4. 전체 아키텍처

구축이 끝나면 다음과 같은 구조가 된다.

```
[개발자 PC]
    │ git push origin main
    ▼
[GitLab  192.168.28.21:8180] ──Webhook(Secret Token)──► [Jenkins  192.168.28.21:8090]
                                                              │
                                          ┌───────────────────┴────────────────────┐
                                          │ Stage 1: Checkout                        │
                                          │   jenkins-deploy-token으로 소스 받기      │
                                          │ Stage 2: Deploy                          │
                                          │   atms-server-ssh-key로 SSH 접속          │
                                          └───────────────────┬────────────────────┘
                                                              │ SSH (atec 계정)
                                                              ▼
                                              [배포 서버  192.168.28.21]
                                                  git pull origin main
                                                  docker compose build
                                                  docker compose up -d
                                                              │
                                                              ▼
                                          ┌───────── 운영 컨테이너 7개 ──────────┐
                                          │ nh-atms-frontend       :19080         │
                                          │ nh-atms-web            :19081         │
                                          │ nh-atms-journal        :19082         │
                                          │ nh-atms-batch          :19083         │
                                          │ nh-atms-file           :19084         │
                                          │ nh-atms-if-internal    :19085         │
                                          │ nh-atms-if-external    :19086         │
                                          └───────────────────────────────────────┘
```

---

## 5. Phase 1 — 백엔드 Docker 빌드 환경 구성

### 목적
백엔드 6개 서비스를 각각 Docker 이미지로 빌드할 수 있도록 Dockerfile을 작성한다.

### 5.1 멀티스테이지 Dockerfile 작성

각 서비스 폴더에 `Dockerfile`을 만든다. 아래는 `atms-web` 기준 예시이며, 나머지 5개도 모듈명만 바꿔 동일 구조로 작성한다.

```dockerfile
# ─────────────────────────────────────────────────────────
# Stage 1 : deps — 의존성(라이브러리)만 먼저 받아 캐싱
#   소스 코드는 복사하지 않고 build.gradle만 복사 → 소스가 바뀌어도
#   의존성이 그대로면 이 레이어를 재사용하여 빌드 속도 향상
# ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS deps
WORKDIR /workspace
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle .
COPY build.gradle .
COPY atms-common/build.gradle atms-common/
COPY atms-web/build.gradle atms-web/
RUN chmod +x gradlew
RUN ./gradlew :atms-web:dependencies --no-daemon -q

# ─────────────────────────────────────────────────────────
# Stage 2 : builder — 소스 복사 후 실행 가능한 JAR 빌드
# ─────────────────────────────────────────────────────────
FROM deps AS builder
COPY atms-common/src/ atms-common/src/
COPY atms-web/src/ atms-web/src/
RUN ./gradlew :atms-web:bootJar -x test --no-daemon

# ─────────────────────────────────────────────────────────
# Stage 3 : runtime — 실제 운영용 경량 이미지 (JRE만 포함)
# ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd -r atms && useradd -r -g atms -s /sbin/nologin atms
COPY --from=builder /workspace/atms-web/build/libs/*.jar app.jar
RUN chown atms:atms app.jar
USER atms
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
```

### 5.2 gradle.properties 설정

`Backend/nh-atms-api/gradle.properties`:

```properties
# 빌드 성능
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8

# Alpine 환경 안전장치 (Jammy 사용 시 불필요하나 보조적으로 유지)
org.gradle.native=false
```

> 🔧 **트러블슈팅 [1] — Docker 빌드 시 `UnsatisfiedLinkError: libgradle-fileevents.so`**
>
> **증상** (서버에서 `docker compose build` 실행 시)
> ```
> UnsatisfiedLinkError: libgradle-fileevents.so:
> Error loading shared library ld-linux-x86-64.so.2: No such file or directory
> ```
>
> **원인**
> 처음에 베이스 이미지를 `eclipse-temurin:21-jdk-alpine`(가벼움) 으로 썼다.
> Alpine은 `musl libc`를 사용하는데, Gradle 8.x의 네이티브 파일감시 라이브러리(`libgradle-fileevents.so`)는 **glibc 전용으로 컴파일**되어 있어 Alpine에서 로드에 실패한다.
>
> **1차 시도 (실패)**
> `gradle.properties`에 `org.gradle.native=false`를 추가했으나 **해결되지 않음**.
> Gradle은 이 속성을 읽기 **전에** 이미 네이티브 라이브러리를 로드하려 시도하기 때문이다.
>
> **최종 해결**
> 6개 Dockerfile의 베이스 이미지를 **Alpine → Jammy(Ubuntu 22.04, glibc)** 로 전부 변경.
> ```dockerfile
> # ❌ 변경 전
> FROM eclipse-temurin:21-jdk-alpine AS deps
> FROM eclipse-temurin:21-jre-alpine
> # ✅ 변경 후
> FROM eclipse-temurin:21-jdk-jammy AS deps
> FROM eclipse-temurin:21-jre-jammy
> ```
> 베이스를 바꾸면 사용자 생성 명령도 Alpine 전용 → Ubuntu 계열로 바꿔야 한다.
> ```dockerfile
> # ❌ Alpine 전용
> RUN addgroup -S atms && adduser -S atms -G atms
> # ✅ Ubuntu/Debian 계열
> RUN groupadd -r atms && useradd -r -g atms -s /sbin/nologin atms
> ```
> 트레이드오프: 이미지 용량은 커지지만 빌드는 안정적이다.

**성공 기준**: 6개 서비스 모두 `BUILD SUCCESSFUL` 출력 후 이미지가 생성된다.

---

## 6. Phase 2 — 프론트엔드 빌드 환경

### 목적
React 앱을 빌드하여 nginx 컨테이너로 서빙한다. nginx는 정적 파일 서빙 + `/api` 요청을 백엔드로 프록시한다.

### 6.1 nginx 프록시 핵심 설정

`Frontend/nh-atms-web/nginx/default.conf` 의 API 프록시 부분:

```nginx
# Docker 내장 DNS 사용 — 컨테이너 재시작으로 IP가 바뀌어도 자동 추적
resolver 127.0.0.11 valid=30s ipv6=off;

location /api/ {
    # 변수에 담아 proxy_pass에 쓰면 DNS 조회를 '요청 시점'으로 미룸
    # → atms-web이 늦게 떠도 nginx가 먼저 기동 가능
    set $backend http://atms-web:8080;
    proxy_pass $backend;
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

> 🔧 **트러블슈팅 [2] — 프론트엔드 빌드 시 TypeScript 오류 (TS2339)**
>
> **증상** (Jenkins 빌드 로그, `npm run build` 단계)
> ```
> src/pages/base/BranchInfoPage.tsx(182,51): error TS2339:
>   Property 'faxNo' does not exist on type 'Branch'.
> ... (faxNo, phoneNo, zipcode, address, detailAddress, sotyNm, partnerCd 등 7개)
> ERROR: process "/bin/sh -c npm run build" did not complete successfully: exit code: 2
> ```
>
> **원인**
> 화면(`BranchInfoPage.tsx`)에서 사용하는 필드가 타입 정의(`branchApi.ts`의 `Branch` 인터페이스)에 선언되어 있지 않았다. TypeScript는 빌드 시점에 타입을 엄격히 검사하므로 정의되지 않은 속성 접근을 오류로 처리한다.
>
> **해결** — `Frontend/nh-atms-web/src/api/branchApi.ts`
> ```typescript
> export interface Branch {
>   termGroupId: string;
>   branchCd: string;
>   branchName: string;
>   branchTypeCd?: string;
>   termGroupName?: string;
>   // ↓ 누락되어 있던 필드 7개 추가 (모두 optional)
>   faxNo?: string;
>   phoneNo?: string;
>   zipcode?: string;
>   address?: string;
>   detailAddress?: string;
>   sotyNm?: string;
>   partnerCd?: string;
> }
> ```
>
> **교훈**: 로컬에서 `vite` 개발 서버는 타입 오류가 있어도 동작하지만, 운영 빌드(`tsc -b && vite build`)는 타입 오류 시 **빌드 자체가 실패**한다. CI에서 처음 잡히는 경우가 많다.

**성공 기준**: `npm run build` 가 오류 없이 완료되고 nginx 이미지가 생성된다.

---

## 7. Phase 3 — 서버 배포 환경 설정

### 목적
서버에 이미 다른 서비스가 떠 있는 상황에서 포트 충돌을 피하고, 외부 접속 시 CORS 문제가 없도록 설정한다.

### 7.1 포트 충돌 회피 (.env 오버라이드)

`docker-compose.yml`은 수정하지 않고, 서버의 `.env` 파일에서 포트만 19xxx 대역으로 바꾼다.

| 서비스 | 기본 포트 | 서버 적용 포트 | 충돌 원인 |
|--------|----------|--------------|----------|
| frontend | 80 | **19080** | 기존 nginx 사용 중 |
| atms-web | 8080 | **19081** | halyk_api_ezcams 사용 중 |
| atms-journal | 8081 | **19082** | - |
| atms-batch | 8082 | **19083** | - |
| atms-file | 8083 | **19084** | globus_api_ezcams 사용 중 |
| atms-if-internal | 8084 | **19085** | - |
| atms-if-external | 8085 | **19086** | - |

서버 `.env` 예시:

```env
FRONTEND_PORT=19080
ATMS_WEB_PORT=19081
ATMS_JOURNAL_PORT=19082
ATMS_BATCH_PORT=19083
ATMS_FILE_PORT=19084
ATMS_IF_INTERNAL_PORT=19085
ATMS_IF_EXTERNAL_PORT=19086
```

`docker-compose.yml` 은 다음처럼 환경변수 기본값을 받도록 작성되어 있어야 한다.

```yaml
ports:
  - "${FRONTEND_PORT:-80}:80"      # .env에 값이 없으면 80 사용
  - "${ATMS_WEB_PORT:-8080}:8080"
```

### 7.2 CORS 허용 Origin 외부화

> 🔧 **트러블슈팅 [3] — 외부에서 로그인 시 403, 백엔드 로그가 전혀 안 남음**
>
> **증상** (nginx 로그)
> ```
> "POST /api/auth/login HTTP/1.1" 403 ... "http://192.168.28.21:19080/login"
> ```
> atms-web 컨테이너에는 **로그가 전혀 남지 않음** (컨트롤러까지 도달하지 못함).
>
> **원인 분석 (요청 흐름 추적)**
> ```
> 브라우저 (http://192.168.28.21:19080)
>   │ POST /api/auth/login, Origin: http://192.168.28.21:19080
>   ▼
> nginx → atms-web:8080 (Origin 헤더 그대로 전달)
>   ▼
> Spring Security CorsFilter
>   허용 목록: [http://localhost:3000, http://localhost:5173]  ← 코드에 하드코딩
>   Origin이 목록에 없음 → 403 즉시 반환 (컨트롤러 진입 차단)
> ```
> Spring Security의 필터 체인에서 CorsFilter가 가장 먼저 동작하여, 컨트롤러/서비스 코드가 실행되기 전에 요청을 막는다. 그래서 애플리케이션 로그가 남지 않는다.
> (참고: 브라우저는 같은 주소라도 `fetch` POST 요청에 항상 Origin 헤더를 붙이며, nginx가 이를 그대로 전달한다.)
>
> **해결** — 허용 Origin을 코드에서 빼고 환경변수로 주입하도록 변경.

**① `SecurityConfig.java` 수정** (atms-common)

```java
@Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
private String corsAllowedOrigins;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // 쉼표로 구분된 문자열을 분리하여 목록으로 변환
    List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .toList();
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

**② `application.yml` 기본값 추가**

```yaml
cors:
  allowed-origins: "http://localhost:3000,http://localhost:5173"
```

**③ `docker-compose.yml` 의 atms-web에 환경변수 추가**

```yaml
environment:
  CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:3000,http://localhost:5173}
```

**④ 서버 `.env` 에 실제 접속 URL 입력**

```env
CORS_ALLOWED_ORIGINS=http://192.168.28.21:19080
```

**⑤ atms-web만 재빌드·재시작**

```bash
cd /home/atec/nh-atms
git pull origin main
docker compose build atms-web
docker compose up -d atms-web
```

**성공 기준**: 브라우저에서 로그인 시 200 응답, atms-web 로그에 요청이 기록됨.

---

## 8. Phase 4 — Jenkins 인프라 구축

### 목적
Jenkins를 Docker 컨테이너로 띄운다. 이 Jenkins 이미지는 ① Docker CLI ② 필수 플러그인 ③ 자동 설정(JCasC)을 포함한다.

### 8.1 Jenkins 커스텀 이미지 구성

`infra/jenkins/Dockerfile` 핵심:

```dockerfile
FROM jenkins/jenkins:lts-jdk21
USER root
# Docker CLI 설치 — Jenkins가 호스트 Docker를 제어할 수 있도록
RUN apt-get update && apt-get install -y ca-certificates curl gnupg lsb-release && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
      https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
      > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y docker-ce-cli && rm -rf /var/lib/apt/lists/*
RUN groupadd -f docker && usermod -aG docker jenkins
USER jenkins
# 플러그인 사전 설치
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
# 자동 설정(JCasC)
COPY casc/jenkins.yaml /var/jenkins_casc/jenkins.yaml
ENV CASC_JENKINS_CONFIG=/var/jenkins_casc/jenkins.yaml
```

### 8.2 필수 플러그인 (plugins.txt)

```text
workflow-aggregator      # Declarative Pipeline 통합 패키지
pipeline-stage-view      # 파이프라인 단계 시각화
git                      # Git SCM 체크아웃
gitlab-plugin            # GitLab Webhook 수신 + 빌드 상태 보고
credentials              # 자격증명 저장소
credentials-binding      # 파이프라인에 시크릿 주입
plain-credentials        # 아이디/비밀번호·시크릿 텍스트 타입
ssh-agent                # ★ sshagent() 스텝 — 서버 SSH 접속에 필수
ws-cleanup               # cleanWs() 워크스페이스 정리
build-timeout
timestamper
configuration-as-code    # JCasC (YAML 자동 설정)
```

> ⚠️ `ssh-agent` 가 빠지면 나중에 Deploy 단계가 **아무 로그 없이 실패**한다 (트러블슈팅 [8] 참고).

### 8.3 Jenkins 기동

`infra/jenkins/.env` 에 관리자 계정을 설정한다.

```env
JENKINS_ADMIN_ID=admin
JENKINS_ADMIN_PASSWORD=<관리자_비밀번호>
JENKINS_URL=http://192.168.28.21:8090/
JENKINS_ADMIN_EMAIL=admin@nh-atms.local
```

기동 명령:

```bash
cd /home/atec/nh-atms/infra/jenkins

# 이미지 빌드 (plugins.txt 변경 시 반드시 --no-cache)
docker compose build --no-cache

# 기동
docker compose up -d

# 기동 로그 확인 (완료까지 1~2분 소요)
docker compose logs -f jenkins
```

**성공 기준**: 로그에 `Jenkins is fully up and running` 출력 → `Ctrl+C` 로 로그 종료 → 브라우저에서 `http://192.168.28.21:8090` 접속 후 admin 로그인 성공.

> 🔧 **트러블슈팅 [4] — Jenkins가 무한 재빌드/재시작 반복**
>
> **증상**: `docker compose up` 후 Jenkins가 계속 재시작되며 정상 기동하지 못함.
>
> **원인**: JCasC(`casc/jenkins.yaml`)에 잘못된 설정이 있으면 기동 중 크래시 → 재시작 루프에 빠진다. 실제로 겪은 사례들:
> - `securityRealm`에 `local`과 `gitLab`을 **둘 다** 선언 (하나만 허용됨)
> - 이미지에 설치되지 않은 플러그인이 요구하는 설정 항목을 참조 (`gitlabCommitAuthorization` 등)
> - 최신 Jenkins LTS에서 제거된 속성 참조 (`excludeClientIPFromCrumb`, `crumbIssuer` null 값 등)
>
> **해결**: JCasC를 **최소 구성**으로 유지한다. 로컬 관리자 계정 + 로그인 사용자 전체 권한만 둔다.
> ```yaml
> jenkins:
>   securityRealm:
>     local:
>       allowsSignup: false
>       users:
>         - id: "${JENKINS_ADMIN_ID}"
>           password: "${JENKINS_ADMIN_PASSWORD}"
>   authorizationStrategy:
>     loggedInUsersCanDoAnything:
>       allowAnonymousRead: false
> ```
> GitLab OAuth 연동 등 고급 설정은 기동이 안정된 후 UI에서 추가한다. 설정 변경 후에는 `docker compose build --no-cache` 로 다시 빌드한다.

---

## 9. Phase 5 — Jenkins Credentials 등록

### 목적
Jenkins가 ① GitLab에서 소스를 받고 ② 서버에 SSH 접속할 수 있도록 두 가지 자격증명을 등록한다.

### 9.1 [자격증명 ①] GitLab 소스 체크아웃용 — Deploy Token

> 🔧 **트러블슈팅 [5] — 어떤 토큰을 써야 하나? (PAT는 쓰지 말 것)**
>
> GitLab에는 여러 종류의 토큰이 있다. CI/CD 체크아웃 용도로는 무엇이 맞는가?
>
> | 방식 | 범위 | 계정 종속 | 적합성 |
> |------|------|-----------|--------|
> | Personal Access Token (PAT) | 개인 계정 전체 | ✅ 종속 | ❌ 개인 계정 삭제/비번 변경 시 CI 중단 |
> | Group Access Token | 그룹 전체 | ❌ | ⚠️ GitLab Premium 이상 필요 |
> | Project Access Token | 프로젝트 1개 | ❌ | ✅ 가능 |
> | **Deploy Token** | 프로젝트 1개 (읽기 전용) | ❌ | ✅ **가장 권장** |
>
> **결론**: 개인 계정에 종속되지 않고 읽기 전용 권한만 가지는 **Deploy Token**을 사용한다.

**① GitLab에서 Deploy Token 발급**

```
GitLab → nh/nh-atms 프로젝트 → Settings → Repository → Deploy tokens → Add new deploy token

  Name     : jenkins-deploy-token
  Username : jenkins
  Scopes   : ☑ read_repository
→ Create deploy token
  → 생성된 Username(jenkins)과 Token 값(gldt-...)을 복사해 둔다 (창 닫으면 재확인 불가)
```

**② Jenkins에 등록**

```
Manage Jenkins → Credentials → (global) → Add Credentials

  Kind     : Username with password
  Username : jenkins                      ← Deploy Token의 Username
  Password : <발급받은 Deploy Token 값>
  ID       : jenkins-deploy-token         ← Pipeline Job 설정에서 참조
→ Create
```

### 9.2 [자격증명 ②] 배포 서버 SSH 접속용 — SSH 개인키

> 🔧 **트러블슈팅 [6] — `sshagent`의 역할과 흔한 착각**
>
> `sshagent`는 **Jenkins가 배포 서버에 SSH 접속할 때 쓸 개인키를 꺼내주는** 도구다. **GitLab과는 무관**하다.
>
> 흔한 착각: "GitLab Deploy Token(jenkins-deploy-token)으로 서버에도 접속하면 되지 않나?" → **안 된다.**
> - `jenkins-deploy-token` = GitLab 저장소 읽기용 (아이디/비밀번호)
> - 서버 SSH 접속 = 별도의 **SSH 개인키**가 필요
>
> 또한 SSH 키는 **Jenkins 컨테이너 내부**에서 생성해야 한다. SSH 연결을 시작하는 주체가 Jenkins 컨테이너이기 때문이다. (서버에서 만든 키를 쓰면 안 됨)

**Step 1 — Jenkins 컨테이너 내부에서 키 생성**

```bash
# 서버에서 Jenkins 컨테이너 접속
docker exec -it nh-atms-jenkins bash

# 키 저장 폴더 생성
mkdir -p /var/jenkins_home/.ssh

# SSH 키 쌍 생성 (패스프레이즈 없이 Enter 3번)
ssh-keygen -t ed25519 -C "jenkins-deploy" -f /var/jenkins_home/.ssh/atms_deploy_key

# 공개키 출력 → 복사 (Step 2에서 사용)
cat /var/jenkins_home/.ssh/atms_deploy_key.pub

# 개인키 출력 → 복사 (Step 4에서 사용)
cat /var/jenkins_home/.ssh/atms_deploy_key
```

**Step 2 — 배포 서버(atec 계정)에 공개키 등록**

컨테이너에서 `exit` 후 서버에서 실행:

```bash
mkdir -p /home/atec/.ssh && chmod 700 /home/atec/.ssh

# ⚠️ 반드시 >> (이어붙이기) 사용. > (덮어쓰기)는 기존 키를 모두 삭제하므로 절대 금지
echo '<Step1에서_복사한_공개키>' >> /home/atec/.ssh/authorized_keys
chmod 600 /home/atec/.ssh/authorized_keys
```

> ⚠️ **운영 중인 서버 주의**: `authorized_keys`에는 다른 서비스(gitlab-runner, wms-deploy 등)의 키가 이미 있을 수 있다. `>` 로 덮어쓰면 그 키들이 전부 사라져 기존 자동화가 끊긴다. 반드시 `>>` 로 추가하고, 추가 후 `cat`으로 기존 키가 그대로 있는지 확인한다.

**Step 3 — 접속 테스트 (키 인증 검증)**

```bash
docker exec -it nh-atms-jenkins bash

ssh -i /var/jenkins_home/.ssh/atms_deploy_key \
    -o StrictHostKeyChecking=no \
    -o PasswordAuthentication=no \
    atec@192.168.28.21 "echo 접속성공"
```

`-o PasswordAuthentication=no` 는 비밀번호 폴백을 막아 **키 인증만으로 성공하는지** 확실히 검증한다.

**성공 기준**: 비밀번호 요청 없이 `접속성공` 출력.
실패한다면 → **트러블슈팅 [9]** (Permission denied)로 이동.

**Step 4 — Jenkins에 개인키 등록**

```
Manage Jenkins → Credentials → (global) → Add Credentials

  Kind        : SSH Username with private key
  ID          : atms-server-ssh-key       ← Jenkinsfile에서 참조
  Username    : atec                       ← 서버 접속 계정
  Private Key : ☑ Enter directly → Add
                → 개인키 전체 붙여넣기
                  (-----BEGIN OPENSSH PRIVATE KEY----- 부터
                   -----END OPENSSH PRIVATE KEY----- 까지 + 마지막 빈 줄 1개)
→ Create
```

> 🔧 **트러블슈팅 [7] — `Error loading key: error in libcrypto`**
>
> **증상** (Jenkins 빌드 로그)
> ```
> Error loading key "...private_key_xxx.key": error in libcrypto
> ERROR: Failed to run ssh-add
> ```
> **원인**: Credential에 개인키를 붙여넣을 때 내용이 손상됨 (줄이 잘리거나 마지막 줄바꿈 누락). 키 파일 자체는 정상이다.
> **해결**: 해당 Credential을 **삭제 후 재등록**한다. `cat`으로 출력한 개인키를 **첫 줄(-----BEGIN)부터 마지막 줄(-----END)까지** 빠짐없이 복사하고, 텍스트박스 맨 아래에 **빈 줄 1개**를 둔다.

---

## 10. Phase 6 — Jenkins Pipeline Job 생성

### 목적
GitLab 저장소와 연결된 파이프라인 Job을 만든다.

### 10.1 Job 생성

```
Jenkins 메인 → New Item
  Item name : nh-atms
  Type      : Pipeline
→ OK
```

### 10.2 General 탭

```
☑ GitLab Connection : NH-GitLab   (JCasC로 이미 설정되어 있으면 선택)
```

### 10.3 Build Triggers 탭

```
☑ Build when a change is pushed to GitLab
   GitLab webhook URL : http://192.168.28.21:8090/project/nh-atms
   (이 URL을 메모 → Phase 7에서 GitLab에 입력)

   Advanced → Secret token → [Generate] → 생성된 토큰 복사
   (이 토큰도 메모 → Phase 7에서 GitLab Webhook에 입력)
```

> 🔧 **트러블슈팅 [8] — 브랜치 필터에 `main` 입력 시 HTTP 500**
>
> **증상**
> ```
> An internal error occurred during form field validation (HTTP 500).
> ```
> **원인**: GitLab 플러그인이 브랜치 목록을 GitLab API로 실시간 조회하려는데, `gitlab-api-token` 자격증명이 없어 API 호출에 실패한다.
>
> **해결 A (빠른 우회 — 권장)**: `Filter branches by name` 체크를 **해제**한다. 어차피 다음 Pipeline 탭의 `Branch Specifier`를 `*/main`으로 두면 main만 빌드되므로 기능상 문제없다.
>
> **해결 B (정석)**: GitLab에서 `api` + `read_repository` scope의 토큰을 발급해 Jenkins에 `Kind: GitLab API token`, `ID: gitlab-api-token`으로 등록한다.

### 10.4 Pipeline 탭

```
Definition : Pipeline script from SCM
SCM        : Git
Repository URL : http://192.168.28.21:8180/nh/nh-atms.git
Credentials    : jenkins-deploy-token      ← Phase 5에서 등록한 ①
Branch Specifier : */main
Script Path : Jenkinsfile
→ Save
```

**성공 기준**: Job이 생성되고 설정이 저장됨.

---

## 11. Phase 7 — GitLab Webhook 연동

### 목적
GitLab에 코드가 push되면 Jenkins에 자동으로 알리도록 연결한다.

### 11.1 Webhook 등록

```
GitLab → nh/nh-atms → Settings → Webhooks → Add new webhook

  URL          : http://192.168.28.21:8090/project/nh-atms   (Phase 6의 webhook URL)
  Secret token : <Phase 6에서 Generate한 Secret Token>

  Trigger:
    ☑ Push events
       ◉ Wildcard pattern
         Branch filter : main      (정확히 main만 → 와일드카드 없이 main)

  SSL verification : ☐ 비활성 (내부망 HTTP)
→ Add webhook
```

> 🔧 **트러블슈팅 [9-1] — Webhook 등록 시 `Invalid url given`**
>
> **원인**: GitLab은 내부망 IP(192.168.x.x)로 향하는 Webhook을 SSRF 공격 방지를 위해 기본 차단한다.
> **해결** (GitLab 관리자 권한 필요)
> ```
> GitLab → Admin Area → Settings → Network → Outbound requests
>   ☑ Allow requests to the local network from webhooks and services
> → Save changes
> ```

> 🔧 **트러블슈팅 [9-2] — Webhook 테스트 시 `HTTP 403 anonymous is missing the Job/Build permission`**
>
> **증상**
> ```
> Hook executed successfully but returned HTTP 403
> anonymous is missing the Job/Build permission
> ```
> **원인**: GitLab Webhook이 인증 없이(anonymous) 요청을 보내는데, Jenkins가 익명 빌드 트리거를 차단한다.
> **해결**: Jenkins와 GitLab이 **Secret Token**을 공유하여 인증을 대체한다.
> 1. Jenkins Job → Configure → Build Triggers → Advanced → Secret token → Generate → 복사
> 2. GitLab Webhook → Edit → Secret token에 붙여넣기 → Save
> 3. 다시 Test → Push events

### 11.2 연결 테스트

```
GitLab Webhooks 목록 → Test → Push events
```

**성공 기준**: `Hook executed successfully: HTTP 200` 출력.

---

## 12. Phase 8 — Jenkinsfile 작성

### 목적
파이프라인의 실제 동작(체크아웃 → 배포)을 코드로 정의한다. 이 파일은 **저장소 루트**에 둔다.

### 12.1 Jenkinsfile 전문

```groovy
// =====================================================================
// Jenkinsfile — NH-ATMS CI/CD Pipeline
// 트리거 : GitLab push(main) → Jenkins Webhook
// 동작   : SSH로 배포 서버 접속 → git pull → docker compose build → up
//
// 필수 Credentials:
//   jenkins-deploy-token : GitLab SCM 체크아웃 (Username with password)
//   atms-server-ssh-key  : 배포 서버 SSH 접속 (SSH Username with private key, Username: atec)
// =====================================================================
pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')   // 30분 초과 시 강제 실패
        disableConcurrentBuilds()            // 동시 빌드 방지 (배포 충돌 방지)
        buildDiscarder(logRotator(numToKeepStr: '10'))  // 최근 10개 빌드만 보관
    }

    environment {
        DEPLOY_HOST = '192.168.28.21'
        DEPLOY_USER = 'atec'
        DEPLOY_DIR  = '/home/atec/nh-atms'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                }
                echo " 브랜치 : ${env.GIT_BRANCH}"
                echo " 커밋   : ${env.GIT_COMMIT_SHORT}"
            }
        }

        stage('Deploy') {
            steps {
                sshagent(credentials: ['atms-server-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no \\
                            -o ConnectTimeout=10 \\
                            ${DEPLOY_USER}@${DEPLOY_HOST} '
                            export PATH=/usr/local/bin:/usr/bin:/bin:\$PATH
                            set -e

                            echo "===== [1/3] 소스 동기화 ====="
                            cd ${DEPLOY_DIR}
                            git pull origin main

                            echo "===== [2/3] Docker 빌드 ====="
                            docker compose build

                            echo "===== [3/3] 서비스 재시작 ====="
                            docker compose up -d

                            echo "===== 배포 완료 ====="
                            docker compose ps
                        '
                    """
                }
            }
        }
    }

    post {
        success { echo "✅ 배포 성공 | 커밋: ${env.GIT_COMMIT_SHORT}" }
        failure { echo "❌ 배포 실패 | Jenkins 콘솔 로그를 확인하세요" }
        always  { cleanWs() }
    }
}
```

### 12.2 작성 포인트 설명

| 부분 | 의미 |
|------|------|
| `agent any` | 아무 Jenkins 노드에서나 실행 |
| `disableConcurrentBuilds()` | 배포 중 또 다른 배포가 겹치는 것을 방지 |
| `environment { }` | 서버 접속 정보. **본인 서버에 맞게 수정** (`DEPLOY_USER`, `DEPLOY_DIR`) |
| `sshagent(credentials: ['atms-server-ssh-key'])` | 이 블록 안에서만 SSH 개인키 사용 |
| `export PATH=...` | 비대화형 SSH는 PATH가 축소되어 docker/git을 못 찾을 수 있어 명시 |
| `set -e` | 명령 하나라도 실패하면 즉시 중단 (실패를 빌드 실패로 전파) |

> 🔧 **트러블슈팅 [10] — Deploy 단계가 아무 로그 없이 실패**
>
> 이 문제는 **여러 원인이 순차적으로** 나타났다. 단계별 진단이 핵심이었다.
>
> **진단 방법**: Deploy 스테이지에 단계별 echo를 심어 어디까지 진행되는지 격리한다.
> ```groovy
> stage('Deploy') {
>   steps {
>     echo "[STEP A] Deploy 스테이지 진입"
>     sshagent(credentials: ['atms-server-ssh-key']) {
>       echo "[STEP B] sshagent 블록 진입 (Credential 로드 성공)"
>       sh 'echo "[STEP C] sh 스텝 실행 확인"'
>       sh 'which ssh && echo "[STEP D] ssh 바이너리 존재"'
>       sh "ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} 'echo [STEP E] 접속 성공'"
>     }
>   }
> }
> ```
> 마지막으로 출력된 STEP 번호로 원인을 특정한다.
>
> **원인 1 — STEP A까지만 출력 → `ssh-agent` 플러그인 미설치**
> `sshagent` 스텝 자체를 Jenkins가 모름. → `plugins.txt`에 `ssh-agent` 추가 후 재빌드, 또는 UI에서 SSH Agent 플러그인 설치.
>
> **원인 2 — STEP A까지만 출력 (플러그인 설치 후에도) → Credential ID 불일치**
> Jenkinsfile에 적은 ID(`server-ssh-key`)가 실제 등록된 ID(`atms-server-ssh-key`)와 달라서 `sshagent`가 키를 못 찾음. → ID를 정확히 일치시킨다.

**성공 기준**: Console Output에 `[1/3] → [2/3] → [3/3] → 배포 완료 → ✅ 배포 성공` 출력.

---

## 13. Phase 9 — SSH 키 인증 완성 (Permission denied 해결)

### 목적
Step 3 접속 테스트(9.2)나 실제 Deploy에서 `Permission denied`가 나는 경우, 원인을 순서대로 제거한다.
실제 구축에서 **네 가지 원인이 차례로** 나타났으므로, 위에서부터 하나씩 확인한다.

> 🔧 **트러블슈팅 [11] — `atec@192.168.28.21: Permission denied (publickey,...)`**
>
> 상세 로그를 먼저 확보한다 (Jenkins 컨테이너 내부).
> ```bash
> ssh -vvv -i /var/jenkins_home/.ssh/atms_deploy_key \
>     -o PasswordAuthentication=no \
>     atec@192.168.28.21 "echo test"
> ```
> 로그에 `Offering public key ...` 다음 `Permission denied`가 나오면 **서버가 키를 거부**하는 것이다. 아래 원인을 순서대로 점검한다.

**원인 1 — 공개키/개인키가 실제로 일치하는지 (핑거프린트 대조)**

```bash
# Jenkins 컨테이너: 개인키에서 공개키를 도출
ssh-keygen -y -f /var/jenkins_home/.ssh/atms_deploy_key

# 또는 핑거프린트로 비교
ssh-keygen -l -f /var/jenkins_home/.ssh/atms_deploy_key

# 서버: authorized_keys에 등록된 키들의 핑거프린트
ssh-keygen -l -f /home/atec/.ssh/authorized_keys
```
두 SHA256 해시가 일치해야 한다. (실제 사례에서는 일치했고, 진짜 원인은 아래 3·4였다.)

**원인 2 — authorized_keys 줄바꿈 오염 (Windows에서 복사 시)**

```bash
# ^M 이 보이면 Windows 줄바꿈(CRLF) 혼입 → 키 인식 실패
cat -A /home/atec/.ssh/authorized_keys | grep '\^M'
# 발견 시 제거
sed -i 's/\r$//' /home/atec/.ssh/authorized_keys
```

**원인 3 — 홈 디렉터리 권한이 너무 개방적 (775)**

```bash
stat /home/atec | grep Access
# (0775/drwxrwxr-x) 처럼 그룹에 w(쓰기)가 있으면 SSH가 키 인증을 거부한다
```

| 명령 | 동작 | 위험 |
|------|------|------|
| `chmod 755 /home/atec` | 그룹 쓰기(w)만 제거 | ✅ 안전 |

```bash
chmod 755 /home/atec
stat /home/atec | grep Access   # (0755/drwxr-xr-x) 확인
```

> **영향도 분석 (왜 안전한가)**
> - `atec` 본인은 소유자(Owner) 권한 `rwx`라 변화 없음 → 읽기·쓰기 정상
> - 하위 디렉터리/파일(`nh-atms/`, `.ssh/` 등)은 변경되지 않음 → 컨테이너 동작 영향 없음
> - 실제로 제한되는 건 "같은 그룹의 다른 계정이 `/home/atec` **루트**에 직접 파일 생성"뿐 (거의 발생하지 않음)
> - 리눅스 홈 디렉터리 표준 권한이 755이며, 775가 오히려 비정상이다.

**원인 4 — SELinux 컨텍스트 오류 (CentOS 7의 핵심 원인)**

```bash
ls -Z /home/atec/.ssh/authorized_keys
```
| 컨텍스트 | 의미 |
|----------|------|
| `system_u:object_r:ssh_home_t:s0` | ✅ 정상 (SSH가 신뢰) |
| `unconfined_u:object_r:httpd_sys_content_t:s0` | ❌ 웹서버용으로 잘못 지정 → SSH 거부 |

실제 사례에서 `httpd_sys_content_t`로 되어 있었다. 복원:

```bash
sudo restorecon -Rv /home/atec/.ssh/
ls -Z /home/atec/.ssh/authorized_keys   # ssh_home_t 로 바뀌었는지 확인
```

> **`restorecon`의 효과와 영향도**
> - SELinux 컨텍스트 **레이블만** 정책 기본값으로 복원한다.
> - 파일 내용·권한(chmod)·소유자(chown)는 **전혀 바뀌지 않는다.**
> - `/home/atec/.ssh/` 만 대상이며 다른 서비스·컨테이너·웹서버에 **영향 없다.**

**원인 분석 보조 — 서버 SSH 거부 로그**

```bash
sudo tail -30 /var/log/secure
# Jenkins 컨테이너 IP(예: 172.28.0.2)에서
#   "Failed password" / "Connection closed ... [preauth]" 가 보이면 키 인증 실패 신호
```

**성공 기준**: 9.2 Step 3 재실행 시 비밀번호 없이 `접속성공` 출력.

---

## 14. Phase 10 — 최초 배포 실행 및 검증

### 14.1 파이프라인 트리거

```bash
# 로컬에서 빈 커밋으로 트리거
git commit --allow-empty -m "test: CI/CD end-to-end 테스트"
git push origin main
```

### 14.2 Jenkins에서 확인

```
http://192.168.28.21:8090/job/nh-atms/
→ 최신 빌드 번호 클릭 → Console Output
```

기대 로그:

```
 브랜치 : origin/main
 커밋   : xxxxxxx
===== [1/3] 소스 동기화 =====
Already up to date.
===== [2/3] Docker 빌드 =====
... BUILD SUCCESSFUL (백엔드 6개) + npm run build (프론트)
===== [3/3] 서비스 재시작 =====
===== 배포 완료 =====
NAME                    STATUS
nh-atms-frontend        Up
nh-atms-web             Up
...
✅ 배포 성공 | 커밋: xxxxxxx
Finished: SUCCESS
```

### 14.3 서버에서 최종 확인

```bash
ssh atec@192.168.28.21
cd /home/atec/nh-atms
docker compose ps          # 7개 컨테이너 Up 확인
```

브라우저에서 `http://192.168.28.21:19080` 접속 → 로그인 정상 동작 확인.

**성공 기준**: 모든 컨테이너 `Up`, 웹 로그인 성공.

---

## 15. 명령어 빠른 참조표

### Jenkins 인프라

| 명령어 | 설명 | 사용 시점 |
|--------|------|----------|
| `docker compose build --no-cache` | Jenkins 이미지 재빌드 | plugins.txt / JCasC 변경 후 |
| `docker compose up -d` | Jenkins 기동 | 최초/재시작 |
| `docker compose logs -f jenkins` | 실시간 로그 | 기동 확인 |
| `docker exec -it nh-atms-jenkins bash` | 컨테이너 내부 접속 | SSH 키 생성·점검 |

### SSH 키 관리

| 명령어 | 설명 | 사용 시점 |
|--------|------|----------|
| `ssh-keygen -t ed25519 -C "jenkins-deploy" -f <경로>` | 키 쌍 생성 | 최초 1회 |
| `ssh-keygen -y -f <개인키>` | 개인키→공개키 도출 | 키 일치 확인 |
| `ssh-keygen -l -f <파일>` | 핑거프린트 확인 | 불일치 진단 |
| `ssh -vvv -i <개인키> -o PasswordAuthentication=no <user>@<host>` | 상세 디버그 접속 | 거부 원인 분석 |
| `cat <공개키> >> ~/.ssh/authorized_keys` | 공개키 추가(기존 유지) | 서버 등록 |

### 배포(애플리케이션) 관리

| 명령어 | 설명 | 사용 시점 |
|--------|------|----------|
| `docker compose ps` | 컨테이너 상태 | 배포 후 |
| `docker compose build` | 변경분만 재빌드 | 소스 변경 후 |
| `docker compose up -d` | 전체 기동 | 배포 |
| `docker compose up -d --no-deps atms-web` | 특정 서비스만 재시작 | 단일 서비스 갱신 |
| `docker compose logs -f <서비스>` | 서비스 로그 추적 | 오류 확인 |
| `docker compose down` | 전체 종료 | 점검 |

### SELinux / 권한 (CentOS 7)

| 명령어 | 설명 | 사용 시점 |
|--------|------|----------|
| `getenforce` | SELinux 상태 | 키 인증 실패 시 |
| `ls -Z <파일>` | SELinux 컨텍스트 확인 | 컨텍스트 의심 시 |
| `restorecon -Rv /home/atec/.ssh/` | SSH 컨텍스트 복원 | httpd_sys_content_t 오류 |
| `stat <경로> \| grep Access` | 권한 확인 | 권한 점검 |
| `sudo tail -30 /var/log/secure` | SSH 인증 로그 | 거부 원인 분석 |

---

## 16. 롤백 절차

### 16.1 이전 커밋으로 되돌리기

```bash
ssh atec@192.168.28.21
cd /home/atec/nh-atms
git log --oneline -10            # 되돌릴 커밋 해시 확인
git checkout <COMMIT_HASH>
docker compose build
docker compose up -d
```

### 16.2 특정 서비스만 긴급 롤백

```bash
docker compose stop atms-web
docker tag nh-atms-atms-web:previous nh-atms-atms-web:latest
docker compose up -d atms-web
```

### 16.3 authorized_keys 롤백

```bash
# ⚠️ 변경 전 반드시 백업
cp /home/atec/.ssh/authorized_keys /home/atec/.ssh/authorized_keys.bak

# Jenkins 키만 제거 (다른 키 보존)
grep -v "jenkins-deploy" /home/atec/.ssh/authorized_keys > /tmp/ak_tmp
mv /tmp/ak_tmp /home/atec/.ssh/authorized_keys
chmod 600 /home/atec/.ssh/authorized_keys
```

### 16.4 CORS 설정 롤백

```bash
# 서버 .env에서 CORS_ALLOWED_ORIGINS 제거 (기본값 localhost로 복원)
docker compose up -d --no-deps atms-web
```

---

## 17. Credentials 및 보안 정리

| Credential ID | Kind | 용도 | 보관 위치 |
|---------------|------|------|----------|
| `jenkins-deploy-token` | Username with password | GitLab 소스 체크아웃 | Jenkins Global |
| `atms-server-ssh-key` | SSH Username with private key | 배포 서버 SSH 접속 (atec) | Jenkins Global |
| (선택) `gitlab-api-token` | GitLab API token | 브랜치 조회/상태 보고 | Jenkins Global |

보안 주의:
- `.env`, `infra/jenkins/.env`, `application-local.yml`, SSH 개인키는 **절대 Git에 커밋하지 않는다** (`.gitignore` 확인).
- Jenkins는 `/var/run/docker.sock`을 마운트하여 호스트 Docker를 제어하므로, **Jenkins 관리자 계정 보안이 곧 호스트 보안**이다. 강한 비밀번호를 사용한다.

---

## 18. 트러블슈팅 색인 (빠른 찾기)

> 상세 해결법은 각 Phase 본문의 🔧 박스에 있다. 아래는 증상 → 위치 매핑이다.

| # | 증상(키워드) | 위치 |
|---|--------------|------|
| 1 | `UnsatisfiedLinkError: libgradle-fileevents.so` | Phase 1 (5.2) |
| 2 | `TS2339 Property does not exist on type 'Branch'` | Phase 2 (6.1) |
| 3 | 외부 로그인 403 + 백엔드 로그 없음 (CORS) | Phase 3 (7.2) |
| 4 | Jenkins 무한 재시작/재빌드 | Phase 4 (8.3) |
| 5 | 어떤 토큰? PAT vs Deploy Token | Phase 5 (9.1) |
| 6 | sshagent 역할 / GitLab 토큰으로 SSH 착각 | Phase 5 (9.2) |
| 7 | `error in libcrypto` (개인키 손상) | Phase 5 (9.2 Step4) |
| 8 | 브랜치 필터 `main` 입력 시 HTTP 500 | Phase 6 (10.3) |
| 9-1 | Webhook `Invalid url given` | Phase 7 (11.1) |
| 9-2 | Webhook `403 anonymous is missing permission` | Phase 7 (11.1) |
| 10 | Deploy 단계 로그 없이 실패 (플러그인/ID) | Phase 8 (12.2) |
| 11 | `Permission denied (publickey)` 4종 원인 | Phase 9 (전체) |

---

*이 문서는 NH-ATMS CI/CD 실제 구축 과정에서 발생한 모든 시행착오와 해결 과정을 단계별로 통합하여 작성되었습니다.*
