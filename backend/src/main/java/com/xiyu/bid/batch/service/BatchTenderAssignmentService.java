package com.xiyu.bid.batch.service;

import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.batch.dto.BatchTenderAssignRequest;
import com.xiyu.bid.batch.dto.BatchTenderStatusUpdateRequest;
import com.xiyu.bid.batch.dto.TenderAssignmentCandidateResponse;
import com.xiyu.bid.batch.dto.TenderAssignmentResponse;
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
public class BatchTenderAssignmentService {

    private final TenderRepository tenderRepository;
    private final UserRepository userRepository;
    private final TenderAssignmentRecordRepository tenderAssignmentRecordRepository;
    private final BatchOperationLogService batchOperationLogService;

    @Transactional
    public BatchOperationResponse batchUpdateStatus(BatchTenderStatusUpdateRequest request, User currentUser) {
        Tender.Status status = Tender.Status.valueOf(request.getStatus().trim().toUpperCase());
        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("TENDER_STATUS_UPDATE")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(request.getTenderIds().size());

        for (Long tenderId : request.getTenderIds()) {
            tenderRepository.findById(tenderId).ifPresentOrElse((tender) -> {
                tender.setStatus(status);
                tenderRepository.save(tender);
                response.addSuccess(tenderId);
            }, () -> response.addError(tenderId, "Tender not found", "NOT_FOUND"));
        }
        response.setSuccess(response.getFailureCount() == 0);
        batchOperationLogService.record(response, "TENDER", "STATUS_UPDATE", currentUser == null ? null : currentUser.getId());
        return response;
    }

    @Transactional
    public BatchOperationResponse batchAssign(BatchTenderAssignRequest request, User currentUser) {
        User assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new IllegalArgumentException("Assignee not found: " + request.getAssigneeId()));

        BatchOperationResponse response = BatchOperationResponse.builder()
                .operationType("TENDER_ASSIGN")
                .operationTime(LocalDateTime.now())
                .build();
        response.setTotalCount(request.getTenderIds().size());

        List<TenderAssignmentRecord> records = new ArrayList<>();
        for (Long tenderId : request.getTenderIds()) {
            tenderRepository.findById(tenderId).ifPresentOrElse((tender) -> {
                tender.setStatus(Tender.Status.TRACKING);
                tenderRepository.save(tender);
                records.add(TenderAssignmentRecord.builder()
                        .tenderId(tenderId)
                        .assigneeId(assignee.getId())
                        .assigneeName(assignee.getFullName())
                        .assignedById(currentUser == null ? null : currentUser.getId())
                        .assignedByName(currentUser == null ? "system" : currentUser.getFullName())
                        .remark(request.getRemark())
                        .build());
                response.addSuccess(tenderId);
            }, () -> response.addError(tenderId, "Tender not found", "NOT_FOUND"));
        }
        if (!records.isEmpty()) {
            tenderAssignmentRecordRepository.saveAll(records);
        }
        response.setSuccess(response.getFailureCount() == 0);
        batchOperationLogService.record(response, "TENDER", "ASSIGN", currentUser == null ? null : currentUser.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public TenderAssignmentResponse getAssignment(Long tenderId) {
        List<TenderAssignmentRecord> records = tenderAssignmentRecordRepository.findByTenderIdOrderByAssignedAtDesc(tenderId);
        List<TenderAssignmentResponse.AssignmentRecord> history = records.stream().map(this::toRecord).toList();
        return TenderAssignmentResponse.builder()
                .latest(history.isEmpty() ? null : history.get(0))
                .history(history)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TenderAssignmentCandidateResponse> getCandidates() {
        return userRepository.findByEnabledTrue().stream()
                .map(user -> TenderAssignmentCandidateResponse.builder()
                        .id(user.getId())
                        .name(user.getFullName())
                        .departmentName(user.getDepartmentName())
                        .roleCode(user.getRoleCode())
                        .build())
                .toList();
    }

    private TenderAssignmentResponse.AssignmentRecord toRecord(TenderAssignmentRecord entity) {
        return TenderAssignmentResponse.AssignmentRecord.builder()
                .id(entity.getId())
                .tenderId(entity.getTenderId())
                .assigneeId(entity.getAssigneeId())
                .assigneeName(entity.getAssigneeName())
                .assignedById(entity.getAssignedById())
                .assignedByName(entity.getAssignedByName())
                .remark(entity.getRemark())
                .assignedAt(entity.getAssignedAt())
                .build();
    }
}
