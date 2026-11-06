# NH-ATMS 개발 표준 가이드 (Development Standard Guide)

> **버전**: v1.0
> **작성일**: 2026-05-15
> **대상**: NH-ATMS 프로젝트 전체 개발팀 (Backend / Frontend / DBA / QA)
> **준수 의무**: 본 문서는 **금융권 프로젝트 표준**으로, 모든 산출물·코드·커밋·PR은 본 가이드를 따라야 합니다.

---

## 목차

1. [코딩 컨벤션](#1-코딩-컨벤션)
2. [Git 브랜치 전략](#2-git-브랜치-전략)
3. [커밋 메시지 규칙](#3-커밋-메시지-규칙)
4. [Pull Request 정책 및 템플릿](#4-pull-request-정책-및-템플릿)
5. [개발 워크플로우](#5-개발-워크플로우)
6. [WBS 및 진척도 관리](#6-wbs-및-진척도-관리)
7. [프로젝트 구조 및 모듈 규칙](#7-프로젝트-구조-및-모듈-규칙)
8. [환경 및 설정 관리](#8-환경-및-설정-관리)
9. [테스트 및 품질 전략](#9-테스트-및-품질-전략)

---

# 1. 코딩 컨벤션

## 1.1 공통 원칙

| 원칙 | 설명 |
|---|---|
| **가독성 > 영리함** | 짧지만 이해하기 어려운 코드보다 길더라도 명확한 코드 |
| **명시적 > 암묵적** | 타입·반환·예외를 명시 |
| **단일 책임 (SRP)** | 클래스/함수는 하나의 책임만 |
| **불변성 우선** | `final`, `readonly`, `const` 적극 활용 |
| **금융권 특성** | 모든 핵심 로직은 **로깅 + 트랜잭션 + 예외처리** 3종 세트 필수 |

---

## 1.2 Java / Spring Boot 컨벤션

### 1.2.1 네이밍 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| 패키지 | 전체 소문자, 점 구분 | `kr.co.nh.atms.web.atm` |
| 클래스 | PascalCase, 명사 | `AtmService`, `AtmController` |
| 인터페이스 | PascalCase, 접미사 없음 | `AtmMapper` (구현체 X — MyBatis) |
| 메소드 | camelCase, 동사로 시작 | `findAtmById()`, `updateStatus()` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 변수 | camelCase, 의미 있는 명사 | `termGroupId`, `atmStatusList` |
| Enum 값 | UPPER_SNAKE_CASE | `STATUS_NORMAL`, `STATUS_FAULT` |
| 테스트 클래스 | `~Test` 접미사 | `AtmServiceTest` |
| DTO | 용도 접미사 명시 | `AtmCreateRequest`, `AtmResponse` |

**금지 네이밍**:
- `data`, `info`, `manager`, `helper`, `util` (단독 사용)
- 약어 단독 (`usr`, `mng`, `prc`) — 단, **도메인 표준 약어**(atm, atms, sso, jwt 등)는 허용
- 한글 변수명 (`사용자목록` ❌)

---

### 1.2.2 패키지 구조 (도메인 기반)

```
kr.co.nh.atms
├── common/                      # 공통 (atms-common 모듈)
│   ├── config/                  # Spring Config (Security, Web, Swagger…)
│   ├── exception/               # 전역 예외 + GlobalExceptionHandler
│   ├── response/                # ApiResponse 표준 응답
│   ├── security/                # JWT, AuthFilter
│   └── util/
└── web/                         # 운영 화면 API (atms-web 모듈)
    ├── AtmsWebApplication.java
    ├── auth/                    # 도메인별 패키지
    │   ├── AuthController.java
    │   ├── AuthService.java
    │   ├── AuthMapper.java
    │   ├── dto/
    │   │   ├── LoginRequest.java
    │   │   └── LoginResponse.java
    │   └── entity/
    │       └── User.java
    ├── atm/
    ├── branch/
    ├── atmgroup/
    ├── atmstatus/
    ├── user/
    ├── role/
    ├── notice/
    └── code/
```

> **원칙**: `web/{도메인}/{controller, service, mapper, dto, entity}` — 기능 중심이 아닌 **도메인 중심**으로 묶는다.

---

### 1.2.3 계층 설계 (Layered Architecture)

```
┌──────────────────────────────────┐
│  Controller  (REST API, 검증)     │  ← @RestController
├──────────────────────────────────┤
│  Service     (트랜잭션, 비즈니스)  │  ← @Service, @Transactional
├──────────────────────────────────┤
│  Mapper      (SQL 호출)           │  ← @Mapper (MyBatis)
├──────────────────────────────────┤
│  DB (Oracle 19C / H2 dev)        │
└──────────────────────────────────┘
```

**계층별 책임**:

| 계층 | 책임 | 금지 |
|---|---|---|
| Controller | HTTP 요청/응답, DTO 변환, 입력 검증(`@Valid`) | 비즈니스 로직, 트랜잭션, SQL 호출 |
| Service | 트랜잭션 경계, 도메인 규칙, 외부 호출 | HTTP 객체(`HttpServletRequest`) 직접 사용 |
| Mapper | SQL 매핑만 | 비즈니스 분기 |

---

### 1.2.4 DTO / Entity 규칙

**Entity** — DB 테이블과 1:1 매핑되는 도메인 객체
```java
@Getter
@Setter
@NoArgsConstructor
public class Atm {
    private String termGroupId;   // PK
    private String termId;        // PK
    private String termName;
    private String branchCd;
    private String installAddr;
    private LocalDateTime regDt;
    private String regUser;
    private LocalDateTime updDt;
    private String updUser;
}
```

**DTO** — 계층 간 / 외부 통신 전용
```java
// Request DTO — 입력 검증 포함
@Getter
@Setter
public class AtmCreateRequest {
    @NotBlank(message = "단말그룹ID는 필수입니다")
    @Size(max = 10)
    private String termGroupId;

    @NotBlank(message = "단말ID는 필수입니다")
    @Size(max = 20)
    private String termId;

    @NotBlank
    private String termName;
}

// Response DTO — 외부 노출 필드만
@Getter
@Builder
public class AtmResponse {
    private String termGroupId;
    private String termId;
    private String termName;
    private String statusName;
    private LocalDateTime updDt;

    public static AtmResponse from(Atm atm) {
        return AtmResponse.builder()
            .termGroupId(atm.getTermGroupId())
            .termId(atm.getTermId())
            .termName(atm.getTermName())
            .updDt(atm.getUpdDt())
            .build();
    }
}
```

**규칙**:
- Entity를 Controller에서 직접 반환 ❌ → 반드시 Response DTO로 변환
- Entity에 Validation 어노테이션 ❌ → DTO에서만
- DTO 변환은 **정적 팩토리 메소드** `from()`, `of()` 사용

---

### 1.2.5 Service 작성 규칙

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtmService {

    private final AtmMapper atmMapper;

    /** 단건 조회 */
    public AtmResponse findById(String termGroupId, String termId) {
        Atm atm = atmMapper.selectById(termGroupId, termId);
        if (atm == null) {
            throw new BusinessException("ATM_001", "ATM 정보를 찾을 수 없습니다.");
        }
        return AtmResponse.from(atm);
    }

    /** 등록 */
    @Transactional
    public void create(AtmCreateRequest request, String userId) {
        if (atmMapper.exists(request.getTermGroupId(), request.getTermId())) {
            throw new BusinessException("ATM_002", "이미 등록된 ATM입니다.");
        }
        Atm atm = new Atm();
        BeanUtils.copyProperties(request, atm);
        atm.setRegUser(userId);
        atm.setUpdUser(userId);
        atmMapper.insert(atm);
        log.info("[ATM 등록] user={}, termId={}", userId, request.getTermId());
    }
}
```

**규칙**:
- 클래스 레벨 `@Transactional(readOnly = true)` → 변경 메소드만 `@Transactional` 재선언
- 생성자 주입 (`@RequiredArgsConstructor`) — `@Autowired` 필드 주입 ❌
- 모든 변경 작업에 **감사 로그** (`log.info` with userId)

---

### 1.2.6 Controller 작성 규칙

```java
@Slf4j
@RestController
@RequestMapping("/api/atm")
@RequiredArgsConstructor
@Tag(name = "ATM", description = "ATM 기기 관리 API")
public class AtmController {

    private final AtmService atmService;

    @GetMapping("/{termGroupId}/{termId}")
    @Operation(summary = "ATM 단건 조회")
    public ApiResponse<AtmResponse> findOne(
        @PathVariable String termGroupId,
        @PathVariable String termId
    ) {
        return ApiResponse.ok(atmService.findById(termGroupId, termId));
    }

    @PostMapping
    @Operation(summary = "ATM 등록")
    public ApiResponse<Void> create(
        @Valid @RequestBody AtmCreateRequest request,
        @AuthenticationPrincipal LoginUser user
    ) {
        atmService.create(request, user.getUserId());
        return ApiResponse.ok();
    }
}
```

**규칙**:
- URL: kebab-case (`/api/atm-status`), 복수형은 사용 안 함 (도메인명 단수)
- Request body: `@Valid @RequestBody` 필수
- 응답: `ApiResponse<T>` 표준 래퍼 사용

---

### 1.2.7 API 설계 (RESTful)

| 동작 | HTTP Method | URL 예시 |
|---|---|---|
| 목록 조회 | GET | `/api/atm` |
| 단건 조회 | GET | `/api/atm/{termGroupId}/{termId}` |
| 등록 | POST | `/api/atm` |
| 전체 수정 | PUT | `/api/atm/{termGroupId}/{termId}` |
| 부분 수정 | PATCH | `/api/atm/{termGroupId}/{termId}` |
| 삭제 | DELETE | `/api/atm/{termGroupId}/{termId}` |
| 도메인 액션 | POST | `/api/atm/{id}/restart` |

**표준 응답 포맷**:

```json
// 성공
{
  "success": true,
  "code": "0000",
  "message": "OK",
  "data": { ... },
  "timestamp": "2026-05-15T13:36:26"
}

// 실패
{
  "success": false,
  "code": "ATM_001",
  "message": "ATM 정보를 찾을 수 없습니다.",
  "data": null,
  "timestamp": "2026-05-15T13:36:26"
}
```

**HTTP 상태 코드 가이드**:

| 코드 | 의미 | 사용 시점 |
|---|---|---|
| 200 | OK | 정상 처리 (등록 포함) |
| 400 | Bad Request | 입력 검증 실패 |
| 401 | Unauthorized | JWT 없음 / 만료 |
| 403 | Forbidden | 권한 부족 |
| 404 | Not Found | 리소스 없음 (URL 자체) |
| 409 | Conflict | 중복 등록 |
| 500 | Internal Error | 시스템 오류 (감사 로그 필수) |

---

### 1.2.8 예외 처리

**예외 계층**:

```
RuntimeException
├── BusinessException        (비즈니스 규칙 위반 — 사용자 안내)
├── AuthException             (인증 실패 — 401)
├── AuthorizationException    (권한 부족 — 403)
└── SystemException           (시스템 장애 — 500, 감사 로그)
```

**전역 핸들러**:

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("[BUSINESS] code={}, msg={}", e.getCode(), e.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.fail("VAL_001", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception e) {
        log.error("[SYSTEM] {}", e.getMessage(), e);   // 스택트레이스 필수
        return ResponseEntity.internalServerError()
            .body(ApiResponse.fail("SYS_001", "시스템 오류가 발생했습니다."));
    }
}
```

---

### 1.2.9 로깅 표준

**로깅 레벨 가이드**:

| 레벨 | 사용 시점 | 운영 출력 |
|---|---|---|
| `ERROR` | 시스템 장애, 외부 연동 실패 | ✅ |
| `WARN` | 비즈니스 예외, 비정상 입력 | ✅ |
| `INFO` | 인증/권한, 데이터 변경(C/U/D), 외부 호출 시작·종료 | ✅ |
| `DEBUG` | 파라미터 추적, SQL 결과 | ❌ (dev만) |
| `TRACE` | 상세 흐름 추적 | ❌ |

**필수 로깅 포인트** (금융권 감사 추적):

```java
// 1. 모든 데이터 변경
log.info("[ATM 등록] user={}, termId={}", userId, request.getTermId());

// 2. 인증/권한
log.info("[LOGIN] user={}, ip={}, success", userId, ip);
log.warn("[LOGIN_FAIL] user={}, ip={}, reason={}", userId, ip, reason);

// 3. 외부 시스템 호출
log.info("[EAI] req={}, payload={}", interfaceId, payload);
log.info("[EAI] res={}, elapsed={}ms", interfaceId, elapsed);

// 4. 시스템 오류
log.error("[SYSTEM] message={}", e.getMessage(), e);
```

**금지**:
- `System.out.println()` 사용 ❌
- 비밀번호, 주민번호, 카드번호 평문 로깅 ❌
- 개행/문자열 연결 (`+`) → 반드시 `{}` placeholder 사용

---

## 1.3 React + TypeScript 컨벤션

### 1.3.1 디렉토리 구조

```
src/
├── App.tsx
├── main.tsx
├── index.css
├── api/                      # API 클라이언트 (axios 인스턴스)
│   ├── client.ts
│   └── atm.ts
├── components/               # 재사용 가능한 공통 컴포넌트
│   ├── common/               # Button, Modal, Loading…
│   └── layout/               # Header, Sidebar, Footer
├── pages/                    # 라우트 단위 페이지 (도메인별)
│   ├── auth/
│   │   └── LoginPage.tsx
│   ├── atm/
│   │   ├── AtmListPage.tsx
│   │   └── AtmDetailPage.tsx
│   └── ...
├── hooks/                    # Custom Hook
│   ├── useAuth.ts
│   └── useAtmList.ts
├── stores/                   # Zustand / Context store
├── types/                    # 공통 타입 / API 타입
│   └── atm.ts
├── utils/                    # 순수 유틸 함수
└── routes/
    └── AppRouter.tsx
```

### 1.3.2 네이밍 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| 컴포넌트 파일 | PascalCase | `AtmListPage.tsx` |
| Hook 파일 | camelCase + `use` | `useAtmList.ts` |
| 유틸 파일 | camelCase | `dateFormat.ts` |
| 타입/인터페이스 | PascalCase | `Atm`, `AtmListResponse` |
| 상수 | UPPER_SNAKE_CASE | `API_BASE_URL` |
| 이벤트 핸들러 | `handle` 접두사 | `handleSubmit`, `handleClick` |
| Boolean 변수 | `is/has/can` 접두사 | `isLoading`, `hasError` |

### 1.3.3 컴포넌트 작성 규칙

```tsx
// pages/atm/AtmListPage.tsx
import { Table, Button, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAtmList } from '@/hooks/useAtmList';
import type { Atm } from '@/types/atm';

interface Props {
  branchCd?: string;
}

export default function AtmListPage({ branchCd }: Props) {
  const navigate = useNavigate();
  const { data, isLoading } = useAtmList({ branchCd });

  const handleRowClick = (record: Atm) => {
    navigate(`/atm/${record.termGroupId}/${record.termId}`);
  };

  const columns = [
    { title: '단말ID', dataIndex: 'termId', key: 'termId' },
    { title: '단말명', dataIndex: 'termName', key: 'termName' },
    { title: '상태', dataIndex: 'statusName', key: 'statusName' },
  ];

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Button type="primary" onClick={() => navigate('/atm/new')}>
        ATM 등록
      </Button>
      <Table<Atm>
        columns={columns}
        dataSource={data?.list ?? []}
        loading={isLoading}
        rowKey={(r) => `${r.termGroupId}-${r.termId}`}
        onRow={(record) => ({ onClick: () => handleRowClick(record) })}
      />
    </Space>
  );
}
```

**규칙**:
- **함수형 컴포넌트만 사용** (Class 컴포넌트 ❌)
- `default export` — 페이지 단위 / `named export` — 공통 컴포넌트
- Props는 `interface Props` 로 명시
- `any` 타입 절대 사용 금지 → `unknown` + 타입 가드
- 상태관리: 서버 상태는 `@tanstack/react-query`, 클라이언트 상태는 `zustand`

### 1.3.4 API 호출 표준

```ts
// api/client.ts — 공통 axios 인스턴스
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30_000,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (res) => res.data,           // ApiResponse 언랩
  (err) => {
    if (err.response?.status === 401) window.location.href = '/login';
    return Promise.reject(err);
  }
);
```

```ts
// api/atm.ts
import { apiClient } from './client';
import type { Atm, AtmListParams, ApiResponse } from '@/types';

export const atmApi = {
  list: (params: AtmListParams): Promise<ApiResponse<Atm[]>> =>
    apiClient.get('/atm', { params }),

  findOne: (termGroupId: string, termId: string): Promise<ApiResponse<Atm>> =>
    apiClient.get(`/atm/${termGroupId}/${termId}`),
};
```

```ts
// hooks/useAtmList.ts
import { useQuery } from '@tanstack/react-query';
import { atmApi } from '@/api/atm';

export function useAtmList(params: AtmListParams) {
  return useQuery({
    queryKey: ['atm', 'list', params],
    queryFn: () => atmApi.list(params),
    staleTime: 60_000,
  });
}
```

---

# 2. Git 브랜치 전략

## 2.1 채택 전략 — **Modified Git Flow** (금융권 안정성 강화)

> 일반 Git Flow를 NH-ATMS 환경에 맞게 변형. **release 브랜치는 정기 릴리즈에만 사용**, 긴급 패치는 hotfix로 운영합니다.

```
main          ●─────●─────●─────●─────●     (운영 배포 = 태그)
                \         \         /
release/v1.0     \         ●───●───●         (릴리즈 안정화)
                  \       /
develop      ●───●─●─────●─●───●─●─●         (통합 개발)
              \   /       \   / \  \
feature/atm    ●─●         \ /   \  \
feature/auth                ●     \  \
hotfix/login                       ●──●──→ main
```

## 2.2 브랜치 종류

| 브랜치 | 용도 | 생성 위치 | 병합 대상 | 보호 |
|---|---|---|---|---|
| `main` | 운영 배포 (release tag) | — | — | ✅ Protected |
| `develop` | 통합 개발 | `main` | — | ✅ Protected |
| `feature/*` | 신규 기능 개발 | `develop` | `develop` | ❌ |
| `release/*` | 릴리즈 안정화 (QA) | `develop` | `main` + `develop` | ✅ |
| `hotfix/*` | 운영 긴급 패치 | `main` | `main` + `develop` | ❌ |
| `bugfix/*` | 개발 중 버그 수정 | `develop` | `develop` | ❌ |
| `refactor/*` | 리팩터링 (기능 변경 X) | `develop` | `develop` | ❌ |

## 2.3 브랜치 네이밍 규칙

```
<type>/<jira-id>-<짧은-영문-설명>
```

**규칙**:
- 소문자 + 하이픈(`-`) 구분
- Jira 이슈번호 필수 포함 (없으면 `nojira`)
- 영문 명사형, **한글 금지**

| 좋은 예 | 나쁜 예 | 사유 |
|---|---|---|
| `feature/ATMS-101-atm-status-api` | `feature/atm` | 이슈번호 누락, 의미 불분명 |
| `fix/ATMS-203-login-token-expire` | `fix/김개발-수정` | 한글, 작성자 명시 X |
| `hotfix/ATMS-999-jwt-null-pointer` | `hotfix/긴급` | 의미 없음 |
| `refactor/ATMS-150-atm-service-split` | `refactor/refactor` | 중복 |

## 2.4 브랜치 생성 → 병합 전체 흐름

### 2.4.1 일반 기능 개발 (feature)

```bash
# 1. 최신 develop 동기화
git checkout develop
git pull origin develop

# 2. feature 브랜치 생성
git checkout -b feature/ATMS-101-atm-status-api

# 3. 개발 + 커밋 (작은 단위로)
git add .
git commit -m "feat(atm): ATM 상태 조회 API 추가"

# 4. 원격 push
git push -u origin feature/ATMS-101-atm-status-api

# 5. PR 생성: feature/ATMS-101 → develop
#    리뷰어 2명 지정, CI 통과 확인

# 6. PR 승인 후 Squash Merge → develop
#    (브랜치 자동 삭제)
```

### 2.4.2 릴리즈 흐름 (release)

```bash
# 1. develop → release 브랜치 분기 (코드 프리즈)
git checkout develop && git pull
git checkout -b release/v1.2.0
git push -u origin release/v1.2.0

# 2. QA 진행 — 버그만 release 브랜치에 직접 fix
git commit -m "fix(release): QA 발견 ATM 목록 페이징 오류"

# 3. QA 완료 → main 병합 + 태깅
git checkout main && git pull
git merge --no-ff release/v1.2.0
git tag -a v1.2.0 -m "Release v1.2.0"
git push origin main --tags

# 4. develop 역병합 (release 수정사항 반영)
git checkout develop
git merge --no-ff release/v1.2.0
git push origin develop

# 5. release 브랜치 삭제
git branch -d release/v1.2.0
git push origin --delete release/v1.2.0
```

### 2.4.3 운영 긴급 패치 (hotfix)

```bash
# 1. 운영(main)에서 분기
git checkout main && git pull
git checkout -b hotfix/ATMS-999-jwt-null-pointer

# 2. 패치 + 커밋
git commit -m "fix(auth): JWT 파싱 시 NPE 방어"

# 3. main 병합 + 태깅 (PATCH 버전)
git checkout main
git merge --no-ff hotfix/ATMS-999-jwt-null-pointer
git tag -a v1.2.1 -m "Hotfix v1.2.1"
git push origin main --tags

# 4. develop 역병합 필수 (회귀 방지)
git checkout develop
git merge --no-ff hotfix/ATMS-999-jwt-null-pointer
git push origin develop
```

## 2.5 보호 브랜치 정책 (GitLab Settings)

| 브랜치 | Push | Merge | Force Push |
|---|---|---|---|
| `main` | Maintainers 만 | PR + 2명 승인 + CI 통과 | ❌ |
| `develop` | No one (PR only) | PR + 1명 승인 + CI 통과 | ❌ |
| `release/*` | Maintainers | PR + 1명 승인 | ❌ |

---

# 3. 커밋 메시지 규칙

## 3.1 표준 — Conventional Commits

```
<type>(<scope>): <subject>          ← 1줄 요약 (필수, 50자 이내)
                                    ← 빈 줄 (필수)
<body>                              ← 변경 이유, 배경 (선택, 72자 줄바꿈)
                                    ← 빈 줄
<footer>                            ← 이슈 번호, BREAKING CHANGE (선택)
```

## 3.2 Type 정의

| Type | 의미 | 예시 |
|---|---|---|
| `feat` | 신규 기능 | `feat(atm): ATM 상태 조회 API 추가` |
| `fix` | 버그 수정 | `fix(auth): JWT 만료 시 401 응답 누락` |
| `refactor` | 리팩터링 (기능 변경 X) | `refactor(atm): 서비스 메소드 분리` |
| `perf` | 성능 개선 | `perf(journal): 거래저널 인덱스 추가` |
| `docs` | 문서 변경 | `docs(readme): 실행 방법 보강` |
| `test` | 테스트 추가/수정 | `test(atm): AtmService 단위 테스트 추가` |
| `style` | 포맷팅 (로직 변경 X) | `style(common): import 정렬` |
| `chore` | 빌드/설정/패키지 | `chore(gradle): Spring Boot 3.5.1 업그레이드` |
| `ci` | CI 설정 변경 | `ci(gitlab): 테스트 단계 추가` |
| `build` | 빌드 시스템 변경 | `build(gradle): JEUS WAR 패키징 설정` |
| `revert` | 커밋 되돌리기 | `revert: feat(atm): ATM 상태 조회 API` |

## 3.3 Scope (도메인 약어)

`auth`, `atm`, `branch`, `atmgroup`, `atmstatus`, `user`, `role`, `notice`, `code`, `journal`, `batch`, `file`, `ifin`, `ifex`, `common`, `infra`

## 3.4 Subject 작성 규칙

- 한글 작성 (팀 표준), 50자 이내
- 마침표(`.`) 금지
- 명령형 현재시제 ("추가**한다**" → "추가")
- 첫 글자 소문자 (영문일 때)

## 3.5 좋은 예 / 나쁜 예

✅ **좋은 예**

```
feat(atm): ATM 상태 조회 API 추가

- GET /api/atm-status/{termGroupId}/{termId} 신규
- 상태 코드 미존재 시 404 반환
- 거래저널 테이블과 조인하여 최근 장애 이력 포함

Resolves: ATMS-101
```

```
fix(auth): JWT 만료 시 401 응답 누락

JwtAuthFilter에서 ExpiredJwtException 발생 시
SecurityContext만 클리어하고 응답을 그대로 통과시켜
컨트롤러 단계에서 NPE가 발생하던 문제 수정.

Resolves: ATMS-203
```

❌ **나쁜 예**

| 메시지 | 문제점 |
|---|---|
| `수정함` | type 없음, scope 없음, 무엇을? |
| `feat: 작업` | subject 의미 없음 |
| `fix: AtmService.java 수정함.` | 마침표, 파일명 명시 (의미 X) |
| `WIP` | 작업 중 커밋 (PR 전에 squash) |
| `feat(atm): add atm api and fix login bug and refactor user service` | 한 커밋에 여러 작업 |
| `feat(atm): ATM API 추가했습니다.` | 존댓말, 마침표 |

## 3.6 이슈 추적 (Jira / GitLab Issue)

**Footer 키워드**:

| 키워드 | 동작 |
|---|---|
| `Resolves: ATMS-101` | 이슈 자동 종료 |
| `Refs: ATMS-101` | 이슈 참조만 (종료 X) |
| `BREAKING CHANGE:` | Major 버전업 트리거 |

**예시**:
```
feat(api): 응답 포맷 변경

기존 success 필드 제거, code 기반으로 통일.

BREAKING CHANGE: 모든 클라이언트가 응답 파싱 로직을 수정해야 함.
Resolves: ATMS-300
```

---

# 4. Pull Request 정책 및 템플릿

## 4.1 PR 생성 기준

- **1 PR = 1 작업** (Jira 이슈 1개 단위)
- **300줄 이내** 권장 (초과 시 분할)
- **로컬 빌드/테스트 통과 후** PR 생성
- **Self-review 1회 필수** (Diff 본인이 먼저 점검)
- WIP 상태는 **Draft PR**로 생성

## 4.2 리뷰 프로세스

```
[작성자]                [리뷰어]              [Maintainer]
  │                       │                       │
  ├──PR 생성 + 리뷰어 지정─→                     │
  │                       │                       │
  │                  코드 리뷰 (24h 내)           │
  │                       │                       │
  ├←──코멘트/요청 사항────┤                       │
  │                       │                       │
  ├──수정 push─────────→  │                       │
  │                       │                       │
  │                  Approve (LGTM)               │
  │                       │                       │
  │                       ├──최종 검토 + Merge──→ │
```

**리뷰 SLA**:
- 일반 PR: **24시간 이내** 첫 응답
- 긴급 hotfix: **2시간 이내**
- 리뷰어 2명 지정 (1명은 도메인 담당, 1명은 다른 파트)

## 4.3 Merge 규칙

| 대상 브랜치 | Merge 방식 | 사유 |
|---|---|---|
| `feature/*` → `develop` | **Squash Merge** | 커밋 히스토리 정리 |
| `release/*` → `main` | **Merge Commit** (`--no-ff`) | 릴리즈 추적성 보존 |
| `hotfix/*` → `main` | **Merge Commit** (`--no-ff`) | 긴급 패치 추적 |
| `release/*` → `develop` | **Merge Commit** (`--no-ff`) | 역병합 명확화 |

**Squash 시 커밋 메시지**: PR 제목을 그대로 사용 (Conventional Commits 형식 유지)

## 4.4 LGTM (승인) 기준

승인 전 리뷰어가 확인할 사항:

- [ ] 코드 컨벤션 준수
- [ ] 테스트 코드 존재 + 통과
- [ ] 비즈니스 로직 정확성
- [ ] 트랜잭션 경계 적절
- [ ] 예외 처리 + 로깅
- [ ] 보안 (SQL Injection, XSS, 권한 체크)
- [ ] 성능 (N+1, 인덱스)

## 4.5 PR 템플릿

> 아래 템플릿을 `1.Project/.gitlab/merge_request_templates/default.md` 로 저장

```markdown
## 📌 작업 내용
<!-- Jira 이슈 링크 + 한 줄 요약 -->
- Jira: [ATMS-XXX](https://jira.example.com/browse/ATMS-XXX)
- 요약: 

## 🛠 변경 사항
<!-- 변경된 주요 파일/모듈을 bullet로 -->
- [ ] 
- [ ] 

## ✅ 테스트 내용
<!-- 어떻게 검증했는지 명시 -->
- [ ] 단위 테스트: 
- [ ] 통합 테스트: 
- [ ] 수동 테스트 (Swagger/UI):

## 📷 스크린샷 (UI 변경 시)
<!-- Before / After -->

## ⚠️ 영향도 분석
<!-- 다른 모듈, DB, 외부 시스템 영향 -->
- DB 변경: [ ] 있음 / [x] 없음
- API 스펙 변경: [ ] 있음 / [x] 없음
- 운영 설정 변경: [ ] 있음 / [x] 없음
- BREAKING CHANGE: [ ] 있음 / [x] 없음

## 📋 체크리스트
- [ ] 코딩 컨벤션 준수
- [ ] 단위 테스트 작성 + 통과
- [ ] 커버리지 70% 이상
- [ ] 로컬 빌드 성공 (`./gradlew build`)
- [ ] Lint / Type 검사 통과 (`npm run lint`)
- [ ] 보안 항목 점검 (SQL Injection, 권한, 로깅)
- [ ] 운영 영향 검토
- [ ] 관련 문서 업데이트 (필요 시)

## 🔍 리뷰 포인트
<!-- 리뷰어가 특히 봐줬으면 하는 부분 -->

## 🧑‍💻 리뷰어
- @reviewer1 (도메인)
- @reviewer2 (크로스파트)
```

---

# 5. 개발 워크플로우

## 5.1 전체 흐름

```
[1] Jira 이슈 생성 (PO/PL)
        ↓
[2] 이슈 Assign + 분석
        ↓
[3] 브랜치 생성 (feature/ATMS-XXX-...)
        ↓
[4] 로컬 개발 + 커밋 (Conventional)
        ↓
[5] 단위 테스트 작성 + 통과
        ↓
[6] Push + Draft PR 생성
        ↓
[7] CI 통과 확인 (빌드 + 테스트 + Lint)
        ↓
[8] Ready for review (리뷰어 지정)
        ↓
[9] 코드 리뷰 + 수정
        ↓
[10] Approve (LGTM)
        ↓
[11] Squash Merge → develop
        ↓
[12] Jira 이슈 자동 종료 (Resolves: 키워드)
        ↓
[13] develop 자동 배포 (DEV 환경)
```

## 5.2 작업 단위 쪼개는 기준

**Task 분할 원칙** — INVEST

| 항목 | 의미 |
|---|---|
| **I**ndependent | 다른 task와 독립 가능 |
| **N**egotiable | 협상 가능한 수준 |
| **V**aluable | 사용자/시스템에 가치 |
| **E**stimable | 산정 가능 |
| **S**mall | 작은 단위 (**3일 이내**) |
| **T**estable | 테스트 가능 |

**예시 — "ATM 관리 기능"**

❌ 너무 큼:
- ATMS-100: ATM 관리 기능 (10일)

✅ 적정 분할:
- ATMS-101: ATM 등록 API (1.5일)
- ATMS-102: ATM 수정 API (1일)
- ATMS-103: ATM 삭제 API (0.5일)
- ATMS-104: ATM 목록 조회 API + 페이징 (1일)
- ATMS-105: ATM 단건 조회 API (0.5일)
- ATMS-106: ATM 등록/수정 화면 (2일)
- ATMS-107: ATM 목록 화면 (1.5일)
- ATMS-108: ATM 통합 테스트 (1일)

## 5.3 Definition of Done (완료 기준)

PR 머지 = 완료가 아닙니다. **DoD 모두 충족 시** Jira "Done" 처리:

### Backend Task DoD
- [ ] 코드 작성 + 컨벤션 준수
- [ ] 단위 테스트 작성 (커버리지 70%+)
- [ ] Swagger 문서화 완료
- [ ] PR 머지 완료 (리뷰 승인)
- [ ] DEV 환경 배포 + 동작 확인
- [ ] API 스펙 문서 업데이트 (`02_API스펙정의.md`)

### Frontend Task DoD
- [ ] 컴포넌트 구현 + 컨벤션 준수
- [ ] TypeScript 컴파일 에러 0
- [ ] ESLint 경고 0
- [ ] 반응형 확인 (1920×1080 / 1366×768)
- [ ] 크로스 브라우저 (Chrome / Edge)
- [ ] PR 머지 완료
- [ ] DEV 환경 배포 + 시연 가능

### 공통 DoD (전체 기능)
- [ ] 단위 테스트 + 통합 테스트 통과
- [ ] QA 시나리오 작성 + 통과
- [ ] 운영 가이드 / 사용자 매뉴얼 반영
- [ ] PO/PL 승인

---

# 6. WBS 및 진척도 관리

## 6.1 NH-ATMS WBS 구조 (3-Level)

```
[L1: 영역]    [L2: 모듈]           [L3: Task]
─────────────────────────────────────────────────────
1. 기반     1.1 인프라 구축       1.1.1 RHEL 환경 구성
                                  1.1.2 JEUS 9.1 설치
                                  1.1.3 WebtoB 5 설정
            1.2 프로젝트 셋업     1.2.1 Gradle 멀티모듈 구성
                                  1.2.2 Spring Boot 3.5 셋업
                                  1.2.3 React + Vite 셋업
            1.3 공통 모듈         1.3.1 JWT 인증
                                  1.3.2 ApiResponse 표준
                                  1.3.3 GlobalExceptionHandler
                                  1.3.4 감사 로깅 AOP

2. 인증     2.1 SSO 연동          2.1.1 Mock SSO (개발용)
                                  2.1.2 농협 SSO 어댑터
            2.2 사용자/권한       2.2.1 사용자 CRUD API
                                  2.2.2 역할/권한 관리

3. ATM      3.1 기기 관리         3.1.1 ATM 정보 CRUD
                                  3.1.2 ATM 그룹 관리
                                  3.1.3 영업점 관리
            3.2 상태 모니터링     3.2.1 실시간 상태 조회
                                  3.2.2 상태 변경 이벤트
                                  3.2.3 대시보드

4. 거래     4.1 거래저널          4.1.1 거래내역 수집
                                  4.1.2 저널 조회 API
                                  4.1.3 통계/집계 배치

5. 인터페이스 5.1 내부 IF        5.1.1 ATM Agent ↔ 서버
                                  5.1.2 EAI 메시지 처리
              5.2 외부 IF        5.2.1 대외기관 연동
                                  5.2.2 암호화 처리

6. 운영     6.1 공지/코드         6.1.1 공지사항
                                  6.1.2 공통코드
            6.2 파일/배치         6.2.1 파일 업/다운로드
                                  6.2.2 배치 스케줄러

7. 테스트   7.1 단위/통합         7.1.1 백엔드 단위 테스트
            7.2 시스템            7.2.1 E2E 시나리오
            7.3 성능              7.3.1 부하 테스트

8. 이행     8.1 데이터 마이그레이션 8.1.1 MySQL → Oracle 변환
            8.2 운영 이관          8.2.1 운영 배포
```

## 6.2 일정 산정 기준

**MD (Man-Day) 기준** — 1MD = 1인 8시간 순수 개발 시간

| 난이도 | 설명 | 기준 |
|---|---|---|
| **S** (Simple) | CRUD, 단순 화면 | 0.5 ~ 1 MD |
| **M** (Medium) | 비즈니스 로직 포함, 외부 호출 | 1 ~ 3 MD |
| **L** (Large) | 복잡한 도메인, 트랜잭션 다수 | 3 ~ 5 MD |
| **XL** (Extra Large) | **분할 권장** (최대 5MD) | — |

**산정 시 고려 항목**:
- 개발 (60%) + 테스트 (20%) + 리뷰/수정 (10%) + 문서 (10%)
- 신규 기술 학습 시 **+30%** 버퍼

**예시**:
```
Task: ATMS-101 ATM 등록 API
- 분석/설계: 0.5MD
- Entity/Mapper/Service/Controller: 0.5MD
- 단위 테스트: 0.3MD
- Swagger 문서: 0.1MD
- PR 리뷰 대응: 0.1MD
─────────────────────
합계: 1.5MD (난이도 M)
```

## 6.3 진척 관리 방식

### 6.3.1 일일 (Daily Stand-up) — 15분

**3가지 질문**:
1. 어제 한 일 (Done)
2. 오늘 할 일 (Doing)
3. 블로커 (Impediment)

**Jira 보드 운영**:
```
Backlog → To Do → In Progress → In Review → Done
                       │            │
                  최대 2개         WIP 제한
```

### 6.3.2 주간 (Weekly Report)

매주 금요일 17:00, **WBS 진척률** 보고:

| 영역 | 계획 MD | 실적 MD | 진척률 | 이슈 |
|---|---|---|---|---|
| 1. 기반 | 20 | 18 | 90% | — |
| 2. 인증 | 15 | 15 | 100% | ✅ 완료 |
| 3. ATM | 30 | 12 | 40% | 외부 IF 스펙 지연 |

### 6.3.3 리포팅 도구

| 도구 | 용도 |
|---|---|
| Jira Burndown | Sprint 잔여 작업 추적 |
| Jira Velocity | 스프린트별 처리량 |
| GitLab Insights | 머지 추이, 리뷰 대기 시간 |
| Confluence | 주간 보고서 / 회의록 |

---

# 7. 프로젝트 구조 및 모듈 규칙

## 7.1 멀티모듈 설계

```
nh-atms-api/                       (root project)
├── settings.gradle
├── build.gradle                   (공통 dependencies, plugin 정의)
│
├── atms-common/                   ★ 공통 라이브러리
│   └── build.gradle               (모든 모듈이 의존)
│
├── atms-web/                      운영 화면 REST API (★ 메인)
├── atms-journal/                  거래저널
├── atms-batch/                    배치
├── atms-file/                     파일 처리
├── atms-if-internal/              내부 IF
└── atms-if-external/              외부 IF
```

## 7.2 모듈 의존성 규칙

```
            ┌──────────────┐
            │ atms-common  │  ← 모든 모듈이 의존 (공통)
            └──────┬───────┘
                   │
   ┌────────┬──────┼─────────┬────────┐
   ↓        ↓      ↓         ↓        ↓
 web    journal  batch     file   if-internal  if-external
   │
   └→ (의존 X) — 모듈 간 직접 호출 금지
```

**철칙**:
1. **`atms-common`만 모든 모듈이 의존** 가능
2. **도메인 모듈끼리 직접 의존 금지** — 통신은 REST/Message로
3. **순환 참조 절대 금지** — Gradle이 빌드 시 차단
4. 모듈 간 데이터 공유는 **공통 DTO** (atms-common) 통해서만

**위반 예시**:
```groovy
// ❌ 금지
// atms-web/build.gradle
dependencies {
    implementation project(':atms-journal')  // 도메인 직접 참조
}

// ✅ 허용
dependencies {
    implementation project(':atms-common')   // common만
}
```

## 7.3 도메인 분리 기준

**도메인 = 독립적으로 변경 가능한 비즈니스 단위**

| 모듈 | 책임 | 분리 사유 |
|---|---|---|
| `atms-web` | 사용자 화면 API | UI 변경 빈도 높음 |
| `atms-journal` | 거래저널 (대용량) | 별도 DB 파티셔닝 |
| `atms-batch` | 배치 (집계, 정산) | 스케줄러 독립 운영 |
| `atms-file` | 파일 (POI Excel) | 무거운 의존성 격리 |
| `atms-if-internal` | ATM ↔ 서버 IF | 프로토콜 특화 |
| `atms-if-external` | 대외 기관 연동 | 보안/암호화 격리 |

## 7.4 패키지 구조 표준

```
kr.co.nh.atms.{module}.{domain}.{layer}

예시:
kr.co.nh.atms.web.atm.AtmController        (web 모듈, atm 도메인)
kr.co.nh.atms.journal.tran.TranService     (journal 모듈, tran 도메인)
kr.co.nh.atms.common.exception.BusinessException
```

---

# 8. 환경 및 설정 관리

## 8.1 Profile 전략

| Profile | 용도 | DB | 외부 시스템 |
|---|---|---|---|
| `local` | 개발자 PC | H2 (인메모리) | Mock |
| `dev` | 개발 서버 | Oracle (DEV) | Mock / Stub |
| `stg` | 검증 (UAT) | Oracle (STG) | 실제 (테스트 계정) |
| `prod` | 운영 | Oracle (PROD) | 실제 |

**활성화**:
```bash
# JVM 옵션
java -Dspring.profiles.active=stg -jar atms-web.jar

# IntelliJ Run Configuration
Active profiles: dev
```

## 8.2 설정 파일 구조

```
atms-web/src/main/resources/
├── application.yml              # 공통 (모든 profile)
├── application-local.yml        # 로컬 (H2)
├── application-dev.yml          # 개발 서버
├── application-stg.yml          # 검증
├── application-prod.yml         # 운영 (배포 시 외부 주입)
├── logback-spring.xml           # 로그 설정 (profile별)
├── mybatis-config.xml
└── mappers/
    └── *.xml
```

**`application.yml` 예시 (공통)**:
```yaml
spring:
  application:
    name: atms-web
  profiles:
    active: local
  jackson:
    time-zone: Asia/Seoul
    date-format: yyyy-MM-dd HH:mm:ss

server:
  port: 8080
  servlet:
    context-path: /

springdoc:
  swagger-ui:
    path: /swagger-ui/index.html
```

**`application-prod.yml` 예시 (운영)**:
```yaml
spring:
  datasource:
    url: ${DB_URL}                            # 환경변수에서 주입
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000

jwt:
  secret: ${JWT_SECRET}                       # ★ 절대 yml에 평문 X
  access-token-expire: 1800000                # 30분
  refresh-token-expire: 86400000              # 24시간

logging:
  level:
    root: INFO
    kr.co.nh.atms: INFO
  file:
    name: /logs/atms/atms-web.log
```

## 8.3 환경 변수 / Secrets 관리

**원칙**:
- 운영 환경 모든 비밀값은 **OS 환경변수** 또는 **Vault**로 주입
- `application-prod.yml`에 평문 비밀값 절대 금지 (`git secret-scan` 검사)
- 개발자 로컬은 IntelliJ Run Configuration의 Environment variables에 설정

**Secrets 분류**:

| 항목 | 저장 위치 |
|---|---|
| DB 패스워드 | OS 환경변수 (운영) / IntelliJ env (로컬) |
| JWT secret | OS 환경변수 (운영) |
| 외부 API 키 | OS 환경변수 |
| 암호화 키 | HSM 또는 Vault |

**`.gitignore` 필수 항목**:
```
*.key
*.pem
application-prod.yml      # 운영 설정은 git 관리 X
.env
.env.local
```

## 8.4 DB 마이그레이션 전략 (MySQL → Oracle)

### 8.4.1 AS-IS 분석

기존 새마을금고 ATMS DB(MySQL) → NH-ATMS Oracle 19C 이행.
관련 분석 문서: [00_DB분석_모듈매핑.md](00_DB분석_모듈매핑.md)

### 8.4.2 마이그레이션 단계

```
[1] AS-IS 스키마 추출 (MySQL DDL)
       ↓
[2] 매핑 테이블 작성 (테이블/컬럼 한글명, 타입 변환)
       ↓
[3] Oracle DDL 생성
       ↓
[4] 차이점 보정 스크립트 작성
       ↓
[5] 데이터 추출 → 변환 → 적재 (ETL)
       ↓
[6] 검증 (건수, 합계, 샘플링)
       ↓
[7] 운영 이관
```

### 8.4.3 타입 매핑 표

| MySQL | Oracle | 비고 |
|---|---|---|
| `INT` | `NUMBER(10)` | |
| `BIGINT` | `NUMBER(19)` | |
| `VARCHAR(n)` | `VARCHAR2(n CHAR)` | **CHAR 단위 명시** (한글) |
| `TEXT` | `CLOB` | |
| `DATETIME` | `TIMESTAMP` | |
| `DATE` | `DATE` | Oracle DATE는 시각 포함 |
| `BOOLEAN` (TINYINT) | `NUMBER(1)` | 0/1 |
| `AUTO_INCREMENT` | `SEQUENCE` + Trigger | 또는 `IDENTITY` (12c+) |

### 8.4.4 SQL 호환성 주의사항

| MySQL | Oracle |
|---|---|
| `LIMIT 10` | `FETCH FIRST 10 ROWS ONLY` |
| `IFNULL(a, b)` | `NVL(a, b)` |
| `NOW()` | `SYSDATE` / `SYSTIMESTAMP` |
| `CONCAT(a, b)` | `a \|\| b` |
| Backtick `` `col` `` | Double quote `"col"` |
| `GROUP_CONCAT` | `LISTAGG` |

### 8.4.5 MyBatis 호환 작성

```xml
<!-- ❌ MySQL 전용 -->
<select id="selectAtmList">
  SELECT * FROM tb_atm
  WHERE reg_dt > NOW() - INTERVAL 7 DAY
  LIMIT #{offset}, #{size}
</select>

<!-- ✅ Oracle 호환 (또는 양쪽 동작) -->
<select id="selectAtmList">
  SELECT *
    FROM tb_atm
   WHERE reg_dt > SYSDATE - 7
   ORDER BY reg_dt DESC
   OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY
</select>
```

### 8.4.6 마이그레이션 도구

| 도구 | 용도 |
|---|---|
| **Oracle SQL Developer** (Migration Workbench) | DDL 자동 변환 |
| **Flyway** | 버전 관리 (`V1__init.sql`, `V2__add_atm.sql`) |
| **DataPump (expdp/impdp)** | 대용량 이행 |

---

# 9. 테스트 및 품질 전략

## 9.1 테스트 피라미드

```
        ┌──────┐
        │  E2E │  10%   — Cypress / 수동 시나리오
        ├──────┤
        │ 통합 │  20%   — @SpringBootTest, REST Assured
        ├──────┤
        │ 단위 │  70%   — JUnit5 + Mockito
        └──────┘
```

## 9.2 단위 테스트 (Unit Test)

**대상**: Service, Util, Validator (비즈니스 로직)

**Mock 전략**:

| 의존 객체 | 처리 |
|---|---|
| Mapper (DB) | `@Mock` (Mockito) |
| 외부 API | `@Mock` |
| 다른 Service | `@Mock` (단위) / 실제 (통합) |

**예시**:
```java
@ExtendWith(MockitoExtension.class)
class AtmServiceTest {

    @Mock
    private AtmMapper atmMapper;

    @InjectMocks
    private AtmService atmService;

    @Test
    @DisplayName("ATM 단건 조회 - 존재할 경우 정상 반환")
    void findById_success() {
        // given
        Atm atm = new Atm();
        atm.setTermGroupId("G01");
        atm.setTermId("T001");
        when(atmMapper.selectById("G01", "T001")).thenReturn(atm);

        // when
        AtmResponse result = atmService.findById("G01", "T001");

        // then
        assertThat(result.getTermId()).isEqualTo("T001");
        verify(atmMapper).selectById("G01", "T001");
    }

    @Test
    @DisplayName("ATM 단건 조회 - 없을 경우 BusinessException")
    void findById_notFound() {
        when(atmMapper.selectById(any(), any())).thenReturn(null);

        assertThatThrownBy(() -> atmService.findById("G01", "T001"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("찾을 수 없습니다");
    }
}
```

## 9.3 통합 테스트 (Integration Test)

**대상**: Controller → Service → Mapper → DB 전체 흐름

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AtmIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin")
    void atm_등록_조회_통합() throws Exception {
        String body = """
            {"termGroupId":"G01","termId":"T999","termName":"테스트ATM"}
            """;

        mockMvc.perform(post("/api/atm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/atm/G01/T999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.termName").value("테스트ATM"));
    }
}
```

## 9.4 커버리지 기준

| 계층 | 목표 커버리지 |
|---|---|
| Service (비즈니스 로직) | **80% 이상** |
| Controller | 60% 이상 (입력 검증 위주) |
| Mapper | 통합 테스트로 검증 |
| Util | 90% 이상 |
| **전체** | **70% 이상** |

**측정 도구**: JaCoCo

```groovy
// build.gradle
plugins {
    id 'jacoco'
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.70
            }
        }
    }
}
```

## 9.5 Frontend 테스트

| 도구 | 용도 |
|---|---|
| **Vitest** | 단위 테스트 |
| **React Testing Library** | 컴포넌트 테스트 |
| **MSW** | API Mock |
| **Cypress** / Playwright | E2E |

**예시**:
```ts
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import LoginPage from './LoginPage';

describe('LoginPage', () => {
  it('로그인 폼이 렌더링된다', () => {
    render(<LoginPage />);
    expect(screen.getByLabelText('아이디')).toBeInTheDocument();
    expect(screen.getByLabelText('비밀번호')).toBeInTheDocument();
  });
});
```

## 9.6 품질 게이트 (CI Pipeline)

```yaml
# .gitlab-ci.yml (개념)
stages:
  - build
  - test
  - quality
  - deploy

backend-test:
  stage: test
  script:
    - ./gradlew test jacocoTestReport jacocoTestCoverageVerification
  coverage: '/Total.*?([0-9]{1,3})%/'

frontend-test:
  stage: test
  script:
    - npm ci
    - npm run lint
    - npm run typecheck
    - npm run test

sonar-scan:
  stage: quality
  script:
    - sonar-scanner
  only: [merge_requests]
```

**Merge 차단 조건** (Quality Gate):
- 테스트 실패 1건 이상
- 커버리지 70% 미만
- SonarQube Critical / Blocker 이슈 1건 이상
- 보안 취약점 (OWASP Top 10) 검출

---

## 부록 A. 참고 문서

| 문서 | 위치 |
|---|---|
| 코딩 컨벤션 (요약) | [Document/01_코딩컨벤션.md](01_코딩컨벤션.md) |
| API 스펙 정의 | [Document/02_API스펙정의.md](02_API스펙정의.md) |
| DB 분석/모듈 매핑 | [Document/00_DB분석_모듈매핑.md](00_DB분석_모듈매핑.md) |
| 모듈 분석 / 마이그레이션 | [Document/01_모듈분석_마이그레이션가이드.md](01_모듈분석_마이그레이션가이드.md) |
| Backend README | [Backend/nh-atms-api/atms-web/README.md](../Backend/nh-atms-api/atms-web/README.md) |
| 프로젝트 README | [README.md](../README.md) |

## 부록 B. 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|---|---|---|---|
| v1.0 | 2026-05-15 | Tech Lead | 최초 작성 |
