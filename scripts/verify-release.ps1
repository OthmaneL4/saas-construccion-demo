param(
    [Parameter(Mandatory = $true)]
    [string]$ReleasePath,
    [switch]$RequireTests
)

$ErrorActionPreference = "Stop"
$failures = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

function Add-Pass {
    param([string]$Message)
    Write-Host "[ OK ] $Message" -ForegroundColor Green
}

function Add-Warning {
    param([string]$Message)
    $warnings.Add($Message) | Out-Null
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Add-Failure {
    param([string]$Message)
    $failures.Add($Message) | Out-Null
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

if (-not (Test-Path -LiteralPath $ReleasePath -PathType Container)) {
    throw "Release path does not exist or is not a directory: $ReleasePath"
}

$releaseRoot = (Resolve-Path -LiteralPath $ReleasePath).Path
$manifestPath = Join-Path $releaseRoot "release-manifest.json"

Write-Host "LSOTOTALBOUW release verification"
Write-Host "Release path: $releaseRoot"

if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "release-manifest.json was not found: $manifestPath"
}

try {
    $manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
    Add-Pass "release-manifest.json is valid JSON"
} catch {
    throw "release-manifest.json is not valid JSON: $($_.Exception.Message)"
}

if ($manifest.application -ne "LSOTOTALBOUW") {
    Add-Failure "manifest application is not LSOTOTALBOUW"
} else {
    Add-Pass "manifest application matches"
}

if ([string]::IsNullOrWhiteSpace($manifest.artifactId)) {
    Add-Failure "artifactId is missing"
} else {
    Add-Pass "artifactId is present: $($manifest.artifactId)"
}

if ([string]::IsNullOrWhiteSpace($manifest.version)) {
    Add-Failure "version is missing"
} else {
    Add-Pass "version is present: $($manifest.version)"
}

if ([string]::IsNullOrWhiteSpace($manifest.jar.file)) {
    Add-Failure "jar file name is missing from manifest"
} else {
    $jarPath = Join-Path $releaseRoot $manifest.jar.file
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        Add-Failure "jar file is missing: $($manifest.jar.file)"
    } else {
        Add-Pass "jar file exists"

        $jarInfo = Get-Item -LiteralPath $jarPath
        if ($jarInfo.Length -le 0) {
            Add-Failure "jar file is empty"
        } else {
            Add-Pass "jar file is not empty"
        }

        if ($manifest.jar.sizeBytes -and [int64]$manifest.jar.sizeBytes -ne $jarInfo.Length) {
            Add-Failure "jar size does not match manifest"
        } else {
            Add-Pass "jar size matches manifest"
        }

        if ([string]::IsNullOrWhiteSpace($manifest.jar.sha256)) {
            Add-Failure "jar SHA-256 is missing from manifest"
        } else {
            $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $jarPath).Hash
            if ($actualHash -ne $manifest.jar.sha256) {
                Add-Failure "jar SHA-256 does not match manifest"
            } else {
                Add-Pass "jar SHA-256 matches manifest"
            }
        }
    }
}

if ($manifest.build.testsSkipped -eq $true) {
    if ($RequireTests) {
        Add-Failure "release was built with tests skipped"
    } else {
        Add-Warning "release was built with tests skipped"
    }
} else {
    Add-Pass "release was built with tests enabled"
}

if ([string]::IsNullOrWhiteSpace($manifest.git.revision)) {
    Add-Warning "git revision is missing from manifest"
} else {
    Add-Pass "git revision is present"
}

if ($manifest.git.dirty -eq $true) {
    Add-Warning "release was built from a dirty working tree"
}

if ($warnings.Count -gt 0) {
    Write-Host ""
    Write-Host "Release verification completed with $($warnings.Count) warning(s)." -ForegroundColor Yellow
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Release verification failed with $($failures.Count) issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Release verification passed." -ForegroundColor Green
