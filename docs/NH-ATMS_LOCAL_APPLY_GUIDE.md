# NH-ATMS 소스 폐쇄망 로컬 반영 가이드 (JEUS WAR · 매니지드 서버 6개 · 로컬 게이트웨이)

> **읽는 법 (2026-11-03)**: 아래 1~9단계 본문은 2026-10-13(서버 1개·게이트웨이) 기준으로 쓰였고, **현재 구조(매니지드 서버 6개, `gateway.enabled: false`, JNDI)는 바로 아래 "개정" 항목들이 덮어쓴다** — 위에 있는 개정일수록 최신이다. 게이트웨이 클래스는 로컬 프로파일용으로 그대로 넣는다.

작성일: 2026-10-13 · 기준: 업체 원본 소스(이 저장소 커밋 `d98d00d`) → 반영 후 = 커밋 `5f18457` 의 `NH-ATMS/Backend/nh-atms-api`
설명 문서: [JEUS_SOURCE_CHANGES.md](JEUS_SOURCE_CHANGES.md) (왜 바꾸는지), 서버 배포: [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md)

> 이 문서는 생성 스크립트로 저장소의 실제 파일에서 그대로 뽑았다. 손으로 옮겨 적을 때 오타만 주의하면 된다.
>
> **개정 2026-11-06(2) — 11-24 npm 설치 방법 정정**: tgz 만 반입하면 `npm install node-forge@1.4.0 --offline` 이 `ENOTCACHED` 로 실패한다. **앞으로는 npm 캐시 폴더째 반입**([REQUIRED_FILES.md](REQUIRED_FILES.md) 3-5) → npm 이 json 을 자동으로 고친다. 이미 tgz 만 있으면 11-24 의 손 추가 방법.

> **개정 2026-11-06 — 화면 `api/http.ts` 전환과 암호화 모듈**: 폐쇄망 화면의 다른 모듈은 `axiosInstance` → `api/http.ts` 로 바꿨지만, **암호화 모듈(11-20~11-23)은 반입한 axios 기반 그대로** 쓴다. 따라서 `package.json` 의 **`axios` 의존성은 유지**한다 (11-20 참고). 같은 날 잠시 실었던 fetch 버전 `fieldCrypto.ts` 는 취소.

> **개정 2026-11-05 — 로컬 DB 계정을 PC별로 분리 + 계정 잠김 방지**: DB 계정이 PC마다 다르고, `gradlew build` 의 DB 접속 테스트·앱 기동이 남의 계정/틀린 비번으로 반복 로그인해 계정이 잠기던 문제. 계정은 각 PC의 **`D:\local-db.yml`** (Git·WAR 밖)에 두고, 소스 4개(`local-common.yml`·루트 `build.gradle`·`AtmsWebApplicationTests.java`·신규 `LocalDbLoginGuard.java`)를 고친다. 파일별 내용은 맨 아래 **12장** (9-1단계 `local-common.yml` 도 12장 내용이 최신).

> **개정 2026-11-03 — 화면↔서버 민감필드(비밀번호) 암호화**: 서버 21개·화면 6개 파일 + npm 반입 2개(node-forge). 파일별 내용은 맨 아래 **11장**. 설계: [FIELD_ENCRYPTION_DESIGN.md](FIELD_ENCRYPTION_DESIGN.md)
>
> **개정 2026-11-02 — DB 연결: 개발서버는 JEUS JNDI, 로컬은 공통 파일 1개**: 개발서버(jeus 프로파일)는 WAS 담당이 등록한 JEUS 데이터소스 **JNDI `dsATMS01`** (nbatmsltdb01_NBATMSTP, 16.88.250.91:1531, ATMS_APP)을 쓰고, 로컬(PC)은 계정·비밀번호를 **`atms-common` 의 `local-common.yml` 한 곳**에 적는다. 바뀐 파일 10개:
> - 1단계 `build.gradle`: `-Pjeus` 일 때 `application-local.yml` 을 WAR 에서 뺌 (4줄)
> - 9단계 `jeus-common.yml`: `spring.datasource` 가 `url/username/password/hikari` → `jndi-name: dsATMS01` (WAR 에 DB 비밀번호 없음)
> - 9-1단계 `local-common.yml` **신규**, 9-2단계 `atms-common/build.gradle` (`-Pjeus` 일 때 jar 에서 제외), 9-3단계 `application-local.yml` 6개 (`SPRING_DATASOURCE_PASSWORD` 줄 → `spring.config.import: classpath:local-common.yml`)
>
> 이미 반영한 PC 는 이 10개만 고친다. 내부망 사본: `local-common.yml` 의 `CHANGE_ME_DB_PASSWORD` 에 실제 비밀번호, `jeus-common.yml` 의 JWT·cors 는 다시 기입. 개발서버 쪽 조건(데이터소스가 Oracle 드라이버·서버 6개 대상·JEUS 라이브러리에 orai18n)은 [WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md) 2-8.

> **개정 2026-10-27(3) — 에이전트 직접 연결 확정 (구성도)**: ATM·STM·공과금수납기는 L4 → ws01 직접 (저널 8081, 파일 8083, TCP 소켓 21001, http). 바뀐 파일 3개: 9단계 `jeus-common.yml` (`jeus.agent-entry` 를 `host: CHANGE_ME_L4_VIP`·`journal-port: 8081`·`file-port: 8083`·`schema: http` 로), 11단계 atms-journal `application-jeus.yml` (`agent.atms.port`·`agent.atmf.port`), 13단계 atms-file `application-jeus.yml` (`atmf.server.enabled: true`, `bind-ip`, `port: 21001`). 이 3개를 이 문서 내용 전체로 교체.
>
> **개정 2026-10-27(2) — 서버 이름 확정**: 주석의 서버 이름만 `atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01` 로 바뀜 (설정값 변화 없음). 이미 반영한 PC 는 그대로 써도 동작한다.
>
> **개정 2026-10-27 — JEUS 매니지드 서버 6개 구성 (2026-10-26 결정)**: 모듈마다 JEUS 서버 하나, 포트 8080~8085, 컨텍스트 `/`, 게이트웨이 끔(WebtoB 가 경로별 분배). **이미 반영한 PC 는 아래 7개 파일만 이 문서 내용 전체로 교체**한다 (Java·빌드 파일 변경 없음):
> 9단계 `jeus-common.yml` (`jeus.base-url` → `jeus.host` + `jeus.url.web/journal/batch/file/ifin/ifex`, `log-dir` → `/WASlogs/jeusATMSlogs/nbatmsltws01/app`), 10~15단계 `application-jeus.yml` 6개 (`${jeus.base-url}/<컨텍스트>` → `${jeus.url.<모듈>}`, atms-web `gateway.enabled: false`).
> 게이트웨이 클래스(16~18단계)는 그대로 둔다 — 로컬 프로파일(`local`)에서 계속 쓰인다. 내부망 사본의 `CHANGE_ME` 실제 값은 교체 후 다시 넣는다.

> **개정 2026-10-26 — 은행 DB 문자셋 대응 (`atms-common/build.gradle` 1줄 + jar 반입)**: 은행 Oracle 이 **KO16MSWIN949** 라 로그인 시 `ORA-17056: 지원되지 않는 문자 집합(클래스 경로에 orai18n.jar 추가)` 발생. `atms-common\build.gradle` 의 `runtimeOnly 'com.oracle.database.jdbc:ojdbc11'` 바로 아래에 두 줄 추가 (버전은 Spring Boot 가 ojdbc11 과 같은 **23.7.0.25.01** 로 맞춤 — 적지 않는다):
>
> ```groovy
>     // 은행 DB 문자셋 KO16MSWIN949 처리 (없으면 ORA-17056 "지원되지 않는 문자 집합"). 버전은 Spring Boot BOM 이 ojdbc11 과 맞춤
>     runtimeOnly 'com.oracle.database.nls:orai18n'
> ```
>
> 폐쇄망은 `--offline` 이라 **Gradle 캐시에 orai18n 이 있어야** 빌드된다 (없으면 `No cached version of com.oracle.database.nls:orai18n:23.7.0.25.01 available for offline mode`). 반입 방법:
> 1. 인터넷 PC: 위 두 줄을 넣은 소스로 `gradlew.bat :atms-web:bootJar` 1회 실행 (orai18n jar·pom 이 캐시로 내려옴, 약 1.7MB)
> 2. 인터넷 PC 의 `%USERPROFILE%\.gradle\caches\modules-2` 폴더를 통째로 압축해 반입 (Gradle 은 `files-2.1` 의 jar 만이 아니라 `metadata-2.*` 색인도 봐서, jar 파일 하나만 복사하면 인식하지 못할 수 있다)
> 3. 폐쇄망 PC: 기존 `%USERPROFILE%\.gradle\caches\modules-2` 를 백업 후 반입본으로 교체 → `gradlew.bat :atms-web:bootJar --offline` 성공 확인
> 4. Jenkins 도 같은 캐시가 필요하다: ws01 `~/atms/infra/jenkins/cache/gradle/caches/modules-2` 를 같은 방법으로 교체 ([DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) 9-5) — 하지 않으면 백엔드 잡이 오프라인 빌드 실패
>
> 확인: 빌드된 JAR/WAR 의 `BOOT-INF/lib`·`WEB-INF/lib` 에 `orai18n-23.7.0.25.01.jar` 가 있고, 로그인 시 ORA-17056 이 사라짐.
> 급한 로컬 확인만 할 때(임시): jar 하나를 `C:\libs\` 에 두고 `runtimeOnly files('C:/libs/orai18n-23.7.0.25.01.jar')` 로 바꿔 실행할 수 있다 — 서버 빌드용으로는 쓰지 말고 위 방법으로 되돌린다.

> **개정 2026-10-23**: ws01 에서 `ss -ltn` 확인 결과 8766 이 `16.88.122.113` 에만 LISTEN → `jeus-common.yml` 의 `jeus.base-url` 을 **`http://16.88.122.113:8766`** 으로 (9단계, 주석 1줄 추가). **이미 반영한 PC 는 이 한 줄(+주석)만 고치면 된다.**
>
> **개정 2026-10-15**: JEUS 실제 HTTP 포트 확인 → `jeus-common.yml` 의 `jeus.base-url` 을 `http://127.0.0.1:8088` → **`http://127.0.0.1:8766`** (9단계, 주석 1줄 추가), `ModuleGatewayProperties.java` 주석 예시 포트도 8766 (11단계 부근, 동작 영향 없음). **이미 반영한 PC 는 `jeus-common.yml` 의 `base-url` 한 줄만 고치면 된다.**

## 0. 시작 전에

| 항목 | 내용 |
|---|---|
| 대상 | 폐쇄망 Windows PC 의 `NH-ATMS\Backend\nh-atms-api` (업체 원본 그대로인 상태) |
| 파일 수 | **24개** — 수정 10개, 신규 14개 (운영 코드·설정 21 + 테스트 3). 2026-11-02 부터 + 8개 (9-1~9-3: 신규 1, 수정 7) |
| 프런트엔드 | **변경 없음** (`Frontend\nh-atms-web` 손대지 않음) |
| 백업 | 시작 전에 `nh-atms-api` 폴더를 통째로 zip 해 둔다 |
| 인코딩 | 모든 파일 **UTF-8** 로 저장 (한글 주석 포함). IntelliJ 기본값이면 그대로 OK |
| 새 폴더 | 신규 파일 중 폴더가 없으면 만든다: `jeus`, `atms-web\...\web\gateway`, 각 모듈 `src\test\java\...` |
| 비밀값 | `jeus-common.yml` 의 `CHANGE_ME_...` 는 **내부망 사본(Gitea)에서만** 실제 값으로 바꾼다 (3단계 참고) |

원본 상태 확인 (하나라도 다르면 이미 일부 반영된 것 → 해당 단계는 건너뛰거나 비교):
- `build.gradle` 에 `jeus` 라는 단어가 없다
- `atms-web\src\main\java\kr\co\nh\atms\web\AtmsWebApplication.java` 가 `extends` 없이 `public class AtmsWebApplication {`
- `atms-web\src\main\java\kr\co\nh\atms\web\gateway` 폴더가 없다

## 순서 한눈에 보기

| 단계 | 파일 | 구분 | 없으면 생기는 일 |
|---|---|---|---|
| 1 | `build.gradle`, `jeus/web.xml` | 수정 1, 신규 1 | WAR 빌드(`-Pjeus`) 불가 |
| 2 | `Atms*Application.java` 6개 | 수정 6 | JEUS 가 Spring 을 시작하지 못함 |
| 3 | `atms-common/.../jeus-common.yml` | 신규 1 | jeus 프로파일 공통값(DB·JWT·주소) 없음 |
| 4 | 모듈별 `application-jeus.yml` 6개 | 신규 6 | 모듈별 로그·주소·저장 경로 없음 |
| 5 | atms-web 게이트웨이 3개 + `application.yml` | 신규 3, 수정 1 | `/api/file`, `/api/batch` 등이 다른 모듈로 안 감 (JEUS·로컬 모두) |
| 6 | atms-journal `HmacAuthFilter.java` | 수정 1 | JEUS 에서 저널 수신 HMAC 검증이 빠짐 (보안) |
| 7 | atms-batch `DynamicBatchScheduler.java` | 수정 1 | batch 재배포 시 스케줄러 스레드 잔류 |
| 8 | 테스트 3개 | 신규 3 | (검증용) |
| 9 | 빌드·테스트·로컬 실행 확인 | — | — |

**1~7 은 한 번에 반영한 뒤 빌드한다** (2단계만 넣고 5단계를 빼는 식으로 중간에 끊으면 로컬 동작이 원본과도, 목표와도 다를 수 있다).


## 1단계. 빌드 설정

### 1. `build.gradle` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\build.gradle`
- 목적: `-Pjeus` 를 줄 때만 6개 모듈을 WAR 로 빌드 (없으면 기존과 100% 동일)

`subprojects { ... }` 블록의 **맨 끝**, `tasks.withType(JavaCompile)...` 블록 바로 아래·`subprojects` 를 닫는 `}` 바로 위에 추가한다.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음). 나머지 줄은 위치 확인용이다.

```diff
@@ -58,4 +58,30 @@ subprojects {
     tasks.withType(JavaCompile).configureEach {
         options.encoding = 'UTF-8'
     }
+
+    // ── JEUS(외부 WAS) 배포용 WAR ───────────────────────────────────────
+    // 사용: ./gradlew bootWar -Pjeus            → <모듈>/build/libs/<모듈>-<버전>.war (Spring Boot 6개 모듈 전부)
+    //       ./gradlew :atms-web:bootWar -Pjeus  → 한 모듈만
+    // -Pjeus 가 없으면 기존과 동일 (bootJar/bootRun, 내장 톰캣) → 로컬 개발·컨테이너 빌드 영향 없음
+    //  - providedRuntime : 내장 톰캣을 WEB-INF/lib 에서 제외 (JEUS 가 서블릿 컨테이너 역할)
+    //  - WEB-INF/web.xml : jeus/web.xml 템플릿. spring.profiles.active 를 지정 (기본 jeus, -PjeusProfile=<이름> 으로 변경)
+    pluginManager.withPlugin('org.springframework.boot') {
+        if (project.hasProperty('jeus')) {
+            apply plugin: 'war'
+            dependencies {
+                providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat'
+            }
+            def jeusProfile = project.findProperty('jeusProfile') ?: 'jeus'
+            // 로컬 전용 설정(application-local.yml: 로컬 DB·JWT 값)은 WAR 에 넣지 않는다 (2026-11-02)
+            tasks.named('processResources') {
+                exclude 'application-local.yml'
+            }
+            tasks.named('bootWar') {
+                from(rootProject.file('jeus/web.xml')) {
+                    into 'WEB-INF'
+                    expand(profile: jeusProfile, moduleName: project.name)
+                }
+            }
+        }
+    }
 }
```

