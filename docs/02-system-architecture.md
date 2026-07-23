# System Architecture

## Product Architecture

### CreatorBrain
- **Functions**: Save Content, Metadata Extraction, AI Understanding, Memory Storage, Search

### Impulse
- **Functions**: Understand user intent, Retrieve memories, Apply filters, Rank memories, Generate recommendations, Explain reasoning

## High‑Level Architecture

```
Chrome Extension → Spring Boot API → Content Processor → Gemini → PostgreSQL + pgvector → React Dashboard
```

## AI Responsibilities

### Memory AI
- Summarization, categorization, tagging, metadata extraction, embedding generation

### Impulse AI
- Intent detection, memory retrieval, recommendation generation, planning, reasoning
