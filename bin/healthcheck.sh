#!/usr/bin/env bash
# 헬스체크 (2xx/3xx = 성공):  bin/healthcheck.sh <앱>
source "$(dirname "$0")/common.sh" "${1:-}"
[[ -n "${APP}" ]] || die "사용법: healthcheck.sh <앱>"
URL="http://127.0.0.1:${HOST_PORT}${HEALTH_PATH}"
log "헬스체크 대기: ${URL} (최대 ${HEALTH_TIMEOUT_SEC}s)"
code=000
for ((i=0; i<HEALTH_TIMEOUT_SEC; i+=5)); do
  code="$(curl -s -o /dev/null -m 3 -w '%{http_code}' "${URL}" || true)"
  if [[ "${code}" =~ ^[23] ]]; then log "OK (HTTP ${code}, ${i}s)"; exit 0; fi
  sleep 5
done
log "실패 (마지막 HTTP ${code})"
exit 1