### 2. `jeus/web.xml` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\jeus\web.xml`
- 목적: WAR 안 `WEB-INF/web.xml` 템플릿 — 활성 프로파일 `jeus` 지정 (`${profile}`, `${moduleName}` 은 빌드 때 자동으로 채워지므로 **그대로** 입력)

`nh-atms-api` 바로 아래에 `jeus` 폴더를 만들고 그 안에 둔다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  JEUS 배포용 web.xml (빌드 시 -Pjeus 로 각 모듈 WAR 의 WEB-INF/web.xml 에 복사됨)
  - Spring Boot 시작은 SpringBootServletInitializer(각 모듈 *Application 클래스)가 담당
  - 여기서는 활성 프로파일만 지정 → application-<profile>.yml (+ atms-common 의 jeus-common.yml) 적용
  - version 5.0 (Jakarta EE 9) 로 선언: Servlet 5.0/6.0 컨테이너 모두에서 읽힘
-->
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
         version="5.0" metadata-complete="false">

    <display-name>${moduleName}</display-name>

    <context-param>
        <param-name>spring.profiles.active</param-name>
        <param-value>${profile}</param-value>
    </context-param>

</web-app>
```


## 2단계. 6개 모듈 진입점 (`SpringBootServletInitializer`)

### 3. `atms-web/src/main/java/kr/co/nh/atms/web/AtmsWebApplication.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\java\kr\co\nh\atms\web\AtmsWebApplication.java`
- 목적: JEUS(외부 WAS)가 이 클래스로 Spring 을 시작 (컨텍스트 `/`). `main()` 은 그대로라 로컬 실행 영향 없음

파일 내용을 아래 **전체**로 바꾼다 (원본과 다른 곳: `import` 2줄, `extends SpringBootServletInitializer`, `configure()` 메서드, 주석 1줄).

```java
package kr.co.nh.atms.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * ATMS Web (화면 백엔드) Spring Boot Application.
 * 포트: 8080 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /, 다른 모듈로 가는 입구 게이트웨이 겸함)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
public class AtmsWebApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsWebApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsWebApplication.class, args);
    }
}
```

### 4. `atms-journal/src/main/java/kr/co/nh/atms/journal/AtmsJournalApplication.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-journal\src\main\java\kr\co\nh\atms\journal\AtmsJournalApplication.java`
- 목적: JEUS(외부 WAS)가 이 클래스로 Spring 을 시작 (컨텍스트 `/journal`). `main()` 은 그대로라 로컬 실행 영향 없음

파일 내용을 아래 **전체**로 바꾼다 (원본과 다른 곳: `import` 2줄, `extends SpringBootServletInitializer`, `configure()` 메서드, 주석 1줄).

```java
package kr.co.nh.atms.journal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ATMS Journal (전자저널) Spring Boot Application.
 * 구 atmr 모듈에 해당. 포트: 8081 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /journal)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
@ConfigurationPropertiesScan(basePackages = "kr.co.nh.atms.journal")
@EnableScheduling
public class AtmsJournalApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsJournalApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsJournalApplication.class, args);
    }
}
```

### 5. `atms-batch/src/main/java/kr/co/nh/atms/batch/AtmsBatchApplication.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-batch\src\main\java\kr\co\nh\atms\batch\AtmsBatchApplication.java`
- 목적: JEUS(외부 WAS)가 이 클래스로 Spring 을 시작 (컨텍스트 `/batch`). `main()` 은 그대로라 로컬 실행 영향 없음

파일 내용을 아래 **전체**로 바꾼다 (원본과 다른 곳: `import` 2줄, `extends SpringBootServletInitializer`, `configure()` 메서드, 주석 1줄).

```java
package kr.co.nh.atms.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ATMS Batch (배치) Spring Boot Application.
 * 구 atmb 모듈에 해당. 포트: 8082 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /batch)
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
public class AtmsBatchApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsBatchApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsBatchApplication.class, args);
    }
}
```

### 6. `atms-file/src/main/java/kr/co/nh/atms/file/AtmsFileApplication.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-file\src\main\java\kr\co\nh\atms\file\AtmsFileApplication.java`
- 목적: JEUS(외부 WAS)가 이 클래스로 Spring 을 시작 (컨텍스트 `/file`). `main()` 은 그대로라 로컬 실행 영향 없음

파일 내용을 아래 **전체**로 바꾼다 (원본과 다른 곳: `import` 2줄, `extends SpringBootServletInitializer`, `configure()` 메서드, 주석 1줄).

```java
package kr.co.nh.atms.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * ATMS File (파일 송수신/펌웨어 다운로드) Spring Boot Application.
 * 구 atmf 모듈에 해당. 포트: 8083 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /file)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
@ConfigurationPropertiesScan(basePackages = "kr.co.nh.atms.file")
public class AtmsFileApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsFileApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsFileApplication.class, args);
    }
}
```

### 7. `atms-if-internal/src/main/java/kr/co/nh/atms/ifin/AtmsIfInternalApplication.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-if-internal\src\main\java\kr\co\nh\atms\ifin\AtmsIfInternalApplication.java`
- 목적: JEUS(외부 WAS)가 이 클래스로 Spring 을 시작 (컨텍스트 `/ifin`). `main()` 은 그대로라 로컬 실행 영향 없음

파일 내용을 아래 **전체**로 바꾼다 (원본과 다른 곳: `import` 2줄, `extends SpringBootServletInitializer`, `configure()` 메서드, 주석 1줄).

```java
package kr.co.nh.atms.ifin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * ATMS IF-Internal (대내 인터페이스) Spring Boot Application.
 * 구 atmi 모듈에 해당. EAI / 계정계 / SMS / 메신저 등.
 * 포트: 8084 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /ifin)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
@ConfigurationPropertiesScan(basePackages = "kr.co.nh.atms.ifin")
public class AtmsIfInternalApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsIfInternalApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsIfInternalApplication.class, args);
    }
}
```

### 8. `atms-if-external/src/main/java/kr/co/nh/atms/ifex/AtmsIfExternalApplication.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-if-external\src\main\java\kr\co\nh\atms\ifex\AtmsIfExternalApplication.java`
- 목적: JEUS(외부 WAS)가 이 클래스로 Spring 을 시작 (컨텍스트 `/ifex`). `main()` 은 그대로라 로컬 실행 영향 없음

파일 내용을 아래 **전체**로 바꾼다 (원본과 다른 곳: `import` 2줄, `extends SpringBootServletInitializer`, `configure()` 메서드, 주석 1줄).

```java
package kr.co.nh.atms.ifex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * ATMS IF-External (대외 인터페이스) Spring Boot Application.
 * 구 atme 모듈에 해당. API GW / FOS / 금결원 / 카드사 등.
 * 포트: 8085 (로컬 내장 톰캣) / 개발서버는 JEUS 에 WAR 배포 (컨텍스트 /ifex)
 */
@SpringBootApplication(scanBasePackages = "kr.co.nh.atms")
public class AtmsIfExternalApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AtmsIfExternalApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AtmsIfExternalApplication.class, args);
    }
}
```


## 3단계. jeus 프로파일 공통 설정

### 9. `atms-common/src/main/resources/jeus-common.yml` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\resources\jeus-common.yml`
- 목적: 6개 모듈 공통값 (DB = JEUS 데이터소스 JNDI `dsATMS01`, JWT, 모듈별 서버 주소 `jeus.url.*`, 로그·데이터 폴더). jeus 프로파일에서만 읽힘 → 로컬 영향 없음

`CHANGE_ME_...` 는 **이 단계에서는 그대로 둔다.** 실제 값(JWT 시크릿 32바이트 이상, WebtoB 주소, L4 VIP)은 내부망 Gitea 사본에서만 넣는다. DB 는 2026-11-02 부터 JNDI 라 이 파일에 주소·비밀번호가 없다 (JEUS 데이터소스에 있음). 서버 주소·포트(`jeus.host`, `jeus.url.*`)는 2026-10-26 결정값(16.88.122.113, 8080~8085)이 이미 들어 있다.

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```yaml
# =====================================================================
# JEUS(은행 개발서버 nbatmsltws01) 공통 설정
#  - 모든 모듈의 application-jeus.yml 이 spring.config.import 로 읽는다 (atms-common jar 안에 포함)
#  - 활성화: WAR 의 WEB-INF/web.xml 이 spring.profiles.active=jeus 지정 (빌드: ./gradlew bootWar -Pjeus)
#  - 우선순위: 이 파일 > 각 모듈 application-jeus.yml > application.yml
#  - JVM 옵션(-Djeus.host=... 등)이나 OS 환경변수로 지정하면 이 파일 값보다 우선한다
#
#  ※ CHANGE_ME 항목(JWT 시크릿·WebtoB 주소·L4 VIP)은 "내부망 소스(Gitea)" 에서만 실제 값으로 바꾼다.
#    외부(GitHub 등)에 실제 JWT 시크릿을 push 하지 않는다. DB 는 JNDI 라 이 파일에 비밀번호가 없다.
# =====================================================================

jeus:
  # 2026-10-26 결정: 도메인 1개 + 매니지드 서버 6개 (모듈마다 JVM 하나, 포트 8080~8085, 컨텍스트 '/')
  #   업체 원본 멀티모듈과 같은 배치 → 모듈 간 호출은 각 서버의 HTTP 리스너로 직접 (게이트웨이 없음)
  #   리스너는 16.88.122.113 에서 LISTEN 해야 함 (127.0.0.1 만 열면 모듈 간 호출 실패 — 2026-10-23 8766 사례)
  host: 16.88.122.113
  url:
    web:     http://${jeus.host}:8080   # atms01  (atms-web)
    journal: http://${jeus.host}:8081   # ajou01 (atms-journal)
    batch:   http://${jeus.host}:8082   # abat01   (atms-batch)
    file:    http://${jeus.host}:8083   # file01    (atms-file)
    ifin:    http://${jeus.host}:8084   # ifin01    (atms-if-internal)
    ifex:    http://${jeus.host}:8085   # ifex01    (atms-if-external)
  # 앱 로그 폴더 (6개 서버 공용, 파일 이름은 모듈별. JEUS 실행 계정 jeusatms 가 쓰기 가능해야 함)
  log-dir: /WASlogs/jeusATMSlogs/nbatmsltws01/app
  # 업로드·이미지·다운로드 저장소 (jeusatms 쓰기 가능, web·file 모듈이 공유)
  data-dir: /home/atmswas/data
  # ATM·STM·공과금수납기 에이전트가 접속할 입구 (에이전트 open 응답에 내려감)
  # 2026-10-27 확정 구성: 에이전트 → 내부망 L4 → ws01 직접 (WebtoB 를 거치지 않음)
  #   저널 REST → ajou01 :8081, 파일 REST → file01 :8083, 파일 TCP 소켓 → file01 :21001, http (개발서버)
  agent-entry:
    host: CHANGE_ME_L4_VIP         # L4 VIP 확정 후 기입 (L4 가 16.88.122.113 의 8081·8083·21001 로 분산)
    journal-port: 8081
    file-port: 8083
    schema: http

spring:
  datasource:
    # 2026-11-02: JEUS 데이터소스(JNDI) 사용 — DB 주소·계정·비밀번호는 JEUS(WAS 담당)에만 있고 WAR 에는 없다
    #   데이터소스: nbatmsltdb01_NBATMSTP (16.88.250.91:1531, 계정 ATMS_APP), JNDI 이름 dsATMS01, ws01 서버 6개 대상
    #   jndi-name 이 있으면 Spring Boot 는 자체 커넥션 풀(Hikari)을 만들지 않고 JEUS 풀을 찾아 쓴다
    #   찾는 순서: java:comp/env/dsATMS01 → dsATMS01 (Spring XML 의 resource-ref="true" 와 같은 동작)
    #   각 모듈 application.yml 의 url·hikari 값은 이 프로파일에서는 쓰이지 않는다 (풀 크기는 JEUS 데이터소스 설정)
    jndi-name: dsATMS01

# 6개 모듈 모두 같은 값이어야 함 (atms-web 이 발급한 토큰을 다른 모듈이 검증). 32바이트 이상
jwt:
  secret: CHANGE_ME_JWT_SECRET_AT_LEAST_32_BYTES_LONG

# 화면은 WebtoB 를 통해 같은 주소로 들어오므로 보통 불필요. 직접 접근 시를 위해 WebtoB 주소 지정
cors:
  allowed-origins: "http://CHANGE_ME_WEBTOB_HOST:9000"

logging:
  level:
    kr.co.nh.atms: INFO
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

### 9-1. `atms-common/src/main/resources/local-common.yml` — **신규** (2026-11-02)

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\resources\local-common.yml`
- 목적: **로컬(PC) DB 접속을 한 곳에** — 6개 모듈의 `application-local.yml` 이 이 파일을 불러온다 (9-3). 개발서버(jeus)는 JNDI 라 이 파일을 쓰지 않고, `-Pjeus` WAR 에는 들어가지도 않는다 (9-2)
- **2026-11-05 개정**: 계정·비밀번호는 이 파일에 적지 않고 각 PC 의 `D:\local-db.yml` 에 적는다 (12장). 아래는 개정 후 내용

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```yaml
# =====================================================================
# 로컬(PC, bootRun·IntelliJ, 프로파일 local) 공통 DB 접속 — 2026-11-02, PC별 계정 분리 2026-11-05
#  - 6개 모듈의 application-local.yml 이 spring.config.import 로 읽는다 → DB 주소는 이 파일 한 곳만 고친다
#  - DB 계정·비밀번호는 PC마다 다르므로 저장소에 두지 않는다 → 각 PC의 D:\local-db.yml (Git·WAR 밖)
#    D:\local-db.yml 이 없거나 비어 있으면 DB 에 접속하지 않고 기동을 멈춘다 (atms-common LocalDbLoginGuard)
#    로그인 실패는 D:\db-login-failed 에 기록되어 같은 계정·비번으로 다시 접속하지 않는다 (계정 잠김 방지)
#  - 우선순위: D:\local-db.yml > 이 파일 > 각 모듈 application-local.yml > application.yml (OS 환경변수·-D 는 더 우선)
#  - 개발서버(jeus 프로파일)는 JEUS 데이터소스(JNDI dsATMS01)를 쓴다 → jeus-common.yml
#  - -Pjeus WAR 에는 이 파일이 들어가지 않는다 (atms-common/build.gradle)
# =====================================================================

spring:
  config:
    # PC별 계정 파일 (Git 밖): D:\local-db.yml — 없으면 기동 중단(LocalDbLoginGuard)
    import: optional:file:D:/local-db.yml
  datasource:
    # 은행 개발 DB nbatmsltdb01 (서비스 NBATMSTP, 문자셋 KO16MSWIN949 → orai18n 필요, atms-common/build.gradle)
    url: jdbc:oracle:thin:@//16.88.250.91:1531/NBATMSTP
    # 계정·비번은 여기 적지 않는다 → 위 local-db.yml 이 덮어씀
    username: ""
    password: ""
    hikari:
      initialization-fail-timeout: 1   # 접속 실패 시 재시도 없이 바로 실패
```

### 9-2. `atms-common/build.gradle` — **수정** (2026-11-02)

