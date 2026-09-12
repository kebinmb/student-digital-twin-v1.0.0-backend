package com.sdt.web_app.service.authentication;

import com.sdt.web_app.config.RateLimitingFilter;
import com.sdt.web_app.controller.TestSecurityControllers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityControllers.class)
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        RateLimitingFilter.setEnabledForTesting(true);
        rateLimitingFilter.clear();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        RateLimitingFilter.setEnabledForTesting(null);
        rateLimitingFilter.clear();
    }

    @Test
    @DisplayName("SEC-04: Excess public auth requests trigger 429 Too Many Requests rate limiting")
    void testRateLimitingOnPublicLogin() throws Exception {
        String invalidPayload = """
                {
                    "usernameOrEmail": "non_existent_user",
                    "password": "WrongPassword!"
                }
                """;

        // Perform 5 allowed failed requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/public/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 6th request must trigger 429 Too Many Requests
        mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }
}
