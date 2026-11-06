# =====================================================================
# Start-ApSimulator.ps1 — ATM AP 소켓 시뮬레이터 (agent 핸드셰이크용)
#
# agent(eztams.agent)는 기동 시 AP(127.0.0.1:[apif]port)에 접속해
# AG요청(txcode=10, ATM ID 조회)이 성공해야 저널 송신 워커를 시작한다.
# 이 스크립트는 atms.ap.tp(Program.tp.ap.cs)와 동일한 전문 규칙으로 응답한다.
#
# 전문: len(5,ASCII) + txcode(2) + trcode(2) + seqno(1) + maxseq(1) + respcd(1) + filler(8) + body
# 응답: trcode='02', respcd='0', txcode 10 → body "|{AtmId}|", 12 → "|10|0123456789|"
#
# 사용: .\Start-ApSimulator.ps1 [-Port 21001] [-AtmId 00722447]   (Ctrl+C 종료)
# =====================================================================
param(
    [int]$Port = 21001,
    [string]$AtmId = "00722447"
)

$enc = [System.Text.Encoding]::GetEncoding(949)
$HEAD = 20   # len5 + txcode2 + trcode2 + seq1 + maxseq1 + respcd1 + filler8

function Read-N($stream, [byte[]]$buf, [int]$off, [int]$len) {
    while ($len -gt 0) {
        $io = $stream.Read($buf, $off, $len)
        if ($io -le 0) { return $false }
        $off += $io; $len -= $io
    }
    return $true
}

$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $Port)
$listener.Start()
Write-Host "[ap-sim] listening on :$Port (atmid=$AtmId)  Ctrl+C to stop"

while ($true) {
    $client = $listener.AcceptTcpClient()
    $addr = $client.Client.RemoteEndPoint.ToString()
    Write-Host "[ap-sim][$addr] connected"
    $stream = $client.GetStream()
    $buf = New-Object byte[] 65536
    try {
        while ($true) {
            if (-not (Read-N $stream $buf 0 5)) { break }
            $slen = $enc.GetString($buf, 0, 5)
            # 시재조회 특수 전문 (ap.tp 동일)
            if ($slen -eq "X0001") { $b = $enc.GetBytes("000182601110        |8|");           $stream.Write($b, 0, $b.Length); continue }
            if ($slen -eq "X0002") { $b = $enc.GetBytes("000261801110        |90000|800|");  $stream.Write($b, 0, $b.Length); continue }
            $io = [int]$slen
            if (-not (Read-N $stream $buf 5 $io)) { break }
            $txcode = $enc.GetString($buf, 5, 2)
            $trcode = $enc.GetString($buf, 7, 2)
            Write-Host "[ap-sim][$addr][rcv] $($enc.GetString($buf, 0, 5 + $io))"
            if ($trcode -ne "01") { continue }   # AG응답(02) 등은 무시

            # 응답: trcode=02, respcd=0
            $buf[7] = [byte][char]'0'; $buf[8] = [byte][char]'2'
            $buf[11] = [byte][char]'0'
            $body = switch ($txcode) {
                "10" { "|$AtmId|" }          # ATM ID 조회
                "12" { "|10|0123456789|" }   # (ap.tp 동일 응답)
                default { "" }
            }
            $bodyBytes = $enc.GetBytes($body)
            [Array]::Copy($bodyBytes, 0, $buf, $HEAD, $bodyBytes.Length)
            $total = $HEAD + $bodyBytes.Length
            $lenBytes = $enc.GetBytes(($total - 5).ToString("D5"))
            [Array]::Copy($lenBytes, 0, $buf, 0, 5)
            Write-Host "[ap-sim][$addr][snd] $($enc.GetString($buf, 0, $total))"
            $stream.Write($buf, 0, $total)
        }
    } catch {
        Write-Host "[ap-sim][$addr] $($_.Exception.Message)"
    } finally {
        $client.Close()
        Write-Host "[ap-sim][$addr] closed"
    }
}
