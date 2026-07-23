# Impulse Chrome Extension

The existing UI captures visible YouTube and Instagram links from pages the
user is already signed into. It does not require a YouTube API key or Instagram
password.

Only integration behavior was added:

- Backend base URL defaults to `http://localhost:8081`.
- A stable User ID is generated in extension storage and shown in Settings.
- Saves call `POST /api/memories/import`; the backend invokes the AI service and
  stores structured memories and embeddings in PostgreSQL/pgvector.
- Failed saves remain in the existing offline queue.
- Query & Plan calls `POST /api/impulse/plan` with the same User ID.

Reload the unpacked extension after code changes. Scroll a social page first if
the site lazily loads saved videos/Reels.
