#!/usr/bin/env bash
# Input: database connection environment variables and backup destination arguments
# Output: release backup archive files and backup metadata
# Pos: scripts/release/ - Release automation and rehearsal helpers
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
set -euo pipefail

required_env=(DB_HOST DB_PORT DB_NAME DB_USER DB_PASSWORD)
for name in "${required_env[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Missing required env: %s\n' "$name" >&2
    exit 1
  fi
done

if ! command -v pg_dump >/dev/null 2>&1 && [[ -z "${PG_CONTAINER_NAME:-}" ]]; then
  printf 'pg_dump is unavailable. Set PG_CONTAINER_NAME to use docker exec fallback.\n' >&2
  exit 1
fi

OUTPUT_DIR="${BACKUP_DIR:-$(pwd)/backups}"
mkdir -p "$OUTPUT_DIR"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_FILE="$OUTPUT_DIR/${DB_NAME}-${TIMESTAMP}.dump"

export PGPASSWORD="$DB_PASSWORD"
if command -v pg_dump >/dev/null 2>&1; then
  pg_dump \
    --host "$DB_HOST" \
    --port "$DB_PORT" \
    --username "$DB_USER" \
    --format custom \
    --file "$OUTPUT_FILE" \
    "$DB_NAME"
else
  docker exec -e PGPASSWORD="$DB_PASSWORD" "$PG_CONTAINER_NAME" \
    pg_dump \
    --host localhost \
    --port 5432 \
    --username "$DB_USER" \
    --format custom \
    "$DB_NAME" > "$OUTPUT_FILE"
fi
unset PGPASSWORD

printf 'Backup created: %s\n' "$OUTPUT_FILE"
