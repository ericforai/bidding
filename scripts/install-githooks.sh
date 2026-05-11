#!/usr/bin/env bash
# scripts/install-githooks.sh — enable repo-local git hooks for this clone.
#
# The hooks live in .githooks/ but git ignores them by default. Running this
# once per fresh clone tells git to use that directory:
#
#   git config core.hooksPath .githooks
#
# Re-running is safe — it just re-asserts the config.
#
# Why this isn't automatic: git intentionally won't honor in-tree hooks for
# repos you've just cloned, to prevent supply-chain attacks where a malicious
# repo runs code on your machine the moment you `git checkout`. The developer
# has to opt in.

set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null)" || {
    echo "Not inside a git working tree." >&2
    exit 1
}
cd "${ROOT_DIR}"

git config core.hooksPath .githooks
echo "core.hooksPath -> .githooks"
git config --get core.hooksPath

echo ""
echo "Installed hooks:"
ls -1 .githooks
