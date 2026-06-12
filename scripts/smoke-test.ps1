param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = $env:APP_BOOTSTRAP_ADMIN_EMAIL,
    [string]$Password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD,
    [switch]$SkipAuthenticatedRoutes
)

$ErrorActionPreference = "Stop"

$BaseUrl = $BaseUrl.TrimEnd("/")
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$failedChecks = New-Object System.Collections.Generic.List[string]

function Get-ResponseText {
    param($Response)

    if ($Response.Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Response.Content)
    }

    return [string]$Response.Content
}

function Add-Failure {
    param([string]$Message)
    $failedChecks.Add($Message) | Out-Null
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Add-Pass {
    param([string]$Message)
    Write-Host "[ OK ] $Message" -ForegroundColor Green
}

function Get-CsrfToken {
    param([string]$Html)

    $nameBeforeValue = [regex]::Match($Html, 'name="_csrf"[^>]*value="([^"]+)"')
    if ($nameBeforeValue.Success) {
        return $nameBeforeValue.Groups[1].Value
    }

    $valueBeforeName = [regex]::Match($Html, 'value="([^"]+)"[^>]*name="_csrf"')
    if ($valueBeforeName.Success) {
        return $valueBeforeName.Groups[1].Value
    }

    return $null
}

function Test-PageContent {
    param(
        [string]$Path,
        [string]$Content
    )

    $errorMarkers = @(
        "Whitelabel Error Page",
        "There was an unexpected error",
        "Servlet.service()",
        "org.springframework"
    )

    foreach ($marker in $errorMarkers) {
        if ($Content -like "*$marker*") {
            Add-Failure "$Path returned a Spring error marker: $marker"
            return
        }
    }

    Add-Pass "$Path returned a valid page"
}

Write-Host "LSOTOTALBOUW smoke test"
Write-Host "Target: $BaseUrl"

try {
    $health = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -WebSession $session -UseBasicParsing
    $healthContent = Get-ResponseText -Response $health
    if ($health.StatusCode -eq 200 -and $healthContent -like '*"UP"*') {
        Add-Pass "/actuator/health is UP"
    } else {
        Add-Failure "/actuator/health did not return status UP"
    }
} catch {
    Add-Failure "/actuator/health request failed: $($_.Exception.Message)"
}

if (-not $SkipAuthenticatedRoutes) {
    if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Password)) {
        Add-Failure "Username and password are required for authenticated route checks. Set APP_BOOTSTRAP_ADMIN_EMAIL and APP_BOOTSTRAP_ADMIN_PASSWORD or pass -Username/-Password."
    } else {
        try {
            $loginPage = Invoke-WebRequest -Uri "$BaseUrl/login" -WebSession $session -UseBasicParsing
            $form = @{
                username = $Username
                password = $Password
            }

            $csrfToken = Get-CsrfToken -Html $loginPage.Content
            if ($csrfToken) {
                $form["_csrf"] = $csrfToken
            }

            $loginResult = Invoke-WebRequest -Uri "$BaseUrl/login" -Method Post -Body $form -WebSession $session -UseBasicParsing
            if ($loginResult.Content -like "*Credenciales incorrectas*") {
                Add-Failure "Login failed for configured smoke-test user"
            } else {
                Add-Pass "Login completed"
            }
        } catch {
            Add-Failure "Login request failed: $($_.Exception.Message)"
        }

        $authenticatedRoutes = @(
            "/dashboard",
            "/customers",
            "/projects",
            "/quotations",
            "/invoices",
            "/payments",
            "/receivables",
            "/expenses",
            "/materials",
            "/tools",
            "/suppliers",
            "/calendar",
            "/documents",
            "/work-logs",
            "/profitability",
            "/audit",
            "/users",
            "/settings/company",
            "/notifications"
        )

        foreach ($route in $authenticatedRoutes) {
            try {
                $response = Invoke-WebRequest -Uri "$BaseUrl$route" -WebSession $session -UseBasicParsing
                if ($response.StatusCode -ne 200) {
                    Add-Failure "$route returned HTTP $($response.StatusCode)"
                    continue
                }

                $responseContent = Get-ResponseText -Response $response
                if ($responseContent -like "*Acceso seguro*" -and $route -ne "/login") {
                    Add-Failure "$route redirected to login instead of loading as authenticated page"
                    continue
                }

                Test-PageContent -Path $route -Content $responseContent
            } catch {
                Add-Failure "$route request failed: $($_.Exception.Message)"
            }
        }
    }
}

if ($failedChecks.Count -gt 0) {
    Write-Host ""
    Write-Host "Smoke test failed with $($failedChecks.Count) issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Smoke test passed." -ForegroundColor Green
