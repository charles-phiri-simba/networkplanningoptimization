#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root

function Test-ExactEnv([string]$Name, [string]$Expected) {
    $actual = [System.Environment]::GetEnvironmentVariable($Name)
    return [string]::Equals($actual, $Expected, [System.StringComparison]::Ordinal)
}

function Stop-OwnedDemoPid([string]$PidFile, [string[]]$CommandNeedles) {
    if (-not (Test-Path $PidFile)) { return }
    $raw = Get-Content $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    $procId = 0
    if (-not [int]::TryParse("$raw", [ref]$procId) -or $procId -le 0) { return }
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$procId" -ErrorAction SilentlyContinue
    if (-not $proc) { return }
    $command = [string]$proc.CommandLine
    $owned = $false
    foreach ($needle in $CommandNeedles) {
        if ($command -like "*$needle*") { $owned = $true; break }
    }
    if (-not $owned) { return }
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
}

if (-not (Test-ExactEnv 'SNIP_DEMO_RESET' 'YES') -or -not (Test-ExactEnv 'SNIP_DEMO_RESET_CONFIRM' 'snip-demo')) {
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

$prev = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
docker info --format '{{.ServerVersion}}' 1>$null 2>$null
$dockerInfoCode = $LASTEXITCODE
$ErrorActionPreference = $prev
if ($dockerInfoCode -ne 0) {
    Write-Host 'SNIP demo reset refused: Docker daemon is unavailable'
    exit 1
}

$volumeName = 'snip-demo_snip-postgres'
$ErrorActionPreference = 'Continue'
$inspectOutput = docker volume inspect $volumeName 2>&1
$inspectCode = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
$inspectText = ($inspectOutput | Out-String)
if ($inspectCode -ne 0) {
    if ($inspectText -match '(?i)no such volume') {
        Write-Host 'SNIP demo volume already absent'
        exit 0
    }
    Write-Host 'SNIP demo reset refused: docker volume inspect failed'
    exit 1
}

$jsonLines = $inspectOutput | Where-Object { $_ -isnot [System.Management.Automation.ErrorRecord] }
$meta = ($jsonLines | Out-String).Trim() | ConvertFrom-Json
$labels = $meta[0].Labels
if ($labels.'com.docker.compose.project' -ne 'snip-demo' -or $labels.'com.docker.compose.volume' -ne 'snip-postgres') {
    Write-Host 'SNIP demo reset refused: volume labels do not prove snip-demo/snip-postgres ownership'
    exit 1
}

$pidDir = Join-Path $Root '.snip-demo'
Stop-OwnedDemoPid (Join-Path $pidDir 'api.pid') @('snip-npo-app', 'spring-boot:run', 'snip-demo')
Stop-OwnedDemoPid (Join-Path $pidDir 'vite.pid') @('snip-web', 'vite', 'npm run dev')

if ([string]::Equals($env:SNIP_DEMO_RESET_TELEMETRY, 'YES', [System.StringComparison]::Ordinal)) {
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
