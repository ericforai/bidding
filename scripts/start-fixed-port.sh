#!/usr/bin/env bash

set -euo pipefail

MODE="${1:-}"

if [[ -z "$MODE" ]]; then
  echo "Usage: $0 <mock|api>"
  exit 1
fi

case "$MODE" in
  mock)
    PORT=1314
    ;;
  api)
    PORT=1818
    ;;
  *)
    echo "Unsupported mode: $MODE"
    exit 1
    ;;
esac

PIDS="$(lsof -ti tcp:${PORT} || true)"
if [[ -n "$PIDS" ]]; then
  echo "Killing existing process on port ${PORT}: ${PIDS}"
  kill $PIDS
  sleep 1
fi

exec vite --mode "$MODE" --host 0.0.0.0 --port "$PORT" --strictPort
