param(
    [string]$DbHost = $env:DB_HOST,
    [string]$DbPort = $env:DB_PORT,
    [string]$DbName = $(if ($env:DB_NAME) { $env:DB_NAME } else { "lsototalbouw" }),
    [string]$DbUsername = $env:DB_USERNAME,
    [string]$DbPassword = $env:DB_PASSWORD,
    [string]$DocumentsDir = $env:DOCUMENTS_DIR,
    [string]$BackupRoot = "backups",
    [string]$Label = "manual",
    [switch]$SkipDocuments,
    [switch]$SkipGitRevision
)

$ErrorActionPreference = "Stop"

function Assert-Value {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required. Set it as an environment variable or pass it as a parameter."
    }
}

function Get-GitRevision {
    try {
        $revision = (& git rev-parse HEAD 2>$null)
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($revision)) {
            return $revision.Trim()
        }
    } catch {
        return $null
    }

    return $null
}

Assert-Value -Name "DB_HOST" -Value $DbHost
Assert-Value -Name "DB_USERNAME" -Value $DbUsername
Assert-Value -Name "DB_PASSWORD" -Value $DbPassword
Assert-Value -Name "DB_NAME" -Value $DbName

if (-not $SkipDocuments) {
    Assert-Value -Name "DOCUMENTS_DIR" -Value $DocumentsDir
    if (-not (Test-Path -LiteralPath $DocumentsDir -PathType Container)) {
        throw "DOCUMENTS_DIR does not exist or is not a directory: $DocumentsDir"
    }
}

$mysqldump = Get-Command mysqldump -ErrorAction SilentlyContinue
if (-not $mysqldump) {
    throw "mysqldump was not found in PATH. Install MySQL client tools or add them to PATH."
}

if (-not $SkipDocuments) {
    $robocopy = Get-Command robocopy -ErrorAction SilentlyContinue
    if (-not $robocopy) {
        throw "robocopy was not found in PATH. It is required for document backups on Windows."
    }
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$safeLabel = ($Label -replace "[^a-zA-Z0-9_-]", "-").Trim("-")
if ([string]::IsNullOrWhiteSpace($safeLabel)) {
    $safeLabel = "manual"
}

$backupSet = Join-Path $BackupRoot "$timestamp-$safeLabel"
$databaseDir = Join-Path $backupSet "database"
$documentsBackupDir = Join-Path $backupSet "documents"
$logsDir = Join-Path $backupSet "logs"
New-Item -ItemType Directory -Force -Path $databaseDir, $logsDir | Out-Null

$dumpFile = Join-Path $databaseDir "$DbName.sql"
$dumpLog = Join-Path $logsDir "mysqldump.log"
$documentsLog = Join-Path $logsDir "documents-robocopy.log"
$manifestFile = Join-Path $backupSet "manifest.json"
$defaultsFile = Join-Path ([System.IO.Path]::GetTempPath()) "lsototalbouw-mysql-$timestamp.cnf"

try {
    $defaults = @(
        "[client]",
        "host=$DbHost",
        "user=$DbUsername",
        "password=$DbPassword"
    )

    if (-not [string]::IsNullOrWhiteSpace($DbPort)) {
        $defaults += "port=$DbPort"
    }

    Set-Content -LiteralPath $defaultsFile -Value $defaults -Encoding ASCII

    $dumpArgs = @(
        "--defaults-extra-file=$defaultsFile",
        "--single-transaction",
        "--routines",
        "--triggers",
        "--set-gtid-purged=OFF",
        "--result-file=$dumpFile",
        $DbName
    )

    Write-Host "Creating MySQL dump: $dumpFile"
    & $mysqldump.Source @dumpArgs *> $dumpLog
    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump failed with exit code $LASTEXITCODE. See $dumpLog"
    }

    $dumpHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $dumpFile).Hash

    $documentsCopied = $false
    if (-not $SkipDocuments) {
        New-Item -ItemType Directory -Force -Path $documentsBackupDir | Out-Null
        Write-Host "Copying documents: $DocumentsDir -> $documentsBackupDir"
        & robocopy $DocumentsDir $documentsBackupDir /E /R:2 /W:2 /NFL /NDL /NP /LOG:$documentsLog | Out-Null
        $robocopyExitCode = $LASTEXITCODE
        if ($robocopyExitCode -gt 7) {
            throw "robocopy failed with exit code $robocopyExitCode. See $documentsLog"
        }
        $documentsCopied = $true
    }

    $gitRevision = $null
    if (-not $SkipGitRevision) {
        $gitRevision = Get-GitRevision
    }

    $manifest = [ordered]@{
        application = "LSOTOTALBOUW"
        createdAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        label = $safeLabel
        database = [ordered]@{
            host = $DbHost
            port = $DbPort
            name = $DbName
            username = $DbUsername
            dumpFile = "database/$DbName.sql"
            sha256 = $dumpHash
        }
        documents = [ordered]@{
            included = $documentsCopied
            source = $(if ($SkipDocuments) { $null } else { $DocumentsDir })
            backupPath = $(if ($documentsCopied) { "documents" } else { $null })
        }
        gitRevision = $gitRevision
        restoreRunbook = "docs/PRODUCTION_RUNBOOK.md"
    }

    $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestFile -Encoding UTF8

    Write-Host ""
    Write-Host "Backup completed successfully." -ForegroundColor Green
    Write-Host "Backup set: $backupSet"
    Write-Host "Manifest: $manifestFile"
} finally {
    if (Test-Path -LiteralPath $defaultsFile) {
        Remove-Item -LiteralPath $defaultsFile -Force
    }
}
