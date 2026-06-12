param(
    [Parameter(Mandatory = $true)]
    [string]$ReleasePath,
    [int]$Port = $(if ($env:PORT) { [int]$env:PORT } else { 8080 }),
    [string]$LogsDir = "logs",
    [switch]$RequireTests,
    [switch]$SkipReleaseVerification
)

$ErrorActionPreference = "Stop"

function Assert-Value {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required before starting a production release."
    }
}

function Resolve-ReleaseJar {
    param([string]$ResolvedReleasePath)

    $manifestPath = Join-Path $ResolvedReleasePath "release-manifest.json"
    if (Test-Path -LiteralPath $manifestPath -PathType Leaf) {
        $manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
        if (-not [string]::IsNullOrWhiteSpace($manifest.jar.file)) {
            $manifestJar = Join-Path $ResolvedReleasePath $manifest.jar.file
            if (Test-Path -LiteralPath $manifestJar -PathType Leaf) {
                return $manifestJar
            }
        }
    }

    $jars = Get-ChildItem -LiteralPath $ResolvedReleasePath -Filter "*.jar" -File
    if ($jars.Count -eq 1) {
        return $jars[0].FullName
    }

    throw "Could not resolve a single release jar in $ResolvedReleasePath."
}

if (-not (Test-Path -LiteralPath $ReleasePath -PathType Container)) {
    throw "ReleasePath does not exist or is not a directory: $ReleasePath"
}

$releaseRoot = (Resolve-Path -LiteralPath $ReleasePath).Path

Assert-Value -Name "DB_URL" -Value $env:DB_URL
Assert-Value -Name "DB_USERNAME" -Value $env:DB_USERNAME
Assert-Value -Name "DB_PASSWORD" -Value $env:DB_PASSWORD
Assert-Value -Name "DOCUMENTS_DIR" -Value $env:DOCUMENTS_DIR

if ($env:DB_USERNAME -eq "root") {
    throw "Refusing to start production release with DB_USERNAME=root."
}

if (-not (Test-Path -LiteralPath $env:DOCUMENTS_DIR -PathType Container)) {
    throw "DOCUMENTS_DIR does not exist or is not a directory: $env:DOCUMENTS_DIR"
}

if (-not $SkipReleaseVerification) {
    $verifyScript = Join-Path $PSScriptRoot "verify-release.ps1"
    if (-not (Test-Path -LiteralPath $verifyScript -PathType Leaf)) {
        throw "verify-release.ps1 was not found beside start-release.ps1."
    }

    $verifyArgs = @("-ExecutionPolicy", "Bypass", "-File", $verifyScript, "-ReleasePath", $releaseRoot)
    if ($RequireTests) {
        $verifyArgs += "-RequireTests"
    }

    & powershell @verifyArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Release verification failed. Refusing to start release."
    }
}

$jarPath = Resolve-ReleaseJar -ResolvedReleasePath $releaseRoot
New-Item -ItemType Directory -Force -Path $LogsDir | Out-Null
$logFile = Join-Path $LogsDir ("lsototalbouw-" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".log")

$env:SPRING_PROFILES_ACTIVE = "prod"
$env:PORT = "$Port"

Write-Host "Starting LSOTOTALBOUW release" -ForegroundColor Green
Write-Host "Release: $releaseRoot"
Write-Host "Jar: $jarPath"
Write-Host "Port: $Port"
Write-Host "Log file: $logFile"
Write-Host ""
Write-Host "Press Ctrl+C to stop the foreground process."
Write-Host ""

& java -jar $jarPath *>&1 | Tee-Object -FilePath $logFile
