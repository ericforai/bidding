package com.xiyu.bid.service;

import com.xiyu.bid.auth.JwtUtil;
import com.xiyu.bid.dto.AuthSessionResult;
import com.xiyu.bid.dto.LoginRequest;
import com.xiyu.bid.entity.RefreshSession;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.RefreshSessionRepository;
import com.xiyu.bid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ProjectAccessScopeService projectAccessScopeService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshSessionRepository,
                projectAccessScopeService,
                passwordEncoder,
                jwtUtil,
                authenticationManager
        );
        ReflectionTestUtils.setField(authService, "refreshExpiration", 7 * 24 * 60 * 60 * 1000L);
    }

    @Test
    void login_ShouldCreateRefreshSessionAndReturnTokens() {
        User user = buildUser();
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("alice")).thenReturn("access-token");
        when(projectAccessScopeService.getAllowedProjectIds(user)).thenReturn(List.of(11L, 12L));
        when(projectAccessScopeService.getAllowedDepartmentCodes(user)).thenReturn(List.of("SALES"));

        AuthSessionResult result = authService.login(request);

        verify(authenticationManager).authenticate(any());
        verify(refreshSessionRepository).save(any(RefreshSession.class));
        assertThat(result.getAuthResponse().getToken()).isEqualTo("access-token");
        assertThat(result.getAuthResponse().getAllowedProjectIds()).containsExactly(11L, 12L);
        assertThat(result.getAuthResponse().getAllowedDepts()).containsExactly("SALES");
        assertThat(result.getRefreshToken()).isNotBlank();
    }

    @Test
    void refreshToken_ShouldRevokePreviousSessionAndCreateNewOne() {
        User user = buildUser();
        RefreshSession session = RefreshSession.builder()
                .id(1L)
                .user(user)
                .tokenHash("existing-hash")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(refreshSessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session));
        when(jwtUtil.generateAccessToken("alice")).thenReturn("rotated-access-token");
        when(projectAccessScopeService.getAllowedProjectIds(user)).thenReturn(List.of(21L));
        when(projectAccessScopeService.getAllowedDepartmentCodes(user)).thenReturn(List.of("BID"));

        AuthSessionResult result = authService.refreshToken("raw-refresh-token");

        ArgumentCaptor<RefreshSession> captor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getRevokedAt()).isNotNull();
        assertThat(captor.getAllValues().get(1).getTokenHash()).isNotBlank();
        assertThat(captor.getAllValues().get(1).getUser()).isEqualTo(user);
        assertThat(result.getAuthResponse().getToken()).isEqualTo("rotated-access-token");
        assertThat(result.getAuthResponse().getAllowedProjectIds()).containsExactly(21L);
        assertThat(result.getAuthResponse().getAllowedDepts()).containsExactly("BID");
        assertThat(result.getRefreshToken()).isNotBlank();
    }

    @Test
    void refreshToken_ShouldRejectMissingRefreshToken() {
        assertThatThrownBy(() -> authService.refreshToken(null))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    void logout_ShouldRevokeMatchingSession() {
        RefreshSession session = RefreshSession.builder()
                .user(buildUser())
                .tokenHash("existing-hash")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(refreshSessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session));

        authService.logout("raw-refresh-token");

        verify(refreshSessionRepository).save(session);
        assertThat(session.getRevokedAt()).isNotNull();
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .fullName("Alice")
                .role(User.Role.ADMIN)
                .enabled(true)
                .password("encoded")
                .build();
    }
}
