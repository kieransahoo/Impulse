# Impulse: Presentation Context and Cross-Questions

## Two-Minute Presentation Script

### Slide 1 — Product Introduction

Impulse is a personal knowledge and planning application. Users save YouTube videos, Shorts, playlists, Instagram posts or Reels, and web pages through the Chrome extension or Android application. Impulse converts this scattered content into searchable memory that can support grounded answers and plans.

### Slide 2 — Customer Problem and Solution

Students, creators, travelers, and professionals save useful content but often cannot find or apply it later. A bookmark preserves only the link—not its meaning.

Impulse solves this by:

- Capturing the URL, available text, metadata, and user note.
- Using AI to create a compact, structured memory.
- Retrieving the right memories when the user asks a question.
- Generating an answer supported by the user’s saved content.

### Slide 3 — End-to-End Architecture

1. The Chrome extension or Android app sends the URL and available content.
2. The Spring Boot backend authenticates the user and validates the request.
3. The FastAPI AI service extracts available metadata, captions, and visible text.
4. The first LLM creates a title, summary, category, tags, topics, and actions.
5. An embedding model converts the memory’s meaning into 768 numbers.
6. PostgreSQL stores readable data; pgvector stores the embedding.

The application never communicates directly with Gemini or the database. The backend controls the complete flow.

### Slide 4 — LLM and RAG in Simple Words

The first LLM works like a reader: it reads the supplied content and creates clean notes.

The embedding works like a meaning-based fingerprint. Content with similar meaning receives mathematically similar vectors.

RAG works like a librarian:

1. It understands the user’s question.
2. It searches the user’s saved memories.
3. It removes weak, duplicate, or unrelated results.
4. It gives a maximum of six relevant memories to the final LLM.
5. The final LLM answers using that selected evidence.

Before saving AI output, the backend checks that the title, summary, and category are present and that the embedding contains exactly 768 values.

### Slide 5 — Future Scope

Future improvements include:

- Audio transcription for videos and podcasts.
- Selected video-frame and on-screen-text analysis.
- Additional approved content integrations.
- Asynchronous processing, retry handling, and status tracking.
- Faster pgvector search, reranking, and caching.
- Editable AI tags and recommendation feedback.
- Stronger privacy, retention, and explanation controls.

### Slide 6 — Closing

Impulse follows one reusable flow:

> Capture → Understand → Embed → Store → Retrieve → Answer

The result is not another bookmark manager. It is a personal knowledge layer built from content the user already values.

---

# Cross-Questions and Short Answers

## Product Questions

### 1. How is Impulse different from a bookmark manager?

A bookmark manager stores links. Impulse stores the meaning, topics, actions, and vector representation of the content so it can be retrieved and used later.

### 2. Who is the target customer?

People who save large amounts of useful online content—especially students, creators, travelers, researchers, and working professionals.

### 3. What customer problem is being solved?

Saved content becomes difficult to find, remember, and apply. Impulse converts it into searchable knowledge and grounded recommendations.

### 4. Why not ask a general AI tool directly?

A general AI tool does not automatically know the user’s private saved content. Impulse retrieves the user’s relevant memories before generating an answer.

### 5. What is the main product value?

Users can reuse ideas they already discovered without manually reopening and reviewing every saved link.

## Content-Extraction Questions

### 6. Are complete videos currently analyzed?

Not yet. The current system primarily analyzes available metadata, captions, visible page text, thumbnails, user notes, and collected playlist content.

### 7. Why is frame-by-frame video analysis not included now?

It increases cost, latency, storage, and processing complexity. It is planned as a controlled future extension.

### 8. How is YouTube content processed?

The system uses normalized URLs, available page content, YouTube oEmbed metadata, and playlist information collected through the YouTube API.

### 9. How is Instagram content processed?

The extension supplies visible content when available. Public Open Graph metadata can be used as a bounded fallback.

### 10. Does the backend scrape every submitted URL?

No. It validates sources and uses allow-listed or controlled extraction paths. Arbitrary unsafe URL fetching is restricted.

## AI and Tag Questions

### 11. What does the first LLM generate?

It generates:

- Title
- Description
- Summary
- Category
- Tags
- Topics
- Suggested actions

### 12. What is the difference between tags and topics?

Tags are short searchable labels. Topics describe the broader subjects or themes represented in the content.

### 13. What are actions?

Actions are practical steps extracted from the content, such as “visit this location,” “try this workflow,” or “use this study method.”

### 14. Can the LLM generate incorrect information?

Yes. AI output can be imperfect. The system limits risk through structured output, backend validation, source references, relevance filtering, and future user-editing controls.

### 15. Does backend validation prove that the AI answer is factually correct?

