// Input: 结项请求 + 保证金快照 + 闸门策略
// Output: ClosureDTO / ClosurePreviewDTO；通过策略校验 + 持久化 + 审计
// Pos: project/service/ - 编排层（不含纯规则）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.fees.entity.Fee;
import com.xiyu.bid.fees.repository.FeeRepository;
import com.xiyu.bid.project.core.ProjectClosureGatePolicy;
import com.xiyu.bid.project.core.ProjectClosureGatePolicy.ClosureInput;
import com.xiyu.bid.project.core.ProjectClosureGatePolicy.DepositReturnStatus;
import com.xiyu.bid.project.core.ProjectClosureGatePolicy.DepositSnapshot;
import com.xiyu.bid.project.dto.ClosureDTO;
import com.xiyu.bid.project.dto.ClosurePreviewDTO;
import com.xiyu.bid.project.dto.ClosureSubmitRequest;
import com.xiyu.bid.project.entity.ProjectClosure;
import com.xiyu.bid.project.entity.ProjectDepositSnapshot;
import com.xiyu.bid.project.repository.ProjectClosureRepository;
import com.xiyu.bid.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectClosureService {

    private final ProjectClosureRepository closureRepository;
    private final FeeRepository feeRepository;
    private final ProjectRepository projectRepository;

    /**
     * GET preview: 返回保证金快照 + 是否可结项 + 阻断原因。
     */
    @Transactional(readOnly = true)
    public ClosurePreviewDTO preview(Long projectId) {
        mustGetProject(projectId);
        ProjectDepositSnapshot snap = buildDepositSnapshot(projectId);
        DepositSnapshot gateSnap = snap.toGateInput();
        var decision = ProjectClosureGatePolicy.decide(gateSnap, ClosureInput.EMPTY);

        boolean alreadyClosed = closureRepository.existsByProjectIdAndStageLockedTrue(projectId);
        List<String> blockingReasons = decision.allowed() ? List.of()
                : ((ProjectClosureGatePolicy.Decision.Deny) decision).reasons();

        return ClosurePreviewDTO.builder()
                .projectId(projectId)
                .hasDeposit(snap.hasDeposit())
                .depositAmount(snap.depositAmount())
                .depositReturnStatus(snap.returnStatus().name())
                .depositReturnDate(snap.returnDate())
                .depositReturnEvidenceId(snap.evidenceDocId())
                .canClose(decision.allowed() && !alreadyClosed)
                .blockingReasons(blockingReasons)
                .alreadyClosed(alreadyClosed)
                .stageLocked(alreadyClosed)
                .build();
    }

    /**
     * POST submit: 校验保证金闸门 → 持久化结项 → 锁定。
     */
    @Auditable(action = "PROJECT_CLOSED", entityType = "ProjectClosure", description = "提交项目结项")
    public ClosureDTO submitClosure(Long projectId, ClosureSubmitRequest req, Long userId) {
        mustGetProject(projectId);

        // 重复结项检测
        if (closureRepository.existsByProjectIdAndStageLockedTrue(projectId)) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "项目已结项，不可重复操作");
        }

        // 组装保证金快照
        ProjectDepositSnapshot depositSnap = buildDepositSnapshot(projectId);
        DepositSnapshot gateSnap = mergeWithRequest(depositSnap, req);

        // 闸门校验
        var decision = ProjectClosureGatePolicy.decide(gateSnap, new ClosureInput(req.getArchiveLocation(), req.getNotes()));
        if (!decision.allowed()) {
            var deny = (ProjectClosureGatePolicy.Decision.Deny) decision;
            throw new ResponseStatusException(HttpStatus.CONFLICT, deny.reasonText());
        }

        // 持久化
        LocalDateTime now = LocalDateTime.now();
        ProjectClosure entity = closureRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectClosure.builder().projectId(projectId).createdBy(userId).build());
        entity.setDepositReturned(gateSnap.hasDeposit() && gateSnap.returnStatus() == DepositReturnStatus.RETURNED);
        entity.setDepositReturnEvidenceId(gateSnap.evidenceDocId());
        entity.setArchiveLocation(req.getArchiveLocation());
        entity.setNotes(req.getNotes());
        entity.setClosedAt(now);
        entity.setClosedBy(userId);
        entity.setStageLocked(true);
        entity.setUpdatedBy(userId);
        ProjectClosure saved = closureRepository.save(entity);

        log.info("Project closed: projectId={} userId={}", projectId, userId);
        return toDto(saved);
    }

    // ----- internal -----

    private ProjectDepositSnapshot buildDepositSnapshot(Long projectId) {
        List<Fee> bonds = feeRepository.findByProjectIdAndStatus(projectId, Fee.Status.RETURNED)
                .stream().filter(f -> f.getFeeType() == Fee.FeeType.BID_BOND).toList();
        List<Fee> allBonds = feeRepository.findByProjectId(projectId)
                .stream().filter(f -> f.getFeeType() == Fee.FeeType.BID_BOND
                        && f.getStatus() != Fee.Status.CANCELLED).toList();

        if (allBonds.isEmpty()) {
            return new ProjectDepositSnapshot(projectId, false, BigDecimal.ZERO, DepositReturnStatus.NA, null, null);
        }

        BigDecimal totalAmount = allBonds.stream().map(Fee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!bonds.isEmpty()) {
            Fee latestReturned = bonds.stream()
                    .max((a, b) -> a.getReturnDate() != null && b.getReturnDate() != null
                            ? a.getReturnDate().compareTo(b.getReturnDate()) : 0)
                    .orElse(bonds.get(0));
            // evidenceDocId 从 closure 表取（如果已存在）
            Long evidenceId = closureRepository.findByProjectId(projectId)
                    .map(ProjectClosure::getDepositReturnEvidenceId).orElse(null);
            return new ProjectDepositSnapshot(projectId, true, totalAmount,
                    DepositReturnStatus.RETURNED, latestReturned.getReturnDate(), evidenceId);
        }

        return new ProjectDepositSnapshot(projectId, true, totalAmount, DepositReturnStatus.NOT_RETURNED, null, null);
    }

    /**
     * 合并请求中的 deposit 信息到快照（补充 evidence 等字段）。
     */
    private DepositSnapshot mergeWithRequest(ProjectDepositSnapshot snap, ClosureSubmitRequest req) {
        if (!snap.hasDeposit()) {
            return snap.toGateInput();
        }
        // 如果请求中显式声明了 depositReturned，并提供了日期和凭证
        if (Boolean.TRUE.equals(req.getDepositReturned())) {
            return new DepositSnapshot(true, DepositReturnStatus.RETURNED,
                    req.getDepositReturnDate() != null ? req.getDepositReturnDate() : snap.returnDate(),
                    req.getDepositReturnEvidenceId() != null ? req.getDepositReturnEvidenceId() : snap.evidenceDocId());
        }
        return snap.toGateInput();
    }

    private Project mustGetProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
    }

    private ClosureDTO toDto(ProjectClosure e) {
        return ClosureDTO.builder()
                .id(e.getId())
                .projectId(e.getProjectId())
                .closedAt(e.getClosedAt())
                .closedBy(e.getClosedBy())
                .depositReturned(e.getDepositReturned())
                .depositReturnEvidenceId(e.getDepositReturnEvidenceId())
                .archiveLocation(e.getArchiveLocation())
                .stageLocked(e.getStageLocked())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
