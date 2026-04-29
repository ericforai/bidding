package com.xiyu.bid.matrixcollaboration.infrastructure.persistence.repository;

import com.xiyu.bid.matrixcollaboration.infrastructure.persistence.entity.CrmCustomerPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmCustomerPermissionRepository extends JpaRepository<CrmCustomerPermission, Long> {

    List<CrmCustomerPermission> findByCustomerId(String customerId);

    List<CrmCustomerPermission> findByUserId(Long userId);

    void deleteByCustomerId(String customerId);
}
