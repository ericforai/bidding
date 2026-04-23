package com.xiyu.bid.settings.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.settings.dto.SettingsResponse;
import com.xiyu.bid.settings.dto.SettingsUpdateRequest;
import com.xiyu.bid.settings.repository.SystemSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DataJpaTest
@ActiveProfiles("test")
class SettingsServiceTest {

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    private UserRepository userRepository;
    private ObjectMapper objectMapper;
    private SettingsPayloadMapper payloadMapper;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        payloadMapper = new SettingsPayloadMapper(new SettingsDefaultPayloadFactory());
        settingsService = new SettingsService(systemSettingRepository, userRepository, objectMapper, payloadMapper);
    }

    @Test
    void getSettings_ShouldSeedPersistentDefaultRecord() {
        assertEquals(0, systemSettingRepository.count());

        SettingsResponse settings = settingsService.getSettings();

        assertNotNull(settings);
        assertEquals("西域数智化投标管理平台", settings.getSystemConfig().getSysName());
        assertEquals(1, systemSettingRepository.count());
        assertTrue(systemSettingRepository.findByConfigKey("default").isPresent());
    }

    @Test
    void updateSettings_ShouldRemainVisibleAcrossNewServiceInstance() {
        settingsService.updateSettings(SettingsUpdateRequest.builder()
                .systemConfig(SettingsUpdateRequest.SystemConfigUpdate.builder()
                        .sysName("整改后平台")
                        .depositWarnDays(12)
                        .qualWarnDays(60)
                        .enableAI(false)
                        .build())
                .roles(List.of(
                        SettingsUpdateRequest.RoleSettingUpdate.builder()
                                .code("admin")
                                .name("管理员")
                                .description("持久化验证")
                                .userCount(2)
                                .dataScope("all")
                                .menuPermissions(List.of("dashboard", "settings"))
                                .allowedProjects(List.of("pg1"))
                                .allowedDepts(List.of("dept1"))
                                .build()
                ))
                .integrationConfig(SettingsUpdateRequest.IntegrationConfigUpdate.builder()
                        .apiKey("sk-system-test")
                        .aiBaseUrl("https://gateway.example.test/v1")
                        .aiModel("gpt-system")
                        .build())
                .build());

        SettingsService reloadedService = new SettingsService(systemSettingRepository, userRepository, objectMapper, payloadMapper);
        SettingsResponse reloaded = reloadedService.getSettings();

        assertEquals("整改后平台", reloaded.getSystemConfig().getSysName());
        assertEquals(12, reloaded.getSystemConfig().getDepositWarnDays());
        assertFalse(reloaded.getSystemConfig().getEnableAI());
        assertEquals("settings", reloaded.getRoles().get(0).getMenuPermissions().get(1));
        assertEquals("https://gateway.example.test/v1", reloaded.getIntegrationConfig().getAiBaseUrl());
        assertEquals("gpt-system", reloaded.getIntegrationConfig().getAiModel());
    }
}
