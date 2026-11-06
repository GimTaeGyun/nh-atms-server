# NH-ATMS 화면설계 초안 (AS-IS 기반)

> 최종 수정: 2026-04-02  
> ⚠️ **본 문서는 고객 요구사항 확정 전, AS-IS 분석 기반으로 작성한 초안입니다.**  
> 향후 요구사항 협의 결과에 따라 조회조건, 그리드 컬럼, 화면 구성이 변경될 수 있습니다.  
> NH AS-IS 화면 캡쳐 + 새마을금고 JSP 패턴을 참고하여 작성

---

## 공통 설계 규칙

### 1. 조회조건 레이아웃

- **4열 그리드** 기준 (Row당 필드 4개)
- 날짜범위는 1칸(시작~종료), 일반 필드도 1칸
- 조회/초기화 버튼은 조회영역 우측
- 엑셀다운로드/출력 등 액션 버튼은 그리드 우상단

### 2. 계층 종속 필드 (Cascade)

모든 화면 공통으로 아래 계층 종속 구조 적용:

```
사무소(branchCode) → 코너(cornerCode) → 기기번호(deviceId)
```

- 사무소 선택 시 → 코너 목록 자동 필터링
- 코너 선택 시 → 기기번호 목록 자동 필터링
- 기기업체(atmVendor) 선택 시 → 모델명(modelCode) 자동 필터링

### 3. 컴포넌트 타입 정의

| 타입 | 설명 | 비고 |
|------|------|------|
| `input` | 텍스트 입력 | maxLength 지정 |
| `select` | 드롭다운 | 공통코드 API 연동 |
| `cascadeSelect` | 종속 드롭다운 | 상위 선택 시 하위 갱신 |
| `datePicker` | 단일 날짜 | 기본값: 오늘 |
| `dateRangePicker` | 날짜 범위 | 시작일~종료일 |
| `radio` | 라디오 버튼 | 단일 선택 |
| `checkbox` | 체크박스 | 다중 선택 |
| `inputRange` | 숫자 범위 | 최소값~최대값 |

### 4. 그리드 공통 컬럼

| 컬럼 | dataIndex | width | 정렬 | 포맷 |
|------|-----------|-------|------|------|
| No | rowNum | 60 | center | 자동번호 |
| 사무소명 | branchName | 120 | left | |
| 코너명 | cornerName | 100 | left | |
| 기기번호 | deviceId | 80 | center | |
| 일시 | datetime | 150 | center | YYYY-MM-DD HH:mm:ss |
| 금액 | amount | 120 | right | 천단위 콤마 |
| 상태 | status | 80 | center | 뱃지/색상 |

### 5. 공통 버튼

| 버튼 | 기능 | 위치 |
|------|------|------|
| 조회 | 검색 실행 | 조회영역 우측 |
| 초기화 | 조회조건 리셋 | 조회영역 우측 |
| 엑셀 | 엑셀 다운로드 | 그리드 우상단 |
| 출력 | 인쇄 | 그리드 우상단 |

---

---

# 1. 대시보드

---

## DASH-001. 메인 대시보드

| 항목 | 내용 |
|------|------|
| **화면ID** | DASH-001 |
| **화면명** | 메인 대시보드 |
| **메뉴경로** | 로그인 후 메인 |
| **난이도** | 상 |

### 화면 설명
로그인 후 첫 화면. ATM 전체 현황을 한눈에 파악하는 종합 모니터링 화면.

### 화면 구성 (위젯)

| 영역 | 위젯 | 데이터 | 차트 타입 |
|------|------|--------|----------|
| 상단 | 기기현황 요약 | 총운영/정상/미개국/장애/통신단절 건수 | 숫자 카드 (Statistic) |
| 좌상 | 지역별 기기 현황 | 지역본부별 ATM 가동률 | 막대그래프 (Bar) |
| 우상 | 시간대별 거래량 | 최근 24시간 거래 추이 | 꺾은선그래프 (Line) |
| 좌하 | 장애 현황 | 장애유형별 건수 | 도넛차트 (Pie) |
| 우하 | 최근 장애 목록 | 최근 장애 발생 기기 리스트 | 테이블 |

### API 연동

| API | 경로 | 용도 |
|-----|------|------|
| 기기현황 요약 | `GET /api/dashboard/device-summary` | 상단 숫자 카드 |
| 지역별 현황 | `GET /api/dashboard/region-status` | 지역 막대그래프 |
| 시간대별 거래 | `GET /api/dashboard/hourly-transaction` | 거래 추이 |
| 장애 현황 | `GET /api/dashboard/fault-summary` | 장애 도넛차트 |
| 최근 장애 | `GET /api/dashboard/recent-faults` | 장애 리스트 |

### 특이사항
- 자동 갱신: 30초 간격 polling 또는 WebSocket
- 차트 라이브러리: `@ant-design/charts` 사용
- 숫자 카드에 전일 대비 증감 표시 (↑↓)

---

---

# 2. 시재관리 (CASH)

---

## CASH-001. 거래내역 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | CASH-001 |
| **화면명** | 거래내역 조회 |
| **메뉴경로** | 시재관리 > 거래내역조회 |
| **난이도** | 중 |
| **NH AS-IS** | 1000~1008 전체 통합 |

### 화면 설명
ATM 거래내역(입금/출금/이체/공과금/신용카드/무통장/외화 등)을 통합 조회하는 화면. NH AS-IS에서 9개 화면으로 분리되어 있으나 거래유형 필터로 1개 화면에 통합.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | 하위: 코너, 기기번호 연동 |
| 2 | 코너 | cornerCode | cascadeSelect | N | 전체 | 사무소 선택 시 갱신 |
| 3 | 기기번호 | deviceId | cascadeSelect | N | 전체 | 코너 선택 시 갱신 |
| 4 | 모델명 | modelName | input | N | | 텍스트 검색 |
| 5 | 거래일자 | startDate~endDate | dateRangePicker | Y | 오늘 | 합계후~합계전 기준 |
| 6 | 거래유형 | transType | select | N | 전체 | 입금/출금/이체/공과금/신용카드/무통장/통장기장/외화 |
| 7 | 코너형태 | cornerType | select | N | 전체 | 공통코드 |
| 8 | 운영구분 | operationType | select | N | 전체 | 공통코드 |

### 탭 구성

| 탭 | 설명 | 필터 조건 |
|----|------|----------|
| 전체 | 모든 거래 | 없음 |
| 입출금 | 자행/타행 입출금 | transType IN (10,20) |
| 공과금 | 장표납부/조회납부 | transType = 30 |
| 신용카드 | 단기/장기/기타 | transType = 40 |
| 수표 | 수표 입출금 | transType = 50 |
| 외화 | 외화 입출금 | transType = 60 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 거래종류 | transTypeName | 100 | center | 코드→명칭 |
| 3 | 거래일시 | transDatetime | 150 | center | YYYY-MM-DD HH:mm:ss |
| 4 | 사무소 | branchName | 120 | left | |
| 5 | 기기번호 | deviceId | 80 | center | |
| 6 | 일련번호 | seqNo | 80 | center | |
| 7 | 계좌은행 | accountBank | 80 | center | |
| 8 | 예금주명 | accountHolder | 100 | left | 마스킹 처리 |
| 9 | 계좌번호 | accountNo | 140 | center | 마스킹 처리 |
| 10 | 거래금액 | amount | 120 | right | 천단위 콤마 |
| 11 | 수수료 | fee | 80 | right | 천단위 콤마 |
| 12 | 거래상태 | transStatus | 80 | center | 정상/불능/취소 |
| 13 | 합계구분 | totalDiv | 80 | center | |

### 추가 컬럼 (탭별)

| 탭 | 추가 컬럼 |
|----|----------|
| 공과금 | 장표건수(docCount), 수납결과(receiptResult) |
| 신용카드 | 카드구분(cardType), 카드번호(cardNo) |
| 수표 | 수표번호(checkNo), 수표은행(checkBank), 수표권종(checkDenomination) |
| 외화 | 거래금액(원)(amountKRW), 거래금액(외화)(amountForeign), 통화(currency) |

### 버튼

| 버튼 | 기능 |
|------|------|
| 조회 | 거래내역 목록 조회 |
| 초기화 | 조회조건 리셋 |
| 엑셀 | 엑셀 다운로드 |
| 출납과입금 | 출납과입금 집계 팝업 |
| 거래상태별 집계 | 상태별 집계 팝업 |

### 상세 조회 (행 더블클릭)
- 거래 상세 정보 모달 표시
- 저널조회 버튼 → 해당 거래의 저널 화면 이동

### 특이사항
- NH AS-IS 1000~1008을 탭으로 통합
- 계좌번호, 예금주명은 마스킹 표시 (※※※)
- 날짜 기준: "합계후~합계전" 개념 (전일 합계 처리 후 ~ 당일 합계 처리 전)
- 페이징 처리 (기본 20건)

---

