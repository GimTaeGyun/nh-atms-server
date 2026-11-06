#!/usr/bin/env bash
# 컨테이너 기동:  bin/start.sh <앱>   (이미지 태그 current 사용)
source "$(dirname "$0")/common.sh" "${1:-}"
[[ -n "${APP}" ]] || die "사용법: start.sh <앱>"
image_exists "${IMAGE_NAME}:current" || die "${IMAGE_NAME}:current 없음 (bin/deploy.sh 로 배포)"

if use_quadlet; then
  log "systemctl --user start ${APP}.service"
  systemctl --user start "${APP}.service"
else
  podman rm -f "${APP}" >/dev/null 2>&1 || true
  read -ra LIMITS <<< "$(limit_args)"
  log "podman run: ${APP} (port ${HOST_PORT}->${CONTAINER_PORT}, 자원 제한: ${LIMITS[*]:-없음})"
  podman run -d --name "${APP}" \
    --restart=unless-stopped \
    -p "${HOST_PORT}:${CONTAINER_PORT}" \
    "${LIMITS[@]}" \
    --env-file "${APP_ENV_FILE}" \
    -v "${LOG_DIR}:/app/logs:Z" \
    -v "${CONFIG_DIR}:/app/config:ro,Z" \
    "${IMAGE_NAME}:current"
fi