- 목적: `-Pjeus` 로 WAR 를 만들 때 `local-common.yml`(로컬 DB 비밀번호)을 atms-common jar 에서 뺀다 → 서버 WAR 에 로컬 비밀번호가 들어가지 않음. `-Pjeus` 가 없으면 영향 없음

파일 **맨 끝**(마지막 `}` 다음)에 추가한다:

```groovy

// 로컬 전용 DB 설정(local-common.yml)은 JEUS WAR 용 빌드(-Pjeus)에서 jar 에 넣지 않는다 — 개발서버는 JNDI (2026-11-02)
if (project.hasProperty('jeus')) {
    tasks.named('jar') {
        exclude 'local-common.yml'
    }
}
```

### 9-3. 모듈별 `application-local.yml` 6개 — **수정** (2026-11-02)

대상: `atms-web`, `atms-journal`, `atms-batch`, `atms-file`, `atms-if-internal`, `atms-if-external` 의 `src\main\resources\application-local.yml`

각 파일에서 아래 두 줄(설명 1줄 + 값 1줄, 설명 문구는 모듈마다 조금 다름)을 **지우고**:

```yaml
# application.yml 의 ${SPRING_DATASOURCE_PASSWORD} 플레이스홀더 해소
SPRING_DATASOURCE_PASSWORD: Atms2026!
```

그 자리에 아래 4줄을 넣는다 (`JWT_SECRET` 줄은 그대로 둔다):

```yaml
# DB 접속(주소·계정·비밀번호)은 atms-common 의 local-common.yml 한 곳에서 (2026-11-02)
spring:
  config:
    import: classpath:local-common.yml
```

- 결과: 로컬 실행 시 DB 는 `local-common.yml` 값 (우선순위: `local-common.yml` > `application-local.yml` > `application.yml`). OS 환경변수 `SPRING_DATASOURCE_URL` 등을 주면 그것이 우선
- 이미 PC 에서 `application.yml` 등에 은행 DB 값을 직접 넣어 두었다면, 그 줄은 원래대로(업체 값) 되돌리고 `local-common.yml` 로 옮긴다 — 값이 여러 곳에 있으면 어느 것이 쓰이는지 헷갈린다

## 4단계. 모듈별 jeus 설정 (`application-jeus.yml` 6개)

### 10. `atms-web/src/main/resources/application-jeus.yml` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\resources\application-jeus.yml`
- 목적: 로그 파일, 레거시 에이전트·원격제어 대상 주소, 저장 경로, **게이트웨이 끔** (jeus 프로파일 전용 — 모듈 경로는 WebtoB 가 각 서버로 분배)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```yaml
# =====================================================================
# atms-web — JEUS 배포 (서버 atms01 :8080, 컨텍스트 '/')
# 2026-10-26: 매니지드 서버 6개 구성 — WebtoB 가 경로별로 각 모듈 서버에 직접 분배하므로 게이트웨이는 끈다
# 공통값(DB·JWT·주소)은 atms-common 의 jeus-common.yml
# =====================================================================
spring:
  config:
    import: classpath:jeus-common.yml

logging:
  file:
    name: ${jeus.log-dir}/atms-web.log

# 레거시 에이전트 .do 경로(/atms/agent, /atmr/agent, /atms/remote-exec) → atms-journal
agent:
  proxy:
    journal-base-url: ${jeus.url.journal}

# 원격제어 전문 송신 → atms-file
atmf:
  remote:
    snd-msg-uri: ${jeus.url.file}/atmf/snd.msg.do

notice:
  upload-dir: ${jeus.data-dir}/uploads/notice

download:
  storage:
    base-dir: ${jeus.data-dir}/atms-download

# 모듈 경로(/api/journal, /api/batch, /api/file, /atmf, /api/if/...) 는 WebtoB http.m 의 *URI 가
# 해당 서버로 바로 보낸다 (deploy/webtob/http.m.atms-jsv.txt) → atms-web 이 전달하지 않음
gateway:
  enabled: false
```

### 11. `atms-journal/src/main/resources/application-jeus.yml` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-journal\src\main\resources\application-jeus.yml`
- 목적: 로그 파일, IF 모듈 주소, 에이전트 접속 정보 (jeus 프로파일 전용)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```yaml
# =====================================================================
# atms-journal — JEUS 배포 (서버 ajou01 :8081, 컨텍스트 '/')
# 화면의 /api/journal/ 은 WebtoB 가, ATM 에이전트(/api/agent/·/api/journal/·레거시 .do)는 L4 가 이 서버(8081)로 보낸다
# =====================================================================
spring:
  config:
    import: classpath:jeus-common.yml

logging:
  file:
    name: ${jeus.log-dir}/atms-journal.log

if:
  queue:
    internal-base-url: ${jeus.url.ifin}
    external-base-url: ${jeus.url.ifex}

# 에이전트 open 응답으로 내려줄 접속 정보 → 에이전트는 L4 를 거쳐 저널은 ajou01(8081), 파일은 file01(8083) 로 직접
agent:
  atms:
    host: ${jeus.agent-entry.host}
    port: ${jeus.agent-entry.journal-port}
    schema: ${jeus.agent-entry.schema}
  atmf:
    host: ${jeus.agent-entry.host}
    port: ${jeus.agent-entry.file-port}
```

### 12. `atms-batch/src/main/resources/application-jeus.yml` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-batch\src\main\resources\application-jeus.yml`
- 목적: 로그 파일 (jeus 프로파일 전용)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```yaml
# =====================================================================
# atms-batch — JEUS 배포 (서버 abat01 :8082, 컨텍스트 '/')
# WebtoB 가 /api/batch/ 를 이 서버로 직접 보낸다. DB 가 없으면 기동 실패 (Spring Batch 메타 테이블)
# =====================================================================
spring:
  config:
    import: classpath:jeus-common.yml

logging:
  file:
    name: ${jeus.log-dir}/atms-batch.log
```

### 13. `atms-file/src/main/resources/application-jeus.yml` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-file\src\main\resources\application-jeus.yml`
- 목적: 로그 파일, web 콜백 주소, 파일 저장소 3곳 (jeus 프로파일 전용)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```yaml
# =====================================================================
# atms-file — JEUS 배포 (서버 file01 :8083, 컨텍스트 '/')
# 화면의 /api/file/ 은 WebtoB 가, ATM 에이전트의 파일 REST(/atmf/)·TCP 소켓(21001)은 L4 가 이 서버로 보낸다
# =====================================================================
spring:
  config:
    import: classpath:jeus-common.yml

logging:
  file:
    name: ${jeus.log-dir}/atms-file.log

# 비동기 명령 결과 콜백 → atms-web
atmf:
  remote:
    msg-async-resp-forward-uri: ${jeus.url.web}/atms/remote-exec/put.msg.resp.do
  # 에이전트 파일 송수신 TCP 소켓 서버 (구성도 "TCP socket #21001", L4 → file01). jeus 프로파일에서 켬
  server:
    enabled: true
    bind-ip: ${jeus.host}
    port: 21001
    repository: ${jeus.data-dir}/atms-repo

file:
  storage:
    base-dir: ${jeus.data-dir}/atms-img
  firmware:
    repo-dir: ${jeus.data-dir}/atms-firmware
```

### 14. `atms-if-internal/src/main/resources/application-jeus.yml` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-if-internal\src\main\resources\application-jeus.yml`
- 목적: 로그 파일 (jeus 프로파일 전용)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```yaml
# =====================================================================
# atms-if-internal — JEUS 배포 (서버 ifin01 :8084, 컨텍스트 '/')
# WebtoB 가 /api/if/internal/ 을 이 서버로 직접 보낸다. atms-journal 은 jeus.url.ifin 으로 호출
# =====================================================================
spring:
  config:
    import: classpath:jeus-common.yml

logging:
  file:
    name: ${jeus.log-dir}/atms-if-internal.log
```

### 15. `atms-if-external/src/main/resources/application-jeus.yml` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-if-external\src\main\resources\application-jeus.yml`
- 목적: 로그 파일 (jeus 프로파일 전용)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```yaml
# =====================================================================
# atms-if-external — JEUS 배포 (서버 ifex01 :8085, 컨텍스트 '/')
# WebtoB 가 /api/if/external/ 을 이 서버로 직접 보낸다. atms-journal 은 jeus.url.ifex 로 호출
# =====================================================================
spring:
  config:
    import: classpath:jeus-common.yml

logging:
  file:
    name: ${jeus.log-dir}/atms-if-external.log
```


## 5단계. atms-web 모듈 게이트웨이

`atms-web\src\main\java\kr\co\nh\atms\web\gateway` 폴더를 새로 만들고 아래 3개 클래스를 넣는다.

### 16. `atms-web/src/main/java/kr/co/nh/atms/web/gateway/ModuleGatewayProperties.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\java\kr\co\nh\atms\web\gateway\ModuleGatewayProperties.java`
- 목적: 게이트웨이 설정값 (`gateway.enabled`, 경로표, 타임아웃)
- 참고: 이 파일만 만든 상태에서는 IntelliJ 가 `@ConfigurationProperties` 에 "Not registered via @EnableConfigurationProperties…" 경고를 띄운다. **정상이다** — 다음 17번 `ModuleGatewayConfig` 가 등록하므로 그 파일을 만들면 사라진다 (빌드·실행 영향 없음. 남아 있으면 Gradle 새로고침).

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.web.gateway;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 모듈 게이트웨이 설정 (JEUS 단일 서버 배포 + 로컬 bootRun).
 *
 * JEUS 에서는 6개 모듈이 한 서버(atms01)에 컨텍스트 경로로 나뉘어 올라간다
 * (atms-web='/', atms-journal='/journal', atms-batch='/batch' …).
 * 화면·ATM 에이전트·대외기관은 기존처럼 '/api/batch', '/atmf/agent' 같은 고정 경로를 부르므로,
 * 루트 컨텍스트(atms-web)가 접두어별로 해당 모듈 컨텍스트로 요청을 그대로 전달한다.
 *
 * 로컬(bootRun)에서는 같은 고정 경로를 각 모듈 포트(8081~8085)로 전달해 JEUS 와 같은 경로 구조로 동작한다.
 *
 * 기본값 enabled=false. 켜는 곳: application-jeus.yml (JEUS), application.yml 의 local 프로파일 문서 (로컬).
 * 컨테이너(Docker Compose, 프로파일 dev)는 nginx 가 모듈별로 직접 라우팅하므로 꺼져 있다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class ModuleGatewayProperties {

    /** 게이트웨이 사용 여부. */
    private boolean enabled = false;

    /** 다운스트림 연결 타임아웃(ms). */
    private int connectTimeoutMs = 3000;

    /** 다운스트림 응답 타임아웃(ms). 파일 다운로드 등을 고려해 넉넉히. */
    private int readTimeoutMs = 120000;

    /** 접두어 → 대상 모듈 주소. 위에서부터 먼저 일치하는 항목 사용. */
    private List<Route> routes = new ArrayList<>();

    @Getter
    @Setter
    public static class Route {
        /** 요청 경로 접두어 (예: /api/batch). 이 경로 자체 또는 '접두어/...' 가 일치. */
        private String prefix;
        /** 대상 주소 (예: http://127.0.0.1:8766/batch). 원래 경로가 그대로 뒤에 붙는다. */
        private String target;
    }
}
```

### 17. `atms-web/src/main/java/kr/co/nh/atms/web/gateway/ModuleGatewayConfig.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\java\kr\co\nh\atms\web\gateway\ModuleGatewayConfig.java`
- 목적: `gateway.enabled=true` 일 때만 필터 등록 (Spring Security 보다 앞)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.web.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 모듈 게이트웨이 등록 (gateway.enabled=true 일 때만: JEUS 프로파일 jeus, 로컬 프로파일 local).
 * Spring Security 필터 체인(-100)보다 먼저 실행되도록 가장 앞 순서로 등록한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "gateway", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ModuleGatewayProperties.class)
public class ModuleGatewayConfig {

    @Bean
    public FilterRegistrationBean<ModuleGatewayFilter> moduleGatewayFilter(ModuleGatewayProperties props) {
        FilterRegistrationBean<ModuleGatewayFilter> reg = new FilterRegistrationBean<>(new ModuleGatewayFilter(props));
        reg.setName("moduleGatewayFilter");
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
```

### 18. `atms-web/src/main/java/kr/co/nh/atms/web/gateway/ModuleGatewayFilter.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\java\kr\co\nh\atms\web\gateway\ModuleGatewayFilter.java`
- 목적: 고정 경로(`/api/file` 등)를 담당 모듈로 그대로 전달하는 리버스 프록시. 연결 실패 시 502 JSON

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.web.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 루트 컨텍스트(atms-web)로 들어온 다른 모듈 경로를 해당 모듈 컨텍스트로 그대로 전달하는 리버스 프록시.
 *
 * - Spring Security·DispatcherServlet 보다 앞에서 동작한다. 인증·HMAC 검증은 원래처럼
 *   대상 모듈의 보안 설정이 판단하므로(헤더·본문을 바꾸지 않고 전달) 기존 동작과 같다.
 * - 본문은 스트림 그대로 전달한다 (JSON·form·multipart·바이너리 공통, 서명 검증에 필요).
 * - 레거시 에이전트 .do 경로(/atms/agent 등)는 기존 LegacyAgentGatewayController 가 담당한다.
 */
@Slf4j
public class ModuleGatewayFilter extends OncePerRequestFilter {

    /** 대상 서버로 넘기지 않는 hop-by-hop 헤더 및 HttpClient 가 직접 설정하는 헤더. */
    private static final Set<String> SKIP_REQUEST_HEADERS = Set.of(
            "host", "connection", "content-length", "expect", "upgrade", "keep-alive",
            "transfer-encoding", "te", "trailer", "proxy-connection", "proxy-authorization",
            "http2-settings");

    private static final Set<String> SKIP_RESPONSE_HEADERS = Set.of(
            "connection", "content-length", "transfer-encoding", "keep-alive", "te", "trailer",
            "upgrade", "proxy-connection", ":status");

    private final List<ModuleGatewayProperties.Route> routes;
    private final Duration readTimeout;
    private final HttpClient client;

    public ModuleGatewayFilter(ModuleGatewayProperties props) {
        this.routes = List.copyOf(props.getRoutes());
        this.readTimeout = Duration.ofMillis(props.getReadTimeoutMs());
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        routes.forEach(r -> log.info("[module gateway] {} -> {}", r.getPrefix(), r.getTarget()));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return findRoute(pathOf(request)) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = pathOf(req);
        ModuleGatewayProperties.Route route = findRoute(path);
        if (route == null) {
            chain.doFilter(req, res);
            return;
        }

        String query = req.getQueryString();
        URI target = URI.create(stripTrailingSlash(route.getTarget()) + path + (query != null ? "?" + query : ""));

        HttpRequest.Builder builder = HttpRequest.newBuilder(target).timeout(readTimeout);
        for (String name : Collections.list(req.getHeaderNames())) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (SKIP_REQUEST_HEADERS.contains(lower) || lower.startsWith("x-forwarded-")) {
                continue;
            }
            for (String value : Collections.list(req.getHeaders(name))) {
                builder.header(name, value);
            }
        }
        builder.header("X-Forwarded-For", forwardedFor(req));
        builder.header("X-Forwarded-Proto", req.getScheme());
        if (req.getHeader("Host") != null) {
            builder.header("X-Forwarded-Host", req.getHeader("Host"));
        }
        builder.method(req.getMethod(), bodyOf(req));

        HttpResponse<InputStream> down;
        try {
            down = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[module gateway] {} {} -> {} 중단", req.getMethod(), path, target);
            badGateway(res);
            return;
        } catch (IOException e) {
            log.warn("[module gateway] {} {} -> {} 전달 실패: {}", req.getMethod(), path, target, e.toString());
            badGateway(res);
            return;
        }

