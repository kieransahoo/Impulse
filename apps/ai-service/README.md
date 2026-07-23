# Impulse AI Service

FastAPI service that converts selected YouTube/Instagram links and normalized
playlist content into structured memories with 768-dimension Gemini embeddings.

## Setup

From the repository root:

```bash
source .venv/bin/activate
pip install -r apps/ai-service/requirements.txt
cp apps/ai-service/.env.example apps/ai-service/.env
```

Set a real `GEMINI_API_KEY` in `.env`, then start:

```bash
cd apps/ai-service
../../.venv/bin/uvicorn main:app --host 127.0.0.1 --port 8001 --reload
```

`GEMINI_PLANNER_MODEL` defaults to `gemini-3.1-flash-lite`, while memory
extraction remains on `GEMINI_MODEL`. This keeps structured ingestion stable
and reduces grounded-plan generation latency.

Health:

```bash
curl http://localhost:8001/health
```

Expected:

```json
{"status":"UP","geminiConfigured":true}
```

Run tests:

```bash
../../.venv/bin/python -m pytest -q
```

## Processing behavior

- Collected playlists arrive with normalized titles, URLs, and descriptions.
- YouTube video/Short metadata is read through YouTube's public oEmbed endpoint.
- Instagram uses visible content supplied by the extension first, then bounded
  public Open Graph metadata as a fallback.
- Only allow-listed YouTube and Instagram hosts are fetched.
- Source content is treated as untrusted text and capped at 50,000 characters.
- Gemini generates structured metadata and a 768-dimension embedding.
- The Spring backend owns PostgreSQL/pgvector persistence and user isolation.

## HTTP contract

`POST /api/v1/memories/process`

```json
{
  "sourceUrl": "https://www.youtube.com/watch?v=abc",
  "platform": "YOUTUBE_VIDEO",
  "userNote": "Remember this",
  "content": "Optional visible or collected content"
}
```
