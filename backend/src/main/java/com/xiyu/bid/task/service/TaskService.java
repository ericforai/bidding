// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.task.service;

import com.xiyu.bid.task.dto.TaskDTO;
import com.xiyu.bid.task.dto.TaskAssignmentCandidateDTO;
import com.xiyu.bid.task.dto.TaskAssignmentRequest;
import com.xiyu.bid.task.dto.TeamTaskWorkloadDTO;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import com.xiyu.bid.service.RoleProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectAccessScopeService projectAccessScopeService;
    private final RoleProfileService roleProfileService;

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        log.info("Creating task: {}", taskDTO.getTitle());
        AssignmentSnapshot assignment = resolveAssignmentSnapshot(TaskAssignmentRequest.builder()
                .assigneeId(taskDTO.getAssigneeId())
                .assigneeDeptCode(taskDTO.getAssigneeDeptCode())
                .assigneeDeptName(taskDTO.getAssigneeDeptName())
                .assigneeRoleCode(taskDTO.getAssigneeRoleCode())
                .assigneeRoleName(taskDTO.getAssigneeRoleName())
                .build(), null);
        Task task = Task.builder()
                .projectId(taskDTO.getProjectId())
                .title(taskDTO.getTitle())
                .description(taskDTO.getDescription())
                .assigneeId(assignment.assigneeId())
                .assigneeDeptCode(assignment.assigneeDeptCode())
                .assigneeDeptName(assignment.assigneeDeptName())
                .assigneeRoleCode(assignment.assigneeRoleCode())
                .assigneeRoleName(assignment.assigneeRoleName())
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
        if (taskDTO.getAssigneeId() != null
                || hasText(taskDTO.getAssigneeDeptCode())
                || hasText(taskDTO.getAssigneeRoleCode())) {
            AssignmentSnapshot assignment = resolveAssignmentSnapshot(TaskAssignmentRequest.builder()
                    .assigneeId(taskDTO.getAssigneeId())
                    .assigneeDeptCode(taskDTO.getAssigneeDeptCode())
                    .assigneeDeptName(taskDTO.getAssigneeDeptName())
                    .assigneeRoleCode(taskDTO.getAssigneeRoleCode())
                    .assigneeRoleName(taskDTO.getAssigneeRoleName())
                    .build(), null);
            applyAssignment(existingTask, assignment);
        }
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
    public List<TaskDTO> getAccessibleTasksByAssigneeId(Long assigneeId, String username) {
        User currentUser = resolveEnabledUserByUsername(username);
        if (assigneeId == null || Objects.equals(currentUser.getId(), assigneeId)) {
            return getTasksByAssigneeId(currentUser.getId());
        }

        User targetUser = resolveEnabledUserById(assigneeId);
        assertCanAccessTargetUser(currentUser, targetUser, false);
        return getTasksByAssigneeId(assigneeId);
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
    public TaskDTO assignTask(Long id, TaskAssignmentRequest request, String username) {
        log.info("Assigning task {} to user: {}", id, request == null ? null : request.getAssigneeId());
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));
        User currentUser = resolveEnabledUserByUsername(username);
        AssignmentSnapshot assignment = resolveAssignmentSnapshot(request, currentUser);
        applyAssignment(task, assignment);
        return toDTO(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskAssignmentCandidateDTO> getAssignmentCandidates(String deptCode, String roleCode, String username) {
        User currentUser = resolveEnabledUserByUsername(username);
        List<String> allowedDeptCodes = normalizeAllowedDeptCodes(currentUser);
        String normalizedDeptCode = trimToNull(deptCode);
        String normalizedRoleCode = trimToNull(roleCode);

        return userRepository.findByEnabledTrue().stream()
                .filter(user -> canSeeCandidate(currentUser, user, allowedDeptCodes))
                .filter(user -> normalizedDeptCode == null || normalizedDeptCode.equalsIgnoreCase(user.getDepartmentCode()))
                .filter(user -> normalizedRoleCode == null || normalizedRoleCode.equalsIgnoreCase(user.getRoleCode()))
                .sorted(Comparator.comparing(User::getDepartmentCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(User::getRoleName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(User::getFullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(user -> TaskAssignmentCandidateDTO.builder()
                        .userId(user.getId())
                        .name(user.getFullName())
                        .roleCode(user.getRoleCode())
                        .roleName(user.getRoleName())
                        .deptCode(defaultText(user.getDepartmentCode(), "UNASSIGNED"))
                        .deptName(defaultText(user.getDepartmentName(), "未配置部门"))
                        .enabled(Boolean.TRUE.equals(user.getEnabled()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamTaskWorkloadDTO getTeamTaskWorkload(String username) {
        User currentUser = resolveEnabledUserByUsername(username);
        List<String> allowedDeptCodes = normalizeAllowedDeptCodes(currentUser);

        if (!roleProfileService.isAdminRole(currentUser)
                && !"manager".equalsIgnoreCase(currentUser.getRoleCode())
                && !"部门主管".equals(currentUser.getRoleName())) {
            return TeamTaskWorkloadDTO.builder()
                    .scope("self")
                    .orgConfigured(hasText(currentUser.getDepartmentCode()))
                    .emptyReason("仅管理角色可查看团队任务分配")
                    .members(List.of())
                    .build();
        }

        if (allowedDeptCodes.isEmpty()) {
            return TeamTaskWorkloadDTO.builder()
                    .scope(roleProfileService.isAdminRole(currentUser) ? "all" : "dept")
                    .orgConfigured(false)
                    .emptyReason("未配置组织关系")
                    .members(List.of())
                    .build();
        }

        List<User> visibleUsers = userRepository.findByEnabledTrue().stream()
                .filter(user -> allowedDeptCodes.contains(defaultText(user.getDepartmentCode(), "")))
                .sorted(Comparator.comparing(User::getDepartmentCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(User::getRoleName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(User::getFullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        if (visibleUsers.isEmpty()) {
            return TeamTaskWorkloadDTO.builder()
                    .scope(roleProfileService.isAdminRole(currentUser) ? "all" : "dept")
                    .orgConfigured(true)
                    .emptyReason("当前范围内无团队成员")
                    .members(List.of())
                    .build();
        }

        List<Long> userIds = visibleUsers.stream().map(User::getId).toList();
        Map<Long, List<Task>> tasksByAssignee = taskRepository.findByAssigneeIdIn(userIds).stream()
                .collect(Collectors.groupingBy(Task::getAssigneeId));

        List<TeamTaskWorkloadDTO.TeamMemberWorkloadDTO> members = visibleUsers.stream()
                .map(user -> buildTeamMemberWorkload(user, tasksByAssignee.getOrDefault(user.getId(), List.of())))
                .toList();

        String emptyReason = members.stream().allMatch(member -> member.getTasks().isEmpty())
                ? "暂无任务数据"
                : null;

        return TeamTaskWorkloadDTO.builder()
                .scope(roleProfileService.isAdminRole(currentUser) ? "all" : "dept")
                .orgConfigured(true)
                .emptyReason(emptyReason)
                .members(members)
                .build();
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
                .assigneeDeptCode(task.getAssigneeDeptCode())
                .assigneeDeptName(task.getAssigneeDeptName())
                .assigneeRoleCode(task.getAssigneeRoleCode())
                .assigneeRoleName(task.getAssigneeRoleName())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private TeamTaskWorkloadDTO.TeamMemberWorkloadDTO buildTeamMemberWorkload(User user, List<Task> tasks) {
        long todoCount = tasks.stream().filter(task -> task.getStatus() == Task.Status.TODO).count();
        long inProgressCount = tasks.stream().filter(task -> task.getStatus() == Task.Status.IN_PROGRESS).count();
        long overdueCount = tasks.stream().filter(this::isOverdue).count();
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        long completedThisWeekCount = tasks.stream()
                .filter(task -> task.getStatus() == Task.Status.COMPLETED)
                .filter(task -> task.getUpdatedAt() != null && !task.getUpdatedAt().isBefore(weekStart))
                .count();

        int workloadScore = tasks.stream().mapToInt(this::calculateWorkloadScore).sum();
        String workloadLevel = workloadScore >= 10 ? "high" : workloadScore >= 5 ? "medium" : "low";

        List<TeamTaskWorkloadDTO.MemberTaskSummaryDTO> summaries = tasks.stream()
                .sorted(Comparator
                        .comparing((Task task) -> !isOverdue(task))
                        .thenComparing((Task task) -> !isDueSoon(task))
                        .thenComparing((Task task) -> task.getPriority() == null ? Integer.MAX_VALUE : task.getPriority().ordinal())
                        .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(3)
                .map(task -> TeamTaskWorkloadDTO.MemberTaskSummaryDTO.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .priority(task.getPriority() == null ? "MEDIUM" : task.getPriority().name())
                        .status(task.getStatus() == null ? "TODO" : task.getStatus().name())
                        .deadline(task.getDueDate() == null ? "待排期" : task.getDueDate().format(DISPLAY_DATE_TIME))
                        .overdue(isOverdue(task))
                        .dueSoon(isDueSoon(task))
                        .build())
                .toList();

        return TeamTaskWorkloadDTO.TeamMemberWorkloadDTO.builder()
                .userId(user.getId())
                .name(user.getFullName())
                .roleCode(user.getRoleCode())
                .roleName(user.getRoleName())
                .deptCode(defaultText(user.getDepartmentCode(), "UNASSIGNED"))
                .deptName(defaultText(user.getDepartmentName(), "未配置部门"))
                .todoCount(todoCount)
                .inProgressCount(inProgressCount)
                .overdueCount(overdueCount)
                .completedThisWeekCount(completedThisWeekCount)
                .workloadScore(workloadScore)
                .workloadLevel(workloadLevel)
                .tasks(summaries)
                .build();
    }

    private int calculateWorkloadScore(Task task) {
        int score = switch (task.getPriority() == null ? Task.Priority.MEDIUM : task.getPriority()) {
            case URGENT -> 3;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
        if (isOverdue(task)) {
            score += 2;
        }
        if (isDueSoon(task)) {
            score += 1;
        }
        return score;
    }

    private boolean isOverdue(Task task) {
        return task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDateTime.now())
                && task.getStatus() != Task.Status.COMPLETED;
    }

    private boolean isDueSoon(Task task) {
        return task.getDueDate() != null
                && !isOverdue(task)
                && task.getDueDate().isBefore(LocalDateTime.now().plusHours(48));
    }

    private void applyAssignment(Task task, AssignmentSnapshot assignment) {
        task.setAssigneeId(assignment.assigneeId());
        task.setAssigneeDeptCode(assignment.assigneeDeptCode());
        task.setAssigneeDeptName(assignment.assigneeDeptName());
        task.setAssigneeRoleCode(assignment.assigneeRoleCode());
        task.setAssigneeRoleName(assignment.assigneeRoleName());
    }

    private AssignmentSnapshot resolveAssignmentSnapshot(TaskAssignmentRequest request, User currentUser) {
        if (request == null || !request.hasAssignmentTarget()) {
            return AssignmentSnapshot.empty();
        }

        if (request.getAssigneeId() != null) {
            User assignee = resolveEnabledUserById(request.getAssigneeId());
            if (currentUser != null) {
                assertCanAccessTargetUser(currentUser, assignee, Boolean.TRUE.equals(request.getAllowCrossDeptCollaboration()));
            }
            return AssignmentSnapshot.fromUser(assignee);
        }

        String deptCode = trimToNull(request.getAssigneeDeptCode());
        String deptName = trimToNull(request.getAssigneeDeptName());
        String roleCode = trimToNull(request.getAssigneeRoleCode());
        String roleName = trimToNull(request.getAssigneeRoleName());

        if (currentUser != null && !Boolean.TRUE.equals(request.getAllowCrossDeptCollaboration())) {
            List<String> allowedDeptCodes = normalizeAllowedDeptCodes(currentUser);
            if (deptCode != null && !allowedDeptCodes.contains(deptCode)) {
                throw new AccessDeniedException("当前用户无权向该部门分配任务");
            }
        }

        return new AssignmentSnapshot(null, deptCode, defaultText(deptName, "未配置部门"), roleCode, roleName);
    }

    private void assertCanAccessTargetUser(User currentUser, User targetUser, boolean allowCrossDeptCollaboration) {
        if (roleProfileService.isAdminRole(currentUser)) {
            return;
        }

        if (!Boolean.TRUE.equals(targetUser.getEnabled())) {
            throw new IllegalArgumentException("目标责任人已停用，无法分配");
        }

        List<String> allowedDeptCodes = normalizeAllowedDeptCodes(currentUser);
        String targetDeptCode = defaultText(targetUser.getDepartmentCode(), "");
        if (allowCrossDeptCollaboration) {
            if (allowedDeptCodes.isEmpty()) {
                return;
            }
            if (!allowedDeptCodes.contains(targetDeptCode)) {
                throw new AccessDeniedException("跨部门协作未在当前数据权限范围内");
            }
            return;
        }

        if (!allowedDeptCodes.isEmpty() && !allowedDeptCodes.contains(targetDeptCode)) {
            throw new AccessDeniedException("当前用户无权查看或分配该部门任务");
        }
    }

    private boolean canSeeCandidate(User currentUser, User candidate, List<String> allowedDeptCodes) {
        if (!Boolean.TRUE.equals(candidate.getEnabled())) {
            return false;
        }
        if (roleProfileService.isAdminRole(currentUser)) {
            return true;
        }
        if (allowedDeptCodes.isEmpty()) {
            return Objects.equals(currentUser.getId(), candidate.getId());
        }
        return allowedDeptCodes.contains(defaultText(candidate.getDepartmentCode(), ""));
    }

    private User resolveEnabledUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("当前用户不存在"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AccessDeniedException("当前用户已停用");
        }
        return user;
    }

    private User resolveEnabledUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("目标责任人已停用，无法分配");
        }
        return user;
    }

    private List<String> normalizeAllowedDeptCodes(User user) {
        LinkedHashSet<String> allowedDeptCodes = new LinkedHashSet<>(projectAccessScopeService.getAllowedDepartmentCodes(user));
        if (hasText(user.getDepartmentCode())) {
            allowedDeptCodes.add(user.getDepartmentCode().trim());
        }
        return allowedDeptCodes.stream().filter(this::hasText).toList();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private record AssignmentSnapshot(
            Long assigneeId,
            String assigneeDeptCode,
            String assigneeDeptName,
            String assigneeRoleCode,
            String assigneeRoleName
    ) {
        private static AssignmentSnapshot empty() {
            return new AssignmentSnapshot(null, null, null, null, null);
        }

        private static AssignmentSnapshot fromUser(User user) {
            return new AssignmentSnapshot(
                    user.getId(),
                    user.getDepartmentCode(),
                    user.getDepartmentName(),
                    user.getRoleCode(),
                    user.getRoleName()
            );
        }
    }
}
