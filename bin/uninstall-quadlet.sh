#!/usr/bin/env bash
# 자동기동 해제:  bin/uninstall-quadlet.sh <앱>
source "$(dirname "$0")/common.sh" "${1:-}"
[[ -n "${APP}" ]] || die "사용법: uninstall-quadlet.sh <앱>"
systemctl --user stop "${APP}.service" 2>/dev/null || true
rm -f "${QUADLET_FILE}"
systemctl --user daemon-reload
log "해제 완료. 이후 bin/start.sh ${APP} 로 기동"
