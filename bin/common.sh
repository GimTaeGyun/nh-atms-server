#!/usr/bin/env bash
# 공통 설정 로더
#   source common.sh            -> 공통 설정만
#   source common.sh <앱이름>    -> 공통 + conf/apps/<앱이름>.env
set -euo pipefail

KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

log() { echo "[$(date '+%F %T')] $*"; }
die() { echo "[$(date '+%F %T')] [ERROR] $*" >&2; exit 1; }

[[ -f "${KIT_DIR}/conf/atms.env" ]] || die "conf/atms.env 없음 -> bin/init-dirs.sh 먼저 실행"
# shellcheck disable=SC1091
source "${KIT_DIR}/conf/atms.env"

: "${BASE_DIR:=${KIT_DIR}}"
: "${BASE_IMAGE:=docker.io/library/eclipse-temurin:21-jre-jammy}"
: "${DOCKERFILE:=Dockerfile}"
: "${HEALTH_TIMEOUT_SEC:=180}"
: "${KEEP_IMAGES:=3}"
JDK_DIR="${BASE_DIR}/jdk"
RUN_DIR="${BASE_DIR}/run"
RELEASE_DIR="${BASE_DIR}/release"
QUADLET_DIR="${HOME}/.config/containers/systemd"

list_apps() { ls "${KIT_DIR}/conf/apps/"*.env 2>/dev/null | grep -v '\.container\.env$' | xargs -r -n1 basename | sed 's/\.env$//'; }

APP="${1:-}"
if [[ -n "${APP}" ]]; then
  APP_CONF="${KIT_DIR}/conf/apps/${APP}.env"
  [[ -f "${APP_CONF}" ]] || die "앱 설정 없음: conf/apps/${APP}.env (등록된 앱: $(list_apps | tr '\n' ' '))"
  # shellcheck disable=SC1090
  source "${APP_CONF}"
  : "${HOST_PORT:?conf/apps/${APP}.env 에 HOST_PORT 필요}"
  : "${CONTAINER_PORT:=8080}"
  : "${HEALTH_PATH:=/actuator/health}"
  : "${CPU_LIMIT:=1.0}"
  : "${MEM_LIMIT:=2g}"
  APP_ENV_FILE="${KIT_DIR}/conf/apps/${APP}.container.env"
  IMAGE_NAME="localhost/${APP}"
  LOG_DIR="${BASE_DIR}/logs/${APP}"
  CONFIG_DIR="${BASE_DIR}/config/${APP}"
  BUILD_DIR="${BASE_DIR}/build/${APP}"
  QUADLET_FILE="${QUADLET_DIR}/${APP}.container"
  PID_FILE="${RUN_DIR}/${APP}.pid"
  [[ -f "${APP_ENV_FILE}" ]] || die "conf/apps/${APP}.container.env 없음"
fi

use_quadlet()  { [[ -n "${QUADLET_FILE:-}" && -f "${QUADLET_FILE}" ]]; }
image_exists() { podman image exists "$1"; }

# 자원 제한 옵션: 이 계정에 위임된 cgroup 컨트롤러만 사용 (RHEL 9 rootless 기본은 memory·pids → cpu 미위임이면 --cpus 생략)
#   cpu 를 쓰려면 root 가 user@.service 에 Delegate 추가 (docs/ROOT_DAY_CHECKLIST.md)
limit_args() {
  local ctrl args=()
  ctrl=" $(podman info --format '{{range .Host.CgroupControllers}}{{.}} {{end}}' 2>/dev/null || true) "
  [[ "${ctrl}" == *" cpu "* ]] && args+=("--cpus=${CPU_LIMIT}")
  [[ "${ctrl}" == *" memory "* ]] && args+=("--memory=${MEM_LIMIT}")
  echo "${args[*]}"
}
