param(
    [string]$DbUrl = $(if ($env:DB_URL) { $env:DB_URL } else { "jdbc:h2:file:./data/lsototalbouw;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE" }),
    [string]$DbUsername = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "sa" }),
    [string]$DbPassword = $env:DB_PASSWORD,
    [string]$Pattern = "(test|qa|dummy|error|lorem|asdf|sample|prueba)",
    [string]$H2Jar,
    [switch]$AllowArchivedMatches
)

$ErrorActionPreference = "Stop"
$findings = New-Object System.Collections.Generic.List[string]

function Add-Finding {
    param([string]$Message)
    $findings.Add($Message) | Out-Null
    Write-Host "[FIND] $Message" -ForegroundColor Yellow
}

function Add-Pass {
    param([string]$Message)
    Write-Host "[ OK ] $Message" -ForegroundColor Green
}

function Assert-Value {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required. Set it as an environment variable or pass it as a parameter."
    }
}

function Get-DatabaseKind {
    param([string]$JdbcUrl)

    if ($JdbcUrl -like "jdbc:mysql://*") {
        return "mysql"
    }
    if ($JdbcUrl -like "jdbc:h2:*") {
        return "h2"
    }

    throw "Unsupported DB_URL. Expected a MySQL or H2 JDBC URL."
}

function Get-MySqlConnectionParts {
    param([string]$JdbcUrl)

    $match = [regex]::Match($JdbcUrl, "^jdbc:mysql://(?<host>[^:/?]+)(:(?<port>[0-9]+))?/(?<database>[^?]+)")
    if (-not $match.Success) {
        throw "DB_URL could not be parsed as a MySQL JDBC URL."
    }

    return [ordered]@{
        Host = $match.Groups["host"].Value
        Port = $(if ($match.Groups["port"].Success) { $match.Groups["port"].Value } else { "3306" })
        Database = $match.Groups["database"].Value
    }
}

