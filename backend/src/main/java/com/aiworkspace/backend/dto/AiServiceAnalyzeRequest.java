package com.aiworkspace.backend.dto;

import java.util.List;

/** Matches ai-service's {@code AnalyzeRequest} pydantic model, sent to POST /analyze. */
public record AiServiceAnalyzeRequest(String sourceId, List<AiServiceMessagePayload> messages) {
}
