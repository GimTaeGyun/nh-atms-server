# atms-if-external 대외 전문 처리 이식 노트 (AS-IS atme → TO-BE)

- 기준: AS-IS `0.기본자료/새마을금고/atme` (proc 클래스 + ExternalInterfaceMapper.xml) /
  `3.Migration/atme.lib` (Java 21 검증 포팅본 Converter/Gateway)
- 검증: `:atms-if-external:compileJava` 통과 (2026-07-10)

## 1. 전문별 구현 상태

### OUT (ATMS → 대외업체)

| 전문 | 내용 | 상태 | 구현 위치 |
|---|---|---|---|
| EM_01001110 | 개국/장애/상태통보 (47필드) | 완료(기존) | `ExternalIfService.statusNotify` + `StatusEnrichMapper` |
| EM_01001120 | 출동요청 (25필드) | 완료 | `ExternalIfService.dispatchServiceCall` + `MsgEnrichMapper.selectServiceCallInfo` |
| EM_01001160 | 출동취소 (18필드) | 완료 | `ExternalIfService.serviceCallCancel` |
| EM_01001170 | 출동정보변경 (13필드, 변경구분=고객대기여부) | 완료 | `ExternalIfService.serviceCallChange` |
| EM_01001180 | 동시다발장애 처리요청 (14필드) | 완료(변형) | `ExternalIfService.manyFailure` |
| SM_02001160 | 코너변경요청 (20필드, 변경전 정보 포함) | 완료 | `ExternalIfService.cornerChange` + `selectCorner` |
| IQ_04001140 | 기기정보통보 (6필드) | 완료 | `ExternalIfService.machineInfo` + `selectMachineInfo` |

- EM_01001120 정합 포인트: 출동요청일련번호=JNL_TIME, 출동요청차수=MANAGE_NO(LPAD 2),
  출동차수구분=`PROVIDER_TYPE_CD IN ('1','4') → '2' 그외 '1'`, 수신업체=`PROVIDER_TYPE_CD='4'(협력)
  → ATM_CO_CD 대체, 그외 PROVIDER_CD`, 2차 자동출동은 무조건 ATM_CO_CD.
  송신 후 DISPATCH_STATUS_CD(성공 1:출동요청/실패 0:미처리) 갱신 + T_ATM_MONITOR(AGENT_TYPE '9') 이력 생성.
- EM_01001160 정합 포인트: 송신 전 상태 6(출동취소) 갱신 + T_ATM_MONITOR + T_ATM_STATUS 출동정보 클리어
  (`updateAtmStatusClear` — 기기/용역/경비(Key지원 50) 분기 레거시 그대로).

### IN (대외업체 → ATMS)

| 전문 | 내용 | 상태 | 구현 위치 (엔드포인트) |
|---|---|---|---|
| EM_01001130 | 도착예정 | 완료(보강) | `/inbound/departure-plan` |
| EM_01001140 | 도착통보 | 완료(보강) | `/inbound/arrival` |
| EM_01001150 | 조치결과 (+2차 자동출동) | 완료 | `/inbound/repair-result` |
| EM_01001190 | 동시다발장애 조치결과 | 완료(변형) | `/inbound/many-failure-result` |
| IQ_04001110 | 결번요청 (재전송/최종일련번호) | 완료 | `/inbound/missing-msg` |
| IQ_04001130 | 코너입퇴실시간정보 | 완료 | `/inbound/corner-time` |
| SM_02001180 | 시설물관리업무보고 | 완료(이력만) | `/inbound/facility-report` |
| SM_02001190 | 환경관리업무 | 완료(이력만) | `/inbound/clean-manage` |

- 공통 수신 부가로직: 수신 이력 적재(T_NET_RECV_DATA, "004"/netrecv_reg 채번 + vend_sn 동기화) →
  업무 처리 → 응답전문 역송신(responseCd 0000/0098/0099 + atms_sn/vend_sn 반향, 레거시
  `procResponseForInbound` 등가). vendorCd 미제공 호출(기존 REST 호환)은 이력/역송신 생략.
- EM_01001150 2차 자동출동: 1차출동(PROVIDER_TYPE_CD ∉ {1,4}) & 조치결과 0(미조치) &
  조치불가사유 1(기기A/S) → 원 건 복제(manage_no+1, requestTypeCd 3, providerTypeCd 1,
  vendorCd=ATM_CO_CD, 코멘트 `[1차출동]…[2차보수출동]…` 레거시 포맷) 후 EM_01001120 재호출 +
  T_ATM_STATUS LAST_ATM_* 갱신. 조치일시 공백 시 전문 헤더 일시(msgDate/msgTime) → 현재시각 순 fallback.
- IQ_04001110: 요청일련번호 공백 → T_SEQ_NO_MASTER("004"/vendor/"netsend_snd") 최종일련번호를
  message 로 0098 응답(없으면 0099). 존재 → T_NET_SEND_DATA(PROVIDER_CD+SEND_DATE+ATMS_SN)에서
  원 송신전문(JSON) 재조회 후 그대로 재전송(응답전문 없음).

## 2. 이력/채번 (작업 5)

- `ExternalDispatchService`(신규): 모든 송신 경로에서 atms_sn=`SP_GET_SEQNO("004", vendor, "netsend_snd")`,
  vend_sn=`("005", vendor, "")` 채번(`SequenceNoService.nextFormatted`, 7자리) 후
  T_NET_SEND_DATA/T_NET_RECV_DATA 에 PROVIDER_CD/ATMS_SN/VEND_SN/RAW_MSG 포함 적재.
  수신 시 `recordInbound` 가 "004"/netrecv_reg 채번 + `syncVendorSeqNo`(구 SP_UPDATE_SEQNO).
