// Input: TenderEvaluationRepository, TenderRepository, ProjectRepository, TaskService, UserRepository
// Output: TenderEvaluation operations - admin review, proceed-to-bid + V119 facade
// Pos: Service/业务编排层
// 维护声明: 仅维护标讯评估业务规则。V118 facade (submitEvaluation) 已 retired (V119 移除)。
package com.xiyu.bid.tender.service;

import com.xiyu.bid.batch.core.TenderStatusTransitionPolicy;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.project.dto.ProjectDTO;
import com.xiyu.bid.project.service.ProjectService;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.tender.controller.TenderEvaluationController.TenderBidResult;
import com.xiyu.bid.tender.dto.TenderEvaluationDTO;
import com.xiyu.bid.tender.dto.TenderEvaluationSubmitRequest;
import com.xiyu.bid.tender.dto.TenderReviewRequest;
import com.xiyu.bid.tender.entity.TenderEvaluation;
import com.xiyu.bid.tender.repository.TenderEvaluationRepository;
import com.xiyu.bid.task.dto.TaskDTO;
import com.xiyu.bid.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 标讯评估服务（V119 收口后）。
 * <p>当前职责：
 * <ul>
 *   <li>V119 评估表草稿 / 提交（委托给 {@link TenderEvaluationSubmissionService}）</li>
 *   <li>管理员审核（投标 / 弃标）</li>
 *   <li>投标立项（创建项目 + 待办）</li>
 * </ul>
 */
@Service
@Slf4j
@Transactional
public class TenderEvaluationService {

    private final TenderEvaluationRepository tenderEvaluationRepository;
    private final TenderRepository tenderRepository;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final TenderStatusTransitionPolicy statusTransitionPolicy;
    private final TenderEvaluationSubmissionService submissionService;
    private final TenderAssignmentPermissions permissions;
    private final TenderProjectAccessGuard accessGuard;

    public TenderEvaluationService(
            TenderEvaluationRepository tenderEvaluationRepository,
            TenderRepository tenderRepository,
            ProjectService projectService,
            TaskService taskService,
            UserRepository userRepository,
            TenderStatusTransitionPolicy statusTransitionPolicy,
            TenderEvaluationSubmissionService submissionService,
            TenderAssignmentPermissions permissions,
            TenderProjectAccessGuard accessGuard) {
        this.tenderEvaluationRepository = tenderEvaluationRepository;
        this.tenderRepository = tenderRepository;
        this.projectService = projectService;
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.statusTransitionPolicy = statusTransitionPolicy;
        this.submissionService = submissionService;
        this.permissions = permissions;
        this.accessGuard = accessGuard;
    }

    // ---------- V119: 项目评估表草稿/提交 facade（委托给 TenderEvaluationSubmissionService） ----------

    /** 加载或初始化草稿（V119）。 */
    public TenderEvaluationDTO loadOrInitDraft(Long tenderId, Long evaluatorId) {
        return submissionService.loadOrInitDraft(tenderId, evaluatorId);
    }

    /** 保存草稿（V119）。 */
    public TenderEvaluationDTO saveDraft(Long tenderId,
                                         TenderEvaluationSubmitRequest request,
                                         Long evaluatorId) {
        return submissionService.saveDraft(tenderId, request, evaluatorId);
    }

    /** 提交评估（V119）。 */
    public TenderEvaluationDTO submit(Long tenderId,
                                      TenderEvaluationSubmitRequest request,
                                      Long evaluatorId) {
        return submissionService.submit(tenderId, request, evaluatorId);
    }

    /**
     * 获取标讯评估详情（旧 API，已无前端调用方；保留以避免破坏单测）。
     * <p>实例级权限标志默认为 false：本路径不携带 userId，不能做相对判定。
     */
    @Transactional(readOnly = true)
    public Optional<TenderEvaluationDTO> getEvaluation(Long tenderId) {
        return tenderEvaluationRepository.findByTenderId(tenderId)
                .map(e -> toDTO(e, false, false));
    }

