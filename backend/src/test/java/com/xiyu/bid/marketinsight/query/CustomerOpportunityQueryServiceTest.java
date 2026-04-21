package com.xiyu.bid.marketinsight.query;

import com.xiyu.bid.marketinsight.dto.CustomerInsightDTO;
import com.xiyu.bid.marketinsight.dto.request.CustomerInsightQuery;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import com.xiyu.bid.marketinsight.repository.CustomerPredictionRepository;
import com.xiyu.bid.repository.TenderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerOpportunityQueryServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private CustomerPredictionRepository customerPredictionRepository;

    @InjectMocks
    private CustomerOpportunityQueryService queryService;

    @Test
    void getCustomerInsights_shouldFilterByStatusAndSortByScoreDescending() {
        CustomerPrediction recommend = prediction(10L, "华东智造", CustomerPrediction.Status.RECOMMEND, 92, "A 省", "制造", "李经理");
        CustomerPrediction watch = prediction(11L, "华南能源", CustomerPrediction.Status.WATCH, 60, "B 省", "能源", "王经理");
        when(customerPredictionRepository.findByStatus(CustomerPrediction.Status.RECOMMEND)).thenReturn(List.of(watch, recommend));

        CustomerInsightQuery query = new CustomerInsightQuery();
        query.setStatus("recommend");
        query.setKeyword("华东");
        query.setRegion("A 省");
        query.setIndustry("制造");
        query.setSalesRep("李经理");

        List<CustomerInsightDTO> result = queryService.getCustomerInsights(query);

        verify(customerPredictionRepository).findByStatus(CustomerPrediction.Status.RECOMMEND);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("华东智造");
        assertThat(result.get(0).getOpportunityScore()).isEqualTo(92);
        assertThat(result.get(0).getStatus()).isEqualTo("recommend");
    }

    private CustomerPrediction prediction(Long id, String name, CustomerPrediction.Status status, int score, String region, String industry, String salesRep) {
        return CustomerPrediction.builder()
                .id(id)
                .purchaserHash("HASH-" + id)
                .purchaserName(name)
                .region(region)
                .industry(industry)
                .salesRep(salesRep)
                .opportunityScore(score)
                .status(status)
                .build();
    }
}
