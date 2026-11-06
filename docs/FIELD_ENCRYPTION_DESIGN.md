# 화면↔서버 민감정보 암호화 설계 (HTTPS 미적용 환경) + 반입 파일

작성일: 2026-11-03 · 상태: **1차 구현 완료 (2026-11-03, 12장)** · 대상: `NH-ATMS` (atms-common, atms-web, Frontend)

## 0. 결정 사항 (2026-11-03 사용자 답변)

| 항목 | 결정 |
|---|---|
| JWT 제거 | **보류** — NH SSO 모듈 사용 예정, 다른 모듈(batch·file 등)을 화면이 직접 부를지 미확정. SSO 정보를 받은 뒤 다시 정한다 (7장) |
| 암호화 구간 | **브라우저 ↔ 서버의 민감정보**. HTTPS 는 적용하지 않는 것이 규칙 |
| 암호 방식 | 은행 지정 제품·규칙 없음 → **Java 표준 AES-256-GCM + RSA-2048-OAEP(SHA-256)** |
| 1차 범위 | **로그인·비밀번호 변경·사용자 등록의 비밀번호 필드** → 검증 후 개인정보 필드로 확대 |
| 서버 개인키 | **서버 파일 (WAR 밖)** — 6개 서버·ws02 가 같은 키 사용 |
| 이번 산출물 | 이 설계 문서 + 반입 파일 정리 (코드는 아직 없음) |

## 1. 꼭 알아둘 한계

- HTTPS 없이 하는 앱 단 암호화는 **엿보기(도청)는 막지만 중간자 공격(MITM)은 못 막는다**. 화면 JS 와 공개키 자체가 HTTP 로 오기 때문에, 중간에서 바꿔치기하면 무력화된다.
- 따라서 보안심의에는 "HTTPS 미적용 시 대체 통제: 민감필드 하이브리드 암호화 + 재전송 방지"로 설명하고, **MITM 은 내부망 통제(망분리·HIWARE·방화벽)로 보완**한다고 적는다.
- 브라우저 내장 암호 기능(Web Crypto `crypto.subtle`)은 **HTTPS·localhost 에서만** 열린다 → `http://16.88.122.112:9000` 에서는 쓸 수 없어 **순수 JS 라이브러리(node-forge)** 를 쓴다.

## 2. 대상 API (1차)

| API | 필드 | 서버 적용 | 화면 |
|---|---|---|---|
| `POST /api/auth/login` | `password` | `LoginRequest.password` 에 `@EncryptedField` | `LoginPage.tsx` |
| `POST /api/user` (사용자 등록) | `password` | `User.password` 에 `@EncryptedField` | `userApi.create` |
| `PUT /api/user/{userId}/password` | `password` | `UserController.changePassword` 에서 `fieldDecryptor.decrypt(...)` (Map 바디) | `userApi.changePassword` |

> 설계 초안의 `PUT /api/auth/{userId}/password` 는 소스에 없는 경로였다 (구현 시 확인, 비밀번호 변경은 위 하나뿐). 사용자 수정(`PUT /api/user/{userId}`)은 화면이 비밀번호를 보내지 않고 SQL 도 비밀번호를 바꾸지 않는다.
> 모두 atms-web(atms01) 이므로 1차는 서버 1개에서 끝난다. 2차(개인정보 필드)에서 다른 모듈이 생기면 같은 공통 코드(atms-common)를 쓴다.

## 3. 동작 흐름

```
[서버] RSA-2048 키쌍 1개 (개인키: 서버 파일, 공개키: API 로 제공, kid 로 버전 구분)

[화면]                                                  [서버 atms-web]
 1. GET /api/auth/public-key  ─────────────────────▶  { kid, publicKeyPem }   (로그인 전 허용, 캐시)
 2. 평문 = {"v":"<비밀번호>","ts":<epoch초>,"n":"<16바이트 난수>"}
 3. 임의 AES-256 키 K, 12바이트 IV 생성
 4. C = AES-256-GCM(K, IV, 평문)          (128bit 태그 포함)
 5. W = RSA-OAEP-SHA256(공개키, K)
 6. 필드값 = "ENC1.<kid>.<b64(W)>.<b64(IV)>.<b64(C)>"
 7. POST /api/auth/login {userId, password:"ENC1...."} ─▶ 8. 역직렬화 단계에서 자동 복호화
                                                          - kid 확인 → 개인키로 K 복원 → GCM 복호화(변조 시 실패)
                                                          - ts 가 ±120초 이내인지, n 이 처음 보는 값인지 (재전송 방지)
                                                          - 컨트롤러·서비스는 평문 비밀번호만 본다 (기존 코드 그대로)
```

