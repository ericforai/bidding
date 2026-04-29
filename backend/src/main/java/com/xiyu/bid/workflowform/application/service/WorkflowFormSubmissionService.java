package com.xiyu.bid.workflowform.application.service;

import com.xiyu.bid.workflowform.application.command.WorkflowFormSubmitCommand;
import com.xiyu.bid.workflowform.application.port.OaStartCommand;
import com.xiyu.bid.workflowform.application.port.OaStartResult;
import com.xiyu.bid.workflowform.application.port.OaWorkflowGateway;
import com.xiyu.bid.workflowform.application.port.WorkflowFormInstanceRecord;
import com.xiyu.bid.workflowform.application.port.WorkflowFormInstanceStore;
import com.xiyu.bid.workflowform.application.view.WorkflowFormInstanceView;
import com.xiyu.bid.workflowform.domain.FormBusinessType;
import com.xiyu.bid.workflowform.domain.FormSubmissionValidator;
import com.xiyu.bid.workflowform.domain.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class WorkflowFormSubmissionService {

    private final WorkflowFormInstanceStore store;
    private final OaWorkflowGateway oaWorkflowGateway;
    private final WorkflowFormAccessGuard accessGuard;
    private final TransactionTemplate transactionTemplate;

    public WorkflowFormInstanceView submit(WorkflowFormSubmitCommand command) {
        accessGuard.assertCanAccessProject(command.projectId());
        validate(command);
        requireConsistentProjectId(command);
        Long id = transactionTemplate.execute(status ->
                store.create(command.businessType(), command.templateCode(), command.projectId(), command.applicantName(), command.formData()));
        OaStartResult result = oaWorkflowGateway.startProcess(new OaStartCommand(
                command.templateCode(), command.businessType(), id, command.applicantName(), command.formData()));
        if (!result.success()) {
            transactionTemplate.executeWithoutResult(status ->
                    store.markOaFailed(id, result.oaInstanceId(), result.errorMessage() == null ? "OA 流程发起失败" : result.errorMessage()));
            return toView(store.findById(id).orElseThrow());
        }
        transactionTemplate.executeWithoutResult(status -> store.markOaApproving(id, result.oaInstanceId()));
        return toView(store.findById(id).orElseThrow());
    }

    private void requireConsistentProjectId(WorkflowFormSubmitCommand command) {
        Object formProjectId = command.formData().get("projectId");
        if (command.projectId() != null && formProjectId != null
                && !String.valueOf(command.projectId()).equals(String.valueOf(formProjectId))) {
            throw new IllegalArgumentException("表单项目必须与提交项目一致");
        }
    }

    private void validate(WorkflowFormSubmitCommand command) {
        if (command.businessType() == FormBusinessType.QUALIFICATION_BORROW) {
            ValidationResult result = FormSubmissionValidator.validateQualificationBorrow(command.formData());
            if (!result.valid()) {
                throw new IllegalArgumentException(String.join(";", result.errors()));
            }
        }
    }

    private WorkflowFormInstanceView toView(WorkflowFormInstanceRecord record) {
        return new WorkflowFormInstanceView(record.id(), record.businessType(), record.templateCode(),
                record.projectId(), record.status(), record.oaInstanceId(), record.businessApplyError());
    }
}
