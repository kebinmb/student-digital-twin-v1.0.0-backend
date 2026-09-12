package com.sdt.web_app.service.authentication;

import com.sdt.web_app.controller.TestSecurityControllers;
import com.sdt.web_app.dto.authentication.AuthDtos;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityControllers.class)
class JwtDenylistSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenDenylistService denylistService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        denylistService.clear();
        if (userRepository.findByUsername("denylist_user").isEmpty()) {
            User user = User.builder()
                    .username("denylist_user")
                    .email("denylist@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .enabled(true)
                    .roles(Set.of(Roles.STUDENT))
                    .build();
            userRepository.saveAndFlush(user);
        }
    }

    @Test
    @DisplayName("SEC-03: Logged out access token is revoked and subsequent request returns 401 Unauthorized")
    void testRevokedTokenIsRejected() throws Exception {
        // 1. Login to get token
        String loginPayload = """
                {
                    "usernameOrEmail": "denylist_user",
                    "password": "Password123!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = jsonNode.get("accessToken").asText();
        assertThat(accessToken).isNotEmpty();

        // 2. Verify token works before logout
        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 3. Logout with Bearer token header
        mockMvc.perform(post("/api/public/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // 4. Verify revoked token returns 401 Unauthorized
        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }
}
