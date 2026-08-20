$ErrorActionPreference = "Stop"

$root = Split-Path $PSScriptRoot -Parent
$backend = Join-Path $root "backend"
$frontend = Join-Path $root "frontend"

mvn.cmd -f "$backend\pom.xml" -DskipTests clean package
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

docker compose -f "$root\docker-compose.yml" build
