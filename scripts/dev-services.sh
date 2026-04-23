#!/usr/bin/env bash
# Input: local dev environment, backend profile options, and repository startup commands
# Output: stable daemon-like start/stop/status/log control for frontend/backend dev services
# Pos: scripts/ - local service lifecycle management
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.runtime/dev-services"
mkdir -p "$RUNTIME_DIR"

BACKEND_PORT="${BACKEND_PORT:-18080}"
FRONTEND_PORT="${FRONTEND_PORT:-1314}"
BACKEND_HEALTH_URL="http://127.0.0.1:${BACKEND_PORT}/actuator/health"
FRONTEND_URL="http://127.0.0.1:${FRONTEND_PORT}/"

BACKEND_PID_FILE="$RUNTIME_DIR/backend.pid"
FRONTEND_PID_FILE="$RUNTIME_DIR/frontend.pid"
BACKEND_LOG="$RUNTIME_DIR/backend.log"
FRONTEND_LOG="$RUNTIME_DIR/frontend.log"
WATCHDOG_PID_FILE="$RUNTIME_DIR/watchdog.pid"
WATCHDOG_LOG="$RUNTIME_DIR/watchdog.log"
WATCHDOG_INTERVAL_SECONDS="${WATCHDOG_INTERVAL_SECONDS:-5}"
DEFAULT_BACKEND_PROFILE="${BACKEND_PROFILE:-dev,mysql}"
BACKEND_PROFILE_OVERRIDE=""
ACTIVE_BACKEND_PROFILE=""
JWT_SECRET="${JWT_SECRET:-xiyu-bid-poc-local-dev-secret-key-please-change-in-prod-32bytes-min}"

is_valid_command() {
  case "$1" in
    start|stop|restart|status|logs|watch-start|watch-stop|watch-status|watch-run)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

parse_args() {
  local cmd_seen=""
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -p|--profile)
        if [[ $# -lt 2 ]]; then
          echo "missing value for $1" >&2
          usage
          exit 1
        fi
        BACKEND_PROFILE_OVERRIDE="$2"
        shift 2
        ;;
      --profile=*)
        BACKEND_PROFILE_OVERRIDE="${1#--profile=}"
        shift
        ;;
      *)
        if is_valid_command "$1"; then
          if [[ -n "$cmd_seen" ]]; then
            echo "multiple commands specified: $cmd_seen and $1" >&2
            usage
            exit 1
          fi
          cmd_seen="$1"
          shift
        else
          echo "unknown argument: $1" >&2
          usage
          exit 1
        fi
        ;;
    esac
  done

  if [[ -n "$BACKEND_PROFILE_OVERRIDE" ]]; then
    ACTIVE_BACKEND_PROFILE="$BACKEND_PROFILE_OVERRIDE"
  else
    ACTIVE_BACKEND_PROFILE="$DEFAULT_BACKEND_PROFILE"
  fi

  if [[ -n "$cmd_seen" ]]; then
    CMD="$cmd_seen"
  else
    CMD="status"
  fi
}

profile_args() {
  printf '%s\n%s\n' "--profile" "$ACTIVE_BACKEND_PROFILE"
}

is_pid_running() {
  local pid="$1"
  [[ -n "${pid:-}" ]] && kill -0 "$pid" >/dev/null 2>&1
}

is_port_listening() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

read_pid() {
  local file="$1"
  [[ -f "$file" ]] || return 1
  tr -d '[:space:]' <"$file"
}

wait_http() {
  local url="$1"
  local timeout="${2:-90}"
  local start
  start="$(date +%s)"
  while true; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    if (( $(date +%s) - start >= timeout )); then
      return 1
    fi
    sleep 1
  done
}

start_backend() {
  local pid=""
  pid="$(read_pid "$BACKEND_PID_FILE" 2>/dev/null || true)"
  if is_pid_running "$pid"; then
    if is_port_listening "$BACKEND_PORT"; then
      echo "[backend] already running (pid=$pid, port=$BACKEND_PORT)"
    else
      echo "[backend] process is running (pid=$pid), waiting for port $BACKEND_PORT to become ready"
    fi
    return 0
  fi

  echo "[backend] starting on :$BACKEND_PORT"
  echo "[backend] profile: $ACTIVE_BACKEND_PROFILE"
  : >"$BACKEND_LOG"
  (
    cd "$ROOT_DIR/backend"
    nohup env \
      SPRING_PROFILES_ACTIVE="$ACTIVE_BACKEND_PROFILE" \
      JWT_SECRET="$JWT_SECRET" \
      CORS_ALLOWED_ORIGINS="http://localhost:${FRONTEND_PORT},http://127.0.0.1:${FRONTEND_PORT}" \
      mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=${BACKEND_PORT}" \
      >>"$BACKEND_LOG" 2>&1 < /dev/null &
    echo $! >"$BACKEND_PID_FILE"
  )
}

