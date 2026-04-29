package com.xiyu.bid.workflowform.infrastructure.persistence.repository;

import com.xiyu.bid.workflowform.infrastructure.persistence.entity.WorkflowFormTemplateVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowFormTemplateVersionJpaRepository extends JpaRepository<WorkflowFormTemplateVersionEntity, Long> {
    List<WorkflowFormTemplateVersionEntity> findByTemplateCodeOrderByVersionDesc(String templateCode);

    @Query("select coalesce(max(v.version), 0) from WorkflowFormTemplateVersionEntity v where v.templateCode = :templateCode")
    Integer findMaxVersion(@Param("templateCode") String templateCode);
}
