package com.xiyu.bid.marketinsight.gateway;

import com.xiyu.bid.marketinsight.entity.CustomerPrediction;

import java.util.List;
import java.util.Optional;

/**
 * Persistence gateway for customer prediction aggregates.
 */
public interface CustomerPredictionGateway {

    List<CustomerPrediction> findAll();

    List<CustomerPrediction> findAllByOpportunityScoreDesc();

    List<CustomerPrediction> findByPurchaserHash(String purchaserHash);

    Optional<CustomerPrediction> findById(Long id);

    Optional<CustomerPrediction> findFirstByPurchaserHash(String purchaserHash);

    CustomerPrediction save(CustomerPrediction prediction);

    List<CustomerPrediction> saveAll(List<CustomerPrediction> predictions);

    void delete(CustomerPrediction prediction);
}
