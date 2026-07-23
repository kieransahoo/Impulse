# Impulse Backend

Simple Kotlin/Spring Boot API backed by PostgreSQL. Hibernate creates and updates
the schema directly; this project intentionally does not use Flyway or Liquibase.

## Run locally

Requirements: Java 21 and Docker.

```bash
docker compose up -d
./gradlew bootRun
```

Build and run the tests with:

```bash
./gradlew clean test
```

The API runs at `http://localhost:8080`. Health is available at
`GET /actuator/health`.

The included pgvector database listens on host port `5433` and uses database
`impulse`, username `postgres`, and password `root`. Start the application with
`DB_URL=jdbc:postgresql://localhost:5433/impulse`, `DB_USERNAME=postgres`, and
`DB_PASSWORD=root`.

## Post API

- `POST /api/posts`
- `GET /api/posts?page=0&size=20`
- `GET /api/posts?category=CAFE`
- `GET /api/posts/{id}`
- `PUT /api/posts/{id}`
- `DELETE /api/posts/{id}`

Example:

```bash
curl -X POST http://localhost:8080/api/posts \
  -H 'Content-Type: application/json' \
  -d '{
    "caption": "Coffee and a slow morning.",
    "image": "https://loremflickr.com/1080/1080/coffee?lock=1",
    "category": "CAFE",
    "tags": ["Cafe", "Coffee"]
  }'
```

Valid categories are `CAFE`, `SHOPPING`, `RECIPE`, `TRAVEL`, and `AI`.

## Memory ingestion API

`POST /api/memories/import` accepts YouTube playlist links and Instagram
post/reel links. It sends the link and optional content to the configured AI
processor, then stores the returned structured memory and embedding in
PostgreSQL using a 768-dimension `pgvector` column.

```bash
curl -X POST http://localhost:8080/api/memories/import \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "11111111-1111-1111-1111-111111111111",
    "url": "https://www.youtube.com/playlist?list=PL123",
    "userNote": "Ideas for my next trip",
    "content": "Optional transcript or extracted source content"
  }'
```

List a user's memories:

```bash
curl 'http://localhost:8080/api/memories?userId=11111111-1111-1111-1111-111111111111'
```

Configure the AI processor with `AI_SERVICE_URL` and, if necessary,
`AI_MEMORY_PATH`. The processor must accept:

```json
{
  "sourceUrl": "https://www.youtube.com/playlist?list=PL123",
  "platform": "YOUTUBE_PLAYLIST",
  "userNote": "Ideas for my next trip",
  "content": "Optional transcript or extracted source content"
}
```

The `embedding` array must contain exactly 768 values. Spring initializes only
the PostgreSQL `vector` extension; Hibernate creates and updates the tables.
There are no Flyway or Liquibase migrations.

## YouTube playlist collection

Set a YouTube Data API key:

```bash
export YOUTUBE_API_KEY="your-key"
```

Collect and store the links from a public playlist:

```bash
curl -X POST http://localhost:8081/api/link-collections \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "11111111-1111-1111-1111-111111111111",
    "url": "https://www.youtube.com/playlist?list=PL123"
  }'
```

The response contains the collection ID and normalized individual video links.
Send that collected content to the AI service and save the returned memory:

```bash
curl -X POST http://localhost:8081/api/link-collections/COLLECTION_ID/process \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "11111111-1111-1111-1111-111111111111",
    "userNote": "Ideas to remember"
  }'
```

Public YouTube playlists are the first automated source. Instagram posts and
Reels can be submitted individually through `/api/memories/import`, but the
Instagram API does not provide general synchronization of a personal user's
saved Reels. See `docs/08-link-ingestion-plan.md` and
`docs/09-ai-memory-contract.md` for the verified boundary and full flow.

It must return:

```json
{
  "title": "Travel ideas",
  "description": "Videos collected for trip planning",
  "summary": "A collection of travel recommendations.",
  "category": "Travel",
  "tags": ["travel", "planning"],
  "embedding": [0.12, -0.08, 0.31]
}
```
