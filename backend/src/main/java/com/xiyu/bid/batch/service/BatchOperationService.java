// Input: batch repositories, DTOs, and support services
// Output: Batch Operation business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.batch.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.batch.dto.BatchApproveFeesRequest;
import com.xiyu.bid.batch.dto.BatchAssignRequest;
import com.xiyu.bid.batch.dto.BatchDeleteRequest;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import com.xiyu.bid.batch.dto.BatchProjectUpdateRequest;
import com.xiyu.bid.audit.service.IAuditLogService;
import com.xiyu.bid.fees.repository.FeeRepository;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Batch operation service with improved security and transaction isolation.
 *
 * SECURITY: All batch operations require proper authorization checks.
 * TRANSACTION: Uses READ_COMMITTED isolation level to prevent dirty reads
 * while allowing reasonable concurrency.
 */
@Service
@Slf4j
public class BatchOperationService {

    private final BatchTenderClaimExecutor batchTenderClaimExecutor;
    private final BatchTaskAssignExecutor batchTaskAssignExecutor;
    private final BatchProjectDeleteExecutor batchProjectDeleteExecutor;
    private final BatchProjectUpdateExecutor batchProjectUpdateExecutor;
    private final BatchFeeApproveExecutor batchFeeApproveExecutor;
    private final BatchItemDeleteExecutor batchItemDeleteExecutor;

    @Autowired
    public BatchOperationService(
            BatchTenderClaimExecutor batchTenderClaimExecutor,
            BatchTaskAssignExecutor batchTaskAssignExecutor,
            BatchProjectDeleteExecutor batchProjectDeleteExecutor,
            BatchProjectUpdateExecutor batchProjectUpdateExecutor,
            BatchFeeApproveExecutor batchFeeApproveExecutor,
            BatchItemDeleteExecutor batchItemDeleteExecutor) {
        this.batchTenderClaimExecutor = batchTenderClaimExecutor;
        this.batchTaskAssignExecutor = batchTaskAssignExecutor;
        this.batchProjectDeleteExecutor = batchProjectDeleteExecutor;
        this.batchProjectUpdateExecutor = batchProjectUpdateExecutor;
        this.batchFeeApproveExecutor = batchFeeApproveExecutor;
        this.batchItemDeleteExecutor = batchItemDeleteExecutor;
    }

    // Test constructor for legacy unit tests before executor-level extraction migration.
    BatchOperationService(
            TenderRepository tenderRepository,
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            IAuditLogService auditLogService,
            FeeRepository feeRepository,
            ProjectAccessScopeService projectAccessScopeService) {
        BatchOperationLogService batchOperationLogService = new BatchOperationLogService(auditLogService);
        this.batchTenderClaimExecutor = new BatchTenderClaimExecutor(tenderRepository, batchOperationLogService);
        this.batchTaskAssignExecutor = new BatchTaskAssignExecutor(
                taskRepository,
                new BatchAssignmentPolicy(userRepository, projectAccessScopeService),
                batchOperationLogService
        );
        this.batchProjectDeleteExecutor = new BatchProjectDeleteExecutor(projectRepository, batchOperationLogService);
        this.batchProjectUpdateExecutor = new BatchProjectUpdateExecutor(projectRepository, batchOperationLogService);
        this.batchFeeApproveExecutor = new BatchFeeApproveExecutor(feeRepository, batchOperationLogService);
        this.batchItemDeleteExecutor = new BatchItemDeleteExecutor(
                tenderRepository,
                taskRepository,
                batchProjectDeleteExecutor,
                batchOperationLogService
        );
    }

    @Auditable(action = "BATCH_CLAIM", entityType = "TENDER", description = "Batch claim tenders")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAuthority('CLAIM_TENDER')")
    public BatchOperationResponse batchClaimTenders(List<Long> tenderIds, Long userId) {
        log.info("Batch claiming tenders: count={}, userId={}", tenderIds == null ? null : tenderIds.size(), userId);
        return batchTenderClaimExecutor.execute(tenderIds, userId);
    }

    @Auditable(action = "BATCH_ASSIGN", entityType = "TASK", description = "Batch assign tasks")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAuthority('ASSIGN_TASK')")
    public BatchOperationResponse batchAssignTasks(List<Long> taskIds, Long assigneeId) {
        log.info("Batch assigning tasks: count={}, assigneeId={}", taskIds == null ? null : taskIds.size(), assigneeId);
        return batchTaskAssignExecutor.assign(taskIds, assigneeId);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAuthority('ASSIGN_TASK')")
    public BatchOperationResponse batchAssignTasks(BatchAssignRequest request, User currentUser) {
        return batchTaskAssignExecutor.assign(request, currentUser);
    }

    @Auditable(action = "BATCH_DELETE", entityType = "PROJECT", description = "Batch delete projects")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAuthority('DELETE_PROJECT')")
    public BatchOperationResponse batchDeleteProjects(List<Long> projectIds, Long userId, User.Role userRole) {
        log.info("Batch deleting projects: count={}, userId={}", projectIds == null ? null : projectIds.size(), userId);
        return batchProjectDeleteExecutor.execute(projectIds, userId, userRole);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAuthority('DELETE_PROJECT')")
    public BatchOperationResponse batchDeleteProjects(BatchDeleteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch delete request cannot be null");
        }
        throw new IllegalStateException("Use the authenticated batchDeleteProjects overload with the current user context");
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public BatchOperationResponse batchDeleteItems(String itemType, List<Long> ids, Long userId, User.Role userRole) {
        log.info("Batch deleting items: type={}, count={}", itemType, ids == null ? null : ids.size());
        return batchItemDeleteExecutor.deleteItems(itemType, ids, userId, userRole);
    }

    @Auditable(action = "BATCH_UPDATE", entityType = "PROJECT", description = "Batch update projects")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAuthority('UPDATE_PROJECT')")
    public BatchOperationResponse batchUpdateProjects(BatchProjectUpdateRequest request, Long userId, User.Role userRole) {
        log.info("Batch updating projects: count={}, userId={}, status={}, managerId={}",
                request == null || request.getProjectIds() == null ? null : request.getProjectIds().size(),
                userId,
                request == null ? null : request.getStatus(),
                request == null ? null : request.getManagerId());
        return batchProjectUpdateExecutor.execute(request, userId, userRole);
    }

    @Auditable(action = "BATCH_PAY", entityType = "FEE", description = "Batch mark fees as paid")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAuthority('PAY_FEE')")
    public BatchOperationResponse batchApproveFees(BatchApproveFeesRequest request, Long userId) {
        log.info("Batch approving fees: count={}, userId={}, paidBy={}",
                request == null || request.getFeeIds() == null ? null : request.getFeeIds().size(),
                userId,
                request == null ? null : request.getPaidBy());
        return batchFeeApproveExecutor.execute(request, userId);
    }
}
