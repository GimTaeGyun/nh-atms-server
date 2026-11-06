# NH ATMS AS-IS 전수분석 및 TO-BE 테이블 설계 (종합)

> 분석 대상
> - ERD 이미지 11장: `0.기본자료/농협은행/1.DB/1-1-1 ~ 2-2-3.png` (AS-IS 96개 테이블)
> - 화면 이미지 약 250장 + 화면트리 4장: `0.수신문서/Asis화면/*.png` (AS-IS 179개 화면)
> - 기존 문서: `NH AS-IS 테이블 분석.md`, `09_메뉴별_테이블_매핑_및_추가설계.md`
> 작성일: 2026-06-02
> DB: Oracle (ATMSPDB) / 컨벤션: `T_` 접두어 + UPPER SNAKE_CASE, 한글 COMMENT, FK 논리참조
>
> ⚠️ **현행 배포 정본은 `11_최종테이블설계서.md` 참조** — 운영 DB에 실제 존재하는 테이블은 48종이며,
> 본 문서의 "생성 완료 80" 는 누적 설계 기준(미배포 포함)임.

---

## 0. 요약

| 구분 | 수량 | 비고 |
| --- | --- | --- |
| AS-IS 테이블 (ERD) | 96 | `NH AS-IS 테이블 분석.md` 기준 |
| AS-IS 화면 (화면트리) | 179 | UI빌더 트리 4장에서 추출 |
| TO-BE 생성 완료 (V1.0.0~V1.2.8) | 80 | 아래 §2 |
| TO-BE 추가 설계 (V1.2.9) | 10 | TO-BE 메뉴 직접연관 미커버 거래/상태/마스터 |
| TO-BE 추가 설계 (V1.3.0) | 8 | 일괄용역/SLA(6200번대) 평가관리 |
| 제외(TO-BE 범위 밖) | ~15 | CPC, 방카슈랑스, 타사카드론, SMS홍보, Raw Data 등 |

---

## 1. AS-IS 화면 트리 (179개) — 메뉴 그룹 구조

> 출처: `Asis화면/메뉴1~4.png` (UI빌더 화면 트리)

### 1.1 거래내역 (49)
- **거래확인 (47)**: 1000 특정(건별)조회 · 1001 입금/출금 · 1002 이체(연동) · 1003 공과금/납부 · 1004 신용카드 · 1005 수표입출금 · 1006 무통장/무카드 · 1007 통장기장 · 1008 외화입출금 · 1020 거래내역 2차파기조회 · 1091 1차파기대상 조회 · 1099 파기정보 보호조치자료 · 1300 전체거래조회(Admin) · 1301 거래내역 2차파기 · 1501 외화입출금
  - 상세화면: 9101 출금 · 9102 입금 · 9103 서비스 · 9104 공과금수납 · 9105 아파트관리비 · 9106 지로공과금 · 9107 지방세납 · 9108 자행이체 · 9109 타행이체 · 9110 CMS이체 · 9111 선결제/청구대금/농협카드 · 9112 국고/2D납부 · 9113 전자화폐 · 9114 보험계약대출 · 9115 통장기장 · 9116 통합지방세/서울시 · 9117 대출금납부 · 9118 방카슈랑스 · 9119 소송인지대/송달료 · 9120 타행입금 · 9121 기프트카드 · 9122 카드거래선조회 · 9123 외화거래 · 9190 개인신용정보 이력 · 9191 개인신용정보 출력사유 · 9198 에러내용 · 9199 상세화면(Default)
- **거래상태별 집계**: 9200 거래상태별 집계 · 9201 수표입금 집계 · 9202 수표출금 집계 · 9210 출납과잉금 조치메모 · 9501 외화거래
- **자금반환확인 (1)**: 12000 자금반환 등록내역
- **필수출력자료 (1)**: 11000 출납과잉금 예상거래/자금반환

### 1.2 마감처리 (7)
- **개국/합계 (3)**: 2000 개국/합계 등록 · 2101 집계내역/합계상태 · 2501 외화합계상태
- **마감절차 (4)**: 2102 외화합계상태 · 2200 마감장표 전자결재(ATM) · 2201 일마감 전자결재 · 2301 장애명세표 조회

