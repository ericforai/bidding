package com.xiyu.bid.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.settings.dto.SettingsUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getSettings_AsAdmin_ShouldReturnCurrentSettings() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemConfig.sysName").exists())
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.deptDataScope").isArray())
                .andExpect(jsonPath("$.data.projectGroupScope").isArray());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateSettings_AsAdmin_ShouldPersistCriticalGovernanceSections() throws Exception {
        SettingsUpdateRequest request = SettingsUpdateRequest.builder()
                .systemConfig(SettingsUpdateRequest.SystemConfigUpdate.builder()
                        .sysName("西域数智化投标管理平台-整改版")
                        .depositWarnDays(10)
                        .qualWarnDays(45)
                        .enableAI(false)
                        .build())
                .roles(List.of(
                        SettingsUpdateRequest.RoleSettingUpdate.builder()
                                .code("admin")
                                .name("管理员")
                                .description("系统管理员")
                                .userCount(1)
                                .dataScope("all")
                                .menuPermissions(List.of("dashboard", "settings"))
                                .allowedProjects(List.of("pg1", "pg2"))
                                .allowedDepts(List.of("dept1"))
                                .build()
                ))
                .deptDataScope(List.of(
                        SettingsUpdateRequest.DeptDataScopeUpdate.builder()
                                .deptName("投标管理部")
                                .dataScope("all")
                                .canViewOtherDepts(true)
                                .allowedDepts(List.of("dept1", "dept2"))
                                .build()
                ))
                .projectGroupScope(List.of(
                        SettingsUpdateRequest.ProjectGroupScopeUpdate.builder()
                                .groupName("央企项目组")
                                .manager("张经理")
                                .memberCount(5)
                                .visibility("custom")
                                .allowedRoles(List.of("admin", "manager"))
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/settings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemConfig.sysName").value("西域数智化投标管理平台-整改版"))
                .andExpect(jsonPath("$.data.systemConfig.depositWarnDays").value(10))
                .andExpect(jsonPath("$.data.roles[0].menuPermissions[1]").value("settings"))
                .andExpect(jsonPath("$.data.deptDataScope[0].canViewOtherDepts").value(true))
                .andExpect(jsonPath("$.data.projectGroupScope[0].visibility").value("custom"));

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemConfig.sysName").value("西域数智化投标管理平台-整改版"))
                .andExpect(jsonPath("$.data.roles[0].menuPermissions[1]").value("settings"))
                .andExpect(jsonPath("$.data.deptDataScope[0].allowedDepts[1]").value("dept2"))
                .andExpect(jsonPath("$.data.projectGroupScope[0].visibility").value("custom"));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void updateSettings_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        SettingsUpdateRequest request = SettingsUpdateRequest.builder()
                .systemConfig(SettingsUpdateRequest.SystemConfigUpdate.builder()
                        .sysName("无权修改")
                        .depositWarnDays(9)
                        .qualWarnDays(30)
                        .enableAI(true)
                        .build())
                .build();

        mockMvc.perform(put("/api/settings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
