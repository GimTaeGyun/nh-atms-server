#!/usr/bin/env bash
# Kibana 초기 설정: atms-journal-* 데이터뷰 등록 (멱등 — 재실행 안전)
# 사용: docker compose up -d 후 Kibana 기동 완료(1~2분)를 기다렸다가 실행
#   ./setup-kibana.sh [kibana_url]   (기본 http://localhost:19601)
set -euo pipefail

KIBANA_URL="${1:-http://localhost:19601}"
DIR="$(cd "$(dirname "$0")" && pwd)"

echo "waiting for kibana at ${KIBANA_URL} ..."
for i in $(seq 1 30); do
  if curl -sf "${KIBANA_URL}/api/status" >/dev/null 2>&1; then break; fi
  sleep 5
done

curl -sf -X POST "${KIBANA_URL}/api/saved_objects/_import?overwrite=true" \
  -H "kbn-xsrf: true" \
  --form file=@"${DIR}/kibana-saved-objects.ndjson"
echo
echo "done. Kibana → Analytics → Discover 에서 'ATMS Journal' 데이터뷰 선택"
