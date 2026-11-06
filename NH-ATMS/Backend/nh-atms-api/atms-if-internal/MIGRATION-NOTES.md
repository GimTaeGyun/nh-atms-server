# atms-if-internal MIGRATION NOTES — 은행 EAI 실연동 계층

구 새마을금고 atmi / atmi.lib(Gateway, Converter) → NH-ATMS atms-if-internal 포팅 기록.

- AS-IS 원본: `2.Project/0.기본자료/새마을금고/atmi` (+ `3.Migration/atmi.lib/src` Java 21 검증본)
- 활성화: `internal.gateway.mode=eai` (기본 `mock` — MockInternalGateway 유지)

## 구현 내역

### 1. 고정길이 전문 변환 엔진 (완료)
- `gateway/eai/Converter.java` : atmi.lib `atmi.proxy.Converter` 원본 이식 (IIF 싱글톤, JSON↔EUC-KR 고정길이,
  `#iif#` 전문 종료부 ZZ, stnd_msg_len 재계산 로직 포함). 로직 무변경, 패키지/import 변경만.
- 추가: `load(String, String)` 클래스패스/문자열 로딩 오버로드 (atms-file `remote/mif/Converter` 패턴).
- 스펙 파일: AS-IS `WEB-INF/config/interface.internal.json` → `src/main/resources/eai/interface.internal.json` 복사
  (헤더 50필드 + ATMONST10100-S/ATMONST10100/SMSONSS10200 바디). Gson lenient 파서라 파일 내 주석 허용.
- 보조 유틸 이식: `gateway/eai/util/Json,Str,Hex` (atmi.util 원본).

### 2. EAI 암복호화 (완료)
- `gateway/eai/EaiCipher.java` : 구 `Gateway.EAI` 이식 — AES/CBC/PKCS5Padding + Base64,
  헤더(300+35byte) 평문 유지·바디만 암호화 후 stnd_msg_len/data_length 재계산(`packetEncrypt/Decrypt`).
- 키 파일: AS-IS 와 동일하게 **직렬화된 java.security.Key 객체 파일**(구 eai.cipher.client.key)을 ObjectInputStream 으로 로드.
- 외부화: `@ConfigurationProperties(prefix="eai.cipher")` → `eai.cipher.key-path` / `eai.cipher.iv`(기본 AS-IS 고정값
  `14D7A984685CA8B0`) / `eai.cipher.packet-body-encrypt`(기본 false, 구 config.json 플래그).
- key-path 미지정/미존재 시 암호화 비활성(평문 송수신) — AS-IS 와 동일한 degradation.

### 3. EaiInternalGateway TCP 실구현 (완료)
- `gateway/eai/EaiInternalGateway.java` : `@ConditionalOnProperty(internal.gateway.mode=eai)`,
  `SmartLifecycle` 로 워커 기동/종료. 기본은 GatewayConfig 의 `@ConditionalOnMissingBean` Mock.
- 구 `Gateway.send_process` 이식: 큐(LinkedBlockingQueue) 기반 송신, keep-connect 상시연결(미연결 시 30초 간격
  재접속 시도), 표준전문길이(8byte) 프리픽스 프레이밍 수신, 연결/송신/수신 실패 메시지 체계 유지.
- **적응(의도적 변경)**: NIO Selector 논블로킹 → 블로킹 Socket + connect/read 타임아웃으로 단순화 (전문/프로토콜 동일).
  응답은 AS-IS 와 같이 동일 커넥션 동기 수신이며, 호출 스레드는 CompletableFuture 로 30초(reply-timeout) 대기
  (구 `waitor.wait(1000*30)` 대응).
- 설정: `internal.gateway.eai.*` (ip/port/send-worker/keep-connect/타임아웃/spec-location/hostname/client-ip/client-mac).