- **재전송 방지가 필요한 이유**: 암호문을 그대로 다시 보내면(재전송) 비밀번호를 몰라도 로그인된다. `ts`(시각) + `n`(1회용 난수)로 막는다. `n` 은 서버 메모리에 `max-skew` 동안 보관(ConcurrentHashMap + 만료).
- 응답은 1차 범위에서 암호화하지 않는다 (비밀번호는 응답에 없음 — `UserService` 가 이미 `password=null` 로 응답).

## 4. 서버 구성 (atms-common 공통 + atms-web)

| 위치 | 클래스/설정 | 역할 |
|---|---|---|
| atms-common `security/crypto/` | `FieldCryptoProperties` | `crypto.field.enabled`, `required`, `kid`, `private-key-path`, `public-key-path`, `max-skew-seconds`(120) |
|  | `FieldDecryptor` | 위 8번. RSA 는 `Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")` + **`OAEPParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,PSource.PSpecified.DEFAULT)` 를 반드시 명시** (생략하면 Java 기본 MGF1 이 SHA-1 이라 node-forge 와 맞지 않음). AES 는 `AES/GCM/NoPadding`, 태그 128 |
|  | `ReplayGuard` | `n` 중복·`ts` 범위 검사 |
|  | `@EncryptedField` + `EncryptedStringDeserializer` | DTO 필드에 붙이면 Jackson 이 복호화. `required=false` 이면 `ENC1.` 로 시작하지 않는 평문도 통과(전환기간), `true` 이면 평문 거부(400) |
| atms-web | `FieldCryptoController` `GET /api/auth/public-key` | `{kid, publicKeyPem}` 반환. `SecurityConfig` 의 permitAll 에 추가 |
|  | `LoginRequest.password`, 사용자 등록 DTO 의 `password` | `@EncryptedField` |
|  | `UserController` 비밀번호 변경 | Map 바디라 `FieldDecryptor.decryptIfEncrypted(body.get("password"))` 한 줄 (또는 DTO 로 교체) |

- 서버에 **추가 라이브러리 없음** (JDK 표준 JCA). 오류 메시지·로그에 평문·암호문을 남기지 않는다 (`ApiLoggingAspect` 는 URI 만 남김 — 확인 완료).

### 설정 (구현: atms-web 에만 둠 — 다른 모듈은 기본값 enabled=false)

`atms-web/src/main/resources/application-jeus.yml` (서버):
```yaml
crypto:
  field:
    enabled: true
    required: false          # 화면 배포 확인 후 true
    kid: k202611
    private-key-path: /home/atmswas/keys/field-k202611.key.pem
    public-key-path:  /home/atmswas/keys/field-k202611.pub.pem
    max-skew-seconds: 120
```
`atms-web/src/main/resources/application-local.yml` (로컬 PC): `enabled: true`, `required: false`, `kid: local`, **`generate-if-missing: true`** — 키 파일 없이 기동 시 메모리 임시 키 생성(재기동마다 바뀜, 화면이 매번 공개키를 다시 받으므로 문제없음). 서버 키를 PC 로 복사하지 않는다.

> 서버에서 키 파일이 없으면 atms-web(atms01)이 **기동하지 못한다** (`[field-crypto] … 키 파일이 없습니다`). 첫 배포 전에 5장을 먼저 한다. 공통 설정(`jeus-common.yml`)에 두지 않은 이유: 다른 5개 서버까지 키 파일이 필요해지지 않게.

## 5. 키 생성·보관·교체 (ws01, atmswas — 한 줄씩)

```bash
mkdir -p /home/atmswas/keys
chgrp jeus /home/atmswas/keys
chmod 2750 /home/atmswas/keys
cd /home/atmswas/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out field-k202611.key.pem
openssl pkey -in field-k202611.key.pem -pubout -out field-k202611.pub.pem
chgrp jeus field-k202611.key.pem field-k202611.pub.pem
chmod 640 field-k202611.key.pem field-k202611.pub.pem
ls -l /home/atmswas/keys
```
- `jeusatms` 는 `jeus` 그룹이라 읽을 수 있고, 그 외 계정은 못 읽는다. `data`(2770, 앱이 쓰는 곳)와 분리한 이유.
- **ws02(2호기)** 도 같은 두 파일을 같은 경로·권한으로 둔다 (HIWARE 파일 전송, `ssh`/`scp` 입력 금지).
- **교체**: 새 kid(예: `k202705`)로 키를 하나 더 만들고 `kid`·경로를 바꾼 뒤 재기동. 화면은 공개키를 다시 받으므로 소스 변경 없음. 옛 키 파일은 1주 보관 후 삭제.
- 백업: 키 파일은 Gitea·Jenkins·WAR 어디에도 넣지 않는다. 분실 시 새 키 생성으로 충분(저장 데이터 암호화가 아니므로 복구 대상 없음).

