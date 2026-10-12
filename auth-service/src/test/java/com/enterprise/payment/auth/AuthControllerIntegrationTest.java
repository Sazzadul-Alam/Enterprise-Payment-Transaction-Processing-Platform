package com.enterprise.payment.auth;

import com.enterprise.payment.auth.dto.LoginRequest;
import com.enterprise.payment.auth.dto.RefreshTokenRequest;
import com.enterprise.payment.auth.dto.RegisterRequest;
import com.enterprise.payment.auth.repository.AuthUserRepository;
import com.enterprise.payment.auth.repository.RefreshTokenRepository;
import com.enterprise.payment.common.constant.HeaderConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        authUserRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new user and return JWT tokens")
    void shouldRegisterNewUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@enterprise.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.CORRELATION_ID, "corr-reg-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.correlationId").value("corr-reg-1"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.email").value("alice@enterprise.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email (409 Conflict)")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@enterprise.com")
                .password("Password123!")
                .build();

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration fails with 409
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("Should reject registration with invalid email or short password (400 Bad Request)")
    void shouldRejectInvalidRegistrationPayload() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should successfully login and return JWT tokens")
    void shouldLoginSuccessfully() throws Exception {
        // Register user first
        RegisterRequest regRequest = RegisterRequest.builder()
                .email("bob@enterprise.com")
                .password("SecurePass456!")
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest loginRequest = LoginRequest.builder()
                .email("bob@enterprise.com")
                .password("SecurePass456!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.CORRELATION_ID, "corr-login-1")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("bob@enterprise.com"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    @DisplayName("Should reject login with incorrect password (401 Unauthorized)")
    void shouldRejectLoginWithWrongPassword() throws Exception {
        RegisterRequest regRequest = RegisterRequest.builder()
                .email("carol@enterprise.com")
                .password("CorrectPassword1!")
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("carol@enterprise.com")
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should rotate refresh token and issue new token pair")
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Register user
        RegisterRequest regRequest = RegisterRequest.builder()
                .email("dave@enterprise.com")
                .password("DaveSecret123!")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String initialRefreshToken = rootNode.path("data").path("refreshToken").asText();

        // Refresh token
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(initialRefreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        JsonNode refreshedNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshedNode.path("data").path("refreshToken").asText();

        // Ensure token was rotated (new value issued)
        assertThat(newRefreshToken).isNotEqualTo(initialRefreshToken);

        // Attempting to reuse old refresh token should fail (401 Unauthorized)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should access protected /api/v1/auth/me with valid Bearer token and reject without token")
    void shouldAccessProtectedMeEndpoint() throws Exception {
        // 1. Without token -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        // 2. Register and obtain access token
        RegisterRequest regRequest = RegisterRequest.builder()
                .email("eve@enterprise.com")
                .password("EveSecret123!")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = rootNode.path("data").path("accessToken").asText();

        // 3. With valid token -> 200 OK
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("eve@enterprise.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Should revoke token on logout")
    void shouldRevokeTokenOnLogout() throws Exception {
        RegisterRequest regRequest = RegisterRequest.builder()
                .email("frank@enterprise.com")
                .password("FrankPassword123!")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = rootNode.path("data").path("accessToken").asText();
        String refreshToken = rootNode.path("data").path("refreshToken").asText();

        // Logout
        RefreshTokenRequest logoutReq = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Subsequent refresh with revoked token should fail (401 Unauthorized)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should lock account after maximum failed login attempts")
    void shouldLockAccountAfterMaxFailedAttempts() throws Exception {
        RegisterRequest regRequest = RegisterRequest.builder()
                .email("grace@enterprise.com")
                .password("GracePass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        LoginRequest wrongLogin = LoginRequest.builder()
                .email("grace@enterprise.com")
                .password("WrongPassword!")
                .build();

        // 5 consecutive failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // Even with the correct password, login is blocked due to lockout
        LoginRequest correctLogin = LoginRequest.builder()
                .email("grace@enterprise.com")
                .password("GracePass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("locked")));
    }
}
