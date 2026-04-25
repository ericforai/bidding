package com.xiyu.bid.task.core;

import com.xiyu.bid.entity.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskProjectVisibilityPolicyTest {

    @Test
    void emptyAllowedProjectIdsMeansUnrestrictedAccess() {
        assertThat(TaskProjectVisibilityPolicy.canAccessProject(10L, List.of())).isTrue();
    }

    @Test
    void filtersTasksByAllowedProjectIds() {
        List<Task> tasks = List.of(task(1L, 10L), task(2L, 20L));

        List<Task> visibleTasks = TaskProjectVisibilityPolicy.filterVisibleTasks(tasks, List.of(10L));

        assertThat(visibleTasks).extracting(Task::getId).containsExactly(1L);
    }

    private Task task(Long id, Long projectId) {
        return Task.builder()
                .id(id)
                .projectId(projectId)
                .title("任务")
                .status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM)
                .build();
    }
}