        res.setStatus(down.statusCode());
        down.headers().map().forEach((name, values) -> {
            if (SKIP_RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            values.forEach(v -> res.addHeader(name, v));
        });
        try (InputStream in = down.body(); OutputStream out = res.getOutputStream()) {
            in.transferTo(out);
        }
    }

    /**
     * 대상 모듈 연결 실패 → 502 를 직접 쓴다.
     * sendError() 를 쓰면 /error 로 다시 디스패치되는데, 그 디스패치에는 인증 정보가 없어 Spring Security 가
     * 401 로 바꾼다 (로그인한 사용자도 동일). 화면(axios)은 401 이면 로그아웃시키므로,
     * 모듈 하나가 내려가 있는 것만으로 사용자가 로그인 화면으로 튕기게 된다.
     */
    private static void badGateway(HttpServletResponse res) throws IOException {
        if (res.isCommitted()) {
            return;
        }
        res.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                "{\"success\":false,\"code\":\"502\",\"message\":\"대상 모듈에 연결할 수 없습니다.\",\"data\":null}");
    }

    /** 요청 본문을 스트림 그대로 전달. 길이를 알면 Content-Length 를 유지한다. */
    private static HttpRequest.BodyPublisher bodyOf(HttpServletRequest req) {
        long length = req.getContentLengthLong();
        boolean chunked = req.getHeader("Transfer-Encoding") != null;
        if (length <= 0 && !chunked) {
            return HttpRequest.BodyPublishers.noBody();
        }
        HttpRequest.BodyPublisher stream = HttpRequest.BodyPublishers.ofInputStream(() -> {
            try {
                return req.getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        return length > 0 ? HttpRequest.BodyPublishers.fromPublisher(stream, length) : stream;
    }

    private ModuleGatewayProperties.Route findRoute(String path) {
        for (ModuleGatewayProperties.Route r : routes) {
            String prefix = stripTrailingSlash(r.getPrefix());
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return r;
            }
        }
        return null;
    }

    /** 컨텍스트 경로를 뺀 요청 경로 (atms-web 은 JEUS 에서 '/' 이므로 보통 그대로). */
    private static String pathOf(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        return (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
    }

    private static String stripTrailingSlash(String s) {
        return (s != null && s.length() > 1 && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }

    /** 기존 X-Forwarded-For 뒤에 직전 클라이언트 IP 를 덧붙인다 (원 클라이언트 IP 보존). */
    private static String forwardedFor(HttpServletRequest req) {
        String remote = req.getRemoteAddr();
        String xff = req.getHeader("X-Forwarded-For");
        return (xff == null || xff.isBlank()) ? remote : xff + ", " + remote;
    }
}
```

### 19. `atms-web/src/main/resources/application.yml` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\resources\application.yml`
- 목적: `local` 프로파일(로컬 bootRun)에서도 게이트웨이 켜기 → 8080 하나로 JEUS 와 같은 경로 구조

파일 **맨 끝**(`management:` 블록 아래)에 빈 줄 하나 띄우고 추가한다. 첫 줄 `---` 는 YAML 문서 구분자로 **반드시** 넣는다. 들여쓰기는 공백(탭 금지).

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음). 나머지 줄은 위치 확인용이다.

```diff
@@ -94,3 +94,33 @@ management:
   endpoint:
     health:
       show-details: never
+
+---
+# ── 로컬(bootRun, 내장 톰캣) 전용: 모듈 게이트웨이 ─────────────────────
+# JEUS 와 같은 경로 구조로 동작하도록, 8080 으로 들어온 다른 모듈의 고정 경로를
+# 각 모듈 포트로 그대로 전달한다 (JEUS 는 application-jeus.yml 의 같은 설정이 컨텍스트로 전달).
+#   화면 /api/file, /api/batch · 에이전트 /api/agent, /api/journal, /atmf · 대외기관 /api/if/*
+# 대상 모듈이 떠 있지 않으면 해당 경로만 502. 끄려면 GATEWAY_ENABLED=false.
+# Docker Compose(SPRING_PROFILES_ACTIVE=dev)는 이 문서가 적용되지 않는다 (nginx 가 라우팅).
+spring:
+  config:
+    activate:
+      on-profile: local
+
+gateway:
+  enabled: ${GATEWAY_ENABLED:true}
+  routes:
+    - prefix: /api/agent
+      target: ${ATMS_JOURNAL_URL:http://localhost:8081}
+    - prefix: /api/journal
+      target: ${ATMS_JOURNAL_URL:http://localhost:8081}
+    - prefix: /api/batch
+      target: ${ATMS_BATCH_URL:http://localhost:8082}
+    - prefix: /api/file
+      target: ${ATMS_FILE_URL:http://localhost:8083}
+    - prefix: /atmf
+      target: ${ATMS_FILE_URL:http://localhost:8083}
+    - prefix: /api/if/internal
+      target: ${ATMS_IF_INTERNAL_URL:http://localhost:8084}
+    - prefix: /api/if/external
+      target: ${ATMS_IF_EXTERNAL_URL:http://localhost:8085}
```


## 6단계. atms-journal HMAC 필터 (보안 결함 수정)

### 20. `atms-journal/src/main/java/kr/co/nh/atms/journal/security/HmacAuthFilter.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-journal\src\main\java\kr\co\nh\atms\journal\security\HmacAuthFilter.java`
- 목적: JEUS `/journal` 컨텍스트에서도 `/api/journal` 판정이 맞도록 컨텍스트 경로를 뺀 경로로 비교

`shouldNotFilter` 메서드 안 2줄을 바꾸고, 그 아래에 `pathWithinApplication` 메서드를 추가한다.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음). 나머지 줄은 위치 확인용이다.

```diff
@@ -46,8 +46,15 @@ public class HmacAuthFilter extends OncePerRequestFilter {
     @Override
     protected boolean shouldNotFilter(HttpServletRequest request) {
         // /api/journal/** 만 적용. actuator 등은 패스.
-        String path = request.getRequestURI();
-        return !path.startsWith("/api/journal");
+        // 컨텍스트 경로를 뺀 경로로 판정: JEUS 에서는 '/journal' 컨텍스트에 배포되어
+        // getRequestURI() 가 '/journal/api/journal/...' 이 되므로 그대로 비교하면 HMAC 검증이 빠진다.
+        return !pathWithinApplication(request).startsWith("/api/journal");
+    }
+
+    static String pathWithinApplication(HttpServletRequest request) {
+        String uri = request.getRequestURI();
+        String ctx = request.getContextPath();
+        return (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
     }
 
     @Override
```


## 7단계. atms-batch 스케줄러 종료 처리

### 21. `atms-batch/src/main/java/kr/co/nh/atms/batch/scheduler/DynamicBatchScheduler.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-batch\src\main\java\kr\co\nh\atms\batch\scheduler\DynamicBatchScheduler.java`
- 목적: 앱 종료(재배포) 시 등록 잡 해제·스케줄러 스레드 종료

① import 1줄 추가(`BatchRunService` import 아래), ② `hostMatches` 메서드 바로 위에 `shutdown()` 메서드 추가.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음). 나머지 줄은 위치 확인용이다.

```diff
@@ -3,6 +3,7 @@ package kr.co.nh.atms.batch.scheduler;
 import kr.co.nh.atms.batch.entity.BatchJob;
 import kr.co.nh.atms.batch.mapper.BatchJobMapper;
 import kr.co.nh.atms.batch.service.BatchRunService;
+import jakarta.annotation.PreDestroy;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.boot.context.event.ApplicationReadyEvent;
@@ -88,6 +89,21 @@ public class DynamicBatchScheduler {
         }
     }
 
+    /**
+     * 앱 종료 시 등록된 잡을 모두 해제하고 스케줄러 스레드를 끝낸다.
+     * 스케줄러가 Spring 빈이 아니라 자동 종료되지 않으므로 직접 정리한다 — 그대로 두면
+     * JEUS 에서 이 앱만 재배포/중지할 때 이전 버전의 스케줄러 스레드(비데몬)가 JVM 에 남아
+     * 닫힌 컨텍스트로 잡 실행을 계속 시도하고, 이전 클래스로더도 해제되지 않는다.
+     * 이미 실행 중인 잡은 중단하지 않는다 (waitForTasksToCompleteOnShutdown=true).
+     */
+    @PreDestroy
+    public synchronized void shutdown() {
+        futures.values().forEach(f -> f.cancel(false));
+        futures.clear();
+        taskScheduler.shutdown();
+        log.info("[BATCH-SCHED] 종료 — 등록 잡 해제, 스케줄러 중지");
+    }
+
     private boolean hostMatches(String hostName) {
         if (hostName == null || hostName.isBlank()) return true;
         try {
```


## 8단계. 테스트 (검증용)

폴더가 없으면 만든다 (`atms-batch\src\test\java\...` 는 원본에 없음).

### 22. `atms-web/src/test/java/kr/co/nh/atms/web/gateway/ModuleGatewayFilterTest.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\test\java\kr\co\nh\atms\web\gateway\ModuleGatewayFilterTest.java`
- 목적: 게이트웨이 전달·통과·502 (3건)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.web.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModuleGatewayFilter 단위 테스트 — JDK HttpServer 를 대상 모듈로 띄워 실제 전달을 확인한다.
 */
@DisplayName("ModuleGatewayFilter 단위 테스트")
class ModuleGatewayFilterTest {

    private HttpServer downstream;
    private final AtomicReference<String> seenUri = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();
    private final AtomicReference<String> seenXff = new AtomicReference<>();
    private final AtomicReference<String> seenAuth = new AtomicReference<>();
    private ModuleGatewayFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        downstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        downstream.createContext("/", ex -> {
            seenUri.set(ex.getRequestURI().toString());
            seenBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            seenXff.set(ex.getRequestHeaders().getFirst("X-Forwarded-For"));
            seenAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            byte[] out = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.getResponseHeaders().add("X-Module", "journal");
            ex.sendResponseHeaders(201, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        downstream.start();

        ModuleGatewayProperties.Route journal = new ModuleGatewayProperties.Route();
        journal.setPrefix("/api/agent");
        journal.setTarget("http://127.0.0.1:" + downstream.getAddress().getPort() + "/journal/");
        ModuleGatewayProperties props = new ModuleGatewayProperties();
        props.setRoutes(List.of(journal));
        filter = new ModuleGatewayFilter(props);
    }

    @AfterEach
    void tearDown() {
        downstream.stop(0);
    }

    @Test
    @DisplayName("접두어가 일치하면 대상 컨텍스트로 경로·쿼리·본문·헤더를 그대로 전달한다")
    void matchingPrefix_forwardsToModuleContext() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/agent/ATM00001/command");
        req.setQueryString("a=1&b=%ED%95%9C");
        req.setContentType("application/json");
        req.setContent("{\"cmd\":\"REBOOT\"}".getBytes(StandardCharsets.UTF_8));
        req.addHeader("Authorization", "Bearer t");
        req.addHeader("X-Forwarded-For", "10.0.0.9");
        req.setRemoteAddr("10.1.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).as("다음 필터로 넘기지 않음").isNull();
        assertThat(seenUri.get()).isEqualTo("/journal/api/agent/ATM00001/command?a=1&b=%ED%95%9C");
        assertThat(seenBody.get()).isEqualTo("{\"cmd\":\"REBOOT\"}");
        assertThat(seenAuth.get()).isEqualTo("Bearer t");
        assertThat(seenXff.get()).isEqualTo("10.0.0.9, 10.1.1.1");
        assertThat(res.getStatus()).isEqualTo(201);
        assertThat(res.getHeader("X-Module")).isEqualTo("journal");
        assertThat(res.getContentAsString()).isEqualTo("{\"ok\":true}");
    }

    @Test
    @DisplayName("접두어가 다르면 전달하지 않고 atms-web 이 처리한다")
    void otherPaths_passThroughChain() throws Exception {
        for (String path : List.of("/api/agentx", "/api/atm/list", "/journal/api/agent/x")) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(req, new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).as(path).isSameAs(req);
        }
        assertThat(seenUri.get()).isNull();
    }

    @Test
    @DisplayName("대상 모듈에 연결할 수 없으면 502 를 직접 응답 (sendError 로 /error 에 넘기면 Security 가 401 로 바꿈)")
    void downstreamUnavailable_returns502() throws Exception {
        downstream.stop(0);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/agent");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(502);
        assertThat(res.getContentType()).startsWith("application/json");
        assertThat(res.getContentAsString(StandardCharsets.UTF_8)).contains("\"success\":false", "\"code\":\"502\"");
    }
}
```

### 23. `atms-journal/src/test/java/kr/co/nh/atms/journal/security/HmacAuthFilterTest.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-journal\src\test\java\kr\co\nh\atms\journal\security\HmacAuthFilterTest.java`
- 목적: HMAC 적용 경로 판정 — 루트/`/journal` 컨텍스트 (4건)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.journal.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HMAC 필터 적용 경로 판정 — 루트 컨텍스트(로컬 bootRun)와 '/journal' 컨텍스트(JEUS) 모두 같은 결과여야 한다.
 */
class HmacAuthFilterTest {

    private static HmacAuthFilter enabledFilter() {
        JournalHmacProperties props = new JournalHmacProperties();
        props.setEnabled(true);
        props.setSecret("test-secret");
        return new HmacAuthFilter(props);
    }

    private static MockHttpServletResponse run(String contextPath, String path) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", contextPath + path);
        req.setContextPath(contextPath);
        req.setContent("{}".getBytes());
        MockHttpServletResponse res = new MockHttpServletResponse();
        enabledFilter().doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void rootContext_journalPathWithoutSignature_isRejected() throws Exception {
        assertEquals(401, run("", "/api/journal/receive").getStatus());
    }

    @Test
    void jeusContext_journalPathWithoutSignature_isRejected() throws Exception {
        assertEquals(401, run("/journal", "/api/journal/receive").getStatus());
    }

    @Test
    void jeusContext_otherPath_isNotFiltered() throws Exception {
        assertEquals(200, run("/journal", "/api/agent/open").getStatus());
    }

    @Test
    void pathWithinApplication_stripsContextPath() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/journal/api/journal/receive");
        req.setContextPath("/journal");
        assertEquals("/api/journal/receive", HmacAuthFilter.pathWithinApplication(req));
    }
}
```

### 24. `atms-batch/src/test/java/kr/co/nh/atms/batch/scheduler/DynamicBatchSchedulerTest.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-batch\src\test\java\kr\co\nh\atms\batch\scheduler\DynamicBatchSchedulerTest.java`
- 목적: 종료 시 스케줄러 스레드 정리 (1건)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.batch.scheduler;

import kr.co.nh.atms.batch.entity.BatchJob;
import kr.co.nh.atms.batch.mapper.BatchJobMapper;
import kr.co.nh.atms.batch.service.BatchRunService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 앱 종료(@PreDestroy) 시 스케줄러 스레드가 남지 않아야 한다.
 * JEUS 에서 atms-batch 만 재배포/중지해도 이전 버전 스케줄러가 JVM 에 남지 않게 하기 위함.
 */
class DynamicBatchSchedulerTest {

