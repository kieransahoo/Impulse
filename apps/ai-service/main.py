from fastapi import FastAPI, HTTPException

from memory_processor import (
    AiConfigurationError,
    MemoryProcessRequest,
    MemoryProcessResponse,
    MemoryProcessor,
    EmbedQueryRequest,
    EmbedQueryResponse,
    PlanRequest,
    PlanResponse,
    SourceExtractionError,
)

app = FastAPI(
    title="Impulse AI Service",
    version="0.1.0",
    description="Converts selected social links and collected playlists into structured memories.",
)
processor = MemoryProcessor()


@app.get("/health")
def health():
    return {
        "status": "UP",
        "geminiConfigured": processor.gemini_configured,
    }


@app.get("/ready")
def ready():
    try:
        processor.verify()
        return {"status": "UP", "geminiReady": True}
    except AiConfigurationError as exception:
        raise HTTPException(status_code=503, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=502, detail="Gemini readiness check failed") from exception


@app.post("/api/v1/memories/process", response_model=MemoryProcessResponse)
def process_memory(request: MemoryProcessRequest):
    try:
        return processor.process(request)
    except AiConfigurationError as exception:
        raise HTTPException(status_code=503, detail=str(exception)) from exception
    except SourceExtractionError as exception:
        raise HTTPException(status_code=422, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=502, detail="AI memory processing failed") from exception


@app.post("/api/v1/embeddings/query", response_model=EmbedQueryResponse)
def embed_query(request: EmbedQueryRequest):
    try:
        return processor.embed_query(request)
    except AiConfigurationError as exception:
        raise HTTPException(status_code=503, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=502, detail="Query embedding failed") from exception


@app.post("/api/v1/plans", response_model=PlanResponse)
def create_plan(request: PlanRequest):
    try:
        return processor.create_plan(request)
    except AiConfigurationError as exception:
        raise HTTPException(status_code=503, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=502, detail="Plan generation failed") from exception
