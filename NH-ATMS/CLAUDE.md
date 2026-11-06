# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

NH-ATMS is a Korean banking (NongHyup / 농협은행) ATM fleet management system: real-time device monitoring, fault management, remote control, transaction/electronic-journal ingestion, and batch settlement. It's a rewrite of a legacy Saemaul Geumgo (새마을금고) ATMS (Spring 4 / eGov / MySQL), migrated to Spring Boot 3.5 / Java 21 / Oracle 19c. A lot of naming (table names, batch job codes, protocol field names) still reflects that legacy origin — see `Document/00_DB분석_모듈매핑.md` and `Document/01_모듈분석_마이그레이션가이드.md` for the AS-IS mapping.

Repo layout: `Backend/nh-atms-api` (Gradle multi-module Spring Boot API), `Frontend/nh-atms-web` (React), `Document/` (design docs, mostly Korean), `infra/` (Jenkins, Elastic dashboard provisioning), `tools/agent-e2e/` (PowerShell scripts for testing the real ATM agent protocol end-to-end), `docker-compose.yml` (full-stack deploy).

## Commands

### Backend (`Backend/nh-atms-api`, Gradle multi-module)

```bash
# run one service (dev, from IntelliJ or CLI) — H2 is NOT used; see "Local DB" gotcha below
./gradlew :atms-web:bootRun          # port 8080 — main screen/REST API
./gradlew :atms-journal:bootRun      # port 8081 — journal/agent ingestion
./gradlew :atms-batch:bootRun        # port 8082 — batch/scheduler
./gradlew :atms-file:bootRun         # port 8083 — file/image/firmware
./gradlew :atms-if-internal:bootRun  # port 8084 — internal (EAI) interface
./gradlew :atms-if-external:bootRun  # port 8085 — external interface

./gradlew build                      # build everything
./gradlew :atms-web:bootJar          # fat jar for one module
./gradlew bootWar -Pjeus             # WARs for the bank's JEUS server, all 6 modules (embedded Tomcat → providedRuntime,
                                     #   WEB-INF/web.xml activates profile "jeus"); without -Pjeus nothing changes
./gradlew :atms-web:bootWar -Pjeus   # one module

./gradlew test                                 # all tests
./gradlew :atms-web:test                       # one module
./gradlew :atms-web:test --tests "kr.co.nh.atms.web.atm.service.AtmServiceTest"   # single class
```

Local runs need `JWT_SECRET` and the DB settings: `application.yml` defaults `spring.profiles.active=local`, so each module's `src/main/resources/application-local.yml` is loaded — it holds `JWT_SECRET` and `spring.config.import: classpath:local-common.yml`, and **`atms-common/src/main/resources/local-common.yml`** is the one place for the local DB url (bank Oracle `jdbc:oracle:thin:@//16.88.250.91:1531/NBATMSTP`). **Since 2026-11-05 the account/password live per PC in `D:\local-db.yml`** (`spring.datasource.username/password`, space-indented YAML — a Tab gives `ScannerException`), imported with `optional:file:D:/local-db.yml`; `atms-common/.../common/db/LocalDbLoginGuard` (profile `local` only) refuses to start without it (no DB contact) and, after one ORA-01017/28000, writes a hash to `D:\db-login-failed` and never retries the same url/user/password (fix the password to retry once; delete the file only after an unlock with an unchanged password). Env vars `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` still override. Tests that hit the DB (`AtmsWebApplicationTests`) are `@Tag("db")` and skipped by `./gradlew test` unless `-PdbTest` — this is what stopped builds from locking bank DB accounts.

### Frontend (`Frontend/nh-atms-web`)

```bash
npm install
npm run dev       # Vite dev server on :3000 (see vite.config.ts, not the 5173 default)
npm run build      # tsc -b && vite build
npm run lint
```

There is no frontend test runner configured (no vitest/jest in `package.json`) despite `Document/10_개발표준가이드.md` describing one — don't assume `npm run test` exists.

