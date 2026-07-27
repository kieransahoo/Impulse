# Impulse: Complete End-to-End Internal Working

## Purpose of This Document

This document explains, in simple language, what happens from the moment a user saves a URL until Impulse creates a personalized plan.

It explains:

- What the Chrome extension and Android app do.
- What the backend does.
- What information is sent to the AI service.
- How the first LLM analyzes content.
- What an embedding is.
- What is stored in PostgreSQL and pgvector.
- How saved memories are searched.
- How RAG works.
- How the final plan is created and validated.
- What happens when something fails or no useful memory is found.

---

# 1. The Complete Flow in One View

```mermaid
flowchart LR
    A["User saves a URL"] --> B["Extension or Android app"]
    B --> C["Spring Boot backend"]
    C --> D["Validate and normalize URL"]
    D --> E["FastAPI AI service"]
    E --> F["Extract available content"]
    F --> G["First LLM analyzes content"]
    G --> H["Create 768-value embedding"]
    H --> I["Store memory in PostgreSQL + pgvector"]
    I --> J["User asks for a plan"]
    J --> K["Create query embedding"]
    K --> L["Search and rank saved memories"]
    L --> M["Send best memories to final LLM"]
    M --> N["Validate and return plan"]
```

In simple terms:

> The application collects a link, the backend turns it into a clean memory, the database stores it, RAG finds the right memories later, and the final LLM uses them to create a plan.

---

# Part A — Saving and Processing a URL

## 2. The User Saves a URL

The process begins when a user finds useful content, such as:

- A YouTube video.
- A YouTube Short.
- A YouTube playlist.
- An Instagram post.
- An Instagram Reel.
- A normal web article or page.

The user can save it through:

- The Impulse Chrome extension.
- The Android sharing flow.
- Another Impulse application screen connected to the backend.

The user may also add a note, for example:

> “Use this video while planning my Goa trip.”

This note helps the system understand why the content matters to the user.

---

## 3. The Extension or Android App Collects Basic Information

The client application does not perform the main AI processing.

Its job is to collect and send available information to the backend.

### Typical information collected

| Field | Meaning | Example |
|---|---|---|
| `url` | The content address | `https://youtube.com/watch?v=...` |
| `platform` | Where the content came from | YouTube, Instagram, or web |
| `contentType` | Type of item | Video, Short, Reel, post, article |
| `title` | Available page title | “Three-day Goa itinerary” |
| `description` | Available description or caption | Creator-provided text |
| `thumbnailUrl` | Preview-image address | YouTube thumbnail |
| `content` | Visible or collected page content | Caption, page text, playlist items |
| `userNote` | User’s personal note | “Use this for December trip” |

### What the client does with the URL

The client:

1. Identifies the platform.
2. Extracts the YouTube video ID or Instagram shortcode when applicable.
3. Creates a consistent canonical URL.
4. Removes known tracking parameters.
5. Sends the cleaned information to the backend.

### Why URL cleaning matters

These two URLs may represent the same content:

```text
https://example.com/article?utm_source=email
https://example.com/article
```

Removing tracking information helps prevent the same content from being stored multiple times.

---

## 4. Temporary Client-Side Storage

The Chrome extension can temporarily store:

- A local saved-content library.
- The user identifier.
- Backend configuration.
- Items waiting to be synchronized.
- Failed requests that should be retried.

This temporary storage is not the main AI memory database.

The main permanent memory is owned by the Spring Boot backend and PostgreSQL.

---

## 5. The Client Sends the Request to the Spring Boot Backend

The client sends a request similar to:

```json
{
  "userId": "user-uuid",
  "url": "https://www.youtube.com/watch?v=example",
  "userNote": "Use this for my travel plan",
  "content": "Optional visible text or collected content"
}
```

The application does **not** directly call:

- Gemini.
- PostgreSQL.
- pgvector.

Everything goes through the backend.

### Why the backend is placed in the middle

The backend provides one trusted control point for:

- Authentication.
- User isolation.
- URL validation.
- Duplicate checking.
- AI-service access.
- Database access.
- Error handling.
- Data validation.
- Retrieval and plan generation.

---

## 6. The Backend Parses and Normalizes the URL

The backend identifies the source as one of:

- `YOUTUBE_PLAYLIST`
- `YOUTUBE_VIDEO`
- `INSTAGRAM`
- `WEB`

It then creates a normalized URL.

For example, different YouTube URL formats can be converted into one standard format.

