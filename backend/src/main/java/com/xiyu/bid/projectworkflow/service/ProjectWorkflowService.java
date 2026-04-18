// Input: task/document/score-draft workflow services
// Output: Project workflow facade methods for controllers
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.projectworkflow.service;

import com.xiyu.bid.projectworkflow.dto.ProjectDocumentCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectDocumentDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectReminderCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectReminderDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftGenerateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftParseResponse;
import com.xiyu.bid.projectworkflow.dto.ProjectScoreDraftUpdateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectShareLinkCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectShareLinkDTO;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskCreateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskStatusUpdateRequest;
import com.xiyu.bid.projectworkflow.dto.ProjectTaskViewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectWorkflowService {

    private final ProjectTaskWorkflowService taskWorkflowService;
    private final ProjectDocumentWorkflowService documentWorkflowService;
    private final ProjectScoreDraftWorkflowService scoreDraftWorkflowService;

    @Transactional(readOnly = true)
    public List<ProjectTaskViewDTO> getProjectTasks(Long projectId) {
        return taskWorkflowService.getProjectTasks(projectId);
    }

    public ProjectTaskViewDTO createProjectTask(Long projectId, ProjectTaskCreateRequest request) {
        return taskWorkflowService.createProjectTask(projectId, request);
    }

    public ProjectTaskViewDTO updateProjectTaskStatus(Long projectId, Long taskId, ProjectTaskStatusUpdateRequest request) {
        return taskWorkflowService.updateProjectTaskStatus(projectId, taskId, request);
    }

    @Transactional(readOnly = true)
    public List<ProjectDocumentDTO> getProjectDocuments(Long projectId) {
        return documentWorkflowService.getProjectDocuments(projectId);
    }

    public ProjectDocumentDTO createProjectDocument(Long projectId, ProjectDocumentCreateRequest request) {
        return documentWorkflowService.createProjectDocument(projectId, request);
    }

    public void deleteProjectDocument(Long projectId, Long documentId) {
        documentWorkflowService.deleteProjectDocument(projectId, documentId);
    }

    @Transactional(readOnly = true)
    public List<ProjectReminderDTO> getProjectReminders(Long projectId) {
        return documentWorkflowService.getProjectReminders(projectId);
    }

    public ProjectReminderDTO createProjectReminder(Long projectId, ProjectReminderCreateRequest request) {
        return documentWorkflowService.createProjectReminder(projectId, request);
    }

    @Transactional(readOnly = true)
    public List<ProjectShareLinkDTO> getProjectShareLinks(Long projectId) {
        return documentWorkflowService.getProjectShareLinks(projectId);
    }

    public ProjectShareLinkDTO createProjectShareLink(Long projectId, ProjectShareLinkCreateRequest request) {
        return documentWorkflowService.createProjectShareLink(projectId, request);
    }

    @Transactional(readOnly = true)
    public List<ProjectScoreDraftDTO> getProjectScoreDrafts(Long projectId) {
        return scoreDraftWorkflowService.getProjectScoreDrafts(projectId);
    }

    public ProjectScoreDraftParseResponse parseProjectScoreDrafts(Long projectId, MultipartFile file) {
        return scoreDraftWorkflowService.parseProjectScoreDrafts(projectId, file);
    }

    public ProjectScoreDraftDTO updateProjectScoreDraft(Long projectId, Long draftId, ProjectScoreDraftUpdateRequest request) {
        return scoreDraftWorkflowService.updateProjectScoreDraft(projectId, draftId, request);
    }

    public List<ProjectTaskViewDTO> generateTasksFromScoreDrafts(Long projectId, ProjectScoreDraftGenerateRequest request) {
        return scoreDraftWorkflowService.generateTasksFromScoreDrafts(projectId, request);
    }

    public void clearNonGeneratedDrafts(Long projectId) {
        scoreDraftWorkflowService.clearNonGeneratedDrafts(projectId);
    }
}
