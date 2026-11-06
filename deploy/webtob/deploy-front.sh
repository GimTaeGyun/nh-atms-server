#!/usr/bin/env bash
# =====================================================================
# 화면(프론트엔드) 배포 — nbatmsltwb01(WebtoB), atmswas 계정
#   Jenkins(ws01) 가 SSH 로 호출한다. wb01 의 ~/.ssh/authorized_keys 에서
#   배포 전용 키에 command="/home/atmswas/deploy/bin/deploy-front.sh" 를 걸어
#   그 키로는 이 스크립트만 실행되게 고정한다 (셸 접속·다른 명령 불가)
#   → 키로 보낸 명령은 SSH_ORIGINAL_COMMAND 로 들어오고, 아래 3가지만 받는다
#
# 사용 (Jenkins 의 ssh 명령, 또는 wb01 에서 직접):
#   deploy <버전> <sha256> [커밋]  < 화면.tar.gz   # 표준입력의 tar.gz 를 검증 → swap-deploy.sh 로 교체 → 확인
#   rollback                                      # 직전 백업으로 되돌림
#   status                                        # 현재 버전·최근 기록·백업 목록·응답 코드
#   예) ssh atmswas@16.88.122.112 "deploy 20261021-15 <sha256> <커밋>" < nh-atms-web-dist-20261021-15.tar.gz
#
# 배포 확인: / 가 200 이고, WebtoB 가 주는 index.html 이 새 파일과 같아야 성공.
#           실패하면 자동으로 직전 버전으로 되돌리고 종료코드 1 (Jenkins 빌드 실패로 표시)
# 환경변수:
#   DOCBASE     (기본 /home/atmswas/docs)          WebtoB DocBase
#   DEPLOY_HOME (기본 /home/atmswas/deploy)        inbox·backup·기록 위치 (swap-deploy.sh 와 같음)
#   SWAP        (기본 $DEPLOY_HOME/bin/swap-deploy.sh)
#   URL         (기본 http://127.0.0.1:9000)       확인용 WebtoB 주소
#   MAX_MB      (기본 200)                         받을 수 있는 최대 크기
#   KEEP_INBOX  (기본 5)                           inbox 에 남길 tar.gz 개수
# 문서: docs/FRONTEND_CICD_PLAN.md
# =====================================================================
set -euo pipefail
umask 027

DOCBASE="${DOCBASE:-/home/atmswas/docs}"
DEPLOY_HOME="${DEPLOY_HOME:-/home/atmswas/deploy}"
SWAP="${SWAP:-${DEPLOY_HOME}/bin/swap-deploy.sh}"
URL="${URL:-http://127.0.0.1:9000}"
MAX_MB="${MAX_MB:-200}"
KEEP_INBOX="${KEEP_INBOX:-5}"
INBOX="${DEPLOY_HOME}/inbox"
LOG_FILE="${DEPLOY_HOME}/front-deploy.log"
export DEPLOY_HOME
CURRENT="${DEPLOY_HOME}/front-current.txt"

# 호출 방식: SSH 고정 명령이면 SSH_ORIGINAL_COMMAND, wb01 에서 직접 실행하면 인자
if [[ -n "${SSH_ORIGINAL_COMMAND:-}" ]]; then
  read -r -a ARGS <<< "${SSH_ORIGINAL_COMMAND}"
  CALLER="ssh:${SSH_CLIENT:-?}"; CALLER="${CALLER%% *}"
else
  ARGS=("$@")
  CALLER="local"
fi
CMD="${ARGS[0]:-}"

mkdir -p "${INBOX}"
log()    { echo "[$(date '+%F %T')] $*"; }
record() { echo "$(date '+%F %T') ${CALLER} $*" >> "${LOG_FILE}"; }
die()    { echo "[$(date '+%F %T')] [ERROR] $*" >&2; record "FAIL ${CMD:-?} $*"; exit 1; }
usage()  { echo "사용법: deploy <버전> <sha256> [커밋] < 화면.tar.gz | rollback | status" >&2; exit 2; }

[[ -x "${SWAP}" ]] || die "swap-deploy.sh 없음 또는 실행 권한 없음: ${SWAP}"

# 동시에 하나만 (Jenkins 배포와 수동 작업이 겹치지 않게), 최대 5분 대기
lock() {
  exec 9>"${DEPLOY_HOME}/.front.lock"
  flock -w 300 9 || die "다른 배포가 진행 중 (5분 대기 초과)"
}