### The backend rejects invalid input

The backend can reject:

- Missing URLs.
- Unsupported URLs.
- Invalid URL formats.
- Content that is too large.
- Unsupported platform values.
- Requests without a valid user.

---

## 7. The Backend Checks for Duplicates

Before running an expensive AI request, the backend asks:

> “Has this user already saved this normalized URL?”

The duplicate check uses:

- The user ID.
- The normalized source URL.

If the same user has already stored the same URL, the backend can:

- Return the existing memory in a shared-import flow.
- Reject it as a duplicate in a normal import flow.

This prevents:

- Repeated AI processing.
- Duplicate memories.
- Unnecessary embedding generation.
- Wasted API cost.

---

## 8. The Backend Calls the FastAPI AI Service

After validation, the Spring Boot backend sends a controlled request to the FastAPI AI service.

The AI request contains:

```json
{
  "sourceUrl": "normalized URL",
  "platform": "YOUTUBE_VIDEO",
  "userNote": "Optional note",
  "content": "Optional visible or collected content"
}
```

The AI service has two main responsibilities:

1. Obtain usable source information.
2. Convert that information into a structured memory and embedding.

---

## 9. The AI Service Chooses How to Obtain Content

The extraction method depends on the platform and the content supplied by the application.

### Case A — Visible content was supplied

If the extension or application already supplied useful content, the AI service uses it first.

This can include:

- Visible captions.
- Page text.
- Playlist titles and descriptions.
- Metadata available in the browser.
- User-supplied content.

This is useful because some platforms do not provide complete public data through an open API.

### Case B — YouTube video or Short

The AI service can use YouTube’s public oEmbed endpoint to obtain information such as:

- Video title.
- Creator name.
- Thumbnail.
- Source URL.

### Case C — YouTube playlist

Playlist items must be collected before AI processing.

The collected data can include:

- Playlist-item order.
- Video title.
- Video URL.
- Description.
- Thumbnail.

The backend sends a bounded text representation of the collected playlist to the AI service.

### Case D — Instagram post or Reel

The preferred input is visible content supplied by the extension.

If that is unavailable, the service may attempt to use bounded public metadata, such as:

- Open Graph title.
- Open Graph description.
- Public thumbnail.

The backend does not support unrestricted scraping of authenticated Instagram pages.

### Case E — General web page

The service can read a controlled amount of public HTML and extract:

- Page title.
- Meta description.
- Open Graph metadata.
- Visible text.
- Thumbnail metadata.

Safety checks are used to avoid fetching private or unsafe network addresses.

---

## 10. Current Video-Analysis Boundary

The current system primarily understands:

- Titles.
- Descriptions.
- Captions supplied by the page or extension.
- Visible page text.
- Thumbnails.
- Playlist metadata.
- User notes.

The current production path does **not** fully analyze:

- Every video frame.
- The complete audio track.
- Full speech transcription.
- Every visual object appearing in a video.

Audio transcription and selected-frame analysis are future extensions.

This distinction is important:

> The system currently analyzes the information available around the video, not necessarily the entire raw video itself.

---

## 11. The First LLM Converts Content into a Structured Memory

The extracted text is not stored as an unorganized block and immediately reused for every question.

Instead, the first LLM reads it once and produces a compact structured memory.

The first LLM returns:

| Output | Simple meaning |
|---|---|
| `title` | A clean name for the memory |
| `description` | Supporting source information |
| `thumbnailUrl` | Preview-image URL |
| `summary` | Short explanation of the important content |
| `category` | Main type of content |
| `tags` | Short searchable labels |
| `topics` | Larger subjects discussed |
| `actions` | Practical things the user can do |

### Example

Suppose the user saves a video about a three-day Goa trip.

The AI result may look like:

```json
{
  "title": "Three-day Goa itinerary",
  "summary": "A compact itinerary covering North Goa, South Goa, beaches and local food.",
  "category": "Travel",
  "tags": ["Goa", "beaches", "three-day trip"],
  "topics": ["travel planning", "local food", "sightseeing"],
  "actions": [
    {
      "action": "Visit North Goa beaches on day one",
      "useWhen": ["planning a short Goa trip"],
      "durationMinutes": 480,
      "category": "Travel"
    }
  ]
}
```

### Why analyze the source only once

Without this step, the system would need to send the full source content to an LLM every time the user asks a question.

Analyzing once:

