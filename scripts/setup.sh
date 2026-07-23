#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VENV_DIR="${PROJECT_ROOT}/.venv"
AI_ENV="${PROJECT_ROOT}/apps/ai-service/.env"

if ! command -v python3 >/dev/null 2>&1; then
  echo "Python 3 is required." >&2
  exit 1
fi

if [[ ! -x "${VENV_DIR}/bin/python" ]]; then
  echo "Creating Python environment..."
  python3 -m venv "${VENV_DIR}"
fi

echo "Installing AI service dependencies..."
"${VENV_DIR}/bin/pip" install -r "${PROJECT_ROOT}/apps/ai-service/requirements.txt"

if [[ -f "${AI_ENV}" ]]; then
  echo "Keeping existing private configuration: apps/ai-service/.env"
else
  KEY="${GEMINI_API_KEY:-}"
  if [[ -z "${KEY}" && -t 0 ]]; then
    read -r -s -p "Gemini API key (input hidden): " KEY
    echo
  fi
  if [[ -z "${KEY}" ]]; then
    echo "No key supplied. Set GEMINI_API_KEY or rerun make setup interactively." >&2
    exit 1
  fi

  umask 077
  {
    printf 'GEMINI_API_KEY=%s\n' "${KEY}"
    printf 'GEMINI_MODEL=gemini-2.5-flash-lite\n'
    printf 'GEMINI_EMBEDDING_MODEL=gemini-embedding-2\n'
    printf 'GEMINI_PLANNER_MODEL=gemini-3.1-flash-lite\n'
  } > "${AI_ENV}"
  echo "Saved key privately to ignored file: apps/ai-service/.env"
fi

echo
echo "Setup complete. Run: make dev"