### 1.3 일괄용역 (38) — SLA
- **SLA (15)**: 6200 용역업체 SLA등급조회 · 6201 SLA등급 이력 · 6202 코너SLA평가점수 · 6203 기기별가동율 이력 · 6204 기기별장애이력 · 6205 코너환경평가관리 · 6206 C/S평가관리 · 6207 C/SO이력관리 · 6208 평가기준관리 · 6209 P&등급관리 · 6210 미가동사유관리 · 6211 용역기준관리 · 6212 용역료 산정 · 6213 미가동이력조회 · 9601 도착시간수정
- (그 외 보고서/시재/장애 항목은 아래 그룹과 중복)

### 1.4 시재관리 (8)
- 6100 시재조회 · 6101 마감내역조회 · 6102 미처리내역조회 · 6103 용역자금대사 · 6104 운영자금대사 · 6105 정규현송계획내역 · 6106 추가현송내역 · 6107 반환금액상세

### 1.5 장애관리 (15)
- 6000 기기상태조회 · 6001 기기상태이력 · 6002 업체출동현황 · 6003 개국및거래현황 · 6004 2차출동현황 · 6300 용역업체직원조회 · 6301 업체간 전문송수신이력 · 6302 설치코너관리 · 6303 용역업체등록관리 · 6304 전산비용 청구액 산정 · 9630 코너정보관리 · 9631 한시적등록 · 9632 자동화기 정보 · 9633 긴급승인 · 9634 코너위치정보 수정

### 1.6 저널관리 (17)
- **운영 (7)**: 4001 저널업무개시현황 · 4100 업무개시현황 · 4101 운영환경설정 · 4102 결번요청 · 4103 운영로그요청 · 4104 저널처리유형별집계 · 4105 저널운영현황
- **저널 (7)**: 4000 저널조회 · 4002/4005 저널이미지 권한관리 · 4003/4006 저널이미지 열람기록 · 4020 저널 2차파기조회 · 9400 명세표 및 이미지
- **통계 (3)**: 4200 저널처리 일/월별건수 · 4201 결번건수 · 4202 운영모드 운영률

### 1.7 종합관리 (35)
- **영업점 등록사항 (12)**: 3000 설치코너관리 · 3001 합계/폐국예약 · 3002 알림설정 · 3003 우수고객관리 · 3004 고객권 사용유무 · 3005 MS거래제한설정 · 3006 수표설정 · 9300 코너정보관리 · 9301 한시적등록 · 9302 자동화기 정보 · 9303 긴급승인 · 9304 코너위치정보 수정
- **장애 (5)**: 3100 장애현황 · 3101 장애내역조회 · 3102 콜센터 장애조치현황 · 3103 장애통보현황 · 9311 장애내역 통보현황 상세
- **현황조회 (18)**: 0000 농협 자동화기기 · 3200 시재현황 · 3201 자금운영 · 3202 자산현황 · 3203 시재확인 · 3204 위치조회 · 3205 글로벌ATM/바이오 위치 · 3206 지역본부 담당자 · 3207 STM매체현황 · 3208 외국통화 시재현황 · 3209 외화ATM 시재요청·확인 · 3210 글로벌ATM현황 · 3500 외화ATM 시재현황 · 9001/9002 알림 · 9320 시재상세 · 9321 외화시재상세 · 9322 운영현황(자원정보)

### 1.8 통계 (12)
- **거래 (3)**: 5031 통계-시간별 · 5100 거래량/수수료 통계 · 5500 일괄용역기기 정보·통계
- **운영 (9)**: 5102 홍보관리 · 5130 전국운영현황 · 5160 가동률 · 5161 가동률 저하점 · 5162 휴일 가동률 · 5555 현금사용량 집계 · 5556 365기기현황(콜센터) · 5959 추석연휴 거래량(임시) · 9512 홍보관리 상세

### 1.9 게시판 (21)
- **게시정보 (3)**: 7000 공지사항 · 7001 업무연락 · 7002 명세표 에러사유
- **관리자기능 (15)**: 7100/7200 시스템 사용권한부여 · 7201 코드관리 · 7202 AriTAMS 코드관리 · 7203 에러메시지관리 · 7204 전문가져오기 · 7205 기기원격지시(매체배포) · 7206 버전리스트 · 7207 실시간 거래현황 · 7208 업체 코드관리 · 7209 라우터 서비스목록 · 7210 테이블관리 · 7227 재무회계 미완료 모니터링 · 9701/9702 권한부여 상세
- **영업점정보 (3)**: 8000 자동화면 표준 · 9998 일괄용역 표준레이아웃 · 9999 화면레이아웃

---

