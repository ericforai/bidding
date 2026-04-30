package com.xiyu.bid.roleprofile;

import com.xiyu.bid.entity.RoleProfile;
import com.xiyu.bid.entity.RoleProfileCatalog;
import com.xiyu.bid.repository.RoleProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleProfileBootstrapTest {

    @Mock
    private RoleProfileRepository roleProfileRepository;

    @InjectMocks
    private RoleProfileBootstrap roleProfileBootstrap;

    @Test
    void ensureSystemRolesShouldAppendMissingSeedPermissions() {
        Map<String, RoleProfile> roles = seedRoles();
        RoleProfile manager = roles.get(RoleProfileCatalog.MANAGER_CODE);
        manager.setMenuPermissions(List.of("dashboard"));
        when(roleProfileRepository.findByCodeIgnoreCase(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(roles.get(invocation.getArgument(0))));

        roleProfileBootstrap.ensureSystemRoles();

        assertThat(manager.getMenuPermissions()).contains("dashboard", "operation-logs");
        verify(roleProfileRepository).save(manager);
    }

    private Map<String, RoleProfile> seedRoles() {
        Map<String, RoleProfile> roles = new HashMap<>();
        RoleProfileCatalog.seedDefinitions().forEach(definition -> {
            RoleProfile role = RoleProfile.builder()
                    .code(definition.code())
                    .name(definition.name())
                    .isSystem(true)
                    .enabled(true)
                    .dataScope(definition.dataScope())
                    .build();
            role.setMenuPermissions(definition.menuPermissions());
            roles.put(definition.code(), role);
        });
        return roles;
    }
}