- Reduces repeated cost.
- Reduces response time.
- Creates consistent searchable fields.
- Keeps later prompts smaller.
- Makes the memory reusable.

---

## 12. The AI Service Creates the Memory Embedding

After the structured memory is created, the service builds a compact text representation using fields such as:

- Title.
- Summary.
- Category.
- Tags.
- Topics.
- Actions.

This compact text is sent to an embedding model.

The embedding model returns exactly **768 floating-point numbers**.

Example:

```text
[0.021, -0.117, 0.083, ... 765 more values]
```

### What an embedding means

An embedding is a mathematical representation of meaning.

It is similar to a meaning-based fingerprint.

Content about:

- Goa travel.
- Beach itineraries.
- Weekend vacations.

will normally be closer in vector space than content about:

- Java programming.
- Weight training.
- Interior design.

The embedding does not contain readable sentences. It helps the system compare meanings.

---

## 13. AI Contract Validation

The Spring Boot backend does not blindly trust the AI response.

Before saving, it checks that:

- `title` is not blank.
- `summary` is not blank.
- `category` is not blank.
- The embedding contains exactly 768 values.
- The response can be converted into the expected data types.

It also cleans returned data:

- Removes blank tags.
- Removes blank topics.
- Removes actions without action text.
- Trims unnecessary spaces.

### Important limitation

Contract validation checks that the AI response is structurally usable.

It does **not** prove that every AI-generated statement is factually correct.

Source visibility, grounded retrieval, and future user-editing features help manage factual risk.

---

## 14. What Is Stored in PostgreSQL

The main memory record is stored in the `memories` table.

### Identity and ownership

| Field | Meaning |
|---|---|
| `id` | Unique memory ID |
| `user_id` | Owner of the memory |
| `created_at` | Time the memory was created |

### Source information

| Field | Meaning |
|---|---|
| `source_url` | Normalized original URL |
| `platform` | YouTube, Instagram, playlist, or web |
| `title` | Clean memory title |
| `description` | Source description |
| `thumbnail_url` | Preview image |
| `user_note` | User’s personal note |

### AI-analyzed meaning

| Field | Meaning |
|---|---|
| `summary` | Compact explanation |
| `category` | Main content type |
| `tags` | Short labels |
| `topics` | Broader subjects |
| `actions` | Practical steps extracted from the source |

### Separate supporting collections

The implementation also stores:

- Memory tags.
- Memory topics.
- Memory actions.
- User-created collections.
- Collected links or playlists.
- Saved plans and their plan steps.

---

## 15. What Is Stored in pgvector

The embedding is stored in a PostgreSQL column with this type:

```text
vector(768)
```

Pgvector is not a completely separate database.

It is a PostgreSQL extension that allows PostgreSQL to store and compare vectors.

### PostgreSQL versus pgvector

| PostgreSQL data | pgvector data |
|---|---|
| Human-readable fields | 768 numeric values |
| Title and summary | Semantic meaning |
| Tags and topics | Used for similarity comparison |
| Actions and source URL | Not intended for direct reading |
| User ownership | Connected to the same memory |

The vector is not the complete post.

It is only the numeric meaning representation attached to the memory.

---

## 16. The Memory Is Now Ready

At this point, the system has completed the ingestion flow.

The saved memory can now be:

- Displayed in the application.
- Added to a collection.
- Deleted by the user.
- Retrieved for a future question.
- Used as evidence for a plan.

---

# Part B — Searching Memories and Creating a Plan

## 17. The User Asks for a Plan

The user enters a question such as:

> “Create a three-day Goa plan using the travel content I saved.”

The request can contain:

- User ID.
- Natural-language question.
- Additional constraints.
- A flag allowing or disallowing general knowledge.

Example constraints:

```json
{
  "budget": "₹20,000",
  "days": 3,
  "travelStyle": "relaxed"
}
```

---

## 18. The Backend Detects the User’s Intent

Before calling the final LLM, the backend performs a lightweight intent check.

It looks for words related to common planning categories:

- Study.
- Learning.
- Workout.
- Meal.
- Room design.
- Product comparison.
- Project.
- Routine.
- General planning.

For example:

```text
"Create a workout plan" → WORKOUT
"Help me revise for an exam" → STUDY
"Compare products I saved" → PRODUCT
```

This does not require an LLM call.

It is a simple backend keyword check.

### Why intent detection is useful

It helps the backend:

- Understand the type of plan.
- Check whether important information is missing.
- Suggest useful source types.
- Give the final LLM clearer instructions.

