# CreatorBrain + Impulse

## Fresh clone to running MVP

Requirements: Docker, Java 21, Python 3, and Make.

```bash
git clone https://github.com/kieransahoo/Impulse.git
cd Impulse
make setup
make dev
```

`make setup` creates `.venv`, installs the AI dependencies, and asks for a
Gemini API key with hidden input. The key is stored only in
`apps/ai-service/.env`, which Git ignores.

Open the test console at [http://localhost:8081/](http://localhost:8081/).

## API key options

Each developer can run `make setup` and use their own Gemini key.

For your own machine, keep your key in:

```text
apps/ai-service/.env
```

For CI, containers, or hosted environments, inject the key without a file:

```bash
export GEMINI_API_KEY="..."
make dev
```

Never put a real key in `.env.example`, source code, documentation, commits, or
shell scripts. `.env.example` contains safe placeholders only.

## Services

`make dev` starts:

- PostgreSQL + pgvector on `localhost:5433`
- FastAPI/Gemini AI service on `localhost:8001`
- Kotlin Spring Boot backend and webpage on `localhost:8081`

Health checks:

- `http://localhost:8001/health`
- `http://localhost:8001/ready`
- `http://localhost:8081/actuator/health`

Press Ctrl+C to stop the AI and backend. PostgreSQL data remains available.

```bash
make down
```

## Verification

```bash
make test
make secrets-check
```

The test workflow also fails if a likely API key or tracked private `.env` is
detected.
