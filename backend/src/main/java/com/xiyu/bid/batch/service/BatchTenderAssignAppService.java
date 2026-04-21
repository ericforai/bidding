package com.xiyu.bid.batch.service;

import com.xiyu.bid.batch.core.TenderStatusTransitionPolicy;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.batch.dto.BatchTenderAssignRequest;
import com.xiyu.bid.batch.entity.TenderAssignmentRecord;
import com.xiyu.bid.batch.repository.TenderAssignmentRecordRepository;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchTenderAssignAppService {

    private final TenderRepository tenderRepository;
    private final UserRepository userRepository;
    private final TenderAssignmentRecordRepository tenderAssignmentRecordRepository;
    private final BatchOperationLogService batchOperationLogService;
    private final TenderStatusTransitionPolicy transitionPolicy;

    @Transactional
    public BatchOperationResponse batchAssign(BatchTenderAssignRequest request, User currentUser) {
        validateRequest(request);

        User assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new IllegalArgumentException("Assignee not found: " + request.getAssigneeId()));

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("TENDER_ASSIGN")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(request.getTenderIds().size());

        List<Tender> changedTenders = new ArrayList<>();
        List<TenderAssignmentRecord> records = new ArrayList<>();
        for (Long tenderId : request.getTenderIds()) {
            tenderRepository.findById(tenderId).ifPresentOrElse(
                    tender -> collectAssignment(tender, assignee, request, currentUser, changedTenders, records, response),
                    () -> response.addError(tenderId, "Tender not found", "NOT_FOUND")
            );
        }

        if (!changedTenders.isEmpty()) {
            tenderRepository.saveAll(changedTenders);
        }
        if (!records.isEmpty()) {
            tenderAssignmentRecordRepository.saveAll(records);
        }
        response.setSuccess(response.getFailureCount() == 0);
        batchOperationLogService.record(response, "TENDER", "ASSIGN", currentUser == null ? null : currentUser.getId());
        return response;
    }

    private void collectAssignment(
            Tender tender,
            User assignee,
            BatchTenderAssignRequest request,
            User currentUser,
            List<Tender> changedTenders,
            List<TenderAssignmentRecord> records,
            BatchOperationResponse response
    ) {
        try {
            transitionPolicy.assertTransition(tender.getStatus(), Tender.Status.TRACKING);
            if (tender.getStatus() != Tender.Status.TRACKING) {
                tender.setStatus(Tender.Status.TRACKING);
                changedTenders.add(tender);
            }
            records.add(buildRecord(tender.getId(), assignee, request.getRemark(), currentUser));
            response.addSuccess(tender.getId());
        } catch (IllegalArgumentException exception) {
            response.addError(tender.getId(), exception.getMessage(), "INVALID_STATUS_TRANSITION");
        }
    }

    private TenderAssignmentRecord buildRecord(Long tenderId, User assignee, String remark, User currentUser) {
        return TenderAssignmentRecord.builder()
                .tenderId(tenderId)
                .assigneeId(assignee.getId())
                .assigneeName(assignee.getFullName())
                .assignedById(currentUser == null ? null : currentUser.getId())
                .assignedByName(currentUser == null ? "system" : currentUser.getFullName())
                .remark(remark)
                .build();
    }

    private void validateRequest(BatchTenderAssignRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch tender assign request cannot be null");
        }
        List<Long> tenderIds = request.getTenderIds();
        if (tenderIds == null || tenderIds.isEmpty()) {
            throw new IllegalArgumentException("Tender IDs cannot be null or empty");
        }
        if (tenderIds.size() > 100) {
            throw new IllegalArgumentException("Batch size cannot exceed 100 items");
        }
        if (request.getAssigneeId() == null || request.getAssigneeId() <= 0) {
            throw new IllegalArgumentException("Assignee ID must be a positive number");
        }
    }
}
