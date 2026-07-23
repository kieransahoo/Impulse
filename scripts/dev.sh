#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/apps/backend/compose.yml"
AI_DIR="${PROJECT_ROOT}/apps/ai-service"
UVICORN="${PROJECT_ROOT}/.venv/bin/uvicorn"
GRADLEW="${PROJECT_ROOT}/apps/backend/gradlew"
AI_PID=""

cleanup() {
  if [[ -n "${AI_PID}" ]] && kill -0 "${AI_PID}" 2>/dev/null; then
    kill "${AI_PID}" 2>/dev/null || true
    wait "${AI_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_command docker
require_command curl
require_command java

if [[ ! -x "${UVICORN}" ]]; then
  echo "Python environment is missing. Run: python3 -m venv .venv && .venv/bin/pip install -r apps/ai-service/requirements.txt" >&2
  exit 1
fi

if [[ ! -x "${GRADLEW}" ]]; then
  echo "Gradle wrapper is missing or is not executable: ${GRADLEW}" >&2
  exit 1
fi

if [[ -z "${GEMINI_API_KEY:-}" ]] && (
  [[ ! -f "${AI_DIR}/.env" ]] ||
  ! grep -Eq '^GEMINI_API_KEY=.+$' "${AI_DIR}/.env" ||
  grep -Eq '^GEMINI_API_KEY=(your-key|your_new_key|replace_me|change_me)$' "${AI_DIR}/.env"
); then
  echo "Gemini key is missing. Run make setup, or export GEMINI_API_KEY." >&2
  exit 1
fi

echo "Starting PostgreSQL/pgvector on localhost:5433..."
docker compose -f "${COMPOSE_FILE}" up -d --wait

if curl --silent --fail --max-time 1 http://localhost:8001/health >/dev/null 2>&1; then
  echo "Port 8001 already has an AI service running. Stop the existing make dev process and retry." >&2
  exit 1
fi

echo "Starting Impulse AI service on http://localhost:8001..."
(
  cd "${AI_DIR}"
  exec "${UVICORN}" main:app --host 127.0.0.1 --port 8001 --reload
) &
AI_PID=$!

for _ in {1..30}; do
  if ! kill -0 "${AI_PID}" 2>/dev/null; then
    echo "AI service stopped before becoming healthy." >&2
    exit 1
  fi
  if curl --silent --fail http://localhost:8001/health >/dev/null; then
    break
  fi
  sleep 1
done

if ! curl --silent --fail http://localhost:8001/health >/dev/null; then
  echo "AI service did not become healthy within 30 seconds." >&2
  exit 1
fi

echo "Validating Gemini configuration..."
READY_RESPONSE="$(curl --silent --show-error http://localhost:8001/ready || true)"
if ! curl --silent --fail http://localhost:8001/ready >/dev/null; then
  echo "AI service is running, but Gemini is not ready." >&2
  echo "${READY_RESPONSE}" >&2
  echo "Update the private .env key or exported GEMINI_API_KEY, then run make dev again." >&2
  exit 1
fi

echo
echo "Services:"
echo "  AI health:      http://localhost:8001/health"
echo "  AI readiness:   http://localhost:8001/ready"
echo "  Backend health: http://localhost:8081/actuator/health"
echo "  Retrieval API:  http://localhost:8081/api/memories/search"
echo "  Planning API:   http://localhost:8081/api/impulse/plan"
echo
echo "Starting Impulse backend. Press Ctrl+C to stop AI and backend."

cd "${PROJECT_ROOT}/apps/backend"
DB_URL="jdbc:postgresql://localhost:5433/impulse" \
DB_USERNAME="postgres" \
DB_PASSWORD="root" \
AI_SERVICE_URL="http://localhost:8001" \
PORT="8081" \
"${GRADLEW}" bootRun
