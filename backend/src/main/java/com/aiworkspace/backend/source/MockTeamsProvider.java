package com.aiworkspace.backend.source;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Stands in for {@code GraphTeamsProvider} until a real tenant is available. Returns fixed,
 * realistic seeded messages across a few channels so the AI pipeline has something believable
 * to chew on — mentions of decisions, deadlines, and action items, the same shape real Teams
 * chatter would have.
 */
@Component
public class MockTeamsProvider implements ConversationSourceProvider {

    private static final Map<String, List<MessageDTO>> CHANNEL_MESSAGES = Map.of(
            "general", List.of(
                    new MessageDTO("teams-msg-1001", null, "Priya Nair",
                            "Heads up team, we're moving the sprint review from Friday to Thursday 2pm since half of us are out Friday.",
                            Instant.parse("2026-06-29T13:02:00Z")),
                    new MessageDTO("teams-msg-1002", "teams-msg-1001", "Diego Alvarez",
                            "Works for me. I'll have the demo environment ready by Wednesday EOD.",
                            Instant.parse("2026-06-29T13:05:00Z")),
                    new MessageDTO("teams-msg-1003", null, "Priya Nair",
                            "Decision: we're going with the new pricing tiers proposal as-is, no further changes before launch.",
                            Instant.parse("2026-06-30T09:14:00Z"))
            ),
            "product-launch", List.of(
                    new MessageDTO("teams-msg-2001", null, "Wei Chen",
                            "Action item: @Diego please finalize the App Store screenshots by end of day Monday, marketing needs them for the press kit.",
                            Instant.parse("2026-06-30T15:40:00Z")),
                    new MessageDTO("teams-msg-2002", "teams-msg-2001", "Diego Alvarez",
                            "On it. Will also need someone to confirm the final tagline before I export them.",
                            Instant.parse("2026-06-30T15:44:00Z")),
                    new MessageDTO("teams-msg-2003", null, "Sarah Kim",
                            "Launch date is confirmed for July 15th. Anything not ready by July 10th gets cut from v1.",
                            Instant.parse("2026-07-01T10:12:00Z"))
            ),
            "eng-standup", List.of(
                    new MessageDTO("teams-msg-3001", null, "Diego Alvarez",
                            "Yesterday: finished the auth refactor. Today: starting on the rate limiter. Blocked on: waiting for infra to provision the Redis instance.",
                            Instant.parse("2026-07-01T08:31:00Z")),
                    new MessageDTO("teams-msg-3002", null, "Priya Nair",
                            "I'll ping infra directly, that Redis ticket has been sitting for three days. Deadline for rate limiter is end of next week either way.",
                            Instant.parse("2026-07-01T08:35:00Z")),
                    new MessageDTO("teams-msg-3003", null, "Wei Chen",
                            "Decision from yesterday's arch review: we're keeping the monolith for now, revisit splitting out the notifications service after launch.",
                            Instant.parse("2026-07-01T08:40:00Z"))
            )
    );

    @Override
    public String getProviderType() {
        return "teams";
    }

    @Override
    public List<MessageDTO> fetchNewMessages(SourceConfig config) {
        List<MessageDTO> channelMessages = CHANNEL_MESSAGES.get(config.externalId());
        if (channelMessages == null) {
            return List.of();
        }
        if (config.since() == null) {
            return channelMessages;
        }
        return channelMessages.stream()
                .filter(message -> message.sentAt().isAfter(config.since()))
                .toList();
    }
}
