# =====================================================================
# atms-file | 파일 송수신 서비스 (펌웨어 다운로드 / FTP·SFTP)
# Build context : Backend/nh-atms-api/
# Build command : docker build -f atms-file/Dockerfile -t atms-file .
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
COPY atms-file/build.gradle atms-file/

RUN chmod +x gradlew
RUN ./gradlew :atms-file:dependencies --no-daemon -q

# ─────────────────────────────────────────────────────────────────────
FROM deps AS builder

COPY atms-common/src/ atms-common/src/
COPY atms-file/src/ atms-file/src/

RUN ./gradlew :atms-file:bootJar -x test --no-daemon

# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r atms && useradd -r -g atms -s /sbin/nologin atms

# atms-file 전용: 업로드/다운로드 파일 임시 저장 디렉터리 생성
# application.yml에서 100MB 파일 허용 (max-file-size: 100MB)
# 컨테이너 내부에 저장 공간이 필요하며, 볼륨 마운트로 연결하는 것을 권장
#   예) docker run -v /host/files:/app/files atms-file
RUN mkdir -p /app/files && chown -R atms:atms /app/files

COPY --from=builder /workspace/atms-file/build/libs/*.jar app.jar
RUN chown atms:atms app.jar
USER atms

EXPOSE 8083

# atms-file 특이사항:
#   -XX:MaxRAMPercentage=75.0 유지하되,
#   100MB 파일을 메모리로 읽는 작업이 발생하므로
#   docker run 시 --memory=2g 이상 권장 (컨테이너 메모리 제한 넉넉히)
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
