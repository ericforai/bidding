package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.entity.RoleProfile;
import com.xiyu.bid.entity.RoleProfileCatalog;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.integration.organization.domain.OrganizationSyncPolicy;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSnapshot;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSyncPlan;
import com.xiyu.bid.repository.RoleProfileRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrganizationUserSyncWriter {
    private static final String LOCKED_PASSWORD_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOHIhi4YhML26vP7Hk1UR93E1Vda8yI9W";

    private final UserRepository userRepository;
    private final RoleProfileRepository roleProfileRepository;
    private final OrganizationIntegrationProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User upsert(String sourceApp, String eventKey, OrganizationUserSnapshot snapshot) {
        User user = userRepository.findByExternalOrgSourceAppAndExternalOrgUserId(sourceApp, snapshot.externalUserId())
                .or(() -> userRepository.findByUsername(fallbackUsername(snapshot)))
                .orElseGet(User::new);
        OrganizationUserSyncPlan plan = OrganizationSyncPolicy.planUserSync(
                snapshot,
                user.getRoleCode(),
                normalizeSet(properties.getAdminRoleCodes()),
                normalizeSet(properties.getManagerRoleCodes())
        );
        user.setUsername(plan.username());
        user.setPassword(user.getPassword() == null ? LOCKED_PASSWORD_HASH : user.getPassword());
        user.setEmail(plan.email());
        user.setFullName(plan.fullName());
        user.setPhone(plan.phone());
        user.setDepartmentCode(plan.departmentCode());
        user.setDepartmentName(plan.departmentName());
        user.setEnabled(plan.enabled());
        user.setExternalOrgUserId(snapshot.externalUserId());
        user.setExternalOrgSourceApp(sourceApp);
        user.setLastOrgEventKey(eventKey);
        user.setLastOrgSyncedAt(LocalDateTime.now());
        applyRole(user, plan.roleCode());
        return userRepository.save(user);
    }

    private void applyRole(User user, String roleCode) {
        RoleProfile roleProfile = roleProfileRepository.findByCodeIgnoreCase(roleCode)
                .or(() -> roleProfileRepository.findByCodeIgnoreCase(RoleProfileCatalog.STAFF_CODE))
                .orElse(null);
        user.setRoleProfile(roleProfile);
        user.setRole(RoleProfileCatalog.legacyRoleForCode(roleProfile == null ? roleCode : roleProfile.getCode()));
    }

    private Set<String> normalizeSet(java.util.List<String> values) {
        return values.stream().map(value -> value.trim().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
    }

    private String fallbackUsername(OrganizationUserSnapshot snapshot) {
        return (snapshot.username() == null || snapshot.username().isBlank())
                ? snapshot.externalUserId().trim().toLowerCase(Locale.ROOT)
                : snapshot.username().trim().toLowerCase(Locale.ROOT);
    }
}
