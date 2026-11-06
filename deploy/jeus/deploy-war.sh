#!/usr/bin/env bash
# =====================================================================
# 백엔드 WAR 배포 — nbatmsltws01(JEUS), atmswas 계정
#   Jenkins(같은 서버의 컨테이너) 가 SSH 로 호출한다. ws01 의 ~/.ssh/authorized_keys 에서
#   배포 전용 키에 command="/home/atmswas/atms/deploy/jeus/deploy-war.sh" 를 걸어
#   그 키로는 이 스크립트만 실행되게 고정한다 (셸 접속·다른 명령 불가)
#   → 키로 보낸 명령은 SSH_ORIGINAL_COMMAND 로 들어오고, 아래 4가지만 받는다
#
# 사용 (Jenkins 의 ssh 명령, 또는 ws01 에서 직접):
#   deploy <WAR파일명> <sha256> [커밋]  < WAR   # 표준입력의 WAR 를 검증 → release/<모듈>/ 보관 → swap-deploy.sh 로 배포 폴더 교체
#   rollback <모듈>                              # 그 모듈 폴더를 직전 백업으로 되돌림
#   status [모듈]                                # 모듈별 최근 기록·백업 목록·기동 확인 주소 응답 코드
#   restart [모듈…]                              # 그 모듈의 서버만 차례로 재기동 → 기동 확인 (기본 6개)
#   restart --rollback <모듈…>                   # 위와 같고, 확인 실패한 모듈은 직전 백업으로 되돌린 뒤 다시 재기동 (Jenkins 배포 직후)
#   WAR파일명 형식: <모듈>-<버전>-<YYYYMMDD>-<빌드번호>.war  (예: atms-web-0.0.1-20261023-12.war, 수동 빌드 atms-web-0.0.1-SNAPSHOT-20261023-01.war)
#   모듈: atms-web atms-journal atms-batch atms-file atms-if-internal atms-if-external
#
# 구성 (2026-10-26 결정): JEUS 도메인 1개 + 매니지드 서버 6개 — 모듈마다 JVM 하나, 포트 하나, 컨텍스트 '/'
#   모듈              서버(예정)              포트  폴더
#   atms-web          atms01                  8080  catms           (기존 nbatmsltws01_atms01(8766) 대체)
#   atms-journal      ajou01    8081  cajou
#   atms-batch        abat01      8082  cabat
#   atms-file         file01       8083  cfile
#   atms-if-internal  ifin01       8084  cifin
#   atms-if-external  ifex01       8085  cifex
#   기동 확인: 모듈마다 http://<JEUS_HOST>:<포트>/v3/api-docs 가 200
#
# 재기동: deploy 는 파일 교체까지만. 반영은 restart — 모듈의 서버만 하나씩 (다른 모듈은 멈추지 않음)
#   jeusRstart(root setuid 메뉴)에 그 서버의 정지 메뉴 → 기동 메뉴 번호를 표준입력으로 넣는다
#   메뉴 번호는 ~/deploy/deploy-war.env 의 STOP_MENU_<키>·BOOT_MENU_<키> (키: web journal batch file ifin ifex)
#   서버 6개 모두 WAS 담당이 메뉴를 추가해 준 뒤 번호를 채운다 (예전 5·6 은 nbatmsltws01_atms01 용)
#   번호가 비어 있는 모듈은 재기동하지 않고 "WAS 담당 재기동 필요" 로 남김 → 종료코드 4 (Jenkins 는 UNSTABLE)
#   jeusRstart 는 root 로 돌아 중간에 멈출 수 없으므로 숫자만, 그리고 3(java 전부 kill)·7·8(WebtoB) 은 거부
# 환경변수:
#   APP_HOME    (기본 /home/atmswas)               배포 폴더(catms·cajou·cabat·cfile·cifin·cifex) 상위
#   DEPLOY_HOME (기본 /home/atmswas/deploy)        백업·이력 (swap-deploy.sh 와 같음)
#   RELEASE     (기본 /home/atmswas/atms/release)  받은 WAR 보관 (모듈별 하위 폴더)
#   SWAP        (기본 이 스크립트와 같은 폴더의 swap-deploy.sh)
#   MAX_MB      (기본 300)                         받을 수 있는 최대 크기
#   KEEP_RELEASE(기본 5)                           모듈별로 남길 WAR 개수
#   ~/deploy/deploy-war.env (있으면 읽음, 예시: deploy-war.env.example):
#     JEUS_HOST(16.88.122.113) PORT_<키> SERVER_<키> STOP_MENU_<키> BOOT_MENU_<키>
#     JEUS_RSTART STOP_WAIT(180초) BOOT_WAIT(600초) HEALTH_SKIP("atms-batch" 처럼 기동 확인 제외 모듈)
# 문서: docs/BACKEND_CICD_PLAN.md
# =====================================================================
set -euo pipefail
umask 027

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="${APP_HOME:-/home/atmswas}"
DEPLOY_HOME="${DEPLOY_HOME:-/home/atmswas/deploy}"
RELEASE="${RELEASE:-/home/atmswas/atms/release}"
SWAP="${SWAP:-${HERE}/swap-deploy.sh}"
MAX_MB="${MAX_MB:-300}"
KEEP_RELEASE="${KEEP_RELEASE:-5}"
LOG_FILE="${DEPLOY_HOME}/war-deploy.log"
export APP_HOME DEPLOY_HOME
JEUS_RSTART="/JEUS/jeusATMS/jeusMENU/jeusRstart"
JEUS_HOST="16.88.122.113"
PORT_web=8080;     SERVER_web=atms01
PORT_journal=8081; SERVER_journal=ajou01
PORT_batch=8082;   SERVER_batch=abat01
PORT_file=8083;    SERVER_file=file01
PORT_ifin=8084;    SERVER_ifin=ifin01
PORT_ifex=8085;    SERVER_ifex=ifex01
STOP_MENU_web="";     BOOT_MENU_web=""     # 새 서버 6개의 메뉴는 WAS 담당 회신 후 deploy-war.env 에 (예전 5·6 은 nbatmsltws01_atms01 용)
STOP_MENU_journal=""; BOOT_MENU_journal=""
STOP_MENU_batch="";   BOOT_MENU_batch=""
STOP_MENU_file="";    BOOT_MENU_file=""
STOP_MENU_ifin="";    BOOT_MENU_ifin=""
STOP_MENU_ifex="";    BOOT_MENU_ifex=""
DENY_MENUS="3 7 8"                            # 3 = jeusatms java 전부 kill -9, 7·8 = WebtoB 기동·정지
STOP_WAIT=180
BOOT_WAIT=600
HEALTH_SKIP=""
JEUS_STOP_MENU=""; JEUS_BOOT_MENU=""          # 예전(nbatmsltws01_atms01 하나) 설정 이름 — 2026-10-27 서버 이름 변경으로 쓰지 않음
if [[ -f "${DEPLOY_HOME}/deploy-war.env" ]]; then . "${DEPLOY_HOME}/deploy-war.env"; fi
if [[ -n "${JEUS_STOP_MENU}${JEUS_BOOT_MENU}" ]]; then
  echo "[경고] deploy-war.env 의 JEUS_STOP_MENU/JEUS_BOOT_MENU 는 예전 서버(nbatmsltws01_atms01) 용이라 무시 → STOP_MENU_web·BOOT_MENU_web 에 새 atms01 메뉴를 적는다" >&2
