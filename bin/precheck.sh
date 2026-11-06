#!/usr/bin/env bash
# 서버 사전 점검 (읽기 전용 / root 권한 불필요)
# 주의: sudo, su 등 HIWARE 금지 명령은 절대 사용하지 않음
set -uo pipefail

ok()   { echo "  [OK]   $*"; }
warn() { echo "  [WARN] $*"; }
ng()   { echo "  [NG]   $*"; }
sec()  { echo; echo "== $* =="; }

PORT="${1:-8080}"

sec "OS / 자원"
cat /etc/redhat-release 2>/dev/null
echo "  CPU: $(nproc) core / MEM: $(free -h | awk '/Mem:/{print $2" total, "$7" available"}')"
df -h "${HOME}" | tail -1 | awk '{print "  HOME 디스크: "$2" 중 "$4" 여유 ("$6")"}'

sec "Podman"
if command -v podman >/dev/null 2>&1; then
  ok "$(podman --version)"
else
  ng "podman 미설치 -> 인프라팀 설치 요청 필요"; exit 1
fi
podman info --format '  rootless={{.Host.Security.Rootless}} cgroup={{.Host.CgroupsVersion}} network={{.Host.NetworkBackend}}' 2>/dev/null \
  || ng "podman info 실패 (subuid/subgid, XDG_RUNTIME_DIR 확인)"
podman info --format '  graphRoot={{.Store.GraphRoot}}' 2>/dev/null
command -v podman-compose >/dev/null 2>&1 && ok "podman-compose 있음" || warn "podman-compose 없음 (스크립트/Quadlet 방식 사용하므로 필수 아님)"

sec "subuid / subgid (rootless 필수)"
grep -q "^${USER}:" /etc/subuid 2>/dev/null && ok "subuid: $(grep "^${USER}:" /etc/subuid)" || ng "/etc/subuid 에 ${USER} 없음 -> 인프라팀 요청"
grep -q "^${USER}:" /etc/subgid 2>/dev/null && ok "subgid: $(grep "^${USER}:" /etc/subgid)" || ng "/etc/subgid 에 ${USER} 없음 -> 인프라팀 요청"

sec "systemd --user / linger (재부팅 후 자동기동)"
echo "  XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR:-<없음>}"
LINGER="$(loginctl show-user "${USER}" -p Linger 2>/dev/null | cut -d= -f2)"
[[ "${LINGER}" == "yes" ]] && ok "Linger=yes" || warn "Linger=${LINGER:-unknown} -> 로그아웃/재부팅 시 컨테이너 중지됨, 인프라팀에 enable-linger 요청"
systemctl --user is-system-running >/dev/null 2>&1 && ok "systemd --user 사용 가능" || warn "systemd --user 상태: $(systemctl --user is-system-running 2>&1)"

sec "SELinux"
command -v getenforce >/dev/null 2>&1 && echo "  $(getenforce)  (Enforcing 이면 볼륨 마운트에 :Z 필요 - 스크립트에 반영됨)"

sec "포트 ${PORT} 사용 여부"
if ss -tln 2>/dev/null | awk '{print $4}' | grep -qE "[:.]${PORT}\$"; then
  ng "${PORT} 포트 이미 사용 중 -> conf/atms.env 의 HOST_PORT 변경"
else
  ok "${PORT} 포트 사용 가능"
fi
echo "  현재 LISTEN 포트: $(ss -tln 2>/dev/null | awk 'NR>1{print $4}' | sed 's/.*://' | sort -n | uniq | tr '\n' ' ')"

sec "로그 디렉토리 후보"
for d in /WASlogs /nhod "${HOME}"; do
  [[ -w "$d" ]] && ok "$d 쓰기 가능" || warn "$d 쓰기 불가"
done

sec "JDK (~/atms/jdk)"
JDK="$(cd "$(dirname "$0")/.." && pwd)/jdk/bin/java"
[[ -x "${JDK}" ]] && ok "$("${JDK}" -version 2>&1 | head -1)" || warn "${JDK} 없음 (jdk-21.0.12_linux 업로드 필요)"

sec "Podman 이미지"
podman images 2>/dev/null || true
echo
echo "점검 완료. [NG]/[WARN] 항목은 가이드의 '인프라팀 요청' 절을 참고하세요."
