package com.xiyu.bid.projectworkflow.repository;

import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    List<ProjectDocument> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
