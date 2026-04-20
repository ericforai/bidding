package com.xiyu.bid.batch.service;

import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import com.xiyu.bid.task.dto.TaskAssignmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchTaskAssignmentResolverTest {

    private UserRepository userRepository;
    private ProjectAccessScopeService projectAccessScopeService;
    private BatchTaskAssignmentResolver batchTaskAssignmentResolver;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        projectAccessScopeService = mock(ProjectAccessScopeService.class);
        batchTaskAssignmentResolver = new BatchTaskAssignmentResolver(userRepository, projectAccessScopeService);
    }

    @Test
    void resolveAssignment_ShouldReturnSnapshotFromActiveAssignee() {
        User assignee = User.builder()
                .id(20L)
                .enabled(true)
                .role(User.Role.STAFF)
                .departmentCode("D1")
                .departmentName("研发部")
                .build();
        when(userRepository.findById(20L)).thenReturn(Optional.of(assignee));

        BatchTaskAssignmentResolver.AssignmentSnapshot snapshot = batchTaskAssignmentResolver.resolveAssignment(
                TaskAssignmentRequest.builder().assigneeId(20L).build(),
                null
        );

        assertThat(snapshot.assigneeId()).isEqualTo(20L);
        assertThat(snapshot.assigneeDeptCode()).isEqualTo("D1");
    }

    @Test
    void resolveAssignment_ShouldRejectDisabledAssignee() {
        User assignee = User.builder().id(20L).enabled(false).build();
        when(userRepository.findById(20L)).thenReturn(Optional.of(assignee));

        assertThatThrownBy(() -> batchTaskAssignmentResolver.resolveAssignment(
                TaskAssignmentRequest.builder().assigneeId(20L).build(),
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已停用");
    }

    @Test
    void resolveAssignment_ShouldRejectCrossDepartmentWithoutPermission() {
        User currentUser = User.builder()
                .id(1L)
                .role(User.Role.STAFF)
                .departmentCode("D1")
                .build();
        when(projectAccessScopeService.getAllowedDepartmentCodes(currentUser)).thenReturn(List.of("D1"));

        assertThatThrownBy(() -> batchTaskAssignmentResolver.resolveAssignment(
                TaskAssignmentRequest.builder().assigneeDeptCode("D2").build(),
                currentUser
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权向该部门分配任务");
    }
}
