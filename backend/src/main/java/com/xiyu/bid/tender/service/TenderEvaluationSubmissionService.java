// Input: 评估表请求（TenderEvaluationSubmitRequest）、标讯 ID、评估人 ID
// Output: TenderEvaluationDTO（含状态机切换的最新视图）
// Pos: Service/业务编排层（命令式外壳）
// 维护声明: 仅做编排：取标讯/评估人 -> Guard -> 委托 Policy 校验 -> 状态切换 -> 持久化。
//          业务规则（必填/范围）一律下沉至 com.xiyu.bid.tender.core.TenderEvaluationFormPolicy。
package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.tender.core.FieldError;
import com.xiyu.bid.tender.core.TenderEvaluationFormPolicy;
import com.xiyu.bid.tender.core.ValidationResult;
import com.xiyu.bid.tender.dto.TenderEvaluationDTO;
import com.xiyu.bid.tender.dto.TenderEvaluationSubmitRequest;
import com.xiyu.bid.tender.entity.TenderEvaluation;
import com.xiyu.bid.tender.entity.TenderEvaluation.EvaluationStatus;
import com.xiyu.bid.tender.repository.TenderEvaluationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

/**
 * 标讯项目评估表草稿与提交服务（V119 新口径）。
 *
 * <p>三个操作：
 * <ul>
 *   <li>{@link #loadOrInitDraft(Long, Long)} — 已有记录返回原值；无记录返回空白 DRAFT（不持久化）</li>
 *   <li>{@link #saveDraft(Long, TenderEvaluationSubmitRequest, Long)} — upsert 草稿；SUBMITTED 拒绝</li>
 *   <li>{@link #submit(Long, TenderEvaluationSubmitRequest, Long)} — 走 Policy 校验后 DRAFT→SUBMITTED</li>
 * </ul>
 *
 * <p>所有时间戳通过注入的 {@link Clock} 取值，便于测试固定时间。
 *
 * <p>H4 决策：允许 submit 没有前置 saveDraft（"一步到位"），便于 PM 第一次写完直接提交。
 * <p>H5 决策：submit 成功后将 tender.status 推进到 EVALUATED（仅在状态机允许的情况下）。
 */
@Service
@Transactional
public class TenderEvaluationSubmissionService {

