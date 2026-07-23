# Backend integration contract

The Settings value is a backend base URL, not an individual endpoint.

## Memory ingestion

```http
POST /api/memories/import
```

```json
{
  "userId": "stable-extension-uuid",
  "url": "https://www.youtube.com/watch?v=...",
  "userNote": "Optional note",
  "content": "Title, description, visible text, content type, and tags"
}
```

The Spring Boot backend calls FastAPI/Gemini for structured extraction and
embedding, then persists episodic, semantic, and action memory in PostgreSQL.

## Personalized planning

```http
POST /api/impulse/plan
```

```json
{
  "userId": "the-same-stable-extension-uuid",
  "query": "Plan my evening from my saved productivity ideas",
  "constraints": {}
}
```

The backend retrieves only this user's memories, ranks them, asks the planner
LLM for structured steps, and returns the plan with memory citations.