The dev server proxies `/api/batch/**` → `localhost:8082` (atms-batch) and everything else under `/api/**` → `localhost:8080` (atms-web), whose `local`-profile gateway forwards `/api/file`, `/api/agent`, `/api/journal`, `/api/if/*` on to the owning module; run the relevant backend module(s) alongside `npm run dev`.

### Full stack (Docker Compose, repo root)

```bash
cp .env.example .env   # then fill SPRING_DATASOURCE_PASSWORD and JWT_SECRET at minimum
docker compose up -d --remove-orphans
docker compose logs -f atms-web
docker compose down
```

Brings up all 6 backend services + frontend (nginx) + Kafka + Elasticsearch + Kibana. Oracle itself is **not** containerized — the Compose `dev` profile still points at the vendor's Oracle `192.168.28.21:1521/ATMSPDB` (vendor environment only; the bank uses `local-common.yml` locally and JNDI on JEUS); there's no embedded DB path. Host ports are 19080-19086 (see table in `docker-compose.yml` header comment); container-internal ports are the same as the `bootRun` ports above.

### CI/CD

The `Jenkinsfile` is deploy-only: on push to `main` it SSHes to the deploy host, `git pull`s, and runs `docker compose build && up`. **It does not run tests, lint, or any quality gate** — run `./gradlew test` / `npm run lint` yourself before pushing. (The elaborate CI quality-gate/coverage/SonarQube pipeline described in `Document/10_개발표준가이드.md` is aspirational, not implemented.)

## Architecture

### Backend: 6 deployable services + 1 shared library

```
atms-common   shared lib (not bootable): SecurityConfig, JWT filter/provider, SSO abstraction,
              ApiResponse<T>/PageResult<T>, BusinessException + GlobalExceptionHandler, audit logging AOP
atms-web          :8080  main screen REST API — ~24 domains (atm, branch, user, journal, faultmgmt, remotecontrol, ...)
atms-journal      :8081  ATM-agent-facing ingestion: legacy binary journal protocol, remote-command dispatch
atms-batch        :8082  settlement/statistics batch jobs, DB-driven dynamic cron scheduler
atms-file         :8083  file/image transfer, Excel (POI), firmware SFTP, legacy NIO socket file server
atms-if-internal  :8084  internal (EAI) bank-system interface — TCP gateway, mock by default
atms-if-external  :8085  external-institution interface — fixed-length wire protocol, mock by default
```

Rule enforced by Gradle dependencies: **every module depends only on `atms-common`; domain modules never depend on each other.** Cross-module calls happen over HTTP (see below), never `project(':atms-x')`.

### Per-domain layering (inside atms-web and friends)

Each business domain is a vertical package, not a horizontal layer:
`kr.co.nh.atms.{module}.{domain}.{controller,service,mapper,entity,dto}` (e.g. `kr.co.nh.atms.web.atm.controller.AtmController`). Adding a domain means adding a whole new package, not touching a shared "controllers" folder.

