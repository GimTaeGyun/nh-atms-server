# Gitea 토큰 → 첫 push → 팀원 clone → Jenkins 연동·웹훅 (단계별)

작성일: 2026-10-23 · 전제: [GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) 3-2 완료 (조직 `NH-ATMS`, 팀 `developers`·`readers`, 계정 5개, 빈 저장소 `nh-atms`), PC → 113:3000·8090 방화벽 개방

| 주소 | 어디서 |
|---|---|
| `http://16.88.122.113:3000` | PC 브라우저·PC git → Gitea |
| `http://16.88.122.113:8090` | PC 브라우저 → Jenkins |
| `http://atms-gitea:3000`, `http://atms-jenkins:8080` | 컨테이너끼리 (Jenkins 소스 받기, Gitea 웹훅). PC 에서는 쓰지 않는다 |

---

## 1. 계정별 토큰 (각자, PC 브라우저)

1. `http://16.88.122.113:3000` → 본인 ID·임시 비밀번호로 로그인 → 새 비밀번호 설정
2. 우상단 프로필 → **설정** → **애플리케이션** → "토큰 생성"

| 항목 | 개발자 4명 | `jenkins-bot` |
|---|---|---|
| 토큰 이름 | `pc-git` (용도를 알 수 있게) | `jenkins` |
| 저장소 및 조직 접근 | 공개+비공개 (모든 저장소) | 같음 |
| 권한 `repository` | **읽기 및 쓰기** | **읽기** |
| 그 외 권한(issue, user, admin …) | 선택 안 함 | 선택 안 함 |

3. "토큰 생성" → 화면 위에 나온 값을 **바로 복사**해 안전한 곳에 보관 (다시 볼 수 없음. 잃어버리면 지우고 새로 만든다)
4. 토큰은 비밀번호와 같다: 메신저·문서에 붙여 넣지 않는다. 퇴사·분실 시 같은 화면에서 "삭제"

## 2. 첫 push (이태성 PC, 업체 GitLab 에서 clone 한 저장소)

**2-1. git 기본 설정 (PC 마다 1회, PowerShell)**
```powershell
git --version
git config --global user.name "이태성"
git config --global user.email "<Gitea 계정 이메일>"
git config --global credential.helper manager
git config --global core.autocrlf true
```

**2-2. 저장소 상태 확인**
```powershell
cd D:\NH-ATMS
git status
git branch -a
git remote -v
git log --oneline -5
dir
```
- `dir` 에 `Backend`, `Frontend` 가 **바로 보이는 폴더**가 저장소 최상위여야 한다 (Jenkinsfile 이 `Frontend/…`, `Backend/…` 경로를 씀)
- `git status` 에 커밋 안 한 변경이 있으면 먼저 정리 (2-4 참고)
- `git remote -v` 의 `origin` 은 업체 GitLab — 지우지 않고 그대로 둔다

**2-3. 은행 Gitea 를 `bank` 로 추가하고 push**
```powershell
git remote add bank http://16.88.122.113:3000/NH-ATMS/nh-atms.git
git remote -v
git push -u bank main
```
- 로그인 창: 사용자명 = Gitea ID, 비밀번호 = **1번 토큰** (Windows 자격 증명 관리자에 저장돼 다음부터는 안 물음)
- 업체 브랜치가 `master` 면: `git push -u bank master:main` (은행 쪽은 `main`)
- 업체 태그도 남기려면: `git push bank --tags`
- 완료 후 Gitea 화면 `NH-ATMS/nh-atms` 에 `Backend`, `Frontend` 폴더와 업체 커밋 이력이 보이면 성공