### 4. GlobalID AS-IS 규약 복원 (완료)
- `gateway/eai/EaiGlobalIdGenerator.java` : `yyMMdd(6)+HHmmss(6)+hostname(8)+일련번호(10,lpad0)+"00"(2)` = 32자.
  `substring(23,30)` = 7자리 일련번호 → MSG_SEQNO 매칭 성립.
- atms-common 의 `GlobalIdGenerator` 는 **수정 금지 규칙**에 따라 미변경 — 본 모듈 전용 생성기 추가로 대체,
  InternalIfService 가 모드와 무관하게 이 생성기를 사용 (Mock 모드도 동일 규약).
- 일련번호 채번: 구 `SP_GET_SEQNO('010','0','0')`(MySQL 프로시저) → Oracle 시퀀스 `SEQ_EAI_MSG_SEQNO`
  (7자리 CYCLE, `GwInterfaceMapper.selectNextMsgSeqNo`). DB 채번 실패 시 in-memory AtomicLong 폴백(경고 로그).
- hostname: env `HOSTNAME`(AS-IS: atmap* 아니면 atmap01t 폴백, 대문자화) — `internal.gateway.eai.hostname` 으로 재정의 가능. 8자 미만이면 '0' 패딩(가정).
- SMS 는 AS-IS 와 동일하게 SMS 일련번호(구 SMS_SNO → SMS_SEQ %07d)를 GlobalID 일련번호로 사용.

### 5. T_GW_INTERFACE 2-phase (완료)
- `gateway/GwInterfaceRecorder.java` : 송신 시 INSERT(GW_FLAG '0', SEND_DATA=전문 원문, MSG_SEQNO)
  → 응답 수신 시 UPDATE(GW_FLAG '1', RECV_DATA, RESULT_CD/MSG, `LPAD(MSG_SEQNO,7,'0')` 매칭 — AS-IS updateGwInterface 복원).
  AS-IS 가 별도 SqlSession 즉시 commit 이던 것에 맞춰 `REQUIRES_NEW` 트랜잭션.
- DDL: `src/main/resources/sql/alter_t_gw_interface_eai.sql` (MSG_SEQNO/GW_FLAG 컬럼 + 인덱스 + SEQ_EAI_MSG_SEQNO). **수동 적용 필요.**
- 응답코드 매핑 복원(구 ATMIConstants): 0000 정상 / 0097 조회자료없음(시재조회 occur=0) / 0098 결번·무응답(타임아웃, 메신저 스킵) / 0099 오류(msg_flag≠'0', 통신오류).
- 기록 주체를 서비스 → 게이트웨이 계층으로 이동 (AS-IS IPackProcess 가 Gateway 내부에서 기록하던 구조와 일치).
  Mock 도 동일 2-phase 기록 수행.

### 6. 시재조회 ATMONST10100 정합 (완료)
- 요청: `teller_no[]` 다건(occur %02d, 텔러번호 3자 연결 90byte), `gmgo_cd = term_group_id.substring(1)`,
  tr_cd `S0ACH0107I001`, eai_svc_cd `ATMONST10100-S` — AS-IS ATMONST10100_OUT 헤더 50필드 값 그대로
  (`gateway/eai/EaiMessageBuilder`). API 요청(DrawerCheckRequest)에 `tellerNos` 추가, 미지정 시 termId 1건.
- 응답: `s_amt`(1710byte = 57byte×30 : 텔러번호3/텔러시재18/자점18/타점18) 반복 파싱 → `TellerAmount[]`
  (구 Gateway 의 ATMONST10100 파싱 이식). totalCashAmount = 텔러시재 합계.
- `data_length = 27 + body spec tlen` 재계산 (구 Gateway.send_process 동일).

