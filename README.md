# ATMS-Server 배포킷

개발서버 `nbatmsltws01` 의 **`/home/atmswas/atms`** 에 두고 **`atmswas` 계정**으로 쓰는 배포킷: NH-ATMS WAR 6개의 JEUS 배포(`deploy/jeus/swap-deploy.sh`), 지원 인프라(Gitea·Jenkins, rootless Podman), 운영 문서.
2026-10-12 부터 `/home/userATMS` 는 사용하지 않는다.

- **인프라 구성 매뉴얼 (통합본, 인수인계용 — 반입물 제작·서버 셋팅·Gitea·Jenkins·배포·JEUS/WebtoB 연동·2호기·운영·백업·보안·트러블슈팅)**: [docs/INFRA_MANUAL.md](docs/INFRA_MANUAL.md)
- 현업 실제 값(재기동 메뉴·서버 이름·포트·registration-id·JNDI·L4) 반영 명령: [docs/POST_HANDOVER_EDITS.md](docs/POST_HANDOVER_EDITS.md)
- **개발서버 셋팅 순서 (atmswas 기준, 처음 여기부터)**: [docs/DEV_SERVER_SETUP.md](docs/DEV_SERVER_SETUP.md)
- 작업 이력·남은 일 (날짜순): [docs/WORK_LOG.md](docs/WORK_LOG.md)
- Jenkins 잡(프론트·백엔드)·Gitea 웹훅 설정 순서: [docs/BACKEND_CICD_PLAN.md](docs/BACKEND_CICD_PLAN.md)
- (임시) 방화벽·JEUS 전 jar 6개 기동 + WebtoB 화면 연동: [docs/JAR_PREVIEW_GUIDE.md](docs/JAR_PREVIEW_GUIDE.md)

- 개발서버 인프라 구성도(서버·계정·포트·흐름·미확인 사항): [docs/DEV_SERVER_INFRA.md](docs/DEV_SERVER_INFRA.md)
- (보류) 컨테이너 대안 경로 기동: [docs/PROTOTYPE_QUICKSTART.md](docs/PROTOTYPE_QUICKSTART.md)
- 필요 파일 총정리(반입 체크리스트): [docs/REQUIRED_FILES.md](docs/REQUIRED_FILES.md)
- root 권한(1일) 작업 체크리스트: [docs/ROOT_DAY_CHECKLIST.md](docs/ROOT_DAY_CHECKLIST.md)
- 서버 폴더 구조 & WAR 반입·배포 위치: [docs/SERVER_DIRECTORY_LAYOUT.md](docs/SERVER_DIRECTORY_LAYOUT.md)
- JEUS 배포를 위한 NH-ATMS 소스 변경 설명 (WAR 6개, 컨텍스트 경로 분리, 게이트웨이): [docs/JEUS_SOURCE_CHANGES.md](docs/JEUS_SOURCE_CHANGES.md)
- WAS 담당 요청서·가이드 (서버 5개 생성, atms01 8080, 커넥터, 재기동 메뉴): [docs/WAS_REQUEST_MANAGED_SERVERS.md](docs/WAS_REQUEST_MANAGED_SERVERS.md)
- JEUS 매니지드 서버 6개 구성 결정 (개념·고민 과정·변경 범위·WAS 요청문): [docs/JEUS_MANAGED_SERVERS_DECISION.md](docs/JEUS_MANAGED_SERVERS_DECISION.md)
- JEUS 멀티 포트 구성 비교 (서버1+리스너6 / 서버6 / JEUS6, 바뀌는 것): [docs/JEUS_MULTI_PORT_OPTIONS.md](docs/JEUS_MULTI_PORT_OPTIONS.md)
- JEUS(백엔드 WAR 6개) + WebtoB(프론트) 배포: [docs/JEUS_WEBTOB_DEPLOY_GUIDE.md](docs/JEUS_WEBTOB_DEPLOY_GUIDE.md)
- 화면 빌드·배포 (Gitea + Jenkins → SSH → WebtoB): [docs/FRONTEND_CICD_PLAN.md](docs/FRONTEND_CICD_PLAN.md)
- 개발서버 Git(Gitea) 구축·운영: [docs/GITEA_SETUP_GUIDE.md](docs/GITEA_SETUP_GUIDE.md)
- 매뉴얼 & 셋팅 계획: [docs/SERVER_SETUP_GUIDE.md](docs/SERVER_SETUP_GUIDE.md)
- 저장소 브랜치는 `main` **하나만** 사용 — PC 에서 받기: `git pull origin main`
- 서버는 HIWARE 통제: 평소 `sudo`, `su` 등 금지명령 입력 금지 (3회 시 세션 차단). root 는 1일 권한 신청으로만 사용

## (보류) 컨테이너 대안 경로 빠른 시작
JEUS 가 Jakarta 를 지원하지 않을 때만 쓰는 경로다.
```bash
cd /home/atmswas/atms
sed -i 's/\r$//' bin/*.sh && chmod +x bin/*.sh
bin/init-dirs.sh                                   # 설정/디렉토리 초기화
bin/precheck.sh                                    # 서버 점검
bin/load-image.sh images/temurin-21-jre-jammy.tar.gz        # 베이스 이미지 적재
bin/deploy.sh atms-web release/atms-web-0.0.1.jar  # 빌드+기동+헬스체크(+자동롤백)
```
베이스 이미지가 아직 없으면: `bin/direct.sh start atms-web release/atms-web-0.0.1.jar`

## 구성
| 경로 | 내용 |
|---|---|
| `bin/` | precheck, init-dirs, load-image, build, deploy, rollback, start/stop/status/logs, install/uninstall-quadlet, direct(Plan B) |
| `conf/atms.env.example` | 공통 설정 (베이스 이미지 등) |
| `conf/apps/<앱>.env.example` | 앱별 포트/자원/헬스체크 — 파일 추가 = 앱(컨테이너) 추가 |
| `conf/apps/<앱>.container.env.example` | 컨테이너 환경변수 (프로파일, JAVA_OPTS) |
| `conf/application-dev.yml.example` | 서버 전용 Spring 설정 템플릿 (DB 등) |
| `deploy/Dockerfile` | JRE 베이스 이미지용 |
| `deploy/Dockerfile.jdk-local` | OS 베이스 이미지 + 서버 `jdk/` 복사용 |
| `deploy/jeus/swap-deploy.sh` | WAR·dist(zip, tar.gz) 를 새 폴더에 풀어 통째로 교체, 백업·롤백 (WAS 와 WebtoB 서버 공용) |
| `deploy/jeus/deploy-war.sh` | ws01 WAR 배포 — Jenkins 가 SSH 로 호출 (배포 전용 키의 고정 명령: deploy/rollback/status), 교체는 swap-deploy.sh |
| `deploy/jeus/install-deploy-war.sh` | ws01 WAR 배포 준비·배포 키 등록 |
| `deploy/webtob/deploy-front.sh` | wb01 화면 배포 — Jenkins 가 SSH 로 호출 (배포 전용 키의 고정 명령) |
| `deploy/webtob/install-wb01.sh` | wb01 폴더·스크립트 설치, 배포 키 등록 |
| `deploy/jenkins/run-jenkins.sh`, `jenkins.env.example` | Jenkins 컨테이너 생성·재생성 (데이터·캐시 유지) |
| `deploy/jenkins/pipelines/frontend.Jenkinsfile` | Jenkins 화면 빌드·배포 잡 (`nh-atms-frontend`) |
| `deploy/jenkins/pipelines/backend.Jenkinsfile` | Jenkins WAR 빌드(·선택 배포) 잡 (`nh-atms-backend`) |
