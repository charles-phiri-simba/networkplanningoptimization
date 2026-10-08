#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root

$DbPort = if ($env:SNIP_DB_PORT) { [int]$env:SNIP_DB_PORT } else { 5432 }
$ApiPort = if ($env:SNIP_HOST_PORT) { [int]$env:SNIP_HOST_PORT } else { 8080 }
$ApiTarget = if ($env:SNIP_API_TARGET) { $env:SNIP_API_TARGET.TrimEnd('/') } else { "http://127.0.0.1:$ApiPort" }
$DatasourceUrl = "jdbc:postgresql://127.0.0.1:$DbPort/snip"

$env:SNIP_DB_PORT = "$DbPort"
$env:SNIP_HOST_PORT = "$ApiPort"
$env:SNIP_API_TARGET = $ApiTarget
$env:SERVER_PORT = "$ApiPort"
$env:SPRING_DATASOURCE_URL = $DatasourceUrl
$env:SNIP_KAFKA_ENABLED = 'false'

$pidDir = Join-Path $Root '.snip-demo'
$logDir = Join-Path $pidDir 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$apiLog = Join-Path $logDir 'api.log'
$viteLog = Join-Path $logDir 'vite.log'

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

function Write-DemoLogExcerpt([string]$Path) {
    Write-Host "SNIP demo log file location: $Path"
    if (-not (Test-Path $Path)) {
        Write-Host 'SNIP demo log file is missing.'
        return
    }
    Get-Content $Path -Tail 80 -ErrorAction SilentlyContinue | ForEach-Object {
        $line = "$_"
        $line = $line -replace '(?i)password=\S+', 'password=***'
        $line = $line -replace '(?i)POSTGRES_PASSWORD=\S+', 'POSTGRES_PASSWORD=***'
        $line = $line -replace '(?i)SPRING_DATASOURCE_PASSWORD=\S+', 'SPRING_DATASOURCE_PASSWORD=***'
        $line = $line -replace '(?i)(jdbc:postgresql://[^\s:\]]+:)[^@/\s]+@', '$1***@'
        Write-Host $line
    }
    $text = Get-Content $Path -Raw -ErrorAction SilentlyContinue
    if ($text -match 'checksum mismatch|FlywayValidateException|Migrations have failed validation') {
        Write-Host 'SNIP demo backend health failure: Flyway validation. Demo database schema history does not match this source tree. After dual-confirm snip-demo-reset, run snip-demo-up. Do not edit V1-V20 or run SQL.'
    }
}

function Test-Java17Home([string]$HomePath) {
    if ([string]::IsNullOrWhiteSpace($HomePath)) { return $false }
    $java = Join-Path $HomePath 'bin\java.exe'
    if (-not (Test-Path $java)) { return $false }
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $output = & $java -version 2>&1 | Out-String
    $ErrorActionPreference = $prev
    return $output -match 'version "17[\."]'
}

function Resolve-SnipJava17Home {
    $candidates = New-Object System.Collections.Generic.List[string]
    if ($env:SNIP_JAVA17_HOME) { $candidates.Add($env:SNIP_JAVA17_HOME) }
    foreach ($p in @(
            'C:\Program Files\Java\jdk-17',
            'C:\Program Files\Java\jdk-17.0.12',
            'C:\Program Files\Eclipse Adoptium\jdk-17',
            'C:\Program Files\Microsoft\jdk-17'
        )) {
        $candidates.Add($p)
    }
    foreach ($root in @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Microsoft')) {
        if (Test-Path $root) {
            Get-ChildItem $root -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match 'jdk-?17|17' } |
                ForEach-Object { $candidates.Add($_.FullName) }
        }
    }
    foreach ($jdkHome in $candidates) {
        if (Test-Java17Home $jdkHome) { return $jdkHome }
    }
    throw 'SNIP demo requires Java 17. Set SNIP_JAVA17_HOME to a JDK 17 installation. Global JAVA_HOME was not changed.'
}

function Get-MavenCmd {
    $cmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $cmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw 'SNIP demo requires Maven 3.9+ on PATH.'
}

