from memory_processor import (
    EMBEDDING_DIMENSIONS,
    ExtractedSource,
    MemoryProcessRequest,
    MemoryProcessor,
    SourceExtractionError,
    SourceExtractor,
    StructuredMemory,
    MemoryAction,
    PlanRequest,
    PlanningMemory,
)


class FakeExtractor:
    def extract(self, request):
        return ExtractedSource(
            title="Weekend ideas",
            description="A saved playlist",
            content="Collection: Weekend ideas\n1. Visit the museum",
        )


class FakeGateway:
    configured = True

    def analyze(self, request, source):
        return StructuredMemory(
            title="Weekend ideas",
            description="A saved playlist",
            summary="Ideas for a weekend plan.",
            category="Travel",
            tags=["Weekend", "Planning", "weekend"],
            topics=["weekend planning"],
            actions=[
                MemoryAction(
                    action="Visit the museum",
                    useWhen=["free Saturday"],
                    durationMinutes=120,
                    category="travel",
                )
            ],
        )

    def embed(self, document):
        return [0.01] * EMBEDDING_DIMENSIONS


def test_processor_returns_backend_contract_with_768_dimensions():
    processor = MemoryProcessor(extractor=FakeExtractor(), gateway=FakeGateway())
    response = processor.process(
        MemoryProcessRequest(
            sourceUrl="https://www.youtube.com/playlist?list=PL123",
            platform="YOUTUBE_PLAYLIST",
            userNote="Plan Saturday",
            content="Collection: Weekend ideas\n1. Visit the museum",
        )
    )

    assert response.title == "Weekend ideas"
    assert response.tags == ["planning", "weekend"]
    assert len(response.embedding) == 768
    assert response.topics == ["weekend planning"]
    assert response.actions[0].action == "Visit the museum"


def test_playlist_requires_collected_content():
    extractor = SourceExtractor()
    request = MemoryProcessRequest(
        sourceUrl="https://www.youtube.com/playlist?list=PL123",
        platform="YOUTUBE_PLAYLIST",
    )

    try:
        extractor.extract(request)
        assert False, "Expected SourceExtractionError"
    except SourceExtractionError:
        pass


def test_accepts_generic_web_source_and_rejects_non_http_scheme():
    request = MemoryProcessRequest(
        sourceUrl="https://example.com/article",
        platform="WEB",
        content="Visible article text",
    )
    assert request.platform == "WEB"

    try:
        MemoryProcessRequest(
            sourceUrl="file:///etc/passwd",
            platform="WEB",
        )
        assert False, "Expected validation failure"
    except ValueError:
        pass


def test_generic_fetch_rejects_local_network_urls():
    extractor = SourceExtractor()
    try:
        extractor._require_public_url("http://127.0.0.1/private")
        assert False, "Expected local network URL rejection"
    except SourceExtractionError:
        pass


def test_local_embedding_fallback_keeps_rag_vector_contract():
    embedding = MemoryProcessor._local_embedding("focused work planning habits")

    assert len(embedding) == EMBEDDING_DIMENSIONS
    assert any(value != 0 for value in embedding)


def test_extractive_plan_fallback_stays_grounded_in_retrieved_memory():
    request = PlanRequest(
        query="Plan my work session",
        memories=[
            PlanningMemory(
                id="memory-1",
                title="Focused work",
                summary="Use distraction-free blocks.",
                category="Productivity",
                sourceUrl="https://example.com/focus",
            )
        ],
    )

    plan = MemoryProcessor._extractive_plan(request)

    assert plan.goal == "Plan my work session"
    assert plan.plan[0].memoryIds == ["memory-1"]
    assert plan.plan[0].sourceType == "MEMORY"


def test_general_starter_fallback_is_explicitly_labelled():
    request = PlanRequest(
        query="Create a study plan",
        memories=[],
        allowGeneralKnowledge=True,
        groundingStatus="NO_GROUNDING",
    )

    plan = MemoryProcessor._extractive_plan(request)

    assert plan.plan
    assert all(step.sourceType == "GENERAL" for step in plan.plan)
    assert all(not step.memoryIds for step in plan.plan)


def test_embedding_document_excludes_full_source_content():
    request = MemoryProcessRequest(
        sourceUrl="https://example.com/large",
        platform="WEB",
        userNote="Keep this",
    )
    source = ExtractedSource(
        title="Large page",
        description="Description",
        content="SENSITIVE_FULL_SOURCE " * 1_000,
    )
    memory = StructuredMemory(
        title="Compact memory",
        summary="Reusable summary",
        category="Learning",
        topics=["retrieval"],
        actions=[MemoryAction(action="Practice retrieval")],
    )

    document = MemoryProcessor._document(request, source, memory)

    assert "Reusable summary" in document
    assert "Practice retrieval" in document
    assert "SENSITIVE_FULL_SOURCE" not in document
