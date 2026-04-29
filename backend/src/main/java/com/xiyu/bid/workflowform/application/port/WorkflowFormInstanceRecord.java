package com.xiyu.bid.workflowform.application.port;

import com.xiyu.bid.workflowform.domain.FormBusinessType;
import com.xiyu.bid.workflowform.domain.WorkflowFormStatus;

import java.util.Map;

public record WorkflowFormInstanceRecord(
        Long id,
        FormBusinessType businessType,
        String templateCode,
        Long projectId,
        String applicantName,
        WorkflowFormStatus status,
        Map<String, Object> formData,
        String oaInstanceId,
        boolean businessApplied,
        String businessApplyError
) {
    public WorkflowFormInstanceRecord withStatus(WorkflowFormStatus newStatus) {
        return new WorkflowFormInstanceRecord(id, businessType, templateCode, projectId, applicantName,
                newStatus, formData, oaInstanceId, businessApplied, businessApplyError);
    }

    public WorkflowFormInstanceRecord withOaInstanceId(String newOaInstanceId) {
        return new WorkflowFormInstanceRecord(id, businessType, templateCode, projectId, applicantName,
                status, formData, newOaInstanceId, businessApplied, businessApplyError);
    }

    public WorkflowFormInstanceRecord withBusinessApplied(boolean applied) {
        return new WorkflowFormInstanceRecord(id, businessType, templateCode, projectId, applicantName,
                status, formData, oaInstanceId, applied, businessApplyError);
    }

    public WorkflowFormInstanceRecord withBusinessApplyError(String error) {
        return new WorkflowFormInstanceRecord(id, businessType, templateCode, projectId, applicantName,
                status, formData, oaInstanceId, businessApplied, error);
    }
}
