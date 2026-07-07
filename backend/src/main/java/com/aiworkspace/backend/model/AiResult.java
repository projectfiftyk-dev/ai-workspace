package com.aiworkspace.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "ai_results")
public class AiResult {

    @Id
    private String id;

    private String sourceId;

    private List<String> messageIds;

    private String summary;

    private List<ActionItem> actionItems;

    private List<String> decisions;

    private List<String> deadlines;

    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public List<String> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ActionItem> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<ActionItem> actionItems) {
        this.actionItems = actionItems;
    }

    public List<String> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<String> decisions) {
        this.decisions = decisions;
    }

    public List<String> getDeadlines() {
        return deadlines;
    }

    public void setDeadlines(List<String> deadlines) {
        this.deadlines = deadlines;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
