package com.aiworkspace.backend.service;

import com.aiworkspace.backend.dto.AiServiceAnalyzeRequest;
import com.aiworkspace.backend.dto.AiServiceAnalyzeResponse;
import com.aiworkspace.backend.dto.AiServiceMessagePayload;
import com.aiworkspace.backend.model.ActionItem;
import com.aiworkspace.backend.model.AiResult;
import com.aiworkspace.backend.model.Message;
import com.aiworkspace.backend.model.Source;
import com.aiworkspace.backend.model.Task;
import com.aiworkspace.backend.repository.AiResultRepository;
import com.aiworkspace.backend.repository.MessageRepository;
import com.aiworkspace.backend.repository.SourceRepository;
import com.aiworkspace.backend.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Wires the core loop: {@code messages} -> ai-service /analyze -> {@code ai_results}.
 * Action items land staged (confirmed: false); {@link #confirmActionItem} is the only
 * path that creates a {@link Task}.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final SourceRepository sourceRepository;
    private final MessageRepository messageRepository;
    private final AiResultRepository aiResultRepository;
    private final TaskRepository taskRepository;
    private final RestTemplate restTemplate;
    private final String aiServiceBaseUrl;

    public AnalysisService(SourceRepository sourceRepository,
                            MessageRepository messageRepository,
                            AiResultRepository aiResultRepository,
                            TaskRepository taskRepository,
                            RestTemplate restTemplate,
                            @Value("${app.ai-service.base-url}") String aiServiceBaseUrl) {
        this.sourceRepository = sourceRepository;
        this.messageRepository = messageRepository;
        this.aiResultRepository = aiResultRepository;
        this.taskRepository = taskRepository;
        this.restTemplate = restTemplate;
        this.aiServiceBaseUrl = aiServiceBaseUrl;
    }

    public AiResult analyzeSource(String orgId, String sourceId) {
        Source source = loadOwnedSource(orgId, sourceId);

        List<Message> messages = messageRepository.findBySourceIdOrderBySentAtAsc(source.getId());
        if (messages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "source has no messages to analyze");
        }

        AiServiceAnalyzeResponse response = callAiService(source.getId(), messages);

        AiResult aiResult = new AiResult();
        aiResult.setSourceId(source.getId());
        aiResult.setMessageIds(messages.stream().map(Message::getId).toList());
        aiResult.setSummary(response.summary());
        aiResult.setActionItems(response.actionItems().stream()
                .map(text -> new ActionItem(text, null, null, false))
                .toList());
        aiResult.setDecisions(response.decisions());
        aiResult.setDeadlines(response.deadlines());
        aiResult.setCreatedAt(Instant.now());

        return aiResultRepository.save(aiResult);
    }

    public AiResult getAiResult(String orgId, String aiResultId) {
        AiResult aiResult = aiResultRepository.findById(aiResultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ai result not found"));
        loadOwnedSource(orgId, aiResult.getSourceId());
        return aiResult;
    }

    public Task confirmActionItem(String orgId, String aiResultId, int index) {
        AiResult aiResult = aiResultRepository.findById(aiResultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ai result not found"));
        loadOwnedSource(orgId, aiResult.getSourceId());

        List<ActionItem> actionItems = aiResult.getActionItems();
        if (actionItems == null || index < 0 || index >= actionItems.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "action item not found");
        }

        ActionItem actionItem = actionItems.get(index);
        if (actionItem.isConfirmed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "action item already confirmed");
        }

        actionItem.setConfirmed(true);
        aiResultRepository.save(aiResult);

        Instant now = Instant.now();
        Task task = new Task();
        task.setSourceId(aiResult.getSourceId());
        task.setAiResultId(aiResult.getId());
        task.setActionItemIndex(index);
        task.setTitle(actionItem.getText());
        task.setAssignee(actionItem.getAssignee());
        task.setStatus("backlog");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        return taskRepository.save(task);
    }

    private Source loadOwnedSource(String orgId, String sourceId) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "source not found"));
        if (!source.getOrgId().equals(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "source does not belong to your organization");
        }
        return source;
    }

    private AiServiceAnalyzeResponse callAiService(String sourceId, List<Message> messages) {
        AiServiceAnalyzeRequest request = new AiServiceAnalyzeRequest(
                sourceId,
                messages.stream()
                        .map(m -> new AiServiceMessagePayload(m.getAuthorName(), m.getContent(), m.getSentAt()))
                        .toList()
        );

        try {
            AiServiceAnalyzeResponse response = restTemplate.postForObject(
                    aiServiceBaseUrl + "/analyze", request, AiServiceAnalyzeResponse.class);
            if (response == null) {
                throw new AiServiceException("ai-service returned an empty response");
            }
            return response;
        } catch (RestClientException e) {
            log.error("ai-service call failed for source {}", sourceId, e);
            throw new AiServiceException("Could not reach the AI service — it may be down or unreachable", e);
        }
    }
}
