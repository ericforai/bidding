package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerOpportunityRefreshServiceTest extends AbstractCustomerOpportunityServiceTest {

    @Test
    void refreshInsights_ShouldSaveMergedPredictionsAndDeleteMissingNonConvertedOnes() {
        Tender tender = Tender.builder().id(11L).title("国网江苏省电力办公设备采购项目").build();
        CustomerPrediction existing = prediction(1L, "hash-1", CustomerPrediction.Status.WATCH);
        CustomerPrediction staleWatch = prediction(2L, "hash-stale", CustomerPrediction.Status.WATCH);
        CustomerPrediction staleConverted = prediction(3L, "hash-converted", CustomerPrediction.Status.CONVERTED);
        CustomerPrediction merged = prediction(1L, "hash-1", CustomerPrediction.Status.WATCH);

        when(tenderRepository.findAll()).thenReturn(List.of(tender));
        when(tenderSupport.createSnapshots(List.of(tender))).thenReturn(List.of(snapshot(11L, "hash-1", 1)));
        when(tenderSupport.groupByPurchaserHash(any())).thenReturn(Map.of("hash-1", List.of(snapshot(11L, "hash-1", 1))));
        when(customerPredictionGateway.findAll()).thenReturn(List.of(existing, staleWatch, staleConverted));
        when(snapshotMapper.mergePrediction(eq(existing), any(), any())).thenReturn(merged);

        refreshService.refreshInsights();

        verify(customerPredictionGateway).saveAll(List.of(merged));
        verify(customerPredictionGateway).delete(staleWatch);
        verify(customerPredictionGateway, never()).delete(staleConverted);
    }
}