function Get-H2JarPath {
    param([string]$ExplicitPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath -PathType Leaf) {
            return (Resolve-Path -LiteralPath $ExplicitPath).Path
        }
        throw "H2 jar was not found: $ExplicitPath"
    }

    $candidate = Get-ChildItem "$env:USERPROFILE\.m2\repository\com\h2database\h2" `
        -Recurse -Filter "h2-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*sources*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($candidate) {
        return $candidate.FullName
    }

    throw "H2 jar was not found in the local Maven repository. Run Maven once or pass -H2Jar."
}

function Invoke-MySqlScalarQuery {
    param(
        [string]$DefaultsFile,
        [string]$Database,
        [string]$Sql
    )

    $result = (& mysql "--defaults-extra-file=$DefaultsFile" "--batch" "--skip-column-names" $Database "-e" $Sql)
    if ($LASTEXITCODE -ne 0) {
        throw "mysql query failed: $Sql"
    }

    if ([string]::IsNullOrWhiteSpace($result)) {
        return 0
    }

    return [int]($result | Select-Object -First 1)
}

function Invoke-H2ScalarQuery {
    param(
        [string]$JarPath,
        [string]$JdbcUrl,
        [string]$Username,
        [string]$Password,
        [string]$Sql
    )

    $javaArgs = @("-cp", $JarPath, "org.h2.tools.Shell", "-url", $JdbcUrl, "-user", $Username)
    if (-not [string]::IsNullOrWhiteSpace($Password)) {
        $javaArgs += "-password"
        $javaArgs += $Password
    }
    $javaArgs += "-sql"
    $javaArgs += $Sql

    $result = (& java @javaArgs)
    if ($LASTEXITCODE -ne 0) {
        throw "H2 query failed: $Sql"
    }

    $countLine = $result | Where-Object { $_ -match "^\s*[0-9]+\s*$" } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($countLine)) {
        return 0
    }

    return [int]$countLine.Trim()
}

Assert-Value -Name "DB_URL" -Value $DbUrl

$databaseKind = Get-DatabaseKind -JdbcUrl $DbUrl
$connection = $null
$defaultsFile = $null
$h2JarPath = $null

if ($databaseKind -eq "mysql") {
    Assert-Value -Name "DB_USERNAME" -Value $DbUsername
    Assert-Value -Name "DB_PASSWORD" -Value $DbPassword

    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if (-not $mysql) {
        throw "mysql was not found in PATH. Install MySQL client tools or add them to PATH."
    }

    $connection = Get-MySqlConnectionParts -JdbcUrl $DbUrl
    $defaultsFile = Join-Path ([System.IO.Path]::GetTempPath()) ("lsototalbouw-demo-audit-" + [guid]::NewGuid() + ".cnf")
} else {
    Assert-Value -Name "DB_USERNAME" -Value $DbUsername
    $h2JarPath = Get-H2JarPath -ExplicitPath $H2Jar
}

$checks = @(
    @{ Table = "customers"; Label = "Customers"; Columns = @("name", "email", "phone", "address", "city") },
    @{ Table = "projects"; Label = "Projects"; Columns = @("name", "work_address") },
    @{ Table = "quotations"; Label = "Quotations"; Columns = @("quotation_number", "title", "description") },
    @{ Table = "quotation_lines"; Label = "Quotation lines"; Columns = @("description") },
    @{ Table = "invoices"; Label = "Invoices"; Columns = @("invoice_number") },
    @{ Table = "invoice_lines"; Label = "Invoice lines"; Columns = @("description") },
    @{ Table = "payments"; Label = "Payments"; Columns = @("method") },
    @{ Table = "expenses"; Label = "Expenses"; Columns = @("description", "category") },
    @{ Table = "materials"; Label = "Materials"; Columns = @("name", "unit") },
    @{ Table = "tools"; Label = "Tools"; Columns = @("name", "serial_number") },
    @{ Table = "suppliers"; Label = "Suppliers"; Columns = @("name", "contact_name", "email", "phone", "address", "city") },
    @{ Table = "calendar_events"; Label = "Calendar events"; Columns = @("title", "notes") },
    @{ Table = "business_documents"; Label = "Documents"; Columns = @("title", "original_filename", "notes") },
    @{ Table = "work_logs"; Label = "Work logs"; Columns = @("worker_name", "description") },
    @{ Table = "notifications"; Label = "Notifications"; Columns = @("title", "message") }
)

try {
    if ($databaseKind -eq "mysql") {
        @(
            "[client]",
            "host=$($connection.Host)",
            "port=$($connection.Port)",
            "user=$DbUsername",
            "password=$DbPassword",
            "database=$($connection.Database)"
        ) | Set-Content -LiteralPath $defaultsFile -Encoding ASCII
    }

    Write-Host "LSOTOTALBOUW demo data audit"
    if ($databaseKind -eq "mysql") {
        Write-Host "Database: $($connection.Database)"
    } else {
        Write-Host "Database: local H2 development database"
    }
    Write-Host "Pattern: $Pattern"

    foreach ($check in $checks) {
        $conditions = New-Object System.Collections.Generic.List[string]
        foreach ($column in $check.Columns) {
            $conditions.Add("LOWER(COALESCE($column, '')) REGEXP '$Pattern'") | Out-Null
        }

        $where = "(" + ($conditions -join " OR ") + ")"
        if (-not $AllowArchivedMatches) {
            $where = "active = TRUE AND $where"
        }

        $sql = "SELECT COUNT(*) FROM $($check.Table) WHERE $where;"
        if ($databaseKind -eq "mysql") {
            $count = Invoke-MySqlScalarQuery -DefaultsFile $defaultsFile -Database $connection.Database -Sql $sql
        } else {
            $count = Invoke-H2ScalarQuery -JarPath $h2JarPath -JdbcUrl $DbUrl -Username $DbUsername `
                -Password $DbPassword -Sql $sql
        }
        if ($count -gt 0) {
            Add-Finding "$($check.Label): $count suspicious active record(s)"
        } else {
            Add-Pass "$($check.Label) look demo-ready"
        }
    }
} finally {
    if ($defaultsFile -and (Test-Path -LiteralPath $defaultsFile)) {
        Remove-Item -LiteralPath $defaultsFile -Force
    }
}

Write-Host ""
if ($findings.Count -gt 0) {
    Write-Host "Demo data audit found $($findings.Count) issue area(s)." -ForegroundColor Red
    Write-Host "Archive, rename, or replace these records before presenting." -ForegroundColor Red
    exit 1
}

Write-Host "Demo data audit passed." -ForegroundColor Green
