package com.xiyu.bid.batch.service;

import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import com.xiyu.bid.task.dto.TaskAssignmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
class BatchTaskAssignmentResolver {

    private final UserRepository userRepository;
    private final ProjectAccessScopeService projectAccessScopeService;

    AssignmentSnapshot resolveAssignment(TaskAssignmentRequest request, User currentUser) {
        if (request == null || !request.hasAssignmentTarget()) {
            throw new IllegalArgumentException("Assignment target cannot be empty");
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found"));
            if (!Boolean.TRUE.equals(assignee.getEnabled())) {
                throw new IllegalArgumentException("目标责任人已停用，无法分配");
            }
            assertDeptAccess(currentUser, assignee.getDepartmentCode(), Boolean.TRUE.equals(request.getAllowCrossDeptCollaboration()));
            return AssignmentSnapshot.fromUser(assignee);
        }

        assertDeptAccess(currentUser, request.getAssigneeDeptCode(), Boolean.TRUE.equals(request.getAllowCrossDeptCollaboration()));
        return new AssignmentSnapshot(
                null,
                normalizeText(request.getAssigneeDeptCode()),
                normalizeText(request.getAssigneeDeptName(), "未配置部门"),
                normalizeText(request.getAssigneeRoleCode()),
                normalizeText(request.getAssigneeRoleName())
        );
    }

    private void assertDeptAccess(User currentUser, String targetDeptCode, boolean allowCrossDeptCollaboration) {
        if (currentUser == null || isAdmin(currentUser)) {
            return;
        }
        List<String> allowedDeptCodes = new ArrayList<>(projectAccessScopeService.getAllowedDepartmentCodes(currentUser));
        if (currentUser.getDepartmentCode() != null && !currentUser.getDepartmentCode().isBlank()) {
            allowedDeptCodes.add(currentUser.getDepartmentCode().trim());
        }
        String normalizedTargetDept = normalizeText(targetDeptCode);
        if (normalizedTargetDept == null || allowedDeptCodes.isEmpty()) {
            return;
        }
        if (!allowedDeptCodes.contains(normalizedTargetDept)) {
            throw new IllegalArgumentException(allowCrossDeptCollaboration
                    ? "跨部门协作不在当前数据权限范围内"
                    : "当前用户无权向该部门分配任务");
        }
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equalsIgnoreCase(user.getRoleCode());
    }

    private String normalizeText(String value) {
        return normalizeText(value, null);
    }

    private String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    record AssignmentSnapshot(
            Long assigneeId,
            String assigneeDeptCode,
            String assigneeDeptName,
            String assigneeRoleCode,
            String assigneeRoleName
    ) {
        static AssignmentSnapshot fromUser(User user) {
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
