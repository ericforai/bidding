#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT_DIR/scripts/release/rehearsal-env.sh"

cleanup() {
  bash "$ROOT_DIR/scripts/release/rehearsal-down.sh" >/dev/null 2>&1 || true
}
trap cleanup EXIT

printf '==> Starting local rehearsal environment\n'
bash "$ROOT_DIR/scripts/release/rehearsal-up.sh"

printf '\n==> Running automated UAT\n'
cd "$ROOT_DIR"
node "$ROOT_DIR/scripts/release/run-uat.mjs"
UAT_REPORT_JSON="$(ls -1t "$REPORT_DIR"/uat-report-*.json | head -n 1)"
export UAT_REPORT_JSON
export PLAYWRIGHT_BASE_URL="$UAT_WEB_BASE_URL"
printf 'UAT report: %s\n' "$UAT_REPORT_JSON"

printf '\n==> Running browser E2E gate\n'
npm run test:e2e:commercial

printf '\n==> Creating rehearsal backup\n'
export PG_CONTAINER_NAME="$POSTGRES_CONTAINER_NAME"
export BACKUP_DIR="$STATE_DIR/backups"
bash "$ROOT_DIR/scripts/release/backup-db.sh"
BACKUP_FILE="$(ls -1t "$BACKUP_DIR"/*.dump | head -n 1)"
printf 'Backup file: %s\n' "$BACKUP_FILE"

printf '\n==> Creating post-backup mutation marker\n'
RESTORE_MARKER_JSON="$(node "$ROOT_DIR/scripts/release/create-restore-marker.mjs" "$UAT_REPORT_JSON")"
printf 'Restore marker: %s\n' "$RESTORE_MARKER_JSON"

printf '\n==> Stopping backend before restore verification\n'
if [[ -f "$STATE_DIR/backend.pid" ]]; then
  kill "$(cat "$STATE_DIR/backend.pid")" >/dev/null 2>&1 || true
  rm -f "$STATE_DIR/backend.pid"
fi

printf '==> Restoring rehearsal backup\n'
CONFIRM_RESTORE=YES bash "$ROOT_DIR/scripts/release/restore-db.sh" "$BACKUP_FILE"

printf '==> Restarting backend after restore\n'
cd "$BACKEND_DIR"
nohup env \
SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
DB_PASSWORD="$DB_PASSWORD" \
DB_USERNAME="$DB_USERNAME" \
JWT_SECRET="$JWT_SECRET" \
REDIS_HOST="$REDIS_HOST" \
REDIS_PORT="$REDIS_PORT" \
CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
PLATFORM_ACCOUNT_ENCRYPTION_KEY="$PLATFORM_ACCOUNT_ENCRYPTION_KEY" \
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=${BACKEND_PORT} --spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} --spring.datasource.username=${DB_USER} --spring.datasource.password=${DB_PASSWORD} --spring.data.redis.host=${REDIS_HOST} --spring.data.redis.port=${REDIS_PORT}" \
  > "$STATE_DIR/backend.log" 2>&1 < /dev/null &
echo $! > "$STATE_DIR/backend.pid"

printf '==> Waiting for backend after restore\n'
for i in {1..120}; do
  if curl -fsS "$UAT_API_BASE_URL/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
curl -fsS "$UAT_API_BASE_URL/actuator/health" >/dev/null

printf '==> Verifying restore removed post-backup mutation\n'
POST_RESTORE_REPORT="$(node "$ROOT_DIR/scripts/release/verify-restore.mjs" "$UAT_REPORT_JSON" "$RESTORE_MARKER_JSON")"

printf '\n==> Rehearsal completed\n'
printf 'UAT and restore reports are in %s\n' "$REPORT_DIR"