## 2. TO-BE 생성 완료 테이블 (80개, V1.0.0~V1.2.8)

| 분류 | 테이블 |
| --- | --- |
| 공통/인증 (V1.0.0) | T_USER_GROUP, T_USER, T_AUTH, T_USER_GROUP_AUTH, T_MENU, T_AUTH_MENU, T_LOGIN_HIST, T_USER_AUTH_HIST |
| 저널 기본 (V1.0.2/0.6/0.9) | T_JOURNAL, T_ATMMONITOR, T_TRANSACTIONJOURNAL, T_OPERATIONJOURNAL, T_JOURNAL_DATA, T_JOURNAL_ERROR, T_JOURNAL_FOOTNOTE, T_JOURNAL_IMAGE, T_JNLIMAGE_HISTORY, T_JOURNALDATA, T_JOURNALIMAGE |
| 집계/배치 (V1.0.2/1.1/1.2) | T_DAYTOTAL, T_ADDCASH, T_ADDCHECK, T_BATCHJOB, T_BATCHHISTORY, T_TRANSACTIONDAILYTOTAL, T_FAULTRATEMANAGEMENT, T_OPERATIONRATEMANAGEMENT |
| 에이전트/상태/IF (V1.1.0/1.3/1.4/1.7/1.8) | T_AGENTSTATUS, T_ATMUNITSTATUS, T_GWINTERFACE, T_SMSSENDDATA, T_NETSENDDATA, T_NETRECVDATA, T_FAULTMANAGEMENT, T_IF_QUEUE |
| 마스터 (V1.2.0/2.1) | T_BRANCH, T_CORNER, T_CODE, T_CODEGROUP, T_NOTICE, T_ATMGROUP, T_ATM, T_ATMSTATUS |
| 저널상세 (V1.2.2) | T_CASHTRANSACTION, T_CHECKTRANSACTION, T_CASSETTESTATUS, T_FAULTANALYSIS, T_ATMSTOPHISTORY |
| 통계집계 (V1.2.4) | T_TRANSACTIONHOURLYTOTAL, T_TRANSACTIONMONTHLYTOTAL, T_JOURNALPROCESSTOTAL, T_OPENCLOSESTATUS |
| 마스터확장 (V1.2.5) | T_ATMMODEL, T_VENDOR, T_VENDORATM, T_ERRORMESSAGE, T_HOLIDAY, T_FEE, T_DISPATCHHISTORY |
| 인사/권한/감사 (V1.2.6) | T_PERSON, T_JOURNALAUTH, T_JOURNALAUTHHIST, T_SYSTEMCONFIG, T_JOURNALVIEWLOG, T_PERSONALINFOPRINTHIST, T_DATADELETEHIST |
| 거래상세확장 (V1.2.7) | T_GIROTRANSACTION, T_FOREXTRANSACTION, T_PASSBOOKTRANSACTION, T_FOREXTOTAL, T_FILEATTACH, T_SLIPFORMAT, T_REMOTECOMMAND |
| AS-IS 미매핑 (V1.2.8) | T_VIPCUSTOMER, T_PROMOTION, T_OVERSEAREMITTANCE, T_DESTROYVIEWHIST, T_ACCIDENTREPORT, T_CASHSHORTAGE, T_EAPPROVALHIST, T_SLIPEDIT |

---

## 3. AS-IS 96개 테이블 → TO-BE 매핑 전수표

> 범례: ✅ 생성완료 · 🆕 추가설계(V1.2.9/1.3.0) · ⛔ TO-BE 범위 제외

