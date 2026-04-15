package com.xiyu.bid.export.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.config.ExportConfig;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.service.AuthService;
import com.xiyu.bid.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ExportControllerSecurityTest {

    @Mock
    private AuthService authService;

    @Test
    void extractUserId_ResolvesPersistedUserByUsername() throws Exception {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("alice")
                .password("password")
                .authorities("ROLE_STAFF")
                .build();
        User user = User.builder()
                .id(7L)
                .username("alice")
                .role(User.Role.STAFF)
                .email("alice@example.com")
                .fullName("Alice")
                .password("secret")
                .enabled(true)
                .build();

        org.mockito.Mockito.when(authService.resolveUserByUsername("alice")).thenReturn(user);

        Method method = ExportController.class.getDeclaredMethod("extractUserId", UserDetails.class);
        method.setAccessible(true);

        ExportController controller = new ExportController(
                null,
                (ExportConfig) null,
                (RateLimitService) null,
                new ObjectMapper(),
                authService
        );

        Long userId = (Long) method.invoke(controller, userDetails);

        assertThat(userId).isEqualTo(7L);
    }

    @Test
    void extractUserId_RejectsUnknownAuthenticatedUser() throws Exception {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("ghost")
                .password("password")
                .authorities("ROLE_STAFF")
                .build();

        org.mockito.Mockito.when(authService.resolveUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        Method method = ExportController.class.getDeclaredMethod("extractUserId", UserDetails.class);
        method.setAccessible(true);

        ExportController controller = new ExportController(
                null,
                (ExportConfig) null,
                (RateLimitService) null,
                new ObjectMapper(),
                authService
        );

        assertThatThrownBy(() -> method.invoke(controller, userDetails))
                .hasCauseExactlyInstanceOf(AuthenticationServiceException.class)
                .cause()
                .hasMessageContaining("Authenticated user not found");
    }
}
