// Input: TenderEvaluationRepository, TenderRepository, ProjectRepository, TaskService, UserRepository
// Output: TenderEvaluation operations - submit evaluation, review, and proceed to bid
// Pos: Service/业务编排层
// 维护声明: 仅维护标讯评估业务规则。
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
import com.xiyu.bid.tender.dto.TenderEvaluationRequest;
import com.xiyu.bid.tender.dto.TenderReviewRequest;
import com.xiyu.bid.tender.entity.TenderEvaluation;
import com.xiyu.bid.tender.repository.TenderEvaluationRepository;
import com.xiyu.bid.task.dto.TaskDTO;
import com.xiyu.bid.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 标讯评估服务
 * 处理项目经理提交评估、投标部管理员审核和投标立项
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenderEvaluationService {

    private final TenderEvaluationRepository tenderEvaluationRepository;
    private final TenderRepository tenderRepository;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final TenderStatusTransitionPolicy statusTransitionPolicy;

    /**
     * 获取标讯评估详情
     */
    @Transactional(readOnly = true)
    public Optional<TenderEvaluationDTO> getEvaluation(Long tenderId) {
        return tenderEvaluationRepository.findByTenderId(tenderId)
                .map(this::toDTO);
    }

    /**
     * 项目经理提交评估
     * 状态从 TRACKING 变为 EVALUATED
     */
    public TenderEvaluationDTO submitEvaluation(Long tenderId, TenderEvaluationRequest request, Long evaluatorId) {
        log.info("Submitting evaluation for tender {} by user {}", tenderId, evaluatorId);

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", tenderId.toString()));

        // 验证状态转换
        statusTransitionPolicy.assertTransition(tender.getStatus(), Tender.Status.EVALUATED);

        // 获取评估人信息
        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", evaluatorId.toString()));

        // 创建或更新评估记录
        TenderEvaluation evaluation = tenderEvaluationRepository.findByTenderId(tenderId)
                .orElse(TenderEvaluation.builder()
                        .tenderId(tenderId)
                        .reviewStatus(TenderEvaluation.ReviewStatus.PENDING)
                        .build());

        evaluation.setEvaluationContent(request.evaluationContent());
        evaluation.setEstimatedBudget(request.estimatedBudget());
        evaluation.setRiskAssessment(request.riskAssessment());
        evaluation.setNotes(request.notes());
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setEvaluatorName(evaluator.getUsername());
        evaluation.setEvaluatedAt(LocalDateTime.now());

        TenderEvaluation savedEvaluation = tenderEvaluationRepository.save(evaluation);

        // 更新标讯状态
        tender.setStatus(Tender.Status.EVALUATED);
        tenderRepository.save(tender);

        log.info("Evaluation submitted for tender {}, status changed to EVALUATED", tenderId);
        return toDTO(savedEvaluation);
    }

    /**
     * 管理员审核标讯
     */
    public TenderEvaluationDTO reviewTender(Long tenderId, TenderReviewRequest request, Long reviewerId) {
        log.info("Reviewing tender {} by admin {}, approved={}", tenderId, reviewerId, request.approved());

        TenderEvaluation evaluation = tenderEvaluationRepository.findByTenderId(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("标讯尚未提交评估"));

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", tenderId.toString()));

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
            statusTransitionPolicy.assertTransition(tender.getStatus(), Tender.Status.BIDDED);
            tender.setStatus(Tender.Status.BIDDED);
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
        return toDTO(savedEvaluation);
    }

    /**
     * 投标立项：审核通过后创建项目和待办
     */
    public TenderBidResult proceedToBid(Long tenderId, Long adminId) {
        log.info("Proceeding to bid for tender {} by admin {}", tenderId, adminId);

        TenderEvaluation evaluation = tenderEvaluationRepository.findByTenderId(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("标讯尚未提交评估"));

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", tenderId.toString()));

        // 验证状态是 BIDDED
        if (tender.getStatus() != Tender.Status.BIDDED) {
            throw new IllegalStateException("标讯状态不是已投标，无法创建立项待办");
        }

        // 创建项目
        ProjectDTO projectDTO = ProjectDTO.builder()
                .name(tender.getTitle())
                .tenderId(tenderId)
                .status(Project.Status.INITIATED)
                .managerId(evaluation.getEvaluatorId())
                .budget(evaluation.getEstimatedBudget())
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

    private TenderEvaluationDTO toDTO(TenderEvaluation evaluation) {
        Tender tender = tenderRepository.findById(evaluation.getTenderId()).orElse(null);
        return new TenderEvaluationDTO(
                evaluation.getTenderId(),
                tender != null ? tender.getTitle() : null,
                tender != null ? tender.getStatus() : null,
                evaluation.getEvaluationContent(),
                evaluation.getEstimatedBudget(),
                evaluation.getRiskAssessment(),
                evaluation.getNotes(),
                evaluation.getEvaluatorId(),
                evaluation.getEvaluatorName(),
                evaluation.getEvaluatedAt()
        );
    }
}
