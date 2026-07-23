# API Specification

## Content Endpoints (CreatorBrain)

- `POST /contents`
  - Description: Save a new piece of content (URL, note, etc.)
  - Request Body: `{ url, platform?, userNote?, ... }`
  - Response: Created memory object with generated fields (title, summary, tags, embedding, ...)

- `GET /contents`
  - Description: Retrieve a paginated list of saved memories for the user.
  - Query Parameters: `page`, `size`, optional filters (`category`, `tags`, `date`)
  - Response: List of memory objects.

- `GET /contents/search`
  - Description: Search memories using keyword and semantic filters.
  - Query Parameters:
    - `q` – search query text
    - `filters` – JSON of constraints (budget, mood, location, tags, etc.)
    - `type` – `keyword` | `semantic` | `hybrid`
  - Response: Ranked list of relevant memories.

## Impulse Endpoints (Recommendation Engine)

- `POST /impulse/ask`
  - Description: Ask a natural‑language question; returns AI‑generated answer with cited memories.
  - Request Body: `{ question, constraints? }`
  - Response: `{ answer, sources: [memoryIds], reasoning }`

- `POST /impulse/plan`
  - Description: Request a multi‑step plan or timeline (e.g., "Plan a Saturday date under ₹2,000").
  - Request Body: `{ query, constraints? }`
  - Response: `{ plan: [{ step, memoryId }], explanation }`
