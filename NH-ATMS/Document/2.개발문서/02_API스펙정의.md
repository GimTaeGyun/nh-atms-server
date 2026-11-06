# NH-ATMS API 스펙 정의

> 최종 수정: 2026-04-01  
> Swagger UI: `http://localhost:8085/swagger-ui.html`

---

## 1. 공통 사항

### 1.1 Base URL
```
http://{서버IP}:{포트}/api
```
- 개발: `http://localhost:8085/api`
- Vite 프록시: `http://localhost:5173/api` → `http://localhost:8085/api`

### 1.2 인증
- **방식**: Bearer Token (JWT)
- **헤더**: `Authorization: Bearer {accessToken}`
- **예외 URL**: `/api/auth/login`, `/api/auth/refresh` (인증 없이 접근 가능)
- **토큰 만료 시**: 401 응답 → 프론트에서 자동으로 `/api/auth/refresh` 호출

### 1.3 공통 응답 포맷

```json
// 성공
{
    "success": true,
    "code": "200",
    "message": "성공",
    "data": { ... }
}

// 실패
{
    "success": false,
    "code": "TRANS_001",
    "message": "거래를 찾을 수 없습니다.",
    "data": null
}
```

### 1.4 에러 코드 체계

| 코드 | 설명 |
|------|------|
| `200` | 성공 |
| `AUTH_001` | 인증 실패 (잘못된 자격 증명) |
| `AUTH_002` | 토큰 만료 |
| `AUTH_003` | 권한 없음 |
| `TRANS_0xx` | 거래내역 관련 오류 |
| `DEV_0xx` | 기기관리 관련 오류 |
| `FAULT_0xx` | 장애관리 관련 오류 |
| `CASH_0xx` | 시재관리 관련 오류 |
| `CLOSE_0xx` | 마감처리 관련 오류 |
| `SYS_0xx` | 시스템 관련 오류 |

### 1.5 페이징 요청/응답

**요청 파라미터:**
| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| page | int | 1 | 현재 페이지 |
| size | int | 20 | 페이지당 건수 |

**응답 data:**
```json
{
    "list": [ ... ],
    "totalCount": 150,
    "page": 1,
    "size": 20
}
```

---

## 2. 인증 API (구현 완료)

### 2.1 로그인
```
POST /api/auth/login
```

**Request Body:**
```json
{
    "userId": "admin",
    "password": "1234",
    "loginType": "FORM"   // FORM | SSO
}
```

**Response:**
```json
{
    "success": true,
    "code": "200",
    "message": "로그인 성공",
    "data": {
        "accessToken": "eyJ...",
        "refreshToken": "eyJ...",
        "userId": "admin",
        "userName": "관리자",
        "roles": ["ADMIN"]
    }
}
```

### 2.2 토큰 갱신
```
POST /api/auth/refresh
```

**Request Body:**
```json
{
    "refreshToken": "eyJ..."
}
```

