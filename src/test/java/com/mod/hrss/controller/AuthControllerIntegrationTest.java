package com.mod.hrss.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mod.hrss.dto.request.LoginRequest;
import com.mod.hrss.dto.response.AuthResponse;
import com.mod.hrss.security.CustomUserDetailsService;
import com.mod.hrss.security.jwt.JwtTokenProvider;
import com.mod.hrss.security.jwt.TokenBlacklistService;
import com.mod.hrss.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // disables spring security filters for easy controller-only testing
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private com.mod.hrss.service.AuditLogService auditLogService;

    @MockBean
    private com.mod.hrss.security.filter.RateLimiterFilter rateLimiterFilter;

    @MockBean
    private com.mod.hrss.security.oauth2.CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private com.mod.hrss.security.handler.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@company.com");
        loginRequest.setPassword("password");

        AuthResponse authResponse = new AuthResponse(
                "mock-access-token",
                "mock-refresh-token",
                "Bearer",
                "admin@company.com",
                Set.of("ROLE_ADMIN")
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.email").value("admin@company.com"));
    }
}
