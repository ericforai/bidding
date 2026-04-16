package com.xiyu.bid.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.dto.DataScopeConfigPayload;
import com.xiyu.bid.dto.DataScopeConfigResponse;
import com.xiyu.bid.entity.RoleProfile;
import com.xiyu.bid.entity.RoleProfileCatalog;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.RoleProfileRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.roleprofile.RoleProfileBootstrap;
import com.xiyu.bid.settings.entity.SystemSetting;
import com.xiyu.bid.settings.repository.SystemSettingRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataScopeConfigService {

    public static final String DATA_SCOPE_CONFIG_KEY = "data_scope_config";
    private static final String DEFAULT_SCOPE = "self";
    private static final String UNASSIGNED_DEPT_CODE = "UNASSIGNED";
    private static final String UNASSIGNED_DEPT_NAME = "未分配";
    private static final Set<String> ALLOWED_SCOPES = Set.of("all", "dept", "deptAndSub", "self");
    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;
    private final RoleProfileRepository roleProfileRepository;
    private final RoleProfileBootstrap roleProfileBootstrap;
    private final ObjectMapper objectMapper;

    public DataScopeConfigResponse getConfig() {
        List<User> users = loadUsers();
        DataScopeConfigPayload payload = loadPayload();
        DepartmentGraph departmentGraph = buildDepartmentGraph(users, payload.getDepartmentTree());

        Map<Long, DataScopeConfigPayload.UserScopeRule> userRules = payload.getUserRules().stream()
                .filter(rule -> rule.getUserId() != null)
                .collect(Collectors.toMap(
                        DataScopeConfigPayload.UserScopeRule::getUserId,
                        Function.identity(),
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
        Map<String, DataScopeConfigPayload.DepartmentScopeRule> departmentRules = payload.getDepartmentRules().stream()
                .filter(rule -> hasText(rule.getDepartmentCode()))
                .collect(Collectors.toMap(
                        rule -> normalizeDepartmentCode(rule.getDepartmentCode()),
                        Function.identity(),
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
        return DataScopeConfigResponse.builder()
                .userDataScope(users.stream()
                        .map(user -> toUserItem(user, userRules.get(user.getId())))
                        .toList())
                .deptDataScope(departmentGraph.options().stream()
                        .map(option -> toDepartmentItem(option, departmentRules.get(option.getCode())))
                        .toList())
                .deptOptions(departmentGraph.options())
                .deptTree(departmentGraph.tree())
                .userOptions(users.stream()
                        .map(this::toUserOptionItem)
                        .toList())
                .users(users.stream()
                        .map(this::toManagedUserItem)
                        .toList())
                .roles(buildRoleItems(users))
                .build();
    }

    @Transactional
    public DataScopeConfigResponse saveConfig(DataScopeConfigResponse request) {
        DataScopeConfigResponse safeRequest = request == null ? DataScopeConfigResponse.builder().build() : request;
        DataScopeConfigPayload payload = DataScopeConfigPayload.builder()
                .departmentTree(normalizeDepartmentTree(safeRequest.getDeptTree()))
                .userRules(normalizeUserRules(safeRequest.getUserDataScope()))
                .departmentRules(normalizeDepartmentRules(safeRequest.getDeptDataScope()))
                .build();

        SystemSetting setting = systemSettingRepository.findByConfigKey(DATA_SCOPE_CONFIG_KEY)
                .orElseGet(() -> SystemSetting.builder().configKey(DATA_SCOPE_CONFIG_KEY).build());
        setting.setPayloadJson(serializePayload(payload));
        systemSettingRepository.save(setting);
        return getConfig();
    }

    public AccessProfile getAccessProfile(User user) {
        if (user == null) {
            return AccessProfile.empty();
        }

        List<User> users = loadUsers();
        DataScopeConfigPayload payload = loadPayload();
        DepartmentGraph departmentGraph = buildDepartmentGraph(users, payload.getDepartmentTree());
        String ownDeptCode = normalizeDepartmentCode(user.getDepartmentCode());

        DataScopeConfigPayload.UserScopeRule userRule = payload.getUserRules().stream()
                .filter(rule -> Objects.equals(rule.getUserId(), user.getId()))
                .findFirst()
                .orElse(null);
        DataScopeConfigPayload.DepartmentScopeRule departmentRule = payload.getDepartmentRules().stream()
                .filter(rule -> normalizeDepartmentCode(rule.getDepartmentCode()).equals(ownDeptCode))
                .findFirst()
                .orElse(null);

        RoleProfile roleProfile = resolveRoleProfile(user);
        String dataScope = normalizeScope(userRule != null
                ? userRule.getDataScope()
                : departmentRule != null
                ? departmentRule.getDataScope()
                : roleProfile.getDataScope());
        List<Long> explicitProjectIds = userRule != null
                ? normalizeProjectIds(userRule.getAllowedProjectIds())
                : normalizeProjectIds(roleProfile.getAllowedProjects());
        List<String> explicitDeptCodes = normalizeDepartmentCodes(
                userRule != null
                        ? userRule.getAllowedDeptCodes()
                        : departmentRule != null
                        ? departmentRule.getAllowedDeptCodes()
                        : roleProfile.getAllowedDepts()
        );

        if ("all".equals(dataScope)) {
            return AccessProfile.builder()
                    .dataScope(dataScope)
                    .allowedDepartmentCodes(departmentGraph.options().stream()
                            .map(DataScopeConfigResponse.DepartmentOptionItem::getCode)
                            .toList())
                    .explicitProjectIds(explicitProjectIds)
                    .build();
        }

        LinkedHashSet<String> allowedDepartmentCodes = new LinkedHashSet<>();
        if ("dept".equals(dataScope) && !UNASSIGNED_DEPT_CODE.equals(ownDeptCode)) {
            allowedDepartmentCodes.add(ownDeptCode);
        }
        if ("deptAndSub".equals(dataScope) && !UNASSIGNED_DEPT_CODE.equals(ownDeptCode)) {
            allowedDepartmentCodes.addAll(departmentGraph.descendantsOf(ownDeptCode));
        }
        if (!"self".equals(dataScope)) {
            allowedDepartmentCodes.addAll(explicitDeptCodes);
        }

        return AccessProfile.builder()
                .dataScope(dataScope)
                .allowedDepartmentCodes(List.copyOf(allowedDepartmentCodes))
                .explicitProjectIds(explicitProjectIds)
                .build();
    }

    public List<String> getRoleMenuPermissions(User user) {
        return normalizeMenuPermissions(resolveRoleProfile(user).getMenuPermissions());
    }

    private List<User> loadUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private DataScopeConfigResponse.UserDataScopeItem toUserItem(User user, DataScopeConfigPayload.UserScopeRule rule) {
        return DataScopeConfigResponse.UserDataScopeItem.builder()
                .userId(user.getId())
                .userName(user.getFullName())
                .deptCode(normalizeDepartmentCode(user.getDepartmentCode()))
                .dept(normalizeDepartmentName(user.getDepartmentName()))
                .role(user.getRoleCode())
                .dataScope(normalizeScope(rule == null ? null : rule.getDataScope()))
                .allowedProjects(rule == null ? List.of() : normalizeProjectIds(rule.getAllowedProjectIds()))
                .allowedDepts(rule == null ? List.of() : normalizeDepartmentCodes(rule.getAllowedDeptCodes()))
                .build();
    }

    private DataScopeConfigResponse.DepartmentDataScopeItem toDepartmentItem(
            DataScopeConfigResponse.DepartmentOptionItem option,
            DataScopeConfigPayload.DepartmentScopeRule rule
    ) {
        return DataScopeConfigResponse.DepartmentDataScopeItem.builder()
                .deptCode(option.getCode())
                .deptName(option.getName())
                .dataScope(normalizeScope(rule == null ? null : rule.getDataScope()))
                .canViewOtherDepts(rule != null && rule.isCanViewOtherDepts())
                .allowedDepts(rule == null ? List.of() : normalizeDepartmentCodes(rule.getAllowedDeptCodes()))
                .build();
    }

    private DataScopeConfigResponse.UserOptionItem toUserOptionItem(User user) {
        return DataScopeConfigResponse.UserOptionItem.builder()
                .id(user.getId())
                .name(user.getFullName())
                .roleId(user.getRoleProfile() == null ? null : user.getRoleProfile().getId())
                .role(user.getRoleCode())
                .roleName(user.getRoleName())
                .deptCode(normalizeDepartmentCode(user.getDepartmentCode()))
                .dept(normalizeDepartmentName(user.getDepartmentName()))
                .build();
    }

    private DataScopeConfigResponse.UserItem toManagedUserItem(User user) {
        return DataScopeConfigResponse.UserItem.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .departmentCode(normalizeDepartmentCode(user.getDepartmentCode()))
                .departmentName(normalizeDepartmentName(user.getDepartmentName()))
                .roleId(user.getRoleProfile() == null ? null : user.getRoleProfile().getId())
                .role(user.getRoleCode())
                .roleName(user.getRoleName())
                .enabled(Boolean.TRUE.equals(user.getEnabled()))
                .build();
    }

    private DepartmentGraph buildDepartmentGraph(List<User> users, List<DataScopeConfigPayload.DepartmentNode> configuredTree) {
        Map<String, DepartmentDefinition> definitions = new LinkedHashMap<>();

        normalizeStoredDepartmentTree(configuredTree).forEach(node -> definitions.put(
                node.getDepartmentCode(),
                new DepartmentDefinition(
                        node.getDepartmentCode(),
                        normalizeDepartmentName(node.getDepartmentName()),
                        normalizeParentDepartmentCode(node.getParentDepartmentCode(), node.getDepartmentCode()),
                        node.getSortOrder() == null ? 0 : node.getSortOrder()
                )
        ));

        users.forEach(user -> {
            String code = normalizeDepartmentCode(user.getDepartmentCode());
            definitions.putIfAbsent(code, new DepartmentDefinition(
                    code,
                    normalizeDepartmentName(user.getDepartmentName()),
                    null,
                    definitions.size()
            ));
        });

        if (definitions.isEmpty()) {
            definitions.put(UNASSIGNED_DEPT_CODE, new DepartmentDefinition(
                    UNASSIGNED_DEPT_CODE,
                    UNASSIGNED_DEPT_NAME,
                    null,
                    0
            ));
        }

        List<DataScopeConfigResponse.DepartmentOptionItem> options = definitions.values().stream()
                .sorted(Comparator.comparingInt(DepartmentDefinition::sortOrder).thenComparing(DepartmentDefinition::name, String.CASE_INSENSITIVE_ORDER))
                .map(definition -> DataScopeConfigResponse.DepartmentOptionItem.builder()
                        .code(definition.code())
                        .name(definition.name())
                        .build())
                .toList();

        List<DataScopeConfigResponse.DepartmentTreeItem> tree = definitions.values().stream()
                .sorted(Comparator.comparingInt(DepartmentDefinition::sortOrder).thenComparing(DepartmentDefinition::name, String.CASE_INSENSITIVE_ORDER))
                .map(definition -> DataScopeConfigResponse.DepartmentTreeItem.builder()
                        .deptCode(definition.code())
                        .deptName(definition.name())
                        .parentDeptCode(definition.parentCode())
                        .sortOrder(definition.sortOrder())
                        .build())
                .toList();

        return new DepartmentGraph(definitions, options, tree);
    }

    private List<DataScopeConfigResponse.RolePermissionItem> buildRoleItems(List<User> users) {
        roleProfileBootstrap.ensureSystemRoles();
        Map<String, Integer> userCountByRole = users.stream()
                .collect(Collectors.groupingBy(
                        User::getRoleCode,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        return roleProfileRepository.findAll().stream()
                .sorted(Comparator.comparing(RoleProfile::getIsSystem).reversed()
                        .thenComparing(RoleProfile::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(role -> DataScopeConfigResponse.RolePermissionItem.builder()
                        .id(role.getId())
                        .code(normalizeRoleCode(role.getCode()))
                        .name(role.getName())
                        .description(role.getDescription())
                        .system(Boolean.TRUE.equals(role.getIsSystem()))
                        .enabled(Boolean.TRUE.equals(role.getEnabled()))
                        .userCount(userCountByRole.getOrDefault(normalizeRoleCode(role.getCode()), 0))
                        .dataScope(normalizeScope(role.getDataScope()))
                        .menuPermissions(normalizeMenuPermissions(role.getMenuPermissions()))
                        .allowedProjects(normalizeProjectIds(role.getAllowedProjects()))
                        .allowedDepts(normalizeDepartmentCodes(role.getAllowedDepts()))
                        .build())
                .toList();
    }

    private DataScopeConfigPayload loadPayload() {
        return systemSettingRepository.findByConfigKey(DATA_SCOPE_CONFIG_KEY)
                .map(SystemSetting::getPayloadJson)
                .filter(this::hasText)
                .map(this::deserializePayload)
                .orElseGet(() -> DataScopeConfigPayload.builder().build());
    }

    private DataScopeConfigPayload deserializePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, DataScopeConfigPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("数据权限配置读取失败", ex);
        }
    }

    private String serializePayload(DataScopeConfigPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize data scope payload", ex);
            throw new IllegalStateException("数据权限配置保存失败", ex);
        }
    }

    private List<DataScopeConfigPayload.DepartmentNode> normalizeDepartmentTree(List<DataScopeConfigResponse.DepartmentTreeItem> deptTree) {
        if (deptTree == null) {
            return List.of();
        }
        return deptTree.stream()
                .filter(item -> hasText(item.getDeptCode()))
                .map(item -> DataScopeConfigPayload.DepartmentNode.builder()
                        .departmentCode(normalizeDepartmentCode(item.getDeptCode()))
                        .departmentName(normalizeDepartmentName(item.getDeptName()))
                        .parentDepartmentCode(normalizeParentDepartmentCode(item.getParentDeptCode(), item.getDeptCode()))
                        .sortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .build())
                .toList();
    }

    private List<DataScopeConfigPayload.DepartmentNode> normalizeStoredDepartmentTree(List<DataScopeConfigPayload.DepartmentNode> deptTree) {
        if (deptTree == null) {
            return List.of();
        }
        return deptTree.stream()
                .filter(item -> hasText(item.getDepartmentCode()))
                .map(item -> DataScopeConfigPayload.DepartmentNode.builder()
                        .departmentCode(normalizeDepartmentCode(item.getDepartmentCode()))
                        .departmentName(normalizeDepartmentName(item.getDepartmentName()))
                        .parentDepartmentCode(normalizeParentDepartmentCode(item.getParentDepartmentCode(), item.getDepartmentCode()))
                        .sortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .build())
                .toList();
    }

    private List<DataScopeConfigPayload.UserScopeRule> normalizeUserRules(List<DataScopeConfigResponse.UserDataScopeItem> userDataScope) {
        if (userDataScope == null) {
            return List.of();
        }
        return userDataScope.stream()
                .filter(item -> item.getUserId() != null)
                .map(item -> DataScopeConfigPayload.UserScopeRule.builder()
                        .userId(item.getUserId())
                        .dataScope(normalizeScope(item.getDataScope()))
                        .allowedProjectIds(normalizeProjectIds(item.getAllowedProjects()))
                        .allowedDeptCodes(normalizeDepartmentCodes(item.getAllowedDepts()))
                        .build())
                .toList();
    }

    private List<DataScopeConfigPayload.DepartmentScopeRule> normalizeDepartmentRules(List<DataScopeConfigResponse.DepartmentDataScopeItem> deptDataScope) {
        if (deptDataScope == null) {
            return List.of();
        }
        return deptDataScope.stream()
                .filter(item -> hasText(item.getDeptCode()))
                .map(item -> DataScopeConfigPayload.DepartmentScopeRule.builder()
                        .departmentCode(normalizeDepartmentCode(item.getDeptCode()))
                        .dataScope(normalizeScope(item.getDataScope()))
                        .canViewOtherDepts(item.isCanViewOtherDepts())
                        .allowedDeptCodes(normalizeDepartmentCodes(item.getAllowedDepts()))
                        .build())
                .toList();
    }

    private List<Long> normalizeProjectIds(List<Long> projectIds) {
        if (projectIds == null) {
            return List.of();
        }
        return projectIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> normalizeDepartmentCodes(List<String> departmentCodes) {
        if (departmentCodes == null) {
            return List.of();
        }
        return departmentCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(this::hasText)
                .map(this::normalizeDepartmentCode)
                .distinct()
                .toList();
    }

    private List<String> normalizeMenuPermissions(List<String> menuPermissions) {
        if (menuPermissions == null) {
            return List.of();
        }
        return menuPermissions.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String normalizeScope(String dataScope) {
        String candidate = dataScope == null ? DEFAULT_SCOPE : dataScope.trim();
        return ALLOWED_SCOPES.contains(candidate) ? candidate : DEFAULT_SCOPE;
    }

    private String normalizeDepartmentCode(String departmentCode) {
        if (!hasText(departmentCode)) {
            return UNASSIGNED_DEPT_CODE;
        }
        return departmentCode.trim();
    }

    private String normalizeDepartmentName(String departmentName) {
        if (!hasText(departmentName)) {
            return UNASSIGNED_DEPT_NAME;
        }
        return departmentName.trim();
    }

    private String normalizeParentDepartmentCode(String parentCode, String currentCode) {
        if (!hasText(parentCode)) {
            return null;
        }
        String normalizedParent = normalizeDepartmentCode(parentCode);
        return normalizedParent.equals(normalizeDepartmentCode(currentCode)) ? null : normalizedParent;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Builder
    public static class AccessProfile {
        @Builder.Default
        private String dataScope = DEFAULT_SCOPE;
        @Builder.Default
        private List<Long> explicitProjectIds = List.of();
        @Builder.Default
        private List<String> allowedDepartmentCodes = List.of();

        public static AccessProfile empty() {
            return AccessProfile.builder().build();
        }

        public String getDataScope() {
            return dataScope;
        }

        public List<Long> getExplicitProjectIds() {
            return explicitProjectIds == null ? List.of() : explicitProjectIds;
        }

        public List<String> getAllowedDepartmentCodes() {
            return allowedDepartmentCodes == null ? List.of() : allowedDepartmentCodes;
        }
    }

    private record DepartmentDefinition(String code, String name, String parentCode, int sortOrder) {
    }

    private record DepartmentGraph(
            Map<String, DepartmentDefinition> definitions,
            List<DataScopeConfigResponse.DepartmentOptionItem> options,
            List<DataScopeConfigResponse.DepartmentTreeItem> tree
    ) {
        List<String> descendantsOf(String rootCode) {
            if (rootCode == null || !definitions.containsKey(rootCode)) {
                return List.of();
            }
            LinkedHashSet<String> visited = new LinkedHashSet<>();
            Deque<String> queue = new ArrayDeque<>();
            queue.add(rootCode);
            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                if (!visited.add(current)) {
                    continue;
                }
                definitions.values().stream()
                        .filter(definition -> Objects.equals(definition.parentCode(), current))
                        .map(DepartmentDefinition::code)
                        .forEach(queue::addLast);
            }
            return List.copyOf(visited);
        }
    }

    private RoleProfile resolveRoleProfile(User user) {
        String roleCode = user == null ? null : user.getRoleCode();
        Optional<RoleProfile> roleProfile = roleProfileRepository.findByCodeIgnoreCase(roleCode);
        if (roleProfile.isPresent()) {
            return roleProfile.get();
        }
        RoleProfileCatalog.SeedDefinition definition = RoleProfileCatalog.definitionForCode(roleCode);
        RoleProfile fallbackRole = RoleProfile.builder()
                .code(definition.code())
                .name(definition.name())
                .description(definition.description())
                .isSystem(definition.system())
                .enabled(true)
                .dataScope(definition.dataScope())
                .build();
        fallbackRole.setMenuPermissions(definition.menuPermissions());
        return fallbackRole;
    }
}
