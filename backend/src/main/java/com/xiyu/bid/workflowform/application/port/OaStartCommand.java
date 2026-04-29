package com.xiyu.bid.workflowform.application.port;

import com.xiyu.bid.workflowform.domain.FormBusinessType;

import java.util.Map;

public record OaStartCommand(
        String workflowCode,
        FormBusinessType businessType,
        Long formInstanceId,
        String applicantName,
        Map<String, Object> formData,
        String templateCode,
        Map<String, Object> mappedPayload
) {
}
