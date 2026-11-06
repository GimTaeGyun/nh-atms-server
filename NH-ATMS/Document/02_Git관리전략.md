</br>  

# 목차
```  
1. 자주 사용하는 Git 용어  
2. Git 전략  
```  

# 소스 형상 및 브랜치 관리
> 분석일: 2026-05-18  
> 대상: NH-ATMS

</br>  

# 자주 사용하는 Git 용어

## <font color="#548dd4">Repository</font> (레포지토리)
Git 프로젝트 저장소.  
프로젝트 소스와, README.md, .gitignore 추가로 문서(Document)를 포함한다.

- Local Repository
    - 내 PC 저장소
- Remote Repository
    - GitHub/GitLab 서버 저장소

</br>  

## <font color="#548dd4">Commit</font> (커밋)
변경사항 저장 단위.

![Pasted image 20260518091434.png](file/Pasted%20image%2020260518091434.png)

위와 같이 로컬과 main(origin)과의 변경사항을 **커밋 메세지**와 함께 저장한다.

>[!info] 커밋 메세지와 git 커밋 컨벤션
>- `feat` : 새로운 기능 추가
>- `fix` : 버그 수정
>- `docs` : 문서 수정
>- `style` : 코드 포맷팅, 세미콜론 누락, 코드 변경이 없는 경우
>- `refactor` : 코드 리펙토링
>- `test` : 테스트 코드, 리펙토링 테스트 코드 추가
>- `chore` : 빌드 업무 수정, 패키지 매니저 수정
>-  ---  
> 위와 같이, 어떠한 변경사항이 있는지에 따라 한 눈에 알아볼 수 있도록 커밋 메세지에 규칙을 적용하는 방법.

</br>  

## <font color="#548dd4">Branch</font> (브랜치)

독립적인 작업 공간.

![Pasted image 20260518091926.png](file/Pasted%20image%2020260518091926.png)

### 목적
- 기능별 개발 분리
- 충돌 최소화
- **어떠한 Git 전략을 적용하냐에 따라, 각 브랜치의 역할과 수명이 달라진다**

</br>  

## <font color="#548dd4">Merge</font> / <font color="#548dd4">Rebase</font> (머지 & 리베이스)

Rebase - 커밋 기반을 재정렬하는 작업  
Merge - 브랜치를 통합하는 작업

아래의 이미지의 A는 main branch, B는 A2베이스의 추가 기능을 개발한 브랜치다.

![Pasted image 20260518092627.png](file/Pasted%20image%2020260518092627.png)

위 상황에서 **Merge**를 사용하면

![Pasted image 20260518092743.png](file/Pasted%20image%2020260518092743.png)

아래와 같이 A4와 B2의 커밋기록을 모두 고려한 C3가 생성된다.  
이때, 커밋기록(Commit ID)는 변경되지 않으며, 시간순서로 쌓이게 된다.

다시 첫번째 상황으로 돌아가 **Rabase**를 진행한다면,

![Pasted image 20260518092954.png](file/Pasted%20image%2020260518092954.png)

A4브랜치를 기준으로 로컬 커밋 기록이 새로 쓰이게 된다.

각각의 장단점으로는, 다음과 같다.

| 항목             | Merge       | Rebase      |  
| -------------- | ----------- | ----------- |  
| 개념             | 브랜치를 그대로 합침 | 커밋 기반을 재배치  |  
| 히스토리           | 분기 구조 유지    | 직선형 히스토리    |  
| 커밋 기록          | 실제 작업 흐름 보존 | 깔끔하게 정리     |  
| 협업 안정성         | 높음          | 잘못 사용 시 위험  |  
| 충돌 처리          | 한 번에 처리     | 커밋마다 발생 가능  |  
| 사용 난이도         | 쉬움          | 상대적으로 어려움   |  
| 히스토리 추적        | 쉬움          | 변경 이력 왜곡 가능 |  
| force push 필요성 | 거의 없음       | 자주 필요       |  
| 권장 상황          | 협업 브랜치      | 개인 작업 정리    |  

