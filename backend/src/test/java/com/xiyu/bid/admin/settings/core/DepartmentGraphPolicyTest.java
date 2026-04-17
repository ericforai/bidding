package com.xiyu.bid.admin.settings.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentGraphPolicyTest {

    @Test
    void buildGraph_ShouldNormalizeTreeAndExpandDescendants() {
        DepartmentGraph graph = DepartmentGraphPolicy.buildGraph(List.of(
                new DepartmentNode("SALES", "销售部", null, 1),
                new DepartmentNode("SALES_EAST", "华东销售", "SALES", 2)
        ));

        assertThat(graph.options()).extracting(DepartmentOption::code).containsExactly("SALES", "SALES_EAST");
        assertThat(graph.descendantsOf("SALES")).containsExactly("SALES", "SALES_EAST");
    }

    @Test
    void validateTree_ShouldRejectDuplicateCodeSelfParentAndCycles() {
        assertThat(DepartmentGraphPolicy.validateTree(List.of(
                new DepartmentNode("SALES", "销售部", null, 1),
                new DepartmentNode("SALES", "销售一部", null, 2)
        )).valid()).isFalse();

        assertThat(DepartmentGraphPolicy.validateTree(List.of(
                new DepartmentNode("SALES", "销售部", "SALES", 1)
        )).valid()).isFalse();

        assertThat(DepartmentGraphPolicy.validateTree(List.of(
                new DepartmentNode("A", "A部", "B", 1),
                new DepartmentNode("B", "B部", "A", 2)
        )).valid()).isFalse();
    }

    @Test
    void findRemovedBoundDepartments_ShouldReturnDepartmentsStillAssignedToUsers() {
        List<DepartmentNode> nextTree = List.of(new DepartmentNode("SALES", "销售部", null, 1));

        assertThat(DepartmentGraphPolicy.findRemovedBoundDepartments(nextTree, Set.of("SALES", "TECH")))
                .containsExactly("TECH");
    }
}
