package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerOpportunityStatusCommandServiceTest extends AbstractCustomerOpportunityServiceTest {

    @Test
    void transitionPrediction_ShouldPersistAllowedTransition() {
        CustomerPrediction prediction = prediction(1L, "hash-1", CustomerPrediction.Status.WATCH);
        when(customerPredictionGateway.findById(1L)).thenReturn(Optional.of(prediction));
        when(customerPredictionGateway.save(any(CustomerPrediction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = statusCommandService.transitionPrediction(1L, "RECOMMEND");

        assertThat(result.getOpportunityId()).isEqualTo(1L);
        assertThat(prediction.getStatus()).isEqualTo(CustomerPrediction.Status.RECOMMEND);
        verify(customerPredictionGateway).save(prediction);
    }

    @Test
    void transitionPrediction_ShouldAcceptLowercaseStatusPayload() {
        CustomerPrediction prediction = prediction(1L, "hash-1", CustomerPrediction.Status.WATCH);
        when(customerPredictionGateway.findById(1L)).thenReturn(Optional.of(prediction));
        when(customerPredictionGateway.save(any(CustomerPrediction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = statusCommandService.transitionPrediction(1L, "recommend");

        assertThat(result.getOpportunityId()).isEqualTo(1L);
        assertThat(prediction.getStatus()).isEqualTo(CustomerPrediction.Status.RECOMMEND);
        verify(customerPredictionGateway).save(prediction);
    }

    @Test
    void transitionPrediction_WithDeniedTransition_ShouldFailWithoutPersisting() {
        CustomerPrediction prediction = prediction(1L, "hash-1", CustomerPrediction.Status.WATCH);
        when(customerPredictionGateway.findById(1L)).thenReturn(Optional.of(prediction));

        assertThatThrownBy(() -> statusCommandService.transitionPrediction(1L, "CONVERTED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WATCH");

        verify(customerPredictionGateway, never()).save(any(CustomerPrediction.class));
    }

    @Test
    void convertPrediction_ShouldSetProjectIdAndConvertedStatus() {
        CustomerPrediction prediction = prediction(1L, "hash-1", CustomerPrediction.Status.RECOMMEND);
        when(customerPredictionGateway.findById(1L)).thenReturn(Optional.of(prediction));
        when(customerPredictionGateway.save(any(CustomerPrediction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = statusCommandService.convertPrediction(1L, 99L);

        assertThat(result.getConvertedProjectId()).isEqualTo(99L);
        assertThat(prediction.getStatus()).isEqualTo(CustomerPrediction.Status.CONVERTED);
        assertThat(prediction.getConvertedProjectId()).isEqualTo(99L);
    }
}
