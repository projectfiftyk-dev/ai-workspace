package com.aiworkspace.backend.controller;

import com.aiworkspace.backend.repository.OrganizationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27018/ai-workspace-authtest");
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

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void registerLoginAndMeRoundTrip() throws Exception {
        String registerBody = objectMapper.writeValueAsString(new RegisterPayload(
                "Ada Lovelace", "ada@example.com", "supersecret1", "Analytical Engines"));

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.user.role").value("admin"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();

        // duplicate registration is rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isConflict());

        // wrong password is rejected
        String badLogin = objectMapper.writeValueAsString(new LoginPayload("ada@example.com", "wrongpassword"));
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(badLogin))
                .andExpect(status().isUnauthorized());

        // correct login works
        String goodLogin = objectMapper.writeValueAsString(new LoginPayload("ada@example.com", "supersecret1"));
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(goodLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // /me with no token -> 401
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        // /me with garbage token -> 401
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        // /me with valid token -> 200
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.name").value("Ada Lovelace"));
    }

    @Test
    void secondUserJoiningExistingOrgGetsMemberRole() throws Exception {
        String first = objectMapper.writeValueAsString(new RegisterPayload(
                "Founder", "founder@example.com", "supersecret1", "Shared Org"));
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(first))
                .andExpect(status().isCreated());

        String second = objectMapper.writeValueAsString(new RegisterPayload(
                "Teammate", "teammate@example.com", "supersecret1", "shared org"));
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(second))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("member"));
    }

    private record RegisterPayload(String name, String email, String password, String organizationName) {
    }

    private record LoginPayload(String email, String password) {
    }
}
