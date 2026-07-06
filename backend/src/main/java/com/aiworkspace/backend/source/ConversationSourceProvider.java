package com.aiworkspace.backend.source;

import java.util.List;

/**
 * A pull-based conversation source: something the backend periodically asks for new messages,
 * as opposed to paste (push-based — see {@link com.aiworkspace.backend.service.PasteIngestService}).
 */
public interface ConversationSourceProvider {

    /** "teams" | "slack" | "messenger", etc. — matches {@code Source.provider}. */
    String getProviderType();

    List<MessageDTO> fetchNewMessages(SourceConfig config);
}
