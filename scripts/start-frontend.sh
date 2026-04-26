#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/dev-env.sh"

echo "Starting frontend on port $FRONTEND_PORT..."
# vite respects --port
npm run dev -- --port "$FRONTEND_PORT"
