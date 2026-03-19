package com.xiyu.bid.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.dto.DataScopeConfigPayload;
import com.xiyu.bid.dto.DataScopeConfigResponse;
import com.xiyu.bid.entity.SystemSetting;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.SystemSettingRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataScopeConfigService {

    public static final String DATA_SCOPE_CONFIG_KEY = "data_scope_config";
    private static final String DEFAULT_SCOPE = "self";
    private static final String DEFAULT_GROUP_VISIBILITY = "members";
    private static final String UNASSIGNED_DEPT_CODE = "UNASSIGNED";
    private static final String UNASSIGNED_DEPT_NAME = "未分配";
    private static final Set<String> ALLOWED_SCOPES = Set.of("all", "dept", "deptAndSub", "self");
    private static final Set<String> ALLOWED_VISIBILITIES = Set.of("all", "members", "manager", "custom");

    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;
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
        Map<Long, User> usersById = users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return DataScopeConfigResponse.builder()
                .userDataScope(users.stream()
                        .map(user -> toUserItem(user, userRules.get(user.getId())))
                        .toList())
                .deptDataScope(departmentGraph.options().stream()
                        .map(option -> toDepartmentItem(option, departmentRules.get(option.getCode())))
                        .toList())
                .deptOptions(departmentGraph.options())
                .deptTree(departmentGraph.tree())
                .projectGroupScope(payload.getProjectGroupRules().stream()
                        .map(rule -> toProjectGroupItem(rule, usersById))
                        .toList())
                .userOptions(users.stream()
                        .map(this::toUserOptionItem)
                        .toList())
                .build();
    }

    @Transactional
    public DataScopeConfigResponse saveConfig(DataScopeConfigResponse request) {
        DataScopeConfigResponse safeRequest = request == null ? DataScopeConfigResponse.builder().build() : request;
        DataScopeConfigPayload payload = DataScopeConfigPayload.builder()
                .departmentTree(normalizeDepartmentTree(safeRequest.getDeptTree()))
                .userRules(normalizeUserRules(safeRequest.getUserDataScope()))
                .departmentRules(normalizeDepartmentRules(safeRequest.getDeptDataScope()))
                .projectGroupRules(normalizeProjectGroupRules(safeRequest.getProjectGroupScope()))
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

        String dataScope = normalizeScope(userRule != null ? userRule.getDataScope() : departmentRule != null ? departmentRule.getDataScope() : DEFAULT_SCOPE);
        LinkedHashSet<Long> explicitProjectIds = new LinkedHashSet<>(userRule == null ? List.of() : normalizeProjectIds(userRule.getAllowedProjectIds()));
        explicitProjectIds.addAll(getProjectIdsGrantedByGroups(payload.getProjectGroupRules(), user));

        List<String> explicitDeptCodes = normalizeDepartmentCodes(
                userRule != null ? userRule.getAllowedDeptCodes() : departmentRule != null ? departmentRule.getAllowedDeptCodes() : List.of()
        );

        if ("all".equals(dataScope)) {
            return AccessProfile.builder()
                    .dataScope(dataScope)
                    .allowedDepartmentCodes(departmentGraph.options().stream().map(DataScopeConfigResponse.DepartmentOptionItem::getCode).toList())
                    .explicitProjectIds(List.copyOf(explicitProjectIds))
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
                .explicitProjectIds(List.copyOf(explicitProjectIds))
                .build();
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
                .role(String.valueOf(user.getRole()).toLowerCase())
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

    private DataScopeConfigResponse.ProjectGroupScopeItem toProjectGroupItem(
            DataScopeConfigPayload.ProjectGroupRule rule,
            Map<Long, User> usersById
    ) {
        User manager = rule.getManagerUserId() == null ? null : usersById.get(rule.getManagerUserId());
        return DataScopeConfigResponse.ProjectGroupScopeItem.builder()
                .groupCode(normalizeGroupCode(rule.getGroupCode(), rule.getGroupName()))
                .groupName(normalizeGroupName(rule.getGroupName()))
                .managerUserId(rule.getManagerUserId())
                .manager(manager == null ? "" : manager.getFullName())
                .memberCount(normalizeLongIds(rule.getMemberUserIds()).size())
                .visibility(normalizeVisibility(rule.getVisibility()))
                .memberUserIds(normalizeLongIds(rule.getMemberUserIds()))
                .allowedRoles(normalizeRoles(rule.getAllowedRoles()))
                .projectIds(normalizeProjectIds(rule.getProjectIds()))
                .build();
    }

    private DataScopeConfigResponse.UserOptionItem toUserOptionItem(User user) {
        return DataScopeConfigResponse.UserOptionItem.builder()
                .id(user.getId())
                .name(user.getFullName())
                .role(String.valueOf(user.getRole()).toLowerCase())
                .deptCode(normalizeDepartmentCode(user.getDepartmentCode()))
                .dept(normalizeDepartmentName(user.getDepartmentName()))
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

    private List<DataScopeConfigPayload.ProjectGroupRule> normalizeProjectGroupRules(List<DataScopeConfigResponse.ProjectGroupScopeItem> projectGroupScope) {
        if (projectGroupScope == null) {
            return List.of();
        }
        return projectGroupScope.stream()
                .filter(item -> hasText(item.getGroupName()) || hasText(item.getGroupCode()))
                .map(item -> DataScopeConfigPayload.ProjectGroupRule.builder()
                        .groupCode(normalizeGroupCode(item.getGroupCode(), item.getGroupName()))
                        .groupName(normalizeGroupName(item.getGroupName()))
                        .managerUserId(item.getManagerUserId())
                        .visibility(normalizeVisibility(item.getVisibility()))
                        .memberUserIds(normalizeLongIds(item.getMemberUserIds()))
                        .allowedRoles(normalizeRoles(item.getAllowedRoles()))
                        .projectIds(normalizeProjectIds(item.getProjectIds()))
                        .build())
                .toList();
    }

    private List<Long> getProjectIdsGrantedByGroups(List<DataScopeConfigPayload.ProjectGroupRule> projectGroupRules, User user) {
        if (projectGroupRules == null || user == null) {
            return List.of();
        }
        String currentRole = String.valueOf(user.getRole()).toLowerCase();
        LinkedHashSet<Long> granted = new LinkedHashSet<>();

        for (DataScopeConfigPayload.ProjectGroupRule rule : projectGroupRules) {
            if (matchesGroupRule(rule, user, currentRole)) {
                granted.addAll(normalizeProjectIds(rule.getProjectIds()));
            }
        }
        return List.copyOf(granted);
    }

    private boolean matchesGroupRule(DataScopeConfigPayload.ProjectGroupRule rule, User user, String currentRole) {
        String visibility = normalizeVisibility(rule.getVisibility());
        List<Long> memberUserIds = normalizeLongIds(rule.getMemberUserIds());
        List<String> allowedRoles = normalizeRoles(rule.getAllowedRoles());

        return switch (visibility) {
            case "all" -> true;
            case "manager" -> Objects.equals(rule.getManagerUserId(), user.getId());
            case "custom" -> allowedRoles.contains(currentRole) || Objects.equals(rule.getManagerUserId(), user.getId());
            default -> Objects.equals(rule.getManagerUserId(), user.getId()) || memberUserIds.contains(user.getId());
        };
    }

    private List<Long> normalizeProjectIds(List<Long> projectIds) {
        return normalizeLongIds(projectIds);
    }

    private List<Long> normalizeLongIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
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

    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.trim().toLowerCase())
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String normalizeScope(String dataScope) {
        String candidate = dataScope == null ? DEFAULT_SCOPE : dataScope.trim();
        return ALLOWED_SCOPES.contains(candidate) ? candidate : DEFAULT_SCOPE;
    }

    private String normalizeVisibility(String visibility) {
        String candidate = visibility == null ? DEFAULT_GROUP_VISIBILITY : visibility.trim();
        return ALLOWED_VISIBILITIES.contains(candidate) ? candidate : DEFAULT_GROUP_VISIBILITY;
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

    private String normalizeGroupCode(String groupCode, String groupName) {
        if (hasText(groupCode)) {
            return groupCode.trim();
        }
        if (!hasText(groupName)) {
            return UUID.randomUUID().toString();
        }
        return groupName.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT);
    }

    private String normalizeGroupName(String groupName) {
        return hasText(groupName) ? groupName.trim() : "未命名项目组";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
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
}
