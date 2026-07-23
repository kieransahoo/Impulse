.PHONY: setup dev down test secrets-check

# Prepare a fresh clone and securely collect a local Gemini key.
setup:
	@./scripts/setup.sh

# Start PostgreSQL/pgvector, FastAPI AI, and Spring Boot from the repository root.
dev:
	@./scripts/dev.sh

# Stop the shared PostgreSQL container. Its named volume is preserved.
down:
	@docker compose -f apps/backend/compose.yml down

# Run all automated service and extension checks.
test:
	@./scripts/check-secrets.sh
	@python3 apps/extension/tests/run_tests.py
	@node --check apps/extension/utils/api.js
	@node --check apps/extension/popup/popup.js
	@node --check apps/backend/src/main/resources/static/app.js
	@.venv/bin/pytest -q apps/ai-service
	@cd apps/backend && ./gradlew test

# Verify that common cloud/API key patterns are absent from tracked files.
secrets-check:
	@./scripts/check-secrets.sh
