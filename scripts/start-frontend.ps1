$ErrorActionPreference = "Stop"

$frontend = Join-Path (Split-Path $PSScriptRoot -Parent) "frontend"
Set-Location $frontend

if (!(Test-Path "node_modules")) {
    npm.cmd install --cache "$env:TEMP\user-management-npm-cache"
}

npm.cmd run dev -- --host localhost
