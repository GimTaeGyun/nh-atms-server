#!/usr/bin/env bash
# 컨테이너 중지:  bin/stop.sh <앱>
source "$(dirname "$0")/common.sh" "${1:-}"
[[ -n "${APP}" ]] || die "사용법: stop.sh <앱>"
if use_quadlet; then
  log "systemctl --user stop ${APP}.service"
  systemctl --user stop "${APP}.service" || true
else
  log "podman stop ${APP}"
  podman stop -t 30 "${APP}" >/dev/null 2>&1 || true
  podman rm -f "${APP}" >/dev/null 2>&1 || true
fi
