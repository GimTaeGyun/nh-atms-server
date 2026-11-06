# =====================================================================
# atms-web | ATMS 화면 백엔드 (REST API)
# Build context : Backend/nh-atms-api/
# Build command : docker build -f atms-web/Dockerfile -t atms-web .
# =====================================================================

# ─────────────────────────────────────────────────────────────────────
# Stage 1 : deps
#   목적  : Gradle 의존성(라이브러리)만 미리 다운로드하여 Docker 레이어로 캐싱
#   원리  : 소스 코드(.java)는 복사하지 않고 build.gradle만 복사한다.
#           → 소스가 바뀌어도 build.gradle이 안 바뀌면 이 레이어를 재사용(캐시 HIT)
#           → 매 빌드마다 수백 MB 라이브러리를 다시 받는 상황을 방지
#
#   ⚠ Alpine(musl libc) → Jammy(Ubuntu 22.04, glibc) 변경 이유:
#     Gradle 8.x의 네이티브 파일감시 라이브러리(libgradle-fileevents.so)가
#     glibc 전용으로 컴파일되어 있어 Alpine에서 UnsatisfiedLinkError 발생.
#     org.gradle.native=false 속성으로도 해결 불가(초기화 시점 문제).
#     Jammy는 glibc 기반이므로 ld-linux-x86-64.so.2 존재 → 정상 동작.
# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS deps
WORKDIR /workspace

# 1-1. Gradle Wrapper 실행 파일 복사
#      gradlew : Unix/Linux/Mac 용 실행 스크립트
#      gradle/ : wrapper 설정(버전 정보, 배포 URL) + wrapper JAR
COPY gradlew .
COPY gradle/ gradle/

# 1-2. 루트 빌드 스크립트 복사
#      settings.gradle : 멀티모듈 구성 선언 (어떤 모듈이 있는지)
#      build.gradle    : 모든 서브프로젝트 공통 설정 (Java 버전, 의존성 등)
COPY settings.gradle .
COPY build.gradle .

# 1-3. 각 모듈의 build.gradle 복사 (소스 제외)
#      atms-common 포함 이유 : atms-web이 project(':atms-common')에 의존하므로
#      Gradle이 의존성 트리를 계산하려면 common의 build.gradle도 필요
COPY atms-common/build.gradle atms-common/
COPY atms-web/build.gradle atms-web/

# 1-4. gradlew에 실행 권한 부여
#      Windows에서 복사된 파일은 실행 권한이 없을 수 있음
RUN chmod +x gradlew

# 1-5. 의존성 다운로드만 실행 (소스 컴파일 X)
#      --no-daemon  : CI 환경에서 Gradle 데몬 프로세스 불필요 (컨테이너는 1회 실행 후 종료)
#      -q           : 불필요한 로그 최소화
RUN ./gradlew :atms-web:dependencies --no-daemon -q


# ─────────────────────────────────────────────────────────────────────
# Stage 2 : builder
#   목적  : 소스 코드를 복사하고 실행 가능한 JAR(bootJar) 빌드
#   원리  : Stage 1에서 받아놓은 의존성 캐시를 그대로 이어받아 시작
#           소스 파일은 자주 바뀌므로 의존성 레이어와 분리
# ─────────────────────────────────────────────────────────────────────
FROM deps AS builder

# 2-1. 공유 라이브러리(atms-common) 소스 복사
#      atms-web이 컴파일 시 atms-common 클래스를 직접 참조하므로 반드시 필요
COPY atms-common/src/ atms-common/src/

# 2-2. atms-web 소스 복사
COPY atms-web/src/ atms-web/src/

# 2-3. bootJar 태스크 실행
#      bootJar  : Spring Boot 실행 가능한 Fat JAR 생성
#                 일반 jar와 달리 모든 의존성 라이브러리가 내장됨
#      -x test  : 테스트 스킵 (Docker 빌드 중 DB 연결 불가 등 환경 문제 회피)
#                 실제 테스트는 Jenkins 파이프라인의 별도 Test 스테이지에서 실행
RUN ./gradlew :atms-web:bootJar -x test --no-daemon


# ─────────────────────────────────────────────────────────────────────
# Stage 3 : runtime
#   목적  : 실제 운영에 사용할 최종 경량 이미지
#   원리  : JDK가 아닌 JRE만 포함 → 이미지 크기 약 100MB 절감
#           빌드 도구(Gradle, javac 등)는 운영 환경에서 불필요
# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 3-1. 비루트(non-root) 사용자 생성
#      보안 원칙: 컨테이너 내부 프로세스가 root로 실행되면
#      컨테이너 탈출 시 호스트 시스템 전체가 위험해짐
#      Ubuntu/Debian 계열: groupadd / useradd 사용
#      (Alpine의 addgroup -S / adduser -S 와 동일 목적)
RUN groupadd -r atms && useradd -r -g atms -s /sbin/nologin atms

# 3-2. Stage 2에서 빌드한 JAR만 복사
#      *.jar 와일드카드 사용 이유 : 버전 번호(0.0.1-SNAPSHOT)가 파일명에 포함되어
#      버전이 바뀔 때마다 Dockerfile을 수정하지 않아도 됨
COPY --from=builder /workspace/atms-web/build/libs/*.jar app.jar

# 3-3. 파일 소유권 변경 및 업로드 디렉토리 사전 생성
#      - app.jar : COPY는 기본적으로 root 소유로 복사되므로 명시적 변경 필요
#      - uploads/notice : Docker named volume 마운트 시 디렉토리가 root 소유로 생성되는 것을 방지.
#        이미지 레이어에 디렉토리가 존재하면 Docker가 volume 초기화 시 해당 디렉토리의
#        소유권/권한을 그대로 사용함 → atms 유저가 파일을 쓸 수 있게 됨
#      - data/atms-download/temp : 개별다운로드 업로드 ZIP 저장소(download.storage.base-dir).
#        비루트(atms) 유저가 /app/data 하위에 디렉토리를 생성할 수 있도록 사전 생성+소유권 부여.
#        (미생성 시 AccessDeniedException: /app/data 발생)
RUN chown atms:atms app.jar \
    && mkdir -p /app/uploads/notice \
    && mkdir -p /app/data/atms-download/temp \
    && chown -R atms:atms /app/uploads /app/data
USER atms

# 3-4. 컨테이너가 사용하는 포트 선언 (실제 개방은 docker run -p 로 수행)
EXPOSE 8080

# 3-5. JVM 실행 옵션
#      -XX:+UseVirtualThreads     : Java 21 가상 스레드 활성화
#                                   Spring Boot 3.5 Tomcat이 가상 스레드로 요청 처리
#                                   OS 스레드 수 제한 없이 높은 동시성 처리 가능
#      -XX:MaxRAMPercentage=75.0  : 컨테이너 메모리의 75%를 JVM 힙으로 사용
#                                   고정값(-Xmx512m) 대신 사용하는 이유:
#                                   docker run --memory=1g 설정에 자동 적응
#      -Dfile.encoding=UTF-8      : 한글 로그/파일 처리 시 인코딩 보장
#      -Dspring.profiles.active   : 기본 프로파일 (docker run -e 로 오버라이드 가능)
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