start_frontend() {
  local pid=""
  pid="$(read_pid "$FRONTEND_PID_FILE" 2>/dev/null || true)"
  if is_pid_running "$pid"; then
    if is_port_listening "$FRONTEND_PORT"; then
      echo "[frontend] already running (pid=$pid, port=$FRONTEND_PORT)"
    else
      echo "[frontend] process is running (pid=$pid), waiting for port $FRONTEND_PORT to become ready"
    fi
    return 0
  fi

  echo "[frontend] starting on :$FRONTEND_PORT"
  : >"$FRONTEND_LOG"
  (
    cd "$ROOT_DIR"
    nohup env \
      VITE_API_MODE=api \
      VITE_API_BASE_URL="http://127.0.0.1:${BACKEND_PORT}" \
      npm run dev -- --host 127.0.0.1 --port "$FRONTEND_PORT" \
      >>"$FRONTEND_LOG" 2>&1 < /dev/null &
    echo $! >"$FRONTEND_PID_FILE"
  )
}

stop_one() {
  local name="$1"
  local pid_file="$2"
  local pid=""
  pid="$(read_pid "$pid_file" 2>/dev/null || true)"
  if is_pid_running "$pid"; then
    echo "[$name] stopping pid=$pid"
    kill "$pid" >/dev/null 2>&1 || true
    sleep 1
    if is_pid_running "$pid"; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
  else
    echo "[$name] no tracked pid running, trying port-based cleanup"
  fi

  if [[ "$name" == "frontend" ]]; then
    while IFS= read -r p; do kill "$p" >/dev/null 2>&1 || true; done < <(lsof -tiTCP:"$FRONTEND_PORT" -sTCP:LISTEN || true)
    sleep 1
    while IFS= read -r p; do kill -9 "$p" >/dev/null 2>&1 || true; done < <(lsof -tiTCP:"$FRONTEND_PORT" -sTCP:LISTEN || true)
  elif [[ "$name" == "backend" ]]; then
    while IFS= read -r p; do kill "$p" >/dev/null 2>&1 || true; done < <(lsof -tiTCP:"$BACKEND_PORT" -sTCP:LISTEN || true)
    sleep 1
    while IFS= read -r p; do kill -9 "$p" >/dev/null 2>&1 || true; done < <(lsof -tiTCP:"$BACKEND_PORT" -sTCP:LISTEN || true)
  fi

  rm -f "$pid_file"
}

status() {
  local bpid fpid
  bpid="$(read_pid "$BACKEND_PID_FILE" 2>/dev/null || true)"
  fpid="$(read_pid "$FRONTEND_PID_FILE" 2>/dev/null || true)"
  local bstate="down" fstate="down"
  local bhttp="down" fhttp="down"

  if is_port_listening "$BACKEND_PORT"; then
    if is_pid_running "$bpid"; then
      bstate="up(pid=$bpid)"
    else
      bstate="up(port=$BACKEND_PORT)"
    fi
  fi
  if is_port_listening "$FRONTEND_PORT"; then
    if is_pid_running "$fpid"; then
      fstate="up(pid=$fpid)"
    else
      fstate="up(port=$FRONTEND_PORT)"
    fi
  fi
  if curl -fsS "$BACKEND_HEALTH_URL" >/dev/null 2>&1; then
    bhttp="ok"
  fi
  if curl -fsS "$FRONTEND_URL" >/dev/null 2>&1; then
    fhttp="ok"
  fi

  echo "backend: $bstate http=$bhttp url=$BACKEND_HEALTH_URL"
  echo "frontend: $fstate http=$fhttp url=$FRONTEND_URL"
}

logs() {
  echo "=== backend ($BACKEND_LOG) ==="
  tail -n 80 "$BACKEND_LOG" 2>/dev/null || true
  echo
  echo "=== frontend ($FRONTEND_LOG) ==="
  tail -n 80 "$FRONTEND_LOG" 2>/dev/null || true
}