No. Contract validation checks structure and compatibility—not factual truth. Grounding and source visibility help users evaluate the answer.

## Embedding and Database Questions

### 16. What is an embedding?

An embedding is a list of numbers representing semantic meaning. Similar ideas produce vectors that are close together mathematically.

### 17. Why are there exactly 768 values?

The selected embedding model and backend contract use a fixed 768-dimensional vector. A fixed size is required for consistent storage and comparison.

### 18. What is stored in PostgreSQL?

PostgreSQL stores user ownership, URL, platform, title, description, thumbnail, note, summary, category, tags, topics, actions, timestamps, collections, and saved-plan data.

### 19. What is stored in pgvector?

Pgvector stores the 768-dimensional embedding associated with each structured memory.

### 20. Is pgvector a separate database?

No. Pgvector is a PostgreSQL extension that adds vector storage and similarity-search capabilities.

### 21. Is the complete post stored inside the vector?

No. The vector is only a numeric representation of meaning. Readable fields remain in PostgreSQL.

### 22. Why not store only embeddings?

Embeddings are useful for similarity search but are not readable or sufficient for explanations, filtering, auditing, or display.

## RAG and Retrieval Questions

### 23. What does RAG mean?

Retrieval-Augmented Generation. The system retrieves relevant saved memories before asking the LLM to generate an answer.

### 24. How are memories selected?

The backend combines vector similarity, keyword matches, topics, actions, recency, and relevance thresholds.

### 25. Why use both keywords and vector search?

Keywords find exact terms; vectors find similar meaning. Combining both produces stronger results.

### 26. What filters are applied?

The backend filters by user ownership, relevance, duplicate URL, matching fields, and the maximum context limit.

### 27. Why are only six memories sent to the final LLM?

It keeps the context focused, reduces token usage, lowers latency, and prevents unrelated memories from weakening the answer.

### 28. What happens when no relevant memory is found?

The system reports missing context rather than presenting an unrelated saved memory as evidence. General knowledge can be used only when explicitly allowed.

### 29. What is the difference between the first and final LLM calls?

The first LLM understands and structures newly saved content. The final LLM uses retrieved memories to answer a user’s question or create a plan.

### 30. Is RAG the same as training the LLM?

No. RAG supplies relevant context at request time. It does not retrain or permanently modify the model.

## Backend and Validation Questions

### 31. Why is the backend required?

It controls authentication, user isolation, validation, AI calls, persistence, retrieval, filtering, and error handling.

### 32. What is AI contract validation?

The backend confirms that required fields are present and the embedding contains exactly 768 values before the memory is stored.

### 33. What happens if AI contract validation fails?

The request fails and incomplete or incompatible AI output is not saved.

### 34. What is validated before calling the AI service?

The backend checks the user, source type, URL format, allowed platform, payload size, and required input fields.

### 35. What is validated before generating an answer?

The backend ensures memories belong to the requesting user, removes duplicates and weak matches, and limits the final context.

### 36. Does the mobile application call the AI service directly?

No. It communicates with the Spring Boot backend, which controls AI-service access.

## Security and Privacy Questions

### 37. How is user data isolated?

Memories and retrieval queries are scoped to the authenticated user. One user’s memories should not be included in another user’s search.

### 38. Are API keys stored in the extension or Android app?

No. Provider and database secrets remain in backend-controlled services.

### 39. Can private content be collected automatically?

Only through approved access and explicit user consent. Unsupported authenticated scraping is outside the current scope.

### 40. Can prompt injection exist inside saved content?

Yes. Source text is treated as untrusted data. It is bounded and used as context, not as trusted system instructions.

## Scalability and Future Questions

### 41. Will the current retrieval approach scale indefinitely?

No. Higher volume will require indexed approximate-nearest-neighbor search, reranking, caching, and asynchronous processing.

### 42. What happens if the AI provider is unavailable?

Ingestion can return an error or use the implemented conservative fallback where applicable. Production expansion should add retries, job states, and monitoring.

### 43. Can embeddings become outdated?

Yes. Future model changes may require controlled re-embedding and vector-index maintenance.

### 44. What are the most important next features?

Audio transcription, selected frame analysis, asynchronous ingestion, faster retrieval, user-editable tags, and stronger feedback controls.

### 45. What is the biggest technical risk?

Maintaining relevant, trustworthy retrieval while controlling AI cost, latency, privacy, and incorrect model output.

### 46. What is the biggest product risk?

Users may save content but not return to use it. The product must make retrieval and planning immediately valuable.

## Strong Closing Answer

### 47. Explain the entire application in one sentence.

Impulse securely converts saved online content into structured memories and embeddings, retrieves the most relevant memories for each question, and gives the final LLM grounded personal context.

