package com.xiyu.bid.workflowform.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FormSubmissionValidatorTest {

    @Test
    void rejects_empty_required_qualification_borrow_values() {
        ValidationResult result = FormSubmissionValidator.validateQualificationBorrow(Map.of(
                "qualificationId", "",
                "borrower", "",
                "department", "投标管理部",
                "projectId", "",
                "purpose", "",
                "expectedReturnDate", ""
        ));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains(
                "请选择资质",
                "请填写借用人",
                "请选择项目",
                "请填写用途",
                "请选择预计归还日期"
        );
    }

    @Test
    void accepts_complete_qualification_borrow_values() {
        ValidationResult result = FormSubmissionValidator.validateQualificationBorrow(Map.of(
                "qualificationId", "1001",
                "borrower", "小王",
                "department", "投标管理部",
                "projectId", "P-2026-001",
                "purpose", "用于投标文件编制",
                "expectedReturnDate", "2026-05-10"
        ));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }
}
