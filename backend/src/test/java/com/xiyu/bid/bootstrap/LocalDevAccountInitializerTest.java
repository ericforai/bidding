package com.xiyu.bid.bootstrap;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalDevAccountInitializerTest {

    @Test
    void seedLocalAccountsShouldCreateStaffAndManagerLoginUsers() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleProfileRepository roleProfileRepository = mock(RoleProfileRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        LocalDevAccountInitializer initializer =
                new LocalDevAccountInitializer(userRepository, roleProfileRepository, passwordEncoder);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(roleProfileRepository.findByCodeIgnoreCase("staff"))
                .thenReturn(Optional.of(RoleProfile.builder().id(3L).code("staff").build()));
        when(roleProfileRepository.findByCodeIgnoreCase("manager"))
                .thenReturn(Optional.of(RoleProfile.builder().id(2L).code("manager").build()));
        when(passwordEncoder.encode(LocalDevAccountInitializer.LOCAL_TEST_PASSWORD)).thenReturn("encoded-test-password");

        initializer.seedLocalAccounts();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        verify(passwordEncoder, times(2)).encode(LocalDevAccountInitializer.LOCAL_TEST_PASSWORD);

        List<User> savedUsers = userCaptor.getAllValues();
        assertThat(savedUsers)
                .extracting(User::getUsername)
                .containsExactly("staff", "manager");
        assertThat(savedUsers)
                .extracting(User::getFullName)
                .containsExactly("小王", "张经理");
        assertThat(savedUsers)
                .extracting(User::getRole)
                .containsExactly(User.Role.STAFF, User.Role.MANAGER);
        assertThat(savedUsers)
                .extracting(User::getEnabled)
                .containsOnly(true);
        assertThat(savedUsers)
                .extracting(User::getPassword)
                .containsOnly("encoded-test-password");
    }
}