## CASH-002. 투입내역 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | CASH-002 |
| **화면명** | 투입내역 조회 |
| **메뉴경로** | 시재관리 > 투입내역조회 |
| **난이도** | 중 |
| **NH AS-IS** | 3202 자동화기기 시재확인 |

### 화면 설명
ATM 기기에 현금을 보충(투입)하거나 회수한 내역을 조회하는 화면. Cash(41) 저널 기반.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 코너 | cornerCode | cascadeSelect | N | 전체 | |
| 3 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 4 | 모델명 | modelName | input | N | | |
| 5 | 투입일자 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 6 | 코너형태 | cornerType | select | N | 전체 | 공통코드 |
| 7 | 운영구분 | operationType | select | N | 전체 | 공통코드 |
| 8 | 업무구분 | businessType | select | N | 전체 | 보충/회수/기타 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 투입일시 | addCashDatetime | 150 | center | |
| 3 | 사무소명 | branchName | 120 | left | |
| 4 | 코너명 | cornerName | 100 | left | |
| 5 | 기기번호 | deviceId | 80 | center | |
| 6 | 업무구분 | businessTypeName | 80 | center | 보충/회수 |
| 7 | 권종(5만) | amount50k | 100 | right | 장수 |
| 8 | 권종(1만) | amount10k | 100 | right | 장수 |
| 9 | 권종(5천) | amount5k | 100 | right | 장수 |
| 10 | 권종(1천) | amount1k | 100 | right | 장수 |
| 11 | 합계금액 | totalAmount | 120 | right | 천단위 콤마 |
| 12 | 처리자 | operatorName | 80 | center | |

### 버튼
- 조회, 초기화, 엑셀

### 특이사항
- 권종별 장수와 합계금액을 동시 표시
- 보충과 회수를 업무구분으로 구분

---

## CASH-003. 수표거래 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | CASH-003 |
| **화면명** | 수표거래 조회 |
| **메뉴경로** | 시재관리 > 수표거래조회 |
| **난이도** | 중 |
| **NH AS-IS** | 1005 수표입출금 거래조회 |

### 화면 설명
수표 입출금 거래를 전문적으로 조회. 수표번호, 발행은행, 권종 등 수표 특화 정보 제공.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 3 | 거래일자 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 4 | 수표번호 | checkNo | input | N | | |
| 5 | 발행은행 | issueBank | select | N | 전체 | |
| 6 | 당/타점권구분 | checkDiv | select | N | 전체 | 당점권/타점권 |
| 7 | 거래구분 | transDiv | select | N | 전체 | 입금/출금 |
| 8 | 코너형태 | cornerType | select | N | 전체 | |

### 탭 구성

| 탭 | 설명 |
|----|------|
| 출금+입금 | 전체 수표 거래 |
| 출금거래 | 수표 출금만 |
| 입금거래 | 수표 입금만 (수표 상세정보 컬럼 추가) |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 거래종류 | transTypeName | 80 | center | |
| 3 | 거래일시 | transDatetime | 150 | center | |
| 4 | 기기번호 | deviceId | 80 | center | |
| 5 | 일련번호 | seqNo | 80 | center | |
| 6 | 수표은행 | checkBank | 80 | center | 입금탭 |
| 7 | 수표사무소 | checkBranch | 80 | center | 입금탭 |
| 8 | 수표권종 | checkDenomination | 80 | center | 입금탭 |
| 9 | 수표번호 | checkNo | 120 | center | 입금탭 |
| 10 | 당/타점 구분 | checkDiv | 80 | center | 입금탭 |
| 11 | 계좌정보 | accountInfo | 200 | left | |
| 12 | 거래금액 | amount | 120 | right | |
| 13 | 거래상태 | transStatus | 80 | center | |

### 버튼
- 조회, 초기화, 엑셀, 수표입금자료출력(입금탭)

### 추가 조회옵션 (입금거래 탭)
- ☑자행 ☑타행 ☑정상건만 조회 (체크박스)

---

## CASH-004. 기기시재 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | CASH-004 |
| **화면명** | 기기시재 조회 |
| **메뉴경로** | 시재관리 > 기기시재조회 |
| **난이도** | 중 |
| **NH AS-IS** | 3200 자동화기기 시재현황 |

### 화면 설명
ATM 기기별 현재 현금 잔액 현황을 조회. 권종별 시재, 전일잔액, 인수금액, 입금, 출금 등 시재 흐름 파악.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 코너 | cornerCode | cascadeSelect | N | 전체 | |
| 3 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 4 | 거래일 | baseDate | datePicker | Y | 오늘 | |
| 5 | 관리구분 | manageType | select | N | 전체 | 직영/위탁 등 |
| 6 | 코너형태 | cornerType | select | N | 전체 | |
| 7 | 운영구분 | operationType | select | N | 전체 | |
| 8 | 기기시재범위 | balanceStart~balanceEnd | inputRange | N | | 만원 단위 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소명 | branchName | 120 | left | |
| 3 | 코너명 | cornerName | 100 | left | |
| 4 | 기기번호 | deviceId | 80 | center | |
| 5 | 최종변경시간 | lastUpdateTime | 150 | center | |
| 6 | 전일시재 | prevBalance | 120 | right | ① |
| 7 | 인수금액 | receivedAmount | 120 | right | ② |
| 8 | 입금 | depositAmount | 120 | right | ③ |
| 9 | 출금 | withdrawAmount | 120 | right | ④ |
| 10 | 회수 | collectAmount | 120 | right | ⑤ |
| 11 | 현시재 | currentBalance | 120 | right | ⑥=①+②+③-④-⑤ |
| 12 | 5만원권 | bill50k | 80 | right | 장수 |
| 13 | 1만원권 | bill10k | 80 | right | 장수 |

### 버튼
- 조회, 초기화, 엑셀, 갱신(시재 최신화 요청)

### 특이사항
- 현시재 = 전일시재 + 인수금액 + 입금 - 출금 - 회수
- 시재 부족 기기는 행 하이라이트 (빨간색)
- 외화 ATM은 별도 탭 또는 통화 필터로 처리

---

## CASH-005. 운영자금 예측

| 항목 | 내용 |
|------|------|
| **화면ID** | CASH-005 |
| **화면명** | 운영자금 예측 |
| **메뉴경로** | 시재관리 > 운영자금예측 |
| **난이도** | 상 |
| **NH AS-IS** | 6104 운영자금대사 |

### 화면 설명
권종별 현금 사용 추이를 분석하고 향후 운영자금 필요량을 예측. 현송 계획 수립에 활용.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 3 | 기준일 | baseDate | datePicker | Y | 오늘 | |
| 4 | 조회기간 | period | select | N | 7일 | 1일/3일/5일/7일/14일/30일 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소명 | branchName | 120 | left | |
| 3 | 기기번호 | deviceId | 80 | center | |
| 4 | 현재시재 | currentBalance | 120 | right | |
| 5 | 일평균출금 | avgDailyWithdraw | 120 | right | |
| 6 | 예상소진일 | estimatedExhaustDate | 100 | center | D-day 표시 |
| 7 | 권종별잔여(5만) | remaining50k | 80 | right | |
| 8 | 권종별잔여(1만) | remaining10k | 80 | right | |
| 9 | 보충필요여부 | needRefill | 80 | center | Y/N 뱃지 |

### 버튼
- 조회, 초기화, 엑셀

### 특이사항
- 소진 예상일 3일 이내 기기 하이라이트
- 차트: 일별 출금 추이 꺾은선 그래프 (클릭한 기기)

---

## CASH-006. 거래내역 2차파기 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | CASH-006 |
| **화면명** | 거래내역 2차파기 조회 |
| **메뉴경로** | 시재관리 > 거래내역2차파기조회 |
| **난이도** | 중 |
| **NH AS-IS** | 1020/1301 통합 |

### 화면 설명
개인정보 보호조치(파기)된 거래내역을 승인 절차를 거쳐 조회. 금융감독규정 필수 기능.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 거래일자 | startDate~endDate | dateRangePicker | Y | | 조회가능기간 제한 표시 |
| 2 | 처리유형 | searchType | radio | Y | 자행계좌번호 | ○자행 계좌번호 ○타행 계좌번호 ○카드 번호 ○명세표 일련번호 |
| 3 | 계좌번호 | accountNo | input | Y | | 처리유형에 따라 라벨 변경 |
| 4 | 조회사유 | searchReason | select | Y | | 법원자료제출/조세관련제출/본인요청/민원관련조회/기타 |
| 5 | 상세사유 | detailReason | input | N | | 기타 선택 시 필수 |

### 탭 구성

