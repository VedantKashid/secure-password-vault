package com.vault.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class VaultIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Test 1: Verify the Password Generator works and returns valid JSON
    @Test
    @WithMockUser(username = "admin") // This bypasses the JWT requirement for testing
    public void testGeneratePassword_ReturnsSecureString() throws Exception {
        mockMvc.perform(get("/api/vault/generate?length=20&useSpecial=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length").value(20))
                .andExpect(jsonPath("$.useSpecial").value(true))
                .andExpect(jsonPath("$.password").exists());
    }

    // Test 2: Verify the Strength Checker accurately grades a good password
    @Test
    @WithMockUser(username = "admin")
    public void testPasswordStrength_EvaluatesCorrectly() throws Exception {
        // The exact JSON payload our frontend sends
        String requestBody = "{\"password\": \"StrongP@ssw0rd!\"}";

        mockMvc.perform(post("/api/vault/check-strength")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.strength").value("STRONG"));
    }
}