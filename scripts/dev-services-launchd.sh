#!/usr/bin/env bash
# Input: launchd command argument, optional runtime environment variables
# Output: launchd-managed dev service lifecycle actions and status logs
# Pos: scripts/ - macOS launchd wrapper for dev-services watchdog
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.runtime/dev-services"
mkdir -p "$RUNTIME_DIR"

BACKEND_PROFILE="${BACKEND_PROFILE:-dev,mysql}"
WATCHDOG_INTERVAL_SECONDS="${WATCHDOG_INTERVAL_SECONDS:-5}"
LAUNCHD_LABEL="${LAUNCHD_LABEL:-com.xiyu.bid.dev-services}"
LAUNCHD_DOMAIN="gui/$(id -u)"
LAUNCHD_PATH="${LAUNCHD_PATH:-/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin}"
JWT_SECRET="${JWT_SECRET:-xiyu-bid-poc-local-dev-secret-key-please-change-in-prod-32bytes-min}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-xiyu_bid}"
DB_USERNAME="${DB_USERNAME:-xiyu_user}"
DB_PASSWORD="${DB_PASSWORD:-XiyuDB!2026}"
PLIST_PATH="${HOME}/Library/LaunchAgents/${LAUNCHD_LABEL}.plist"
LAUNCHD_STDOUT_LOG="$RUNTIME_DIR/launchd.out.log"
LAUNCHD_STDERR_LOG="$RUNTIME_DIR/launchd.err.log"
BACKEND_PORT="${BACKEND_PORT:-18080}"
FRONTEND_PORT="${FRONTEND_PORT:-1314}"
FRONTEND_HEALTH_SCRIPT="$ROOT_DIR/scripts/dev-frontend-health.sh"

usage() {
  cat <<'EOF'
Usage: scripts/dev-services-launchd.sh <install|start|stop|restart|status|logs|uninstall>

Environment variables:
  BACKEND_PROFILE            Spring profile for backend (default: dev,mysql)
  WATCHDOG_INTERVAL_SECONDS  Watchdog interval seconds (default: 5)
  LAUNCHD_LABEL              launchd service label (default: com.xiyu.bid.dev-services)
  LAUNCHD_PATH               PATH for launchd process (default includes Homebrew)
  JWT_SECRET                 JWT secret passed to backend process
  DB_HOST/DB_PORT/DB_NAME    MySQL connection target
  DB_USERNAME/DB_PASSWORD    MySQL credentials
EOF
}

service_target() {
  printf "%s/%s" "$LAUNCHD_DOMAIN" "$LAUNCHD_LABEL"
}

is_loaded() {
  launchctl print "$(service_target)" >/dev/null 2>&1
}

write_plist() {
  mkdir -p "$(dirname "$PLIST_PATH")"
  cat >"$PLIST_PATH" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>${LAUNCHD_LABEL}</string>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>WorkingDirectory</key>
  <string>${ROOT_DIR}</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/bash</string>
    <string>-lc</string>
    <string>cd "${ROOT_DIR}" &amp;&amp; exec "${ROOT_DIR}/scripts/dev-services.sh" --profile "${BACKEND_PROFILE}" watch-run</string>
  </array>
  <key>EnvironmentVariables</key>
  <dict>
    <key>PATH</key>
    <string>${LAUNCHD_PATH}</string>
    <key>BACKEND_PROFILE</key>
    <string>${BACKEND_PROFILE}</string>
    <key>WATCHDOG_INTERVAL_SECONDS</key>
    <string>${WATCHDOG_INTERVAL_SECONDS}</string>
    <key>JWT_SECRET</key>
    <string>${JWT_SECRET}</string>
    <key>DB_HOST</key>
    <string>${DB_HOST}</string>
    <key>DB_PORT</key>
    <string>${DB_PORT}</string>
    <key>DB_NAME</key>
    <string>${DB_NAME}</string>
    <key>DB_USERNAME</key>
    <string>${DB_USERNAME}</string>
    <key>DB_PASSWORD</key>
    <string>${DB_PASSWORD}</string>
    <key>BACKEND_PORT</key>
    <string>${BACKEND_PORT}</string>
    <key>FRONTEND_PORT</key>
    <string>${FRONTEND_PORT}</string>
  </dict>
  <key>StandardOutPath</key>
  <string>${LAUNCHD_STDOUT_LOG}</string>
  <key>StandardErrorPath</key>
  <string>${LAUNCHD_STDERR_LOG}</string>
</dict>
</plist>
EOF
}