| # | AS-IS 테이블 | TO-BE 테이블 | 상태 | 근거 화면 |
| --- | --- | --- | --- | --- |
| 1 | 자동화기기_거래 | T_TRANSACTIONJOURNAL + 유형별 상세 | ✅ | 1000~1008 |
| 2 | 전자화폐 | T_ECASHTRANSACTION | 🆕 V1.2.9 | 9113 |
| 3 | 타이(타행이체) | T_TRANSACTIONJOURNAL(공통) | ✅ | 9109 |
| 4 | 수표 거래 | T_CHECKTRANSACTION | ✅ | 1005/9201/9202 |
| 5 | 방카슈랑스상세내역 | — | ⛔ | 9118 (범위밖) |
| 6 | 카드론거래 | — | ⛔ | (범위밖) |
| 7 | 환전거래 | T_FOREXTRANSACTION | ✅ | 1008/9123 |
| 8 | VM환전 | T_FOREXTRANSACTION(공통) | ✅ | 1008 |
| 9 | 지로 | T_GIROTRANSACTION | ✅ | 1003/9106 |
| 10 | 지로납부 | T_GIROTRANSACTION | ✅ | 1003 |
| 11 | 공과금수납기지로내역 | T_GIROTRANSACTION | ✅ | 9104 |
| 12 | 공과금수납기거래합계 | T_GIROMONTHLYTOTAL | 🆕 V1.2.9 | 통계 |
| 13 | 해외송금관리 | T_OVERSEAREMITTANCE | ✅ | — |
| 14 | 자금반환 | T_FUNDRETURN | 🆕 V1.2.9 | 12000/11000 |
| 15 | 자금반환메모 | T_FUNDRETURNMEMO | 🆕 V1.2.9 | 12000 |
| 16 | 이중처리거래 | — | ⛔ | 내부처리 |
| 17 | 통장기장 | T_PASSBOOKTRANSACTION | ✅ | 1007/9115 |
| 18 | 통장가입정보 | T_PASSBOOKTRANSACTION(확장) | ✅ | 1007 |
| 19 | 재정지로납부 | T_GIROTRANSACTION | ✅ | 9107 |
| 20 | 통신료상세내역 | T_GIROTRANSACTION(공통) | ✅ | 1003 |
| 21 | 시재부족관리거래 | T_CASHSHORTAGE | ✅ | 6100 |
| 22 | 1차파기거래조회기록 | T_DESTROYVIEWHIST | ✅ | 1091 |
| 23 | 고액권수표사용관리 | T_CHECKTRANSACTION(공통) | ✅ | 3006 |
| 24 | 검합성처리 | — | ⛔ | 내부처리 |
| 25 | 사무소등록관리 | T_BRANCH + T_CORNER | ✅ | 3000 |
| 26 | 사무소경보(Raw) | — | ⛔ | Raw |
| 27 | 기기정보(ATM) | T_ATM | ✅ | 3000/3202 |
| 28 | 기기HOSTIP정보 | T_ATM(확장) | ✅ | 7205 |
| 29 | 온열시간 | T_DEVICEHEATTIME | 🆕 V1.2.9 | 9302 |
| 30 | 등급금 | — | ⛔ | (범위밖) |
| 31 | 용역관리기기정보 | T_VENDORATM | ✅ | 3202 |
| 32 | 자동화기기이벤트 | T_OPERATIONJOURNAL | ✅ | — |
| 33 | 프리폼명세표관리 | T_SLIPFORMAT | ✅ | 2301 |
| 34 | 명세표편집 | T_SLIPEDIT | ✅ | 2301 |
| 35 | 상태 | T_ATMSTATUS + T_CASSETTESTATUS | ✅ | 6000/3200 |
| 36 | 거래상태메세지 | T_TRANSTATUSMSG | 🆕 V1.2.9 | 3100/3101 |
| 37 | 망상태상세 | T_NETWORKSTATUS | 🆕 V1.2.9 | 3100 |
| 38 | 사고신고전문 | T_ACCIDENTREPORT | ✅ | — |
| 39 | 데몬가동정보 | T_DAEMONSTATUS | 🆕 V1.2.9 | 7227 |
| 40 | 일별거래통계 | T_TRANSACTIONDAILYTOTAL | ✅ | 4104/5100 |
| 41 | 시간별거래통계 | T_TRANSACTIONHOURLYTOTAL | ✅ | 5031 |
| 42 | 월별거래통계 | T_TRANSACTIONMONTHLYTOTAL | ✅ | 4200 |
| 43 | 시간별월거래통계 | T_TRANSACTIONMONTHLYTOTAL | ✅ | 4200 |
| 44 | 지로월별거래통계 | T_GIROMONTHLYTOTAL | 🆕 V1.2.9 | 통계 |
| 45 | 집계 | T_DAYTOTAL | ✅ | 2101/6101 |
| 46 | CPC자료집계 | — | ⛔ | (범위밖) |
| 47 | 타행도 | — | ⛔ | (범위밖) |
| 48 | 코드마스터 | T_CODEGROUP | ✅ | 7201 |
| 49 | 상세코드 | T_CODE | ✅ | 7201 |
| 50 | 휴일코드 | T_HOLIDAY | ✅ | — |
| 51 | 카드빈 | T_CARDBIN | 🆕 V1.2.9 | 1004/9122 |
| 52 | ATM에러메세지 | T_ERRORMESSAGE | ✅ | 7203/7002 |
| 53 | BP라우팅정보 | — | ⛔ | 7209(범위밖) |
| 54 | TB_INSUR_GBN | — | ⛔ | (범위밖) |
| 55 | 엡출금수수료 | T_FEE | ✅ | — |
| 56 | 수수료제정처리관리 | T_FEE(확장) | ✅ | — |
| 57 | 홍보문구관리 | T_PROMOTION | ✅ | 5102/9512 |
| 58 | 인사정보 | T_PERSON | ✅ | 3206 |
| 59 | 인사정보(Raw) | — | ⛔ | Raw |
| 60 | 통합업무시스템권한 | T_AUTH + T_USER_GROUP_AUTH | ✅ | 7100/7200 |
| 61 | 저널권한 | T_JOURNALAUTH | ✅ | 4002 |
| 62 | 저널권한이력 | T_JOURNALAUTHHIST | ✅ | 4002 |
| 63 | 우수고객 | T_VIPCUSTOMER | ✅ | 3003 |
| 64 | 문자사용자 | T_SMSSENDDATA(확장) | ✅ | 3103 |
| 65 | 문자내용 | T_SMSSENDDATA | ✅ | 3103 |
| 66 | 전자결재이력 | T_EAPPROVALHIST | ✅ | 2200/2201 |
| 67 | 거래내역열람이력 | T_JOURNALVIEWLOG | ✅ | 4003/4006 |
| 68 | 개인정보출력이력 | T_PERSONALINFOPRINTHIST | ✅ | 9191 |
| 69 | 데이터삭제이력 | T_DATADELETEHIST | ✅ | — |
| 70 | TB_APT_HISTORY | — | ⛔ | (범위밖) |
| 71 | TB_ATM_CRM | — | ⛔ | (범위밖) |
| 72 | SMS URL홍보 | — | ⛔ | (범위밖) |
| 73 | 업무제리로그기본 | T_JOURNAL/T_OPERATIONJOURNAL | ✅ | 4000 |
| 74 | 업무제리로그상세 | T_JOURNAL_DATA | ✅ | 4000 |
| 75 | 업무별미완료건수 | T_UNFINISHEDCOUNT | 🆕 V1.2.9 | 6102/7227 |
| 76 | 업무부채널 | — | ⛔ | 내부 |
| 77 | 채널수신전문 | T_NETRECVDATA | ✅ | 6301 |
| 78 | 타사채널수신전문 | T_NETRECVDATA(공통) | ✅ | 6301 |
| 79 | 인수도DATA | — | ⛔ | 내부 |
| 80 | 무매 실명입금현황 | T_NONBANKDEPOSIT | 🆕 V1.2.9 | 1006 |
| 81 | 공지사항 | T_NOTICE | ✅ | 7000 |
| 82 | 업무연락 | T_NOTICE(type) | ✅ | 7001 |
| 83 | TB_BIZ_TALK | — | ⛔ | (범위밖) |
| 84 | 첨부파일관리 | T_FILEATTACH | ✅ | 7001 |
| 85 | 시스템공통관리 | T_SYSTEMCONFIG | ✅ | 4101/3002 |
| 86 | 수신공통GRID정보 | — | ⛔ | 내부 |
| 87 | 통장가입정보 | T_PASSBOOKTRANSACTION | ✅ | 1007 |
| 88 | 공과금수납기밴드정보 | T_GIROTRANSACTION | ✅ | 1003 |
| 89 | TB_TONG_ATM_RESOURCE | T_ATM(확장) | ✅ | 9322 |
| 90~94 | CPC/타사 5종 | — | ⛔ | (범위밖) |
| 95~99 | 기타 5종(계좌건수/뷰) | — | ⛔ | 내부/중복뷰 |

