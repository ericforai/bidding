package com.xiyu.bid.biddraftagent.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenderRequirementProfileTest {

    @Test
    void constructor_shouldDropObviousMetadataNoiseFromReadableLists() {
        TenderRequirementProfile profile = new TenderRequirementProfile(
                "项目A",
                "招标文件",
                "范围",
                "采购人",
                List.of("供应商须具备独立法人资格", "qualification", "true", "50", "null", "未明确提及，根据通用要求推断"),
                List.of("提供实施方案", "technical", "100"),
                List.of("响应付款条款", "commercial", "false"),
                List.of("技术方案50分", "scoring", "100"),
                "2026-05-31",
                List.of("营业执照", "material", "100"),
                List.of("履约保证金40万元", "risk", "null"),
                List.of("电子商城", "工业品电商"),
                List.of()
        );

        assertThat(profile.qualificationRequirements()).containsExactly("供应商须具备独立法人资格");
        assertThat(profile.technicalRequirements()).containsExactly("提供实施方案");
        assertThat(profile.commercialRequirements()).containsExactly("响应付款条款");
        assertThat(profile.scoringCriteria()).containsExactly("技术方案50分");
        assertThat(profile.requiredMaterials()).containsExactly("营业执照");
        assertThat(profile.riskPoints()).containsExactly("履约保证金40万元");
        assertThat(profile.tags()).containsExactly("电子商城", "工业品电商");
    }
}