| 탭 | 설명 |
|----|------|
| 농협은행+농축협 | 전체 |
| 농협은행 | 농협은행만 |
| 농축협 | 농축협만 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 거래종류 | transTypeName | 100 | center | |
| 3 | 거래일시 | transDatetime | 150 | center | |
| 4 | 사무소 | branchName | 120 | left | |
| 5 | 기기번호 | deviceId | 80 | center | |
| 6 | 일련번호 | seqNo | 80 | center | |
| 7 | 개설계좌은행 | openBank | 80 | center | |
| 8 | 예금주명 | accountHolder | 100 | left | 마스킹 |
| 9 | 계좌번호 | accountNo | 140 | center | 마스킹 |
| 10 | 수취계좌은행 | recvBank | 80 | center | |
| 11 | 수취예금주명 | recvHolder | 100 | left | 마스킹 |
| 12 | 수취계좌번호 | recvAccountNo | 140 | center | 마스킹 |
| 13 | 거래금액 | amount | 120 | right | |

### 버튼
- 승인요청, 조회

### 특이사항
- ⚠ "해당 정보는 보호 조치되었습니다. 조회사유입력 및 책임자 승인 후 조회 가능합니다." 안내 문구 표시
- 조회사유 입력 필수 → 책임자 승인 프로세스 후 데이터 표시
- 조회 이력 자동 로깅 (감사 추적)
- 조회가능기간 표시 (서버에서 기간 정보 받아옴)
- 권한: 특정 권한을 가진 사용자만 접근 가능

---

---

# 3. 마감처리 (CLOSE)

---

## CLOSE-001. 집계내역/합계상태 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | CLOSE-001 |
| **화면명** | 집계내역/합계상태 조회 |
| **메뉴경로** | 마감처리 > 집계내역/합계상태조회 |
| **난이도** | 중 |
| **NH AS-IS** | 2101 |

### 화면 설명
일마감 집계 데이터를 센터분/기기분으로 구분하여 조회. Total(31) 저널 기반.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | Y | | |
| 2 | 거래일 | baseDate | datePicker | Y | 오늘 | ※ 30일 이내 조회 가능 |

### 화면 구성 (좌우 분할)

**좌측: 기기 리스트**

| 순서 | 컬럼명 | dataIndex | width |
|------|--------|-----------|-------|
| 1 | No | rowNum | 40 |
| 2 | 기기번호 | deviceId | 80 |
| 3 | 합계상태 | totalStatus | 80 |

- [합계상태조회 ▶] 버튼: 선택 기기의 상세 집계 표시

**우측: 센터집계분 + 기기집계분**

| 구분 | 항목 | 전일합계후 (건수/금액) | 당일합계전 (건수/금액) | 합계 (건수/금액) | 당일합계후 (건수/금액) |
|------|------|----------------------|----------------------|----------------|----------------------|
| 센터분 | 현금지급 | | | | |
| 센터분 | 수표지급 | | | | |
| 센터분 | 현금입금 | | | | |
| 센터분 | 수표입금 | | | | |
| 기기분 | 현금지급 | | | | |
| 기기분 | 수표지급 | | | | |
| 기기분 | 현금입금 | | | | |
| 기기분 | 수표입금 | | | | |

### 버튼
- 조회, 출력

### 특이사항
- 30일 이내만 조회 가능
- 센터분(HOST 기준)과 기기분(ATM 기준) 집계값이 일치해야 정상
- 불일치 시 빨간색 하이라이트

---

## CLOSE-002. 마감 전자결재

| 항목 | 내용 |
|------|------|
| **화면ID** | CLOSE-002 |
| **화면명** | 마감 전자결재 |
| **메뉴경로** | 마감처리 > 마감전자결재 |
| **난이도** | 상 |
| **NH AS-IS** | 2200/2201 통합 |

### 화면 설명
매 영업일 마감 후 팀원→팀장 순서로 전자결재를 수행. 마감장표 자료를 확인한 후 결재.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | Y | | |
| 2 | 거래기준일자 | baseDate | datePicker | Y | 전영업일 | |

### 화면 구성

**기준일자 표시:** `2026.03.31 합계후 ~ 2026.04.01 합계전`

**마감 자료명 목록:**

| 화면번호 | 마감 자료명 | 자료확인 |
|---------|------------|---------|
| 26-1005 | 수표입금거래 | [내역확인] 버튼 |
| 26-1005 | 수표입금/거래상태별 집계표 | [내역확인] 버튼 |
| 26-1006 | 무통장/무카드거래 | [내역확인] 버튼 |
| 26-1100 | 출납과입금 예상거래 | [내역확인] 버튼 |
| 26-2101 | 입출금 합계내역 조회표 | [내역확인] 버튼 |
| 전체 | | [전체 내역확인] 버튼 |

**결재정보:**

| 구분 | 개인번호 | 성명 | 결재일시 |
|------|---------|------|---------|
| 팀원 | | | |
| 팀장 | | | |

### 버튼
- 결재(팀원), 결재(팀장)

### 전자결재 절차
1. ① 각 자료별 [내역확인] 클릭하여 검증
2. ② [결재(팀원)] 버튼으로 팀원 결재
3. ③ 팀장이 동일하게 [내역확인] 검증 후
4. ④ [결재(팀장)] 버튼으로 팀장 결재

### 특이사항
- 팀원 결재 완료 전에는 팀장 결재 불가
- 모든 자동화기기 합계 완료 후 진행 필수
- 매영업일 반드시 수행해야 하는 업무
- 결재 이력은 DB에 저장 (감사 추적)

---

## CLOSE-003. 마감내역 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | CLOSE-003 |
| **화면명** | 마감내역 조회 |
| **메뉴경로** | 마감처리 > 마감내역조회 |
| **난이도** | 중 |
| **NH AS-IS** | 6101 |

### 화면 설명
일별 마감 처리 결과를 조회. 기기별 마감 완료 여부, 합계 상태 등 확인.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 3 | 마감일자 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 4 | 마감상태 | closingStatus | select | N | 전체 | 완료/미완료/오류 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 마감일자 | closingDate | 100 | center | |
| 3 | 사무소명 | branchName | 120 | left | |
| 4 | 기기번호 | deviceId | 80 | center | |
| 5 | 합계상태 | totalStatus | 80 | center | 뱃지 |
| 6 | 센터합계 | centerTotal | 120 | right | 금액 |
| 7 | 기기합계 | deviceTotal | 120 | right | 금액 |
| 8 | 차이 | difference | 100 | right | 불일치 시 빨간 |
| 9 | 결재상태 | approvalStatus | 80 | center | 미결재/팀원/완료 |
| 10 | 마감시간 | closingTime | 80 | center | |

### 버튼
- 조회, 초기화, 엑셀

---

## CLOSE-004. 미처리내역 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | CLOSE-004 |
| **화면명** | 미처리내역 조회 |
| **메뉴경로** | 마감처리 > 미처리내역조회 |
| **난이도** | 중 |
| **NH AS-IS** | 6102 |

### 화면 설명
마감이 완료되지 않은 기기 또는 미결재 건을 조회. 일마감 업무의 누락 방지용.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기준일 | baseDate | datePicker | Y | 오늘 | |
| 3 | 미처리유형 | unprocessedType | select | N | 전체 | 미합계/미결재/미마감 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소명 | branchName | 120 | left | |
| 3 | 기기번호 | deviceId | 80 | center | |
| 4 | 미처리유형 | unprocessedTypeName | 100 | center | |
| 5 | 마지막합계시간 | lastTotalTime | 150 | center | |
| 6 | 기기상태 | deviceStatus | 80 | center | |
| 7 | 비고 | remark | 200 | left | |

### 버튼
- 조회, 초기화, 엑셀

---

---

# 4. 장애관리 (FAULT)

---

## FAULT-001. 기기상태 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | FAULT-001 |
| **화면명** | 기기상태 조회 (기기장애관리) |
| **메뉴경로** | 장애관리 > 기기장애관리 > 기기상태조회 |
| **난이도** | 상 |
| **NH AS-IS** | 3100/6000 통합 |

### 화면 설명
전체 ATM 기기의 실시간 상태를 조회. 상단에 요약 통계, 하단에 상세 목록.

### 상단 요약 (Statistic 카드)

| 항목 | 라벨 | 설명 |
|------|------|------|
| 총운영대수 | total | 전체 운영 대수 |
| 정상운영 | normal | 정상 가동 중 |
| 미개국 | notOpened | 미개국 기기 |
| 기기장애 | deviceFault | H/W 장애 |
| 운영장애 | operationFault | S/W 장애 |
| 통신단절 | disconnected | 통신 불가 |

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 코너 | cornerCode | cascadeSelect | N | 전체 | |
| 3 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 4 | 기기업체 | atmVendor | cascadeSelect | N | 전체 | 하위: 모델명 연동 |
| 5 | 모델명 | modelCode | cascadeSelect | N | 전체 | 기기업체 선택 시 갱신 |
| 6 | 코너형태 | cornerType | select | N | 전체 | |
| 7 | 운영구분 | operationType | select | N | 전체 | |
| 8 | 기기상태 | deviceStatus | select | N | 전체 | 정상/장애/미개국/통신단절 |
| 9 | 협력업체 | cooperator | select | N | 전체 | |
| 10 | 경비업체 | securityCompany | select | N | 전체 | |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소명 | branchName | 120 | left | |
| 3 | 코너명 | cornerName | 100 | left | |
| 4 | 운영시간 | serviceTime | 80 | center | |
| 5 | 용역업체 | serviceProvider | 80 | center | |
| 6 | 기기번호 | deviceId | 80 | center | |
| 7 | 상태 | status | 80 | center | 색상 뱃지 |
| 8 | 장애명 | faultName | 150 | left | |
| 9 | 발생일시 | faultDatetime | 150 | center | |
| 10 | 출동구분 | dispatchType | 80 | center | |
| 11 | 도착예정 | arrivalExpected | 80 | center | |
| 12 | 도착통보 | arrivalNotified | 80 | center | |
| 13 | 조치완료 | actionCompleted | 80 | center | |
| 14 | 조치결과 | actionResult | 100 | left | |

