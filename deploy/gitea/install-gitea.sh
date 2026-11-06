#!/usr/bin/env bash
# =====================================================================
# Gitea 설치·기동 (atmswas, rootless Podman, sudo 불필요)
#   deploy/gitea/install-gitea.sh <서버IP>
#
#  1) 이미지 확인 (podman load 로 미리 적재)
#  2) ~/atms/infra/gitea/{data,config} 생성 (700) + 컨테이너 git(1000) 소유로 맞춤 (podman unshare chown)
#     --userns=keep-id 는 쓰지 않는다: 이 서버(podman 5.8.2 / crun 1.27)에서
#     'crun: readlink ``: No such file or directory' 로 기동 실패 (2026-10-14 확인)
#  3) gitea.env 생성 (600) + 서버 IP 반영(다시 실행하면 새 IP 로 갱신) + 비밀키 자동 생성
#     웹훅 허용 목록(ALLOWED_HOST_LIST)은 기본 private 그대로: 웹훅은 같은 네트워크의 atms-jenkins(10.89.x)로 보낸다
#  4) linger=yes 면 Quadlet(재부팅 자동기동), 아니면 podman run
#     네트워크: Jenkins 와 같은 podman 네트워크 ATMS_NETWORK(기본 atms-net, 없으면 생성)
#     → Gitea↔Jenkins 를 컨테이너 이름으로 접속 (rootless pasta 기본 모드에서는 서버 IP·host.containers.internal 로 서로 안 닿음, 2026-10-22 확인)
#     자원 제한은 이 계정에 위임된 cgroup 컨트롤러만 사용
#     기본 GITEA_MEMORY=768m. CPU 제한은 보류(2026-10-16 결정) → GITEA_CPUS=0.5 처럼 지정할 때만, cpu 가 위임돼 있으면 적용
#  5) http://127.0.0.1:3000/api/healthz 응답 확인
# 관리자 계정 생성은 출력되는 안내 명령으로 별도 실행
# =====================================================================
set -euo pipefail

KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="${GITEA_IMAGE:-docker.io/gitea/gitea:latest-rootless}"
G_DIR="${GITEA_DIR:-${HOME}/atms/infra/gitea}"
ENV_FILE="${G_DIR}/gitea.env"
QUADLET_DIR="${HOME}/.config/containers/systemd"
NAME=atms-gitea
NETWORK="${ATMS_NETWORK:-atms-net}"

log() { echo "[$(date '+%F %T')] $*"; }
die() { echo "[$(date '+%F %T')] [ERROR] $*" >&2; exit 1; }

