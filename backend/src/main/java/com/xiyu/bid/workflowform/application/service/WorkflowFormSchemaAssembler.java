package com.xiyu.bid.workflowform.application.service;

import com.xiyu.bid.workflowform.application.port.WorkflowFormTemplateRecord;
import com.xiyu.bid.workflowform.domain.FormFieldDefinition;
import com.xiyu.bid.workflowform.domain.FormFieldType;
import com.xiyu.bid.workflowform.domain.FormSchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class WorkflowFormSchemaAssembler {

    public FormSchema toSchema(WorkflowFormTemplateRecord record) {
        return new FormSchema(record.templateCode(), record.businessType(), fields(record.schema()));
    }

    private List<FormFieldDefinition> fields(Map<String, Object> schema) {
        Object rawFields = schema == null ? null : schema.get("fields");
        if (!(rawFields instanceof List<?> items)) {
            return List.of();
        }
        return items.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(this::field)
                .toList();
    }

    private FormFieldDefinition field(Map<?, ?> value) {
        String key = stringValue(value.get("key"));
        String label = stringValue(value.get("label"));
        String type = stringValue(value.get("type"));
        boolean required = Boolean.TRUE.equals(value.get("required"));
        return new FormFieldDefinition(key, label == null ? key : label, fieldType(type), required);
    }

    private FormFieldType fieldType(String value) {
        if (value == null) {
            return FormFieldType.TEXT;
        }
        return FormFieldType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private String stringValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value).trim();
    }
}