### 버튼
- 조회, 초기화, 엑셀

### 행 클릭 시 하단 탭

| 탭 | 내용 |
|----|------|
| 미복구장애내역 | 해당 기기의 현재 장애 목록 |
| 거래내역 | 최근 거래 5건 |
| 장치별상태 | 각 장치(카드리더, 입금기, 출금기 등) 상태 |
| 카세트상태 | 카세트별 잔여량 |
| 투입내역 | 최근 투입/회수 이력 |
| 합계내역 | 마감 합계 정보 |

### 특이사항
- 상태별 행 색상: 정상(흰), 장애(빨강), 미개국(노랑), 통신단절(회색)
- 자동 갱신 옵션 (30초/1분/수동)

---

## FAULT-002. 출동조치 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | FAULT-002 |
| **화면명** | 출동조치 관리 |
| **메뉴경로** | 장애관리 > 출동조치관리 |
| **난이도** | 상 |
| **NH AS-IS** | 6002/3102/3103 통합 |

### 화면 설명
장애 발생 시 출동 요청~조치 완료까지의 전체 프로세스를 관리.

### 탭 구성

| 탭 | 설명 |
|----|------|
| 출동조치이력 | 출동 이력 조회 |
| 기기유지보수접수 | H/W 수리 접수 |
| 장애SMS통보이력 | SMS 통보 로그 |

### 조회 조건 (출동조치이력)

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 코너 | cornerCode | cascadeSelect | N | 전체 | |
| 3 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 4 | 장애발생일 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 5 | 조치상태 | actionStatus | select | N | 전체 | 출동요청/응답/도착/조치완료 |
| 6 | 협력업체 | cooperator | select | N | 전체 | |

### 그리드 컬럼 (출동조치이력)

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소명 | branchName | 120 | left | |
| 3 | 기기번호 | deviceId | 80 | center | |
| 4 | 장애명 | faultName | 150 | left | |
| 5 | 발생일시 | faultDatetime | 150 | center | |
| 6 | 출동요청시간 | requestTime | 80 | center | |
| 7 | 응답시간 | responseTime | 80 | center | |
| 8 | 도착시간 | arrivalTime | 80 | center | |
| 9 | 조치완료시간 | completeTime | 80 | center | |
| 10 | 조치결과 | actionResult | 120 | left | |
| 11 | 업체명 | companyName | 100 | left | |
| 12 | 기사명 | engineerName | 80 | center | |

### 버튼
- 조회, 초기화, 엑셀

---

## FAULT-003. 장애내역 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | FAULT-003 |
| **화면명** | 장애내역 조회 |
| **메뉴경로** | 장애관리 > 장애내역조회 |
| **난이도** | 중 |
| **NH AS-IS** | 3101 장애내역 조회 |

### 화면 설명
과거 장애 발생 이력을 조회. Failure(22) 저널 기반.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 코너 | cornerCode | cascadeSelect | N | 전체 | |
| 3 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 4 | 장애발생일 | startDate~endDate | dateRangePicker | Y | 어제~오늘 | |
| 5 | 장애구분 | faultDiv | select | N | 전체 | 장애/복구 |
| 6 | 장치 | deviceType | select | N | 전체 | 카드리더/입금기/출금기/... |
| 7 | 기기업체 | atmVendor | cascadeSelect | N | 전체 | 하위: 모델명 연동 |
| 8 | 모델명 | modelCode | cascadeSelect | N | 전체 | |
| 9 | 협력업체 | cooperator | select | N | 전체 | |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 장애일시 | faultDatetime | 150 | center | |
| 3 | 구분 | faultDiv | 60 | center | 장애/복구 뱃지 |
| 4 | 사무소명 | branchName | 120 | left | |
| 5 | 코너명 | cornerName | 100 | left | |
| 6 | 기기번호 | deviceId | 80 | center | |
| 7 | 단말상태 | terminalStatus | 80 | center | 사용중지/장애/보수 |
| 8 | 장애코드(DEV) | faultCodeDev | 80 | center | |
| 9 | 장애코드(ER) | faultCodeEr | 80 | center | |
| 10 | 장애내용 | faultContent | 200 | left | |
| 11 | 업체장애코드 | vendorFaultCode | 80 | center | |
| 12 | 통보-경비 | notifyGuard | 60 | center | ○/× |
| 13 | 통보-보수 | notifyRepair | 60 | center | ○/× |
| 14 | 통보-알림톡 | notifyAlimtalk | 60 | center | ○/× |

### 버튼
- 조회, 초기화, 엑셀

---

## FAULT-004. 장애/가동률 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | FAULT-004 |
| **화면명** | 장애/가동률 조회 |
| **메뉴경로** | 장애관리 > 장애/가동률조회 |
| **난이도** | 중 |
| **NH AS-IS** | 5102 가동률 |

### 화면 설명
기기별 가동률을 산출하여 조회. 정상가동시간/전체운영시간 기준.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 3 | 기준일 | startDate~endDate | dateRangePicker | Y | 최근7일 | |
| 4 | 가동률범위 | rateStart~rateEnd | inputRange | N | | % 입력 |
| 5 | 기기업체 | atmVendor | cascadeSelect | N | 전체 | |
| 6 | 모델명 | modelCode | cascadeSelect | N | 전체 | |
| 7 | 협력업체 | cooperator | select | N | 전체 | |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소명 | branchName | 120 | left | |
| 3 | 기기번호 | deviceId | 80 | center | |
| 4 | 기기업체 | atmVendorName | 80 | center | |
| 5 | 모델명 | modelName | 100 | left | |
| 6 | 전체운영시간 | totalOperationTime | 100 | right | 분 |
| 7 | 정상가동시간 | normalOperationTime | 100 | right | 분 |
| 8 | 장애시간 | faultTime | 80 | right | 분 |
| 9 | 가동률 | operationRate | 80 | right | % (소수1자리) |
| 10 | 장애건수 | faultCount | 60 | right | |

### 버튼
- 조회, 초기화, 엑셀

### 특이사항
- 가동률 = 정상가동시간 / 전체운영시간 × 100
- 가동률 90% 미만 기기 빨간 하이라이트

---

## FAULT-005. 장애코드 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | FAULT-005 |
| **화면명** | 장애코드 관리 |
| **메뉴경로** | 장애관리 > 장애코드관리 |
| **난이도** | 중 |
| **NH AS-IS** | 7202/4020 |

### 화면 설명
장애코드 체계를 관리. 에이전트 장애코드 ↔ ATMS 장애코드 매핑.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 장애코드 | faultCode | input | N | | |
| 2 | 장애명 | faultName | input | N | | 부분 검색 |
| 3 | 장치분류 | deviceCategory | select | N | 전체 | 카드리더/입금기/출금기/... |
| 4 | 사용여부 | useYn | select | N | 전체 | Y/N |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 장애코드(DEV) | faultCodeDev | 80 | center | |
| 3 | 장애코드(ER) | faultCodeEr | 80 | center | |
| 4 | 장애명 | faultName | 200 | left | |
| 5 | 장치분류 | deviceCategory | 100 | center | |
| 6 | 장애등급 | faultLevel | 80 | center | |
| 7 | 자동출동여부 | autoDispatch | 80 | center | Y/N |
| 8 | 사용여부 | useYn | 60 | center | |
| 9 | 최종수정일 | updateDate | 100 | center | |

### 버튼
- 조회, 초기화, 신규등록, 수정, 삭제

### 상세 (모달 or 우측패널)
- 장애코드 등록/수정 폼
- 에이전트 장애코드 → ATMS 장애코드 매핑 테이블

---

## FAULT-006. 장애명세표 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | FAULT-006 |
| **화면명** | 장애명세표 조회 |
| **메뉴경로** | 장애관리 > 장애명세표 |
| **난이도** | 중 |
| **NH AS-IS** | 2301 |

### 화면 설명
에이전트가 설치된 기기의 장애명세표를 조회. 입금/지급 장애건수 요약.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 3 | 조회일자 | baseDate | datePicker | Y | 오늘 | ※ 최대 10일전 |

### 탭 구성

| 탭 | 설명 |
|----|------|
| 장애명세표 리스트 | 기기별 장애 요약 |
| 장애명세표 상세 | 선택 기기의 장애 상세 |

