# Browser Extension Context

## Purpose

The Manifest V3 Chrome extension is the user's lightweight capture surface.
It captures only links the user explicitly selects and sends them to the
Spring Boot backend.

## User flow

1. User opens YouTube or Instagram.
2. Extension reads the visible page URL, title, platform, and a heuristic
   signed-in indicator.
3. User confirms the link and may add a note.
4. Extension classifies the link:
   - YouTube playlist → Google OAuth collection, then
     `POST /api/link-collections/import`
   - YouTube video/Short → `POST /api/memories/import`
   - Instagram post/Reel → `POST /api/memories/import`
5. The extension shows backend progress or error details.
6. The Memories tab reads `GET /api/memories?userId=...`.

For playlists, steps 4–5 are one user action: collect through Google OAuth,
import the normalized links, call collection processing, and persist the final
AI memory.

## Component responsibilities

### `content.js`

- Runs only on YouTube and Instagram.
- Reads visible page context.
- Uses DOM heuristics to indicate sign-in state.
- Never reads cookies, passwords, access tokens, or browser history.

### `popup.js`

- Classifies and validates the selected URL.
- Stores local backend URL and development user ID.
- Routes requests to the correct backend endpoint.
- Requests read-only Google OAuth consent for playlist collection.
- Renders collection, memory, and error states.

### `background.js`

- Performs allow-listed backend requests using extension host permissions.
- Returns parsed responses to the popup.

### Backend

- Re-validates every URL; extension validation is never trusted.
- Accepts playlist data collected by the OAuth-authorized extension.
- Sends individual links or collected playlist content to the AI service.
- Stores final embeddings in PostgreSQL/pgvector.

## Authentication boundary

The current user ID is a development identifier stored in extension-local
storage. It is not authentication. Production must replace it with a backend
session or signed access token.

Visible sign-in detection is informational only. Playlist access uses explicit
Google OAuth consent and the read-only YouTube scope; website cookies are never
read or forwarded. A Google OAuth client ID is required, but a YouTube API key
is not.
Instagram saved-Reel enumeration is not available through the general
Instagram API, so the MVP accepts explicit user-selected Reel URLs.

## Planning UI

The Plan tab is present as a product placeholder. It must call a future
authenticated recommendation endpoint that retrieves only the current user's
memories, performs hybrid RAG retrieval, and returns cited memory IDs.

## Local installation

Load `apps/extension` as an unpacked extension from `chrome://extensions`.
No build step is required.
