$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $PSScriptRoot
$migrationDir = Join-Path $rootDir 'database\migration'

if (!(Test-Path -LiteralPath $migrationDir -PathType Container)) {
    throw "Missing Flyway migration directory: $migrationDir"
}

$migrations = Get-ChildItem -LiteralPath $migrationDir -Filter 'V*__*.sql' -File
if ($migrations.Count -eq 0) {
    throw "No Flyway migrations found in $migrationDir"
}

$versions = @{}
foreach ($migration in $migrations) {
    $name = $migration.Name
    if ($name -notmatch '^V[0-9]+(__[A-Za-z0-9_]+)\.sql$') {
        throw "Invalid Flyway migration filename: $name. Expected format: V1__description.sql"
    }

    $version = ($name -split '__', 2)[0]
    if ($versions.ContainsKey($version)) {
        throw "Duplicate Flyway migration version: $version ($name and $($versions[$version]))"
    }
    $versions[$version] = $name

    if ($version -ne 'V1') {
        $hasDropTable = Select-String -LiteralPath $migration.FullName -Pattern '^\s*drop\s+table' -CaseSensitive:$false -Quiet
        if ($hasDropTable) {
            throw "Destructive DROP TABLE is only allowed in V1 baseline: $name"
        }
    }
}

$initSql = Join-Path $rootDir 'database\init.sql'
$baselineSql = Join-Path $migrationDir 'V1__baseline_schema_and_seed.sql'
$initHash = (Get-FileHash -LiteralPath $initSql -Algorithm SHA256).Hash
$baselineHash = (Get-FileHash -LiteralPath $baselineSql -Algorithm SHA256).Hash
if ($initHash -ne $baselineHash) {
    throw "database/init.sql and Flyway V1 baseline diverged. Keep them identical or explicitly document the cut-over."
}

"Flyway migration checks passed ($($migrations.Count) file(s))."
