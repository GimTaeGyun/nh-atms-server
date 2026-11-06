# =====================================================================
# Send-TestJournal.ps1 — 저널 1건을 만들어 서버로 바로 전송 (가장 쉬운 테스트)
#
# agent·파일 없이, 가상 데이터 저널을 서버(atms-journal)에 직접 POST 한다.
# 은행/금액/권종/고객명을 옵션으로 지정하거나, 생략하면 랜덤 가상값이 들어간다.
#
# 예시:
#   .\Send-TestJournal.ps1                                   # 거래저널 1건(랜덤)
#   .\Send-TestJournal.ps1 -Kind 71 -Bank 088 -Amount 300000 -Name 김철수
#   .\Send-TestJournal.ps1 -Kind 71 -Count 10                # 랜덤 거래 10건
#   .\Send-TestJournal.ps1 -Kind mix -Count 50               # 여러 유형 50건
#   .\Send-TestJournal.ps1 -Server http://192.168.28.21:19082   # 배포 서버
# =====================================================================
param(
    [string]$Server  = "http://192.168.28.21:19082",   # 배포 서버 기본. 로컬은 http://127.0.0.1:8081
    [ValidateSet("11","12","21","22","31","41","52","71","mix")]
    [string]$Kind    = "71",
    [int]$Count      = 1,
    [string]$AtmId   = "00722447",                      # 영업점5+단말3
    [string]$Bank    = "",                              # 은행코드 3자리(011농협 004국민 088신한 020우리 081하나...)
    [long]$Amount    = 0,                               # 거래금액(원). 0이면 랜덤
    [string]$Name    = "",                              # 고객명. 빈값이면 랜덤
    [string]$Denom   = "",                              # 권종(50000/10000/5000/1000). 빈값이면 랜덤
    [switch]$WithReceipt
)

. (Join-Path $PSScriptRoot "JournalLib.ps1")

$uri = "$Server/atmr/agent/put.jnl.data.do"
$mixPool = @("71","71","71","71","21","41","31","22")
$http = [System.Net.Http.HttpClient]::new()
$http.Timeout = [TimeSpan]::FromSeconds(30)

$data = @{}
if ($Bank)   { $data.bank   = $Bank }
if ($Amount) { $data.amount = $Amount }
if ($Name)   { $data.name   = $Name }
if ($Denom)  { $data.denom  = $Denom }

Write-Host "[전송] $Count 건 → $uri (kind=$Kind, atmId=$AtmId)`n"
$ok = 0; $fail = 0
for ($i = 1; $i -le $Count; $i++) {
    $k = if ($Kind -eq "mix") { $mixPool | Get-Random } else { $Kind }
    $packet  = New-JifPacket -Kind $k -T (Get-Date) -AtmId $AtmId -BankCd $AtmId.Substring(0,3) -Seq $i -Data $data
    $receipt = if ($WithReceipt) { "명세표 TEST $AtmId #$i" } else { "" }
    $payload = New-JifWirePayload -Packet $packet -Receipt $receipt
    $content = [System.Net.Http.ByteArrayContent]::new($payload)
    $content.Headers.ContentType = "application/octet-stream"
    try {
        $res  = $http.PostAsync($uri, $content).GetAwaiter().GetResult()
        $body = $res.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if ($res.IsSuccessStatusCode -and $body -notmatch '##emsg##"\s*:\s*"[^"]') {
            $ok++;   Write-Host ("  [{0,3}] OK   kind={1}" -f $i, $k)
        } else {
            $fail++; Write-Host ("  [{0,3}] FAIL kind={1} → HTTP {2} {3}" -f $i, $k, [int]$res.StatusCode, $body) -ForegroundColor Red
        }
    } catch {
        $fail++; Write-Host ("  [{0,3}] ERROR {1}" -f $i, $_.Exception.Message) -ForegroundColor Red
    }
}
$http.Dispose()
Write-Host "`n결과: 성공 $ok / 실패 $fail"
if ($ok -gt 0) {
    Write-Host "→ Kibana 에서 확인: $($Server -replace ':19082',':19601') /app/dashboards"
    Write-Host "   (또는 Discover 에서 데이터뷰 'ATMS Journal' 선택)"
}
