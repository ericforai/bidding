package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.marketinsight.core.CustomerOpportunityTenderSnapshot;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import com.xiyu.bid.marketinsight.gateway.CustomerPredictionGateway;
import com.xiyu.bid.marketinsight.mapper.CustomerOpportunitySnapshotMapper;
import com.xiyu.bid.marketinsight.support.CustomerOpportunityTenderSupport;
import com.xiyu.bid.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
abstract class AbstractCustomerOpportunityServiceTest {

    @Mock
    protected TenderRepository tenderRepository;

    @Mock
    protected CustomerPredictionGateway customerPredictionGateway;

    @Mock
    protected CustomerOpportunityTenderSupport tenderSupport;

    @Mock
    protected CustomerOpportunitySnapshotMapper snapshotMapper;

    protected CustomerOpportunityRefreshService refreshService;
    protected CustomerOpportunityStatusCommandService statusCommandService;

    @BeforeEach
    void setUpCustomerOpportunityServices() {
        refreshService = new CustomerOpportunityRefreshService(
                tenderRepository,
                customerPredictionGateway,
                tenderSupport,
                snapshotMapper);
        statusCommandService = new CustomerOpportunityStatusCommandService(customerPredictionGateway);
    }

    protected CustomerPrediction prediction(
            final Long id,
            final String purchaserHash,
            final CustomerPrediction.Status status) {
        return CustomerPrediction.builder()
                .id(id)
                .purchaserHash(purchaserHash)
                .purchaserName("国网江苏省电力")
                .status(status)
                .industry("能源电力")
                .avgBudget(new BigDecimal("800000.00"))
                .predictedWindow("2026-06")
                .build();
    }

    protected CustomerOpportunityTenderSnapshot snapshot(
            final Long id,
            final String purchaserHash,
            final int month) {
        return new CustomerOpportunityTenderSnapshot(
                id,
                "国网江苏省电力办公设备采购项目",
                "国网江苏省电力",
                purchaserHash,
                "能源电力",
                new BigDecimal("800000.00"),
                LocalDateTime.of(2026, month, 10, 9, 0));
    }
}
