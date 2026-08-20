$ErrorActionPreference = "Stop"

$mysqlHome = "D:\software\mysql"
$mysqlExe = Join-Path $mysqlHome "mysql-9.7.0-winx64\bin\mysql.exe"
$passwordFile = Join-Path $mysqlHome "ROOT_PASSWORD.txt"
$initSql = Join-Path (Split-Path $PSScriptRoot -Parent) "database\init.sql"

if (!(Test-Path $mysqlExe)) {
    throw "MySQL client not found: $mysqlExe"
}
if (!(Test-Path $passwordFile)) {
    throw "MySQL root password file not found: $passwordFile"
}

$password = (Get-Content $passwordFile | Where-Object { $_ -like "Password:*" } | Select-Object -First 1) -replace "^Password:\s*", ""
if ([string]::IsNullOrWhiteSpace($password)) {
    throw "Could not read MySQL root password."
}

Get-Content -Raw -Encoding UTF8 $initSql | & $mysqlExe --defaults-file="$mysqlHome\my.ini" -uroot "-p$password"
Write-Host "Database user_management initialized."
