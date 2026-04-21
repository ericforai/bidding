package com.xiyu.bid.marketinsight.mapper;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.marketinsight.core.CustomerOpportunityRefreshPolicy;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerOpportunitySnapshotMapperTest {

    private final CustomerOpportunitySnapshotMapper snapshotMapper =
            new CustomerOpportunitySnapshotMapper();

    @Test
    void toTenderSnapshot_ShouldExtractPurchaserAndIndustry() {
        Tender tender = Tender.builder()
                .id(1L)
                .title("国网江苏省电力办公设备采购项目")
                .budget(new BigDecimal("1000000.00"))
                .createdAt(LocalDateTime.of(2026, 1, 5, 9, 0))
                .build();

        var snapshot = snapshotMapper.toTenderSnapshot(tender);

        assertThat(snapshot.purchaserName()).isEqualTo("国网江苏省电力办公设备");
        assertThat(snapshot.purchaserHash()).isNotBlank();
        assertThat(snapshot.industry()).isEqualTo("办公");
    }

    @Test
    void mergePrediction_ShouldUpdateFieldsAndPreserveExistingStatus() {
        CustomerPrediction existing = CustomerPrediction.builder()
                .id(1L)
                .purchaserHash("hash-1")
                .purchaserName("旧客户")
                .status(CustomerPrediction.Status.RECOMMEND)
                .build();
        CustomerOpportunityRefreshPolicy.RefreshEvaluation evaluation =
                new CustomerOpportunityRefreshPolicy.RefreshEvaluation(
                        "hash-1",
                        "国网江苏省电力",
                        "能源电力",
                        78,
                        "能源电力",
                        new BigDecimal("600000.00"),
                        new BigDecimal("900000.00"),
                        "2026-06",
                        new BigDecimal("0.75"),
                        "reason",
                        "11,12",
                        "能源电力",
                        new BigDecimal("750000.00"),
                        "年度集中采购",
                        2,
                        "1,6");

        CustomerPrediction merged = snapshotMapper.mergePrediction(
                existing,
                evaluation,
                LocalDateTime.of(2026, 4, 21, 10, 0));

        assertThat(merged.getPurchaserName()).isEqualTo("国网江苏省电力");
        assertThat(merged.getOpportunityScore()).isEqualTo(78);
        assertThat(merged.getLastComputedAt()).isEqualTo(LocalDateTime.of(2026, 4, 21, 10, 0));
        assertThat(merged.getStatus()).isEqualTo(CustomerPrediction.Status.RECOMMEND);
    }
}
