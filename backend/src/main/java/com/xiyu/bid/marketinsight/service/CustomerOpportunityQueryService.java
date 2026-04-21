package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.marketinsight.dto.CustomerInsightDTO;
import com.xiyu.bid.marketinsight.dto.CustomerOpportunityAssembler;
import com.xiyu.bid.marketinsight.dto.CustomerPredictionDTO;
import com.xiyu.bid.marketinsight.dto.CustomerPurchaseDTO;
import com.xiyu.bid.marketinsight.gateway.CustomerPredictionGateway;
import com.xiyu.bid.marketinsight.support.CustomerOpportunityTenderSupport;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query application service for customer opportunity read models.
 */
@Service
@RequiredArgsConstructor
public class CustomerOpportunityQueryService {

    private final TenderRepository tenderRepository;
    private final CustomerPredictionGateway customerPredictionGateway;
    private final CustomerOpportunityTenderSupport tenderSupport;

    @Transactional(readOnly = true)
    public List<CustomerInsightDTO> getCustomerInsights() {
        return customerPredictionGateway.findAllByOpportunityScoreDesc()
                .stream()
                .map(CustomerOpportunityAssembler::toInsightDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerPurchaseDTO> getCustomerPurchases(final String purchaserHash) {
        return tenderSupport.createSnapshots(tenderRepository.findAll()).stream()
                .filter(snapshot -> purchaserHash.equals(snapshot.purchaserHash()))
                .map(snapshot -> CustomerOpportunityAssembler.toPurchaseDTO(
                        snapshot.tenderId(),
                        snapshot.purchaserHash(),
                        snapshot.createdAt(),
                        snapshot.tenderTitle(),
                        snapshot.industry(),
                        snapshot.budget()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerPredictionDTO> getCustomerPredictions(final String purchaserHash) {
        return customerPredictionGateway.findByPurchaserHash(purchaserHash)
                .stream()
                .map(CustomerOpportunityAssembler::toDTO)
                .collect(Collectors.toList());
    }
}
