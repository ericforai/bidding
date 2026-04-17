package com.xiyu.bid.admin.settings.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataScopePolicyTest {

    @Test
    void resolveAccessProfile_ShouldPreferUserRuleOverDepartmentAndRoleRules() {
        DepartmentGraph graph = DepartmentGraphPolicy.buildGraph(List.of(
                new DepartmentNode("SALES", "销售部", null, 1),
                new DepartmentNode("TECH", "技术部", "SALES", 2)
        ));

        CoreAccessProfile profile = DataScopePolicy.resolveAccessProfile(
                new UserAccessSubject(1L, "SALES"),
                List.of(new UserScopeRule(1L, "self", List.of(9L), List.of("TECH"))),
                List.of(new DepartmentScopeRule("SALES", "deptAndSub", List.of())),
                new RoleAccessRule("dept", List.of(), List.of()),
                graph
        );

        assertThat(profile.dataScope()).isEqualTo("self");
        assertThat(profile.explicitProjectIds()).containsExactly(9L);
        assertThat(profile.allowedDepartmentCodes()).isEmpty();
    }

    @Test
    void resolveAccessProfile_ShouldExpandDepartmentAndSubDepartments() {
        DepartmentGraph graph = DepartmentGraphPolicy.buildGraph(List.of(
                new DepartmentNode("SALES", "销售部", null, 1),
                new DepartmentNode("TECH", "技术部", "SALES", 2)
        ));

        CoreAccessProfile profile = DataScopePolicy.resolveAccessProfile(
                new UserAccessSubject(2L, "SALES"),
                List.of(),
                List.of(new DepartmentScopeRule("SALES", "deptAndSub", List.of("FIN"))),
                new RoleAccessRule("self", List.of(), List.of()),
                graph
        );

        assertThat(profile.allowedDepartmentCodes()).containsExactly("SALES", "TECH", "FIN");
    }
}
