package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.marketinsight.core.CustomerOpportunityRefreshPolicy;
import com.xiyu.bid.marketinsight.core.CustomerOpportunityTenderSnapshot;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import com.xiyu.bid.marketinsight.gateway.CustomerPredictionGateway;
import com.xiyu.bid.marketinsight.mapper.CustomerOpportunitySnapshotMapper;
import com.xiyu.bid.marketinsight.support.CustomerOpportunityTenderSupport;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Command application service that recomputes customer opportunity projections.
 */
@Service
@RequiredArgsConstructor
public class CustomerOpportunityRefreshService {

    private final TenderRepository tenderRepository;
    private final CustomerPredictionGateway customerPredictionGateway;
    private final CustomerOpportunityTenderSupport tenderSupport;
    private final CustomerOpportunitySnapshotMapper snapshotMapper;

    @Transactional
    public void refreshInsights() {
        List<CustomerOpportunityTenderSnapshot> snapshots =
                tenderSupport.createSnapshots(tenderRepository.findAll());
        Map<String, List<CustomerOpportunityTenderSnapshot>> grouped =
                tenderSupport.groupByPurchaserHash(snapshots);
        Map<String, CustomerPrediction> existingByPurchaserHash =
                customerPredictionGateway.findAll().stream()
                        .collect(Collectors.toMap(
                                CustomerPrediction::getPurchaserHash,
                                Function.identity(),
                                (left, right) -> left));

        LocalDateTime referenceTime = LocalDateTime.now();
        List<CustomerPrediction> refreshedPredictions = grouped.entrySet().stream()
                .map(entry -> snapshotMapper.mergePrediction(
                        existingByPurchaserHash.get(entry.getKey()),
                        CustomerOpportunityRefreshPolicy.evaluate(
                                entry.getKey(),
                                entry.getValue(),
                                referenceTime),
                        referenceTime))
                .collect(Collectors.toList());

        customerPredictionGateway.saveAll(refreshedPredictions);

        customerPredictionGateway.findAll().stream()
                .filter(prediction -> prediction.getStatus() != CustomerPrediction.Status.CONVERTED)
                .filter(prediction -> !grouped.containsKey(prediction.getPurchaserHash()))
                .forEach(customerPredictionGateway::delete);
    }
}
