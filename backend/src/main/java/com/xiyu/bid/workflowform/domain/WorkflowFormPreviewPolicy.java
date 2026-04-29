package com.xiyu.bid.workflowform.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WorkflowFormPreviewPolicy {

    private WorkflowFormPreviewPolicy() {
    }

    public static Map<String, Object> previewPayload(
            Map<String, Object> mapping,
            Map<String, Object> formData,
            Map<String, Object> context,
            Map<String, Object> applicant
    ) {
        Map<String, Object> mainFields = new LinkedHashMap<>();
        for (Map<String, Object> field : WorkflowFormOaMappingPolicy.mainFields(mapping)) {
            String source = WorkflowFormSchemaPolicy.text(field.get("source"));
            String target = WorkflowFormSchemaPolicy.text(field.get("target"));
            if (!source.isBlank() && !target.isBlank() && WorkflowFormOaMappingPolicy.safeSource(source)) {
                mainFields.put(target, resolve(source, formData, context, applicant));
            }
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workflowCode", WorkflowFormSchemaPolicy.text(mapping.get("workflowCode")));
        payload.put("trial", true);
        payload.put("mainFields", mainFields);
        return payload;
    }

    private static Object resolve(String source, Map<String, Object> formData, Map<String, Object> context, Map<String, Object> applicant) {
        if (source.startsWith("formData.")) {
            return formData.get(source.substring("formData.".length()));
        }
        if (source.startsWith("context.")) {
            return context.get(source.substring("context.".length()));
        }
        if (source.startsWith("applicant.")) {
            return applicant.get(source.substring("applicant.".length()));
        }
        return null;
    }
}