fi

MODULES="atms-web atms-journal atms-batch atms-file atms-if-internal atms-if-external"

# 호출 방식: SSH 고정 명령이면 SSH_ORIGINAL_COMMAND, ws01 에서 직접 실행하면 인자
if [[ -n "${SSH_ORIGINAL_COMMAND:-}" ]]; then
  read -r -a ARGS <<< "${SSH_ORIGINAL_COMMAND}"
  CALLER="ssh:${SSH_CLIENT:-?}"; CALLER="${CALLER%% *}"
else
  ARGS=("$@")
  CALLER="local"
fi
CMD="${ARGS[0]:-}"

mkdir -p "${DEPLOY_HOME}" "${RELEASE}"
log()    { echo "[$(date '+%F %T')] $*"; }
record() { echo "$(date '+%F %T') ${CALLER} $*" >> "${LOG_FILE}"; }
die()    { echo "[$(date '+%F %T')] [ERROR] $*" >&2; record "FAIL ${CMD:-?} $*"; exit 1; }
usage()  { echo "사용법: deploy <WAR파일명> <sha256> [커밋] < WAR | rollback <모듈> | status [모듈] | restart [--rollback] [모듈…]" >&2; exit 2; }

is_module() { [[ " ${MODULES} " == *" $1 "* ]]; }
key_of() {
  case "$1" in
    atms-web) echo web ;; atms-journal) echo journal ;; atms-batch) echo batch ;;
    atms-file) echo file ;; atms-if-internal) echo ifin ;; atms-if-external) echo ifex ;;
  esac
}
cfg()       { local v="${1}_$(key_of "$2")"; echo "${!v:-}"; }   # cfg PORT atms-web → 8080
url_of()    { echo "http://${JEUS_HOST}:$(cfg PORT "$1")"; }
# 기동 확인 주소: 6개 모두 공통 springdoc(/v3/api-docs, permitAll) — DB 상태와 무관하게 앱이 떴는지만 본다
#   (/actuator/health 는 DB 가 없거나 끊기면 503(DOWN) 이라 기동 확인이 실패·롤백됨, 2026-10-23)
health_code() { curl -s -m 10 -o /dev/null -w '%{http_code}' "$(url_of "$1")/v3/api-docs" || true; }
server_up()   { [[ "$(curl -s -m 5 -o /dev/null -w '%{http_code}' "$(url_of "$1")/" || true)" != "000" ]]; }
has_menus()   { [[ -n "$(cfg STOP_MENU "$1")" && -n "$(cfg BOOT_MENU "$1")" ]]; }

