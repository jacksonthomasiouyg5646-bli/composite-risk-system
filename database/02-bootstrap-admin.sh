#!/bin/sh
set -eu

: "${BOOTSTRAP_ADMIN_PASSWORD_HASH:?BOOTSTRAP_ADMIN_PASSWORD_HASH is required}"

if ! printf '%s' "$BOOTSTRAP_ADMIN_PASSWORD_HASH" | grep -Eq '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'; then
  echo "BOOTSTRAP_ADMIN_PASSWORD_HASH must be a valid 60-character BCrypt hash." >&2
  exit 1
fi

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" \
  --execute="UPDATE sys_user SET password_hash='${BOOTSTRAP_ADMIN_PASSWORD_HASH}', status='ENABLED' WHERE username='admin';"
