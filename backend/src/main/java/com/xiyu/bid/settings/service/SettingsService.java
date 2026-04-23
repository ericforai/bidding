package com.xiyu.bid.settings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.settings.dto.SettingsResponse;
import com.xiyu.bid.settings.dto.SettingsUpdateRequest;
import com.xiyu.bid.settings.entity.SystemSetting;
import com.xiyu.bid.settings.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SettingsService {

    private static final String DEFAULT_CONFIG_KEY = "default";

    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;
    private final SettingsPayloadMapper payloadMapper;
    private final ObjectReader settingsReader;
    private final ObjectWriter settingsWriter;

    public SettingsService(
            SystemSettingRepository systemSettingRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            SettingsPayloadMapper payloadMapper
    ) {
        this.systemSettingRepository = systemSettingRepository;
        this.userRepository = userRepository;
        this.payloadMapper = payloadMapper;
        this.settingsReader = objectMapper.readerFor(SettingsResponse.class);
        this.settingsWriter = objectMapper.writerFor(SettingsResponse.class);
    }

    @Transactional
    public SettingsResponse getSettings() {
        return payloadMapper.copy(readSettings());
    }

    @Transactional
    public SettingsResponse updateSettings(SettingsUpdateRequest request) {
        SettingsResponse current = readSettings();

        if (request.getSystemConfig() != null) {
            current.setSystemConfig(payloadMapper.copySystemConfigFromUpdate(request.getSystemConfig()));
        }
        if (request.getRoles() != null) {
            current.setRoles(payloadMapper.copyRolesFromUpdate(request.getRoles()));
        }
        if (request.getDeptDataScope() != null) {
            current.setDeptDataScope(payloadMapper.copyDeptScopesFromUpdate(request.getDeptDataScope()));
        }
        if (request.getProjectGroupScope() != null) {
            current.setProjectGroupScope(payloadMapper.copyProjectScopesFromUpdate(request.getProjectGroupScope()));
        }
        if (request.getIntegrationConfig() != null) {
            current.setIntegrationConfig(payloadMapper.copyIntegrationConfigFromUpdate(request.getIntegrationConfig()));
        }
        if (request.getFlowMappings() != null) {
            current.setFlowMappings(payloadMapper.copyFlowMappingsFromUpdate(request.getFlowMappings()));
        }
        if (request.getApiList() != null) {
            current.setApiList(payloadMapper.copyApiSettingsFromUpdate(request.getApiList()));
        }

        saveSettings(current);
        return payloadMapper.copy(current);
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
                .menuPermissions(payloadMapper.copyStringList(roleSetting.getMenuPermissions()))
                .dataScope(roleSetting.getDataScope())
                .allowedProjects(payloadMapper.copyStringList(roleSetting.getAllowedProjects()))
                .allowedDepts(payloadMapper.copyStringList(roleSetting.getAllowedDepts()))
                .build();
    }

    private SettingsResponse readSettings() {
        return systemSettingRepository.findByConfigKey(DEFAULT_CONFIG_KEY)
                .map(SystemSetting::getPayloadJson)
                .map(this::deserialize)
                .orElseGet(this::createAndPersistDefaultSettings);
    }

    private SettingsResponse createAndPersistDefaultSettings() {
        SettingsResponse defaults = payloadMapper.createDefaultSettings();
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
            return settingsReader.readValue(payloadJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize persisted settings payload", exception);
        }
    }

    private String serialize(SettingsResponse settings) {
        try {
            return settingsWriter.writeValueAsString(settings);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize settings payload", exception);
        }
    }
}
