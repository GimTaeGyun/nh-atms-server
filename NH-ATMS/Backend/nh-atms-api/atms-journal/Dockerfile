# =====================================================================
# atms-journal | 전자저널 / 원격명령 서비스
# Build context : Backend/nh-atms-api/
# Build command : docker build -f atms-journal/Dockerfile -t atms-journal .
# =====================================================================

# ⚠ Alpine(musl libc) → Jammy(Ubuntu 22.04, glibc) 변경
#   Gradle 8.x 네이티브 라이브러리(libgradle-fileevents.so)가 glibc 전용으로
#   컴파일되어 Alpine에서 UnsatisfiedLinkError 발생 → Jammy로 전환하여 해결
FROM eclipse-temurin:21-jdk-jammy AS deps
WORKDIR /workspace

COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle .
COPY build.gradle .

# atms-web과 달리 atms-journal의 build.gradle을 복사
COPY atms-common/build.gradle atms-common/
COPY atms-journal/build.gradle atms-journal/

RUN chmod +x gradlew
RUN ./gradlew :atms-journal:dependencies --no-daemon -q

# ─────────────────────────────────────────────────────────────────────
FROM deps AS builder

COPY atms-common/src/ atms-common/src/
COPY atms-journal/src/ atms-journal/src/

RUN ./gradlew :atms-journal:bootJar -x test --no-daemon

# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r atms && useradd -r -g atms -s /sbin/nologin atms

COPY --from=builder /workspace/atms-journal/build/libs/*.jar app.jar
RUN chown atms:atms app.jar
USER atms

# atms-journal 전용 포트
EXPOSE 8081

# atms-journal은 HMAC 서명 비밀키를 환경변수로 주입받음
# 예) docker run -e JOURNAL_HMAC_SECRET=실제시크릿 ...
# application.yml의 ${JOURNAL_HMAC_SECRET:기본값} 패턴으로 읽힘
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
