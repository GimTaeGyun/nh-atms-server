#!/usr/bin/env bash
# 최초 1회: 설정파일 복사 + 앱별 디렉토리 생성
#   bin/init-dirs.sh            -> conf/apps/*.env.example 전체 기준
KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ -f "${KIT_DIR}/conf/atms.env" ]] || cp "${KIT_DIR}/conf/atms.env.example" "${KIT_DIR}/conf/atms.env"
for ex in "${KIT_DIR}"/conf/apps/*.env.example; do
  [[ -f "${ex%.example}" ]] || cp "${ex}" "${ex%.example}"
done
source "${KIT_DIR}/bin/common.sh"

mkdir -p "${RELEASE_DIR}" "${RUN_DIR}" "${BASE_DIR}/images" "${BASE_DIR}/logs" "${BASE_DIR}/config" "${BASE_DIR}/build"
for app in $(list_apps); do
  mkdir -p "${BASE_DIR}/logs/${app}" "${BASE_DIR}/config/${app}" "${BASE_DIR}/build/${app}"
  if [[ ! -f "${BASE_DIR}/config/${app}/application-dev.yml" ]]; then
    cp "${KIT_DIR}/conf/application-dev.yml.example" "${BASE_DIR}/config/${app}/application-dev.yml"
    log "생성: config/${app}/application-dev.yml  (DB 접속정보 수정 필요)"
  fi
done
chmod 700 "${BASE_DIR}/config"
find "${BASE_DIR}/config" -name '*.yml' -exec chmod 600 {} +
chmod 600 "${KIT_DIR}"/conf/*.env "${KIT_DIR}"/conf/apps/*.env
chmod +x "${KIT_DIR}"/bin/*.sh
log "준비 완료. 등록된 앱: $(list_apps | tr '\n' ' ')"
