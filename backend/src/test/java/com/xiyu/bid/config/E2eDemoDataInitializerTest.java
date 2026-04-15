package com.xiyu.bid.config;

import com.xiyu.bid.entity.RoleProfile;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.RoleProfileRepository;
import com.xiyu.bid.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class E2eDemoDataInitializerTest {

    @Test
    void seedDemoUsers_ShouldCreateExpectedUsersWithEncodedPasswords() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RoleProfileRepository roleProfileRepository = mock(RoleProfileRepository.class);
        E2eDemoDataInitializer initializer = new E2eDemoDataInitializer(userRepository, roleProfileRepository, passwordEncoder);

        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(roleProfileRepository.findByCodeIgnoreCase(anyString()))
                .thenReturn(Optional.of(RoleProfile.builder().id(1L).code("TEST").build()));
        when(passwordEncoder.encode("123456")).thenReturn("encoded-123456");

        initializer.seedDemoUsers();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(userCaptor.capture());
        verify(passwordEncoder, times(3)).encode("123456");

        List<User> savedUsers = userCaptor.getAllValues();
        assertThat(savedUsers)
                .extracting(User::getUsername)
                .containsExactly("lizong", "zhangjingli", "xiaowang");
        assertThat(savedUsers)
                .extracting(User::getPassword)
                .containsOnly("encoded-123456");
        assertThat(savedUsers)
                .extracting(User::getEnabled)
                .containsOnly(true);
    }
}