## 6. 화면 구성 (Frontend/nh-atms-web)

| 파일 | 변경 |
|---|---|
| `package.json` | `"node-forge": "1.4.0"` (dependencies), `"@types/node-forge": "1.3.14"` (devDependencies) — 버전 고정 |
| `src/utils/fieldCrypto.ts` (신규) | 공개키 조회·캐시, `encryptField(value): Promise<string>` (`forge.random`, `forge.cipher.createCipher('AES-GCM')`, `publicKey.encrypt(K,'RSA-OAEP',{md:sha256, mgf1:{md:sha256}})`) |
| `src/pages/login/LoginPage.tsx` | 전송 직전 `password: await encryptField(values.password)` |
| `src/api/userApi.ts` | `changePassword`, 사용자 등록 시 `password` 암호화 |

- 공개키 응답의 `kid` 가 바뀌면(서버 키 교체) 캐시를 버리고 다시 받는다. 서버가 `kid` 불일치로 400 을 주면 1회 재시도.

## 7. JWT 제거(보류)와의 관계

- 이 암호화는 **로그인 전·후 인증 방식과 무관**하게 동작한다 (비밀번호 필드만 다룸).
- NH SSO 로 로그인이 넘어가면 로그인 API 의 비밀번호 암호화는 SSO 쪽 규격을 따르고, 이 설계는 **비밀번호 변경·사용자 등록**에 남는다.
- SSO 담당에게 받을 것: 연동 방식(에이전트/토큰 검증 API/연동 jar), 로그인 후 상태 유지(SSO 쿠키 이름·도메인), 화면이 batch·file 서버를 직접 부를지 → 그 뒤 세션 공유 방식(DB 세션 등) 결정.

## 8. 전환 순서

1. 키 생성 (5장) → `jeus-common.yml` 에 `enabled: true`, `required: false` → 백엔드 배포 (평문·암호문 모두 받음)
2. 화면 배포 → 로그인·비밀번호 변경이 `ENC1.` 로 가는지 브라우저 F12 Network 에서 확인
3. `required: true` 로 바꿔 재배포 → 평문 비밀번호 요청은 400
4. 2차: 개인정보 필드(연락처 등) 목록을 정해 `@EncryptedField` 추가

## 9. 시험 항목

| 구분 | 확인 |
|---|---|
| 단위 (서버) | Java 로 만든 암호문 복호화, node-forge 로 만든 암호문(고정 테스트 벡터) 복호화, 태그 변조 → 실패, `ts` 범위 밖 → 실패, 같은 `n` 두 번 → 실패, `required=false` 평문 통과·`true` 거부 |
| 화면 | 로그인·비밀번호 변경·사용자 등록 정상, F12 에 평문 비밀번호가 보이지 않음 |
| 운영 | 키 파일 권한 640·`jeus` 그룹, 로그에 비밀번호·암호문 없음, 공개키 API 응답 |

## 10. 반입 파일 정리

**서버(Java) 쪽 반입 없음** — JDK 표준 기능만 사용, 키는 서버에서 `openssl` 로 생성 (5장).

> **2026-11-06 결정: 앞으로 npm 라이브러리는 tgz 가 아니라 npm 캐시 폴더째 반입한다** — 그러면 폐쇄망에서 `npm install 이름@버전 --offline` 이 json 을 자동으로 고친다. 절차 [REQUIRED_FILES.md](REQUIRED_FILES.md) 3-5. 아래 tgz 절차는 이미 tgz 로 반입한 경우용.

**화면(npm) 쪽 반입 2개** (2026-11-03 npm 레지스트리 확인값):

