from fastapi import Depends, FastAPI, HTTPException

from llm_provider import LLMProvider, LLMResponseError
from models import AnalysisResult, AnalyzeRequest
from provider_factory import get_llm_provider

app = FastAPI(title="ai-service")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/analyze", response_model=AnalysisResult)
def analyze(
    request: AnalyzeRequest,
    provider: LLMProvider = Depends(get_llm_provider),
) -> AnalysisResult:
    try:
        return provider.analyze(request.messages)
    except LLMResponseError as e:
        raise HTTPException(status_code=502, detail=str(e)) from e
