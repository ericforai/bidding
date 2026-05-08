package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.domain.OrganizationDepartmentSnapshot;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrganizationDirectoryGateway {
    Optional<OrganizationDepartmentSnapshot> fetchDepartmentByDeptId(String deptId);

    Optional<OrganizationUserSnapshot> fetchUserByUserId(String userId);

    List<OrganizationDepartmentSnapshot> listDepartmentsByWindow(LocalDateTime startAt, LocalDateTime endAt);

    List<OrganizationUserSnapshot> listUsersByWindow(LocalDateTime startAt, LocalDateTime endAt);
}
