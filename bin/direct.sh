#!/usr/bin/env bash
# [Plan B] 컨테이너 없이 JDK 로 직접 실행 (베이스 이미지 확보 전 임시 운영용)
#   bin/direct.sh start  <앱> <jar>
#   bin/direct.sh stop   <앱>
#   bin/direct.sh status <앱>
#  - JDK: ${BASE_DIR}/jdk (반입한 jdk-21.0.12_linux 업로드)
#  - 설정: config/<앱>/application-dev.yml, conf/apps/<앱>.container.env 그대로 사용
CMD="${1:-}"
source "$(dirname "$0")/common.sh" "${2:-}"
[[ -n "${APP}" ]] || die "사용법: direct.sh <start|stop|status> <앱> [jar]"
JAVA="${JDK_DIR}/bin/java"

is_running() { [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; }

case "${CMD}" in
  start)
    JAR="${3:-}"; [[ -f "${JAR}" ]] || die "jar 경로 필요"
    [[ -x "${JAVA}" ]] || die "${JAVA} 없음"
    is_running && die "이미 실행 중 (pid $(cat "${PID_FILE}"))"
    mkdir -p "${RUN_DIR}" "${LOG_DIR}"
    cp "${JAR}" "${RUN_DIR}/${APP}.jar"
    # env 파일은 podman 형식(따옴표 없음)이므로 source 하지 않고 한 줄씩 export
    while IFS= read -r line; do
      [[ -z "${line}" || "${line}" == \#* ]] && continue
      export "${line%%=*}=${line#*=}"
    done < "${APP_ENV_FILE}"
    cd "${RUN_DIR}"
    # shellcheck disable=SC2086
    nohup "${JAVA}" ${JAVA_OPTS:-} -jar "${RUN_DIR}/${APP}.jar" \
      --server.port="${HOST_PORT}" \
      --spring.config.additional-location="optional:file:${CONFIG_DIR}/" \
      --logging.file.path="${LOG_DIR}" \
      > "${LOG_DIR}/stdout.log" 2>&1 &
    echo $! > "${PID_FILE}"
    log "기동: ${APP} pid=$(cat "${PID_FILE}") port=${HOST_PORT}"
    "${KIT_DIR}/bin/healthcheck.sh" "${APP}"
    ;;
  stop)
    if is_running; then
      pid="$(cat "${PID_FILE}")"; kill "${pid}"
      for _ in $(seq 1 30); do kill -0 "${pid}" 2>/dev/null || break; sleep 1; done
      kill -0 "${pid}" 2>/dev/null && kill -9 "${pid}"
      log "중지: ${APP} (pid ${pid})"
    else
      log "실행 중 아님"
    fi
    rm -f "${PID_FILE}"
    ;;
  status)
    is_running && log "실행 중 pid=$(cat "${PID_FILE}")" || log "중지 상태"
    curl -s -m 5 -o /dev/null -w "HTTP %{http_code}\n" "http://127.0.0.1:${HOST_PORT}${HEALTH_PATH}" || true
    ;;
  *) die "사용법: direct.sh <start|stop|status> <앱> [jar]" ;;
esac
