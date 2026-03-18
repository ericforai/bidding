package com.xiyu.bid.controller;

import com.xiyu.bid.dto.AuthResponse;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    @WithMockUser(username = "alice", roles = {"ADMIN"})
    void logout_ShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService).logout("alice", "access-token", "refresh-token");
    }

    @Test
    void refresh_ShouldRotateRefreshTokenAndIssueNewAccessToken() throws Exception {
        AuthResponse refreshResponse = AuthResponse.builder()
                .token("refreshed-token")
                .type("Bearer")
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .fullName("Alice")
                .role(User.Role.ADMIN)
                .build();

        when(authService.refreshSession(eq("refresh-token")))
                .thenReturn(new AuthService.RefreshSession(refreshResponse, "refresh-token-next"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.token").value("refreshed-token"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(header().string("X-Refresh-Token", "refresh-token-next"));
    }

    @Test
    void getCurrentUser_ShouldRejectUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }
}