- **Controller**: `@RestController` + `@RequiredArgsConstructor`, returns `ApiResponse<T>` (`success`/`code`/`message`/`data`). No business logic.
- **Service**: `@Transactional(readOnly = true)` at class level, `@Transactional` re-declared on write methods. Constructor injection only. Throws `BusinessException("CODE", "message")` for domain errors (`GlobalExceptionHandler` converts these to a 200 `ApiResponse` with `success=false`, not an HTTP error — mirror this, don't invent new exception→HTTP-status mappings).
- **Mapper**: MyBatis `@Mapper` interface + `resources/mapper/{domain}/{Domain}Mapper.xml`. `type-aliases-package` is scoped per-module (e.g. `kr.co.nh.atms.web`, but `kr.co.nh.atms.file.image.entity` for atms-file and `kr.co.nh.atms.ifin.entity`/`kr.co.nh.atms.ifex.entity` for the IF modules) — **keep it scoped to an entity package, don't widen it to the whole module package**, or SqlSessionFactory init fails at boot when local classes inside converter/protocol code produce duplicate MyBatis type aliases (this already broke if/external once — see commit `0d9d39f`).
- **Entity**: `@Data` (not separate `@Getter`/`@Setter`), maps 1:1 to an Oracle `T_*` table, plain fields (no JPA annotations — this is MyBatis, not JPA/Hibernate).
- Pagination: Oracle `OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY`, wrapped in `PageResult<T>` (1-based `page`).
- Timestamps are written as separate `VARCHAR` date/time columns via `TO_CHAR(SYSDATE,'YYYYMMDD'/'HH24MISS')`, not native `DATE`/`TIMESTAMP` — follow the existing pattern per table rather than introducing `LocalDateTime` columns.
- Reference implementation to copy when adding a new domain: `atm/` package in atms-web, plus its writeup in `Backend/nh-atms-api/atms-web/README.md`.

### Auth

Stateless JWT (HS256, `atms-common`'s `JwtAuthenticationFilter`/`JwtTokenProvider`), identical `SecurityConfig` inherited by every module (each has its own CORS `allowed-origins`). `sso.provider` is `local`/`mock` in every profile today — no real NH SSO integration is wired up yet. Local/dev login is `admin` / `admin1234!`; `AdminPasswordInitializer` (atms-web) re-hashes the seeded admin password on boot when `init.admin.reset-password=true`, so it stays usable even if the seed BCrypt hash drifts.

### JEUS deployment (bank dev server) — 6 managed servers, one per module

Current target (decisions 2026-10-26/27, DB 2026-11-02; operator procedure `../docs/INFRA_MANUAL.md`): one JEUS 9.1 domain on ws01 (`16.88.122.113`) with **six managed servers, one JVM and one port per module, every context `/`** — the same layout as local `bootRun`:

| Module | Server | Port | docBase (`/home/atmswas/…`) |
|---|---|---|---|
| atms-web | atms01 | 8080 | `catms` |
| atms-journal | ajou01 | 8081 | `cajou` |
| atms-batch | abat01 | 8082 | `cabat` |
| atms-file | file01 | 8083 | `cfile` |
| atms-if-internal | ifin01 | 8084 | `cifin` |
| atms-if-external | ifex01 | 8085 | `cifex` |

Pieces (all inert unless built with `-Pjeus`, which makes `jeus/web.xml` set `spring.profiles.active=jeus` per WAR):

- Each `Atms*Application` extends `SpringBootServletInitializer`; `./gradlew bootWar -Pjeus` builds WARs (embedded Tomcat → `providedRuntime`) and drops `application-local.yml` (root `build.gradle`) and `local-common.yml` (`atms-common/build.gradle`) so no local DB password reaches the server.
- `application-jeus.yml` per module imports `atms-common`'s `jeus-common.yml`: `jeus.host` (16.88.122.113) + `jeus.url.web…ifex` (`http://${jeus.host}:8080…8085`) used for every module-to-module call (`agent.proxy.journal-base-url`, `atmf.remote.snd-msg-uri`, `if.queue.*-base-url`, …), `jeus.log-dir` `/WASlogs/jeusATMSlogs/nbatmsltws01/app` (`<module>.log`), `jeus.data-dir` `/home/atmswas/data`, `jeus.agent-entry` (`host: CHANGE_ME_L4_VIP`, journal-port 8081, file-port 8083 — sent to agents in the open response), JWT/CORS `CHANGE_ME` values filled only in the bank-internal Gitea copy. Imported values win over the importing file; per-server JVM options (`-Djeus.host=…`, `-Djeus.log-dir=…`, used for the planned ws02 node) win over both.
- **DB on JEUS = JNDI only**: `spring.datasource.jndi-name: dsATMS01` (no url/user/password/Hikari in the WAR, no thin fallback). The WAS staff must point `dsATMS01` at Oracle (16.88.250.91:1531 NBATMSTP, ATMS_APP), target all six servers, and put Oracle JDBC **and orai18n** in the JEUS library — the WAR's copies aren't used by the JEUS pool.
- **No gateway on the server**: atms-web's `application-jeus.yml` sets `gateway.enabled: false`; WebtoB (wb01:9000) routes by path to each server over JSV (`/api/journal/`, `/api/agent/` → ajou01, `/api/batch/` → abat01, `/api/file/`, `/atmf/` → file01, `/api/if/internal/` → ifin01, `/api/if/external/` → ifex01, rest → atms01). ATM agents come through the internal L4 **directly** to ajou01:8081 (REST journal), file01:8083 (REST file) and file01:21001 (NIO socket server, `atmf.server.enabled: true` in atms-file's jeus profile, bound to `${jeus.host}`); legacy `.do` paths exist in atms-journal too, so direct 8081 works for them.
- `kr.co.nh.atms.web.gateway.ModuleGatewayFilter` stays in the source for the **`local` profile only** (last document of atms-web `application.yml`, targets `localhost:8081-8085`, override with `ATMS_JOURNAL_URL`/`ATMS_BATCH_URL`/`ATMS_FILE_URL`/`ATMS_IF_INTERNAL_URL`/`ATMS_IF_EXTERNAL_URL`, disable with `GATEWAY_ENABLED=false`), so `bootRun` on 8080 serves `/api/file/**` etc. On connection failure it writes a 502 JSON body itself — don't switch to `sendError`: the `/error` re-dispatch carries no auth, Spring Security turns it into 401, and the frontend logs the user out. Docker Compose (`dev`) leaves it off.
- Path matching must still use the path within the application (`getRequestURI()` minus `getContextPath()`, see `HmacAuthFilter`) so code keeps working under any context.
- Threads/schedulers you create yourself (not as Spring beans) must be stopped on `@PreDestroy`/`SmartLifecycle.stop()` (see `DynamicBatchScheduler.shutdown()`).
- Don't merge modules into one WAR: MyBatis aliases, bean names, security chains and yml keys collide.
- atms-batch needs a reachable DB at startup (Spring Batch job repository); the others start without one.
- Known vendor bug (found 2026-10-30, not fixed): `LegacyAgentController.ping` sits inside an unclosed `/** …` comment, so `/api/agent/ping` and `/atmr/agent/ping.do` are not mapped (agent polling 404s).
- Planned 2호기 (ws02, same domain): same WAR, per-server JVM options `-Djeus.host=<ws02 IP>` and `-Djeus.log-dir=/WASlogs/jeusATMSlogs/nbatmsltws02/app`; open data issues (batch running twice → `T_BATCH_JOB.HOST_NAME`, per-node `data` dir, in-memory refresh tokens) in `../docs/INFRA_MANUAL.md` 14-8.

History (superseded, kept for context): 2026-10-12~25 the six WARs ran in **one** server (`nbatmsltws01_atms01`, port 8766) under context paths `/`, `/journal`, `/batch`, `/file`, `/ifin`, `/ifex`, with the atms-web gateway forwarding the context-less paths and Spring Hikari in the WAR. That is where the context-aware `HmacAuthFilter` fix, the 502 gateway response and the batch scheduler shutdown came from (`../docs/JEUS_SOURCE_CHANGES.md`, gateway performance review §12).

Explanation and server-side requests: `../docs/INFRA_MANUAL.md` (start here), `../docs/JEUS_MANAGED_SERVERS_DECISION.md`, `../docs/WAS_REQUEST_MANAGED_SERVERS.md`, per-file closed-network apply guide `../docs/NH-ATMS_LOCAL_APPLY_GUIDE.md` (repo root `docs/`).

### Legacy ATM agent integration (spans atms-web + atms-journal)

Real ATM agent firmware is hardcoded to call **atms-web** (`:8080`/`19081`) at fixed legacy paths (`/atms/agent/**`, `/atmr/agent/**`, `/atms/remote-exec/**`) and cannot be changed. The actual handling logic for those paths lives in **atms-journal**. `LegacyAgentGatewayController` in atms-web is a reverse proxy that forwards those paths byte-for-byte to atms-journal (propagating `X-Forwarded-For`) so the protocol logic has a single source of truth. If you're chasing an agent-facing endpoint, check atms-journal even if the request enters through atms-web.

Journal ingestion pipeline (atms-journal): binary JIF messages → `JifFieldUtils`/`JifSpecConverter` parse the legacy wire format → `JournalDispatchService.dispatch()` inserts the `T_JOURNAL` header, then looks up a `JournalHandler` by `jnl_kind` from a `Map` built at `@PostConstruct` from every `JournalHandler` Spring bean. **To support a new journal kind, add a new `JournalHandler` implementation annotated `@Component` — it self-registers, no dispatcher edit needed.** Failures at any stage are swallowed and persisted to `T_JOURNAL_ERROR` (never thrown back to the agent) so the device doesn't get stuck retrying; `JournalErrorRetryWorker` reprocesses them later.

Gotcha worth knowing before touching `JournalDispatchService`: its internal transactional methods (`doDispatchInTx`, `saveError`) are called via **self-invocation** (`this.x()`), so a plain `@Transactional` annotation on them is silently ignored (Spring proxies don't intercept self-calls) — this previously caused per-statement autocommit instead of one transaction (fixed in `9cf226e`). They intentionally use explicit `TransactionTemplate`/`PlatformTransactionManager` instead. Don't "simplify" that back to `@Transactional`.

Journal streaming (optional, off by default via `atms.stream.kafka.enabled`/`atms.stream.elasticsearch.enabled`): after a successful DB insert, `JournalEventPublisher` publishes to Kafka; `JournalEsIndexConsumer` indexes into per-day Elasticsearch indices (`atms-journal-YYYY.MM.dd`) for the Kibana ops dashboard. docker-compose turns both on; local `bootRun` leaves them off unless you export `ATMS_KAFKA_ENABLED`/`ATMS_ES_ENABLED`.

`tools/agent-e2e/` has PowerShell scripts (`New-Journal.ps1`, `Invoke-JournalLoad.ps1`, `Start-ApSimulator.ps1`) to exercise this pipeline without real ATM hardware — see `tools/agent-e2e/README.md` and `DEMO-RUNBOOK.md` before rebuilding this kind of tooling from scratch.

### Batch (atms-batch)

Job code names (`Batday01`, `Batday02`, `Batday08`, `Batday09`, `Batday30`, `Batday98`, `Batday99`, ...) are inherited verbatim from the legacy system; each has a `{Name}JobConfig` (Spring Batch) plus usually a dedicated mapper. Jobs are **not** cron-annotated in code — `DynamicBatchScheduler` reads enabled rows from `T_BATCH_JOB` on `ApplicationReadyEvent` and registers a `CronTrigger` per row (with optional `HOST_NAME` affinity for multi-instance deployments); `BatchJobService` reschedules/unschedules at runtime when that table is edited via the CRUD API. To change a schedule, edit the DB row (or via `/api/batch` from the UI), not the Java source.

### Frontend (`Frontend/nh-atms-web`)

React 19 + TypeScript + Vite + Ant Design 6. `src/pages/{domain}/`, `src/api/{domain}Api.ts` mirror the backend domain packages 1:1. Server state uses `@tanstack/react-query`; client/UI state (auth, menu, tabs, favorites, selected branch/code/corner) uses individual `zustand` stores in `src/stores/`. Routing is a single flat table in `src/routes/AppRouter.tsx` (no lazy loading / route-based code splitting currently). Path alias `@/` → `src/`.

Auth tokens live in `sessionStorage` (not `localStorage`), read directly by `src/api/axiosInstance.ts`'s interceptors: attaches `Authorization: Bearer`, and on a 401 (excluding the login/logout calls themselves) transparently calls `/api/auth/refresh` once and retries, else forces a redirect to `/login`.

## Environment / config gotchas

- **No H2 anywhere in practice.** The README and `atms-web/README.md` describe `local`/`dev` as using an in-memory H2 DB, but no `h2` config exists in any yml. Local dev talks to the bank's shared dev Oracle (`16.88.250.91:1531/NBATMSTP` via `local-common.yml`; the `application.yml` fallback URL is still the vendor's `192.168.28.21`) — be careful with destructive DML against shared tables. SQL uses unqualified table names (`T_USER`), so the connecting account must see the tables (own schema or synonyms + grants).
- **Frontend dev port is 3000**, not Vite's default 5173, per `vite.config.ts`'s explicit `server.port`. (CORS allow-lists both, so 5173 would still work if you override the port, but `npm run dev` alone gives you 3000.)
- Each backend module's `src/main/resources/application-local.yml` holds the local dev JWT secret and imports `local-common.yml` (since 2026-11-02 the per-module `SPRING_DATASOURCE_PASSWORD` line is gone). These files and `local-common.yml` **are** tracked in git (the `.gitignore` rule is commented out) — since 2026-11-05 they hold no DB password at all (it is in each PC's `D:\local-db.yml`). `-Pjeus` builds exclude both from the WARs.
- `atms-common`'s `SecurityConfig` `permitAll`s the legacy agent paths (`/atms/agent/**`, `/atmr/agent/**`, `/atmf/agent/**`, `/atms/remote-exec/**`) plus Swagger/actuator/h2-console — real ATM devices never send a JWT, so don't add auth requirements to those paths.
- The bank's Oracle uses **KO16MSWIN949**; the thin driver needs `orai18n` on the classpath or every query returning text fails with `ORA-17056` (seen at login, 2026-10-26). `atms-common/build.gradle` declares `runtimeOnly 'com.oracle.database.nls:orai18n'` (version from the Boot BOM, same as ojdbc11) — keep it, and remember offline caches (closed-network PCs, Jenkins `/cache/gradle`) must contain it (a closed-network PC that only has the jar can use `runtimeOnly files("${rootDir}/libs/orai18n-23.7.0.25.01.jar")` with the jar committed to its Gitea copy). On JEUS the datasource is JNDI, so orai18n must also be in the **JEUS library**.
- **Sensitive-field encryption (2026-11-03, `../docs/FIELD_ENCRYPTION_DESIGN.md`)**: HTTPS is not allowed, so password fields travel as `ENC1.<kid>.<RSA-OAEP-SHA256 wrapped AES key>.<iv>.<AES-256-GCM>` built by `Frontend/src/utils/fieldCrypto.ts` (node-forge; `crypto.subtle` doesn't exist over http). Server side: annotate request DTO String fields with `@EncryptedField` (or call `FieldDecryptor.decrypt` for Map bodies); plaintext JSON carries `ts`+nonce and replays are rejected. Keep the explicit `OAEPParameterSpec` (MGF1 SHA-256) — Java's default MGF1 is SHA-1 and won't match node-forge. Config `crypto.field.*` lives only in atms-web (`application-jeus.yml` key files under `/home/atmswas/keys`, `application-local.yml` generates a temp key). Errors are `BusinessException` `CRYPTO_*` → 400 (also when thrown inside Jackson, via `GlobalExceptionHandler`'s `HttpMessageNotReadableException` handler).
- `atms-web`'s `NoticeServiceTest` does not compile (calls `NoticeService.get(String, long)`, which no longer exists), so `./gradlew :atms-web:test` fails at `compileTestJava` until it is fixed or removed.
- Git: only a `main` branch is in use in practice (no `develop`/`feature/*`/GitFlow despite `Document/10_개발표준가이드.md` describing one) — commits go straight to `main`. Do follow the commit message convention actually used: `type(scope): subject` in Korean, e.g. `fix(journal): 저널 dispatch 트랜잭션 미적용 결함 교정`, matching `git log`.

## Documentation map

`Document/` is mostly Korean and mostly analysis/planning material, not enforced rules — treat `Document/10_개발표준가이드.md` (coding/git/PR standards) and `Document/02_Git관리전략.md` (branch strategy) as aspirational team process docs rather than what's actually practiced (see gotchas above). More reliably accurate, code-adjacent docs:

- `Backend/nh-atms-api/atms-web/README.md` — atms-web endpoints, error codes, conventions (matches actual code)
- `tools/agent-e2e/README.md`, `DEMO-RUNBOOK.md` — real-agent/simulator E2E journal testing
- `Document/09_ATMR_저널프로토콜_분석.md`, `Document/11_저널수신파이프라인.md` — legacy journal wire protocol / ingestion pipeline analysis
- `Document/00_DB분석_모듈매핑.md`, `Document/01_모듈분석_마이그레이션가이드.md` — legacy-system → NH-ATMS mapping (explains odd naming)
- `infra/elastic/README.md` — Kibana journal dashboard provisioning
