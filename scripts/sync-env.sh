#!/usr/bin/env bash
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
    cp "$ROOT_DIR/$file" "$TARGET_DIR/$file"
    echo "Copied $file to $TARGET_DIR"
  else
    echo "Warning: $file not found in $ROOT_DIR"
  fi
done
