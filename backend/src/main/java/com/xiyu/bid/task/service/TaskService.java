// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.task.service;

import com.xiyu.bid.task.dto.TaskDTO;
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

    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasks() {
        log.debug("Fetching all tasks");
        return taskRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long id) {
        log.debug("Fetching task by id: {}", id);
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));
        return toDTO(task);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        log.info("Updating task: {}", id);
        Task existingTask = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));
        if (taskDTO.getTitle() != null) existingTask.setTitle(taskDTO.getTitle());
        if (taskDTO.getDescription() != null) existingTask.setDescription(taskDTO.getDescription());
        if (taskDTO.getAssigneeId() != null) existingTask.setAssigneeId(taskDTO.getAssigneeId());
        if (taskDTO.getStatus() != null) existingTask.setStatus(taskDTO.getStatus());
        if (taskDTO.getPriority() != null) existingTask.setPriority(taskDTO.getPriority());
        if (taskDTO.getDueDate() != null) existingTask.setDueDate(taskDTO.getDueDate());
        Task updatedTask = taskRepository.save(existingTask);
        log.info("Task updated successfully: {}", id);
        return toDTO(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        log.info("Deleting task: {}", id);
        if (!taskRepository.existsById(id)) throw new ResourceNotFoundException("Task", id.toString());
        taskRepository.deleteById(id);
        log.info("Task deleted successfully: {}", id);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        log.debug("Fetching tasks for project: {}", projectId);
        return taskRepository.findByProjectId(projectId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByAssigneeId(Long assigneeId) {
        log.debug("Fetching tasks for assignee: {}", assigneeId);
        return taskRepository.findByAssigneeId(assigneeId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByStatus(Task.Status status) {
        log.debug("Fetching tasks with status: {}", status);
        return taskRepository.findByStatus(status).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO updateTaskStatus(Long id, Task.Status status) {
        log.info("Updating task {} status to: {}", id, status);
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));
        task.setStatus(status);
        return toDTO(taskRepository.save(task));
    }

    @Transactional
    public TaskDTO assignTask(Long id, Long assigneeId) {
        log.info("Assigning task {} to user: {}", id, assigneeId);
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));
        task.setAssigneeId(assigneeId);
        return toDTO(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getUpcomingTasks(LocalDateTime beforeDate) {
        log.debug("Fetching tasks due before: {}", beforeDate);
        return taskRepository.findByDueDateBefore(beforeDate).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getOverdueTasks() {
        log.debug("Fetching overdue tasks");
        return taskRepository.findByDueDateBeforeAndStatusNot(LocalDateTime.now(), Task.Status.COMPLETED).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Long countProjectTasks(Long projectId) { return taskRepository.countByProjectId(projectId); }
    public Long countUserTasks(Long assigneeId) { return taskRepository.countByAssigneeId(assigneeId); }

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
