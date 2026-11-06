# NH-ATMS 개발 로드맵 & TODO 리스트

> **작성일**: 2026-05-15
> **현재 상태**: 프로젝트 스켈레톤 완료 (Backend 멀티모듈 8개 도메인 + Frontend 골격 + GitLab 초기 push)
> **목표**: AS-IS(농협 기존) DB/기능 분석 → TO-BE 설계 → 단계별 구현 → 운영 이행
> **참고 자료**:
> - AS-IS DB 이미지: `0.기본자료/농협은행/1.DB/*.png`
> - AS-IS 분석: `0.기본자료/농협은행/1.DB/NH AS-IS 테이블 분석.md`
> - DB 매핑: [Document/00_DB분석_모듈매핑.md](00_DB분석_모듈매핑.md)
> - 개발 표준: [Document/10_개발표준가이드.md](10_개발표준가이드.md)

---

## 📍 전체 로드맵 한눈에 보기

```
[Phase 0] 분석/설계 준비 ─── 2주
    │
[Phase 1] DB 설계 + 공통 인프라 ─── 3주
    │
[Phase 2] 인증/권한 + 핵심 도메인 ─── 4주
    │
[Phase 3] 운영 화면 (메인 기능) ─── 6주
    │
[Phase 4] 거래/저널/배치/IF ─── 6주
    │
[Phase 5] 통합 테스트 + 성능/보안 ─── 3주
    │
[Phase 6] UAT + 운영 이행 ─── 2주
                                                총 ≈ 26주 (6개월)
```

---

# Phase 0. 분석 / 설계 준비 (2주)

## 🎯 목표
AS-IS 시스템을 정확히 이해하고, TO-BE 범위를 확정한다.

## ✅ TODO

### 0.1 AS-IS 분석 (1주)
- [ ] **AS-IS DB 스키마 정리** — 이미지 11장(`1-1-1.png` ~ `2-2-3.png`) 기반
  - [ ] 테이블 목록 → Excel/Markdown 표로 정리 (테이블명, 한글명, 용도, 컬럼수)
  - [ ] PK/FK 관계도 작성 (draw.io 또는 dbdiagram.io)
  - [ ] 레거시 테이블 vs 사용 중 테이블 구분 (Deprecated 마킹)
- [ ] **AS-IS 기능 인벤토리** — 새마을금고 ATMS 메뉴 매핑
  - 참고: `0.기본자료/새마을금고/새마을금고 메뉴리스트.md`
  - [ ] 화면 단위 기능 리스트 작성 (대분류/중분류/소분류)
  - [ ] 화면별 핵심 SQL 추출
- [ ] **기존 인터페이스 스펙 분석**
  - 참고: `Document/08_IF분석_Agent_ATM.md`, `agent-api-openapi.yaml`
  - [ ] ATM Agent ↔ 서버 메시지 포맷
  - [ ] EAI/대외기관 연동 스펙

### 0.2 TO-BE 범위 확정 (1주)
- [ ] **이행 대상 vs 신규 개발 분리**
  - 이행: 거래저널, 기기정보, 거래코드, 사용자/권한
  - 신규: 실시간 모니터링 대시보드, 알림, 대화형 분석
- [ ] **Out-of-Scope 명시** (RFP 기반 제외 기능)
- [ ] **요구사항 매트릭스** (RFP 항목 ↔ 기능 ID ↔ Phase)
- [ ] **WBS 초안 작성** → Jira Epic 등록

### 산출물
- `Document/11_AS-IS_DB분석.md`
- `Document/12_요구사항매트릭스.md`
- Jira Epic 8개 (Phase별)

---

# Phase 1. DB 설계 + 공통 인프라 (3주)

## 🎯 목표
TO-BE 스키마 확정 + 모든 모듈이 의존할 공통 기반을 안정화한다.

## ✅ TODO

### 1.1 TO-BE DB 설계 (2주)

#### 설계 시 고려사항 (AS-IS → TO-BE 전환 포인트)

| 항목 | AS-IS (MySQL) | TO-BE (Oracle 19C) |
|---|---|---|
| **문자셋** | utf8mb4 | AL32UTF8 + `VARCHAR2(n CHAR)` |
| **Boolean** | TINYINT(1) | NUMBER(1) — 0/1 |
| **Auto-increment** | AUTO_INCREMENT | SEQUENCE + Trigger (또는 IDENTITY) |
| **페이징** | LIMIT/OFFSET | OFFSET … FETCH NEXT |
| **함수** | NOW(), IFNULL | SYSDATE, NVL |
| **명명규칙** | camelCase 혼재 | **snake_case + 접두사 통일** (`tb_`, `tc_`, `th_`) |
| **컬럼 표준** | 누락 | **공통 audit 컬럼 의무화** (reg_dt, reg_user, upd_dt, upd_user) |
| **삭제** | DELETE | **Soft Delete** (del_yn = 'Y') 권장 |

