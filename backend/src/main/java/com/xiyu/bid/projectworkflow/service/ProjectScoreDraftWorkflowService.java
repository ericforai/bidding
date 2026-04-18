// Input: project/score-draft repositories, parser, object mapper, and task workflow service
// Output: score draft workflow orchestration
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.projectworkflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.projectworkflow.core.ScoreDraftPolicy;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftGenerateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftParseResponse;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftUpdateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskViewDTO;
import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import com.xiyu.bid.projectworkflow.repository.ProjectScoreDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectScoreDraftWorkflowService {

    private final ProjectWorkflowGuard projectWorkflowGuard;
    private final ProjectScoreDraftRepository projectScoreDraftRepository;
    private final ScoreDraftParserService scoreDraftParserService;
    private final ProjectTaskWorkflowService projectTaskWorkflowService;
    private final ObjectMapper objectMapper;

    public List<ProjectScoreDraftDTO> getProjectScoreDrafts(Long projectId) {
        requireProject(projectId);
        return projectScoreDraftRepository.findByProjectIdOrderByCategoryAscSourceTableIndexAscSourceRowIndexAsc(projectId)
                .stream()
                .map(this::toScoreDraftDTO)
                .toList();
    }

    public ProjectScoreDraftParseResponse parseProjectScoreDrafts(Long projectId, MultipartFile file) {
        requireProject(projectId);
        clearNonGeneratedDrafts(projectId);
        List<ProjectScoreDraftDTO> draftDTOs = projectScoreDraftRepository.saveAll(scoreDraftParserService.parse(projectId, file))
                .stream()
                .map(this::toScoreDraftDTO)
                .toList();
        return buildParseResponse(draftDTOs);
    }

    public ProjectScoreDraftDTO updateProjectScoreDraft(Long projectId, Long draftId, ProjectScoreDraftUpdateRequest request) {
        requireProject(projectId);
        ProjectScoreDraft draft = requireDraft(projectId, draftId);
        ScoreDraftPolicy.UpdateDecision decision = ScoreDraftPolicy.decideUpdate(new ScoreDraftPolicy.UpdateCommand(
                toCoreStatus(draft.getStatus()),
                request.getAssigneeId(),
                request.getAssigneeName(),
                request.getDueDate(),
                request.getGeneratedTaskTitle(),
                request.getGeneratedTaskDescription(),
                toCoreStatus(request.getStatus()),
                request.getSkipReason()
        ));
        if (!decision.ok()) {
            throw toScoreDraftRuleException(decision.failure());
        }
        applyScoreDraftUpdate(draft, decision);
        return toScoreDraftDTO(projectScoreDraftRepository.save(draft));
    }

    public List<ProjectTaskViewDTO> generateTasksFromScoreDrafts(Long projectId, ProjectScoreDraftGenerateRequest request) {
        requireProject(projectId);
        List<ProjectScoreDraft> drafts = request.getDraftIds().stream()
                .map(draftId -> requireDraft(projectId, draftId))
                .toList();

        ScoreDraftPolicy.GenerationDecision decision = ScoreDraftPolicy.decideGeneration(
                drafts.stream().map(draft -> toCoreStatus(draft.getStatus())).toList()
        );
        if (!decision.ok()) {
            throw toScoreDraftRuleException(decision.failure());
        }

        return drafts.stream()
                .map(this::createTaskFromDraft)
                .toList();
    }

    public void clearNonGeneratedDrafts(Long projectId) {
        requireProject(projectId);
        projectScoreDraftRepository.deleteByProjectIdAndStatusIn(
                projectId,
                List.of(ProjectScoreDraft.Status.DRAFT, ProjectScoreDraft.Status.READY, ProjectScoreDraft.Status.SKIPPED)
        );
    }

    private void requireProject(Long projectId) {
        projectWorkflowGuard.requireProject(projectId);
    }

    private ProjectScoreDraft requireDraft(Long projectId, Long draftId) {
        ProjectScoreDraft draft = projectScoreDraftRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectScoreDraft", String.valueOf(draftId)));
        if (!projectId.equals(draft.getProjectId())) {
            throw new IllegalArgumentException("Score draft does not belong to the specified project");
        }
        return draft;
    }

    private ProjectScoreDraftDTO toScoreDraftDTO(ProjectScoreDraft draft) {
        return ProjectScoreDraftDTO.builder()
                .id(draft.getId())
                .projectId(draft.getProjectId())
                .sourceFileName(draft.getSourceFileName())
                .category(draft.getCategory())
                .scoreItemTitle(draft.getScoreItemTitle())
                .scoreRuleText(draft.getScoreRuleText())
                .scoreValueText(draft.getScoreValueText())
                .taskAction(draft.getTaskAction())
                .generatedTaskTitle(draft.getGeneratedTaskTitle())
                .generatedTaskDescription(draft.getGeneratedTaskDescription())
                .suggestedDeliverables(readDeliverables(draft.getSuggestedDeliverables()))
                .assigneeId(draft.getAssigneeId())
                .assigneeName(draft.getAssigneeName())
                .dueDate(draft.getDueDate())
                .status(draft.getStatus())
                .skipReason(draft.getSkipReason())
                .sourcePage(draft.getSourcePage())
                .sourceTableIndex(draft.getSourceTableIndex())
                .sourceRowIndex(draft.getSourceRowIndex())
                .generatedTaskId(draft.getGeneratedTaskId())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }

    private ProjectScoreDraftParseResponse buildParseResponse(List<ProjectScoreDraftDTO> draftDTOs) {
        return ProjectScoreDraftParseResponse.builder()
                .drafts(draftDTOs)
                .totalCount(draftDTOs.size())
                .draftCount(countByStatus(draftDTOs, ProjectScoreDraft.Status.DRAFT))
                .readyCount(countByStatus(draftDTOs, ProjectScoreDraft.Status.READY))
                .skippedCount(countByStatus(draftDTOs, ProjectScoreDraft.Status.SKIPPED))
                .build();
    }

    private long countByStatus(Collection<ProjectScoreDraftDTO> drafts, ProjectScoreDraft.Status status) {
        return drafts.stream().filter(draft -> draft.getStatus() == status).count();
    }

    private List<String> readDeliverables(String serializedValue) {
        if (serializedValue == null || serializedValue.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(serializedValue, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            return List.of(serializedValue);
        }
    }

    private ProjectTaskViewDTO createTaskFromDraft(ProjectScoreDraft draft) {
        ProjectTaskViewDTO createdTask = projectTaskWorkflowService.createTaskFromDraft(
                draft.getProjectId(),
                draft.getGeneratedTaskTitle(),
                draft.getGeneratedTaskDescription(),
                draft.getAssigneeId(),
                draft.getAssigneeName(),
                draft.getScoreValueText(),
                draft.getDueDate()
        );
        draft.setGeneratedTaskId(createdTask.getId());
        draft.setStatus(ProjectScoreDraft.Status.GENERATED);
        projectScoreDraftRepository.save(draft);
        return createdTask;
    }

    private void applyScoreDraftUpdate(ProjectScoreDraft draft, ScoreDraftPolicy.UpdateDecision decision) {
        draft.setAssigneeId(decision.assigneeId());
        draft.setAssigneeName(decision.assigneeName());
        draft.setDueDate(decision.dueDate());
        if (decision.generatedTaskTitle() != null) {
            draft.setGeneratedTaskTitle(decision.generatedTaskTitle());
        }
        if (decision.generatedTaskDescription() != null) {
            draft.setGeneratedTaskDescription(decision.generatedTaskDescription());
        }
        draft.setStatus(toEntityStatus(decision.status()));
        draft.setSkipReason(decision.skipReason());
    }

    private RuntimeException toScoreDraftRuleException(ScoreDraftPolicy.RuleFailure failure) {
        return switch (failure) {
            case GENERATED_NOT_EDITABLE -> new IllegalStateException("已生成正式任务的草稿不可修改");
            case READY_REQUIRES_ASSIGNEE -> new IllegalArgumentException("生成正式任务前必须指定责任人");
            case ONLY_READY_DRAFTS_CAN_GENERATE_TASKS -> new IllegalArgumentException("仅 READY 状态的评分草稿可生成正式任务");
        };
    }

    private ScoreDraftPolicy.DraftStatus toCoreStatus(ProjectScoreDraft.Status status) {
        if (status == null) {
            return null;
        }
        return ScoreDraftPolicy.DraftStatus.valueOf(status.name());
    }

    private ProjectScoreDraft.Status toEntityStatus(ScoreDraftPolicy.DraftStatus status) {
        if (status == null) {
            return null;
        }
        return ProjectScoreDraft.Status.valueOf(status.name());
    }
}
