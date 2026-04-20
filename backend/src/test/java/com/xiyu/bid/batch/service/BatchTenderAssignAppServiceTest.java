package com.xiyu.bid.batch.service;

import com.xiyu.bid.audit.service.IAuditLogService;
import com.xiyu.bid.batch.core.TenderStatusTransitionPolicy;
import com.xiyu.bid.batch.dto.BatchTenderAssignRequest;
import com.xiyu.bid.batch.entity.TenderAssignmentRecord;
import com.xiyu.bid.batch.repository.TenderAssignmentRecordRepository;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchTenderAssignAppServiceTest {

    private TenderRepository tenderRepository;
    private UserRepository userRepository;
    private TenderAssignmentRecordRepository assignmentRecordRepository;
    private BatchTenderAssignAppService service;

    @BeforeEach
    void setUp() {
        tenderRepository = mock(TenderRepository.class);
        userRepository = mock(UserRepository.class);
        assignmentRecordRepository = mock(TenderAssignmentRecordRepository.class);
        IAuditLogService auditLogService = mock(IAuditLogService.class);
        service = new BatchTenderAssignAppService(
                tenderRepository,
                userRepository,
                assignmentRecordRepository,
                new BatchOperationLogService(auditLogService),
                new TenderStatusTransitionPolicy()
        );
    }

    @Test
    void shouldAssignTrackingTendersAndPersistAssignmentRecords() {
        User assignee = User.builder().id(9L).fullName("销售甲").build();
        User currentUser = User.builder().id(1L).fullName("经理乙").build();
        Tender pending = Tender.builder().id(1L).status(Tender.Status.PENDING).build();
        Tender tracking = Tender.builder().id(2L).status(Tender.Status.TRACKING).build();

        when(userRepository.findById(9L)).thenReturn(Optional.of(assignee));
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(tenderRepository.findById(2L)).thenReturn(Optional.of(tracking));

        BatchTenderAssignRequest request = new BatchTenderAssignRequest();
        request.setTenderIds(List.of(1L, 2L));
        request.setAssigneeId(9L);
        request.setRemark("follow-up");

        var response = service.batchAssign(request, currentUser);

        assertTrue(response.getSuccess());
        assertEquals(2, response.getSuccessCount());
        assertEquals(Tender.Status.TRACKING, pending.getStatus());
        verify(assignmentRecordRepository).saveAll(anyList());
    }

    @Test
    void shouldRejectAssigningBiddedTenderBackToTracking() {
        User assignee = User.builder().id(9L).fullName("销售甲").build();
        Tender bidded = Tender.builder().id(1L).status(Tender.Status.BIDDED).build();

        when(userRepository.findById(9L)).thenReturn(Optional.of(assignee));
        when(tenderRepository.findById(1L)).thenReturn(Optional.of(bidded));

        BatchTenderAssignRequest request = new BatchTenderAssignRequest();
        request.setTenderIds(List.of(1L));
        request.setAssigneeId(9L);

        var response = service.batchAssign(request, null);

        assertFalse(response.getSuccess());
        assertEquals(1, response.getFailureCount());
        assertEquals("INVALID_STATUS_TRANSITION", response.getErrors().get(0).getErrorCode());
    }
}
