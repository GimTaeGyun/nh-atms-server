# =====================================================================
# JournalLib.ps1 — JIF 저널 전문 빌더 공통 라이브러리
# interface.journal.json 스펙 기준. New-Journal.ps1(파일 생성)과
# Invoke-JournalLoad.ps1(직접 POST 부하)이 공유한다.
#
# 규칙 (AS-IS Converter.toPacket writeValue 동일):
#   - delim 필드: 값을 스펙 len 으로 rpad(공백)/절단 후 '|' 구분
#   - dt=1(숫자) 필드: lpad('0')
#   - 배열 필드: 개수(lpad0) 토큰 후 항목별 필드 토큰 반복
# =====================================================================

$script:JifEnc = [System.Text.Encoding]::GetEncoding(949)

function PadF([string]$v, [int]$len) {   # rpad space + truncate (euc-kr 바이트 기준)
    $b = $script:JifEnc.GetBytes([string]$v)
    if ($b.Length -ge $len) { return $script:JifEnc.GetString($b, 0, $len) }
    return ([string]$v) + (' ' * ($len - $b.Length))
}
function PadN([string]$v, [int]$len) { return ([string]$v).PadLeft($len, '0') }   # lpad zero (dt=1)

# ── 가상(fake) 데이터 풀 ────────────────────────────────────────────
# 은행코드(3자리) — 자행(농협 011) + 주요 타행. 실제 표준 은행코드 근사값
$script:Banks = @(
    @{cd="011"; nm="농협"}, @{cd="004"; nm="국민"}, @{cd="088"; nm="신한"},
    @{cd="020"; nm="우리"}, @{cd="081"; nm="하나"}, @{cd="003"; nm="기업"},
    @{cd="090"; nm="카카오뱅크"}, @{cd="032"; nm="부산"}, @{cd="007"; nm="수협"}
)
$script:Names = @("김철수","이영희","박민수","최지은","정대현","강수진","윤서준","임하늘","한소영","오진우","서예린","신동욱")
# 거래코드: 출금/입금/이체/조회 (앞2=구분)
$script:TrxCodes = @("440100","440200","450100","460100","410100","470100")
$script:Denoms = @(
    @{denom="50000"; curr="KRW"}, @{denom="10000"; curr="KRW"}, @{denom="5000"; curr="KRW"}, @{denom="1000"; curr="KRW"}
)

function Rand-Bank { $script:Banks | Get-Random }
function Rand-Name { $script:Names | Get-Random }
function Rand-Amt([int]$min = 1, [int]$max = 200) {   # 만원 단위 → 원. 1만~200만
    (Get-Random -Minimum $min -Maximum ($max + 1)) * 10000
}
function Rand-Acct { -join (1..12 | ForEach-Object { Get-Random -Minimum 0 -Maximum 10 }) }
function Rand-Card { "1234" + (-join (1..12 | ForEach-Object { Get-Random -Minimum 0 -Maximum 10 })) }

