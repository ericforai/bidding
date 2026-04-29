// Input: notification id + recipient ids + descriptive fields
// Output: event payload consumed by outbound push listeners
// Pos: Event/通知创建领域事件
package com.xiyu.bid.notification.outbound.event;

import java.util.List;

public record NotificationCreatedEvent(
    Long notificationId,
    List<Long> recipientUserIds,
    String type,
    String title,
    String sourceEntityType,
    Long sourceEntityId
) {
}
