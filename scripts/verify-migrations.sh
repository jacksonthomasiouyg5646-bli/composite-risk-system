#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
migration_dir="$root_dir/database/migration"

if [[ ! -d "$migration_dir" ]]; then
  echo "Missing Flyway migration directory: $migration_dir" >&2
  exit 1
fi

shopt -s nullglob
migrations=("$migration_dir"/V*__*.sql)
if (( ${#migrations[@]} == 0 )); then
  echo "No Flyway migrations found in $migration_dir" >&2
  exit 1
fi

declare -A versions=()
for migration in "${migrations[@]}"; do
  name="$(basename "$migration")"
  if [[ ! "$name" =~ ^V[0-9]+(__[A-Za-z0-9_]+)\.sql$ ]]; then
    echo "Invalid Flyway migration filename: $name" >&2
    echo "Expected format: V1__description.sql" >&2
    exit 1
  fi

  version="${name%%__*}"
  if [[ -n "${versions[$version]:-}" ]]; then
    echo "Duplicate Flyway migration version: $version ($name and ${versions[$version]})" >&2
    exit 1
  fi
  versions[$version]="$name"

  if [[ "$version" != "V1" ]] && grep -Eiq '^[[:space:]]*drop[[:space:]]+table' "$migration"; then
    echo "Destructive DROP TABLE is only allowed in V1 baseline: $name" >&2
    exit 1
  fi
done

if ! cmp -s "$root_dir/database/init.sql" "$root_dir/database/migration/V1__baseline_schema_and_seed.sql"; then
  echo "database/init.sql and Flyway V1 baseline diverged." >&2
  echo "For now, keep them identical or explicitly document the cut-over." >&2
  exit 1
fi

echo "Flyway migration checks passed (${#migrations[@]} file(s))."
