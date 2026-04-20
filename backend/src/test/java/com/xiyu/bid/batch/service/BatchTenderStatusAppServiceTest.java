package com.xiyu.bid.batch.service;

import com.xiyu.bid.audit.service.IAuditLogService;
import com.xiyu.bid.batch.core.BatchValidationPolicy;
import com.xiyu.bid.batch.core.TenderStatusTransitionPolicy;
import com.xiyu.bid.batch.dto.BatchTenderStatusUpdateRequest;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchTenderStatusAppServiceTest {

    private TenderRepository tenderRepository;
    private BatchTenderStatusAppService service;

    @BeforeEach
    void setUp() {
        tenderRepository = mock(TenderRepository.class);
        IAuditLogService auditLogService = mock(IAuditLogService.class);
        service = new BatchTenderStatusAppService(
                tenderRepository,
                new BatchOperationLogService(auditLogService),
                new BatchValidationPolicy(),
                new TenderStatusTransitionPolicy()
        );
    }

    @Test
    void shouldReturnPartialFailureWhenTransitionIsInvalid() {
        Tender pending = Tender.builder().id(1L).status(Tender.Status.PENDING).build();
        Tender bidded = Tender.builder().id(2L).status(Tender.Status.BIDDED).build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(tenderRepository.findById(2L)).thenReturn(Optional.of(bidded));
        when(tenderRepository.saveAll(anyList())).thenReturn(List.of(pending));

        BatchTenderStatusUpdateRequest request = new BatchTenderStatusUpdateRequest();
        request.setTenderIds(List.of(1L, 2L));
        request.setStatus("TRACKING");

        var response = service.batchUpdateStatus(request, null);

        assertFalse(response.getSuccess());
        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        assertEquals(Tender.Status.TRACKING, pending.getStatus());
        assertEquals("INVALID_STATUS_TRANSITION", response.getErrors().get(0).getErrorCode());
    }

    @Test
    void shouldTreatSameStatusAsIdempotentSuccess() {
        Tender tracking = Tender.builder().id(1L).status(Tender.Status.TRACKING).build();
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(tracking));

        BatchTenderStatusUpdateRequest request = new BatchTenderStatusUpdateRequest();
        request.setTenderIds(List.of(1L));
        request.setStatus("TRACKING");

        var response = service.batchUpdateStatus(request, null);

        assertTrue(response.getSuccess());
        assertEquals(1, response.getSuccessCount());
        verify(tenderRepository, never()).saveAll(anyList());
    }
}