**2-4. JEUS 수정분은 두 번째 커밋으로**
[NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 의 24개 파일(`jeus.base-url` 8766 포함)을 반영한 뒤:
```powershell
git status
git add Backend
git commit -m "feat(jeus): JEUS WAR 배포 지원 (컨텍스트 경로 분리, 게이트웨이, base-url 8766)"
git push
```
- `jeus-common.yml` 의 `CHANGE_ME` 실제 값(DB·JWT)은 이 은행 Gitea 에만 들어간다. GitHub 등 외부로는 절대 올리지 않는다
- 이 push 는 `Backend/` 변경이라 웹훅 등록 후라면 백엔드 잡이 자동으로 돈다

**2-5. main 보호** — [GITEA_SETUP_GUIDE.md](GITEA_SETUP_GUIDE.md) 3-2 ⑤ (강제 푸시 금지)

## 3. 팀원 clone (나머지 3명, 각자 PC)

```powershell
git config --global user.name "<이름>"
git config --global user.email "<Gitea 계정 이메일>"
git config --global credential.helper manager
git config --global core.autocrlf true
cd D:\
git clone http://16.88.122.113:3000/NH-ATMS/nh-atms.git NH-ATMS
cd NH-ATMS
git log --oneline -3
```
- 로그인 창: 본인 ID + 본인 토큰
- 이 경우 원격 이름은 `origin` = 은행 Gitea (업체 GitLab 은 없음)
- 기존에 업체 저장소를 받아 둔 팀원은 2-3 처럼 `bank` 를 추가하고 `git fetch bank` → `git switch -c bank-main bank/main` 로 은행 기준 작업. 혼동되면 새 폴더에 clone 을 권장
- 매일: `git pull` → 작업 → `git add` → `git commit` → `git push` (main 에 직접, 강제 푸시 금지)

## 4. Jenkins 연동 (PC 브라우저, Jenkins `admin`)

**4-1. Credentials 2개** — Jenkins 관리 → Credentials → System → Global credentials → Add Credentials

> 종류 선택 창에서 **"Gitea Personal Access Token" 을 고르지 않는다** (2026-10-23 실수 사례). 그 종류는 Gitea 플러그인의 서버 연결용이라 Jenkinsfile 의 `git`/`checkout` 이 쓰지 못한다. **"Username with password"** 를 고르고 Password 칸에 토큰을 넣는다.

| 항목 | `gitea-jenkins-bot` | `wb01-deploy-key` |
|---|---|---|
| Kind | Username with password | SSH Username with private key |
| Scope | Global | Global |
| Username | `jenkins-bot` | `atmswas` |
| Password / Private key | 1번의 jenkins-bot 토큰 | "Enter directly" → `cat ~/atms/infra/jenkins/keys/wb01-deploy` 내용 전체 (`-----BEGIN` ~ `END-----`) |
| ID | **`gitea-jenkins-bot`** (Jenkinsfile 과 글자까지 같게) | **`wb01-deploy-key`** |
| Description | Gitea 읽기 (jenkins-bot) | wb01 화면 배포 |

`wb01-deploy-key` 등록을 확인하면 서버의 개인키 파일은 지운다 ([FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) B-4).

**4-2. 잡 2개** — 대시보드 → 새로운 Item

| 항목 | 화면 잡 | 백엔드 잡 |
|---|---|---|
| 이름 | `nh-atms-frontend` | `nh-atms-backend` |
| 종류 | Pipeline | Pipeline |
| Pipeline → Definition | **Pipeline script** | **Pipeline script** |
| Script 칸 | `deploy/jenkins/pipelines/frontend.Jenkinsfile` 전체 붙여 넣기 | `deploy/jenkins/pipelines/backend.Jenkinsfile` 전체 |
| "Use Groovy Sandbox" | 체크 그대로 | 체크 그대로 |

- 그 외 칸(Build Triggers 등)은 비워 둔다 — 트리거·매개변수는 스크립트 안에 있다
- "Pipeline script" = 빌드 절차를 잡 설정에 직접 넣는 방식. 저장소 안의 `NH-ATMS/Jenkinsfile`(업체용)은 읽지 않는다
- 붙여 넣을 파일은 GitHub `main` 또는 서버 배포킷(`~/atms/deploy/jenkins/pipelines/`)에서 열어 복사

**4-3. 첫 실행 (매개변수·트리거 등록)**
- 처음에는 "Build Now" 만 보인다 → 한 번 실행 (매개변수 없이 기본값으로 돈다) → 이후 "Build with Parameters" 로 바뀜
  - 화면 잡: 첫 실행 전에 [FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4장(wb01 스크립트·키)이 끝나 있어야 배포까지 성공. 아직이면 `MODE=build-only` 로 다시 실행해 빌드만 확인
  - 백엔드 잡: `MODULE=all` → 콘솔 끝 `BUILD SUCCESSFUL`·`Finished: SUCCESS`, 빌드 화면 "Build Artifacts" 에 WAR 6개 + `.sha256`
- 스크립트 승인 요청(“Scripts not permitted…”)이 뜨면 Jenkins 관리 → In-process Script Approval → Approve

## 5. 웹훅 (push 하면 자동 빌드)

**5-1. Jenkins notifyCommit 토큰** — Jenkins 관리 → Security → 아래 "Git plugin notifyCommit access tokens" → Add new access token
- 이름 `gitea-webhook` → 생성 → 값 복사 (한 번만 보임) → 화면 아래 **Save**

**5-2. Gitea 웹훅 1개** — Gitea `NH-ATMS/nh-atms` → 설정 → 웹훅 → 웹훅 추가 → **Gitea**

| 항목 | 값 |
|---|---|
| 대상 URL | `http://atms-jenkins:8080/git/notifyCommit?url=http://atms-gitea:3000/NH-ATMS/nh-atms.git&token=<5-1 토큰>` |
| HTTP 방법 | **POST** (GET 은 push 내용 전체를 URL 에 붙여 보내 Jenkins 가 `414 URI Too Long` 으로 거부 — 2026-10-23 확인) |
| POST 콘텐츠 유형 | `application/json` |
| 비밀 | 비움 |
| 트리거 | 푸시 이벤트 |
| 브랜치 필터 | `main` |
| 활성 | 체크 |

- `url=` 값은 두 Jenkinsfile 의 `GITEA_REPO` 와 **글자까지 같아야** 한다 (Jenkins 가 이 값으로 잡을 찾음)
- 웹훅 대상이 사설 주소(10.89.x)라 Gitea 의 `ALLOWED_HOST_LIST=private` 이 필요 — `gitea.env` 에 이미 있다

**5-3. 시험**
- 웹훅 화면 → "테스트 전송" → 아래 "최근 전송" 펼치기 → 응답 **200**, 본문에 `Scheduled polling of nh-atms-frontend`, `Scheduled polling of nh-atms-backend`
- 실제 확인: `Frontend/` 의 파일 하나만 고쳐 push → 화면 잡만 새 빌드, `Backend/` 만 고쳐 push → 백엔드 잡만 새 빌드. 다른 잡은 "Polling Log" 에 변경 없음

## 6. 문제 해결

| 증상 | 대처 |
|---|---|
| push 때 `Authentication failed` | 비밀번호 칸에 Gitea 로그인 비밀번호가 아니라 **토큰**. 잘못 저장됐으면 Windows "자격 증명 관리자" → Windows 자격 증명 → `git:http://16.88.122.113:3000` 삭제 후 다시 |
| push 때 `403` / `User permission denied` | 그 계정이 `developers` 팀에 있는지, 토큰 권한이 `repository` 쓰기인지 |
| `fatal: unable to access ... Failed to connect` | PC → 113:3000 방화벽 (`Test-NetConnection 16.88.122.113 -Port 3000`) |
| `! [rejected] main -> main (fetch first)` | 다른 사람이 먼저 push → `git pull` 후 다시 push (강제 푸시 금지) |
| Jenkins 콘솔 `Authentication failed` / `repository not found` (소스 단계) | `gitea-jenkins-bot` 토큰·ID 오타, jenkins-bot 이 `readers` 팀에 있는지 |
| 웹훅 응답 `No git jobs using repository` | `url=` 과 Jenkinsfile `GITEA_REPO` 가 다르거나, 잡 첫 실행(4-3)을 안 함 |
| 웹훅 응답 `403` | notifyCommit 토큰 오타 / 5-1 Save 안 함 |
| 웹훅 "전송 실패: ... not allowed" | `gitea.env` 의 `ALLOWED_HOST_LIST=private` 확인 후 `install-gitea.sh` 재실행 |
| 웹훅 "connection refused / no such host" | Gitea·Jenkins 가 `atms-net` 에 같이 있는지 ([FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4-7) |