    /**
     * 决策标讯（投标 / 弃标）—— 不再依赖角色 enum；改用实例级 canDecide 判定。
     */
    public TenderEvaluationDTO reviewTender(Long tenderId, TenderReviewRequest request, Long reviewerId) {
        log.info("Reviewing tender {} by user {}, approved={}", tenderId, reviewerId, request.approved());

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", tenderId.toString()));
        // Project-scope guard 与 sibling participate/abandon 保持一致 — 设计明确
        // "保持不变：TenderProjectAccessGuard.assertCanAccessTender"。
        accessGuard.assertCanAccessTender(tender);

        if (!permissions.canDecide(tenderId, reviewerId)) {
            throw new AccessDeniedException(
                    "user " + reviewerId + " is not the assigner of tender " + tenderId);
        }

        TenderEvaluation evaluation = tenderEvaluationRepository.findByTenderId(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("标讯尚未提交评估"));

        // 获取审核人信息
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId.toString()));

        // 更新审核信息
        evaluation.setReviewStatus(request.approved()
                ? TenderEvaluation.ReviewStatus.APPROVED
                : TenderEvaluation.ReviewStatus.REJECTED);
        evaluation.setReviewerId(reviewerId);
        evaluation.setReviewerName(reviewer.getUsername());
        evaluation.setReviewedAt(LocalDateTime.now());
        evaluation.setReviewComment(request.reviewComment());

        TenderEvaluation savedEvaluation = tenderEvaluationRepository.save(evaluation);

        // 更新标讯状态
        if (request.approved()) {
            statusTransitionPolicy.assertTransition(tender.getStatus(), Tender.Status.BIDDING);
            tender.setStatus(Tender.Status.BIDDING);
        } else {
            // 弃标
            statusTransitionPolicy.assertTransition(tender.getStatus(), Tender.Status.ABANDONED);
            tender.setStatus(Tender.Status.ABANDONED);
            if (request.abandonmentReason() != null && !request.abandonmentReason().isBlank()) {
                tender.setAbandonmentReason(request.abandonmentReason());
            }
        }
        tenderRepository.save(tender);

        log.info("Tender {} reviewed, status changed to {}", tenderId, tender.getStatus());
        boolean canFill = permissions.canFill(tenderId, reviewerId);
        boolean canDecide = permissions.canDecide(tenderId, reviewerId);
        return toDTO(savedEvaluation, canFill, canDecide);
    }

    /**
     * 投标立项：审核通过后创建项目和待办。
     * <p>实例级权限：调用方必须是 latest assigned-by。
     */
    public TenderBidResult proceedToBid(Long tenderId, Long adminId) {
        log.info("Proceeding to bid for tender {} by user {}", tenderId, adminId);

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", tenderId.toString()));
        accessGuard.assertCanAccessTender(tender);

        if (!permissions.canDecide(tenderId, adminId)) {
            throw new AccessDeniedException(
                    "user " + adminId + " is not the assigner of tender " + tenderId);
        }

        TenderEvaluation evaluation = tenderEvaluationRepository.findByTenderId(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("标讯尚未提交评估"));

        // 验证状态是 BIDDING
        if (tender.getStatus() != Tender.Status.BIDDING) {
            throw new IllegalStateException("标讯状态不是已投标，无法创建立项待办");
        }

        // 创建项目
        ProjectDTO projectDTO = ProjectDTO.builder()
                .name(tender.getTitle())
                .tenderId(tenderId)
                .status(Project.Status.INITIATED)
                .managerId(evaluation.getEvaluatorId())
                .budget(null)
                .customer(tender.getPurchaserName())
                .industry(tender.getIndustry())
                .region(tender.getRegion())
                .deadline(tender.getDeadline() != null ? tender.getDeadline().toLocalDate() : null)
                .description(tender.getDescription())
                .build();

        ProjectDTO createdProject = projectService.createProject(projectDTO);

        // 创建待办：待立项
        TaskDTO taskDTO = TaskDTO.builder()
                .projectId(createdProject.getId())
                .title("【待立项】" + tender.getTitle())
                .description("标讯「" + tender.getTitle() + "」已通过审核，请项目经理尽快完成立项流程。")
                .assigneeId(evaluation.getEvaluatorId())
                .status(Task.Status.TODO)
                .priority(Task.Priority.HIGH)
                .build();

        TaskDTO createdTask = taskService.createTask(taskDTO);

        log.info("Project {} and task {} created for tender {}", createdProject.getId(), createdTask.getId(), tenderId);
        return new TenderBidResult(
                createdProject.getId(),
                createdProject.getName(),
                createdTask.getId(),
                createdTask.getTitle()
        );
    }

    private TenderEvaluationDTO toDTO(TenderEvaluation evaluation, boolean canFill, boolean canDecide) {
        Tender tender = tenderRepository.findById(evaluation.getTenderId()).orElse(null);
        return new TenderEvaluationDTO(
                evaluation.getTenderId(),
                tender != null ? tender.getTitle() : null,
                tender != null ? tender.getStatus() : null,
                evaluation.getEvaluationStatus(),
                evaluation.getProjectBackground(),
                evaluation.getCompetitorAnalysis(),
                evaluation.getContractPeriodStart(),
                evaluation.getContractPeriodEnd(),
                evaluation.getShortlistedCount(),
                evaluation.getPlatformServiceFee(),
                evaluation.getPreviousQuotation(),
                evaluation.getBidRecommendation(),
                evaluation.getSubmittedAt(),
                evaluation.getEvaluatorId(),
                evaluation.getEvaluatorName(),
                evaluation.getEvaluatedAt(),
                canFill,
                canDecide
        );
    }
}
