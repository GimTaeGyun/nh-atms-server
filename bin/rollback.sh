#!/usr/bin/env bash
# 롤백:  bin/rollback.sh <앱> [태그]   (태그 생략 시 previous)
source "$(dirname "$0")/common.sh" "${1:-}"
[[ -n "${APP}" ]] || die "사용법: rollback.sh <앱> [태그]"
TARGET="${2:-previous}"
image_exists "${IMAGE_NAME}:${TARGET}" || die "이미지 없음: ${IMAGE_NAME}:${TARGET}"
podman tag "${IMAGE_NAME}:${TARGET}" "${IMAGE_NAME}:current"
log "${APP}: current -> ${TARGET}"
"${KIT_DIR}/bin/stop.sh" "${APP}"
"${KIT_DIR}/bin/start.sh" "${APP}"
"${KIT_DIR}/bin/healthcheck.sh" "${APP}"
