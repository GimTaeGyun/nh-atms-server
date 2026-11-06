#!/usr/bin/env bash
# 상태:  bin/status.sh          -> 전체 앱 요약
#        bin/status.sh <앱>     -> 상세
source "$(dirname "$0")/common.sh"
if [[ -z "${1:-}" ]]; then
  podman ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Image}}'
  echo; podman stats --no-stream 2>/dev/null || true
  exit 0
fi
source "$(dirname "$0")/common.sh" "$1"
echo "== 컨테이너 =="; podman ps -a --filter "name=^${APP}\$"
echo; echo "== 자원 =="; podman stats --no-stream "${APP}" 2>/dev/null || echo "실행 중 아님"
echo; echo "== 이미지 =="; podman images "${IMAGE_NAME}"
if use_quadlet; then echo; systemctl --user status "${APP}.service" --no-pager | head -12; fi
echo; echo "== 헬스체크 http://127.0.0.1:${HOST_PORT}${HEALTH_PATH} =="
curl -s -m 5 -w '\nHTTP %{http_code}\n' "http://127.0.0.1:${HOST_PORT}${HEALTH_PATH}" || echo "응답 없음"
