#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/dev-env.sh"

export SERVER_PORT="$BACKEND_PORT"
# Map DB_NAME and REDIS_DB to SPRING_DATASOURCE_URL and SPRING_DATA_REDIS_DATABASE
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/${DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export SPRING_DATA_REDIS_DATABASE="$REDIS_DB"

# Also set CORS allowed origins to include the dynamically assigned frontend port
export CORS_ALLOWED_ORIGINS="http://localhost:${FRONTEND_PORT},http://127.0.0.1:${FRONTEND_PORT}"

echo "Starting backend on port $SERVER_PORT (DB: $DB_NAME, Redis: $REDIS_DB)..."
# Call the existing start script in the backend directory
cd "$SCRIPT_DIR/../backend"
./start.sh
