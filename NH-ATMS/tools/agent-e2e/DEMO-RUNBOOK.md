# 시연 러너북 — 저널 생성 → Agent → atms-journal → 웹 화면 조회

2026-07-14 사전 점검·리허설 완료 (전 구간 성공). 시연 대상: 배포 서버(192.168.28.21).

## 0. 시연 전 점검 (5분)

| 항목 | 확인 방법 | 리허설 결과 |
|---|---|---|
| 서버 컨테이너 | `ssh atec@192.168.28.21 "docker ps | grep nh-atms"` — journal/web/frontend/kafka/ES/kibana Up | ✅ (if-internal/external 재시작 루프는 시연 무관) |
| 개국 응답 | `curl.exe -X POST "http://192.168.28.21:19082/atms/agent/open.do?atmid=00722447"` → `succ:true` | ✅ (**GET은 500 — 반드시 POST**) |
| 저널 폴더 정리 | `D:\atms\ap-jnl\jnl` 아래 **지난 날짜 폴더 삭제** (남아있으면 agent가 당일 파일 못 봄) | ✅ 20260713 삭제함 |
| AP 심 포트 | 21001/21002 미사용 확인 | ✅ |

## 1. 시연 절차 (PowerShell 7 = pwsh 필수 — Windows PowerShell 5는 스크립트 오류)

```powershell
cd D:\0.atec\2.Project\25.NH-ATMS\2.Project\1.Project\tools\agent-e2e

# ① AP 시뮬레이터 (창 1 — 켜둔 채 유지)
.\Start-ApSimulator.ps1

# ② 실 agent 기동 (창 2) — ini는 이미 192.168.28.21:19082 설정됨
& "D:\0.atec\2.Project\25.NH-ATMS\2.Project\0.기본자료\새마을금고\agent\atms.agent\bin\Debug\eztams.agent.exe"

# ③ 저널 생성 (창 3) — 금액·고객명 지정 가능 (2026-07-14 New-Journal.ps1 에 -Amount/-Name/-Denom 추가됨)
.\New-Journal.ps1 -Kind 71 -Count 1 -Amount 300000 -Name 김철수 -WithReceipt

# ④ 송신 확인: idx 파일이 사라지면 성공 (폴링 주기상 5~20초)
Get-ChildItem -Recurse D:\atms\ap-jnl\jnl -Filter *.idx
```

## 2. 웹 화면 조회

- **URL**: http://192.168.28.21:19080 → 로그인 `admin / admin1234!`
- 메뉴 **저널 관리 > 거래저널 조회** (`/journal/transaction`) — 방금 만든 기기(00722/447) 30만원 거래 확인
- 저널 원장은 `/journal/list`, Agent 상태는 `/atm/agent-status`

## 3. 실시간 대시보드 (보너스)

- Kibana: http://192.168.28.21:19601/app/dashboards#/view/nhatms-journal-dashboard
  (시간범위 Last 15 minutes + 자동 새로고침 10초)
- 물량감 연출: `.\Send-TestJournal.ps1 -Kind mix -Count 50` (직접 전송 — agent 불필요)
- 피크 부하: `.\Invoke-JournalLoad.ps1 -Server http://192.168.28.21:19082 -Devices 550 -PerDevice 10 -Parallel 32` → TPS 표시

## 알려진 무해 로그 (당황 금지)

- agent 콘솔의 ping 404 (`/api/agent/ping`) — ping URL 불일치(수정 대기), 저널 송신 무관
- open.do 시 서버 로그 ORA-00942 — AP 버전 조회 테이블(T_DOWNLOAD_SERVER 계열) DDL 미적용, 예외 흡수되어 개국 정상
- agent의 `Path.GetDirectoryName("")` 예외 — open 응답 AP 파일 항목이 빈 값일 때, 무해

## 시연 후 정리

```powershell
Stop-Process -Name eztams.agent -Force   # agent 종료
# AP 심 창 Ctrl+C, 당일 저널 폴더는 그대로 둬도 무방 (다음 시연 전 지난 날짜만 정리)
```
