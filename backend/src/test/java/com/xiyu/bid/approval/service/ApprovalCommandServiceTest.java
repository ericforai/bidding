package com.xiyu.bid.approval.service;

import com.xiyu.bid.approval.core.ApprovalDecisionPolicy;
import com.xiyu.bid.approval.core.ApprovalPermissionPolicy;
import com.xiyu.bid.approval.dto.ApprovalDetailDTO;
import com.xiyu.bid.approval.dto.ApprovalSubmitRequest;
import com.xiyu.bid.approval.entity.ApprovalRequest;
import com.xiyu.bid.approval.enums.ApprovalStatus;
import com.xiyu.bid.approval.repository.ApprovalRequestRepository;
import com.xiyu.bid.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalCommandServiceTest {

    @Mock
    private ApprovalRequestRepository requestRepository;

    @Mock
    private ApprovalActionRecorder actionRecorder;

    @Mock
    private ApprovalDetailAssembler detailAssembler;

    @InjectMocks
    private ApprovalCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new ApprovalCommandService(
                requestRepository,
                new ApprovalDecisionPolicy(),
                new ApprovalPermissionPolicy(),
                actionRecorder,
                detailAssembler
        );
    }

    @Test
    void submitForApproval_RejectsDuplicatePendingRequest() {
        when(requestRepository.findByProjectIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(
                ApprovalRequest.builder().status(ApprovalStatus.PENDING).build()
        ));

        ApprovalSubmitRequest request = ApprovalSubmitRequest.builder()
                .projectId(100L)
                .approvalType("PROJECT")
                .title("Need approval")
                .build();

        assertThatThrownBy(() -> commandService.submitForApproval(request, 1L, "tester"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有待审批");

        verify(requestRepository, never()).save(any());
    }

    @Test
    void submitForApproval_PersistsRequestAndRecordsSubmitAction() {
        when(requestRepository.findByProjectIdOrderByCreatedAtDesc(100L)).thenReturn(List.of());
        when(requestRepository.save(any())).thenAnswer(invocation -> {
            ApprovalRequest request = invocation.getArgument(0);
            request.setId(UUID.randomUUID());
            return request;
        });
        when(detailAssembler.toDetailDTO(any())).thenReturn(ApprovalDetailDTO.builder().title("ok").build());

        ApprovalSubmitRequest request = ApprovalSubmitRequest.builder()
                .projectId(100L)
                .projectName("P")
                .approvalType("PROJECT")
                .title("Need approval")
                .description("desc")
                .attachmentIds(List.of(1L, 2L))
                .build();

        ApprovalDetailDTO result = commandService.submitForApproval(request, 9L, "tester");

        ArgumentCaptor<ApprovalRequest> captor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(requestRepository).save(captor.capture());
        assertThat(captor.getValue().getAttachmentIds()).isEqualTo("1,2");
        verify(actionRecorder).record(any(), any(), any(), any(), any(), any(), any());
        assertThat(result.getTitle()).isEqualTo("ok");
    }

    @Test
    void approve_RejectsNonApprover() {
        ApprovalRequest request = ApprovalRequest.builder()
                .id(UUID.randomUUID())
                .status(ApprovalStatus.PENDING)
                .currentApproverId(20L)
                .submittedAt(LocalDateTime.now())
                .build();
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> commandService.approve(request.getId(), 30L, "other", "ok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有权限");
    }
}
