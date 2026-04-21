package com.xiyu.bid.marketinsight.mapper;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.marketinsight.core.CustomerOpportunityLifecyclePolicy;
import com.xiyu.bid.marketinsight.core.CustomerOpportunityRefreshPolicy;
import com.xiyu.bid.marketinsight.core.CustomerOpportunityTenderSnapshot;
import com.xiyu.bid.marketinsight.core.IndustryClassificationPolicy;
import com.xiyu.bid.marketinsight.core.PurchaserExtractionPolicy;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Maps persistence models into refresh snapshots and back into prediction entities.
 */
@Component
public class CustomerOpportunitySnapshotMapper {

    public CustomerOpportunityTenderSnapshot toTenderSnapshot(final Tender tender) {
        PurchaserExtractionPolicy.ExtractionResult extraction =
                PurchaserExtractionPolicy.extractPurchaser(tender.getTitle());
        return new CustomerOpportunityTenderSnapshot(
                tender.getId(),
                tender.getTitle(),
                extraction.found() ? extraction.purchaserName() : "",
                extraction.found() ? extraction.purchaserHash() : "",
                IndustryClassificationPolicy.classifyIndustry(tender.getTitle()),
                tender.getBudget(),
                tender.getCreatedAt());
    }

    public CustomerPrediction mergePrediction(
            final CustomerPrediction existing,
            final CustomerOpportunityRefreshPolicy.RefreshEvaluation evaluation,
            final LocalDateTime computedAt) {
        CustomerPrediction prediction = existing != null
                ? existing
                : CustomerPrediction.builder()
                .purchaserHash(evaluation.purchaserHash())
                .purchaserName(evaluation.purchaserName())
                .build();

        prediction.setPurchaserHash(evaluation.purchaserHash());
        prediction.setPurchaserName(evaluation.purchaserName());
        prediction.setIndustry(evaluation.industry());
        prediction.setOpportunityScore(evaluation.opportunityScore());
        prediction.setPredictedCategory(evaluation.predictedCategory());
        prediction.setPredictedBudgetMin(evaluation.predictedBudgetMin());
        prediction.setPredictedBudgetMax(evaluation.predictedBudgetMax());
        prediction.setPredictedWindow(evaluation.predictedWindow());
        prediction.setConfidence(evaluation.confidence());
        prediction.setReasoningSummary(evaluation.reasoningSummary());
        prediction.setEvidenceRecordIds(evaluation.evidenceRecordIds());
        prediction.setMainCategories(evaluation.mainCategories());
        prediction.setAvgBudget(evaluation.avgBudget());
        prediction.setCycleType(evaluation.cycleType());
        prediction.setFrequency(evaluation.frequency());
        prediction.setPeriodMonths(evaluation.periodMonths());
        prediction.setLastComputedAt(computedAt);
        prediction.setStatus(mapStatus(CustomerOpportunityLifecyclePolicy.resolveRefreshStatus(
                mapStatus(prediction.getStatus()))));
        return prediction;
    }

    private CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus mapStatus(
            final CustomerPrediction.Status status) {
        if (status == null) {
            return null;
        }
        return CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus.valueOf(status.name());
    }

    private CustomerPrediction.Status mapStatus(
            final CustomerOpportunityLifecyclePolicy.PredictionLifecycleStatus status) {
        return CustomerPrediction.Status.valueOf(status.name());
    }
}
