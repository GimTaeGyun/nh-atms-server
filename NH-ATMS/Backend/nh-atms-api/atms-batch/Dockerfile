# =====================================================================
# atms-batch | 배치 / 정산 / 통계 서비스
# Build context : Backend/nh-atms-api/
# Build command : docker build -f atms-batch/Dockerfile -t atms-batch .
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
COPY atms-batch/build.gradle atms-batch/

RUN chmod +x gradlew
RUN ./gradlew :atms-batch:dependencies --no-daemon -q

# ─────────────────────────────────────────────────────────────────────
FROM deps AS builder

COPY atms-common/src/ atms-common/src/
COPY atms-batch/src/ atms-batch/src/

RUN ./gradlew :atms-batch:bootJar -x test --no-daemon

# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r atms && useradd -r -g atms -s /sbin/nologin atms

COPY --from=builder /workspace/atms-batch/build/libs/*.jar app.jar
RUN chown atms:atms app.jar
USER atms

EXPOSE 8082

# atms-batch 특이사항:
#   - Spring Batch 메타 테이블을 H2 in-memory DB에 자동 생성 (initialize-schema: always)
#   - Quartz 스케줄러도 메모리 모드로 동작
#   - 컨테이너 재시작 시 배치 실행 이력이 초기화됨 (개발/테스트 환경에 적합)
#   - 운영에서는 별도 외부 DB에 배치 메타 테이블을 두는 것을 권장
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