# ── jnl_kind 별 body (delim, leading/trailing '|') ──────────────────
# $Data(hashtable, 선택): bank/amount/denom/name 등 특정값 강제. 미지정 시 랜덤 생성.
function New-JifBody {
    param([string]$Kind, [datetime]$T, [string]$BankCd, [hashtable]$Data = @{})

    # 가상 데이터 확정 ($Data 로 강제 가능, 미지정 시 랜덤)
    $fBank  = if ($Data.bank)    { $Data.bank }    else { (Rand-Bank).cd }
    $fName  = if ($Data.name)    { $Data.name }    else { Rand-Name }
    $fAcct  = if ($Data.account) { $Data.account } else { Rand-Card }
    $fTrx   = if ($Data.trxcode) { $Data.trxcode } else { $script:TrxCodes | Get-Random }
    $fAmt   = if ($Data.amount)  { [int64]$Data.amount } else { Rand-Amt }
    $fFee   = if ($Data.fee)     { [int64]$Data.fee }    else { @(0,600,900,1000,1300 | Get-Random) }
    $fBal   = if ($Data.balance) { [int64]$Data.balance } else { (Rand-Amt 5 500) }
    $fDenom = if ($Data.denom)   { $Data.denom }  else { ($script:Denoms | Get-Random).denom }
    $fQty   = if ($Data.qty)     { [int]$Data.qty } else { [int]($fAmt / [int]$fDenom) }
    if ($fQty -lt 1) { $fQty = 1 }
    # 카세트 잔량(랜덤)
    $c10 = Get-Random -Minimum 300 -Maximum 2500
    $c50 = Get-Random -Minimum 100 -Maximum 1500

    $f = switch ($Kind) {
        "11" { @(  # AP Start
                (PadF $T.ToString("MMdd") 4), (PadF $T.ToString("dd") 2), (PadF $T.ToString("yy") 2),
                (PadF $T.ToString("HH") 2), (PadF $T.ToString("mm") 2), (PadF $T.ToString("ss") 2),
                "02",
                  "1", (PadF "AP-1.0.0-TEST" 32), (PadF "FW-2.0.0-TEST" 32),
                  "2", (PadF "CDU-3.1.0" 32),     (PadF "CDU-FW-1.1" 32)
        ) }
        "12" { @(  # 서비스오픈(개국)
                (PadF $T.ToString("MM") 2), (PadF $T.ToString("dd") 2), (PadF $T.ToString("yyyy") 4),
                (PadF $T.ToString("HH") 2), (PadF $T.ToString("mm") 2), (PadF $T.ToString("ss") 2),
                (PadF "440010" 6), (PadF "000000" 6), "0", "11", (PadF "710" 3),
                (PadF "SN-TEST-0001" 20), (PadF "BC-TEST-0001" 20), (PadF "AP-1.0.0-TEST" 32),
                "3","2","2","0","2","0","2","A",
                (PadF "VENDOR-DATA-TEST" 200), "1","1","1","0",
                (PadF "192.168.0.10 Win10 IntelCPU 8GB HDD1 50% HDD2 30% 20260101" 200)
        ) }
        { $_ -in "21","22" } { # 상태(21)/장애(22) — 22는 장애코드 포함
            $fault = ($Kind -eq "22")
            @(
                (PadF ($fault ? "02" : "00") 2), "1",
                (PadF ($fault ? "DEV0730001" : "") 10),      # bk_ecd  (73: 만원부족 계열 예시)
                (PadF ($fault ? "VD-E-0001"  : "") 10),      # vd_ecd
                "1","1","1","1","1","1","1","1",             # 각종 가능여부
                (PadF "0012" 4), (PadF "440010" 6),
                (PadF $fBank 3), (PadF $fAcct 32), (PadN $fAmt 15), (PadN 0 15),
                "03",                                        # devices 3개
                  "1", (PadF "00000000" 8),                  #  CDU 정상
                  "2", (PadF ($fault ? "10000000" : "00000000") 8),
                  "3", (PadF "00000000" 8),
                "02",                                        # cassettes_log 2개
                  (PadF "KRW" 3), (PadF "10000" 10), (PadN $c10 5), "00",
                  (PadF "KRW" 3), (PadF "50000" 10), (PadN $c50 5), "00",
                "02",                                        # cassettes_phy 2개
                  "01", (PadF "KRW" 3), (PadF "10000" 10), (PadN $c10 5), "00",
                  "02", (PadF "KRW" 3), (PadF "50000" 10), (PadN $c50 5), "00",
                "00"                                         # checks 0개
        ) }
        "31" { @(  # 마감 (스펙 len 충실 — bgn_mm len1 등 원 스펙 그대로)
                "6",
                (PadF $T.ToString("MM") 1), (PadF $T.ToString("dd") 2), (PadF $T.ToString("yy") 2),
                (PadF $T.ToString("HH") 4), (PadF $T.ToString("mm") 2), (PadF $T.ToString("ss") 2),
                (PadF $T.ToString("MM") 1), (PadF $T.ToString("dd") 2), (PadF $T.ToString("yy") 2),
                (PadF $T.ToString("HH") 4), (PadF $T.ToString("mm") 2), (PadF $T.ToString("ss") 2),
                "03",                                        # host_entries
                  (PadF "1010" 4), (PadN 152 15),
                  (PadF "1020" 4), (PadN 1520000 15),
                  (PadF "1030" 4), (PadN 48 15),
                "02",                                        # atm_entries
                  (PadF "2010" 4), (PadN 152 15),
                  (PadF "2020" 4), (PadN 1520000 15)
        ) }
        "41" { @(  # 시재(보충/회수)
                (@("0","1","2","3") | Get-Random),          # cash_tp
                "01",                                        # checks 1개
                  (PadF "KRW" 3), (PadF "100000" 10), (PadN ($c50 * 100000) 15), (PadN $c50 5),
                  (PadN 0 15), (PadN 0 5), (PadF $fBank 3), (PadF "00722" 5), (PadF "REG-0001" 32),
                "02",                                        # cassettes_log
                  (PadN $c10 5), (PadN 1000 5), (PadF "KRW" 3), (PadF "10000" 10),
                  (PadN $c50 5), (PadN  500 5), (PadF "KRW" 3), (PadF "50000" 10),
                "02",                                        # cassettes_phy
                  "01", (PadN $c10 5), (PadN 1000 5), (PadF "KRW" 3), (PadF "10000" 10),
                  "02", (PadN $c50 5), (PadN  500 5), (PadF "KRW" 3), (PadF "50000" 10)
        ) }
        "52" { @(  # 운영저널: op_cd(2, 코드표) + op_act(256, 내용)
                (PadF (@("01","02","03","10","20") | Get-Random) 2),
                (PadF ("운영자 조작 TEST - " + $fName + " " + $T.ToString("HH:mm:ss")) 256)
        ) }
        "71" { @(  # 거래
                (PadF "0012" 4), (PadF $fTrx 6), "10", "20",
                (PadF ("0" + $fBank) 4), (PadF $fAcct 32),
                (PadF "" 4), (PadF "" 32),
                (PadF $T.ToString("MM") 2), (PadF $T.ToString("dd") 2), (PadF $T.ToString("yyyy") 4),
                (PadF $T.ToString("HH") 2), (PadF $T.ToString("mm") 2), (PadF $T.ToString("ss") 2),
                (PadF "12345678" 8), (PadF "87654321" 8), (PadF "NET00001" 8),
                (PadF $T.ToString("MM") 2), (PadF $T.ToString("dd") 2), (PadF $T.ToString("yyyy") 4),
                (PadF "" 8),
                (PadN $fFee 15), (PadN $fAmt 15), (PadN $fAmt 15), (PadN $fBal 15), (PadN $fBal 15),
                "1", (PadF "0000" 6),
                (PadF "" 10), (PadF "" 10), (PadF "" 10),
                (PadF "" 104), (PadF "" 16), (PadF "" 2), (PadF "HOST OK" 64),
                "1", "1",
                (PadN 99000000 15), (PadN 5000000 15),
                "01",                                        # trx_cash 1개
                  (PadF "KRW" 3), (PadF $fDenom 10), (PadN $fQty 5),
                "00",                                        # trx_checks 0
                "02",                                        # cassettes_log
                  (PadF "KRW" 3), (PadF "10000" 10), (PadN $c10 5), "00",
                  (PadF "KRW" 3), (PadF "50000" 10), (PadN $c50 5), "00",
                "02",                                        # cassettes_phy
                  "01", (PadF "KRW" 3), (PadF "10000" 10), (PadN $c10 5), "00",
                  "02", (PadF "KRW" 3), (PadF "50000" 10), (PadN $c50 5), "00",
                "00",                                        # atm_checks 0
                (PadF $fName 20)                             # cust_nm
        ) }
        default { throw "unsupported kind: $Kind" }
    }
    return "|" + ($f -join "|") + "|"
}

