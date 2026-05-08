package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.domain.OrganizationDepartmentSnapshot;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class DisabledOrganizationDirectoryGateway implements OrganizationDirectoryGateway {
    @Override
    public Optional<OrganizationDepartmentSnapshot> fetchDepartmentByDeptId(String deptId) {
        return Optional.empty();
    }

    @Override
    public Optional<OrganizationUserSnapshot> fetchUserByUserId(String userId) {
        return Optional.empty();
    }

    @Override
    public List<OrganizationDepartmentSnapshot> listDepartmentsByWindow(LocalDateTime startAt, LocalDateTime endAt) {
        return List.of();
    }

    @Override
    public List<OrganizationUserSnapshot> listUsersByWindow(LocalDateTime startAt, LocalDateTime endAt) {
        return List.of();
    }
}
