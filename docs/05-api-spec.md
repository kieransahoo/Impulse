# API Specification

## Content Endpoints (CreatorBrain)

### User collections

- `POST /api/collections`
  - Creates a named user collection, stores up to 20 shared public URLs as
    `PENDING`, and processes them asynchronously.
  - Request:
    `{ userId, name, description?, sources: [{ url, userNote?, content? }] }`
  - Each new source initially returns `PENDING`, then becomes `PROCESSED` with
    `memoryId` or `FAILED` with an independent `errorMessage`.
  - Duplicate URLs in one request are removed. A URL already stored for the
    user reuses the existing memory.

- `GET /api/collections?userId={uuid}`
  - Lists the user's collections and per-source processing state.

- `GET /api/collections/{id}?userId={uuid}`
  - Returns one collection only when owned by the supplied user.

- `POST /api/collections/sources`
  - Adds exactly one URL to an existing collection.
  - Request: `{ userId, collectionId?, url, userNote?, content? }`.
  - A missing `collectionId` uses the user's protected `ALL` collection.
  - Adding the same normalized URL to the same collection is idempotent.
  - Returns after the source is safely stored; extraction, AI analysis, and
    embedding continue in the background and update it to `PROCESSED` or
    `FAILED`.

- `PATCH /api/collections/{id}`
  - Renames a collection and optionally updates its description.
  - Request: `{ userId, name, description? }`.
  - Names are unique per user, ignoring case. `ALL` cannot be renamed.

- `DELETE /api/collections/{id}?userId={uuid}`
  - Deletes a user-owned collection and its source associations.
  - Stored memories remain available to other collections and retrieval.
  - `ALL` cannot be deleted.

- `DELETE /api/collections/{collectionId}/sources/{sourceId}?userId={uuid}`
  - Removes one URL association from a collection without deleting its stored
    memory.

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

- `DELETE /api/memories/{id}?userId={uuid}`
  - Deletes one user-owned memory and removes its collection source
    associations. Saved plan history remains available.

- `DELETE /api/memories?userId={uuid}`
  - Deletes every memory and collection source association for the user.
  - Collection names and saved plans remain available.

- `POST /api/memories/search`
  - Performs user-scoped hybrid retrieval without generating a plan.
  - Request: `{ userId, query, limit? }`, where `limit` is 1–20.
  - Response: ranked memories with total, semantic, keyword, and recency scores.

## Impulse Endpoints (Recommendation Engine)

- `POST /api/impulse/plan`
  - Description: Request a multi‑step plan or timeline (e.g., "Plan a Saturday date under ₹2,000").
  - Request Body: `{ userId, query, constraints?, allowGeneralKnowledge? }`
  - `constraints.collectionIds` may contain one or more user-owned collection
    IDs. When supplied, retrieval is limited to memories associated with those
    collections. Missing, empty, malformed, or another user's collection IDs
    never broaden retrieval.
  - Collection-scoped requests never fall back to general model knowledge. If
    the selected collections have no relevant memory, the response is
    `NO_GROUNDING` with an empty plan so the client can offer a broader search.
  - Retrieval also applies an intent/category compatibility gate. Collection
    membership alone does not make a memory relevant.
  - Response:
    `{ intent, groundingStatus, goal, plan: [{ step, durationMinutes, reason, memoryIds, sourceType }], explanation, retrievedMemoryIds, groundingMemories, missingContext, suggestedSources }`
  - Each grounding memory includes its title, summary, source URL, platform, and
    thumbnail when the source exposes one.
  - With no relevant memory, returns `NO_GROUNDING` and an empty plan without
    invoking generation. Set `allowGeneralKnowledge=true` only after the user
    chooses a clearly labelled general starter plan.

### Saved plans

- `POST /api/plans`
  - Saves a generated personalized plan only after the user chooses to save it.
  - Request:
    `{ userId, goal, explanation, plan: [{ step, durationMinutes?, reason?, memoryIds }], retrievedMemoryIds }`
  - Returns the saved plan with a stable ID, ordered step IDs, citations, and
    creation time.
  - Every cited memory ID is validated as belonging to the supplied user.

- `GET /api/plans?userId={uuid}`
  - Lists the user's saved plans in newest-first order.

- `GET /api/plans/{id}?userId={uuid}`
  - Returns a saved plan only when owned by the supplied user.

- `GET /api/plans/active?userId={uuid}`
  - Returns the user's most recently activated `ACTIVE` plan for the Home
    screen, or an empty response when no plan is active.

- `POST /api/plans/{id}/regenerate`
  - Reuses the saved plan's original goal and retrieves against all memories
    the user currently owns, including memories added through newer collections.
  - Request: `{ userId, constraints?, allowGeneralKnowledge?, query? }`.
  - `query` lets the user edit the goal before regeneration; blank or omitted
    values reuse the original saved goal.
  - Replaces the existing saved plan goal, explanation, steps, citations, and
    progress with the newly generated result.
  - Returns the updated saved plan. It does not create a duplicate plan.
  - If no updated steps can be generated, returns HTTP `409` and leaves the
    existing plan unchanged.

- `DELETE /api/plans/{id}?userId={uuid}`
  - Deletes the user-owned saved plan and its progress.
  - Does not delete memories or collections.

### Plan execution

- `PATCH /api/plans/{id}/activate?userId={uuid}`
  - Changes a saved plan from `SAVED` to `ACTIVE` and records `activatedAt`.

- `PATCH /api/plans/{id}/steps/{stepId}?userId={uuid}`
  - Request: `{ completed: true|false }`.
  - Persists checklist progress and returns the complete updated saved plan.
  - Only an `ACTIVE` plan accepts progress changes; invalid transitions return
    HTTP `409`.

- `PATCH /api/plans/{id}/complete?userId={uuid}`
  - Marks an active plan `COMPLETED` only after every task is checked.
  - Records `completedAt`; incomplete plans return HTTP `409`.

Saved-plan responses include `status`, `activatedAt`, and `completedAt`.
Each saved step includes `completed` and `completedAt`.
