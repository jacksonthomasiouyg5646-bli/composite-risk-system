param(
    [string]$EnvFile,
    [string]$ApolloComposeFile
)

$ErrorActionPreference = "Stop"

$root = Split-Path $PSScriptRoot -Parent
$backend = Join-Path $root "backend"
$frontend = Join-Path $root "frontend"
$mavenRepo = Join-Path $root ".m2repo"
$dockerConfig = Join-Path $root ".docker"

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $root ".env"
}
if ([string]::IsNullOrWhiteSpace($ApolloComposeFile)) {
    $softwareRoot = Split-Path (Split-Path $root -Parent) -Parent
    $ApolloComposeFile = Join-Path $softwareRoot "apollo\compose.yaml"
}
if (!(Test-Path -LiteralPath $EnvFile)) {
    throw "Docker environment file does not exist: $EnvFile"
}
if (!(Test-Path -LiteralPath $ApolloComposeFile)) {
    throw "Apollo Compose file does not exist: $ApolloComposeFile"
}

New-Item -ItemType Directory -Force -Path $dockerConfig | Out-Null
$env:DOCKER_CONFIG = $dockerConfig

docker compose -f $ApolloComposeFile -p apollo-local up -d
if ($LASTEXITCODE -ne 0) {
    throw "Apollo startup failed. Risk services were not started."
}

$apolloReady = $false
for ($attempt = 1; $attempt -le 20; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:8080/services/config" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            $apolloReady = $true
            break
        }
    } catch {
        if ($attempt -lt 20) {
            Start-Sleep -Seconds 3
        }
    }
}
if (!$apolloReady) {
    throw "Apollo ConfigService is unavailable. Risk services were not started."
}

& (Join-Path $PSScriptRoot "publish-apollo-config.ps1") -EnvFile $EnvFile
if ($LASTEXITCODE -ne 0) {
    throw "Apollo configuration publication failed. Risk services were not started."
}

& mvn.cmd "-Dmaven.repo.local=$mavenRepo" "-f" "$backend\pom.xml" "-DskipTests" "clean" "package"
if ($LASTEXITCODE -ne 0) {
    throw "Backend package failed."
}

Push-Location $frontend
try {
    npm.cmd install
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend dependency install failed."
    }
    npm.cmd run build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed."
    }
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    $javacPath = if ($javacCommand) { $javacCommand.Source } else { $null }
    if (-not $javacPath) {
        $candidate = "D:\software\maven\tools\jdk-21.0.11+10\bin\javac.exe"
        if (Test-Path $candidate) {
            $javacPath = $candidate
        }
    }
    if (-not $javacPath) {
        throw "javac.exe is required to compile the Docker frontend server."
    }
    & $javacPath --release 17 "$frontend\DockerFrontendServer.java"
    if ($LASTEXITCODE -ne 0) {
        throw "Docker frontend server compile failed."
    }
} finally {
    Pop-Location
}

docker compose --env-file $EnvFile -f "$root\docker-compose.yml" up -d --build
if ($LASTEXITCODE -ne 0) {
    throw "Risk system Docker startup failed."
}

Write-Host "Apollo-backed Docker deployment is starting."
Write-Host "Apollo Portal:      http://localhost:8070"
Write-Host "Frontend:          http://localhost:5173"
Write-Host "Gateway:           http://localhost:8088"
Write-Host "RocketMQ Console:  http://localhost:8082"
Write-Host "Prometheus:        http://localhost:9090"
Write-Host "Grafana:           http://localhost:3000"
