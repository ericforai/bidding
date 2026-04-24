#!/bin/bash

# 西域数智化投标管理平台 Backend - 启动脚本

echo "Starting 西域数智化投标管理平台 Backend..."

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: Java 21 or higher is required"
    exit 1
fi

# 必需环境变量（允许外部覆盖，未设置则使用本地开发默认值）
# 生产部署必须通过真实环境变量注入，不得依赖以下默认值
export JWT_SECRET="${JWT_SECRET:-xiyu-bid-poc-local-dev-secret-key-please-change-in-prod-32bytes-min}"
export DB_USERNAME="${DB_USERNAME:-xiyu_user}"
export DB_PASSWORD="${DB_PASSWORD:-XiyuDB!2026}"
export CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:1314,http://127.0.0.1:1314}"

# 服务端口（默认 18080，与前端和文档保持一致）
SERVER_PORT="${SERVER_PORT:-18080}"
BACKEND_PROFILES="${SPRING_PROFILES_ACTIVE:-dev,mysql}"
DB_HOST_DISPLAY="${DB_HOST:-localhost}"
DB_PORT_DISPLAY="${DB_PORT:-3306}"

# 使用 MySQL 8.0 开发配置启动（自动种子化默认管理员 admin / XiyuAdmin2026!）
echo "Using ${BACKEND_PROFILES} profile(s) (MySQL 8.0 on ${DB_HOST_DISPLAY}:${DB_PORT_DISPLAY}, auto-seeds default admin)"
echo "Server port: ${SERVER_PORT}"
SPRING_PROFILES_ACTIVE="${BACKEND_PROFILES}" mvn spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=${SERVER_PORT}"
