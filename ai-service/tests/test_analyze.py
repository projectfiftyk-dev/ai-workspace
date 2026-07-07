from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient

from main import app
from provider_factory import get_llm_provider
from providers.gemini_provider import GeminiProvider

client = TestClient(app)

SAMPLE_REQUEST = {
    "sourceId": "src-123",
    "messages": [
        {
            "author": "Alice",
            "content": "Let's ship the release by Friday.",
            "sentAt": "2026-07-01T10:00:00Z",
        },
        {
            "author": "Bob",
            "content": "I'll write the migration tests before then.",
            "sentAt": "2026-07-01T10:01:00Z",
        },
    ],
}

VALID_JSON_RESPONSE = """{
  "summary": "Alice and Bob agreed to ship the release by Friday; Bob will write migration tests.",
  "actionItems": ["Bob writes the migration tests"],
  "decisions": ["Ship the release by Friday"],
  "deadlines": ["Friday - release ship date"]
}"""


@pytest.fixture
def gemini_provider(monkeypatch):
    monkeypatch.setenv("GEMINI_API_KEY", "test-key")
    with patch("providers.gemini_provider.genai.Client") as mock_client_cls:
        mock_client = MagicMock()
        mock_client_cls.return_value = mock_client
        provider = GeminiProvider()
        yield provider, mock_client
    app.dependency_overrides.clear()


def test_analyze_returns_shaped_result_from_mocked_gemini_call(gemini_provider):
    provider, mock_client = gemini_provider
    mock_client.models.generate_content.return_value = MagicMock(text=VALID_JSON_RESPONSE)
    app.dependency_overrides[get_llm_provider] = lambda: provider

    response = client.post("/analyze", json=SAMPLE_REQUEST)

    assert response.status_code == 200
    body = response.json()
    assert body["summary"].startswith("Alice and Bob agreed")
    assert body["actionItems"] == ["Bob writes the migration tests"]
    assert body["decisions"] == ["Ship the release by Friday"]
    assert body["deadlines"] == ["Friday - release ship date"]


def test_malformed_gemini_response_raises_clear_error(gemini_provider):
    provider, mock_client = gemini_provider
    mock_client.models.generate_content.return_value = MagicMock(text="not valid json at all")
    app.dependency_overrides[get_llm_provider] = lambda: provider

    response = client.post("/analyze", json=SAMPLE_REQUEST)

    assert response.status_code == 502
    assert "not valid JSON" in response.json()["detail"]
