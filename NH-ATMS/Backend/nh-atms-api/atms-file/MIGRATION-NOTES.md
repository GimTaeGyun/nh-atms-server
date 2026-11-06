# atms-file 마이그레이션 노트 — 인바운드 NIO 소켓 파일서버 (구 atmf)

## 절대 조건

**Agent(.NET 클라이언트)와의 프로토콜 바이트 호환 유지가 절대 조건이다.**
아래 클래스의 와이어 포맷(리틀엔디언, 헤더 크기, 필드 순서, 암호화 스펙)은 어떤 리팩터링에서도 변경 금지:

- `remote/protocol/ProtcolRsaTransfer` — RSA(2048) 핸드셰이크: ReqHead(모듈러스2B+지수2B) → 공개키 수신 → ResHead(버퍼4B+AES키길이2B+MAC키길이2B)+암호화 키
- `remote/protocol/ProtcolAesTransfer` — AES(128)+CBC+PKCS7+HMACSHA256: Head = NONCE(16)+HMAC(32)+BLOCK_COUNT(2)+RESERVE(14)
- `remote/protocol/ProtcolDataTransfer` — FIXED_HEAD(4B, json 길이) / CHUNK_HEAD(4B, chunk 길이, 0=EOF) / UTF-8 / 최대 1MB
- `remote/protocol/ByteBufferLE`, `Stream` — 리틀엔디언 버퍼/스트림
- `remote/server/Client` — 핸드셰이크·get/put 청크 송수신 전문 처리

## 구현 내역

### 1. 인바운드 NIO 소켓 서버 (완료)

원본: `3.Migration/atmf.lib/src/atmf/server` (Java 21 검증 포팅본) → `kr.co.nh.atms.file.remote.server`

| TO-BE | AS-IS | 변경 내용 |
|---|---|---|
| `FileTransferServer` | `atmf.server.Server` | static → SmartLifecycle 인스턴스 빈. `start()`=bgn, `stop()`=end. JsonObject 설정 → `FileServerProperties`. accept/세션관리/종료대기 로직 무변경 |
| `FileServerProperties` | config.*.json | `@ConfigurationProperties(prefix="atmf.server")`: enabled(기본 **false**)/bind-ip/port/buf/max/repository/l4-allowlist |
| `ServerContext` | `Client`의 static 필드 | 버퍼풀·저장소·fd 카운터·max_block/max_plain 을 서버 인스턴스 단위로 변환. 산출식은 구 `Client.init` 과 동일 |
| `Client` | `atmf.server.Client` | static 참조를 `ctx.*` 로 치환한 것 외 프로토콜 로직 무변경 |
| `AsyncSocketChannel` / `AsyncFileChannel` / `ByteBufferPool` | 동명 클래스 | 패키지 변경만 (ByteBufferPool 은 protocol.ByteBufferLE 참조) |
| `remote/util/Hex`, `remote/util/Directory` | `atmf.util.Hex`, `atmf.util.Directory` | 원본 이식 (Client 의존성) |

- 프로토콜 클래스(`remote/protocol/*`)는 기존 이관본 **재사용** — 신규 생성 없음.
- `atmf.server.enabled=false` 기본: SmartLifecycle `isAutoStartup()` 이 false 를 반환해 포트를 bind 하지 않음 → 기존 배포 무영향. phase=Integer.MAX_VALUE (기동 마지막 / 종료 최우선).
- 기존 아웃바운드 push (`RemoteCommand`/`Converter`/`RemoteMessageController`) 는 무변경.

### 2. L4/IP 차단 (완료)

구 `Server.bgn` 의 l4 배열 매칭 로직 그대로: `l4-allowlist` 에 등록된 IP 에서 접속하면 L4 헬스체크 프로브로 간주하고 핸드셰이크 없이 즉시 연결 종료. (설정명은 지시서에 따라 `l4-allowlist` 이나, 실제 의미는 구 atmf 와 동일하게 "매칭 시 차단(close)" 목록임에 유의)

### 3. 청크 암호화 파일배포 (완료)

`Client.recvPlainAsync` 의 get/put 전문 흐름 원본 그대로 복원:

- **get**: `{act:"get", src, off}` → `{req,res:{err,len}}` 응답 후 `off`(이어받기 오프셋, ≤0 이면 0)부터 CHUNK_HEAD(4B)+데이터 반복, chunk=0 으로 종료. Agent 가 접속해 파일을 청크 단위로 수신하는 AS-IS 시나리오 성립.
- **put**: `{act:"put", dst, len}` → 저장소에 `yyyyMMddHHmmssSSS.{일련12자리}.{ip12자리}.bin` 으로 수신, len 검증, `{res:{err,dst}}` 응답.
- 경로는 repository 루트 하위로 한정(`..` 차단 — AS-IS 로직 그대로).

### 4. HTTP 다운로드 스트리밍화 (완료)

- `FirmwareController.download`: `byte[]` 전체 적재 → `FileSystemResource` 기반 스트리밍 응답 (Content-Length 유지, 대용량 타임아웃/OOM 방지).
- `FirmwareService.resolve`: SHA-256 을 64KB 버퍼 스트리밍으로 계산. `X-Checksum-SHA256` 헤더 유지.
- `FirmwareFile` record: `bytes` → `path`+`size` 로 변경 (모듈 내부 DTO, 외부 계약 무변경).

## 가정

- **포트 기본값 21001**: 지시서 기준. 구 atmf 파일서버 config 는 21025 였음 (21001 은 구성상 agent 원격명령 포트). 실배포 시 `ATMS_FILE_SERVER_PORT` 로 조정.
- **repository 기본값** `./data/atms-repo` (구 atmf 는 `c:/nas`). `ATMS_FILE_SERVER_REPO` 환경변수로 재정의.
- buf/max 기본값은 구 config.json 과 동일 (131072 / 1000). buf 는 구 로직대로 128 배수로 보정 후 사용.
- `Client.loopback` 필드는 AS-IS 에서도 미사용이나 원본 유지 차원에서 보존.

## 스킵

- 구 `Server.end` 의 주석 처리된 CompletableFuture 종료 코드: AS-IS 에서도 주석이므로 미이식.
- 구 atmf 의 웹 계층(Servlet/JSP) 및 `SemaphoreSlim` 등 서버 미사용 유틸: 이번 범위 아님.
- TLS/추가 인증: AS-IS 프로토콜에 없음(RSA 핸드셰이크 자체가 키교환) — 바이트 호환 유지를 위해 추가하지 않음.
- 소켓 서버 통합 테스트: .NET Agent 실물 필요 — 컴파일 검증(`:atms-file:compileJava`)까지 수행.

## 검증

- `.\gradlew.bat :atms-file:compileJava` → BUILD SUCCESSFUL (2026-07-10)