### 2.3 로그아웃
```
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

---

## 3. 도메인별 API 목록

> 아래는 TO-BE 메뉴 기반 API 설계안입니다.  
> 각 API의 상세 파라미터는 화면별 작업 명세서(`03_화면작업명세서_템플릿.md`) 참고.

### 3.1 시재관리 (cash)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| GET | `/api/cash/transaction/list` | 거래내역 조회 | 건별 거래 |
| GET | `/api/cash/transaction/{id}` | 거래 상세 조회 | |
| GET | `/api/cash/supply/list` | 투입내역 조회 | 보충/회수 |
| GET | `/api/cash/check/list` | 수표거래 조회 | |
| GET | `/api/cash/device-balance/list` | 기기시재 조회 | 카세트별 잔량 |
| GET | `/api/cash/prediction/list` | 운영자금 예측 | |
| POST | `/api/cash/destroy` | 거래내역 2차 파기 | |

### 3.2 마감처리 (closing)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| GET | `/api/closing/summary/list` | 집계내역 조회 | |
| GET | `/api/closing/status/list` | 합계상태 조회 | |
| GET | `/api/closing/approval/list` | 전자결재 목록 | |
| POST | `/api/closing/approval` | 전자결재 등록/승인 | |
| PUT | `/api/closing/approval/{id}` | 전자결재 수정 | |
| GET | `/api/closing/history/list` | 마감내역 조회 | |
| GET | `/api/closing/pending/list` | 미처리내역 조회 | |

### 3.3 장애관리 (fault)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| GET | `/api/fault/device/list` | 기기장애 목록 | |
| GET | `/api/fault/device/{id}` | 기기장애 상세 | |
| PUT | `/api/fault/device/{id}` | 장애조치 등록/수정 | |
| GET | `/api/fault/dispatch/list` | 출동조치 관리 목록 | |
| POST | `/api/fault/dispatch` | 출동 요청 등록 | |
| PUT | `/api/fault/dispatch/{id}` | 출동조치 결과 입력 | |
| GET | `/api/fault/history/list` | 장애내역 조회 | |
| GET | `/api/fault/availability/list` | 가동률 조회 | |
| GET | `/api/fault/code/list` | 장애코드 목록 | |
| POST | `/api/fault/code` | 장애코드 등록 | |
| PUT | `/api/fault/code/{id}` | 장애코드 수정 | |
| DELETE | `/api/fault/code/{id}` | 장애코드 삭제 | |
| GET | `/api/fault/report/list` | 장애명세표 조회 | |

### 3.4 저널관리 (journal)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| GET | `/api/journal/list` | 저널 조회 | |
| GET | `/api/journal/{id}` | 저널 상세 | |
| GET | `/api/journal/mode/list` | 저널관리모드 조회 | |
| GET | `/api/journal/image/history/list` | 저널이미지 조회이력 | |
| GET | `/api/journal/image/auth/list` | 저널이미지 권한관리 | |
| PUT | `/api/journal/image/auth/{id}` | 저널이미지 권한 수정 | |
| GET | `/api/journal/destroy/list` | 저널 2차파기 조회 | |
| POST | `/api/journal/destroy` | 저널 2차파기 실행 | |
| GET | `/api/journal/status/list` | 저널처리 현황 | |

### 3.5 기기관리 (device)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| GET | `/api/device/branch/list` | 본지점 목록 | |
| GET | `/api/device/branch/{id}` | 본지점 상세 | |
| GET | `/api/device/corner/list` | 코너 목록 | |
| GET | `/api/device/corner/{id}` | 코너 상세 | |
| GET | `/api/device/atm/list` | 기기정보 목록 | |
| GET | `/api/device/atm/{id}` | 기기정보 상세 | |

### 3.6 원격관리 (remote)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| POST | `/api/remote/control` | 원격제어 실행 | 재부팅, 모드변경 등 |
| GET | `/api/remote/control/history/list` | 원격제어 이력 | |
| GET | `/api/remote/download/list` | 다운로드 관리 목록 | |
| POST | `/api/remote/download` | 개별 다운로드 요청 | |
| POST | `/api/remote/download/batch` | 일괄 다운로드 요청 | |
| GET | `/api/remote/download/history/list` | 다운로드 이력 조회 | |
| POST | `/api/remote/open` | 개국/합계등록 | |
| POST | `/api/remote/close/reserve` | 합계/폐국 예약 | |

### 3.7 정보관리 (info)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| GET | `/api/info/user/list` | 사용자 목록 | |
| POST | `/api/info/user` | 사용자 등록 | |
| PUT | `/api/info/user/{id}` | 사용자 수정 | |
| DELETE | `/api/info/user/{id}` | 사용자 삭제 | |
| GET | `/api/info/code/list` | 공통코드 목록 | |
| POST | `/api/info/code` | 공통코드 등록 | |
| PUT | `/api/info/code/{id}` | 공통코드 수정 | |
| GET | `/api/info/menu/list` | 메뉴 목록 | |
| POST | `/api/info/menu` | 메뉴 등록 | |
| PUT | `/api/info/menu/{id}` | 메뉴 수정 | |
| GET | `/api/info/role/list` | 권한 목록 | |
| PUT | `/api/info/role/{id}` | 권한 수정 | |
| GET | `/api/info/model/list` | 기기모델 목록 | |
| GET | `/api/info/vendor/list` | 업체 목록 | |
| GET | `/api/info/notice/list` | 공지사항 목록 | |
| POST | `/api/info/notice` | 공지사항 등록 | |
| PUT | `/api/info/notice/{id}` | 공지사항 수정 | |
| DELETE | `/api/info/notice/{id}` | 공지사항 삭제 | |

### 3.8 대시보드 (dashboard)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| GET | `/api/dashboard/summary` | 대시보드 요약 | 기기현황, 장애현황 |
| GET | `/api/dashboard/fault-status` | 장애 현황 | |
| GET | `/api/dashboard/device-status` | 기기 상태 현황 | |
| GET | `/api/dashboard/cash-status` | 시재 현황 | |

### 3.9 일괄용역 (batch)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| GET | `/api/batch/list` | 배치작업 목록 | |
| POST | `/api/batch/execute` | 배치작업 실행 | |
| GET | `/api/batch/history/list` | 배치 실행 이력 | |

### 3.10 에이전트 수신 (agent)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| POST | `/api/agent/receive` | 에이전트 저널 수신 | 7개 저널타입 |
| GET | `/api/agent/status/list` | 에이전트 상태 목록 | |

---

## 4. 공통 조회 조건 패턴

대부분의 목록 조회 API는 아래 조건들을 조합:

| 파라미터 | 타입 | 설명 | 사용 빈도 |
|---------|------|------|----------|
| startDate | String (yyyyMMdd) | 조회 시작일 | 거의 모든 조회 |
| endDate | String (yyyyMMdd) | 조회 종료일 | 거의 모든 조회 |
| branchCode | String | 사무소(지점) 코드 | 높음 |
| cornerCode | String | 코너 코드 | 높음 |
| deviceId | String | 기기번호 | 높음 |
| deviceModel | String | 기기 모델 | 중간 |
| faultCode | String | 장애코드 | 장애 관련 |
| transType | String | 거래종류 | 거래 관련 |
| status | String | 상태 | 중간 |
| keyword | String | 검색 키워드 | 낮음 |
| page | int | 페이지 번호 (기본 1) | 모든 목록 |
| size | int | 페이지 크기 (기본 20) | 모든 목록 |

---

## 5. Swagger 어노테이션 작성 가이드

### 5.1 Controller 레벨

```java
@Tag(name = "시재관리 API", description = "거래내역, 투입내역, 기기시재 등 시재 관련 API")
@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {
```

### 5.2 메서드 레벨

```java
@Operation(
    summary = "거래내역 목록 조회",
    description = "기간, 기기번호, 사무소 등 조건으로 거래내역을 조회합니다."
)
@GetMapping("/transaction/list")
public ResponseEntity<ApiResponse<PageResponse<TransactionListResponse>>> 
    getTransactionList(@Valid TransactionSearchRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(cashService.getTransactionList(request)));
}
```

### 5.3 DTO 레벨

```java
@Getter @Setter
@Schema(description = "거래내역 조회 요청")
public class TransactionSearchRequest {
    @Schema(description = "조회 시작일", example = "20260401", required = true)
    @NotBlank(message = "조회 시작일은 필수입니다")
    private String startDate;
    
    @Schema(description = "기기번호", example = "ATM0001")
    private String deviceId;
}
```

---

## 6. API 개발 체크리스트

새 API를 만들 때 다음 순서로 작업:

- [ ] Mapper XML 작성 (SQL)
- [ ] Mapper Interface 작성
- [ ] Request/Response DTO 작성
- [ ] Service 작성 (비즈니스 로직)
- [ ] Controller 작성 (Swagger 어노테이션 포함)
- [ ] Swagger UI 확인
- [ ] 프론트 연동 테스트
