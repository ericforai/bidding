// Input: 结项请求 + 保证金快照 + 闸门策略
// Output: ClosureDTO / ClosurePreviewDTO；通过策略校验 + 持久化 + 审计；§3.6 全字段锁定（仅 closure 写入豁免）
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
import com.xiyu.bid.project.core.ProjectStage;
import com.xiyu.bid.project.core.ProjectStageTransitionPolicy;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectClosureService {

    private final ProjectClosureRepository closureRepository;
    private final FeeRepository feeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStageService projectStageService;

    /**
     * GET preview: 返回保证金快照 + 是否可结项 + 阻断原因。
     */
    @Transactional(readOnly = true)
    public ClosurePreviewDTO preview(Long projectId) {
        mustGetProject(projectId);
        // H5: 一次性读 closure，避免 N+1。
        Optional<ProjectClosure> existingClosure = closureRepository.findByProjectId(projectId);
        ProjectDepositSnapshot snap = buildDepositSnapshot(projectId, existingClosure);
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

        // 重复结项检测（保留：closure 自身写入需要这个守卫，不能用 ProjectFieldLockPolicy 替代——
        // CLOSED 全字段锁定如果应用到 closure 写入本身会形成自锁循环）
        if (closureRepository.existsByProjectIdAndStageLockedTrue(projectId)) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "项目已结项，不可重复操作");
        }

        // H5: 一次性读 closure，传给 buildDepositSnapshot 复用。
        Optional<ProjectClosure> existingClosure = closureRepository.findByProjectId(projectId);
        ProjectDepositSnapshot depositSnap = buildDepositSnapshot(projectId, existingClosure);
        DepositSnapshot gateSnap = mergeWithRequest(depositSnap, req);

        // 闸门校验
        var decision = ProjectClosureGatePolicy.decide(gateSnap, new ClosureInput(req.getArchiveLocation(), req.getNotes()));
        if (!decision.allowed()) {
            var deny = (ProjectClosureGatePolicy.Decision.Deny) decision;
            throw new ResponseStatusException(HttpStatus.CONFLICT, deny.reasonText());
        }

        // 持久化
        LocalDateTime now = LocalDateTime.now();
        ProjectClosure entity = existingClosure
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

        // §5.4: 结项后推进 RETROSPECTIVE → CLOSED（已是 CLOSED 则幂等跳过）
        ProjectStage current = projectStageService.currentStage(projectId);
        if (current == ProjectStage.RETROSPECTIVE) {
            projectStageService.requestTransition(projectId, ProjectStage.CLOSED,
                    ProjectStageTransitionPolicy.GateInputs.EMPTY);
        }
        log.info("Project closed: projectId={} userId={}", projectId, userId);
        return toDto(saved);
    }

    // ----- internal -----

    /**
     * C3 修复：保证金快照 returnStatus = RETURNED 当且仅当所有未取消 BID_BOND 都已 RETURNED。
     * 部分退回 → NOT_RETURNED（前端展示 + 闸门拒绝）。
     */
    private ProjectDepositSnapshot buildDepositSnapshot(Long projectId, Optional<ProjectClosure> existingClosure) {
        List<Fee> allBonds = feeRepository.findByProjectId(projectId)
                .stream().filter(f -> f.getFeeType() == Fee.FeeType.BID_BOND
                        && f.getStatus() != Fee.Status.CANCELLED).toList();

        if (allBonds.isEmpty()) {
            return new ProjectDepositSnapshot(projectId, false, BigDecimal.ZERO, DepositReturnStatus.NA, null, null);
        }

        BigDecimal totalAmount = allBonds.stream().map(Fee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Fee> returnedBonds = allBonds.stream()
                .filter(f -> f.getStatus() == Fee.Status.RETURNED).toList();

        // 必须全部 RETURNED 才算 RETURNED；任何一笔未退回 → NOT_RETURNED。
        if (returnedBonds.size() == allBonds.size()) {
            Fee latestReturned = returnedBonds.stream()
                    .max((a, b) -> a.getReturnDate() != null && b.getReturnDate() != null
                            ? a.getReturnDate().compareTo(b.getReturnDate()) : 0)
                    .orElse(returnedBonds.get(0));
            Long evidenceId = existingClosure
                    .map(ProjectClosure::getDepositReturnEvidenceId).orElse(null);
            return new ProjectDepositSnapshot(projectId, true, totalAmount,
                    DepositReturnStatus.RETURNED, latestReturned.getReturnDate(), evidenceId);
        }

        return new ProjectDepositSnapshot(projectId, true, totalAmount, DepositReturnStatus.NOT_RETURNED, null, null);
    }

    /**
     * 合并请求中的 deposit 信息到快照（补充 evidence 等字段）。
     * H1: 显式 depositReturned=false 必须强制 NOT_RETURNED，覆盖 fees 派生 RETURNED 的情况，
     * 避免快照与用户声明矛盾时偏向闸门"通过"侧。
     */
    private DepositSnapshot mergeWithRequest(ProjectDepositSnapshot snap, ClosureSubmitRequest req) {
        if (!snap.hasDeposit()) {
            return snap.toGateInput();
        }
        if (Boolean.FALSE.equals(req.getDepositReturned())) {
            return new DepositSnapshot(true, DepositReturnStatus.NOT_RETURNED, null, null);
        }
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
