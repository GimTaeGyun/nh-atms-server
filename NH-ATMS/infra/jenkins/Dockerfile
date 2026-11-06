# =====================================================================
# NH-ATMS Jenkins 커스텀 이미지
#
# 기반 이미지: jenkins/jenkins:lts-jdk21
# 추가 구성:
#   ① Docker CLI 설치   → Jenkinsfile 내에서 docker build/push 실행 가능
#   ② 플러그인 사전 설치 → 최초 기동 시 플러그인 다운로드 대기 시간 제거
# =====================================================================

FROM jenkins/jenkins:lts-jdk21

# ─────────────────────────────────────────────────────────────────────
# root로 전환하여 시스템 패키지 설치
# jenkins/jenkins 이미지는 기본적으로 jenkins 사용자로 실행됨
# Docker CLI 설치는 root 권한 필요
# ─────────────────────────────────────────────────────────────────────
USER root

# ─────────────────────────────────────────────────────────────────────
# Docker CLI 설치
#
# 목적: Jenkins가 docker build, docker push 명령을 실행할 수 있도록
# 방식: Docker 공식 APT 저장소에서 docker-ce-cli만 설치
#       (Docker Engine 전체는 불필요 — 호스트의 Docker 데몬을 소켓으로 공유)
#
# 동작 원리:
#   호스트의 /var/run/docker.sock → 컨테이너에 마운트
#   Jenkins 컨테이너 내 Docker CLI → 소켓을 통해 호스트 Docker 데몬과 통신
#   → 실제 이미지 빌드는 호스트 Docker 엔진이 수행
# ─────────────────────────────────────────────────────────────────────
RUN apt-get update && \
    apt-get install -y \
        ca-certificates \
        curl \
        gnupg \
        lsb-release && \
    # Docker 공식 GPG 키 등록
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg \
         -o /etc/apt/keyrings/docker.asc && \
    chmod a+r /etc/apt/keyrings/docker.asc && \
    # Docker APT 저장소 추가
    echo "deb [arch=$(dpkg --print-architecture) \
          signed-by=/etc/apt/keyrings/docker.asc] \
          https://download.docker.com/linux/debian \
          $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
        | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    # docker-ce-cli만 설치 (Engine, containerd 불필요)
    apt-get install -y docker-ce-cli && \
    # 캐시 정리 (이미지 크기 최소화)
    rm -rf /var/lib/apt/lists/*

# ─────────────────────────────────────────────────────────────────────
# jenkins 사용자를 docker 그룹에 추가
#
# 이유: /var/run/docker.sock 파일은 docker 그룹만 읽기/쓰기 가능
#       jenkins 사용자가 이 소켓에 접근하려면 그룹 멤버여야 함
#       (sudo 없이 docker 명령 실행 가능)
# ─────────────────────────────────────────────────────────────────────
RUN groupadd -f docker && \
    usermod -aG docker jenkins

# ─────────────────────────────────────────────────────────────────────
# jenkins 사용자로 복귀
# ─────────────────────────────────────────────────────────────────────
USER jenkins

# ─────────────────────────────────────────────────────────────────────
# 플러그인 사전 설치
#
# jenkins-plugin-cli: 플러그인과 그 의존성을 자동으로 함께 설치
# plugins.txt: 설치할 플러그인 목록 (ID 또는 ID:버전 형식)
#
# 이 레이어를 Dockerfile에서 처리하는 이유:
#   최초 컨테이너 기동 시 플러그인 다운로드(수분) 없이 즉시 시작 가능
#   이미지로 고정되어 항상 동일한 버전의 플러그인 보장
# ─────────────────────────────────────────────────────────────────────
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# ─────────────────────────────────────────────────────────────────────
# JCasC(Jenkins Configuration as Code) 설정 파일 복사
#
# casc/jenkins.yaml: 관리자 계정, 보안 정책 등을 코드로 정의
# CASC_JENKINS_CONFIG: JCasC 플러그인이 읽을 설정 파일 경로 (환경변수)
# ─────────────────────────────────────────────────────────────────────
COPY casc/jenkins.yaml /var/jenkins_casc/jenkins.yaml
ENV CASC_JENKINS_CONFIG=/var/jenkins_casc/jenkins.yaml
