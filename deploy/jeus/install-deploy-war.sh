#!/usr/bin/env bash
# =====================================================================
# ws01(JEUS) 백엔드 WAR 배포 준비 — nbatmsltws01, atmswas 계정 (sudo 불필요, 다시 실행해도 안전)
#   배포킷이 ~/atms 에 있을 때:
#     ~/atms/deploy/jeus/install-deploy-war.sh                          # 폴더·스크립트 확인만
#     ~/atms/deploy/jeus/install-deploy-war.sh <공개키파일> [출발IP]     # + Jenkins 배포 키 등록
#
#  1) deploy-war.sh, swap-deploy.sh 실행 권한·CRLF·문법
#  2) ~/deploy (700, 백업·기록), ~/atms/release (받은 WAR), ~/deploy/deploy-war.env (없으면 예시 복사, 600)
#  3) 홈 /home/atmswas 가 jeus 그룹·750 인지 (JEUS 의 jeusatms 가 배포 폴더(catms·cajou·cabat·cfile·cifin·cifex)를 jeus 그룹으로 읽음)
#  4) (공개키를 주면) ~/.ssh/authorized_keys 에 한 줄 등록 — 같은 키 줄이 있으면 바꿔 씀
#       restrict[,from="<출발IP>"],command="/home/atmswas/atms/deploy/jeus/deploy-war.sh" ssh-ed25519 AAAA… jenkins-ws01-deploy
#     Jenkins 는 같은 서버의 컨테이너라, 접속이 어느 IP 로 보이는지 미리 알 수 없다
#     → 처음에는 출발IP 없이 등록 → Jenkins 잡 MODE=status 의 "호출 : ssh:<IP>" 를 보고 → 그 IP 로 다시 실행
# 문서: docs/BACKEND_CICD_PLAN.md 6장
# =====================================================================
set -euo pipefail
umask 027

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMAND="${HERE}/deploy-war.sh"
DEPLOY_HOME="${DEPLOY_HOME:-${HOME}/deploy}"
RELEASE="${RELEASE:-${HOME}/atms/release}"
PUBKEY="${1:-}"
FROM_IP="${2:-}"

log() { echo "[$(date '+%F %T')] $*"; }
die() { echo "[$(date '+%F %T')] [ERROR] $*" >&2; exit 1; }

[[ "$(id -un)" == "atmswas" ]] || log "경고: atmswas 가 아닌 계정($(id -un))으로 실행 중"
[[ "${COMMAND}" == /home/atmswas/atms/deploy/jeus/deploy-war.sh ]] \
  || log "경고: 배포킷 위치가 ~/atms 가 아님 (${COMMAND}) — authorized_keys 에는 이 경로가 들어간다"

# 1) 스크립트
for f in "${HERE}/deploy-war.sh" "${HERE}/swap-deploy.sh"; do
  [[ -f "${f}" ]] || die "없음: ${f}"
  sed -i 's/\r$//' "${f}"
  chmod 700 "${f}"
  bash -n "${f}" || die "문법 오류: ${f}"
done
for c in flock curl sha256sum; do
  command -v "${c}" >/dev/null 2>&1 || die "명령 없음: ${c}"
done
command -v unzip >/dev/null 2>&1 || command -v python3 >/dev/null 2>&1 || die "WAR 를 풀 명령(unzip 또는 python3) 없음"
log "스크립트·명령 확인 완료"

# 2) 폴더
mkdir -p "${DEPLOY_HOME}" "${RELEASE}"
chmod 700 "${DEPLOY_HOME}"

# 2-1) 서버 설정 deploy-war.env (모듈별 포트·재기동 메뉴) — 없을 때만 예시를 복사, 있으면 그대로 두고 확인만
ENV_FILE="${DEPLOY_HOME}/deploy-war.env"
if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${HERE}/deploy-war.env.example" "${ENV_FILE}"
  log "생성: ${ENV_FILE} (예시 복사 — WAS 담당 회신 후 STOP_MENU_*·BOOT_MENU_* 를 채운다)"
