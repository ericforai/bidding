package com.xiyu.bid.biddraftagent.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BidDraftAgentDomainTest {

    @Test
    void domain_policies_should_classify_score_and_cover_a_rich_snapshot() {
        BidDraftSnapshot snapshot = new BidDraftSnapshot(
                11L,
                22L,
                "华东智慧园区改造项目",
                "项目需要报价、合同法务条款、实施方案和交付验收材料",
                "已确认招标背景和客户范围",
                "西域智算中心",
                "重点客户",
                "上海",
                "信息化",
                new BigDecimal("5000000"),
                LocalDate.of(2026, 5, 30),
                "2026园区改造招标公告",
                "投标要求包含资质、报价、合同和实施计划",
                "上海采购集团",
                "公开招标",
                List.of("智慧园区", "改造"),
                List.of("建筑业企业资质证书 / CONSTRUCTION / FIRST", "安全生产许可证 / SERVICE / SECOND"),
                List.of("法务合同模板 / LEGAL / 投标说明"),
                List.of("智慧园区实施案例 / 方案 / 交付验收 / 售后支持")
        );

        RequirementClassification classification = new RequirementClassificationPolicy().classify(snapshot);
        MaterialMatchScore materialMatchScore = new MaterialMatchScoringPolicy().score(snapshot, classification);
        GapCheckResult gapCheck = new GapCheckPolicy().check(snapshot, classification, materialMatchScore);
        ManualConfirmationDecision manualConfirmation = new ManualConfirmationPolicy().evaluate(classification, gapCheck);
        WriteCoverageDecision writeCoverage = new WriteCoveragePolicy().evaluate(snapshot, classification, materialMatchScore, gapCheck, manualConfirmation);

        assertThat(classification.categories())
                .containsExactly("pricing", "legal", "qualification", "technical", "delivery", "commercial");
        assertThat(materialMatchScore.score()).isEqualTo(100);
        assertThat(materialMatchScore.matchedCategoryCount()).isEqualTo(6);
        assertThat(gapCheck.ready()).isTrue();
        assertThat(gapCheck.gaps()).isEmpty();
        assertThat(manualConfirmation.requiresConfirmation()).isTrue();
        assertThat(writeCoverage.sufficient()).isTrue();
        assertThat(writeCoverage.coverageScore()).isGreaterThanOrEqualTo(60);
    }
}