SERVER_IP="${1:-}"
[[ -n "${SERVER_IP}" ]] || die "사용법: $0 <서버IP>   (PC 가 접속하는 IPv4, 예: 16.88.122.113)"
[[ "${SERVER_IP}" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] \
  || die "서버 IP 는 IPv4 로 입력: ${SERVER_IP} (hostname -I 의 fe80::... 는 IPv6 내부 주소라 PC 에서 접속 불가)"

podman image exists "${IMAGE}" || die "이미지 없음: ${IMAGE} → podman load -i ~/atms/images/gitea-rootless.tar"

mkdir -p "${G_DIR}/data" "${G_DIR}/config" "${HOME}/atms/backup/gitea"   # 매일 dump 는 데이터 폴더 밖 ~/atms/backup/gitea (T65)
chmod 700 "${G_DIR}"
# 컨테이너 안 git(1000) 이 쓸 수 있게: 호스트에서는 atmswas 의 subuid 소유가 된다 (atmswas 로 직접 지우려면 podman unshare 사용)
podman unshare chown -R 1000:1000 "${G_DIR}/data" "${G_DIR}/config"

if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${KIT_DIR}/gitea.env.example" "${ENV_FILE}"
  log "생성: ${ENV_FILE}"
fi
chmod 600 "${ENV_FILE}"
sed -i -e "s|^GITEA__server__DOMAIN=.*|GITEA__server__DOMAIN=${SERVER_IP}|" \
       -e "s|^GITEA__server__ROOT_URL=.*|GITEA__server__ROOT_URL=http://${SERVER_IP}:3000/|" "${ENV_FILE}"

gen_secret() {
  podman run --rm --entrypoint /usr/local/bin/gitea "${IMAGE}" generate secret "$1" | tail -1 | tr -d '\r\n'
}
for key in SECRET_KEY INTERNAL_TOKEN; do
  if grep -q "^GITEA__security__${key}=$" "${ENV_FILE}"; then
    val="$(gen_secret "${key}")"
    [[ -n "${val}" ]] || die "${key} 생성 실패"
    sed -i "s|^GITEA__security__${key}=$|GITEA__security__${key}=${val}|" "${ENV_FILE}"
    log "${key} 생성 완료"
  fi
done

# rootless 에 위임된 cgroup 컨트롤러 (RHEL 9 기본: memory pids 만, cpu 는 root 가 위임해야 함)
CTRL=" $(podman info --format '{{range .Host.CgroupControllers}}{{.}} {{end}}' 2>/dev/null || true) "
LIMITS=()
[[ -n "${GITEA_CPUS:-}" && "${CTRL}" == *" cpu "* ]] && LIMITS+=("--cpus=${GITEA_CPUS}")
[[ "${CTRL}" == *" memory "* ]] && LIMITS+=("--memory=${GITEA_MEMORY:-768m}")
log "cgroup 컨트롤러:${CTRL}→ 자원 제한: ${LIMITS[*]:-(없음)}"
[[ -n "${GITEA_CPUS:-}" ]] || log "참고: CPU 제한 보류 중 → CPU 제한 없이 기동 (ROOT_DAY_CHECKLIST 2-9 참고)"

if ! podman network exists "${NETWORK}"; then
  podman network create "${NETWORK}" >/dev/null
  log "네트워크 생성: ${NETWORK}"
fi

LINGER="$(loginctl show-user "${USER:-$(id -un)}" -p Linger 2>/dev/null | cut -d= -f2 || true)"
podman rm -f "${NAME}" >/dev/null 2>&1 || true

if [[ "${LINGER}" == "yes" ]]; then
  mkdir -p "${QUADLET_DIR}"
  if (( ${#LIMITS[@]} )); then ARGS_SED="s|^PodmanArgs=.*|PodmanArgs=${LIMITS[*]}|"; else ARGS_SED="/^PodmanArgs=/d"; fi
  sed -e "s|^Image=.*|Image=${IMAGE}|" -e "s|^Network=.*|Network=${NETWORK}|" -e "${ARGS_SED}" "${KIT_DIR}/atms-gitea.container" > "${QUADLET_DIR}/${NAME}.container"
  systemctl --user daemon-reload
  systemctl --user restart "${NAME}.service"
  log "Quadlet 으로 기동 (재부팅 시 자동 기동)"
else
  log "Linger 미설정 → podman run 으로 기동 (서버 재부팅 시 이 스크립트 재실행 필요)"
  podman run -d --name "${NAME}" --restart=unless-stopped \
    -p 3000:3000 \
    --network "${NETWORK}" \
    --env-file "${ENV_FILE}" \
    -v "${G_DIR}/data:/var/lib/gitea:Z" \
    -v "${G_DIR}/config:/etc/gitea:Z" \
    "${LIMITS[@]}" \
    "${IMAGE}" >/dev/null
fi

log "기동 대기: http://127.0.0.1:3000/api/healthz"
for i in $(seq 1 30); do
  if curl -fsS -m 3 http://127.0.0.1:3000/api/healthz >/dev/null 2>&1; then
    log "Gitea 정상 기동 (${i}회차)"
    cat <<MSG

다음 단계 — 관리자 계정 생성 (비밀번호는 화면에 한 번 출력됨, 첫 로그인 시 변경):
  podman exec ${NAME} gitea admin user create --admin --username <관리자ID> --email <메일> --random-password --must-change-password

접속: http://${SERVER_IP}:3000/   (PC 에서 접속하려면 방화벽 3000 개방 필요)
MSG
    exit 0
  fi
  sleep 4
done
podman logs --tail 50 "${NAME}" 2>&1 | sed 's/^/    /' || true
die "Gitea 응답 없음 (위 로그 확인)"
