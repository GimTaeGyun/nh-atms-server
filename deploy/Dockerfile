# ATMS Spring Boot 앱 이미지 (JRE 베이스 이미지 사용)
#  - 폐쇄망: 베이스 이미지는 미리 podman load 로 적재, build 시 --pull=never
#  - 빌드 컨텍스트: build/ (bin/build.sh 가 app.jar 를 복사)
ARG BASE_IMAGE=docker.io/library/eclipse-temurin:21-jre-jammy
FROM ${BASE_IMAGE}

ENV TZ=Asia/Seoul \
    LANG=C.UTF-8 \
    JAVA_OPTS=""

WORKDIR /app
COPY app.jar /app/app.jar

# rootless podman 에서는 컨테이너 root == 호스트 atmswas(작업 계정) 로 매핑되므로
# 마운트한 logs/config 디렉토리 권한 문제가 생기지 않도록 root 로 실행함
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