</br>  

## <font color="#548dd4">Fetch</font> (패치)

원격 변경사항을 로컬로 가져오기만 수행.  
안전한 작업이다.

## <font color="#548dd4">Pull</font> (풀)

원격 변경사항 가져와서 **merge**까지 수행

## <font color="#548dd4">Push</font> (푸시)

내 커밋(로컬)을 원격 저장소(원격 브랜치)로 업로드

## <font color="#548dd4">Staging</font> (스테이징 또는 스테이징 에리어)

커밋 전 임시 저장 공간

 </br>  

## <font color="#548dd4">revert</font> (리버트)
**현재에 있으면서** 과거의 특정 사건(commit)들만 없던 일로 만든다.  
즉, 과거로 소스를 되돌리는 기록이 남는다.

</br>  

## <font color="#548dd4">Reset</font> (리셋)
현재 브랜치의 시간을 **아예** 과거의 특정 사건(commit)으로 되돌린다.  
즉, 되돌아가는 시점까지의 기록(commit)을 모두 삭제한다.

이 Reset에는 3가지의 모드가 있다.

| 옵션             | HEAD | Staging Area | Working Directory(로컬) |  
| -------------- | ---- | ------------ | --------------------- |  
| `--soft`       | 이동   | 유지           | 유지                    |  
| `--mixed` (기본) | 이동   | 초기화          | 유지                    |  
| `--hard`       | 이동   | 초기화          | 초기화                   |  
>[!danger] 주의사항  
> soft reset의 경우에는 비교적 안전하지만, hard reset은 변경사항은 물론, 변경기록까지 모두 삭제함으로  
> 실행시 복구가 불가능하므로, 매우 주의하여야 한다.   
>**로컬 브랜치의 신규 커밋건(로컬에만 존제하는 소스)에만 사용하며, 협업시 원격저장소의 커밋기록을 건드는 것은 매우매우 위험하다**

</br>  
  
---  

</br>  

# Git 브랜치 전략
브랜치 전략이란 **여러 개발자가 하나의 저장소(레포지토리)를 사용하는 환경에서 저장소를 효과적으로 활용하기 위한 방법** 이다.

그 중에서 가장 대표적인 방식은 **git flow, github flow** 라는 2가지 관리 전략이 있다.

</br>  

## 1. <font color="#548dd4">Git Flow</font>

Git 기반 브랜치 전략 중 가장 전통적이고 체계적인 방식이다.  
대규모 프로젝트나 정기 릴리즈(버전 배포)가 중요한 환경에서 많이 사용된다.