---

## 4. TO-BE 추가 설계 — V1.2.9 (10개)

> AS-IS에 존재하고 TO-BE 메뉴에 직접 연관되나 아직 미생성인 거래/상태/마스터 테이블.

| 테이블 | AS-IS 원천 | 화면 | 키 구성 |
| --- | --- | --- | --- |
| T_FUNDRETURN | 자금반환 | 12000/11000 | (JNL_DATE, TERM_GROUP_ID, TERM_ID, RETURN_SEQ) |
| T_FUNDRETURNMEMO | 자금반환메모 | 12000 | (JNL_DATE, TERM_GROUP_ID, TERM_ID, MEMO_SEQ) |
| T_ECASHTRANSACTION | 전자화폐 | 9113 | (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME) |
| T_NONBANKDEPOSIT | 무매 실명입금현황 | 1006 | (JNL_DATE, TERM_GROUP_ID, TERM_ID, TR_SEQ) |
| T_TRANSTATUSMSG | 거래상태메세지 | 3100/3101 | (STATUS_CD, MSG_TYPE) |
| T_NETWORKSTATUS | 망상태상세 | 3100 | (CHANNEL_ID, ORG_CD) |
| T_DAEMONSTATUS | 데몬가동정보 | 7227 | (PROG_ID, RUN_SERVER_ID) |
| T_UNFINISHEDCOUNT | 업무별미완료건수 | 6102/7227 | (JNL_DATE, EQUIP_KIND, PROC_STATUS) |
| T_CARDBIN | 카드빈 | 1004/9122 | (BIN_CD) |
| T_DEVICEHEATTIME | 온열시간 | 9302 | (TERM_GROUP_ID, TERM_ID) |
| T_GIROMONTHLYTOTAL | 지로월별거래통계 | 통계 | (YYYYMM, TERM_GROUP_ID, TERM_ID, REGION_CD) |

