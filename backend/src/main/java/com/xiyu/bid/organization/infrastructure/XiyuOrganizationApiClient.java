package com.xiyu.bid.organization.infrastructure;

import com.xiyu.bid.organization.config.OrganizationSyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Client for lookback queries to the Xiyu organization API.
 * After an event is received, this client fetches the authoritative
 * master data from the Xiyu REST API — the event payload is only
 * used as a trigger, per the reference document's section 2.2 constraint.
 */
@Component
public class XiyuOrganizationApiClient {

    private static final Logger log = LoggerFactory.getLogger(XiyuOrganizationApiClient.class);
    private final OrganizationSyncProperties properties;

    public XiyuOrganizationApiClient(OrganizationSyncProperties properties) {
        this.properties = properties;
    }

    /**
     * Fetch a single department by its deptId from the Xiyu API.
     * Returns empty if the department no longer exists (deleted upstream).
     */
    public DepartmentLookupResult fetchDepartment(String deptId) {
        log.info("Fetching department from Xiyu API: deptId={}", deptId);
        // TODO: implement HTTP call to {properties.getXiuyApiBaseUrl()}/departments/{deptId}
        return DepartmentLookupResult.notFound(deptId);
    }

    /**
     * Fetch a single user by its userId from the Xiyu API.
     */
    public UserLookupResult fetchUser(String userId) {
        log.info("Fetching user from Xiyu API: userId={}", userId);
        // TODO: implement HTTP call to {properties.getXiuyApiBaseUrl()}/users/{userId}
        return UserLookupResult.notFound(userId);
    }

    /**
     * Paginated fetch of all departments from the Xiyu API, for full initialization.
     */
    public List<DepartmentLookupResult> fetchAllDepartments(int page, int pageSize) {
        log.info("Fetching all departments page {} (size={})", page, pageSize);
        // TODO: implement HTTP call with pagination
        return List.of();
    }

    /**
     * Paginated fetch of all users from the Xiyu API, for full initialization.
     */
    public List<UserLookupResult> fetchAllUsers(int page, int pageSize) {
        log.info("Fetching all users page {} (size={})", page, pageSize);
        // TODO: implement HTTP call with pagination
        return List.of();
    }

    public record DepartmentLookupResult(
            String deptId, String deptName, String parentDeptId,
            String deptPath, String status, boolean found
    ) {
        public static DepartmentLookupResult notFound(String deptId) {
            return new DepartmentLookupResult(deptId, null, null, null, null, false);
        }
    }

    public record UserLookupResult(
            String userId, String userName, String email, String mobile,
            String deptId, String position, String status, boolean found
    ) {
        public static UserLookupResult notFound(String userId) {
            return new UserLookupResult(userId, null, null, null, null, null, null, false);
        }
    }
}
