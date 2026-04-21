package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import com.xiyu.bid.marketinsight.gateway.CustomerPredictionGateway;
import com.xiyu.bid.marketinsight.support.CustomerOpportunityTenderSupport;
import com.xiyu.bid.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerOpportunityQueryServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private CustomerPredictionGateway customerPredictionGateway;

    @Mock
    private CustomerOpportunityTenderSupport tenderSupport;

    private CustomerOpportunityQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new CustomerOpportunityQueryService(
                tenderRepository,
                customerPredictionGateway,
                tenderSupport);
    }

    @Test
    void getCustomerPurchases_ShouldMapSnapshotsWithoutRebuildingTenderEntity() {
        Tender sourceTender = Tender.builder().id(11L).title("国网江苏办公设备采购").build();
        when(tenderRepository.findAll()).thenReturn(List.of(sourceTender));
        when(tenderSupport.createSnapshots(List.of(sourceTender))).thenReturn(List.of(
                new com.xiyu.bid.marketinsight.core.CustomerOpportunityTenderSnapshot(
                        11L,
                        "国网江苏办公设备采购",
                        "国网江苏省电力",
                        "hash-1",
                        "能源电力",
                        new BigDecimal("1280000"),
                        java.time.LocalDateTime.of(2026, 4, 10, 9, 0))));

        var purchases = queryService.getCustomerPurchases("hash-1");

        assertThat(purchases).singleElement().satisfies(item -> {
            assertThat(item.getRecordId()).isEqualTo(11L);
            assertThat(item.getCustomerId()).isEqualTo("hash-1");
            assertThat(item.getPublishDate()).isEqualTo("2026-04-10");
            assertThat(item.getTitle()).isEqualTo("国网江苏办公设备采购");
            assertThat(item.getCategory()).isEqualTo("能源电力");
            assertThat(item.getBudget()).isEqualTo(128L);
            assertThat(item.isKey()).isTrue();
        });
    }

    @Test
    void getCustomerPredictions_ShouldPreserveExistingDtoContract() {
        CustomerPrediction prediction = CustomerPrediction.builder()
                .id(9L)
                .purchaserHash("hash-9")
                .purchaserName("华东制造中心")
                .predictedCategory("机加")
                .build();
        when(customerPredictionGateway.findByPurchaserHash("hash-9")).thenReturn(List.of(prediction));

        var predictions = queryService.getCustomerPredictions("hash-9");

        assertThat(predictions).singleElement().satisfies(item -> {
            assertThat(item.getOpportunityId()).isEqualTo(9L);
            assertThat(item.getCustomerId()).isEqualTo("hash-9");
            assertThat(item.getPredictedCategory()).isEqualTo("机加");
        });
    }
}
