#!/usr/bin/env bash
# 앱 이미지 빌드:  bin/build.sh <앱> <jar>   -> localhost/<앱>:<yyyyMMdd-HHmmss>
# 마지막 줄에 태그 출력 (deploy.sh 가 사용)
source "$(dirname "$0")/common.sh" "${1:-}"; shift || true
JAR="${1:-}"
[[ -n "${APP}" && -f "${JAR}" ]] || die "사용법: build.sh <앱> <jar>"
image_exists "${BASE_IMAGE}" || die "베이스 이미지 없음: ${BASE_IMAGE} (bin/load-image.sh 로 먼저 적재)"

TAG="$(date '+%Y%m%d-%H%M%S')"
rm -rf "${BUILD_DIR}" && mkdir -p "${BUILD_DIR}"
cp "${JAR}" "${BUILD_DIR}/app.jar"
cp "${KIT_DIR}/deploy/${DOCKERFILE}" "${BUILD_DIR}/Dockerfile"
if [[ "${DOCKERFILE}" == "Dockerfile.jdk-local" ]]; then
  [[ -x "${JDK_DIR}/bin/java" ]] || die "${JDK_DIR}/bin/java 없음 (jdk-21.0.12_linux 업로드 필요)"
  cp -r "${JDK_DIR}" "${BUILD_DIR}/jdk"
fi

log "빌드 시작: ${IMAGE_NAME}:${TAG} (base=${BASE_IMAGE})" >&2
podman build --pull=never \
  --build-arg BASE_IMAGE="${BASE_IMAGE}" \
  --label "atms.app=${APP}" --label "atms.jar=$(basename "${JAR}")" \
  -t "${IMAGE_NAME}:${TAG}" "${BUILD_DIR}" >&2
log "빌드 완료: ${IMAGE_NAME}:${TAG}" >&2
echo "${TAG}"
