param(
    [Parameter(Mandatory = $true)]
    [string]$BackupSetPath,
    [string]$ExpectedDatabaseName = "lsototalbouw",
    [switch]$RequireDocuments
)

$ErrorActionPreference = "Stop"
$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure {
    param([string]$Message)
    $failures.Add($Message) | Out-Null
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Add-Pass {
    param([string]$Message)
    Write-Host "[ OK ] $Message" -ForegroundColor Green
}

function Resolve-BackupPath {
    param(
        [string]$Root,
        [string]$RelativePath
    )

    if ([string]::IsNullOrWhiteSpace($RelativePath)) {
        return $null
    }

    $normalized = $RelativePath -replace "/", [System.IO.Path]::DirectorySeparatorChar
    return Join-Path $Root $normalized
}

if (-not (Test-Path -LiteralPath $BackupSetPath -PathType Container)) {
    throw "Backup set path does not exist or is not a directory: $BackupSetPath"
}

$backupRoot = (Resolve-Path -LiteralPath $BackupSetPath).Path
$manifestPath = Join-Path $backupRoot "manifest.json"

Write-Host "LSOTOTALBOUW backup verification"
Write-Host "Backup set: $backupRoot"

if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "manifest.json was not found in backup set: $manifestPath"
}

try {
    $manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
    Add-Pass "manifest.json is valid JSON"
} catch {
    throw "manifest.json is not valid JSON: $($_.Exception.Message)"
}

if ($manifest.application -ne "LSOTOTALBOUW") {
    Add-Failure "manifest application is not LSOTOTALBOUW"
} else {
    Add-Pass "manifest application matches"
}

if ($manifest.database.name -ne $ExpectedDatabaseName) {
    Add-Failure "manifest database name '$($manifest.database.name)' does not match expected '$ExpectedDatabaseName'"
} else {
    Add-Pass "database name matches expected value"
}

$dumpPath = Resolve-BackupPath -Root $backupRoot -RelativePath $manifest.database.dumpFile
if (-not $dumpPath -or -not (Test-Path -LiteralPath $dumpPath -PathType Leaf)) {
    Add-Failure "database dump file is missing: $($manifest.database.dumpFile)"
} else {
    $dumpInfo = Get-Item -LiteralPath $dumpPath
    if ($dumpInfo.Length -le 0) {
        Add-Failure "database dump file is empty: $dumpPath"
    } else {
        Add-Pass "database dump exists and is not empty"
    }

    if ([string]::IsNullOrWhiteSpace($manifest.database.sha256)) {
        Add-Failure "manifest database SHA-256 is missing"
    } else {
        $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $dumpPath).Hash
        if ($actualHash -ne $manifest.database.sha256) {
            Add-Failure "database dump SHA-256 does not match manifest"
        } else {
            Add-Pass "database dump SHA-256 matches manifest"
        }
    }

    $firstChunk = Get-Content -LiteralPath $dumpPath -TotalCount 40
    $joinedChunk = $firstChunk -join "`n"
    if ($joinedChunk -notmatch "MySQL|MariaDB|Dump|CREATE|INSERT|SET") {
        Add-Failure "database dump does not look like a SQL dump"
    } else {
        Add-Pass "database dump has SQL-like content"
    }
}

$documentsIncluded = [bool]$manifest.documents.included
if ($RequireDocuments -and -not $documentsIncluded) {
    Add-Failure "documents are required but manifest says documents were not included"
}

if ($documentsIncluded) {
    $documentsPath = Resolve-BackupPath -Root $backupRoot -RelativePath $manifest.documents.backupPath
    if (-not $documentsPath -or -not (Test-Path -LiteralPath $documentsPath -PathType Container)) {
        Add-Failure "documents backup directory is missing: $($manifest.documents.backupPath)"
    } else {
        Add-Pass "documents backup directory exists"
    }
} else {
    Add-Pass "documents were intentionally skipped according to manifest"
}

$logsPath = Join-Path $backupRoot "logs"
if (Test-Path -LiteralPath $logsPath -PathType Container) {
    Add-Pass "logs directory exists"
} else {
    Add-Failure "logs directory is missing"
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Backup verification failed with $($failures.Count) issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Backup verification passed." -ForegroundColor Green
