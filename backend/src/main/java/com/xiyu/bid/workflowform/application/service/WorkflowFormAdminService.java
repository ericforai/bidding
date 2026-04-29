package com.xiyu.bid.workflowform.application.service;

import com.xiyu.bid.workflowform.application.command.WorkflowFormOaBindingCommand;
import com.xiyu.bid.workflowform.application.command.WorkflowFormTemplateDraftCommand;
import com.xiyu.bid.workflowform.application.port.OaProcessBindingRecord;
import com.xiyu.bid.workflowform.application.port.WorkflowFormAdminStore;
import com.xiyu.bid.workflowform.application.port.WorkflowFormTemplateAdminRecord;
import com.xiyu.bid.workflowform.domain.ValidationResult;
import com.xiyu.bid.workflowform.domain.WorkflowFormOaMappingPolicy;
import com.xiyu.bid.workflowform.domain.WorkflowFormPreviewPolicy;
import com.xiyu.bid.workflowform.domain.WorkflowFormSchemaPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowFormAdminService {

    private final WorkflowFormAdminStore store;

    public List<WorkflowFormTemplateAdminRecord> listTemplates() {
        return store.listTemplates();
    }

    public WorkflowFormTemplateAdminRecord saveDraft(WorkflowFormTemplateDraftCommand command) {
        requireValidSchema(command.schema());
        return store.saveDraft(command);
    }

    public OaProcessBindingRecord saveOaBinding(WorkflowFormOaBindingCommand command) {
        requireValidMapping(command.fieldMapping());
        return store.saveBinding(command);
    }

    public WorkflowFormTemplateAdminRecord publish(String templateCode, String publishedBy) {
        WorkflowFormTemplateAdminRecord draft = store.findDraft(templateCode)
                .orElseThrow(() -> new IllegalArgumentException("流程表单草稿不存在"));
        requireValidSchema(draft.schema());
        OaProcessBindingRecord binding = store.findBinding(templateCode)
                .filter(OaProcessBindingRecord::enabled)
                .orElseThrow(() -> new IllegalArgumentException("流程表单未配置启用的 OA 绑定"));
        requireValidMapping(binding.fieldMapping());
        return store.publish(templateCode, publishedBy);
    }

    public Map<String, Object> previewTrialSubmit(String templateCode, Map<String, Object> formData, String applicantName) {
        OaProcessBindingRecord binding = store.findBinding(templateCode)
                .orElseThrow(() -> new IllegalArgumentException("流程表单未配置 OA 绑定"));
        requireValidMapping(binding.fieldMapping());
        return WorkflowFormPreviewPolicy.previewPayload(
                binding.fieldMapping(),
                formData,
                Map.of("formInstanceId", "PREVIEW", "templateCode", templateCode),
                Map.of("name", applicantName == null ? "" : applicantName)
        );
    }

    private void requireValidSchema(Map<String, Object> schema) {
        requireValid(WorkflowFormSchemaPolicy.validate(schema));
    }

    private void requireValidMapping(Map<String, Object> mapping) {
        Map<String, Object> merged = new LinkedHashMap<>(mapping == null ? Map.of() : mapping);
        requireValid(WorkflowFormOaMappingPolicy.validate(merged));
    }

    private void requireValid(ValidationResult result) {
        if (!result.valid()) {
            throw new IllegalArgumentException(String.join(";", result.errors()));
        }
    }
}