    private static boolean schedulerThreadAlive() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.isAlive() && t.getName().startsWith("atms-batch-"));
    }

    @Test
    void shutdown_stopsSchedulerThreads() throws Exception {
        DynamicBatchScheduler scheduler =
                new DynamicBatchScheduler(mock(BatchJobMapper.class), mock(BatchRunService.class));
        BatchJob job = BatchJob.builder().procName("TEST01").cronExpr("* * * * * *").useFlag("Y").build();

        assertTrue(scheduler.schedule(job));
        for (int i = 0; i < 30 && !schedulerThreadAlive(); i++) {
            Thread.sleep(100);
        }
        assertTrue(schedulerThreadAlive(), "잡 등록 후 스케줄러 스레드가 떠 있어야 함");

        scheduler.shutdown();
        for (int i = 0; i < 50 && schedulerThreadAlive(); i++) {
            Thread.sleep(100);
        }
        assertFalse(schedulerThreadAlive(), "shutdown 후 스케줄러 스레드가 남으면 안 됨");
    }
}
```



## 9단계. 확인 (폐쇄망 PC, `cmd`)

Gradle 은 오프라인(`--offline`)으로 실행한다. Temurin 21 이 아닌 JDK 만 있으면 `-Porg.gradle.java.installations.paths=<Temurin21 폴더> -Porg.gradle.java.installations.auto-download=false` 를 붙인다.

```bat
cd /d <NH-ATMS>\Backend\nh-atms-api
gradlew.bat compileJava --offline
gradlew.bat :atms-journal:test :atms-batch:test --offline
gradlew.bat bootJar --offline -x test
gradlew.bat bootWar -Pjeus --offline -x test
```

| 확인 | 기대 결과 |
|---|---|
| `compileJava` | `BUILD SUCCESSFUL` |
| journal·batch 테스트 | 통과 (HmacAuthFilterTest 4건, DynamicBatchSchedulerTest 1건) |
| atms-web 테스트 | 원본부터 `NoticeServiceTest` 가 컴파일되지 않아 `:atms-web:test` 는 실패한다 (이번 변경과 무관). 게이트웨이 테스트만 보려면 `NoticeServiceTest.java` 를 잠시 다른 곳으로 옮긴 뒤 `gradlew.bat :atms-web:test --offline --tests "*ModuleGatewayFilterTest"` 후 되돌린다 |
| `bootJar` | `<모듈>\build\libs\<모듈>-0.0.1-SNAPSHOT.jar` 6개 (로컬용, 기존과 같음) |
| `bootWar -Pjeus` | `<모듈>\build\libs\<모듈>-0.0.1-SNAPSHOT.war` 6개 (JEUS 용) |

로컬 실행 확인 (내장 톰캣, 프로파일 `local`):
1. IntelliJ 또는 `gradlew.bat :atms-web:bootRun`, `:atms-file:bootRun` 실행
2. atms-web 로그에 `[module gateway] /api/file -> http://localhost:8083` 등 7줄이 나오면 게이트웨이 켜짐
3. 화면(`npm run dev`, http://localhost:3000)에서 첨부·이미지·펌웨어 등 `/api/file` 기능 확인 — 원본에서는 로컬에서 안 되던 부분
4. 띄우지 않은 모듈의 경로는 **502** (로그아웃되지 않음). 게이트웨이를 끄려면 환경변수 `GATEWAY_ENABLED=false`
5. 다른 PC·포트의 모듈로 보내려면 `ATMS_JOURNAL_URL`, `ATMS_BATCH_URL`, `ATMS_FILE_URL`, `ATMS_IF_INTERNAL_URL`, `ATMS_IF_EXTERNAL_URL` (예: `http://localhost:8083`)

## 10. 반영 후

- 내부망 Gitea 에 커밋할 때 `jeus-common.yml` 실제 값 기입은 **별도 커밋**으로 하고, 그 사본은 외부로 내보내지 않는다.
- 서버 배포 순서는 [JEUS_WEBTOB_DEPLOY_GUIDE.md](JEUS_WEBTOB_DEPLOY_GUIDE.md) STEP 2(설정값 기입·빌드)부터.

---

## 11. 개정 2026-11-03 — 화면↔서버 민감필드(비밀번호) 암호화

설계·한계·키 관리: [FIELD_ENCRYPTION_DESIGN.md](FIELD_ENCRYPTION_DESIGN.md). 서버 21개 파일(신규 13 — 시험 4개 포함, 수정 8) + 화면 6개(신규 1, 수정 5). 아래 내용은 저장소의 실제 파일에서 뽑았다.

**먼저 반입·설치할 것** (설계서 10장): npm `node-forge-1.4.0.tgz`, `types-node-forge-1.3.14.tgz` → `npm cache add` 후 11-24 (두 파일에 항목 추가 → `npm install --offline`). 서버(Java) 쪽 반입은 없다. **atms-common 테스트를 돌리려면** Gradle 캐시에 `org.junit.platform:junit-platform-launcher`(Spring Boot BOM 버전)가 있어야 한다 — 없으면 11-12 의 한 줄을 빼고 테스트만 건너뛰어도 운영 빌드(`bootWar`)에는 영향 없다.

### 11-A. 서버 공통 (atms-common) — 새 폴더 `security\crypto`

#### 11-1. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/crypto/FieldCryptoProperties.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\crypto\FieldCryptoProperties.java`
- 목적: `crypto.field.*` 설정

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 화면↔서버 민감필드 암호화 설정 (crypto.field.*). 설계: docs/FIELD_ENCRYPTION_DESIGN.md
 *
 * 기본값 enabled=false → 설정하지 않은 모듈·환경은 영향 없음 (평문 그대로 통과).
 * 1차 적용은 atms-web 의 application-jeus.yml(서버 키 파일) / application-local.yml(임시 키 자동 생성).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "crypto.field")
public class FieldCryptoProperties {

    /** 암호화 사용 여부. false 면 ENC1 값도 해석하지 않고 그대로 둔다. */
    private boolean enabled = false;

    /** true 면 평문(ENC1 로 시작하지 않는 값)을 거부한다. 화면 배포 확인 전까지 false. */
    private boolean required = false;

    /** 키 버전. 화면이 보낸 kid 와 다르면 거부 (키 교체 시 변경). */
    private String kid = "dev";

    /** RSA 개인키 PEM (PKCS#8, openssl genpkey 결과). */
    private String privateKeyPath;

    /** RSA 공개키 PEM (X.509 SubjectPublicKeyInfo, openssl pkey -pubout 결과). */
    private String publicKeyPath;

    /** 키 파일이 없으면 메모리에 임시 키를 만든다 — 로컬 PC 전용. 서버(jeus)에서는 켜지 않는다. */
    private boolean generateIfMissing = false;

    /** 화면 시각(ts)과 서버 시각의 허용 차이(초). 재전송 방지 nonce 보관 시간도 이 값. */
    private long maxSkewSeconds = 120;
}
```

#### 11-2. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/crypto/FieldKeyHolder.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\crypto\FieldKeyHolder.java`
- 목적: RSA 키 읽기(서버 파일) / 로컬 임시 키

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 키쌍 보관. 서버는 파일(WAR 밖, /home/atmswas/keys)에서 읽고,
 * 로컬은 파일이 없으면 generate-if-missing 으로 메모리 임시 키를 만든다 (재기동마다 바뀜).
 */
@Slf4j
public class FieldKeyHolder {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String publicKeyPem;

    public FieldKeyHolder(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.publicKeyPem = toPem("PUBLIC KEY", publicKey.getEncoded());
    }

    public static FieldKeyHolder load(FieldCryptoProperties p) {
        Path priv = p.getPrivateKeyPath() == null ? null : Path.of(p.getPrivateKeyPath());
        Path pub = p.getPublicKeyPath() == null ? null : Path.of(p.getPublicKeyPath());
        if (priv != null && pub != null && Files.isReadable(priv) && Files.isReadable(pub)) {
            try {
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey pk = kf.generatePrivate(new PKCS8EncodedKeySpec(fromPem(Files.readString(priv, StandardCharsets.US_ASCII))));
                PublicKey pu = kf.generatePublic(new X509EncodedKeySpec(fromPem(Files.readString(pub, StandardCharsets.US_ASCII))));
                log.info("[field-crypto] 키 로드 kid={} ({})", p.getKid(), pub);
                return new FieldKeyHolder(pk, pu);
            } catch (Exception e) {
                throw new IllegalStateException("[field-crypto] 키 파일을 읽을 수 없습니다: " + priv + " / " + pub, e);
            }
        }
        if (p.isGenerateIfMissing()) {
            try {
                KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
                g.initialize(2048);
                KeyPair kp = g.generateKeyPair();
                log.warn("[field-crypto] 키 파일 없음 → 임시 키 생성 (로컬 전용, 재기동하면 바뀜) kid={}", p.getKid());
                return new FieldKeyHolder(kp.getPrivate(), kp.getPublic());
            } catch (Exception e) {
                throw new IllegalStateException("[field-crypto] 임시 키 생성 실패", e);
            }
        }
        throw new IllegalStateException("[field-crypto] crypto.field.enabled=true 인데 키 파일이 없습니다: "
                + priv + " / " + pub + " (docs/FIELD_ENCRYPTION_DESIGN.md 5장)");
    }

    public PrivateKey privateKey() { return privateKey; }
    public PublicKey publicKey() { return publicKey; }
    public String publicKeyPem() { return publicKeyPem; }

    static byte[] fromPem(String pem) {
        String body = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    static String toPem(String type, byte[] der) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }
}
```

#### 11-3. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/crypto/ReplayGuard.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\crypto\ReplayGuard.java`
- 목적: 재전송 방지 (nonce 1회용)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 재전송 방지: 같은 nonce 를 유효시간(maxSkew) 안에 두 번 받으면 거부한다.
 * 서버(JVM)별 메모리 — 비밀번호 요청은 atms-web(atms01) 하나로만 들어오므로 1대 기준으로 충분.
 */
public class ReplayGuard {

    private final Map<String, Long> seen = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public ReplayGuard(long ttlSeconds) {
        this.ttlMillis = Math.max(1, ttlSeconds) * 2000L; // ts 허용 범위가 ±maxSkew 이므로 2배 보관
    }

    /** 처음 보는 nonce 면 true (기록), 이미 본 값이면 false. */
    public boolean firstUse(String nonce, long nowMillis) {
        if (seen.size() > 10_000) {
            seen.entrySet().removeIf(e -> e.getValue() < nowMillis);
        }
        Long prev = seen.putIfAbsent(nonce, nowMillis + ttlMillis);
        if (prev == null) return true;
        if (prev < nowMillis) {           // 만료된 기록이면 새로 사용 가능
            seen.put(nonce, nowMillis + ttlMillis);
            return true;
        }
        return false;
    }
}
```

#### 11-4. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/crypto/FieldDecryptor.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\crypto\FieldDecryptor.java`
- 목적: 복호화 + 시각·nonce 검사

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * 화면이 보낸 민감필드 복호화.
 *
 * 형식: ENC1.&lt;kid&gt;.&lt;b64 RSA-OAEP(AES키)&gt;.&lt;b64 IV(12)&gt;.&lt;b64 AES-256-GCM 암호문+태그&gt;
 * 평문: {"v":"&lt;값&gt;","ts":&lt;epoch초&gt;,"n":"&lt;nonce&gt;"}
 *
 * 실패는 BusinessException(CRYPTO_*) → GlobalExceptionHandler 가 400 으로 응답. 값·암호문은 로그에 남기지 않는다.
 */
@Slf4j
public class FieldDecryptor {

    public static final String PREFIX = "ENC1.";

    /** OAEP 파라미터를 명시한다 — 생략하면 Java 기본 MGF1 이 SHA-1 이라 화면(node-forge, MGF1 SHA-256)과 맞지 않는다. */
    static final OAEPParameterSpec OAEP_SHA256 =
            new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private static volatile FieldDecryptor current;

    private final FieldCryptoProperties props;
    private final FieldKeyHolder keys;          // enabled=false 이면 null
    private final ReplayGuard replayGuard;
    private final ObjectMapper json = new ObjectMapper();

    public FieldDecryptor(FieldCryptoProperties props, FieldKeyHolder keys) {
        this.props = props;
        this.keys = keys;
        this.replayGuard = new ReplayGuard(props.getMaxSkewSeconds());
        current = this;
    }

    /** Jackson 역직렬화기(@EncryptedField)가 쓰는 현재 앱의 인스턴스 (WAR 마다 클래스로더가 달라 앱별로 따로 존재). */
    public static FieldDecryptor current() {
        return current;
    }

    String publicKeyPem() {
        return keys == null ? null : keys.publicKeyPem();
    }

    public boolean isEnabled() {
        return props.isEnabled() && keys != null;
    }

    /** 암호문이면 복호화, 평문이면 required 설정에 따라 통과 또는 거부. null·빈 값은 그대로. */
    public String decrypt(String value) {
        if (value == null || value.isEmpty() || !isEnabled()) {
            return value;
        }
        if (!value.startsWith(PREFIX)) {
            if (props.isRequired()) {
                throw new BusinessException("CRYPTO_REQUIRED", "암호화되지 않은 값은 받을 수 없습니다.");
            }
            return value;
        }
        String[] p = value.split("\\.");
        if (p.length != 5) {
            throw new BusinessException("CRYPTO_FORMAT", "암호화 값 형식이 올바르지 않습니다.");
        }
        if (!props.getKid().equals(p[1])) {
            throw new BusinessException("CRYPTO_KID", "암호화 키가 바뀌었습니다. 다시 시도하세요.");
        }
        byte[] plain;
        try {
            Base64.Decoder b64 = Base64.getDecoder();
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.DECRYPT_MODE, keys.privateKey(), OAEP_SHA256);
            byte[] aesKey = rsa.doFinal(b64.decode(p[2]));
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, b64.decode(p[3])));
            plain = aes.doFinal(b64.decode(p[4]));
        } catch (Exception e) {
            log.warn("[field-crypto] 복호화 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException("CRYPTO_DECRYPT", "암호화 값을 해독할 수 없습니다.");
        }
        JsonNode node;
        try {
            node = json.readTree(new String(plain, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException("CRYPTO_FORMAT", "암호화 값 형식이 올바르지 않습니다.");
        }
        if (!node.hasNonNull("v") || !node.hasNonNull("ts") || !node.hasNonNull("n")) {
            throw new BusinessException("CRYPTO_FORMAT", "암호화 값 형식이 올바르지 않습니다.");
        }
        long nowMillis = System.currentTimeMillis();
        long skew = Math.abs(nowMillis / 1000 - node.get("ts").asLong());
        if (skew > props.getMaxSkewSeconds()) {
            throw new BusinessException("CRYPTO_EXPIRED", "요청 시각이 맞지 않습니다. 다시 시도하세요.");
        }
        if (!replayGuard.firstUse(node.get("n").asText(), nowMillis)) {
            throw new BusinessException("CRYPTO_REPLAY", "이미 사용된 요청입니다.");
        }
        return node.get("v").asText();
    }
}
```

#### 11-5. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/crypto/EncryptedField.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\crypto\EncryptedField.java`
- 목적: DTO 필드 자동 복호화 표시

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청 DTO 의 String 필드에 붙이면, 화면이 보낸 ENC1 암호문을 역직렬화 단계에서 평문으로 바꾼다.
 * 컨트롤러·서비스는 평문만 본다. (Map 바디는 FieldDecryptor.decrypt 를 직접 호출)
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = EncryptedStringDeserializer.class)
public @interface EncryptedField {
}
```

#### 11-6. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/crypto/EncryptedStringDeserializer.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\crypto\EncryptedStringDeserializer.java`
- 목적: `@EncryptedField` 역직렬화기

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;

/** {@link EncryptedField} 용 역직렬화기. 복호화기가 아직 없으면(테스트 등) 값을 그대로 둔다. */
public class EncryptedStringDeserializer extends StdScalarDeserializer<String> {

