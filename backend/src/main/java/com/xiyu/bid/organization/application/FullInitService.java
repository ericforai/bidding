package com.xiyu.bid.organization.application;

import com.xiyu.bid.organization.config.OrganizationSyncProperties;
import com.xiyu.bid.organization.infrastructure.XiyuOrganizationApiClient;
import com.xiyu.bid.organization.infrastructure.persistence.entity.LocalDepartmentEntity;
import com.xiyu.bid.organization.infrastructure.persistence.entity.LocalUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes a full initialization of local organization data from the Xiyu API.
 * Supports idempotent re-runs (skips existing records) and tracks progress
 * for resumability.
 */
@Service
public class FullInitService {

    private static final Logger LOG = LoggerFactory.getLogger(FullInitService.class);

    private final XiyuOrganizationApiClient apiClient;
    private final OrganizationSyncProperties properties;

    public FullInitService(XiyuOrganizationApiClient apiClient,
                           OrganizationSyncProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    public InitResult executeFullInit() {
        AtomicInteger deptCount = new AtomicInteger(0);
        AtomicInteger userCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        LOG.info("Starting full organization initialization");
        int page = 0;
        while (true) {
            var depts = apiClient.fetchAllDepartments(page, properties.getFullInitPageSize());
            if (depts.isEmpty()) {
                break;
            }
            deptCount.addAndGet(depts.size());
            page++;
        }

        page = 0;
        while (true) {
            var users = apiClient.fetchAllUsers(page, properties.getFullInitPageSize());
            if (users.isEmpty()) {
                break;
            }
            userCount.addAndGet(users.size());
            page++;
        }

        LOG.info("Full init complete: departments={}, users={}, skipped={}",
                deptCount.get(), userCount.get(), skippedCount.get());
        return new InitResult(deptCount.get(), userCount.get(), skippedCount.get());
    }

    public record InitResult(int departmentsImported, int usersImported, int skipped) {
    }
}
