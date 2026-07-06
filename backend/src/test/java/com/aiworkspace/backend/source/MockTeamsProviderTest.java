package com.aiworkspace.backend.source;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockTeamsProviderTest {

    private final MockTeamsProvider provider = new MockTeamsProvider();

    @Test
    void reportsTeamsAsProviderType() {
        assertThat(provider.getProviderType()).isEqualTo("teams");
    }

    @Test
    void returnsAllSeededMessagesForAKnownChannelWhenNoSinceGiven() {
        List<MessageDTO> messages = provider.fetchNewMessages(new SourceConfig("general", null));

        assertThat(messages).hasSize(3);
        assertThat(messages).extracting(MessageDTO::authorName)
                .contains("Priya Nair", "Diego Alvarez");
        assertThat(messages).extracting(MessageDTO::content)
                .anyMatch(content -> content.contains("Decision"));
    }

    @Test
    void returnsDifferentSeededDataForDifferentChannels() {
        List<MessageDTO> general = provider.fetchNewMessages(new SourceConfig("general", null));
        List<MessageDTO> productLaunch = provider.fetchNewMessages(new SourceConfig("product-launch", null));

        assertThat(general).isNotEmpty();
        assertThat(productLaunch).isNotEmpty();
        assertThat(general).extracting(MessageDTO::externalMessageId)
                .doesNotContainAnyElementsOf(
                        productLaunch.stream().map(MessageDTO::externalMessageId).toList());
    }

    @Test
    void onlyReturnsMessagesSentAfterSince() {
        List<MessageDTO> all = provider.fetchNewMessages(new SourceConfig("eng-standup", null));
        Instant cutoff = all.get(0).sentAt();

        List<MessageDTO> incremental = provider.fetchNewMessages(new SourceConfig("eng-standup", cutoff));

        assertThat(incremental).allMatch(message -> message.sentAt().isAfter(cutoff));
        assertThat(incremental.size()).isLessThan(all.size());
    }

    @Test
    void returnsEmptyListForUnknownChannel() {
        assertThat(provider.fetchNewMessages(new SourceConfig("does-not-exist", null))).isEmpty();
    }
}
