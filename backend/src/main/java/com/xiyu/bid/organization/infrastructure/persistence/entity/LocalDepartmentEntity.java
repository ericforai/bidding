package com.xiyu.bid.organization.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_departments")
@Getter
@Setter
public class LocalDepartmentEntity {
    @Id
    @Column(name = "department_code", nullable = false, unique = true, length = 100)
    private String departmentCode;

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;

    @Column(name = "parent_department_code", length = 100)
    private String parentDepartmentCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
