# NH-ATMS 투입 전 사전 준비 WBS & TODO (3주 운영안)

> **작성일**: 2026-05-18
> **목적**: 본 투입(2026-06-08 예정) 전 3주간 팀 내부에서 정의·합의해야 할 항목을 체계적으로 관리
> **운영 방식**: 회의 기반 점진 채움 — 주간 회의에서 진척 점검, 미결 항목은 다음 주로 이관
> **출력 형태**: 본 문서(설명) + [12_사전준비_WBS.csv](12_사전준비_WBS.csv) (엑셀 import)

---

## 1) 전체 WBS 구조 설명

### 1.1 계층 구조 (3-Level)

```
[L1: 대분류]              [L2: 중분류]            [L3: 세부 항목]
─────────────────────────────────────────────────────────────────
A. 요구사항 정의          A1. 기능 요구사항       A1-01. 화면 단위 기능 목록 확정
                          A2. 비기능 요구사항     A1-02. 우선순위 분류 (Must/Should/Could)
                          ...                     ...

B. 아키텍처/설계          B1. 시스템 아키텍처     B1-01. 전체 구성도 확정
                          B2. 모듈 설계           ...
                          ...

C. 개발 표준              C1. 코딩 컨벤션         ...
                          C2. 네이밍 규칙
                          C3. 패키지 구조

D. Git 전략               D1. 브랜치 전략
                          D2. 커밋 규칙
                          D3. 코드리뷰

E. 개발 프로세스          E1. 이슈 관리
                          E2. Sprint 운영
                          E3. DoD/DoR

F. 협업 도구/환경         F1. 이슈 트래커
                          F2. 문서 도구
                          F3. 소스 저장소
                          F4. 커뮤니케이션

G. 테스트 전략            G1. 단위 테스트
                          G2. 통합 테스트
                          G3. 성능 테스트
                          G4. 보안 테스트

H. 배포/형상관리          H1. 환경 분리
                          H2. 배포 파이프라인
                          H3. 형상관리

I. 공통 컴포넌트          I1. Backend 공통
                          I2. Frontend 공통

J. 리스크/이슈            J1. 리스크 등록부
                          J2. 의존성/외부요인
```

### 1.2 엑셀 컬럼 구조

| 컬럼명 | 타입 | 설명 | 예시 |
|---|---|---|---|
| **ID** | Text | WBS 고유 식별자 (A1-01 형식) | `A1-01` |
| **대분류** | Text | L1 카테고리 | `A. 요구사항 정의` |
| **중분류** | Text | L2 카테고리 | `A1. 기능 요구사항` |
| **세부 항목** | Text | L3 작업명 | 화면 단위 기능 목록 확정 |
| **설명** | Text | 상세 설명 (신규 인원도 이해 가능) | AS-IS 화면 기반으로 TO-BE 화면 list-up |
| **담당자** | Text | PL/BE/FE/QA/PM | `PL` |
| **우선순위** | Text | High / Medium / Low | High |
| **상태** | Text | To-Do / In Progress / Done / Hold | To-Do |
| **회의 필요** | Y/N | 회의 안건 여부 | Y |
| **목표 주차** | Text | W1 / W2 / W3 | W1 |
| **산출물** | Text | 결과물 종류 | 기능목록표.xlsx |
| **비고** | Text | 의존성, 위험요소 등 | RFP 기반 검증 필요 |

### 1.3 우선순위 기준

| 우선순위 | 기준 | 처리 |
|---|---|---|
| **High** | 본 투입 전 반드시 합의 필요 (개발 시작 시 차단요소) | W1~W2 마감 |
| **Medium** | 합의 시 효율 향상, 미합의 시 초기엔 우회 가능 | W2~W3 마감 |
| **Low** | 운영 안정화 단계까지 검토 가능 | W3 또는 보류 |

---

## 2) WBS 테이블 (엑셀 형태 — 샘플 데이터 포함)

> 아래 표는 그대로 엑셀로 복사 가능합니다. CSV 파일도 별도 제공 → [12_사전준비_WBS.csv](12_사전준비_WBS.csv)

