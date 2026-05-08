// Input: Mockito 桩仓库
// Output: 子状态切换 happy / 拒绝 / ANNOUNCED 自动推进 stage 行为断言
// Pos: backend test source
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.entity.Project;
import com.xiyu.bid.project.entity.ProjectEvaluation;
import com.xiyu.bid.project.core.EvaluationSubStage;
import com.xiyu.bid.project.dto.EvaluationEvidenceAttachRequest;
import com.xiyu.bid.project.dto.EvaluationSubStageUpdateRequest;
import com.xiyu.bid.project.repository.ProjectEvaluationRepository;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectEvaluationServiceTest {

    private ProjectEvaluationRepository repo;
    private ProjectRepository projectRepo;
    private ProjectDocumentRepository docRepo;
    private EntityManager entityManager;
    private Query nativeQuery;
    private ProjectEvaluationService service;

    @BeforeEach
    void setup() {
        repo = mock(ProjectEvaluationRepository.class);
        projectRepo = mock(ProjectRepository.class);
        docRepo = mock(ProjectDocumentRepository.class);
        entityManager = mock(EntityManager.class);
        nativeQuery = mock(Query.class);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.executeUpdate()).thenReturn(1);
        service = new ProjectEvaluationService(repo, projectRepo, docRepo);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        Project p = new Project();
        p.setId(1L);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        lenient().when(docRepo.findByProjectIdAndFiltersOrderByCreatedAtDesc(any(), any(), any(), any()))
                .thenReturn(List.of());
        lenient().when(repo.save(any())).thenAnswer(inv -> {
            ProjectEvaluation e = inv.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
    }

    @Test
    void transition_initToAwaitingBoard_happy() {
        ProjectEvaluation existing = ProjectEvaluation.builder()
                .id(10L).projectId(1L).subStage("IN_PROGRESS").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        var req = EvaluationSubStageUpdateRequest.builder()
                .targetSubStage(EvaluationSubStage.AWAITING_BOARD).build();
        var dto = service.transitionSubStage(1L, req, 7L);
        assertEquals("AWAITING_BOARD", dto.getSubStage());
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void transition_skip_in_progress_to_announced_conflict() {
        ProjectEvaluation existing = ProjectEvaluation.builder()
                .id(10L).projectId(1L).subStage("IN_PROGRESS").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        var req = EvaluationSubStageUpdateRequest.builder()
                .targetSubStage(EvaluationSubStage.ANNOUNCED).build();
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.transitionSubStage(1L, req, 7L));
        assertEquals(409, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void transition_reverse_denied() {
        ProjectEvaluation existing = ProjectEvaluation.builder()
                .id(10L).projectId(1L).subStage("AWAITING_BOARD").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        var req = EvaluationSubStageUpdateRequest.builder()
                .targetSubStage(EvaluationSubStage.IN_PROGRESS).build();
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.transitionSubStage(1L, req, 7L));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void transition_announced_autoAdvancesProjectStage() {
        ProjectEvaluation existing = ProjectEvaluation.builder()
                .id(10L).projectId(1L).subStage("AWAITING_BOARD").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        var req = EvaluationSubStageUpdateRequest.builder()
                .targetSubStage(EvaluationSubStage.ANNOUNCED).build();
        var dto = service.transitionSubStage(1L, req, 7L);
        assertEquals("ANNOUNCED", dto.getSubStage());
        verify(entityManager, times(1)).createNativeQuery(anyString());
        verify(nativeQuery).executeUpdate();
    }

    @Test
    void transition_initialEntityCreatedIfMissing() {
        when(repo.findByProjectId(1L)).thenReturn(Optional.empty());
        var req = EvaluationSubStageUpdateRequest.builder()
                .targetSubStage(EvaluationSubStage.AWAITING_BOARD).build();
        var dto = service.transitionSubStage(1L, req, 7L);
        assertEquals("AWAITING_BOARD", dto.getSubStage());
        verify(repo).save(any(ProjectEvaluation.class));
    }

    @Test
    void attachEvidence_linksDocs() {
        ProjectEvaluation existing = ProjectEvaluation.builder()
                .id(10L).projectId(1L).subStage("IN_PROGRESS").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        ProjectDocument d1 = ProjectDocument.builder().id(50L).projectId(1L).build();
        when(docRepo.findById(50L)).thenReturn(Optional.of(d1));
        var req = EvaluationEvidenceAttachRequest.builder()
                .fileIds(List.of(50L)).build();
        service.attachEvidence(1L, req, 7L);
        verify(docRepo).save(d1);
        assertEquals("EVALUATION", d1.getLinkedEntityType());
        assertEquals(10L, d1.getLinkedEntityId());
    }

    @Test
    void attachEvidence_docOfDifferentProject_rejected() {
        ProjectEvaluation existing = ProjectEvaluation.builder()
                .id(10L).projectId(1L).subStage("IN_PROGRESS").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        ProjectDocument d1 = ProjectDocument.builder().id(50L).projectId(99L).build();
        when(docRepo.findById(50L)).thenReturn(Optional.of(d1));
        var req = EvaluationEvidenceAttachRequest.builder()
                .fileIds(List.of(50L)).build();
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.attachEvidence(1L, req, 7L));
        assertEquals(422, ex.getStatusCode().value());
    }
}