---

## 19. The Backend Checks for Missing Context

Different plans require different information.

Examples:

### Study plan

- Current skill level.
- Deadline.
- Available study time.

### Workout plan

- Training experience.
- Available equipment.
- Days per week.
- Physical limitations.

### Meal plan

- Dietary preference.
- Allergies.
- Budget.

### Room plan

- Budget.
- Room size.
- Preferred style.

The backend checks whether this information appears:

- In the question.
- In the supplied constraints.

Missing items are returned to the user as `missingContext`.

---

## 20. The Question Is Converted into an Embedding

The backend sends the user’s question to the AI service’s query-embedding endpoint.

The question:

> “Create a relaxed three-day Goa trip”

is converted into another 768-value vector.

The memory embeddings and question embedding have the same size, so they can be compared.

---

## 21. The Backend Loads Only That User’s Memories

The retrieval process is scoped by `user_id`.

This means:

- User A searches only User A’s memories.
- User B searches only User B’s memories.

The system must not mix memories between users.

User ownership filtering is one of the most important backend safety rules.

---

## 22. Every Candidate Memory Receives Multiple Scores

The current MVP backend scores each of the user’s memories using four signals.

### 1. Semantic similarity — 60%

The backend compares:

- The question embedding.
- The saved memory embedding.

It uses cosine similarity.

In simple terms:

> “How close are these two meanings?”

### 2. Keyword match — 20%

The backend checks whether important words from the question appear in:

- Title.
- Summary.
- Category.
- Tags.
- Topics.
- Actions.
- Action usage conditions.

In simple terms:

> “Do the memory and question use matching words?”

### 3. Recency — 15%

Newer memories receive a small advantage.

This does not completely replace older relevant memories. It only adds a recency signal.

### 4. Action availability — 5%

A memory receives a small boost when it contains practical actions.

This is useful because plans need actionable information.

### Complete scoring formula

```text
Final score =
60% semantic similarity
+ 20% keyword match
+ 15% recency
+ 5% action availability
```

---

## 23. The Relevance Filter Removes Weak Results

The backend does not automatically use the highest-scoring memory if the score is still poor.

A memory must pass the relevance rules:

- Overall score must be at least `0.30`.
- Semantic similarity must be at least `0.25`, or there must be a keyword match.

This is called the relevance gate.

### Why the relevance gate matters

Without it, a travel question might receive the “best” available memory even when the user has saved only programming content.

The gate allows the system to say:

> “No relevant saved memory was found.”

instead of inventing a false connection.

---

## 24. Duplicate Sources Are Removed

The backend removes repeated memories with the same source URL.

This prevents one repeated source from dominating the final context.

---

## 25. A Maximum of Six Memories Is Selected

After ranking, filtering, and deduplication, the backend selects at most six memories.

### Why only six?

A small context:

- Keeps the final LLM focused.
- Reduces token usage.
- Reduces latency.
- Reduces cost.
- Makes source attribution clearer.
- Prevents unrelated content from confusing the answer.

The final LLM does not receive every memory the user has saved.

It receives only the most relevant compact memories.

---

## 26. Grounding Strength Is Calculated

The backend labels the retrieval result as:

- `STRONG_GROUNDING`
- `PARTIAL_GROUNDING`
- `NO_GROUNDING`

### Strong grounding

The result is considered strongly grounded when:

- At least three memories were retrieved.
- The best score is at least `0.45`.

### Partial grounding

Some relevant memories were found, but the evidence was limited.

### No grounding

No memory passed the relevance gate.

---

## 27. What Happens When No Relevant Memory Is Found

If no memory is found and general knowledge is not allowed:

- The backend does not call the final planner.
- It returns an empty plan.
- It explains that no relevant saved memory was found.
- It returns missing context.
- It suggests useful sources the user can save.

Example:

```text
No relevant saved memories were found.
Add related sources or choose a general starter plan.
```

This prevents the application from pretending that an answer came from the user’s memory.

---

## 28. Optional General-Knowledge Mode

The user may explicitly set:

```text
allowGeneralKnowledge = true
```

In this mode, the LLM can create general steps even when saved-memory evidence is missing.

These steps are marked as:

```text
GENERAL
```

They are not presented as evidence from a saved memory.

---

## 29. The Backend Builds a Small RAG Context

RAG means:

> Retrieval-Augmented Generation

Before asking the final LLM to generate the plan, the backend prepares a compact package containing:

- The user’s question.
- Constraints.
- Detected intent.
- Grounding status.
- Missing context.
- Up to six retrieved memories.

For each memory, the final LLM receives only important fields:

- Memory ID.
- Title.
- Summary.
- Category.
- Topics.
- Actions.
- Source URL.

It does not need the complete original page body for every plan request.

---

## 30. The Final LLM Creates the Plan

The final LLM receives the prepared RAG context.

Its job is to produce:

- A clear goal.
- A short explanation.
- A list of plan steps.

Each plan step can contain:

| Field | Meaning |
|---|---|
| `step` | What the user should do |
| `durationMinutes` | Estimated time |
| `reason` | Why the step is useful |
| `memoryIds` | Saved memories supporting the step |
| `sourceType` | `MEMORY` or `GENERAL` |

Example:

```json
{
  "step": "Spend the first morning visiting North Goa beaches",
  "durationMinutes": 240,
  "reason": "This follows the itinerary in the saved Goa travel memory.",
  "memoryIds": ["valid-memory-uuid"],
  "sourceType": "MEMORY"
}
```

---

## 31. The Backend Validates the Final Plan

The backend does not accept every memory ID returned by the LLM.

For each plan step, it checks:

1. Is the memory ID a valid UUID?
2. Was that memory actually retrieved for this request?
3. Does the step have valid saved-memory evidence?

If an ID was not part of the retrieved set, it is removed.

If general knowledge is not allowed and a step has no valid memory evidence, that step is removed.

### Why this is important

An LLM could accidentally:

- Return an invalid ID.
- Mention a memory that was not retrieved.
- Present a general statement as saved-memory evidence.

The backend prevents those results from being trusted automatically.

---

## 32. The Backend Returns the Final Response to the App

The final response contains:

- Detected intent.
- Grounding status.
- Goal.
- Explanation.
- Plan steps.
- IDs of retrieved memories.
- Source-memory details.
- Missing context.
- Suggested sources.

The application can show:

- The plan.
- Step duration.
- Reasons.
- Sources that influenced the answer.
- Whether the plan was strongly or partially grounded.

---

## 33. Saving a Plan

The user can save the generated plan.

A saved plan is an immutable snapshot.

This means:

- The original saved plan does not silently change.
- The plan keeps its original goal and steps.
- The user can see what was generated at that time.

If the user regenerates the plan:

- The original goal is reused.
- Retrieval runs again.
- New memories may be included.
- Deleted or changed memory availability can affect the new result.

---

# Part C — Simple Example from Start to Finish

## 34. Example: Saving a Goa Video and Creating a Plan

### Step 1 — User saves a video

The user saves:

```text
https://youtube.com/watch?v=goa-example
```

and adds:

```text
“Use this for a relaxed three-day trip.”
```

### Step 2 — Extension collects information

It collects:

- URL.
- Video ID.
- Title.
- Thumbnail.
- Visible description.
- User note.

### Step 3 — Backend validates the request

It checks:

- URL format.
- Platform.
- User.
- Duplicate URL.
- Payload size.

### Step 4 — AI service obtains source information

It uses supplied visible content or YouTube metadata.

### Step 5 — First LLM creates a memory

It produces:

- Travel summary.
- `Travel` category.
- Tags such as `Goa`, `beach`, and `three-day trip`.
- Topics such as `travel planning`.
- Actions such as visiting North Goa beaches.

### Step 6 — Embedding is created

The compact travel memory becomes 768 numbers representing its meaning.

### Step 7 — Database saves the memory

PostgreSQL stores the readable fields.

Pgvector stores the 768-value embedding.

### Step 8 — User asks a question

```text
“Create a relaxed three-day Goa plan under ₹20,000.”
```

### Step 9 — The question becomes an embedding

The question receives its own 768-value meaning vector.

### Step 10 — Backend searches the user’s memories

It checks:

- Semantic meaning.
- Matching words.
- Recency.
- Available actions.

### Step 11 — Weak memories are removed

Programming or workout memories do not pass the travel relevance gate.

### Step 12 — Best memories are sent to the final LLM

The final LLM receives only the relevant Goa and travel memories.

### Step 13 — Final LLM builds a plan

It generates:

- Day-by-day steps.
- Time estimates.
- Reasons.
- Supporting memory IDs.

### Step 14 — Backend validates the plan

It removes invalid or unsupported memory references.

### Step 15 — App displays the grounded plan

