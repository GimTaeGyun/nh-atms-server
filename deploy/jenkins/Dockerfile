# =====================================================================
# NH-ATMS Jenkins 이미지 (은행 개발서버 / 폐쇄망용)
#
#  - 반드시 "외부망 PC"에서 빌드한다 (플러그인·패키지 다운로드 필요)
#      docker build --platform linux/amd64 -t localhost/nh-atms-jenkins:bank deploy/jenkins
#      docker save -o nh-atms-jenkins-bank.tar localhost/nh-atms-jenkins:bank
#  - 원본(NH-ATMS/infra/jenkins)과 차이
#      * GitLab/GitHub/Slack 연동 제거, Gitea 연동 추가 (사내 Git 서버)
#      * docker compose 플러그인 + Node.js 22 내장 (폐쇄망에서 설치 불가)
#      * Executor 1개 (서버 CPU 2 core, JEUS 공존)
#  - 버전 고정 (2026-10-08, docs/REQUIRED_FILES.md 2-3장과 동일)
#      Jenkins 2.580.1 LTS(JDK 21, Debian trixie) · Node 22.23.3
#      docker-ce-cli 29.8.2 · docker-compose-plugin 5.6.0 · 플러그인은 plugins.txt
# =====================================================================
FROM node:22.23.3-trixie-slim AS node

FROM jenkins/jenkins:2.580.1-lts-jdk21

USER root

# Docker CLI + compose 플러그인 (서버의 Podman 소켓에 붙어서 동작)
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && chmod a+r /etc/apt/keyrings/docker.asc \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
      > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends \
      docker-ce-cli=5:29.8.2-1~debian.13~trixie \
      docker-compose-plugin=5.6.0-1~debian.13~trixie \
 && rm -rf /var/lib/apt/lists/*

# Node.js 22 (Frontend 빌드용) — 공식 node 이미지에서 바이너리만 복사
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -sf /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
 && ln -sf /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
 && node -v && npm -v && docker --version && docker compose version

USER jenkins

# 플러그인 사전 설치 (폐쇄망에서는 설치 불가하므로 이미지에 포함)
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# JCasC: 로컬 관리자 계정 자동 구성 (설정 마법사 생략)
COPY casc/jenkins.yaml /var/jenkins_casc/jenkins.yaml
ENV CASC_JENKINS_CONFIG=/var/jenkins_casc/jenkins.yaml \
    JAVA_OPTS="-Djenkins.install.runSetupWizard=false -Dfile.encoding=UTF-8"