### A. 요구사항 정의

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| A1-01 | A. 요구사항 정의 | A1. 기능 요구사항 | 화면 단위 기능 목록 확정 | AS-IS 화면(농협/새마을금고) 기반으로 TO-BE 화면 list-up | PL | High | To-Do | Y | W1 | 기능목록표.xlsx | RFP 검증 필수 |
| A1-02 | A. 요구사항 정의 | A1. 기능 요구사항 | MoSCoW 우선순위 분류 | Must/Should/Could/Won't 4단계 분류 | PL,PM | High | To-Do | Y | W1 | 기능목록표(우선순위) | — |
| A1-03 | A. 요구사항 정의 | A1. 기능 요구사항 | 화면 ID 체계 정의 | SCR-AAA-NNN 형식 (도메인+일련번호) | PL | Medium | To-Do | N | W1 | 화면ID규칙.md | — |
| A1-04 | A. 요구사항 정의 | A1. 기능 요구사항 | 화면 wireframe 1차 작성 | 메인 5개 화면 (대시보드/ATM목록/상세/로그인/사용자) | FE,PL | High | To-Do | Y | W2 | Figma URL | — |
| A1-05 | A. 요구사항 정의 | A1. 기능 요구사항 | 업무 시나리오 정의 | 주요 사용자 여정 (Login→Dashboard→ATM관리) | PL | Medium | To-Do | Y | W2 | 시나리오.md | — |
| A2-01 | A. 요구사항 정의 | A2. 비기능 요구사항 | 성능 목표 정의 | 동시접속/응답시간/처리량 수치화 (p95<500ms 등) | PL,BE | High | To-Do | Y | W1 | NFR명세서 | — |
| A2-02 | A. 요구사항 정의 | A2. 비기능 요구사항 | 가용성 목표 | SLA 99.9% (월 43분 다운 허용) | PL | High | To-Do | Y | W1 | NFR명세서 | — |
| A2-03 | A. 요구사항 정의 | A2. 비기능 요구사항 | 보안 정책 | 비밀번호/세션/감사로그/암호화 | PL,BE | High | To-Do | Y | W2 | 보안정책서 | 금감원 가이드 |
| A2-04 | A. 요구사항 정의 | A2. 비기능 요구사항 | 데이터 보관 정책 | 거래저널 7년/로그 1년 등 | PL | Medium | To-Do | Y | W2 | 데이터정책서 | — |
| A2-05 | A. 요구사항 정의 | A2. 비기능 요구사항 | 다국어/접근성 | 다국어 대응 범위 (한국어 only/영문) | PL | Low | To-Do | N | W3 | — | — |

### B. 아키텍처/설계

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| B1-01 | B. 아키텍처/설계 | B1. 시스템 아키텍처 | 전체 시스템 구성도 | WAS/WEB/DB/IF 구성 다이어그램 | TL | High | To-Do | Y | W1 | 아키텍처.drawio | — |
| B1-02 | B. 아키텍처/설계 | B1. 시스템 아키텍처 | 외부 시스템 IF 목록 | SSO/EAI/대외기관/Agent | TL,PL | High | To-Do | Y | W1 | IF목록표 | — |
| B1-03 | B. 아키텍처/설계 | B1. 시스템 아키텍처 | JEUS/WebtoB 배포 토폴로지 | DEV/STG/PROD 서버 구성 | TL,Infra | High | To-Do | Y | W2 | 배포구성도 | — |
| B2-01 | B. 아키텍처/설계 | B2. 모듈 설계 | 멀티모듈 분리 기준 확정 | atms-web/journal/batch/file/ifin/ifex | TL | High | Done | N | W1 | — | 스켈레톤 완료 |
| B2-02 | B. 아키텍처/설계 | B2. 모듈 설계 | 모듈간 의존성 규칙 | common만 공통 의존, 도메인끼리 금지 | TL | High | Done | N | W1 | 개발표준§7 | — |
| B3-01 | B. 아키텍처/설계 | B3. DB 설계 | 명명규칙 (tb_/th_/tc_) | 테이블/컬럼/인덱스/시퀀스 prefix | DBA,TL | High | To-Do | Y | W1 | DB명명규칙.md | — |
| B3-02 | B. 아키텍처/설계 | B3. DB 설계 | 공통 audit 컬럼 표준 | reg_dt/reg_user/upd_dt/upd_user/del_yn | DBA,TL | High | To-Do | Y | W1 | DB표준.md | — |
| B3-03 | B. 아키텍처/설계 | B3. DB 설계 | AS-IS DB 매핑표 작성 | MySQL→Oracle 테이블/컬럼 매핑 | DBA | High | To-Do | Y | W2 | 매핑표.xlsx | 이미지 11장 분석 |
| B3-04 | B. 아키텍처/설계 | B3. DB 설계 | 거래저널 파티셔닝 전략 | RANGE(reg_dt) 월별 | DBA | High | To-Do | Y | W2 | 설계서 | — |
| B3-05 | B. 아키텍처/설계 | B3. DB 설계 | 인덱스 가이드 | 복합 PK/FK/검색 컬럼 | DBA | Medium | To-Do | N | W3 | 인덱스가이드 | — |

