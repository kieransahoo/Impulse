# CreatorBrain + Impulse

## Run everything

Requirements: Docker, Java 21, and the repository Python environment.

Set the Gemini key once:

```bash
cp apps/ai-service/.env.example apps/ai-service/.env
```

Edit `apps/ai-service/.env`, then from the repository root run:

```bash
make dev
```

This single command starts:

- PostgreSQL + pgvector on `localhost:5433`
- FastAPI/Gemini AI service on `localhost:8001`
- Kotlin Spring Boot backend on `localhost:8081`

Open the local MVP console:

- `http://localhost:8081/`

The page creates a stable test-user UUID, processes multi-URL collections,
inspects retrieved memories, and creates grounded personalized plans.

Health URLs:

- `http://localhost:8001/health`
- `http://localhost:8001/ready` (validates the configured Gemini key)
- `http://localhost:8081/actuator/health`

Press Ctrl+C to stop the AI and backend processes. PostgreSQL remains available
for the next run; stop it with:

```bash
make down
```

Run all automated checks with:

```bash
make test
```
