package com.xiyu.bid.workflowform.application.service;

import com.xiyu.bid.workflowform.application.command.WorkflowFormOaBindingCommand;
import com.xiyu.bid.workflowform.application.command.WorkflowFormTemplateDraftCommand;
import com.xiyu.bid.workflowform.application.port.InMemoryWorkflowFormAdminStore;
import com.xiyu.bid.workflowform.domain.FormBusinessType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowFormAdminServiceTest {

    @Test
    void admin_can_save_draft_bind_oa_and_publish_version() {
        InMemoryWorkflowFormAdminStore store = new InMemoryWorkflowFormAdminStore();
        WorkflowFormAdminService service = new WorkflowFormAdminService(store);

        var draft = service.saveDraft(new WorkflowFormTemplateDraftCommand(
                "SEAL_APPLY",
                "用章申请",
                FormBusinessType.GENERAL_WORKFLOW,
                true,
                schema("title")
        ));
        service.saveOaBinding(new WorkflowFormOaBindingCommand(
                "SEAL_APPLY",
                "WEAVER",
                "WF_SEAL",
                mapping("title", "field_title"),
                true
        ));
        var published = service.publish("SEAL_APPLY", "admin");

        assertThat(draft.status()).isEqualTo("DRAFT");
        assertThat(published.version()).isEqualTo(1);
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(store.findActive("SEAL_APPLY").orElseThrow().schema()).isEqualTo(schema("title"));
    }

    @Test
    void preview_trial_submit_returns_mapped_oa_payload_without_publishing_side_effects() {
        InMemoryWorkflowFormAdminStore store = new InMemoryWorkflowFormAdminStore();
        WorkflowFormAdminService service = new WorkflowFormAdminService(store);

        service.saveDraft(new WorkflowFormTemplateDraftCommand(
                "SEAL_APPLY",
                "用章申请",
                FormBusinessType.GENERAL_WORKFLOW,
                true,
                schema("title")
        ));
        service.saveOaBinding(new WorkflowFormOaBindingCommand(
                "SEAL_APPLY",
                "WEAVER",
                "WF_SEAL",
                mapping("title", "field_title"),
                true
        ));

        Map<String, Object> preview = service.previewTrialSubmit(
                "SEAL_APPLY",
                Map.of("title", "测试申请"),
                "李总"
        );

        assertThat(preview).containsEntry("workflowCode", "WF_SEAL");
        assertThat((Map<String, Object>) preview.get("mainFields")).containsEntry("field_title", "测试申请");
        assertThat(store.findActive("SEAL_APPLY")).isEmpty();
    }

    private static Map<String, Object> schema(String key) {
        return Map.of("fields", List.of(Map.of("key", key, "label", "标题", "type", "text", "required", true)));
    }

    private static Map<String, Object> mapping(String sourceKey, String target) {
        return Map.of(
                "workflowCode", "WF_SEAL",
                "mainFields", List.of(Map.of("source", "formData." + sourceKey, "target", target, "type", "string", "required", true))
        );
    }
}
