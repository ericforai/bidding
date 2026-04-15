#!/usr/bin/env bash
# Input: git repository hooks directory and bundled pre-commit source hook
# Output: installed pre-commit hook with executable permissions for Java standards checks
# Pos: scripts/开发环境安装脚本
# 维护声明: 仅维护本地 hook 安装逻辑；若 hook 来源或规则变化请同步更新安装提示。
set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
SOURCE_HOOK="$ROOT_DIR/.githooks/pre-commit"
TARGET_HOOK="$ROOT_DIR/.git/hooks/pre-commit"

if [ ! -f "$SOURCE_HOOK" ]; then
  echo "Missing source hook: $SOURCE_HOOK"
  exit 1
fi

if [ -f "$TARGET_HOOK" ]; then
  backup="$ROOT_DIR/.git/hooks/pre-commit.backup.$(date +%Y%m%d%H%M%S)"
  cp "$TARGET_HOOK" "$backup"
  echo "Backed up existing pre-commit hook to: $backup"
fi

cp "$SOURCE_HOOK" "$TARGET_HOOK"
chmod +x "$TARGET_HOOK"
chmod +x "$SOURCE_HOOK"
chmod +x "$ROOT_DIR/scripts/check-java-coding-standards.sh"

echo "Installed pre-commit hook."
echo "Hook target: $TARGET_HOOK"
