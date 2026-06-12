param(
    [string]$ReleaseRoot = "releases",
    [string]$Label = "manual",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

function Get-GitValue {
    param([string[]]$Arguments)

    try {
        $value = (& git @Arguments 2>$null)
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($value)) {
            return ($value | Select-Object -First 1).Trim()
        }
    } catch {
        return $null
    }

    return $null
}

function Get-PomValue {
    param([string]$XPath)

    [xml]$pom = Get-Content -Raw -LiteralPath "pom.xml"
    $namespaceManager = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
    $namespaceManager.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    return $pom.SelectSingleNode($XPath, $namespaceManager).InnerText
}

if (-not (Test-Path -LiteralPath ".\mvnw.cmd" -PathType Leaf)) {
    throw "Maven wrapper .\mvnw.cmd was not found."
}

$artifactId = Get-PomValue -XPath "/m:project/m:artifactId"
$version = Get-PomValue -XPath "/m:project/m:version"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$safeLabel = ($Label -replace "[^a-zA-Z0-9_-]", "-").Trim("-")
if ([string]::IsNullOrWhiteSpace($safeLabel)) {
    $safeLabel = "manual"
}

$releaseDir = Join-Path $ReleaseRoot "$timestamp-$safeLabel"
New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null

$mavenArgs = @("-q")
if ($SkipTests) {
    $mavenArgs += "-DskipTests"
}
$mavenArgs += "clean"
$mavenArgs += "package"

Write-Host "Building release artifact with Maven..."
& .\mvnw.cmd @mavenArgs
if ($LASTEXITCODE -ne 0) {
    throw "Maven package failed with exit code $LASTEXITCODE"
}

$jarName = "$artifactId-$version.jar"
$sourceJar = Join-Path "target" $jarName
if (-not (Test-Path -LiteralPath $sourceJar -PathType Leaf)) {
    throw "Expected release jar was not found: $sourceJar"
}

$targetJar = Join-Path $releaseDir $jarName
Copy-Item -LiteralPath $sourceJar -Destination $targetJar -Force

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $targetJar).Hash
$gitRevision = Get-GitValue -Arguments @("rev-parse", "HEAD")
$gitBranch = Get-GitValue -Arguments @("rev-parse", "--abbrev-ref", "HEAD")
$gitStatus = Get-GitValue -Arguments @("status", "--short")

$manifest = [ordered]@{
    application = "LSOTOTALBOUW"
    artifactId = $artifactId
    version = $version
    createdAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    label = $safeLabel
    jar = [ordered]@{
        file = $jarName
        sha256 = $hash
        sizeBytes = (Get-Item -LiteralPath $targetJar).Length
    }
    git = [ordered]@{
        branch = $gitBranch
        revision = $gitRevision
        dirty = -not [string]::IsNullOrWhiteSpace($gitStatus)
    }
    build = [ordered]@{
        testsSkipped = [bool]$SkipTests
        command = ".\mvnw.cmd " + ($mavenArgs -join " ")
    }
}

$manifestPath = Join-Path $releaseDir "release-manifest.json"
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

Write-Host ""
Write-Host "Release artifact created successfully." -ForegroundColor Green
Write-Host "Release directory: $releaseDir"
Write-Host "Jar: $targetJar"
Write-Host "Manifest: $manifestPath"
