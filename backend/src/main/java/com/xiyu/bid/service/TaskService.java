// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.service;

import com.xiyu.bid.dto.TaskDTO;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    /**
     * 创建任务
     */
    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        log.info("Creating task: {}", taskDTO.getTitle());

        Task task = Task.builder()
                .projectId(taskDTO.getProjectId())
                .title(taskDTO.getTitle())
                .description(taskDTO.getDescription())
                .assigneeId(taskDTO.getAssigneeId())
                .status(taskDTO.getStatus() != null ? taskDTO.getStatus() : Task.Status.TODO)
                .priority(taskDTO.getPriority() != null ? taskDTO.getPriority() : Task.Priority.MEDIUM)
                .dueDate(taskDTO.getDueDate())
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with id: {}", savedTask.getId());

        return toDTO(savedTask);
    }

    /**
     * 获取所有任务
     */
    public List<TaskDTO> getAllTasks() {
        log.debug("Fetching all tasks");
        return taskRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取任务
     */
    public TaskDTO getTaskById(Long id) {
        log.debug("Fetching task by id: {}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));
        return toDTO(task);
    }

    /**
     * 更新任务
     */
    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        log.info("Updating task: {}", id);

        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));

        if (taskDTO.getTitle() != null) {
            existingTask.setTitle(taskDTO.getTitle());
        }
        if (taskDTO.getDescription() != null) {
            existingTask.setDescription(taskDTO.getDescription());
        }
        if (taskDTO.getAssigneeId() != null) {
            existingTask.setAssigneeId(taskDTO.getAssigneeId());
        }
        if (taskDTO.getStatus() != null) {
            existingTask.setStatus(taskDTO.getStatus());
        }
        if (taskDTO.getPriority() != null) {
            existingTask.setPriority(taskDTO.getPriority());
        }
        if (taskDTO.getDueDate() != null) {
            existingTask.setDueDate(taskDTO.getDueDate());
        }

        Task updatedTask = taskRepository.save(existingTask);
        log.info("Task updated successfully: {}", id);

        return toDTO(updatedTask);
    }

    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long id) {
        log.info("Deleting task: {}", id);

        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id.toString());
        }

        taskRepository.deleteById(id);
        log.info("Task deleted successfully: {}", id);
    }

    /**
     * 根据项目ID获取任务
     */
    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        log.debug("Fetching tasks for project: {}", projectId);
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据受托人ID获取任务
     */
    public List<TaskDTO> getTasksByAssigneeId(Long assigneeId) {
        log.debug("Fetching tasks for assignee: {}", assigneeId);
        return taskRepository.findByAssigneeId(assigneeId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据状态获取任务
     */
    public List<TaskDTO> getTasksByStatus(Task.Status status) {
        log.debug("Fetching tasks with status: {}", status);
        return taskRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public TaskDTO updateTaskStatus(Long id, Task.Status status) {
        log.info("Updating task {} status to: {}", id, status);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));

        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);

        return toDTO(updatedTask);
    }

    /**
     * 分配任务给用户
     */
    @Transactional
    public TaskDTO assignTask(Long id, Long assigneeId) {
        log.info("Assigning task {} to user: {}", id, assigneeId);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));

        task.setAssigneeId(assigneeId);
        Task updatedTask = taskRepository.save(task);

        return toDTO(updatedTask);
    }

    /**
     * 获取即将到期的任务
     */
    public List<TaskDTO> getUpcomingTasks(LocalDateTime beforeDate) {
        log.debug("Fetching tasks due before: {}", beforeDate);
        return taskRepository.findByDueDateBefore(beforeDate).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取已过期但未完成的任务
     */
    public List<TaskDTO> getOverdueTasks() {
        log.debug("Fetching overdue tasks");
        return taskRepository.findByDueDateBeforeAndStatusNot(
                LocalDateTime.now(), Task.Status.COMPLETED).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计项目任务数量
     */
    public Long countProjectTasks(Long projectId) {
        return taskRepository.countByProjectId(projectId);
    }

    /**
     * 统计用户任务数量
     */
    public Long countUserTasks(Long assigneeId) {
        return taskRepository.countByAssigneeId(assigneeId);
    }

    /**
     * 转换为DTO
     */
    private TaskDTO toDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .assigneeId(task.getAssigneeId())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
