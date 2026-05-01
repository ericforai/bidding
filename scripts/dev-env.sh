#!/usr/bin/env bash
# Input: current working directory inside a main checkout or agent worktree
# Output: exported frontend, backend, database, and Redis isolation variables
# Pos: scripts/多 Agent 本地环境识别脚本
# 维护声明: 仅维护本地协作环境端口与资源映射；调整 worktree 分配时请同步 SOP 与脚本目录说明。
# Do not use set -e because this is meant to be sourced

CURRENT_DIR="$(pwd)"

if [[ "$CURRENT_DIR" == *"worktrees/claude"* ]]; then
  export FRONTEND_PORT=1315
  export BACKEND_PORT=18081
  export SIDECAR_PORT=8001
  export DB_NAME="xiyu_bid_claude"
  export REDIS_DB=1
elif [[ "$CURRENT_DIR" == *"worktrees/codex"* ]]; then
  export FRONTEND_PORT=1316
  export BACKEND_PORT=18082
  export SIDECAR_PORT=8002
  export DB_NAME="xiyu_bid_codex"
  export REDIS_DB=2
elif [[ "$CURRENT_DIR" == *"worktrees/gemini"* ]]; then
  export FRONTEND_PORT=1317
  export BACKEND_PORT=18083
  export SIDECAR_PORT=8003
  export DB_NAME="xiyu_bid_gemini"
  export REDIS_DB=3
elif [[ "$CURRENT_DIR" == *"worktrees/cursor"* ]]; then
  export FRONTEND_PORT=1318
  export BACKEND_PORT=18084
  export SIDECAR_PORT=8004
  export DB_NAME="xiyu_bid_cursor"
  export REDIS_DB=4
elif [[ "$CURRENT_DIR" == *"worktrees/integrator"* ]]; then
  export FRONTEND_PORT=1319
  export BACKEND_PORT=18085
  export SIDECAR_PORT=8005
  export DB_NAME="xiyu_bid_integrator"
  export REDIS_DB=5
else
  # Default for main project root (/Users/user/xiyu/xiyu-bid-poc/)
  export FRONTEND_PORT=1314
  export BACKEND_PORT=18080
  export SIDECAR_PORT=8000
  export DB_NAME="xiyu_bid_main"
  export REDIS_DB=0
fi

echo "Environment detected: $(basename "$CURRENT_DIR")"
echo "Frontend Port: $FRONTEND_PORT"
echo "Backend Port: $BACKEND_PORT"
echo "Sidecar Port: $SIDECAR_PORT"
echo "DB Name: $DB_NAME"
echo "Redis DB: $REDIS_DB"
