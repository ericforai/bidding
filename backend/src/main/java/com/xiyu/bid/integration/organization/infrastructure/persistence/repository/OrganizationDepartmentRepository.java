package com.xiyu.bid.integration.organization.infrastructure.persistence.repository;

import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationDepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationDepartmentRepository extends JpaRepository<OrganizationDepartmentEntity, String> {
}
