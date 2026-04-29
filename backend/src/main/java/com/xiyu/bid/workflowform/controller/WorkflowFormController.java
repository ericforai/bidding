package com.xiyu.bid.workflowform.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.workflowform.application.command.WorkflowFormSubmitCommand;
import com.xiyu.bid.workflowform.application.service.WorkflowFormAccessGuard;
import com.xiyu.bid.workflowform.application.service.WorkflowFormSubmissionService;
import com.xiyu.bid.workflowform.application.service.WorkflowFormTemplateQueryService;
import com.xiyu.bid.workflowform.application.view.WorkflowFormInstanceView;
import com.xiyu.bid.workflowform.domain.WorkflowFormStatus;
import com.xiyu.bid.workflowform.dto.WorkflowFormSubmitRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/workflow-forms")
@RequiredArgsConstructor
public class WorkflowFormController {

    private final WorkflowFormSubmissionService submissionService;
    private final WorkflowFormTemplateQueryService templateQueryService;
    private final WorkflowFormAccessGuard accessGuard;

    @GetMapping("/templates/{templateCode}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> activeTemplate(@PathVariable String templateCode) {
        return ResponseEntity.ok(ApiResponse.success(templateQueryService.getActiveSchema(templateCode)));
    }

    @PostMapping("/instances")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<WorkflowFormInstanceView>> submit(@Valid @RequestBody WorkflowFormSubmitRequest request) {
        accessGuard.assertCanAccessProject(request.projectId());
        WorkflowFormInstanceView view = submissionService.submit(new WorkflowFormSubmitCommand(
                request.templateCode(), request.businessType(), request.projectId(), request.applicantName(), request.formData()));
        return ResponseEntity.ok(ApiResponse.success(submitMessage(view), view));
    }

    private String submitMessage(WorkflowFormInstanceView view) {
        if (view.status() == WorkflowFormStatus.OA_FAILED) {
            return "流程表单已保存，OA 发起失败，等待重试";
        }
        return "流程表单已提交 OA 审批";
    }
}
