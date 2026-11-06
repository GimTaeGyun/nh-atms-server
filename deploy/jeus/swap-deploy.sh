#!/usr/bin/env bash
# =====================================================================
# WAR / dist(zip, tar.gz) 를 "새 폴더에 풀고 → 통째로 교체" 하는 배포 스크립트
#   - JEUS: 모듈 6개를 각자의 폴더에 배포 (atmswas 계정으로 실행)
#       2026-10-26 구성: 도메인 1개 + 매니지드 서버 6개, 서버마다 앱 1개·컨텍스트 '/'
#       atms-web          → /home/atmswas/catms          (atms01  :8080) ← 환경구성표 docBase 그대로
#       atms-journal      → /home/atmswas/cajou  (ajou01 :8081)
#       atms-batch        → /home/atmswas/cabat    (abat01   :8082)
#       atms-file         → /home/atmswas/cfile     (file01    :8083)
#       atms-if-internal  → /home/atmswas/cifin     (ifin01    :8084)
#       atms-if-external  → /home/atmswas/cifex     (ifex01    :8085)
#     (catms 안에 다른 모듈을 넣지 않는다: '/' 앱의 정적 경로로 WEB-INF 가 노출될 수 있음)
#   - WebtoB DocBase(/home/atmswas/docs, nbatmsltwb01) 도 같은 방식으로 사용 가능 (대상폴더 직접 지정)
#       Jenkins 화면 배포는 deploy/webtob/deploy-front.sh 가 이 스크립트를 tar.gz 로 호출한다
#   - 기존 폴더 위에 덮어 풀지 않으므로, 이전 버전에서 삭제된 클래스/jar 가 남지 않는다
#
# 사용:
#   swap-deploy.sh deploy   <war|zip|tar.gz> [대상폴더]   # WAR 이름이 atms-<모듈>-<버전>... 이면 대상 자동 (위 표)
#   swap-deploy.sh rollback <대상폴더|모듈명>      # 직전 백업으로 되돌림 (예: rollback atms-journal)
#   swap-deploy.sh list     <대상폴더|모듈명>      # 백업 목록
# 환경변수:
#   APP_HOME    (기본 /home/atmswas)         모듈 폴더(catms·cajou·…)의 상위 경로
#   DEPLOY_HOME (기본 /home/atmswas/deploy)  백업·이력 위치
#   KEEP        (기본 5)                     보관할 백업 개수 (모듈별)
#   PRESERVE    (기본 "error.jsp error")     기존 폴더에서 새 폴더로 옮겨 유지할 항목(은행 에러페이지)
# 재기동은 별도: deploy-war.sh restart <모듈> (그 모듈의 서버만, jeusRstart 메뉴)
# =====================================================================
set -euo pipefail

APP_HOME="${APP_HOME:-/home/atmswas}"
DEPLOY_HOME="${DEPLOY_HOME:-/home/atmswas/deploy}"
KEEP="${KEEP:-5}"
PRESERVE="${PRESERVE:-error.jsp error}"

log() { echo "[$(date '+%F %T')] $*"; }
die() { echo "[$(date '+%F %T')] [ERROR] $*" >&2; exit 1; }

# 모듈 → JEUS 배포 폴더 (각 매니지드 서버의 docBase)
module_dir() {
  case "$1" in
    atms-web)         echo "${APP_HOME}/catms" ;;
    atms-journal)     echo "${APP_HOME}/cajou" ;;
    atms-batch)       echo "${APP_HOME}/cabat" ;;
    atms-file)        echo "${APP_HOME}/cfile" ;;
    atms-if-internal) echo "${APP_HOME}/cifin" ;;
    atms-if-external) echo "${APP_HOME}/cifex" ;;
    *) return 1 ;;
  esac
}

