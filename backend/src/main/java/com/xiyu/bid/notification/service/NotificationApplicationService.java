// Input: REST request payloads, authenticated user id, paging
// Output: paged DTOs, dispatch/read result values, transactional persistence
// Pos: Service/通知应用服务（Split-First 编排层）
package com.xiyu.bid.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.notification.core.NotificationDispatchPolicy;
import com.xiyu.bid.notification.core.NotificationDispatchPolicy.DispatchResult;
import com.xiyu.bid.notification.core.NotificationReadPolicy;
import com.xiyu.bid.notification.core.NotificationReadPolicy.ReadResult;
import com.xiyu.bid.notification.core.NotificationType;
import com.xiyu.bid.notification.dto.CreateNotificationRequest;
import com.xiyu.bid.notification.dto.NotificationAssembler;
import com.xiyu.bid.notification.dto.NotificationSummary;
import com.xiyu.bid.notification.entity.Notification;
import com.xiyu.bid.notification.entity.UserNotification;
import com.xiyu.bid.notification.repository.NotificationRepository;
import com.xiyu.bid.notification.repository.UserNotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates notification dispatch and read flows.
 *
 * <p>Pure validation lives in {@link NotificationDispatchPolicy} /
 * {@link NotificationReadPolicy}. This class only loads/saves and forwards
 * decisions as values.
 */
@Service
@Transactional(readOnly = true)
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ObjectMapper objectMapper;

    public NotificationApplicationService(
        NotificationRepository notificationRepository,
        UserNotificationRepository userNotificationRepository,
        ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.objectMapper = objectMapper;
    }

    public Page<NotificationSummary> getNotifications(Long userId, Pageable pageable) {
        return userNotificationRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(NotificationAssembler::toSummary);
    }

    public long getUnreadCount(Long userId) {
        return userNotificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public ReadResult markAsRead(Long notificationId, Long userId) {
        Optional<UserNotification> found =
            userNotificationRepository.findByNotificationIdAndUserId(notificationId, userId);
        if (found.isEmpty()) {
            return ReadResult.forbidden();
        }
        UserNotification un = found.get();
        ReadResult result = NotificationReadPolicy.validateRead(
            un.getUserId(),
            userId,
            toInstant(un.getReadAt())
        );
        if (result.isValid() && !result.alreadyRead()) {
            un.setReadAt(LocalDateTime.now());
            userNotificationRepository.save(un);
        }
        return result;
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        userNotificationRepository.markAllReadForUser(userId, LocalDateTime.now());
    }

    @Transactional
    public DispatchResult createNotification(CreateNotificationRequest request, Long createdBy) {
        NotificationType type = parseType(request.type());
        if (type == null) {
            return DispatchResult.invalid("INVALID_TYPE",
                "Notification type must be one of NotificationType");
        }

        for (Long recipientId : request.recipientUserIds()) {
            DispatchResult validation = NotificationDispatchPolicy.validateDispatch(
                type, recipientId, request.title(), request.body());
            if (!validation.isValid()) {
                return validation;
            }
        }

        Notification toPersist = Notification.builder()
            .type(type.name())
            .sourceEntityType(request.sourceEntityType())
            .sourceEntityId(request.sourceEntityId())
            .title(request.title())
            .body(request.body())
            .payloadJson(serializePayload(request.payload()))
            .createdBy(createdBy)
            .build();
        Notification saved = notificationRepository.save(toPersist);

        List<UserNotification> userRows = new ArrayList<>(request.recipientUserIds().size());
        for (Long recipientId : request.recipientUserIds()) {
            userRows.add(UserNotification.builder()
                .notification(saved)
                .userId(recipientId)
                .build());
        }
        userNotificationRepository.saveAll(userRows);

        return DispatchResult.valid();
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static NotificationType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return NotificationType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static java.time.Instant toInstant(LocalDateTime ldt) {
        return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
    }
}
