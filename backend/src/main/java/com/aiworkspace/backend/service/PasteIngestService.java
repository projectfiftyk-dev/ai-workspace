package com.aiworkspace.backend.service;

import com.aiworkspace.backend.model.Message;
import com.aiworkspace.backend.model.Source;
import com.aiworkspace.backend.repository.MessageRepository;
import com.aiworkspace.backend.repository.SourceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Push-based ingest path for pasted text. Doesn't implement {@link com.aiworkspace.backend.source.ConversationSourceProvider}
 * since there's nothing to "pull" — the text becomes a source + message immediately.
 */
@Service
public class PasteIngestService {

    private final SourceRepository sourceRepository;
    private final MessageRepository messageRepository;

    public PasteIngestService(SourceRepository sourceRepository, MessageRepository messageRepository) {
        this.sourceRepository = sourceRepository;
        this.messageRepository = messageRepository;
    }

    public PasteIngestResult ingest(String orgId, String text) {
        Instant now = Instant.now();

        Source source = new Source();
        source.setOrgId(orgId);
        source.setProvider("paste");
        source.setExternalId(null);
        source.setDisplayName("Pasted conversation");
        source.setSyncMode(null);
        source.setEnabled(true);
        source.setLastSyncedAt(null);
        source = sourceRepository.save(source);

        Message message = new Message();
        message.setSourceId(source.getId());
        message.setExternalMessageId(null);
        message.setThreadId(null);
        message.setAuthorName(null);
        message.setContent(text);
        message.setSentAt(now);
        message.setSyncedAt(now);
        message = messageRepository.save(message);

        return new PasteIngestResult(source.getId(), message.getId());
    }

    public record PasteIngestResult(String sourceId, String messageId) {
    }
}
