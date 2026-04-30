#!/usr/bin/env bash
# Input: target worktree directory and root environment template files
# Output: copied environment files for the target worktree
# Pos: scripts/多 Agent 环境文件同步脚本
# 维护声明: 仅维护本地 worktree 环境文件同步；新增环境模板时请同步脚本目录说明。
set -e

# ROOT_DIR is the directory where the script is located (xiyu-bid-poc/scripts/..)
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET_DIR="$1"

if [ -z "$TARGET_DIR" ]; then
  echo "Usage: ./scripts/sync-env.sh <target-worktree-dir>"
  exit 1
fi

# Convert TARGET_DIR to absolute path if it's relative
if [[ "$TARGET_DIR" != /* ]]; then
  # If TARGET_DIR doesn't exist yet, we can't cd into it
  if [ -d "$TARGET_DIR" ]; then
    TARGET_DIR="$(cd "$TARGET_DIR" && pwd)"
  else
    # Best effort to resolve relative path
    TARGET_DIR="$(cd "$ROOT_DIR" && cd "$TARGET_DIR" && pwd)" || { echo "Error: Cannot resolve target directory $TARGET_DIR"; exit 1; }
  fi
fi

FILES=(
  ".env.api"
)

for file in "${FILES[@]}"; do
  if [ -f "$ROOT_DIR/$file" ]; then
    source_file="$ROOT_DIR/$file"
    target_file="$TARGET_DIR/$file"
    if [ "$source_file" = "$target_file" ]; then
      echo "Skipped $file; source and target are the same file"
    else
      cp "$source_file" "$target_file"
      echo "Copied $file to $TARGET_DIR"
    fi
  else
    echo "Warning: $file not found in $ROOT_DIR"
  fi
done
