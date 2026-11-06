# NH-ATMS 코딩 컨벤션

> 최종 수정: 2026-04-01  
> 프리랜서 투입 전 반드시 숙지

---

## 1. 공통 규칙

| 항목     | 규칙                      |
| ------ | ----------------------- |
| 인코딩    | UTF-8                   |
| 들여쓰기   | 4 spaces (탭 금지)         |
| 줄 끝 공백 | 금지                      |
| 파일 끝   | 빈 줄 1개                  |
| 주석     | 한글 허용, TODO/FIXME 태그 사용 |

---

## 2. Backend (Java / Spring Boot)

### 2.1 기술 스택

- Java 21, Spring Boot 3.5.0
- MyBatis 3.0.4
- Spring Security + JWT (jjwt 0.12.6)
- Springdoc OpenAPI 2.8.4 (Swagger)
- Lombok
- Gradle

### 2.2 패키지 구조

```
atms/
├── NhAtmsApiApplication.java        # 메인
├── config/                           # 설정 (Security, CORS, Swagger 등)
│   └── SecurityConfig.java
├── common/                           # 공통
│   ├── exception/                    # 예외 처리
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   └── response/                     # 공통 응답
│       └── ApiResponse.java
├── auth/                             # 인증 모듈 (완료)
│   ├── controller/
│   ├── dto/
│   ├── jwt/
│   ├── service/
│   └── sso/
├── {도메인명}/                       # ← 비즈니스 도메인별 패키지
│   ├── controller/                   # REST Controller
│   │   └── {도메인}Controller.java
│   ├── dto/                          # 요청/응답 DTO
│   │   ├── {도메인}Request.java
│   │   └── {도메인}Response.java
│   ├── service/                      # 비즈니스 로직
│   │   └── {도메인}Service.java
│   └── mapper/                       # MyBatis Mapper 인터페이스
│       └── {도메인}Mapper.java
└── logging/                          # AOP 로깅
    └── ApiLoggingAspect.java
```

**도메인 예시:**

- `transaction` - 거래내역
- `closing` - 마감처리
- `device` - 기기관리
- `fault` - 장애관리
- `cash` - 시재관리
- `journal` - 저널관리
- `stats` - 통계
- `system` - 시스템관리 (사용자, 권한, 메뉴, 공통코드)

### 2.3 MyBatis Mapper XML 위치

```
src/main/resources/
├── application.yml
└── mapper/
    ├── transaction/
    │   └── TransactionMapper.xml
    ├── device/
    │   └── DeviceMapper.xml
    └── {도메인}/
        └── {도메인}Mapper.xml
```

### 2.4 네이밍 규칙

| 대상     | 규칙               | 예시                                                    |
| ------ | ---------------- | ----------------------------------------------------- |
| 패키지    | 소문자, 단수형         | `atms.transaction.controller`                         |
| 클래스    | PascalCase       | `TransactionController`, `DeviceService`              |
| 메서드    | camelCase        | `getTransactionList()`, `updateDeviceStatus()`        |
| 변수     | camelCase        | `deviceId`, `branchCode`                              |
| 상수     | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE`, `DEFAULT_TIMEOUT`                    |
| DTO    | 용도 명시            | `TransactionSearchRequest`, `TransactionListResponse` |
| Mapper | 도메인 + Mapper     | `TransactionMapper.java`, `TransactionMapper.xml`     |

### 2.5 Controller 작성 규칙

```java
@RestController
@RequestMapping("/api/{도메인}")
@RequiredArgsConstructor
@Tag(name = "{도메인} API", description = "{도메인} 관련 API")
public class {도메인}Controller {

    private final {도메인}Service {도메인}Service;

    @GetMapping("/list")
    @Operation(summary = "목록 조회")
    public ResponseEntity<ApiResponse<List<{Response}>>> getList({Request} request) {
        return ResponseEntity.ok(ApiResponse.ok(service.getList(request)));
    }
}
```

**규칙:**

- 모든 Controller는 `@RestController` + `@RequestMapping("/api/{도메인}")` 사용
- 반환 타입은 반드시 `ResponseEntity<ApiResponse<T>>`
- Swagger 어노테이션 필수: `@Tag`, `@Operation`
- 비즈니스 로직은 Service에 위임 (Controller에 로직 금지)

### 2.6 API URL 규칙

| HTTP Method | 용도    | URL 패턴                | 예시                          |
| ----------- | ----- | --------------------- | --------------------------- |
| GET         | 목록 조회 | `/api/{도메인}/list`     | `GET /api/transaction/list` |
| GET         | 단건 조회 | `/api/{도메인}/{id}`     | `GET /api/device/ATM001`    |
| POST        | 등록    | `/api/{도메인}`          | `POST /api/fault`           |
| PUT         | 수정    | `/api/{도메인}/{id}`     | `PUT /api/device/ATM001`    |
| DELETE      | 삭제    | `/api/{도메인}/{id}`     | `DELETE /api/notice/123`    |
| POST        | 특수 기능 | `/api/{도메인}/{action}` | `POST /api/closing/execute` |

**URL 네이밍:**

- 소문자, 하이픈(`-`) 사용 (camelCase 금지)
- 복수형 대신 단수형 사용 (`/api/transaction` O, `/api/transactions` X)
- 동사 대신 명사 사용 (`/api/fault/list` O, `/api/getFaults` X)

### 2.7 DTO 규칙

```java
// 요청 DTO (검증 포함)
@Getter
@Setter
public class TransactionSearchRequest {
    @NotBlank(message = "조회 시작일은 필수입니다")
    private String startDate;

