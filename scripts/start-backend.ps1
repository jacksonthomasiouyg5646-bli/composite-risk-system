$ErrorActionPreference = "Stop"

$projectRoot = Split-Path $PSScriptRoot -Parent
$backend = Join-Path $projectRoot "backend"
$ports = 8761, 8088, 9001, 9002, 9003
$logs = Join-Path $env:TEMP "user-management-distributed-logs"

function Require-EnvironmentValue {
    param([string]$Name)
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Required environment variable $Name is not set. Copy .env.example and inject secrets before startup."
    }
}

if ([string]::IsNullOrWhiteSpace($env:MYSQL_PASSWORD) -and $env:MYSQL_PASSWORD_FILE) {
    if (!(Test-Path -LiteralPath $env:MYSQL_PASSWORD_FILE)) {
        throw "MYSQL_PASSWORD_FILE does not exist."
    }
    $env:MYSQL_PASSWORD = (Get-Content -LiteralPath $env:MYSQL_PASSWORD_FILE -Raw).Trim()
}

@(
    "MYSQL_PASSWORD",
    "REDIS_PASSWORD",
    "JWT_RSA_PRIVATE_KEY",
    "JWT_RSA_PUBLIC_KEY",
    "INTERNAL_SERVICE_KEY"
) | ForEach-Object { Require-EnvironmentValue $_ }

$env:MYSQL_USER = if ($env:MYSQL_USER) { $env:MYSQL_USER } else { "root" }
$env:MYSQL_URL = if ($env:MYSQL_URL) { $env:MYSQL_URL } else { "jdbc:mysql://localhost:3306/user_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false" }
$env:APOLLO_ENABLED = if ($env:APOLLO_ENABLED) { $env:APOLLO_ENABLED } else { "false" }
$env:APOLLO_META = if ($env:APOLLO_META) { $env:APOLLO_META } else { "http://localhost:8080" }
$env:GATEWAY_PORT = if ($env:GATEWAY_PORT) { $env:GATEWAY_PORT } else { "8088" }
$env:REDIS_HOST = if ($env:REDIS_HOST) { $env:REDIS_HOST } else { "localhost" }
$env:REDIS_PORT = if ($env:REDIS_PORT) { $env:REDIS_PORT } else { "6379" }
$env:REDIS_DATABASE = if ($env:REDIS_DATABASE) { $env:REDIS_DATABASE } else { "0" }
$env:SESSION_TTL_SECONDS = if ($env:SESSION_TTL_SECONDS) { $env:SESSION_TTL_SECONDS } else { "900" }
$env:MQ_ENABLED = if ($env:MQ_ENABLED) { $env:MQ_ENABLED } else { "true" }
$env:ROCKETMQ_NAMESRV_ADDR = if ($env:ROCKETMQ_NAMESRV_ADDR) { $env:ROCKETMQ_NAMESRV_ADDR } else { "127.0.0.1:9876" }
$env:MAIL_MQ_TOPIC = if ($env:MAIL_MQ_TOPIC) { $env:MAIL_MQ_TOPIC } else { "risk-mail-topic" }
$env:MAIL_MQ_TAG = if ($env:MAIL_MQ_TAG) { $env:MAIL_MQ_TAG } else { "mail-send" }
$env:MAIL_MQ_PRODUCER_GROUP = if ($env:MAIL_MQ_PRODUCER_GROUP) { $env:MAIL_MQ_PRODUCER_GROUP } else { "risk-mail-producer-group" }
$env:MAIL_MQ_CONSUMER_GROUP = if ($env:MAIL_MQ_CONSUMER_GROUP) { $env:MAIL_MQ_CONSUMER_GROUP } else { "risk-mail-consumer-group" }
$env:MAIL_MQ_CONSUME_ENABLED = if ($env:MAIL_MQ_CONSUME_ENABLED) { $env:MAIL_MQ_CONSUME_ENABLED } else { "false" }
$env:MAIL_HOST = if ($env:MAIL_HOST) { $env:MAIL_HOST } else { "localhost" }
$env:MAIL_PORT = if ($env:MAIL_PORT) { $env:MAIL_PORT } else { "25" }
$env:MAIL_USERNAME = if ($env:MAIL_USERNAME) { $env:MAIL_USERNAME } else { "" }
$env:MAIL_PASSWORD = if ($env:MAIL_PASSWORD) { $env:MAIL_PASSWORD } else { "" }
$env:MAIL_FROM = if ($env:MAIL_FROM) { $env:MAIL_FROM } else { "no-reply@risk.local" }
$env:JAVA_TOOL_OPTIONS = "--enable-native-access=ALL-UNNAMED"

foreach ($port in $ports) {
    Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object {
            Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        }
}

Start-Sleep -Seconds 2
New-Item -ItemType Directory -Force -Path $logs | Out-Null

mvn.cmd -f "$backend\pom.xml" clean install
if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed. Stop running Java services and rerun scripts\start-backend.cmd from a normal terminal."
}

function Start-ServiceJar {
    param(
        [string]$Name,
        [string]$Jar,
        [int]$DelaySeconds = 0
    )

    $jarPath = Join-Path $backend $Jar
    if (!(Test-Path -LiteralPath $jarPath)) {
        throw "Jar not found: $jarPath"
    }

    Start-Process java.exe `
        -WindowStyle Hidden `
        -WorkingDirectory $backend `
        -ArgumentList "-jar", $jarPath `
        -RedirectStandardOutput (Join-Path $logs "$Name.log") `
        -RedirectStandardError (Join-Path $logs "$Name.err.log")

    if ($DelaySeconds -gt 0) {
        Start-Sleep -Seconds $DelaySeconds
    }
}

Start-ServiceJar "discovery-server" "discovery-server\target\discovery-server-1.0.0.jar" 8
Start-ServiceJar "auth-service" "auth-service\target\auth-service-1.0.0.jar"
Remove-Item Env:JWT_RSA_PRIVATE_KEY
Start-ServiceJar "api-gateway" "api-gateway\target\api-gateway-1.0.0.jar"
Start-ServiceJar "user-service" "user-service\target\user-service-1.0.0.jar"
Start-ServiceJar "system-service" "system-service\target\system-service-1.0.0.jar"

Write-Host "Backend services are starting."
Write-Host "Gateway: http://localhost:8088"
Write-Host "Eureka:  http://localhost:8761"
