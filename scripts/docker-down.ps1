$ErrorActionPreference = "Stop"

$root = Split-Path $PSScriptRoot -Parent
docker compose -f "$root\docker-compose.yml" down