# ── head(60B) + body → idx 패킷 바이트 ──────────────────────────────
function New-JifPacket {
    param([string]$Kind, [datetime]$T, [string]$AtmId, [string]$BankCd, [int]$Seq, [string]$Flag = "0", [hashtable]$Data = @{})
    $body = New-JifBody -Kind $Kind -T $T -BankCd $BankCd -Data $Data
    $bodyBytes = $script:JifEnc.GetBytes($body)
    $head = (PadN (56 + $bodyBytes.Length) 4) + $T.ToString("yyyyMMddHHmmss") + $T.ToString("fff") + $Flag +
            $BankCd + (PadF $AtmId 20) + (PadN $Seq 5) + $Kind + "0" + "0" + "0" + (' ' * 5)
    $headBytes = $script:JifEnc.GetBytes($head)
    if ($headBytes.Length -ne 60) { throw "head length $($headBytes.Length) != 60" }
    return ($headBytes + $bodyBytes)
}

# ── agent 송신 wire format: 마스킹키 + [len+jnl] + [len+명세표] + [len+img]×3 ──
function Mask-Chain([byte[]]$data, [byte]$key) {   # agent Mask() 동일 (체인 XOR)
    $m = $key
    for ($i = 0; $i -lt $data.Length; $i++) {
        $orig = $data[$i]
        $data[$i] = $data[$i] -bxor $m
        $m = $orig
    }
    return $data
}

function New-JifWirePayload {
    param([byte[]]$Packet, [string]$Receipt = "")
    $ms = New-Object System.IO.MemoryStream
    $key = [byte](Get-Random -Minimum 1 -Maximum 254)
    $ms.WriteByte($key)
    $jnl = Mask-Chain ([byte[]]$Packet.Clone()) $key
    $ms.Write([BitConverter]::GetBytes([int]$jnl.Length), 0, 4)     # little-endian
    $ms.Write($jnl, 0, $jnl.Length)
    if ($Receipt) {
        $r = Mask-Chain ($script:JifEnc.GetBytes($Receipt)) $key
        $ms.Write([BitConverter]::GetBytes([int]$r.Length), 0, 4)
        $ms.Write($r, 0, $r.Length)
    } else {
        $ms.Write([BitConverter]::GetBytes(0), 0, 4)
    }
    for ($i = 0; $i -lt 3; $i++) { $ms.Write([BitConverter]::GetBytes(0), 0, 4) }  # 이미지 3종 없음
    return $ms.ToArray()
}
