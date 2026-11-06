#!/usr/bin/env bash
# 배포:  bin/deploy.sh <앱> <jar>
#   빌드 -> current/previous 태깅 -> 재기동 -> 헬스체크 -> 실패 시 자동 롤백
source "$(dirname "$0")/common.sh" "${1:-}"
JAR="${2:-}"
[[ -n "${APP}" && -f "${JAR}" ]] || die "사용법: deploy.sh <앱> <jar>"

TAG="$("${KIT_DIR}/bin/build.sh" "${APP}" "${JAR}" | tail -1)"

image_exists "${IMAGE_NAME}:current" && podman tag "${IMAGE_NAME}:current" "${IMAGE_NAME}:previous"
podman tag "${IMAGE_NAME}:${TAG}" "${IMAGE_NAME}:current"
log "${APP}: current -> ${TAG}"

"${KIT_DIR}/bin/stop.sh" "${APP}"
"${KIT_DIR}/bin/start.sh" "${APP}"

if "${KIT_DIR}/bin/healthcheck.sh" "${APP}"; then
  log "배포 성공: ${IMAGE_NAME}:${TAG}"
  "${KIT_DIR}/bin/cleanup-images.sh" "${APP}" || true
else
  log "헬스체크 실패 -> 최근 로그"
  podman logs --tail 100 "${APP}" 2>&1 | sed 's/^/    /' || true
  if image_exists "${IMAGE_NAME}:previous"; then
    log "previous 로 자동 롤백"
    "${KIT_DIR}/bin/rollback.sh" "${APP}" previous || true
  fi
  die "배포 실패: ${IMAGE_NAME}:${TAG}"
fi
