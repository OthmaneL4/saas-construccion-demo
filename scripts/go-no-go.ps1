param(
    [string]$ReleasePath,
    [string]$BaseUrl,
    [switch]$FullTests,
    [switch]$SkipCompile,
    [switch]$SkipPreflight,
    [switch]$SkipReleaseVerification,
    [switch]$SkipSmokeTest,
    [switch]$RequireReleaseTests
)

$ErrorActionPreference = "Stop"
$failures = New-Object System.Collections.Generic.List[string]

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "== $Name ==" -ForegroundColor Cyan
    try {
        & $Action
        if ($LASTEXITCODE -ne 0) {
            throw "$Name failed with exit code $LASTEXITCODE"
        }
        Write-Host "[ OK ] $Name" -ForegroundColor Green
    } catch {
        $failures.Add("${Name}: $($_.Exception.Message)") | Out-Null
        Write-Host "[FAIL] ${Name}: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "LSOTOTALBOUW technical Go/No-Go"

if (-not $SkipCompile) {
    Invoke-Step -Name "Maven verification" -Action {
        if ($FullTests) {
            & .\mvnw.cmd -q clean test
        } else {
            & .\mvnw.cmd -q -DskipTests compile
        }
    }
}

if (-not $SkipPreflight) {
    Invoke-Step -Name "Production preflight" -Action {
        powershell -ExecutionPolicy Bypass -File .\scripts\preflight-production.ps1
    }
}

if (-not $SkipReleaseVerification) {
    if ([string]::IsNullOrWhiteSpace($ReleasePath)) {
        $failures.Add("Release verification: ReleasePath was not provided") | Out-Null
        Write-Host "[FAIL] Release verification: ReleasePath was not provided" -ForegroundColor Red
    } else {
        Invoke-Step -Name "Release verification" -Action {
            $args = @("-ExecutionPolicy", "Bypass", "-File", ".\scripts\verify-release.ps1", "-ReleasePath", $ReleasePath)
            if ($RequireReleaseTests) {
                $args += "-RequireTests"
            }
            & powershell @args
        }
    }
}

if (-not $SkipSmokeTest) {
    if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        $failures.Add("Smoke test: BaseUrl was not provided") | Out-Null
        Write-Host "[FAIL] Smoke test: BaseUrl was not provided" -ForegroundColor Red
    } else {
        Invoke-Step -Name "Smoke test" -Action {
            powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1 -BaseUrl $BaseUrl
        }
    }
}

Write-Host ""
if ($failures.Count -gt 0) {
    Write-Host "NO-GO: $($failures.Count) blocking issue(s) detected." -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host "- $failure" -ForegroundColor Red
    }
    exit 1
}

Write-Host "GO: technical checks passed." -ForegroundColor Green
