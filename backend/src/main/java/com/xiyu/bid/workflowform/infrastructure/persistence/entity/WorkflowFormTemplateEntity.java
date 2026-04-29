package com.xiyu.bid.workflowform.infrastructure.persistence.entity;

import com.xiyu.bid.workflowform.domain.FormBusinessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workflow_form_templates")
@Getter
@Setter
public class WorkflowFormTemplateEntity {
    @Id
    @Column(name = "template_code", length = 80)
    private String templateCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 64)
    private FormBusinessType businessType;

    @Column(nullable = false)
    private Integer version;

    @Lob
    @Column(name = "schema_json", nullable = false)
    private String schemaJson;

    @Column(nullable = false)
    private boolean enabled;
}
