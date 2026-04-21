package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.marketinsight.core.CustomerOpportunityLifecyclePolicy;
import com.xiyu.bid.marketinsight.dto.CustomerOpportunityAssembler;
import com.xiyu.bid.marketinsight.dto.CustomerPredictionDTO;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import com.xiyu.bid.marketinsight.gateway.CustomerPredictionGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Command application service for customer opportunity lifecycle changes.
 */
@Service
@RequiredArgsConstructor
public class CustomerOpportunityStatusCommandService {

    private final CustomerPredictionGateway customerPredictionGateway;

    @Transactional
    public CustomerPredictionDTO transitionPrediction(final Long id, final String targetStatus) {
        CustomerPrediction prediction = findPrediction(id);
        CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus target =
                parseStatus(targetStatus);
        CustomerOpportunityLifecyclePolicy.LifecycleDecision decision =
                CustomerOpportunityLifecyclePolicy.transition(
                        mapStatus(prediction.getStatus()),
                        target);
        if (!decision.allowed()) {
            throw new IllegalStateException(decision.reason());
        }

        prediction.setStatus(CustomerPrediction.Status.valueOf(decision.nextStatus().name()));
        return CustomerOpportunityAssembler.toDTO(customerPredictionGateway.save(prediction));
    }

    @Transactional
    public CustomerPredictionDTO convertPrediction(final Long id, final Long projectId) {
        CustomerPrediction prediction = findPrediction(id);
        CustomerOpportunityLifecyclePolicy.LifecycleDecision decision =
                CustomerOpportunityLifecyclePolicy.convert(
                        mapStatus(prediction.getStatus()),
                        projectId);
        if (!decision.allowed()) {
            throw new IllegalStateException(decision.reason());
        }

        prediction.setStatus(CustomerPrediction.Status.valueOf(decision.nextStatus().name()));
        prediction.setConvertedProjectId(projectId);
        return CustomerOpportunityAssembler.toDTO(customerPredictionGateway.save(prediction));
    }

    private CustomerPrediction findPrediction(final Long id) {
        return customerPredictionGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerPrediction", String.valueOf(id)));
    }

    private CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus parseStatus(
            final String targetStatus) {
        if (targetStatus == null || targetStatus.isBlank()) {
            throw new IllegalArgumentException("status 不能为空");
        }
        try {
            return CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.valueOf(
                    targetStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的状态: " + targetStatus, exception);
        }
    }

    private CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus mapStatus(
            final CustomerPrediction.Status status) {
        return status == null
                ? null
                : CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.valueOf(status.name());
    }
}
