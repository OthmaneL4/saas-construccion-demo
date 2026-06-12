param(
    [string]$DbUrl = $env:DB_URL,
    [string]$DbUsername = $env:DB_USERNAME,
    [string]$DbPassword = $env:DB_PASSWORD,
    [string]$DocumentsDir = $env:DOCUMENTS_DIR,
    [string]$SpringProfile = $env:SPRING_PROFILES_ACTIVE,
    [string]$ExpectedDatabaseName = "lsototalbouw",
    [switch]$SkipMySqlConnection,
    [switch]$SkipMySqlToolsCheck,
    [switch]$SkipMavenCheck
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

function Test-RequiredValue {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        Add-Failure "$Name is missing"
        return $false
    }

    if ($Value -like "<*>" -or $Value -like "*CHANGE-ME*" -or $Value -like "*change-me*") {
        Add-Failure "$Name still contains a placeholder value"
        return $false
    }

    Add-Pass "$Name is configured"
    return $true
}

function Get-MySqlConnectionParts {
    param([string]$JdbcUrl)

    $pattern = "^jdbc:mysql://(?<host>[^:/?]+)(:(?<port>[0-9]+))?/(?<database>[^?]+)"
    $match = [regex]::Match($JdbcUrl, $pattern)
    if (-not $match.Success) {
        return $null
    }

    return [ordered]@{
        Host = $match.Groups["host"].Value
        Port = $(if ($match.Groups["port"].Success) { $match.Groups["port"].Value } else { "3306" })
        Database = $match.Groups["database"].Value
    }
}

function Test-CommandAvailable {
    param([string]$Name)

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        Add-Pass "$Name is available"
        return $true
    }

    Add-Failure "$Name is not available in PATH"
    return $false
}

Write-Host "LSOTOTALBOUW production preflight"

Test-RequiredValue -Name "SPRING_PROFILES_ACTIVE" -Value $SpringProfile | Out-Null
if ($SpringProfile -ne "prod") {
    Add-Failure "SPRING_PROFILES_ACTIVE must be prod for production preflight"
}

$hasDbUrl = Test-RequiredValue -Name "DB_URL" -Value $DbUrl
$hasDbUsername = Test-RequiredValue -Name "DB_USERNAME" -Value $DbUsername
$hasDbPassword = Test-RequiredValue -Name "DB_PASSWORD" -Value $DbPassword
$hasDocumentsDir = Test-RequiredValue -Name "DOCUMENTS_DIR" -Value $DocumentsDir

if ($hasDbUrl) {
    if ($DbUrl -notlike "jdbc:mysql://*") {
        Add-Failure "DB_URL must use jdbc:mysql://"
    } else {
        Add-Pass "DB_URL uses MySQL JDBC format"
    }

    if ($DbUrl -like "*allowPublicKeyRetrieval=true*") {
        Add-Warning "DB_URL enables allowPublicKeyRetrieval=true; avoid this in real production when possible"
    }

    if ($DbUrl -notlike "*serverTimezone=UTC*") {
        Add-Warning "DB_URL does not explicitly set serverTimezone=UTC"
    }

    $connectionParts = Get-MySqlConnectionParts -JdbcUrl $DbUrl
    if (-not $connectionParts) {
        Add-Failure "DB_URL could not be parsed"
    } else {
        Add-Pass "DB_URL parsed as $($connectionParts.Host):$($connectionParts.Port)/$($connectionParts.Database)"
        if ($connectionParts.Database -ne $ExpectedDatabaseName) {
            Add-Failure "DB_URL database '$($connectionParts.Database)' does not match expected '$ExpectedDatabaseName'"
        }
    }
}

if ($DbUsername -eq "root") {
    Add-Failure "DB_USERNAME must not be root"
}

if ($hasDocumentsDir) {
    if (Test-Path -LiteralPath $DocumentsDir -PathType Container) {
        Add-Pass "DOCUMENTS_DIR exists"

        try {
            $probe = Join-Path $DocumentsDir ("preflight-" + [guid]::NewGuid() + ".tmp")
            Set-Content -LiteralPath $probe -Value "LSOTOTALBOUW preflight" -Encoding ASCII
            Remove-Item -LiteralPath $probe -Force
            Add-Pass "DOCUMENTS_DIR is writable and deletable"
        } catch {
            Add-Failure "DOCUMENTS_DIR is not writable/deletable: $($_.Exception.Message)"
        }
    } else {
        Add-Failure "DOCUMENTS_DIR does not exist or is not a directory: $DocumentsDir"
    }
}

Test-CommandAvailable -Name "java" | Out-Null
if (-not $SkipMySqlToolsCheck) {
    Test-CommandAvailable -Name "mysql" | Out-Null
    Test-CommandAvailable -Name "mysqldump" | Out-Null
} else {
    Add-Warning "MySQL client tool checks were skipped"
}
Test-CommandAvailable -Name "robocopy" | Out-Null

if (-not $SkipMavenCheck) {
    if (Test-Path -LiteralPath ".\mvnw.cmd" -PathType Leaf) {
        Add-Pass "Maven wrapper exists"
    } else {
        Add-Failure "Maven wrapper .\mvnw.cmd was not found"
    }
}

if (-not $SkipMySqlConnection -and $hasDbUrl -and $hasDbUsername -and $hasDbPassword) {
    $connectionParts = Get-MySqlConnectionParts -JdbcUrl $DbUrl
    if ($connectionParts -and (Get-Command mysql -ErrorAction SilentlyContinue)) {
        $defaultsFile = Join-Path ([System.IO.Path]::GetTempPath()) ("lsototalbouw-preflight-" + [guid]::NewGuid() + ".cnf")
        try {
            @(
                "[client]",
                "host=$($connectionParts.Host)",
                "port=$($connectionParts.Port)",
                "user=$DbUsername",
                "password=$DbPassword",
                "database=$($connectionParts.Database)"
            ) | Set-Content -LiteralPath $defaultsFile -Encoding ASCII

            & mysql "--defaults-extra-file=$defaultsFile" "--batch" "--skip-column-names" "-e" "SELECT 1;" *> $null
            if ($LASTEXITCODE -eq 0) {
                Add-Pass "MySQL connection check succeeded"
            } else {
                Add-Failure "MySQL connection check failed with exit code $LASTEXITCODE"
            }
        } finally {
            if (Test-Path -LiteralPath $defaultsFile) {
                Remove-Item -LiteralPath $defaultsFile -Force
            }
        }
    }
}

if ($warnings.Count -gt 0) {
    Write-Host ""
    Write-Host "Preflight completed with $($warnings.Count) warning(s)." -ForegroundColor Yellow
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Preflight failed with $($failures.Count) issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Production preflight passed." -ForegroundColor Green
