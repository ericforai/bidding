// Input: dispatch parameters (type, recipient, title, body)
// Output: DispatchResult value indicating validity or error code
// Pos: Pure Core/通知派发策略
package com.xiyu.bid.notification.core;

/**
 * Pure validation policy for notification dispatch.
 *
 * <p>Returns business validation outcomes as values; never throws.
 */
public final class NotificationDispatchPolicy {

    private static final int MAX_TITLE_LENGTH = 200;

    private NotificationDispatchPolicy() {
    }

    public static DispatchResult validateDispatch(NotificationType type, Long userId, String title, String body) {
        if (type == null) {
            return DispatchResult.invalid("INVALID_TYPE", "Notification type must not be null");
        }
        if (userId == null) {
            return DispatchResult.invalid("INVALID_USER", "Recipient user id must not be null");
        }
        if (title == null || title.isBlank()) {
            return DispatchResult.invalid("INVALID_TITLE", "Notification title must not be blank");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return DispatchResult.invalid("TITLE_TOO_LONG",
                "Notification title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        return DispatchResult.valid();
    }

    public record DispatchResult(boolean isValid, String errorCode, String errorMessage) {

        public static DispatchResult valid() {
            return new DispatchResult(true, null, null);
        }

        public static DispatchResult invalid(String errorCode, String errorMessage) {
            return new DispatchResult(false, errorCode, errorMessage);
        }
    }
}
