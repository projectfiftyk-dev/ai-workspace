package com.aiworkspace.backend.source;

import java.time.Instant;

/**
 * Provider-agnostic shape a {@link ConversationSourceProvider} hands back for a pull-based
 * fetch, before it's persisted as a {@link com.aiworkspace.backend.model.Message}.
 */
public record MessageDTO(
        String externalMessageId,
        String threadId,
        String authorName,
        String content,
        Instant sentAt
) {
}