fi
sed -i 's/\r$//' "${ENV_FILE}"
chmod 600 "${ENV_FILE}"
bash -n "${ENV_FILE}" || die "문법 오류: ${ENV_FILE}"
grep -q '^JEUS_URL=' "${ENV_FILE}" && log "참고: ${ENV_FILE} 의 JEUS_URL 은 더 이상 쓰지 않음 (모듈별 PORT_*, 2026-10-26)"
grep -q '^JEUS_STOP_MENU=' "${ENV_FILE}" && log "참고: JEUS_STOP_MENU 는 STOP_MENU_web 으로 읽힘 (예전 이름)"
log "재기동 메뉴 설정:"
grep -E '^(STOP|BOOT)_MENU_|^JEUS_(STOP|BOOT)_MENU=' "${ENV_FILE}" | sed 's/^/    /' || true

# 3) 홈 권한 (바꾸지 않고 확인만 — DEV_SERVER_SETUP 2단계에서 맞춘다)
HG="$(stat -c '%G %a' "${HOME}")"
[[ "${HG}" == "jeus 750" ]] || log "경고: 홈이 '${HG}' → 'jeus 750' 이어야 JEUS 가 배포 폴더(catms·cajou·cabat·cfile·cifin·cifex)를 읽는다 (chgrp jeus ~ ; chmod 750 ~)"
ls -ld "${HOME}" "${HOME}"/catms "${HOME}"/cajou "${HOME}"/cabat "${HOME}"/cfile "${HOME}"/cifin "${HOME}"/cifex 2>/dev/null || log "배포 폴더 일부 없음 → 첫 deploy 때 홈 그룹(jeus)·2750 으로 만들어진다"
ls -d "${HOME}"/catms-* 2>/dev/null && log "참고: 예전 폴더(catms-journal 등)가 남아 있음 → 2026-10-27 폴더 이름 변경 (BACKEND_CICD_PLAN 6-10)" || true

# 4) 배포 키 등록 (선택)
if [[ -n "${PUBKEY}" ]]; then
  [[ -f "${PUBKEY}" ]] || die "공개키 파일 없음: ${PUBKEY}"
  KEY="$(tr -d '\r' < "${PUBKEY}" | head -1)"
  [[ "${KEY}" =~ ^ssh-ed25519\ [A-Za-z0-9+/=]+ ]] || die "ssh-ed25519 공개키가 아님 (개인키를 넣지 않았는지 확인): ${PUBKEY}"
  OPTS="restrict"
  if [[ -n "${FROM_IP}" ]]; then
    [[ "${FROM_IP}" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] || die "출발IP 는 IPv4: ${FROM_IP}"
    OPTS="${OPTS},from=\"${FROM_IP}\""
  else
    log "참고: 출발IP 없이 등록 → Jenkins MODE=status 로 호출 IP 확인 후 '$0 ${PUBKEY} <IP>' 로 다시 실행"
  fi
  BODY="$(echo "${KEY}" | cut -d' ' -f2)"
  mkdir -p "${HOME}/.ssh"
  chmod 700 "${HOME}/.ssh"
  AK="${HOME}/.ssh/authorized_keys"
  touch "${AK}"
  grep -vF "${BODY}" "${AK}" > "${AK}.new" || true
  echo "${OPTS},command=\"${COMMAND}\" ${KEY}" >> "${AK}.new"
  mv "${AK}.new" "${AK}"
  chmod 600 "${AK}"
  command -v restorecon >/dev/null 2>&1 && restorecon -R "${HOME}/.ssh" 2>/dev/null || true
  log "authorized_keys 등록 (같은 키의 예전 줄은 교체):"
  grep -F "${BODY}" "${AK}"
  ls -Zd "${HOME}/.ssh" "${AK}" 2>/dev/null || true
fi

cat <<MSG

완료. 확인 (ws01, atmswas):
  ${COMMAND} status
  키 확인은 Jenkins nh-atms-backend MODE=status 로 (ws01 셸에서 ssh 는 HIWARE 금지명령)
배포는 파일 교체까지만 — 반영은 ${COMMAND} restart <모듈> (그 모듈의 서버만, deploy-war.env 의 재기동 메뉴)
MSG
