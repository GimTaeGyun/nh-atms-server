#!/usr/bin/env bash
# 오래된 앱 이미지 정리 (current/previous 와 최근 KEEP_IMAGES 개 보존)
source "$(dirname "$0")/common.sh" "${1:-}"
[[ -n "${APP}" ]] || die "사용법: cleanup-images.sh <앱>"
keep_ids="$(podman image inspect --format '{{.Id}}' "${IMAGE_NAME}:current" "${IMAGE_NAME}:previous" 2>/dev/null || true)"
podman images "${IMAGE_NAME}" --format '{{.Tag}} {{.ID}}' \
  | grep -E '^[0-9]{8}-[0-9]{6} ' | sort -r | tail -n +"$((KEEP_IMAGES + 1))" \
  | while read -r tag id; do
      grep -q "^${id}" <<<"${keep_ids}" && continue
      log "삭제: ${IMAGE_NAME}:${tag}"
      podman rmi "${IMAGE_NAME}:${tag}" >/dev/null || true
    done
podman image prune -f >/dev/null || true
