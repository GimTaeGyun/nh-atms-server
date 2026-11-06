// =====================================================================
// 백엔드 WAR 빌드·배포 — Jenkins(ws01 컨테이너) : bootWar -Pjeus → WAR 6개 + sha256 보관 → (선택) ws01 배포 폴더 교체
//
//  잡 만들기: 새 Item → Pipeline, 이름 nh-atms-backend (폴더 없이 최상위)
//            → Pipeline 칸 "Pipeline script" 에 이 파일 내용을 붙여 넣는다
//            → 처음 한 번 "Build with Parameters" 로 실행해야 매개변수·트리거가 등록된다
//  자동 빌드: 프론트 잡과 같은 Gitea 웹훅 1개(notifyCommit, url=GITEA_REPO)를 함께 쓴다.
//            소스 받기에 PathRestriction(Backend/.*) 을 걸어, Backend/ 아래가 바뀐 push 에만 빌드된다
//  필요한 Credentials (Jenkins 관리 → Credentials → System → Global)
//    gitea-jenkins-bot : Username with password  (jenkins-bot / Gitea 토큰)        소스 받기
//    ws01-deploy-key   : SSH Username with private key (atmswas / 배포 전용 키)  ws01 WAR 배포 (배포 MODE 에만 필요)
//  캐시: offline-seed 의 Gradle 캐시가 /cache/gradle 에 마운트돼 있고 GRADLE_USER_HOME=/cache/gradle
//        (DEV_SERVER_SETUP.md 9-5). 의존성이 바뀌어 "Could not resolve" 가 나면 캐시 재반입
//  구성 (2026-10-26): JEUS 도메인 1개 + 매니지드 서버 6개 (모듈마다 JVM·포트 하나, 컨텍스트 '/')
//        atms-web 8080 · journal 8081 · batch 8082 · file 8083 · if-internal 8084 · if-external 8085 (16.88.122.113)
//  배포: Jenkins 컨테이너 → ws01 호스트 SSH (WS01_SSH). ws01 의 authorized_keys 에서 이 키는
//        command="/home/atmswas/atms/deploy/jeus/deploy-war.sh" 로 고정 → deploy/rollback/status/restart 만 가능
//        (deploy/jeus/install-deploy-war.sh 로 등록). 교체 뒤 RESTART=true 면 deploy-war.sh restart 가
//        교체한 모듈의 서버만 하나씩 jeusRstart 로 재기동 → 기동 확인 → 실패한 모듈은 자동 롤백·재기동
//        서버의 재기동 메뉴가 ws01 ~/deploy/deploy-war.env 에 없으면 그 모듈은 교체만 하고 UNSTABLE(노란색)
//        → WAS 담당에 재기동 요청 (deploy-war.sh 종료코드 4)
//  문서: docs/BACKEND_CICD_PLAN.md
//
//  MODE
//    build-only   (기본, 웹훅도 이것) : 빌드·보관만. 재기동은 사람이 시점을 정하므로 자동 배포는 하지 않는다
//    build-deploy                     : 빌드 → MODULE(all 이면 6개) WAR 를 ws01 배포 폴더(catms·cajou·…)에 교체 → (RESTART) 재기동·확인
//    redeploy                         : REDEPLOY_BUILD 번 빌드의 보관 WAR 를 다시 교체 → (RESTART) 재기동·확인
//    rollback                         : MODULE 하나를 ws01 의 직전 백업으로 (all 불가) → (RESTART) 재기동·확인
//    restart                          : 교체 없이 재기동·확인만 (MODULE=all 이면 6개 서버를 하나씩)
//    status                           : ws01 의 모듈별 기동 확인 응답 코드·최근 수신 WAR·백업 목록
// =====================================================================
pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    timeout(time: 60, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
  }

  // 일정 없는 pollSCM = 웹훅(notifyCommit)이 올 때만 새 커밋 확인. Backend/ 밖만 바뀐 push 는 빌드 안 함
  triggers { pollSCM('') }

  parameters {
    choice(name: 'MODE', choices: ['build-only', 'build-deploy', 'redeploy', 'rollback', 'restart', 'status'], description: '기본 build-only (웹훅 빌드는 배포하지 않음). 설명은 스크립트 맨 위')
    choice(name: 'MODULE', choices: ['all', 'atms-web', 'atms-journal', 'atms-batch', 'atms-file', 'atms-if-internal', 'atms-if-external'], description: '기본 all (6개). 하나만 빌드·배포할 때 선택 (rollback 은 하나만)')
    booleanParam(name: 'RESTART', defaultValue: true, description: 'build-deploy·redeploy·rollback 뒤 그 모듈의 서버만 재기동 + 기동 확인 (실패 시 자동 롤백)')
    string(name: 'REDEPLOY_BUILD', defaultValue: '', description: 'MODE=redeploy 일 때 다시 배포할 이 잡의 빌드 번호')
  }

  environment {
    GITEA_REPO  = 'http://atms-gitea:3000/NH-ATMS/nh-atms.git'  // 프론트 잡·웹훅 url= 과 글자까지 같아야 함
    SRC_BRANCH  = 'main'
    BACK_DIR    = 'Backend/nh-atms-api'
    WAR_VERSION = '0.0.1'
    WS01_SSH    = 'atmswas@host.containers.internal'  // 컨테이너에서 ws01 호스트 sshd. 안 되면 BACKEND_CICD_PLAN 6-3 대로 바꿈
    GRADLE_USER_HOME = '/cache/gradle'  // offline-seed 캐시 마운트 위치 (jenkins.env 에 없어도 동작하게 고정, 2026-10-23)
    TZ          = 'Asia/Seoul'
  }

  stages {
    // 모든 MODE 에서 소스를 받는다: 비교 기준(마지막 빌드의 커밋)이 끊기지 않게 (프론트 잡과 같은 이유)
    stage('소스') {
      steps {
        checkout([$class: 'GitSCM',
          branches: [[name: "*/${env.SRC_BRANCH}"]],
          userRemoteConfigs: [[url: env.GITEA_REPO, credentialsId: 'gitea-jenkins-bot']],
          extensions: [[$class: 'PathRestriction', includedRegions: 'Backend/.*', excludedRegions: '']]])
      }
    }

    stage('빌드') {
      when { expression { params.MODE == 'build-only' || params.MODE == 'build-deploy' } }
      steps {
        dir(env.BACK_DIR) {
          sh '''
            set -eu
            java -version
            echo "GRADLE_USER_HOME=${GRADLE_USER_HOME:-미설정}"
            # 캐시가 없으면 gradlew 가 인터넷에서 배포판을 받으려다 UnknownHostException 으로 실패 → 원인을 먼저 알려 줌
            ls -d "${GRADLE_USER_HOME}"/wrapper/dists/gradle-8.14.4-bin >/dev/null 2>&1 \
              || { echo "Gradle 캐시 없음: ${GRADLE_USER_HOME}/wrapper/dists/gradle-8.14.4-bin → DEV_SERVER_SETUP 9-5 (컨테이너에 -v .../cache/gradle:/cache/gradle 마운트 확인)"; exit 1; }
            if [ "${MODULE}" = "all" ]; then TASK="bootWar"; else TASK=":${MODULE}:bootWar"; fi
            # gradle.properties 의 -Xmx2g·parallel 은 컨테이너(2g)에 과함 → 명령줄 -D 로 덮어씀
            sh ./gradlew ${TASK} -Pjeus --offline --no-daemon --max-workers=1 \
              -Dorg.gradle.parallel=false \
              -Dorg.gradle.jvmargs="-Xmx768m -Dfile.encoding=UTF-8" \
              -Porg.gradle.java.installations.auto-download=false \
              -Porg.gradle.java.installations.paths="${JAVA_HOME:-/opt/java/openjdk}"
          '''
        }
      }
    }

    stage('묶기') {
      when { expression { params.MODE == 'build-only' || params.MODE == 'build-deploy' } }
      steps {
        sh '''
          set -eu
          STAMP="$(date +%Y%m%d)-${BUILD_NUMBER}"
          rm -rf out && mkdir out
          for w in "${BACK_DIR}"/atms-*/build/libs/*.war; do
            [ -f "$w" ] || continue
            case "$w" in *-plain.war) continue ;; esac
            m="$(basename "$(dirname "$(dirname "$(dirname "$w")")")")"
            n="${m}-${WAR_VERSION}-${STAMP}.war"
            cp "$w" "out/${n}"
            (cd out && sha256sum "${n}" > "${n}.sha256")
          done
          ls out/*.war >/dev/null
          echo "${STAMP}" > out/VERSION
          git rev-parse HEAD > out/COMMIT
          ls -l out
        '''
        archiveArtifacts artifacts: 'out/*', fingerprint: true
        script { currentBuild.description = readFile('out/VERSION').trim() + ' ' + params.MODULE }
      }
    }

    stage('배포') {
      when { expression { params.MODE != 'build-only' } }
      steps {
        script {
          if (params.MODE == 'rollback' && params.MODULE == 'all') { error 'rollback 은 MODULE 을 하나 고르세요' }
          if (params.MODE == 'redeploy') {
            if (!(params.REDEPLOY_BUILD ==~ /\d+/)) { error 'REDEPLOY_BUILD 에 빌드 번호(숫자)를 넣으세요' }
            sh '''
              set -eu
              SRC="${JENKINS_HOME}/jobs/${JOB_NAME}/builds/${REDEPLOY_BUILD}/archive/out"
              test -d "${SRC}" || { echo "빌드 #${REDEPLOY_BUILD} 의 보관 파일 없음 (최근 10개 빌드만 보관)"; exit 1; }
              rm -rf out && cp -r "${SRC}" out
            '''
            currentBuild.description = 'redeploy ' + readFile('out/VERSION').trim() + ' ' + params.MODULE
          }
        }
        script {
          // 종료코드 4 = 재기동 메뉴가 없는 모듈이 있음(교체는 됨) → UNSTABLE. 그 밖의 0 이 아닌 값은 실패
          int rc = 0
          sshagent(credentials: ['ws01-deploy-key']) {
            rc = sh(returnStatus: true, script: '''
            set -eu
            SSH="ssh -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10 -o ServerAliveInterval=30"
            case "${MODE}" in
              status)
                if [ "${MODULE}" = "all" ]; then ${SSH} "${WS01_SSH}" status; else ${SSH} "${WS01_SSH}" "status ${MODULE}"; fi ;;
              restart)
                if [ "${MODULE}" = "all" ]; then ${SSH} "${WS01_SSH}" restart; else ${SSH} "${WS01_SSH}" "restart ${MODULE}"; fi ;;
              rollback)
                ${SSH} "${WS01_SSH}" "rollback ${MODULE}"
                if [ "${RESTART}" = "true" ]; then ${SSH} "${WS01_SSH}" "restart ${MODULE}"; fi ;;
              *)
                COMMIT="$(cat out/COMMIT 2>/dev/null || echo -)"
                n=0; MODS=""
                for w in out/*.war; do
                  [ -f "$w" ] || continue
                  f="$(basename "$w")"
                  case "$f" in "${MODULE}"-[0-9]*) ;; *) [ "${MODULE}" = "all" ] || continue ;; esac
                  SHA="$(cut -d' ' -f1 "$w.sha256")"
                  echo "=== ${f}"
                  ${SSH} "${WS01_SSH}" "deploy ${f} ${SHA} ${COMMIT}" < "$w"
                  n=$((n + 1)); MODS="${MODS} ${f%%-[0-9]*}"
                done
                [ "$n" -gt 0 ] || { echo "배포할 WAR 없음 (MODULE=${MODULE})"; exit 1; }
                if [ "${RESTART}" = "true" ]; then
                  ${SSH} "${WS01_SSH}" "restart --rollback${MODS}"
                else
                  echo "WAR ${n}개 교체 완료 (재기동 안 함). 반영: MODE=restart"
                fi
                ;;
            esac
            ''')
          }
          if (rc == 4) {
            unstable('재기동 메뉴가 없는 모듈이 있음 — 파일은 교체됨. WAS 담당에 그 서버 재기동 요청 (콘솔의 "WAS 담당 재기동 필요" 줄)')
          } else if (rc != 0) {
            error("ws01 deploy-war.sh 실패 (종료코드 ${rc}) — 콘솔 확인")
          }
        }
      }
    }
  }
}