### C. 개발 표준

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| C1-01 | C. 개발 표준 | C1. 코딩 컨벤션 | Java 스타일 가이드 | Google/Naver 중 선택 + 인텔리J 포맷터 배포 | TL,BE | High | To-Do | Y | W1 | settings.jar | — |
| C1-02 | C. 개발 표준 | C1. 코딩 컨벤션 | TypeScript/React 스타일 | ESLint + Prettier 설정 통일 | TL,FE | High | To-Do | Y | W1 | .eslintrc | — |
| C1-03 | C. 개발 표준 | C1. 코딩 컨벤션 | SQL 작성 규칙 | 키워드 대문자, 들여쓰기, alias | DBA | Medium | To-Do | N | W2 | SQL규칙.md | — |
| C1-04 | C. 개발 표준 | C1. 코딩 컨벤션 | 주석 작성 규칙 | JavaDoc/JSDoc 필수 범위 | TL | Low | To-Do | N | W3 | — | — |
| C2-01 | C. 개발 표준 | C2. 네이밍 규칙 | 패키지/클래스/메소드 네이밍 | 개발표준가이드 §1.2 참조 | TL | High | Done | N | W1 | 표준§1.2 | — |
| C2-02 | C. 개발 표준 | C2. 네이밍 규칙 | URL/API 엔드포인트 | kebab-case, 단수형 | TL,BE | High | Done | N | W1 | 표준§1.2.7 | — |
| C2-03 | C. 개발 표준 | C2. 네이밍 규칙 | DB 객체 네이밍 | snake_case, prefix 규칙 | DBA | High | To-Do | Y | W1 | DB명명규칙.md | B3-01과 통합 |
| C2-04 | C. 개발 표준 | C2. 네이밍 규칙 | 에러코드 체계 | `{도메인}_{3자리}` (ATM_001) | TL | High | To-Do | Y | W2 | 에러코드표.md | — |
| C3-01 | C. 개발 표준 | C3. 패키지 구조 | Backend 도메인 패키지 | kr.co.nh.atms.{module}.{domain} | TL | High | Done | N | W1 | 표준§1.2.2 | — |
| C3-02 | C. 개발 표준 | C3. 패키지 구조 | Frontend 디렉토리 구조 | pages/components/hooks/api/types | FE | High | To-Do | Y | W1 | 표준§1.3.1 | — |
| C4-01 | C. 개발 표준 | C4. 예외/로깅 | 예외 계층 확정 | Business/Auth/System | TL | High | To-Do | Y | W2 | 표준§1.2.8 | — |
| C4-02 | C. 개발 표준 | C4. 예외/로깅 | 로깅 마스킹 정책 | 비밀번호/주민번호/카드번호 | TL,Sec | High | To-Do | Y | W2 | 로깅정책.md | — |
| C4-03 | C. 개발 표준 | C4. 예외/로깅 | 감사 로그 항목 정의 | 로그인/권한변경/데이터변경 | TL,Sec | High | To-Do | Y | W2 | 감사정책.md | 금융권 필수 |

