#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root

$ApiPort = if ($env:SNIP_HOST_PORT) { [int]$env:SNIP_HOST_PORT } else { 8080 }
$Api = if ($env:SNIP_API_TARGET) { $env:SNIP_API_TARGET.TrimEnd('/') } else { "http://127.0.0.1:$ApiPort" }
$UiHost = '127.0.0.1'
$UiPort = 5173

function Fail([string]$Message) {
    Write-Host $Message
    exit 1
}

function Get-Json([string]$Url, [hashtable]$Headers = @{}) {
    try {
        return Invoke-RestMethod -Uri $Url -Headers $Headers -TimeoutSec 10
    } catch {
        Fail "SNIP demo ready failed: GET $Url"
    }
}

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

$health = Get-Json "$Api/health"
if ($health.status -ne 'UP') {
    Fail "SNIP demo ready failed: backend is not UP at $Api/health"
}

$sites = Get-Json "$Api/api/v1/sites"
$siteIds = @($sites | ForEach-Object { $_.siteId })
foreach ($id in @('SITE-001','SITE-002','SITE-003','SITE-004','SITE-005','SITE-006')) {
    if ($siteIds -notcontains $id) {
        Fail "unexpected demo inventory; run snip-demo-reset (missing $id)"
    }
}

$cells = Get-Json "$Api/api/v1/cells"
$cellIds = @($cells | ForEach-Object { $_.cellId })
1..18 | ForEach-Object {
    $id = 'CELL-{0:D3}' -f $_
    if ($cellIds -notcontains $id) {
        Fail "unexpected demo inventory; run snip-demo-reset (missing $id)"
    }
}

$cases = Get-Json "$Api/api/v1/cells/CELL-001/assurance"
$featured = @($cases | Where-Object { $_.caseType -eq 'DEGRADING_RADIO_QUALITY' -and $_.severity -eq 'CRITICAL' -and $_.status -eq 'OPEN' })
if ($featured.Count -lt 1) {
    Fail 'SNIP demo ready failed: CELL-001 OPEN CRITICAL Assurance case missing'
}

$twinFile = Join-Path $Root '.snip-demo/featured-twins.properties'
if (-not (Test-Path $twinFile)) {
    Fail 'SNIP demo ready failed: featured twin registry missing (bootstrap may have failed). The file is a hint only; CURRENT is proven by GET /api/v1/twins/{id}.'
}
$twinId = (Get-Content $twinFile | Where-Object { $_ -like 'CELL-001=*' } | Select-Object -First 1)
if (-not $twinId) {
    Fail 'SNIP demo ready failed: featured twin registry missing CELL-001'
}
$uuid = $twinId.Substring('CELL-001='.Length).Trim()
try {
    $twin = Invoke-RestMethod -Uri "$Api/api/v1/twins/$uuid" -TimeoutSec 10
} catch {
    Fail "SNIP demo ready failed: local featured-twins.properties is stale or does not match this backend (GET $Api/api/v1/twins/$uuid). CURRENT is not proven by the file. Re-run snip-demo-up."
}
if ($twin.freshness -ne 'CURRENT' -or $twin.scopeId -ne 'CELL-001') {
    Fail "SNIP demo ready failed: backend twin is not a CURRENT CELL-001 Cell Digital Twin (freshness=$($twin.freshness) scopeId=$($twin.scopeId)). Local file is not authoritative."
}

$headers = @{ 'X-SNIP-VENDOR-IMPORT-PERMISSION' = 'VIEW_SYNCHRONIZATION_STATUS' }
$knowledge = Get-Json "$Api/api/v1/integration/sync/sources/ERICSSON_ENM_SIMULATOR/DEFAULT" $headers
if ($knowledge.knowledgeConfidence -notin @('HIGH','MEDIUM')) {
    Fail "SNIP demo ready failed: knowledge $($knowledge.knowledgeConfidence) is not recommendable"
}

$appYml = Get-Content (Join-Path $Root 'snip-npo-app/src/main/resources/application.yml') -Raw
$demoYml = Get-Content (Join-Path $Root 'snip-npo-app/src/main/resources/application-demo.yml') -Raw
if ($appYml -notmatch '(?s)change-execution:\s*\r?\n\s+enabled:\s*false') {
    Fail 'SNIP demo ready failed: committed application.yml must keep change-execution.enabled false'
}
if ($demoYml -notmatch '(?s)production-change:\s*\r?\n\s+enabled:\s*false' -or $demoYml -notmatch 'global-execution-enabled:\s*false') {
    Fail 'SNIP demo ready failed: demo profile must keep production-change disabled'
}
if ($demoYml -notmatch '(?s)change-execution:\s*\r?\n\s+enabled:\s*true') {
    Fail 'SNIP demo ready failed: demo profile does not enable sandbox change-execution'
}

if (-not (Test-Tcp $UiHost $UiPort)) {
    Fail "SNIP demo ready failed: customer UI is not listening on ${UiHost}:${UiPort}. Authoritative UI is http://127.0.0.1:5173 (not :8080)."
}

Write-Host 'SNIP demo ready'
Write-Host 'Authoritative customer UI: http://127.0.0.1:5173'
Write-Host "Backend health: $Api/health"
Write-Host 'Legacy static UI on :8080 is not the SNIP 1.0 customer demo.'
exit 0
