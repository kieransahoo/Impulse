# API Specification

## Content Endpoints (CreatorBrain)

### User collections

- `POST /api/collections`
  - Creates a named user collection and synchronously processes up to 20 shared
    public URLs.
  - Request:
    `{ userId, name, description?, sources: [{ url, userNote?, content? }] }`
  - Each source returns `PROCESSED` with `memoryId`, or `FAILED` with an
    independent `errorMessage`.
  - Duplicate URLs in one request are removed. A URL already stored for the
    user reuses the existing memory.

- `GET /api/collections?userId={uuid}`
  - Lists the user's collections and per-source processing state.

- `GET /api/collections/{id}?userId={uuid}`
  - Returns one collection only when owned by the supplied user.

### Link collection

- `POST /api/link-collections`
  - Collects metadata and normalized links from a public YouTube playlist.
  - Request: `{ userId, url }`
  - Requires `YOUTUBE_API_KEY`.
  - Returns the stored collection and its ordered items.

- `POST /api/link-collections/import`
  - Stores playlist metadata collected by the OAuth-authorized extension.
  - Request: `{ userId, url, title, items[] }`
  - Does not require `YOUTUBE_API_KEY`; the extension uses Google OAuth.

- `POST /api/link-collections/{id}/process`
  - Formats the stored collection, sends it to the AI service, and stores the
    returned structured memory and embedding.
  - Request: `{ userId, userNote? }`
  - Returns the created memory.
  - The collection ID is scoped to the supplied user ID.

- `POST /api/memories/import`
  - Sends selected visible content to AI understanding and stores layered memory.
  - Request: `{ userId, url, userNote?, content? }`
  - Response includes summary, category, tags, topics, and action memories.
  - Supports YouTube videos/Shorts/playlists, Instagram posts/Reels, and public
    HTML webpages.

- `GET /api/memories?userId={uuid}`
  - Returns the user's stored memories.

- `POST /api/memories/search`
  - Performs user-scoped hybrid retrieval without generating a plan.
  - Request: `{ userId, query, limit? }`, where `limit` is 1–20.
  - Response: ranked memories with total, semantic, keyword, and recency scores.

## Impulse Endpoints (Recommendation Engine)

- `POST /api/impulse/plan`
  - Description: Request a multi‑step plan or timeline (e.g., "Plan a Saturday date under ₹2,000").
  - Request Body: `{ userId, query, constraints? }`
  - Response:
    `{ goal, plan: [{ step, durationMinutes, reason, memoryIds }], explanation, retrievedMemoryIds, groundingMemories }`
  - Each grounding memory includes its title, summary, source URL, platform, and
    thumbnail when the source exposes one.
  - Returns HTTP `422` if the user has no persisted memories.
