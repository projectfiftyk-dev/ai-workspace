import os

from llm_provider import LLMProvider
from providers.gemini_provider import GeminiProvider


def get_llm_provider() -> LLMProvider:
    provider_name = os.getenv("LLM_PROVIDER", "gemini").lower()
    if provider_name == "gemini":
        return GeminiProvider()
    raise ValueError(f"Unknown LLM_PROVIDER: {provider_name}")
