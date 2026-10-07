#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root

if ($env:SNIP_DEMO_RESET -ne 'YES' -or $env:SNIP_DEMO_RESET_CONFIRM -ne 'snip-demo') {
    Write-Host 'SNIP demo reset refused: set SNIP_DEMO_RESET=YES and SNIP_DEMO_RESET_CONFIRM=snip-demo'
    exit 2
}

if (-not (Test-Path (Join-Path $Root 'docker-compose.yml')) -or -not (Test-Path (Join-Path $Root 'snip-npo-app'))) {
    Write-Host 'SNIP demo reset refused: run from the SNIP repository root'
    exit 1
}

$compose = Get-Content (Join-Path $Root 'docker-compose.yml') -Raw
if ($compose -notmatch '(?m)^\s+snip-postgres:') {
    Write-Host 'SNIP demo reset refused: docker-compose.yml does not define volume key snip-postgres'
    exit 1
}

$volumeName = 'snip-demo_snip-postgres'
$inspect = docker volume inspect $volumeName 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'SNIP demo volume already absent'
    exit 0
}

$meta = $inspect | ConvertFrom-Json
$labels = $meta[0].Labels
if ($labels.'com.docker.compose.project' -ne 'snip-demo' -or $labels.'com.docker.compose.volume' -ne 'snip-postgres') {
    Write-Host 'SNIP demo reset refused: volume labels do not prove snip-demo/snip-postgres ownership'
    exit 1
}

$pidDir = Join-Path $Root '.snip-demo'
foreach ($name in @('api.pid', 'vite.pid')) {
    $pidFile = Join-Path $pidDir $name
    if (Test-Path $pidFile) {
        $procId = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
        if ($procId) {
            Stop-Process -Id ([int]$procId) -Force -ErrorAction SilentlyContinue
        }
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    }
}

if ($env:SNIP_DEMO_RESET_TELEMETRY -eq 'YES') {
    docker compose -p snip-demo --profile telemetry down
}

docker compose -p snip-demo down
docker volume rm $volumeName
if ($LASTEXITCODE -ne 0) {
    Write-Host 'SNIP demo reset failed to remove the proven snip-demo postgres volume'
    exit 1
}
Write-Host 'Reset complete. Run snip-demo-up.'
exit 0