    public EncryptedStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        FieldDecryptor d = FieldDecryptor.current();
        return d == null ? value : d.decrypt(value);
    }
}
```

#### 11-7. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/crypto/FieldPublicKeyProvider.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\crypto\FieldPublicKeyProvider.java`
- 목적: 공개키 응답 내용

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import java.util.LinkedHashMap;
import java.util.Map;

/** 화면에 내려줄 공개키 정보 (GET /api/auth/public-key 응답 data). */
public class FieldPublicKeyProvider {

    private final FieldCryptoProperties props;
    private final FieldDecryptor decryptor;

    FieldPublicKeyProvider(FieldCryptoProperties props, FieldDecryptor decryptor) {
        this.props = props;
        this.decryptor = decryptor;
    }

    /** enabled=false 면 {enabled:false} 만 — 화면은 평문으로 보낸다. serverTime 은 화면 시계 보정용(초). */
    public Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", decryptor.isEnabled());
        m.put("serverTime", System.currentTimeMillis() / 1000);
        if (decryptor.isEnabled()) {
            m.put("kid", props.getKid());
            m.put("alg", "RSA-OAEP-256/A256GCM");
            m.put("publicKey", decryptor.publicKeyPem());
        }
        return m;
    }
}
```

#### 11-8. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/crypto/FieldCryptoConfig.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\crypto\FieldCryptoConfig.java`
- 목적: 빈 등록

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 민감필드 암호화 빈. enabled=false 면 키를 읽지 않고 복호화기는 평문을 그대로 통과시킨다. */
@Configuration
@EnableConfigurationProperties(FieldCryptoProperties.class)
public class FieldCryptoConfig {

    @Bean
    public FieldDecryptor fieldDecryptor(FieldCryptoProperties props) {
        FieldKeyHolder keys = props.isEnabled() ? FieldKeyHolder.load(props) : null;
        return new FieldDecryptor(props, keys);
    }

    @Bean
    public FieldPublicKeyProvider fieldPublicKeyProvider(FieldCryptoProperties props, FieldDecryptor decryptor) {
        return new FieldPublicKeyProvider(props, decryptor);
    }
}
```

#### 11-9. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/security/SecurityConfig.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\security\SecurityConfig.java`
- 목적: 공개키 API 를 로그인 전에 허용