The user sees the final plan and the saved sources that influenced it.

---

# Part D — What Each Part of the System Owns

## 35. Chrome Extension

Responsible for:

- Detecting supported URLs.
- Reading available page metadata.
- Capturing visible content.
- Normalizing common links.
- Sending content to the backend.
- Maintaining a temporary retry queue.

Not responsible for:

- Storing permanent embeddings.
- Directly calling Gemini.
- Searching PostgreSQL.
- Creating the final plan.

---

## 36. Android Application

Responsible for:

- User-facing save and share flows.
- Login and session handling.
- Displaying memories.
- Accepting planning questions.
- Showing plans and source information.

Not responsible for:

- Direct database access.
- Direct provider-secret access.
- On-device RAG processing.

---

## 37. Spring Boot Backend

Responsible for:

- Authentication and user ownership.
- URL parsing and normalization.
- Duplicate checks.
- AI-service calls.
- AI contract validation.
- Transactional database saving.
- Query preparation.
- Intent detection.
- Missing-context detection.
- Memory retrieval and scoring.
- Relevance filtering.
- RAG-context construction.
- Final-plan validation.
- Saved-plan management.

---

## 38. FastAPI AI Service

Responsible for:

- Controlled source extraction.
- First-LLM memory analysis.
- Memory embedding creation.
- Query embedding creation.
- Final-LLM plan generation.
- Conservative fallback behavior where implemented.

---

## 39. PostgreSQL and pgvector

Responsible for:

- Permanent user data.
- Structured memories.
- Tags, topics, and actions.
- Collections.
- Saved plans.
- 768-value embeddings.

The current MVP stores embeddings in pgvector but performs the final scoring logic in the backend over the requesting user’s memories.

At higher scale, vector-indexed nearest-neighbor database search can replace the current in-memory scoring approach.

---

# Part E — Failure and Safety Behaviour

## 40. If the URL Is Invalid

The backend rejects it before AI processing.

## 41. If the URL Is Already Saved

The backend returns the existing memory or reports a duplicate, depending on the import flow.

## 42. If Source Content Cannot Be Read

The AI service returns an extraction error instead of creating an empty memory.

## 43. If the AI Service Is Unavailable

The backend returns an AI-processing error.

Production expansion should add:

- Asynchronous jobs.
- Retry policies.
- Processing states.
- Monitoring.
- Idempotency keys.

## 44. If the AI Response Is Incomplete

The backend rejects it and does not store the memory.

## 45. If the Embedding Has the Wrong Size

The backend rejects it because all memory and query embeddings must use the same 768-value format.

## 46. If No Relevant Memory Is Found

The backend returns no grounded plan unless the user explicitly allows general knowledge.

## 47. If the Final LLM Returns a False Memory ID

The backend removes it because the ID was not part of the retrieved memory set.

---

# Part F — Current Limitations and Future Improvements

## 48. Current Limitations

- Full audio transcription is not yet part of the main ingestion flow.
- Full frame-by-frame video understanding is not yet implemented.
- Some platform data depends on visible content supplied by the extension.
- Current MVP retrieval scores a user’s memories in the backend.
- AI contract validation does not guarantee factual correctness.
- Large-scale asynchronous processing requires additional infrastructure.

## 49. Future Improvements

### Better content understanding

- Audio transcription.
- Selected frame analysis.
- On-screen-text recognition.
- Multimodal combination of speech, text, and images.

### Better processing

- Background job queue.
- Retry and failure states.
- Idempotent ingestion.
- Progress updates.

### Better retrieval

- Pgvector approximate-nearest-neighbor indexes.
- Reranking models.
- Query-result caching.
- Controlled re-embedding.

### Better user control

- Edit AI-generated tags.
- Edit topics and actions.
- Give feedback on recommendations.
- Explain why each source was selected.
- Manage data retention and privacy.

---

# 50. Final Summary

Impulse uses two separate AI stages.

## Stage 1 — Understand and store

```text
URL
→ available content
→ first LLM
→ structured memory
→ 768-value embedding
→ PostgreSQL + pgvector
```

## Stage 2 — Retrieve and plan

```text
User question
→ query embedding
→ score personal memories
→ remove weak and duplicate results
→ select up to six memories
→ final LLM
→ validate memory references
→ return grounded plan
```

The most important principle is:

> The LLM does not search the complete database by itself. The backend first retrieves and filters the right memories, then gives only that controlled context to the final LLM.

