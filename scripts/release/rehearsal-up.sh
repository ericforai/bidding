#!/usr/bin/env bash
# Input: rehearsal environment variables and database container configuration
# Output: running rehearsal database services and pid/state files
# Pos: scripts/release/ - Release automation and rehearsal helpers
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT_DIR/scripts/release/rehearsal-env.sh"

printf '==> Starting PostgreSQL container %s on %s\n' "$POSTGRES_CONTAINER_NAME" "$POSTGRES_PORT"
docker rm -f "$POSTGRES_CONTAINER_NAME" >/dev/null 2>&1 || true
docker run -d \
  --name "$POSTGRES_CONTAINER_NAME" \
  -e POSTGRES_DB="$DB_NAME" \
  -e POSTGRES_USER="$DB_USER" \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -p "${POSTGRES_PORT}:5432" \
  postgres:16-alpine >/dev/null

printf '==> Starting Redis container %s on %s\n' "$REDIS_CONTAINER_NAME" "$REDIS_PORT"
docker rm -f "$REDIS_CONTAINER_NAME" >/dev/null 2>&1 || true
docker run -d \
  --name "$REDIS_CONTAINER_NAME" \
  -p "${REDIS_PORT}:6379" \
  redis:7-alpine >/dev/null

printf '==> Waiting for PostgreSQL\n'
for i in {1..60}; do
  if docker exec "$POSTGRES_CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
docker exec "$POSTGRES_CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null

printf '==> Waiting for Redis\n'
for i in {1..60}; do
  if docker exec "$REDIS_CONTAINER_NAME" redis-cli ping >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
docker exec "$REDIS_CONTAINER_NAME" redis-cli ping >/dev/null

printf '==> Building frontend assets\n'
cd "$ROOT_DIR"
VITE_API_MODE=api VITE_API_BASE_URL="$UAT_API_BASE_URL" npm run build >/dev/null

printf '==> Starting backend on %s\n' "$BACKEND_PORT"
cd "$BACKEND_DIR"
nohup env \
SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
DB_PASSWORD="$DB_PASSWORD" \
DB_USERNAME="$DB_USERNAME" \
JWT_SECRET="$JWT_SECRET" \
REDIS_HOST="$REDIS_HOST" \
REDIS_PORT="$REDIS_PORT" \
CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
PLATFORM_ENCRYPTION_KEY="$PLATFORM_ENCRYPTION_KEY" \
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=${BACKEND_PORT} --spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} --spring.datasource.username=${DB_USER} --spring.datasource.password=${DB_PASSWORD} --spring.data.redis.host=${REDIS_HOST} --spring.data.redis.port=${REDIS_PORT}" \
  > "$STATE_DIR/backend.log" 2>&1 < /dev/null &
echo $! > "$STATE_DIR/backend.pid"

printf '==> Waiting for backend health\n'
for i in {1..120}; do
  if curl -fsS "$UAT_API_BASE_URL/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
curl -fsS "$UAT_API_BASE_URL/actuator/health" >/dev/null

printf '==> Seeding default users when database is empty\n'
USER_COUNT="$(docker exec "$POSTGRES_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c 'select count(*) from users;' | tr -d '[:space:]')"
if [[ "${USER_COUNT:-0}" == "0" ]]; then
  seed_user() {
    local username="$1"
    local full_name="$2"
    local email="$3"
    local role="$4"

    curl -fsS -X POST "$UAT_API_BASE_URL/api/auth/register" \
      -H 'Content-Type: application/json' \
      -d "$(cat <<EOF
{"username":"$username","password":"XiyuDemo!2026","email":"$email","fullName":"$full_name","role":"$role"}
EOF
)" >/dev/null
  }

  seed_user "xiaowang" "小王" "xiaowang@example.com" "STAFF"
  seed_user "zhangjingli" "张经理" "zhang.manager@example.com" "MANAGER"
  seed_user "lizong" "李总" "li.admin@example.com" "ADMIN"
  seed_user "ligong" "李工" "li.engineer@example.com" "STAFF"
fi

printf '==> Starting frontend preview on %s\n' "$FRONTEND_PORT"
cd "$ROOT_DIR"
nohup npm run preview -- --host 127.0.0.1 --port "$FRONTEND_PORT" > "$STATE_DIR/frontend.log" 2>&1 < /dev/null &
echo $! > "$STATE_DIR/frontend.pid"

printf '==> Waiting for frontend preview\n'
for i in {1..60}; do
  if curl -fsS "$UAT_WEB_BASE_URL" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
curl -fsS "$UAT_WEB_BASE_URL" >/dev/null

printf '==> Rehearsal stack is ready\n'
printf 'API: %s\n' "$UAT_API_BASE_URL"
printf 'WEB: %s\n' "$UAT_WEB_BASE_URL"
