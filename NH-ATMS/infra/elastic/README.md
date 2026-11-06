# 저널 스트리밍 파이프라인 (Kafka → Elasticsearch → Kibana)

## 구조

```
agent(5,500대) → atms-journal /api/journal/receive
                    │ 1. DB 저장 (T_JOURNAL, T_JOURNAL_DATA …)  ← 기존 경로 그대로 (원본은 항상 DB)
                    │ 2. 저장 성공 시 Kafka 발행 (topic: atms.journal.received, key=기기ID)
                    ▼
                 Kafka (파티션 12 — 기기별 순서 보장)
                    │ consumer group: atms-journal-es-indexer
                    ▼
                 Elasticsearch 일별 인덱스 atms-journal-yyyy.MM.dd
                    │ 문서 ID = 기기+저널일시+일련번호 → 재전송에도 멱등
                    ▼
                 Kibana (http://localhost:19601) — 검색/운영 대시보드
```

설계 원칙:
- **DB가 원본, ES는 사본** — ES 유실 시 T_JOURNAL_DATA에서 재색인으로 복구
- **수신 경로 무영향** — Kafka 발행은 fire-and-forget, 실패해도 수신 응답 정상
- `ATMS_KAFKA_ENABLED`/`ATMS_ES_ENABLED` 미설정(false) 시 완전 비활성 — 기존 환경 동작 불변

## 기동

```bash
docker compose up -d kafka elasticsearch kibana atms-journal
./infra/elastic/setup-kibana.sh          # 데이터뷰(atms-journal-*) 등록
```

로컬 IntelliJ에서 붙을 때: Kafka `localhost:19092`, ES `http://localhost:19200`
(application.yml 기본값과 일치 — `ATMS_KAFKA_ENABLED=true;ATMS_ES_ENABLED=true` 환경변수만 주면 됨)

## 인덱스/보존

- 인덱스 템플릿은 atms-journal 기동 시 자동 등록 (`JournalEsIndexInitializer`)
- 일별 인덱스이므로 보존주기 관리는 인덱스 삭제로: `DELETE atms-journal-2026.06.*`
  (운영 전환 시 ILM 정책으로 대체 권장)
- 용량 산정 (5,500대, 일 100~150만건, 건당 ~1.5KB 색인): **일 2~3GB, 90일 보관 시 ~200GB + 여유분**

## Kibana 대시보드 구성 가이드 (요구사항 협의 후 패널 확정)

Discover 데이터뷰 `ATMS Journal` 기준 권장 패널:
1. 저널 유입 추이 (Lens: receivedAt date histogram) — 수신 정상 여부 실시간 확인
2. jnlKind 분포 (도넛: 11 AP기동 / 12 개국 / 21·22 상태·장애 / 31 마감 / 41 시재 / 52 운영 / 71 거래)
3. 장애 저널(jnlKind 21/22) 발생 Top 기기 (테이블: termGroupId+termId)
4. 기기별 최근 수신 시각 (수신 두절 탐지)
5. searchText 전문검색 바 — 계좌/에러코드 임의 검색

## 운영 전환 시 필수 변경 (개발용 설정임)

| 항목 | 현재(개발) | 운영 |
|---|---|---|
| Kafka | 단일 노드, PLAINTEXT | 3노드, replicas=3 (`atms.stream.kafka.replicas`), SASL/TLS |
| ES | 단일 노드, 보안 off | 3노드, xpack.security on, 계정 분리 |
| 개인정보 | 원문 색인 | **계좌번호 등 마스킹 후 색인** (보안성 심의 필수) |
| 보존 | 수동 삭제 | ILM 정책 |
