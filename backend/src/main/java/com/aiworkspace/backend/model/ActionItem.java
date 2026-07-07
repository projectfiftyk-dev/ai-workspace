package com.aiworkspace.backend.model;

/**
 * A single detected action item within an {@link AiResult}. Staged, not a {@link Task} —
 * only becomes one when the user explicitly confirms it (see AnalysisService#confirmActionItem).
 */
public class ActionItem {

    private String text;

    private String assignee;

    private String deadline;

    private boolean confirmed;

    public ActionItem() {
    }

    public ActionItem(String text, String assignee, String deadline, boolean confirmed) {
        this.text = text;
        this.assignee = assignee;
        this.deadline = deadline;
        this.confirmed = confirmed;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
}
