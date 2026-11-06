#!/usr/bin/env bash
# =====================================================================
# wb01(WebtoB) 화면 배포 준비 — nbatmsltwb01, atmswas 계정 (sudo 불필요, 다시 실행해도 안전)
#   배포킷 zip 을 wb01 에 풀어 둔 곳에서 실행:
#     bash <배포킷>/deploy/webtob/install-wb01.sh                       # 폴더·스크립트만
#     bash <배포킷>/deploy/webtob/install-wb01.sh <공개키파일> [출발IP]  # + Jenkins 배포 키 등록
#
#  1) ~/deploy/{bin,inbox,backup} (700)
#  2) deploy-front.sh, swap-deploy.sh → ~/deploy/bin (CRLF 제거, 700, 문법 검사)
#  3) 필요한 명령(flock, curl, sha256sum, tar) 확인
#  4) 홈 atmswas:jeus 750, DocBase ~/docs atmswas:jeus 2750 (WebtoB 의 webatms 가 jeus 그룹으로 읽음)
#  5) (공개키를 주면) ~/.ssh/authorized_keys 에 한 줄 등록 — 같은 키 줄이 있으면 바꿔 씀
#       restrict,from="<출발IP>",command="/home/atmswas/deploy/bin/deploy-front.sh" ssh-ed25519 AAAA… jenkins-wb01-deploy
#     출발IP 기본 16.88.122.113 (ws01). 라벨이 틀리면 restorecon
# 문서: docs/FRONTEND_CICD_PLAN.md 4장
# =====================================================================
set -euo pipefail
umask 027

KIT_DEPLOY="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"   # 배포킷의 deploy/
DEPLOY_HOME="${DEPLOY_HOME:-${HOME}/deploy}"
DOCBASE="${DOCBASE:-${HOME}/docs}"
COMMAND="${DEPLOY_HOME}/bin/deploy-front.sh"
PUBKEY="${1:-}"
FROM_IP="${2:-16.88.122.113}"

log() { echo "[$(date '+%F %T')] $*"; }
die() { echo "[$(date '+%F %T')] [ERROR] $*" >&2; exit 1; }

[[ "$(id -un)" == "atmswas" ]] || log "경고: atmswas 가 아닌 계정($(id -un))으로 실행 중"

# 1) 폴더
mkdir -p "${DEPLOY_HOME}/bin" "${DEPLOY_HOME}/inbox" "${DEPLOY_HOME}/backup"
chmod 700 "${DEPLOY_HOME}"

# 2) 스크립트 설치
for src in "${KIT_DEPLOY}/webtob/deploy-front.sh" "${KIT_DEPLOY}/jeus/swap-deploy.sh"; do
  [[ -f "${src}" ]] || die "배포킷에 없음: ${src}"
  dst="${DEPLOY_HOME}/bin/$(basename "${src}")"
  if [[ "${src}" != "${dst}" ]]; then cp "${src}" "${dst}"; fi
  sed -i 's/\r$//' "${dst}"
  chmod 700 "${dst}"
  bash -n "${dst}" || die "문법 오류: ${dst}"
  log "설치: ${dst}"
done
grep -q 'tar.gz' "${DEPLOY_HOME}/bin/swap-deploy.sh" || die "swap-deploy.sh 가 옛 버전 (tar.gz 미지원) → 최신 배포킷 사용"

# 3) 명령 확인
for c in flock curl sha256sum tar; do
  command -v "${c}" >/dev/null 2>&1 || die "명령 없음: ${c}"
done
log "명령 확인: flock curl sha256sum tar"

# 4) 홈·DocBase 권한
if [[ "$(stat -c '%G' "${HOME}")" != "jeus" ]]; then chgrp jeus "${HOME}" && log "홈 그룹 → jeus"; fi
chmod 750 "${HOME}"
if [[ -d "${DOCBASE}" ]]; then
  [[ "$(stat -c '%G' "${DOCBASE}")" == "jeus" ]] || { chgrp jeus "${DOCBASE}"; log "DocBase 그룹 → jeus"; }
  chmod 2750 "${DOCBASE}"
else
  log "경고: DocBase 없음 (${DOCBASE}) → 첫 배포 때 만들어진다. WebtoB 설정의 DocBase 와 같은지 확인"
fi
ls -ld "${HOME}" "${DOCBASE}" 2>/dev/null || true

# 5) 배포 키 등록 (선택)
if [[ -n "${PUBKEY}" ]]; then
  [[ -f "${PUBKEY}" ]] || die "공개키 파일 없음: ${PUBKEY}"
  [[ "${FROM_IP}" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] || die "출발IP 는 IPv4: ${FROM_IP}"
  KEY="$(tr -d '\r' < "${PUBKEY}" | head -1)"
  [[ "${KEY}" =~ ^ssh-ed25519\ [A-Za-z0-9+/=]+ ]] || die "ssh-ed25519 공개키가 아님 (개인키를 넣지 않았는지 확인): ${PUBKEY}"
  BODY="$(echo "${KEY}" | cut -d' ' -f2)"
  mkdir -p "${HOME}/.ssh"
  chmod 700 "${HOME}/.ssh"
  AK="${HOME}/.ssh/authorized_keys"
  touch "${AK}"
  grep -vF "${BODY}" "${AK}" > "${AK}.new" || true
  echo "restrict,from=\"${FROM_IP}\",command=\"${COMMAND}\" ${KEY}" >> "${AK}.new"
  mv "${AK}.new" "${AK}"
  chmod 600 "${AK}"
  command -v restorecon >/dev/null 2>&1 && restorecon -R "${HOME}/.ssh" 2>/dev/null || true
  log "authorized_keys 등록 (같은 키의 예전 줄은 교체):"
  grep -F "${BODY}" "${AK}"
  ls -Zd "${HOME}/.ssh" "${AK}" 2>/dev/null || true
fi

cat <<MSG

완료. 다음 (FRONTEND_CICD_PLAN 4장):
  교체 연습 : tar -C ~/docs -czf ~/front-test.tar.gz . 후 ${COMMAND} deploy 20261021-0 <sha256> < ~/front-test.tar.gz
  상태      : ${COMMAND} status
MSG
