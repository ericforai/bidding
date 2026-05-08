// Input: Mockito 桩仓库
// Output: 提交/审核/驳回行为断言
// Pos: backend test source
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.entity.Project;
import com.xiyu.bid.project.entity.ProjectRetrospective;
import com.xiyu.bid.project.core.BidResultType;
import com.xiyu.bid.project.dto.RetrospectiveReviewRequest;
import com.xiyu.bid.project.dto.RetrospectiveSubmitRequest;
import com.xiyu.bid.project.repository.ProjectRetrospectiveRepository;
import com.xiyu.bid.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRetrospectiveServiceTest {

    private ProjectRetrospectiveRepository repo;
    private ProjectRepository projectRepo;
    private ProjectRetrospectiveService service;

    @BeforeEach
    void setup() {
        repo = mock(ProjectRetrospectiveRepository.class);
        projectRepo = mock(ProjectRepository.class);
        service = new ProjectRetrospectiveService(repo, projectRepo);
        Project p = new Project();
        p.setId(1L);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
    }

    @Test
    void submit_won_complete_persistsPending() {
        when(repo.findByProjectId(1L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var req = RetrospectiveSubmitRequest.builder()
                .resultType(BidResultType.WON)
                .summary("亮点").winFactors("优势").improvementActions("建议").build();
        var dto = service.submit(1L, req, 99L);
        assertEquals("PENDING_REVIEW", dto.getReviewStatus());
        assertEquals("WON", dto.getResultType());
        verify(repo).save(any(ProjectRetrospective.class));
    }

    @Test
    void submit_lost_missing_throws422() {
        var req = RetrospectiveSubmitRequest.builder()
                .resultType(BidResultType.LOST)
                .lossReasons("丢标").build();
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.submit(1L, req, 99L));
        assertEquals(422, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void review_approve_setsApproved() {
        ProjectRetrospective existing = ProjectRetrospective.builder()
                .id(10L).projectId(1L)
                .reviewStatus(ProjectRetrospective.ReviewStatus.PENDING_REVIEW.name())
                .build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var dto = service.review(1L,
                RetrospectiveReviewRequest.builder().approve(true).build(), 7L);
        assertEquals("APPROVED", dto.getReviewStatus());
        assertEquals(7L, dto.getReviewedBy());
    }

    @Test
    void review_reject_requiresComment() {
        ProjectRetrospective existing = ProjectRetrospective.builder()
                .projectId(1L).reviewStatus("PENDING_REVIEW").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.review(1L,
                        RetrospectiveReviewRequest.builder().approve(false).comment(" ").build(), 7L));
        assertEquals(422, ex.getStatusCode().value());
    }

    @Test
    void review_reject_withComment_setsRejected() {
        ProjectRetrospective existing = ProjectRetrospective.builder()
                .projectId(1L).reviewStatus("PENDING_REVIEW").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var dto = service.review(1L,
                RetrospectiveReviewRequest.builder().approve(false).comment("缺细节").build(), 7L);
        assertEquals("REJECTED", dto.getReviewStatus());
        assertEquals("缺细节", dto.getReviewComment());
    }

    @Test
    void review_alreadyApproved_conflict() {
        ProjectRetrospective existing = ProjectRetrospective.builder()
                .projectId(1L).reviewStatus("APPROVED").build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.review(1L,
                        RetrospectiveReviewRequest.builder().approve(true).build(), 7L));
        assertEquals(409, ex.getStatusCode().value());
    }
}