### D. Git 전략

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| D1-01 | D. Git 전략 | D1. 브랜치 전략 | 브랜치 전략 채택 | Modified Git Flow (main/develop/feature/release/hotfix) | TL | High | Done | N | W1 | 표준§2 | — |
| D1-02 | D. Git 전략 | D1. 브랜치 전략 | 브랜치 네이밍 규칙 | type/ATMS-NNN-영문설명 | TL | High | Done | N | W1 | 표준§2.3 | — |
| D1-03 | D. Git 전략 | D1. 브랜치 전략 | 보호 브랜치 설정 | main/develop direct push 금지 | TL,Infra | High | To-Do | Y | W1 | GitLab 설정 | — |
| D1-04 | D. Git 전략 | D1. 브랜치 전략 | 릴리즈 태그 규칙 | SemVer (v1.2.3) | TL | Medium | To-Do | N | W2 | 표준§2.4 | — |
| D2-01 | D. Git 전략 | D2. 커밋 규칙 | Conventional Commits 적용 | feat/fix/refactor/docs/test/chore | TL,BE,FE | High | Done | N | W1 | 표준§3 | — |
| D2-02 | D. Git 전략 | D2. 커밋 규칙 | 커밋 메시지 lint | commitlint + husky | TL | Medium | To-Do | Y | W2 | commitlint.config | — |
| D2-03 | D. Git 전략 | D2. 커밋 규칙 | Jira 이슈 연동 | Resolves/Refs 키워드 | PM,TL | High | To-Do | Y | W2 | 연동가이드 | Jira webhook |
| D3-01 | D. Git 전략 | D3. 코드리뷰 | PR 템플릿 적용 | .gitlab/merge_request_templates/ | TL | High | To-Do | N | W1 | 템플릿.md | 표준§4.5 |
| D3-02 | D. Git 전략 | D3. 코드리뷰 | 리뷰어 지정 규칙 | 도메인1 + 크로스1 | TL,PL | High | To-Do | Y | W2 | 리뷰규칙.md | — |
| D3-03 | D. Git 전략 | D3. 코드리뷰 | 리뷰 SLA | 일반 24h / hotfix 2h | TL | High | To-Do | Y | W2 | 리뷰규칙.md | — |
| D3-04 | D. Git 전략 | D3. 코드리뷰 | Merge 방식 | feature: Squash, release: Merge Commit | TL | High | Done | N | W1 | 표준§4.3 | — |
| D3-05 | D. Git 전략 | D3. 코드리뷰 | CODEOWNERS 설정 | 도메인별 자동 리뷰어 지정 | TL | Medium | To-Do | N | W3 | CODEOWNERS | — |

### E. 개발 프로세스

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| E1-01 | E. 개발 프로세스 | E1. 이슈 관리 | Jira 프로젝트 생성 | Key: ATMS | PM | High | To-Do | Y | W1 | Jira URL | — |
| E1-02 | E. 개발 프로세스 | E1. 이슈 관리 | 이슈 타입 정의 | Epic/Story/Task/Bug/Sub-task | PM,PL | High | To-Do | Y | W1 | Jira 설정 | — |
| E1-03 | E. 개발 프로세스 | E1. 이슈 관리 | Workflow 정의 | To-Do→In Progress→In Review→Done | PM | High | To-Do | Y | W1 | 워크플로우 | — |
| E1-04 | E. 개발 프로세스 | E1. 이슈 관리 | 이슈 작성 템플릿 | 배경/요구사항/완료조건 | PL | Medium | To-Do | N | W2 | 템플릿 | — |
| E2-01 | E. 개발 프로세스 | E2. Sprint 운영 | Sprint 주기 결정 | 2주 권장 | PM,PL | High | To-Do | Y | W1 | — | — |
| E2-02 | E. 개발 프로세스 | E2. Sprint 운영 | 데일리/위클리 미팅 | 일일 15분 / 주간 1시간 | PM | High | To-Do | Y | W1 | 회의일정 | — |
| E2-03 | E. 개발 프로세스 | E2. Sprint 운영 | 산정 단위 (MD vs SP) | MD 권장 (금융권) | PM,TL | Medium | To-Do | Y | W2 | — | — |
| E2-04 | E. 개발 프로세스 | E2. Sprint 운영 | 회고 운영 | KPT 또는 Start/Stop/Continue | PM | Medium | To-Do | N | W3 | 템플릿 | — |
| E3-01 | E. 개발 프로세스 | E3. DoR/DoD | Definition of Ready | 이슈 작업 착수 기준 | PL | High | To-Do | Y | W2 | DoR.md | — |
| E3-02 | E. 개발 프로세스 | E3. DoR/DoD | Definition of Done | Task/기능 단위 완료 기준 | PL | High | To-Do | Y | W2 | DoD.md | 표준§5.3 |

