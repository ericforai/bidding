package com.xiyu.bid.workflowform.domain;

public record FormFieldDefinition(String key, String label, FormFieldType type, boolean required) {
}
