package com.xiyu.bid.entity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RoleProfileCatalog {

    public static final String ADMIN_CODE = "admin";
    public static final String AUDITOR_CODE = "auditor";
    public static final String MANAGER_CODE = "manager";
    public static final String STAFF_CODE = "staff";
    public static final String QUICK_START_PERMISSION = "dashboard.quickStart";
    public static final String AI_CENTER_PERMISSION = "ai-center";

    private static final Map<String, SeedDefinition> DEFINITIONS = Map.of(
            ADMIN_CODE, new SeedDefinition(ADMIN_CODE, "管理员", "系统管理员，拥有所有权限", true, "all", List.of("all")),
            AUDITOR_CODE, new SeedDefinition(AUDITOR_CODE, "审计员", "审计人员，可查看全量审计日志和个人操作日志", true, "all",
                    List.of("dashboard", "operation-logs", "audit-logs")),
            MANAGER_CODE, new SeedDefinition(MANAGER_CODE, "经理", "部门经理，可查看项目、知识库、资源与分析数据", true, "dept",
                    List.of("dashboard", "operation-logs", "bidding", "project", "knowledge", "resource",
                            AI_CENTER_PERMISSION, "analytics", "settings")),
            STAFF_CODE, new SeedDefinition(STAFF_CODE, "员工", "业务人员，可查看工作台、标讯、项目、知识库与资源", true, "self",
                    List.of("dashboard", "operation-logs", QUICK_START_PERMISSION, "bidding", "project", "knowledge",
                            "resource", AI_CENTER_PERMISSION))
    );

    private RoleProfileCatalog() {
    }

    public static List<SeedDefinition> seedDefinitions() {
        return List.of(
                DEFINITIONS.get(ADMIN_CODE),
                DEFINITIONS.get(AUDITOR_CODE),
                DEFINITIONS.get(MANAGER_CODE),
                DEFINITIONS.get(STAFF_CODE)
        );
    }

    public static SeedDefinition definitionForCode(String roleCode) {
        if (roleCode == null) {
            return DEFINITIONS.get(STAFF_CODE);
        }
        return DEFINITIONS.getOrDefault(roleCode.trim().toLowerCase(Locale.ROOT), DEFINITIONS.get(STAFF_CODE));
    }

    public static SeedDefinition definitionForLegacyRole(User.Role role) {
        if (role == null) {
            return DEFINITIONS.get(STAFF_CODE);
        }
        return switch (role) {
            case ADMIN -> DEFINITIONS.get(ADMIN_CODE);
            case MANAGER -> DEFINITIONS.get(MANAGER_CODE);
            case STAFF -> DEFINITIONS.get(STAFF_CODE);
        };
    }

    public static User.Role legacyRoleForCode(String roleCode) {
        String normalizedCode = roleCode == null ? STAFF_CODE : roleCode.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedCode) {
            case ADMIN_CODE -> User.Role.ADMIN;
            case MANAGER_CODE -> User.Role.MANAGER;
            default -> User.Role.STAFF;
        };
    }

    public record SeedDefinition(
            String code,
            String name,
            String description,
            boolean system,
            String dataScope,
            List<String> menuPermissions
    ) {
    }
}
