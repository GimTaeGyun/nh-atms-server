#!/usr/bin/env bash
# =====================================================================
# Jenkins 컨테이너 생성·재생성 (ws01, atmswas, rootless Podman, sudo 불필요)
#   ~/atms/deploy/jenkins/run-jenkins.sh
#   데이터(home)·캐시는 그대로 두고 컨테이너만 다시 만든다 → 설정 변경·이미지 교체 때 다시 실행하면 된다
#
#  1) 이미지 확인 (podman load 로 미리 적재: localhost/nh-atms-jenkins:bank)
#  2) ~/atms/infra/jenkins/{home,cache/gradle,cache/npm} 생성 (700) → 컨테이너 jenkins(1000) 소유가 아니면 맞춤
#     --userns=keep-id 는 쓰지 않는다 (이 서버 podman 5.8.2 / crun 1.27 에서 기동 실패, 2026-10-14)
#  3) jenkins.env 가 없으면 jenkins.env.example 복사 → CHANGE_ME 가 남아 있으면 멈춤 (바꾸고 다시 실행)
#     예전 수동 jenkins.env 에 GRADLE_USER_HOME·npm_config_cache 가 없으면 추가
#  4) podman 네트워크 ATMS_NETWORK(기본 atms-net) — Gitea 와 컨테이너 이름으로 접속
#  5) podman rm -f → podman run --restart=always (+ podman-restart.service 로 재부팅 자동기동, linger 필요)
#     자원 제한: memory 만 (CPU 제한 보류, 2026-10-16 결정). JENKINS_CPUS 를 지정하고 cpu 가 위임돼 있을 때만 --cpus
#  6) http://127.0.0.1:<포트>/login 200 대기
# 환경변수: JENKINS_DIR(기본 ~/atms/infra/jenkins) JENKINS_IMAGE JENKINS_PORT(8090) JENKINS_MEMORY(2g) JENKINS_CPUS ATMS_NETWORK
# 문서: docs/DEV_SERVER_SETUP.md 9단계
# =====================================================================
set -euo pipefail

KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="${JENKINS_IMAGE:-localhost/nh-atms-jenkins:bank}"
J_DIR="${JENKINS_DIR:-${HOME}/atms/infra/jenkins}"
ENV_FILE="${J_DIR}/jenkins.env"
PORT="${JENKINS_PORT:-8090}"
NAME=atms-jenkins
NETWORK="${ATMS_NETWORK:-atms-net}"

log() { echo "[$(date '+%F %T')] $*"; }
die() { echo "[$(date '+%F %T')] [ERROR] $*" >&2; exit 1; }

podman image exists "${IMAGE}" || die "이미지 없음: ${IMAGE} → podman load -i ~/atms/images/<jenkins 이미지>.tar"

mkdir -p "${J_DIR}/home" "${J_DIR}/cache/gradle" "${J_DIR}/cache/npm"
chmod 700 "${J_DIR}"

# 컨테이너 jenkins(1000) 소유로 (이미 맞으면 건너뜀 — 캐시는 수만 개 파일이라 매번 chown 하지 않는다)
for d in home cache/gradle cache/npm; do
  # 맨 위 폴더만 보면 나중에 mv 로 넣은 하위 폴더(offline-seed 등)를 놓친다 → 1000 이 아닌 파일이 하나라도 있으면 맞춤
  owner="$(podman unshare find "${J_DIR}/${d}" \( ! -uid 1000 -o ! -gid 1000 \) -print -quit)"
  if [[ -n "${owner}" ]]; then
    log "소유자 맞춤: ${d} (예: ${owner} → 1000:1000)"
    podman unshare chown -R 1000:1000 "${J_DIR}/${d}"
  fi
done

if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${KIT_DIR}/jenkins.env.example" "${ENV_FILE}"
  chmod 600 "${ENV_FILE}"
  die "생성: ${ENV_FILE} → vi 로 JENKINS_ADMIN_PASSWORD(CHANGE_ME) 를 바꾼 뒤 다시 실행"
fi
chmod 600 "${ENV_FILE}"
grep -q 'CHANGE_ME' "${ENV_FILE}" && die "${ENV_FILE} 에 CHANGE_ME 가 남아 있음 → 바꾼 뒤 다시 실행"
sed -i 's/\r$//' "${ENV_FILE}"
for kv in GRADLE_USER_HOME=/cache/gradle npm_config_cache=/cache/npm; do
  grep -q "^${kv%%=*}=" "${ENV_FILE}" || { echo "${kv}" >> "${ENV_FILE}"; log "jenkins.env 에 추가: ${kv}"; }
done

CTRL=" $(podman info --format '{{range .Host.CgroupControllers}}{{.}} {{end}}' 2>/dev/null || true) "
LIMITS=()
[[ -n "${JENKINS_CPUS:-}" && "${CTRL}" == *" cpu "* ]] && LIMITS+=("--cpus=${JENKINS_CPUS}")
[[ "${CTRL}" == *" memory "* ]] && LIMITS+=("--memory=${JENKINS_MEMORY:-2g}")
log "cgroup 컨트롤러:${CTRL}→ 자원 제한: ${LIMITS[*]:-(없음)}"

if ! podman network exists "${NETWORK}"; then
  podman network create "${NETWORK}" >/dev/null
  log "네트워크 생성: ${NETWORK}"
fi

if podman container exists "${NAME}"; then
  log "기존 컨테이너 삭제 (데이터는 ${J_DIR}/home 에 남음)"
  podman rm -f "${NAME}" >/dev/null
fi
podman run -d --name "${NAME}" --restart=always \
  --network "${NETWORK}" -p "${PORT}:8080" \
  --env-file "${ENV_FILE}" \
  -v "${J_DIR}/home:/var/jenkins_home:Z" \
  -v "${J_DIR}/cache/npm:/cache/npm:Z" \
  -v "${J_DIR}/cache/gradle:/cache/gradle:Z" \
  "${LIMITS[@]}" \
  "${IMAGE}" >/dev/null
log "기동: ${NAME} (${IMAGE}, 네트워크 ${NETWORK}, 포트 ${PORT})"

LINGER="$(loginctl show-user "${USER:-$(id -un)}" -p Linger 2>/dev/null | cut -d= -f2 || true)"
if [[ "${LINGER}" == "yes" ]]; then
  systemctl --user enable podman-restart.service >/dev/null 2>&1 \
    && log "재부팅 자동기동: podman-restart.service enabled" \
    || log "경고: podman-restart.service enable 실패 → systemctl --user enable podman-restart.service"
else
  log "Linger 미설정 → 로그아웃·재부팅 때 멈춤 (ROOT_DAY_CHECKLIST)"
fi

log "기동 대기: http://127.0.0.1:${PORT}/login (최대 3분)"
for i in $(seq 1 36); do
  code="$(curl -s -m 3 -o /dev/null -w '%{http_code}' "http://127.0.0.1:${PORT}/login" || true)"
  if [[ "${code}" == "200" ]]; then
    log "Jenkins 정상 기동 (${i}회차)"
    podman exec "${NAME}" sh -c 'echo "  GRADLE_USER_HOME=$GRADLE_USER_HOME npm_config_cache=$npm_config_cache"; echo "  플러그인 $(ls /var/jenkins_home/plugins/*.jpi 2>/dev/null | wc -l)개"'
    exit 0
  fi
  sleep 5
done
podman logs --tail 50 "${NAME}" 2>&1 | sed 's/^/    /' || true
die "Jenkins 응답 없음 (위 로그 확인)"