### F. 협업 도구/환경

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| F1-01 | F. 협업 도구 | F1. 이슈 트래커 | Jira 셋업 | 계정 발급, 권한 설정 | PM,Infra | High | To-Do | N | W1 | Jira URL | — |
| F2-01 | F. 협업 도구 | F2. 문서 도구 | Confluence Space 생성 | 회의록/설계서/매뉴얼 | PM | High | To-Do | N | W1 | Confluence URL | — |
| F2-02 | F. 협업 도구 | F2. 문서 도구 | 문서 명명 규칙 | NN_분류_제목 | PM,TL | Medium | To-Do | Y | W1 | 규칙.md | — |
| F2-03 | F. 협업 도구 | F2. 문서 도구 | ERD/다이어그램 도구 | dbdiagram.io / draw.io | TL | Medium | To-Do | N | W1 | — | — |
| F3-01 | F. 협업 도구 | F3. 소스 저장소 | GitLab 그룹/프로젝트 생성 | NH-ATMS 그룹 | Infra,TL | High | Done | N | W1 | GitLab URL | — |
| F3-02 | F. 협업 도구 | F3. 소스 저장소 | Webhook 설정 | Jira-GitLab 양방향 | Infra | Medium | To-Do | N | W2 | — | — |
| F4-01 | F. 협업 도구 | F4. 커뮤니케이션 | 채널/방 개설 | 전체/BE/FE/이슈공유 | PM | High | To-Do | N | W1 | 슬랙/팀즈 | — |
| F4-02 | F. 협업 도구 | F4. 커뮤니케이션 | 회의실 예약 정책 | 정기/임시 분리 | PM | Low | To-Do | N | W2 | — | — |
| F5-01 | F. 협업 도구 | F5. 개발 환경 | IntelliJ 설정 통일 | 인코딩(UTF-8), JDK21, 포맷터 | TL,BE | High | To-Do | Y | W1 | 가이드.md | — |
| F5-02 | F. 협업 도구 | F5. 개발 환경 | Node 버전 통일 | nvm + .nvmrc (v20) | TL,FE | High | To-Do | N | W1 | .nvmrc | — |
| F5-03 | F. 협업 도구 | F5. 개발 환경 | 로컬 DB | H2(개발) + Oracle XE Docker | Infra,BE | Medium | To-Do | Y | W2 | docker-compose | — |

### G. 테스트 전략

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| G1-01 | G. 테스트 전략 | G1. 단위 테스트 | 프레임워크 선정 | JUnit5 + Mockito | TL,BE | High | Done | N | W1 | — | — |
| G1-02 | G. 테스트 전략 | G1. 단위 테스트 | 커버리지 목표 | Service 80% / 전체 70% | TL,QA | High | To-Do | Y | W2 | 표준§9.4 | — |
| G1-03 | G. 테스트 전략 | G1. 단위 테스트 | Mock 전략 가이드 | Mapper/외부API Mock 처리 | TL | Medium | To-Do | N | W2 | 가이드 | — |
| G1-04 | G. 테스트 전략 | G1. 단위 테스트 | Frontend 테스트 도구 | Vitest + RTL + MSW | TL,FE | Medium | To-Do | Y | W2 | — | — |
| G2-01 | G. 테스트 전략 | G2. 통합 테스트 | @SpringBootTest 기준 | profile=local, H2 사용 | TL,BE | High | To-Do | Y | W2 | 가이드 | — |
| G2-02 | G. 테스트 전략 | G2. 통합 테스트 | 테스트 데이터 관리 | data.sql / @Sql / Testcontainers | BE,QA | Medium | To-Do | Y | W3 | — | — |
| G3-01 | G. 테스트 전략 | G3. 성능 테스트 | 도구 선정 | JMeter or k6 | QA,Infra | High | To-Do | Y | W2 | — | — |
| G3-02 | G. 테스트 전략 | G3. 성능 테스트 | 목표 시나리오 정의 | 동시 200 / p95 500ms | QA,PL | High | To-Do | Y | W2 | 시나리오.md | A2-01 연동 |
| G4-01 | G. 테스트 전략 | G4. 보안 테스트 | OWASP Top 10 점검 항목 | SQLi/XSS/권한우회 | Sec,QA | High | To-Do | Y | W3 | 체크리스트 | — |
| G4-02 | G. 테스트 전략 | G4. 보안 테스트 | 정적 분석 도구 | SonarQube + Snyk | TL,Sec | Medium | To-Do | Y | W3 | — | — |
| G5-01 | G. 테스트 전략 | G5. QA 프로세스 | 버그 트래킹 워크플로우 | Bug→Triage→Fix→Verify | QA,PM | High | To-Do | Y | W2 | 워크플로우 | — |
| G5-02 | G. 테스트 전략 | G5. QA 프로세스 | UAT 시나리오 양식 | 시나리오 ID/단계/기대결과 | QA,PL | Medium | To-Do | N | W3 | 양식 | — |