→ 마이그레이션: `V1.2.9__transaction_status_master_extension_schema.sql`

## 5. TO-BE 추가 설계 — V1.3.0 (8개, 일괄용역/SLA)

> 6200번대 SLA 메뉴는 AS-IS에 단일 원천테이블이 없고 통계/평가 결과를 별도 관리. 신규 평가관리 테이블로 설계.

| 테이블 | 화면 | 키 구성 |
| --- | --- | --- |
| T_SLAGRADE | 6200 SLA등급조회 | (EVAL_YYYYMM, VENDOR_CD) |
| T_SLAGRADEHIST | 6201 SLA등급 이력 | (EVAL_YYYYMM, VENDOR_CD, SEQ) |
| T_CORNERSLA | 6202 코너SLA평가 | (EVAL_YYYYMM, TERM_GROUP_ID) |
| T_DEVICEOPRATEHIST | 6203 기기별가동율 | (STAT_DATE, TERM_GROUP_ID, TERM_ID) |
| T_DEVICEFAULTHIST | 6204 기기별장애이력 | (FAULT_DATE, TERM_GROUP_ID, TERM_ID, SEQ) |
| T_VENDORCSEVAL | 6206 C/S평가 | (EVAL_YYYYMM, VENDOR_CD) |
| T_NONOPREASON | 6210 미가동사유 | (NONOP_DATE, TERM_GROUP_ID, TERM_ID, SEQ) |
| T_SERVICEFEECALC | 6212 용역료 산정 | (CALC_YYYYMM, VENDOR_CD) |

→ 마이그레이션: `V1.3.0__sla_outsourcing_schema.sql`

---

## 6. 정규화 원칙 (적용 완료)

1. **거래 정규화**: AS-IS `자동화기기_거래`(60+컬럼) → 공통 `T_TRANSACTIONJOURNAL` + 유형별 상세(현금/수표/지로/외화/통장/전자화폐). 화면 상세(9101~9123)가 유형별 상세의 근거.
2. **상태 정규화**: AS-IS `자동화기기_상태`(70+컬럼) → `T_ATMSTATUS`(기기상태) + `T_CASSETTESTATUS`(카세트/시재) + `T_ATMUNITSTATUS`(장치별).
3. **사무소 정규화**: AS-IS `사무소등록관리`(70+컬럼) → `T_BRANCH`(영업점) + `T_CORNER`(코너/설치장소).
4. **FK 논리참조**: 저널 INSERT 성능 보호를 위해 물리 FK 미설정, 애플리케이션/조회 레벨 논리참조.
5. **코드→명 변환**: SQL JOIN 대신 프론트 Zustand 스토어(useBranchStore/useCodeStore)에서 변환.

---

## 7. 적용 순서

```
(기 적용) V1.0.0 ~ V1.2.8   : 80개
V1.2.9                       : +10개 (거래/상태/마스터 보강)
V1.3.0                       : +8개  (일괄용역/SLA)
─────────────────────────────────────
합계 TO-BE 테이블            : 98개
제외(범위밖)                 : ~15개 (CPC/방카/타사/Raw/내부처리)
```