### 특징
- 브랜치 종류가 많다
- 역할이 명확히 분리된다
- 릴리즈 관리에 강하다  
  ![Pasted image 20260518101102.png](file/Pasted%20image%2020260518101102.png)  
  출처: 강남언니 기술블로그 (https://blog.gangnamunni.com/post/understanding_git_flow/)

- 기본적인 가지의 이름은 보통 아래의 5가지로 구분한다.
- **feature > develop > release > hotfix > master**

항상유지(삭제되지 않음) : master / develop  
Merge후 사라지는 브렌치 : feature / release / hotfix

각 브랜치 별 설명은 다음과 같다.

#### master
- 라이브 서버에 제품으로 **출시**되는 브랜치.
- **배포 가능한 상태**만을 관리한다.
- 삭제하지 않음.
#### develop 
- 다음 출시 버전을 대비하여 **개발**하는 브랜치.
- feature브랜치가 기반으로 하는 브랜치.
- 삭제하지 않음.
#### feature
- **추가 기능** 개발 브랜치.
- **이름 설정**은 도메인(기능)별로 자유롭게 설정 가능 (ex. feat/login)
- **feature ← develop** (pull)
- **feature → develop** (push)
#### release
- **다음 버전** 출시를 준비하는 브랜치.   
- develop 브랜치를 release 브랜치로 옮긴 후 QA, 테스트를 진행하고 master 브랜치로 합친다.
- **develop ← release ← master** (pull)
- **develop ← release → master** (push)
- release 브랜치에서 기능을 점검하며 발견한 버그 수정 사항은 develop 브랜치에도 적용해줘야 한다. 그러므로 배포 완료 후 develop 브랜치에 대해서도 merge 작업을 수행해야 한다.

#### hotfix
- master 브랜치에서 발생한 **버그를 수정**하는 브랜치.
- Hotfix는 보통 **다급하게 버그를 고치기 위해 생성**되는 가지이기 때문에 버그를 해결하면 보통 제거하는 일회성 가지다.
- **hotfix ← master** (pull)
- **develop ← hotfix → master** (push)

</br>  

## 2. <font color="#548dd4">GitHub Flow</font>

수시로 릴리즈 되어야 할 필요가 있는 서비스를 지속적으로 테스트하고 배포하는 팀이라면  
**github-flow** 와 같은 간단한 work-flow가 적합하다

![Pasted image 20260518115659.png](file/Pasted%20image%2020260518115659.png)
>[!info]  *PR(Pull Request) 이란?*  
> 팀원이 작업한 코드를 메인 브랜치에 합치기 전(Merge), 다른 팀원들에게 _"내 코드를 검토하고 메인 브랜치에 반영(Pull)해 줘"_ 라고 요청하는 협업 및 코드 리뷰 프로세스
> - 팀장, 팀원 등 Role(역할)을 정하여 *팀장의 승인 이후에야 팀원의 코드가 메인브랜치에 반양 가능토록* 할 수 있다.
### 특징
- 브랜치 종류가 많다
- 역할이 명확히 분리된다
- 릴리즈 관리에 강하다

#### master
- 라이브 서버에 제품으로 **출시**되는 브랜치.
- **배포 가능한 상태**만을 관리한다.
- 삭제하지 않음.
- Git-flow와 역할은 동일

#### feature(기능 브랜치)
- **feature ←→ master** (pull / push)
- 새 브렌치는 항상 master브랜치에서 온다.
- git flow에서 각 **브렌치별 기능이 정의되어있는것과 다르게** 브렌치 생성시 기능과 의도를 명확히하여 만든다.  
  ex) feat/login, feat/hotfix-dateModule

>[!tip]  주요 포인트
>- **커밋메시지를 명확하게 작성하라**
>- **수시로 push** 하라
>- 원격에 자신이 하고 있는 일들을 올려 다른 사람들도 확인할 수 있도록 해준다
  
---  

# 정리

 *기존 SVN기반의 폴더 구조로 잘 운영하고 있는데 왜 굳이 Git Flow와 같이 복잡한 것을 도입하는가?*

1. 여러 기능을 동시에 개발할 때
   - 예를들어 login기능을 개발할때 svn은 하나의 폴더에 계속해서 합침 → 충돌확률 증가
   - 이때, git-flow를 사용한다면, `feature/login`, `feature/payment`와 같이 독립개발이 가능하다.

2. 긴급 패치(hotfix) 해야 할 때
3. 운영 버전과 개발 버전을 동시에 유지할 때
4. merge 충돌이 많아질 때
5. **누가 무엇을 언제 반영했는지 추적**할 때
6. “브랜치 비용” 차이
    - svn에서의 브랜치는 사실상 디렉토리 복사이다.
    - 이는 merge가 힘들 뿐 더러, 생성 자체에 부담을 느끼게 된다.

다음과 같은 상황에서 *GitHub Flow*와 *Git Flow*는 해결책을 제시해 줄 수 있다.

위 두 전략 외에도 **gitLab-Flow**와 같이 두 성격 중간에 위치한 전략 또한 존재하며,   
프로젝트 성격과 팀 상황을 고려하여 적절한 전략을 구성하는 것이 중요하다.