    @NotBlank(message = "조회 종료일은 필수입니다") 
    private String endDate;

    private String deviceId;      // 선택 조건
    private String branchCode;    // 선택 조건
}

// 응답 DTO (불변)
@Getter
@Builder
@AllArgsConstructor
public class TransactionListResponse {
    private String transactionId;
    private String deviceId;
    private String transactionDate;
    private Long amount;
}
```

**규칙:**

- 요청 DTO: `@Getter` + `@Setter` + `@Valid` 검증
- 응답 DTO: `@Getter` + `@Builder` + `@AllArgsConstructor` (불변)
- Entity 클래스 직접 반환 금지 → 반드시 DTO로 변환

### 2.8 Service 규칙

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionMapper transactionMapper;

    public List<TransactionListResponse> getList(TransactionSearchRequest request) {
        List<Transaction> list = transactionMapper.selectTransactionList(request);
        return list.stream()
            .map(this::toResponse)
            .toList();
    }
}
```

**규칙:**

- `@RequiredArgsConstructor` + `final` 필드로 DI
- `@Autowired` 사용 금지
- 예외는 `BusinessException` 사용: `throw new BusinessException("TRANS_001", "거래를 찾을 수 없습니다.")`
- 에러 코드: `{도메인약어}_{번호}` (예: `AUTH_001`, `TRANS_001`, `DEV_001`)

### 2.9 공통 응답 포맷

모든 API는 다음 포맷으로 응답:

```json
{
    "success": true,
    "code": "200",
    "message": "성공",
    "data": { ... }
}
```

실패 시:

```json
{
    "success": false,
    "code": "TRANS_001",
    "message": "거래를 찾을 수 없습니다.",
    "data": null
}
```

---

## 3. Frontend (React + TypeScript)

### 3.1 기술 스택

- React 19, TypeScript 5.9
- Vite 7.3
- Ant Design 6.3
- Zustand 5.0 (상태관리)
- React Router 7.13
- Axios + React Query
- dayjs (날짜)

### 3.2 폴더 구조

```
src/
├── App.tsx                           # 앱 루트
├── main.tsx                          # 엔트리
├── index.css                         # 글로벌 스타일
├── api/                              # API 호출
│   └── axiosInstance.ts              # Axios 설정 (토큰 자동 첨부/갱신)
├── components/                       # 공통 컴포넌트
│   ├── DataTable.tsx                 # 공통 테이블
│   ├── SearchForm.tsx                # 공통 조회조건 폼
│   └── PageTemplate.tsx              # 페이지 기본 템플릿
├── layouts/                          # 레이아웃
│   ├── AppLayout.tsx
│   ├── AppHeader.tsx
│   ├── AppSidebar.tsx
│   └── AppTabs.tsx
├── pages/                            # 페이지 (도메인별 폴더)
│   ├── {도메인}/
│   │   └── {화면명}Page.tsx
│   ├── dashboard/
│   │   └── DashboardPage.tsx
│   ├── login/
│   │   └── LoginPage.tsx
│   ├── transaction/                  # 거래내역
│   │   └── TransactionSpecificPage.tsx
│   └── cash/                         # 시재
│       └── CashInquiryPage.tsx
├── routes/                           # 라우팅
│   └── AppRouter.tsx
├── stores/                           # Zustand 상태관리
│   ├── useAuthStore.ts
│   └── useTabStore.ts
├── types/                            # 타입 정의
│   ├── menu.ts
│   └── search.ts
└── utils/                            # 유틸리티
    └── excelDownload.ts
```

### 3.3 네이밍 규칙

| 대상      | 규칙                | 예시                            |
| ------- | ----------------- | ----------------------------- |
| 폴더      | camelCase, 도메인명   | `transaction/`, `cash/`       |
| 컴포넌트 파일 | PascalCase + .tsx | `TransactionSpecificPage.tsx` |
| 페이지     | {기능}Page.tsx      | `CashInquiryPage.tsx`         |
| Hook    | use{기능}.ts        | `useAuthStore.ts`             |
| 유틸      | camelCase.ts      | `excelDownload.ts`            |
| 타입      | camelCase.ts      | `search.ts`, `menu.ts`        |
| 변수/함수   | camelCase         | `handleSearch`, `deviceList`  |
| 컴포넌트    | PascalCase        | `DataTable`, `SearchForm`     |
| 상수      | UPPER_SNAKE_CASE  | `API_BASE_URL`                |
| 인터페이스   | PascalCase        | `SearchField`, `TabItem`      |

