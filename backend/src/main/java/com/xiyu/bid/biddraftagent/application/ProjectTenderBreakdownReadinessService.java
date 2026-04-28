package com.xiyu.bid.biddraftagent.application;

import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectTenderBreakdownReadinessService {

    private final ProjectAccessScopeService projectAccessScopeService;
    private final TenderIntakeConfigurationReadiness configurationReadiness;

    public TenderBreakdownReadiness readiness(Long projectId) {
        projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
        return configurationReadiness.current();
    }
}