#### 작업 항목
- [ ] **테이블 명명 규칙 확정**
  - 마스터: `tb_*` (tb_atm, tb_branch)
  - 거래/이력: `th_*` (th_journal, th_atm_status)
  - 코드: `tc_*` (tc_common_code, tc_role)
  - 매핑: `tm_*` (tm_user_role)
- [ ] **공통 표준 컬럼 정의** — 모든 테이블 의무
  ```sql
  reg_dt    TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL
  reg_user  VARCHAR2(20 CHAR) NOT NULL
  upd_dt    TIMESTAMP
  upd_user  VARCHAR2(20 CHAR)
  del_yn    CHAR(1)      DEFAULT 'N' NOT NULL
  ```
- [ ] **도메인별 ERD 작성** (8개 도메인)
  1. 사용자/권한 (user, role, user_role, menu, role_menu)
  2. ATM 마스터 (atm, atm_group, branch)
  3. ATM 상태/이력 (atm_status, atm_status_history)
  4. 거래저널 (journal, journal_detail) — **파티셔닝 검토**
  5. 코드 (common_code, common_code_group)
  6. 공지 (notice, notice_attach)
  7. 인터페이스 로그 (if_log_internal, if_log_external)
  8. 배치 이력 (batch_history)
- [ ] **인덱스 전략**
  - PK는 자동
  - FK + 자주 검색되는 컬럼 (term_id, branch_cd, reg_dt)
  - 거래저널: `(reg_dt, term_id)` 복합 인덱스
- [ ] **파티셔닝 전략** — 대용량 테이블
  - `th_journal`: RANGE 파티셔닝 (월별)
  - `th_atm_status_history`: RANGE 파티셔닝 (월별)
- [ ] **시퀀스 정의** (각 테이블별 또는 공용)
- [ ] **Flyway 스크립트 작성**
  ```
  V1__create_user_role.sql
  V2__create_atm.sql
  V3__create_journal.sql
  V4__seed_common_code.sql
  ```

### 1.2 공통 인프라 보강 (1주)

- [ ] **글로벌 예외 처리 강화**
  - BusinessException, AuthException, SystemException 분리
  - 에러코드 표 작성 (`Document/13_에러코드표.md`)
- [ ] **표준 응답 포맷 확정** — `ApiResponse<T>` 정착
- [ ] **AOP 감사 로그**
  - `@Auditable` 어노테이션 → 사용자/IP/요청파라미터 자동 기록
- [ ] **로깅 표준화**
  - logback-spring.xml: profile별 분리 (콘솔/파일/JSON)
  - 운영: JSON 포맷 + ELK/Grafana Loki 연동 대비
  - 마스킹 필터: 비밀번호/주민번호/카드번호
- [ ] **MyBatis Type Handler**
  - LocalDateTime ↔ TIMESTAMP
  - Boolean ↔ CHAR(1) 'Y'/'N'
- [ ] **Swagger 그룹화** (도메인별 Tag, Bearer 인증 통합)
- [ ] **HikariCP 튜닝** (운영 풀 사이즈, idle timeout)
- [ ] **CORS / CSRF 정책 확정**
- [ ] **CI 파이프라인 구축** (GitLab CI)
  - build → test → jacoco → sonar → artifact

### 산출물
- `Document/04_DB스키마설계.md` (확정판)
- `Document/13_에러코드표.md`
- Flyway 마이그레이션 V1~V10
- ERD 다이어그램 (PNG + dbdiagram.io 링크)

---

# Phase 2. 인증/권한 + 핵심 도메인 CRUD (4주)

## 🎯 목표
모든 후속 기능이 의존하는 **인증·권한·기준정보**를 안정화한다.

## ✅ TODO

### 2.1 인증 / 권한 (2주)

#### 설계 원칙 (내부 시스템 기준)
- **JWT** access(30분) + refresh(8시간) — 내부망이지만 토큰 기반 일관성 유지
- **SSO 어댑터 패턴** — Mock SSO(개발) ↔ 농협 SSO(운영) 인터페이스 동일
- **RBAC** (Role-Based Access Control) — 사용자 ↔ 역할 ↔ 메뉴/API
- **세션 동시 접속 제한** (1 user = 1 session, 강제 로그아웃)
- **로그인 실패 5회 → 계정 잠금** (보안 정책)

