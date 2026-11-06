# atms-web

NH-ATMS 시스템 — **운영 화면용 Web API 모듈**.
기기 관리, 사용자/권한, 공지사항, 공통코드 등 8개 핵심 도메인의 REST API를 제공한다.

## 1. 개요

| 항목 | 값 |
|---|---|
| Spring Boot | 3.5.0 |
| Java | 21 |
| DB | Oracle 19C (운영) / H2 (dev) |
| ORM | MyBatis 3.0.4 |
| 인증 | JWT (HS256) |
| API 문서 | springdoc-openapi 2.8.4 |
| 빌드 | Gradle (multi-module) |

기반 라이브러리(`ApiResponse`, `BusinessException`, `PageRequest`, `SecurityConfig`, `JwtAuthenticationFilter` 등)는 [atms-common](../atms-common)에서 제공.

## 2. 실행

```bash
# 빌드
./gradlew :atms-web:bootJar

# 실행 (dev 프로파일 = H2)
java -jar atms-web/build/libs/atms-web-0.0.1-SNAPSHOT.jar

# 또는 직접 실행
./gradlew :atms-web:bootRun
```

기본 포트: **8080**.

| URL | 설명 |
|---|---|
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI 3.0 spec (JSON) |
| `http://localhost:8080/h2-console` | H2 콘솔 (dev 전용, JDBC: `jdbc:h2:mem:atmsdb`) |

## 3. 도메인 / 엔드포인트

모든 엔드포인트는 `/api/auth/login`, `/api/auth/refresh`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`를 제외하고 **JWT 인증** 필요.

### 3.1 인증 (auth)

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/auth/login` | 로그인 (JWT 발급) |
| POST | `/api/auth/refresh` | 토큰 갱신 |
| POST | `/api/auth/logout` | 로그아웃 |

### 3.2 ATM 기기 (atm)

PK: `termGroupId + termId`

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/atm` | 목록 (페이징, 검색: termGroupId/termId/atmType/branchCd) |
| GET | `/api/atm/{termGroupId}/{termId}` | 단건 조회 |
| POST | `/api/atm` | 등록 |
| PUT | `/api/atm/{termGroupId}/{termId}` | 수정 |
| DELETE | `/api/atm/{termGroupId}/{termId}` | 삭제 |

### 3.3 영업점 (branch)

PK: `termGroupId + branchCd + branchName`

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/branch` | 목록 |
| GET/POST/PUT/DELETE | `/api/branch/...` | 표준 CRUD |

### 3.4 ATM 그룹 (atmgroup)

PK: `termGroupId`

| Method | Path | 설명 |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/atm-group` | 표준 CRUD (그룹 계층: parentId, superId) |

### 3.5 ATM 상태 (atmstatus)

PK: `termGroupId + termId` — **upsert(MERGE) 방식**

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/atm-status` | 목록 (검색: serviceStatus, atmFaultStatus, errGroupCd) |
| GET | `/api/atm-status/{termGroupId}/{termId}` | 단건 조회 |
| PUT | `/api/atm-status` | 상태 갱신 (없으면 INSERT, 있으면 UPDATE) |

### 3.6 사용자 (user)

PK: `userId` — 비밀번호는 **BCrypt**로 인코딩

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/user` | 목록 |
| GET | `/api/user/{userId}` | 단건 |
| POST | `/api/user` | 등록 (PW 자동 BCrypt) |
| PUT | `/api/user/{userId}` | 수정 |
| DELETE | `/api/user/{userId}` | 삭제 |
| PUT | `/api/user/{userId}/password` | 비밀번호 변경 (만료일 +90일, 잠금 해제) |

### 3.7 역할 (role)

PK: `roleCode`

| Method | Path | 설명 |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/role` | 표준 CRUD |

### 3.8 공지사항 (notice)

PK: `catCd + seqNo` — `seqNo`는 카테고리별 `MAX+1` 자동 채번, 조회 시 hit++

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/notice` | 목록 (검색: catCd/subject/bankCd/날짜범위) |
| GET | `/api/notice/{catCd}/{seqNo}` | 단건 (조회수 증가) |
| POST | `/api/notice` | 등록 (seqNo 자동 채번) |
| PUT | `/api/notice/{catCd}/{seqNo}` | 수정 |
| DELETE | `/api/notice/{catCd}/{seqNo}` | 삭제 |

### 3.9 공통코드 (code)

PK: `langCd + groupCd + code`

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/code` | 전체 목록 (페이징) |
| GET | `/api/code/group/{langCd}/{groupCd}` | 그룹 단위 조회 (드롭다운용, 페이징 X) |
| GET | `/api/code/{langCd}/{groupCd}/{code}` | 단건 |
| POST/PUT/DELETE | `/api/code/...` | CRUD |

