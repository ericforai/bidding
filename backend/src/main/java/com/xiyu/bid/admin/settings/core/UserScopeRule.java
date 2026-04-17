package com.xiyu.bid.admin.settings.core;

import java.util.List;

public record UserScopeRule(Long userId, String dataScope, List<Long> allowedProjectIds, List<String> allowedDeptCodes) {
}
