import json
import os
import time

from google import genai
from google.genai import errors
from pydantic import ValidationError

from llm_provider import LLMProvider, LLMResponseError
from models import AnalysisResult, MessageInput

# Free-tier default (Pro was pulled from the free tier in April 2026; Flash
# is sufficient for structured extraction). Override via GEMINI_MODEL if a
# newer free-tier Flash model becomes the recommended default.
DEFAULT_MODEL = "gemini-2.5-flash"

RETRY_DELAYS_SECONDS = [1, 2, 4, 8]

PROMPT_TEMPLATE = """You are analyzing a workplace conversation. Read the transcript below and extract its structure.

Transcript:
{transcript}

Return ONLY valid JSON, with no markdown code fences and no preamble, matching exactly this shape:
{{"summary": "string", "actionItems": ["string", ...], "decisions": ["string", ...], "deadlines": ["string", ...]}}

- summary: a concise 1-3 sentence summary of the conversation.
- actionItems: concrete tasks someone needs to do, as short imperative phrases.
- decisions: decisions that were made during the conversation.
- deadlines: explicit dates or deadlines mentioned, as short phrases including what they refer to.

Use an empty array for any category with nothing to report."""


class GeminiProvider(LLMProvider):
    def __init__(self) -> None:
        api_key = os.getenv("GEMINI_API_KEY")
        if not api_key:
            raise RuntimeError("GEMINI_API_KEY environment variable is not set")
        self._model = os.getenv("GEMINI_MODEL", DEFAULT_MODEL)
        self._client = genai.Client(api_key=api_key)

    def analyze(self, messages: list[MessageInput]) -> AnalysisResult:
        prompt = self._build_prompt(messages)
        raw_text = self._generate_with_retry(prompt)
        return self._parse_response(raw_text)

    def _build_prompt(self, messages: list[MessageInput]) -> str:
        transcript = "\n".join(
            f"{message.author or 'Unknown'} ({message.sentAt.isoformat()}): {message.content}"
            for message in messages
        )
        return PROMPT_TEMPLATE.format(transcript=transcript)

    def _generate_with_retry(self, prompt: str) -> str:
        attempts = len(RETRY_DELAYS_SECONDS) + 1
        for attempt in range(attempts):
            try:
                response = self._client.models.generate_content(
                    model=self._model,
                    contents=prompt,
                )
                return response.text
            except errors.ClientError as e:
                is_rate_limited = getattr(e, "code", None) == 429
                if not is_rate_limited or attempt == attempts - 1:
                    raise
                time.sleep(RETRY_DELAYS_SECONDS[attempt])
        raise AssertionError("unreachable")  # pragma: no cover

    def _parse_response(self, raw_text: str) -> AnalysisResult:
        cleaned = self._strip_code_fences(raw_text)
        try:
            data = json.loads(cleaned)
        except json.JSONDecodeError as e:
            raise LLMResponseError(
                f"Gemini response was not valid JSON: {e}. Raw response: {raw_text!r}"
            ) from e
        try:
            return AnalysisResult.model_validate(data)
        except ValidationError as e:
            raise LLMResponseError(
                f"Gemini response did not match the expected AnalysisResult schema: {e}"
            ) from e

    @staticmethod
    def _strip_code_fences(text: str) -> str:
        stripped = text.strip()
        if stripped.startswith("```"):
            stripped = stripped.split("\n", 1)[-1]
            if stripped.endswith("```"):
                stripped = stripped.rsplit("```", 1)[0]
        return stripped.strip()
