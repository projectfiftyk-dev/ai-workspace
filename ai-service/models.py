from datetime import datetime

from pydantic import BaseModel, Field


class MessageInput(BaseModel):
    author: str | None = None
    content: str
    sentAt: datetime


class AnalysisResult(BaseModel):
    summary: str
    actionItems: list[str] = Field(default_factory=list)
    decisions: list[str] = Field(default_factory=list)
    deadlines: list[str] = Field(default_factory=list)


class AnalyzeRequest(BaseModel):
    sourceId: str
    messages: list[MessageInput]
