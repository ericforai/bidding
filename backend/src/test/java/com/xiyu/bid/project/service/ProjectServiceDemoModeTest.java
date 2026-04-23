package com.xiyu.bid.project.service;

import com.xiyu.bid.demo.service.DemoDataProvider;
import com.xiyu.bid.demo.service.DemoFusionService;
import com.xiyu.bid.demo.service.DemoModeService;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceDemoModeTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectAccessScopeService projectAccessScopeService;
    @Mock
    private DemoModeService demoModeService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                projectAccessScopeService,
                demoModeService,
                new DemoDataProvider(),
                new DemoFusionService()
        );
    }

    @Test
    void getProjectById_shouldReturnDemoProjectWhenE2eAndNegativeId() {
        when(demoModeService.isEnabled()).thenReturn(true);

        var project = projectService.getProjectById(-101L);

        assertThat(project).isNotNull();
        assertThat(project.getId()).isEqualTo(-101L);
        assertThat(project.getName()).isNotBlank();
    }

    @Test
    void updateProject_shouldRejectDemoMutation() {
        when(demoModeService.isEnabled()).thenReturn(true);

        assertThatThrownBy(() -> projectService.updateProject(-101L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void getProjectById_shouldThrowWhenDemoIdUnknown() {
        when(demoModeService.isEnabled()).thenReturn(true);

        assertThatThrownBy(() -> projectService.getProjectById(-9999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
