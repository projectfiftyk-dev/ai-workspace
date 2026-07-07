package com.aiworkspace.backend;

import com.aiworkspace.backend.model.AiResult;
import com.aiworkspace.backend.model.Task;
import com.aiworkspace.backend.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the paste -> analyze -> confirm loop end to end against a real MongoDB and a
 * real HTTP call to a local stub of ai-service's /analyze contract (see
 * ai-workspace/scratchpad/stub_ai_service.py, run manually alongside this test — it
 * returns a fixed AnalysisResult so the test doesn't depend on a live Gemini key).
 *
 * Uses MOCK web environment (no bound Tomcat port) rather than a random port, since this
 * host's JDK build can't open an NIO Selector (AF_UNIX loopback pipe fails) — a host
 * limitation unrelated to the code under test. MockMvc dispatches through the real
 * Spring MVC stack without needing a socket-bound server.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnalysisFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void pasteAnalyzeAndConfirmActionItemCreatesTask() throws Exception {
        String email = "loop-test-" + System.nanoTime() + "@example.com";
        String registerBody = """
                {"email": "%s", "password": "Sup3rSecret!", "name": "Loop Tester", "organizationName": "Loop Test Org %s"}
                """.formatted(email, System.nanoTime());

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        String pasteText = """
                Alice: We need to decide on the login redesign before the sprint ends.
                Bob: Agreed, let's ship the new login redesign instead of iterating on the old one.
                Alice: Great, that's decided then. Can you finalize the API contract for it?
                Bob: Sure, I'll get the API contract done by Friday.
                Alice: Perfect, API contract due Friday. Also someone needs to update staging with the new auth flow.
                """;
        String pasteBody = objectMapper.writeValueAsString(new PasteBody(pasteText));

        String pasteResponse = mockMvc.perform(post("/api/paste")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(pasteBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode pasteJson = objectMapper.readTree(pasteResponse);
        assertThat(pasteJson.get("analysisError").isNull()).isTrue();
        JsonNode aiResultJson = pasteJson.get("aiResult");
        assertThat(aiResultJson).isNotNull();
        assertThat(aiResultJson.get("summary").asText()).isNotBlank();
        assertThat(aiResultJson.get("decisions")).hasSizeGreaterThan(0);
        assertThat(aiResultJson.get("deadlines")).hasSizeGreaterThan(0);
        assertThat(aiResultJson.get("actionItems")).hasSizeGreaterThan(0);
        assertThat(aiResultJson.get("actionItems").get(0).get("confirmed").asBoolean()).isFalse();

        String aiResultId = aiResultJson.get("id").asText();
        String firstActionItemText = aiResultJson.get("actionItems").get(0).get("text").asText();

        String confirmResponse = mockMvc.perform(post("/api/ai-results/" + aiResultId + "/action-items/0/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Task createdTask = objectMapper.readValue(confirmResponse, Task.class);
        assertThat(createdTask.getId()).isNotBlank();
        assertThat(createdTask.getStatus()).isEqualTo("backlog");
        assertThat(createdTask.getTitle()).isEqualTo(firstActionItemText);
        assertThat(createdTask.getAiResultId()).isEqualTo(aiResultId);

        assertThat(taskRepository.findById(createdTask.getId())).isPresent();

        String getResponse = mockMvc.perform(get("/api/ai-results/" + aiResultId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        AiResult refreshed = objectMapper.readValue(getResponse, AiResult.class);
        assertThat(refreshed.getActionItems().get(0).isConfirmed()).isTrue();
        assertThat(refreshed.getActionItems().get(1).isConfirmed()).isFalse();
    }

    private record PasteBody(String text) {
    }
}