#### 작업 항목
- [ ] **사용자 도메인 완성**
  - [ ] 사용자 등록/수정/삭제/잠금/비밀번호 초기화
  - [ ] 비밀번호 정책 (10자 이상, 영문+숫자+특수문자, 90일 변경)
  - [ ] BCrypt 강도 12
- [ ] **역할/권한 도메인 완성**
  - [ ] 역할 CRUD
  - [ ] 메뉴 트리 관리 (parent_id, sort_order)
  - [ ] 역할-메뉴 매핑 (조회/등록/수정/삭제 권한)
- [ ] **JWT 인증 필터**
  - [ ] AccessToken 만료 시 RefreshToken 갱신
  - [ ] 블랙리스트 (로그아웃된 토큰)
- [ ] **메소드 보안**
  - `@PreAuthorize("hasRole('ADMIN')")` 적용
  - 또는 URL 패턴 + 메뉴매핑으로 동적 권한 검사
- [ ] **감사 로그 (필수)**
  - 로그인/로그아웃, 권한 변경, 데이터 변경 → `tb_audit_log`
- [ ] **메뉴 권한 API**
  - `GET /api/auth/menu` → 로그인 사용자가 접근 가능한 메뉴 트리

### 2.2 기준정보 도메인 강화 (2주)

- [ ] **공통코드 (common-code)**
  - [ ] 그룹/항목 관리 + 캐시 (Caffeine, 5분 TTL)
  - [ ] 다국어 지원 컬럼 유지 (lang_cd)
- [ ] **영업점 (branch)**
  - [ ] 트리 구조 지원 (본부 → 지역본부 → 영업점)
  - [ ] Excel 업/다운로드
- [ ] **ATM 그룹 (atm-group)**
  - [ ] 그룹별 ATM 일괄 정책 적용
- [ ] **ATM 마스터 (atm)**
  - [ ] 영업점/그룹 연관 검색
  - [ ] 일괄 등록 (Excel)
  - [ ] 변경 이력 추적

### 산출물
- 8개 도메인 API 완성 + 통합 테스트
- 권한 매트릭스 표 (`Document/14_권한매트릭스.md`)
- 감사로그 정책서

---

# Phase 3. 운영 화면 (메인 기능) (6주)

## 🎯 목표
사용자가 매일 사용하는 **운영 대시보드 + 기준정보 화면**을 완성한다.

## ✅ TODO

### 3.1 Frontend 공통 인프라 (1주)
- [ ] **공통 레이아웃**
  - Header (로그아웃, 사용자정보) / Sidebar (메뉴트리) / Footer
  - 메뉴는 `/api/auth/menu` 응답 기반 동적 렌더링
- [ ] **라우터 설계** (React Router v6)
  - PrivateRoute (JWT 검증)
  - 페이지 lazy loading
- [ ] **상태 관리**
  - 서버 상태: `@tanstack/react-query`
  - 인증: `zustand` persist
- [ ] **공통 컴포넌트**
  - DataTable (Ant Design Table 래퍼 — 페이징/정렬/검색 표준)
  - SearchForm (검색 폼 표준)
  - DateRangePicker, CodeSelect (공통코드 셀렉터)
  - ConfirmModal, ExcelUploadModal
- [ ] **에러 바운더리** + 401 자동 로그아웃
- [ ] **다국어 (i18n)** — 향후 확장 대비 구조만 (한국어 기본)

### 3.2 화면 우선순위 (Critical Path)

#### 우선순위 ★★★ (Phase 3 필수)
- [ ] **로그인 / 로그아웃 / 비밀번호 변경**
- [ ] **메인 대시보드** — ATM 실시간 현황 (정상/장애/점검 카운트, 차트)
- [ ] **ATM 목록/상세/등록/수정**
- [ ] **ATM 상태 모니터링** — 실시간 상태 (5초 폴링 또는 WebSocket)
- [ ] **영업점 관리**
- [ ] **사용자/역할/권한 관리**
- [ ] **공통코드 관리**

#### 우선순위 ★★ (Phase 3 후반)
- [ ] **공지사항**
- [ ] **메뉴 관리**
- [ ] **My Page** (개인정보 수정)

### 3.3 사용자 흐름 (Critical User Journey)

