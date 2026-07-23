# Database Design

## Primary Data Store
- **PostgreSQL** – reliable relational DB for structured memory records.
- **pgvector** extension – stores 1536‑dimensional embeddings for semantic search.

## Schema Overview
```sql
CREATE TABLE memory (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    url           TEXT,
    platform      TEXT,
    title         TEXT,
    description   TEXT,
    summary       TEXT,
    category      TEXT,
    tags          TEXT[],
    user_note     TEXT,
    location      TEXT,
    budget        NUMERIC,
    mood          TEXT,
    embedding     VECTOR(1536),
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Index for fast keyword search
CREATE INDEX idx_memory_fulltext ON memory USING gin (
    to_tsvector('english', title || ' ' || description || ' ' || tags || ' ' || user_note)
);

-- Index for ANN vector search
CREATE INDEX idx_memory_embedding ON memory USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

## Relationships
- **User** table (not shown) holds authentication info; `memory.user_id` references it.
- **Content** table can be introduced later for raw HTML snapshots.

## Migration & Maintenance
- Use Flyway or Liquibase for versioned schema migrations.
- Periodically **re‑index** the `embedding` index after bulk embedding updates.
- Run a nightly job to **re‑embed** older memories when the embedding model improves.