### 그리드 컬럼 (리스트)

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 기기번호 | deviceId | 80 | center | |
| 3 | 업체 | vendorName | 80 | center | |
| 4 | 기종 | modelName | 100 | left | |
| 5 | 입금장애건수 | depositFaultCount | 80 | right | |
| 6 | 지급장애건수 | withdrawFaultCount | 80 | right | |
| 7 | | viewDetail | 80 | center | [명세표보기] 링크 |

### 버튼
- 조회, 초기화, 출력

### 특이사항
- 에이전트 설치 기기만 조회 가능
- 최대 10일전까지만 조회 가능

---

## FAULT-007. 환경설정

| 항목 | 내용 |
|------|------|
| **화면ID** | FAULT-007 |
| **화면명** | 환경설정 |
| **메뉴경로** | 장애관리 > 환경설정 |
| **난이도** | 중 |

### 화면 설명
장애 SMS 통보 설정, 자동출동 설정, 자동출동 장애설정, 변경이력을 관리하는 설정 화면.

### 탭 구성

| 탭 | 설명 |
|----|------|
| 장애SMS통보설정 | 장애 발생 시 SMS 수신자 설정 |
| 자동출동설정 | 자동출동 조건 설정 |
| 자동출동장애설정 | 자동출동 대상 장애코드 설정 |
| 자동출동변경이력 | 설정 변경 이력 조회 |

### 장애SMS통보설정

**조회 조건:**

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 |
|------|--------|--------|---------|------|--------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 |

**그리드:**

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | 기기번호 | deviceId | 80 | center | |
| 2 | 기종 | modelName | 100 | left | |
| 3 | 기기상태알림 | statusAlarm | 80 | center | 설정/해제 토글 |
| 4 | 수신자1 | receiver1 | 120 | left | 이름+연락처 |
| 5 | 수신자2 | receiver2 | 120 | left | |
| 6 | 수신자3 | receiver3 | 120 | left | |

### 버튼
- 조회, 초기화, 저장

---

---

# 5. 저널관리 (JOURNAL)

---

## JRN-001. 저널 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | JRN-001 |
| **화면명** | 저널 조회 |
| **메뉴경로** | 저널관리 > 저널조회 |
| **난이도** | 중 |
| **NH AS-IS** | 4000 |

### 화면 설명
에이전트가 수집한 전체 저널 데이터를 통합 조회. 롤방식/리스트 보기 지원.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 3 | 저널발생일 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 4 | 저널구분 | journalType | select | N | 전체 | 일반/수표/개국/합계/운영 |
| 5 | 거래결과 | transResult | select | N | 전체 | 정상/불능/취소정상/취소불능/확인요망 |
| 6 | 거래구분 | transDiv | select | N | 전체 | 종속: 저널구분에 따라 변경 |
| 7 | 거래매체 | transMedium | select | N | 전체 | 종속: 거래구분에 따라 변경 |
| 8 | 일련번호범위 | seqStart~seqEnd | inputRange | N | | 최대 4자리 |

### 화면 구성 (좌우 분할)

**좌측: 저널 목록 테이블 + 페이징**

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 발생일시 | journalDatetime | 150 | center | |
| 3 | 사무소명 | branchName | 120 | left | |
| 4 | 기기번호 | deviceId | 80 | center | |
| 5 | 일련번호 | seqNo | 60 | center | |
| 6 | 저널구분 | journalTypeName | 80 | center | |
| 7 | 거래구분 | transDivName | 80 | center | |
| 8 | 거래금액 | amount | 120 | right | |
| 9 | 거래결과 | transResult | 80 | center | 뱃지 |

**우측: 저널 상세 정보 (고정 너비 300px)**
- 선택 행의 상세 저널 데이터 표시
- 탭: 기본정보 / 저널원문

### 버튼
- 조회, 초기화, 엑셀, 롤방식, 출력, 거래이미지

---

## JRN-002. 저널관리모드 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | JRN-002 |
| **화면명** | 저널관리모드 조회 |
| **메뉴경로** | 저널관리 > 저널관리모드조회 |
| **난이도** | 중 |

### 화면 설명
운영모드(기술자모드/운영자모드) 전환 이력과 운영률을 조회. Operation(52) 저널 기반.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 3 | 기준일 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 4 | 모드구분 | modeType | select | N | 전체 | 기술자/운영자 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 전환일시 | modeDatetime | 150 | center | |
| 3 | 사무소명 | branchName | 120 | left | |
| 4 | 기기번호 | deviceId | 80 | center | |
| 5 | 전환모드 | modeName | 100 | center | 기술자/운영자 |
| 6 | 모드시작 | modeStartTime | 130 | center | |
| 7 | 모드종료 | modeEndTime | 130 | center | |
| 8 | 지속시간(분) | durationMin | 80 | right | |

### 버튼
- 조회, 초기화, 엑셀

---

## JRN-003. 저널이미지 조회이력

| 항목 | 내용 |
|------|------|
| **화면ID** | JRN-003 |
| **화면명** | 저널이미지 조회이력 |
| **메뉴경로** | 저널관리 > 저널이미지조회이력 |
| **난이도** | 중 |

### 화면 설명
저널 이미지(명세표 스캔 등)를 조회한 이력을 감사 목적으로 기록/조회.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 조회일자 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 2 | 조회자 | searchUser | input | N | | 사번 또는 이름 |
| 3 | 기기번호 | deviceId | input | N | | |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 조회일시 | viewDatetime | 150 | center | |
| 3 | 조회자 | viewUser | 100 | center | 사번+이름 |
| 4 | 소속 | department | 120 | left | |
| 5 | 조회대상기기 | targetDeviceId | 80 | center | |
| 6 | 저널일시 | journalDatetime | 150 | center | |
| 7 | 저널구분 | journalType | 80 | center | |
| 8 | 조회사유 | viewReason | 200 | left | |

### 버튼
- 조회, 초기화, 엑셀

---

## JRN-004. 저널이미지 권한관리

| 항목 | 내용 |
|------|------|
| **화면ID** | JRN-004 |
| **화면명** | 저널이미지 권한관리 |
| **메뉴경로** | 저널관리 > 저널이미지권한관리 |
| **난이도** | 중 |
| **NH AS-IS** | 4002 신규 |

### 화면 설명
저널 이미지 열람 권한을 사용자별로 관리. 보안 목적.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사용자명 | userName | input | N | | |
| 2 | 사번 | userId | input | N | | |
| 3 | 권한상태 | authStatus | select | N | 전체 | 부여/미부여 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사번 | userId | 100 | center | |
| 3 | 사용자명 | userName | 100 | center | |
| 4 | 소속 | department | 150 | left | |
| 5 | 직급 | position | 80 | center | |
| 6 | 이미지조회권한 | imageAuth | 80 | center | 체크박스 |
| 7 | 부여일시 | grantDate | 130 | center | |
| 8 | 부여자 | grantUser | 100 | center | |

### 버튼
- 조회, 초기화, 권한부여, 권한해제

---

## JRN-005. 저널 2차파기 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | JRN-005 |
| **화면명** | 저널 2차파기 조회 |
| **메뉴경로** | 저널관리 > 저널2차파기조회 |
| **난이도** | 중 |
| **NH AS-IS** | 4005 신규 |

### 화면 설명
파기된 저널 데이터를 승인 절차 후 조회. CASH-006과 유사한 프로세스.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 저널발생일 | startDate~endDate | dateRangePicker | Y | | |
| 2 | 처리유형 | searchType | radio | Y | 기기번호 | ○기기번호 ○일련번호 |
| 3 | 기기번호 | deviceId | input | Y | | |
| 4 | 조회사유 | searchReason | select | Y | | CASH-006과 동일 |
| 5 | 상세사유 | detailReason | input | N | | |

### 그리드 컬럼
- CASH-006 그리드에서 계좌정보 대신 저널 정보(저널구분, 거래구분, 거래매체 등) 표시

### 특이사항
- CASH-006(거래내역 2차파기)과 동일한 승인 프로세스
- 조회 이력 자동 로깅

---

## JRN-006. 저널처리 현황

| 항목 | 내용 |
|------|------|
| **화면ID** | JRN-006 |
| **화면명** | 저널처리 현황 |
| **메뉴경로** | 저널관리 > 저널처리현황 |
| **난이도** | 중 |
| **NH AS-IS** | 4102~4105 통합 |

### 화면 설명
저널 수신/처리 현황 모니터링. 유형별 처리 건수, 결번 건수 등 관리자용.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 기준일 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 2 | 저널유형 | journalType | select | N | 전체 | APStart/Open/Status/... |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | 기준일 | baseDate | 100 | center | |
| 2 | 저널유형 | journalTypeName | 120 | center | |
| 3 | 수신건수 | receivedCount | 80 | right | |
| 4 | 처리건수 | processedCount | 80 | right | |
| 5 | 오류건수 | errorCount | 80 | right | 0 아니면 빨간색 |
| 6 | 결번건수 | missingCount | 80 | right | 0 아니면 빨간색 |
| 7 | 처리율 | processRate | 80 | right | % |

