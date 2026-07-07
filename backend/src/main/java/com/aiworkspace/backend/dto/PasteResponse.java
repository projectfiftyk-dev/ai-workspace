package com.aiworkspace.backend.dto;

import com.aiworkspace.backend.model.AiResult;

public record PasteResponse(String sourceId, String messageId, AiResult aiResult, String analysisError) {
}
