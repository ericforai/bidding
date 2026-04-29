// Input: EntityChangedEvent 领域事件（AFTER_COMMIT）
// Output: 订阅者扇出 + DOCUMENT_CHANGE 通知派发
// Pos: Listener/变更追踪通知扩散
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.changetracking.listener;

import com.xiyu.bid.changetracking.core.ChangeDiffPolicy;
import com.xiyu.bid.changetracking.core.FieldChange;
import com.xiyu.bid.changetracking.event.EntityChangedEvent;
import com.xiyu.bid.notification.core.NotificationType;
import com.xiyu.bid.notification.dto.CreateNotificationRequest;
import com.xiyu.bid.notification.service.NotificationApplicationService;
import com.xiyu.bid.subscription.entity.Subscription;
import com.xiyu.bid.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class EntityChangedNotificationListener {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationApplicationService notificationService;

    public EntityChangedNotificationListener(
        SubscriptionRepository subscriptionRepository,
        NotificationApplicationService notificationService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntityChanged(EntityChangedEvent event) {
        List<Long> recipientIds = subscriptionRepository
            .findByTargetEntityTypeAndTargetEntityId(event.entityType(), event.entityId())
            .stream()
            .map(Subscription::getUserId)
            .filter(id -> !Objects.equals(id, event.actorUserId()))
            .distinct()
            .toList();

        if (recipientIds.isEmpty()) {
            return;
        }

        List<FieldChange> changes = ChangeDiffPolicy.diff(event.before(), event.after());
        if (changes.isEmpty()) {
            return;
        }

        String title = buildTitle(event.entityTitle());
        Map<String, Object> payload = Map.of("changes", changes);

        notificationService.createNotification(
            new CreateNotificationRequest(
                NotificationType.DOCUMENT_CHANGE.name(),
                event.entityType(),
                event.entityId(),
                title,
                null,
                payload,
                recipientIds
            ),
            event.actorUserId()
        );
    }

    private static String buildTitle(String entityTitle) {
        String safe = entityTitle == null || entityTitle.isBlank() ? "对象" : entityTitle;
        return "《" + safe + "》有更新";
    }
}