```
[로그인]
   ↓
[메인 대시보드] ──→ ATM 카드 클릭 ──→ [ATM 상세]
   ↓
[좌측메뉴: 기기관리]
   ├─ ATM 목록 → 검색/필터 → 상세 → 수정
   ├─ ATM 그룹
   ├─ 영업점
   └─ 사용자/권한
```

### 산출물
- 화면 15~20개 완성
- Storybook (공통 컴포넌트 카탈로그) — 선택
- 화면별 테스트 시나리오 (`Document/15_화면시나리오.md`)

---

# Phase 4. 거래/저널/배치/인터페이스 (6주)

## 🎯 목표
대용량/비동기 영역 — **거래저널, 배치, 외부 IF**를 구현한다.

## ✅ TODO

### 4.1 거래저널 (`atms-journal`) (2주)
- [ ] 거래저널 조회 (날짜/단말/거래유형 검색, 페이징)
- [ ] 상세 조회 (거래 추적)
- [ ] Excel 다운로드 (대용량 — Streaming POI)
- [ ] 파티션 검색 최적화 (`reg_dt` 범위 우선)

### 4.2 내부 인터페이스 (`atms-if-internal`) (2주)
- [ ] ATM Agent 헬스체크 수신
- [ ] ATM 상태 변경 이벤트 수신 → 실시간 반영
- [ ] 명령 송신 (재시작, 점검모드)
- [ ] 메시지 큐 검토 (Kafka or RabbitMQ — 향후)

### 4.3 외부 인터페이스 (`atms-if-external`) (1주)
- [ ] 대외기관 연동 (암호화 — `eaiCryptoSample/CryptoManager.java` 참조)
- [ ] IF 호출 로그 + 재시도 정책
- [ ] Circuit Breaker (Resilience4j)

### 4.4 배치 (`atms-batch`) (1주)
- [ ] Spring Batch 설정
- [ ] 일일 통계 집계 (전일 거래량, 장애 건수)
- [ ] 월말 정산
- [ ] 배치 모니터링 화면 (실행 이력, 실패 재실행)

### 산출물
- 비동기 처리 아키텍처 다이어그램
- IF 스펙서 (`Document/16_IF스펙.md`)

---

# Phase 5. 통합 테스트 + 성능/보안 (3주)

## ✅ TODO

### 5.1 통합 테스트 (1주)
- [ ] E2E 시나리오 작성 (Cypress 또는 수동)
- [ ] 회귀 테스트 자동화
- [ ] 데이터 이행 리허설 (AS-IS → TO-BE)

### 5.2 성능 (1주)
- [ ] 부하 테스트 (JMeter / k6)
  - 목표: 동시접속 200명, API p95 < 500ms
- [ ] 거래저널 조회 1억건 기준 성능 측정
- [ ] N+1 쿼리 점검
- [ ] HikariCP, JEUS Thread Pool 튜닝

### 5.3 보안 (1주)
- [ ] **OWASP Top 10 점검**
- [ ] SQL Injection (MyBatis `${}` 사용 금지 점검)
- [ ] XSS (Frontend 입력 검증)
- [ ] 권한 우회 (수평/수직 권한 상승 테스트)
- [ ] 보안 점검 도구: SonarQube, Snyk, OWASP Dependency-Check
- [ ] 모의해킹 (외부 업체)

---

# Phase 6. UAT + 운영 이행 (2주)

## ✅ TODO
- [ ] UAT 환경 구성 (운영과 동일 — JEUS + WebtoB + Oracle)
- [ ] UAT 시나리오 수행 (현업 담당자)
- [ ] 데이터 이행 본 작업 (AS-IS DB → TO-BE)
- [ ] 운영 매뉴얼 / 사용자 매뉴얼 작성
- [ ] 운영자 교육
- [ ] 컷오버 계획서 (Rollback 시나리오 포함)
- [ ] 운영 배포 & 모니터링

---

# 🌳 Git 협업 전략 (전 Phase 공통)

> 상세: [Document/10_개발표준가이드.md § 2~5](10_개발표준가이드.md)

## 운영 규칙 요약

| 항목 | 규칙 |
|---|---|
| 브랜치 전략 | Modified Git Flow (main / develop / feature / release / hotfix) |
| 브랜치명 | `<type>/ATMS-<번호>-<영문설명>` |
| 커밋 메시지 | Conventional Commits — `feat(atm): ATM 등록 API 추가` |
| PR 정책 | 1 PR = 1 이슈, 300줄 이내, 리뷰어 2명, CI 통과 |
| Merge 방식 | feature→develop: Squash / release→main: Merge Commit |
| 보호 브랜치 | main, develop (직접 push 금지) |
| 리뷰 SLA | 일반 24h, hotfix 2h |