# 절대경로는 그대로, 'atms-journal' 같은 모듈명은 module_dir 로
resolve_target() {
  case "$1" in
    /*) echo "$1" ;;
    *) module_dir "$1" || die "대상은 절대경로 또는 모듈명(atms-web, atms-journal, atms-batch, atms-file, atms-if-internal, atms-if-external): $1" ;;
  esac
}

CMD="${1:-}"
case "${CMD}" in
  deploy)
    ARCHIVE="${2:-}"
    if [[ -n "${3:-}" ]]; then
      TARGET="$(resolve_target "$3")"
    elif [[ "$(basename "${ARCHIVE}")" =~ ^(atms-[a-z]+(-[a-z]+)*)-[0-9].*\.war$ ]] && module_dir "${BASH_REMATCH[1]}" >/dev/null; then
      TARGET="$(module_dir "${BASH_REMATCH[1]}")"
    else
      die "대상폴더를 지정하세요 (WAR 이름에서 모듈을 알 수 없음): $0 deploy <war|zip|tar.gz> <대상폴더>"
    fi
    ;;
  rollback|list)
    [[ -n "${2:-}" ]] || die "사용법: $0 ${CMD} <대상폴더|모듈명>   (예: ${CMD} atms-journal)"
    TARGET="$(resolve_target "$2")"
    ;;
  *) die "사용법: $0 deploy <war|zip|tar.gz> [대상폴더] | rollback <대상폴더|모듈명> | list <대상폴더|모듈명>" ;;
esac
TARGET="${TARGET%/}"
NAME="$(basename "${TARGET}")"
BACKUP_DIR="${DEPLOY_HOME}/backup"
mkdir -p "${BACKUP_DIR}"

backups() { ls -1dt "${BACKUP_DIR}/${NAME}"-[0-9]* 2>/dev/null || true; }

case "${CMD}" in
  list)
    backups
    ;;

  deploy)
    [[ -f "${ARCHIVE}" ]] || die "파일 없음: ${ARCHIVE}"
    if [[ -f "${ARCHIVE}.sha256" ]]; then
      ( cd "$(dirname "${ARCHIVE}")" && sha256sum -c "$(basename "${ARCHIVE}").sha256" ) || die "SHA256 불일치"
    else
      log "경고: ${ARCHIVE}.sha256 없음 → 무결성 확인 생략"
    fi

    TS="$(date '+%Y%m%d%H%M%S')"
    NEW="${TARGET}.new-${TS}"
    mkdir -p "${NEW}"
    log "압축 해제: $(basename "${ARCHIVE}") → ${NEW}"
    case "${ARCHIVE}" in
      *.tar.gz|*.tgz)
        tar -xzf "${ARCHIVE}" -C "${NEW}" ;;
      *)
        if command -v unzip >/dev/null 2>&1; then
          unzip -q "${ARCHIVE}" -d "${NEW}"
        else
          python3 -I -m zipfile -e "${ARCHIVE}" "${NEW}"
        fi ;;
    esac

    # zip 안에 최상위 폴더 하나만 있으면(예: dist/) 한 단계 끌어올림
    mapfile -t TOP < <(ls -A "${NEW}")
    if [[ ${#TOP[@]} -eq 1 && -d "${NEW}/${TOP[0]}" && "${TOP[0]}" != "WEB-INF" ]]; then
      log "최상위 폴더 '${TOP[0]}' 내용을 대상 루트로 이동"
      mv "${NEW}/${TOP[0]}" "${NEW}.inner" && rmdir "${NEW}" && mv "${NEW}.inner" "${NEW}"
    fi

    # WAR 정리: 내장 톰캣(lib-provided) 은 외부 WAS 에서 사용하지 않음
    rm -rf "${NEW}/WEB-INF/lib-provided"
    if [[ -d "${NEW}/WEB-INF" ]]; then
      [[ -d "${NEW}/WEB-INF/classes" ]] || die "WEB-INF/classes 없음 → 올바른 WAR 가 아님"
    else
      [[ -f "${NEW}/index.html" ]] || log "경고: WEB-INF, index.html 모두 없음 → 내용 확인 필요"
    fi

    # 기존 폴더의 유지 항목(은행 에러페이지 등) 이어받기, 권한·그룹도 기존과 동일하게
    if [[ -d "${TARGET}" ]]; then
      for p in ${PRESERVE}; do
        if [[ -e "${TARGET}/${p}" && ! -e "${NEW}/${p}" ]]; then
          cp -a "${TARGET}/${p}" "${NEW}/${p}" && log "유지: ${p}"
        fi
      done
      GRP="$(stat -c '%G' "${TARGET}")"
      chmod --reference="${TARGET}" "${NEW}" 2>/dev/null || true
    else
      # 첫 배포: atmswas 의 기본 그룹은 atms 라, 상위(/home/atmswas = jeus) 그룹을 따라야 JEUS 가 읽는다
      GRP="$(stat -c '%G' "$(dirname "${TARGET}")")"
      chmod 2750 "${NEW}"
    fi
    chgrp -R "${GRP}" "${NEW}" 2>/dev/null || log "경고: 그룹 ${GRP} 지정 실패 → ls -ld 로 확인"
    chmod -R g+rX "${NEW}"

    # 교체
    if [[ -d "${TARGET}" ]]; then
      mv "${TARGET}" "${BACKUP_DIR}/${NAME}-${TS}"
      log "백업: ${BACKUP_DIR}/${NAME}-${TS}"
    fi
    mv "${NEW}" "${TARGET}"
    echo "${TS} deploy $(basename "${ARCHIVE}") -> ${TARGET}" >> "${DEPLOY_HOME}/deploy-history.log"
    log "교체 완료: ${TARGET}"

    # 오래된 백업 정리
    backups | tail -n +"$((KEEP + 1))" | while read -r old; do rm -rf "${old}"; log "백업 정리: ${old}"; done
    if [[ -d "${TARGET}/WEB-INF" ]]; then
      PROFILE="$(grep -A1 'spring.profiles.active' "${TARGET}/WEB-INF/web.xml" 2>/dev/null | sed -n 's/.*<param-value>\([^<]*\)<.*/\1/p')"
      if [[ -n "${PROFILE}" ]]; then
        log "WAR 프로파일: ${PROFILE}"
      else
        log "경고: WEB-INF/web.xml 에 프로파일 지정 없음 → -Pjeus 로 빌드한 WAR 인지 확인"
      fi
      log "다음: deploy-war.sh restart <모듈> (그 모듈의 서버만 재기동)"
    else
      log "정적 파일(WebtoB) 배포 → 재기동 불필요. 브라우저 캐시 새로고침(Ctrl+F5) 후 확인"
    fi
    ;;

  rollback)
    LAST="$(backups | head -1)"
    [[ -n "${LAST}" ]] || die "되돌릴 백업 없음 (${BACKUP_DIR}/${NAME}-*)"
    TS="$(date '+%Y%m%d%H%M%S')"
    if [[ -d "${TARGET}" ]]; then
      mv "${TARGET}" "${BACKUP_DIR}/${NAME}.failed-${TS}"
      log "현재 버전 보관: ${BACKUP_DIR}/${NAME}.failed-${TS}"
    fi
    mv "${LAST}" "${TARGET}"
    echo "${TS} rollback $(basename "${LAST}") -> ${TARGET}" >> "${DEPLOY_HOME}/deploy-history.log"
    log "롤백 완료: ${LAST} → ${TARGET}"
    if [[ -d "${TARGET}/WEB-INF" ]]; then
      log "다음: deploy-war.sh restart <모듈> (그 모듈의 서버만 재기동)"
    else
      log "정적 파일(WebtoB) → 재기동 불필요"
    fi
    ;;
esac
