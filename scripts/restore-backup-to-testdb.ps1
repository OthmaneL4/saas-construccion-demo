param(
    [Parameter(Mandatory = $true)]
    [string]$BackupSetPath,
    [string]$DbHost = $env:DB_HOST,
    [string]$DbPort = $env:DB_PORT,
    [string]$DbUsername = $env:DB_USERNAME,
    [string]$DbPassword = $env:DB_PASSWORD,
    [string]$TargetDatabaseName = "lsototalbouw_restore_test",
    [string]$RestoreDocumentsDir = (Join-Path $env:TEMP "lsototalbouw-restore-test-documents"),
    [switch]$SkipDocuments,
    [switch]$KeepExistingDatabase
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

function Invoke-MySql {
    param(
        [string]$DefaultsFile,
        [string]$Database,
        [string[]]$Arguments
    )

    $mysqlArgs = @("--defaults-extra-file=$DefaultsFile")
    if (-not [string]::IsNullOrWhiteSpace($Database)) {
        $mysqlArgs += $Database
    }
    $mysqlArgs += $Arguments

    & mysql @mysqlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "mysql failed with exit code $LASTEXITCODE"
    }
}

Assert-Value -Name "DB_HOST" -Value $DbHost
Assert-Value -Name "DB_USERNAME" -Value $DbUsername
Assert-Value -Name "DB_PASSWORD" -Value $DbPassword
Assert-Value -Name "TargetDatabaseName" -Value $TargetDatabaseName

if ($TargetDatabaseName -eq "lsototalbouw") {
    throw "Refusing to restore into production database name 'lsototalbouw'. Use a dedicated restore-test database."
}

if ($TargetDatabaseName -notmatch "(_restore_test|_test|_sandbox)$") {
    throw "TargetDatabaseName must end with _restore_test, _test, or _sandbox. Current value: $TargetDatabaseName"
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    throw "mysql was not found in PATH. Install MySQL client tools or add them to PATH."
}

if (-not $SkipDocuments) {
    $robocopy = Get-Command robocopy -ErrorAction SilentlyContinue
    if (-not $robocopy) {
        throw "robocopy was not found in PATH. It is required for document restore testing on Windows."
    }
}

if (-not (Test-Path -LiteralPath $BackupSetPath -PathType Container)) {
    throw "Backup set path does not exist or is not a directory: $BackupSetPath"
}

$backupRoot = (Resolve-Path -LiteralPath $BackupSetPath).Path
$manifestPath = Join-Path $backupRoot "manifest.json"
if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "manifest.json was not found in backup set: $manifestPath"
}

$manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
if ($manifest.application -ne "LSOTOTALBOUW") {
    throw "Backup manifest does not belong to LSOTOTALBOUW."
}

$dumpPath = Resolve-BackupPath -Root $backupRoot -RelativePath $manifest.database.dumpFile
if (-not $dumpPath -or -not (Test-Path -LiteralPath $dumpPath -PathType Leaf)) {
    throw "Database dump file is missing: $($manifest.database.dumpFile)"
}

$actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $dumpPath).Hash
if ($actualHash -ne $manifest.database.sha256) {
    throw "Database dump SHA-256 does not match manifest. Refusing restore test."
}

$defaultsFile = Join-Path ([System.IO.Path]::GetTempPath()) ("lsototalbouw-restore-test-" + [guid]::NewGuid() + ".cnf")

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

    Write-Host "Preparing restore-test database: $TargetDatabaseName"
    if (-not $KeepExistingDatabase) {
        Invoke-MySql -DefaultsFile $defaultsFile -Database "" -Arguments @("-e", "DROP DATABASE IF EXISTS ``$TargetDatabaseName``; CREATE DATABASE ``$TargetDatabaseName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
    } else {
        Invoke-MySql -DefaultsFile $defaultsFile -Database "" -Arguments @("-e", "CREATE DATABASE IF NOT EXISTS ``$TargetDatabaseName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
    }

    Write-Host "Restoring SQL dump into $TargetDatabaseName"
    Get-Content -LiteralPath $dumpPath | & mysql "--defaults-extra-file=$defaultsFile" $TargetDatabaseName
    if ($LASTEXITCODE -ne 0) {
        throw "mysql restore failed with exit code $LASTEXITCODE"
    }

    $tableCount = (& mysql "--defaults-extra-file=$defaultsFile" "--batch" "--skip-column-names" $TargetDatabaseName "-e" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE();")
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect restored database tables"
    }

    $tableCountValue = [int]($tableCount | Select-Object -First 1)
    if ($tableCountValue -le 0) {
        throw "Restore completed but no tables were found in $TargetDatabaseName"
    }

    Write-Host "Restored table count: $tableCountValue" -ForegroundColor Green

    if (-not $SkipDocuments -and [bool]$manifest.documents.included) {
        $documentsPath = Resolve-BackupPath -Root $backupRoot -RelativePath $manifest.documents.backupPath
        if (-not $documentsPath -or -not (Test-Path -LiteralPath $documentsPath -PathType Container)) {
            throw "Documents backup directory is missing: $($manifest.documents.backupPath)"
        }

        New-Item -ItemType Directory -Force -Path $RestoreDocumentsDir | Out-Null
        Write-Host "Restoring documents into test directory: $RestoreDocumentsDir"
        & robocopy $documentsPath $RestoreDocumentsDir /E /R:2 /W:2 /NFL /NDL /NP | Out-Null
        $robocopyExitCode = $LASTEXITCODE
        if ($robocopyExitCode -gt 7) {
            throw "robocopy failed with exit code $robocopyExitCode"
        }
    }

    Write-Host ""
    Write-Host "Restore test completed successfully." -ForegroundColor Green
    Write-Host "Database: $TargetDatabaseName"
    if (-not $SkipDocuments -and [bool]$manifest.documents.included) {
        Write-Host "Documents: $RestoreDocumentsDir"
    }
} finally {
    if (Test-Path -LiteralPath $defaultsFile) {
        Remove-Item -LiteralPath $defaultsFile -Force
    }
}
