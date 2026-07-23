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
2. **Intent Detection** – Impulse AI determines the query’s purpose.
3. **Memory Retrieval** – a hybrid search:
   - **Keyword Filter** on fields like tags, category, location.
   - **Semantic Search** using the stored embedding via `pgvector`.
4. **Apply Constraints** (budget, mood, location, etc.).
5. **Rank Results** – combine relevance scores from keyword match and vector similarity.
6. **Generate Recommendation** – LLM crafts a personalized answer and a step‑by‑step timeline.
7. **Explain Why** – the model surfaces the memories influencing the recommendation.

## Retrieval Strategy
- **Hybrid Retrieval**: Combine BM25‑style keyword matching (PostgreSQL `LIKE` / full‑text search) with cosine similarity on embeddings.
- **Rerank**: Use a lightweight LLM to rerank the top‑N results based on the user’s intent and constraints.
- **Context Window**: Selected memories are concatenated with the user query and fed to the LLM (Gemini) for final generation.

## Scalability Considerations
- Store embeddings in a `pgvector` column for efficient ANN search.
- Periodically recompute embeddings when the underlying model improves.
- Cache recent query results per user session to reduce latency.
