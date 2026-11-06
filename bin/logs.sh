#!/usr/bin/env bash
# 로그:  bin/logs.sh <앱>        -> 컨테이너 표준출력 (follow)
#        bin/logs.sh <앱> file   -> 앱 로그파일 tail
source "$(dirname "$0")/common.sh" "${1:-}"
[[ -n "${APP}" ]] || die "사용법: logs.sh <앱> [file]"
if [[ "${2:-}" == "file" ]]; then
  tail -F "${LOG_DIR}"/*.log
else
  podman logs -f --tail 200 "${APP}"
fi
