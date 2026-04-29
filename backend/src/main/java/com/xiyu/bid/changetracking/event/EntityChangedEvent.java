package com.xiyu.bid.changetracking.event;

import java.util.Map;

public record EntityChangedEvent(
    String entityType,
    Long entityId,
    Long actorUserId,
    Map<String, Object> before,
    Map<String, Object> after,
    String entityTitle
) {}