## Phase별 브랜치 전략

```
Phase 1~2: feature 브랜치 → develop 통합 (DEV 자동 배포)
Phase 3~4: 같은 흐름 + 주요 마일스톤마다 release/v0.x 분기 → STG 배포
Phase 5: release/v1.0 → UAT
Phase 6: main 머지 + tag v1.0.0 → PROD
```

---

# 🏛 확장성/유지보수 아키텍처 포인트

## 단기 (현재 ~ Phase 6)
- ✅ **Modular Monolith** 유지 — 도메인별 모듈 분리, 간섭 최소화
- ✅ **공통 라이브러리(`atms-common`) 일원화**
- ✅ **표준 응답/예외 일관성**
- ✅ **Profile 기반 환경 분리**

## 중기 (운영 안정화 후)
- [ ] **이벤트 기반 통신** — 도메인 간 직접 호출 → Spring Application Event
- [ ] **캐시 도입** — Redis (공통코드, 세션, 권한)
- [ ] **메시지 큐** — Kafka 도입 (저널 비동기 적재, 알림)
- [ ] **모니터링 강화** — Micrometer + Prometheus + Grafana (이미 폴더 구조 있음)
- [ ] **분산 추적** — Sleuth / Zipkin

## 장기 (필요 시)
- [ ] **MSA 분리 전략** — 도메인별 모듈을 독립 서비스로 분리 가능하도록 인터페이스 명확화
  - 분리 후보: `atms-journal` (대용량), `atms-if-external` (보안)
- [ ] **Read Replica** — 조회 트래픽 분리
- [ ] **CQRS** — 거래저널 조회 전용 데이터 모델

## 코드 레벨 설계 원칙
| 원칙 | 적용 |
|---|---|
| **DIP** | Service는 인터페이스(예: SsoService)에 의존 → 구현체 교체 가능 (Mock/Real) |
| **OCP** | 새 IF 추가 시 기존 코드 수정 X → 인터페이스 + 구현체 추가 |
| **SRP** | Service 200줄 초과 시 분리 검토 |
| **계층 격리** | Controller에서 Mapper 직접 호출 금지 |

---

# 📊 마일스톤 요약

| Phase | 기간 | 주요 산출물 | DEV/STG/PROD |
|---|---|---|---|
| 0 | 2주 | AS-IS 분석, WBS | — |
| 1 | 3주 | DB 스키마, 공통 인프라 | DEV |
| 2 | 4주 | 인증/권한, 기준정보 | DEV |
| 3 | 6주 | 운영 화면 | DEV |
| 4 | 6주 | 거래/배치/IF | STG |
| 5 | 3주 | 성능/보안 검증 | STG |
| 6 | 2주 | UAT + 이행 | PROD |
| **합계** | **26주** | | |

---

# 🚀 다음 7일 즉시 착수 항목

이번 주 / 다음 주 바로 시작할 작업:

1. [ ] **AS-IS DB 이미지 11장 → 마크다운 정리** (Phase 0.1)
2. [ ] **요구사항 매트릭스 1차 작성** + Jira Epic 등록 (Phase 0.2)
3. [ ] **TO-BE 테이블 명명 규칙 + 공통 audit 컬럼 표준 확정** (Phase 1.1)
4. [ ] **Flyway 도입 + V1 (user, role) 작성** (Phase 1.1)
5. [ ] **GitLab CI 파이프라인 초안** (build + test) (Phase 1.2)
6. [ ] **logback-spring.xml + 마스킹 필터** (Phase 1.2)
7. [ ] **Vite Proxy 정상화** ✅ 완료
8. [ ] **JWT RefreshToken 흐름 완성** (Phase 2.1 선행)

---

## 부록. 위험요소 & 대응

| 위험 | 영향 | 대응 |
|---|---|---|
| AS-IS DB 분석 지연 | Phase 1 일정 지연 | 분석 전담 1명 배정, 일일 진척 점검 |
| MySQL→Oracle SQL 호환 이슈 | 개발 후반 대량 수정 | Phase 1부터 Oracle 호환 SQL만 작성 (Codify) |
| 농협 SSO 스펙 미확정 | Phase 2 인증 지연 | Mock SSO 우선 + 어댑터 패턴 |
| 거래저널 대용량 성능 | Phase 5에서 발견 시 재설계 | Phase 1에서 파티셔닝/인덱스 선반영 |
| JEUS 배포 노하우 부족 | Phase 6 컷오버 위험 | Phase 4 단계에서 STG 배포 리허설 |