### H. 배포/형상관리

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| H1-01 | H. 배포/형상 | H1. 환경 분리 | Profile 정의 | local/dev/stg/prod | TL,Infra | High | To-Do | Y | W1 | 표준§8.1 | — |
| H1-02 | H. 배포/형상 | H1. 환경 분리 | 환경별 DB/서버 매핑 | DEV/STG/PROD 인스턴스 | Infra | High | To-Do | Y | W2 | 환경표 | — |
| H1-03 | H. 배포/형상 | H1. 환경 분리 | Secrets 관리 방식 | OS env / Vault | Infra,TL | High | To-Do | Y | W2 | 정책 | — |
| H2-01 | H. 배포/형상 | H2. 배포 파이프라인 | CI 구성 | GitLab CI: build/test/quality | Infra,TL | High | To-Do | Y | W2 | .gitlab-ci.yml | — |
| H2-02 | H. 배포/형상 | H2. 배포 파이프라인 | CD 구성 | DEV 자동 / STG·PROD 수동 승인 | Infra | High | To-Do | Y | W2 | — | — |
| H2-03 | H. 배포/형상 | H2. 배포 파이프라인 | JEUS WAR 패키징 | bootWar + jeus-web-dd.xml | TL,Infra | High | To-Do | Y | W3 | 가이드 | — |
| H2-04 | H. 배포/형상 | H2. 배포 파이프라인 | 롤백 절차 | 이전 버전 즉시 복구 절차 | Infra | High | To-Do | Y | W3 | 절차서 | — |
| H3-01 | H. 배포/형상 | H3. 형상관리 | 산출물 버전 관리 | 문서/스크립트/스키마 버전 정책 | PM,TL | Medium | To-Do | N | W3 | 정책 | — |
| H3-02 | H. 배포/형상 | H3. 형상관리 | DB 변경 관리 | Flyway 도입 | DBA,TL | High | To-Do | Y | W2 | — | — |

### I. 공통 컴포넌트

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| I1-01 | I. 공통 컴포넌트 | I1. Backend 공통 | ApiResponse 표준 응답 | success/code/message/data | TL,BE | High | Done | N | W1 | 표준§1.2.7 | — |
| I1-02 | I. 공통 컴포넌트 | I1. Backend 공통 | GlobalExceptionHandler | 예외→ApiResponse 변환 | TL,BE | High | Done | N | W1 | — | — |
| I1-03 | I. 공통 컴포넌트 | I1. Backend 공통 | JWT 인증 필터 | access/refresh, 블랙리스트 | BE | High | To-Do | Y | W2 | — | — |
| I1-04 | I. 공통 컴포넌트 | I1. Backend 공통 | 감사로그 AOP | @Auditable 어노테이션 | BE,Sec | High | To-Do | Y | W3 | — | — |
| I1-05 | I. 공통 컴포넌트 | I1. Backend 공통 | MyBatis TypeHandler | LocalDateTime/Boolean | BE | Medium | To-Do | N | W2 | — | — |
| I1-06 | I. 공통 컴포넌트 | I1. Backend 공통 | 페이징 유틸 | PageRequest/PageResponse | BE | High | To-Do | Y | W2 | — | — |
| I1-07 | I. 공통 컴포넌트 | I1. Backend 공통 | Excel 업/다운 유틸 | POI 기반 | BE | Medium | To-Do | N | W3 | — | — |
| I1-08 | I. 공통 컴포넌트 | I1. Backend 공통 | 공통코드 캐시 | Caffeine 5분 TTL | BE | Medium | To-Do | N | W3 | — | — |
| I2-01 | I. 공통 컴포넌트 | I2. Frontend 공통 | axios client + 인터셉터 | JWT 자동 첨부, 401 처리 | FE | High | To-Do | Y | W2 | — | — |
| I2-02 | I. 공통 컴포넌트 | I2. Frontend 공통 | 공통 Layout (Header/Side) | 메뉴 동적 렌더링 | FE | High | To-Do | Y | W2 | — | — |
| I2-03 | I. 공통 컴포넌트 | I2. Frontend 공통 | DataTable 래퍼 | 페이징/정렬/검색 표준 | FE | High | To-Do | Y | W2 | — | — |
| I2-04 | I. 공통 컴포넌트 | I2. Frontend 공통 | SearchForm 표준 | 검색 폼 컴포넌트 | FE | Medium | To-Do | N | W3 | — | — |
| I2-05 | I. 공통 컴포넌트 | I2. Frontend 공통 | CodeSelect | 공통코드 셀렉터 | FE | Medium | To-Do | N | W3 | — | — |
| I2-06 | I. 공통 컴포넌트 | I2. Frontend 공통 | ConfirmModal/Toast | 확인/알림 유틸 | FE | Medium | To-Do | N | W3 | — | — |
| I2-07 | I. 공통 컴포넌트 | I2. Frontend 공통 | PrivateRoute | JWT 검증 + 권한 체크 | FE | High | To-Do | Y | W2 | — | — |
| I2-08 | I. 공통 컴포넌트 | I2. Frontend 공통 | ErrorBoundary | 화면 단위 오류 격리 | FE | Medium | To-Do | N | W3 | — | — |

