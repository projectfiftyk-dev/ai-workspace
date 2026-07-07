package com.aiworkspace.backend.dto;

import java.util.List;

/** Matches ai-service's {@code AnalysisResult} pydantic model, returned from POST /analyze. */
public record AiServiceAnalyzeResponse(
        String summary,
        List<String> actionItems,
        List<String> decisions,
        List<String> deadlines
) {
}
