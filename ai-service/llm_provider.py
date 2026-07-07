from abc import ABC, abstractmethod

from models import AnalysisResult, MessageInput


class LLMResponseError(RuntimeError):
    """Raised when an LLM's response can't be parsed into a valid AnalysisResult."""


class LLMProvider(ABC):
    @abstractmethod
    def analyze(self, messages: list[MessageInput]) -> AnalysisResult:
        ...