| 파일 | 버전 | 크기(풀었을 때) | integrity (sha512) | 비고 |
|---|---|---|---|---|
| `node-forge-1.4.0.tgz` | 1.4.0 (2026-03-24 공개) | 약 1.6MB | `sha512-LarFH0+6VfriEhqMMcLX2F7SwSXeWwnEAJEsYm5QKWchiVYVvJyV9v7UDvUv+w5HO23ZpQTXDv/GxdDdMyOuoQ==` | 의존성 없음 |
| `types-node-forge-1.3.14.tgz` | @types 1.3.14 (2025-08-20) | 약 62KB | `sha512-mhVF2BnD4BO+jtOp7z1CdzaK4mbuK0LLQYAvdOLqHTavxFNq4zA1EmYkpnFjP8HOUzedfQkRnp0E2ulSAYSzAw==` | `@types/node` 필요 — 이미 devDependencies 에 있음 |

> 1.4.1·1.4.2 는 2026-10-27 같은 날 연달아 나온 최신판이라 이번에는 고르지 않았다 (공개 후 시간이 짧음). 반입 전 변경 내역에 보안 수정이 있으면 그 버전으로 바꾼다.

① 인터넷 PC (`cmd`, 빈 폴더에서):
```bat
npm pack node-forge@1.4.0 @types/node-forge@1.3.14
certutil -hashfile node-forge-1.4.0.tgz SHA256
certutil -hashfile types-node-forge-1.3.14.tgz SHA256
```
생성된 tgz 2개와 SHA256 값을 반입 신청한다.

