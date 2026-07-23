# Memory Design

## Memory Structure

Every saved item becomes a **Memory** with the following fields:

- **URL** – Original source link.
- **Platform** – e.g., YouTube, Instagram, Web.
- **Title** – Extracted title of the content.
- **Description** – Short description or excerpt.
- **Summary** – AI‑generated concise summary.
- **Category** – Logical grouping (e.g., Article, Product, Place).
- **Tags** – User‑defined or AI‑suggested keywords.
- **User Note** – Personal annotation.
- **Location** – Physical location if relevant (e.g., restaurant address).
- **Budget** – Cost information when applicable.
- **Mood** – Sentiment or vibe of the content.
- **Embedding** – Vector representation stored in `pgvector` for semantic search.

These fields enable rich, multi‑dimensional retrieval and recommendation.

## Implemented layers

- Episodic source data lives in the parent memory record.
- Semantic memory includes summary, category, tags, topics, and embedding.
- Action memory includes executable actions, use conditions, duration, and
  category.
