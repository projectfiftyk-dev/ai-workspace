package com.aiworkspace.backend.dto;

import java.time.Instant;

/** Matches ai-service's {@code MessageInput} pydantic model. */
public record AiServiceMessagePayload(String author, String content, Instant sentAt) {
}
