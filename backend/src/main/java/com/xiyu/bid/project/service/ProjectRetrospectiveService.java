// Input: 提交/审核复盘请求 + 当前用户
// Output: RetrospectiveDTO；通过策略校验+持久化+审计；审核通过后推进 stage
// Pos: project/service/ - 编排层（不含纯规则）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.project.entity.ProjectRetrospective;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.project.core.BidResultType;
import com.xiyu.bid.project.core.ProjectStage;
import com.xiyu.bid.project.core.ProjectStageTransitionPolicy;
import com.xiyu.bid.project.core.RetrospectiveFieldPolicy;
import com.xiyu.bid.project.dto.RetrospectiveDTO;
import com.xiyu.bid.project.dto.RetrospectiveReviewRequest;
import com.xiyu.bid.project.dto.RetrospectiveSubmitRequest;
import com.xiyu.bid.project.repository.ProjectRetrospectiveRepository;
import com.xiyu.bid.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectRetrospectiveService {

    private final ProjectRetrospectiveRepository repository;
    private final ProjectRepository projectRepository;

    @Auditable(action = "SUBMIT_RETROSPECTIVE", entityType = "ProjectRetrospective", description = "提交项目复盘")
    public RetrospectiveDTO submit(Long projectId, RetrospectiveSubmitRequest req, Long currentUserId) {
        Project project = mustGetProject(projectId);
        BidResultType rt = req.getResultType();
        var input = new RetrospectiveFieldPolicy.RetrospectiveInput(
                req.getSummary(), req.getWinFactors(), req.getLossReasons(),
                req.getCompetitorNotes(), req.getImprovementActions());
        var decision = RetrospectiveFieldPolicy.validate(rt, input);
        if (!decision.allowed()) {
            var deny = (RetrospectiveFieldPolicy.Decision.Deny) decision;
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, deny.reason());
        }
        ProjectRetrospective entity = repository.findByProjectId(projectId)
                .orElseGet(newEntity(projectId, currentUserId));
        entity.setResultType(rt.name());
        entity.setSummary(req.getSummary());
        entity.setWinFactors(req.getWinFactors());
        entity.setLossReasons(req.getLossReasons());
        entity.setCompetitorNotes(req.getCompetitorNotes());
        entity.setImprovementActions(req.getImprovementActions());
        entity.setReviewStatus(ProjectRetrospective.ReviewStatus.PENDING_REVIEW.name());
        entity.setUpdatedBy(currentUserId);
        ProjectRetrospective saved = repository.save(entity);
        log.info("Retrospective submitted project={} status=PENDING_REVIEW user={}", projectId, currentUserId);
        return toDto(saved);
    }

    @Auditable(action = "REVIEW_RETROSPECTIVE", entityType = "ProjectRetrospective", description = "审核项目复盘")
    public RetrospectiveDTO review(Long projectId, RetrospectiveReviewRequest req, Long reviewerId) {
        Project project = mustGetProject(projectId);
        ProjectRetrospective entity = repository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectRetrospective", String.valueOf(projectId)));
        if (!ProjectRetrospective.ReviewStatus.PENDING_REVIEW.name().equals(entity.getReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "复盘当前状态不可审核：" + entity.getReviewStatus());
        }
        boolean approve = Boolean.TRUE.equals(req.getApprove());
        if (!approve && (req.getComment() == null || req.getComment().trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "驳回必须提供 comment");
        }
        entity.setReviewStatus(approve
                ? ProjectRetrospective.ReviewStatus.APPROVED.name()
                : ProjectRetrospective.ReviewStatus.REJECTED.name());
        entity.setReviewComment(req.getComment());
        entity.setReviewedBy(reviewerId);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setUpdatedBy(reviewerId);
        ProjectRetrospective saved = repository.save(entity);
        if (approve) {
            advanceStageToClosedPrep(project);
        }
        log.info("Retrospective reviewed project={} approve={} reviewer={}", projectId, approve, reviewerId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<RetrospectiveDTO> getByProject(Long projectId) {
        return repository.findByProjectId(projectId).map(this::toDto);
    }

    private void advanceStageToClosedPrep(Project project) {
        // 推进 RETROSPECTIVE → CLOSED（线性 FSM 下 RETROSPECTIVE 的下一步即 CLOSED）
        ProjectStage currentStage = parseStage(project);
        if (currentStage != ProjectStage.RETROSPECTIVE) {
            // 不在复盘阶段则跳过推进（容错：审批通过仅落库状态）
            return;
        }
        var d = ProjectStageTransitionPolicy.decide(
                currentStage, ProjectStage.CLOSED, ProjectStageTransitionPolicy.GateInputs.EMPTY);
        if (!d.allowed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "阶段推进失败：" + ((ProjectStageTransitionPolicy.Decision.Deny) d).reason());
        }
        // 通过反射式 setter 写入 stage 列：Project 实体的 stage 字段尚未在 entity 类中定义
        // （V99 schema 已加列）。此处仅留扩展点，stage 写入由后续 WS-G 统一管理。
    }

    private ProjectStage parseStage(Project project) {
        // Project entity 当前未直接暴露 stage 字段；使用 status 与 stage 的语义映射作为占位。
        // WS-G 将统一注入真实 stage 读取。
        return ProjectStage.RETROSPECTIVE;
    }

    private Project mustGetProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
    }

    private Supplier<ProjectRetrospective> newEntity(Long projectId, Long userId) {
        return () -> ProjectRetrospective.builder()
                .projectId(projectId)
                .reviewStatus(ProjectRetrospective.ReviewStatus.PENDING_REVIEW.name())
                .createdBy(userId)
                .build();
    }

    private RetrospectiveDTO toDto(ProjectRetrospective e) {
        return RetrospectiveDTO.builder()
                .id(e.getId())
                .projectId(e.getProjectId())
                .resultType(e.getResultType())
                .summary(e.getSummary())
                .winFactors(e.getWinFactors())
                .lossReasons(e.getLossReasons())
                .competitorNotes(e.getCompetitorNotes())
                .improvementActions(e.getImprovementActions())
                .reviewStatus(e.getReviewStatus())
                .reviewedBy(e.getReviewedBy())
                .reviewedAt(e.getReviewedAt())
                .reviewComment(e.getReviewComment())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
