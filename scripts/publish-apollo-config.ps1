param(
    [string]$EnvFile = (Join-Path $env:TEMP "risk-p0-preview.env"),
    [string]$ApolloContainer = "apollo-db",
    [string]$ApolloMeta = "http://127.0.0.1:8080"
)

$ErrorActionPreference = "Stop"

$root = Split-Path $PSScriptRoot -Parent
$templatePath = Join-Path $PSScriptRoot "apollo-risk-config.sql"

if (!(Test-Path -LiteralPath $EnvFile)) {
    throw "Preview environment file does not exist: $EnvFile"
}
if (!(Test-Path -LiteralPath $templatePath)) {
    throw "Apollo SQL template does not exist: $templatePath"
}

function Read-DotEnv([string]$Path) {
    $result = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        if ($line -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') {
            continue
        }
        $value = $Matches[2].Trim()
        if ($value.Length -ge 2 -and $value.StartsWith("'") -and $value.EndsWith("'")) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $result[$Matches[1]] = $value
    }
    return $result
}

$values = Read-DotEnv $EnvFile
$values["GATEWAY_PORT"] = if ($values["GATEWAY_PORT"]) { $values["GATEWAY_PORT"] } else { "8088" }
$values["MYSQL_URL"] = "jdbc:mysql://mysql:3306/user_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$values["MYSQL_USER"] = "root"
$values["MYSQL_PASSWORD"] = $values["MYSQL_ROOT_PASSWORD"]
$values["REDIS_HOST"] = "redis"
$values["REDIS_PORT"] = "6379"
$values["REDIS_DATABASE"] = "0"
$values["ROCKETMQ_NAMESRV_ADDR"] = "rocketmq-namesrv:9876"

$requiredInputs = @(
    "MYSQL_PASSWORD",
    "REDIS_PASSWORD",
    "JWT_RSA_PRIVATE_KEY",
    "JWT_RSA_PUBLIC_KEY",
    "INTERNAL_SERVICE_KEY"
)
foreach ($key in $requiredInputs) {
    if ([string]::IsNullOrWhiteSpace([string]$values[$key])) {
        throw "Required Apollo configuration input is missing: $key"
    }
}

$template = [IO.File]::ReadAllText($templatePath, [Text.Encoding]::UTF8)
$pattern = '\$\{([A-Z][A-Z0-9_]*)(?::([^}]*))?\}'
$rendered = [Text.RegularExpressions.Regex]::Replace(
    $template,
    $pattern,
    [Text.RegularExpressions.MatchEvaluator]{
        param($match)
        $key = $match.Groups[1].Value
        if ($values.ContainsKey($key)) {
            return ([string]$values[$key]).Replace("'", "''")
        }
        if ($match.Groups[2].Success) {
            return $match.Groups[2].Value.Replace("'", "''")
        }
        throw "No value or default is available for Apollo placeholder: $key"
    }
)

if ($rendered.Contains('${')) {
    throw "Rendered Apollo SQL still contains unresolved placeholders."
}

$tempSql = Join-Path $env:TEMP "risk-p0-preview-apollo.sql"
[IO.File]::WriteAllText($tempSql, $rendered, [Text.UTF8Encoding]::new($false))

docker cp $tempSql "${ApolloContainer}:/tmp/risk-p0-preview-apollo.sql"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to copy rendered Apollo SQL into $ApolloContainer."
}

docker exec $ApolloContainer sh -ec "mysql -uroot < /tmp/risk-p0-preview-apollo.sql"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to publish risk configuration into Apollo."
}

$requiredNamespaces = [ordered]@{
    "discovery-server/application" = @("server.port", "spring.application.name", "eureka.client.register-with-eureka")
    "api-gateway/application" = @("server.port", "spring.application.name", "eureka.client.service-url.defaultZone")
    "api-gateway/gateway" = @("spring.cloud.gateway.routes[0].uri", "spring.cloud.gateway.routes[2].uri")
    "api-gateway/security" = @("app.jwt.rsa.public-key", "spring.data.redis.password", "app.session.redis-required")
    "auth-service/application" = @("server.port", "spring.application.name", "eureka.client.service-url.defaultZone")
    "auth-service/database" = @("spring.datasource.url", "spring.datasource.password")
    "auth-service/security" = @("app.jwt.rsa.private-key", "app.jwt.rsa.public-key", "spring.data.redis.password")
    "user-service/application" = @("server.port", "spring.application.name", "eureka.client.service-url.defaultZone")
    "user-service/database" = @("spring.datasource.url", "spring.datasource.password")
    "user-service/security" = @("app.jwt.rsa.public-key", "app.security.internal-service-key", "spring.data.redis.password")
    "system-service/application" = @("server.port", "spring.application.name", "eureka.client.service-url.defaultZone")
    "system-service/database" = @("spring.datasource.url", "spring.datasource.password")
    "system-service/security" = @("app.jwt.rsa.public-key", "app.security.internal-service-key", "spring.data.redis.password")
    "system-service/risk" = @("risk.system.name", "risk.high.threshold", "risk.ai.external-data.enabled")
    "system-service/mq" = @("app.mq.enabled", "app.mq.namesrv-addr", "app.mail.send-enabled")
}

foreach ($entry in $requiredNamespaces.GetEnumerator()) {
    $parts = $entry.Key.Split('/', 2)
    $appId = $parts[0]
    $namespace = $parts[1]
    $uri = "$($ApolloMeta.TrimEnd('/'))/configs/$appId/default/$namespace"
    $response = Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 15
    if (!$response.releaseKey) {
        throw "Apollo namespace is not published: $($entry.Key)"
    }
    $names = @($response.configurations.PSObject.Properties.Name)
    foreach ($requiredKey in $entry.Value) {
        if ($names -notcontains $requiredKey) {
            throw "Apollo namespace $($entry.Key) is missing required key: $requiredKey"
        }
    }
    foreach ($property in $response.configurations.PSObject.Properties) {
        if ([string]$property.Value -like '*${*') {
            throw "Apollo namespace $($entry.Key) contains an unresolved value: $($property.Name)"
        }
    }
    Write-Host "$($entry.Key): published ($($names.Count) keys)"
}

Write-Host "Apollo risk configuration publication and validation completed."
