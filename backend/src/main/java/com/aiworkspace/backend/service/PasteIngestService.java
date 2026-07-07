package com.aiworkspace.backend.service;

import com.aiworkspace.backend.model.AiResult;
import com.aiworkspace.backend.model.Message;
import com.aiworkspace.backend.model.Source;
import com.aiworkspace.backend.repository.MessageRepository;
import com.aiworkspace.backend.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Push-based ingest path for pasted text. Doesn't implement {@link com.aiworkspace.backend.source.ConversationSourceProvider}
 * since there's nothing to "pull" — the text becomes a source + message immediately.
 */
@Service
public class PasteIngestService {

    private static final Logger log = LoggerFactory.getLogger(PasteIngestService.class);

    private final SourceRepository sourceRepository;
    private final MessageRepository messageRepository;
    private final AnalysisService analysisService;

    public PasteIngestService(SourceRepository sourceRepository, MessageRepository messageRepository,
                               AnalysisService analysisService) {
        this.sourceRepository = sourceRepository;
        this.messageRepository = messageRepository;
        this.analysisService = analysisService;
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

        // Paste storage already succeeded at this point (Week 1's checkpoint), so an
        // unavailable AI service shouldn't fail the whole request — surface the error
        // to the caller instead and let them retry analysis explicitly.
        AiResult aiResult = null;
        String analysisError = null;
        try {
            aiResult = analysisService.analyzeSource(orgId, source.getId());
        } catch (AiServiceException e) {
            log.warn("Auto-analysis failed for source {}: {}", source.getId(), e.getMessage());
            analysisError = e.getMessage();
        }

        return new PasteIngestResult(source.getId(), message.getId(), aiResult, analysisError);
    }

    public record PasteIngestResult(String sourceId, String messageId, AiResult aiResult, String analysisError) {
    }
}
