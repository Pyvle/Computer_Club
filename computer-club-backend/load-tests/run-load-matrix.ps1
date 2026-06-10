param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$RunTime = "2m",
    [int]$SpawnRate = 5,
    [string]$Users = "25,50,100,200",
    [string]$PythonExe = "python",
    [string]$OutputRoot = ".\load-tests\results",
    [switch]$UseTargetDeps
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$locustFile = Join-Path $scriptDir "locustfile.py"
$summaryScript = Join-Path $scriptDir "summarize_results.py"
$depsDir = Join-Path $scriptDir ".deps"

if (!(Test-Path $locustFile)) {
    throw "Locust file not found: $locustFile"
}

$previousPythonPath = $env:PYTHONPATH
if ($UseTargetDeps -and (Test-Path $depsDir)) {
    if ([string]::IsNullOrWhiteSpace($previousPythonPath)) {
        $env:PYTHONPATH = $depsDir
    } else {
        $env:PYTHONPATH = "$depsDir;$previousPythonPath"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputDir = Join-Path $OutputRoot $timestamp
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$userLevels = $Users.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ } | ForEach-Object { [int]$_ }

Write-Host "Output directory: $outputDir"
Write-Host "Host: $BaseUrl"
Write-Host "Run time: $RunTime"
Write-Host "Spawn rate: $SpawnRate"
Write-Host "Users: $($userLevels -join ', ')"

foreach ($userCount in $userLevels) {
    $prefix = Join-Path $outputDir ("users-{0:000}" -f $userCount)
    Write-Host ""
    Write-Host "=== Running load test for $userCount users ==="

    & $PythonExe -m locust `
        -f $locustFile `
        --headless `
        --host $BaseUrl `
        --users $userCount `
        --spawn-rate $SpawnRate `
        --run-time $RunTime `
        --stop-timeout 30 `
        --only-summary `
        --csv $prefix `
        --csv-full-history `
        --html "$prefix.html"

    if ($LASTEXITCODE -ne 0) {
        throw "Locust run failed for $userCount users"
    }
}

Write-Host ""
Write-Host "=== Building summary report ==="
& $PythonExe $summaryScript $outputDir

if ($LASTEXITCODE -ne 0) {
    throw "Summary generation failed"
}

Write-Host ""
Write-Host "Done."
Write-Host "Summary markdown: $(Join-Path $outputDir 'summary.md')"
Write-Host "Summary chart:    $(Join-Path $outputDir 'summary.svg')"

$env:PYTHONPATH = $previousPythonPath
