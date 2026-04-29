package com.xiyu.bid.workflowform.application.service;

import com.xiyu.bid.workflowform.application.command.WorkflowFormSubmitCommand;
import com.xiyu.bid.workflowform.application.port.OaStartCommand;
import com.xiyu.bid.workflowform.application.port.OaStartResult;
import com.xiyu.bid.workflowform.application.port.OaWorkflowGateway;
import com.xiyu.bid.workflowform.application.port.WorkflowFormInstanceStore;
import com.xiyu.bid.workflowform.domain.FormBusinessType;
import com.xiyu.bid.workflowform.domain.WorkflowFormStatus;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowFormSubmissionServiceTest {

    @Test
    void submit_qualification_borrow_form_starts_oa_without_applying_borrow() {
        InMemoryWorkflowFormInstanceStore store = new InMemoryWorkflowFormInstanceStore();
        CapturingOaWorkflowGateway gateway = new CapturingOaWorkflowGateway();
        WorkflowFormSubmissionService service = new WorkflowFormSubmissionService(store, gateway, new NoopWorkflowFormAccessGuard(), TestTransactionTemplates.immediate());

        var view = service.submit(new WorkflowFormSubmitCommand(
                "QUALIFICATION_BORROW",
                FormBusinessType.QUALIFICATION_BORROW,
                10L,
                "小王",
                values()
        ));

        assertThat(view.id()).isEqualTo(1L);
        assertThat(view.status()).isEqualTo(WorkflowFormStatus.OA_APPROVING);
        assertThat(view.oaInstanceId()).isEqualTo("OA-1");
        assertThat(store.findById(1L).orElseThrow().businessApplied()).isFalse();
        assertThat(gateway.lastCommand.workflowCode()).isEqualTo("QUALIFICATION_BORROW");
    }

    @Test
    void oa_start_failure_preserves_local_instance_for_retry() {
        InMemoryWorkflowFormInstanceStore store = new InMemoryWorkflowFormInstanceStore();
        CapturingOaWorkflowGateway gateway = new CapturingOaWorkflowGateway();
        gateway.result = new OaStartResult(false, null, "OA 暂不可用");
        WorkflowFormSubmissionService service = new WorkflowFormSubmissionService(store, gateway, new NoopWorkflowFormAccessGuard(), TestTransactionTemplates.immediate());

        var view = service.submit(new WorkflowFormSubmitCommand(
                "QUALIFICATION_BORROW",
                FormBusinessType.QUALIFICATION_BORROW,
                10L,
                "小王",
                values()
        ));

        assertThat(view.status()).isEqualTo(WorkflowFormStatus.OA_FAILED);
        assertThat(store.findById(1L).orElseThrow().businessApplyError()).isEqualTo("OA 暂不可用");
    }

    private static Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("qualificationId", "1001");
        values.put("borrower", "小王");
        values.put("department", "投标管理部");
        values.put("projectId", "10");
        values.put("purpose", "用于投标文件编制");
        values.put("expectedReturnDate", "2026-05-10");
        return values;
    }

    static class CapturingOaWorkflowGateway implements OaWorkflowGateway {
        OaStartCommand lastCommand;
        OaStartResult result = new OaStartResult(true, "OA-1", null);

        @Override
        public OaStartResult startProcess(OaStartCommand command) {
            lastCommand = command;
            return result;
        }
    }
}
