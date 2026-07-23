# CreatorBrain + Impulse implementation tracker

## Completion

- **MVP extension → memory → RAG → plan flow: 100% (6/6 stages).**
- **Backend MVP shared URL → collection → memory → retrieval → plan: 100%.**
- **Full supplied target architecture: 70%.**

| # | Stage | Status | Completion |
|---|---|---|---:|
| 1 | Existing extension ingestion | Complete | 100% |
| 2 | Gemini content understanding | Complete for visible text/metadata MVP | 100% MVP |
| 3 | Episodic + semantic + action memory | Complete | 100% |
| 4 | Gemini embeddings + PostgreSQL/pgvector | Complete | 100% |
| 5 | User-scoped hybrid RAG retrieval | Complete for MVP | 100% MVP |
| 6 | Structured LLM planner returned to extension | Complete | 100% |
| 7 | Audio transcription | Deferred | 0% |
| 8 | Screenshot/frame analysis | Deferred | 0% |
| 9 | Recommendation feedback learning | Deferred | 0% |
| 10 | Production ANN retrieval/reranking/jobs | Deferred | 0% |

## Backend MVP completed surface

- Multi-URL user collections with ownership by `userId`.
- YouTube, Instagram, and generic public webpage memory processing.
- Independent per-source success/failure status and existing-memory reuse.
- Layered episodic, semantic, and action memory persistence.
- Inspectable hybrid retrieval plus grounded planning.
- Same-origin test console at `http://localhost:8081/`.
- Interactive prompt-only plan timeline with progress tracking, expandable
  reasoning, source links, and memory thumbnails.
- Dedicated `gemini-3.1-flash-lite` planner model; live benchmark reduced
  structured plan generation from roughly 19 seconds to roughly 5 seconds.
- Future authentication boundary is isolated to replacing the test UUID with
  the logged-in app user's ID.

## Implemented flow

```text
Existing Chrome extension
  → POST /api/memories/import
  → FastAPI /api/v1/memories/process
  → Gemini summary + topics + actions + embedding
  → Spring Boot
  → PostgreSQL/pgvector layered memory
  → POST /api/impulse/plan
  → query embedding
  → user-only hybrid ranking (top 8)
  → Gemini structured planner
  → personalized plan in the same extension
```

## Memory layers

- **Episodic:** source URL, platform, user, note, and saved time.
- **Semantic:** summary, category, tags, topics, and 768-dimensional embedding.
- **Action:** action, use conditions, optional duration, and category.

The structured memory is embedded; the system does not simply dump an entire
raw transcript into the vector store.

## Current retrieval score

```text
0.60 semantic cosine similarity
+ 0.20 keyword/topic/action match
+ 0.15 recency
+ 0.05 action-memory availability
```

Planner citations are filtered so they can reference only memories retrieved
for the requesting user.

## Retrieval test API

Use the same User ID shown in extension Settings:

```bash
curl -X POST http://localhost:8081/api/memories/search \
  -H 'Content-Type: application/json' \
  -d '{"userId":"YOUR-UUID","query":"deep work coding plan","limit":8}'
```

Then test grounded planning:

```bash
curl -X POST http://localhost:8081/api/impulse/plan \
  -H 'Content-Type: application/json' \
  -d '{"userId":"YOUR-UUID","query":"Plan a focused coding evening","constraints":{"availableMinutes":180}}'
```

The search response lets you inspect retrieval before involving the planner.
The plan response includes `retrievedMemoryIds`, and each step may include the
specific `memoryIds` used.

## Verification ledger

- Extension automated checks: **26 passed**.
- Extension extractor checks: **16 passed**.
- Extension JavaScript syntax: **passed**.
- Spring Boot compile and tests: **16 passed**.
- AI Python compilation: **passed**.
- AI pytest: **5 passed**.

## Next order

1. Store plan accepted/modified/skipped/completed feedback.
2. Apply feedback to importance and preference scoring.
3. Add asynchronous Whisper transcription and frame analysis after defining
   media-access and storage policies.
4. Move high-volume retrieval to pgvector ANN and add reranking.