### 버튼
- 조회, 초기화, 엑셀

---

---

# 6. 기기관리 (DEV)

---

## DEV-001. 본지점 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | DEV-001 |
| **화면명** | 본지점 조회 |
| **메뉴경로** | 기기관리 > 본지점조회 |
| **난이도** | 하 |

### 화면 설명
농협은행 사무소(지점) 정보를 조회/관리.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 지점구분 | branchType | select | N | 전체 | 공통코드 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소코드 | branchCode | 80 | center | |
| 3 | 사무소명 | branchName | 150 | left | |
| 4 | 지점구분 | branchTypeName | 80 | center | |
| 5 | 우편번호 | zipCode | 80 | center | |
| 6 | 주소 | address | 250 | left | |
| 7 | 전화번호 | phone | 120 | center | |
| 8 | 팩스번호 | fax | 120 | center | |

### 버튼
- 조회, 초기화, 엑셀

---

## DEV-002. 코너정보 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | DEV-002 |
| **화면명** | 코너정보 조회 |
| **메뉴경로** | 기기관리 > 코너정보조회 |
| **난이도** | 중 |
| **NH AS-IS** | 3000 |

### 화면 설명
ATM 설치 코너(장소) 정보를 조회. 운영시간, 경비업체, 용역업체 등 포함.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 코너상태 | cornerStatus | select | N | 전체 | 운영/폐쇄 |
| 3 | 코너형태 | cornerType | select | N | 전체 | 공통코드(dvsnCd:016) |
| 4 | 운영구분 | operationType | select | N | 전체 | |
| 5 | 설치장소 | installPlace | select | N | 전체 | 점내/점외 |
| 6 | 용역업체 | serviceProvider | select | N | 전체 | |
| 7 | 협력업체 | cooperator | select | N | 전체 | |
| 8 | 경비업체 | securityCompany | select | N | 전체 | |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소명 | branchName | 120 | left | |
| 3 | 코너명 | cornerName | 120 | left | |
| 4 | 코너형태 | cornerTypeName | 80 | center | |
| 5 | 위치구분 | installPlace | 80 | center | 점내/점외 |
| 6 | 운영시간 | serviceTime | 100 | center | |
| 7 | 365코너 | is365 | 60 | center | Y/N |
| 8 | 경비업체 | securityCompanyName | 100 | left | |
| 9 | 용역업체 | serviceProviderName | 100 | left | |
| 10 | 설치주소 | address | 200 | left | |

### 버튼
- 조회, 초기화, 엑셀

### 상세 (행 클릭 시)
- 코너 상세 정보 모달: 코너 편집, 기기 목록 표시

---

## DEV-003. 기기정보 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | DEV-003 |
| **화면명** | 기기정보 조회 |
| **메뉴경로** | 기기관리 > 기기정보조회 |
| **난이도** | 중 |
| **NH AS-IS** | 3201 |

### 화면 설명
ATM 기기 마스터 정보를 관리. IP, 기종, 카세트, 설치장소 등 자산 관리 목적.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | IP주소 | ipAddress | input | N | | maxLength:15 |
| 3 | 기기업체 | atmVendor | cascadeSelect | N | 전체 | 하위: 모델명 |
| 4 | 모델명 | modelCode | cascadeSelect | N | 전체 | |
| 5 | 기종 | deviceType | select | N | 전체 | |
| 6 | 운영구분 | operationType | select | N | 전체 | |
| 7 | 협력업체 | cooperator | select | N | 전체 | |
| 8 | 경비업체 | securityCompany | select | N | 전체 | |
| 9 | 기기상태 | deviceStatus | select | N | 전체 | 운영/폐기/미설치 |
| 10 | 기능체크 | features | checkbox | N | | ☑5만원권 ☑IC카드 ☑RF ☑EMV ☑장애인지원 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사무소명 | branchName | 120 | left | |
| 3 | 코너명 | cornerName | 100 | left | |
| 4 | 기기번호 | deviceId | 80 | center | |
| 5 | 기기업체 | vendorName | 80 | center | |
| 6 | 기종 | deviceType | 80 | center | |
| 7 | 모델명 | modelName | 100 | left | |
| 8 | IP주소 | ipAddress | 120 | center | |
| 9 | 설치장소 | installPlace | 120 | left | |
| 10 | 기기상태 | deviceStatus | 80 | center | |
| 11 | 최종거래일시 | lastTransDate | 130 | center | |

### 버튼
- 조회, 초기화, 엑셀, 신규등록, 수정

### 상세 (모달)
- 기기 상세 등록/수정 폼
- 카세트 정보 (카세트번호, 권종, 용량)
- 설치 이력

---

---

# 7. 원격관리 (REMOTE)

---

## RMT-001. 원격제어

| 항목 | 내용 |
|------|------|
| **화면ID** | RMT-001 |
| **화면명** | 원격제어 |
| **메뉴경로** | 원격관리 > 원격제어 |
| **난이도** | 상 |

### 화면 설명
ATM 기기에 원격 명령을 전송. 6개 탭으로 구분된 제어 기능.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | Y | | |
| 2 | 기기번호 | deviceId | cascadeSelect | Y | | |
| 3 | 타임아웃 | timeout | select | N | 5분 | 1분~30분 |

### 탭 구성

| 탭 | 기능 |
|----|------|
| 원격제어 | 기기 재부팅, 서비스 시작/정지 등 기본 제어 |
| 파일수집 | 로그파일 수집 |
| 메시지 | 기기 화면 메시지 전송 |
| 이미지 | 기기 화면 캡처 |
| 캡쳐 | CCTV/카메라 이미지 수집 |
| CMD | 명령어 직접 실행 |

### 선택 기기 정보 표시

| 항목 | 내용 |
|------|------|
| 기기명 | (자동 표시) |
| 코너 | (자동 표시) |
| 기기번호 | (자동 표시) |
| 업체명 | (자동 표시) |
| 모델명 | (자동 표시) |

### 버튼 (탭별)
- 실행, 취소

### 특이사항
- 원격 명령 실행 전 확인 모달 필수
- 실행 결과 실시간 표시
- 권한: 관리자급만 접근 가능

---

## RMT-002~003. 개별/일괄 다운로드 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | RMT-002, RMT-003 |
| **화면명** | 개별/일괄 다운로드 관리 |
| **메뉴경로** | 원격관리 > 개별다운로드관리 / 일괄다운로드관리 |
| **난이도** | 중 |
| **NH AS-IS** | 7204 |

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | RMT-002만 |
| 3 | 배포상태 | deployStatus | select | N | 전체 | 예약/진행/완료/실패 |
| 4 | 등록일 | startDate~endDate | dateRangePicker | N | | |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 파일명 | fileName | 200 | left | |
| 3 | 버전 | version | 80 | center | |
| 4 | 대상기기수 | targetCount | 80 | right | RMT-003만 |
| 5 | 완료수 | completeCount | 80 | right | |
| 6 | 실패수 | failCount | 80 | right | |
| 7 | 상태 | deployStatus | 80 | center | 뱃지 |
| 8 | 등록일시 | registerDate | 130 | center | |
| 9 | 등록자 | registerUser | 80 | center | |

### 버튼
- 조회, 초기화, 신규등록, 엑셀

---

## RMT-004~005. 원격이력/다운로드이력 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | RMT-004, RMT-005 |
| **화면명** | 원격이력 조회, 다운로드이력 조회 |
| **메뉴경로** | 원격관리 > 원격이력조회 / 다운로드이력조회 |
| **난이도** | 하 |

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 기기번호 | deviceId | cascadeSelect | N | 전체 | |
| 3 | 기준일 | startDate~endDate | dateRangePicker | Y | 최근7일 | |
| 4 | 기기IP | ipAddress | input | N | | RMT-004만 |
| 5 | 원격명령 | remoteCommand | select | N | 전체 | RMT-004만 |
| 6 | 기기업체 | atmVendor | cascadeSelect | N | 전체 | |
| 7 | 모델명 | modelCode | cascadeSelect | N | 전체 | |

### 그리드 컬럼 (RMT-004)

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 실행일시 | executeDatetime | 150 | center | |
| 3 | 사무소명 | branchName | 120 | left | |
| 4 | 기기번호 | deviceId | 80 | center | |
| 5 | 원격명령 | remoteCommand | 120 | center | |
| 6 | 결과 | result | 80 | center | 성공/실패 뱃지 |
| 7 | 실행자 | executeUser | 80 | center | |
| 8 | 결과메시지 | resultMessage | 200 | left | |

### 버튼
- 조회, 초기화, 엑셀

---

## RMT-006. 개국/합계등록

| 항목 | 내용 |
|------|------|
| **화면ID** | RMT-006 |
| **화면명** | 개국/합계등록 |
| **메뉴경로** | 원격관리 > 개국/합계등록 |
| **난이도** | 상 |
| **NH AS-IS** | 2000 |

