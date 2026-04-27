#!/usr/bin/env bash
# Input: agent name, task slug, and optional base ref
# Output: isolated worktree, task branch, and local .agent-task-context
# Pos: scripts/多 Agent 工作区初始化
# 维护声明: 若工作区根目录、分支前缀或任务上下文字段变化，请同步更新 scripts/README.md。
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/agent-start-task.sh <agent> <task-slug> [base-ref]

Example:
  scripts/agent-start-task.sh codex project-task-breakdown-from-tender origin/main
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ $# -lt 2 || $# -gt 3 ]]; then
  usage >&2
  exit 1
fi

AGENT_NAME="$1"
TASK_SLUG="$2"
BASE_REF="${3:-origin/main}"
WORKTREES_ROOT="/Users/user/xiyu/worktrees"
WORKTREE_PATH="$WORKTREES_ROOT/$AGENT_NAME-$TASK_SLUG"
BRANCH_NAME="$AGENT_NAME/$TASK_SLUG"

if [[ ! "$AGENT_NAME" =~ ^[a-z][a-z0-9-]*$ ]]; then
  echo "agent-start-task: invalid agent name '$AGENT_NAME'." >&2
  exit 1
fi

if [[ ! "$TASK_SLUG" =~ ^[a-z0-9][a-z0-9-]*$ ]]; then
  echo "agent-start-task: invalid task slug '$TASK_SLUG'." >&2
  exit 1
fi

if [[ -e "$WORKTREE_PATH" ]]; then
  echo "agent-start-task: worktree already exists: $WORKTREE_PATH" >&2
  exit 1
fi

if git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
  echo "agent-start-task: branch already exists: $BRANCH_NAME" >&2
  exit 1
fi

mkdir -p "$WORKTREES_ROOT"
git fetch origin --prune
git worktree add -b "$BRANCH_NAME" "$WORKTREE_PATH" "$BASE_REF"

cat > "$WORKTREE_PATH/.agent-task-context" <<EOF
agent=$AGENT_NAME
task=$TASK_SLUG
branch=$BRANCH_NAME
base=$BASE_REF
worktree=$WORKTREE_PATH
created_at=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
EOF

echo "Created task worktree:"
echo "  worktree: $WORKTREE_PATH"
echo "  branch:   $BRANCH_NAME"
echo "  base:     $BASE_REF"
echo
echo "Next:"
echo "  cd $WORKTREE_PATH"
echo "  scripts/install-java-standards-hook.sh"