### 7. SMS SMSONSS10200 (완료)
- T_GW_INTERFACE 2-phase 기록 추가 (AS-IS 도 SMS000001 tr_cd 에 대해 INSERT/UPDATE 수행 — 복원).
- UMS 30필드 바디 AS-IS 기준 조립: snd_hist_id=GlobalID+"00000000"(40, '0' rpad), trans_id UMSSTD0001,
  msg_type SM, ums_biz_id BKATM2601, resrv_flag N, 수신/발신번호, snd_br_cd=term_group_id.substring(1), sms_subject=본문.
- **AS-IS 편차**: AS-IS 는 trans_dt/req_dt 에 12자(yyMMdd+HHmmss)를 넣었으나(스펙 14자 대비 부족 — AS-IS 결함 추정)
  스펙 예시(20250414164010)에 맞춰 14자(yyyyMMddHHmmss)로 조립.
- 응답 수신 시 T_SMS_SEND_DATA 상태 갱신은 기존 서비스 로직 유지(S/F).

## 스킵 (사유)

- **ATMBYDD10100 장애 메신저(배치모드)**: AS-IS 는 `batch.sh`(DB2DB 배치 에이전트, 구 Gateway.to_eai_batch) 실행
  방식으로 온라인 전문이 아니고 배치 인프라(NAS 스크립트, MGenie DB) 의존이라 스킵.
  EAI 모드에서는 T_GW_INTERFACE 송신 이력(GW_FLAG '0')만 남기고 0098("배치모드 미구현") 반환. Mock 은 기존 더미 동작.
- **EAI 인바운드 수신 서버(bind/accept + recv worker)**: 구 Gateway 의 ServerSocketChannel 리슨(echo 응답,
  EAI 발신 전문 수신) — 현행 업무 플로우(시재조회/SMS/메신저)는 모두 아웃바운드 동기 응답이라 사용처 없음.
  NH 에서 EAI→ATMS 인바운드 전문이 확정되면 추가 필요.
- **L4 헬스체크 IP 필터(config.json "l4")**: 인바운드 서버 스킵에 따라 함께 스킵.

## 가정 (NH EAI 규격 확정 시 변경 지점)

1. **전문 정의**: 헤더 50필드/바디 레이아웃은 새마을금고 IIF 규격 그대로 (`resources/eai/interface.internal.json`).
   NH 표준전문 확정 시 이 파일과 EaiMessageBuilder 의 고정값(tr_cd, 거래금고코드 0001, 본지점 999, 사원 D010001,
   채널 GH, UMSSTD0001/BKATM2601 등) 교체 필요.
2. **암호화**: AES/CBC/PKCS5+Base64, IV 고정값, 직렬화 Key 파일 방식 모두 AS-IS 가정. NH 키 배포 방식(JCEKS 등)
   확정 시 EaiCipher.loadKey() 변경.
3. **접속정보**: `internal.gateway.eai.ip/port` 기본값은 개발 더미(127.0.0.1:39021). client-ip/client-mac 헤더 값은
   AS-IS 하드코딩 값을 프로퍼티 기본값으로 이식 — 실배포 시 환경변수로 주입.
4. hostname 8자 미만 시 '0' 우측 패딩(AS-IS 는 8자 전제만 있고 패딩 규칙 없음).
5. 응답 없는 실패(타임아웃/통신오류) 시 T_GW_INTERFACE 는 GW_FLAG '0' 으로 남김 (AS-IS 동일 — UPDATE 는 수신 성공시에만).
6. 시재조회 baseDate 파라미터는 AS-IS 전문에 존재하지 않아 EAI 모드에서 미사용(API 하위호환 유지용).

## 빌드/적용

- `.\gradlew.bat :atms-if-internal:compileJava` 통과 확인 (2026-07-10).
- DB 반영: `src/main/resources/sql/alter_t_gw_interface_eai.sql` 수동 실행 필요.
- EAI 모드 기동: `INTERNAL_GATEWAY_MODE=eai` + `EAI_IP/EAI_PORT` (+ 암호화 시 `EAI_CIPHER_KEY_PATH`,
  `eai.cipher.packet-body-encrypt=true`).