### 3.4 페이지 작성 패턴

모든 업무 화면은 아래 패턴을 따름:

```tsx
import React, { useState } from 'react';
import PageTemplate from '@/components/PageTemplate';
import SearchForm from '@/components/SearchForm';
import DataTable from '@/components/DataTable';
import type { SearchField } from '@/types/search';
import type { ColumnsType } from 'antd/es/table';
import api from '@/api/axiosInstance';
import { downloadExcel } from '@/utils/excelDownload';

// 1. 조회조건 필드 정의
const searchFields: SearchField[] = [
    { name: 'startDate', label: '시작일', type: 'datePicker' },
    { name: 'endDate', label: '종료일', type: 'datePicker' },
    { name: 'deviceId', label: '기기번호', type: 'input', placeholder: '기기번호 입력' },
];

// 2. 테이블 컬럼 정의
const columns: ColumnsType<SomeType> = [
    { title: 'No', dataIndex: 'no', width: 60 },
    { title: '기기번호', dataIndex: 'deviceId', width: 120 },
    // ...
];

const SomePage: React.FC = () => {
    const [data, setData] = useState<SomeType[]>([]);
    const [loading, setLoading] = useState(false);
    const [searched, setSearched] = useState(false);

    // 3. 조회
    const handleSearch = async (values: Record<string, unknown>) => {
        setLoading(true);
        try {
            const res = await api.get('/도메인/list', { params: values });
            setData(res.data.data);
            setSearched(true);
        } finally {
            setLoading(false);
        }
    };

    // 4. 엑셀
    const handleExcel = () => {
        downloadExcel(columns as any, data, '파일명');
    };

    return (
        <PageTemplate title="화면 제목">
            <SearchForm fields={searchFields} onSearch={handleSearch} onExcel={handleExcel} loading={loading} />
            <DataTable columns={columns} dataSource={data} loading={loading} visible={searched} />
        </PageTemplate>
    );
};

export default SomePage;
```

### 3.5 API 호출 규칙

- 모든 API 호출은 `@/api/axiosInstance`의 `api` 인스턴스 사용
- `axios` 직접 import 금지 (토큰 자동 첨부/갱신이 안 됨)
- baseURL이 `/api`로 설정되어 있으므로 경로에 `/api` 중복 금지

```tsx
// ✅ 올바른 사용
import api from '@/api/axiosInstance';
const res = await api.get('/transaction/list', { params });

// ❌ 잘못된 사용
import axios from 'axios';
const res = await axios.get('/api/transaction/list');
```

### 3.6 Import 경로 규칙

- 절대 경로 `@/` 사용 (vite alias 설정됨)
- 상대 경로 `../../` 사용 금지

```tsx
// ✅ 올바른 사용
import api from '@/api/axiosInstance';
import DataTable from '@/components/DataTable';

// ❌ 잘못된 사용
import api from '../../api/axiosInstance';
```

---

## 4. Git 규칙

### 4.1 커밋 메시지

```
{타입}: {설명}

feat: 거래내역 조회 API 구현
fix: 시재조회 날짜 필터 오류 수정
refactor: 공통 응답 포맷 변경
docs: API 스펙 문서 업데이트
style: 코드 포맷팅
```

| 타입       | 설명             |
| -------- | -------------- |
| feat     | 새 기능           |
| fix      | 버그 수정          |
| refactor | 리팩토링 (기능 변경 X) |
| docs     | 문서             |
| style    | 포맷팅, 세미콜론 등    |
| test     | 테스트            |
| chore    | 빌드, 설정 변경      |

### 4.2 브랜치 전략

```
main          ← 배포용
  └── develop ← 통합 개발
       ├── feature/{도메인}-{기능}   ← 기능 개발
       └── fix/{이슈}               ← 버그 수정
```

예시: `feature/transaction-list`, `feature/fault-management`, `fix/login-token-expire`

---

## 5. 기타 규칙

### 5.1 Swagger 문서화

- 모든 API에 `@Tag`, `@Operation`, `@Parameter` 어노테이션 필수
- Swagger UI 접속: `http://localhost:8085/swagger-ui.html`

### 5.2 로깅

- Controller/Service에서 `@Slf4j` 사용
- `log.info()` - 정상 흐름 (로그인, 조회 등)
- `log.warn()` - 예외 상황 (인증 실패 등)
- `log.error()` - 시스템 오류
- 개인정보(비밀번호 등) 로그 출력 금지

### 5.3 보안

- SQL Injection 방지: MyBatis에서 `${}` 사용 금지, `#{}` 사용
- XSS 방지: 입력값 검증 필수
- 인증 필수: `/api/auth/login`, `/api/auth/refresh` 외 모든 API는 JWT 필수
