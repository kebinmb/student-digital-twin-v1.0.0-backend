package com.sdt.web_app;

import com.sdt.web_app.dto.authentication.AuthDtos;
import com.sdt.web_app.entities.authentication.RefreshToken;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.RefreshTokenRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==========================================
    // 1. REGISTRATION TESTS
    // ==========================================
    @Nested
    @DisplayName("User Registration Scenarios")
    class RegistrationTests {

        @Test
        @DisplayName("Should successfully register a new user with default role and Argon2 password")
        void shouldRegisterNewUser() throws Exception {
            AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                    "johndoe",
                    "john@example.com",
                    "SecurePassword123!"
            );

            mockMvc.perform(post("/api/public/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            Optional<User> savedUser = userRepository.findByUsername("johndoe");
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail()).isEqualTo("john@example.com");

            // Update assertion from Roles.FACULTY to Roles.STUDENT
            assertThat(savedUser.get().getRoles()).contains(Roles.STUDENT);

            assertThat(passwordEncoder.matches("SecurePassword123!", savedUser.get().getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("Should fail when registering with an existing username (409 Conflict)")
        void shouldFailWhenUsernameTaken() throws Exception {
            User existing = User.builder()
                    .username("existinguser")
                    .email("unique@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .roles(Set.of(Roles.FACULTY))
                    .build();
            userRepository.save(existing);

            AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                    "existinguser",
                    "different@example.com",
                    "Password123!"
            );

            mockMvc.perform(post("/api/public/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title", is("Resource Conflict")));
        }

        @Test
        @DisplayName("Should fail validation on invalid payload (400 Bad Request)")
        void shouldFailOnInvalidData() throws Exception {
            AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest("ab", "not-an-email", "short");

            mockMvc.perform(post("/api/public/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.invalidParams", notNullValue()));
        }
    }

    // ==========================================
    // 2. LOGIN & TOKEN ISSUANCE TESTS
    // ==========================================
    @Nested
    @DisplayName("User Login Scenarios")
    class LoginTests {

        @BeforeEach
        void createTestUser() {
            User user = User.builder()
                    .username("alice")
                    .email("alice@example.com")
                    .password(passwordEncoder.encode("SecretPass123!"))
                    .enabled(true)
                    .roles(Set.of(Roles.FACULTY))
                    .build();
            userRepository.save(user);
        }

        @Test
        @DisplayName("Should login via username and return JWT + Partitioned HttpOnly Cookie")
        void shouldLoginWithValidUsername() throws Exception {
            AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("alice", "SecretPass123!");

            MvcResult result = mockMvc.perform(post("/api/public/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.tokenType", is("Bearer")))
                    .andExpect(jsonPath("$.expiresInSeconds", is(900)))
                    .andExpect(cookie().exists("REFRESH_TOKEN"))
                    .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
                    .andExpect(cookie().secure("REFRESH_TOKEN", true))
                    .andExpect(cookie().path("REFRESH_TOKEN", "/api/public/auth"))
                    .andReturn();

            String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
            assertThat(setCookieHeader).contains("Partitioned");
        }

        @Test
        @DisplayName("Should login via email interchangeably")
        void shouldLoginWithValidEmail() throws Exception {
            AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("alice@example.com", "SecretPass123!");

            mockMvc.perform(post("/api/public/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()));
        }

        @Test
        @DisplayName("Should reject invalid password with 401 Unauthorized")
        void shouldRejectInvalidPassword() throws Exception {
            AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("alice", "WrongPassword!");

            mockMvc.perform(post("/api/public/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title", is("Authentication Failed")));
        }

        @Test
        @DisplayName("Should reject disabled user account")
        void shouldRejectDisabledUser() throws Exception {
            User disabledUser = User.builder()
                    .username("bob_inactive")
                    .email("bob@example.com")
                    .password(passwordEncoder.encode("SecretPass123!"))
                    .enabled(false)
                    .roles(Set.of(Roles.FACULTY))
                    .build();
            userRepository.save(disabledUser);

            AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("bob_inactive", "SecretPass123!");

            mockMvc.perform(post("/api/public/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title", is("Authentication Failed")));
        }

        @Test
        @DisplayName("Login with non-existent user should fail with 401 Unauthorized")
        void loginWithNonExistentUserReturns401() throws Exception {
            AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("nonexistent_user", "RandomPassword123!");

            mockMvc.perform(post("/api/public/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title", is("Authentication Failed")));
        }
    }

    // ==========================================
    // 3. END-TO-END ACCESS TO PROTECTED API
    // ==========================================
    @Nested
    @DisplayName("Protected Endpoints JWT Verification")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("Minted Access Token should grant access to protected routes")
        void shouldAccessProtectedEndpointWithIssuedToken() throws Exception {
            User user = User.builder()
                    .username("carol")
                    .email("carol@example.com")
                    .password(passwordEncoder.encode("CarolPassword123!"))
                    .enabled(true)
                    .roles(Set.of(Roles.FACULTY))
                    .build();
            userRepository.save(user);

            // 1. Authenticate to obtain real access token
            MvcResult loginResult = mockMvc.perform(post("/api/public/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AuthDtos.LoginRequest("carol", "CarolPassword123!"))))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = loginResult.getResponse().getContentAsString();
            String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();

            // 2. Call protected resource
            mockMvc.perform(get("/api/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Unauthenticated request to protected API should return 401 Unauthorized")
        void unauthenticatedRequestReturns401() throws Exception {
            mockMvc.perform(get("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("User with FACULTY role should access faculty/user-protected API")
        void validJwtWithFacultyRoleReturns200() throws Exception {
            mockMvc.perform(get("/api/orders")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FACULTY"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("User with FACULTY role cannot access ADMIN endpoints (403 Forbidden)")
        void facultyCannotAccessAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard")
                            .with(jwt().jwt(builder -> builder
                                    .subject("faculty-user-123")
                                    .claim("roles", List.of("FACULTY"))
                            )))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("User with ADMIN role can access ADMIN endpoints")
        void adminCanAccessAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Public endpoints remain open to anonymous requests")
        void publicEndpointsAccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/public/health-check"))
                    .andExpect(status().isOk());
        }
    }

    // ==========================================
    // 4. REFRESH TOKEN ROTATION & THEFT DETECTION
    // ==========================================
    @Nested
    @DisplayName("Token Refresh & Reuse Detection")
    class RefreshTokenTests {

        private User testUser;

        @BeforeEach
        void createTestUser() {
            testUser = User.builder()
                    .username("dave")
                    .email("dave@example.com")
                    .password(passwordEncoder.encode("DavePassword123!"))
                    .enabled(true)
                    .roles(Set.of(Roles.FACULTY))
                    .build();
            userRepository.save(testUser);
        }

        @Test
        @DisplayName("Should rotate Refresh Token and issue new Access Token upon valid refresh request")
        void shouldRotateRefreshTokenSuccessfully() throws Exception {
            // 1. Initial Login
            MvcResult loginResult = mockMvc.perform(post("/api/public/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AuthDtos.LoginRequest("dave", "DavePassword123!"))))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie refreshCookie = loginResult.getResponse().getCookie("REFRESH_TOKEN");
            assertThat(refreshCookie).isNotNull();
            String initialTokenValue = refreshCookie.getValue();

            // 2. Refresh Request
            MvcResult refreshResult = mockMvc.perform(post("/api/public/auth/refresh")
                            .cookie(refreshCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(cookie().exists("REFRESH_TOKEN"))
                    .andReturn();

            Cookie newRefreshCookie = refreshResult.getResponse().getCookie("REFRESH_TOKEN");
            assertThat(newRefreshCookie).isNotNull();
            assertThat(newRefreshCookie.getValue()).isNotEqualTo(initialTokenValue);

            // 3. Confirm old token was marked as revoked
            RefreshToken oldTokenEntity = refreshTokenRepository.findByToken(initialTokenValue).orElseThrow();
            assertThat(oldTokenEntity.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("THEFT DETECTION: Using a revoked token must wipe all user tokens and return 401")
        void shouldDetectTokenReuseAndWipeAllSessions() throws Exception {
            // 1. Create a revoked and an active token for this user
            RefreshToken revokedToken = RefreshToken.builder()
                    .user(testUser)
                    .token("stolen-revoked-token")
                    .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                    .revoked(true)
                    .build();

            RefreshToken validToken = RefreshToken.builder()
                    .user(testUser)
                    .token("legitimate-active-token")
                    .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();

            refreshTokenRepository.save(revokedToken);
            refreshTokenRepository.save(validToken);

            // 2. Attempt reuse of the revoked token
            mockMvc.perform(post("/api/public/auth/refresh")
                            .cookie(new Cookie("REFRESH_TOKEN", "stolen-revoked-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title", is("Invalid Token")));

            // 3. Confirm all refresh tokens were purged
            assertThat(refreshTokenRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Logout should revoke the refresh token and clear the client cookie")
        void shouldLogoutSuccessfully() throws Exception {
            RefreshToken activeToken = RefreshToken.builder()
                    .user(testUser)
                    .token("active-session-token")
                    .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(activeToken);

            mockMvc.perform(post("/api/public/auth/logout")
                            .cookie(new Cookie("REFRESH_TOKEN", "active-session-token")))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().maxAge("REFRESH_TOKEN", 0));

            RefreshToken revokedToken = refreshTokenRepository.findByToken("active-session-token").orElseThrow();
            assertThat(revokedToken.isRevoked()).isTrue();
        }
    }

    // ==========================================
    // 5. CONCURRENCY & DOMAIN LOGIC TESTS
    // ==========================================
    @Test
    @DisplayName("RACE CONDITION: Concurrent refresh requests on the same token must result in at most one success")
    void concurrentRefreshTokenRequestsShouldDetectReplay() throws Exception {
        User testUser = User.builder()
                .username("concurrent_refresh_user")
                .email("concurrent_refresh@example.com")
                .password(passwordEncoder.encode("SecretPass123!"))
                .enabled(true)
                .roles(Set.of(Roles.FACULTY))
                .build();
        userRepository.save(testUser);

        RefreshToken activeToken = RefreshToken.builder()
                .user(testUser)
                .token("concurrent-test-token")
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(activeToken);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger unauthorizedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/public/auth/refresh")
                                    .cookie(new Cookie("REFRESH_TOKEN", "concurrent-test-token")))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 401) {
                        unauthorizedCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isLessThanOrEqualTo(1);
        assertThat(unauthorizedCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("RACE CONDITION: Concurrent registration with identical credentials must result in exactly one 201 Created and subsequent 409 Conflict")
    void concurrentDuplicateRegistrationHandling() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger createdCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                "concurrent_user",
                "concurrent@example.com",
                "SecurePass123!"
        );
        String payload = objectMapper.writeValueAsString(request);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/public/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 201) {
                        createdCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(createdCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
        assertThat(userRepository.findByUsername("concurrent_user")).isPresent();
    }

    @Test
    @DisplayName("User entity should properly add, check, and remove Roles enum")
    void testUserRoleAdditionAndRemoval() {
        User user = User.builder()
                .username("roleuser")
                .email("roleuser@example.com")
                .password(passwordEncoder.encode("SecretPass123!"))
                .build();

        user.addRole(Roles.FACULTY);
        user.addRole(Roles.ADMIN);
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Roles.FACULTY, Roles.ADMIN);
        assertThat(user.hasRole(Roles.ADMIN)).isTrue();

        user.removeRole(Roles.FACULTY);
        assertThat(user.getRoles()).containsExactly(Roles.ADMIN);

        user.removeRole(Roles.ADMIN);
        assertThat(user.getRoles()).isEmpty();
    }
}