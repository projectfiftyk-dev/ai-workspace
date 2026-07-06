package com.aiworkspace.backend.source;

import java.time.Instant;

/**
 * What a pull-based {@link ConversationSourceProvider} needs to fetch new messages for one
 * source: which external channel, and how far back (null means "from the beginning").
 */
public record SourceConfig(String externalId, Instant since) {
}
