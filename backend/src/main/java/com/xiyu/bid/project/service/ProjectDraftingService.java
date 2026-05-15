// Input: 项目 id、leads 请求、当前用户；依赖 ProjectLeadAssignmentRepository + TaskRepository + ProjectStageService
// Output: ProjectDraftingViewDto；纯编排，核心规则委托给 AllTasksCompletedPolicy；§3.6 CLOSED 全字段锁定
// Pos: project/service/ - 编排层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.project.core.AllTasksCompletedPolicy;
import com.xiyu.bid.project.core.ProjectFieldLockPolicy;
import com.xiyu.bid.project.core.ProjectStage;
import com.xiyu.bid.project.dto.ProjectDraftingViewDto;
import com.xiyu.bid.project.dto.ProjectLeadAssignmentRequest;
import com.xiyu.bid.project.entity.ProjectLeadAssignment;
import com.xiyu.bid.project.repository.ProjectLeadAssignmentRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PRD §3.2 标书编制阶段编排服务：主/副负责人分配 + 推进闸门。
 * <p>不持有任务 CRUD 逻辑，仅读取任务状态委托 {@link AllTasksCompletedPolicy}。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectDraftingService {

    private final ProjectLeadAssignmentRepository leadRepo;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectStageService projectStageService;

    @Auditable(action = "ASSIGN_PROJECT_LEADS", entityType = "ProjectLeadAssignment",
            description = "分配主/副投标负责人")
    public ProjectDraftingViewDto assignLeads(
            Long projectId, ProjectLeadAssignmentRequest req, Long currentUserId) {
        mustGetProject(projectId);
        // §3.6 全字段锁定 — CLOSED 阶段拒绝写入。
        ProjectStage stage = projectStageService.currentStage(projectId);
        var lockDecision = ProjectFieldLockPolicy.assertWritable(stage, "leads");
        if (!lockDecision.allowed()) {
            var deny = (ProjectFieldLockPolicy.Decision.Deny) lockDecision;
            throw new ResponseStatusException(HttpStatus.LOCKED, deny.reason());
        }
        if (req == null || req.getPrimaryLeadUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "主投标负责人不能为空");
        }
        if (req.getSecondaryLeadUserId() != null
                && req.getSecondaryLeadUserId().equals(req.getPrimaryLeadUserId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "主/副负责人不能相同");
        }
        ProjectLeadAssignment entity = leadRepo.findByProjectId(projectId)
                .orElseGet(() -> ProjectLeadAssignment.builder().projectId(projectId).build());
        entity.setPrimaryLeadUserId(req.getPrimaryLeadUserId());
        entity.setSecondaryLeadUserId(req.getSecondaryLeadUserId());
        entity.setAssignedAt(LocalDateTime.now());
        entity.setAssignedBy(currentUserId);
        ProjectLeadAssignment saved = leadRepo.save(entity);
        log.info("Project leads assigned project={} primary={} secondary={} by={}",
                projectId, saved.getPrimaryLeadUserId(), saved.getSecondaryLeadUserId(), currentUserId);
        return toView(projectId, saved, gateDecision(projectId));
    }

    /**
     * §3.2.3 闸门检查：所有任务都完成才允许推进到 EVALUATING。
     * 返回 decision view；实际 stage 推进由 WS-G 编排层在调用方处理。
     */
    @Auditable(action = "GATE_ADVANCE_TO_EVALUATION", entityType = "Project",
            description = "DRAFTING → EVALUATING 闸门检查")
    public ProjectDraftingViewDto gateAdvanceToEvaluation(Long projectId, Long currentUserId) {
        mustGetProject(projectId);
        AllTasksCompletedPolicy.Decision d = gateDecision(projectId);
        if (!d.allowed()) {
            int incomplete = ((AllTasksCompletedPolicy.Decision.Deny) d).incompleteCount();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "仍有 " + incomplete + " 个任务未完成，无法推进到评标");
        }
        ProjectLeadAssignment lead = leadRepo.findByProjectId(projectId).orElse(null);
        log.info("Drafting gate passed project={} user={}", projectId, currentUserId);
        return toView(projectId, lead, d);
    }

    @Transactional(readOnly = true)
    public ProjectDraftingViewDto get(Long projectId) {
        mustGetProject(projectId);
        ProjectLeadAssignment lead = leadRepo.findByProjectId(projectId).orElse(null);
        return toView(projectId, lead, gateDecision(projectId));
    }

    private AllTasksCompletedPolicy.Decision gateDecision(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        List<AllTasksCompletedPolicy.TaskState> states = tasks.stream()
                .map(t -> t.getStatus() == null
                        ? AllTasksCompletedPolicy.TaskState.TODO
                        : AllTasksCompletedPolicy.TaskState.valueOf(t.getStatus().name()))
                .toList();
        return AllTasksCompletedPolicy.decide(states);
    }

    private Project mustGetProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
    }

    private ProjectDraftingViewDto toView(Long projectId, ProjectLeadAssignment lead,
                                          AllTasksCompletedPolicy.Decision decision) {
        int incomplete = decision instanceof AllTasksCompletedPolicy.Decision.Deny d
                ? d.incompleteCount() : 0;
        return ProjectDraftingViewDto.builder()
                .projectId(projectId)
                .primaryLeadUserId(lead == null ? null : lead.getPrimaryLeadUserId())
                .secondaryLeadUserId(lead == null ? null : lead.getSecondaryLeadUserId())
                .incompleteTaskCount(incomplete)
                .gateReady(decision.allowed())
                .build();
    }
}
