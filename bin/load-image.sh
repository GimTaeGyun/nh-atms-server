#!/usr/bin/env bash
# 반입한 이미지 tar 적재:  bin/load-image.sh images/temurin-21-jre-jammy.tar.gz [...]
source "$(dirname "$0")/common.sh"
[[ $# -ge 1 ]] || die "사용법: $0 <image.tar|image.tar.gz> [...]"
for f in "$@"; do
  [[ -f "$f" ]] || die "파일 없음: $f"
  log "이미지 적재: $f"
  podman load -i "$f"
done
podman images
