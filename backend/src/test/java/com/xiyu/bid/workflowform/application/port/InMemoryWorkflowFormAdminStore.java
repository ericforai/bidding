package com.xiyu.bid.workflowform.application.port;

import com.xiyu.bid.workflowform.application.command.WorkflowFormOaBindingCommand;
import com.xiyu.bid.workflowform.application.command.WorkflowFormTemplateDraftCommand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryWorkflowFormAdminStore implements WorkflowFormAdminStore {
    private final Map<String, WorkflowFormTemplateAdminRecord> drafts = new LinkedHashMap<>();
    private final Map<String, WorkflowFormTemplateRecord> active = new LinkedHashMap<>();
    private final Map<String, OaProcessBindingRecord> bindings = new LinkedHashMap<>();
    private final Map<String, Integer> versions = new LinkedHashMap<>();

    @Override
    public List<WorkflowFormTemplateAdminRecord> listTemplates() {
        return new ArrayList<>(drafts.values());
    }

    @Override
    public Optional<WorkflowFormTemplateAdminRecord> findDraft(String templateCode) {
        return Optional.ofNullable(drafts.get(templateCode));
    }

    @Override
    public Optional<WorkflowFormTemplateRecord> findActive(String templateCode) {
        return Optional.ofNullable(active.get(templateCode));
    }

    @Override
    public Optional<OaProcessBindingRecord> findBinding(String templateCode) {
        return Optional.ofNullable(bindings.get(templateCode));
    }

    @Override
    public WorkflowFormTemplateAdminRecord saveDraft(WorkflowFormTemplateDraftCommand command) {
        WorkflowFormTemplateAdminRecord record = new WorkflowFormTemplateAdminRecord(command.templateCode(),
                command.name(), command.businessType(), versions.getOrDefault(command.templateCode(), 0),
                command.enabled(), "DRAFT", command.schema());
        drafts.put(command.templateCode(), record);
        return record;
    }

    @Override
    public OaProcessBindingRecord saveBinding(WorkflowFormOaBindingCommand command) {
        OaProcessBindingRecord record = new OaProcessBindingRecord(command.templateCode(), command.provider(),
                command.workflowCode(), command.fieldMapping(), command.enabled());
        bindings.put(command.templateCode(), record);
        return record;
    }

    @Override
    public WorkflowFormTemplateAdminRecord publish(String templateCode, String publishedBy) {
        WorkflowFormTemplateAdminRecord draft = drafts.get(templateCode);
        int next = versions.getOrDefault(templateCode, 0) + 1;
        versions.put(templateCode, next);
        active.put(templateCode, new WorkflowFormTemplateRecord(templateCode, draft.businessType(), next, draft.schema()));
        WorkflowFormTemplateAdminRecord published = new WorkflowFormTemplateAdminRecord(templateCode, draft.name(),
                draft.businessType(), next, draft.enabled(), "PUBLISHED", draft.schema());
        drafts.put(templateCode, published);
        return published;
    }
}