### 화면 설명
원격으로 ATM 기기에 개국/합계 명령을 전송하는 제어 화면.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | Y | | |
| 2 | 기기번호 | deviceId | cascadeSelect | Y | | |

### 정보 표시

| 항목 | 내용 |
|------|------|
| 합계가능시각 | 16:00 (서버 설정값) |
| 현서버시각 | 실시간 표시 |

### 처리구분 (라디오)

| 값 | 라벨 | 설명 |
|----|------|------|
| FORCE_OPEN | ○ 강제개국등록 | 기기 강제 개국 |
| FORCE_TOTAL | ○ 강제합계등록 | 기기 강제 합계 |
| TOTAL_OPEN | ○ 원격합계등록후 기기 OPEN | 합계 후 재개국 |
| TOTAL_DOWN | ○ 원격합계등록후 기기 DOWN | 합계 후 정지 |
| CANCEL_STOP | ○ 사용중지해제 | 중지 상태 해제 |

### 실행결과 표시

| 항목 | 내용 |
|------|------|
| 실행일시 | |
| 사무소 | |
| 기기번호 | |
| 실행결과 | 성공/실패 + 메시지 |

### 버튼
- 실행 (확인 모달 후)

### 특이사항
- 합계가능시각(16시) 이전에는 합계 등록 경고
- 실행 전 반드시 확인 모달
- 권한: 책임자급 이상

---

## RMT-007. 합계/폐국예약

| 항목 | 내용 |
|------|------|
| **화면ID** | RMT-007 |
| **화면명** | 합계/폐국예약 |
| **메뉴경로** | 원격관리 > 합계/폐국예약 |
| **난이도** | 중 |
| **NH AS-IS** | 3001 |

### 화면 설명
기기별 합계 시간, 폐국 시간을 예약 설정하는 화면.

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 코너 | cornerCode | cascadeSelect | N | 전체 | |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 기기번호 | deviceId | 80 | center | |
| 3 | 기종 | deviceType | 80 | center | |
| 4 | 코너명 | cornerName | 120 | left | |
| 5 | 설치위치 | installPlace | 120 | left | |
| 6 | 폐국예약 | closingReserved | 80 | center | Y/N 토글 |
| 7 | 폐국예약시간 | closingTime | 80 | center | HH:mm |
| 8 | 합계예약 | totalReserved | 80 | center | Y/N 토글 |
| 9 | 합계예약시간 | totalTime | 80 | center | HH:mm |
| 10 | 합계후지시 | afterTotalAction | 80 | center | OPEN/DOWN |

### 버튼
- 조회, 초기화, 저장

### 특이사항
- 그리드 인라인 편집 (예약시간 직접 입력)
- 일괄 설정 기능 (선택한 기기들에 동일 시간 적용)

---

---

# 8. 정보관리 (INFO)

---

## INFO-001. 사용자 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | INFO-001 |
| **화면명** | 사용자 관리 |
| **메뉴경로** | 정보관리 > 사용자관리 |
| **난이도** | 중 |

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 사무소 | branchCode | cascadeSelect | N | 전체 | |
| 2 | 사용자명 | userName | input | N | | |
| 3 | 사번 | userId | input | N | | |
| 4 | 사용여부 | useYn | select | N | Y | Y/N |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 사번 | userId | 100 | center | |
| 3 | 사용자명 | userName | 100 | center | |
| 4 | 소속 | department | 150 | left | |
| 5 | 직급 | position | 80 | center | |
| 6 | 연락처 | phone | 120 | center | |
| 7 | 이메일 | email | 150 | left | |
| 8 | 역할 | roleName | 100 | center | |
| 9 | 사용여부 | useYn | 60 | center | Y/N |
| 10 | 최종접속일 | lastLoginDate | 130 | center | |

### 버튼
- 조회, 초기화, 신규등록, 수정, 삭제

---

## INFO-002. 권한 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | INFO-002 |
| **화면명** | 권한 관리 |
| **메뉴경로** | 정보관리 > 권한관리 |
| **난이도** | 상 |

### 화면 구성 (좌우 분할)

**좌측: 역할 목록**

| 순서 | 컬럼명 | dataIndex | width |
|------|--------|-----------|-------|
| 1 | 역할코드 | roleCode | 80 |
| 2 | 역할명 | roleName | 150 |
| 3 | 설명 | description | 200 |

**우측: 선택 역할의 메뉴 권한 (트리 + 체크박스)**
- 메뉴 트리 구조 표시
- 각 메뉴에 대해: ☑조회 ☑등록 ☑수정 ☑삭제 ☑출력 권한 체크

### 버튼
- 역할 추가, 역할 삭제, 권한 저장

---

## INFO-003. 메뉴 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | INFO-003 |
| **화면명** | 메뉴 관리 |
| **메뉴경로** | 정보관리 > 메뉴관리 |
| **난이도** | 상 |

### 화면 구성 (좌우 분할)

**좌측: 메뉴 트리**
- 대메뉴 > 중메뉴 > 소메뉴 트리 구조
- 드래그&드롭으로 순서 변경

**우측: 메뉴 상세 (선택 메뉴)**

| 필드 | 타입 | 설명 |
|------|------|------|
| 메뉴코드 | input (readonly) | 자동생성 |
| 메뉴명 | input | |
| 상위메뉴 | select | |
| URL | input | 라우트 경로 |
| 아이콘 | select | Ant Design 아이콘 |
| 정렬순서 | input (number) | |
| 사용여부 | select | Y/N |

### 버튼
- 추가, 수정, 삭제, 저장

---

## INFO-004. 공통코드 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | INFO-004 |
| **화면명** | 공통코드 관리 |
| **메뉴경로** | 정보관리 > 코드관리 |
| **난이도** | 중 |

### 화면 구성 (좌우 분할)

**좌측: 그룹코드 목록**

| 순서 | 컬럼명 | dataIndex | width |
|------|--------|-----------|-------|
| 1 | 그룹코드 | groupCode | 80 |
| 2 | 그룹명 | groupName | 150 |
| 3 | 사용 | useYn | 60 |

**우측: 선택 그룹의 상세코드 목록**

| 순서 | 컬럼명 | dataIndex | width |
|------|--------|-----------|-------|
| 1 | 코드 | code | 80 |
| 2 | 코드명 | codeName | 150 |
| 3 | 정렬순서 | sortOrder | 60 |
| 4 | 비고 | remark | 200 |
| 5 | 사용 | useYn | 60 |

### 버튼
- 조회, 그룹코드 추가, 상세코드 추가, 수정, 삭제, 저장

---

## INFO-005~007. 거래코드/기기모델/업체 관리

간단한 CRUD 화면. 동일 패턴.

| 화면ID | 화면명 | 주요 필드 |
|--------|--------|----------|
| INFO-005 | 거래코드 관리 | 거래코드, 거래명, 거래유형, 사용여부 |
| INFO-006 | 기기모델 관리 | 모델코드, 모델명, 업체, 기종, 카세트수, 사용여부 |
| INFO-007 | 업체 관리 | 업체코드, 업체명, 업체유형(용역/경비/기기), 연락처, 주소, 사용여부 |

---

## INFO-008. 공지사항 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | INFO-008 |
| **화면명** | 공지사항 관리 |
| **메뉴경로** | 정보관리 > 게시물관리 > 공지사항 |
| **난이도** | 중 |

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 등록일 | startDate~endDate | dateRangePicker | N | | |
| 2 | 제목/내용 | keyword | input | N | | 키워드 검색 |
| 3 | 공지유형 | noticeType | select | N | 전체 | 일반/긴급/시스템 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 유형 | noticeTypeName | 80 | center | 뱃지 |
| 3 | 제목 | title | 300 | left | 링크 |
| 4 | 첨부 | hasAttachment | 40 | center | 📎 아이콘 |
| 5 | 등록자 | registerUser | 80 | center | |
| 6 | 등록일 | registerDate | 100 | center | |
| 7 | 조회수 | viewCount | 60 | right | |

### 상세/등록 (새 화면 or 모달)
- 제목, 공지유형, 내용(에디터), 첨부파일, 공지기간(시작~종료)
- 첨부파일 업로드/다운로드

### 버튼
- 조회, 초기화, 등록, 수정, 삭제

---

---

# 9. 시스템관리 (SYS)

---

## SYS-001. 접속이력 조회

| 항목 | 내용 |
|------|------|
| **화면ID** | SYS-001 |
| **화면명** | 접속이력 조회 |
| **메뉴경로** | 시스템관리 > 접속이력조회 |
| **난이도** | 하 |

### 조회 조건