### J. 리스크/이슈

| ID | 대분류 | 중분류 | 세부 항목 | 설명 | 담당자 | 우선순위 | 상태 | 회의 필요 | 목표 주차 | 산출물 | 비고 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| J1-01 | J. 리스크/이슈 | J1. 리스크 등록부 | 리스크 등록부 양식 | ID/내용/영향/확률/대응 | PM | High | To-Do | Y | W1 | 양식.xlsx | — |
| J1-02 | J. 리스크/이슈 | J1. 리스크 등록부 | 주간 리스크 리뷰 | 정기 회의 안건 | PM | Medium | To-Do | N | W2 | 회의록 | — |
| J2-01 | J. 리스크/이슈 | J2. 외부 의존성 | 농협 SSO 스펙 확정 | 인증 모듈 차단 위험 | PM,TL | High | To-Do | Y | W2 | 스펙서 | 외부협의 |
| J2-02 | J. 리스크/이슈 | J2. 외부 의존성 | EAI/대외기관 스펙 | IF 개발 차단 위험 | PM,TL | High | To-Do | Y | W2 | 스펙서 | 외부협의 |
| J2-03 | J. 리스크/이슈 | J2. 외부 의존성 | Oracle 라이센스/계정 | DB 환경 차단 위험 | PM,Infra | High | To-Do | Y | W1 | — | — |
| J2-04 | J. 리스크/이슈 | J2. 외부 의존성 | JEUS 라이센스/환경 | 운영 배포 차단 위험 | PM,Infra | High | To-Do | Y | W2 | — | — |
| J3-01 | J. 리스크/이슈 | J3. 기술 리스크 | 거래저널 대용량 성능 | 조회 성능 저하 위험 | TL,DBA | High | To-Do | Y | W2 | 검증계획 | 파티셔닝 |
| J3-02 | J. 리스크/이슈 | J3. 기술 리스크 | MySQL→Oracle SQL 호환 | 개발 후반 대량 수정 위험 | TL,DBA | High | To-Do | Y | W2 | 가이드 | 표준§8.4 |
| J3-03 | J. 리스크/이슈 | J3. 기술 리스크 | JEUS 9.1 노하우 부족 | 컷오버 위험 | TL,Infra | Medium | To-Do | Y | W3 | 학습계획 | — |

---

## 3) 3주 운영 전략

### 3.1 주간 캘린더

| 주차 | 기간 | 주제 | 핵심 목표 |
|---|---|---|---|
| **W1** | 05/19 ~ 05/22 | **기반 표준 확정** | 표준/규칙/도구 셋업 |
| **W2** | 05/25 ~ 05/29 | **설계 합의** | DB/IF/보안/CI 설계 합의 |
| **W3** | 06/01 ~ 06/05 | **검증 & 리허설** | 환경 검증, 미결사항 클로징, 킥오프 준비 |

### 3.2 정례 회의 운영안

| 회의 | 빈도 | 참석 | 시간 | 안건 |
|---|---|---|---|---|
| **Daily Standup** | 매일 09:30 | 전원 | 15분 | 어제/오늘/블로커 |
| **Working Group (DB)** | 화/목 14:00 | TL/DBA/BE | 1시간 | DB 설계 |
| **Working Group (API)** | 화/목 16:00 | TL/BE/FE | 1시간 | API/공통컴포넌트 |
| **Weekly Review** | 매주 금 17:00 | 전원 | 1시간 | WBS 진척, 다음주 계획 |
| **리스크 리뷰** | 격주 금 16:00 | PM/PL/TL | 30분 | 리스크 등록부 점검 |

### 3.3 W1 (Week 1) — 기반 표준 확정 [05/19~05/22]

**목표**: 모든 도구/표준/규칙을 합의하고 문서화

**핵심 미팅 안건** (High + 회의 필요 Y 항목 기반)

| 일자 | 안건 | ID |
|---|---|---|
| 화 | NFR(성능/가용성) 목표 + 화면 기능 목록 1차 | A1-01, A1-02, A2-01, A2-02 |
| 수 | 시스템 아키텍처/외부 IF 목록 | B1-01, B1-02 |
| 목 | DB 명명규칙/audit 컬럼 표준 + 패키지/네이밍 컨벤션 | B3-01, B3-02, C1-01, C1-02 |
| 금 | Jira/Confluence 셋업, GitLab 보호브랜치, Sprint 운영안 | E1-01~03, E2-01~02, D1-03, F1-01, F2-01 |

