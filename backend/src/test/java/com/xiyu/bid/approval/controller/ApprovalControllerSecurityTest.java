package com.xiyu.bid.approval.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security-focused tests for ApprovalController getUserIdFromDetails method.
 * Tests to verify authentication bypass vulnerabilities are fixed.
 *
 * Note: These tests directly test the private getUserIdFromDetails method behavior
 * through public API endpoints to ensure the security fix is in place.
 */
class ApprovalControllerSecurityTest {

    /**
     * This test verifies that when submitting an approval with a non-numeric username,
     * the controller throws an AuthenticationServiceException instead of falling back
     * to a default user ID (which was the security vulnerability).
     *
     * The test uses reflection to access the private method for testing.
     */
    @Test
    void getUserIdFromDetails_WithNonNumericUsername_ShouldThrowException() throws Exception {
        // Given: UserDetails with non-numeric username (simulating malformed auth token)
        TestUserDetails userDetails = new TestUserDetails("invalid_user");

        // Create an instance of ApprovalController
        Method method = ApprovalController.class.getDeclaredMethod(
                "getUserIdFromDetails",
                org.springframework.security.core.userdetails.UserDetails.class
        );
        method.setAccessible(true);

        ApprovalController controller = new ApprovalController(null);

        // When & Then: Should throw exception instead of returning default user ID
        assertThatThrownBy(() -> method.invoke(controller, userDetails))
                .hasCauseExactlyInstanceOf(AuthenticationServiceException.class)
                .cause()
                .hasMessageContaining("Invalid user identifier");
    }

    @Test
    void getUserIdFromDetails_WithNullUsername_ShouldThrowException() throws Exception {
        // Given: UserDetails with null username
        TestUserDetails userDetails = new TestUserDetails(null);

        Method method = ApprovalController.class.getDeclaredMethod(
                "getUserIdFromDetails",
                org.springframework.security.core.userdetails.UserDetails.class
        );
        method.setAccessible(true);

        ApprovalController controller = new ApprovalController(null);

        // When & Then: Should throw exception
        assertThatThrownBy(() -> method.invoke(controller, userDetails))
                .hasCauseExactlyInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void getUserIdFromDetails_WithEmptyUsername_ShouldThrowException() throws Exception {
        // Given: UserDetails with empty username
        TestUserDetails userDetails = new TestUserDetails("");

        Method method = ApprovalController.class.getDeclaredMethod(
                "getUserIdFromDetails",
                org.springframework.security.core.userdetails.UserDetails.class
        );
        method.setAccessible(true);

        ApprovalController controller = new ApprovalController(null);

        // When & Then: Should throw exception
        assertThatThrownBy(() -> method.invoke(controller, userDetails))
                .hasCauseExactlyInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void getUserIdFromDetails_WithNegativeUserId_ShouldThrowException() throws Exception {
        // Given: UserDetails with negative user ID
        TestUserDetails userDetails = new TestUserDetails("-1");

        Method method = ApprovalController.class.getDeclaredMethod(
                "getUserIdFromDetails",
                org.springframework.security.core.userdetails.UserDetails.class
        );
        method.setAccessible(true);

        ApprovalController controller = new ApprovalController(null);

        // When & Then: Should throw exception
        assertThatThrownBy(() -> method.invoke(controller, userDetails))
                .hasCauseExactlyInstanceOf(AuthenticationServiceException.class)
                .cause()
                .hasMessageContaining("must be positive");
    }

    @Test
    void getUserIdFromDetails_WithZeroUserId_ShouldThrowException() throws Exception {
        // Given: UserDetails with zero user ID (invalid)
        TestUserDetails userDetails = new TestUserDetails("0");

        Method method = ApprovalController.class.getDeclaredMethod(
                "getUserIdFromDetails",
                org.springframework.security.core.userdetails.UserDetails.class
        );
        method.setAccessible(true);

        ApprovalController controller = new ApprovalController(null);

        // When & Then: Should throw exception
        assertThatThrownBy(() -> method.invoke(controller, userDetails))
                .hasCauseExactlyInstanceOf(AuthenticationServiceException.class)
                .cause()
                .hasMessageContaining("must be positive");
    }

    @Test
    void getUserIdFromDetails_WithValidNumericUsername_ShouldSucceed() throws Exception {
        // Given: Valid UserDetails with numeric username
        TestUserDetails userDetails = new TestUserDetails("123");

        Method method = ApprovalController.class.getDeclaredMethod(
                "getUserIdFromDetails",
                org.springframework.security.core.userdetails.UserDetails.class
        );
        method.setAccessible(true);

        ApprovalController controller = new ApprovalController(null);

        // When: Extract user ID
        Long userId = (Long) method.invoke(controller, userDetails);

        // Then: Should return the correct user ID
        assertThat(userId).isEqualTo(123L);
    }

    /**
     * Test UserDetails implementation that allows null/empty username
     * for testing edge cases.
     */
    private static class TestUserDetails implements org.springframework.security.core.userdetails.UserDetails {
        private final String username;

        TestUserDetails(String username) {
            this.username = username;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        @Override
        public String getPassword() {
            return "password";
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
