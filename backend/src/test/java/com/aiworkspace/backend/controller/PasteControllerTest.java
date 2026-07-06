package com.aiworkspace.backend.controller;

import com.aiworkspace.backend.model.Message;
import com.aiworkspace.backend.model.Source;
import com.aiworkspace.backend.repository.MessageRepository;
import com.aiworkspace.backend.repository.OrganizationRepository;
import com.aiworkspace.backend.repository.SourceRepository;
import com.aiworkspace.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasteControllerTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27018/ai-workspace-pastetest");
        registry.add("app.jwt.secret", () -> "test-secret-please-be-at-least-32-bytes-long");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        sourceRepository.deleteAll();
        messageRepository.deleteAll();
    }

    @Test
    void pastingTextIsRejectedWithoutAuthentication() throws Exception {
        String body = objectMapper.writeValueAsString(new PastePayload("some raw chat text"));

        mockMvc.perform(post("/api/paste").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pastingTextCreatesALinkedSourceAndMessageInMongo() throws Exception {
        String registerBody = objectMapper.writeValueAsString(new RegisterPayload(
                "Ada Lovelace", "ada@example.com", "supersecret1", "Analytical Engines"));
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        String pasteBody = objectMapper.writeValueAsString(new PastePayload(
                "Diego: I'll have the report ready by Friday. Decision: we're shipping v2 next sprint."));

        String pasteResponse = mockMvc.perform(post("/api/paste")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(pasteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceId").exists())
                .andExpect(jsonPath("$.messageId").exists())
                .andReturn().getResponse().getContentAsString();

        String sourceId = objectMapper.readTree(pasteResponse).get("sourceId").asText();
        String messageId = objectMapper.readTree(pasteResponse).get("messageId").asText();

        // verify against MongoDB directly, not just the HTTP response
        Optional<Source> source = sourceRepository.findById(sourceId);
        assertThat(source).isPresent();
        assertThat(source.get().getProvider()).isEqualTo("paste");
        assertThat(source.get().getExternalId()).isNull();

        Optional<Message> message = messageRepository.findById(messageId);
        assertThat(message).isPresent();
        assertThat(message.get().getSourceId()).isEqualTo(sourceId);
        assertThat(message.get().getContent()).contains("shipping v2 next sprint");

        assertThat(messageRepository.findBySourceIdOrderBySentAtAsc(sourceId))
                .extracting(Message::getId)
                .containsExactly(messageId);
    }

    @Test
    void blankTextIsRejected() throws Exception {
        String registerBody = objectMapper.writeValueAsString(new RegisterPayload(
                "Ada Lovelace", "ada2@example.com", "supersecret1", "Analytical Engines"));
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        String pasteBody = objectMapper.writeValueAsString(new PastePayload("   "));

        mockMvc.perform(post("/api/paste")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(pasteBody))
                .andExpect(status().isBadRequest());
    }

    private record RegisterPayload(String name, String email, String password, String organizationName) {
    }

    private record PastePayload(String text) {
    }
}