install_service() {
  write_plist
  if is_loaded; then
    launchctl bootout "$(service_target)" >/dev/null 2>&1 || true
  fi
  launchctl bootstrap "$LAUNCHD_DOMAIN" "$PLIST_PATH"
  launchctl kickstart -k "$(service_target)"
  echo "installed and started: $(service_target)"
  echo "plist: $PLIST_PATH"
}

start_service() {
  if [[ ! -f "$PLIST_PATH" ]]; then
    echo "plist not found: $PLIST_PATH" >&2
    echo "run: scripts/dev-services-launchd.sh install" >&2
    exit 1
  fi
  if ! is_loaded; then
    launchctl bootstrap "$LAUNCHD_DOMAIN" "$PLIST_PATH"
  fi
  launchctl kickstart -k "$(service_target)"
  echo "started: $(service_target)"
}

stop_service() {
  if is_loaded; then
    launchctl bootout "$(service_target)"
    echo "stopped: $(service_target)"
  else
    echo "already stopped: $(service_target)"
  fi
}

status_service() {
  local bstate="down"
  local fstate="down"
  local bhttp="down"
  local fhttp="down"

  if is_loaded; then
    echo "launchd: up ($(service_target))"
  else
    echo "launchd: down ($(service_target))"
  fi

  if lsof -nP -iTCP:"$BACKEND_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    bstate="up(port=$BACKEND_PORT)"
  fi
  if lsof -nP -iTCP:"$FRONTEND_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    fstate="up(port=$FRONTEND_PORT)"
  fi
  if curl -fsS "http://127.0.0.1:${BACKEND_PORT}/actuator/health" >/dev/null 2>&1; then
    bhttp="ok"
  fi
  if ROOT_DIR="$ROOT_DIR" FRONTEND_URL="http://127.0.0.1:${FRONTEND_PORT}/" BACKEND_PORT="$BACKEND_PORT" "$FRONTEND_HEALTH_SCRIPT" >/dev/null 2>&1; then
    fhttp="ok"
  fi

  echo "backend: $bstate http=$bhttp url=http://127.0.0.1:${BACKEND_PORT}/actuator/health"
  echo "frontend: $fstate http=$fhttp url=http://127.0.0.1:${FRONTEND_PORT}/"
}

logs_service() {
  echo "=== launchd stdout ($LAUNCHD_STDOUT_LOG) ==="
  tail -n 80 "$LAUNCHD_STDOUT_LOG" 2>/dev/null || true
  echo
  echo "=== launchd stderr ($LAUNCHD_STDERR_LOG) ==="
  tail -n 80 "$LAUNCHD_STDERR_LOG" 2>/dev/null || true
}

uninstall_service() {
  if is_loaded; then
    launchctl bootout "$(service_target)" >/dev/null 2>&1 || true
  fi
  rm -f "$PLIST_PATH"
  echo "uninstalled: $(service_target)"
}

CMD="${1:-status}"
case "$CMD" in
  install)
    install_service
    ;;
  start)
    start_service
    ;;
  stop)
    stop_service
    ;;
  restart)
    stop_service
    start_service
    ;;
  status)
    status_service
    ;;
  logs)
    logs_service
    ;;
  uninstall)
    uninstall_service
    ;;
  *)
    usage
    exit 1
    ;;
esac