    private final TenderEvaluationRepository evaluationRepository;
    private final TenderRepository tenderRepository;
    private final UserRepository userRepository;
    private final TenderProjectAccessGuard accessGuard;
    private final TenderAssignmentPermissions permissions;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TenderEvaluationSubmissionService.class);

    private final TenderEvaluationNotificationService evaluationNotificationService;
    private final Clock clock;

    public TenderEvaluationSubmissionService(
            TenderEvaluationNotificationService evaluationNotificationService,
            TenderEvaluationRepository evaluationRepository,
            TenderRepository tenderRepository,
            UserRepository userRepository,
            TenderProjectAccessGuard accessGuard,
            TenderAssignmentPermissions permissions,
            Clock clock) {
        this.evaluationNotificationService = evaluationNotificationService;
        this.evaluationRepository = evaluationRepository;
        this.tenderRepository = tenderRepository;
        this.userRepository = userRepository;
        this.accessGuard = accessGuard;
        this.permissions = permissions;
        this.clock = clock;
    }

    /**
     * 加载已有评估表，或返回一个未持久化的空白 DRAFT 视图。
     */
    @Transactional(readOnly = true)
    public TenderEvaluationDTO loadOrInitDraft(Long tenderId, Long evaluatorId) {
        Tender tender = requireTender(tenderId);
        accessGuard.assertCanAccessTender(tender);
        User evaluator = requireUser(evaluatorId);
        boolean canFill = permissions.canFill(tenderId, evaluatorId);
        boolean canDecide = permissions.canDecide(tenderId, evaluatorId);
        return evaluationRepository.findByTenderId(tenderId)
                .map(existing -> toDTO(existing, tender, canFill, canDecide))
                .orElseGet(() -> emptyDraftDTO(tender, evaluator, canFill, canDecide));
    }

    /**
     * 保存或更新草稿；不做业务必填校验。
     * <p>已是 SUBMITTED 的记录抛 {@link IllegalStateException}。
     */
    public TenderEvaluationDTO saveDraft(Long tenderId,
                                         TenderEvaluationSubmitRequest req,
                                         Long evaluatorId) {
        Tender tender = requireTender(tenderId);
        accessGuard.assertCanAccessTender(tender);
        User evaluator = requireUser(evaluatorId);

        if (!permissions.canFill(tenderId, evaluatorId)) {
            throw new AccessDeniedException(
                    "user " + evaluatorId + " is not the assignee of tender " + tenderId);
        }

        TenderEvaluation entity = evaluationRepository.findByTenderId(tenderId)
                .orElseGet(() -> newEntity(tenderId, evaluator));

        if (entity.getEvaluationStatus() == EvaluationStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "evaluation already submitted; cannot save as draft for tender " + tenderId);
        }

        applyRequest(entity, req);
        entity.setEvaluationStatus(EvaluationStatus.DRAFT);

        TenderEvaluation saved = evaluationRepository.save(entity);
        boolean canDecide = permissions.canDecide(tenderId, evaluatorId);
        return toDTO(saved, tender, true, canDecide);
    }

    /**
     * 提交评估：经 Policy 校验通过后将 DRAFT 切换为 SUBMITTED 并打时间戳。
     * <p>H4: 没有前置 DRAFT 也允许（一步到位创建 + 提交）。
     * <p>H5: submit 成功后若 tender.status 仍为 TRACKING / PENDING_ASSIGNMENT，
     * 推进到 EVALUATED（让上层 UI 看见状态切换）。
     * <p>已 SUBMITTED → {@link IllegalStateException}（"already submitted"）。
     * <p>Policy 返回错误 → {@link IllegalArgumentException}（聚合所有错误消息）。
     */
    public TenderEvaluationDTO submit(Long tenderId,
                                      TenderEvaluationSubmitRequest req,
                                      Long evaluatorId) {
        Tender tender = requireTender(tenderId);
        accessGuard.assertCanAccessTender(tender);
        User evaluator = requireUser(evaluatorId);

        if (!permissions.canFill(tenderId, evaluatorId)) {
            throw new AccessDeniedException(
                    "user " + evaluatorId + " is not the assignee of tender " + tenderId);
        }

        ValidationResult result = TenderEvaluationFormPolicy.validate(req);
        if (!result.isValid()) {
            String aggregated = result.errors().stream()
                    .map(FieldError::message)
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Validation failed: " + aggregated);
        }

        TenderEvaluation entity = evaluationRepository.findByTenderId(tenderId)
                .orElseGet(() -> newEntity(tenderId, evaluator));

        if (entity.getEvaluationStatus() == EvaluationStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "evaluation already submitted for tender " + tenderId);
        }

        applyRequest(entity, req);
        entity.setEvaluationStatus(EvaluationStatus.SUBMITTED);
        entity.setSubmittedAt(now());
        if (entity.getEvaluatorId() == null) {
            entity.setEvaluatorId(evaluator.getId());
        }
        if (entity.getEvaluatorName() == null) {
            entity.setEvaluatorName(evaluator.getUsername());
        }

        TenderEvaluation saved = evaluationRepository.save(entity);

        // H5: 推进 tender.status 到 EVALUATED。
        // 仅 TRACKING -> EVALUATED 在状态机允许范围内；PENDING_ASSIGNMENT 跳过
        // （PM 在标讯被分配前不应进入评估流程，不放宽状态机）。
        if (tender.getStatus() == Tender.Status.TRACKING) {
            tender.setStatus(Tender.Status.EVALUATED);
            tenderRepository.save(tender);
        }

        // REQ-BC-010: 评估提交后为相关角色创建待办
        evaluationNotificationService.createEvaluationNotificationTodos(tender, evaluator);

        boolean canDecide = permissions.canDecide(tenderId, evaluatorId);
        return toDTO(saved, tender, true, canDecide);
    }

    // ---------- helpers ----------

    private Tender requireTender(Long tenderId) {
        return tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", String.valueOf(tenderId)));
    }

    private User requireUser(Long evaluatorId) {
        return userRepository.findById(evaluatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(evaluatorId)));
    }

    private TenderEvaluation newEntity(Long tenderId, User evaluator) {
        return TenderEvaluation.builder()
                .tenderId(tenderId)
                .evaluatorId(evaluator.getId())
                .evaluatorName(evaluator.getUsername())
                .evaluationStatus(EvaluationStatus.DRAFT)
                .reviewStatus(TenderEvaluation.ReviewStatus.PENDING)
                .build();
    }

    private void applyRequest(TenderEvaluation entity, TenderEvaluationSubmitRequest req) {
        entity.setProjectBackground(req.projectBackground());
        entity.setCompetitorAnalysis(req.competitorAnalysis());
        entity.setContractPeriodStart(req.contractPeriodStart());
        entity.setContractPeriodEnd(req.contractPeriodEnd());
        entity.setShortlistedCount(req.shortlistedCount());
        entity.setPlatformServiceFee(req.platformServiceFee());
        entity.setPreviousQuotation(req.previousQuotation());
        entity.setBidRecommendation(req.bidRecommendation());
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }

    private TenderEvaluationDTO emptyDraftDTO(Tender tender, User evaluator,
                                              boolean canFill, boolean canDecide) {
        return new TenderEvaluationDTO(
                tender.getId(),
                tender.getTitle(),
                tender.getStatus(),
                EvaluationStatus.DRAFT,
                null, null, null, null, null, null, null, null, null,
                evaluator.getId(),
                evaluator.getUsername(),
                null,
                canFill,
                canDecide
        );
    }

    private TenderEvaluationDTO toDTO(TenderEvaluation e, Tender tender,
                                      boolean canFill, boolean canDecide) {
        return new TenderEvaluationDTO(
                e.getTenderId(),
                tender != null ? tender.getTitle() : null,
                tender != null ? tender.getStatus() : null,
                e.getEvaluationStatus(),
                e.getProjectBackground(),
                e.getCompetitorAnalysis(),
                e.getContractPeriodStart(),
                e.getContractPeriodEnd(),
                e.getShortlistedCount(),
                e.getPlatformServiceFee(),
                e.getPreviousQuotation(),
                e.getBidRecommendation(),
                e.getSubmittedAt(),
                e.getEvaluatorId(),
                e.getEvaluatorName(),
                e.getEvaluatedAt(),
                canFill,
                canDecide
        );
    }
}
