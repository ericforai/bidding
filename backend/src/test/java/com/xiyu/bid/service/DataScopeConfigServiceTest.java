package com.xiyu.bid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.dto.DataScopeConfigResponse;
import com.xiyu.bid.entity.SystemSetting;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.SystemSettingRepository;
import com.xiyu.bid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataScopeConfigServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private UserRepository userRepository;

    private DataScopeConfigService dataScopeConfigService;

    @BeforeEach
    void setUp() {
        dataScopeConfigService = new DataScopeConfigService(systemSettingRepository, userRepository, new ObjectMapper());
    }

    @Test
    void getConfig_ShouldMergeStoredRulesWithCurrentUsers() {
        User salesUser = User.builder()
                .id(1L)
                .username("alice")
                .fullName("Alice")
                .role(User.Role.STAFF)
                .departmentCode("SALES")
                .departmentName("销售部")
                .enabled(true)
                .build();
        String payloadJson = """
                {
                  "departmentTree": [
                    {
                      "departmentCode": "SALES",
                      "departmentName": "销售部",
                      "parentDepartmentCode": null,
                      "sortOrder": 1
                    },
                    {
                      "departmentCode": "TECH",
                      "departmentName": "技术部",
                      "parentDepartmentCode": "SALES",
                      "sortOrder": 2
                    }
                  ],
                  "userRules": [
                    {
                      "userId": 1,
                      "dataScope": "dept",
                      "allowedProjectIds": [101, 102],
                      "allowedDeptCodes": ["TECH"]
                    }
                  ],
                  "departmentRules": [
                    {
                      "departmentCode": "SALES",
                      "dataScope": "dept",
                      "canViewOtherDepts": true,
                      "allowedDeptCodes": ["TECH"]
                    }
                  ]
                }
                """;

        when(userRepository.findAll()).thenReturn(List.of(salesUser));
        when(systemSettingRepository.findByConfigKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY))
                .thenReturn(Optional.of(SystemSetting.builder().configKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY).payloadJson(payloadJson).build()));

        DataScopeConfigResponse response = dataScopeConfigService.getConfig();

        assertThat(response.getUserDataScope()).hasSize(1);
        assertThat(response.getUserDataScope().get(0).getAllowedProjects()).containsExactly(101L, 102L);
        assertThat(response.getUserDataScope().get(0).getAllowedDepts()).containsExactly("TECH");
        assertThat(response.getDeptDataScope()).hasSize(2);
        assertThat(response.getDeptDataScope().get(0).isCanViewOtherDepts()).isTrue();
        assertThat(response.getDeptTree()).hasSize(2);
    }

    @Test
    void getAccessProfile_ShouldPreferUserRuleOverDepartmentRule() {
        User salesUser = User.builder()
                .id(1L)
                .username("alice")
                .fullName("Alice")
                .role(User.Role.STAFF)
                .departmentCode("SALES")
                .departmentName("销售部")
                .enabled(true)
                .build();
        String payloadJson = """
                {
                  "departmentTree": [
                    {
                      "departmentCode": "SALES",
                      "departmentName": "销售部",
                      "parentDepartmentCode": null,
                      "sortOrder": 1
                    }
                  ],
                  "userRules": [
                    {
                      "userId": 1,
                      "dataScope": "self",
                      "allowedProjectIds": [9],
                      "allowedDeptCodes": ["FINANCE"]
                    }
                  ],
                  "departmentRules": [
                    {
                      "departmentCode": "SALES",
                      "dataScope": "dept",
                      "canViewOtherDepts": true,
                      "allowedDeptCodes": ["TECH"]
                    }
                  ]
                }
                """;

        when(userRepository.findAll()).thenReturn(List.of(salesUser));
        when(systemSettingRepository.findByConfigKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY))
                .thenReturn(Optional.of(SystemSetting.builder().configKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY).payloadJson(payloadJson).build()));

        DataScopeConfigService.AccessProfile profile = dataScopeConfigService.getAccessProfile(salesUser);

        assertThat(profile.getDataScope()).isEqualTo("self");
        assertThat(profile.getExplicitProjectIds()).containsExactly(9L);
        assertThat(profile.getAllowedDepartmentCodes()).isEmpty();
    }

    @Test
    void getAccessProfile_ShouldExpandDescendantsForDeptAndSub() {
        User managerUser = User.builder()
                .id(2L)
                .username("manager")
                .fullName("Manager")
                .role(User.Role.MANAGER)
                .departmentCode("SALES")
                .departmentName("销售部")
                .enabled(true)
                .build();
        String payloadJson = """
                {
                  "departmentTree": [
                    {
                      "departmentCode": "SALES",
                      "departmentName": "销售部",
                      "parentDepartmentCode": null,
                      "sortOrder": 1
                    },
                    {
                      "departmentCode": "SALES_EAST",
                      "departmentName": "华东销售部",
                      "parentDepartmentCode": "SALES",
                      "sortOrder": 2
                    }
                  ],
                  "userRules": [],
                  "departmentRules": [
                    {
                      "departmentCode": "SALES",
                      "dataScope": "deptAndSub",
                      "canViewOtherDepts": false,
                      "allowedDeptCodes": []
                    }
                  ]
                }
                """;

        when(userRepository.findAll()).thenReturn(List.of(managerUser));
        when(systemSettingRepository.findByConfigKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY))
                .thenReturn(Optional.of(SystemSetting.builder().configKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY).payloadJson(payloadJson).build()));

        DataScopeConfigService.AccessProfile profile = dataScopeConfigService.getAccessProfile(managerUser);

        assertThat(profile.getAllowedDepartmentCodes()).containsExactly("SALES", "SALES_EAST");
    }

    @Test
    void getAccessProfile_ShouldGrantProjectsFromMatchingProjectGroupRule() {
        User staffUser = User.builder()
                .id(3L)
                .username("staff")
                .fullName("Staff")
                .role(User.Role.STAFF)
                .departmentCode("TECH")
                .departmentName("技术部")
                .enabled(true)
                .build();
        String payloadJson = """
                {
                  "departmentTree": [
                    {
                      "departmentCode": "TECH",
                      "departmentName": "技术部",
                      "parentDepartmentCode": null,
                      "sortOrder": 1
                    }
                  ],
                  "userRules": [],
                  "departmentRules": [],
                  "projectGroupRules": [
                    {
                      "groupCode": "G1",
                      "groupName": "重点项目组",
                      "managerUserId": 10,
                      "visibility": "custom",
                      "memberUserIds": [],
                      "allowedRoles": ["staff"],
                      "projectIds": [88, 99]
                    }
                  ]
                }
                """;

        when(userRepository.findAll()).thenReturn(List.of(staffUser));
        when(systemSettingRepository.findByConfigKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY))
                .thenReturn(Optional.of(SystemSetting.builder().configKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY).payloadJson(payloadJson).build()));

        DataScopeConfigService.AccessProfile profile = dataScopeConfigService.getAccessProfile(staffUser);

        assertThat(profile.getExplicitProjectIds()).containsExactly(88L, 99L);
    }

    @Test
    void saveConfig_ShouldPersistNormalizedRules() {
        User salesUser = User.builder()
                .id(1L)
                .username("alice")
                .fullName("Alice")
                .role(User.Role.STAFF)
                .departmentCode("SALES")
                .departmentName("销售部")
                .enabled(true)
                .build();
        DataScopeConfigResponse request = DataScopeConfigResponse.builder()
                .userDataScope(List.of(DataScopeConfigResponse.UserDataScopeItem.builder()
                        .userId(1L)
                        .dataScope("dept")
                        .allowedProjects(List.of(100L, 99L, 99L))
                        .allowedDepts(List.of("TECH", "TECH"))
                        .build()))
                .deptDataScope(List.of(DataScopeConfigResponse.DepartmentDataScopeItem.builder()
                        .deptCode("SALES")
                        .dataScope("dept")
                        .canViewOtherDepts(true)
                        .allowedDepts(List.of("TECH"))
                        .build()))
                .deptTree(List.of(
                        DataScopeConfigResponse.DepartmentTreeItem.builder()
                                .deptCode("SALES")
                                .deptName("销售部")
                                .sortOrder(1)
                                .build(),
                        DataScopeConfigResponse.DepartmentTreeItem.builder()
                                .deptCode("TECH")
                                .deptName("技术部")
                                .parentDeptCode("SALES")
                                .sortOrder(2)
                                .build()))
                .projectGroupScope(List.of(DataScopeConfigResponse.ProjectGroupScopeItem.builder()
                        .groupCode("G1")
                        .groupName("重点项目组")
                        .managerUserId(1L)
                        .visibility("custom")
                        .memberUserIds(List.of(2L, 2L))
                        .allowedRoles(List.of("staff", "staff"))
                        .projectIds(List.of(8L, 7L, 7L))
                        .build()))
                .build();
        AtomicReference<String> savedPayload = new AtomicReference<>();

        when(userRepository.findAll()).thenReturn(List.of(salesUser));
        when(systemSettingRepository.findByConfigKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY))
                .thenAnswer(invocation -> savedPayload.get() == null
                        ? Optional.empty()
                        : Optional.of(SystemSetting.builder()
                                .configKey(DataScopeConfigService.DATA_SCOPE_CONFIG_KEY)
                                .payloadJson(savedPayload.get())
                                .build()));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> {
            SystemSetting setting = invocation.getArgument(0);
            savedPayload.set(setting.getPayloadJson());
            return setting;
        });

        DataScopeConfigResponse response = dataScopeConfigService.saveConfig(request);

        assertThat(response.getUserDataScope().get(0).getAllowedProjects()).containsExactly(99L, 100L);
        assertThat(response.getDeptDataScope().get(0).getAllowedDepts()).containsExactly("TECH");
        assertThat(response.getDeptTree()).hasSize(2);
        assertThat(response.getProjectGroupScope().get(0).getProjectIds()).containsExactly(7L, 8L);
        assertThat(response.getProjectGroupScope().get(0).getMemberUserIds()).containsExactly(2L);
    }
}
