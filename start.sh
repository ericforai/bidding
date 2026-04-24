#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
DEV_LOG="/tmp/xiyu-bid-poc-dev.log"
BACKEND_PORT="${BACKEND_PORT:-18080}"
FRONTEND_PORT="${FRONTEND_PORT:-1314}"
BACKEND_HEALTH_URL="http://127.0.0.1:${BACKEND_PORT}/actuator/health"
FRONTEND_URL="http://127.0.0.1:${FRONTEND_PORT}/"
CORS_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:1314,http://127.0.0.1:1314}"
FRONTEND_HEALTH_SCRIPT="$ROOT_DIR/scripts/dev-frontend-health.sh"
BACKEND_PROFILES="${SPRING_PROFILES_ACTIVE:-dev,mysql}"
JWT_SECRET="${JWT_SECRET:-xiyu-bid-poc-local-dev-secret-key-please-change-in-prod-32bytes-min}"
DB_USERNAME="${DB_USERNAME:-xiyu_user}"
DB_PASSWORD="${DB_PASSWORD:-XiyuDB!2026}"
BACKEND_PID=""
FRONTEND_PID=""
STARTED_ANYTHING=false
FRONTEND_ALREADY_UP=false

is_http_ready() {
  curl -fsS "$1" >/dev/null 2>&1
}

is_expected_frontend() {
  ROOT_DIR="$ROOT_DIR" \
    FRONTEND_URL="$FRONTEND_URL" \
    FRONTEND_PORT="$FRONTEND_PORT" \
    BACKEND_PORT="$BACKEND_PORT" \
    "$FRONTEND_HEALTH_SCRIPT" >/dev/null 2>&1
}

print_frontend_mismatch() {
  ROOT_DIR="$ROOT_DIR" \
    FRONTEND_URL="$FRONTEND_URL" \
    FRONTEND_PORT="$FRONTEND_PORT" \
    BACKEND_PORT="$BACKEND_PORT" \
    "$FRONTEND_HEALTH_SCRIPT" >&2 || true
}

wait_for_http() {
  local url="$1"
  local label="$2"

  for _ in {1..60}; do
    if is_http_ready "$url"; then
      return 0
    fi
    sleep 2
  done

  printf '%s did not become ready in time.\n' "$label" >&2
  if [[ -f "$DEV_LOG" ]]; then
    printf -- '--- %s log tail ---\n' "$label" >&2
    tail -n 80 "$DEV_LOG" >&2 || true
  fi
  return 1
}

cleanup() {
  for pid in "$FRONTEND_PID" "$BACKEND_PID"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
    fi
  done
}

trap cleanup EXIT INT TERM
: > "$DEV_LOG"

printf 'Dev log: %s\n' "$DEV_LOG"

if is_http_ready "$BACKEND_HEALTH_URL"; then
  printf '[backend] already healthy at %s\n' "$BACKEND_HEALTH_URL"
else
  printf '[backend] starting on %s\n' "$BACKEND_PORT"
  printf '[backend] profile: %s\n' "$BACKEND_PROFILES"
  printf '[backend] database: MySQL 8.0 at %s:%s/%s\n' "${DB_HOST:-localhost}" "${DB_PORT:-3306}" "${DB_NAME:-xiyu_bid}"
  pushd "$BACKEND_DIR" >/dev/null
  env \
    SPRING_PROFILES_ACTIVE="$BACKEND_PROFILES" \
    JWT_SECRET="$JWT_SECRET" \
    DB_USERNAME="$DB_USERNAME" \
    DB_PASSWORD="$DB_PASSWORD" \
    CORS_ALLOWED_ORIGINS="$CORS_ORIGINS" \
    mvn clean spring-boot:run -Dspring-boot.run.arguments="--server.port=${BACKEND_PORT}" \
    >> "$DEV_LOG" 2>&1 &
  BACKEND_PID=$!
  popd >/dev/null
  STARTED_ANYTHING=true
fi

if is_http_ready "$FRONTEND_URL"; then
  if is_expected_frontend; then
    printf '[frontend] already healthy at %s\n' "$FRONTEND_URL"
    FRONTEND_ALREADY_UP=true
  else
    printf '[frontend] port %s is reachable but not serving this workspace in API mode.\n' "$FRONTEND_PORT" >&2
    print_frontend_mismatch
    printf 'Stop the conflicting service or run: npm run dev:stable:stop\n' >&2
    exit 1
  fi
else
  printf '[frontend] starting on %s\n' "$FRONTEND_PORT"
  STARTED_ANYTHING=true
fi

if [[ "$STARTED_ANYTHING" == false ]]; then
  printf 'Both services are already up.\n'
  printf 'Frontend: %s\n' "$FRONTEND_URL"
  printf 'Backend: %s\n' "http://127.0.0.1:${BACKEND_PORT}/actuator/health"
  exit 0
fi

wait_for_http "$BACKEND_HEALTH_URL" "Backend"

if [[ "$FRONTEND_ALREADY_UP" == true ]]; then
  if [[ "$STARTED_ANYTHING" == true ]]; then
    printf '\nReady.\n'
    printf 'Frontend: %s\n' "$FRONTEND_URL"
    printf 'Backend: %s\n' "http://127.0.0.1:${BACKEND_PORT}/actuator/health"
    printf 'Press Ctrl-C to stop both services.\n\n'
    tail -f "$DEV_LOG"
  else
    printf 'Both services are already up.\n'
    printf 'Frontend: %s\n' "$FRONTEND_URL"
    printf 'Backend: %s\n' "http://127.0.0.1:${BACKEND_PORT}/actuator/health"
  fi
  exit 0
fi

pushd "$ROOT_DIR" >/dev/null
env \
  VITE_API_MODE=api \
  VITE_API_BASE_URL="http://127.0.0.1:${BACKEND_PORT}" \
  npm run dev -- --host 127.0.0.1 --port "$FRONTEND_PORT" \
  >> "$DEV_LOG" 2>&1
popd >/dev/null
