#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  printf 'Usage: %s <backup-file>\n' "$0" >&2
  exit 1
fi

BACKUP_FILE="$1"
if [[ ! -f "$BACKUP_FILE" ]]; then
  printf 'Backup file not found: %s\n' "$BACKUP_FILE" >&2
  exit 1
fi

required_env=(DB_HOST DB_PORT DB_NAME DB_USER DB_PASSWORD)
for name in "${required_env[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Missing required env: %s\n' "$name" >&2
    exit 1
  fi
done

if ! command -v pg_restore >/dev/null 2>&1 && [[ -z "${PG_CONTAINER_NAME:-}" ]]; then
  printf 'pg_restore is unavailable. Set PG_CONTAINER_NAME to use docker exec fallback.\n' >&2
  exit 1
fi

if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  printf 'Restore is destructive. Re-run with CONFIRM_RESTORE=YES to continue.\n' >&2
  exit 1
fi

export PGPASSWORD="$DB_PASSWORD"
if command -v pg_restore >/dev/null 2>&1; then
  pg_restore \
    --host "$DB_HOST" \
    --port "$DB_PORT" \
    --username "$DB_USER" \
    --clean \
    --if-exists \
    --no-owner \
    --dbname "$DB_NAME" \
    "$BACKUP_FILE"
else
  cat "$BACKUP_FILE" | docker exec -i -e PGPASSWORD="$DB_PASSWORD" "$PG_CONTAINER_NAME" \
    pg_restore \
    --host localhost \
    --port 5432 \
    --username "$DB_USER" \
    --clean \
    --if-exists \
    --no-owner \
    --dbname "$DB_NAME"
fi
unset PGPASSWORD

printf 'Restore completed from: %s\n' "$BACKUP_FILE"