**W1 종료 시 산출물**
- ✅ NFR 명세서 v0.1
- ✅ 기능 목록표 v0.1 (우선순위 포함)
- ✅ DB 명명규칙/표준 컬럼 정의서
- ✅ IntelliJ/ESLint 포맷터 배포
- ✅ Jira/Confluence/GitLab 환경 완료
- ✅ Sprint 주기/회의 일정 합의

---

### 3.4 W2 (Week 2) — 설계 합의 [05/25~05/29]

**목표**: DB/IF/보안/CI 등 깊이 있는 설계를 끝낸다.

**핵심 미팅 안건**

| 일자 | 안건 | ID |
|---|---|---|
| 월 | AS-IS DB → TO-BE 매핑표 1차 리뷰 | B3-03 |
| 화 | 거래저널 파티셔닝/인덱스 전략 | B3-04, J3-01 |
| 수 | 보안 정책 / 감사 로그 / 로깅 마스킹 | A2-03, C4-02, C4-03 |
| 목 | CI/CD 파이프라인 + 환경 분리 + Secrets | H1-01~03, H2-01~02 |
| 금 | JWT/RBAC + 공통 컴포넌트 (페이징/axios/Layout) | I1-03, I1-06, I2-01~03, I2-07 |

**W2 종료 시 산출물**
- ✅ DB 매핑표 + Flyway V1~Vn 초안
- ✅ 보안 정책서 / 감사로그 정책서
- ✅ `.gitlab-ci.yml` 동작 (build+test)
- ✅ DoR/DoD 합의
- ✅ 외부 의존성 (SSO/EAI/Oracle/JEUS) 확보 현황 명확화

---

### 3.5 W3 (Week 3) — 검증 & 리허설 [06/01~06/05]

**목표**: 합의된 표준이 실제로 동작하는지 검증하고, 본 투입 킥오프를 준비한다.

**핵심 활동**

| 일자 | 활동 | 비고 |
|---|---|---|
| 월 | 합의 사항 dry-run (가짜 이슈로 전체 흐름 1회 실행: 이슈→브랜치→PR→Merge→배포) | E1~E3, D1~D3, H2 검증 |
| 화 | JEUS WAR 패키징/배포 리허설 | H2-03, J3-03 |
| 수 | 성능 테스트 환경 구축 + 시나리오 작성 | G3-01~02 |
| 목 | 미결 사항 클로징 회의 — 모든 W1/W2 High 항목 Done 확인 | — |
| 금 | **킥오프 준비 회의** — 본 투입 W1 Sprint Backlog 확정 | — |

**W3 종료 시 산출물**
- ✅ 전체 협업 흐름 검증 완료 (이슈→배포)
- ✅ JEUS 배포 절차서 v1.0
- ✅ 성능 테스트 시나리오 v1.0
- ✅ 본 투입 첫 Sprint Backlog 확정
- ✅ Risk Register v1.0

---

### 3.6 진척 관리 방식

**Jira 운영 (3주간)**:
- Epic: `SETUP-3주사전준비` 1개
- Story: 대분류 단위 (A~J) 10개
- Sub-task: WBS 세부항목 (A1-01 …) 100여개

**대시보드**:
- 주간 진척률 (전체 / 우선순위별 / 담당자별)
- 회의 필요(Y) 미해결 항목 Top 10
- High 우선순위 미완료 항목 → **W3 금요일까지 0건 목표**

**Exit Criteria (3주 종료 = 본 투입 가능 기준)**:
- ☑ High 우선순위 100% Done
- ☑ Medium 우선순위 80% 이상 Done
- ☑ 외부 의존성(J2) 모두 클리어 또는 Workaround 확정
- ☑ 본 투입 W1 Sprint Backlog (10~15개 Story) 확정
- ☑ 전원 표준 가이드 숙지 (오리엔테이션 완료)

---

## 부록. 참조 문서

| 문서 | 위치 |
|---|---|
| 개발 표준 가이드 | [10_개발표준가이드.md](10_개발표준가이드.md) |
| 개발 로드맵 | [11_개발로드맵_TODO.md](11_개발로드맵_TODO.md) |
| AS-IS DB 분석 | [00_DB분석_모듈매핑.md](00_DB분석_모듈매핑.md) |
| 본 WBS (CSV) | [12_사전준비_WBS.csv](12_사전준비_WBS.csv) |