# jeusRstart 에 메뉴 번호 하나를 넣어 실행 (root 로 돌아 멈출 수 없으므로 숫자만, 위험 메뉴 거부)
jeus_menu() {
  [[ "$1" =~ ^[0-9]{1,2}$ ]] || die "메뉴 번호 오류: $1"
  [[ " ${DENY_MENUS} " == *" $1 "* ]] && die "쓰지 않는 메뉴: $1 (3 = java 전부 kill, 7·8 = WebtoB)"
  log "jeusRstart 메뉴 $1"
  printf '%s\n' "$1" | "${JEUS_RSTART}" || true
}

# 모듈 하나의 서버를 정지 → 정지 확인 → 기동 (기동 확인은 wait_ready)
restart_server() {
  local m="$1" i
  [[ -x "${JEUS_RSTART}" ]] || die "jeusRstart 없음: ${JEUS_RSTART}"
  log "재기동: ${m} ($(cfg SERVER "${m}"), $(url_of "${m}"))"
  jeus_menu "$(cfg STOP_MENU "${m}")"
  for (( i = 0; i < STOP_WAIT; i += 5 )); do server_up "${m}" || break; sleep 5; done
  if server_up "${m}"; then log "정지 확인 실패 (${STOP_WAIT}초 후에도 응답) → ${m} 기동하지 않음"; return 1; fi
  log "정지 확인: ${m}"
  jeus_menu "$(cfg BOOT_MENU "${m}")"
}

# 모듈 하나의 기동 확인 주소가 200 이 될 때까지 (HEALTH_SKIP 모듈은 통과)
wait_ready() {
  local m="$1" i
  [[ " ${HEALTH_SKIP} " == *" ${m} "* ]] && { log "기동 확인 생략 (HEALTH_SKIP): ${m}"; return 0; }
  for (( i = 0; i <= BOOT_WAIT; i += 10 )); do
    [[ "$(health_code "${m}")" == "200" ]] && { log "기동 확인: ${m}"; return 0; }
    sleep 10
  done
  log "기동 확인 실패 (${BOOT_WAIT}초): ${m} → $(health_code "${m}")"
  return 1
}

[[ -x "${SWAP}" ]] || die "swap-deploy.sh 없음 또는 실행 권한 없음: ${SWAP}"

lock() {
  exec 9>"${DEPLOY_HOME}/.war.lock"
  flock -w 300 9 || die "다른 배포가 진행 중 (5분 대기 초과)"
}

