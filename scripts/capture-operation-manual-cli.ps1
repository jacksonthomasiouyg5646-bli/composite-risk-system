$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$browser = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$root = Split-Path -Parent $PSScriptRoot
$profile = Join-Path $root ".operation-manual-chrome-profile"
$output = Join-Path $root "docs\images\operation-manual"

if (-not (Test-Path -LiteralPath $browser)) {
    throw "Chrome was not found: $browser"
}

New-Item -ItemType Directory -Path $output -Force | Out-Null

$pages = @(
    @{ Name = "03-credit-query.png"; Path = "/risks/credit-domain-query" },
    @{ Name = "04-risk-ledger.png"; Path = "/risks/ledgers" },
    @{ Name = "05-default-trends.png"; Path = "/risks/default-trends" },
    @{ Name = "06-lgd-overview.png"; Path = "/risks/lgd-center" },
    @{ Name = "08-portfolio-limits.png"; Path = "/risks/portfolio-management" },
    @{ Name = "11-alert-cases.png"; Path = "/risks/alert-cases" },
    @{ Name = "12-ai-analysis.png"; Path = "/risks/ai-assistant" },
    @{ Name = "13-data-governance.png"; Path = "/risks/data-governance" },
    @{ Name = "14-model-governance.png"; Path = "/risks/model-governance" },
    @{ Name = "15-relationship-graph.png"; Path = "/risks/relationship-graph" },
    @{ Name = "16-management-reports.png"; Path = "/risks/management-reports" },
    @{ Name = "17-scoring-rules.png"; Path = "/risks/scoring-rules" },
    @{ Name = "18-alert-subscriptions.png"; Path = "/risks/alert-subscriptions" },
    @{ Name = "19-model-monitoring.png"; Path = "/risks/model-monitoring" }
)

$pageIndex = 0
foreach ($page in $pages) {
    $pageIndex++
    $target = Join-Path $output $page.Name
    $url = "http://localhost:5173$($page.Path)"
    $pageProfile = Join-Path $root (".operation-manual-shot-profile-" + $pageIndex)
    Remove-Item -LiteralPath $pageProfile -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item -LiteralPath $profile -Destination $pageProfile -Recurse -Force
    $arguments = @(
        "--headless=new",
        "--disable-gpu",
        "--hide-scrollbars",
        "--no-first-run",
        "--no-default-browser-check",
        "--window-size=1440,1000",
        "--virtual-time-budget=7000",
        "--user-data-dir=$pageProfile",
        "--screenshot=$target",
        $url
    )
    & $browser $arguments
    $browserExitCode = $LASTEXITCODE
    for ($attempt = 0; $attempt -lt 30 -and -not (Test-Path -LiteralPath $target); $attempt++) {
        Start-Sleep -Milliseconds 500
    }
    if (-not (Test-Path -LiteralPath $target)) {
        throw "Screenshot was not created: $target (Chrome exit code $browserExitCode)"
    }
    Start-Sleep -Seconds 2
    Write-Host "Captured $($page.Name)"
    $global:LASTEXITCODE = 0
}
