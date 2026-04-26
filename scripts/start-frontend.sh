#!/usr/bin/env bash
# Input: detected agent worktree environment from scripts/dev-env.sh
# Output: starts the frontend dev server on the assigned isolated port
# Pos: scripts/多 Agent 前端启动脚本
# 维护声明: 仅维护本地前端启动端口注入；端口分配或真实 API 启动口径变化时请同步协作 SOP。
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/dev-env.sh"

echo "Starting frontend on port $FRONTEND_PORT..."
# vite respects --port
npm run dev -- --port "$FRONTEND_PORT"