case "${CMD}" in
  deploy)
    [[ ${#ARGS[@]} -ge 3 && ${#ARGS[@]} -le 4 ]] || usage
    FILE="${ARGS[1]}"; SHA="${ARGS[2]}"; COMMIT="${ARGS[3]:--}"
    [[ "${FILE}" =~ ^(atms-[a-z]+(-[a-z]+)*)-[0-9][0-9A-Za-z.-]*-[0-9]{8}-[0-9]{1,6}\.war$ ]] || die "WAR 파일명 형식 오류: ${FILE}"
    MODULE="${BASH_REMATCH[1]}"
    is_module "${MODULE}" || die "모르는 모듈: ${MODULE}"
    [[ "${SHA}" =~ ^[0-9a-f]{64}$ ]] || die "sha256 형식 오류"
    [[ "${COMMIT}" == "-" || "${COMMIT}" =~ ^[0-9a-f]{7,40}$ ]] || die "커밋 형식 오류: ${COMMIT}"
    [[ -t 0 ]] && die "표준입력으로 WAR 를 넘겨야 함 (예: ... < 파일.war)"
    lock

    DIR="${RELEASE}/${MODULE}"
    mkdir -p "${DIR}"
    WAR="${DIR}/${FILE}"
    LIMIT=$(( MAX_MB * 1024 * 1024 ))
    log "수신: ${FILE} (커밋 ${COMMIT}) → ${WAR}"
    head -c $(( LIMIT + 1 )) > "${WAR}.part"
    SIZE="$(stat -c %s "${WAR}.part")"
    (( SIZE > 0 )) || { rm -f "${WAR}.part"; die "받은 파일이 비어 있음"; }
    (( SIZE <= LIMIT )) || { rm -f "${WAR}.part"; die "파일이 ${MAX_MB}MB 를 넘음"; }
    GOT="$(sha256sum "${WAR}.part" | cut -d' ' -f1)"
    [[ "${GOT}" == "${SHA}" ]] || { rm -f "${WAR}.part"; die "SHA256 불일치 (보낸 값 ${SHA}, 받은 파일 ${GOT})"; }
    mv "${WAR}.part" "${WAR}"
    echo "${SHA}  ${FILE}" > "${WAR}.sha256"
    log "수신 완료: ${SIZE} bytes, SHA256 일치"

    "${SWAP}" deploy "${WAR}" || die "교체 실패: ${FILE}"
    record "OK deploy ${FILE} commit=${COMMIT}"
    ls -1t "${DIR}"/*.war 2>/dev/null | tail -n +$(( KEEP_RELEASE + 1 )) | while read -r old; do
      rm -f "${old}" "${old}.sha256"
    done
    log "교체 완료: ${MODULE}. 반영은 restart"
    ;;

  rollback)
    [[ ${#ARGS[@]} -eq 2 ]] || usage
    MODULE="${ARGS[1]}"
    is_module "${MODULE}" || die "모르는 모듈: ${MODULE}"
    lock
    "${SWAP}" rollback "${MODULE}" || die "롤백 실패: ${MODULE}"
    record "OK rollback ${MODULE}"
    log "롤백 완료: ${MODULE}. 반영은 restart"
    ;;

  restart)
    if [[ "${ARGS[1]:-}" == "--rollback" ]]; then
      RB=1; CHECK=("${ARGS[@]:2}"); [[ ${#CHECK[@]} -gt 0 ]] || usage
    else
      RB=""; CHECK=("${ARGS[@]:1}")
    fi
    [[ ${#CHECK[@]} -gt 0 ]] || read -r -a CHECK <<< "${MODULES}"
    for m in "${CHECK[@]}"; do is_module "${m}" || die "모르는 모듈: ${m}"; done
    lock
    OK=""; FAIL=""; MANUAL=""
    # 2코어 서버: 한 서버씩 정지·기동·확인한 뒤 다음 서버로
    for m in "${CHECK[@]}"; do
      if ! has_menus "${m}"; then
        MANUAL="${MANUAL} ${m}"
        log "재기동 메뉴 미설정: ${m} ($(cfg SERVER "${m}")) → WAS 담당 재기동 필요 (deploy-war.env STOP_MENU_$(key_of "${m}")·BOOT_MENU_$(key_of "${m}"))"
        continue
      fi
      if restart_server "${m}" && wait_ready "${m}"; then OK="${OK} ${m}"; continue; fi
      if [[ -z "${RB}" ]]; then FAIL="${FAIL} ${m}"; continue; fi
      log "자동 롤백: ${m}"
      "${SWAP}" rollback "${m}" || log "경고: 롤백 실패 ${m}"
      if restart_server "${m}" && wait_ready "${m}"; then
        FAIL="${FAIL} ${m}(롤백됨)"
      else
        FAIL="${FAIL} ${m}(롤백후에도실패)"
      fi
    done
    log "결과 — 정상:${OK:- 없음} / 실패:${FAIL:- 없음} / WAS 담당 재기동 필요:${MANUAL:- 없음}"
    if [[ -n "${FAIL}" ]]; then die "재기동·기동 확인 실패:${FAIL}"; fi
    if [[ -n "${MANUAL}" ]]; then record "PARTIAL restart ok=${OK:--} manual=${MANUAL}"; exit 4; fi
    record "OK restart ${CHECK[*]}"
    log "재기동 완료"
    ;;

  status)
    [[ ${#ARGS[@]} -le 2 ]] || usage
    if [[ ${#ARGS[@]} -eq 2 ]]; then is_module "${ARGS[1]}" || die "모르는 모듈: ${ARGS[1]}"; LIST="${ARGS[1]}"; else LIST="${MODULES}"; fi
    echo "호출    : ${CALLER}"
    echo "JEUS    : ${JEUS_HOST} (모듈별 서버·포트)"
    for m in ${LIST}; do
      code="$(health_code "${m}")"
      last="$(ls -1t "${RELEASE}/${m}"/*.war 2>/dev/null | head -1 || true)"
      if has_menus "${m}"; then menu="재기동 메뉴 $(cfg STOP_MENU "${m}")/$(cfg BOOT_MENU "${m}")"; else menu="재기동 메뉴 미설정"; fi
      echo "--- ${m}  $(cfg SERVER "${m}") $(url_of "${m}")  (응답 ${code:-000}, ${menu}, 최근 수신 $(basename "${last:-없음}"))"
      "${SWAP}" list "${m}" | head -3 || true
    done
    echo "--- 최근 기록 (${LOG_FILE})"
    tail -n 10 "${LOG_FILE}" 2>/dev/null || echo "(없음)"
    ;;

  *)
    record "REJECT '${ARGS[*]:-}'"
    usage
    ;;
esac