`/api/auth/refresh` 줄 바로 아래에 한 줄 추가.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -63,6 +63,7 @@ public class SecurityConfig {
                         .requestMatchers(
                                 "/api/auth/login",
                                 "/api/auth/refresh",
+                                "/api/auth/public-key",   // 민감필드 암호화 공개키 (로그인 전 필요)
                                 "/h2-console/**",
                                 "/swagger-ui/**",
                                 "/v3/api-docs/**",
```

#### 11-10. `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/exception/GlobalExceptionHandler.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\exception\GlobalExceptionHandler.java`
- 목적: 복호화 실패(CRYPTO_*)를 400 으로

import 1줄, `BadCredentialsException` 처리 메서드 바로 위에 메서드 1개 추가.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -4,6 +4,7 @@ import kr.co.nh.atms.common.response.ApiResponse;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
+import org.springframework.http.converter.HttpMessageNotReadableException;
 import org.springframework.security.authentication.BadCredentialsException;
 import org.springframework.web.bind.annotation.ExceptionHandler;
 import org.springframework.web.bind.annotation.RestControllerAdvice;
@@ -19,6 +20,20 @@ public class GlobalExceptionHandler {
                 .body(ApiResponse.fail(e.getCode(), e.getMessage()));
     }
 
+    /**
+     * 요청 본문 역직렬화 중 BusinessException(예: @EncryptedField 복호화 실패 CRYPTO_*)이 나면
+     * Jackson 이 감싸서 여기로 온다 → 원래 코드로 400. 그 밖의 본문 오류는 기존과 같이 500 처리.
+     */
+    @ExceptionHandler(HttpMessageNotReadableException.class)
+    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
+        for (Throwable t = e; t != null && t.getCause() != t; t = t.getCause()) {
+            if (t instanceof BusinessException be) {
+                return handleBusinessException(be);
+            }
+        }
+        return handleException(e);
+    }
+
     @ExceptionHandler(BadCredentialsException.class)
     public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e) {
         log.warn("인증 실패: {}", e.getMessage());
```

### 11-B. 시험 (atms-common) — 선택

#### 11-11. `Backend/nh-atms-api/atms-common/src/test/java/kr/co/nh/atms/common/security/crypto/FieldDecryptorTest.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\test\java\kr\co\nh\atms\common\security\crypto\FieldDecryptorTest.java`
- 목적: 복호화 시험 (화면 암호문 교차 검증 포함)

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.common.security.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 민감필드 복호화 — 화면(node-forge) 암호문 교차 검증 + 변조·키·시각·재전송·평문 정책.
 * 테스트 키(test-only.*.pem)는 openssl 로 만든 시험 전용 키다 (서버 키 아님).
 */
class FieldDecryptorTest {

    private static final long HUNDRED_YEARS = 100L * 365 * 24 * 3600;

    private FieldCryptoProperties props;
    private FieldKeyHolder keys;

    private static Path res(String name) throws URISyntaxException {
        return Path.of(FieldDecryptorTest.class.getResource("/field-crypto/" + name).toURI());
    }

    @BeforeEach
    void setUp() throws Exception {
        props = new FieldCryptoProperties();
        props.setEnabled(true);
        props.setKid("test");
        props.setPrivateKeyPath(res("test-only.key.pem").toString());
        props.setPublicKeyPath(res("test-only.pub.pem").toString());
        keys = FieldKeyHolder.load(props);   // openssl PEM(PKCS#8 / X.509) 읽기 확인
    }

    /** Java 로 화면과 같은 형식의 암호문을 만든다 (시각·nonce·kid 를 바꿔가며 시험하려고). */
    private String encrypt(String value, long ts, String nonce, String kid) throws Exception {
        byte[] aesKey = new byte[32];
        byte[] iv = new byte[12];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(aesKey);
        rnd.nextBytes(iv);
        String plain = new ObjectMapper().writeValueAsString(java.util.Map.of("v", value, "ts", ts, "n", nonce));
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = aes.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, keys.publicKey(), FieldDecryptor.OAEP_SHA256);
        Base64.Encoder b = Base64.getEncoder();
        return "ENC1." + kid + "." + b.encodeToString(rsa.doFinal(aesKey)) + "." + b.encodeToString(iv) + "." + b.encodeToString(ct);
    }

    private static long now() {
        return System.currentTimeMillis() / 1000;
    }

    private static String code(Runnable r) {
        return assertThrows(BusinessException.class, r::run).getCode();
    }

    @Test
    void decryptsCiphertextMadeByFrontendNodeForge() throws Exception {
        props.setMaxSkewSeconds(HUNDRED_YEARS);   // 저장된 시험 벡터라 시각 검사는 끈다
        FieldDecryptor d = new FieldDecryptor(props, keys);
        List<String> lines = Files.readAllLines(res("node-forge-vectors.txt")).stream()
                .filter(l -> l.startsWith("ENC1.")).toList();
        assertEquals(List.of("admin1234!", "한글비번Ab1!", "p@ss.word.with.dots"),
                lines.stream().map(d::decrypt).toList());
    }

    @Test
    void roundTripAndPolicies() throws Exception {
        FieldDecryptor d = new FieldDecryptor(props, keys);
        String enc = encrypt("secret", now(), "n-1", "test");
        assertEquals("secret", d.decrypt(enc));
        assertEquals("CRYPTO_REPLAY", code(() -> d.decrypt(enc)));                       // 같은 요청 재전송
        assertEquals("CRYPTO_KID", code(() -> d.decrypt(encryptUnchecked("x", now(), "n-2", "old"))));
        assertEquals("CRYPTO_EXPIRED", code(() -> d.decrypt(encryptUnchecked("x", now() - 600, "n-3", "test"))));
        String fresh = encrypt("x", now(), "n-4", "test");
        String tampered = fresh.substring(0, fresh.length() - 4) + (fresh.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertEquals("CRYPTO_DECRYPT", code(() -> d.decrypt(tampered)));                  // GCM 태그 불일치
        assertEquals("CRYPTO_FORMAT", code(() -> d.decrypt("ENC1.test.abc")));
    }

    @Test
    void plainValueFollowsRequiredFlag() {
        FieldDecryptor lenient = new FieldDecryptor(props, keys);
        assertEquals("plain", lenient.decrypt("plain"));
        assertNull(lenient.decrypt(null));
        assertEquals("", lenient.decrypt(""));

        props.setRequired(true);
        FieldDecryptor strict = new FieldDecryptor(props, keys);
        assertEquals("CRYPTO_REQUIRED", code(() -> strict.decrypt("plain")));
    }

    @Test
    void disabledPassesThroughWithoutKeys() {
        FieldCryptoProperties off = new FieldCryptoProperties();
        FieldDecryptor d = new FieldDecryptor(off, null);
        assertFalse(d.isEnabled());
        assertEquals("ENC1.whatever", d.decrypt("ENC1.whatever"));
    }

    @Test
    void jacksonFieldIsDecryptedOnDeserialization() throws Exception {
        new FieldDecryptor(props, keys);   // current() 등록
        String body = "{\"userId\":\"admin\",\"password\":\"" + encrypt("admin1234!", now(), "n-json", "test") + "\"}";
        Req r = new ObjectMapper().readValue(body, Req.class);
        assertEquals("admin", r.userId);
        assertEquals("admin1234!", r.password);
    }

    @Test
    void keyFilesMissing() {
        FieldCryptoProperties p = new FieldCryptoProperties();
        p.setEnabled(true);
        p.setPrivateKeyPath("/no/such.key.pem");
        p.setPublicKeyPath("/no/such.pub.pem");
        assertThrows(IllegalStateException.class, () -> FieldKeyHolder.load(p));   // 서버: 기동 실패
        p.setGenerateIfMissing(true);
        assertNotNull(FieldKeyHolder.load(p).publicKeyPem());                       // 로컬: 임시 키
    }

    private String encryptUnchecked(String v, long ts, String n, String kid) {
        try {
            return encrypt(v, ts, n, kid);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static class Req {
        public String userId;
        @EncryptedField
        public String password;
    }
}
```

#### 11-12. `Backend/nh-atms-api/atms-common/build.gradle` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\build.gradle`
- 목적: atms-common 테스트 실행기 (테스트 전용)

`spring-security-test` 줄 아래 2줄.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -37,6 +37,8 @@ dependencies {
 
     // Test
     testImplementation 'org.springframework.security:spring-security-test'
+    // atms-common 은 Spring Boot 플러그인이 없어 JUnit 실행기 버전을 맞춰 줘야 테스트가 돈다 (테스트 전용, WAR 영향 없음)
+    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
 }
 
 // 로컬 전용 DB 설정(local-common.yml)은 JEUS WAR 용 빌드(-Pjeus)에서 jar 에 넣지 않는다 — 개발서버는 JNDI (2026-11-02)
```

#### 11-13. 시험 리소스 3개 — **신규 (반입 권장)**

`atms-common\src\test\resources\field-crypto\` 에 `test-only.key.pem`, `test-only.pub.pem`, `node-forge-vectors.txt` 를 둔다 (시험 전용 키와 화면 코드로 만든 암호문 — 서버 키 아님). 손으로 옮겨 적기 어려우므로 GitHub 저장소의 같은 경로 파일을 반입하거나, 이 3개 없이 `FieldDecryptorTest` 를 빼도 된다.

### 11-C. atms-web

#### 11-14. `Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/auth/controller/FieldCryptoController.java` — **신규**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\java\kr\co\nh\atms\web\auth\controller\FieldCryptoController.java`
- 목적: `GET /api/auth/public-key`

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```java
package kr.co.nh.atms.web.auth.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.common.security.crypto.FieldPublicKeyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 민감필드 암호화용 공개키 (로그인 전 호출 — SecurityConfig permitAll).
 * 응답: {enabled, serverTime, kid, alg, publicKey(PEM)}. 설계: docs/FIELD_ENCRYPTION_DESIGN.md
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class FieldCryptoController {

    private final FieldPublicKeyProvider publicKeyProvider;

    @GetMapping("/public-key")
    public ApiResponse<Map<String, Object>> publicKey() {
        return ApiResponse.ok(publicKeyProvider.describe());
    }
}
```

#### 11-15. `Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/auth/dto/LoginRequest.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\java\kr\co\nh\atms\web\auth\dto\LoginRequest.java`
- 목적: 로그인 비밀번호 자동 복호화

import 1줄 + 어노테이션 1줄.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -2,6 +2,7 @@ package kr.co.nh.atms.web.auth.dto;
 
 import com.fasterxml.jackson.annotation.JsonIgnore;
 import jakarta.validation.constraints.NotBlank;
+import kr.co.nh.atms.common.security.crypto.EncryptedField;
 import lombok.Getter;
 import lombok.Setter;
 
@@ -13,6 +14,7 @@ public class LoginRequest {
     private String userId;
 
     @NotBlank(message = "비밀번호를 입력하세요")
+    @EncryptedField   // 화면이 ENC1 암호문으로 보냄 → 여기서 평문으로 (docs/FIELD_ENCRYPTION_DESIGN.md)
     private String password;
 
     @JsonIgnore
```

#### 11-16. `Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/user/entity/User.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\java\kr\co\nh\atms\web\user\entity\User.java`
- 목적: 사용자 등록 비밀번호 자동 복호화

import 1줄 + 어노테이션 1줄.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -1,5 +1,6 @@
 package kr.co.nh.atms.web.user.entity;
 
+import kr.co.nh.atms.common.security.crypto.EncryptedField;
 import lombok.AllArgsConstructor;
 import lombok.Builder;
 import lombok.Data;
@@ -15,6 +16,7 @@ import java.time.LocalDateTime;
 public class User {
     private String userId;
     private String userName;
+    @EncryptedField   // 사용자 등록 시 화면이 ENC1 암호문으로 보냄 (docs/FIELD_ENCRYPTION_DESIGN.md)
     private String password;
     private String empNo;
     private Long userGroupNo;
```

#### 11-17. `Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/user/controller/UserController.java` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\java\kr\co\nh\atms\web\user\controller\UserController.java`
- 목적: 비밀번호 변경 복호화 (Map 바디)

import 1줄, 필드 1줄, `changePassword` 안 1줄 교체.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -2,6 +2,7 @@ package kr.co.nh.atms.web.user.controller;
 
 import kr.co.nh.atms.common.page.PageResult;
 import kr.co.nh.atms.common.response.ApiResponse;
+import kr.co.nh.atms.common.security.crypto.FieldDecryptor;
 import kr.co.nh.atms.web.user.dto.UserSearchDto;
 import kr.co.nh.atms.web.user.entity.User;
 import kr.co.nh.atms.web.user.service.UserService;
@@ -15,6 +16,7 @@ import java.util.Map;
 @RequiredArgsConstructor
 public class UserController {
     private final UserService service;
+    private final FieldDecryptor fieldDecryptor;
 
     @GetMapping
     public ApiResponse<PageResult<User>> list(@ModelAttribute UserSearchDto s) {
@@ -42,7 +44,8 @@ public class UserController {
     }
     @PutMapping("/{userId}/password")
     public ApiResponse<Void> changePassword(@PathVariable String userId, @RequestBody Map<String, String> body) {
-        service.changePassword(userId, body.get("password"));
+        // 화면이 ENC1 암호문으로 보냄 → 평문으로 (Map 바디라 직접 복호화)
+        service.changePassword(userId, fieldDecryptor.decrypt(body.get("password")));
         return ApiResponse.ok(null, "비밀번호 변경 성공");
     }
 
```

#### 11-18. `Backend/nh-atms-api/atms-web/src/main/resources/application-jeus.yml` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\resources\application-jeus.yml`
- 목적: 서버 설정 (키 파일 경로)

파일 맨 끝에 추가.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -32,3 +32,15 @@ download:
 # 해당 서버로 바로 보낸다 (deploy/webtob/http.m.atms-jsv.txt) → atms-web 이 전달하지 않음
 gateway:
   enabled: false
+
+# 화면↔서버 민감필드(비밀번호) 암호화 — docs/FIELD_ENCRYPTION_DESIGN.md
+# 키 파일은 WAR 밖 /home/atmswas/keys (jeus 그룹 640, 같은 파일을 ws02 에도). 없으면 기동 실패한다.
+# required: 화면 배포 확인 후 true (그 전까지는 평문도 받음)
+crypto:
+  field:
+    enabled: true
+    required: false
+    kid: k202611
+    private-key-path: /home/atmswas/keys/field-k202611.key.pem
+    public-key-path: /home/atmswas/keys/field-k202611.pub.pem
+    max-skew-seconds: 120
```

#### 11-19. `Backend/nh-atms-api/atms-web/src/main/resources/application-local.yml` — **수정**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-web\src\main\resources\application-local.yml`
- 목적: 로컬 설정 (임시 키 자동 생성)

파일 맨 끝에 추가.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -20,3 +20,11 @@ JWT_SECRET: nh-atms-dev-secret-key-must-be-at-least-256-bits-long-for-hs256
 spring:
   config:
     import: classpath:local-common.yml
+
+# 화면↔서버 민감필드(비밀번호) 암호화 — 로컬은 키 파일 없이 기동 시 임시 키 자동 생성 (재기동마다 바뀜)
+crypto:
+  field:
+    enabled: true
+    required: false
+    kid: local
+    generate-if-missing: true
```

### 11-D. 화면 (Frontend/nh-atms-web)

#### 11-20. `Frontend/nh-atms-web/src/utils/fieldCrypto.ts` — **신규**

- Windows 경로: `NH-ATMS\Frontend\nh-atms-web\src\utils\fieldCrypto.ts`
- 목적: node-forge 로 비밀번호 암호화
- **axios 기반 그대로 쓴다** (2026-11-06 결정): 다른 화면은 `api/http.ts` 로 바꿨지만 이 파일은 반입본대로 `axios` 를 직접 쓴다 → **`package.json` 의 `axios` 의존성을 지우지 않는다** (`npm uninstall axios` 금지). 지우면 `npm run build` 가 `Cannot find module 'axios'` 로 실패한다

아래 내용 **전체**로 파일을 만든다 (UTF-8).

```ts
/**
 * 화면↔서버 민감필드 암호화 (HTTPS 미적용 환경) — docs/FIELD_ENCRYPTION_DESIGN.md
 *
 * 필드값 = "ENC1.<kid>.<b64 RSA-OAEP-SHA256(AES키)>.<b64 IV>.<b64 AES-256-GCM 암호문+태그>"
 * 평문   = {"v": 값, "ts": 서버기준 epoch초, "n": 1회용 난수}  (서버가 시각·중복으로 재전송을 막음)
 *
 * http 화면에서는 브라우저 crypto.subtle 을 쓸 수 없어 node-forge 로 암호화한다.
 * (난수는 forge 가 crypto.getRandomValues 를 사용 — 이것은 http 에서도 동작)
 */
import forge from 'node-forge';
import axios from 'axios';

interface PublicKeyInfo {
  enabled: boolean;
  serverTime: number;
  kid?: string;
  publicKey?: string;
}

interface CachedKey {
  enabled: boolean;
  kid?: string;
  key?: forge.pki.rsa.PublicKey;
  clockOffset: number; // 서버시각 - 화면시각 (초)
}

let cached: Promise<CachedKey> | null = null;

const nowSec = () => Math.floor(Date.now() / 1000);

function loadKey(): Promise<CachedKey> {
  if (!cached) {
    // 인증 토큰이 필요 없는 공개 API — 토큰 인터셉터가 없는 기본 axios 사용
    cached = axios
      .get<{ data: PublicKeyInfo }>('/api/auth/public-key')
      .then((res) => {
        const info = res.data.data;
        return {
          enabled: info.enabled,
          kid: info.kid,
          key: info.enabled && info.publicKey ? forge.pki.publicKeyFromPem(info.publicKey) : undefined,
          clockOffset: info.serverTime - nowSec(),
        };
      })
      .catch((e) => {
        cached = null;
        throw e;
      });
  }
  return cached;
}

/** 서버 키가 바뀌었을 때(CRYPTO_KID 등) 공개키를 다시 받도록 캐시를 버린다. */
export function resetFieldCrypto(): void {
  cached = null;
}

/** 서버 응답 코드가 암호화 관련 오류(CRYPTO_*)면 캐시를 버리고 true. 화면은 사용자에게 다시 시도하게 한다. */
export function handleFieldCryptoError(error: unknown): boolean {
  const code = (error as { response?: { data?: { code?: string } } })?.response?.data?.code;
  if (typeof code === 'string' && code.startsWith('CRYPTO_')) {
    resetFieldCrypto();
    return true;
  }
  return false;
}

/** 값을 ENC1 형식으로 암호화한다. 서버가 암호화를 끈 경우(enabled=false) 원래 값을 그대로 돌려준다. */
export async function encryptField(value: string): Promise<string> {
  if (!value) return value;
  const k = await loadKey();
  if (!k.enabled || !k.key || !k.kid) return value;

  const plain = JSON.stringify({
    v: value,
    ts: nowSec() + k.clockOffset,
    n: forge.util.bytesToHex(forge.random.getBytesSync(16)),
  });

  const aesKey = forge.random.getBytesSync(32);
  const iv = forge.random.getBytesSync(12);
  const cipher = forge.cipher.createCipher('AES-GCM', aesKey);
  cipher.start({ iv, tagLength: 128 });
  cipher.update(forge.util.createBuffer(forge.util.encodeUtf8(plain)));
  cipher.finish();
  const encrypted = cipher.output.getBytes() + cipher.mode.tag.getBytes();

  const wrapped = k.key.encrypt(aesKey, 'RSA-OAEP', {
    md: forge.md.sha256.create(),
    mgf1: { md: forge.md.sha256.create() },
  });

  return [
    'ENC1',
    k.kid,
    forge.util.encode64(wrapped),
    forge.util.encode64(iv),
    forge.util.encode64(encrypted),
  ].join('.');
}
```

#### 11-21. `Frontend/nh-atms-web/src/pages/login/LoginPage.tsx` — **수정**

- Windows 경로: `NH-ATMS\Frontend\nh-atms-web\src\pages\login\LoginPage.tsx`
- 목적: 로그인 시 암호화

import 1줄, 요청 1줄 교체, catch 에 1줄.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -4,6 +4,7 @@ import { UserOutlined, LockOutlined } from '@ant-design/icons';
 import { useNavigate } from 'react-router-dom';
 import { useAuthStore } from '@/stores/useAuthStore';
 import api from '@/api/axiosInstance';
+import { encryptField, handleFieldCryptoError } from '@/utils/fieldCrypto';
 
 const LoginPage: React.FC = () => {
   const navigate = useNavigate();
@@ -13,13 +14,15 @@ const LoginPage: React.FC = () => {
   const handleLogin = async (values: { userId: string; password: string }) => {
     setLoading(true);
     try {
-      const res = await api.post('/auth/login', values);
+      // 비밀번호는 암호화해서 보냄 (docs/FIELD_ENCRYPTION_DESIGN.md)
+      const res = await api.post('/auth/login', { ...values, password: await encryptField(values.password) });
       const { accessToken, refreshToken, user } = res.data.data;
 
       login(accessToken, refreshToken, user);
       message.success(`${user.userName}님, 환영합니다.`);
       navigate('/dashboard');
     } catch (error: any) {
+      handleFieldCryptoError(error); // 서버 키 교체 등 → 다음 시도에서 공개키를 다시 받음
       const msg = error.response?.data?.message || '로그인에 실패했습니다.';
       message.error(msg);
     } finally {
```

#### 11-22. `Frontend/nh-atms-web/src/api/userApi.ts` — **수정**

- Windows 경로: `NH-ATMS\Frontend\nh-atms-web\src\api\userApi.ts`
- 목적: 사용자 등록·비밀번호 변경 암호화

import 1줄, `create`·`changePassword` 교체.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -1,4 +1,5 @@
 import api from './axiosInstance';
+import { encryptField } from '@/utils/fieldCrypto';
 import type { PageResult } from './loginHistApi';
 
 interface ApiResponse<T> {
@@ -48,14 +49,15 @@ export const userApi = {
     api.get<ApiResponse<PageResult<User>>>('/user', { params }),
   get: (userId: string) =>
     api.get<ApiResponse<User>>(`/user/${userId}`),
-  create: (u: User) =>
-    api.post<ApiResponse<void>>('/user', u),
+  // 비밀번호는 암호화해서 보냄 (docs/FIELD_ENCRYPTION_DESIGN.md)
+  create: async (u: User) =>
+    api.post<ApiResponse<void>>('/user', { ...u, password: u.password ? await encryptField(u.password) : u.password }),
   update: (userId: string, u: User) =>
     api.put<ApiResponse<void>>(`/user/${userId}`, u),
   remove: (userId: string) =>
     api.delete<ApiResponse<void>>(`/user/${userId}`),
-  changePassword: (userId: string, password: string) =>
-    api.put<ApiResponse<void>>(`/user/${userId}/password`, { password }),
+  changePassword: async (userId: string, password: string) =>
+    api.put<ApiResponse<void>>(`/user/${userId}/password`, { password: await encryptField(password) }),
   unlock: (userId: string) =>
     api.put<ApiResponse<void>>(`/user/${userId}/unlock`),
   toggleUseYn: (userId: string, useYn: string) =>
```

#### 11-23. `Frontend/nh-atms-web/src/pages/system/UserMgmtPage.tsx` — **수정**

- Windows 경로: `NH-ATMS\Frontend\nh-atms-web\src\pages\system\UserMgmtPage.tsx`
- 목적: 오류 시 공개키 다시 받기

import 1줄, catch 2곳에 1줄씩.

`-` 줄은 지우고 `+` 줄은 추가한다 (앞의 `-`/`+`/공백 한 칸은 입력하지 않음).

```diff
@@ -24,6 +24,7 @@ import {
 import dayjs from 'dayjs';
 import PageTemplate from '@/components/PageTemplate';
 import { userApi, type User } from '@/api/userApi';
+import { handleFieldCryptoError } from '@/utils/fieldCrypto';
 import { userGroupApi, type UserGroup } from '@/api/userGroupApi';
 
 const userTypeOptions = [
@@ -116,6 +117,7 @@ const UserMgmtPage: React.FC = () => {
       fetchList(page, size);
     } catch (e: any) {
       if (e?.errorFields) return;
+      handleFieldCryptoError(e); // 서버 키 교체 등 → 다음 시도에서 공개키를 다시 받음
       message.error(e?.response?.data?.message ?? '저장 실패');
     }
   };
@@ -167,6 +169,7 @@ const UserMgmtPage: React.FC = () => {
       message.success('비밀번호가 변경되었습니다.');
       setPwModalOpen(false);
     } catch (e: any) {
+      handleFieldCryptoError(e); // 서버 키 교체 등 → 다음 시도에서 공개키를 다시 받음
       message.error(e?.response?.data?.message ?? '비밀번호 변경 실패');
     }
   };
```

#### 11-24. `package.json`·`package-lock.json`

**권장 (2026-11-06 결정) — npm 캐시 폴더째 반입**: 인터넷 PC 에서 `npm cache add node-forge@1.4.0 @types/node-forge@1.3.14 --cache D:\npm-carry` 로 만든 캐시 폴더(zip)를 반입해 내 npm 캐시에 합치면, 폐쇄망에서도 `npm install node-forge@1.4.0 --save-exact --offline` 이 그대로 되고 **두 json 파일을 npm 이 자동으로 고친다**. 절차: [REQUIRED_FILES.md](REQUIRED_FILES.md) 3-5 (①~③ PC, ④ Jenkins).

**tgz 만 이미 반입한 경우 (아래, 손으로 항목 추가)**:

> 정정: 처음 적었던 `npm install node-forge@1.4.0 --save-exact --offline` 은 **폐쇄망에서 `ENOTCACHED` 로 실패한다** (2026-11-06 PC 발생, 재현 확인). `npm cache add <tgz>` 는 파일 내용만 캐시에 넣고, 이름·버전으로 설치하려면 레지스트리 목록 정보(packument)가 더 필요하기 때문이다. 아래처럼 두 파일에 **버전·integrity 를 먼저 적어 두면** npm 이 integrity 로 캐시에서 바로 꺼내 설치한다 (Jenkins `npm ci --offline` 도 같은 원리).

① 반입 tgz 를 캐시에 넣는다 (`Frontend\nh-atms-web` 에서). **경로에 공백·한글이 있으면 큰따옴표로 감싸거나 공백 없는 폴더로 복사**한다 — 따옴표 없이 `D:\07. 반입파일\…` 을 쓰면 `D:\07.` 폴더로 잘못 읽혀 `DirFetcher requires either a tree or an Arborist constructor` 오류가 난다.
```bat
npm cache add "C:\반입\node-forge-1.4.0.tgz"
npm cache add "C:\반입\types-node-forge-1.3.14.tgz"
```
(아무것도 출력되지 않으면 정상)

② `package.json` — `"dependencies"` 안에 한 줄, `"devDependencies"` 안에 한 줄 (알파벳 순서 자리, 앞뒤 쉼표 주의):
```json
    "node-forge": "1.4.0",
```
```json
    "@types/node-forge": "1.3.14",
```

③ `package-lock.json` — 세 곳.
- 맨 위 `"packages": { "": { … } }` 안의 `"dependencies"` 에 `"node-forge": "1.4.0",`, `"devDependencies"` 에 `"@types/node-forge": "1.3.14",` (②와 같은 두 줄)
- 같은 `"packages"` 안, 다른 `"node_modules/…"` 항목들 사이에 아래 두 덩어리를 추가 (각 덩어리 뒤 쉼표)
```json
    "node_modules/@types/node-forge": {
      "version": "1.3.14",
      "resolved": "https://registry.npmjs.org/@types/node-forge/-/node-forge-1.3.14.tgz",
      "integrity": "sha512-mhVF2BnD4BO+jtOp7z1CdzaK4mbuK0LLQYAvdOLqHTavxFNq4zA1EmYkpnFjP8HOUzedfQkRnp0E2ulSAYSzAw==",
      "dev": true,
      "license": "MIT",
      "dependencies": {
        "@types/node": "*"
      }
    },
```
```json
    "node_modules/node-forge": {
      "version": "1.4.0",
      "resolved": "https://registry.npmjs.org/node-forge/-/node-forge-1.4.0.tgz",
      "integrity": "sha512-LarFH0+6VfriEhqMMcLX2F7SwSXeWwnEAJEsYm5QKWchiVYVvJyV9v7UDvUv+w5HO23ZpQTXDv/GxdDdMyOuoQ==",
      "license": "(BSD-3-Clause OR GPL-2.0)",
      "engines": {
        "node": ">= 6.13.0"
      }
    },
```
`resolved` 는 인터넷 주소지만 실제로 접속하지 않는다 (`--offline` 이면 integrity 로 캐시에서 찾음). Jenkins 도 이 lock 으로 `npm ci --offline` 하므로 이 모양 그대로 둔다.

④ 설치·빌드:
```bat
npm install --offline
npm run build
```
확인: `dir node_modules\node-forge` 가 있고, `git diff package.json package-lock.json` 에 위에서 넣은 줄만 보이면 된다. `"file:…"` 경로가 들어갔으면 지우고 ②~④ 를 다시 한다 (tgz 경로로 `npm install <파일>` 을 하면 생김 — 다른 PC·Jenkins 빌드가 깨진다).

### 11-E. 확인 (폐쇄망 PC)

1. `gradlew.bat :atms-web:bootRun --offline` → 로그 `[field-crypto] 키 파일 없음 → 임시 키 생성 (로컬 전용…)`
2. 브라우저 `http://localhost:8080/api/auth/public-key` → `"enabled":true`, `"kid":"local"`, `"publicKey":"-----BEGIN PUBLIC KEY-----…"`
3. `npm run dev` → 로그인. F12 → Network → `login` 요청의 `password` 가 `ENC1.local.…` 이고 로그인 성공
4. 사용자관리 → 비밀번호 변경·사용자 등록도 `ENC1.` 로 가는지 확인
5. 서버 반영 전 키 생성은 설계서 5장 (키 파일이 없으면 atms01 이 기동하지 못함)


---

## 12. 개정 2026-11-05 — 로컬 DB 계정 PC별 분리 + 계정 잠김 방지

**왜**: 계정이 `local-common.yml` 한 곳(=Gitea)에 있어 PC마다 다른 계정을 쓸 수 없었고, 다른 PC에서는 남의 계정·틀린 비번으로 접속했다. `gradlew build` 는 테스트를 돌리는데 atms-web `AtmsWebApplicationTests`(`@SpringBootTest`)가 실제 DB 에 로그인하고, 앱 기동 때도 atms-web(`AdminPasswordInitializer`)·atms-batch 가 바로 접속한다 → 빌드·재기동마다 실패가 쌓여 Oracle 이 계정을 잠금 → 현업에 해제 요청.

**어떻게** (3중 방지):

| | 내용 | 파일 |
|---|---|---|
| 계정 분리 | 각 PC 의 `D:\local-db.yml` 에 본인 계정. 저장소·WAR 에 없음 | 12-1, 12-5 |
| ① | DB 에 접속하는 테스트(`@Tag("db")`)는 빌드 기본 제외 (`-PdbTest` 일 때만) | 12-2, 12-3 |
| ② | `D:\local-db.yml` 이 없거나 비어 있으면 **DB 접속 없이** 기동 중단 | 12-4 |
| ③ | 로그인 실패(ORA-01017 아이디/비번 틀림, ORA-28000 잠김)를 `D:\db-login-failed` 에 기록 → 같은 주소·계정·비번으로는 다시 접속 안 함 (재기동·재빌드 포함). 비번을 고치면 자동으로 1회 다시 시도. 기록에는 해시만 남음 | 12-4 |

개발서버(jeus 프로파일, JNDI)에는 영향 없음 (`@Profile("local")`, `-Pjeus` WAR 에는 `local-common.yml` 없음). atms-web 은 p6spy 가 DataSource 를 감싸므로 안쪽 Hikari 를 꺼내 검사한다.

### 12-1. `atms-common/src/main/resources/local-common.yml` — **전체 교체**

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\resources\local-common.yml`
- 계정·비밀번호 줄이 빠지고 `D:/local-db.yml` 을 불러온다. 경로는 슬래시(`/`)로 적는다

```yaml
# =====================================================================
# 로컬(PC, bootRun·IntelliJ, 프로파일 local) 공통 DB 접속 — 2026-11-02, PC별 계정 분리 2026-11-05
#  - 6개 모듈의 application-local.yml 이 spring.config.import 로 읽는다 → DB 주소는 이 파일 한 곳만 고친다
#  - DB 계정·비밀번호는 PC마다 다르므로 저장소에 두지 않는다 → 각 PC의 D:\local-db.yml (Git·WAR 밖)
#    D:\local-db.yml 이 없거나 비어 있으면 DB 에 접속하지 않고 기동을 멈춘다 (atms-common LocalDbLoginGuard)
#    로그인 실패는 D:\db-login-failed 에 기록되어 같은 계정·비번으로 다시 접속하지 않는다 (계정 잠김 방지)
#  - 우선순위: D:\local-db.yml > 이 파일 > 각 모듈 application-local.yml > application.yml (OS 환경변수·-D 는 더 우선)
#  - 개발서버(jeus 프로파일)는 JEUS 데이터소스(JNDI dsATMS01)를 쓴다 → jeus-common.yml
#  - -Pjeus WAR 에는 이 파일이 들어가지 않는다 (atms-common/build.gradle)
# =====================================================================

spring:
  config:
    # PC별 계정 파일 (Git 밖): D:\local-db.yml — 없으면 기동 중단(LocalDbLoginGuard)
    import: optional:file:D:/local-db.yml
  datasource:
    # 은행 개발 DB nbatmsltdb01 (서비스 NBATMSTP, 문자셋 KO16MSWIN949 → orai18n 필요, atms-common/build.gradle)
    url: jdbc:oracle:thin:@//16.88.250.91:1531/NBATMSTP
    # 계정·비번은 여기 적지 않는다 → 위 local-db.yml 이 덮어씀
    username: ""
    password: ""
    hikari:
      initialization-fail-timeout: 1   # 접속 실패 시 재시도 없이 바로 실패
```

### 12-2. 루트 `build.gradle` — **수정** (`tasks.named('test')` 블록)

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\build.gradle` (`subprojects { … }` 안)

바꾸기 전:
```groovy
    tasks.named('test') {
        useJUnitPlatform()
    }
```
바꾼 후:
```groovy
    tasks.named('test') {
        // 실제 DB 에 접속하는 테스트(@Tag("db"))는 기본 제외 — 로컬 계정 잠김 방지. 돌리려면 -PdbTest (2026-11-05)
        useJUnitPlatform {
            if (!project.hasProperty('dbTest')) {
                excludeTags 'db'
            }
        }
    }
```

### 12-3. `atms-web/src/test/java/kr/co/nh/atms/web/AtmsWebApplicationTests.java` — **수정**

- `import org.junit.jupiter.api.Tag;` 와 `@Tag("db")` 두 줄 추가. 전체:

```java
package kr.co.nh.atms.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("db")   // 실제 DB 접속 → 빌드 기본 제외, -PdbTest 일 때만 (2026-11-05)
@SpringBootTest
class AtmsWebApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

### 12-4. `atms-common/src/main/java/kr/co/nh/atms/common/db/LocalDbLoginGuard.java` — **신규** (새 폴더 `db`)

- Windows 경로: `NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\db\LocalDbLoginGuard.java`
- 6개 모듈 모두 `scanBasePackages = "kr.co.nh.atms"` 라 따로 등록하지 않아도 적용된다

```java
package kr.co.nh.atms.common.db;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;

/**
 * 로컬(PC, 프로파일 local) 전용 DB 계정 잠김 방지 (2026-11-05).
 * - 계정 파일 D:\local-db.yml 이 없거나 비어 있으면 DB 에 접속하지 않고 기동을 멈춘다.
 * - 로그인 실패(ORA-01017 아이디/비번 틀림, ORA-28000 잠김)를 D:\db-login-failed 에 기록하고,
 *   같은 주소·계정·비번으로는 다시 접속하지 않는다 (재기동·재빌드 포함).
 *   비번을 고치면 자동으로 1회 다시 시도한다. 기록 파일에는 해시만 남고 비번은 남지 않는다.
 * 개발서버(jeus 프로파일, JNDI)에는 적용되지 않는다.
 */
@Slf4j
@Component
@Profile("local")
public class LocalDbLoginGuard implements BeanPostProcessor {

    private static final Path DIR = Path.of("D:/");   // PC별 파일은 D 드라이브 루트
    private static final Path ACCOUNT_FILE = DIR.resolve("local-db.yml");
    private static final Path FAILED_FILE = DIR.resolve("db-login-failed");
    private static final Set<Integer> LOGIN_ERRORS = Set.of(1017, 28000);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // p6spy 등이 먼저 감싸도 안쪽 Hikari 를 찾아 검사한다
        if (!(bean instanceof DataSource outer) || bean instanceof Guarded) {
            return bean;
        }
        HikariDataSource ds = unwrapHikari(outer);
        if (ds == null) {
            return bean;
        }
        if (isBlank(ds.getUsername()) || isBlank(ds.getPassword())) {
            throw new IllegalStateException("[DB-GUARD] 로컬 DB 계정이 없습니다. " + ACCOUNT_FILE
                    + " 에 spring.datasource.username / password 를 적으세요. (DB 접속은 시도하지 않았습니다)");
        }
        String key = fingerprint(ds.getJdbcUrl(), ds.getUsername(), ds.getPassword());
        if (alreadyFailed(key)) {
            throw new IllegalStateException("[DB-GUARD] 이 계정(" + ds.getUsername()
                    + ")·비밀번호는 이미 로그인에 실패한 기록이 있어 DB 에 접속하지 않습니다 (계정 잠김 방지). "
                    + ACCOUNT_FILE + " 의 비밀번호를 고치면 자동으로 다시 시도합니다. "
                    + "비밀번호가 맞는데 잠김 해제를 받은 경우에만 " + FAILED_FILE + " 파일을 지우세요.");
        }
        return new Guarded(outer, key, ds.getUsername());
    }

    private static final class Guarded extends DelegatingDataSource {
        private final String key;
        private final String username;
        private volatile SQLException blocked;

        Guarded(DataSource target, String key, String username) {
            super(target);
            this.key = key;
            this.username = username;
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (blocked != null) {
                throw new SQLException("[DB-GUARD] 로그인 실패 후 재접속 차단: " + blocked.getMessage(), blocked);
            }
            try {
                return super.getConnection();
            } catch (SQLException e) {
                Integer code = loginErrorCode(e);
                if (code != null) {
                    blocked = e;
                    record(key, code, username);
                }
                throw e;
            }
        }
    }

    private static HikariDataSource unwrapHikari(DataSource ds) {
        try {
            return ds.isWrapperFor(HikariDataSource.class) ? ds.unwrap(HikariDataSource.class) : null;
        } catch (SQLException e) {
            return null;
        }
    }

    private static Integer loginErrorCode(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException s && LOGIN_ERRORS.contains(s.getErrorCode())) {
                return s.getErrorCode();
            }
        }
        return null;
    }

    private static void record(String key, int code, String username) {
        try {
            String line = key + " " + LocalDateTime.now().withNano(0) + " ORA-" + String.format("%05d", code)
                    + " " + username + System.lineSeparator();
            Files.writeString(FAILED_FILE, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) {
            log.warn("[DB-GUARD] 실패 기록 저장 못함 {} : {}", FAILED_FILE, ex.getMessage());
        }
        log.error("[DB-GUARD] DB 로그인 실패 ORA-{} (계정 {}) → 이 계정·비번으로는 더 접속하지 않습니다. {} 를 확인하세요.",
                String.format("%05d", code), username, ACCOUNT_FILE);
    }

    private static boolean alreadyFailed(String key) {
        try {
            return Files.exists(FAILED_FILE)
                    && Files.readAllLines(FAILED_FILE, StandardCharsets.UTF_8).stream().anyMatch(l -> l.startsWith(key));
        } catch (Exception ex) {
            return false;
        }
    }

    private static String fingerprint(String url, String user, String password) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest((url + "\n" + user + "\n" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
```

### 12-5. 각 PC 에서 1회 — `D:\local-db.yml` 만들기 (저장소 밖)

`cmd`:
```
notepad D:\local-db.yml
```
"새 파일을 만드시겠습니까?" → 예. 아래 4줄을 입력하고 저장한다.

```yaml
spring:
  datasource:
    username: '본인DB계정'
    password: '본인비밀번호'
```

- 값은 작은따옴표로 감싼다 (`^ ! # :` 등 특수문자). 값 안의 작은따옴표는 `''` 로 두 번
- **들여쓰기는 스페이스만** — `datasource:` 앞 2칸, `username:`/`password:` 앞 4칸. **Tab 키 금지**
- 프로젝트 폴더로 복사하지 않는다 (Git 에 올라갈 수 있음)

#### Tab 을 넣었을 때 (실제 발생 2026-11-05)

증상 — 기동 직후:
```
org.yaml.snakeyaml.scanner.ScannerException: while scanning for the next token
found character '\t(TAB)' that cannot start any token. (Do not use \t(TAB) for indentation)
 in 'reader', line 2, column 1:
```
파일을 읽는 단계라 **DB 접속은 일어나지 않는다** (잠김 위험 없음). 메모장에서 내용을 지우고 스페이스로 다시 입력한다 (IntelliJ 로 열면 yml 의 Tab 을 스페이스로 넣어 준다).

Tab 검사 (PowerShell, **아무것도 안 나오면 정상**. `` `t `` 의 백틱은 숫자 1 왼쪽 키):
```
Select-String -Path D:\local-db.yml -Pattern "`t"
```
내용 확인:
```
Get-Content D:\local-db.yml
```

### 12-6. 확인 (폐쇄망 PC)

| 확인 | 기대 결과 |
|---|---|
| `gradlew.bat build --offline` (또는 `test`) | 성공, `AtmsWebApplicationTests` 는 실행되지 않음 (DB 로그인 0회) |
| `D:\local-db.yml` 이름을 잠시 바꾸고 atms-web 실행 | `[DB-GUARD] 로컬 DB 계정이 없습니다. D:\local-db.yml …` 로 멈춤 (DB 접속 없음) → 이름 되돌리기 |
| 정상 파일로 atms-web 실행 → 로그인 | 정상 |
| DB 테스트를 일부러 돌릴 때 | `gradlew.bat test -PdbTest --offline` |

상황별:

| 콘솔 메시지 | 조치 |
|---|---|
| `[DB-GUARD] 로컬 DB 계정이 없습니다` | `dir D:\local-db*` — `local-db.yml.txt` 로 저장됐는지 확인 |
| `[DB-GUARD] DB 로그인 실패 ORA-01017` | 계정·비번 오류. `D:\local-db.yml` 을 고치면 1회만 다시 시도 |
| `[DB-GUARD] … 이미 로그인에 실패한 기록이 있어` | 비번을 고치지 않고 재실행한 경우. 비번은 맞았고 잠김만 해제받았다면 `del D:\db-login-failed` |
| `[DB-GUARD] 실패 기록 저장 못함` | D 드라이브 루트 쓰기 권한 없음 → 차단은 그 실행 중에만 동작 |
| `ScannerException … TAB` | 12-5 의 Tab 대처 |

주의: 비번을 바꾼 직후 첫 실행은 **atms-web 하나만** 띄워 확인한다 (6개를 동시에 띄우면 기록이 남기 전에 모듈마다 1회씩 시도할 수 있다).

검증(2026-11-05, 저장소 사본, Linux·대체 환경): 빌드 성공(`contextLoads` 제외), atms-web·atms-batch 계정 파일 없음 → `[DB-GUARD]` 중단·Hikari 시도 0회, 실패 기록 있음 → 중단·시도 0회, ORA-01017 흉내 → 첫 시도만 DB 로 가고 이후 차단·비번 변경 시 1회 재시도, 네트워크 오류(ORA-12541)는 기록 안 함, `D:/local-db.yml` 의 작은따옴표 값(`^ !` 포함) 정상 적용. 실제 Windows D 드라이브·은행 DB 의 ORA-01017 은 폐쇄망 PC 첫 실행 때 확인.
