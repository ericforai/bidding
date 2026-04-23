package com.xiyu.bid.settings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.settings.dto.SettingsResponse;
import com.xiyu.bid.settings.dto.SettingsUpdateRequest;
import com.xiyu.bid.settings.entity.SystemSetting;
import com.xiyu.bid.settings.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String DEFAULT_CONFIG_KEY = "default";

    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncryptionUtil passwordEncryptionUtil;
    private final AiProviderCatalog aiProviderCatalog;

    @Transactional
    public SettingsResponse getSettings() {
        return copy(readSettings());
    }

    @Transactional
    public SettingsResponse updateSettings(SettingsUpdateRequest request) {
        SettingsResponse current = readSettings();

        if (request.getSystemConfig() != null) {
            current.setSystemConfig(SettingsResponse.SystemConfig.builder()
                    .sysName(request.getSystemConfig().getSysName())
                    .depositWarnDays(request.getSystemConfig().getDepositWarnDays())
                    .qualWarnDays(request.getSystemConfig().getQualWarnDays())
                    .enableAI(request.getSystemConfig().getEnableAI())
                    .build());
        }
        if (request.getRoles() != null) {
            current.setRoles(copyRolesFromUpdate(request.getRoles()));
        }
        if (request.getDeptDataScope() != null) {
            current.setDeptDataScope(copyDeptScopesFromUpdate(request.getDeptDataScope()));
        }
        if (request.getProjectGroupScope() != null) {
            current.setProjectGroupScope(copyProjectScopesFromUpdate(request.getProjectGroupScope()));
        }
        if (request.getIntegrationConfig() != null) {
            current.setIntegrationConfig(SettingsResponse.IntegrationConfig.builder()
                    .orgEnabled(request.getIntegrationConfig().getOrgEnabled())
                    .orgSystem(request.getIntegrationConfig().getOrgSystem())
                    .orgAppKey(request.getIntegrationConfig().getOrgAppKey())
                    .orgAppSecret(request.getIntegrationConfig().getOrgAppSecret())
                    .oaEnabled(request.getIntegrationConfig().getOaEnabled())
                    .oaUrl(request.getIntegrationConfig().getOaUrl())
                    .ssoEnabled(request.getIntegrationConfig().getSsoEnabled())
                    .callbackUrl(request.getIntegrationConfig().getCallbackUrl())
                    .apiKey(request.getIntegrationConfig().getApiKey())
                    .ipWhitelist(request.getIntegrationConfig().getIpWhitelist())
                    .build());
        }
        if (request.getAiModelConfig() != null) {
            current.setAiModelConfig(mergeAiModelConfig(current.getAiModelConfig(), request.getAiModelConfig()));
        }
        if (request.getFlowMappings() != null) {
            current.setFlowMappings(copyFlowMappingsFromUpdate(request.getFlowMappings()));
        }
        if (request.getApiList() != null) {
            current.setApiList(copyApiSettingsFromUpdate(request.getApiList()));
        }

        saveSettings(current);
        return copy(current);
    }

    @Transactional(readOnly = true)
    public SettingsResponse.RuntimePermissionProfile getRuntimePermissionProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String roleCode = user.getRoleCode();
        SettingsResponse.RoleSetting roleSetting = readSettings().getRoles().stream()
                .filter(role -> roleCode.equalsIgnoreCase(role.getCode()))
                .findFirst()
                .orElse(SettingsResponse.RoleSetting.builder()
                        .code(roleCode)
                        .menuPermissions(List.of())
                        .dataScope("self")
                        .allowedProjects(List.of())
                        .allowedDepts(List.of())
                        .build());

        return SettingsResponse.RuntimePermissionProfile.builder()
                .code(roleSetting.getCode())
                .menuPermissions(copyStringList(roleSetting.getMenuPermissions()))
                .dataScope(roleSetting.getDataScope())
                .allowedProjects(copyStringList(roleSetting.getAllowedProjects()))
                .allowedDepts(copyStringList(roleSetting.getAllowedDepts()))
                .build();
    }

    private SettingsResponse readSettings() {
        return systemSettingRepository.findByConfigKey(DEFAULT_CONFIG_KEY)
                .map(SystemSetting::getPayloadJson)
                .map(this::deserialize)
                .orElseGet(this::createAndPersistDefaultSettings);
    }

    private SettingsResponse createAndPersistDefaultSettings() {
        SettingsResponse defaults = createDefaultSettings();
        saveSettings(defaults);
        return defaults;
    }

    private void saveSettings(SettingsResponse settings) {
        SystemSetting record = systemSettingRepository.findByConfigKey(DEFAULT_CONFIG_KEY)
                .orElseGet(() -> SystemSetting.builder().configKey(DEFAULT_CONFIG_KEY).build());
        record.setPayloadJson(serialize(settings));
        systemSettingRepository.save(record);
    }

    private SettingsResponse deserialize(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, SettingsResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize persisted settings payload", exception);
        }
    }

    private String serialize(SettingsResponse settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize settings payload", exception);
        }
    }

    private SettingsResponse createDefaultSettings() {
        return SettingsResponse.builder()
                .systemConfig(SettingsResponse.SystemConfig.builder()
                        .sysName("西域数智化投标管理平台")
                        .depositWarnDays(7)
                        .qualWarnDays(30)
                        .enableAI(true)
                        .build())
                .roles(List.of(
                        SettingsResponse.RoleSetting.builder().code("admin").name("管理员").description("系统管理员，拥有所有权限").userCount(1).dataScope("all").menuPermissions(List.of("all")).allowedProjects(List.of("pg1", "pg2", "pg3")).allowedDepts(List.of("dept1", "dept2", "dept3", "dept4", "dept5")).build(),
                        SettingsResponse.RoleSetting.builder().code("manager").name("经理").description("部门经理，可查看报表和审批").userCount(1).dataScope("dept").menuPermissions(List.of("dashboard", "project", "analytics")).allowedProjects(List.of("pg1", "pg2", "pg3")).allowedDepts(List.of("dept1", "dept5")).build(),
                        SettingsResponse.RoleSetting.builder().code("sales").name("销售").description("销售人员，可创建项目和查看数据").userCount(5).dataScope("self").menuPermissions(List.of("dashboard", "project", "bidding")).allowedProjects(List.of("pg1")).allowedDepts(List.of("dept1", "dept2")).build(),
                        SettingsResponse.RoleSetting.builder().code("tech").name("技术人员").description("技术人员，可参与项目任务").userCount(10).dataScope("self").menuPermissions(List.of("dashboard", "project")).allowedProjects(List.of("pg2")).allowedDepts(List.of("dept3")).build()
                ))
                .deptDataScope(List.of(
                        SettingsResponse.DeptDataScopeSetting.builder().deptName("华南销售部").dataScope("dept").canViewOtherDepts(false).allowedDepts(List.of("dept1")).build(),
                        SettingsResponse.DeptDataScopeSetting.builder().deptName("华东销售部").dataScope("dept").canViewOtherDepts(false).allowedDepts(List.of("dept2")).build(),
                        SettingsResponse.DeptDataScopeSetting.builder().deptName("技术部").dataScope("dept").canViewOtherDepts(false).allowedDepts(List.of("dept3")).build(),
                        SettingsResponse.DeptDataScopeSetting.builder().deptName("投标管理部").dataScope("all").canViewOtherDepts(true).allowedDepts(List.of("dept1", "dept2", "dept3")).build()
                ))
                .projectGroupScope(List.of(
                        SettingsResponse.ProjectGroupScopeSetting.builder().groupName("央企项目组").manager("张经理").memberCount(5).visibility("members").allowedRoles(List.of("admin", "manager", "sales")).build(),
                        SettingsResponse.ProjectGroupScopeSetting.builder().groupName("政府项目组").manager("李经理").memberCount(3).visibility("members").allowedRoles(List.of("admin", "manager", "sales")).build(),
                        SettingsResponse.ProjectGroupScopeSetting.builder().groupName("军队项目组").manager("王经理").memberCount(2).visibility("manager").allowedRoles(List.of("admin", "manager")).build()
                ))
                .integrationConfig(SettingsResponse.IntegrationConfig.builder()
                        .orgEnabled(false)
                        .orgSystem("dingtalk")
                        .orgAppKey("")
                        .orgAppSecret("")
                        .oaEnabled(false)
                        .oaUrl("")
                        .ssoEnabled(false)
                        .callbackUrl("")
                        .apiKey("sk_xiyu_bid_server_default")
                        .ipWhitelist("")
                        .build())
                .aiModelConfig(defaultAiModelConfig())
                .flowMappings(List.of(
                        SettingsResponse.FlowMappingSetting.builder().systemFlow("项目立项审批").oaFlow("project_start").oaFlowCode("FLOW_001").oaFlowName("项目立项流程").description("新建项目时的审批流程").build(),
                        SettingsResponse.FlowMappingSetting.builder().systemFlow("投标审批").oaFlow("bidding_approval").oaFlowCode("FLOW_002").oaFlowName("投标审批流程").description("投标前的审批流程").build(),
                        SettingsResponse.FlowMappingSetting.builder().systemFlow("合同审批").oaFlow("contract_approval").oaFlowCode("FLOW_003").oaFlowName("合同审批流程").description("合同签署的审批流程").build(),
                        SettingsResponse.FlowMappingSetting.builder().systemFlow("用印申请").oaFlow("seal_application").oaFlowCode("FLOW_004").oaFlowName("用印申请流程").description("公章使用申请流程").build()
                ))
                .apiList(List.of(
                        SettingsResponse.ApiSetting.builder().name("获取项目列表").path("/api/projects").method("GET").description("查询投标项目列表").status("enabled").enabled(true).build(),
                        SettingsResponse.ApiSetting.builder().name("创建项目").path("/api/projects").method("POST").description("创建新的投标项目").status("enabled").enabled(true).build(),
                        SettingsResponse.ApiSetting.builder().name("获取项目详情").path("/api/projects/{id}").method("GET").description("获取指定项目详情").status("enabled").enabled(true).build(),
                        SettingsResponse.ApiSetting.builder().name("更新项目").path("/api/projects/{id}").method("PUT").description("更新项目信息").status("enabled").enabled(true).build(),
                        SettingsResponse.ApiSetting.builder().name("数据看板总览").path("/api/analytics/overview").method("GET").description("获取核心看板指标").status("enabled").enabled(true).build()
                ))
                .build();
    }

    private SettingsResponse copy(SettingsResponse source) {
        return SettingsResponse.builder()
                .systemConfig(SettingsResponse.SystemConfig.builder()
                        .sysName(source.getSystemConfig().getSysName())
                        .depositWarnDays(source.getSystemConfig().getDepositWarnDays())
                        .qualWarnDays(source.getSystemConfig().getQualWarnDays())
                        .enableAI(source.getSystemConfig().getEnableAI())
                        .build())
                .roles(copyRoles(source.getRoles()))
                .deptDataScope(copyDeptScopes(source.getDeptDataScope()))
                .projectGroupScope(copyProjectScopes(source.getProjectGroupScope()))
                .integrationConfig(SettingsResponse.IntegrationConfig.builder()
                        .orgEnabled(source.getIntegrationConfig().getOrgEnabled())
                        .orgSystem(source.getIntegrationConfig().getOrgSystem())
                        .orgAppKey(source.getIntegrationConfig().getOrgAppKey())
                        .orgAppSecret(source.getIntegrationConfig().getOrgAppSecret())
                        .oaEnabled(source.getIntegrationConfig().getOaEnabled())
                        .oaUrl(source.getIntegrationConfig().getOaUrl())
                        .ssoEnabled(source.getIntegrationConfig().getSsoEnabled())
                        .callbackUrl(source.getIntegrationConfig().getCallbackUrl())
                        .apiKey(source.getIntegrationConfig().getApiKey())
                        .ipWhitelist(source.getIntegrationConfig().getIpWhitelist())
                        .build())
                .aiModelConfig(copyAiModelConfigForResponse(normalizeAiModelConfig(source.getAiModelConfig())))
                .flowMappings(copyFlowMappings(source.getFlowMappings()))
                .apiList(copyApiSettings(source.getApiList()))
                .build();
    }

    private List<SettingsResponse.RoleSetting> copyRoles(List<SettingsResponse.RoleSetting> source) {
        List<SettingsResponse.RoleSetting> target = new ArrayList<>();
        for (SettingsResponse.RoleSetting item : source) {
            target.add(SettingsResponse.RoleSetting.builder()
                    .code(item.getCode())
                    .name(item.getName())
                    .description(item.getDescription())
                    .userCount(item.getUserCount())
                    .dataScope(item.getDataScope())
                    .menuPermissions(copyStringList(item.getMenuPermissions()))
                    .allowedProjects(copyStringList(item.getAllowedProjects()))
                    .allowedDepts(copyStringList(item.getAllowedDepts()))
                    .build());
        }
        return target;
    }

    private List<SettingsResponse.DeptDataScopeSetting> copyDeptScopes(List<SettingsResponse.DeptDataScopeSetting> source) {
        List<SettingsResponse.DeptDataScopeSetting> target = new ArrayList<>();
        for (SettingsResponse.DeptDataScopeSetting item : source) {
            target.add(SettingsResponse.DeptDataScopeSetting.builder()
                    .deptName(item.getDeptName())
                    .dataScope(item.getDataScope())
                    .canViewOtherDepts(item.getCanViewOtherDepts())
                    .allowedDepts(copyStringList(item.getAllowedDepts()))
                    .build());
        }
        return target;
    }

    private List<SettingsResponse.ProjectGroupScopeSetting> copyProjectScopes(List<SettingsResponse.ProjectGroupScopeSetting> source) {
        List<SettingsResponse.ProjectGroupScopeSetting> target = new ArrayList<>();
        for (SettingsResponse.ProjectGroupScopeSetting item : source) {
            target.add(SettingsResponse.ProjectGroupScopeSetting.builder()
                    .groupName(item.getGroupName())
                    .manager(item.getManager())
                    .memberCount(item.getMemberCount())
                    .visibility(item.getVisibility())
                    .allowedRoles(copyStringList(item.getAllowedRoles()))
                    .build());
        }
        return target;
    }

    private List<SettingsResponse.FlowMappingSetting> copyFlowMappings(List<SettingsResponse.FlowMappingSetting> source) {
        List<SettingsResponse.FlowMappingSetting> target = new ArrayList<>();
        for (SettingsResponse.FlowMappingSetting item : source) {
            target.add(SettingsResponse.FlowMappingSetting.builder()
                    .systemFlow(item.getSystemFlow())
                    .oaFlow(item.getOaFlow())
                    .oaFlowCode(item.getOaFlowCode())
                    .oaFlowName(item.getOaFlowName())
                    .description(item.getDescription())
                    .build());
        }
        return target;
    }

    private List<SettingsResponse.ApiSetting> copyApiSettings(List<SettingsResponse.ApiSetting> source) {
        List<SettingsResponse.ApiSetting> target = new ArrayList<>();
        for (SettingsResponse.ApiSetting item : source) {
            target.add(SettingsResponse.ApiSetting.builder()
                    .name(item.getName())
                    .path(item.getPath())
                    .method(item.getMethod())
                    .description(item.getDescription())
                    .status(item.getStatus())
                    .enabled(item.getEnabled())
                    .build());
        }
        return target;
    }

    public SettingsResponse.AiModelConfig getInternalAiModelConfig() {
        return normalizeAiModelConfig(readSettings().getAiModelConfig());
    }

    public boolean isAiEnabled() {
        SettingsResponse.SystemConfig systemConfig = readSettings().getSystemConfig();
        return systemConfig == null || systemConfig.getEnableAI() == null || systemConfig.getEnableAI();
    }

    public String resolveAiApiKey(String providerCode) {
        SettingsResponse.AiProviderSetting provider = findAiProvider(getInternalAiModelConfig(), providerCode);
        if (provider == null || provider.getEncryptedApiKey() == null || provider.getEncryptedApiKey().isBlank()) {
            return null;
        }
        return passwordEncryptionUtil.decrypt(provider.getEncryptedApiKey());
    }

    @Transactional
    public SettingsResponse.AiModelConfig updateAiProviderTestResult(
            String providerCode,
            String status,
            String message
    ) {
        SettingsResponse settings = readSettings();
        SettingsResponse.AiModelConfig config = normalizeAiModelConfig(settings.getAiModelConfig());
        SettingsResponse.AiProviderSetting provider = findAiProvider(config, providerCode);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported AI provider: " + providerCode);
        }
        provider.setLastTestStatus(status);
        provider.setLastTestMessage(message);
        provider.setLastTestAt(java.time.Instant.now());
        settings.setAiModelConfig(config);
        saveSettings(settings);
        return copyAiModelConfigForResponse(config);
    }

    @Transactional
    public SettingsResponse.AiModelConfig saveSuccessfulAiProviderTestConfig(
            String providerCode,
            String baseUrl,
            String model,
            String apiKeyPlaintext,
            String message
    ) {
        SettingsResponse settings = readSettings();
        SettingsResponse.AiModelConfig config = normalizeAiModelConfig(settings.getAiModelConfig());
        String normalizedProviderCode = normalizeProviderCode(providerCode);
        SettingsResponse.AiProviderSetting provider = findAiProvider(config, normalizedProviderCode);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported AI provider: " + providerCode);
        }

        if (baseUrl != null && !baseUrl.isBlank()) {
            aiProviderCatalog.validateBaseUrl(normalizedProviderCode, baseUrl);
            provider.setBaseUrl(baseUrl.trim());
        }
        if (model != null && !model.isBlank()) {
            provider.setModel(model.trim());
        }
        if (apiKeyPlaintext != null && !apiKeyPlaintext.isBlank()) {
            provider.setEncryptedApiKey(passwordEncryptionUtil.encrypt(apiKeyPlaintext.trim()));
        }
        provider.setLastTestStatus("success");
        provider.setLastTestMessage(message);
        provider.setLastTestAt(java.time.Instant.now());
        settings.setAiModelConfig(config);
        saveSettings(settings);
        return copyAiModelConfigForResponse(config);
    }

    private SettingsResponse.AiModelConfig mergeAiModelConfig(
            SettingsResponse.AiModelConfig current,
            SettingsUpdateRequest.AiModelConfigUpdate update
    ) {
        SettingsResponse.AiModelConfig normalizedCurrent = normalizeAiModelConfig(current);
        if (update.getActiveProvider() != null && !update.getActiveProvider().isBlank()) {
            normalizedCurrent.setActiveProvider(normalizeProviderCode(update.getActiveProvider()));
        }

        Map<String, SettingsResponse.AiProviderSetting> providerMap = new HashMap<>();
        for (SettingsResponse.AiProviderSetting provider : normalizedCurrent.getProviders()) {
            providerMap.put(provider.getProviderCode(), provider);
        }

        if (update.getProviders() != null) {
            for (SettingsUpdateRequest.AiProviderSettingUpdate providerUpdate : update.getProviders()) {
                String providerCode = normalizeProviderCode(providerUpdate.getProviderCode());
                if (!aiProviderCatalog.isSupported(providerCode)) {
                    continue;
                }
                SettingsResponse.AiProviderSetting target = providerMap.get(providerCode);
                if (target == null) {
                    target = defaultAiProviderSetting(providerCode);
                    providerMap.put(providerCode, target);
                }
                if (providerUpdate.getEnabled() != null) target.setEnabled(providerUpdate.getEnabled());
                if (providerUpdate.getBaseUrl() != null) {
                    aiProviderCatalog.validateBaseUrl(providerCode, providerUpdate.getBaseUrl());
                    target.setBaseUrl(providerUpdate.getBaseUrl().trim());
                }
                if (providerUpdate.getModel() != null) target.setModel(providerUpdate.getModel().trim());
                if (providerUpdate.getApiKeyPlaintext() != null && !providerUpdate.getApiKeyPlaintext().isBlank()) {
                    target.setEncryptedApiKey(passwordEncryptionUtil.encrypt(providerUpdate.getApiKeyPlaintext().trim()));
                }
                if (providerUpdate.getLastTestStatus() != null) target.setLastTestStatus(providerUpdate.getLastTestStatus());
                if (providerUpdate.getLastTestMessage() != null) target.setLastTestMessage(providerUpdate.getLastTestMessage());
                if (providerUpdate.getLastTestAt() != null) target.setLastTestAt(providerUpdate.getLastTestAt());
            }
        }

        normalizedCurrent.setProviders(aiProviderCatalog.supportedProviderCodes().stream()
                .map(code -> providerMap.getOrDefault(code, defaultAiProviderSetting(code)))
                .toList());
        if (!aiProviderCatalog.isSupported(normalizedCurrent.getActiveProvider())) {
            normalizedCurrent.setActiveProvider(aiProviderCatalog.defaultActiveProvider());
        }
        return normalizedCurrent;
    }

    private SettingsResponse.AiModelConfig normalizeAiModelConfig(SettingsResponse.AiModelConfig source) {
        SettingsResponse.AiModelConfig defaults = defaultAiModelConfig();
        if (source == null) {
            return defaults;
        }

        Map<String, SettingsResponse.AiProviderSetting> sourceProviders = new HashMap<>();
        if (source.getProviders() != null) {
            for (SettingsResponse.AiProviderSetting provider : source.getProviders()) {
                String providerCode = normalizeProviderCode(provider.getProviderCode());
                if (!aiProviderCatalog.isSupported(providerCode)) {
                    continue;
                }
                SettingsResponse.AiProviderSetting merged = defaultAiProviderSetting(providerCode);
                merged.setEnabled(provider.getEnabled() != null ? provider.getEnabled() : merged.getEnabled());
                merged.setBaseUrl(nonBlankOrDefault(provider.getBaseUrl(), merged.getBaseUrl()));
                merged.setModel(nonBlankOrDefault(provider.getModel(), merged.getModel()));
                merged.setEncryptedApiKey(provider.getEncryptedApiKey());
                merged.setLastTestStatus(provider.getLastTestStatus());
                merged.setLastTestMessage(provider.getLastTestMessage());
                merged.setLastTestAt(provider.getLastTestAt());
                sourceProviders.put(providerCode, merged);
            }
        }

        defaults.setActiveProvider(aiProviderCatalog.isSupported(normalizeProviderCode(source.getActiveProvider()))
                ? normalizeProviderCode(source.getActiveProvider())
                : aiProviderCatalog.defaultActiveProvider());
        defaults.setProviders(aiProviderCatalog.supportedProviderCodes().stream()
                .map(code -> sourceProviders.getOrDefault(code, defaultAiProviderSetting(code)))
                .toList());
        return defaults;
    }

    private SettingsResponse.AiModelConfig defaultAiModelConfig() {
        return SettingsResponse.AiModelConfig.builder()
                .activeProvider(aiProviderCatalog.defaultActiveProvider())
                .providers(aiProviderCatalog.supportedProviderCodes().stream().map(this::defaultAiProviderSetting).toList())
                .build();
    }

    private SettingsResponse.AiProviderSetting defaultAiProviderSetting(String providerCode) {
        return aiProviderCatalog.defaultProviderSetting(providerCode);
    }

    private SettingsResponse.AiModelConfig copyAiModelConfigForResponse(SettingsResponse.AiModelConfig source) {
        SettingsResponse.AiModelConfig normalized = normalizeAiModelConfig(source);
        return SettingsResponse.AiModelConfig.builder()
                .activeProvider(normalized.getActiveProvider())
                .providers(normalized.getProviders().stream()
                        .map(this::copyAiProviderForResponse)
                        .toList())
                .build();
    }

    private SettingsResponse.AiProviderSetting copyAiProviderForResponse(SettingsResponse.AiProviderSetting source) {
        String encryptedApiKey = source.getEncryptedApiKey();
        String plaintext = null;
        if (encryptedApiKey != null && !encryptedApiKey.isBlank()) {
            try {
                plaintext = passwordEncryptionUtil.decrypt(encryptedApiKey);
            } catch (RuntimeException ignored) {
                plaintext = null;
            }
        }
        return SettingsResponse.AiProviderSetting.builder()
                .providerCode(source.getProviderCode())
                .providerName(source.getProviderName())
                .enabled(source.getEnabled())
                .baseUrl(source.getBaseUrl())
                .model(source.getModel())
                .apiKeyMasked(maskApiKey(plaintext))
                .apiKeyConfigured(plaintext != null && !plaintext.isBlank())
                .lastTestStatus(source.getLastTestStatus())
                .lastTestMessage(source.getLastTestMessage())
                .lastTestAt(source.getLastTestAt())
                .build();
    }

    private SettingsResponse.AiProviderSetting findAiProvider(SettingsResponse.AiModelConfig config, String providerCode) {
        String normalizedCode = normalizeProviderCode(providerCode);
        if (config == null || config.getProviders() == null) {
            return null;
        }
        return config.getProviders().stream()
                .filter(provider -> normalizedCode.equals(provider.getProviderCode()))
                .findFirst()
                .orElse(null);
    }

    private String normalizeProviderCode(String providerCode) {
        return aiProviderCatalog.normalize(providerCode);
    }

    private String nonBlankOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) {
            return "****" + trimmed.substring(Math.max(0, trimmed.length() - 2));
        }
        return trimmed.substring(0, Math.min(4, trimmed.length())) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private List<SettingsResponse.RoleSetting> copyRolesFromUpdate(List<SettingsUpdateRequest.RoleSettingUpdate> source) {
        List<SettingsResponse.RoleSetting> target = new ArrayList<>();
        for (SettingsUpdateRequest.RoleSettingUpdate item : source) {
            target.add(SettingsResponse.RoleSetting.builder()
                    .code(item.getCode())
                    .name(item.getName())
                    .description(item.getDescription())
                    .userCount(item.getUserCount())
                    .dataScope(item.getDataScope())
                    .menuPermissions(copyStringList(item.getMenuPermissions()))
                    .allowedProjects(copyStringList(item.getAllowedProjects()))
                    .allowedDepts(copyStringList(item.getAllowedDepts()))
                    .build());
        }
        return target;
    }

    private List<SettingsResponse.DeptDataScopeSetting> copyDeptScopesFromUpdate(List<SettingsUpdateRequest.DeptDataScopeUpdate> source) {
        List<SettingsResponse.DeptDataScopeSetting> target = new ArrayList<>();
        for (SettingsUpdateRequest.DeptDataScopeUpdate item : source) {
            target.add(SettingsResponse.DeptDataScopeSetting.builder()
                    .deptName(item.getDeptName())
                    .dataScope(item.getDataScope())
                    .canViewOtherDepts(item.getCanViewOtherDepts())
                    .allowedDepts(copyStringList(item.getAllowedDepts()))
                    .build());
        }
        return target;
    }

    private List<SettingsResponse.ProjectGroupScopeSetting> copyProjectScopesFromUpdate(List<SettingsUpdateRequest.ProjectGroupScopeUpdate> source) {
        List<SettingsResponse.ProjectGroupScopeSetting> target = new ArrayList<>();
        for (SettingsUpdateRequest.ProjectGroupScopeUpdate item : source) {
            target.add(SettingsResponse.ProjectGroupScopeSetting.builder()
                    .groupName(item.getGroupName())
                    .manager(item.getManager())
                    .memberCount(item.getMemberCount())
                    .visibility(item.getVisibility())
                    .allowedRoles(copyStringList(item.getAllowedRoles()))
                    .build());
        }
        return target;
    }

    private List<SettingsResponse.FlowMappingSetting> copyFlowMappingsFromUpdate(List<SettingsUpdateRequest.FlowMappingUpdate> source) {
        List<SettingsResponse.FlowMappingSetting> target = new ArrayList<>();
        for (SettingsUpdateRequest.FlowMappingUpdate item : source) {
            target.add(SettingsResponse.FlowMappingSetting.builder()
                    .systemFlow(item.getSystemFlow())
                    .oaFlow(item.getOaFlow())
                    .oaFlowCode(item.getOaFlowCode())
                    .oaFlowName(item.getOaFlowName())
                    .description(item.getDescription())
                    .build());
        }
        return target;
    }

    private List<SettingsResponse.ApiSetting> copyApiSettingsFromUpdate(List<SettingsUpdateRequest.ApiSettingUpdate> source) {
        List<SettingsResponse.ApiSetting> target = new ArrayList<>();
        for (SettingsUpdateRequest.ApiSettingUpdate item : source) {
            target.add(SettingsResponse.ApiSetting.builder()
                    .name(item.getName())
                    .path(item.getPath())
                    .method(item.getMethod())
                    .description(item.getDescription())
                    .status(item.getStatus())
                    .enabled(item.getEnabled())
                    .build());
        }
        return target;
    }

    private List<String> copyStringList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
