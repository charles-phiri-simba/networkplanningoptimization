#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root

$DbPort = if ($env:SNIP_DB_PORT) { [int]$env:SNIP_DB_PORT } else { 5432 }
$ApiPort = if ($env:SNIP_HOST_PORT) { [int]$env:SNIP_HOST_PORT } else { 8080 }
$ApiTarget = if ($env:SNIP_API_TARGET) { $env:SNIP_API_TARGET } else { "http://127.0.0.1:$ApiPort" }

function Test-Tcp([string]$TargetHost, [int]$Port) {
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $client.Connect($TargetHost, $Port)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

$ready = Join-Path $PSScriptRoot 'snip-demo-ready.ps1'
& powershell -NoProfile -File $ready
if ($LASTEXITCODE -eq 0) {
    Write-Host 'SNIP demo already ready'
    Write-Host 'Authoritative customer UI: http://127.0.0.1:5173'
    exit 0
}

if (Test-Tcp '127.0.0.1' $DbPort) {
    Write-Host "Port $DbPort is occupied. Set SNIP_DB_PORT to a free localhost port. Do not bind another process's database."
    exit 1
}
if (Test-Tcp '127.0.0.1' $ApiPort) {
    Write-Host "Port $ApiPort is occupied. Set SNIP_HOST_PORT and SNIP_API_TARGET. Authoritative UI remains http://127.0.0.1:5173."
    exit 1
}
if (Test-Tcp '127.0.0.1' 5173) {
    Write-Host 'Port 5173 is occupied. Stop the other process. Authoritative SNIP 1.0 UI is http://127.0.0.1:5173, not :8080.'
    exit 1
}

docker compose -p snip-demo up postgres -d
if ($LASTEXITCODE -ne 0) { exit 1 }

$pidDir = Join-Path $Root '.snip-demo'
New-Item -ItemType Directory -Force -Path $pidDir | Out-Null

$api = Start-Process -FilePath 'mvn' -ArgumentList @('-pl','snip-npo-app','-am','spring-boot:run','-Dspring-boot.run.profiles=demo') -PassThru -WindowStyle Hidden
Set-Content (Join-Path $pidDir 'api.pid') $api.Id

$deadline = (Get-Date).AddMinutes(4)
$up = $false
while ((Get-Date) -lt $deadline) {
    try {
        $health = Invoke-RestMethod -Uri "$ApiTarget/health" -TimeoutSec 2
        if ($health.status -eq 'UP') { $up = $true; break }
    } catch { }
    Start-Sleep -Seconds 3
}
if (-not $up) {
    Write-Host 'SNIP demo API did not become healthy. Check Maven output. Authoritative UI is http://127.0.0.1:5173.'
    exit 1
}

$web = Join-Path $Root 'snip-web'
if (-not (Test-Path (Join-Path $web 'node_modules'))) {
    Start-Process -FilePath 'npm' -ArgumentList @('ci') -WorkingDirectory $web -Wait
}
$vite = Start-Process -FilePath 'npm' -ArgumentList @('run','dev') -WorkingDirectory $web -PassThru -WindowStyle Hidden
Set-Content (Join-Path $pidDir 'vite.pid') $vite.Id

$deadline = (Get-Date).AddMinutes(2)
while ((Get-Date) -lt $deadline) {
    if (Test-Tcp '127.0.0.1' 5173) { break }
    Start-Sleep -Seconds 2
}

& $ready
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Authoritative customer UI: http://127.0.0.1:5173'
Write-Host 'Do not use http://127.0.0.1:8080/ as the SNIP 1.0 customer demo.'
exit 0
