// Input: 子状态切换 / 证据附加 / 查询 入参 + 当前用户
// Output: EvaluationDTO；策略校验+持久化+审计；ANNOUNCED 时推进 Project.stage→RESULT_PENDING
// Pos: project/service/ - 编排层（不含纯规则）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.project.entity.ProjectEvaluation;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.project.core.EvaluationStateTransitionPolicy;
import com.xiyu.bid.project.core.EvaluationSubStage;
import com.xiyu.bid.project.core.ProjectStage;
import com.xiyu.bid.project.core.ProjectStageTransitionPolicy;
import com.xiyu.bid.project.dto.EvaluationDTO;
import com.xiyu.bid.project.dto.EvaluationEvidenceAttachRequest;
import com.xiyu.bid.project.dto.EvaluationSubStageUpdateRequest;
import com.xiyu.bid.project.repository.ProjectEvaluationRepository;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectEvaluationService {

    public static final String LINKED_ENTITY_TYPE = "EVALUATION";

    private final ProjectEvaluationRepository repository;
    private final ProjectRepository projectRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectStageService projectStageService;

    @Auditable(action = "TRANSITION_EVALUATION_SUB_STAGE", entityType = "ProjectEvaluation",
            description = "切换评标子状态")
    public EvaluationDTO transitionSubStage(Long projectId, EvaluationSubStageUpdateRequest req, Long userId) {
        mustGetProject(projectId);
        ProjectEvaluation entity = repository.findByProjectId(projectId)
                .orElseGet(() -> initEntity(projectId, userId));
        EvaluationSubStage current = parseSubStage(entity.getSubStage());
        EvaluationSubStage target = req.getTargetSubStage();
        var decision = EvaluationStateTransitionPolicy.decide(current, target);
        if (!decision.allowed()) {
            var deny = (EvaluationStateTransitionPolicy.Decision.Deny) decision;
            throw new ResponseStatusException(HttpStatus.CONFLICT, deny.reason());
        }
        applyTimestamps(entity, target);
        entity.setSubStage(target.name());
        if (req.getNotes() != null) entity.setNotes(req.getNotes());
        entity.setUpdatedBy(userId);
        ProjectEvaluation saved = repository.save(entity);
        if (target == EvaluationSubStage.ANNOUNCED) {
            advanceProjectStageToResultPending(projectId);
        }
        log.info("Evaluation sub-stage transitioned project={} {}->{} user={}",
                projectId, current, target, userId);
        return toDto(saved);
    }

    @Auditable(action = "ATTACH_EVALUATION_EVIDENCE", entityType = "ProjectEvaluation",
            description = "附加评标证据文档")
    public EvaluationDTO attachEvidence(Long projectId, EvaluationEvidenceAttachRequest req, Long userId) {
        mustGetProject(projectId);
        ProjectEvaluation entity = repository.findByProjectId(projectId)
                .orElseGet(() -> initEntity(projectId, userId));
        if (entity.getId() == null) {
            entity = repository.save(entity);
        }
        Long evalId = entity.getId();
        for (Long fileId : req.getFileIds()) {
            ProjectDocument doc = projectDocumentRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("ProjectDocument", String.valueOf(fileId)));
            if (!projectId.equals(doc.getProjectId())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "文档 " + fileId + " 不属于项目 " + projectId);
            }
            doc.setLinkedEntityType(LINKED_ENTITY_TYPE);
            doc.setLinkedEntityId(evalId);
            projectDocumentRepository.save(doc);
        }
        entity.setUpdatedBy(userId);
        ProjectEvaluation saved = repository.save(entity);
        log.info("Evaluation evidence attached project={} count={} user={}",
                projectId, req.getFileIds().size(), userId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<EvaluationDTO> getByProject(Long projectId) {
        return repository.findByProjectId(projectId).map(this::toDto);
    }

    private void advanceProjectStageToResultPending(Long projectId) {
        ProjectStage current = projectStageService.currentStage(projectId);
        if (current != ProjectStage.EVALUATING) {
            return; // 已被外部推进或当前不在评标阶段，幂等跳过
        }
        projectStageService.requestTransition(projectId, ProjectStage.RESULT_PENDING,
                ProjectStageTransitionPolicy.GateInputs.EMPTY);
    }

    private void applyTimestamps(ProjectEvaluation entity, EvaluationSubStage target) {
        LocalDateTime now = LocalDateTime.now();
        switch (target) {
            case IN_PROGRESS -> entity.setEvaluationStartedAt(now);
            case AWAITING_BOARD -> entity.setBoardReceivedAt(now);
            case ANNOUNCED -> entity.setAnnouncedAt(now);
        }
    }

    private EvaluationSubStage parseSubStage(String code) {
        try {
            return code == null ? EvaluationSubStage.IN_PROGRESS : EvaluationSubStage.valueOf(code);
        } catch (IllegalArgumentException ex) {
            return EvaluationSubStage.IN_PROGRESS;
        }
    }

    private ProjectEvaluation initEntity(Long projectId, Long userId) {
        return ProjectEvaluation.builder()
                .projectId(projectId)
                .subStage(EvaluationSubStage.IN_PROGRESS.name())
                .evaluationStartedAt(LocalDateTime.now())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
    }

    private Project mustGetProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
    }

    private EvaluationDTO toDto(ProjectEvaluation e) {
        List<Long> evidenceIds = e.getId() == null ? List.of()
                : projectDocumentRepository.findByProjectIdAndFiltersOrderByCreatedAtDesc(
                        e.getProjectId(), null, LINKED_ENTITY_TYPE, e.getId())
                .stream().map(ProjectDocument::getId).toList();
        return EvaluationDTO.builder()
                .id(e.getId())
                .projectId(e.getProjectId())
                .subStage(e.getSubStage())
                .evaluationStartedAt(e.getEvaluationStartedAt())
                .boardReceivedAt(e.getBoardReceivedAt())
                .announcedAt(e.getAnnouncedAt())
                .notes(e.getNotes())
                .evidenceDocIds(evidenceIds)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .updatedBy(e.getUpdatedBy())
                .build();
    }
}
