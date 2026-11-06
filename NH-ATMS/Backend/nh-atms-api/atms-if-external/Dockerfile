# =====================================================================
# atms-if-external | 대외 인터페이스 서비스 (API GW / FOS / 외부 채널)
# Build context : Backend/nh-atms-api/
# Build command : docker build -f atms-if-external/Dockerfile -t atms-if-external .
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

COPY atms-common/build.gradle atms-common/
COPY atms-if-external/build.gradle atms-if-external/

RUN chmod +x gradlew
RUN ./gradlew :atms-if-external:dependencies --no-daemon -q

# ─────────────────────────────────────────────────────────────────────
FROM deps AS builder

COPY atms-common/src/ atms-common/src/
COPY atms-if-external/src/ atms-if-external/src/

RUN ./gradlew :atms-if-external:bootJar -x test --no-daemon

# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r atms && useradd -r -g atms -s /sbin/nologin atms

COPY --from=builder /workspace/atms-if-external/build/libs/*.jar app.jar
RUN chown atms:atms app.jar
USER atms

EXPOSE 8085

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