watchdog_pid() {
  read_pid "$WATCHDOG_PID_FILE" 2>/dev/null || true
}

watchdog_running() {
  local pid
  pid="$(watchdog_pid)"
  is_pid_running "$pid"
}

watchdog_loop() {
  echo "[$(date '+%F %T')] watchdog loop started interval=${WATCHDOG_INTERVAL_SECONDS}s"
  while true; do
    if ! curl -fsS "$BACKEND_HEALTH_URL" >/dev/null 2>&1; then
      echo "[$(date '+%F %T')] backend unhealthy, attempting restart"
      start_backend
      wait_http "$BACKEND_HEALTH_URL" 120 >/dev/null 2>&1 || true
    fi

    if ! curl -fsS "$FRONTEND_URL" >/dev/null 2>&1; then
      echo "[$(date '+%F %T')] frontend unhealthy, attempting restart"
      start_frontend
      wait_http "$FRONTEND_URL" 60 >/dev/null 2>&1 || true
    fi

    sleep "$WATCHDOG_INTERVAL_SECONDS"
  done
}

watchdog_start() {
  if watchdog_running; then
    echo "[watchdog] already running (pid=$(watchdog_pid))"
    return 0
  fi

  : >"$WATCHDOG_LOG"
  nohup bash -lc "cd \"$ROOT_DIR\" && \"$0\" --profile \"$ACTIVE_BACKEND_PROFILE\" watch-run" >>"$WATCHDOG_LOG" 2>&1 < /dev/null &
  echo $! >"$WATCHDOG_PID_FILE"
  sleep 1
  if watchdog_running; then
    echo "[watchdog] started pid=$(watchdog_pid), interval=${WATCHDOG_INTERVAL_SECONDS}s, profile=${ACTIVE_BACKEND_PROFILE}"
  else
    echo "[watchdog] failed to start"
    tail -n 80 "$WATCHDOG_LOG" 2>/dev/null || true
    exit 1
  fi
}

watchdog_stop() {
  local pid
  pid="$(watchdog_pid)"
  if is_pid_running "$pid"; then
    echo "[watchdog] stopping pid=$pid"
    kill "$pid" >/dev/null 2>&1 || true
    sleep 1
    if is_pid_running "$pid"; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
  else
    echo "[watchdog] not running"
  fi
  rm -f "$WATCHDOG_PID_FILE"
}

watchdog_status() {
  local state="down"
  if watchdog_running; then
    state="up(pid=$(watchdog_pid), interval=${WATCHDOG_INTERVAL_SECONDS}s)"
  fi
  echo "watchdog: $state"
}

usage() {
  cat <<'EOF'
Usage: scripts/dev-services.sh [--profile <spring_profiles>] <start|stop|restart|status|logs|watch-start|watch-stop|watch-status>

Examples:
  scripts/dev-services.sh start
  scripts/dev-services.sh --profile e2e restart
  scripts/dev-services.sh --profile dev,mysql watch-start
EOF
}

CMD=""
parse_args "$@"

case "$CMD" in
  start)
    start_backend
    if ! wait_http "$BACKEND_HEALTH_URL" 120; then
      echo "[backend] failed to become healthy. See logs:"
      logs
      exit 1
    fi
    start_frontend
    if ! wait_http "$FRONTEND_URL" 60; then
      echo "[frontend] failed to become healthy. See logs:"
      logs
      exit 1
    fi
    echo "services started successfully."
    echo "backend profile: $ACTIVE_BACKEND_PROFILE"
    status
    ;;
  stop)
    watchdog_stop
    stop_one "frontend" "$FRONTEND_PID_FILE"
    stop_one "backend" "$BACKEND_PID_FILE"
    ;;
  restart)
    "$0" --profile "$ACTIVE_BACKEND_PROFILE" stop
    "$0" --profile "$ACTIVE_BACKEND_PROFILE" start
    ;;
  status)
    status
    watchdog_status
    ;;
  logs)
    logs
    echo
    echo "=== watchdog ($WATCHDOG_LOG) ==="
    tail -n 80 "$WATCHDOG_LOG" 2>/dev/null || true
    ;;
  watch-start)
    watchdog_start
    ;;
  watch-stop)
    watchdog_stop
    ;;
  watch-status)
    watchdog_status
    ;;
  watch-run)
    watchdog_loop
    ;;
  *)
    usage
    exit 1
    ;;
esac
