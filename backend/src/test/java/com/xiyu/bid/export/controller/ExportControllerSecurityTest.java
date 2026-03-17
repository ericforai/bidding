package com.xiyu.bid.export.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.config.ExportConfig;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ExportControllerSecurityTest {

    @Mock
    private UserRepository userRepository;

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

        org.mockito.Mockito.when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        Method method = ExportController.class.getDeclaredMethod("extractUserId", UserDetails.class);
        method.setAccessible(true);

        ExportController controller = new ExportController(
                null,
                (ExportConfig) null,
                (RateLimitService) null,
                new ObjectMapper(),
                userRepository
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

        org.mockito.Mockito.when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Method method = ExportController.class.getDeclaredMethod("extractUserId", UserDetails.class);
        method.setAccessible(true);

        ExportController controller = new ExportController(
                null,
                (ExportConfig) null,
                (RateLimitService) null,
                new ObjectMapper(),
                userRepository
        );

        assertThatThrownBy(() -> method.invoke(controller, userDetails))
                .hasCauseExactlyInstanceOf(AuthenticationServiceException.class)
                .cause()
                .hasMessageContaining("Authenticated user not found");
    }
}
