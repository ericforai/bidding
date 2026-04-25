package com.xiyu.bid.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleProfileCatalogTest {

    @Test
    @DisplayName("业务人员默认拥有工作台快速发起权限")
    void staffRoleShouldIncludeQuickStartPermission() {
        RoleProfileCatalog.SeedDefinition definition =
                RoleProfileCatalog.definitionForCode(RoleProfileCatalog.STAFF_CODE);

        assertThat(definition.menuPermissions())
                .contains("dashboard", RoleProfileCatalog.QUICK_START_PERMISSION);
    }
}
