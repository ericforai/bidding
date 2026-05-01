package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.entity.RoleProfile;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;
import com.xiyu.bid.repository.RoleProfileRepository;
import com.xiyu.bid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationUserSyncWriter - user persistence")
class OrganizationUserSyncWriterTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleProfileRepository roleProfileRepository;

    private OrganizationUserSyncWriter writer;

    @BeforeEach
    void setUp() {
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        writer = new OrganizationUserSyncWriter(userRepository, roleProfileRepository, properties);
    }

    @Test
    @DisplayName("upsert maps external user id without using it as username")
    void upsert_mapsExternalUserIdSeparately() {
        when(userRepository.findByExternalOrgSourceAppAndExternalOrgUserId("customer-org", "10001")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("zhangsan")).thenReturn(Optional.empty());
        when(roleProfileRepository.findByCodeIgnoreCase("staff")).thenReturn(Optional.of(role("staff")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        writer.upsert("customer-org", "event-key", new OrganizationUserSnapshot(
                "10001", "zhangsan", "张三", "zhangsan@example.com",
                "13800000000", "sales", "销售部", "", true
        ));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("zhangsan");
        assertThat(saved.getValue().getExternalOrgUserId()).isEqualTo("10001");
        assertThat(saved.getValue().getLastOrgEventKey()).isEqualTo("event-key");
    }

    private RoleProfile role(String code) {
        RoleProfile role = new RoleProfile();
        role.setCode(code);
        role.setName(code);
        role.setEnabled(true);
        return role;
    }
}
