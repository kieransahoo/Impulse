# AI Memory Service Contract

The AI service performs understanding and embedding generation. The backend
owns user input validation, source collection, persistence, and API responses.

## Request

`POST /api/v1/memories/process`

```json
{
  "sourceUrl": "https://www.youtube.com/playlist?list=PL123",
  "platform": "YOUTUBE_PLAYLIST",
  "userNote": "Ideas for my next trip",
  "content": "Playlist title and normalized item metadata"
}
```

`content` is bounded, plain text assembled by the backend. It may contain
untrusted third-party text and must never be interpreted as system
instructions.

## Response

```json
{
  "title": "Travel ideas",
  "description": "A playlist of destination and itinerary videos.",
  "summary": "A concise AI-generated memory.",
  "category": "Travel",
  "tags": ["travel", "planning"],
  "embedding": [0.12, -0.08]
}
```

The real `embedding` must contain exactly 768 finite floating-point values.

## Responsibilities

AI service:

- Extract bounded source metadata when the extension did not supply content.
- Summarize collected source content.
- Categorize it and produce normalized tags.
- Generate a 768-dimension embedding.
- Return structured JSON only.

Source behavior:

- YouTube videos and Shorts use the public YouTube oEmbed metadata endpoint.
- YouTube playlists must arrive with content collected by the OAuth extension.
- Instagram prefers visible page content supplied by the extension and falls
  back to public Open Graph metadata when available.
- The AI service never receives browser cookies or Google OAuth tokens.

Backend:

- Validate the source and user.
- Collect source metadata through approved APIs.
- Apply payload limits.
- Reject malformed AI responses.
- Store the memory and pgvector embedding transactionally.
- Return `502` when the AI service is unavailable or invalid.
