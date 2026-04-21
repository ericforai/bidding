package com.xiyu.bid.marketinsight.gateway;

import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import com.xiyu.bid.marketinsight.repository.CustomerPredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data adapter for customer prediction persistence.
 */
@Component
@RequiredArgsConstructor
public class CustomerPredictionRepositoryGateway implements CustomerPredictionGateway {

    private final CustomerPredictionRepository customerPredictionRepository;

    @Override
    public List<CustomerPrediction> findAll() {
        return customerPredictionRepository.findAll();
    }

    @Override
    public List<CustomerPrediction> findAllByOpportunityScoreDesc() {
        return customerPredictionRepository.findAllByOrderByOpportunityScoreDesc();
    }

    @Override
    public List<CustomerPrediction> findByPurchaserHash(final String purchaserHash) {
        return customerPredictionRepository.findByPurchaserHash(purchaserHash);
    }

    @Override
    public Optional<CustomerPrediction> findById(final Long id) {
        return customerPredictionRepository.findById(id);
    }

    @Override
    public Optional<CustomerPrediction> findFirstByPurchaserHash(final String purchaserHash) {
        return customerPredictionRepository.findByPurchaserHash(purchaserHash).stream().findFirst();
    }

    @Override
    public CustomerPrediction save(final CustomerPrediction prediction) {
        return customerPredictionRepository.save(prediction);
    }

    @Override
    public List<CustomerPrediction> saveAll(final List<CustomerPrediction> predictions) {
        return customerPredictionRepository.saveAll(predictions);
    }

    @Override
    public void delete(final CustomerPrediction prediction) {
        customerPredictionRepository.delete(prediction);
    }
}