# / 응답 코드와, WebtoB 가 실제로 주는 index.html 이 DocBase 파일과 같은지
check_served() {
  local code want served
  code="$(curl -s -m 10 -o /dev/null -w '%{http_code}' "${URL}/" || true)"
  want="$(sha256sum "${DOCBASE}/index.html" 2>/dev/null | cut -d' ' -f1 || true)"
  served="$(curl -s -m 10 "${URL}/index.html" | sha256sum | cut -d' ' -f1 || true)"
  log "확인: / → ${code:-000}, index.html 일치 $([[ -n "${want}" && "${want}" == "${served}" ]] && echo 예 || echo 아니오)"
  [[ "${code}" == "200" && -n "${want}" && "${want}" == "${served}" ]]
}

case "${CMD}" in
  deploy)
    [[ ${#ARGS[@]} -ge 3 && ${#ARGS[@]} -le 4 ]] || usage
    VER="${ARGS[1]}"; SHA="${ARGS[2]}"; COMMIT="${ARGS[3]:--}"
    [[ "${VER}" =~ ^[0-9]{8}-[0-9]{1,6}$ ]] || die "버전 형식 오류 (YYYYMMDD-빌드번호): ${VER}"
    [[ "${SHA}" =~ ^[0-9a-f]{64}$ ]] || die "sha256 형식 오류"
    [[ "${COMMIT}" == "-" || "${COMMIT}" =~ ^[0-9a-f]{7,40}$ ]] || die "커밋 형식 오류: ${COMMIT}"
    [[ -t 0 ]] && die "표준입력으로 tar.gz 를 넘겨야 함 (예: ... < 파일.tar.gz)"
    lock

    PKG="${INBOX}/nh-atms-web-dist-${VER}.tar.gz"
    LIMIT=$(( MAX_MB * 1024 * 1024 ))
    log "수신: ${VER} (커밋 ${COMMIT}) → ${PKG}"
    head -c $(( LIMIT + 1 )) > "${PKG}.part"
    SIZE="$(stat -c %s "${PKG}.part")"
    (( SIZE > 0 )) || { rm -f "${PKG}.part"; die "받은 파일이 비어 있음"; }
    (( SIZE <= LIMIT )) || { rm -f "${PKG}.part"; die "파일이 ${MAX_MB}MB 를 넘음"; }
    GOT="$(sha256sum "${PKG}.part" | cut -d' ' -f1)"
    [[ "${GOT}" == "${SHA}" ]] || { rm -f "${PKG}.part"; die "SHA256 불일치 (보낸 값 ${SHA}, 받은 파일 ${GOT})"; }
    mv "${PKG}.part" "${PKG}"
    echo "${SHA}  $(basename "${PKG}")" > "${PKG}.sha256"
    log "수신 완료: ${SIZE} bytes, SHA256 일치"

    "${SWAP}" deploy "${PKG}" "${DOCBASE}" || die "교체 실패: ${VER}"
    if ! check_served; then
      log "확인 실패 → 직전 버전으로 되돌림"
      "${SWAP}" rollback "${DOCBASE}" || true
      die "배포 실패 (자동 롤백): ${VER}"
    fi

    PREV="$(cat "${CURRENT}" 2>/dev/null || echo '-')"
    echo "${VER} ${COMMIT} $(date '+%F %T')" > "${CURRENT}"
    record "OK deploy ${VER} commit=${COMMIT} (이전: ${PREV})"
    ls -1t "${INBOX}"/nh-atms-web-dist-*.tar.gz 2>/dev/null | tail -n +$(( KEEP_INBOX + 1 )) | while read -r old; do
      rm -f "${old}" "${old}.sha256"
    done
    log "배포 완료: ${VER} (WebtoB 재기동 불필요, 브라우저는 새로고침)"
    ;;

  rollback)
    [[ ${#ARGS[@]} -eq 1 ]] || usage
    lock
    PREV="$(cat "${CURRENT}" 2>/dev/null || echo '-')"
    "${SWAP}" rollback "${DOCBASE}" || die "롤백 실패"
    check_served || log "경고: 롤백 후 확인 실패 → status 로 상태 확인"
    echo "rollback $(date '+%F %T') (되돌리기 전: ${PREV})" > "${CURRENT}"
    record "OK rollback (되돌리기 전: ${PREV})"
    log "롤백 완료. 실제 버전은 front-deploy.log 의 이전 deploy 기록 참고"
    ;;

  status)
    [[ ${#ARGS[@]} -eq 1 ]] || usage
    echo "DocBase : ${DOCBASE}"
    echo "현재    : $(cat "${CURRENT}" 2>/dev/null || echo '(기록 없음)')"
    check_served || true
    echo "--- 최근 기록 (${LOG_FILE})"
    tail -n 10 "${LOG_FILE}" 2>/dev/null || echo "(없음)"
    echo "--- 백업 (최근 순)"
    "${SWAP}" list "${DOCBASE}"
    ;;

  *)
    record "REJECT '${ARGS[*]:-}'"
    usage
    ;;
esac
