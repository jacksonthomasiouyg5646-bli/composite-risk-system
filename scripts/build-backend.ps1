$ErrorActionPreference = "Stop"

$backend = Join-Path (Split-Path $PSScriptRoot -Parent) "backend"
Set-Location $backend

$env:JAVA_TOOL_OPTIONS = "--enable-native-access=ALL-UNNAMED"
mvn.cmd -DskipTests clean package
if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed."
}
Write-Host "Backend build completed."
