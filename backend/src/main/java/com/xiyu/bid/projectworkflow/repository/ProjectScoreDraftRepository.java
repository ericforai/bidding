package com.xiyu.bid.projectworkflow.repository;

import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProjectScoreDraftRepository extends JpaRepository<ProjectScoreDraft, Long> {

    List<ProjectScoreDraft> findByProjectIdOrderByCategoryAscSourceTableIndexAscSourceRowIndexAsc(Long projectId);

    List<ProjectScoreDraft> findByProjectIdAndStatusIn(Long projectId, Collection<ProjectScoreDraft.Status> statuses);

    void deleteByProjectIdAndStatusIn(Long projectId, Collection<ProjectScoreDraft.Status> statuses);
}
