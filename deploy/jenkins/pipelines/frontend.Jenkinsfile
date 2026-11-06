// =====================================================================
// 화면(프론트엔드) 빌드·배포 — Jenkins(ws01 컨테이너) → WebtoB DocBase(wb01)
//
//  잡 만들기: 새 Item → Pipeline, 이름 nh-atms-frontend (폴더 없이 최상위)
//            → Pipeline 칸 "Pipeline script" 에 이 파일 내용을 붙여 넣는다
//            → 처음 한 번 "Build with Parameters" 로 실행해야 매개변수·트리거가 등록된다
//  자동 빌드: Gitea 웹훅 → http://atms-jenkins:8080/git/notifyCommit?url=<GITEA_REPO>&token=<notifyCommit 토큰>
//            (Gitea·Jenkins 는 podman 네트워크 atms-net 에서 컨테이너 이름으로 서로 접속)
//            → 이 잡이 Gitea 에 새 커밋이 있는지 바로 확인하고, 있으면 build-deploy 로 빌드 (정해진 주기 폴링은 없음)
//  필요한 Credentials (Jenkins 관리 → Credentials → System → Global)
//    gitea-jenkins-bot : Username with password  (jenkins-bot / Gitea 토큰)        소스 받기
//    wb01-deploy-key   : SSH Username with private key (atmswas / 배포 전용 키)  wb01 배포
//  wb01 쪽: ~/deploy/bin/deploy-front.sh — authorized_keys 의 command= 로 이 스크립트만 실행됨
//  문서: docs/FRONTEND_CICD_PLAN.md
//
//  MODE
//    build-deploy (기본, 웹훅도 이것) : 빌드 → tar.gz → wb01 배포
//    build-only                       : 빌드·보관만 (배포 안 함)
//    redeploy                         : REDEPLOY_BUILD 번 빌드의 보관 파일을 다시 배포 (최근 10개 빌드만 보관)
//    rollback                         : wb01 의 직전 백업으로 되돌림
//    status                           : wb01 의 현재 버전·기록·백업 목록
// =====================================================================
pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    timeout(time: 30, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
  }

  // 일정 없는 pollSCM = 주기 폴링은 하지 않고, Gitea 웹훅(notifyCommit)이 올 때만 새 커밋 확인 (2026-10-22 결정: 웹훅만, 보조 폴링 없음)
  // 이 줄을 지우면 웹훅이 와도 빌드되지 않는다. 자동 빌드를 잠시 멈추려면 잡의 "Disable Project"
  triggers { pollSCM('') }

  parameters {
    choice(name: 'MODE', choices: ['build-deploy', 'build-only', 'redeploy', 'rollback', 'status'], description: '기본 build-deploy. 설명은 스크립트 맨 위')
    string(name: 'REDEPLOY_BUILD', defaultValue: '', description: 'MODE=redeploy 일 때 다시 배포할 이 잡의 빌드 번호')
  }

  environment {
    GITEA_REPO = 'http://atms-gitea:3000/NH-ATMS/nh-atms.git'  // 같은 podman 네트워크(atms-net)의 Gitea 컨테이너 이름. 웹훅 url= 과 글자까지 같아야 함
    SRC_BRANCH = 'main'
    FRONT_DIR  = 'Frontend/nh-atms-web'
    WB01       = 'atmswas@16.88.122.112'
    NPM_CACHE  = '/cache/npm'                                  // offline-seed npm 캐시 마운트 위치
    TZ         = 'Asia/Seoul'
  }

  stages {
    // 모든 MODE 에서 소스를 받는다: 비교 기준(마지막 빌드의 커밋)이 끊기면 rollback 직후 웹훅 확인이 다시 배포하기 때문
    stage('소스') {
      steps {
        // PathRestriction: 웹훅 1개를 백엔드 잡과 함께 쓰므로, Frontend/ 아래가 바뀐 push 에만 빌드 (2026-10-22 결정)
        checkout([$class: 'GitSCM',
          branches: [[name: "*/${env.SRC_BRANCH}"]],
          userRemoteConfigs: [[url: env.GITEA_REPO, credentialsId: 'gitea-jenkins-bot']],
          extensions: [[$class: 'PathRestriction', includedRegions: 'Frontend/.*', excludedRegions: '']]])
      }
    }

    stage('빌드') {
      when { expression { params.MODE == 'build-deploy' || params.MODE == 'build-only' } }
      steps {
        dir(env.FRONT_DIR) {
          sh 'node -v && npm -v'
          sh 'npm ci --offline --cache "$NPM_CACHE" --no-audit --no-fund'
          sh 'NODE_OPTIONS=--max-old-space-size=1024 npm run build'
        }
      }
    }

    stage('묶기') {
      when { expression { params.MODE == 'build-deploy' || params.MODE == 'build-only' } }
      steps {
        sh '''
          set -eu
          VERSION="$(date +%Y%m%d)-${BUILD_NUMBER}"
          PKG="nh-atms-web-dist-${VERSION}.tar.gz"
          test -f "${FRONT_DIR}/dist/index.html"
          rm -rf out && mkdir out
          tar -C "${FRONT_DIR}/dist" -czf "out/${PKG}" .
          (cd out && sha256sum "${PKG}" > "${PKG}.sha256")
          echo "${VERSION}" > out/VERSION
          git rev-parse HEAD > out/COMMIT
          ls -l out
        '''
        archiveArtifacts artifacts: 'out/*', fingerprint: true
        script { currentBuild.description = readFile('out/VERSION').trim() }
      }
    }

    stage('배포') {
      when { expression { params.MODE != 'build-only' } }
      steps {
        script {
          if (params.MODE == 'redeploy') {
            if (!(params.REDEPLOY_BUILD ==~ /\d+/)) { error 'REDEPLOY_BUILD 에 빌드 번호(숫자)를 넣으세요' }
            sh '''
              set -eu
              SRC="${JENKINS_HOME}/jobs/${JOB_NAME}/builds/${REDEPLOY_BUILD}/archive/out"
              test -d "${SRC}" || { echo "빌드 #${REDEPLOY_BUILD} 의 보관 파일 없음 (최근 10개 빌드만 보관)"; exit 1; }
              rm -rf out && cp -r "${SRC}" out
            '''
            currentBuild.description = 'redeploy ' + readFile('out/VERSION').trim()
          }
        }
        sshagent(credentials: ['wb01-deploy-key']) {
          sh '''
            set -eu
            SSH="ssh -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10"
            case "${MODE}" in
              rollback) ${SSH} "${WB01}" rollback ;;
              status)   ${SSH} "${WB01}" status ;;
              *)
                VERSION="$(cat out/VERSION)"
                PKG="out/nh-atms-web-dist-${VERSION}.tar.gz"
                SHA="$(cut -d' ' -f1 "${PKG}.sha256")"
                ${SSH} "${WB01}" "deploy ${VERSION} ${SHA} $(cat out/COMMIT)" < "${PKG}"
                ;;
            esac
          '''
        }
      }
    }
  }
}
