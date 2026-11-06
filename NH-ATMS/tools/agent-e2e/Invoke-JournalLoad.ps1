# =====================================================================
# Invoke-JournalLoad.ps1 — 저널 수신 부하 테스트 (기기 N대 병렬 시뮬레이션)
#
# agent 와 동일한 바이너리 wire format(마스킹+청크)을 서버에 직접 병렬 POST.
# ※ 실 agent 1프로세스 = 기기 1대(직렬 송신)라 부하가 나오지 않음 —
#    부하는 이 스크립트(direct), 프로토콜 검증은 실 agent E2E 로 역할 분리.
#
# 5,500대 피크 산정: 평균 20~40건/초, 피크 100~300건/초 (Kafka/ES 검토보고서)
#
# 사용 예:
#   .\Invoke-JournalLoad.ps1 -Devices 20 -PerDevice 5                  # 스모크 100건
#   .\Invoke-JournalLoad.ps1 -Devices 550 -PerDevice 10 -Parallel 32   # 5,500건
#   .\Invoke-JournalLoad.ps1 -Devices 100 -PerDevice 30 -Kind 71       # 거래 집중
# =====================================================================
param(
    [string]$Server    = "http://127.0.0.1:8081",
    [string]$Path      = "/atmr/agent/put.jnl.data.do",
    [int]$Devices      = 20,                 # 시뮬레이션 기기 수 (00722447 부터 순번)
    [int]$PerDevice    = 5,                  # 기기당 저널 건수
    [ValidateSet("11","12","21","22","31","41","52","71","mix")]
    [string]$Kind      = "mix",
    [int]$Parallel     = 16,                 # 동시 전송 스레드 (기기 단위 병렬)
    [string]$BankCd    = "011",
    [switch]$WithReceipt
)

$libPath = Join-Path $PSScriptRoot "JournalLib.ps1"
$uri = "$Server$Path"
$mixPool = @("71","71","71","71","21","41","31","22")

# 기기 목록: 영업점 5자리(00700~) + 단말 3자리(001~999) 순번
$deviceIds = 0..($Devices - 1) | ForEach-Object { "{0:D5}{1:D3}" -f (700 + [int]($_ / 999)), (1 + ($_ % 999)) }

Write-Host "[load] $Devices devices x $PerDevice journals = $($Devices * $PerDevice) 건 → $uri (parallel=$Parallel, kind=$Kind)"
$sw = [System.Diagnostics.Stopwatch]::StartNew()

$results = $deviceIds | ForEach-Object -ThrottleLimit $Parallel -Parallel {
    . $using:libPath
    $ok = 0; $fail = 0; $failMsg = $null
    $http = [System.Net.Http.HttpClient]::new()
    $http.Timeout = [TimeSpan]::FromSeconds(30)
    try {
        for ($i = 0; $i -lt $using:PerDevice; $i++) {
            $k = if ($using:Kind -eq "mix") { $using:mixPool | Get-Random } else { $using:Kind }
            $packet = New-JifPacket -Kind $k -T (Get-Date) -AtmId $_ -BankCd $using:BankCd -Seq ($i + 1)
            $receipt = if ($using:WithReceipt) { "명세표 TEST $_ #$i" } else { "" }
            $payload = New-JifWirePayload -Packet $packet -Receipt $receipt

            $content = [System.Net.Http.ByteArrayContent]::new($payload)
            $content.Headers.ContentType = "application/octet-stream"
            $res = $http.PostAsync($using:uri, $content).GetAwaiter().GetResult()
            $body = $res.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            if ($res.IsSuccessStatusCode -and $body -notmatch '##emsg##"\s*:\s*"[^"]') { $ok++ }
            else { $fail++; if (-not $failMsg) { $failMsg = "HTTP $([int]$res.StatusCode) $body" } }
        }
    } catch {
        $fail += ($using:PerDevice - $ok)
        if (-not $failMsg) { $failMsg = $_.Exception.Message }
    } finally {
        $http.Dispose()
    }
    [pscustomobject]@{ Device = $_; Ok = $ok; Fail = $fail; FirstError = $failMsg }
}

$sw.Stop()
$okTotal = ($results | Measure-Object -Property Ok -Sum).Sum
$failTotal = ($results | Measure-Object -Property Fail -Sum).Sum
$tps = if ($sw.Elapsed.TotalSeconds -gt 0) { [math]::Round($okTotal / $sw.Elapsed.TotalSeconds, 1) } else { 0 }

Write-Host ""
Write-Host "========== 결과 =========="
Write-Host "성공 : $okTotal 건 / 실패 : $failTotal 건"
Write-Host "시간 : $([math]::Round($sw.Elapsed.TotalSeconds,1))s → TPS: $tps 건/초"
if ($failTotal -gt 0) {
    Write-Host "--- 실패 상세 (기기별 첫 오류) ---"
    $results | Where-Object Fail -gt 0 | Select-Object -First 5 | Format-Table Device, Ok, Fail, FirstError -AutoSize
}
Write-Host "피크 기준: 5,500대 피크 100~300건/초 — TPS가 이를 상회하면 수신 계층 여유 확인"
