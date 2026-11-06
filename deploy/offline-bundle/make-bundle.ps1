# =====================================================================
# 반입 묶음 생성 스크립트 (외부망 Windows PC + Docker Desktop)
#
# 실행 (저장소 루트에서, PowerShell):
#   powershell -ExecutionPolicy Bypass -File deploy\offline-bundle\make-bundle.ps1
#   powershell -ExecutionPolicy Bypass -File deploy\offline-bundle\make-bundle.ps1 -Out D:\반입\atms-bundle
#
# 결과물 (-Out 폴더):
#   temurin-21-jre-jammy.tar      백엔드 6개 모듈 런타임
#   nginx-alpine.tar              프론트엔드
#   gitea-rootless.tar            사내 Git 서버
#   nh-atms-jenkins-bank.tar      Jenkins (플러그인·docker compose·Node 22 포함, 여기서 빌드)
#   docker-compose-linux-x86_64   서버용 compose 바이너리
#   versions.txt                  이미지 실제 버전/ID 기록
#   SHA256SUMS.txt                서버에서 sha256sum -c 로 무결성 확인
# =====================================================================
param(
    [string]$Out = (Join-Path (Get-Location) "atms-offline-bundle")
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")

function Invoke-Native {
    param([string]$Exe, [string[]]$Arguments)
    Write-Host ">> $Exe $($Arguments -join ' ')" -ForegroundColor Cyan
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "실패: $Exe $($Arguments -join ' ') (exit $LASTEXITCODE)" }
}

New-Item -ItemType Directory -Force -Path $Out | Out-Null

# Docker Desktop 엔진 확인
Invoke-Native "docker" @("version", "--format", "{{.Server.Version}}")

# 1) 공개 이미지: pull -> save
$images = @(
    @{ Ref = "eclipse-temurin:21-jre-jammy"; File = "temurin-21-jre-jammy.tar" },
    @{ Ref = "nginx:alpine";                 File = "nginx-alpine.tar" },
    @{ Ref = "gitea/gitea:latest-rootless";  File = "gitea-rootless.tar" }
)
foreach ($img in $images) {
    Invoke-Native "docker" @("pull", "--platform", "linux/amd64", $img.Ref)
    Invoke-Native "docker" @("save", "-o", (Join-Path $Out $img.File), $img.Ref)
}

# 2) Jenkins 이미지: 외부망에서 빌드 (플러그인 포함) -> save
$jenkinsRef = "localhost/nh-atms-jenkins:bank"
Invoke-Native "docker" @("build", "--platform", "linux/amd64", "-t", $jenkinsRef, (Join-Path $RepoRoot "deploy\jenkins"))
Invoke-Native "docker" @("save", "-o", (Join-Path $Out "nh-atms-jenkins-bank.tar"), $jenkinsRef)

# 3) docker compose 바이너리 (서버 호스트용, root 불필요)
$composeUrl = "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64"
Write-Host ">> download $composeUrl" -ForegroundColor Cyan
Invoke-WebRequest -Uri $composeUrl -OutFile (Join-Path $Out "docker-compose-linux-x86_64") -UseBasicParsing

# 4) 버전 기록
$versions = @()
foreach ($ref in ($images.Ref + $jenkinsRef)) {
    $id  = docker image inspect --format "{{.Id}}" $ref
    $ver = docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.version"}}' $ref
    $versions += "{0}`tversion={1}`tid={2}" -f $ref, $ver, $id
}
$versions += "jenkins-core`t" + (docker run --rm --entrypoint java $jenkinsRef -jar /usr/share/jenkins/jenkins.war --version)
$versions | Set-Content -Path (Join-Path $Out "versions.txt") -Encoding ascii

# 5) 체크섬 (sha256sum -c 형식: 소문자 해시 + 공백 2칸 + 파일명)
Get-ChildItem -Path $Out -File |
    Where-Object { $_.Name -ne "SHA256SUMS.txt" } |
    ForEach-Object { "{0}  {1}" -f (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower(), $_.Name } |
    Set-Content -Path (Join-Path $Out "SHA256SUMS.txt") -Encoding ascii

Write-Host ""
Write-Host "완료: $Out" -ForegroundColor Green
Get-ChildItem -Path $Out -File | Format-Table Name, @{ n = "MB"; e = { [math]::Round($_.Length / 1MB, 1) } } -AutoSize
