package com.xiyu.bid.service;

import com.xiyu.bid.entity.PasswordResetToken;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.PasswordResetTokenRepository;
import com.xiyu.bid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PasswordResetService单元测试
 * 使用TDD方法测试密码重置核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken testToken;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                tokenRepository,
                userRepository,
                passwordEncoder,
                emailService
        );

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .role(User.Role.STAFF)
                .enabled(true)
                .build();

        testToken = PasswordResetToken.builder()
                .id(1L)
                .token("hashedToken")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== createPasswordResetToken Tests ==========

    @Test
    void createPasswordResetToken_WithValidEmail_ShouldCreateTokenAndSendEmail() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenReturn(testToken);
        when(emailService.sendPasswordResetEmail(anyString(), anyString()))
                .thenReturn("raw-token-for-dev");

        // When
        String result = passwordResetService.createPasswordResetToken("test@example.com");

        // Then
        assertThat(result).isEqualTo("raw-token-for-dev");
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }

    @Test
    void createPasswordResetToken_WithNonExistentEmail_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.createPasswordResetToken("nonexistent@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void createPasswordResetToken_WithDisabledUser_ShouldThrowException() {
        // Given
        User disabledUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .role(User.Role.STAFF)
                .enabled(false)
                .build();
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(disabledUser));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.createPasswordResetToken("test@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void createPasswordResetToken_ShouldSetExpirationToOneHour() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendPasswordResetEmail(anyString(), anyString()))
                .thenReturn("raw-token");

        // When
        passwordResetService.createPasswordResetToken("test@example.com");

        // Then
        verify(tokenRepository).save(argThat(token ->
                token.getExpiresAt().isAfter(LocalDateTime.now())
                        && token.getExpiresAt().isBefore(LocalDateTime.now().plusHours(1).plusMinutes(1))
        ));
    }

    // ========== validateToken Tests ==========

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String rawToken = "valid-raw-token";
        String hashedToken = hashTokenForTest(rawToken);
        testToken.setToken(hashedToken);
        testToken.setUsedAt(null);

        when(tokenRepository.findByToken(hashedToken))
                .thenReturn(Optional.of(testToken));

        // When
        boolean result = passwordResetService.validateToken(rawToken);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateToken_WithUsedToken_ShouldReturnFalse() {
        // Given
        String rawToken = "used-token";
        String hashedToken = hashTokenForTest(rawToken);
        testToken.setToken(hashedToken);
        testToken.setUsedAt(LocalDateTime.now());

        when(tokenRepository.findByToken(hashedToken))
                .thenReturn(Optional.of(testToken));

        // When
        boolean result = passwordResetService.validateToken(rawToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given
        String rawToken = "expired-token";
        String hashedToken = hashTokenForTest(rawToken);
        testToken.setToken(hashedToken);
        testToken.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByToken(hashedToken))
                .thenReturn(Optional.of(testToken));

        // When
        boolean result = passwordResetService.validateToken(rawToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateToken_WithNonExistentToken_ShouldReturnFalse() {
        // Given
        String rawToken = "non-existent-token";
        String hashedToken = hashTokenForTest(rawToken);

        when(tokenRepository.findByToken(hashedToken))
                .thenReturn(Optional.empty());

        // When
        boolean result = passwordResetService.validateToken(rawToken);

        // Then
        assertThat(result).isFalse();
    }

    // ========== resetPassword Tests ==========

    @Test
    void resetPassword_WithValidToken_ShouldUpdatePasswordAndMarkTokenUsed() {
        // Given
        String rawToken = "valid-reset-token";
        String hashedToken = hashTokenForTest(rawToken);
        testToken.setToken(hashedToken);
        testToken.setUsedAt(null);
        String newPassword = "NewSecurePassword123!";

        when(tokenRepository.findByToken(hashedToken))
                .thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(newPassword))
                .thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        // When
        passwordResetService.resetPassword(rawToken, newPassword);

        // Then
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(argThat(user ->
                "newEncodedPassword".equals(user.getPassword())
        ));
        verify(tokenRepository).markAsUsed(eq(hashedToken), any(LocalDateTime.class));
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldThrowException() {
        // Given
        String rawToken = "invalid-token";
        String hashedToken = hashTokenForTest(rawToken);

        when(tokenRepository.findByToken(hashedToken))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "NewPassword123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_WithUsedToken_ShouldThrowException() {
        // Given
        String rawToken = "already-used-token";
        String hashedToken = hashTokenForTest(rawToken);
        testToken.setToken(hashedToken);
        testToken.setUsedAt(LocalDateTime.now());

        when(tokenRepository.findByToken(hashedToken))
                .thenReturn(Optional.of(testToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "NewPassword123!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been used");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowException() {
        // Given
        String rawToken = "expired-token";
        String hashedToken = hashTokenForTest(rawToken);
        testToken.setToken(hashedToken);
        testToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByToken(hashedToken))
                .thenReturn(Optional.of(testToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "NewPassword123!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    // ========== cleanupExpiredTokens Tests ==========

    @Test
    void cleanupExpiredTokens_ShouldDeleteExpiredAndUsedTokens() {
        // Given
        when(tokenRepository.deleteExpiredOrUsedTokens(any(LocalDateTime.class)))
                .thenReturn(5);

        // When
        passwordResetService.cleanupExpiredTokens();

        // Then
        verify(tokenRepository).deleteExpiredOrUsedTokens(any(LocalDateTime.class));
    }

    @Test
    void cleanupExpiredTokens_WithNoTokensToDelete_ShouldHandleGracefully() {
        // Given
        when(tokenRepository.deleteExpiredOrUsedTokens(any(LocalDateTime.class)))
                .thenReturn(0);

        // When
        passwordResetService.cleanupExpiredTokens();

        // Then
        verify(tokenRepository).deleteExpiredOrUsedTokens(any(LocalDateTime.class));
    }

    // ========== Helper Methods ==========

    /**
     * Helper method to hash tokens for testing
     * This mirrors the implementation in PasswordResetService
     */
    private String hashTokenForTest(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