function Start-LoggedCmd {
    param(
        [Parameter(Mandatory = $true)][string]$CommandLine,
        [Parameter(Mandatory = $true)][string]$WorkDir,
        [Parameter(Mandatory = $true)][string]$LogPath,
        [Parameter(Mandatory = $true)][hashtable]$EnvOverrides
    )
    $wrapper = Join-Path $pidDir ("run-" + [guid]::NewGuid().ToString('N').Substring(0, 8) + '.cmd')
    $lines = New-Object System.Collections.Generic.List[string]
    [void]$lines.Add('@echo off')
    foreach ($key in $EnvOverrides.Keys) {
        if ($key -eq 'PATH') { continue }
        $value = [string]$EnvOverrides[$key]
        if ($value -match '[\r\n]') {
            throw 'SNIP demo refused to write a multi-line environment value'
        }
        [void]$lines.Add("set `"$key=$value`"")
    }
    [void]$lines.Add('if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"')
    [void]$lines.Add("cd /d `"$WorkDir`"")
    [void]$lines.Add("echo SNIP demo launching >> `"$LogPath`"")
    [void]$lines.Add($CommandLine + " >> `"$LogPath`" 2>&1")
    Set-Content -Path $wrapper -Value $lines -Encoding ASCII
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.Arguments = '/c "' + $wrapper + '"'
    $psi.WorkingDirectory = $WorkDir
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi
    if (-not $proc.Start()) {
        throw "SNIP demo failed to start process for $LogPath"
    }
    return $proc
}

function Test-OwnDemoPostgres {
    $names = docker compose -p snip-demo ps --status running --format '{{.Name}}' 2>$null
    if ($LASTEXITCODE -ne 0) { return $false }
    return ($names | Out-String) -match 'postgres'
}

$ready = Join-Path $PSScriptRoot 'snip-demo-ready.ps1'
$alreadyHealthy = $false
try {
    $existingHealth = Invoke-RestMethod -Uri "$ApiTarget/health" -TimeoutSec 2
    if ($existingHealth.status -eq 'UP' -and (Test-Tcp '127.0.0.1' 5173)) {
        $alreadyHealthy = $true
    }
} catch { }
if ($alreadyHealthy) {
    Write-Host "SNIP demo startup category: reuse-existing health=$ApiTarget/health ui=http://127.0.0.1:5173"
    & powershell -NoProfile -File $ready
    if ($LASTEXITCODE -eq 0) {
        Write-Host 'SNIP demo already ready'
        Write-Host 'Authoritative customer UI: http://127.0.0.1:5173'
        exit 0
    }
}

if (Test-Tcp '127.0.0.1' $DbPort) {
    if (-not (Test-OwnDemoPostgres)) {
        Write-Host "Port $DbPort is occupied. Set SNIP_DB_PORT to a free localhost port. Do not bind another process's database."
        Write-Host 'Example: SNIP_DB_PORT=15432 SNIP_HOST_PORT=18080 SNIP_API_TARGET=http://127.0.0.1:18080'
        exit 1
    }
    Write-Host "Reusing snip-demo PostgreSQL already listening on 127.0.0.1:$DbPort"
} else {
    docker compose -p snip-demo up postgres -d
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

if (Test-Tcp '127.0.0.1' $ApiPort) {
    try {
        $existing = Invoke-RestMethod -Uri "$ApiTarget/health" -TimeoutSec 2
        if ($existing.status -ne 'UP') { throw 'not up' }
        Write-Host "Port $ApiPort is already a healthy SNIP API at $ApiTarget"
    } catch {
        Write-Host "Port $ApiPort is occupied. Set SNIP_HOST_PORT and SNIP_API_TARGET. Authoritative UI remains http://127.0.0.1:5173."
        Write-Host 'Example: SNIP_DB_PORT=15432 SNIP_HOST_PORT=18080 SNIP_API_TARGET=http://127.0.0.1:18080'
        exit 1
    }
}

if (Test-Tcp '127.0.0.1' 5173) {
    Write-Host 'Port 5173 is occupied. Stop the other process. Authoritative SNIP 1.0 UI is http://127.0.0.1:5173, not :8080.'
    exit 1
}

$javaHome = Resolve-SnipJava17Home
$mvn = Get-MavenCmd
Write-Host "SNIP demo using process-local Java 17 at $javaHome"
Write-Host "SNIP demo mapping DB host port $DbPort, API host port $ApiPort, ready/proxy $ApiTarget"

Write-Host 'SNIP demo startup category: sibling-install (does not repackage the NPO boot jar)'
$depLog = Join-Path $logDir 'deps.log'
$depProc = Start-LoggedCmd -CommandLine "call `"$mvn`" -pl production-change-protocol,production-write-gateway -am -DskipTests install" -WorkDir $Root -LogPath $depLog -EnvOverrides @{
    JAVA_HOME = $javaHome
}
if (-not $depProc.WaitForExit(600000) -or $depProc.ExitCode -ne 0) {
    Write-Host "SNIP demo module install failed (exit=$($depProc.ExitCode))."
    Write-DemoLogExcerpt $depLog
    exit 1
}

Write-Host "SNIP demo startup category: backend-launch command=spring-boot:run profile=demo server.port=$ApiPort"
$apiCmd = "call `"$mvn`" -f snip-npo-app\pom.xml spring-boot:run `"-Dspring-boot.run.profiles=demo`" `"-Dspring-boot.run.arguments=--server.port=$ApiPort --spring.datasource.url=$DatasourceUrl`""
$apiProc = Start-LoggedCmd -CommandLine $apiCmd -WorkDir $Root -LogPath $apiLog -EnvOverrides @{
    JAVA_HOME = $javaHome
    SERVER_PORT = "$ApiPort"
    SPRING_DATASOURCE_URL = $DatasourceUrl
    SNIP_KAFKA_ENABLED = 'false'
    SNIP_API_TARGET = $ApiTarget
    SNIP_HOST_PORT = "$ApiPort"
    SNIP_DB_PORT = "$DbPort"
}
Set-Content (Join-Path $pidDir 'api.pid') $apiProc.Id
Write-Host "SNIP demo process status: api pid=$($apiProc.Id) hasExited=$($apiProc.HasExited) log=$apiLog"

$deadline = (Get-Date).AddMinutes(5)
$up = $false
while ((Get-Date) -lt $deadline) {
    if ($apiProc.HasExited) {
        Write-Host "SNIP demo process status: api pid=$($apiProc.Id) terminated exit=$($apiProc.ExitCode) before health"
        Write-Host "SNIP demo API process exited before health (pid=$($apiProc.Id) exit=$($apiProc.ExitCode))."
        Write-DemoLogExcerpt $apiLog
        Remove-Item (Join-Path $pidDir 'api.pid') -ErrorAction SilentlyContinue
        exit 1
    }
    try {
        $health = Invoke-RestMethod -Uri "$ApiTarget/health" -TimeoutSec 2
        if ($health.status -eq 'UP') { $up = $true; break }
    } catch { }
    Start-Sleep -Seconds 3
}
if (-not $up) {
    Write-Host "SNIP demo backend health failure: $ApiTarget/health not UP (pid=$($apiProc.Id) running=$(-not $apiProc.HasExited))."
    Write-Host "SNIP demo API did not become healthy at $ApiTarget/health (pid=$($apiProc.Id) running=$(-not $apiProc.HasExited))."
    Write-DemoLogExcerpt $apiLog
    if ($apiProc.HasExited) {
        Remove-Item (Join-Path $pidDir 'api.pid') -ErrorAction SilentlyContinue
    }
    exit 1
}

$web = Join-Path $Root 'snip-web'
if (-not (Test-Path (Join-Path $web 'node_modules'))) {
    Start-Process -FilePath 'npm' -ArgumentList @('ci') -WorkingDirectory $web -Wait -NoNewWindow
}
Write-Host "SNIP demo startup category: frontend-launch command=npm-run-dev port=5173 proxy=$ApiTarget"
$viteProc = Start-LoggedCmd -CommandLine 'npm run dev' -WorkDir $web -LogPath $viteLog -EnvOverrides @{
    SNIP_API_TARGET = $ApiTarget
}
Set-Content (Join-Path $pidDir 'vite.pid') $viteProc.Id
Write-Host "SNIP demo process status: vite pid=$($viteProc.Id) hasExited=$($viteProc.HasExited) log=$viteLog"

$deadline = (Get-Date).AddMinutes(2)
while ((Get-Date) -lt $deadline) {
    if ($viteProc.HasExited) {
        Write-Host "SNIP demo Vite process exited before port 5173 (exit=$($viteProc.ExitCode))."
        Write-DemoLogExcerpt $viteLog
        Remove-Item (Join-Path $pidDir 'vite.pid') -ErrorAction SilentlyContinue
        exit 1
    }
    if (Test-Tcp '127.0.0.1' 5173) { break }
    Start-Sleep -Seconds 2
}
if (-not (Test-Tcp '127.0.0.1' 5173)) {
    Write-Host 'SNIP demo Vite did not listen on 127.0.0.1:5173.'
    Write-DemoLogExcerpt $viteLog
    exit 1
}

& $ready
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Authoritative customer UI: http://127.0.0.1:5173'
Write-Host 'Do not use http://127.0.0.1:8080/ as the SNIP 1.0 customer demo.'
exit 0
