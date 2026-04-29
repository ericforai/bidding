package com.xiyu.bid.workflowform.infrastructure.oa;

import com.xiyu.bid.workflowform.application.port.OaStartCommand;
import com.xiyu.bid.workflowform.application.port.OaStartResult;
import com.xiyu.bid.workflowform.application.port.OaWorkflowGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "oa.workflow.mode", havingValue = "mock")
public class MockOaWorkflowGateway implements OaWorkflowGateway {

    @Override
    public OaStartResult startProcess(OaStartCommand command) {
        return new OaStartResult(true, "MOCK-OA-" + UUID.randomUUID(), null);
    }
}
