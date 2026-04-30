#!/usr/bin/env bash
# Input: optional dev-services command arguments; defaults to stable local service start
# Output: delegates legacy one-command startup to the identity-checked dev service manager
# Pos: root - compatibility entrypoint for local real-API development
# 一旦我被更新，务必更新 README.md 中的启动说明。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ $# -eq 0 ]]; then
  set -- start
fi

echo "[dev] start.sh delegates to scripts/dev-services.sh $*"
exec "$ROOT_DIR/scripts/dev-services.sh" "$@"
