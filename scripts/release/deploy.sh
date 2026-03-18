#!/usr/bin/env bash
# Input: release environment variables, repository filesystem, and deployment arguments
# Output: release preflight, backup, deployment, and signoff side effects
# Pos: scripts/release/ - Release automation and rehearsal helpers
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"

printf '==> Running release preflight\n'
bash "$ROOT_DIR/scripts/release/preflight.sh"

printf '\n==> Building frontend (mock mode)\n'
cd "$ROOT_DIR"
npm run build

printf '\n==> Building frontend (api mode)\n'
VITE_API_MODE=api npm run build

printf '\n==> Compiling backend\n'
cd "$BACKEND_DIR"
mvn -DskipTests compile

printf '\n==> Running critical backend tests\n'
mvn -Dtest=FlywayBaselineContextTest,ExpenseControllerIntegrationTest,BarCertificateControllerIntegrationTest test

if command -v docker >/dev/null 2>&1; then
  printf '\n==> Running PostgreSQL Testcontainers baseline verification\n'
  mvn -Dtest=FlywayPostgresContainerTest test
else
  printf '\nSkipping PostgreSQL Testcontainers verification because Docker is unavailable.\n'
fi

printf '\n==> Release rehearsal completed\n'
printf 'Next steps:\n'
printf '1. Take a production backup with scripts/release/backup-db.sh\n'
printf '2. Apply migration and deploy application artifacts in the target environment\n'
printf '3. Execute docs/GO_LIVE_CHECKLIST.md and docs/UAT_PLAN.md\n'