| 순서 | 필드명 | 필드ID | 컴포넌트 | 필수 | 기본값 | 비고 |
|------|--------|--------|---------|------|--------|------|
| 1 | 접속일 | startDate~endDate | dateRangePicker | Y | 오늘 | |
| 2 | 사용자 | userId | input | N | | |
| 3 | 접속IP | accessIp | input | N | | |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 접속일시 | accessDatetime | 150 | center | |
| 3 | 사번 | userId | 80 | center | |
| 4 | 사용자명 | userName | 80 | center | |
| 5 | 접속IP | accessIp | 120 | center | |
| 6 | 접속유형 | accessType | 80 | center | 로그인/로그아웃 |
| 7 | 결과 | result | 80 | center | 성공/실패 뱃지 |
| 8 | 실패사유 | failReason | 150 | left | |

### 버튼
- 조회, 초기화, 엑셀

---

## SYS-002. 시스템 설정

| 항목 | 내용 |
|------|------|
| **화면ID** | SYS-002 |
| **화면명** | 시스템 설정 |
| **메뉴경로** | 시스템관리 > 환경변수 |
| **난이도** | 중 |
| **NH AS-IS** | 4100 |

### 화면 구성
키-값 형태의 환경변수 설정.

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | 그룹 | configGroup | 100 | center | |
| 2 | 설정키 | configKey | 200 | left | |
| 3 | 설정값 | configValue | 200 | left | 인라인 편집 |
| 4 | 설명 | description | 250 | left | |
| 5 | 수정일 | updateDate | 130 | center | |

### 버튼
- 조회, 저장

---

## SYS-003. 배치작업 관리

| 항목 | 내용 |
|------|------|
| **화면ID** | SYS-003 |
| **화면명** | 배치작업 관리 |
| **메뉴경로** | 시스템관리 > 배치작업관리 |
| **난이도** | 중 |

### 그리드 컬럼

| 순서 | 컬럼명 | dataIndex | width | 정렬 | 비고 |
|------|--------|-----------|-------|------|------|
| 1 | No | rowNum | 60 | center | |
| 2 | 배치명 | batchName | 200 | left | |
| 3 | 실행주기 | cronExpression | 120 | center | cron 식 |
| 4 | 마지막실행 | lastExecuteDate | 150 | center | |
| 5 | 다음실행 | nextExecuteDate | 150 | center | |
| 6 | 상태 | status | 80 | center | 활성/비활성 토글 |
| 7 | 결과 | lastResult | 80 | center | 성공/실패 뱃지 |

### 버튼
- 조회, 즉시실행, 활성/비활성, 편집

---

---

# 부록

---

## 부록A. 화면 전체 목록 (요약)

| 도메인 | ID | 화면명 | 난이도 | 조회조건수 | 그리드컬럼수 |
|--------|-----|--------|--------|-----------|------------|
| 대시보드 | DASH-001 | 메인 대시보드 | 상 | - | 위젯 5개 |
| 시재 | CASH-001 | 거래내역 조회 | 중 | 8 | 13+α |
| 시재 | CASH-002 | 투입내역 조회 | 중 | 8 | 12 |
| 시재 | CASH-003 | 수표거래 조회 | 중 | 8 | 13 |
| 시재 | CASH-004 | 기기시재 조회 | 중 | 8 | 13 |
| 시재 | CASH-005 | 운영자금 예측 | 상 | 4 | 9 |
| 시재 | CASH-006 | 거래내역 2차파기 | 중 | 5 | 13 |
| 마감 | CLOSE-001 | 집계내역/합계상태 | 중 | 2 | 좌우분할 |
| 마감 | CLOSE-002 | 마감 전자결재 | 상 | 2 | 특수 |
| 마감 | CLOSE-003 | 마감내역 조회 | 중 | 4 | 10 |
| 마감 | CLOSE-004 | 미처리내역 조회 | 중 | 3 | 7 |
| 장애 | FAULT-001 | 기기상태 조회 | 상 | 10 | 14+탭 |
| 장애 | FAULT-002 | 출동조치 관리 | 상 | 6 | 12 |
| 장애 | FAULT-003 | 장애내역 조회 | 중 | 9 | 14 |
| 장애 | FAULT-004 | 장애/가동률 조회 | 중 | 7 | 10 |
| 장애 | FAULT-005 | 장애코드 관리 | 중 | 4 | 9 |
| 장애 | FAULT-006 | 장애명세표 조회 | 중 | 3 | 7 |
| 장애 | FAULT-007 | 환경설정 | 중 | 2 | 6 |
| 저널 | JRN-001 | 저널 조회 | 중 | 8 | 9+상세 |
| 저널 | JRN-002 | 저널관리모드 조회 | 중 | 4 | 8 |
| 저널 | JRN-003 | 저널이미지 조회이력 | 중 | 3 | 8 |
| 저널 | JRN-004 | 저널이미지 권한관리 | 중 | 3 | 8 |
| 저널 | JRN-005 | 저널 2차파기 조회 | 중 | 5 | CASH-006 유사 |
| 저널 | JRN-006 | 저널처리 현황 | 중 | 2 | 7 |
| 기기 | DEV-001 | 본지점 조회 | 하 | 2 | 8 |
| 기기 | DEV-002 | 코너정보 조회 | 중 | 8 | 10 |
| 기기 | DEV-003 | 기기정보 조회 | 중 | 10 | 11 |
| 원격 | RMT-001 | 원격제어 | 상 | 3 | 특수 |
| 원격 | RMT-002 | 개별다운로드 관리 | 중 | 4 | 9 |
| 원격 | RMT-003 | 일괄다운로드 관리 | 중 | 3 | 9 |
| 원격 | RMT-004 | 원격이력 조회 | 하 | 7 | 8 |
| 원격 | RMT-005 | 다운로드이력 조회 | 하 | 5 | 8 |
| 원격 | RMT-006 | 개국/합계등록 | 상 | 2 | 특수(제어) |
| 원격 | RMT-007 | 합계/폐국예약 | 중 | 2 | 11 |
| 정보 | INFO-001 | 사용자 관리 | 중 | 4 | 10 |
| 정보 | INFO-002 | 권한 관리 | 상 | - | 좌우분할+트리 |
| 정보 | INFO-003 | 메뉴 관리 | 상 | - | 좌우분할+트리 |
| 정보 | INFO-004 | 공통코드 관리 | 중 | - | 좌우분할 |
| 정보 | INFO-005 | 거래코드 관리 | 하 | 2 | 4 |
| 정보 | INFO-006 | 기기모델 관리 | 하 | 2 | 6 |
| 정보 | INFO-007 | 업체 관리 | 하 | 2 | 6 |
| 정보 | INFO-008 | 공지사항 관리 | 중 | 3 | 7 |
| 시스템 | SYS-001 | 접속이력 조회 | 하 | 3 | 8 |
| 시스템 | SYS-002 | 시스템 설정 | 중 | - | 5 |
| 시스템 | SYS-003 | 배치작업 관리 | 중 | - | 7 |

**총 44개 화면** (대시보드 1 + 시재 6 + 마감 4 + 장애 7 + 저널 6 + 기기 3 + 원격 7 + 정보 8 + 시스템 3)

---

## 부록B. 공통코드 목록 (화면에서 사용)

화면 조회조건의 select 컴포넌트에서 사용하는 공통코드:

| 코드그룹 | 코드명 | 사용 화면 |
|---------|--------|----------|
| CORNER_TYPE | 코너형태 | CASH-001~004, FAULT-001, DEV-002 등 |
| OPER_TYPE | 운영구분 | CASH-001~004, FAULT-001, DEV-002~003 등 |
| TRANS_TYPE | 거래유형 | CASH-001 |
| TRANS_DIV | 거래구분 | CASH-003, JRN-001 |
| DEVICE_STATUS | 기기상태 | FAULT-001, DEV-003 |
| FAULT_DIV | 장애구분 | FAULT-003 |
| DEVICE_CATEGORY | 장치분류 | FAULT-003, FAULT-005 |
| MANAGE_TYPE | 관리구분 | CASH-004 |
| BUSINESS_TYPE | 업무구분 | CASH-002 |
| CLOSING_STATUS | 마감상태 | CLOSE-003 |
| UNPROCESSED_TYPE | 미처리유형 | CLOSE-004 |
| JOURNAL_TYPE | 저널구분 | JRN-001, JRN-006 |
| TRANS_RESULT | 거래결과 | JRN-001 |
| CORNER_STATUS | 코너상태 | DEV-002 |
| INSTALL_PLACE | 설치장소 | DEV-002 |
| DEPLOY_STATUS | 배포상태 | RMT-002~003 |
| NOTICE_TYPE | 공지유형 | INFO-008 |
| SEARCH_REASON | 조회사유 | CASH-006, JRN-005 |

---

## 부록C. 계층 종속 API

| API | 파라미터 | 반환 | 사용처 |
|-----|---------|------|--------|
| `GET /api/code/branch` | - | 사무소 목록 | 전 화면 |
| `GET /api/code/corner` | branchCode | 코너 목록 | 사무소→코너 |
| `GET /api/code/device` | branchCode, cornerCode | 기기 목록 | 코너→기기 |
| `GET /api/code/model` | vendorCode | 모델 목록 | 기기업체→모델 |
| `GET /api/code/common` | groupCode | 공통코드 목록 | select 컴포넌트 |
