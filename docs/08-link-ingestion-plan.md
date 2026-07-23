# Link Ingestion Plan

## Decision

Implement YouTube playlists first.

The YouTube Data API provides playlist metadata and paginated playlist items.
The extension uses user OAuth, so no YouTube API key is required. Accessible
private playlists can be read when the user grants the read-only YouTube scope.
Shorts do not have a separate playlist API: a playlist can contain both
regular videos and Shorts, and each item is collected as a YouTube video.

Instagram does not expose a general API for reading a personal user's saved
posts or saved Reels. The backend must not scrape authenticated Instagram
pages. The supported MVP path is therefore:

1. Accept an Instagram post/Reel URL explicitly shared by the user.
2. Optionally accept content supplied by a browser extension.
3. Process that user-supplied input through the AI service.

Automated Instagram saved-item synchronization is out of scope until Meta
provides an applicable permission/API or an approved extension flow is built.

## Staged flow

```text
User submits playlist URL
        |
        v
Extension validates and extracts playlist ID
        |
        v
Google OAuth consent grants read-only playlist access
        |
        v
YouTube Data API returns playlist items to the extension (paginated)
        |
        v
Extension sends normalized items; backend re-validates and stores them
        |
        v
AI service receives normalized collection content
        |
        v
AI returns summary, category, tags, and 768-value embedding
        |
        v
Backend stores the final pgvector memory
```

## Small implementation stages

### Stage 1 — source validation

- Recognize public YouTube playlist URLs.
- Extract a non-empty `list` query parameter.
- Reject regular video URLs and unrelated hosts.
- Covered by unit tests.

### Stage 2 — playlist collection

- Call `playlistItems.list` with `snippet,contentDetails`.
- Request at most 50 items per page and follow `nextPageToken`.
- Stop at the configured maximum item count.
- Store normalized item links, titles, descriptions, thumbnails, and order.
- Never download video media.

### Stage 3 — AI handoff

- Convert collected items into a bounded text payload.
- Send the payload through the existing AI memory processor contract.
- Require a 768-value embedding in the response.
- Store the resulting memory in PostgreSQL/pgvector.

### Stage 4 — asynchronous processing

- Move collection and AI processing to a job queue when payload sizes or
  latency justify it.
- Add retry states and idempotency keys before production use.

## Configuration

Extension:

```yaml
oauth2:
  client_id: <google-chrome-extension-oauth-client-id>
  scopes:
    - https://www.googleapis.com/auth/youtube.readonly
```

AI service:

```yaml
impulse:
  ai:
    base-url: ${AI_SERVICE_URL:http://localhost:8001}
    memory-path: ${AI_MEMORY_PATH:/api/v1/memories/process}
```

## Security and operational boundaries

- Only call allow-listed Google API hosts.
- Never fetch arbitrary user-provided URLs from the backend.
- Do not log API keys or full AI payloads.
- Enforce collection-size and text-size limits.
- Use OAuth before supporting private YouTube playlists.
- Treat captions and descriptions as untrusted content.

## Official references

- YouTube `playlistItems.list`:
  https://developers.google.com/youtube/v3/docs/playlistItems/list
- YouTube `playlists.list`:
  https://developers.google.com/youtube/v3/docs/playlists/list
- Instagram Platform:
  https://developers.facebook.com/docs/instagram-platform/
