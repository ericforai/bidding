package com.xiyu.bid.integration.organization.controller;

import com.xiyu.bid.integration.organization.application.OrganizationOperationsAppService;
import com.xiyu.bid.integration.organization.application.OrganizationOperationsStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/organization/operations")
@RequiredArgsConstructor
public class OrganizationOperationsController {
    private final OrganizationOperationsAppService operationsAppService;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrganizationOperationsStatusResponse status() {
        return operationsAppService.status();
    }
}
