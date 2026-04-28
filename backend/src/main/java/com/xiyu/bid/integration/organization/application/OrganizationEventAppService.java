package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.entity.RoleProfile;
import com.xiyu.bid.entity.RoleProfileCatalog;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.integration.organization.domain.OrganizationEventEnvelope;
import com.xiyu.bid.integration.organization.domain.OrganizationEventPolicy;
import com.xiyu.bid.integration.organization.domain.OrganizationEventStatus;
import com.xiyu.bid.integration.organization.domain.OrganizationEventValidation;
import com.xiyu.bid.integration.organization.domain.OrganizationEventType;
import com.xiyu.bid.integration.organization.domain.OrganizationUserSyncPlan;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookData;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookRequest;
import com.xiyu.bid.integration.organization.dto.OrganizationEventWebhookResponse;
import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationDepartmentEntity;
import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationEventLogEntity;
import com.xiyu.bid.integration.organization.infrastructure.persistence.repository.OrganizationDepartmentRepository;
import com.xiyu.bid.integration.organization.infrastructure.persistence.repository.OrganizationEventLogRepository;
import com.xiyu.bid.repository.RoleProfileRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrganizationEventAppService {

    private static final String LOCKED_PASSWORD_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOHIhi4YhML26vP7Hk1UR93E1Vda8yI9W";

    private final OrganizationEventLogRepository eventLogRepository;
    private final OrganizationDepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final RoleProfileRepository roleProfileRepository;
    private final OrganizationIntegrationProperties properties;

    @Transactional
    public OrganizationEventWebhookResponse receiveWebhook(
            OrganizationEventWebhookRequest request,
            String traceId,
            String sourceApp
    ) {
        String payload = request == null ? "" : request.eventMessage();
        String eventKey = OrganizationEventKeyFactory.build(sourceApp, traceId, request == null ? "" : request.eventTopic(), payload);
        OrganizationEventEnvelope envelope = new OrganizationEventEnvelope(
                request == null ? null : request.eventTopic(),
                sourceApp,
                traceId,
                payload
        );
        if (!properties.isEnabled()) {
            saveLog(eventKey, envelope, OrganizationEventStatus.REJECTED, "组织架构事件接入已关闭");
            return OrganizationEventResponseFactory.response("500", "组织架构事件接入已关闭", eventKey, false, false, OrganizationEventStatus.REJECTED);
        }
        if (eventLogRepository.existsByEventKey(eventKey)) {
            return OrganizationEventResponseFactory.response("200", "success", eventKey, true, true, OrganizationEventStatus.DUPLICATE);
        }
        OrganizationEventValidation validation = OrganizationEventPolicy.validateEnvelope(
                envelope,
                Set.copyOf(properties.getAllowedSourceApps())
        );
        if (!validation.valid()) {
            saveLog(eventKey, envelope, OrganizationEventStatus.REJECTED, validation.message());
            return OrganizationEventResponseFactory.response("500", validation.message(), eventKey, false, false, OrganizationEventStatus.REJECTED);
        }
        if (!reserveLog(eventKey, envelope)) {
            return OrganizationEventResponseFactory.response("200", "success", eventKey, true, true, OrganizationEventStatus.DUPLICATE);
        }
        return processValidatedEvent(eventKey, envelope, validation.type());
    }

    private OrganizationEventWebhookResponse processValidatedEvent(
            String eventKey,
            OrganizationEventEnvelope envelope,
            OrganizationEventType type
    ) {
        try {
            if (isUserEvent(type)) {
                syncUser(envelope.message(), type != OrganizationEventType.USER_DISABLE, type == OrganizationEventType.USER_ROLE_CHANGED);
            } else if (type == OrganizationEventType.DEPARTMENT_UPSERT || type == OrganizationEventType.DEPARTMENT_DISABLE) {
                syncDepartment(envelope.message(), type == OrganizationEventType.DEPARTMENT_UPSERT);
            }
            saveLog(eventKey, envelope, OrganizationEventStatus.PROCESSED, "success");
            return OrganizationEventResponseFactory.response("200", "success", eventKey, true, false, OrganizationEventStatus.PROCESSED);
        } catch (RuntimeException ex) {
            saveLog(eventKey, envelope, OrganizationEventStatus.FAILED, ex.getMessage());
            return OrganizationEventResponseFactory.response("500", ex.getMessage(), eventKey, false, false, OrganizationEventStatus.FAILED);
        }
    }

    private boolean isUserEvent(OrganizationEventType type) {
        return type == OrganizationEventType.USER_UPSERT
                || type == OrganizationEventType.USER_DISABLE
                || type == OrganizationEventType.USER_ROLE_CHANGED;
    }

    private void syncUser(String payload, boolean enabled, boolean roleOnlyPatch) {
        OrganizationUserSyncPlan plan = OrganizationEventPolicy.planUserSync(
                OrganizationEventPayloadMapper.toUserSnapshot(payload, enabled),
                normalizeSet(properties.getAdminRoleCodes()),
                normalizeSet(properties.getManagerRoleCodes())
        );
        Optional<User> existingUser = userRepository.findByUsername(plan.username());
        User user = existingUser.orElseGet(User::new);
        boolean preserveProfileFields = roleOnlyPatch && existingUser.isPresent();
        user.setUsername(plan.username());
        user.setPassword(user.getPassword() == null ? LOCKED_PASSWORD_HASH : user.getPassword());
        if (!preserveProfileFields) {
            user.setEmail(plan.email());
            user.setFullName(plan.fullName());
            user.setPhone(plan.phone());
            user.setDepartmentCode(plan.departmentCode());
            user.setDepartmentName(plan.departmentName());
        }
        user.setEnabled(plan.enabled());
        applyRole(user, plan.roleCode());
        userRepository.save(user);
    }

    private void syncDepartment(String payload, boolean enabled) {
        OrganizationEventPayloadMapper.DepartmentPayload payloadData = OrganizationEventPayloadMapper.toDepartmentPayload(payload);
        String normalizedCode = payloadData.code().toLowerCase(Locale.ROOT);
        OrganizationDepartmentEntity department = departmentRepository.findById(normalizedCode).orElseGet(OrganizationDepartmentEntity::new);
        department.setDepartmentCode(normalizedCode);
        department.setDepartmentName(payloadData.name());
        department.setParentDepartmentCode(payloadData.parentCode());
        department.setEnabled(enabled);
        departmentRepository.save(department);
    }

    private void applyRole(User user, String roleCode) {
        RoleProfile roleProfile = roleProfileRepository.findByCodeIgnoreCase(roleCode)
                .or(() -> roleProfileRepository.findByCodeIgnoreCase(RoleProfileCatalog.STAFF_CODE))
                .orElse(null);
        user.setRoleProfile(roleProfile);
        user.setRole(RoleProfileCatalog.legacyRoleForCode(roleProfile == null ? roleCode : roleProfile.getCode()));
    }

    private void saveLog(String eventKey, OrganizationEventEnvelope envelope, OrganizationEventStatus status, String message) {
        OrganizationEventLogEntity log = eventLogRepository.findByEventKey(eventKey)
                .orElseGet(() -> buildLog(eventKey, envelope, status, message));
        log.setStatus(status);
        log.setMessage(message == null ? "" : message);
        log.setProcessedAt(LocalDateTime.now());
        eventLogRepository.save(log);
    }

    private boolean reserveLog(String eventKey, OrganizationEventEnvelope envelope) {
        try {
            eventLogRepository.saveAndFlush(buildLog(eventKey, envelope, OrganizationEventStatus.PROCESSING, "processing"));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private OrganizationEventLogEntity buildLog(
            String eventKey,
            OrganizationEventEnvelope envelope,
            OrganizationEventStatus status,
            String message
    ) {
        OrganizationEventLogEntity log = new OrganizationEventLogEntity();
        log.setEventKey(eventKey);
        log.setEventTopic(blankToEmpty(envelope.topic()));
        log.setSourceApp(blankToEmpty(envelope.sourceApp()));
        log.setTraceId(blankToEmpty(envelope.traceId()));
        log.setPayloadHash(OrganizationEventKeyFactory.hash(blankToEmpty(envelope.message())));
        log.setStatus(status);
        log.setMessage(message == null ? "" : message);
        log.setProcessedAt(LocalDateTime.now());
        return log;
    }

    private Set<String> normalizeSet(java.util.List<String> values) {
        return values.stream().map(value -> value.trim().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
