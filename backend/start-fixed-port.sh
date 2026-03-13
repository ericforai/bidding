#!/usr/bin/env bash

set -euo pipefail

PORT=18080
DB_CONTAINER_NAME="${DB_CONTAINER_NAME:-xiyu-bid-local-postgres}"
REDIS_CONTAINER_NAME="${REDIS_CONTAINER_NAME:-xiyu-bid-local-redis}"
DB_VOLUME_NAME="${DB_VOLUME_NAME:-xiyu-bid-local-pgdata}"
POSTGRES_PORT="${POSTGRES_PORT:-25432}"
REDIS_PORT="${REDIS_PORT:-26379}"
DB_NAME="${DB_NAME:-xiyu_bid}"
DB_USER="${DB_USER:-xiyu_user}"
DB_PASSWORD="${DB_PASSWORD:-XiyuDB!2026}"
JWT_SECRET="${JWT_SECRET:-xiyu-local-pg-jwt-secret-2026-with-32-chars}"
PLATFORM_ACCOUNT_ENCRYPTION_KEY="${PLATFORM_ACCOUNT_ENCRYPTION_KEY:-xiyu-platform-key-2026-local-pg}"
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:1818,http://127.0.0.1:1818,http://localhost:1314,http://127.0.0.1:1314}"
PIDS="$(lsof -ti tcp:${PORT} || true)"

if [[ -n "$PIDS" ]]; then
  echo "Killing existing process on port ${PORT}: ${PIDS}"
  kill $PIDS
  sleep 1
fi

kill_port_processes() {
  local port="$1"
  local pids
  pids="$(lsof -ti tcp:${port} || true)"
  if [[ -n "$pids" ]]; then
    echo "Killing existing process on port ${port}: ${pids}"
    kill $pids
    sleep 1
  fi
}

remove_container_if_present() {
  local name="$1"
  if docker ps -a --format '{{.Names}}' | grep -qx "$name"; then
    docker rm -f "$name" >/dev/null
  fi
}

docker volume inspect "$DB_VOLUME_NAME" >/dev/null 2>&1 || docker volume create "$DB_VOLUME_NAME" >/dev/null

remove_container_if_present "$DB_CONTAINER_NAME"
remove_container_if_present "$REDIS_CONTAINER_NAME"

docker run -d \
  --name "$DB_CONTAINER_NAME" \
  -e POSTGRES_DB="$DB_NAME" \
  -e POSTGRES_USER="$DB_USER" \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -p "${POSTGRES_PORT}:5432" \
  -v "${DB_VOLUME_NAME}:/var/lib/postgresql/data" \
  postgres:16-alpine >/dev/null

docker run -d \
  --name "$REDIS_CONTAINER_NAME" \
  -p "${REDIS_PORT}:6379" \
  redis:7-alpine >/dev/null

for i in {1..60}; do
  if docker exec "$DB_CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

docker exec "$DB_CONTAINER_NAME" sh -lc "
  HBA_FILE=\${PGDATA:-/var/lib/postgresql/data}/pg_hba.conf
  grep -q '^host[[:space:]]\\+all[[:space:]]\\+all[[:space:]]\\+0\\.0\\.0\\.0/0[[:space:]]\\+scram-sha-256$' \"\$HBA_FILE\" || \
    printf '\\nhost all all 0.0.0.0/0 scram-sha-256\\n' >> \"\$HBA_FILE\"
  grep -q '^host[[:space:]]\\+all[[:space:]]\\+all[[:space:]]\\+::/0[[:space:]]\\+scram-sha-256$' \"\$HBA_FILE\" || \
    printf 'host all all ::/0 scram-sha-256\\n' >> \"\$HBA_FILE\"
  psql -U \"$DB_USER\" -d \"$DB_NAME\" -c 'SELECT pg_reload_conf();' >/dev/null
"

for i in {1..30}; do
  if docker exec "$REDIS_CONTAINER_NAME" redis-cli ping >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

export SPRING_PROFILES_ACTIVE=local-pg
export DB_HOST=127.0.0.1
export DB_PORT="$POSTGRES_PORT"
export DB_USERNAME="$DB_USER"
export REDIS_HOST=127.0.0.1
export REDIS_PORT
export SERVER_PORT="$PORT"
export DB_PASSWORD
export JWT_SECRET
export PLATFORM_ACCOUNT_ENCRYPTION_KEY
export CORS_ALLOWED_ORIGINS

exec mvn clean spring-boot:run -Dspring-boot.run.profiles=local-pg -Dspring-boot.run.arguments="--server.port=${PORT}"
