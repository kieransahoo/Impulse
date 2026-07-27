# Retrieval‑Augmented Generation (RAG) Design

## User Flow Overview

### Save Memory Flow
1. **User** pastes a URL or uses the Chrome extension to trigger a save.
2. **Metadata Extraction** pulls title, description, platform, etc.
3. **AI Understanding** creates a summary, categories, tags, and an embedding.
4. **Review Memory** – user can edit notes or tags.
5. **Save** – persisted to PostgreSQL with the vector embedding.

### Ask Impulse Flow
1. **User** asks a natural‑language question.
2. **Intent Detection** – deterministic keywords identify common MVP intents
   without spending an LLM call.
3. **Memory Retrieval** – a hybrid search:
   - **Keyword Filter** on fields like tags, category, location.
   - **Semantic Search** using the stored embedding via `pgvector`.
4. **Apply Constraints** (budget, mood, location, etc.).
5. **Rank Results** – combine relevance scores from keyword match and vector similarity.
6. **Generate Recommendation** – LLM crafts a personalized answer and a step‑by‑step timeline.
7. **Explain Why** – the model surfaces the memories influencing the recommendation.

## Retrieval Strategy
- **Hybrid Retrieval**: Combine BM25‑style keyword matching (PostgreSQL `LIKE` / full‑text search) with cosine similarity on embeddings.
- **Relevance gate**: discard weak semantic/keyword matches instead of forcing
  unrelated memories into a plan.
- **Context Window**: send at most six compact memories (title, summary, category,
  topics and actions), never full source content or URLs, to final generation.

## Scalability Considerations
- Store embeddings in a `pgvector` column for efficient ANN search.
- Periodically recompute embeddings when the underlying model improves.
- Cache recent query results per user session to reduce latency.

## Implemented MVP retrieval

`POST /api/impulse/plan` embeds the query and ranks only the requesting user's
memories using semantic similarity, keyword/topic/action matches, recency, and
action availability. At most six relevant, URL-deduplicated memories are sent
to Gemini for structured planning.

The response labels grounding as `STRONG_GROUNDING`, `PARTIAL_GROUNDING`, or
`NO_GROUNDING`. With no match, the default request does not call the planner.
It returns missing context and suggested source types. The user may explicitly
request a general starter plan with `allowGeneralKnowledge=true`; those steps
are labelled `GENERAL` and never presented as saved-memory evidence.

Saved plans are immutable snapshots. Regeneration reuses the snapshot's goal
but performs retrieval again over the user's current memory store. This makes
new memories and newly added collections available without silently changing
the original saved result.

## Token budget strategy

- One understanding call per newly saved source; store the reusable structured result.
- Embed only the compact structured memory (maximum 4,000 characters).
- Retrieve before generation and cap final context at six memories.
- Detect intent and missing fields in the backend without an LLM.
- Keep planning output to 4–7 actionable steps and a 1,000-token output ceiling.
- Cite stable memory IDs; never resend original source bodies during planning.