- DDL: `src/main/resources/sql/ddl_if_external_20260710.sql`
  (T_NET_SEND_DATA/T_NET_RECV_DATA 4컬럼 추가 + 재전송 인덱스,
   T_FAULT_MANAGEMENT MANAGE_NO/REQUEST_REASON_CD/CUSTOMER_WAIT_FLAG/SECURITY_OFF_DATE·TIME 추가,
   T_CORNER_TIME_INFO 신규).

## 3. EIF 고정길이 전문 게이트웨이 (작업 6)

- `gateway/eif/Converter.java` : 3.Migration atme.lib `atme.proxy.Converter` 원본 이식
  (로직 무변경, 패키지 변경 + 클래스패스 문자열 load 오버로드 — atms-file `remote/mif/Converter` 패턴).
- `gateway/eif/Json.java` : atme.lib `atme.util.Json` 최소 이식.
- `resources/eif/interface.external.json` : AS-IS WEB-INF/config 원본 그대로 복사 (euc-kr 스펙, 주석 포함
  — Gson lenient 파싱으로 로딩).
- `gateway/eif/EifExternalGateway` : `external.gateway.mode=eif` 일 때만 빈 등록(`GatewayConfig`),
  기본은 기존 `MockExternalGateway` 유지. 헤더는 레거시 `MessageMakeUtil.makeHead` 정합
  (tlen 0508 / len 0486 / org_cd 045 / snd_cd 0), 바디는 디스패처 JSON 의 필드(헤더 예약키
  vendorCd/atmsSn/vendSn/responseCd 제외)를 스펙 필드에 **순서대로** 매핑.
  업체별 엔드포인트는 `external.gateway.vendors."{업체코드}".ip/port` 설정.

## 4. 주요 가정 (AS-IS 와 의도적 차이)

1. **키 체계**: 레거시 복합키(금고/단말/장애일시/분류/차수) 대신 TO-BE `FAULT_SEQ` 단일키로
   출동 건을 식별 (기존 모듈 설계 유지). 출동요청차수는 신규 MANAGE_NO 컬럼으로 보존.
2. **T_REPEATEDLYFAULTATM 폐지**: EM_01001180/1190 은 T_FAULT_MANAGEMENT(FAULT_TYPE='MANY_FAILURE')로
   대체. 출동요청번호(7자리)=FAULT_SEQ, 관리기준임계치/장애발생매체는 요청 파라미터로 수령,
   장애발생건수=대상 단말 수, 관리기준일 '01' 고정(레거시 동일). 1190 반영은 기기사(vend_cd 선두 '1')만.
3. **게이트웨이 동기화**: 레거시 상주 NIO 세션/큐/POLL(04001120)/락(t_lock) 구조는 요청 단위 동기
   TCP 송신으로 축약. 응답 read timeout 은 "송신완료(응답 미수신)"(0000)으로 처리. POLL 전문,
   다중 인스턴스 송신락, T_VENDOR_INFO 세션상태 갱신은 미이식 (상시 데몬 필요 — 별도 과제).
4. **경과시간**: 단순 분 차이 산출. 영업시간/공휴일 기준 F_GET_ELAPSEDTIME(_VENDOR) DB 함수는 미이식.
5. **IQ_04001130 경비해제 갱신**: 레거시는 T_ATMSTATUS LAST_OUT_* 와 정확 매칭했으나,
   TO-BE 는 동일 LOCATION_TYPE_CD 단말의 진행중(O/P)·미도착·미해제 건 갱신으로 단순화.
6. **응답전문 역송신 바디**: 레거시는 수신 바디 전체 echo, TO-BE 는 핵심 키만 echo
   (잔여 스펙 필드는 EIF 변환 시 공백 채움).
7. **수신(IN) 진입 채널**: 레거시 TCP 서버 수신 대신 REST 엔드포인트(`/inbound/*`)로 수령.
   전문 헤더 값(vend_cd/atms_sn/vend_sn/date/time)은 DTO 필드로 전달받는다.
8. **패키지**: 지시서의 `kr.co.nh.atms.ifext` 대신 모듈 기존 루트 `kr.co.nh.atms.ifex` 하위
   (`kr.co.nh.atms.ifex.gateway.eif`) 사용 — MyBatis 매퍼 자동스캔 기준 패키지와 일치 목적.
9. **스키마 의존**: T_CORNER.GUARD_STATUS/PARTNER_CD, T_ATM.LOCATION_TYPE_CD,
   T_ATM_STATUS.LAST_KEY_* 는 레거시 존재 컬럼으로 가정 (DDL 파일 하단 참고 주석).

## 5. 스킵 (사유)

- **SM_02001180/02001190 저장**: AS-IS 소스 주석 명시 "T_EQUIPMANAGE/T_CLEANMANAGE 미사용으로
  TO-BE 테이블 삭제" — 수신 이력 적재 + 응답만 수행.
- **IQ_04001120 POLL 전문 / 세션 keep-alive**: 상주 소켓 세션 전제 기능 — 동기 게이트웨이에서는 불필요.
- **결번요청 재전송의 고정길이 원문 재현**: 저장된 송신 JSON(SEND_MSG)을 재변환 송신
  (레거시 SEND_DATA_JSON 재전송과 등가).
- **F_GET_ELAPSEDTIME_VENDOR(기기업체 일과시간 기준 경과시간)**: SLA 통계 이관 시 반영 예정.
- **atms-common/타 모듈 수정 없음** (지시 준수).
