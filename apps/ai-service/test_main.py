from fastapi.testclient import TestClient

import main
from memory_processor import MemoryProcessResponse


class FakeProcessor:
    gemini_configured = True

    def process(self, request):
        return MemoryProcessResponse(
            title="Saved idea",
            description="A selected video",
            summary="A useful idea saved for later planning.",
            category="General",
            tags=["idea", "planning"],
            embedding=[0.01] * 768,
        )


def test_memory_endpoint_matches_backend_contract(monkeypatch):
    monkeypatch.setattr(main, "processor", FakeProcessor())
    client = TestClient(main.app)

    response = client.post(
        "/api/v1/memories/process",
        json={
            "sourceUrl": "https://www.youtube.com/watch?v=abc",
            "platform": "YOUTUBE_VIDEO",
            "userNote": "Remember this",
            "content": "A selected video about planning.",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["title"] == "Saved idea"
    assert len(body["embedding"]) == 768