② 폐쇄망 PC (`Frontend\nh-atms-web` 에서):
```bat
npm cache add C:\반입\node-forge-1.4.0.tgz
npm cache add C:\반입\types-node-forge-1.3.14.tgz
```
그다음 `package.json`·`package-lock.json` 에 두 라이브러리 항목(버전·`resolved`·`integrity`)을 **손으로 추가**하고 `npm install --offline` → `npm run build` ([NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 11-24 에 넣을 내용 그대로). 바뀐 두 파일을 Gitea 에 커밋한다.
- 2026-11-06 정정: 처음 적었던 `npm install node-forge@1.4.0 --save-exact --offline` 은 `ENOTCACHED` 로 실패한다 — `npm cache add <tgz>` 는 파일 내용만 넣고, 이름으로 설치할 때 필요한 레지스트리 목록 정보(packument)는 넣지 않기 때문. lock 에 integrity 가 있으면 캐시에서 바로 설치된다 (재현·확인함)
- tgz 경로에 공백·한글이 있으면 큰따옴표로 감싼다 (`npm cache add "D:\07. 반입파일\…\node-forge-1.4.0.tgz"`)

③ Jenkins npm 캐시 (ws01, atmswas — 화면 빌드 잡이 `npm ci --offline --cache /cache/npm` 이라 필요):
컨테이너에는 `~/atms/infra/jenkins/cache/npm` 만 `/cache/npm` 으로 마운트돼 있으므로 그 안의 임시 폴더를 쓴다.
```bash
podman unshare mkdir -p ~/atms/infra/jenkins/cache/npm/_add
podman unshare cp ~/atms/inbox/node-forge-1.4.0.tgz ~/atms/inbox/types-node-forge-1.3.14.tgz ~/atms/infra/jenkins/cache/npm/_add/
podman unshare chown -R 1000:1000 ~/atms/infra/jenkins/cache/npm/_add
podman exec atms-jenkins npm cache add /cache/npm/_add/node-forge-1.4.0.tgz --cache /cache/npm
podman exec atms-jenkins npm cache add /cache/npm/_add/types-node-forge-1.3.14.tgz --cache /cache/npm
podman unshare rm -rf ~/atms/infra/jenkins/cache/npm/_add
```
(마운트 구성: `deploy/jenkins/run-jenkins.sh`. 다음 화면 빌드가 성공하면 반영 완료.)
- `cache/npm` 은 컨테이너 1000(= 호스트 subuid) 소유라 atmswas 가 그냥 `mkdir`·`cp` 하면 `Permission denied` → 위처럼 `podman unshare` 를 붙인다 (2026-11-03 정정)

## 11. 보안팀·심의에 확인할 것

1. HTTPS 미적용 시 대체 통제로 "민감필드 하이브리드 암호화(AES-256-GCM + RSA-2048-OAEP) + 재전송 방지"가 인정되는지
2. KCMVP 등 검증필 모듈 사용 의무가 정말 없는지 (생기면 알고리즘을 ARIA 등으로 교체 — 구조는 그대로)
3. 키 길이(RSA 2048)·교체 주기·보관 위치(서버 파일 640) 기준
4. 2차 암호화 대상 개인정보 필드 목록

## 12. 구현 결과 (2026-11-03)

| 구분 | 파일 | 내용 |
|---|---|---|
| 신규 | `atms-common/.../security/crypto/FieldCryptoProperties.java` | `crypto.field.*` (enabled, required, kid, key 경로, generate-if-missing, max-skew-seconds) |
|  | `FieldKeyHolder.java` | openssl PEM(PKCS#8·X.509) 읽기, 없으면 로컬 임시 키 또는 기동 실패 |
|  | `FieldDecryptor.java` | 복호화 + 시각·nonce 검사 (OAEP SHA-256/MGF1 SHA-256 명시) |
|  | `ReplayGuard.java`, `EncryptedField.java`, `EncryptedStringDeserializer.java` | 재전송 방지, DTO 필드 자동 복호화 |
|  | `FieldCryptoConfig.java`, `FieldPublicKeyProvider.java` | 빈 등록, 공개키 응답 `{enabled, serverTime, kid, alg, publicKey}` |
|  | `atms-web/.../auth/controller/FieldCryptoController.java` | `GET /api/auth/public-key` |
|  | `Frontend/src/utils/fieldCrypto.ts` | node-forge 암호화, 서버시각 보정(`serverTime`), `CRYPTO_*` 오류 시 공개키 캐시 버림 |
|  | `atms-common/src/test/.../FieldDecryptorTest.java` + `test/resources/field-crypto/` | 시험 전용 키(openssl)와 **화면 실제 코드(fieldCrypto.ts + node-forge 1.4.0)로 만든 암호문** |
| 수정 | `SecurityConfig.java` | `/api/auth/public-key` permitAll |
|  | `GlobalExceptionHandler.java` | 본문 역직렬화 중 `BusinessException`(CRYPTO_*)은 원래 코드로 400 (그 외 본문 오류는 기존대로 500) |
|  | `LoginRequest.java`, `User.java`, `UserController.java` | 비밀번호 3곳 |
|  | `atms-web` `application-jeus.yml`, `application-local.yml` | `crypto.field` 설정 |
|  | `atms-common/build.gradle` | `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` (atms-common 첫 테스트 — 없으면 "OutputDirectoryProvider not available" 로 테스트 실행 실패. WAR 빌드에는 영향 없음) |
|  | `LoginPage.tsx`, `userApi.ts`, `UserMgmtPage.tsx`, `package.json`, `package-lock.json` | 화면 3곳 + node-forge 1.4.0 / @types/node-forge 1.3.14 |

오류 코드 (모두 HTTP 400, 화면은 메시지 표시 후 재시도): `CRYPTO_REQUIRED`(평문 거부), `CRYPTO_FORMAT`, `CRYPTO_KID`(키 교체), `CRYPTO_DECRYPT`(변조·키 불일치), `CRYPTO_EXPIRED`(시각 ±120초 초과), `CRYPTO_REPLAY`(재전송).

검증 (이 저장소 환경, 2026-11-03):

| 항목 | 결과 |
|---|---|
| 단위 테스트 | `FieldDecryptorTest` 6건 통과 — node-forge 암호문 3종(영문·한글·점 포함) 복호화, 재전송·kid·시각·변조·형식, required 정책, 비활성 통과, Jackson 필드 복호화, 키 파일 없음(서버 실패/로컬 임시 키). 기존 테스트 전체 통과 |
| 화면 빌드 | `npm run build` 성공 (린트 오류 1건은 기존 `LoginPage` 의 `catch (error: any)`) |
| 실제 기동 (로컬 프로파일 `java -jar`) | 임시 키 생성 로그, `GET /api/auth/public-key` 응답 정상. 화면 코드로 암호화한 로그인 → 복호화 통과 후 DB 단계까지 진행(이 환경엔 은행 DB 가 없어 500), **같은 암호문 재전송 → 400 CRYPTO_REPLAY**, 변조 → 400 CRYPTO_DECRYPT, 평문(required=false) → 통과 |
| WAR (`-Pjeus`) | `application-jeus.yml` 에 `crypto.field` 포함, `application-local.yml`·`local-common.yml` 미포함 |
| 미검증 | 은행 DB 로 실제 로그인 성공, JEUS 위 동작, 실제 브라우저(폐쇄망 PC)에서의 화면 동작 |

폐쇄망 PC 반영 절차: [NH-ATMS_LOCAL_APPLY_GUIDE.md](NH-ATMS_LOCAL_APPLY_GUIDE.md) 개정 2026-11-03.

