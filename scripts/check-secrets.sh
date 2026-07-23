#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${PROJECT_ROOT}"

if ! command -v git >/dev/null 2>&1 || ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Secret check skipped: not inside a Git worktree."
  exit 0
fi

PATTERN='(AIza[0-9A-Za-z_-]{30,}|AQ\.[0-9A-Za-z_-]{20,}|sk-[0-9A-Za-z_-]{20,})'
if git grep -I -l -E "${PATTERN}" -- ':!*.lock' > /tmp/impulse-secret-files.txt; then
  echo "Possible API key found in tracked files:" >&2
  sed -n '1,100p' /tmp/impulse-secret-files.txt >&2
  echo "Move secrets to apps/ai-service/.env or an environment variable." >&2
  exit 1
fi

if git ls-files --error-unmatch apps/ai-service/.env >/dev/null 2>&1; then
  echo "apps/ai-service/.env is tracked. Remove it with:" >&2
  echo "  git rm --cached apps/ai-service/.env" >&2
  exit 1
fi

echo "Tracked secret check passed."