## 4. 표준 응답 / 에러 코드

### 4.1 응답 포맷 (`ApiResponse<T>`)

```json
{ "success": true,  "code": "200", "message": "조회 성공", "data": { ... } }
{ "success": false, "code": "ATM_001", "message": "기기를 찾을 수 없습니다.", "data": null }
```

페이징 응답(`PageResult<T>`):
```json
{
  "content": [ ... ],
  "totalCount": 137,
  "page": 1,
  "size": 20,
  "totalPages": 7
}
```

### 4.2 도메인별 에러 코드

| 도메인 | 코드 | 의미 |
|---|---|---|
| atm | ATM_001 / ATM_002 | 미존재 / 중복 |
| branch | BRA_001 / BRA_002 | 미존재 / 중복 |
| atmgroup | GRP_001 / GRP_002 | 미존재 / 중복 |
| atmstatus | STA_001 | 미존재 |
| user | USR_001 / USR_002 | 미존재 / 중복 |
| role | ROL_001 / ROL_002 | 미존재 / 중복 |
| notice | NTC_001 | 미존재 |
| code | COD_001 / COD_002 | 미존재 / 중복 |
| 공통 | 401 / 403 / 500 | 인증/권한/시스템 |

## 5. 코드 구조

```
src/main/java/kr/co/nh/atms/web/
├── AtmsWebApplication.java
├── auth/        (controller, service)
└── {domain}/
    ├── controller/   {Domain}Controller        — REST 엔드포인트
    ├── service/      {Domain}Service           — 트랜잭션 (@Transactional readOnly + write)
    ├── mapper/       {Domain}Mapper            — MyBatis 인터페이스 (@Mapper)
    ├── entity/       {Domain}                  — 테이블 매핑 (Lombok @Data)
    └── dto/          {Domain}SearchDto         — 검색 DTO (extends PageRequest)

src/main/resources/
├── application.yml
└── mapper/{domain}/{Domain}Mapper.xml          — 동적 SQL, Oracle 페이징
```

### 5.1 컨벤션 요약

- 컨트롤러: `@RestController` + `@RequiredArgsConstructor`, 반환은 `ApiResponse.ok(data)` 또는 `ApiResponse.ok(data, msg)` (직접 반환)
- 서비스: 클래스에 `@Transactional(readOnly = true)`, 변경 메서드에 `@Transactional` 추가
- 예외: `throw new BusinessException("CODE", "메시지")` — `GlobalExceptionHandler`가 통일 응답으로 변환
- MyBatis: `<resultMap autoMapping="true"/>` + `map-underscore-to-camel-case: true`로 컬럼-필드 자동 매핑
- 페이징: Oracle `OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY` (1-base page → offset 자동 계산)
- 신규/수정 시각: `TO_CHAR(SYSDATE, 'YYYYMMDD')` / `'HH24MISS'` / `'YYYYMMDDHH24MISS'`

## 6. 테스트

```bash
./gradlew :atms-web:test
```

- 컨트롤러 슬라이스 테스트(`@WebMvcTest`)는 SecurityConfig 등 common 빈 의존이 있어 `@Import(SecurityConfig.class)` 또는 `@AutoConfigureMockMvc(addFilters = false)` 권장.
- Mapper 테스트는 H2 + 별도 DDL 스크립트(`src/test/resources/schema-h2.sql`)가 필요하다(현재 미포함, 도메인 추가 시 작성 권장).

## 7. 다음 작업 후보

- [ ] H2 DDL 스크립트 작성 → Mapper 통합 테스트
- [ ] 컨트롤러 단위 테스트 (`@WebMvcTest` + MockMvc)
- [ ] Bean Validation (`@Valid`, `@NotBlank` 등) 적용
- [ ] 페이징 응답 OpenAPI 스키마 명시
- [ ] 권한 기반 접근 제어 (`@PreAuthorize`)
