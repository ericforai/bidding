package com.xiyu.bid.admin.settings.core;

import java.util.List;

public record RoleAccessRule(String dataScope, List<Long> allowedProjectIds, List<String> allowedDeptCodes) {
}
