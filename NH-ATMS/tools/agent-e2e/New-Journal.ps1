# =====================================================================
# New-Journal.ps1 — agent E2E 테스트용 임의 저널(idx) 생성기
#
# ATM AP 가 남기는 저널 파일을 모사해 agent 저널 루트에 생성한다.
# 레이아웃: {JnlRoot}\{yyyyMMdd}\JNLIDX\{TRA|STA|OPR|BAL|TOT}\*.idx (+ 선택 JNLTXT 명세표)
# 포맷: interface.journal.json — head 60B 고정 + body '|' 구분 (euc-kr, idx_enc=0 평문)
#
# 지원 유형: 11(AP시작) 12(개국) 21(상태) 22(장애) 31(마감) 41(시재) 71(거래)
# 다중 기기: -AtmId 에 배열 전달 시 기기별로 각각 생성
#
# 사용 예:
#   .\New-Journal.ps1 -Kind 71 -Count 5 -WithReceipt
#   .\New-Journal.ps1 -Kind 22 -AtmId 00722447,00722448,00723101
#   .\New-Journal.ps1 -Kind mix -Count 10          # 유형 랜덤 혼합
# =====================================================================
param(
    [string]$JnlRoot = "D:\atms\ap-jnl\jnl",     # agent ini [apif] jnl 과 일치
    [string[]]$AtmId = @("00722447"),
    [string]$BankCd  = "011",
    [ValidateSet("11","12","21","22","31","41","52","71","mix")]
    [string]$Kind    = "11",
    [int]$Count      = 1,                        # 기기당 생성 건수
    [int]$StartSeq   = 1,
    [switch]$WithReceipt,
    [int64]$Amount   = 0,                        # 거래금액(원) — 0이면 랜덤 (71/41 등 금액 유형)
    [string]$Name    = "",                       # 고객명 — 빈값이면 랜덤
    [string]$Denom   = ""                        # 권종(50000/10000/...) — 빈값이면 랜덤
)

. (Join-Path $PSScriptRoot "JournalLib.ps1")

$dirOf = @{ "11"="OPR"; "12"="OPR"; "21"="STA"; "22"="STA"; "31"="TOT"; "41"="BAL"; "52"="OPR"; "71"="TRA" }
$mixPool = @("71","71","71","71","21","41","31","22")   # mix 분포: 거래 위주 (실운영 비율 근사)

$now = Get-Date
$total = 0
foreach ($id in $AtmId) {
    for ($i = 0; $i -lt $Count; $i++) {
        $k = if ($Kind -eq "mix") { $mixPool | Get-Random } else { $Kind }
        $t = $now.AddMilliseconds($i * 137 + $total)
        $seq = $StartSeq + $i

        $dayDir = Join-Path $JnlRoot $t.ToString("yyyyMMdd")
        $idxDir = Join-Path $dayDir "JNLIDX\$($dirOf[$k])"
        $txtDir = Join-Path $dayDir "JNLTXT"
        New-Item -ItemType Directory -Force -Path $idxDir, $txtDir | Out-Null

        $data = @{}
        if ($Amount -gt 0) { $data.amount = $Amount }
        if ($Name)         { $data.name   = $Name }
        if ($Denom)        { $data.denom  = $Denom }
        $packet = if ($data.Count -gt 0) {
            New-JifPacket -Kind $k -T $t -AtmId $id -BankCd $BankCd -Seq $seq -Data $data
        } else {
            New-JifPacket -Kind $k -T $t -AtmId $id -BankCd $BankCd -Seq $seq
        }
        $name = $t.ToString("yyyyMMddHHmmssfff") + "0"
        $idxPath = Join-Path $idxDir "$name.idx"
        [System.IO.File]::WriteAllBytes($idxPath, $packet)

        if ($WithReceipt) {
            $receipt = "=== 명세표(TEST) ===`r`n기기: $id`r`n일시: $($t.ToString('yyyy-MM-dd HH:mm:ss'))`r`n유형: $k`r`n금액: 50,000원`r`n===================="
            [System.IO.File]::WriteAllBytes((Join-Path $txtDir "$name.dat"), $script:JifEnc.GetBytes($receipt))
        }
        Write-Host "created: [$id][$k] $idxPath ($($packet.Length) bytes)"
        $total++
    }
}
Write-Host "`n$total 건 생성 완료. agent 실행 중이면 5초 내 폴링해 송신합니다."
